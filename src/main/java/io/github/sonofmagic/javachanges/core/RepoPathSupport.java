package io.github.sonofmagic.javachanges.core;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class RepoPathSupport {
    private RepoPathSupport() {
    }

    public static Path resolveOutputPath(Path repoRoot, String value, String optionName) {
        Path root = repoRoot.toAbsolutePath().normalize();
        Path input = Paths.get(value);
        Path target = input.isAbsolute() ? input.normalize() : root.resolve(input).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException(optionName + " must stay inside repository: " + value);
        }
        return target;
    }
}
