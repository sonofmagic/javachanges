package io.github.sonofmagic.javachanges.core.gitlab;

import io.github.sonofmagic.javachanges.core.CommandResult;
import io.github.sonofmagic.javachanges.core.ReleaseProcessUtils;
import io.github.sonofmagic.javachanges.core.ReleaseTextUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

public class GitlabReleaseRuntime {
    private final Path repoRoot;

    public GitlabReleaseRuntime(Path repoRoot) {
        this.repoRoot = repoRoot;
    }

    public void configureBotIdentity() throws IOException, InterruptedException {
        String botUsername = ReleaseTextUtils.requireEnv("GITLAB_RELEASE_BOT_USERNAME");
        String gitlabHost = firstNonBlank(System.getenv("CI_SERVER_HOST"), "gitlab.example.com");
        runGit("config", "user.name", "gitlab-release-bot");
        runGit("config", "user.email", botUsername + "@users.noreply." + gitlabHost);
    }

    public boolean hasNoStagedChanges() throws IOException, InterruptedException {
        return runGitAllowFailure("diff", "--cached", "--quiet") == 0;
    }

    public boolean changedBetween(String beforeSha, String currentSha, String path) throws IOException, InterruptedException {
        return runGitAllowFailure("diff", "--quiet", beforeSha, currentSha, "--", path) != 0;
    }

    public boolean remoteTagExists(String tagName, String remoteUrl) throws IOException, InterruptedException {
        CommandResult result = runGitCapture("ls-remote", "--tags", remoteUrl, "refs/tags/" + tagName);
        if (result.exitCode != 0) {
            throw gitCommandException(result, "ls-remote", "--tags", remoteUrl, "refs/tags/" + tagName);
        }
        return ReleaseTextUtils.trimToNull(result.stdoutText()) != null;
    }

    public String remoteBranchHead(String branchName, String remoteUrl) throws IOException, InterruptedException {
        CommandResult result = runGitCapture("ls-remote", "--heads", remoteUrl, "refs/heads/" + branchName);
        if (result.exitCode != 0) {
            String error = ReleaseTextUtils.trimToNull(ReleaseTextUtils.redactSensitiveText(result.stderrText()));
            throw new IllegalStateException(error == null
                ? "git command failed: [ls-remote, --heads, "
                    + ReleaseTextUtils.redactSensitiveText(remoteUrl) + ", refs/heads/" + branchName + "]"
                : error);
        }
        String stdout = ReleaseTextUtils.trimToNull(result.stdoutText());
        if (stdout == null) {
            return null;
        }
        int tabIndex = stdout.indexOf('\t');
        if (tabIndex <= 0) {
            throw new IllegalStateException("Unexpected git ls-remote output: " + stdout);
        }
        return stdout.substring(0, tabIndex).trim();
    }

    public void pushReleaseBranch(String remoteUrl, String releaseBranch, String expectedOldSha)
        throws IOException, InterruptedException {
        String destination = "HEAD:refs/heads/" + releaseBranch;
        if (ReleaseTextUtils.trimToNull(expectedOldSha) == null) {
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

    public void runGit(String... args) throws IOException, InterruptedException {
        CommandResult result = runGitCapture(args);
        if (result.exitCode != 0) {
            throw gitCommandException(result, args);
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

    private IllegalStateException gitCommandException(CommandResult result, String... args) {
        String error = ReleaseTextUtils.trimToNull(ReleaseTextUtils.redactSensitiveText(result.stderrText()));
        return new IllegalStateException(error == null
            ? "git command failed: " + ReleaseTextUtils.redactSensitiveText(Arrays.asList(args).toString())
            : error);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String trimmed = ReleaseTextUtils.trimToNull(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return null;
    }
}
