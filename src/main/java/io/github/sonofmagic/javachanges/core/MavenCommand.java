package io.github.sonofmagic.javachanges.core;

public final class MavenCommand {
    public final String command;
    public final String source;

    MavenCommand(String command, String source) {
        this.command = command;
        this.source = source;
    }

    public String versionLabel() {
        return command + " -q -version";
    }
}
