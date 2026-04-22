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
            ("{\n" +
                "  \"releaseVersion\": \"1.2.3\",\n" +
                "  \"tagStrategy\": \"per-module\",\n" +
                "  \"releaseTargets\": [\n" +
                "    {\"module\": \"core\", \"tag\": \"core/v1.2.3\"},\n" +
                "    {\"module\": \"starter\", \"tag\": \"starter/v1.2.3\"}\n" +
                "  ]\n" +
                "}\n").getBytes(StandardCharsets.UTF_8)
        );

        ReleaseAutomationSupport support = new ReleaseAutomationSupport(repoRoot);
        ReleaseAutomationSupport.ReleaseDescriptor release = support.descriptorFromManifest();

        assertEquals("1.2.3", support.releaseVersionFromManifest());
        assertEquals("v1.2.3", support.wholeRepoTagFromManifest());
        assertEquals("1.2.3", release.releaseVersion);
        assertEquals(ReleaseTagStrategy.PER_MODULE, release.tagStrategy);
        assertEquals("v1.2.3", release.wholeRepoTagName());
        assertEquals("core/v1.2.3", release.tagNames().get(0));
        assertEquals("starter/v1.2.3", release.tagNames().get(1));
        assertEquals("chore(release): apply changesets for v1.2.3", release.commitMessage());
        assertEquals("chore(release): v1.2.3", release.githubPullRequestTitle());
        assertEquals("chore(release): release v1.2.3", release.gitlabMergeRequestTitle());
        assertTrue(support.releasePlanMarkdownFile().endsWith(".changesets/release-plan.md"));
    }
}
