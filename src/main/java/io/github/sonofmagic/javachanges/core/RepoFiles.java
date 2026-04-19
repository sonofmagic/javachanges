package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.*;

final class RepoFiles {
    private RepoFiles() {
    }

    static Path resolveRepoRoot(String directoryOption) {
        Path current = directoryOption == null
            ? Paths.get("").toAbsolutePath().normalize()
            : Paths.get(directoryOption).toAbsolutePath().normalize();

        Path probe = current;
        while (probe != null) {
            if (Files.exists(probe.resolve("pom.xml")) && !detectKnownModules(probe).isEmpty()) {
                return probe;
            }
            probe = probe.getParent();
        }
        throw new IllegalStateException("Cannot find repository root from " + current);
    }

    static void ensureChangesetReadme(Path repoRoot) throws IOException {
        Path dir = repoRoot.resolve(CHANGESETS_DIR);
        Files.createDirectories(dir);
        Path readme = dir.resolve(CHANGESETS_README);
        if (!Files.exists(readme)) {
            Files.write(readme, Collections.singletonList("# Changesets"), StandardCharsets.UTF_8);
        }
    }

    static Path writeChangeset(Path repoRoot, ChangesetInput input) throws IOException {
        ensureChangesetReadme(repoRoot);
        String slug = Slug.slugify(input.summary);
        String baseName = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + slug;
        Path dir = repoRoot.resolve(CHANGESETS_DIR);
        Path file = dir.resolve(baseName + ".md");
        int counter = 2;
        while (Files.exists(file)) {
            file = dir.resolve(baseName + "-" + counter + ".md");
            counter++;
        }

        List<String> lines = new ArrayList<String>();
        lines.add("---");
        for (String module : input.modules) {
            lines.add("\"" + module + "\": " + input.release.id);
        }
        lines.add("---");
        lines.add("");
        lines.addAll(Arrays.asList(renderChangesetBody(input.summary, input.body).split("\\r?\\n")));
        Files.write(file, lines, StandardCharsets.UTF_8);
        return file;
    }

