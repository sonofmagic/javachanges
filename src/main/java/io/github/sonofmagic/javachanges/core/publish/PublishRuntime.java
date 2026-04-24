package io.github.sonofmagic.javachanges.core.publish;

import io.github.sonofmagic.javachanges.core.CommandResult;
import io.github.sonofmagic.javachanges.core.ReleaseUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PublishRuntime {
    private static final DateTimeFormatter SNAPSHOT_TIMESTAMP =
        DateTimeFormatter.ofPattern("yyyyMMdd.HHmmss").withZone(ZoneOffset.UTC);
    private final Path repoRoot;

    public PublishRuntime(Path repoRoot) {
        this.repoRoot = repoRoot;
    }

    public boolean hasDirtyWorktree() throws IOException, InterruptedException {
        return !ReleaseUtils.gitTextAllowEmpty(repoRoot, "status", "--short").trim().isEmpty();
    }

    public boolean gitRefExists(String ref) throws IOException, InterruptedException {
        return ReleaseUtils.runCapture(repoRoot, "git", "rev-parse", ref).exitCode == 0;
    }

    public String snapshotBuildStamp() throws IOException, InterruptedException {
        String timestamp = SNAPSHOT_TIMESTAMP.format(Instant.now());
        String shortSha = gitHeadShortSha();
        return timestamp + "." + shortSha;
    }

    public Path ensureLocalMavenRepositoryDirectory() throws IOException {
        Path defaultLocalRepo = repoRoot.resolve(".m2/repository").normalize();
        Files.createDirectories(defaultLocalRepo);

        String mavenOpts = ReleaseUtils.trimToNull(System.getenv("MAVEN_OPTS"));
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

    private String gitHeadShortSha() throws IOException, InterruptedException {
        CommandResult result = ReleaseUtils.runCapture(repoRoot, "git", "rev-parse", "--short", "HEAD");
        if (result.exitCode != 0) {
            return "nogit";
        }
        String value = ReleaseUtils.trimToNull(result.stdoutText());
        return value == null ? "nogit" : value;
    }
}
