package io.github.sonofmagic.javachanges.core.env;

import io.github.sonofmagic.javachanges.core.OutputFormat;
import io.github.sonofmagic.javachanges.core.ReleaseEnvJsonSupport;
import io.github.sonofmagic.javachanges.core.ReleaseEnvRuntime;
import io.github.sonofmagic.javachanges.core.ReleaseUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

final class ReleaseEnvAuditSupport {
    private final Path repoRoot;
    private final PrintStream out;
    private final ReleaseEnvRuntime runtime;

    ReleaseEnvAuditSupport(Path repoRoot, PrintStream out, ReleaseEnvRuntime runtime) {
        this.repoRoot = repoRoot;
        this.out = out;
        this.runtime = runtime;
    }

    boolean auditVars(AuditVarsRequest request, LoadedEnv env, String envPath) throws IOException, InterruptedException {
        boolean textOutput = request.format != OutputFormat.JSON;
        boolean failed = false;
        List<ReleaseEnvJsonSupport.JsonSection> sections = new java.util.ArrayList<ReleaseEnvJsonSupport.JsonSection>();
        if (textOutput) {
            out.println("使用 env 文件: " + envPath);
        }

        if (request.platform.includesGithub()) {
            failed = auditGithub(request, env, textOutput, sections) || failed;
        }

        if (request.platform.includesGitlab()) {
            failed = auditGitlab(request, env, textOutput, sections) || failed;
        }

        if (textOutput) {
            out.println();
        }
        if (failed) {
            if (request.format == OutputFormat.JSON) {
                out.println(ReleaseEnvJsonSupport.commandReportJson("audit-vars", false, envPath, request.platform.id,
                    false, sections, Collections.<String>emptyList(),
                    "平台变量审计失败，请修正 MISSING_REMOTE / MISMATCH 项后重试"));
                return false;
            }
            throw new IllegalStateException("平台变量审计失败，请修正 MISSING_REMOTE / MISMATCH 项后重试");
        }
        if (textOutput) {
            out.println("平台变量审计通过");
        }
        if (request.format == OutputFormat.JSON) {
            out.println(ReleaseEnvJsonSupport.commandReportJson("audit-vars", true, envPath, request.platform.id,
                false, sections, Collections.<String>emptyList(), null));
        }
        return true;
    }

    private boolean auditGithub(AuditVarsRequest request, LoadedEnv env, boolean textOutput,
                                List<ReleaseEnvJsonSupport.JsonSection> sections)
        throws IOException, InterruptedException {
        ReleaseEnvJsonSupport.JsonSection githubPreconditions = new ReleaseEnvJsonSupport.JsonSection("GitHub Audit Preconditions");
        ReleaseEnvJsonSupport.JsonSection githubVariablesSection = new ReleaseEnvJsonSupport.JsonSection("GitHub Variables 审计");
        ReleaseEnvJsonSupport.JsonSection githubSecretsSection = new ReleaseEnvJsonSupport.JsonSection("GitHub Secrets 审计");
        sections.add(githubPreconditions);
        sections.add(githubVariablesSection);
        sections.add(githubSecretsSection);
        if (ReleaseUtils.isBlank(request.githubRepo)) {
            githubPreconditions.add("GITHUB_REPO", "MISSING");
            failPrecondition(request, env, sections, "缺少仓库参数: GITHUB_REPO");
        }
        githubPreconditions.add("GITHUB_REPO", request.githubRepo);
        if (!runtime.commandExists("gh")) {
            githubPreconditions.add("gh", "MISSING");
            failPrecondition(request, env, sections, "未找到 gh CLI");
        }
        githubPreconditions.add("gh", "OK");
        if (!runtime.runQuietly(Arrays.asList("gh", "auth", "status"))) {
            githubPreconditions.add("gh auth status", "FAILED");
            failPrecondition(request, env, sections, "gh auth status 失败，请先执行 make auth-help");
        }
        githubPreconditions.add("gh auth status", "OK");
        String variablesJson = runtime.runAndCapture(Arrays.asList("gh", "variable", "list", "--repo", request.githubRepo,
            "--json", "name,value,updatedAt")).stdoutText();
        String secretsJson = runtime.runAndCapture(Arrays.asList("gh", "secret", "list", "--repo", request.githubRepo,
            "--json", "name,updatedAt")).stdoutText();
        Map<String, Map<String, String>> githubVariables = ReleaseUtils.parseFlatJsonObjects(variablesJson);
        Map<String, Map<String, String>> githubSecrets = ReleaseUtils.parseFlatJsonObjects(secretsJson);

        boolean failed = false;
        if (textOutput) {
            out.println();
            out.println("== GitHub Variables 审计 ==");
        }
        for (EnvEntry entry : ReleaseEnvCatalog.GITHUB_ACTIONS_VARIABLES) {
            AuditOutcome outcome = auditValue(env.value(entry.name), githubVariables.get(entry.name), true);
            recordAuditStatus(textOutput, githubVariablesSection, entry.name, outcome);
            if (outcome.isFailure()) {
                failed = true;
            }
        }

        if (textOutput) {
            out.println();
            out.println("== GitHub Secrets 审计 ==");
        }
        for (EnvEntry entry : ReleaseEnvCatalog.GITHUB_ACTIONS_SECRETS) {
            AuditOutcome outcome = auditPresence(env.value(entry.name), githubSecrets.get(entry.name));
            recordAuditStatus(textOutput, githubSecretsSection, entry.name, outcome);
            if (outcome.isFailure()) {
                failed = true;
            }
        }
        return failed;
    }

