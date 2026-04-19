package io.github.sonofmagic.javachanges.core;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.firstBodyLine;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.joinModules;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.jsonEscape;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.releaseLevelHeading;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.renderVisibleType;

final class ReleasePlan {
    private final Path repoRoot;
    private final String currentRevision;
    private final String latestWholeRepoTag;
    private final List<Changeset> changesets;
    private final ReleaseLevel releaseLevel;
    private final String releaseVersion;
    private final String nextSnapshotVersion;

    ReleasePlan(Path repoRoot, String currentRevision, String latestWholeRepoTag,
                List<Changeset> changesets, ReleaseLevel releaseLevel,
                String releaseVersion, String nextSnapshotVersion) {
        this.repoRoot = repoRoot;
        this.currentRevision = currentRevision;
        this.latestWholeRepoTag = latestWholeRepoTag;
        this.changesets = changesets;
        this.releaseLevel = releaseLevel;
        this.releaseVersion = releaseVersion;
        this.nextSnapshotVersion = nextSnapshotVersion;
    }

    Path getRepoRoot() {
        return repoRoot;
    }

    String getCurrentRevision() {
        return currentRevision;
    }

    String getLatestWholeRepoTag() {
        return latestWholeRepoTag;
    }

    List<Changeset> getChangesets() {
        return changesets;
    }

    boolean hasPendingChangesets() {
        return !changesets.isEmpty();
    }

    ReleaseLevel getReleaseLevel() {
        return releaseLevel;
    }

    String getReleaseVersion() {
        return releaseVersion;
    }

    String getNextSnapshotVersion() {
        return nextSnapshotVersion;
    }

    List<String> getAffectedPackages() {
        Set<String> packages = new LinkedHashSet<String>();
        for (Changeset changeset : changesets) {
            packages.addAll(changeset.modules);
        }
        return new ArrayList<String>(packages);
    }

    String renderChangelogSection() {
        StringBuilder builder = new StringBuilder();
        builder.append("## ").append(releaseVersion).append(" - ")
            .append(LocalDate.now().toString()).append("\n\n");

        Map<ReleaseLevel, List<Changeset>> grouped = new LinkedHashMap<ReleaseLevel, List<Changeset>>();
        grouped.put(ReleaseLevel.MAJOR, new ArrayList<Changeset>());
        grouped.put(ReleaseLevel.MINOR, new ArrayList<Changeset>());
        grouped.put(ReleaseLevel.PATCH, new ArrayList<Changeset>());
        for (Changeset changeset : changesets) {
            grouped.get(changeset.release).add(changeset);
        }

        boolean wroteSection = false;
        for (ReleaseLevel level : Arrays.asList(ReleaseLevel.MAJOR, ReleaseLevel.MINOR, ReleaseLevel.PATCH)) {
            List<Changeset> levelChangesets = grouped.get(level);
            if (levelChangesets == null || levelChangesets.isEmpty()) {
                continue;
            }
            builder.append("### ").append(releaseLevelHeading(level)).append("\n\n");
            for (Changeset changeset : levelChangesets) {
                builder.append("- ").append(changeset.summary);
                builder.append(" (packages: ").append(joinModules(changeset.modules)).append(")");
                if (!changeset.body.isEmpty()) {
                    builder.append(" ");
                    builder.append(firstBodyLine(changeset.body));
                }
                builder.append("\n");
            }
            builder.append("\n");
            wroteSection = true;
        }

        if (!wroteSection) {
            builder.append("- No user-facing changes were recorded for this release.\n\n");
        }

        return builder.toString().trim() + "\n";
    }

    List<String> toPullRequestBodyLines() {
        List<String> lines = new ArrayList<String>();
        lines.add("## Release Plan");
        lines.add("");
        lines.add("- Release type: `" + releaseLevel.id + "`");
        lines.add("- Affected packages: `" + joinModules(getAffectedPackages()) + "`");
        lines.add("- Release version: `v" + releaseVersion + "`");
        lines.add("- Next snapshot: `" + nextSnapshotVersion + "`");
        lines.add("");
        lines.add("## Included Changesets");
        lines.add("");
        for (Changeset changeset : changesets) {
            String visibleType = renderVisibleType(changeset.type);
            lines.add("- `" + changeset.release.id + "` "
                + "`packages: " + joinModules(changeset.modules) + "` "
                + (visibleType.isEmpty() ? "" : "`" + visibleType + "` ")
                + changeset.summary);
        }
        lines.add("");
        lines.add("This PR was generated automatically from `.changesets/*.md` files.");
        lines.add("Merging it will trigger an automatic tag push and then reuse the existing release workflows.");
        return lines;
    }

    String toJson() {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        builder.append("  \"releaseVersion\": \"").append(jsonEscape(releaseVersion)).append("\",\n");
        builder.append("  \"nextSnapshotVersion\": \"").append(jsonEscape(nextSnapshotVersion)).append("\",\n");
        builder.append("  \"releaseLevel\": \"").append(jsonEscape(releaseLevel.id)).append("\",\n");
        builder.append("  \"generatedAt\": \"").append(jsonEscape(OffsetDateTime.now().toString())).append("\",\n");
        builder.append("  \"changesets\": [\n");
        for (int i = 0; i < changesets.size(); i++) {
            Changeset changeset = changesets.get(i);
            builder.append("    {\n");
            builder.append("      \"file\": \"").append(jsonEscape(changeset.fileName)).append("\",\n");
            builder.append("      \"release\": \"").append(jsonEscape(changeset.release.id)).append("\",\n");
            builder.append("      \"type\": \"").append(jsonEscape(changeset.type)).append("\",\n");
            builder.append("      \"summary\": \"").append(jsonEscape(changeset.summary)).append("\",\n");
            builder.append("      \"modules\": [");
            for (int moduleIndex = 0; moduleIndex < changeset.modules.size(); moduleIndex++) {
                if (moduleIndex > 0) {
                    builder.append(", ");
                }
                builder.append("\"").append(jsonEscape(changeset.modules.get(moduleIndex))).append("\"");
            }
            builder.append("]\n");
            builder.append("    }");
            if (i + 1 < changesets.size()) {
                builder.append(",");
            }
            builder.append("\n");
        }
        builder.append("  ]\n");
        builder.append("}\n");
        return builder.toString();
    }
}
