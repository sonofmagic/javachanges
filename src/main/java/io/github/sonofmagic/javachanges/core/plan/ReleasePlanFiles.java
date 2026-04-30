package io.github.sonofmagic.javachanges.core.plan;

import io.github.sonofmagic.javachanges.core.ChangesetPaths;
import io.github.sonofmagic.javachanges.core.BuildModelSupport;
import io.github.sonofmagic.javachanges.core.ReleaseModuleUtils;
import io.github.sonofmagic.javachanges.core.changeset.Changeset;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ReleasePlanFiles {
    private static final Pattern JAVACHANGES_SNAPSHOT_PLUGIN_COORDINATE = Pattern.compile(
        "(io\\.github\\.sonofmagic:javachanges:)[^:\\s`]+-SNAPSHOT"
    );
    private static final Pattern CURRENT_SNAPSHOT_REFERENCE = Pattern.compile(
        "(current `|\\u5f53\\u524d `)[^`]+-SNAPSHOT(`)"
    );

    private ReleasePlanFiles() {
    }

    static void applyPlan(Path repoRoot, ReleasePlan plan) throws IOException {
        applyPlan(repoRoot, plan, true);
    }

    static void applyPlan(Path repoRoot, ReleasePlan plan, boolean writeReleasePlanFiles) throws IOException {
        updateRootRevision(repoRoot, plan.getNextSnapshotVersion());
        updateJavachangesSnapshotDocumentation(repoRoot, plan.getNextSnapshotVersion());
        updateChangelog(repoRoot.resolve("CHANGELOG.md"), plan);
        Path releasePlanJson = repoRoot.resolve(ChangesetPaths.DIR).resolve(ChangesetPaths.RELEASE_PLAN_JSON);
        Path releasePlanMarkdown = repoRoot.resolve(ChangesetPaths.DIR).resolve(ChangesetPaths.RELEASE_PLAN_MD);
        if (writeReleasePlanFiles) {
            Files.write(releasePlanJson, Collections.singletonList(plan.toJson()), StandardCharsets.UTF_8);
            Files.write(releasePlanMarkdown, plan.toPullRequestBodyLines(), StandardCharsets.UTF_8);
        } else {
            Files.deleteIfExists(releasePlanJson);
            Files.deleteIfExists(releasePlanMarkdown);
        }
        for (Changeset changeset : plan.getChangesets()) {
            Files.deleteIfExists(changeset.path);
        }
    }

    private static void updateJavachangesSnapshotDocumentation(Path repoRoot, String nextSnapshotVersion)
        throws IOException {
        if (!ReleaseModuleUtils.detectKnownModules(repoRoot).contains("javachanges")) {
            return;
        }
        updateSnapshotDocumentationFile(repoRoot.resolve("README.md"), nextSnapshotVersion);
        updateSnapshotDocumentationFile(repoRoot.resolve("README.zh-CN.md"), nextSnapshotVersion);
    }

    private static void updateSnapshotDocumentationFile(Path path, String nextSnapshotVersion) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        String updated = replaceSnapshotVersion(JAVACHANGES_SNAPSHOT_PLUGIN_COORDINATE, content, nextSnapshotVersion);
        updated = replaceSnapshotVersion(CURRENT_SNAPSHOT_REFERENCE, updated, nextSnapshotVersion);
        if (!updated.equals(content)) {
            Files.write(path, updated.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String replaceSnapshotVersion(Pattern pattern, String content, String nextSnapshotVersion) {
        Matcher matcher = pattern.matcher(content);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String suffix = matcher.groupCount() > 1 ? matcher.group(2) : "";
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group(1) + nextSnapshotVersion + suffix));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static void updateRootRevision(Path repoRoot, String newSnapshotVersion) throws IOException {
        BuildModelSupport.writeRevision(repoRoot, newSnapshotVersion);
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
}
