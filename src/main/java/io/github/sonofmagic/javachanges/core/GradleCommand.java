package io.github.sonofmagic.javachanges.core;

public final class GradleCommand {
    public final String command;
    public final String source;

    GradleCommand(String command, String source) {
        this.command = command;
        this.source = source;
    }
}
