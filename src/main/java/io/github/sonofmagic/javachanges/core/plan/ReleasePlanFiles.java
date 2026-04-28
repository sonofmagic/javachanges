package io.github.sonofmagic.javachanges.core.plan;

import io.github.sonofmagic.javachanges.core.ChangesetPaths;
import io.github.sonofmagic.javachanges.core.BuildModelSupport;
import io.github.sonofmagic.javachanges.core.changeset.Changeset;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

final class ReleasePlanFiles {
    private ReleasePlanFiles() {
    }

    static void applyPlan(Path repoRoot, ReleasePlan plan) throws IOException {
        updateRootRevision(repoRoot, plan.getNextSnapshotVersion());
        updateChangelog(repoRoot.resolve("CHANGELOG.md"), plan);
        Files.write(repoRoot.resolve(ChangesetPaths.DIR).resolve(ChangesetPaths.RELEASE_PLAN_JSON),
            Collections.singletonList(plan.toJson()),
            StandardCharsets.UTF_8);
        Files.write(repoRoot.resolve(ChangesetPaths.DIR).resolve(ChangesetPaths.RELEASE_PLAN_MD),
            plan.toPullRequestBodyLines(),
            StandardCharsets.UTF_8);
        for (Changeset changeset : plan.getChangesets()) {
            Files.deleteIfExists(changeset.path);
        }
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
