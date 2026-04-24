package io.github.sonofmagic.javachanges.core.env;

import io.github.sonofmagic.javachanges.core.MavenCommand;
import io.github.sonofmagic.javachanges.core.OutputFormat;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.isBlank;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.mavenWrapperPath;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.resolveMavenCommand;

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
        ReleaseEnvJsonSupport.JsonSection runtimeSection = new ReleaseEnvJsonSupport.JsonSection("本机运行时");
        ReleaseEnvJsonSupport.JsonSection envSection = new ReleaseEnvJsonSupport.JsonSection("本地 env 文件");
        ReleaseEnvJsonSupport.JsonSection cliSection = new ReleaseEnvJsonSupport.JsonSection("平台 CLI");
        ReleaseEnvJsonSupport.JsonSection repoSection = new ReleaseEnvJsonSupport.JsonSection("仓库标识");
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
                out.println("本机发布环境检查通过");
                out.println("下一步建议:");
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
            out.println("本机发布环境未就绪，建议按顺序处理:");
        }
        if (!state.envPresent) {
            suggestions.add("执行 make env-init 生成 env/release.env.local");
            if (textOutput) {
                out.println("  1. 执行 make env-init 生成 env/release.env.local");
            }
        }
        if (state.envNeedsEdit) {
            suggestions.add("编辑 env/release.env.local，替换仓库地址、用户名、密码和需要的 token");
            if (textOutput) {
                out.println("  2. 编辑 env/release.env.local，替换仓库地址、用户名、密码和需要的 token");
            }
        }
        if (state.javaMissing || state.mavenMissing || state.mavenFailed) {
            suggestions.add("安装并配置可用的 Java Runtime 与 Maven 命令（优先 ./mvnw，其次系统 mvn），然后重新执行 make readiness");
            if (textOutput) {
                out.println("  3. 安装并配置可用的 Java Runtime 与 Maven 命令（优先 ./mvnw，其次系统 mvn），然后重新执行 make readiness");
            }
        }
        if (state.ghMissing || state.ghAuthFailed || state.glabMissing || state.glabAuthFailed) {
            suggestions.add("执行 make auth-help，完成 gh / glab 的安装和登录");
            if (textOutput) {
                out.println("  4. 执行 make auth-help，完成 gh / glab 的安装和登录");
            }
        }
        suggestions.add("通过后再执行 make doctor-github、make doctor-gitlab 和 sync-apply");
        if (textOutput) {
            out.println("  5. 通过后再执行 make doctor-github、make doctor-gitlab 和 sync-apply");
        }
        if (request.format == OutputFormat.JSON) {
            out.println(doctorSupport.commandReportJson("doctor-local", false, runtime.relativizePath(envPath),
                null, sections, suggestions, "本机发布环境未就绪"));
            return false;
        }
        throw new IllegalStateException("本机发布环境未就绪");
    }

    private void checkRuntime(boolean textOutput, ReleaseEnvJsonSupport.JsonSection runtimeSection,
                              DoctorLocalState state) throws IOException, InterruptedException {
        if (textOutput) {
            out.println("== 本机运行时 ==");
        }
        if (runtime.commandAvailable("java", "-version")) {
            doctorSupport.recordStatus(textOutput, runtimeSection, "java -version", "OK");
        } else {
            doctorSupport.recordStatus(textOutput, runtimeSection, "java", "MISSING");
            state.javaMissing = true;
            state.failed = true;
        }

        Path mvnw = repoRoot.resolve(mavenWrapperPath());
        boolean wrapperPresent = Files.exists(mvnw);
        if (wrapperPresent) {
            doctorSupport.recordStatus(textOutput, runtimeSection, mavenWrapperPath(), "OK");
        } else {
            doctorSupport.recordStatus(textOutput, runtimeSection, mavenWrapperPath(), "MISSING");
        }

        boolean systemMavenPresent = runtime.commandExists("mvn");
        if (!wrapperPresent) {
            doctorSupport.recordStatus(textOutput, runtimeSection, "mvn", systemMavenPresent ? "OK" : "MISSING");
        }

        MavenCommand mavenCommand = resolveMavenCommand(repoRoot);
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
            out.println("== 本地 env 文件 ==");
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
            for (EnvEntry entry : ReleaseEnvCatalog.COMMON_VARIABLES) {
                EnvValue value = env.value(entry.name);
                String status = doctorSupport.requiredStatus(value, entry.required);
                doctorSupport.recordStatus(textOutput, envSection, entry.name, status);
                if ("MISSING".equals(status) || "PLACEHOLDER".equals(status)) {
                    if (entry.required) {
                        state.failed = true;
                    }
                    if (!"OPTIONAL".equals(status)) {
                        state.envNeedsEdit = true;
                    }
                }
            }
            String gitlabTokenStatus = doctorSupport.requiredStatus(env.value("GITLAB_RELEASE_TOKEN"), false);
            doctorSupport.recordStatus(textOutput, envSection, "GITLAB_RELEASE_TOKEN", gitlabTokenStatus);
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
            out.println("== 平台 CLI ==");
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
            out.println("== 仓库标识 ==");
        }
        doctorSupport.recordStatus(textOutput, repoSection, "GITHUB_REPO", repoStatusValue(request.githubRepo));
        doctorSupport.recordStatus(textOutput, repoSection, "GITLAB_REPO", repoStatusValue(request.gitlabRepo));
        if (!isBlank(request.githubRepo) && !request.githubRepo.contains("/")) {
            state.failed = true;
        }
        if (!isBlank(request.gitlabRepo) && !request.gitlabRepo.contains("/")) {
            state.failed = true;
        }
    }

    private String repoStatusValue(String value) {
        if (isBlank(value)) {
            return "NOT_SET";
        }
        if (value.contains("/")) {
            return value;
        }
        return "INVALID";
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
