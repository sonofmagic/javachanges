package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.nio.file.Path;

interface MavenCommandProbe {
    boolean fileExists(Path path);

    boolean commandAvailable(Path workingDirectory, String... command) throws IOException, InterruptedException;
}

final class MavenCommand {
    final String command;
    final String source;

    MavenCommand(String command, String source) {
        this.command = command;
        this.source = source;
    }

    String versionLabel() {
        return command + " -q -version";
    }
}
