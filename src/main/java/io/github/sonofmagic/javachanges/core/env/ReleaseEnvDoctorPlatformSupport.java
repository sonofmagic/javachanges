package io.github.sonofmagic.javachanges.core.env;

import io.github.sonofmagic.javachanges.core.OutputFormat;
import io.github.sonofmagic.javachanges.core.ReleaseEnvJsonSupport;
import io.github.sonofmagic.javachanges.core.ReleaseEnvRuntime;
import io.github.sonofmagic.javachanges.core.config.ChangesetConfigSupport;
import io.github.sonofmagic.javachanges.core.gitlab.GitlabProtectionSupport;
import io.github.sonofmagic.javachanges.core.plan.RepoFiles;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.isBlank;

final class ReleaseEnvDoctorPlatformSupport {
    private final Path repoRoot;
    private final PrintStream out;
    private final ReleaseEnvRuntime runtime;
    private final GitlabProtectionSupport gitlabProtectionSupport;
    private final ReleaseEnvDoctorSupport doctorSupport;

    ReleaseEnvDoctorPlatformSupport(Path repoRoot, PrintStream out, ReleaseEnvRuntime runtime,
                                    GitlabProtectionSupport gitlabProtectionSupport) {
        this.repoRoot = repoRoot;
        this.out = out;
        this.runtime = runtime;
        this.gitlabProtectionSupport = gitlabProtectionSupport;
        this.doctorSupport = new ReleaseEnvDoctorSupport(out);
    }

    boolean doctorPlatform(DoctorPlatformRequest request) throws IOException, InterruptedException {
        boolean textOutput = request.format != OutputFormat.JSON;
        LoadedEnv env = LoadedEnv.load(runtime.resolveEnvFile(request.envFile));
        DoctorPlatformState state = new DoctorPlatformState();
        List<String> suggestions = new ArrayList<String>();
        List<ReleaseEnvJsonSupport.JsonSection> sections = new ArrayList<ReleaseEnvJsonSupport.JsonSection>();
        ReleaseEnvJsonSupport.JsonSection envSection = new ReleaseEnvJsonSupport.JsonSection("本地 env 检查");
        sections.add(envSection);

        checkLocalEnv(env, textOutput, envSection, state);
        checkGithub(request, textOutput, sections, suggestions, state);
        if (state.shortCircuited) {
            return false;
        }
        checkGitlab(request, env, textOutput, sections, suggestions, state);
        if (state.shortCircuited) {
            return false;
        }

        if (textOutput) {
            out.println();
        }
        if (state.failed) {
            if (request.format == OutputFormat.JSON) {
                out.println(doctorSupport.commandReportJson("doctor-platform", false, runtime.relativizePath(env.path),
                    request.platform.id, sections, suggestions, "doctor 检查失败，请修正上面的 MISSING / PLACEHOLDER 项后重试"));
                return false;
            }
            throw new IllegalStateException("doctor 检查失败，请修正上面的 MISSING / PLACEHOLDER 项后重试");
        }
        if (textOutput) {
            out.println("doctor 检查完成");
        }
        if (request.format == OutputFormat.JSON) {
            out.println(doctorSupport.commandReportJson("doctor-platform", true, runtime.relativizePath(env.path),
                request.platform.id, sections, suggestions, null));
        }
        return true;
    }

    private void checkLocalEnv(LoadedEnv env, boolean textOutput, ReleaseEnvJsonSupport.JsonSection envSection,
                               DoctorPlatformState state) {
        if (textOutput) {
            out.println("使用 env 文件: " + runtime.relativizePath(env.path));
            out.println();
            out.println("== 本地 env 检查 ==");
        }
        for (EnvEntry entry : ReleaseEnvCatalog.COMMON_VARIABLES) {
            String status = doctorSupport.requiredStatus(env.value(entry.name), entry.required);
            doctorSupport.recordStatus(textOutput, envSection, entry.name, status);
            if (("MISSING".equals(status) || "PLACEHOLDER".equals(status)) && entry.required) {
                state.failed = true;
            }
        }
        String gitlabTokenStatus = doctorSupport.requiredStatus(env.value("GITLAB_RELEASE_TOKEN"), false);
        doctorSupport.recordStatus(textOutput, envSection, "GITLAB_RELEASE_TOKEN", gitlabTokenStatus);
    }

