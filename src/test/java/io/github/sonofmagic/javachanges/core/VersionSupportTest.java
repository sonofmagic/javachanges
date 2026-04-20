package io.github.sonofmagic.javachanges.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VersionSupportTest {

    @Test
    void snapshotPublishVersionUsesUniqueBuildStamp(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, "1.2.3-SNAPSHOT");

        VersionSupport support = new VersionSupport(repoRoot);

        assertEquals("1.2.3-20260420.154500.abc1234-SNAPSHOT",
            support.resolveSnapshotPublishVersion("20260420.154500.abc1234"));
    }

    @Test
    void snapshotPublishVersionRejectsReleaseRevision(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, "1.2.3");

        VersionSupport support = new VersionSupport(repoRoot);

        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> support.resolveSnapshotPublishVersion("20260420.154500.abc1234"));
        assertEquals("当前项目版本不是 SNAPSHOT: 1.2.3", error.getMessage());
    }

    @Test
    void snapshotPublishVersionRejectsInvalidBuildStamp(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, "1.2.3-SNAPSHOT");

        VersionSupport support = new VersionSupport(repoRoot);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
            () -> support.resolveSnapshotPublishVersion("2026-04-20 sha"));
        assertEquals("snapshot build stamp 只允许字母、数字和点号: 2026-04-20 sha", error.getMessage());
    }

    private static Path createRepository(Path tempDir, String revision) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot);
        Files.write(repoRoot.resolve("pom.xml"), pom(revision).getBytes(StandardCharsets.UTF_8));
        return repoRoot;
    }

    private static String pom(String revision) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n"
            + "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
            + "    <modelVersion>4.0.0</modelVersion>\n"
            + "    <groupId>example</groupId>\n"
            + "    <artifactId>fixture-app</artifactId>\n"
            + "    <version>${revision}</version>\n"
            + "    <properties>\n"
            + "        <revision>" + revision + "</revision>\n"
            + "    </properties>\n"
            + "</project>\n";
    }
}
