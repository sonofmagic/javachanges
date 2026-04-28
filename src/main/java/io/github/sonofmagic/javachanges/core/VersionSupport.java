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
            throw new IllegalStateException("未在 Maven pom.xml 或 Gradle gradle.properties 中找到版本配置", exception);
        }
    }

    public void assertSnapshot() throws IOException {
        String version = readRevision();
        if (!version.endsWith("-SNAPSHOT")) {
            throw new IllegalStateException("当前项目版本不是 SNAPSHOT: " + version);
        }
    }

    public String resolveSnapshotPublishVersion(String buildStamp) throws IOException {
        String version = readRevision();
        if (!version.endsWith("-SNAPSHOT")) {
            throw new IllegalStateException("当前项目版本不是 SNAPSHOT: " + version);
        }
        String normalizedBuildStamp = normalizeSnapshotBuildStamp(buildStamp);
        return stripSnapshot(version) + "-" + normalizedBuildStamp + "-SNAPSHOT";
    }

    public String snapshotRevision() throws IOException {
        String version = readRevision();
        if (!version.endsWith("-SNAPSHOT")) {
            throw new IllegalStateException("当前项目版本不是 SNAPSHOT: " + version);
        }
        return version;
    }

    private String normalizeSnapshotBuildStamp(String buildStamp) {
        String normalized = buildStamp == null ? "" : buildStamp.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("snapshot build stamp 不能为空");
        }
        if (!normalized.matches("[A-Za-z0-9.]+")) {
            throw new IllegalArgumentException("snapshot build stamp 只允许字母、数字和点号: " + buildStamp);
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
            throw new IllegalStateException("tag " + tag + " 与项目版本 " + version + " 不匹配，期望基础版本为 " + releaseVersion);
        }
    }
}
