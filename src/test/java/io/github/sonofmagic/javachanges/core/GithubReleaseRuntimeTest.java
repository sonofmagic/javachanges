package io.github.sonofmagic.javachanges.core;

import io.github.sonofmagic.javachanges.core.github.GithubReleaseRuntime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;

class GithubReleaseRuntimeTest {

    @Test
    void remoteTagExistsFailsClosedWhenRemoteLookupFails(@TempDir Path tempDir) {
        GithubReleaseRuntime runtime = new GithubReleaseRuntime(tempDir);
        Path missingRemote = tempDir.resolve("missing.git");

        assertThrows(IllegalStateException.class,
            () -> runtime.remoteTagExists("v1.2.0", missingRemote.toString()));
    }
}
