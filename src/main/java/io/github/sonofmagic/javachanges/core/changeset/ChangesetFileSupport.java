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
        Path dir = repoRoot.resolve(ChangesetPaths.DIR);
        if (!Files.exists(dir)) {
            return Collections.emptyList();
        }
        List<Changeset> changesets = new ArrayList<Changeset>();
        DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.md");
        try {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                if (isChangesetReadme(fileName) || ChangesetPaths.RELEASE_PLAN_MD.equals(fileName)) {
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
        if (ReleaseMessages.zh()) {
            return "# Changesets\n"
                + "\n"
                + "这个目录保存待发布的 release notes。每个面向用户的变更都应该添加一条 changeset。\n"
                + "\n"
                + "创建 changeset:\n"
                + "\n"
                + "```bash\n"
                + "javachanges add --directory . --summary \"描述这次变更\" --release patch\n"
                + "```\n"
                + "\n"
                + "多模块仓库请使用检测到的模块名:\n"
                + "\n"
                + "```bash\n"
                + "javachanges modules --directory .\n"
                + "javachanges add --directory . --modules core --summary \"描述这次变更\" --release patch\n"
                + "```\n"
                + "\n"
                + "Changeset 使用官方 package-map frontmatter 格式:\n"
                + "\n"
                + "```md\n"
                + "---\n"
                + "\"core\": minor\n"
                + "---\n"
                + "\n"
                + "描述面向用户的变更。\n"
                + "```\n"
                + "\n"
                + "支持的发布级别为 `patch`、`minor` 和 `major`。\n"
                + "\n"
                + "检查并应用发布计划:\n"
                + "\n"
                + "```bash\n"
                + "javachanges status --directory .\n"
                + "javachanges plan --directory . --apply true\n"
                + "```\n";
        }
        return "# Changesets\n"
            + "\n"
            + "This directory stores pending release notes. Add one changeset for each user-visible change.\n"
            + "\n"
            + "Create a changeset:\n"
            + "\n"
            + "```bash\n"
            + "javachanges add --directory . --summary \"describe the change\" --release patch\n"
            + "```\n"
            + "\n"
            + "For multi-module repositories, use detected module names:\n"
            + "\n"
            + "```bash\n"
            + "javachanges modules --directory .\n"
            + "javachanges add --directory . --modules core --summary \"describe the change\" --release patch\n"
            + "```\n"
            + "\n"
            + "Changesets use the official package-map frontmatter shape:\n"
            + "\n"
            + "```md\n"
            + "---\n"
            + "\"core\": minor\n"
            + "---\n"
            + "\n"
            + "Describe the user-visible change.\n"
            + "```\n"
            + "\n"
            + "Supported release levels are `patch`, `minor`, and `major`.\n"
            + "\n"
            + "Review and apply the release plan:\n"
            + "\n"
            + "```bash\n"
            + "javachanges status --directory .\n"
            + "javachanges plan --directory . --apply true\n"
            + "```\n";
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
