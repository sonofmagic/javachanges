package io.github.sonofmagic.javachanges.core.changeset;

import io.github.sonofmagic.javachanges.core.ChangesetPaths;
import io.github.sonofmagic.javachanges.core.ReleaseJsonUtils;
import io.github.sonofmagic.javachanges.core.ReleaseMessages;
import io.github.sonofmagic.javachanges.core.ReleaseTextUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class ChangesetFileSupport {
    private ChangesetFileSupport() {
    }

    public static void ensureChangesetReadme(Path repoRoot) throws IOException {
        Path dir = repoRoot.resolve(ChangesetPaths.DIR);
        Files.createDirectories(dir);
        Path readme = dir.resolve(ChangesetPaths.README);
        if (!Files.exists(readme)) {
            writeDefaultChangesetReadme(repoRoot);
        }
    }

    public static void writeDefaultChangesetReadme(Path repoRoot) throws IOException {
        Path dir = repoRoot.resolve(ChangesetPaths.DIR);
        Files.createDirectories(dir);
        Files.write(dir.resolve(ChangesetPaths.README), defaultReadme().getBytes(StandardCharsets.UTF_8));
    }

    public static Path writeChangeset(Path repoRoot, ChangesetInput input) throws IOException {
        ensureChangesetReadme(repoRoot);
        String slug = Slug.slugify(input.summary);
        String baseName = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + slug;
        Path dir = repoRoot.resolve(ChangesetPaths.DIR);
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

    public static List<Changeset> loadChangesets(Path repoRoot) throws IOException {
        List<Changeset> changesets = new ArrayList<Changeset>();
        for (Path path : listPendingChangesetFiles(repoRoot)) {
            changesets.add(parseChangeset(repoRoot, path));
        }
        Collections.sort(changesets, new Comparator<Changeset>() {
            @Override
            public int compare(Changeset left, Changeset right) {
                return left.fileName.compareTo(right.fileName);
            }
        });
        return changesets;
    }

    public static List<Path> listPendingChangesetFiles(Path repoRoot) throws IOException {
        Path dir = repoRoot.resolve(ChangesetPaths.DIR);
        if (!Files.exists(dir)) {
            return Collections.emptyList();
        }
        List<Path> paths = new ArrayList<Path>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.md")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                if (isChangesetReadme(fileName) || ChangesetPaths.RELEASE_PLAN_MD.equals(fileName)) {
                    continue;
                }
                paths.add(path);
            }
        }
        Collections.sort(paths, new Comparator<Path>() {
            @Override
            public int compare(Path left, Path right) {
                return left.getFileName().toString().compareTo(right.getFileName().toString());
            }
        });
        return paths;
    }

    public static Changeset parseChangeset(Path repoRoot, Path path) throws IOException {
        return ChangesetParser.parse(repoRoot, path);
    }

    public static String readManifestField(Path repoRoot, String field) throws IOException {
        Path manifest = repoRoot.resolve(ChangesetPaths.DIR).resolve(ChangesetPaths.RELEASE_PLAN_JSON);
        if (!Files.exists(manifest)) {
            throw new IllegalStateException(ReleaseMessages.missingReleasePlanManifest(manifest));
        }
        String content = new String(Files.readAllBytes(manifest), StandardCharsets.UTF_8);
        com.fasterxml.jackson.databind.JsonNode root = ReleaseJsonUtils.readTree(content);
        com.fasterxml.jackson.databind.JsonNode value = root.get(field);
        if (value == null || value.isNull()) {
            throw new IllegalStateException(ReleaseMessages.missingFieldIn(field, manifest));
        }
        return value.asText();
    }

    private static boolean isChangesetReadme(String fileName) {
        return ChangesetPaths.README.equals(fileName)
            || fileName.startsWith("README.")
            || fileName.startsWith("README-");
    }

    private static String defaultReadme() {
        return ReleaseMessages.changesetReadme();
    }

    private static String renderChangesetBody(String summary, String body) {
        String normalizedSummary = summary.trim();
        String normalizedBody = ReleaseTextUtils.trimToNull(body);
        if (normalizedBody == null) {
            return normalizedSummary;
        }
        if (normalizedSummary.equals(ReleaseTextUtils.firstBodyLine(normalizedBody))) {
            return normalizedBody;
        }
        return normalizedSummary + "\n\n" + normalizedBody;
    }
}
