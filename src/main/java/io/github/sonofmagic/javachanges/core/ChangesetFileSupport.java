package io.github.sonofmagic.javachanges.core;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.CHANGESETS_DIR;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.CHANGESETS_README;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.RELEASE_PLAN_JSON;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.RELEASE_PLAN_MD;

final class ChangesetFileSupport {
    private ChangesetFileSupport() {
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
                if (isChangesetReadme(fileName) || RELEASE_PLAN_MD.equals(fileName)) {
                    continue;
                }
                changesets.add(ChangesetParser.parse(repoRoot, path));
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

    private static boolean isChangesetReadme(String fileName) {
        return CHANGESETS_README.equals(fileName)
            || fileName.startsWith("README.")
            || fileName.startsWith("README-");
    }

    private static String renderChangesetBody(String summary, String body) {
        String normalizedSummary = summary.trim();
        String normalizedBody = ReleaseUtils.trimToNull(body);
        if (normalizedBody == null) {
            return normalizedSummary;
        }
        if (normalizedSummary.equals(ReleaseUtils.firstBodyLine(normalizedBody))) {
            return normalizedBody;
        }
        return normalizedSummary + "\n\n" + normalizedBody;
    }
}
