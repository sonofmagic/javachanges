package io.github.sonofmagic.javachanges.core;

import org.junit.jupiter.api.Test;

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
}
