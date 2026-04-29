package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

final class I18n {
    private static final String RESOURCE_PREFIX = "io/github/sonofmagic/javachanges/messages_";
    private static final Map<ReleaseLanguage, Properties> CACHE = new EnumMap<ReleaseLanguage, Properties>(
        ReleaseLanguage.class
    );

    private I18n() {
    }

    static String message(String key, Object... args) {
        String pattern = lookup(ReleaseLanguageContext.get(), key);
        return format(pattern, args);
    }

    static String format(String pattern, Object... args) {
        if (args == null || args.length == 0) {
            return pattern;
        }
        StringBuilder builder = new StringBuilder(pattern.length() + 16);
        for (int index = 0; index < pattern.length(); index++) {
            char current = pattern.charAt(index);
            if (current != '{') {
                builder.append(current);
                continue;
            }
            int placeholderEnd = pattern.indexOf('}', index + 1);
            int argumentIndex = placeholderEnd < 0 ? -1 : parseArgumentIndex(pattern, index + 1, placeholderEnd);
            if (argumentIndex >= 0 && argumentIndex < args.length) {
                builder.append(String.valueOf(args[argumentIndex]));
                index = placeholderEnd;
            } else {
                builder.append(current);
            }
        }
        return builder.toString();
    }

    static Set<String> keys(ReleaseLanguage language) {
        return properties(language).stringPropertyNames();
    }

    static String pattern(ReleaseLanguage language, String key) {
        return lookup(language, key);
    }

    static Set<Integer> placeholderIndexes(String pattern) {
        Set<Integer> indexes = new TreeSet<Integer>();
        for (int index = 0; index < pattern.length(); index++) {
            if (pattern.charAt(index) != '{') {
                continue;
            }
            int placeholderEnd = pattern.indexOf('}', index + 1);
            int argumentIndex = placeholderEnd < 0 ? -1 : parseArgumentIndex(pattern, index + 1, placeholderEnd);
            if (argumentIndex >= 0) {
                indexes.add(argumentIndex);
                index = placeholderEnd;
            }
        }
        return indexes;
    }

    private static int parseArgumentIndex(String pattern, int start, int end) {
        if (start == end) {
            return -1;
        }
        int value = 0;
        for (int index = start; index < end; index++) {
            char current = pattern.charAt(index);
            if (current < '0' || current > '9') {
                return -1;
            }
            value = value * 10 + current - '0';
        }
        return value;
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
