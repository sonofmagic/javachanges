package io.github.sonofmagic.javachanges.core;

import java.nio.file.Path;
import java.util.List;

public final class ReleaseMessages {
    private ReleaseMessages() {
    }

    public static ReleaseLanguage language() {
        return ReleaseLanguageContext.get();
    }

    public static boolean zh() {
        return language().isChinese();
    }

    private static String text(String english, String chinese) {
        return zh() ? chinese : english;
    }

    private static String message(String key, Object... args) {
        return I18n.message(key, args);
    }

    public static String initialized(Path repoRoot) {
        return message("initialized", repoRoot);
    }

    public static String pathAction(String action) {
        if (!zh()) {
            return action;
        }
        if ("Created".equals(action)) {
            return message("pathAction.created");
        }
        if ("Replaced".equals(action)) {
            return message("pathAction.replaced");
        }
        if ("Kept".equals(action)) {
            return message("pathAction.kept");
        }
        if ("Skipped".equals(action)) {
            return message("pathAction.skipped");
        }
        return action;
    }

    public static String nextSteps() {
        return message("nextSteps");
    }

    public static String describeChangePlaceholder() {
        return message("describeChangePlaceholder");
    }

    public static String changesetSummaryPrompt() {
        return message("changesetSummaryPrompt");
    }

    public static String changesetReleaseLevelPrompt() {
        return message("changesetReleaseLevelPrompt");
    }

    public static String changesetBodyPrompt() {
        return message("changesetBodyPrompt");
    }

    public static String changesetReadme() {
        return I18n.template("changeset-readme.md");
    }

    public static String releaseLevelHeading(ReleaseLevel level) {
        if (level == ReleaseLevel.MAJOR) {
            return message("releaseLevelHeading.major");
        }
        if (level == ReleaseLevel.MINOR) {
            return message("releaseLevelHeading.minor");
        }
        return message("releaseLevelHeading.patch");
    }

    public static String changelogPackagesLabel() {
        return message("changelogPackagesLabel");
    }

    public static String noUserFacingChanges() {
        return message("noUserFacingChanges");
    }

    public static String releasePlanTitle() {
        return message("releasePlanTitle");
    }

    public static String releasePlanIntro() {
        return message("releasePlanIntro");
    }

    public static String fieldHeader() {
        return message("fieldHeader");
    }

    public static String valueHeader() {
        return message("valueHeader");
    }

    public static String releaseTypeField() {
        return message("releaseTypeField");
    }

    public static String affectedPackagesField() {
        return message("affectedPackagesField");
    }

    public static String releaseVersionField() {
        return message("releaseVersionField");
    }

    public static String tagStrategyField() {
        return message("tagStrategyField");
    }

    public static String plannedTagsField() {
        return message("plannedTagsField");
    }

    public static String nextSnapshotField() {
        return message("nextSnapshotField");
    }

    public static String includedChangesetsTitle() {
        return message("includedChangesetsTitle");
    }

    public static String releaseLabel() {
        return message("releaseLabel");
    }

    public static String packagesLabel() {
        return message("packagesLabel");
    }

    public static String typeLabel() {
        return message("typeLabel");
    }

    public static String notesLabel() {
        return message("notesLabel");
    }

    public static String whatHappensNextTitle() {
        return message("whatHappensNextTitle");
    }

    public static String mergeTriggersTagPush() {
        return message("mergeTriggersTagPush");
    }

    public static String workflowsReuseMetadata() {
        return message("workflowsReuseMetadata");
    }

    public static String repository() {
        return message("repository");
    }

    public static String currentRevision() {
        return message("currentRevision");
    }

    public static String latestWholeRepoTag() {
        return message("latestWholeRepoTag");
    }

    public static String none() {
        return message("none");
    }

    public static String pendingChangesets() {
        return message("pendingChangesets");
    }

    public static String releasePlan() {
        return message("releasePlan");
    }

    public static String changesets() {
        return message("changesets");
    }

    public static String releaseType() {
        return message("releaseType");
    }

    public static String affectedPackages() {
        return message("affectedPackages");
    }

    public static String releaseVersion() {
        return message("releaseVersion");
    }

