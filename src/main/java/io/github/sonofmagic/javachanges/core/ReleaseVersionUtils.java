package io.github.sonofmagic.javachanges.core;

public final class ReleaseVersionUtils {
    private ReleaseVersionUtils() {
    }

    public static String releaseVersionForChanges(String currentRevision, String latestTag, ReleaseLevel releaseLevel) {
        Semver currentBaseVersion = Semver.parse(ReleaseTextUtils.stripSnapshot(currentRevision));
        Semver latestTagVersion = latestTag == null ? currentBaseVersion : Semver.parse(latestTag.substring(1));
        Semver bumpedFromTag = latestTag == null ? currentBaseVersion.bump(releaseLevel) : latestTagVersion.bump(releaseLevel);
        return Semver.max(currentBaseVersion, bumpedFromTag).toString();
    }
}
