package io.github.sonofmagic.javachanges.core;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class I18nTest {
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
    void localizedTemplatesStayInSync() throws Exception {
        Path templateRoot = Paths.get("")
            .toAbsolutePath()
            .normalize()
            .resolve("src/main/resources/io/github/sonofmagic/javachanges/templates");
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

    private static final class TemplateName {
        private final String baseName;
        private final String locale;

        private TemplateName(String baseName, String locale) {
            this.baseName = baseName;
            this.locale = locale;
        }
    }
}
