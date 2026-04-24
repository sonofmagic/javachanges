package io.github.sonofmagic.javachanges.core.env;

import io.github.sonofmagic.javachanges.core.ReleaseUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class LoadedEnv {
    public final Path path;
    public final Map<String, String> values;

    private LoadedEnv(Path path, Map<String, String> values) {
        this.path = path;
        this.values = values;
    }

    public static LoadedEnv load(Path path) throws IOException {
        return parse(new String(Files.readAllBytes(path), StandardCharsets.UTF_8), path);
    }

    public static LoadedEnv parse(String content, Path path) {
        Map<String, String> values = new LinkedHashMap<String, String>();
        String[] lines = content.split("\\r?\\n");
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("export ")) {
                line = line.substring("export ".length()).trim();
            }
            int separator = line.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            String key = line.substring(0, separator).trim();
            String value = line.substring(separator + 1).trim();
            values.put(key, ReleaseUtils.stripWrappingQuotes(value));
        }
        return new LoadedEnv(path, values);
    }

    public EnvValue value(String key) {
        return EnvValue.of(values.get(key));
    }
}