    private boolean auditGitlab(AuditVarsRequest request, LoadedEnv env, boolean textOutput,
                                List<ReleaseEnvJsonSupport.JsonSection> sections)
        throws IOException, InterruptedException {
        ReleaseEnvJsonSupport.JsonSection gitlabPreconditions = new ReleaseEnvJsonSupport.JsonSection("GitLab Audit Preconditions");
        ReleaseEnvJsonSupport.JsonSection gitlabSection = new ReleaseEnvJsonSupport.JsonSection("GitLab Variables 审计");
        sections.add(gitlabPreconditions);
        sections.add(gitlabSection);
        if (ReleaseUtils.isBlank(request.gitlabRepo)) {
            gitlabPreconditions.add("GITLAB_REPO", "MISSING");
            failPrecondition(request, env, sections, "缺少仓库参数: GITLAB_REPO");
        }
        gitlabPreconditions.add("GITLAB_REPO", request.gitlabRepo);
        if (!runtime.commandExists("glab")) {
            gitlabPreconditions.add("glab", "MISSING");
            failPrecondition(request, env, sections, "未找到 glab CLI");
        }
        gitlabPreconditions.add("glab", "OK");
        if (!runtime.runQuietly(Arrays.asList("glab", "auth", "status"))) {
            gitlabPreconditions.add("glab auth status", "FAILED");
            failPrecondition(request, env, sections, "glab auth status 失败，请先执行 make auth-help");
        }
        gitlabPreconditions.add("glab auth status", "OK");
        String exported = runtime.runAndCapture(Arrays.asList("glab", "variable", "export", "--repo", request.gitlabRepo,
            "--output", "env")).stdoutText();
        LoadedEnv remoteEnv = LoadedEnv.parse(exported, repoRoot.resolve("target/gitlab-variables.env"));

        boolean failed = false;
        if (textOutput) {
            out.println();
            out.println("== GitLab Variables 审计 ==");
        }
        for (EnvEntry entry : ReleaseEnvCatalog.GITLAB_VARIABLES) {
            AuditOutcome outcome = auditValue(env.value(entry.name), remoteEnv.value(entry.name), true);
            recordAuditStatus(textOutput, gitlabSection, entry.name, outcome);
            if (outcome.isFailure()) {
                failed = true;
            }
        }
        return failed;
    }

    private void failPrecondition(AuditVarsRequest request, LoadedEnv env, List<ReleaseEnvJsonSupport.JsonSection> sections,
                                  String error) {
        String envPath = runtime.relativizePath(env.path);
        if (request.format == OutputFormat.JSON) {
            out.println(ReleaseEnvJsonSupport.commandReportJson("audit-vars", false, envPath, request.platform.id,
                false, sections, Collections.<String>emptyList(), error));
            throw new AuditPreconditionFailure(false);
        }
        throw new IllegalStateException(error);
    }

    private AuditOutcome auditValue(EnvValue localValue, Map<String, String> remote, boolean compareValue) {
        if (remote == null) {
            return auditValue(localValue, EnvValue.missing(), compareValue);
        }
        return auditValue(localValue, EnvValue.of(remote.get("value")), remote.get("updatedAt"), compareValue);
    }

    private AuditOutcome auditPresence(EnvValue localValue, Map<String, String> remote) {
        boolean remoteExists = remote != null;
        String updatedAt = remote == null ? null : remote.get("updatedAt");
        if (localValue.isReal()) {
            if (!remoteExists) {
                return AuditOutcome.failure("MISSING_REMOTE");
            }
            return AuditOutcome.success(updatedAt == null ? "PRESENT" : "PRESENT (" + updatedAt + ")");
        }
        if (remoteExists) {
            return AuditOutcome.success("REMOTE_ONLY");
        }
        return AuditOutcome.success("SKIPPED");
    }

    private AuditOutcome auditValue(EnvValue localValue, EnvValue remoteValue, boolean compareValue) {
        return auditValue(localValue, remoteValue, null, compareValue);
    }

    private AuditOutcome auditValue(EnvValue localValue, EnvValue remoteValue, String updatedAt, boolean compareValue) {
        if (localValue.isReal()) {
            if (!remoteValue.isReal()) {
                return AuditOutcome.failure("MISSING_REMOTE");
            }
            if (compareValue && !localValue.raw.equals(remoteValue.raw)) {
                return AuditOutcome.failure("MISMATCH");
            }
            return AuditOutcome.success(updatedAt == null ? (compareValue ? "MATCH" : "PRESENT")
                : (compareValue ? "MATCH (" + updatedAt + ")" : "PRESENT (" + updatedAt + ")"));
        }
        if (remoteValue.isReal()) {
            return AuditOutcome.success("REMOTE_ONLY");
        }
        return AuditOutcome.success("SKIPPED");
    }

    private void recordAuditStatus(boolean textOutput, ReleaseEnvJsonSupport.JsonSection section, String label, AuditOutcome outcome) {
        if (textOutput) {
            out.printf("%-40s %s%n", label, outcome.message);
        }
        if (section != null) {
            section.add(label, outcome.message);
        }
    }

    static final class AuditPreconditionFailure extends RuntimeException {
        private final boolean ok;

        AuditPreconditionFailure(boolean ok) {
            this.ok = ok;
        }

        boolean ok() {
            return ok;
        }
    }
}