    public static String nextSnapshot() {
        return message("nextSnapshot");
    }

    public static String createdChangeset(Path relativePath) {
        return message("createdChangeset", relativePath);
    }

    public static String releaseLevel() {
        return message("releaseLevel");
    }

    public static String noPendingChangesets() {
        return message("noPendingChangesets");
    }

    public static String noPendingChangesetsToApply() {
        return message("noPendingChangesetsToApply");
    }

    public static String appliedReleasePlan(String version) {
        return message("appliedReleasePlan", version);
    }

    public static String createdOne() {
        return message("createdOne");
    }

    public static String reviewPlan() {
        return message("reviewPlan");
    }

    public static String thenReviewPlan() {
        return message("thenReviewPlan");
    }

    public static String applyLocally() {
        return message("applyLocally");
    }

    public static String openGithubPr() {
        return message("openGithubPr");
    }

    public static String openGitlabMr() {
        return message("openGitlabMr");
    }

    public static String nextStepFor(Path repoRoot) {
        return message("nextStepFor", repoRoot);
    }

    public static String plannedRelease() {
        return message("plannedRelease");
    }

    public static String unsupportedLanguage(String value) {
        return message("unsupportedLanguage", value);
    }

    public static String unsupportedOutputFormat(String value) {
        return message("unsupportedOutputFormat", value);
    }

    public static String unsupportedPlatform(String value) {
        return message("unsupportedPlatform", value);
    }

    public static String unsupportedReleaseLevel(String value) {
        return message("unsupportedReleaseLevel", value);
    }

    public static String unknownArgument(String value) {
        return message("unknownArgument", value);
    }

    public static String missingRequiredOption(String name) {
        return message("missingRequiredOption", name);
    }

    public static String missingEnv(String name) {
        return message("missingEnv", name);
    }

    public static String unknownModule(Path repoRoot, String module, List<String> knownModules) {
        return message("unknownModule", module, knownModules, repoRoot);
    }

    public static String unsupportedReleaseTag(String tag) {
        return message("unsupportedReleaseTag", tag);
    }

    public static String atLeastOneModuleRequired() {
        return message("atLeastOneModuleRequired");
    }

    public static String unsupportedChangeType(String type) {
        return message("unsupportedChangeType", type);
    }

    public static String invalidChangesetFrontmatter(Path path) {
        return message("invalidChangesetFrontmatter", path);
    }

    public static String invalidChangesetLine(Path path, String line) {
        return message("invalidChangesetLine", path, line);
    }

    public static String missingPackageReleaseEntries(Path path) {
        return message("missingPackageReleaseEntries", path);
    }

    public static String missingKeyIn(String key, Path path) {
        return message("missingKeyIn", key, path);
    }

    public static String missingReleasePlanManifest(Path manifest) {
        return text("Missing release plan manifest: " + manifest, "缺少 release plan manifest: " + manifest);
    }

    public static String missingFieldIn(String field, Path manifest) {
        return text("Missing field `" + field + "` in " + manifest, "缺少字段 `" + field + "`: " + manifest);
    }

    public static String cannotFindRepoRoot(Path current) {
        return text("Cannot find repository root from " + current, "无法从 " + current + " 找到仓库根目录");
    }

    public static String unsupportedSnapshotVersionMode(String value) {
        return text(
            "Unsupported snapshot version mode: " + value + ". Use plain or stamped.",
            "不支持的 snapshot version mode: " + value + "，可选值: plain, stamped"
        );
    }

    public static String unsupportedTagStrategy(String value) {
        return text(
            "Unsupported tag strategy: " + value + ". Use whole-repo or per-module.",
            "不支持的 tag 策略: " + value + "，可选值: whole-repo, per-module"
        );
    }

    public static String unsupportedVersion(String value) {
        return text("Unsupported version: " + value, "不支持的版本: " + value);
    }

    public static String missingVersionConfig() {
        return text(
            "Cannot find version configuration in Maven pom.xml or Gradle gradle.properties",
            "未在 Maven pom.xml 或 Gradle gradle.properties 中找到版本配置"
        );
    }

    public static String notSnapshot(String version) {
        return text("Current project version is not a SNAPSHOT: " + version, "当前项目版本不是 SNAPSHOT: " + version);
    }

