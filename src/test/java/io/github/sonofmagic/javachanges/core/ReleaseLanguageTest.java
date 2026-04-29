package io.github.sonofmagic.javachanges.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReleaseLanguageTest {
    @Test
    void parseSupportsDocumentedAliases() {
        assertEquals(ReleaseLanguage.EN, ReleaseLanguage.parse(null));
        assertEquals(ReleaseLanguage.EN, ReleaseLanguage.parse("en"));
        assertEquals(ReleaseLanguage.EN, ReleaseLanguage.parse("en-US"));
        assertEquals(ReleaseLanguage.ZH_CN, ReleaseLanguage.parse("zh"));
        assertEquals(ReleaseLanguage.ZH_CN, ReleaseLanguage.parse("zh-CN"));
        assertEquals(ReleaseLanguage.ZH_CN, ReleaseLanguage.parse("zh_CN"));
        assertEquals(ReleaseLanguage.ZH_CN, ReleaseLanguage.parse("cn"));
    }

    @Test
    void invalidLanguageMessageUsesCurrentLanguage() {
        ReleaseLanguageContext.set(ReleaseLanguage.ZH_CN);
        try {
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ReleaseLanguage.parse("fr")
            );

            assertEquals("不支持的语言: fr。请使用 en 或 zh-CN。", exception.getMessage());
        } finally {
            ReleaseLanguageContext.clear();
        }
    }
}
