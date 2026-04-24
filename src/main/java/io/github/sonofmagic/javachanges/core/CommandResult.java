package io.github.sonofmagic.javachanges.core;

import java.nio.charset.StandardCharsets;

public final class CommandResult {
    public final int exitCode;
    private final byte[] stdout;
    private final byte[] stderr;

    public CommandResult(int exitCode, byte[] stdout, byte[] stderr) {
        this.exitCode = exitCode;
        this.stdout = stdout;
        this.stderr = stderr;
    }

    public String stdoutText() {
        return new String(stdout, StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unused")
    public String stderrText() {
        return new String(stderr, StandardCharsets.UTF_8);
    }
}
