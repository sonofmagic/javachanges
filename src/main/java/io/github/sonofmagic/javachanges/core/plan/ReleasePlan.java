package io.github.sonofmagic.javachanges.core.plan;

import io.github.sonofmagic.javachanges.core.ReleaseLevel;
import io.github.sonofmagic.javachanges.core.ReleaseTagStrategy;
import io.github.sonofmagic.javachanges.core.ReleaseUtils;
import io.github.sonofmagic.javachanges.core.changeset.Changeset;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.firstBodyLine;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.joinModules;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.releaseLevelHeading;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.renderVisibleType;

public final class ReleasePlan {
    private final Path repoRoot;
    private final String currentRevision;
    private final String latestWholeRepoTag;
    private final List<Changeset> changesets;
    private final ReleaseLevel releaseLevel;
    private final String releaseVersion;
    private final String nextSnapshotVersion;
    private final ReleaseTagStrategy tagStrategy;

    ReleasePlan(Path repoRoot, String currentRevision, String latestWholeRepoTag,
                List<Changeset> changesets, ReleaseLevel releaseLevel,
                String releaseVersion, String nextSnapshotVersion, ReleaseTagStrategy tagStrategy) {
        this.repoRoot = repoRoot;
        this.currentRevision = currentRevision;
        this.latestWholeRepoTag = latestWholeRepoTag;
        this.changesets = changesets;
        this.releaseLevel = releaseLevel;
        this.releaseVersion = releaseVersion;
        this.nextSnapshotVersion = nextSnapshotVersion;
        this.tagStrategy = tagStrategy;
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

    public boolean hasPendingChangesets() {
        return !changesets.isEmpty();
    }

    ReleaseLevel getReleaseLevel() {
        return releaseLevel;
    }

    public String getReleaseVersion() {
        return releaseVersion;
    }

    String getNextSnapshotVersion() {
        return nextSnapshotVersion;
    }

    public ReleaseTagStrategy getTagStrategy() {
        return tagStrategy;
    }

    List<String> getAffectedPackages() {
        Set<String> packages = new LinkedHashSet<String>();
        for (Changeset changeset : changesets) {
            packages.addAll(changeset.modules);
        }
        return new ArrayList<String>(packages);
    }

    public List<ReleaseTarget> getReleaseTargets() {
        List<ReleaseTarget> targets = new ArrayList<ReleaseTarget>();
        if (releaseVersion == null) {
            return targets;
        }
        if (tagStrategy == ReleaseTagStrategy.PER_MODULE) {
            for (String module : getAffectedPackages()) {
                targets.add(new ReleaseTarget(module, module + "/v" + releaseVersion));
            }
            return targets;
        }
        targets.add(new ReleaseTarget(null, "v" + releaseVersion));
        return targets;
    }

    List<String> getPlannedTags() {
        List<String> tags = new ArrayList<String>();
        for (ReleaseTarget target : getReleaseTargets()) {
            tags.add(target.tag);
        }
        return tags;
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
        lines.add("| Field | Value |");
        lines.add("| --- | --- |");
        lines.add("| Release type | `" + releaseLevel.id + "` |");
        lines.add("| Affected packages | `" + joinModules(getAffectedPackages()) + "` |");
        lines.add("| Release version | `v" + releaseVersion + "` |");
        lines.add("| Tag strategy | `" + tagStrategy.id + "` |");
        lines.add("| Planned tags | `" + joinModules(getPlannedTags()) + "` |");
        lines.add("| Next snapshot | `" + nextSnapshotVersion + "` |");
        lines.add("");
        lines.add("## Included Changesets");
        lines.add("");
        Map<ReleaseLevel, List<Changeset>> grouped = new LinkedHashMap<ReleaseLevel, List<Changeset>>();
        grouped.put(ReleaseLevel.MAJOR, new ArrayList<Changeset>());
        grouped.put(ReleaseLevel.MINOR, new ArrayList<Changeset>());
        grouped.put(ReleaseLevel.PATCH, new ArrayList<Changeset>());
        for (Changeset changeset : changesets) {
            grouped.get(changeset.release).add(changeset);
        }
        for (ReleaseLevel level : Arrays.asList(ReleaseLevel.MAJOR, ReleaseLevel.MINOR, ReleaseLevel.PATCH)) {
            List<Changeset> levelChangesets = grouped.get(level);
            if (levelChangesets == null || levelChangesets.isEmpty()) {
                continue;
            }
            lines.add("### " + releaseLevelHeading(level));
            lines.add("");
            for (Changeset changeset : levelChangesets) {
                String visibleType = renderVisibleType(changeset.type);
                lines.add("- **" + changeset.summary + "**");
                lines.add("  - Release: `" + changeset.release.id + "`");
                lines.add("  - Packages: `" + joinModules(changeset.modules) + "`");
                if (!visibleType.isEmpty()) {
                    lines.add("  - Type: `" + visibleType + "`");
                }
                if (!changeset.body.isEmpty()) {
                    lines.add("  - Notes: " + firstBodyLine(changeset.body));
                }
            }
            lines.add("");
        }
        lines.add("This PR was generated automatically from `.changesets/*.md` files.");
        lines.add("Merging it will trigger an automatic tag push and then reuse the existing release workflows.");
        return lines;
    }

    String toJson() {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("releaseVersion", releaseVersion);
        payload.put("nextSnapshotVersion", nextSnapshotVersion);
        payload.put("releaseLevel", releaseLevel.id);
        payload.put("tagStrategy", tagStrategy.id);
        payload.put("tags", Collections.unmodifiableList(new ArrayList<String>(getPlannedTags())));
        List<Map<String, Object>> renderedTargets = new ArrayList<Map<String, Object>>();
        for (ReleaseTarget target : getReleaseTargets()) {
            Map<String, Object> entry = new LinkedHashMap<String, Object>();
            entry.put("module", target.module);
            entry.put("tag", target.tag);
            renderedTargets.add(entry);
        }
        payload.put("releaseTargets", renderedTargets);
        payload.put("generatedAt", OffsetDateTime.now().toString());
        List<Map<String, Object>> renderedChangesets = new ArrayList<Map<String, Object>>();
        for (Changeset changeset : changesets) {
            Map<String, Object> entry = new LinkedHashMap<String, Object>();
            entry.put("file", changeset.fileName);
            entry.put("release", changeset.release.id);
            entry.put("type", changeset.type);
            entry.put("summary", changeset.summary);
            entry.put("modules", Collections.unmodifiableList(new ArrayList<String>(changeset.modules)));
            renderedChangesets.add(entry);
        }
        payload.put("changesets", renderedChangesets);
        return ReleaseUtils.toPrettyJson(payload) + "\n";
    }

    public static final class ReleaseTarget {
        public final String module;
        public final String tag;

        public ReleaseTarget(String module, String tag) {
            this.module = module;
            this.tag = tag;
        }
    }
}
