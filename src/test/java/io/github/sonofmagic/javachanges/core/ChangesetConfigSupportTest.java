package io.github.sonofmagic.javachanges.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChangesetConfigSupportTest {

    @Test
    void loadsDefaultsWhenConfigMissing(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot.resolve(".changesets"));

        ChangesetConfigSupport.ChangesetConfig config = ChangesetConfigSupport.load(repoRoot);

        assertEquals("main", config.baseBranch());
        assertEquals("changeset-release/main", config.releaseBranch());
        assertEquals("snapshot", config.snapshotBranch());
    }

    @Test
    void loadsConfiguredBranches(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Path changesetsDir = repoRoot.resolve(".changesets");
        Files.createDirectories(changesetsDir);
        Files.write(changesetsDir.resolve("config.json"), (
            "{\n" +
                "  \"baseBranch\": \"develop\",\n" +
                "  \"releaseBranch\": \"changeset-release/develop\",\n" +
                "  \"snapshotBranch\": \"snapshot-dev\"\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));

        ChangesetConfigSupport.ChangesetConfig config = ChangesetConfigSupport.load(repoRoot);

        assertEquals("develop", config.baseBranch());
        assertEquals("changeset-release/develop", config.releaseBranch());
        assertEquals("snapshot-dev", config.snapshotBranch());
    }

    @Test
    void gitlabReleasePlanRequestFallsBackToChangesetConfig(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Path changesetsDir = repoRoot.resolve(".changesets");
        Files.createDirectories(changesetsDir);
        Files.write(changesetsDir.resolve("config.json"), (
            "{\n" +
                "  \"baseBranch\": \"develop\",\n" +
                "  \"releaseBranch\": \"release/from-config\"\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));

        Map<String, String> options = new LinkedHashMap<String, String>();
        options.put("directory", repoRoot.toString());

        GitlabReleasePlanRequest request = GitlabReleasePlanRequest.fromOptions(options);

        assertEquals("develop", request.targetBranch);
        assertEquals("release/from-config", request.releaseBranch);
    }
}
