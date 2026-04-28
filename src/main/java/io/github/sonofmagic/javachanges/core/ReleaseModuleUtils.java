package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ReleaseModuleUtils {
    private ReleaseModuleUtils() {
    }

    public static List<String> detectKnownModules(Path repoRoot) {
        return BuildModelSupport.detectKnownModules(repoRoot);
    }

    static List<String> detectMavenModules(Path repoRoot) {
        try {
            Path pomPath = repoRoot.resolve("pom.xml");
            if (!java.nio.file.Files.exists(pomPath)) {
                return Collections.emptyList();
            }

            List<String> modulePaths = PomModelSupport.readModulePaths(pomPath);
            if (modulePaths.isEmpty()) {
                String artifactId = PomModelSupport.readArtifactId(pomPath);
                if (artifactId == null) {
                    return Collections.emptyList();
                }
                return Collections.singletonList(artifactId);
            }

            List<String> modules = new ArrayList<String>();
            for (String modulePath : modulePaths) {
                Path modulePom = repoRoot.resolve(modulePath).resolve("pom.xml");
                if (!java.nio.file.Files.exists(modulePom)) {
                    continue;
                }

                String artifactId = PomModelSupport.readArtifactId(modulePom);
                modules.add(artifactId == null ? Paths.get(modulePath).getFileName().toString() : artifactId);
            }

            return modules;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to detect Maven modules from " + repoRoot, exception);
        }
    }

    public static void assertKnownModule(Path repoRoot, String module) {
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
        BuildModelSupport.BuildModel model = BuildModelSupport.detect(repoRoot);
        if (model != null && model.type == BuildModelSupport.BuildType.GRADLE) {
            return ":" + module;
        }
        return "-pl :" + module + " -am";
    }

    public static String releaseVersionFromTag(String tag) {
        if (tag.startsWith("v")) {
            return tag.substring(1);
        }
        int separator = tag.lastIndexOf("/v");
        if (separator > 0) {
            return tag.substring(separator + 2);
        }
        throw new IllegalArgumentException("Unsupported release tag: " + tag);
    }

    public static String releaseModuleFromTag(String tag) {
        if (tag.startsWith("v")) {
            return null;
        }
        int separator = tag.lastIndexOf("/v");
        if (separator > 0) {
            return tag.substring(0, separator);
        }
        throw new IllegalArgumentException("Unsupported release tag: " + tag);
    }

    public static List<String> parseModules(Path repoRoot, String rawModules) {
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

    public static String normalizeType(String rawType) {
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

    public static String joinModules(List<String> modules) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < modules.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(modules.get(i));
        }
        return builder.toString();
    }
}