    public static String emptySnapshotBuildStamp() {
        return text("snapshot build stamp cannot be empty", "snapshot build stamp 不能为空");
    }

    public static String invalidSnapshotBuildStamp(String buildStamp) {
        return text(
            "snapshot build stamp only allows letters, numbers, and dots: " + buildStamp,
            "snapshot build stamp 只允许字母、数字和点号: " + buildStamp
        );
    }

    public static String releaseTagVersionMismatch(String tag, String version, String releaseVersion) {
        return text(
            "tag " + tag + " does not match project version " + version + "; expected base version " + releaseVersion,
            "tag " + tag + " 与项目版本 " + version + " 不匹配，期望基础版本为 " + releaseVersion
        );
    }

    public static String missingRepositoryCredentials(String mode, String specificUser, String specificPassword) {
        return text(
            "Missing " + mode + " repository credentials. Set " + specificUser + "/" + specificPassword
                + " or MAVEN_REPOSITORY_USERNAME/MAVEN_REPOSITORY_PASSWORD.",
            "缺少 " + mode + " 仓库认证信息，请设置 " + specificUser + "/" + specificPassword
                + " 或通用 MAVEN_REPOSITORY_USERNAME/MAVEN_REPOSITORY_PASSWORD"
        );
    }

    public static String outputPathMustStayInsideRepository(String optionName, String value) {
        return text(
            optionName + " must stay inside repository: " + value,
            optionName + " 必须位于仓库内: " + value
        );
    }

    public static String cannotFindPomProperties(Path pomPath) {
        return text("Cannot find <properties> in " + pomPath, "无法在 " + pomPath + " 中找到 <properties>");
    }

    public static String cannotFindPomRevision(Path pomPath) {
        return text("Cannot find <revision> in " + pomPath, "无法在 " + pomPath + " 中找到 <revision>");
    }

    public static String failedToConfigureXmlParser(Path path) {
        return text("Failed to configure XML parser for " + path, "配置 XML parser 失败: " + path);
    }

    public static String failedToParsePom(Path path) {
        return text("Failed to parse pom.xml: " + path, "解析 pom.xml 失败: " + path);
    }

    public static String failedToWritePom(Path path) {
        return text("Failed to write pom.xml: " + path, "写入 pom.xml 失败: " + path);
    }

    public static String failedToDetectMavenModules(Path repoRoot) {
        return text("Failed to detect Maven modules from " + repoRoot, "检测 Maven 模块失败: " + repoRoot);
    }

    public static String failedToDetectGradleModules(Path repoRoot) {
        return text("Failed to detect Gradle modules from " + repoRoot, "检测 Gradle 模块失败: " + repoRoot);
    }

    public static String cannotFindSupportedBuildModel(Path repoRoot) {
        return text(
            "Cannot find supported Maven or Gradle build model in " + repoRoot,
            "无法在 " + repoRoot + " 中找到支持的 Maven 或 Gradle 构建模型"
        );
    }

    public static String cannotFindGradleVersion(Path path) {
        return text("Cannot find version or revision in " + path, "无法在 " + path + " 中找到 version 或 revision");
    }

    public static String cannotParseGradleVersion(Path path) {
        return text("Cannot parse version property in " + path, "无法解析 " + path + " 中的 version 配置");
    }

    public static String failedToParseJson() {
        return text("Failed to parse JSON", "解析 JSON 失败");
    }

    public static String failedToWriteJson() {
        return text("Failed to write JSON", "写入 JSON 失败");
    }

    public static String failedToReadChangesetConfig() {
        return text("Failed to read changeset config", "读取 changeset 配置失败");
    }

    public static String gitCommandFailed() {
        return text("git command failed", "git 命令执行失败");
    }

    public static String gitCommandFailed(String command) {
        return text("git command failed: " + command, "git 命令执行失败: " + command);
    }

    public static String failedToCaptureProcessOutput() {
        return text("Failed to capture process output", "捕获进程输出失败");
    }

    public static String gitTagFailed(String error) {
        return text("git tag failed: " + error, "git tag 执行失败: " + error);
    }

