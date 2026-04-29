package io.github.sonofmagic.javachanges.core;

public final class ReleaseLanguageContext {
    private static final ThreadLocal<ReleaseLanguage> CURRENT = new ThreadLocal<ReleaseLanguage>();

    private ReleaseLanguageContext() {
    }

    public static ReleaseLanguage get() {
        ReleaseLanguage language = CURRENT.get();
        return language == null ? ReleaseLanguage.EN : language;
    }

    public static void set(ReleaseLanguage language) {
        CURRENT.set(language == null ? ReleaseLanguage.EN : language);
    }

    public static void clear() {
        CURRENT.remove();
    }
}
