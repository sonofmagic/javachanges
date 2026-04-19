package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.readAllBytes;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.requireEnv;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.trimToNull;

final class GitlabReleaseRuntime {
    private final Path repoRoot;

    GitlabReleaseRuntime(Path repoRoot) {
        this.repoRoot = repoRoot;
    }

    void configureBotIdentity() throws IOException, InterruptedException {
        String botUsername = requireEnv("GITLAB_RELEASE_BOT_USERNAME");
        String gitlabHost = firstNonBlank(System.getenv("CI_SERVER_HOST"), "gitlab.example.com");
        runGit("config", "user.name", "gitlab-release-bot");
        runGit("config", "user.email", botUsername + "@users.noreply." + gitlabHost);
    }

    boolean hasNoStagedChanges() throws IOException, InterruptedException {
        return runGitAllowFailure("diff", "--cached", "--quiet") == 0;
    }

    boolean changedBetween(String beforeSha, String currentSha, String path) throws IOException, InterruptedException {
        return runGitAllowFailure("diff", "--quiet", beforeSha, currentSha, "--", path) != 0;
    }

    boolean remoteTagExists(String tagName, String remoteUrl) throws IOException, InterruptedException {
        CommandResult result = runGitCapture("ls-remote", "--tags", remoteUrl, "refs/tags/" + tagName);
        return result.exitCode == 0 && trimToNull(result.stdoutText()) != null;
    }

    void runGit(String... args) throws IOException, InterruptedException {
        CommandResult result = runGitCapture(args);
        if (result.exitCode != 0) {
            String error = trimToNull(result.stderrText());
            throw new IllegalStateException(error == null ? "git command failed: " + Arrays.asList(args) : error);
        }
    }

    private int runGitAllowFailure(String... args) throws IOException, InterruptedException {
        return runGitCapture(args).exitCode;
    }

    private CommandResult runGitCapture(String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<String>();
        command.add("git");
        command.addAll(Arrays.asList(args));
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(repoRoot.toFile());
        Process process = builder.start();
        byte[] stdout = readAllBytes(process.getInputStream());
        byte[] stderr = readAllBytes(process.getErrorStream());
        int exitCode = process.waitFor();
        return new CommandResult(exitCode, stdout, stderr);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String trimmed = trimToNull(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return null;
    }
}
