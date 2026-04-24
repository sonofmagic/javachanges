package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.maxReleaseLevel;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.stripSnapshot;

final class ReleasePlanner {
    private final Path repoRoot;

    ReleasePlanner(Path repoRoot) {
        this.repoRoot = repoRoot;
    }

    ReleasePlan plan() throws IOException, InterruptedException {
        String currentRevision = PomModelSupport.readRevision(repoRoot.resolve("pom.xml"));
        Semver currentBaseVersion = Semver.parse(stripSnapshot(currentRevision));
        List<Changeset> changesets = RepoFiles.loadChangesets(repoRoot);
        String latestTag = latestWholeRepoTag();
        ChangesetConfigSupport.ChangesetConfig changesetConfig = RepoFiles.readChangesetConfig(repoRoot);

        if (changesets.isEmpty()) {
            return new ReleasePlan(repoRoot, currentRevision, latestTag, Collections.<Changeset>emptyList(),
                null, null, currentRevision, changesetConfig.tagStrategy());
        }

        ReleaseLevel releaseLevel = maxReleaseLevel(changesets);
        Semver latestTagVersion = latestTag == null ? currentBaseVersion : Semver.parse(latestTag.substring(1));
        Semver bumpedFromTag = latestTag == null ? currentBaseVersion.bump(releaseLevel) : latestTagVersion.bump(releaseLevel);
        Semver releaseVersion = Semver.max(currentBaseVersion, bumpedFromTag);
        String releaseVersionText = releaseVersion.toString();
        String nextSnapshotVersion = releaseVersionText + "-SNAPSHOT";

        return new ReleasePlan(repoRoot, currentRevision, latestTag, changesets, releaseLevel,
            releaseVersionText, nextSnapshotVersion, changesetConfig.tagStrategy());
    }

    private String latestWholeRepoTag() throws IOException, InterruptedException {
        CommandResult result = ReleaseProcessUtils.runCapture(repoRoot, "git", "tag", "--list", "v*", "--sort=-v:refname");
        int exitCode = result.exitCode;
        if (exitCode != 0) {
            String error = result.stderrText().trim();
            if (error.contains("not a git repository")) {
                return null;
            }
            throw new IllegalStateException("git tag failed: " + error);
        }
        String output = result.stdoutText().trim();
        if (output.isEmpty()) {
            return null;
        }
        return output.split("\\r?\\n")[0].trim();
    }
}
