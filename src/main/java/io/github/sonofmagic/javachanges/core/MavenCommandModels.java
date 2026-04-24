package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.nio.file.Path;

interface MavenCommandProbe {
    boolean fileExists(Path path);

    boolean commandAvailable(Path workingDirectory, String... command) throws IOException, InterruptedException;
}