    private void checkGithub(DoctorPlatformRequest request, boolean textOutput,
                             List<ReleaseEnvJsonSupport.JsonSection> sections, List<String> suggestions,
                             DoctorPlatformState state) throws IOException, InterruptedException {
        if (!request.platform.includesGithub()) {
            return;
        }
        if (textOutput) {
            out.println();
            out.println("== GitHub CLI 检查 ==");
        }
        ReleaseEnvJsonSupport.JsonSection githubSection = new ReleaseEnvJsonSupport.JsonSection("GitHub CLI 检查");
        sections.add(githubSection);
        runtime.requireCommand("gh");
        doctorSupport.recordStatus(textOutput, githubSection, "gh", "OK");
        if (!runtime.runQuietly(Arrays.asList("gh", "auth", "status"))) {
            doctorSupport.recordStatus(textOutput, githubSection, "gh auth status", "FAILED");
            if (request.format == OutputFormat.JSON) {
                out.println(doctorSupport.commandReportJson("doctor-platform", false, requestEnvPath(request),
                    request.platform.id, sections, suggestions, "gh auth status 失败，请先执行 make auth-help"));
                state.shortCircuited = true;
                return;
            }
            throw new IllegalStateException("gh auth status 失败，请先执行 make auth-help");
        }
        doctorSupport.recordStatus(textOutput, githubSection, "gh auth status", "OK");
        if (isBlank(request.githubRepo)) {
            doctorSupport.recordStatus(textOutput, githubSection, "GITHUB_REPO", "MISSING");
            state.failed = true;
        } else if (!request.githubRepo.contains("/")) {
            doctorSupport.recordStatus(textOutput, githubSection, "GITHUB_REPO", "INVALID");
            state.failed = true;
        } else {
            doctorSupport.recordStatus(textOutput, githubSection, "GITHUB_REPO", request.githubRepo);
        }
    }

    private void checkGitlab(DoctorPlatformRequest request, LoadedEnv env, boolean textOutput,
                             List<ReleaseEnvJsonSupport.JsonSection> sections, List<String> suggestions,
                             DoctorPlatformState state) throws IOException, InterruptedException {
        if (state.shortCircuited || !request.platform.includesGitlab()) {
            return;
        }
        if (textOutput) {
            out.println();
            out.println("== GitLab CLI 检查 ==");
        }
        ReleaseEnvJsonSupport.JsonSection gitlabSection = new ReleaseEnvJsonSupport.JsonSection("GitLab CLI 检查");
        sections.add(gitlabSection);
        runtime.requireCommand("glab");
        doctorSupport.recordStatus(textOutput, gitlabSection, "glab", "OK");
        if (!runtime.runQuietly(Arrays.asList("glab", "auth", "status"))) {
            doctorSupport.recordStatus(textOutput, gitlabSection, "glab auth status", "FAILED");
            if (request.format == OutputFormat.JSON) {
                out.println(doctorSupport.commandReportJson("doctor-platform", false, runtime.relativizePath(env.path),
                    request.platform.id, sections, suggestions, "glab auth status 失败，请先执行 make auth-help"));
                state.shortCircuited = true;
                return;
            }
            throw new IllegalStateException("glab auth status 失败，请先执行 make auth-help");
        }
        doctorSupport.recordStatus(textOutput, gitlabSection, "glab auth status", "OK");
        if (isBlank(request.gitlabRepo)) {
            doctorSupport.recordStatus(textOutput, gitlabSection, "GITLAB_REPO", "MISSING");
            state.failed = true;
            return;
        }
        if (!request.gitlabRepo.contains("/")) {
            doctorSupport.recordStatus(textOutput, gitlabSection, "GITLAB_REPO", "INVALID");
            state.failed = true;
            return;
        }
        doctorSupport.recordStatus(textOutput, gitlabSection, "GITLAB_REPO", request.gitlabRepo);
        ChangesetConfigSupport.ChangesetConfig changesetConfig = RepoFiles.readChangesetConfig(repoRoot);
        ReleaseEnvJsonSupport.JsonSection protectedVariablesSection =
            new ReleaseEnvJsonSupport.JsonSection("GitLab Protected Variables");
        ReleaseEnvJsonSupport.JsonSection protectedBranchesSection =
            new ReleaseEnvJsonSupport.JsonSection("GitLab Protected Branches");
        sections.add(protectedVariablesSection);
        sections.add(protectedBranchesSection);
        if (textOutput) {
            out.println();
            out.println("== GitLab Protected Variables ==");
        }
        GitlabProtectionSupport.GitlabProtectionCheck protectionCheck =
            gitlabProtectionSupport.inspectProtection(request.gitlabRepo, env, changesetConfig,
                protectedVariablesSection, protectedBranchesSection, textOutput);
        state.failed = state.failed || protectionCheck.failed;
        suggestions.addAll(protectionCheck.suggestions);
    }

    private String requestEnvPath(DoctorPlatformRequest request) {
        return runtime.relativizePath(runtime.resolvePath(request.envFile));
    }

    private static final class DoctorPlatformState {
        private boolean failed;
        private boolean shortCircuited;
    }
}
