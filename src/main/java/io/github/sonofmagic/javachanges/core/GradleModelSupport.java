package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
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
    private static final Pattern INCLUDE_CALL = Pattern.compile("(?m)^\\s*include\\s*(?:\\(([^)]*)\\)|(.+))$");
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
            throw new IllegalStateException("Cannot find version or revision in " + gradlePropertiesPath);
        }
        return property.value;
    }

    static void writeRevision(Path gradlePropertiesPath, String revision) throws IOException {
        List<String> lines = Files.readAllLines(gradlePropertiesPath, StandardCharsets.UTF_8);
        int index = preferredVersionPropertyLine(lines);
        if (index < 0) {
            throw new IllegalStateException("Cannot find version or revision in " + gradlePropertiesPath);
        }
        Matcher matcher = PROPERTY_LINE.matcher(lines.get(index));
        if (!matcher.matches()) {
            throw new IllegalStateException("Cannot parse version property in " + gradlePropertiesPath);
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
        Set<String> modules = new LinkedHashSet<String>();
        Matcher includeMatcher = INCLUDE_CALL.matcher(stripLineComments(settings));
        while (includeMatcher.find()) {
            String arguments = includeMatcher.group(1) == null ? includeMatcher.group(2) : includeMatcher.group(1);
            Matcher valueMatcher = QUOTED_VALUE.matcher(arguments);
            while (valueMatcher.find()) {
                String module = normalizeProjectPath(valueMatcher.group(1));
                if (module != null) {
                    modules.add(module);
                }
            }
        }
        if (!modules.isEmpty()) {
            return new ArrayList<String>(modules);
        }
        String rootProjectName = readRootProjectName(settings);
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

    private static String stripLineComments(String text) {
        StringBuilder builder = new StringBuilder();
        String[] lines = text.split("\\r?\\n", -1);
        for (String line : lines) {
            int comment = line.indexOf("//");
            builder.append(comment >= 0 ? line.substring(0, comment) : line).append('\n');
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
