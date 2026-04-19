package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ReleaseModuleUtils {
    private ReleaseModuleUtils() {
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
                String modulePath = ReleaseTextUtils.trimToNull(moduleMatcher.group(1));
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

    static List<String> parseModules(Path repoRoot, String rawModules) {
        List<String> knownModules = detectKnownModules(repoRoot);
        String trimmed = rawModules.trim();
        if ("all".equalsIgnoreCase(trimmed)) {
            return new ArrayList<String>(knownModules);
        }
        String[] split = trimmed.split(",");
        Set<String> modules = new LinkedHashSet<String>();
        for (String part : split) {
            String module = ReleaseTextUtils.trimToNull(part);
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
        List<String> supported = java.util.Arrays.asList("feat", "fix", "docs", "build", "ci", "test", "refactor", "perf", "chore", "other");
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

    private static String detectRootArtifactId(String pomContent) {
        String withoutParent = pomContent.replaceFirst("(?s)<parent>.*?</parent>", "");
        Matcher artifactMatcher = Pattern.compile("<artifactId>([^<]+)</artifactId>").matcher(withoutParent);
        if (!artifactMatcher.find()) {
            return null;
        }
        return ReleaseTextUtils.trimToNull(artifactMatcher.group(1));
    }
}
