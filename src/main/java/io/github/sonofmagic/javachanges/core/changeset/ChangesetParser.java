package io.github.sonofmagic.javachanges.core.changeset;

import io.github.sonofmagic.javachanges.core.ReleaseLevel;
import io.github.sonofmagic.javachanges.core.ReleaseModuleUtils;
import io.github.sonofmagic.javachanges.core.ReleaseTextUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ChangesetParser {
    private ChangesetParser() {
    }

    static Changeset parse(Path repoRoot, Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.size() < 3 || !"---".equals(lines.get(0))) {
            throw new IllegalStateException("Invalid changeset frontmatter: " + path);
        }
        Map<String, String> frontmatter = new LinkedHashMap<String, String>();
        int index = 1;
        for (; index < lines.size(); index++) {
            String line = lines.get(index);
            if ("---".equals(line)) {
                index++;
                break;
            }
            int separator = line.indexOf(':');
            if (separator <= 0) {
                throw new IllegalStateException("Invalid changeset line in " + path + ": " + line);
            }
            String key = ReleaseTextUtils.stripWrappingQuotes(line.substring(0, separator).trim());
            String value = line.substring(separator + 1).trim();
            frontmatter.put(key, value);
        }

        List<String> bodyLines = new ArrayList<String>();
        for (; index < lines.size(); index++) {
            bodyLines.add(lines.get(index));
        }
        String body = ReleaseTextUtils.trimTrailingBlankLines(bodyLines);

        if (frontmatter.containsKey("release")) {
            return parseLegacyChangeset(repoRoot, path, frontmatter, body);
        }
        return parseOfficialChangeset(repoRoot, path, frontmatter, body);
    }

    private static Changeset parseLegacyChangeset(Path repoRoot, Path path, Map<String, String> frontmatter, String body) {
        String releaseValue = required(frontmatter, "release", path);
        String typeValue = optional(frontmatter, "type", "other");
        String modulesValue = optional(frontmatter, "modules", "all");
        String summaryValue = ReleaseTextUtils.trimToNull(frontmatter.get("summary"));
        if (summaryValue == null) {
            summaryValue = fallbackSummary(path, body);
        }

        return new Changeset(
            path,
            path.getFileName().toString(),
            ReleaseLevel.parse(releaseValue),
            ReleaseModuleUtils.normalizeType(typeValue),
            ReleaseModuleUtils.parseModules(repoRoot, modulesValue),
            summaryValue,
            body
        );
    }

    private static Changeset parseOfficialChangeset(Path repoRoot, Path path, Map<String, String> frontmatter, String body) {
        List<String> knownModules = ReleaseModuleUtils.detectKnownModules(repoRoot);
        List<String> modules = new ArrayList<String>();
        ReleaseLevel releaseLevel = ReleaseLevel.PATCH;
        String typeValue = optional(frontmatter, "type", "other");

        for (Map.Entry<String, String> entry : frontmatter.entrySet()) {
            String key = entry.getKey();
            if ("type".equals(key)) {
                continue;
            }
            if (!knownModules.contains(key)) {
                throw new IllegalArgumentException("Unknown module: " + key + ", allowed: " + knownModules);
            }
            if (!modules.contains(key)) {
                modules.add(key);
            }
            ReleaseLevel declared = ReleaseLevel.parse(entry.getValue());
            if (declared.weight > releaseLevel.weight) {
                releaseLevel = declared;
            }
        }

        if (modules.isEmpty()) {
            throw new IllegalStateException("Missing package release entries in " + path);
        }

        return new Changeset(
            path,
            path.getFileName().toString(),
            releaseLevel,
            ReleaseModuleUtils.normalizeType(typeValue),
            modules,
            fallbackSummary(path, body),
            body
        );
    }

    private static String required(Map<String, String> frontmatter, String key, Path path) {
        String value = frontmatter.get(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Missing `" + key + "` in " + path);
        }
        return value.trim();
    }

    private static String optional(Map<String, String> frontmatter, String key, String defaultValue) {
        String value = ReleaseTextUtils.trimToNull(frontmatter.get(key));
        return value == null ? defaultValue : value;
    }

    private static String fallbackSummary(Path path, String body) {
        String fromBody = ReleaseTextUtils.firstBodyLine(body);
        if (!fromBody.isEmpty()) {
            return fromBody;
        }
        String fileName = path.getFileName().toString().replaceFirst("\\.md$", "");
        return fileName.replaceFirst("^\\d{8}-", "").replace('-', ' ').trim();
    }
}
