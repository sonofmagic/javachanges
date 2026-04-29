package io.github.sonofmagic.javachanges.core;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentationConsistencyTest {
    private static final Pattern SNAPSHOT_PLUGIN_COORDINATE = Pattern.compile(
        "io\\.github\\.sonofmagic:javachanges:([^:\\s]+-SNAPSHOT)"
    );
    private static final Pattern CURRENT_SNAPSHOT_REFERENCE = Pattern.compile(
        "current `([^`]+-SNAPSHOT)`|当前 `([^`]+-SNAPSHOT)`"
    );

    @Test
    void readmeSnapshotPluginExamplesMatchProjectRevision() throws Exception {
        Path repoRoot = Paths.get("").toAbsolutePath().normalize();
        String revision = PomModelSupport.readRevision(repoRoot.resolve("pom.xml"));

        assertReadmeSnapshotExamplesMatch(repoRoot.resolve("README.md"), revision);
        assertReadmeSnapshotExamplesMatch(repoRoot.resolve("README.zh-CN.md"), revision);
    }

    private static void assertReadmeSnapshotExamplesMatch(Path readme, String revision) throws Exception {
        String content = new String(Files.readAllBytes(readme), StandardCharsets.UTF_8);
        Matcher matcher = SNAPSHOT_PLUGIN_COORDINATE.matcher(content);
        int count = 0;
        while (matcher.find()) {
            count++;
            assertEquals(revision, matcher.group(1), readme + " contains stale snapshot plugin version");
        }
        assertTrue(count > 0, readme + " should include at least one snapshot plugin example");

        matcher = CURRENT_SNAPSHOT_REFERENCE.matcher(content);
        count = 0;
        while (matcher.find()) {
            count++;
            String snapshotVersion = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            assertEquals(revision, snapshotVersion, readme + " contains stale current snapshot version");
        }
        assertTrue(count > 0, readme + " should include at least one current snapshot reference");
    }
}
