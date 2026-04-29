package io.github.sonofmagic.javachanges.core;

import java.util.Locale;

public enum ReleaseLanguage {
    EN("en"),
    ZH_CN("zh-CN");

    public static final String ENV_NAME = "JAVACHANGES_LANGUAGE";

    public final String id;

    ReleaseLanguage(String id) {
        this.id = id;
    }

    public static ReleaseLanguage parse(String value) {
        String normalized = ReleaseTextUtils.trimToNull(value);
        if (normalized == null) {
            return EN;
        }
        normalized = normalized.replace('_', '-').toLowerCase(Locale.ROOT);
        if ("en".equals(normalized) || "en-us".equals(normalized)) {
            return EN;
        }
        if ("zh".equals(normalized) || "zh-cn".equals(normalized) || "cn".equals(normalized)) {
            return ZH_CN;
        }
        throw new IllegalArgumentException(ReleaseMessages.unsupportedLanguage(value));
    }

    public static ReleaseLanguage fromEnvironment() {
        return parse(System.getenv(ENV_NAME));
    }

    public boolean isChinese() {
        return this == ZH_CN;
    }

    String resourceSuffix() {
        return id.replace('-', '_');
    }
}
