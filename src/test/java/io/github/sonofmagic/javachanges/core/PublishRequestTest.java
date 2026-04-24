package io.github.sonofmagic.javachanges.core;

import io.github.sonofmagic.javachanges.core.publish.PublishRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublishRequestTest {

    @Test
    void fromOptionsReadsExplicitSnapshotBuildStamp() {
        Map<String, String> options = new LinkedHashMap<String, String>();
        options.put("snapshot", "true");
        options.put("snapshot-build-stamp", "20260420.154500.ci001");

        PublishRequest request = PublishRequest.fromOptions(options, true);

        assertEquals("20260420.154500.ci001", request.snapshotBuildStamp);
    }

    @Test
    void fromOptionsReadsExplicitSnapshotVersionMode() {
        Map<String, String> options = new LinkedHashMap<String, String>();
        options.put("snapshot", "true");
        options.put("snapshot-version-mode", "plain");

        PublishRequest request = PublishRequest.fromOptions(options, true);

        assertEquals(SnapshotVersionMode.PLAIN, request.snapshotVersionMode);
    }

    @Test
    void resolveSnapshotVersionModeFallsBackToConfig(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot.resolve(".changesets"));
        Files.write(repoRoot.resolve(".changesets").resolve("config.json"), (
            "{\n" +
                "  \"snapshotVersionMode\": \"plain\"\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));

        SnapshotVersionMode mode =
            PublishRequest.resolveSnapshotVersionMode(new LinkedHashMap<String, String>(), repoRoot.toString());

        assertEquals(SnapshotVersionMode.PLAIN, mode);
    }

    @Test
    void cliSnapshotVersionModeOverridesConfig(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot.resolve(".changesets"));
        Files.write(repoRoot.resolve(".changesets").resolve("config.json"), (
            "{\n" +
                "  \"snapshotVersionMode\": \"plain\"\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));

        Map<String, String> options = new LinkedHashMap<String, String>();
        options.put("snapshot-version-mode", "stamped");

        SnapshotVersionMode mode = PublishRequest.resolveSnapshotVersionMode(options, repoRoot.toString());

        assertEquals(SnapshotVersionMode.STAMPED, mode);
    }

    @Test
    void shouldDefaultToSnapshotUsesConfiguredSnapshotBranch(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot.resolve(".changesets"));
        Files.write(repoRoot.resolve(".changesets").resolve("config.json"), (
            "{\n" +
                "  \"snapshotBranch\": \"snapshot-dev\"\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));

        assertTrue(PublishRequest.shouldDefaultToSnapshot("snapshot-dev", repoRoot.toString()));
        assertFalse(PublishRequest.shouldDefaultToSnapshot("main", repoRoot.toString()));
    }

    @Test
    void fromOptionsDefaultsToPlainSnapshotOnConfiguredSnapshotBranch(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot.resolve(".changesets"));
        Files.write(repoRoot.resolve(".changesets").resolve("config.json"), (
            "{\n" +
                "  \"snapshotBranch\": \"snapshot-dev\",\n" +
                "  \"snapshotVersionMode\": \"plain\"\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));

        Map<String, String> options = new LinkedHashMap<String, String>();
        options.put("directory", repoRoot.toString());

        PublishRequest request = PublishRequest.fromOptions(options, true, "snapshot-dev");

        assertTrue(request.snapshot);
        assertEquals(SnapshotVersionMode.PLAIN, request.snapshotVersionMode);
    }

    @Test
    void fromOptionsReadsJsonFormatForPublish() {
        Map<String, String> options = new LinkedHashMap<String, String>();
        options.put("tag", "v1.2.3");
        options.put("format", "json");

        PublishRequest request = PublishRequest.fromOptions(options, true);

        assertEquals(OutputFormat.JSON, request.format);
        assertEquals("v1.2.3", request.tag);
        assertFalse(request.snapshot);
        assertFalse(request.execute);
    }
}
