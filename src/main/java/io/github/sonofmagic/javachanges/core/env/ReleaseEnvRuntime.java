package io.github.sonofmagic.javachanges.core.env;

import io.github.sonofmagic.javachanges.core.CommandResult;
import io.github.sonofmagic.javachanges.core.ReleaseProcessUtils;
import io.github.sonofmagic.javachanges.core.ReleaseTextUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class ReleaseEnvRuntime {
    private final Path repoRoot;

    public ReleaseEnvRuntime(Path repoRoot) {
        this.repoRoot = repoRoot;
    }

    public Path resolveEnvFile(String envFile) throws IOException {
        Path path = resolvePath(envFile);
        if (!Files.exists(path)) {
            throw new IllegalStateException("未找到 env 文件: " + relativizePath(path));
        }
        if (isExampleFile(path)) {
            throw new IllegalStateException("请不要直接使用示例文件: " + relativizePath(path));
        }
        return path;
    }

    public Path resolvePath(String value) {
        Path path = Paths.get(value);
        if (!path.isAbsolute()) {
            path = repoRoot.resolve(path).normalize();
        }
        return path;
    }

    public boolean isExampleFile(Path path) {
        return path.getFileName().toString().endsWith(".example");
    }

    public boolean commandAvailable(String... command) throws InterruptedException {
        try {
            return runQuietly(Arrays.asList(command));
        } catch (IOException exception) {
            return false;
        }
    }

    public boolean commandExists(String command) {
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(repoRoot.toFile());
            builder.redirectErrorStream(true);
            Process process = builder.start();
            ReleaseProcessUtils.closeQuietly(process.getInputStream());
            process.destroy();
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    public void requireCommand(String command) {
        if (!commandExists(command)) {
            throw new IllegalStateException("未找到 " + command + " CLI");
        }
    }

    public boolean runQuietly(List<String> command) throws IOException, InterruptedException {
        CommandResult result = runAndCapture(command);
        return result.exitCode == 0;
    }

    public CommandResult runAndCapture(List<String> command) throws IOException, InterruptedException {
        return ReleaseProcessUtils.runCapture(repoRoot, command);
    }

    public String relativizePath(Path path) {
        Path normalizedRoot = repoRoot.toAbsolutePath().normalize();
        Path normalizedPath = path.toAbsolutePath().normalize();
        if (normalizedPath.startsWith(normalizedRoot)) {
            return normalizedRoot.relativize(normalizedPath).toString();
        }
        return normalizedPath.toString();
    }

    public String repoFlagPreview(String repo) {
        if (ReleaseTextUtils.isBlank(repo)) {
            return "";
        }
        return " --repo " + repo;
    }
}
