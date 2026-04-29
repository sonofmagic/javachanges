package io.github.sonofmagic.javachanges.core.env;

import io.github.sonofmagic.javachanges.core.ReleaseMessages;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class ReleaseEnvInitSupport {
    private final ReleaseEnvRuntime runtime;
    private final PrintStream out;

    ReleaseEnvInitSupport(ReleaseEnvRuntime runtime, PrintStream out) {
        this.runtime = runtime;
        this.out = out;
    }

    void initEnv(InitEnvRequest request) throws IOException {
        Path template = runtime.resolvePath(request.template);
        Path target = runtime.resolvePath(request.target);
        if (!Files.exists(template)) {
            throw new IllegalStateException(ReleaseMessages.templateFileNotFound(runtime.relativizePath(template)));
        }
        if (target.getFileName().toString().endsWith(".example")) {
            throw new IllegalStateException(ReleaseMessages.targetFileCannotBeExample(runtime.relativizePath(target)));
        }
        if (Files.exists(target) && !request.force) {
            out.println(ReleaseMessages.targetFileKept(runtime.relativizePath(target)));
            out.println(ReleaseMessages.recreateEnvFileCommand(runtime.relativizePath(target)));
            return;
        }
        Path parent = target.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        Files.write(target, Files.readAllLines(template, StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        try {
            target.toFile().setReadable(false, false);
            target.toFile().setReadable(true, true);
            target.toFile().setWritable(false, false);
            target.toFile().setWritable(true, true);
        } catch (SecurityException ignored) {
        }
        out.println(ReleaseMessages.generatedLocalEnvFile(runtime.relativizePath(target)));
        out.println(ReleaseMessages.editEnvFileNextStep());
    }
}
