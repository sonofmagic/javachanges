package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class GradleModelSupport {
    private static final String VERSION_KEY = "version";
    private static final String REVISION_KEY = "revision";
    private static final Pattern PROPERTY_LINE = Pattern.compile("^(\\s*)([^#!:=\\s]+)(\\s*[:=]\\s*|\\s+)(.*)$");
    private static final Pattern ROOT_PROJECT_NAME = Pattern.compile(
        "(?m)^\\s*rootProject\\.name\\s*=\\s*['\"]([^'\"]+)['\"]"
    );
    private static final Pattern PROJECT_NAME_ASSIGNMENT = Pattern.compile(
        "(?m)project\\s*\\(\\s*['\"]([^'\"]+)['\"]\\s*\\)\\s*\\.\\s*name\\s*=\\s*['\"]([^'\"]+)['\"]"
    );
    private static final Pattern INCLUDE_TOKEN = Pattern.compile("(?m)^\\s*include\\b");
    private static final Pattern QUOTED_VALUE = Pattern.compile("['\"]([^'\"]+)['\"]");

    private GradleModelSupport() {
    }

    static Path settingsFile(Path repoRoot) {
        Path groovy = repoRoot.resolve("settings.gradle");
        if (Files.exists(groovy)) {
            return groovy;
        }
        Path kotlin = repoRoot.resolve("settings.gradle.kts");
        if (Files.exists(kotlin)) {
            return kotlin;
        }
        return null;
    }

    static Path buildFile(Path repoRoot) {
        Path groovy = repoRoot.resolve("build.gradle");
        if (Files.exists(groovy)) {
            return groovy;
        }
        Path kotlin = repoRoot.resolve("build.gradle.kts");
        if (Files.exists(kotlin)) {
            return kotlin;
        }
        return null;
    }

    static String readRevision(Path gradlePropertiesPath) throws IOException {
        VersionProperty property = findVersionProperty(gradlePropertiesPath);
        if (property == null) {
            throw new IllegalStateException(ReleaseMessages.cannotFindGradleVersion(gradlePropertiesPath));
        }
        return property.value;
    }

    static void writeRevision(Path gradlePropertiesPath, String revision) throws IOException {
        List<String> lines = Files.readAllLines(gradlePropertiesPath, StandardCharsets.UTF_8);
        int index = preferredVersionPropertyLine(lines);
        if (index < 0) {
            throw new IllegalStateException(ReleaseMessages.cannotFindGradleVersion(gradlePropertiesPath));
        }
        Matcher matcher = PROPERTY_LINE.matcher(lines.get(index));
        if (!matcher.matches()) {
            throw new IllegalStateException(ReleaseMessages.cannotParseGradleVersion(gradlePropertiesPath));
        }
        lines.set(index, matcher.group(1) + matcher.group(2) + matcher.group(3) + revision);
        Files.write(gradlePropertiesPath, lines, StandardCharsets.UTF_8);
    }

    static List<String> detectModules(Path repoRoot) throws IOException {
        Path settingsPath = settingsFile(repoRoot);
        if (settingsPath == null) {
            return buildFile(repoRoot) == null
                ? Collections.<String>emptyList()
                : Collections.singletonList(repoRoot.getFileName().toString());
        }
        String settings = new String(Files.readAllBytes(settingsPath), StandardCharsets.UTF_8);
        String uncommentedSettings = stripComments(settings);
        Map<String, String> projectNames = projectNameAssignments(uncommentedSettings);
        Set<String> modules = new LinkedHashSet<String>();
        for (String arguments : includeArguments(uncommentedSettings)) {
            Matcher valueMatcher = QUOTED_VALUE.matcher(arguments);
            while (valueMatcher.find()) {
                String module = normalizeProjectPath(valueMatcher.group(1));
                if (module != null) {
                    modules.add(projectNames.containsKey(module) ? projectNames.get(module) : module);
                }
            }
        }
        if (!modules.isEmpty()) {
            return new ArrayList<String>(modules);
        }
        String rootProjectName = readRootProjectName(uncommentedSettings);
        if (rootProjectName != null) {
            return Collections.singletonList(rootProjectName);
        }
        return Collections.singletonList(repoRoot.getFileName().toString());
    }

    private static VersionProperty findVersionProperty(Path gradlePropertiesPath) throws IOException {
        List<String> lines = Files.readAllLines(gradlePropertiesPath, StandardCharsets.UTF_8);
        int versionLine = -1;
        int revisionLine = -1;
        String versionValue = null;
        String revisionValue = null;
        for (int index = 0; index < lines.size(); index++) {
            Matcher matcher = PROPERTY_LINE.matcher(lines.get(index));
            if (!matcher.matches()) {
                continue;
            }
            String key = matcher.group(2);
            if (VERSION_KEY.equals(key)) {
                versionLine = index;
                versionValue = ReleaseTextUtils.trimToNull(matcher.group(4));
            } else if (REVISION_KEY.equals(key)) {
                revisionLine = index;
                revisionValue = ReleaseTextUtils.trimToNull(matcher.group(4));
            }
        }
        if (versionLine >= 0 && versionValue != null) {
            return new VersionProperty(versionValue);
        }
        if (revisionLine >= 0 && revisionValue != null) {
            return new VersionProperty(revisionValue);
        }
        return null;
    }

    private static int preferredVersionPropertyLine(List<String> lines) {
        int revisionLine = -1;
        for (int index = 0; index < lines.size(); index++) {
            Matcher matcher = PROPERTY_LINE.matcher(lines.get(index));
            if (!matcher.matches()) {
                continue;
            }
            String key = matcher.group(2);
            String value = ReleaseTextUtils.trimToNull(matcher.group(4));
            if (VERSION_KEY.equals(key) && value != null) {
                return index;
            }
            if (REVISION_KEY.equals(key) && value != null) {
                revisionLine = index;
            }
        }
        return revisionLine;
    }

    private static String readRootProjectName(String settings) {
        Matcher matcher = ROOT_PROJECT_NAME.matcher(settings);
        if (!matcher.find()) {
            return null;
        }
        return ReleaseTextUtils.trimToNull(matcher.group(1));
    }

    private static Map<String, String> projectNameAssignments(String settings) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        Matcher matcher = PROJECT_NAME_ASSIGNMENT.matcher(settings);
        while (matcher.find()) {
            String projectPath = normalizeProjectPath(matcher.group(1));
            String projectName = ReleaseTextUtils.trimToNull(matcher.group(2));
            if (projectPath != null && projectName != null) {
                result.put(projectPath, projectName);
            }
        }
        return result;
    }

    private static String normalizeProjectPath(String projectPath) {
        String value = ReleaseTextUtils.trimToNull(projectPath);
        if (value == null) {
            return null;
        }
        while (value.startsWith(":")) {
            value = value.substring(1);
        }
        while (value.endsWith(":")) {
            value = value.substring(0, value.length() - 1);
        }
        if (value.isEmpty()) {
            return null;
        }
        int separator = value.lastIndexOf(':');
        return separator >= 0 ? value.substring(separator + 1) : value;
    }

    private static List<String> includeArguments(String settings) {
        String text = settings;
        List<String> arguments = new ArrayList<String>();
        Matcher matcher = INCLUDE_TOKEN.matcher(text);
        while (matcher.find()) {
            int cursor = matcher.end();
            while (cursor < text.length() && Character.isWhitespace(text.charAt(cursor)) && text.charAt(cursor) != '\n') {
                cursor++;
            }
            if (cursor >= text.length()) {
                continue;
            }
            if (text.charAt(cursor) == '(') {
                int end = closingParenthesis(text, cursor);
                if (end > cursor) {
                    arguments.add(text.substring(cursor + 1, end));
                    matcher.region(end + 1, text.length());
                }
                continue;
            }
            int end = text.indexOf('\n', cursor);
            arguments.add(text.substring(cursor, end < 0 ? text.length() : end));
        }
        return arguments;
    }

    private static int closingParenthesis(String text, int openIndex) {
        int depth = 0;
        boolean inString = false;
        char quote = 0;
        boolean escaped = false;
        for (int index = openIndex; index < text.length(); index++) {
            char current = text.charAt(index);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == quote) {
                    inString = false;
                }
                continue;
            }
            if (current == '\'' || current == '"') {
                inString = true;
                quote = current;
                continue;
            }
            if (current == '(') {
                depth++;
            } else if (current == ')') {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
        }
        return -1;
    }

    private static String stripComments(String text) {
        StringBuilder builder = new StringBuilder();
        boolean inString = false;
        char quote = 0;
        boolean escaped = false;
        for (int index = 0; index < text.length(); index++) {
            char current = text.charAt(index);
            if (inString) {
                builder.append(current);
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == quote) {
                    inString = false;
                }
                continue;
            }
            if (current == '\'' || current == '"') {
                inString = true;
                quote = current;
                builder.append(current);
                continue;
            }
            if (current == '/' && index + 1 < text.length()) {
                char next = text.charAt(index + 1);
                if (next == '/') {
                    index += 2;
                    while (index < text.length() && text.charAt(index) != '\n' && text.charAt(index) != '\r') {
                        index++;
                    }
                    if (index < text.length()) {
                        builder.append(text.charAt(index));
                    }
                    continue;
                }
                if (next == '*') {
                    index += 2;
                    while (index + 1 < text.length() && !(text.charAt(index) == '*' && text.charAt(index + 1) == '/')) {
                        if (text.charAt(index) == '\n' || text.charAt(index) == '\r') {
                            builder.append(text.charAt(index));
                        }
                        index++;
                    }
                    index++;
                    continue;
                }
            }
            builder.append(current);
        }
        return builder.toString();
    }

    private static final class VersionProperty {
        final String value;

        private VersionProperty(String value) {
            this.value = value;
        }
    }
}
