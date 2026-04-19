package io.github.sonofmagic.javachanges;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static io.github.sonofmagic.javachanges.ReleaseUtils.*;

final class ReleaseEnvSupport {
    private static final List<EnvEntry> COMMON_VARIABLES = Arrays.asList(
        new EnvEntry("MAVEN_RELEASE_REPOSITORY_URL", false, false),
        new EnvEntry("MAVEN_SNAPSHOT_REPOSITORY_URL", false, false),
        new EnvEntry("MAVEN_RELEASE_REPOSITORY_ID", false, false),
        new EnvEntry("MAVEN_SNAPSHOT_REPOSITORY_ID", false, false),
        new EnvEntry("MAVEN_REPOSITORY_USERNAME", true, false),
        new EnvEntry("MAVEN_REPOSITORY_PASSWORD", true, false),
        new EnvEntry("MAVEN_RELEASE_REPOSITORY_USERNAME", true, false),
        new EnvEntry("MAVEN_RELEASE_REPOSITORY_PASSWORD", true, false),
        new EnvEntry("MAVEN_SNAPSHOT_REPOSITORY_USERNAME", true, false),
        new EnvEntry("MAVEN_SNAPSHOT_REPOSITORY_PASSWORD", true, false)
    );
    private static final List<EnvEntry> GITHUB_ACTIONS_VARIABLES = Arrays.asList(
        new EnvEntry("MAVEN_RELEASE_REPOSITORY_URL", false, false),
        new EnvEntry("MAVEN_SNAPSHOT_REPOSITORY_URL", false, false),
        new EnvEntry("MAVEN_RELEASE_REPOSITORY_ID", false, false),
        new EnvEntry("MAVEN_SNAPSHOT_REPOSITORY_ID", false, false)
    );
    private static final List<EnvEntry> GITHUB_ACTIONS_SECRETS = Arrays.asList(
        new EnvEntry("MAVEN_REPOSITORY_USERNAME", true, false),
        new EnvEntry("MAVEN_REPOSITORY_PASSWORD", true, false),
        new EnvEntry("MAVEN_RELEASE_REPOSITORY_USERNAME", true, false),
        new EnvEntry("MAVEN_RELEASE_REPOSITORY_PASSWORD", true, false),
        new EnvEntry("MAVEN_SNAPSHOT_REPOSITORY_USERNAME", true, false),
        new EnvEntry("MAVEN_SNAPSHOT_REPOSITORY_PASSWORD", true, false)
    );
    private static final List<EnvEntry> GITLAB_VARIABLES = Arrays.asList(
        new EnvEntry("MAVEN_RELEASE_REPOSITORY_URL", false, false),
        new EnvEntry("MAVEN_SNAPSHOT_REPOSITORY_URL", false, false),
        new EnvEntry("MAVEN_RELEASE_REPOSITORY_ID", false, false),
        new EnvEntry("MAVEN_SNAPSHOT_REPOSITORY_ID", false, false),
        new EnvEntry("MAVEN_REPOSITORY_USERNAME", true, true),
        new EnvEntry("MAVEN_REPOSITORY_PASSWORD", true, true),
        new EnvEntry("MAVEN_RELEASE_REPOSITORY_USERNAME", true, true),
        new EnvEntry("MAVEN_RELEASE_REPOSITORY_PASSWORD", true, true),
        new EnvEntry("MAVEN_SNAPSHOT_REPOSITORY_USERNAME", true, true),
        new EnvEntry("MAVEN_SNAPSHOT_REPOSITORY_PASSWORD", true, true),
        new EnvEntry("GITLAB_RELEASE_TOKEN", true, true)
    );

    private final Path repoRoot;
    private final PrintStream out;

    ReleaseEnvSupport(Path repoRoot, PrintStream out) {
        this.repoRoot = repoRoot;
        this.out = out;
    }

