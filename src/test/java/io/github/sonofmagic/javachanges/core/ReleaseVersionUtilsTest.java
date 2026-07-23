package io.github.sonofmagic.javachanges.core;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReleaseVersionUtilsTest {

    @Test
    void usesBumpedCurrentVersionWhenNoPreviousTagExists() {
        assertEquals("1.3.0",
            ReleaseVersionUtils.releaseVersionForChanges("1.2.3-SNAPSHOT", null, ReleaseLevel.MINOR));
    }

    @Test
    void keepsCurrentBaseVersionWhenItIsAheadOfLatestTag() {
        assertEquals("1.5.0",
            ReleaseVersionUtils.releaseVersionForChanges("1.5.0-SNAPSHOT", "v1.4.9", ReleaseLevel.PATCH));
    }

    @Test
    void bumpsReleaseQualifiedVersionUsingConfiguredSuffix() {
        assertEquals("1.2.3", ReleaseVersionUtils.releaseVersionForChanges(
            "1.2.2-RELEASE", "1.2.2-RELEASE", ReleaseLevel.PATCH, "-RELEASE"));
    }

    @Test
    void selectsNewestStandardOrReleaseQualifiedWholeRepoTag() {
        assertEquals("v1.3.0", ReleaseVersionUtils.latestWholeRepoTag(
            Arrays.asList("fixture/v9.0.0", "1.2.2-RELEASE", "v1.3.0", "random"), "-RELEASE"));
    }

    @Test
    void formatsReleaseArtifactVersionSeparatelyFromSemanticVersion() {
        assertEquals("1.2.3-RELEASE", ReleaseVersionUtils.releasePublishVersion("1.2.3", "-RELEASE"));
    }
}