    static List<Changeset> loadChangesets(Path repoRoot) throws IOException {
        Path dir = repoRoot.resolve(CHANGESETS_DIR);
        if (!Files.exists(dir)) {
            return Collections.emptyList();
        }
        List<Changeset> changesets = new ArrayList<Changeset>();
        DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.md");
        try {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                if (CHANGESETS_README.equals(fileName) || RELEASE_PLAN_MD.equals(fileName)) {
                    continue;
                }
                changesets.add(parseChangeset(repoRoot, path));
            }
        } finally {
            stream.close();
        }
        Collections.sort(changesets, new Comparator<Changeset>() {
            @Override
            public int compare(Changeset left, Changeset right) {
                return left.fileName.compareTo(right.fileName);
            }
        });
        return changesets;
    }

    static String readManifestField(Path repoRoot, String field) throws IOException {
        Path manifest = repoRoot.resolve(CHANGESETS_DIR).resolve(RELEASE_PLAN_JSON);
        if (!Files.exists(manifest)) {
            throw new IllegalStateException("Missing release plan manifest: " + manifest);
        }
        String content = new String(Files.readAllBytes(manifest), StandardCharsets.UTF_8);
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(content);
        if (!matcher.find()) {
            throw new IllegalStateException("Missing field `" + field + "` in " + manifest);
        }
        return matcher.group(1);
    }

    static void applyPlan(Path repoRoot, ReleasePlan plan) throws IOException {
        updateRootRevision(repoRoot.resolve("pom.xml"), plan.getNextSnapshotVersion());
        updateChangelog(repoRoot.resolve("CHANGELOG.md"), plan);
        Files.write(repoRoot.resolve(CHANGESETS_DIR).resolve(RELEASE_PLAN_JSON),
            Collections.singletonList(plan.toJson()),
            StandardCharsets.UTF_8);
        Files.write(repoRoot.resolve(CHANGESETS_DIR).resolve(RELEASE_PLAN_MD),
            plan.toPullRequestBodyLines(),
            StandardCharsets.UTF_8);
        for (Changeset changeset : plan.getChangesets()) {
            Files.deleteIfExists(changeset.path);
        }
    }

    private static Changeset parseChangeset(Path repoRoot, Path path) throws IOException {
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
            String key = stripWrappingQuotes(line.substring(0, separator).trim());
            String value = line.substring(separator + 1).trim();
            frontmatter.put(key, value);
        }

        List<String> bodyLines = new ArrayList<String>();
        for (; index < lines.size(); index++) {
            bodyLines.add(lines.get(index));
        }
        String body = trimTrailingBlankLines(bodyLines);

        if (frontmatter.containsKey("release")) {
            return parseLegacyChangeset(repoRoot, path, frontmatter, body);
        }
        return parseOfficialChangeset(repoRoot, path, frontmatter, body);
    }

    private static Changeset parseLegacyChangeset(Path repoRoot, Path path, Map<String, String> frontmatter, String body) {
        String releaseValue = required(frontmatter, "release", path);
        String typeValue = optional(frontmatter, "type", "other");
        String modulesValue = optional(frontmatter, "modules", "all");
        String summaryValue = trimToNull(frontmatter.get("summary"));
        if (summaryValue == null) {
            summaryValue = fallbackSummary(path, body);
        }

        return new Changeset(
            path,
            path.getFileName().toString(),
            ReleaseLevel.parse(releaseValue),
            normalizeType(typeValue),
            parseModules(repoRoot, modulesValue),
            summaryValue,
            body
        );
    }

    private static Changeset parseOfficialChangeset(Path repoRoot, Path path, Map<String, String> frontmatter, String body) {
        List<String> knownModules = detectKnownModules(repoRoot);
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
            normalizeType(typeValue),
            modules,
            fallbackSummary(path, body),
            body
        );
    }

    private static void updateRootRevision(Path pomPath, String newSnapshotVersion) throws IOException {
        String original = new String(Files.readAllBytes(pomPath), StandardCharsets.UTF_8);
        String updated = original.replaceFirst(
            "<revision>[^<]+</revision>",
            "<revision>" + newSnapshotVersion + "</revision>"
        );
        if (original.equals(updated)) {
            throw new IllegalStateException("Unable to update <revision> in " + pomPath);
        }
        Files.write(pomPath, Collections.singletonList(updated), StandardCharsets.UTF_8);
    }

    private static void updateChangelog(Path changelogPath, ReleasePlan plan) throws IOException {
        String section = plan.renderChangelogSection();
        List<String> existing = Files.exists(changelogPath)
            ? Files.readAllLines(changelogPath, StandardCharsets.UTF_8)
            : defaultChangelogLines();

        String heading = "## " + plan.getReleaseVersion() + " - ";
        for (String line : existing) {
            if (line.startsWith(heading)) {
                return;
            }
        }

        List<String> updated = new ArrayList<String>();
        boolean inserted = false;
        for (String line : existing) {
            updated.add(line);
            if (!inserted && "All notable changes to this project will be documented in this file.".equals(line)) {
                updated.add("");
                updated.addAll(Arrays.asList(section.split("\\r?\\n")));
                inserted = true;
            }
        }
        if (!inserted) {
            if (!updated.isEmpty() && !updated.get(updated.size() - 1).isEmpty()) {
                updated.add("");
            }
            updated.addAll(Arrays.asList(section.split("\\r?\\n")));
        }
        Files.write(changelogPath, updated, StandardCharsets.UTF_8);
    }

    private static List<String> defaultChangelogLines() {
        return Arrays.asList(
            "# Changelog",
            "",
            "All notable changes to this project will be documented in this file.",
            ""
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
        String value = trimToNull(frontmatter.get(key));
        return value == null ? defaultValue : value;
    }

    private static String renderChangesetBody(String summary, String body) {
        String normalizedSummary = summary.trim();
        String normalizedBody = trimToNull(body);
        if (normalizedBody == null) {
            return normalizedSummary;
        }
        if (normalizedSummary.equals(firstBodyLine(normalizedBody))) {
            return normalizedBody;
        }
        return normalizedSummary + "\n\n" + normalizedBody;
    }

    private static String fallbackSummary(Path path, String body) {
        String fromBody = firstBodyLine(body);
        if (!fromBody.isEmpty()) {
            return fromBody;
        }
        String fileName = path.getFileName().toString().replaceFirst("\\.md$", "");
        return fileName.replaceFirst("^\\d{8}-", "").replace('-', ' ').trim();
    }
}
