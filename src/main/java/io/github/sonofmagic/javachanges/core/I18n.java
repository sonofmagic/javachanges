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
    private static final String TEMPLATE_PREFIX = "io/github/sonofmagic/javachanges/templates/";
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

    static String template(String name) {
        return readResource(TEMPLATE_PREFIX + localizedTemplateName(name, ReleaseLanguageContext.get()));
    }

    static String[] templateLines(String name) {
        String content = template(name);
        String[] lines = content.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        if (lines.length > 0 && lines[lines.length - 1].isEmpty()) {
            String[] trimmed = new String[lines.length - 1];
            System.arraycopy(lines, 0, trimmed, 0, trimmed.length);
            return trimmed;
        }
        return lines;
    }

    private static String localizedTemplateName(String name, ReleaseLanguage language) {
        int extensionStart = name.lastIndexOf('.');
        if (extensionStart < 0) {
            return name + "." + language.id;
        }
        return name.substring(0, extensionStart) + "." + language.id + name.substring(extensionStart);
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
        InputStream inputStream = openResource(resource);
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

    private static String readResource(String resource) {
        InputStream inputStream = openResource(resource);
        StringBuilder content = new StringBuilder();
        char[] buffer = new char[4096];
        try {
            try {
                InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                int read;
                while ((read = reader.read(buffer)) >= 0) {
                    content.append(buffer, 0, read);
                }
            } finally {
                inputStream.close();
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        return content.toString();
    }

    private static InputStream openResource(String resource) {
        InputStream inputStream = I18n.class.getClassLoader().getResourceAsStream(resource);
        if (inputStream == null) {
            throw new IllegalStateException("Missing i18n resource: " + resource);
        }
        return inputStream;
    }
}
