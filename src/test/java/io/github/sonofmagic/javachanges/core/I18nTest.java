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
        assertEquals("target module: all", ReleaseMessages.targetModule(null));
        assertEquals("target module: core", ReleaseMessages.targetModule("core"));
        ReleaseLanguageContext.set(ReleaseLanguage.ZH_CN);
        try {
            assertEquals("  使用 --force 可替换为默认模板。", ReleaseMessages.useForceToReplaceDefaultTemplate());
            assertEquals(" (使用 --config 写入默认模板)", ReleaseMessages.useConfigToWriteDefaultTemplate());
            assertEquals("== 版本检查 ==", ReleaseMessages.versionCheckHeading());
            assertEquals("Maven 命令: ./mvnw (wrapper)", ReleaseMessages.mavenCommandLabel("./mvnw", "wrapper"));
            assertEquals("目标模块: all", ReleaseMessages.targetModule(null));
            assertEquals("目标模块: core", ReleaseMessages.targetModule("core"));
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
        ReleaseLanguageContext.set(ReleaseLanguage.ZH_CN);
        try {
            assertEquals("git 命令执行失败: [push]", ReleaseMessages.gitCommandFailed("[push]"));
            assertEquals("已生成 GitLab CI template: .gitlab-ci.yml",
                ReleaseMessages.generatedGitlabCiTemplate(Paths.get(".gitlab-ci.yml")));
            assertEquals("GitLab API GET /projects 失败: denied",
                ReleaseMessages.gitlabApiFailed("GET", "/projects", "denied"));
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
        ReleaseLanguageContext.set(ReleaseLanguage.ZH_CN);
        try {
            assertEquals("在 GitLab 项目变量中创建 MAVEN_TOKEN，并勾选 protected",
                ReleaseMessages.createGitlabProtectedVariable("MAVEN_TOKEN"));
            assertEquals("release-plan.json 缺少字段 `releaseVersion`",
                ReleaseMessages.missingFieldInSource("releaseVersion", "release-plan.json"));
            assertEquals("重大变更", ReleaseMessages.releaseNotesSection("Breaking Changes"));
            assertEquals("其他", ReleaseMessages.releaseNotesSection("Unknown"));
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

        Set<String> templates = literalArguments(source, "I18n.template");
        templates.addAll(literalArguments(source, "I18n.templateLines"));
        assertTrue(!templates.isEmpty(), "ReleaseMessages should reference localized templates");
        for (String template : templates) {
            assertLocalizedTemplateExists(template, ReleaseLanguage.EN);
            assertLocalizedTemplateExists(template, ReleaseLanguage.ZH_CN);
        }
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
