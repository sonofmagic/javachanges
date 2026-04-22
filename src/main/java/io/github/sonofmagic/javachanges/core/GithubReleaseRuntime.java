package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.trimToNull;

class GithubReleaseRuntime {
    private final Path repoRoot;

    GithubReleaseRuntime(Path repoRoot) {
        this.repoRoot = repoRoot;
    }

    void configureBotIdentity() throws IOException, InterruptedException {
        runGit("config", "user.name", "github-actions[bot]");
        runGit("config", "user.email", "41898282+github-actions[bot]@users.noreply.github.com");
    }

    boolean hasNoStagedChanges() throws IOException, InterruptedException {
        return runGitAllowFailure("diff", "--cached", "--quiet") == 0;
    }

    boolean remoteTagExists(String tagName, String remoteName) throws IOException, InterruptedException {
        CommandResult result = runGitCapture("ls-remote", "--tags", remoteName, "refs/tags/" + tagName);
        return result.exitCode == 0 && trimToNull(result.stdoutText()) != null;
    }

    String headSha() throws IOException, InterruptedException {
        String githubSha = trimToNull(System.getenv("GITHUB_SHA"));
        if (githubSha != null) {
            return githubSha;
        }
        CommandResult result = runGitCapture("rev-parse", "HEAD");
        if (result.exitCode != 0) {
            throw new IllegalStateException(trimToNull(result.stderrText()) == null
                ? "git rev-parse HEAD failed"
                : result.stderrText().trim());
        }
        String sha = trimToNull(result.stdoutText());
        if (sha == null) {
            throw new IllegalStateException("Current HEAD SHA is empty");
        }
        return sha;
    }

    void createOrUpdateTag(String tagName, String sha) throws IOException, InterruptedException {
        if (runGitAllowFailure("rev-parse", tagName) == 0) {
            runGit("tag", "-f", tagName, sha);
            return;
        }
        runGit("tag", tagName, sha);
    }

    boolean releaseExists(String tagName) throws IOException, InterruptedException {
        return runGhCapture("release", "view", tagName).exitCode == 0;
    }

    void createRelease(String tagName, Path notesFile) throws IOException, InterruptedException {
        runGh(
            "release", "create", tagName,
            "--title", tagName,
            "--notes-file", notesFile.toString()
        );
    }

    void updateRelease(String tagName, Path notesFile) throws IOException, InterruptedException {
        runGh(
            "release", "edit", tagName,
            "--title", tagName,
            "--notes-file", notesFile.toString()
        );
    }

    String findOpenPullRequestNumber(String githubRepo, String headBranch, String baseBranch)
        throws IOException, InterruptedException {
        CommandResult result = runGhCapture(
            "pr", "list",
            "--repo", githubRepo,
            "--base", baseBranch,
            "--head", headBranch,
            "--json", "number",
            "--jq", ".[0].number"
        );
        if (result.exitCode != 0) {
            String error = trimToNull(result.stderrText());
            throw new IllegalStateException(error == null ? "gh pr list failed" : error);
        }
        return trimToNull(result.stdoutText());
    }

    void createPullRequest(String githubRepo, String headBranch, String baseBranch, String title, Path bodyFile)
        throws IOException, InterruptedException {
        runGh(
            "pr", "create",
            "--repo", githubRepo,
            "--base", baseBranch,
            "--head", headBranch,
            "--title", title,
            "--body-file", repoRoot.relativize(bodyFile).toString()
        );
    }

    void updatePullRequest(String githubRepo, String prNumber, String title, Path bodyFile)
        throws IOException, InterruptedException {
        runGh(
            "pr", "edit", prNumber,
            "--repo", githubRepo,
            "--title", title,
            "--body-file", repoRoot.relativize(bodyFile).toString()
        );
    }

    void runGit(String... args) throws IOException, InterruptedException {
        CommandResult result = runGitCapture(args);
        if (result.exitCode != 0) {
            String error = trimToNull(result.stderrText());
            throw new IllegalStateException(error == null ? "git command failed: " + Arrays.asList(args) : error);
        }
    }

    void runGh(String... args) throws IOException, InterruptedException {
        CommandResult result = runGhCapture(args);
        if (result.exitCode != 0) {
            String error = trimToNull(result.stderrText());
            throw new IllegalStateException(error == null ? "gh command failed: " + Arrays.asList(args) : error);
        }
    }

    private int runGitAllowFailure(String... args) throws IOException, InterruptedException {
        return runGitCapture(args).exitCode;
    }

    private CommandResult runGitCapture(String... args) throws IOException, InterruptedException {
        return runCapture("git", args);
    }

    private CommandResult runGhCapture(String... args) throws IOException, InterruptedException {
        return runCapture("gh", args);
    }

    private CommandResult runCapture(String executable, String... args) throws IOException, InterruptedException {
        String[] command = new String[args.length + 1];
        command[0] = executable;
        System.arraycopy(args, 0, command, 1, args.length);
        return ReleaseProcessUtils.runCapture(repoRoot, command);
    }
}