    public static String gitReturnedEmptyOutput(Object args) {
        return text("git returned empty output for " + args, "git 返回了空输出: " + args);
    }

    public static String unexpectedGitLsRemoteOutput(String stdout) {
        return text("Unexpected git ls-remote output: " + stdout, "git ls-remote 输出不符合预期: " + stdout);
    }

    public static String gitRevParseHeadFailed() {
        return text("git rev-parse HEAD failed", "git rev-parse HEAD 执行失败");
    }

    public static String currentHeadShaEmpty() {
        return text("Current HEAD SHA is empty", "当前 HEAD SHA 为空");
    }

    public static String ghPrListFailed() {
        return text("gh pr list failed", "gh pr list 执行失败");
    }

    public static String ghCommandFailed(Object args) {
        return text("gh command failed: " + args, "gh 命令执行失败: " + args);
    }

    public static String targetFileExists(Path target) {
        return text(
            "Target file already exists. Pass --force true to overwrite: " + target,
            "目标文件已存在。传入 --force true 可覆盖: " + target
        );
    }

    public static String generatedGithubActionsWorkflow(Path relativePath) {
        return text("Generated GitHub Actions workflow: ", "已生成 GitHub Actions workflow: ") + relativePath;
    }

    public static String generatedGitlabCiTemplate(Path relativePath) {
        return text("Generated GitLab CI template: ", "已生成 GitLab CI template: ") + relativePath;
    }

    public static String unsupportedBuildTool(String buildTool) {
        return text("Unsupported build tool: " + buildTool, "不支持的构建工具: " + buildTool);
    }

    public static String javachangesFailed(int exitCode) {
        return text("javachanges failed with exit code " + exitCode, "javachanges 执行失败，退出码: " + exitCode);
    }

    public static String unterminatedJavachangesArgsQuote() {
        return text(
            "Unterminated quoted argument in javachanges.args",
            "javachanges.args 中存在未闭合的引号参数"
        );
    }

    public static String attemptsMustBePositive() {
        return text("--attempts must be greater than 0", "--attempts 必须大于 0");
    }

    public static String retryDelayMustBeNonNegative() {
        return text("--retry-delay-seconds must be 0 or greater", "--retry-delay-seconds 必须大于等于 0");
    }

    public static String signingKeyNotVisible(String fingerprint, String primaryKeyserver, String secondaryKeyserver) {
        return text(
            "The signing key fingerprint " + fingerprint + " is still not visible from " + primaryKeyserver
                + " or " + secondaryKeyserver + ". Publish the public key to a supported keyserver and rerun the workflow.",
            "签名密钥指纹 " + fingerprint + " 仍无法从 " + primaryKeyserver + " 或 " + secondaryKeyserver
                + " 查询到。请把公钥发布到受支持的 keyserver 后重新运行 workflow。"
        );
    }

    public static String failedToInspectGpgSecretKeys(String detail) {
        return text("Failed to inspect imported GPG secret keys: " + detail, "检查已导入的 GPG 私钥失败: " + detail);
    }

    public static String noImportedSecretKeyFingerprint() {
        return text(
            "No imported secret key fingerprint was found after actions/setup-java.",
            "actions/setup-java 后未找到已导入私钥的 fingerprint。"
        );
    }

    public static String missingGithubRepo() {
        return text(
            "Missing GitHub repo. Pass --github-repo or set GITHUB_REPOSITORY.",
            "缺少 GitHub repo。请传入 --github-repo 或设置 GITHUB_REPOSITORY。"
        );
    }

    public static String missingGitlabProjectId() {
        return text(
            "Missing GitLab project id. Pass --project-id or set CI_PROJECT_ID.",
            "缺少 GitLab project id。请传入 --project-id 或设置 CI_PROJECT_ID。"
        );
    }

    public static String missingGitlabTag() {
        return text(
            "Missing GitLab tag. Pass --tag or set CI_COMMIT_TAG.",
            "缺少 GitLab tag。请传入 --tag 或设置 CI_COMMIT_TAG。"
        );
    }

    public static String connectTimeoutMustBeNonNegative() {
        return text("connectTimeoutMillis must be 0 or greater", "connectTimeoutMillis 必须大于等于 0");
    }

