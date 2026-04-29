package io.github.sonofmagic.javachanges.core.env;

import io.github.sonofmagic.javachanges.core.OutputFormat;
import io.github.sonofmagic.javachanges.core.ReleaseJsonUtils;
import io.github.sonofmagic.javachanges.core.ReleaseMessages;
import io.github.sonofmagic.javachanges.core.ReleaseTextUtils;

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
    private final ReleaseEnvDoctorSupport doctorSupport;

    ReleaseEnvAuditSupport(Path repoRoot, PrintStream out, ReleaseEnvRuntime runtime) {
        this.repoRoot = repoRoot;
        this.out = out;
        this.runtime = runtime;
        this.doctorSupport = new ReleaseEnvDoctorSupport(out);
    }

    boolean auditVars(AuditVarsRequest request, LoadedEnv env, String envPath) throws IOException, InterruptedException {
        boolean textOutput = request.format != OutputFormat.JSON;
        boolean failed = false;
        List<ReleaseEnvJsonSupport.JsonSection> sections = new java.util.ArrayList<ReleaseEnvJsonSupport.JsonSection>();
        if (textOutput) {
            out.println(ReleaseMessages.usingEnvFile(envPath));
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
            String failure = ReleaseMessages.platformVariableAuditFailed();
            if (request.format == OutputFormat.JSON) {
                out.println(doctorSupport.commandReportJson("audit-vars", false, envPath, request.platform.id,
                    sections, Collections.<String>emptyList(), failure));
                return false;
            }
            throw new IllegalStateException(failure);
        }
        if (textOutput) {
            out.println(ReleaseMessages.platformVariableAuditPassed());
        }
        if (request.format == OutputFormat.JSON) {
            out.println(doctorSupport.commandReportJson("audit-vars", true, envPath, request.platform.id,
                sections, Collections.<String>emptyList(), null));
        }
        return true;
    }

    private boolean auditGithub(AuditVarsRequest request, LoadedEnv env, boolean textOutput,
                                List<ReleaseEnvJsonSupport.JsonSection> sections)
        throws IOException, InterruptedException {
        ReleaseEnvJsonSupport.JsonSection githubPreconditions = new ReleaseEnvJsonSupport.JsonSection("GitHub Audit Preconditions");
        ReleaseEnvJsonSupport.JsonSection githubVariablesSection =
            new ReleaseEnvJsonSupport.JsonSection(ReleaseMessages.githubVariablesAudit());
        ReleaseEnvJsonSupport.JsonSection githubSecretsSection =
            new ReleaseEnvJsonSupport.JsonSection(ReleaseMessages.githubSecretsAudit());
        sections.add(githubPreconditions);
        sections.add(githubVariablesSection);
        sections.add(githubSecretsSection);
        if (ReleaseTextUtils.isBlank(request.githubRepo)) {
            githubPreconditions.add("GITHUB_REPO", "MISSING");
            failPrecondition(request, env, sections, ReleaseMessages.missingRepositoryArgument("GITHUB_REPO"));
        }
        githubPreconditions.add("GITHUB_REPO", request.githubRepo);
        if (!runtime.commandExists("gh")) {
            githubPreconditions.add("gh", "MISSING");
            failPrecondition(request, env, sections, ReleaseMessages.cliNotFound("gh"));
        }
        githubPreconditions.add("gh", "OK");
        if (!runtime.runQuietly(Arrays.asList("gh", "auth", "status"))) {
            githubPreconditions.add("gh auth status", "FAILED");
            failPrecondition(request, env, sections, ReleaseMessages.authStatusFailed("gh"));
        }
        githubPreconditions.add("gh auth status", "OK");
        String variablesJson = runtime.runAndCapture(Arrays.asList("gh", "variable", "list", "--repo", request.githubRepo,
            "--json", "name,value,updatedAt")).stdoutText();
        String secretsJson = runtime.runAndCapture(Arrays.asList("gh", "secret", "list", "--repo", request.githubRepo,
            "--json", "name,updatedAt")).stdoutText();
        Map<String, Map<String, String>> githubVariables = ReleaseJsonUtils.parseFlatJsonObjects(variablesJson);
        Map<String, Map<String, String>> githubSecrets = ReleaseJsonUtils.parseFlatJsonObjects(secretsJson);

        boolean failed = false;
        if (textOutput) {
            out.println();
            out.println(ReleaseMessages.heading(ReleaseMessages.githubVariablesAudit()));
        }
        for (EnvEntry entry : ReleaseEnvCatalog.GITHUB_ACTIONS_VARIABLES) {
            AuditOutcome outcome = auditValue(env.value(entry.name), githubVariables.get(entry.name), true);
            doctorSupport.recordStatus(textOutput, githubVariablesSection, entry.name, outcome.message);
            if (outcome.isFailure()) {
                failed = true;
            }
        }

        if (textOutput) {
            out.println();
            out.println(ReleaseMessages.heading(ReleaseMessages.githubSecretsAudit()));
        }
        for (EnvEntry entry : ReleaseEnvCatalog.GITHUB_ACTIONS_SECRETS) {
            AuditOutcome outcome = auditPresence(env.value(entry.name), githubSecrets.get(entry.name));
            doctorSupport.recordStatus(textOutput, githubSecretsSection, entry.name, outcome.message);
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
        ReleaseEnvJsonSupport.JsonSection gitlabSection =
            new ReleaseEnvJsonSupport.JsonSection(ReleaseMessages.gitlabVariablesAudit());
        sections.add(gitlabPreconditions);
        sections.add(gitlabSection);
        if (ReleaseTextUtils.isBlank(request.gitlabRepo)) {
            gitlabPreconditions.add("GITLAB_REPO", "MISSING");
            failPrecondition(request, env, sections, ReleaseMessages.missingRepositoryArgument("GITLAB_REPO"));
        }
        gitlabPreconditions.add("GITLAB_REPO", request.gitlabRepo);
        if (!runtime.commandExists("glab")) {
            gitlabPreconditions.add("glab", "MISSING");
            failPrecondition(request, env, sections, ReleaseMessages.cliNotFound("glab"));
        }
        gitlabPreconditions.add("glab", "OK");
        if (!runtime.runQuietly(Arrays.asList("glab", "auth", "status"))) {
            gitlabPreconditions.add("glab auth status", "FAILED");
            failPrecondition(request, env, sections, ReleaseMessages.authStatusFailed("glab"));
        }
        gitlabPreconditions.add("glab auth status", "OK");
        String exported = runtime.runAndCapture(Arrays.asList("glab", "variable", "export", "--repo", request.gitlabRepo,
            "--output", "env")).stdoutText();
        LoadedEnv remoteEnv = LoadedEnv.parse(exported, repoRoot.resolve("target/gitlab-variables.env"));

        boolean failed = false;
        if (textOutput) {
            out.println();
            out.println(ReleaseMessages.heading(ReleaseMessages.gitlabVariablesAudit()));
        }
        for (EnvEntry entry : ReleaseEnvCatalog.GITLAB_VARIABLES) {
            AuditOutcome outcome = auditValue(env.value(entry.name), remoteEnv.value(entry.name), true);
            doctorSupport.recordStatus(textOutput, gitlabSection, entry.name, outcome.message);
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
            out.println(doctorSupport.commandReportJson("audit-vars", false, envPath, request.platform.id,
                sections, Collections.<String>emptyList(), error));
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
