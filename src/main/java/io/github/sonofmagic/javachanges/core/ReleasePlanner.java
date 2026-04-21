package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.maxReleaseLevel;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.readAllBytes;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.stripSnapshot;

final class ReleasePlanner {
    private final Path repoRoot;

    ReleasePlanner(Path repoRoot) {
        this.repoRoot = repoRoot;
    }

    ReleasePlan plan() throws IOException, InterruptedException {
        String currentRevision = readRevision(repoRoot.resolve("pom.xml"));
        Semver currentBaseVersion = Semver.parse(stripSnapshot(currentRevision));
        List<Changeset> changesets = RepoFiles.loadChangesets(repoRoot);
        String latestTag = latestWholeRepoTag();

        if (changesets.isEmpty()) {
            return new ReleasePlan(repoRoot, currentRevision, latestTag, Collections.<Changeset>emptyList(),
                null, null, currentRevision);
        }

        ReleaseLevel releaseLevel = maxReleaseLevel(changesets);
        Semver latestTagVersion = latestTag == null ? currentBaseVersion : Semver.parse(latestTag.substring(1));
        Semver bumpedFromTag = latestTag == null ? currentBaseVersion.bump(releaseLevel) : latestTagVersion.bump(releaseLevel);
        Semver releaseVersion = Semver.max(currentBaseVersion, bumpedFromTag);
        String releaseVersionText = releaseVersion.toString();
        String nextSnapshotVersion = releaseVersionText + "-SNAPSHOT";

        return new ReleasePlan(repoRoot, currentRevision, latestTag, changesets, releaseLevel,
            releaseVersionText, nextSnapshotVersion);
    }

    private String readRevision(Path pomPath) throws IOException {
        String content = new String(Files.readAllBytes(pomPath), StandardCharsets.UTF_8);
        int start = content.indexOf("<revision>");
        int end = content.indexOf("</revision>");
        if (start < 0 || end < 0 || end <= start) {
            throw new IllegalStateException("Cannot find <revision> in " + pomPath);
        }
        return content.substring(start + "<revision>".length(), end).trim();
    }

    private String latestWholeRepoTag() throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder("git", "tag", "--list", "v*", "--sort=-v:refname");
        builder.directory(repoRoot.toFile());
        Process process = builder.start();
        byte[] stdout = readAllBytes(process.getInputStream());
        byte[] stderr = readAllBytes(process.getErrorStream());
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            String error = new String(stderr, StandardCharsets.UTF_8).trim();
            if (error.contains("not a git repository")) {
                return null;
            }
            throw new IllegalStateException("git tag failed: " + error);
        }
        String output = new String(stdout, StandardCharsets.UTF_8).trim();
        if (output.isEmpty()) {
            return null;
        }
        return output.split("\\r?\\n")[0].trim();
    }
}
