package io.github.sonofmagic.javachanges.core;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class ReleaseTextUtils {
    private ReleaseTextUtils() {
    }

    static Platform platformOption(Map<String, String> options) {
        return Platform.parse(options.get("platform"), Platform.ALL);
    }

    static Map<String, String> parseOptions(String[] args, int fromIndex) {
        Map<String, String> options = new java.util.LinkedHashMap<String, String>();
        for (int i = fromIndex; i < args.length; i++) {
            String current = args[i];
            if (!current.startsWith("--")) {
                throw new IllegalArgumentException("Unknown argument: " + current);
            }
            String key = current.substring(2);
            String value = "true";
            if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                value = args[++i];
            }
            options.put(key, value);
        }
        return options;
    }

    static ReleaseLevel maxReleaseLevel(List<Changeset> changesets) {
        ReleaseLevel result = ReleaseLevel.PATCH;
        for (Changeset changeset : changesets) {
            if (changeset.release.weight > result.weight) {
                result = changeset.release;
            }
        }
        return result;
    }

    static String stripSnapshot(String version) {
        if (version.endsWith("-SNAPSHOT")) {
            return version.substring(0, version.length() - "-SNAPSHOT".length());
        }
        return version;
    }

    static String requiredOption(Map<String, String> options, String name) {
        String value = trimToNull(options.get(name));
        if (value == null) {
            throw new IllegalArgumentException("Missing required option: --" + name);
        }
        return value;
    }

    static boolean isTrue(String value) {
        return "true".equalsIgnoreCase(trimToNull(value));
    }

    static String requireEnv(String name) {
        String value = trimToNull(System.getenv(name));
        if (value == null) {
            throw new IllegalStateException("缺少环境变量: " + name);
        }
        return value;
    }

    static String firstNonBlank(String first, String second) {
        String candidate = trimToNull(first);
        return candidate != null ? candidate : trimToNull(second);
    }

    static boolean isBlank(String value) {
        return trimToNull(value) == null;
    }

    static String xmlEscape(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }

    static String renderCommand(List<String> command) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < command.size(); i++) {
            if (i > 0) {
                builder.append(' ');
            }
            builder.append(shellEscape(command.get(i)));
        }
        return builder.toString();
    }

    static String shellEscape(String value) {
        if (value.matches("[A-Za-z0-9_./:=+-]+")) {
            return value;
        }
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }

    static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    static String trimTrailingBlankLines(List<String> lines) {
        int end = lines.size();
        while (end > 0 && lines.get(end - 1).trim().isEmpty()) {
            end--;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < end; i++) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(lines.get(i));
        }
        return builder.toString().trim();
    }

    static String changeTypeHeading(String type) {
        if ("breaking".equals(type)) {
            return "Breaking Changes";
        }
        if ("feat".equals(type)) {
            return "Features";
        }
        if ("fix".equals(type)) {
            return "Fixes";
        }
        if ("perf".equals(type)) {
            return "Performance";
        }
        if ("refactor".equals(type)) {
            return "Refactoring";
        }
        if ("build".equals(type)) {
            return "Build";
        }
        if ("docs".equals(type)) {
            return "Documentation";
        }
        if ("test".equals(type)) {
            return "Tests";
        }
        if ("ci".equals(type)) {
            return "CI";
        }
        if ("chore".equals(type)) {
            return "Chores";
        }
        return "Other";
    }

    static String releaseLevelHeading(ReleaseLevel level) {
        if (level == ReleaseLevel.MAJOR) {
            return "Major Changes";
        }
        if (level == ReleaseLevel.MINOR) {
            return "Minor Changes";
        }
        return "Patch Changes";
    }

    static String renderVisibleType(String type) {
        return "other".equals(type) ? "" : type;
    }

    static String firstBodyLine(String body) {
        String[] lines = body.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                return trimmed;
            }
        }
        return "";
    }

    static boolean isPlaceholderValue(String value) {
        return "replace-me".equals(value) || value.startsWith("https://nexus.example.com/");
    }

    static boolean isRequiredName(String name) {
        return !Arrays.asList(
            "MAVEN_RELEASE_REPOSITORY_ID",
            "MAVEN_SNAPSHOT_REPOSITORY_ID",
            "MAVEN_RELEASE_REPOSITORY_USERNAME",
            "MAVEN_RELEASE_REPOSITORY_PASSWORD",
            "MAVEN_SNAPSHOT_REPOSITORY_USERNAME",
            "MAVEN_SNAPSHOT_REPOSITORY_PASSWORD",
            "GITLAB_RELEASE_TOKEN"
        ).contains(name);
    }

    static String stripWrappingQuotes(String value) {
        if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\""))
            || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    static String padRight(String value, int width) {
        StringBuilder builder = new StringBuilder(value);
        while (builder.length() < width) {
            builder.append(' ');
        }
        return builder.toString();
    }
}
