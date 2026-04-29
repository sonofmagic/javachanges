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

    public static String text(String english, String chinese) {
        return zh() ? chinese : english;
    }

    public static String initialized(Path repoRoot) {
        return text("Initialized javachanges in " + repoRoot, "已在 " + repoRoot + " 初始化 javachanges");
    }

    public static String pathAction(String action) {
        if (!zh()) {
            return action;
        }
        if ("Created".equals(action)) {
            return "已创建";
        }
        if ("Replaced".equals(action)) {
            return "已替换";
        }
        if ("Kept".equals(action)) {
            return "已保留";
        }
        if ("Skipped".equals(action)) {
            return "已跳过";
        }
        return action;
    }

    public static String nextSteps() {
        return text("Next steps:", "下一步:");
    }

    public static String describeChangePlaceholder() {
        return text("describe the change", "描述这次变更");
    }

    public static String releaseLevelHeading(ReleaseLevel level) {
        if (level == ReleaseLevel.MAJOR) {
            return text("Major Changes", "重大变更");
        }
        if (level == ReleaseLevel.MINOR) {
            return text("Minor Changes", "功能变更");
        }
        return text("Patch Changes", "修复变更");
    }

    public static String changelogPackagesLabel() {
        return text("packages", "包");
    }

    public static String noUserFacingChanges() {
        return text("No user-facing changes were recorded for this release.", "本次发布没有记录面向用户的变更。");
    }

    public static String releasePlanTitle() {
        return text("Release Plan", "发布计划") + " 🚀";
    }

    public static String releasePlanIntro() {
        return text(
            "Generated from `.changesets/*.md`. Review the plan, then merge when the release looks right.",
            "根据 `.changesets/*.md` 自动生成。请确认发布计划无误后再合并。"
        );
    }

    public static String fieldHeader() {
        return text("Field", "字段");
    }

    public static String valueHeader() {
        return text("Value", "值");
    }

    public static String releaseTypeField() {
        return text("Release type", "发布类型");
    }

    public static String affectedPackagesField() {
        return text("Affected packages", "影响包");
    }

    public static String releaseVersionField() {
        return text("Release version", "发布版本");
    }

    public static String tagStrategyField() {
        return text("Tag strategy", "Tag 策略");
    }

    public static String plannedTagsField() {
        return text("Planned tags", "计划 Tag");
    }

    public static String nextSnapshotField() {
        return text("Next snapshot", "下一个快照版本");
    }

    public static String includedChangesetsTitle() {
        return text("Included Changesets", "包含的 Changeset") + " 📝";
    }

    public static String releaseLabel() {
        return text("Release", "发布");
    }

    public static String packagesLabel() {
        return text("Packages", "包");
    }

    public static String typeLabel() {
        return text("Type", "类型");
    }

    public static String notesLabel() {
        return text("Notes", "备注");
    }

    public static String whatHappensNextTitle() {
        return text("What happens next", "接下来会发生什么") + " ✅";
    }

    public static String mergeTriggersTagPush() {
        return text(
            "Merging this PR triggers the automatic tag push.",
            "合并这个 PR 后会触发自动推送 tag。"
        );
    }

    public static String workflowsReuseMetadata() {
        return text(
            "Existing release workflows reuse the generated release metadata.",
            "现有发布 workflow 会复用生成的发布元数据。"
        );
    }

    public static String repository() {
        return text("Repository", "仓库");
    }

    public static String currentRevision() {
        return text("Current revision", "当前版本");
    }

    public static String latestWholeRepoTag() {
        return text("Latest whole-repo tag", "最新全仓库 tag");
    }

    public static String none() {
        return text("none", "无");
    }

    public static String pendingChangesets() {
        return text("Pending changesets", "待发布 changeset");
    }

    public static String releasePlan() {
        return text("Release plan", "发布计划");
    }

    public static String changesets() {
        return text("Changesets", "Changesets");
    }

    public static String releaseType() {
        return text("Release type", "发布类型");
    }

    public static String affectedPackages() {
        return text("Affected packages", "影响包");
    }

    public static String releaseVersion() {
        return text("Release version", "发布版本");
    }

    public static String nextSnapshot() {
        return text("Next snapshot", "下一个快照版本");
    }

    public static String createdChangeset(Path relativePath) {
        return text("Created changeset: " + relativePath, "已创建 changeset: " + relativePath);
    }

    public static String releaseLevel() {
        return text("Release level", "发布级别");
    }

    public static String noPendingChangesets() {
        return text("No pending changesets.", "没有待发布 changeset。");
    }

    public static String noPendingChangesetsToApply() {
        return text("No pending changesets to apply.", "没有可应用的待发布 changeset。");
    }

    public static String appliedReleasePlan(String version) {
        return text("Applied release plan for v" + version, "已应用 v" + version + " 的发布计划");
    }

    public static String createdOne() {
        return text("Create one:", "创建一个:");
    }

    public static String reviewPlan() {
        return text("Review the plan:", "检查发布计划:");
    }

    public static String thenReviewPlan() {
        return text("Then review the plan:", "然后检查发布计划:");
    }

    public static String applyLocally() {
        return text("Apply it locally:", "在本地应用:");
    }

    public static String openGithubPr() {
        return text("Or open an automated GitHub release PR:", "或者打开自动化 GitHub release PR:");
    }

    public static String openGitlabMr() {
        return text("Or open an automated GitLab release MR:", "或者打开自动化 GitLab release MR:");
    }

    public static String nextStepFor(Path repoRoot) {
        return text("Next step for " + repoRoot + ":", repoRoot + " 的下一步:");
    }

    public static String plannedRelease() {
        return text("Planned release", "计划发布版本");
    }

    public static String unsupportedLanguage(String value) {
        return "Unsupported language: " + value + ". Use en or zh-CN.";
    }

    public static String unsupportedOutputFormat(String value) {
        return text(
            "Unsupported output format: " + value + ". Use text or json.",
            "不支持的输出格式: " + value + "，可选值: text, json"
        );
    }

    public static String unsupportedPlatform(String value) {
        return text(
            "Unsupported platform: " + value + ". Use github, gitlab, or all.",
            "不支持的平台: " + value + "，可选值: github, gitlab, all"
        );
    }

    public static String unsupportedReleaseLevel(String value) {
        return text(
            "Unsupported release level: " + value + ". Use " + ReleaseLevel.ALLOWED_VALUES + ".",
            "不支持的发布级别: " + value + "。请使用 patch、minor 或 major。"
        );
    }

    public static String unknownArgument(String value) {
        return text("Unknown argument: " + value, "未知参数: " + value);
    }

    public static String missingRequiredOption(String name) {
        return text("Missing required option: --" + name, "缺少必填选项: --" + name);
    }

    public static String missingEnv(String name) {
        return text("Missing environment variable: " + name, "缺少环境变量: " + name);
    }

    public static String unknownModule(Path repoRoot, String module, List<String> knownModules) {
        return text(
            "Unknown module: " + module + ", allowed: " + knownModules
                + ". Run `javachanges modules --directory " + repoRoot + "` to list detected modules.",
            "未知模块: " + module + "，可选模块: " + knownModules
                + "。执行 `javachanges modules --directory " + repoRoot + "` 查看检测到的模块。"
        );
    }

    public static String unsupportedReleaseTag(String tag) {
        return text("Unsupported release tag: " + tag, "不支持的发布 tag: " + tag);
    }

    public static String atLeastOneModuleRequired() {
        return text("At least one module is required", "至少需要指定一个模块");
    }

    public static String unsupportedChangeType(String type) {
        return text("Unsupported change type: " + type, "不支持的变更类型: " + type);
    }

    public static String invalidChangesetFrontmatter(Path path) {
        return text("Invalid changeset frontmatter: " + path, "无效的 changeset frontmatter: " + path);
    }

    public static String invalidChangesetLine(Path path, String line) {
        return text("Invalid changeset line in " + path + ": " + line,
            "无效的 changeset 行 " + path + ": " + line);
    }

    public static String missingPackageReleaseEntries(Path path) {
        return text("Missing package release entries in " + path, "缺少包发布条目: " + path);
    }

    public static String missingKeyIn(String key, Path path) {
        return text("Missing `" + key + "` in " + path, "缺少 `" + key + "`: " + path);
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
}
