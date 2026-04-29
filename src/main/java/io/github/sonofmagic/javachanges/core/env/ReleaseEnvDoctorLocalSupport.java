package io.github.sonofmagic.javachanges.core.env;

import io.github.sonofmagic.javachanges.core.MavenCommand;
import io.github.sonofmagic.javachanges.core.OutputFormat;
import io.github.sonofmagic.javachanges.core.ReleaseMessages;
import io.github.sonofmagic.javachanges.core.ReleaseProcessUtils;
import io.github.sonofmagic.javachanges.core.ReleaseTextUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class ReleaseEnvDoctorLocalSupport {
    private final Path repoRoot;
    private final PrintStream out;
    private final ReleaseEnvRuntime runtime;
    private final ReleaseEnvDoctorSupport doctorSupport;

    ReleaseEnvDoctorLocalSupport(Path repoRoot, PrintStream out, ReleaseEnvRuntime runtime) {
        this.repoRoot = repoRoot;
        this.out = out;
        this.runtime = runtime;
        this.doctorSupport = new ReleaseEnvDoctorSupport(out);
    }

    boolean doctorLocal(LocalDoctorRequest request) throws IOException, InterruptedException {
        boolean textOutput = request.format != OutputFormat.JSON;
        DoctorLocalState state = new DoctorLocalState();
        List<ReleaseEnvJsonSupport.JsonSection> sections = new ArrayList<ReleaseEnvJsonSupport.JsonSection>();
        ReleaseEnvJsonSupport.JsonSection runtimeSection =
            new ReleaseEnvJsonSupport.JsonSection(ReleaseMessages.text("Local Runtime", "本机运行时"));
        ReleaseEnvJsonSupport.JsonSection envSection =
            new ReleaseEnvJsonSupport.JsonSection(ReleaseMessages.text("Local Env File", "本地 env 文件"));
        ReleaseEnvJsonSupport.JsonSection cliSection =
            new ReleaseEnvJsonSupport.JsonSection(ReleaseMessages.text("Platform CLI", "平台 CLI"));
        ReleaseEnvJsonSupport.JsonSection repoSection =
            new ReleaseEnvJsonSupport.JsonSection(ReleaseMessages.text("Repository Identifiers", "仓库标识"));
        sections.add(runtimeSection);
        sections.add(envSection);
        sections.add(cliSection);
        sections.add(repoSection);

        checkRuntime(textOutput, runtimeSection, state);
        Path envPath = checkEnvFile(request, textOutput, envSection, state);
        checkPlatformCli(textOutput, cliSection, state);
        checkRepoIdentifiers(request, textOutput, repoSection, state);

        if (textOutput) {
            out.println();
        }
        List<String> suggestions = new ArrayList<String>();
        if (!state.failed) {
            suggestions.add("make doctor-all GITHUB_REPO=owner/repo GITLAB_REPO=group/project");
            suggestions.add("make sync-all");
            if (textOutput) {
                out.println(ReleaseMessages.text("Local release environment check passed", "本机发布环境检查通过"));
                out.println(ReleaseMessages.text("Suggested next steps:", "下一步建议:"));
                out.println("  make doctor-all GITHUB_REPO=owner/repo GITLAB_REPO=group/project");
                out.println("  make sync-all");
            }
            if (request.format == OutputFormat.JSON) {
                out.println(doctorSupport.commandReportJson("doctor-local", true, runtime.relativizePath(envPath),
                    null, sections, suggestions, null));
            }
            return true;
        }

        if (textOutput) {
            out.println(ReleaseMessages.text(
                "Local release environment is not ready. Handle these in order:",
                "本机发布环境未就绪，建议按顺序处理:"
            ));
        }
        if (!state.envPresent) {
            String suggestion = ReleaseMessages.text(
                "Run make env-init to generate env/release.env.local",
                "执行 make env-init 生成 env/release.env.local"
            );
            suggestions.add(suggestion);
            if (textOutput) {
                out.println("  1. " + suggestion);
            }
        }
        if (state.envNeedsEdit) {
            String suggestion = ReleaseMessages.text(
                "Edit env/release.env.local and replace repository addresses, usernames, passwords, and required tokens",
                "编辑 env/release.env.local，替换仓库地址、用户名、密码和需要的 token"
            );
            suggestions.add(suggestion);
            if (textOutput) {
                out.println("  2. " + suggestion);
            }
        }
        if (state.javaMissing || state.mavenMissing || state.mavenFailed) {
            String suggestion = ReleaseMessages.text(
                "Install and configure a usable Java Runtime and Maven command (prefer ./mvnw, then system mvn), then rerun make readiness",
                "安装并配置可用的 Java Runtime 与 Maven 命令（优先 ./mvnw，其次系统 mvn），然后重新执行 make readiness"
            );
            suggestions.add(suggestion);
            if (textOutput) {
                out.println("  3. " + suggestion);
            }
        }
        if (state.ghMissing || state.ghAuthFailed || state.glabMissing || state.glabAuthFailed) {
            String suggestion = ReleaseMessages.text(
                "Run make auth-help to install and log in with gh / glab",
                "执行 make auth-help，完成 gh / glab 的安装和登录"
            );
            suggestions.add(suggestion);
            if (textOutput) {
                out.println("  4. " + suggestion);
            }
        }
        String finalSuggestion = ReleaseMessages.text(
            "After that, run make doctor-github, make doctor-gitlab, and sync-apply",
            "通过后再执行 make doctor-github、make doctor-gitlab 和 sync-apply"
        );
        suggestions.add(finalSuggestion);
        if (textOutput) {
            out.println("  5. " + finalSuggestion);
        }
        String failure = ReleaseMessages.text(
            "Local release environment is not ready",
            "本机发布环境未就绪"
        );
        if (request.format == OutputFormat.JSON) {
            out.println(doctorSupport.commandReportJson("doctor-local", false, runtime.relativizePath(envPath),
                null, sections, suggestions, failure));
            return false;
        }
        throw new IllegalStateException(failure);
    }

    private void checkRuntime(boolean textOutput, ReleaseEnvJsonSupport.JsonSection runtimeSection,
                              DoctorLocalState state) throws IOException, InterruptedException {
        if (textOutput) {
            out.println("== " + ReleaseMessages.text("Local Runtime", "本机运行时") + " ==");
        }
        if (runtime.commandAvailable("java", "-version")) {
            doctorSupport.recordStatus(textOutput, runtimeSection, "java -version", "OK");
        } else {
            doctorSupport.recordStatus(textOutput, runtimeSection, "java", "MISSING");
            state.javaMissing = true;
            state.failed = true;
        }

        Path mvnw = repoRoot.resolve(ReleaseProcessUtils.mavenWrapperPath());
        boolean wrapperPresent = Files.exists(mvnw);
        if (wrapperPresent) {
            doctorSupport.recordStatus(textOutput, runtimeSection, ReleaseProcessUtils.mavenWrapperPath(), "OK");
        } else {
            doctorSupport.recordStatus(textOutput, runtimeSection, ReleaseProcessUtils.mavenWrapperPath(), "MISSING");
        }

        boolean systemMavenPresent = runtime.commandExists("mvn");
        if (!wrapperPresent) {
            doctorSupport.recordStatus(textOutput, runtimeSection, "mvn", systemMavenPresent ? "OK" : "MISSING");
        }

        MavenCommand mavenCommand = ReleaseProcessUtils.resolveMavenCommand(repoRoot);
        if (mavenCommand == null) {
            doctorSupport.recordStatus(textOutput, runtimeSection, "Maven command", "MISSING");
            state.mavenMissing = true;
            state.failed = true;
        } else {
            doctorSupport.recordStatus(textOutput, runtimeSection, "Maven command",
                mavenCommand.command + " (" + mavenCommand.source + ")");
        }

        if (!state.javaMissing && mavenCommand != null) {
            if (runtime.commandAvailable(mavenCommand.command, "-q", "-version")) {
                doctorSupport.recordStatus(textOutput, runtimeSection, mavenCommand.versionLabel(), "OK");
            } else {
                doctorSupport.recordStatus(textOutput, runtimeSection, mavenCommand.versionLabel(), "FAILED");
                state.mavenFailed = true;
                state.failed = true;
            }
        } else {
            String versionLabel = mavenCommand == null ? "maven -q -version" : mavenCommand.versionLabel();
            doctorSupport.recordStatus(textOutput, runtimeSection, versionLabel, "SKIPPED");
        }
    }

    private Path checkEnvFile(LocalDoctorRequest request, boolean textOutput,
                              ReleaseEnvJsonSupport.JsonSection envSection, DoctorLocalState state) throws IOException {
        if (textOutput) {
            out.println();
            out.println("== " + ReleaseMessages.text("Local Env File", "本地 env 文件") + " ==");
        }
        Path envPath = runtime.resolvePath(request.envFile);
        if (Files.exists(envPath)) {
            state.envPresent = true;
            doctorSupport.recordStatus(textOutput, envSection, runtime.relativizePath(envPath), "OK");
        } else {
            doctorSupport.recordStatus(textOutput, envSection, runtime.relativizePath(envPath), "MISSING");
            state.failed = true;
        }

        if (state.envPresent && !runtime.isExampleFile(envPath)) {
            LoadedEnv env = LoadedEnv.load(envPath);
            ReleaseEnvDoctorSupport.EnvStatusSummary summary =
                doctorSupport.recordCommonEnvStatuses(env, textOutput, envSection);
            state.failed = state.failed || summary.failed;
            state.envNeedsEdit = state.envNeedsEdit || summary.needsEdit;
        } else if (state.envPresent) {
            doctorSupport.recordStatus(textOutput, envSection, "env file type", "INVALID");
            state.failed = true;
        } else {
            doctorSupport.recordStatus(textOutput, envSection, "env values", "SKIPPED");
        }
        return envPath;
    }

    private void checkPlatformCli(boolean textOutput, ReleaseEnvJsonSupport.JsonSection cliSection,
                                  DoctorLocalState state) throws IOException, InterruptedException {
        if (textOutput) {
            out.println();
            out.println("== " + ReleaseMessages.text("Platform CLI", "平台 CLI") + " ==");
        }
        if (runtime.commandExists("gh")) {
            doctorSupport.recordStatus(textOutput, cliSection, "gh", "OK");
            if (runtime.runQuietly(Arrays.asList("gh", "auth", "status"))) {
                doctorSupport.recordStatus(textOutput, cliSection, "gh auth status", "OK");
            } else {
                doctorSupport.recordStatus(textOutput, cliSection, "gh auth status", "FAILED");
                state.ghAuthFailed = true;
                state.failed = true;
            }
        } else {
            doctorSupport.recordStatus(textOutput, cliSection, "gh", "MISSING");
            state.ghMissing = true;
            state.failed = true;
        }

        if (runtime.commandExists("glab")) {
            doctorSupport.recordStatus(textOutput, cliSection, "glab", "OK");
            if (runtime.runQuietly(Arrays.asList("glab", "auth", "status"))) {
                doctorSupport.recordStatus(textOutput, cliSection, "glab auth status", "OK");
            } else {
                doctorSupport.recordStatus(textOutput, cliSection, "glab auth status", "FAILED");
                state.glabAuthFailed = true;
                state.failed = true;
            }
        } else {
            doctorSupport.recordStatus(textOutput, cliSection, "glab", "MISSING");
            state.glabMissing = true;
            state.failed = true;
        }
    }

    private void checkRepoIdentifiers(LocalDoctorRequest request, boolean textOutput,
                                      ReleaseEnvJsonSupport.JsonSection repoSection, DoctorLocalState state) {
        if (textOutput) {
            out.println();
            out.println("== " + ReleaseMessages.text("Repository Identifiers", "仓库标识") + " ==");
        }
        doctorSupport.recordStatus(textOutput, repoSection, "GITHUB_REPO", doctorSupport.repoStatusValue(request.githubRepo));
        doctorSupport.recordStatus(textOutput, repoSection, "GITLAB_REPO", doctorSupport.repoStatusValue(request.gitlabRepo));
        if (!ReleaseTextUtils.isBlank(request.githubRepo) && !doctorSupport.isValidRepoIdentifier(request.githubRepo)) {
            state.failed = true;
        }
        if (!ReleaseTextUtils.isBlank(request.gitlabRepo) && !doctorSupport.isValidRepoIdentifier(request.gitlabRepo)) {
            state.failed = true;
        }
    }

    private static final class DoctorLocalState {
        private boolean failed;
        private boolean envPresent;
        private boolean envNeedsEdit;
        private boolean javaMissing;
        private boolean mavenMissing;
        private boolean mavenFailed;
        private boolean ghMissing;
        private boolean ghAuthFailed;
        private boolean glabMissing;
        private boolean glabAuthFailed;
    }
}
