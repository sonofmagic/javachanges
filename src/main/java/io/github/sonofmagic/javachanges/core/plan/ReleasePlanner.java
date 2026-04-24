package io.github.sonofmagic.javachanges.core.plan;

import io.github.sonofmagic.javachanges.core.ChangesetConfigSupport;
import io.github.sonofmagic.javachanges.core.CommandResult;
import io.github.sonofmagic.javachanges.core.ReleaseLevel;
import io.github.sonofmagic.javachanges.core.ReleaseUtils;
import io.github.sonofmagic.javachanges.core.changeset.Changeset;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public final class ReleasePlanner {
    private final Path repoRoot;

    public ReleasePlanner(Path repoRoot) {
        this.repoRoot = repoRoot;
    }

    public ReleasePlan plan() throws IOException, InterruptedException {
        String currentRevision = ReleaseUtils.readRevision(repoRoot.resolve("pom.xml"));
        List<Changeset> changesets = RepoFiles.loadChangesets(repoRoot);
        String latestTag = latestWholeRepoTag();
        ChangesetConfigSupport.ChangesetConfig changesetConfig = RepoFiles.readChangesetConfig(repoRoot);

        if (changesets.isEmpty()) {
            return new ReleasePlan(repoRoot, currentRevision, latestTag, Collections.<Changeset>emptyList(),
                null, null, currentRevision, changesetConfig.tagStrategy());
        }

        ReleaseLevel releaseLevel = ReleaseUtils.maxReleaseLevel(changesets);
        String releaseVersionText = ReleaseUtils.releaseVersionForChanges(currentRevision, latestTag, releaseLevel);
        String nextSnapshotVersion = releaseVersionText + "-SNAPSHOT";

        return new ReleasePlan(repoRoot, currentRevision, latestTag, changesets, releaseLevel,
            releaseVersionText, nextSnapshotVersion, changesetConfig.tagStrategy());
    }

    private String latestWholeRepoTag() throws IOException, InterruptedException {
        CommandResult result = ReleaseUtils.runCapture(repoRoot, "git", "tag", "--list", "v*", "--sort=-v:refname");
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
