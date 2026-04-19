package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.gitTextAllowEmpty;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.readAllBytes;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.trimToNull;

final class PublishRuntime {
    private final Path repoRoot;

    PublishRuntime(Path repoRoot) {
        this.repoRoot = repoRoot;
    }

    boolean hasDirtyWorktree() throws IOException, InterruptedException {
        return !gitTextAllowEmpty(repoRoot, "status", "--short").trim().isEmpty();
    }

    boolean gitRefExists(String ref) throws IOException, InterruptedException {
        List<String> command = Arrays.asList("git", "rev-parse", ref);
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(repoRoot.toFile());
        Process process = builder.start();
        readAllBytes(process.getInputStream());
        readAllBytes(process.getErrorStream());
        return process.waitFor() == 0;
    }

    Path ensureLocalMavenRepositoryDirectory() throws IOException {
        Path defaultLocalRepo = repoRoot.resolve(".m2/repository").normalize();
        Files.createDirectories(defaultLocalRepo);

        String mavenOpts = trimToNull(System.getenv("MAVEN_OPTS"));
        if (mavenOpts == null) {
            return defaultLocalRepo;
        }

        Matcher matcher = Pattern.compile("(?:^|\\s)-Dmaven\\.repo\\.local=([^\\s]+)").matcher(mavenOpts);
        if (!matcher.find()) {
            return defaultLocalRepo;
        }

        String configuredPath = matcher.group(1);
        if ((configuredPath.startsWith("\"") && configuredPath.endsWith("\""))
            || (configuredPath.startsWith("'") && configuredPath.endsWith("'"))) {
            configuredPath = configuredPath.substring(1, configuredPath.length() - 1);
        }

        Path localRepoPath = Paths.get(configuredPath);
        if (!localRepoPath.isAbsolute()) {
            localRepoPath = repoRoot.resolve(localRepoPath).normalize();
        }
        Files.createDirectories(localRepoPath);
        return localRepoPath;
    }
}
