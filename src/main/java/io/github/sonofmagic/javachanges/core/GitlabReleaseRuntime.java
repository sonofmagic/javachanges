package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.requireEnv;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.trimToNull;

class GitlabReleaseRuntime {
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

    String remoteBranchHead(String branchName, String remoteUrl) throws IOException, InterruptedException {
        CommandResult result = runGitCapture("ls-remote", "--heads", remoteUrl, "refs/heads/" + branchName);
        if (result.exitCode != 0) {
            String error = trimToNull(result.stderrText());
            throw new IllegalStateException(error == null
                ? "git command failed: [ls-remote, --heads, " + remoteUrl + ", refs/heads/" + branchName + "]"
                : error);
        }
        String stdout = trimToNull(result.stdoutText());
        if (stdout == null) {
            return null;
        }
        int tabIndex = stdout.indexOf('\t');
        if (tabIndex <= 0) {
            throw new IllegalStateException("Unexpected git ls-remote output: " + stdout);
        }
        return stdout.substring(0, tabIndex).trim();
    }

    void pushReleaseBranch(String remoteUrl, String releaseBranch, String expectedOldSha)
        throws IOException, InterruptedException {
        String destination = "HEAD:refs/heads/" + releaseBranch;
        if (trimToNull(expectedOldSha) == null) {
            runGit("push", remoteUrl, destination);
            return;
        }
        runGit(
            "push",
            "--force-with-lease=refs/heads/" + releaseBranch + ":" + expectedOldSha,
            remoteUrl,
            destination
        );
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
        String[] command = new String[args.length + 1];
        command[0] = "git";
        System.arraycopy(args, 0, command, 1, args.length);
        return ReleaseProcessUtils.runCapture(repoRoot, command);
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
