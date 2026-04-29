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
            throw new IllegalStateException(ReleaseMessages.text(
                "Template file not found: " + runtime.relativizePath(template),
                "未找到模板文件: " + runtime.relativizePath(template)
            ));
        }
        if (target.getFileName().toString().endsWith(".example")) {
            throw new IllegalStateException(ReleaseMessages.text(
                "Target file cannot be an example file: " + runtime.relativizePath(target),
                "目标文件不能是示例文件: " + runtime.relativizePath(target)
            ));
        }
        if (Files.exists(target) && !request.force) {
            out.println(ReleaseMessages.text(
                "Target file already exists; kept unchanged: " + runtime.relativizePath(target),
                "目标文件已存在，未做覆盖: " + runtime.relativizePath(target)
            ));
            out.println(ReleaseMessages.text(
                "To recreate it, run: make env-init RELEASE_ENV_FILE=" + runtime.relativizePath(target) + " FORCE=true",
                "如果你确实要重建，请执行: make env-init RELEASE_ENV_FILE=" + runtime.relativizePath(target) + " FORCE=true"
            ));
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
        out.println(ReleaseMessages.text(
            "Generated local env file: " + runtime.relativizePath(target),
            "已生成本地 env 文件: " + runtime.relativizePath(target)
        ));
        out.println(ReleaseMessages.text(
            "Next, edit the real repository addresses and credentials, then run: make readiness",
            "下一步请编辑真实仓库地址和凭据，然后执行: make readiness"
        ));
    }
}
