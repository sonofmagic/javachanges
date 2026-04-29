package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.nio.file.Path;

import static io.github.sonofmagic.javachanges.core.ReleaseModuleUtils.assertKnownModule;
import static io.github.sonofmagic.javachanges.core.ReleaseModuleUtils.releaseModuleFromTag;
import static io.github.sonofmagic.javachanges.core.ReleaseModuleUtils.releaseVersionFromTag;
import static io.github.sonofmagic.javachanges.core.ReleaseTextUtils.stripSnapshot;

public final class VersionSupport {
    private final Path repoRoot;

    public VersionSupport(Path repoRoot) {
        this.repoRoot = repoRoot;
    }

    public String readRevision() throws IOException {
        try {
            return BuildModelSupport.readRevision(repoRoot);
        } catch (IllegalStateException exception) {
            throw new IllegalStateException(ReleaseMessages.missingVersionConfig(), exception);
        }
    }

    public void assertSnapshot() throws IOException {
        String version = readRevision();
        if (!version.endsWith("-SNAPSHOT")) {
            throw new IllegalStateException(ReleaseMessages.notSnapshot(version));
        }
    }

    public String resolveSnapshotPublishVersion(String buildStamp) throws IOException {
        String version = readRevision();
        if (!version.endsWith("-SNAPSHOT")) {
            throw new IllegalStateException(ReleaseMessages.notSnapshot(version));
        }
        String normalizedBuildStamp = normalizeSnapshotBuildStamp(buildStamp);
        return stripSnapshot(version) + "-" + normalizedBuildStamp + "-SNAPSHOT";
    }

    public String snapshotRevision() throws IOException {
        String version = readRevision();
        if (!version.endsWith("-SNAPSHOT")) {
            throw new IllegalStateException(ReleaseMessages.notSnapshot(version));
        }
        return version;
    }

    private String normalizeSnapshotBuildStamp(String buildStamp) {
        String normalized = buildStamp == null ? "" : buildStamp.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(ReleaseMessages.emptySnapshotBuildStamp());
        }
        if (!normalized.matches("[A-Za-z0-9.]+")) {
            throw new IllegalArgumentException(ReleaseMessages.invalidSnapshotBuildStamp(buildStamp));
        }
        return normalized;
    }

    public void assertReleaseTag(String tag) throws IOException {
        String version = readRevision();
        String releaseVersion = releaseVersionFromTag(tag);
        String module = releaseModuleFromTag(tag);
        String baseVersion = stripSnapshot(version);
        if (module != null) {
            assertKnownModule(repoRoot, module);
        }
        if (!baseVersion.equals(releaseVersion)) {
            throw new IllegalStateException(ReleaseMessages.releaseTagVersionMismatch(tag, version, releaseVersion));
        }
    }
}
