package io.github.sonofmagic.javachanges.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReleaseArtifactSupportTest {

    @Test
    void describeTagSupportsWholeRepoTag(@TempDir Path tempDir) {
        ReleaseArtifactSupport support = new ReleaseArtifactSupport(tempDir);

        ReleaseArtifactSupport.ReleaseTagInfo tagInfo = support.describeTag("v1.2.3");

        assertEquals("v1.2.3", tagInfo.tag);
        assertEquals("1.2.3", tagInfo.releaseVersion);
        assertEquals(null, tagInfo.releaseModule);
        assertEquals("v1.2.3", tagInfo.releaseDisplayName());
    }

    @Test
    void describeTagSupportsModuleTagAndDefaultNotesPath(@TempDir Path tempDir) {
        ReleaseArtifactSupport support = new ReleaseArtifactSupport(tempDir);

        ReleaseArtifactSupport.ReleaseTagInfo tagInfo = support.describeTag("demo-app/v2.0.0");
        Path notesFile = support.resolveReleaseNotesFile(null);

        assertEquals("demo-app", tagInfo.releaseModule);
        assertEquals("2.0.0", tagInfo.releaseVersion);
        assertEquals("demo-app v2.0.0", tagInfo.releaseDisplayName());
        assertEquals(tempDir.resolve("target").resolve("release-notes.md").normalize(), notesFile);
    }
}
