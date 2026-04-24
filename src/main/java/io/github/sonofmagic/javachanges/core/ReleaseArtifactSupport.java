package io.github.sonofmagic.javachanges.core;

import java.nio.file.Path;
import java.nio.file.Paths;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.releaseModuleFromTag;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.releaseVersionFromTag;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.trimToNull;

public final class ReleaseArtifactSupport {
    private final Path repoRoot;

    public ReleaseArtifactSupport(Path repoRoot) {
        this.repoRoot = repoRoot;
    }

    public ReleaseTagInfo describeTag(String tag) {
        return new ReleaseTagInfo(tag, releaseVersionFromTag(tag), releaseModuleFromTag(tag));
    }

    public Path resolveReleaseNotesFile(String value) {
        String resolved = trimToNull(value);
        if (resolved == null) {
            return repoRoot.resolve("target").resolve("release-notes.md").normalize();
        }
        return resolvePath(resolved);
    }

    public Path resolvePath(String value) {
        Path path = Paths.get(value);
        return path.isAbsolute() ? path.normalize() : repoRoot.resolve(path).normalize();
    }

    public Path resolveOptionalPath(String value) {
        String resolved = trimToNull(value);
        return resolved == null ? null : resolvePath(resolved);
    }

    public static final class ReleaseTagInfo {
        public final String tag;
        public final String releaseVersion;
        public final String releaseModule;

        ReleaseTagInfo(String tag, String releaseVersion, String releaseModule) {
            this.tag = tag;
            this.releaseVersion = releaseVersion;
            this.releaseModule = releaseModule;
        }

        public String releaseDisplayName() {
            return releaseModule == null ? "v" + releaseVersion : releaseModule + " v" + releaseVersion;
        }
    }
}
