package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.closeQuietly;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.isBlank;

class ReleaseEnvRuntime {
    private final Path repoRoot;

    ReleaseEnvRuntime(Path repoRoot) {
        this.repoRoot = repoRoot;
    }

    Path resolveEnvFile(String envFile) throws IOException {
        Path path = resolvePath(envFile);
        if (!Files.exists(path)) {
            throw new IllegalStateException("未找到 env 文件: " + relativizePath(path));
        }
        if (isExampleFile(path)) {
            throw new IllegalStateException("请不要直接使用示例文件: " + relativizePath(path));
        }
        return path;
    }

    Path resolvePath(String value) {
        Path path = Paths.get(value);
        if (!path.isAbsolute()) {
            path = repoRoot.resolve(path).normalize();
        }
        return path;
    }

    boolean isExampleFile(Path path) {
        return path.getFileName().toString().endsWith(".example");
    }

    boolean commandAvailable(String... command) throws InterruptedException {
        try {
            return runQuietly(Arrays.asList(command));
        } catch (IOException exception) {
            return false;
        }
    }

    boolean commandExists(String command) {
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(repoRoot.toFile());
            builder.redirectErrorStream(true);
            Process process = builder.start();
            closeQuietly(process.getInputStream());
            process.destroy();
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    void requireCommand(String command) {
        if (!commandExists(command)) {
            throw new IllegalStateException("未找到 " + command + " CLI");
        }
    }

    boolean runQuietly(List<String> command) throws IOException, InterruptedException {
        CommandResult result = runAndCapture(command);
        return result.exitCode == 0;
    }

    CommandResult runAndCapture(List<String> command) throws IOException, InterruptedException {
        return ReleaseProcessUtils.runCapture(repoRoot, command);
    }

    String relativizePath(Path path) {
        Path normalizedRoot = repoRoot.toAbsolutePath().normalize();
        Path normalizedPath = path.toAbsolutePath().normalize();
        if (normalizedPath.startsWith(normalizedRoot)) {
            return normalizedRoot.relativize(normalizedPath).toString();
        }
        return normalizedPath.toString();
    }

    String repoFlagPreview(String repo) {
        if (isBlank(repo)) {
            return "";
        }
        return " --repo " + repo;
    }
}
