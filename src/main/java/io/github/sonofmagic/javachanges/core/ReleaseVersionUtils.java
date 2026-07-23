package io.github.sonofmagic.javachanges.core;

import java.util.List;

public final class ReleaseVersionUtils {
    private ReleaseVersionUtils() {
    }

    public static String releaseVersionForChanges(String currentRevision, String latestTag, ReleaseLevel releaseLevel) {
        return releaseVersionForChanges(currentRevision, latestTag, releaseLevel, "");
    }

    public static String releaseVersionForChanges(String currentRevision, String latestTag, ReleaseLevel releaseLevel,
                                                  String releaseVersionSuffix) {
        Semver currentBaseVersion = Semver.parse(semanticVersion(currentRevision, releaseVersionSuffix));
        Semver latestTagVersion = latestTag == null
            ? currentBaseVersion
            : Semver.parse(semanticVersionFromWholeRepoTag(latestTag, releaseVersionSuffix));
        Semver bumpedFromTag = latestTag == null ? currentBaseVersion.bump(releaseLevel) : latestTagVersion.bump(releaseLevel);
        return Semver.max(currentBaseVersion, bumpedFromTag).toString();
    }

    public static String semanticVersion(String version, String releaseVersionSuffix) {
        String normalized = ReleaseTextUtils.stripSnapshot(version);
        String suffix = normalizedSuffix(releaseVersionSuffix);
        if (!suffix.isEmpty() && normalized.endsWith(suffix)) {
            return normalized.substring(0, normalized.length() - suffix.length());
        }
        return normalized;
    }

    public static String releasePublishVersion(String semanticVersion, String releaseVersionSuffix) {
        return semanticVersion + normalizedSuffix(releaseVersionSuffix);
    }

    public static String latestWholeRepoTag(List<String> tags, String releaseVersionSuffix) {
        String latestTag = null;
        Semver latestVersion = null;
        for (String tag : tags) {
            try {
                Semver candidate = Semver.parse(semanticVersionFromWholeRepoTag(tag, releaseVersionSuffix));
                if (latestVersion == null || candidate.compareTo(latestVersion) > 0) {
                    latestTag = tag;
                    latestVersion = candidate;
                }
            } catch (IllegalArgumentException ignored) {
                // Ignore tags that do not belong to the configured whole-repository release scheme.
            }
        }
        return latestTag;
    }

    static String semanticVersionFromWholeRepoTag(String tag, String releaseVersionSuffix) {
        String normalized = ReleaseTextUtils.trimToNull(tag);
        if (normalized == null || normalized.indexOf('/') >= 0) {
            throw new IllegalArgumentException(ReleaseMessages.unsupportedVersion(tag));
        }
        if (normalized.startsWith("v")) {
            return normalized.substring(1);
        }
        String suffix = normalizedSuffix(releaseVersionSuffix);
        if (!suffix.isEmpty() && normalized.endsWith(suffix)) {
            return normalized.substring(0, normalized.length() - suffix.length());
        }
        throw new IllegalArgumentException(ReleaseMessages.unsupportedVersion(tag));
    }

    private static String normalizedSuffix(String releaseVersionSuffix) {
        String suffix = ReleaseTextUtils.trimToNull(releaseVersionSuffix);
        return suffix == null ? "" : suffix;
    }
}