    public static String readTimeoutMustBeNonNegative() {
        return text("readTimeoutMillis must be 0 or greater", "readTimeoutMillis 必须大于等于 0");
    }

    public static String missingGitlabResponseField(String field, String json) {
        return text(
            "Missing `" + field + "` in GitLab response: " + json,
            "GitLab 响应缺少 `" + field + "`: " + json
        );
    }

    public static String gitlabApiNotFound(String method, String path) {
        return text(
            "GitLab API " + method + " " + path + " failed: Not found",
            "GitLab API " + method + " " + path + " 失败: Not found"
        );
    }

    public static String gitlabApiFailed(String method, String path, String detail) {
        return text(
            "GitLab API " + method + " " + path + " failed: " + detail,
            "GitLab API " + method + " " + path + " 失败: " + detail
        );
    }

    public static String failedToEncodeUrlComponent() {
        return text("Failed to encode URL component", "URL 组件编码失败");
    }

    public static String failedToEncodeGitlabProjectPath() {
        return text("Failed to encode GitLab project path", "GitLab 项目路径编码失败");
    }

    public static String createGitlabProtectedVariable(String name) {
        return text(
            "Create GitLab project variable " + name + " and mark it protected",
            "在 GitLab 项目变量中创建 " + name + "，并勾选 protected"
        );
    }

    public static String markGitlabVariableProtected(String name) {
        return text(
            "Mark GitLab project variable " + name + " as protected",
            "把 GitLab 项目变量 " + name + " 改为 protected"
        );
    }

    public static String protectGitlabBranchForProtectedVariables(String branch) {
        return text(
            "Protect GitLab branch " + branch + " so protected variables are injected into that branch pipeline",
            "保护 GitLab 分支 " + branch + "，否则 protected variables 不会注入该分支的 pipeline"
        );
    }

    public static String removeProtectedVariableFlagWithLowerSecurity() {
        return text(
            "Alternatively, remove protected from the related Maven / release variables, with lower security",
            "或者取消相关 Maven / release 变量的 protected 标记，但这会降低安全性"
        );
    }

    public static String protectGitlabBranchForSnapshot(String branch) {
        return text(
            "Protect GitLab branch " + branch + " so snapshot publishing matches protected variable behavior",
            "保护 GitLab 分支 " + branch + "，让 snapshot 发布与 protected variables 行为一致"
        );
    }

    public static String freshMetadataCannotInferPerModuleTargets() {
        return text(
            "Fresh release metadata cannot infer per-module release targets after changesets are consumed. "
                + "Use the committed release plan manifest or run before applying the plan.",
            "changeset 已消费后，fresh release metadata 无法推断 per-module 发布目标。"
                + "请使用已提交的 release plan manifest，或在应用计划前运行。"
        );
    }

    public static String multipleTagsRequireExplicitTagCommand() {
        return text(
            "Release plan defines multiple tags under per-module strategy. "
                + "Use explicit tag-based release commands instead of release-from-plan.",
            "release plan 在 per-module 策略下定义了多个 tag。"
                + "请使用显式基于 tag 的发布命令，而不是 release-from-plan。"
        );
    }

    public static String missingReleaseManifestField(String field) {
        return text("Missing field " + field + " in release manifest", "release manifest 缺少字段 " + field);
    }

    public static String missingFieldInSource(String field, String source) {
        return text("Missing field `" + field + "` in " + source, source + " 缺少字段 `" + field + "`");
    }

    public static String releaseNotesSection(String section) {
        if ("Breaking Changes".equals(section)) {
            return text("Breaking Changes", "重大变更");
        }
        if ("Features".equals(section)) {
            return text("Features", "功能");
        }
        if ("Fixes".equals(section)) {
            return text("Fixes", "修复");
        }
        if ("Performance".equals(section)) {
            return text("Performance", "性能");
        }
        if ("Refactoring".equals(section)) {
            return text("Refactoring", "重构");
        }
        if ("Build".equals(section)) {
            return text("Build", "构建");
        }
        if ("Documentation".equals(section)) {
            return text("Documentation", "文档");
        }
        if ("Tests".equals(section)) {
            return text("Tests", "测试");
        }
        if ("CI".equals(section)) {
            return text("CI", "CI");
        }
        if ("Chores".equals(section)) {
            return text("Chores", "杂项");
        }
        return text("Other", "其他");
    }

