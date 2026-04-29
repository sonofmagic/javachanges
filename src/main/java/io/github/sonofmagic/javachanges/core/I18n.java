package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

final class I18n {
    private static final String RESOURCE_PREFIX = "io/github/sonofmagic/javachanges/messages_";
    private static final Map<ReleaseLanguage, Properties> CACHE = new EnumMap<ReleaseLanguage, Properties>(
        ReleaseLanguage.class
    );

    private I18n() {
    }

    static String message(String key, Object... args) {
        String pattern = lookup(ReleaseLanguageContext.get(), key);
        return args == null || args.length == 0
            ? pattern
            : new MessageFormat(pattern).format(args);
    }

    static Set<String> keys(ReleaseLanguage language) {
        return properties(language).stringPropertyNames();
    }

    private static String lookup(ReleaseLanguage language, String key) {
        String value = properties(language).getProperty(key);
        if (value != null) {
            return value;
        }
        if (language != ReleaseLanguage.EN) {
            value = properties(ReleaseLanguage.EN).getProperty(key);
            if (value != null) {
                return value;
            }
        }
        throw new IllegalStateException("Missing i18n message key: " + key);
    }

    private static synchronized Properties properties(ReleaseLanguage language) {
        Properties cached = CACHE.get(language);
        if (cached != null) {
            return cached;
        }
        Properties loaded = load(language);
        CACHE.put(language, loaded);
        return loaded;
    }

    private static Properties load(ReleaseLanguage language) {
        String resource = RESOURCE_PREFIX + language.resourceSuffix() + ".properties";
        InputStream inputStream = I18n.class.getClassLoader().getResourceAsStream(resource);
        if (inputStream == null) {
            throw new IllegalStateException("Missing i18n resource: " + resource);
        }
        Properties properties = new Properties();
        try {
            try {
                properties.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            } finally {
                inputStream.close();
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        return properties;
    }
}
