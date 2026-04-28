package io.github.sonofmagic.javachanges.core.config;

import io.github.sonofmagic.javachanges.core.BuildModelSupport;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class RepoRootResolver {
    private RepoRootResolver() {
    }

    public static Path resolveRepoRoot(String directoryOption) {
        Path current = directoryOption == null
            ? Paths.get("").toAbsolutePath().normalize()
            : Paths.get(directoryOption).toAbsolutePath().normalize();

        Path probe = current;
        while (probe != null) {
            if (BuildModelSupport.isSupportedRepository(probe)) {
                return probe;
            }
            probe = probe.getParent();
        }
        throw new IllegalStateException("Cannot find repository root from " + current);
    }
}