    public static String[] githubAuthHelpLines() {
        return I18n.templateLines("github-auth-help.txt");
    }

    public static String[] gitlabAuthHelpLines() {
        return I18n.templateLines("gitlab-auth-help.txt");
    }

    public static String usingEnvFile(String envPath) {
        return message("env.usingEnvFile", envPath);
    }

    public static String sensitiveValuesMaskedByDefault() {
        return message("env.sensitiveValuesMaskedByDefault");
    }

    public static String dryRunOnlyPrintsCommands() {
        return message("env.dryRunOnlyPrintsCommands");
    }

    public static String repoRequiredInExecuteMode() {
        return message("env.repoRequiredInExecuteMode");
    }

    public static String githubCliCommandsHeading() {
        return message("env.githubCliCommandsHeading");
    }

    public static String gitlabCliCommandsHeading() {
        return message("env.gitlabCliCommandsHeading");
    }

    public static String runningCommand(String displayCommand) {
        return message("env.runningCommand", displayCommand);
    }

    public static String commandFailed(String displayCommand) {
        return message("env.commandFailed", displayCommand);
    }

    public static String templateFileNotFound(String path) {
        return message("env.templateFileNotFound", path);
    }

    public static String targetFileCannotBeExample(String path) {
        return message("env.targetFileCannotBeExample", path);
    }

    public static String targetFileKept(String path) {
        return message("env.targetFileKept", path);
    }

    public static String recreateEnvFileCommand(String path) {
        return message("env.recreateEnvFileCommand", path);
    }

    public static String generatedLocalEnvFile(String path) {
        return message("env.generatedLocalEnvFile", path);
    }

    public static String editEnvFileNextStep() {
        return message("env.editEnvFileNextStep");
    }

    public static String platformVariableAuditFailed() {
        return message("env.platformVariableAuditFailed");
    }

    public static String platformVariableAuditPassed() {
        return message("env.platformVariableAuditPassed");
    }

    public static String githubVariablesAudit() {
        return message("env.githubVariablesAudit");
    }

    public static String githubSecretsAudit() {
        return message("env.githubSecretsAudit");
    }

    public static String gitlabVariablesAudit() {
        return message("env.gitlabVariablesAudit");
    }

    public static String heading(String label) {
        return "== " + label + " ==";
    }

    public static String missingRepositoryArgument(String name) {
        return message("env.missingRepositoryArgument", name);
    }

    public static String cliNotFound(String command) {
        return message("env.cliNotFound", command);
    }

    public static String authStatusFailed(String command) {
        return message("env.authStatusFailed", command);
    }

    public static String localRuntime() {
        return message("env.localRuntime");
    }

    public static String localEnvFile() {
        return message("env.localEnvFile");
    }

    public static String platformCli() {
        return message("env.platformCli");
    }

    public static String repositoryIdentifiers() {
        return message("env.repositoryIdentifiers");
    }

    public static String localEnvCheck() {
        return message("env.localEnvCheck");
    }

    public static String githubCliCheck() {
        return message("env.githubCliCheck");
    }

    public static String gitlabCliCheck() {
        return message("env.gitlabCliCheck");
    }

    public static String localReleaseEnvironmentPassed() {
        return message("env.localReleaseEnvironmentPassed");
    }

    public static String suggestedNextSteps() {
        return message("env.suggestedNextSteps");
    }

    public static String localReleaseEnvironmentNotReadyIntro() {
        return message("env.localReleaseEnvironmentNotReadyIntro");
    }

    public static String runEnvInitSuggestion() {
        return message("env.runEnvInitSuggestion");
    }

    public static String editLocalEnvSuggestion() {
        return message("env.editLocalEnvSuggestion");
    }

    public static String installJavaMavenSuggestion() {
        return message("env.installJavaMavenSuggestion");
    }

    public static String runAuthHelpSuggestion() {
        return message("env.runAuthHelpSuggestion");
    }

    public static String runPlatformDoctorSuggestion() {
        return message("env.runPlatformDoctorSuggestion");
    }

