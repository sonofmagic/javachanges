package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.*;

final class GithubReleaseSupport {
    private final Path repoRoot;
    private final PrintStream out;
    private final GithubReleaseRuntime runtime;

    GithubReleaseSupport(Path repoRoot, PrintStream out) {
        this.repoRoot = repoRoot;
        this.out = out;
        this.runtime = new GithubReleaseRuntime(repoRoot);
    }

    void planPullRequest(GithubReleasePlanRequest request) throws IOException, InterruptedException {
        if (trimToNull(request.githubRepo) == null) {
            throw new IllegalArgumentException("Missing GitHub repo. Pass --github-repo or set GITHUB_REPOSITORY.");
        }

        ReleasePlan plan = new ReleasePlanner(repoRoot).plan();
        if (!plan.hasPendingChangesets()) {
            out.println("No pending changesets. Skip release PR.");
            return;
        }

        String releaseVersion = plan.getReleaseVersion();
        String releaseBranch = request.releaseBranch;
        String targetBranch = request.targetBranch;
        String commitMessage = "chore(release): apply changesets for v" + releaseVersion;
        String title = "chore(release): v" + releaseVersion;

        out.println("GitHub repo: " + request.githubRepo);
        out.println("Release branch: " + releaseBranch);
        out.println("Target branch: " + targetBranch);
        out.println("Release version: " + releaseVersion);

        if (!request.execute) {
            out.println("Dry-run only. Use --execute true to create/update the GitHub PR.");
            return;
        }

        runtime.configureBotIdentity();
        runtime.runGit("switch", "-C", releaseBranch);
        RepoFiles.applyPlan(repoRoot, plan);
        runtime.runGit("add", "pom.xml", "CHANGELOG.md", CHANGESETS_DIR);
        if (runtime.hasNoStagedChanges()) {
            out.println("No staged release plan changes. Skip release PR update.");
            return;
        }
        runtime.runGit("commit", "-m", commitMessage);
        runtime.runGit("push", "--force-with-lease", "origin", "HEAD:" + releaseBranch);

        String prNumber = runtime.findOpenPullRequestNumber(request.githubRepo, releaseBranch, targetBranch);
        if (trimToNull(prNumber) == null) {
            runtime.createPullRequest(request.githubRepo, releaseBranch, targetBranch, title,
                repoRoot.resolve(CHANGESETS_DIR).resolve(RELEASE_PLAN_MD));
            out.println("Created GitHub PR for " + title);
            return;
        }

        runtime.updatePullRequest(request.githubRepo, prNumber, title,
            repoRoot.resolve(CHANGESETS_DIR).resolve(RELEASE_PLAN_MD));
        out.println("Updated GitHub PR #" + prNumber);
    }

    void tagFromReleasePlan(GithubTagRequest request) throws IOException, InterruptedException {
        String releaseVersion = RepoFiles.readManifestField(repoRoot, "releaseVersion");
        String currentSha = firstNonBlank(trimToNull(request.currentSha), runtime.headSha());
        String tagName = "v" + releaseVersion;

        out.println("Release tag: " + tagName);
        out.println("Target commit: " + currentSha);

        if (runtime.remoteTagExists(tagName, "origin")) {
            out.println("Tag already exists remotely. Skip.");
            return;
        }

        if (!request.execute) {
            out.println("Dry-run only. Use --execute true to create and push the release tag.");
            return;
        }

        runtime.createOrUpdateTag(tagName, currentSha);
        runtime.runGit("push", "origin", "refs/tags/" + tagName);
        out.println("Created and pushed tag " + tagName);
    }
}
