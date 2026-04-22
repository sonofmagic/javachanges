package io.github.sonofmagic.javachanges.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReleaseAutomationSupportTest {

    @Test
    void exposesManifestTagAndReleasePlanPath(@TempDir Path tempDir) throws IOException {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot.resolve(".changesets"));
        Files.write(
            repoRoot.resolve(".changesets").resolve("release-plan.json"),
            "{\"releaseVersion\":\"1.2.3\"}\n".getBytes(StandardCharsets.UTF_8)
        );

        ReleaseAutomationSupport support = new ReleaseAutomationSupport(repoRoot);

        assertEquals("1.2.3", support.releaseVersionFromManifest());
        assertEquals("v1.2.3", support.wholeRepoTagFromManifest());
        assertTrue(support.releasePlanMarkdownFile().endsWith(".changesets/release-plan.md"));
    }
}
