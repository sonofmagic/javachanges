package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.*;

final class ReleaseEnvSupport {
    private final Path repoRoot;
    private final PrintStream out;
    private final ReleaseEnvRuntime runtime;
    private final GitlabProtectionSupport gitlabProtectionSupport;
    private final ReleaseEnvRenderSupport renderSupport;
    private final ReleaseEnvAuditSupport auditSupport;
    private final ReleaseEnvSyncSupport syncSupport;

    ReleaseEnvSupport(Path repoRoot, PrintStream out) {
        this(repoRoot, out, new ReleaseEnvRuntime(repoRoot));
    }

    ReleaseEnvSupport(Path repoRoot, PrintStream out, ReleaseEnvRuntime runtime) {
        this.repoRoot = repoRoot;
        this.out = out;
        this.runtime = runtime;
        this.gitlabProtectionSupport = new GitlabProtectionSupport(runtime, out);
        this.renderSupport = new ReleaseEnvRenderSupport(out);
        this.auditSupport = new ReleaseEnvAuditSupport(repoRoot, out, runtime);
        this.syncSupport = new ReleaseEnvSyncSupport(repoRoot, out, runtime);
    }

    void initEnv(InitEnvRequest request) throws IOException {
        Path template = runtime.resolvePath(request.template);
        Path target = runtime.resolvePath(request.target);
        if (!Files.exists(template)) {
            throw new IllegalStateException("未找到模板文件: " + runtime.relativizePath(template));
        }
        if (target.getFileName().toString().endsWith(".example")) {
            throw new IllegalStateException("目标文件不能是示例文件: " + runtime.relativizePath(target));
        }
        if (Files.exists(target) && !request.force) {
            out.println("目标文件已存在，未做覆盖: " + runtime.relativizePath(target));
            out.println("如果你确实要重建，请执行: make env-init RELEASE_ENV_FILE=" + runtime.relativizePath(target) + " FORCE=true");
            return;
        }
        Path parent = target.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        Files.write(target, Files.readAllLines(template, StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        try {
            target.toFile().setReadable(false, false);
            target.toFile().setReadable(true, true);
            target.toFile().setWritable(false, false);
            target.toFile().setWritable(true, true);
        } catch (SecurityException ignored) {
        }
        out.println("已生成本地 env 文件: " + runtime.relativizePath(target));
        out.println("下一步请编辑真实仓库地址和凭据，然后执行: make readiness");
    }

    void printAuthHelp(Platform platform) {
        if (platform.includesGithub()) {
            out.println("== GitHub CLI 登录建议 ==");
            out.println();
            out.println("1. 安装或确认 gh 可用");
            out.println("   gh --version");
            out.println();
            out.println("2. 生成本地 env 模板");
            out.println("   make env-init");
            out.println();
            out.println("3. 使用浏览器登录");
            out.println("   gh auth login --web --git-protocol ssh");
            out.println();
            out.println("4. 检查本机 readiness");
            out.println("   make readiness GITHUB_REPO=owner/repo");
            out.println();
            out.println("5. 检查登录状态");
            out.println("   gh auth status");
            out.println();
            out.println("6. 验证本地 doctor");
            out.println("   make doctor-github GITHUB_REPO=owner/repo");
            out.println();
            out.println("7. 预览将写入的平台命令");
            out.println("   make sync-github");
            out.println();
            out.println("8. 真正写入平台变量");
            out.println("   make sync-github-apply GITHUB_REPO=owner/repo");
            out.println();
            out.println("9. 回读审计平台状态");
            out.println("   make audit-github GITHUB_REPO=owner/repo");
            out.println();
            out.println("官方文档:");
            out.println("  https://cli.github.com/manual/gh_auth_login");
        }

        if (platform.includesGithub() && platform.includesGitlab()) {
            out.println();
        }

        if (platform.includesGitlab()) {
            out.println("== GitLab CLI 登录建议 ==");
            out.println();
            out.println("1. 安装或确认 glab 可用");
            out.println("   glab --version");
            out.println();
            out.println("2. 生成本地 env 模板");
            out.println("   make env-init");
            out.println();
            out.println("3. 使用浏览器登录 GitLab");
            out.println("   glab auth login --hostname gitlab.example.com --web --git-protocol ssh --use-keyring");
            out.println();
            out.println("4. 或使用 token 登录");
            out.println("   glab auth login --hostname gitlab.example.com --stdin < token.txt");
            out.println();
            out.println("5. 检查本机 readiness");
            out.println("   make readiness GITLAB_REPO=group/project");
            out.println();
            out.println("6. 检查登录状态");
            out.println("   glab auth status");
            out.println();
            out.println("7. 验证本地 doctor");
            out.println("   make doctor-gitlab GITLAB_REPO=group/project");
            out.println();
            out.println("8. 预览将写入的平台命令");
            out.println("   make sync-gitlab");
            out.println();
            out.println("9. 真正写入平台变量");
            out.println("   make sync-gitlab-apply GITLAB_REPO=group/project");
            out.println();
            out.println("10. 回读审计平台状态");
            out.println("    make audit-gitlab GITLAB_REPO=group/project");
            out.println();
            out.println("官方文档:");
            out.println("  https://docs.gitlab.com/cli/auth/login/");
        }
    }

    boolean renderVars(PlatformEnvRequest request) throws IOException {
        LoadedEnv env = LoadedEnv.load(runtime.resolveEnvFile(request.envFile));
        return renderSupport.renderVars(env, request, runtime.relativizePath(env.path));
    }

    boolean doctorLocal(LocalDoctorRequest request) throws IOException, InterruptedException {
        boolean textOutput = request.format != OutputFormat.JSON;
        boolean failed = false;
        boolean envPresent = false;
        boolean envNeedsEdit = false;
        boolean javaMissing = false;
        boolean mavenMissing = false;
        boolean mavenFailed = false;
        boolean ghMissing = false;
        boolean ghAuthFailed = false;
        boolean glabMissing = false;
        boolean glabAuthFailed = false;
        List<ReleaseEnvJsonSupport.JsonSection> sections = new ArrayList<ReleaseEnvJsonSupport.JsonSection>();
        ReleaseEnvJsonSupport.JsonSection runtimeSection = new ReleaseEnvJsonSupport.JsonSection("本机运行时");
        ReleaseEnvJsonSupport.JsonSection envSection = new ReleaseEnvJsonSupport.JsonSection("本地 env 文件");
        ReleaseEnvJsonSupport.JsonSection cliSection = new ReleaseEnvJsonSupport.JsonSection("平台 CLI");
        ReleaseEnvJsonSupport.JsonSection repoSection = new ReleaseEnvJsonSupport.JsonSection("仓库标识");
        sections.add(runtimeSection);
        sections.add(envSection);
        sections.add(cliSection);
        sections.add(repoSection);

        if (textOutput) {
            out.println("== 本机运行时 ==");
        }
        if (runtime.commandAvailable("java", "-version")) {
            recordStatus(textOutput, runtimeSection, "java -version", "OK");
        } else {
            recordStatus(textOutput, runtimeSection, "java", "MISSING");
            javaMissing = true;
            failed = true;
        }

        Path mvnw = repoRoot.resolve(mavenWrapperPath());
        boolean wrapperPresent = Files.exists(mvnw);
        if (wrapperPresent) {
            recordStatus(textOutput, runtimeSection, mavenWrapperPath(), "OK");
        } else {
            recordStatus(textOutput, runtimeSection, mavenWrapperPath(), "MISSING");
        }

        boolean systemMavenPresent = runtime.commandExists("mvn");
        if (!wrapperPresent) {
            recordStatus(textOutput, runtimeSection, "mvn", systemMavenPresent ? "OK" : "MISSING");
        }

        MavenCommand mavenCommand = resolveMavenCommand(repoRoot);
        if (mavenCommand == null) {
            recordStatus(textOutput, runtimeSection, "Maven command", "MISSING");
            mavenMissing = true;
            failed = true;
        } else {
            recordStatus(textOutput, runtimeSection, "Maven command",
                mavenCommand.command + " (" + mavenCommand.source + ")");
        }

        if (!javaMissing && mavenCommand != null) {
            if (runtime.commandAvailable(mavenCommand.command, "-q", "-version")) {
                recordStatus(textOutput, runtimeSection, mavenCommand.versionLabel(), "OK");
            } else {
                recordStatus(textOutput, runtimeSection, mavenCommand.versionLabel(), "FAILED");
                mavenFailed = true;
                failed = true;
            }
        } else {
            String versionLabel = mavenCommand == null ? "maven -q -version" : mavenCommand.versionLabel();
            recordStatus(textOutput, runtimeSection, versionLabel, "SKIPPED");
        }

        if (textOutput) {
            out.println();
            out.println("== 本地 env 文件 ==");
        }
        Path envPath = runtime.resolvePath(request.envFile);
        if (Files.exists(envPath)) {
            envPresent = true;
            recordStatus(textOutput, envSection, runtime.relativizePath(envPath), "OK");
        } else {
            recordStatus(textOutput, envSection, runtime.relativizePath(envPath), "MISSING");
            failed = true;
        }

        if (envPresent && !runtime.isExampleFile(envPath)) {
            LoadedEnv env = LoadedEnv.load(envPath);
            for (EnvEntry entry : ReleaseEnvCatalog.COMMON_VARIABLES) {
                EnvValue value = env.value(entry.name);
                String status = requiredStatus(value, entry.required);
                recordStatus(textOutput, envSection, entry.name, status);
                if ("MISSING".equals(status) || "PLACEHOLDER".equals(status)) {
                    if (entry.required) {
                        failed = true;
                    }
                    if (!"OPTIONAL".equals(status)) {
                        envNeedsEdit = true;
                    }
                }
            }
            String gitlabTokenStatus = requiredStatus(env.value("GITLAB_RELEASE_TOKEN"), false);
            recordStatus(textOutput, envSection, "GITLAB_RELEASE_TOKEN", gitlabTokenStatus);
        } else if (envPresent) {
            recordStatus(textOutput, envSection, "env file type", "INVALID");
            failed = true;
        } else {
            recordStatus(textOutput, envSection, "env values", "SKIPPED");
        }

        if (textOutput) {
            out.println();
            out.println("== 平台 CLI ==");
        }
        if (runtime.commandExists("gh")) {
            recordStatus(textOutput, cliSection, "gh", "OK");
            if (runtime.runQuietly(Arrays.asList("gh", "auth", "status"))) {
                recordStatus(textOutput, cliSection, "gh auth status", "OK");
            } else {
                recordStatus(textOutput, cliSection, "gh auth status", "FAILED");
                ghAuthFailed = true;
                failed = true;
            }
        } else {
            recordStatus(textOutput, cliSection, "gh", "MISSING");
            ghMissing = true;
            failed = true;
        }

        if (runtime.commandExists("glab")) {
            recordStatus(textOutput, cliSection, "glab", "OK");
            if (runtime.runQuietly(Arrays.asList("glab", "auth", "status"))) {
                recordStatus(textOutput, cliSection, "glab auth status", "OK");
            } else {
                recordStatus(textOutput, cliSection, "glab auth status", "FAILED");
                glabAuthFailed = true;
                failed = true;
            }
        } else {
            recordStatus(textOutput, cliSection, "glab", "MISSING");
            glabMissing = true;
            failed = true;
        }

        if (textOutput) {
            out.println();
            out.println("== 仓库标识 ==");
        }
        recordStatus(textOutput, repoSection, "GITHUB_REPO", repoStatusValue(request.githubRepo));
        recordStatus(textOutput, repoSection, "GITLAB_REPO", repoStatusValue(request.gitlabRepo));
        if (!isBlank(request.githubRepo) && !request.githubRepo.contains("/")) {
            failed = true;
        }
        if (!isBlank(request.gitlabRepo) && !request.gitlabRepo.contains("/")) {
            failed = true;
        }

        if (textOutput) {
            out.println();
        }
        List<String> suggestions = new ArrayList<String>();
        if (!failed) {
            suggestions.add("make doctor-all GITHUB_REPO=owner/repo GITLAB_REPO=group/project");
            suggestions.add("make sync-all");
            if (textOutput) {
                out.println("本机发布环境检查通过");
                out.println("下一步建议:");
                out.println("  make doctor-all GITHUB_REPO=owner/repo GITLAB_REPO=group/project");
                out.println("  make sync-all");
            }
            if (request.format == OutputFormat.JSON) {
                out.println(commandReportJson("doctor-local", true, runtime.relativizePath(envPath), null, false,
                    sections, suggestions, null));
            }
            return true;
        }

        if (textOutput) {
            out.println("本机发布环境未就绪，建议按顺序处理:");
        }
        if (!envPresent) {
            suggestions.add("执行 make env-init 生成 env/release.env.local");
            if (textOutput) {
                out.println("  1. 执行 make env-init 生成 env/release.env.local");
            }
        }
        if (envNeedsEdit) {
            suggestions.add("编辑 env/release.env.local，替换仓库地址、用户名、密码和需要的 token");
            if (textOutput) {
                out.println("  2. 编辑 env/release.env.local，替换仓库地址、用户名、密码和需要的 token");
            }
        }
        if (javaMissing || mavenMissing || mavenFailed) {
            suggestions.add("安装并配置可用的 Java Runtime 与 Maven 命令（优先 ./mvnw，其次系统 mvn），然后重新执行 make readiness");
            if (textOutput) {
                out.println("  3. 安装并配置可用的 Java Runtime 与 Maven 命令（优先 ./mvnw，其次系统 mvn），然后重新执行 make readiness");
            }
        }
        if (ghMissing || ghAuthFailed || glabMissing || glabAuthFailed) {
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
            out.println(commandReportJson("doctor-local", false, runtime.relativizePath(envPath), null, false,
                sections, suggestions, "本机发布环境未就绪"));
            return false;
        }
        throw new IllegalStateException("本机发布环境未就绪");
    }

    boolean doctorPlatform(DoctorPlatformRequest request) throws IOException, InterruptedException {
        boolean textOutput = request.format != OutputFormat.JSON;
        LoadedEnv env = LoadedEnv.load(runtime.resolveEnvFile(request.envFile));
        boolean failed = false;
        List<String> suggestions = new ArrayList<String>();
        List<ReleaseEnvJsonSupport.JsonSection> sections = new ArrayList<ReleaseEnvJsonSupport.JsonSection>();
        ReleaseEnvJsonSupport.JsonSection envSection = new ReleaseEnvJsonSupport.JsonSection("本地 env 检查");
        sections.add(envSection);
        ReleaseEnvJsonSupport.JsonSection githubSection = null;
        ReleaseEnvJsonSupport.JsonSection gitlabSection = null;

        if (textOutput) {
            out.println("使用 env 文件: " + runtime.relativizePath(env.path));
            out.println();
            out.println("== 本地 env 检查 ==");
        }
        for (EnvEntry entry : ReleaseEnvCatalog.COMMON_VARIABLES) {
            String status = requiredStatus(env.value(entry.name), entry.required);
            recordStatus(textOutput, envSection, entry.name, status);
            if (("MISSING".equals(status) || "PLACEHOLDER".equals(status)) && entry.required) {
                failed = true;
            }
        }
        String gitlabTokenStatus = requiredStatus(env.value("GITLAB_RELEASE_TOKEN"), false);
        recordStatus(textOutput, envSection, "GITLAB_RELEASE_TOKEN", gitlabTokenStatus);

        if (request.platform.includesGithub()) {
            if (textOutput) {
                out.println();
                out.println("== GitHub CLI 检查 ==");
            }
            githubSection = new ReleaseEnvJsonSupport.JsonSection("GitHub CLI 检查");
            sections.add(githubSection);
            runtime.requireCommand("gh");
            recordStatus(textOutput, githubSection, "gh", "OK");
            if (!runtime.runQuietly(Arrays.asList("gh", "auth", "status"))) {
                recordStatus(textOutput, githubSection, "gh auth status", "FAILED");
                if (request.format == OutputFormat.JSON) {
                    out.println(commandReportJson("doctor-platform", false, runtime.relativizePath(env.path),
                        request.platform.id, false, sections,
                        suggestions, "gh auth status 失败，请先执行 make auth-help"));
                    return false;
                }
                throw new IllegalStateException("gh auth status 失败，请先执行 make auth-help");
            }
            recordStatus(textOutput, githubSection, "gh auth status", "OK");
            if (isBlank(request.githubRepo)) {
                recordStatus(textOutput, githubSection, "GITHUB_REPO", "MISSING");
                failed = true;
            } else if (!request.githubRepo.contains("/")) {
                recordStatus(textOutput, githubSection, "GITHUB_REPO", "INVALID");
                failed = true;
            } else {
                recordStatus(textOutput, githubSection, "GITHUB_REPO", request.githubRepo);
            }
        }

        if (request.platform.includesGitlab()) {
            if (textOutput) {
                out.println();
                out.println("== GitLab CLI 检查 ==");
            }
            gitlabSection = new ReleaseEnvJsonSupport.JsonSection("GitLab CLI 检查");
            sections.add(gitlabSection);
            runtime.requireCommand("glab");
            recordStatus(textOutput, gitlabSection, "glab", "OK");
            if (!runtime.runQuietly(Arrays.asList("glab", "auth", "status"))) {
                recordStatus(textOutput, gitlabSection, "glab auth status", "FAILED");
                if (request.format == OutputFormat.JSON) {
                    out.println(commandReportJson("doctor-platform", false, runtime.relativizePath(env.path),
                        request.platform.id, false, sections,
                        suggestions, "glab auth status 失败，请先执行 make auth-help"));
                    return false;
                }
                throw new IllegalStateException("glab auth status 失败，请先执行 make auth-help");
            }
            recordStatus(textOutput, gitlabSection, "glab auth status", "OK");
            if (isBlank(request.gitlabRepo)) {
                recordStatus(textOutput, gitlabSection, "GITLAB_REPO", "MISSING");
                failed = true;
            } else if (!request.gitlabRepo.contains("/")) {
                recordStatus(textOutput, gitlabSection, "GITLAB_REPO", "INVALID");
                failed = true;
            } else {
                recordStatus(textOutput, gitlabSection, "GITLAB_REPO", request.gitlabRepo);
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
                failed = failed || protectionCheck.failed;
                suggestions.addAll(protectionCheck.suggestions);
            }
        }

        if (textOutput) {
            out.println();
        }
        if (failed) {
            if (request.format == OutputFormat.JSON) {
                out.println(commandReportJson("doctor-platform", false, runtime.relativizePath(env.path),
                    request.platform.id, false, sections,
                    suggestions, "doctor 检查失败，请修正上面的 MISSING / PLACEHOLDER 项后重试"));
                return false;
            }
            throw new IllegalStateException("doctor 检查失败，请修正上面的 MISSING / PLACEHOLDER 项后重试");
        }
        if (textOutput) {
            out.println("doctor 检查完成");
        }
        if (request.format == OutputFormat.JSON) {
            out.println(commandReportJson("doctor-platform", true, runtime.relativizePath(env.path),
                request.platform.id, false, sections, suggestions, null));
        }
        return true;
    }

    void syncVars(SyncVarsRequest request) throws IOException, InterruptedException {
        LoadedEnv env = LoadedEnv.load(runtime.resolveEnvFile(request.envFile));
        syncSupport.syncVars(request, env, runtime.relativizePath(env.path));
    }

    boolean auditVars(AuditVarsRequest request) throws IOException, InterruptedException {
        LoadedEnv env = LoadedEnv.load(runtime.resolveEnvFile(request.envFile));
        try {
            return auditSupport.auditVars(request, env, runtime.relativizePath(env.path));
        } catch (ReleaseEnvAuditSupport.AuditPreconditionFailure exception) {
            return false;
        }
    }

    private void printStatus(String label, String value) {
        out.printf("%-40s %s%n", label, value);
    }

    private void recordStatus(boolean textOutput, ReleaseEnvJsonSupport.JsonSection section, String label, String value) {
        if (textOutput) {
            printStatus(label, value);
        }
        if (section != null) {
            section.add(label, value);
        }
    }

    private void printRepoStatus(String label, String value) {
        if (isBlank(value)) {
            printStatus(label, "NOT_SET");
        } else if (value.contains("/")) {
            printStatus(label, value);
        } else {
            printStatus(label, "INVALID");
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

    static String errorJson(String command, Exception exception) {
        return ReleaseEnvJsonSupport.errorJson(command, exception);
    }

    private String commandReportJson(String command, boolean ok, String envFile, String platform,
                                     boolean showSecrets, List<ReleaseEnvJsonSupport.JsonSection> sections,
                                     List<String> suggestions, String error) {
        return ReleaseEnvJsonSupport.commandReportJson(command, ok, envFile, platform, showSecrets,
            sections, suggestions, error);
    }

    private String requiredStatus(EnvValue value, boolean required) {
        if (value.missing) {
            return required ? "MISSING" : "OPTIONAL";
        }
        if (value.placeholder) {
            return "PLACEHOLDER";
        }
        return "OK";
    }

}
