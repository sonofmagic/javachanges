package io.github.sonofmagic.javachanges.core.gitlab;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.sonofmagic.javachanges.core.ReleaseEnvJsonSupport;
import io.github.sonofmagic.javachanges.core.ReleaseEnvRuntime;
import io.github.sonofmagic.javachanges.core.ReleaseUtils;
import io.github.sonofmagic.javachanges.core.config.ChangesetConfigSupport;
import io.github.sonofmagic.javachanges.core.env.EnvEntry;
import io.github.sonofmagic.javachanges.core.env.EnvValue;
import io.github.sonofmagic.javachanges.core.env.LoadedEnv;
import io.github.sonofmagic.javachanges.core.env.ReleaseEnvCatalog;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GitlabProtectionSupport {
    private final ReleaseEnvRuntime runtime;
    private final PrintStream out;

    public GitlabProtectionSupport(ReleaseEnvRuntime runtime, PrintStream out) {
        this.runtime = runtime;
        this.out = out;
    }

    public GitlabProtectionCheck inspectProtection(String gitlabRepo, LoadedEnv env,
                                                   ChangesetConfigSupport.ChangesetConfig changesetConfig,
                                                   ReleaseEnvJsonSupport.JsonSection protectedVariablesSection,
                                                   ReleaseEnvJsonSupport.JsonSection protectedBranchesSection,
                                                   boolean textOutput) throws IOException, InterruptedException {
        GitlabProtectionCheck result = new GitlabProtectionCheck();
        String projectRef = urlEncode(gitlabRepo);
        String variablesJson = runtime.runAndCapture(Arrays.asList(
            "glab", "api", "projects/" + projectRef + "/variables?per_page=100"
        )).stdoutText();
        Map<String, GitlabVariableMetadata> remoteVariables = parseGitlabVariableMetadata(variablesJson);
        for (EnvEntry entry : ReleaseEnvCatalog.GITLAB_VARIABLES) {
            if (!entry.protectedValue) {
                continue;
            }
            EnvValue localValue = env.value(entry.name);
            if (!localValue.isReal()) {
                protectedVariablesSection.add(entry.name, "SKIPPED");
                if (textOutput) {
                    printStatus(entry.name, "SKIPPED");
                }
                continue;
            }
            GitlabVariableMetadata metadata = remoteVariables.get(entry.name);
            String status;
            if (metadata == null) {
                status = "MISSING_REMOTE";
                result.failed = true;
                result.suggestions.add("在 GitLab 项目变量中创建 " + entry.name + "，并勾选 protected");
            } else if (!metadata.protectedValue) {
                status = "NOT_PROTECTED";
                result.failed = true;
                result.suggestions.add("把 GitLab 项目变量 " + entry.name + " 改为 protected");
            } else {
                status = "PROTECTED";
            }
            protectedVariablesSection.add(entry.name, status);
            if (textOutput) {
                printStatus(entry.name, status);
            }
        }

        if (textOutput) {
            out.println();
            out.println("== GitLab Protected Branches ==");
        }
        boolean hasProtectedSecrets = remoteHasProtectedSecrets(remoteVariables);
        if (changesetConfig.hasSnapshotBranch()) {
            String snapshotBranch = changesetConfig.snapshotBranch();
            String branchesJson = runtime.runAndCapture(Arrays.asList(
                "glab", "api", "projects/" + projectRef + "/protected_branches?per_page=100"
            )).stdoutText();
            boolean protectedBranch = isProtectedBranch(parseProtectedBranchNames(branchesJson), snapshotBranch);
            String status = protectedBranch ? "PROTECTED" : "UNPROTECTED";
            if (hasProtectedSecrets && !protectedBranch) {
                status = "UNPROTECTED_WITH_PROTECTED_VARIABLES";
                result.failed = true;
                result.suggestions.add("保护 GitLab 分支 " + snapshotBranch + "，否则 protected variables 不会注入该分支的 pipeline");
                result.suggestions.add("或者取消相关 Maven / release 变量的 protected 标记，但这会降低安全性");
            } else if (!protectedBranch) {
                result.failed = true;
                result.suggestions.add("保护 GitLab 分支 " + snapshotBranch + "，让 snapshot 发布与 protected variables 行为一致");
            }
            protectedBranchesSection.add(snapshotBranch, status);
            if (textOutput) {
                printStatus(snapshotBranch, status);
            }
        } else {
            protectedBranchesSection.add("snapshotBranch", "SKIPPED");
            if (textOutput) {
                printStatus("snapshotBranch", "SKIPPED");
            }
        }
        return result;
    }

    private void printStatus(String label, String value) {
        out.printf("%-40s %s%n", label, value);
    }

    private boolean remoteHasProtectedSecrets(Map<String, GitlabVariableMetadata> remoteVariables) {
        for (GitlabVariableMetadata metadata : remoteVariables.values()) {
            if (metadata.protectedValue) {
                return true;
            }
        }
        return false;
    }

    private Map<String, GitlabVariableMetadata> parseGitlabVariableMetadata(String json) {
        Map<String, GitlabVariableMetadata> result = new LinkedHashMap<String, GitlabVariableMetadata>();
        JsonNode root = ReleaseUtils.readJsonTree(json);
        if (!root.isArray()) {
            return result;
        }
        for (JsonNode node : root) {
            String key = jsonText(node, "key");
            if (key == null) {
                continue;
            }
            result.put(key, new GitlabVariableMetadata(
                key,
                jsonBoolean(node, "protected"),
                jsonBoolean(node, "masked")
            ));
        }
        return result;
    }

    private List<String> parseProtectedBranchNames(String json) {
        List<String> names = new ArrayList<String>();
        JsonNode root = ReleaseUtils.readJsonTree(json);
        if (!root.isArray()) {
            return names;
        }
        for (JsonNode node : root) {
            String name = jsonText(node, "name");
            if (name != null) {
                names.add(name);
            }
        }
        return names;
    }

    private boolean isProtectedBranch(List<String> protectedBranches, String branchName) {
        for (String candidate : protectedBranches) {
            if (branchMatches(candidate, branchName)) {
                return true;
            }
        }
        return false;
    }

    private boolean branchMatches(String pattern, String branchName) {
        if (pattern.equals(branchName)) {
            return true;
        }
        if (pattern.indexOf('*') >= 0) {
            String regex = pattern.replace(".", "\\.").replace("*", ".*");
            return branchName.matches(regex);
        }
        return false;
    }

    private String jsonText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asText();
    }

    private boolean jsonBoolean(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && !value.isNull() && value.asBoolean(false);
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to encode GitLab project path", exception);
        }
    }

    static final class GitlabVariableMetadata {
        final String key;
        final boolean protectedValue;
        final boolean masked;

        GitlabVariableMetadata(String key, boolean protectedValue, boolean masked) {
            this.key = key;
            this.protectedValue = protectedValue;
            this.masked = masked;
        }
    }

    public static final class GitlabProtectionCheck {
        public boolean failed;
        public final List<String> suggestions = new ArrayList<String>();
    }
}
