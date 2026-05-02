package io.github.sonofmagic.javachanges.core;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class I18nTest {
    private static final String MESSAGE_RESOURCE_ROOT = "src/main/resources/io/github/sonofmagic/javachanges";
    private static final String TEMPLATE_RESOURCE_ROOT = "src/main/resources/io/github/sonofmagic/javachanges/templates";

    @Test
    void localizedResourceKeysStayInSync() {
        assertEquals(I18n.keys(ReleaseLanguage.EN), I18n.keys(ReleaseLanguage.ZH_CN));
    }

    @Test
    void localizedResourcePlaceholdersStayInSync() {
        for (String key : I18n.keys(ReleaseLanguage.EN)) {
            assertEquals(
                I18n.placeholderIndexes(I18n.pattern(ReleaseLanguage.EN, key)),
                I18n.placeholderIndexes(I18n.pattern(ReleaseLanguage.ZH_CN, key)),
                key
            );
        }
    }

    @Test
    void formatsUtf8MessagesWithArguments() {
        ReleaseLanguageContext.set(ReleaseLanguage.ZH_CN);
        try {
            assertEquals("已在 /repo 初始化 javachanges", ReleaseMessages.initialized(Paths.get("/repo")));
            assertEquals("不支持的语言: fr。请使用 en 或 zh-CN。", ReleaseMessages.unsupportedLanguage("fr"));
        } finally {
            ReleaseLanguageContext.clear();
        }
    }

    @Test
    void formatsSimplePlaceholdersWithoutMessageFormatEscapingRules() {
        assertEquals("Don't use example.env for repo", I18n.format("Don't use {0} for {1}", "example.env", "repo"));
        assertEquals("Keep {missing} and {3}", I18n.format("Keep {missing} and {3}", "value"));
    }

    @Test
    void extractsNumericPlaceholderIndexes() {
        assertEquals("[0, 2]", I18n.placeholderIndexes("Use {0}, {2}, {missing}, and {").toString());
    }

    @Test
    void loadsLocalizedUtf8Templates() {
        assertEquals("# Changesets\n", firstLineWithTrailingNewline(ReleaseMessages.changesetReadme()));
        ReleaseLanguageContext.set(ReleaseLanguage.ZH_CN);
        try {
            String readme = ReleaseMessages.changesetReadme();
            assertEquals("# Changesets\n", firstLineWithTrailingNewline(readme));
            assertTrue(readme.contains("这个目录保存待发布的 release notes。"));
            assertTrue(readme.endsWith("\n"));
        } finally {
            ReleaseLanguageContext.clear();
        }
    }

    @Test
    void loadsLocalizedTemplateLinesWithoutTrailingEmptyLine() {
        assertEquals("== GitHub CLI Login Guide ==", ReleaseMessages.githubAuthHelpLines()[0]);
        ReleaseLanguageContext.set(ReleaseLanguage.ZH_CN);
        try {
            String[] lines = ReleaseMessages.gitlabAuthHelpLines();
            assertEquals("== GitLab CLI 登录建议 ==", lines[0]);
            assertEquals("  https://docs.gitlab.com/cli/auth/login/", lines[lines.length - 1]);
        } finally {
            ReleaseLanguageContext.clear();
        }
    }

    @Test
    void loadsExternalizedReleaseEnvironmentMessages() {
        assertEquals("== GitHub CLI Commands ==", ReleaseMessages.githubCliCommandsHeading());
        assertEquals("gh CLI was not found", ReleaseMessages.cliNotFound("gh"));
        ReleaseLanguageContext.set(ReleaseLanguage.ZH_CN);
        try {
            assertEquals("== GitLab CLI 命令 ==", ReleaseMessages.gitlabCliCommandsHeading());
            assertEquals("未找到 glab CLI", ReleaseMessages.cliNotFound("glab"));
        } finally {
            ReleaseLanguageContext.clear();
        }
    }

    @Test
    void loadsExternalizedPublishMessages() {
        assertEquals("  Use --force to replace it with the default template.", ReleaseMessages.useForceToReplaceDefaultTemplate());
        assertEquals(" (use --config to write the default template)", ReleaseMessages.useConfigToWriteDefaultTemplate());
        assertEquals("== Dry Run Output ==", ReleaseMessages.dryRunOutputHeading());
        assertEquals("Maven command: ./mvnw (wrapper)", ReleaseMessages.mavenCommandLabel("./mvnw", "wrapper"));
        assertEquals("Gradle command: ./gradlew (wrapper)", ReleaseMessages.gradleCommandLabel("./gradlew", "wrapper"));
        assertEquals("publish version: 1.2.3", ReleaseMessages.publishVersion("1.2.3"));
        assertEquals("snapshot version mode: plain", ReleaseMessages.snapshotVersionMode("plain"));
        assertEquals("target module: all", ReleaseMessages.targetModule(null));
        assertEquals("target module: core", ReleaseMessages.targetModule("core"));
        assertEquals("Dry-run only.", ReleaseMessages.dryRunOnlyReason());
        ReleaseLanguageContext.set(ReleaseLanguage.ZH_CN);
        try {
            assertEquals("  使用 --force 可替换为默认模板。", ReleaseMessages.useForceToReplaceDefaultTemplate());
            assertEquals(" (使用 --config 写入默认模板)", ReleaseMessages.useConfigToWriteDefaultTemplate());
            assertEquals("== 版本检查 ==", ReleaseMessages.versionCheckHeading());
            assertEquals("Maven 命令: ./mvnw (wrapper)", ReleaseMessages.mavenCommandLabel("./mvnw", "wrapper"));
            assertEquals("Gradle 命令: ./gradlew (wrapper)", ReleaseMessages.gradleCommandLabel("./gradlew", "wrapper"));
            assertEquals("发布版本: 1.2.3", ReleaseMessages.publishVersion("1.2.3"));
            assertEquals("snapshot version mode: plain", ReleaseMessages.snapshotVersionMode("plain"));
            assertEquals("目标模块: all", ReleaseMessages.targetModule(null));
            assertEquals("目标模块: core", ReleaseMessages.targetModule("core"));
            assertEquals("仅 dry-run，未执行发布。", ReleaseMessages.dryRunOnlyReason());
        } finally {
            ReleaseLanguageContext.clear();
        }
    }

    @Test
    void loadsExternalizedCoreAndBuildMessages() {
        assertEquals("Missing field `releaseVersion` in .changesets/release-plan.json",
            ReleaseMessages.missingFieldIn("releaseVersion", Paths.get(".changesets/release-plan.json")));
        assertEquals("Cannot find <revision> in pom.xml", ReleaseMessages.cannotFindPomRevision(Paths.get("pom.xml")));
        assertEquals("snapshot build stamp cannot be empty", ReleaseMessages.emptySnapshotBuildStamp());
        ReleaseLanguageContext.set(ReleaseLanguage.ZH_CN);
        try {
            assertEquals("缺少字段 `releaseVersion`: .changesets/release-plan.json",
                ReleaseMessages.missingFieldIn("releaseVersion", Paths.get(".changesets/release-plan.json")));
            assertEquals("无法在 pom.xml 中找到 <revision>", ReleaseMessages.cannotFindPomRevision(Paths.get("pom.xml")));
            assertEquals("snapshot build stamp 不能为空", ReleaseMessages.emptySnapshotBuildStamp());
        } finally {
            ReleaseLanguageContext.clear();
        }
    }

    @Test
    void loadsExternalizedPlatformMessages() {
        assertEquals("git command failed: [push]", ReleaseMessages.gitCommandFailed("[push]"));
        assertEquals("Generated GitHub Actions workflow: .github/workflows/release.yml",
            ReleaseMessages.generatedGithubActionsWorkflow(Paths.get(".github/workflows/release.yml")));
        assertEquals("GitLab API GET /projects failed: denied",
            ReleaseMessages.gitlabApiFailed("GET", "/projects", "denied"));
        assertEquals("Public key ABC is visible on keys.example.com",
            ReleaseMessages.publicKeyVisibleOn("ABC", "keys.example.com"));
        assertEquals("Running javachanges command: status", ReleaseMessages.runningJavachangesCommand("status"));
        ReleaseLanguageContext.set(ReleaseLanguage.ZH_CN);
        try {
            assertEquals("git 命令执行失败: [push]", ReleaseMessages.gitCommandFailed("[push]"));
            assertEquals("已生成 GitLab CI template: .gitlab-ci.yml",
                ReleaseMessages.generatedGitlabCiTemplate(Paths.get(".gitlab-ci.yml")));
            assertEquals("GitLab API GET /projects 失败: denied",
                ReleaseMessages.gitlabApiFailed("GET", "/projects", "denied"));
            assertEquals("公钥 ABC 已在 keys.example.com 可见",
                ReleaseMessages.publicKeyVisibleOn("ABC", "keys.example.com"));
            assertEquals("执行 javachanges 命令: status", ReleaseMessages.runningJavachangesCommand("status"));
        } finally {
            ReleaseLanguageContext.clear();
        }
    }

    @Test
    void loadsExternalizedReleaseMetadataMessages() {
        assertEquals("Create GitLab project variable MAVEN_TOKEN and mark it protected",
            ReleaseMessages.createGitlabProtectedVariable("MAVEN_TOKEN"));
        assertEquals("Missing field tag in release manifest", ReleaseMessages.missingReleaseManifestField("tag"));
        assertEquals("Breaking Changes", ReleaseMessages.releaseNotesSection("Breaking Changes"));
        assertEquals("Other", ReleaseMessages.releaseNotesSection("Unknown"));
        assertEquals("Created GitHub pull request.", ReleaseMessages.createdGithubPullRequestReason());
        assertEquals("Tag already exists remotely: v1.2.0", ReleaseMessages.tagAlreadyExistsRemotelyReason("v1.2.0"));
        assertEquals("Release tag(s) already exist at the target commit: [v1.2.0]",
            ReleaseMessages.releaseTagsAlreadyAtTargetCommitReason("[v1.2.0]"));
        assertEquals("Created GitHub PR for chore(release): v1.2.0",
            ReleaseMessages.createdGithubPrFor("chore(release): v1.2.0"));
        assertEquals("Release tags: [v1.2.0]", ReleaseMessages.releaseTagsValue("[v1.2.0]"));
        assertEquals("== GitLab Protected Variables ==", ReleaseMessages.gitlabProtectedVariablesHeading());
        ReleaseLanguageContext.set(ReleaseLanguage.ZH_CN);
        try {
            assertEquals("在 GitLab 项目变量中创建 MAVEN_TOKEN，并勾选 protected",
                ReleaseMessages.createGitlabProtectedVariable("MAVEN_TOKEN"));
            assertEquals("release-plan.json 缺少字段 `releaseVersion`",
                ReleaseMessages.missingFieldInSource("releaseVersion", "release-plan.json"));
            assertEquals("重大变更", ReleaseMessages.releaseNotesSection("Breaking Changes"));
            assertEquals("其他", ReleaseMessages.releaseNotesSection("Unknown"));
            assertEquals("已创建 GitHub pull request。", ReleaseMessages.createdGithubPullRequestReason());
            assertEquals("远端 tag 已存在: v1.2.0", ReleaseMessages.tagAlreadyExistsRemotelyReason("v1.2.0"));
            assertEquals("release tag 已存在且指向目标 commit: [v1.2.0]",
                ReleaseMessages.releaseTagsAlreadyAtTargetCommitReason("[v1.2.0]"));
            assertEquals("已创建 GitHub PR: chore(release): v1.2.0",
                ReleaseMessages.createdGithubPrFor("chore(release): v1.2.0"));
            assertEquals("Release tags: [v1.2.0]", ReleaseMessages.releaseTagsValue("[v1.2.0]"));
            assertEquals("== GitLab Protected Branches ==", ReleaseMessages.gitlabProtectedBranchesHeading());
        } finally {
            ReleaseLanguageContext.clear();
        }
    }

    @Test
    void releaseMessagesOnlyReferencesExistingMessageKeysAndTemplates() throws Exception {
        String source = new String(
            Files.readAllBytes(repoRoot().resolve("src/main/java/io/github/sonofmagic/javachanges/core/ReleaseMessages.java")),
            StandardCharsets.UTF_8
        );

        Set<String> messageKeys = literalArguments(source, "message");
        assertTrue(!messageKeys.isEmpty(), "ReleaseMessages should use message bundle keys");
        for (String key : messageKeys) {
            assertTrue(I18n.keys(ReleaseLanguage.EN).contains(key), key);
            assertTrue(I18n.keys(ReleaseLanguage.ZH_CN).contains(key), key);
        }
        assertEquals(I18n.keys(ReleaseLanguage.EN), messageKeys);

        Set<String> templates = literalArguments(source, "I18n.template");
        templates.addAll(literalArguments(source, "I18n.templateLines"));
        assertTrue(!templates.isEmpty(), "ReleaseMessages should reference localized templates");
        for (String template : templates) {
            assertLocalizedTemplateExists(template, ReleaseLanguage.EN);
            assertLocalizedTemplateExists(template, ReleaseLanguage.ZH_CN);
        }
    }

    @Test
    void localizedResourceFilesDoNotContainDuplicateKeys() throws Exception {
        assertNoDuplicateMessageKeys(messageResource(ReleaseLanguage.EN));
        assertNoDuplicateMessageKeys(messageResource(ReleaseLanguage.ZH_CN));
    }

    @Test
    void localizedTemplatesStayInSync() throws Exception {
        Path templateRoot = repoRoot().resolve(TEMPLATE_RESOURCE_ROOT);
        Map<String, Set<String>> localesByTemplate = new TreeMap<String, Set<String>>();
        Stream<Path> paths = Files.list(templateRoot);
        try {
            paths.filter(Files::isRegularFile).forEach(path -> {
                TemplateName templateName = parseTemplateName(path.getFileName().toString());
                Set<String> locales = localesByTemplate.get(templateName.baseName);
                if (locales == null) {
                    locales = new TreeSet<String>();
                    localesByTemplate.put(templateName.baseName, locales);
                }
                locales.add(templateName.locale);
            });
        } finally {
            paths.close();
        }

        for (Map.Entry<String, Set<String>> entry : localesByTemplate.entrySet()) {
            assertEquals("[en, zh-CN]", entry.getValue().toString(), entry.getKey());
        }
    }

    private static String firstLineWithTrailingNewline(String value) {
        return value.substring(0, value.indexOf('\n') + 1);
    }

    private static Set<String> literalArguments(String source, String functionName) {
        Set<String> values = new TreeSet<String>();
        Matcher matcher = Pattern.compile(Pattern.quote(functionName) + "\\(\"([^\"]+)\"").matcher(source);
        while (matcher.find()) {
            values.add(matcher.group(1));
        }
        return values;
    }

    private static void assertLocalizedTemplateExists(String name, ReleaseLanguage language) {
        Path path = repoRoot().resolve(TEMPLATE_RESOURCE_ROOT).resolve(localizedTemplateName(name, language));
        assertTrue(Files.isRegularFile(path), path.toString());
    }

    private static String localizedTemplateName(String name, ReleaseLanguage language) {
        int extensionStart = name.lastIndexOf('.');
        if (extensionStart < 0) {
            return name + "." + language.id;
        }
        return name.substring(0, extensionStart) + "." + language.id + name.substring(extensionStart);
    }

    private static void assertNoDuplicateMessageKeys(Path path) throws Exception {
        Set<String> keys = new TreeSet<String>();
        Set<String> duplicates = new TreeSet<String>();
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            int separator = line.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            String key = line.substring(0, separator);
            if (!keys.add(key)) {
                duplicates.add(key);
            }
        }
        assertTrue(duplicates.isEmpty(), path + ": " + duplicates);
    }

    private static Path messageResource(ReleaseLanguage language) {
        return repoRoot().resolve(MESSAGE_RESOURCE_ROOT).resolve("messages_" + language.resourceSuffix() + ".properties");
    }

    private static TemplateName parseTemplateName(String fileName) {
        TemplateName english = parseTemplateName(fileName, ".en");
        if (english != null) {
            return english;
        }
        TemplateName chinese = parseTemplateName(fileName, ".zh-CN");
        if (chinese != null) {
            return chinese;
        }
        throw new IllegalArgumentException("Unsupported template locale filename: " + fileName);
    }

    private static TemplateName parseTemplateName(String fileName, String localeSegment) {
        int localeStart = fileName.lastIndexOf(localeSegment + ".");
        if (localeStart < 0) {
            return null;
        }
        return new TemplateName(
            fileName.substring(0, localeStart) + fileName.substring(localeStart + localeSegment.length()),
            localeSegment.substring(1)
        );
    }

    private static Path repoRoot() {
        return Paths.get("").toAbsolutePath().normalize();
    }

    private static final class TemplateName {
        private final String baseName;
        private final String locale;

        private TemplateName(String baseName, String locale) {
            this.baseName = baseName;
            this.locale = locale;
        }
    }
}