    public static String localReleaseEnvironmentNotReady() {
        return message("env.localReleaseEnvironmentNotReady");
    }

    public static String doctorPlatformFailed() {
        return message("env.doctorPlatformFailed");
    }

    public static String doctorCheckCompleted() {
        return message("env.doctorCheckCompleted");
    }

    public static String missingSnapshotOrTag() {
        return text(
            "Must specify --snapshot true or --tag <value>",
            "必须指定 --snapshot true 或 --tag <value>"
        );
    }

    public static String snapshotAndTagMutuallyExclusive() {
        return text("--snapshot and --tag cannot be used together", "--snapshot 和 --tag 不能同时使用");
    }

    public static String useForceToReplaceDefaultTemplate() {
        return text(
            "  Use --force to replace it with the default template.",
            "  使用 --force 可替换为默认模板。"
        );
    }

    public static String useConfigToWriteDefaultTemplate() {
        return text(" (use --config to write the default template)", " (使用 --config 写入默认模板)");
    }

    public static String buildTool() {
        return text("Build tool", "构建工具");
    }

    public static String versionFile() {
        return text("Version file", "版本文件");
    }

    public static String modules() {
        return text("Modules", "模块");
    }

    public static String moduleOk() {
        return text("module ok", "模块校验通过");
    }

    public static String snapshotOk() {
        return text("snapshot ok", "snapshot 校验通过");
    }

    public static String releaseTagOk() {
        return text("release tag ok", "release tag 校验通过");
    }

    public static String generatedMavenSettings(String output) {
        return text("Generated Maven settings: ", "已生成 Maven settings: ") + output;
    }

    public static String generatedReleaseNotes(String output) {
        return text("Generated release notes: ", "已生成 release notes: ") + output;
    }

    public static String gpgPublicKeyOk(String fingerprint) {
        return text("gpg public key ok: ", "gpg 公钥校验通过: ") + fingerprint;
    }

    public static String envFileNotFound(String path) {
        return text("Env file not found: " + path, "未找到 env 文件: " + path);
    }

    public static String doNotUseExampleEnvFile(String path) {
        return text("Do not use the example env file directly: " + path, "请不要直接使用示例文件: " + path);
    }

    public static String explicitModuleDoesNotMatchTagModule(String resolvedModule, String tagModule) {
        return text(
            "Explicit module " + resolvedModule + " does not match module " + tagModule + " from tag",
            "显式指定的模块 " + resolvedModule + " 与 tag 中的模块 " + tagModule + " 不一致"
        );
    }

    public static String noMavenCommandFound() {
        return text(
            "No Maven command found. Expected " + ReleaseProcessUtils.mavenWrapperPath()
                + " in the repository or mvn on PATH.",
            "未找到可用的 Maven 命令，期望仓库内存在 "
                + ReleaseProcessUtils.mavenWrapperPath() + " 或系统中可用 mvn"
        );
    }

    public static String noGradleCommandFound() {
        return text(
            "No Gradle command found. Expected " + ReleaseProcessUtils.gradleWrapperPath()
                + " in the repository or gradle on PATH.",
            "未找到可用的 Gradle 命令，期望仓库内存在 "
                + ReleaseProcessUtils.gradleWrapperPath() + " 或系统中可用 gradle"
        );
    }

    public static String dryRunOutputHeading() {
        return text("== Dry Run Output ==", "== Dry Run 输出 ==");
    }

    public static String generatedMavenSettingsFile() {
        return text("Generated .m2/settings.xml", "已生成 .m2/settings.xml");
    }

    public static String mavenSettingsWillBeWritten() {
        return text(
            "Maven settings generation check passed; execution will write .m2/settings.xml",
            "Maven settings 生成校验通过；执行时将写入 .m2/settings.xml"
        );
    }

    public static String mavenCommandLabel(String command, String source) {
        return text("Maven command: ", "Maven 命令: ") + command + " (" + source + ")";
    }

    public static String localMavenRepository(Path path) {
        return text("Local Maven repository: ", "本地 Maven 仓库: ") + path;
    }

    public static String generatedReleaseNotesFile() {
        return text("Generated target/release-notes.md", "已生成 target/release-notes.md");
    }

