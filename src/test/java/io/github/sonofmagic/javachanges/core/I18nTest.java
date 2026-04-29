package io.github.sonofmagic.javachanges.core;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
