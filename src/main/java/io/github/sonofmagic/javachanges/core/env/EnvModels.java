package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.isPlaceholderValue;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.isRequiredName;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.stripWrappingQuotes;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.trimToNull;

final class EnvEntry {
    final String name;
    final boolean secret;
    final boolean protectedValue;
    final boolean required;

    EnvEntry(String name, boolean secret, boolean protectedValue) {
        this(name, secret, protectedValue, isRequiredName(name));
    }

    EnvEntry(String name, boolean secret, boolean protectedValue, boolean required) {
        this.name = name;
        this.secret = secret;
        this.protectedValue = protectedValue;
        this.required = required;
    }
}

final class EnvValue {
    final String raw;
    final boolean missing;
    final boolean placeholder;

    private EnvValue(String raw, boolean missing, boolean placeholder) {
        this.raw = raw;
        this.missing = missing;
        this.placeholder = placeholder;
    }

    static EnvValue of(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return missing();
        }
        if (isPlaceholderValue(normalized)) {
            return new EnvValue(normalized, false, true);
        }
        return new EnvValue(normalized, false, false);
    }

    static EnvValue missing() {
        return new EnvValue("", true, false);
    }

    boolean isReal() {
        return !missing && !placeholder;
    }

    String statusOrRaw() {
        if (missing) {
            return "MISSING";
        }
        if (placeholder) {
            return "PLACEHOLDER";
        }
        return raw;
    }

    String renderMasked(boolean showSecrets) {
        if (missing) {
            return "MISSING";
        }
        if (placeholder) {
            return "PLACEHOLDER";
        }
        if (showSecrets) {
            return raw;
        }
        if (raw.length() <= 4) {
            return "****";
        }
        return raw.substring(0, 2) + "****" + raw.substring(raw.length() - 2);
    }
}

final class LoadedEnv {
    final Path path;
    final Map<String, String> values;

    private LoadedEnv(Path path, Map<String, String> values) {
        this.path = path;
        this.values = values;
    }

    static LoadedEnv load(Path path) throws IOException {
        return parse(new String(Files.readAllBytes(path), StandardCharsets.UTF_8), path);
    }

    static LoadedEnv parse(String content, Path path) {
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
            values.put(key, stripWrappingQuotes(value));
        }
        return new LoadedEnv(path, values);
    }

    EnvValue value(String key) {
        return EnvValue.of(values.get(key));
    }
}

final class CommandResult {
    final int exitCode;
    private final byte[] stdout;
    private final byte[] stderr;

    CommandResult(int exitCode, byte[] stdout, byte[] stderr) {
        this.exitCode = exitCode;
        this.stdout = stdout;
        this.stderr = stderr;
    }

    String stdoutText() {
        return new String(stdout, StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unused")
    String stderrText() {
        return new String(stderr, StandardCharsets.UTF_8);
    }
}

final class AuditOutcome {
    final String message;
    private final boolean failure;

    private AuditOutcome(String message, boolean failure) {
        this.message = message;
        this.failure = failure;
    }

    static AuditOutcome success(String message) {
        return new AuditOutcome(message, false);
    }

    static AuditOutcome failure(String message) {
        return new AuditOutcome(message, true);
    }

    boolean isFailure() {
        return failure;
    }
}