    public static String releaseNotesWillBeWritten() {
        return text(
            "release notes generation check passed; execution will write target/release-notes.md",
            "release notes 生成校验通过；执行时将写入 target/release-notes.md"
        );
    }

    public static String targetModule(String module) {
        return module == null ? text("target module: all", "目标模块: all") : text("target module: ", "目标模块: ") + module;
    }

    public static String commandToRun() {
        return text("Command to run:", "将执行的命令:");
    }

    public static String dryRunOnlyMavenPublish() {
        return text(
            "Dry-run only. Pass --execute true to run Maven publish.",
            "当前为 dry-run，未执行 Maven。传入 --execute true 才会真正发布。"
        );
    }

    public static String dryRunOnlyGradlePublish() {
        return text(
            "Dry-run only. Pass --execute true to run Gradle publish.",
            "当前为 dry-run，未执行 Gradle。传入 --execute true 才会真正发布。"
        );
    }

    public static String runningMavenHeading() {
        return text("== Running Maven ==", "== 开始执行 ==");
    }

    public static String runningGradleHeading() {
        return text("== Running Gradle ==", "== 开始执行 Gradle ==");
    }

    public static String mavenDeployFailed(int exitCode) {
        return text("Maven deploy failed with exit code " + exitCode, "Maven deploy 失败，退出码: " + exitCode);
    }

    public static String gradlePublishFailed(int exitCode) {
        return text("Gradle publish failed with exit code " + exitCode, "Gradle publish 失败，退出码: " + exitCode);
    }

    public static String dirtyWorktree() {
        return text(
            "Working tree has uncommitted changes. Use --allow-dirty true to skip this check when intentional.",
            "工作区存在未提交修改。若这是预期行为，可使用 --allow-dirty true 跳过检查。"
        );
    }

    public static String versionCheckHeading() {
        return text("== Version Check ==", "== 版本检查 ==");
    }

    public static String currentRevisionValue(String value) {
        return text("Current revision: ", "当前 revision: ") + value;
    }

    public static String publishModeCheckHeading() {
        return text("== Publish Mode Check ==", "== 发布模式检查 ==");
    }

    public static String snapshotCheckPassed() {
        return text("snapshot check passed", "snapshot 校验通过");
    }

    public static String plainSnapshotDescription() {
        return text(
            "plain snapshot: project version keeps the original -SNAPSHOT revision from pom.xml",
            "plain snapshot: 项目版本号保持 pom.xml 中的原始 -SNAPSHOT revision"
        );
    }

    public static String stampedSnapshotDescription() {
        return text(
            "stamped snapshot: project version appends the build stamp before publishing",
            "stamped snapshot: 项目版本号会追加 build stamp 后再发布"
        );
    }

    public static String repositoryVariableCheckHeading() {
        return text("== Repository Variable Check ==", "== 仓库变量检查 ==");
    }

    public static String credentialCheckHeading() {
        return text("== Credential Check ==", "== 凭据检查 ==");
    }

    public static String mavenSettingsGenerationPassed() {
        return text("Maven settings generation check passed", "Maven settings 生成校验通过");
    }

    public static String releaseNotesPreflightHeading() {
        return text("== Release Notes Preflight ==", "== Release Notes 预检查 ==");
    }

    public static String releaseNotesGenerationPassed() {
        return text("release notes generation check passed", "release notes 生成校验通过");
    }

    public static String localTagMissingSkipReleaseNotes(String tag) {
        return text(
            "Local tag " + tag + " was not found; skipping release notes generation check",
            "本地尚未找到 tag " + tag + "，跳过 release notes 生成检查"
        );
    }

    public static String preflightChecksPassed() {
        return text("Preflight checks passed", "发布前检查通过");
    }

    public static String taskMustBeNameWhenModuleSet(String task) {
        return text(
            "--task must be a task name, not a project path, when --module is set: " + task,
            "设置 --module 时，--task 必须是任务名，不能是项目路径: " + task
        );
    }

    public static String unsupportedGradleTask(String task) {
        return text("Unsupported Gradle task: " + task, "不支持的 Gradle task: " + task);
    }
}
