package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.*;

final class GitlabReleaseSupport {
    private final Path repoRoot;
    private final PrintStream out;
    private final GitlabReleaseRuntime runtime;
    private final GitlabApiClient apiClient;

    GitlabReleaseSupport(Path repoRoot, PrintStream out) {
        this.repoRoot = repoRoot;
        this.out = out;
        this.runtime = new GitlabReleaseRuntime(repoRoot);
        this.apiClient = new GitlabApiClient();
    }

    void planMergeRequest(GitlabReleasePlanRequest request) throws IOException, InterruptedException {
        if (trimToNull(request.projectId) == null) {
            throw new IllegalArgumentException("Missing GitLab project id. Pass --project-id or set CI_PROJECT_ID.");
        }

        ReleasePlan plan = new ReleasePlanner(repoRoot).plan();
        if (!plan.hasPendingChangesets()) {
            out.println("No pending changesets. Skip release MR.");
            return;
        }

        String releaseVersion = plan.getReleaseVersion();
        String releaseBranch = request.releaseBranch;
        String targetBranch = request.targetBranch;
        String commitMessage = "chore(release): apply changesets for v" + releaseVersion;
        String title = "chore(release): release v" + releaseVersion;

        out.println("Release branch: " + releaseBranch);
        out.println("Target branch: " + targetBranch);
        out.println("Release version: " + releaseVersion);

        if (!request.execute) {
            out.println("Dry-run only. Use --execute true to create/update the GitLab MR.");
            return;
        }

        runtime.configureBotIdentity();
        runtime.runGit("checkout", "-B", releaseBranch);
        RepoFiles.applyPlan(repoRoot, plan);
        String description = new String(
            Files.readAllBytes(repoRoot.resolve(CHANGESETS_DIR).resolve(RELEASE_PLAN_MD)),
            StandardCharsets.UTF_8
        );
        runtime.runGit("add", "pom.xml", "CHANGELOG.md", CHANGESETS_DIR);
        if (runtime.hasNoStagedChanges()) {
            out.println("No staged release plan changes. Skip release MR update.");
            return;
        }
        runtime.runGit("commit", "-m", commitMessage);
        String remoteUrl = apiClient.authenticatedRemoteUrl();
        runtime.runGit("push", "--force-with-lease", remoteUrl, "HEAD:" + releaseBranch);

        Integer mergeRequestIid = apiClient.findOpenMergeRequestIid(request.projectId, releaseBranch, targetBranch);
        if (mergeRequestIid == null) {
            String response = apiClient.createMergeRequest(request.projectId, releaseBranch, targetBranch, title, description);
            out.println("Created GitLab MR !" + apiClient.requiredJsonInt(response, "iid"));
            return;
        }

        apiClient.updateMergeRequest(request.projectId, mergeRequestIid.intValue(), title, description);
        out.println("Updated GitLab MR !" + mergeRequestIid);
    }

    void tagFromReleasePlan(GitlabTagRequest request) throws IOException, InterruptedException {
        String beforeSha = trimToNull(request.beforeSha);
        String currentSha = trimToNull(request.currentSha);
        if (beforeSha == null || currentSha == null || beforeSha.matches("0+")) {
            out.println("Missing previous SHA. Skip release tag.");
            return;
        }

        if (!runtime.changedBetween(beforeSha, currentSha, CHANGESETS_DIR + "/" + RELEASE_PLAN_JSON)) {
            out.println("No release plan manifest change detected. Skip release tag.");
            return;
        }

        String releaseVersion = RepoFiles.readManifestField(repoRoot, "releaseVersion");
        String tagName = "v" + releaseVersion;
        out.println("Release tag: " + tagName);

        String remoteUrl = apiClient.authenticatedRemoteUrl();
        if (runtime.remoteTagExists(tagName, remoteUrl)) {
            out.println("Tag already exists remotely. Skip.");
            return;
        }

        if (!request.execute) {
            out.println("Dry-run only. Use --execute true to create and push the release tag.");
            return;
        }

        runtime.runGit("tag", tagName, currentSha);
        runtime.runGit("push", remoteUrl, "refs/tags/" + tagName);
        out.println("Created and pushed tag " + tagName);
    }
}