    void initEnv(InitEnvRequest request) throws IOException {
        Path template = resolvePath(request.template);
        Path target = resolvePath(request.target);
        if (!Files.exists(template)) {
            throw new IllegalStateException("未找到模板文件: " + relativizePath(template));
        }
        if (target.getFileName().toString().endsWith(".example")) {
            throw new IllegalStateException("目标文件不能是示例文件: " + relativizePath(target));
        }
        if (Files.exists(target) && !request.force) {
            out.println("目标文件已存在，未做覆盖: " + relativizePath(target));
            out.println("如果你确实要重建，请执行: make env-init RELEASE_ENV_FILE=" + relativizePath(target) + " FORCE=true");
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
        out.println("已生成本地 env 文件: " + relativizePath(target));
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

    void renderVars(PlatformEnvRequest request) throws IOException {
        LoadedEnv env = LoadedEnv.load(resolveEnvFile(request.envFile));
        out.println("使用 env 文件: " + relativizePath(env.path));
        if (!request.showSecrets) {
            out.println("敏感值默认已打码。传入 --show-secrets true 可显示原值。");
        }

        if (request.platform.includesGithub()) {
            out.println();
            out.println("== GitHub Actions Variables ==");
            printEnvEntries(env, GITHUB_ACTIONS_VARIABLES, request.showSecrets);
            out.println();
            out.println("== GitHub Actions Secrets ==");
            printEnvEntries(env, GITHUB_ACTIONS_SECRETS, request.showSecrets);
        }

        if (request.platform.includesGitlab()) {
            out.println();
            out.println("== GitLab CI/CD Variables ==");
            out.println("GITLAB_RELEASE_TOKEN                     OPTIONAL (fallback: CI_JOB_TOKEN)");
            for (EnvEntry entry : GITLAB_VARIABLES) {
                if ("GITLAB_RELEASE_TOKEN".equals(entry.name)) {
                    continue;
                }
                EnvValue value = env.value(entry.name);
                String rendered = entry.secret ? value.renderMasked(request.showSecrets) : value.statusOrRaw();
                printStatus(entry.name, rendered);
            }
            printStatus("GITLAB_RELEASE_TOKEN", env.value("GITLAB_RELEASE_TOKEN").renderMasked(request.showSecrets));
        }
    }

    void doctorLocal(LocalDoctorRequest request) throws IOException, InterruptedException {
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

        out.println("== 本机运行时 ==");
        if (commandAvailable("java", "-version")) {
            printStatus("java -version", "OK");
        } else {
            printStatus("java", "MISSING");
            javaMissing = true;
            failed = true;
        }

        Path mvnw = repoRoot.resolve(mavenWrapperPath());
        boolean wrapperPresent = Files.exists(mvnw);
        if (wrapperPresent) {
            printStatus(mavenWrapperPath(), "OK");
        } else {
            printStatus(mavenWrapperPath(), "MISSING");
        }

        boolean systemMavenPresent = commandExists("mvn");
        if (!wrapperPresent) {
            printStatus("mvn", systemMavenPresent ? "OK" : "MISSING");
        }

        MavenCommand mavenCommand = resolveMavenCommand(repoRoot);
        if (mavenCommand == null) {
            printStatus("Maven command", "MISSING");
            mavenMissing = true;
            failed = true;
        } else {
            printStatus("Maven command", mavenCommand.command + " (" + mavenCommand.source + ")");
        }

        if (!javaMissing && mavenCommand != null) {
            if (commandAvailable(mavenCommand.command, "-q", "-version")) {
                printStatus(mavenCommand.versionLabel(), "OK");
            } else {
                printStatus(mavenCommand.versionLabel(), "FAILED");
                mavenFailed = true;
                failed = true;
            }
        } else {
            String versionLabel = mavenCommand == null ? "maven -q -version" : mavenCommand.versionLabel();
            printStatus(versionLabel, "SKIPPED");
        }

        out.println();
        out.println("== 本地 env 文件 ==");
        Path envPath = resolvePath(request.envFile);
        if (Files.exists(envPath)) {
            envPresent = true;
            printStatus(relativizePath(envPath), "OK");
        } else {
            printStatus(relativizePath(envPath), "MISSING");
            failed = true;
        }

        if (envPresent && !isExampleFile(envPath)) {
            LoadedEnv env = LoadedEnv.load(envPath);
            for (EnvEntry entry : COMMON_VARIABLES) {
                EnvValue value = env.value(entry.name);
                String status = requiredStatus(value, entry.required);
                printStatus(entry.name, status);
                if ("MISSING".equals(status) || "PLACEHOLDER".equals(status)) {
                    if (entry.required) {
                        failed = true;
                    }
                    if (!"OPTIONAL".equals(status)) {
                        envNeedsEdit = true;
                    }
                }
            }
            printStatus("GITLAB_RELEASE_TOKEN", requiredStatus(env.value("GITLAB_RELEASE_TOKEN"), false));
        } else if (envPresent) {
            printStatus("env file type", "INVALID");
            failed = true;
        } else {
            printStatus("env values", "SKIPPED");
        }

        out.println();
        out.println("== 平台 CLI ==");
        if (commandExists("gh")) {
            printStatus("gh", "OK");
            if (runQuietly(Arrays.asList("gh", "auth", "status"))) {
                printStatus("gh auth status", "OK");
            } else {
                printStatus("gh auth status", "FAILED");
                ghAuthFailed = true;
                failed = true;
            }
        } else {
            printStatus("gh", "MISSING");
            ghMissing = true;
            failed = true;
        }

        if (commandExists("glab")) {
            printStatus("glab", "OK");
            if (runQuietly(Arrays.asList("glab", "auth", "status"))) {
                printStatus("glab auth status", "OK");
            } else {
                printStatus("glab auth status", "FAILED");
                glabAuthFailed = true;
                failed = true;
            }
        } else {
            printStatus("glab", "MISSING");
            glabMissing = true;
            failed = true;
        }

        out.println();
        out.println("== 仓库标识 ==");
        printRepoStatus("GITHUB_REPO", request.githubRepo);
        printRepoStatus("GITLAB_REPO", request.gitlabRepo);
        if (!isBlank(request.githubRepo) && !request.githubRepo.contains("/")) {
            failed = true;
        }
        if (!isBlank(request.gitlabRepo) && !request.gitlabRepo.contains("/")) {
            failed = true;
        }

        out.println();
        if (!failed) {
            out.println("本机发布环境检查通过");
            out.println("下一步建议:");
            out.println("  make doctor-all GITHUB_REPO=owner/repo GITLAB_REPO=group/project");
            out.println("  make sync-all");
            return;
        }

        out.println("本机发布环境未就绪，建议按顺序处理:");
        if (!envPresent) {
            out.println("  1. 执行 make env-init 生成 env/release.env.local");
        }
        if (envNeedsEdit) {
            out.println("  2. 编辑 env/release.env.local，替换仓库地址、用户名、密码和需要的 token");
        }
        if (javaMissing || mavenMissing || mavenFailed) {
            out.println("  3. 安装并配置可用的 Java Runtime 与 Maven 命令（优先 ./mvnw，其次系统 mvn），然后重新执行 make readiness");
        }
        if (ghMissing || ghAuthFailed || glabMissing || glabAuthFailed) {
            out.println("  4. 执行 make auth-help，完成 gh / glab 的安装和登录");
        }
        out.println("  5. 通过后再执行 make doctor-github、make doctor-gitlab 和 sync-apply");
        throw new IllegalStateException("本机发布环境未就绪");
    }

    void doctorPlatform(DoctorPlatformRequest request) throws IOException, InterruptedException {
        LoadedEnv env = LoadedEnv.load(resolveEnvFile(request.envFile));
        boolean failed = false;

        out.println("使用 env 文件: " + relativizePath(env.path));
        out.println();
        out.println("== 本地 env 检查 ==");
        for (EnvEntry entry : COMMON_VARIABLES) {
            String status = requiredStatus(env.value(entry.name), entry.required);
            printStatus(entry.name, status);
            if (("MISSING".equals(status) || "PLACEHOLDER".equals(status)) && entry.required) {
                failed = true;
            }
        }
        printStatus("GITLAB_RELEASE_TOKEN", requiredStatus(env.value("GITLAB_RELEASE_TOKEN"), false));

        if (request.platform.includesGithub()) {
            out.println();
            out.println("== GitHub CLI 检查 ==");
            requireCommand("gh");
            printStatus("gh", "OK");
            if (!runQuietly(Arrays.asList("gh", "auth", "status"))) {
                printStatus("gh auth status", "FAILED");
                throw new IllegalStateException("gh auth status 失败，请先执行 make auth-help");
            }
            printStatus("gh auth status", "OK");
            if (isBlank(request.githubRepo)) {
                printStatus("GITHUB_REPO", "MISSING");
                failed = true;
            } else if (!request.githubRepo.contains("/")) {
                printStatus("GITHUB_REPO", "INVALID");
                failed = true;
            } else {
                printStatus("GITHUB_REPO", request.githubRepo);
            }
        }

        if (request.platform.includesGitlab()) {
            out.println();
            out.println("== GitLab CLI 检查 ==");
            requireCommand("glab");
            printStatus("glab", "OK");
            if (!runQuietly(Arrays.asList("glab", "auth", "status"))) {
                printStatus("glab auth status", "FAILED");
                throw new IllegalStateException("glab auth status 失败，请先执行 make auth-help");
            }
            printStatus("glab auth status", "OK");
            if (isBlank(request.gitlabRepo)) {
                printStatus("GITLAB_REPO", "MISSING");
                failed = true;
            } else if (!request.gitlabRepo.contains("/")) {
                printStatus("GITLAB_REPO", "INVALID");
                failed = true;
            } else {
                printStatus("GITLAB_REPO", request.gitlabRepo);
            }
        }

        out.println();
        if (failed) {
            throw new IllegalStateException("doctor 检查失败，请修正上面的 MISSING / PLACEHOLDER 项后重试");
        }
        out.println("doctor 检查完成");
    }

    void syncVars(SyncVarsRequest request) throws IOException, InterruptedException {
        LoadedEnv env = LoadedEnv.load(resolveEnvFile(request.envFile));
        out.println("使用 env 文件: " + relativizePath(env.path));
        if (!request.execute) {
            out.println("当前为 dry-run，只输出命令。传入 --execute true 后才会真正写入平台。");
        }

        if (request.execute) {
            if (isBlank(request.repo)) {
                throw new IllegalArgumentException("执行模式下必须通过 --repo 指定仓库");
            }
            if (request.platform.includesGithub()) {
                requireCommand("gh");
            }
            if (request.platform.includesGitlab()) {
                requireCommand("glab");
            }
        }

        if (request.platform.includesGithub()) {
            out.println();
            out.println("== GitHub CLI 命令 ==");
            for (EnvEntry entry : GITHUB_ACTIONS_VARIABLES) {
                syncGithub(env, request, entry, false);
            }
            for (EnvEntry entry : GITHUB_ACTIONS_SECRETS) {
                syncGithub(env, request, entry, true);
            }
        }

        if (request.platform.includesGitlab()) {
            out.println();
            out.println("== GitLab CLI 命令 ==");
            for (EnvEntry entry : GITLAB_VARIABLES) {
                syncGitlab(env, request, entry);
            }
        }
    }

    void auditVars(AuditVarsRequest request) throws IOException, InterruptedException {
        LoadedEnv env = LoadedEnv.load(resolveEnvFile(request.envFile));
        boolean failed = false;
        out.println("使用 env 文件: " + relativizePath(env.path));

        if (request.platform.includesGithub()) {
            requireRepo("GITHUB_REPO", request.githubRepo);
            requireCommand("gh");
            if (!runQuietly(Arrays.asList("gh", "auth", "status"))) {
                throw new IllegalStateException("gh auth status 失败，请先执行 make auth-help");
            }
            String variablesJson = runAndCapture(Arrays.asList("gh", "variable", "list", "--repo", request.githubRepo,
                "--json", "name,value,updatedAt")).stdoutText();
            String secretsJson = runAndCapture(Arrays.asList("gh", "secret", "list", "--repo", request.githubRepo,
                "--json", "name,updatedAt")).stdoutText();
            Map<String, Map<String, String>> githubVariables = parseFlatJsonObjects(variablesJson);
            Map<String, Map<String, String>> githubSecrets = parseFlatJsonObjects(secretsJson);

            out.println();
            out.println("== GitHub Variables 审计 ==");
            for (EnvEntry entry : GITHUB_ACTIONS_VARIABLES) {
                AuditOutcome outcome = auditValue(env.value(entry.name), githubVariables.get(entry.name), true);
                printAuditStatus(entry.name, outcome);
                if (outcome.isFailure()) {
                    failed = true;
                }
            }

            out.println();
            out.println("== GitHub Secrets 审计 ==");
            for (EnvEntry entry : GITHUB_ACTIONS_SECRETS) {
                AuditOutcome outcome = auditPresence(env.value(entry.name), githubSecrets.get(entry.name));
                printAuditStatus(entry.name, outcome);
                if (outcome.isFailure()) {
                    failed = true;
                }
            }
        }

        if (request.platform.includesGitlab()) {
            requireRepo("GITLAB_REPO", request.gitlabRepo);
            requireCommand("glab");
            if (!runQuietly(Arrays.asList("glab", "auth", "status"))) {
                throw new IllegalStateException("glab auth status 失败，请先执行 make auth-help");
            }
            String exported = runAndCapture(Arrays.asList("glab", "variable", "export", "--repo", request.gitlabRepo,
                "--output", "env")).stdoutText();
            LoadedEnv remoteEnv = LoadedEnv.parse(exported, repoRoot.resolve("target/gitlab-variables.env"));

            out.println();
            out.println("== GitLab Variables 审计 ==");
            for (EnvEntry entry : GITLAB_VARIABLES) {
                AuditOutcome outcome = auditValue(env.value(entry.name), remoteEnv.value(entry.name), true);
                printAuditStatus(entry.name, outcome);
                if (outcome.isFailure()) {
                    failed = true;
                }
            }
        }

        out.println();
        if (failed) {
            throw new IllegalStateException("平台变量审计失败，请修正 MISSING_REMOTE / MISMATCH 项后重试");
        }
        out.println("平台变量审计通过");
    }

    private void printEnvEntries(LoadedEnv env, List<EnvEntry> entries, boolean showSecrets) {
        for (EnvEntry entry : entries) {
            EnvValue value = env.value(entry.name);
            String rendered = entry.secret ? value.renderMasked(showSecrets) : value.statusOrRaw();
            printStatus(entry.name, rendered);
        }
    }

    private void printStatus(String label, String value) {
        out.printf("%-40s %s%n", label, value);
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

    private String requiredStatus(EnvValue value, boolean required) {
        if (value.missing) {
            return required ? "MISSING" : "OPTIONAL";
        }
        if (value.placeholder) {
            return "PLACEHOLDER";
        }
        return "OK";
    }

    private void syncGithub(LoadedEnv env, SyncVarsRequest request, EnvEntry entry, boolean secret)
        throws IOException, InterruptedException {
        EnvValue value = env.value(entry.name);
        if (!value.isReal()) {
            return;
        }
        List<String> command = new ArrayList<String>();
        command.add("gh");
        command.add(secret ? "secret" : "variable");
        command.add("set");
        command.add(entry.name);
        command.add("--body");
        command.add(value.raw);
        if (!isBlank(request.repo)) {
            command.add("--repo");
            command.add(request.repo);
        }
        if (request.execute) {
            out.println("执行: " + renderCommand(command));
            int exitCode = runCommand(command, repoRoot);
            if (exitCode != 0) {
                throw new IllegalStateException("命令执行失败: " + renderCommand(command));
            }
            return;
        }
        String previewValue = secret ? value.renderMasked(request.showSecrets) : value.raw;
        out.println("gh " + (secret ? "secret" : "variable") + " set "
            + padRight(entry.name, secret ? 36 : 34) + " --body " + previewValue
            + repoFlagPreview(request.repo));
    }

    private void syncGitlab(LoadedEnv env, SyncVarsRequest request, EnvEntry entry)
        throws IOException, InterruptedException {
        EnvValue value = env.value(entry.name);
        if (!value.isReal()) {
            return;
        }
        List<String> command = new ArrayList<String>();
        command.add("glab");
        command.add("variable");
        command.add("set");
        command.add(entry.name);
        command.add("--value");
        command.add(value.raw);
        if (entry.secret) {
            command.add("--masked");
        }
        if (entry.protectedValue) {
            command.add("--protected");
        }
        if (!isBlank(request.repo)) {
            command.add("--repo");
            command.add(request.repo);
        }
        if (request.execute) {
            out.println("执行: " + renderCommand(command));
            int exitCode = runCommand(command, repoRoot);
            if (exitCode != 0) {
                throw new IllegalStateException("命令执行失败: " + renderCommand(command));
            }
            return;
        }
        out.println("glab variable set " + padRight(entry.name, 31) + " --value "
            + value.renderMasked(request.showSecrets)
            + (entry.secret ? " --masked" : "")
            + (entry.protectedValue ? " --protected" : "")
            + repoFlagPreview(request.repo));
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

    private void printAuditStatus(String label, AuditOutcome outcome) {
        printStatus(label, outcome.message);
    }

    private Path resolveEnvFile(String envFile) throws IOException {
        Path path = resolvePath(envFile);
        if (!Files.exists(path)) {
            throw new IllegalStateException("未找到 env 文件: " + relativizePath(path));
        }
        if (isExampleFile(path)) {
            throw new IllegalStateException("请不要直接使用示例文件: " + relativizePath(path));
        }
        return path;
    }

    private Path resolvePath(String value) {
        Path path = Paths.get(value);
        if (!path.isAbsolute()) {
            path = repoRoot.resolve(path).normalize();
        }
        return path;
    }

    private boolean isExampleFile(Path path) {
        return path.getFileName().toString().endsWith(".example");
    }

    private boolean commandAvailable(String... command) throws InterruptedException {
        try {
            return runQuietly(Arrays.asList(command));
        } catch (IOException exception) {
            return false;
        }
    }

    private boolean commandExists(String command) {
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(repoRoot.toFile());
            builder.redirectErrorStream(true);
            Process process = builder.start();
            closeQuietly(process.getInputStream());
            process.destroy();
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    private void requireCommand(String command) {
        if (!commandExists(command)) {
            throw new IllegalStateException("未找到 " + command + " CLI");
        }
    }

    private boolean runQuietly(List<String> command) throws IOException, InterruptedException {
        CommandResult result = runAndCapture(command);
        return result.exitCode == 0;
    }

    private CommandResult runAndCapture(List<String> command) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(repoRoot.toFile());
        Process process = builder.start();
        byte[] stdout = readAllBytes(process.getInputStream());
        byte[] stderr = readAllBytes(process.getErrorStream());
        int exitCode = process.waitFor();
        return new CommandResult(exitCode, stdout, stderr);
    }

    private void requireRepo(String label, String value) {
        if (isBlank(value)) {
            throw new IllegalArgumentException("缺少仓库参数: " + label);
        }
    }

    private String relativizePath(Path path) {
        Path normalizedRoot = repoRoot.toAbsolutePath().normalize();
        Path normalizedPath = path.toAbsolutePath().normalize();
        if (normalizedPath.startsWith(normalizedRoot)) {
            return normalizedRoot.relativize(normalizedPath).toString();
        }
        return normalizedPath.toString();
    }

    private String repoFlagPreview(String repo) {
        if (isBlank(repo)) {
            return "";
        }
        return " --repo " + repo;
    }
}
