package io.github.sonofmagic.javachanges;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ReleaseUtils {
    static final String CHANGESETS_DIR = ".changesets";
    static final String CHANGESETS_README = "README.md";
    static final String RELEASE_PLAN_JSON = "release-plan.json";
    static final String RELEASE_PLAN_MD = "release-plan.md";
    static final List<String> CHANGELOG_TYPE_ORDER = Collections.unmodifiableList(Arrays.asList(
        "breaking",
        "feat",
        "fix",
        "perf",
        "refactor",
        "build",
        "docs",
        "test",
        "ci",
        "chore",
        "other"
    ));

    private ReleaseUtils() {
    }

    static Platform platformOption(Map<String, String> options) {
        return Platform.parse(options.get("platform"), Platform.ALL);
    }

    static Map<String, String> parseOptions(String[] args, int fromIndex) {
        Map<String, String> options = new LinkedHashMap<String, String>();
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

    static byte[] readAllBytes(InputStream inputStream) throws IOException {
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = inputStream.read(buffer)) >= 0) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
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

    static List<String> detectKnownModules(Path repoRoot) {
        try {
            Path pomPath = repoRoot.resolve("pom.xml");
            if (!java.nio.file.Files.exists(pomPath)) {
                return Collections.emptyList();
            }

            String pomContent = new String(java.nio.file.Files.readAllBytes(pomPath), StandardCharsets.UTF_8);
            Matcher modulesMatcher = Pattern.compile("(?s)<modules>(.*?)</modules>").matcher(pomContent);
            if (!modulesMatcher.find()) {
                String artifactId = detectRootArtifactId(pomContent);
                if (artifactId == null) {
                    return Collections.emptyList();
                }
                return Collections.singletonList(artifactId);
            }

            List<String> modules = new ArrayList<String>();
            Matcher moduleMatcher = Pattern.compile("<module>([^<]+)</module>").matcher(modulesMatcher.group(1));
            while (moduleMatcher.find()) {
                String modulePath = trimToNull(moduleMatcher.group(1));
                if (modulePath == null) {
                    continue;
                }

                Path modulePom = repoRoot.resolve(modulePath).resolve("pom.xml");
                if (!java.nio.file.Files.exists(modulePom)) {
                    continue;
                }

                String modulePomContent = new String(java.nio.file.Files.readAllBytes(modulePom), StandardCharsets.UTF_8);
                String withoutParent = modulePomContent.replaceFirst("(?s)<parent>.*?</parent>", "");
                Matcher artifactMatcher = Pattern.compile("<artifactId>([^<]+)</artifactId>").matcher(withoutParent);
                if (artifactMatcher.find()) {
                    modules.add(artifactMatcher.group(1).trim());
                } else {
                    modules.add(java.nio.file.Paths.get(modulePath).getFileName().toString());
                }
            }

            return modules;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to detect Maven modules from " + repoRoot, exception);
        }
    }

    private static String detectRootArtifactId(String pomContent) {
        String withoutParent = pomContent.replaceFirst("(?s)<parent>.*?</parent>", "");
        Matcher artifactMatcher = Pattern.compile("<artifactId>([^<]+)</artifactId>").matcher(withoutParent);
        if (!artifactMatcher.find()) {
            return null;
        }
        return trimToNull(artifactMatcher.group(1));
    }

    static void assertKnownModule(Path repoRoot, String module) {
        List<String> knownModules = detectKnownModules(repoRoot);
        if (!knownModules.contains(module)) {
            throw new IllegalArgumentException("Unknown module: " + module + ", allowed: " + knownModules);
        }
    }

    static String moduleSelectorArgs(Path repoRoot, String module) {
        if (module == null || "all".equals(module)) {
            return "";
        }
        assertKnownModule(repoRoot, module);
        return "-pl :" + module + " -am";
    }

    static String releaseVersionFromTag(String tag) {
        if (tag.startsWith("v")) {
            return tag.substring(1);
        }
        int separator = tag.lastIndexOf("/v");
        if (separator > 0) {
            return tag.substring(separator + 2);
        }
        throw new IllegalArgumentException("Unsupported release tag: " + tag);
    }

    static String releaseModuleFromTag(String tag) {
        if (tag.startsWith("v")) {
            return null;
        }
        int separator = tag.lastIndexOf("/v");
        if (separator > 0) {
            return tag.substring(0, separator);
        }
        throw new IllegalArgumentException("Unsupported release tag: " + tag);
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

    static String gitTextAllowEmpty(Path repoRoot, String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<String>();
        command.add("git");
        command.addAll(Arrays.asList(args));
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(repoRoot.toFile());
        Process process = builder.start();
        byte[] stdout = readAllBytes(process.getInputStream());
        byte[] stderr = readAllBytes(process.getErrorStream());
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            String error = new String(stderr, StandardCharsets.UTF_8).trim();
            if (!error.isEmpty()) {
                throw new IllegalStateException(error);
            }
            throw new IllegalStateException("git command failed");
        }
        return new String(stdout, StandardCharsets.UTF_8);
    }

    static int runCommand(List<String> command, Path workingDirectory) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workingDirectory.toFile());
        builder.inheritIO();
        Process process = builder.start();
        return process.waitFor();
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

    static String mavenWrapperPath() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win") ? "mvnw.cmd" : "./mvnw";
    }

    static Map<String, Map<String, String>> parseFlatJsonObjects(String json) {
        Map<String, Map<String, String>> result = new HashMap<String, Map<String, String>>();
        Matcher matcher = Pattern.compile("\\{([^{}]*)\\}").matcher(json);
        while (matcher.find()) {
            String objectText = matcher.group(1);
            Map<String, String> fields = new HashMap<String, String>();
            Matcher fieldMatcher = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(\"((?:\\\\.|[^\"])*)\"|null)").matcher(objectText);
            while (fieldMatcher.find()) {
                String key = fieldMatcher.group(1);
                String rawValue = fieldMatcher.group(2);
                String value = "null".equals(rawValue) ? "" : jsonUnescape(fieldMatcher.group(3));
                fields.put(key, value);
            }
            String name = fields.containsKey("name") ? fields.get("name") : fields.get("key");
            if (name != null) {
                result.put(name, fields);
            }
        }
        return result;
    }

    static List<String> parseModules(Path repoRoot, String rawModules) {
        List<String> knownModules = detectKnownModules(repoRoot);
        String trimmed = rawModules.trim();
        if ("all".equalsIgnoreCase(trimmed)) {
            return new ArrayList<String>(knownModules);
        }
        String[] split = trimmed.split(",");
        Set<String> modules = new LinkedHashSet<String>();
        for (String part : split) {
            String module = trimToNull(part);
            if (module == null) {
                continue;
            }
            if (!knownModules.contains(module)) {
                throw new IllegalArgumentException("Unknown module: " + module + ", allowed: " + knownModules);
            }
            modules.add(module);
        }
        if (modules.isEmpty()) {
            throw new IllegalArgumentException("At least one module is required");
        }
        return new ArrayList<String>(modules);
    }

    static String normalizeType(String rawType) {
        String normalized = rawType.trim().toLowerCase(Locale.ROOT);
        if ("breaking".equals(normalized)) {
            return "breaking";
        }
        List<String> supported = Arrays.asList("feat", "fix", "docs", "build", "ci", "test", "refactor", "perf", "chore", "other");
        if (!supported.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported change type: " + rawType);
        }
        return normalized;
    }

    static String joinModules(List<String> modules) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < modules.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(modules.get(i));
        }
        return builder.toString();
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

    static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    static String jsonUnescape(String value) {
        return value.replace("\\\"", "\"").replace("\\\\", "\\");
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

    static void closeQuietly(InputStream inputStream) {
        try {
            inputStream.close();
        } catch (IOException ignored) {
        }
    }
}
