package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.*;

final class GitlabReleaseSupport {
    private final Path repoRoot;
    private final PrintStream out;
    private final GitlabReleaseRuntime runtime;
    private final GitlabMergeRequestClient apiClient;
    private final ReleaseArtifactSupport artifactSupport;
    private final ReleaseAutomationSupport automationSupport;

    GitlabReleaseSupport(Path repoRoot, PrintStream out) {
        this(repoRoot, out, new GitlabReleaseRuntime(repoRoot), new GitlabApiClient());
    }

    GitlabReleaseSupport(Path repoRoot, PrintStream out, GitlabReleaseRuntime runtime, GitlabMergeRequestClient apiClient) {
        this.repoRoot = repoRoot;
        this.out = out;
        this.runtime = runtime;
        this.apiClient = apiClient;
        this.artifactSupport = new ReleaseArtifactSupport(repoRoot);
        this.automationSupport = new ReleaseAutomationSupport(repoRoot);
    }

    void planMergeRequest(GitlabReleasePlanRequest request) throws IOException, InterruptedException {
        boolean textOutput = AutomationJsonSupport.isText(request.format);
        AutomationJsonSupport.AutomationReport report = new AutomationJsonSupport.AutomationReport("gitlab-release-plan");
        report.projectId = request.projectId;
        report.action = "plan-merge-request";
        report.execute = request.execute;
        report.dryRun = !request.execute;
        if (trimToNull(request.projectId) == null) {
            throw new IllegalArgumentException("Missing GitLab project id. Pass --project-id or set CI_PROJECT_ID.");
        }

        ReleasePlan plan = automationSupport.plan();
        ReleaseAutomationSupport.ReleaseDescriptor release = automationSupport.descriptorFromPlan(plan);
        report.releaseVersion = release.releaseVersion;
        if (!plan.hasPendingChangesets()) {
            report.skipped = true;
            report.reason = "No pending changesets.";
            AutomationJsonSupport.print(out, textOutput, report, "No pending changesets. Skip release MR.");
            return;
        }

        String releaseBranch = request.releaseBranch;
        String targetBranch = request.targetBranch;
        String commitMessage = release.commitMessage();
        String title = release.gitlabMergeRequestTitle();

        AutomationJsonSupport.printLines(out, textOutput,
            "Release branch: " + releaseBranch,
            "Target branch: " + targetBranch,
            "Release version: " + release.releaseVersion
        );

        if (!request.execute) {
            report.reason = "Dry-run only.";
            AutomationJsonSupport.print(out, textOutput, report,
                "Dry-run only. Use --execute true to create/update the GitLab MR.");
            return;
        }

        runtime.configureBotIdentity();
        runtime.runGit("checkout", "-B", releaseBranch);
        RepoFiles.applyPlan(repoRoot, plan);
        String description = new String(
            Files.readAllBytes(automationSupport.releasePlanMarkdownFile()),
            StandardCharsets.UTF_8
        );
        runtime.runGit("add", "pom.xml", "CHANGELOG.md", CHANGESETS_DIR);
        if (runtime.hasNoStagedChanges()) {
            report.skipped = true;
            report.reason = "No staged release plan changes.";
            AutomationJsonSupport.print(out, textOutput, report,
                "No staged release plan changes. Skip release MR update.");
            return;
        }
        runtime.runGit("commit", "-m", commitMessage);
        String remoteUrl = apiClient.authenticatedRemoteUrl();
        Integer mergeRequestIid = apiClient.findOpenMergeRequestIid(request.projectId, releaseBranch, targetBranch);
        String remoteBranchHead = runtime.remoteBranchHead(releaseBranch, remoteUrl);
        runtime.pushReleaseBranch(remoteUrl, releaseBranch, remoteBranchHead);
        if (mergeRequestIid == null) {
            String response = apiClient.createMergeRequest(request.projectId, releaseBranch, targetBranch, title, description);
            report.action = "create-merge-request";
            report.reason = "Created GitLab merge request.";
            AutomationJsonSupport.print(out, textOutput, report,
                "Created GitLab MR !" + apiClient.requiredJsonInt(response, "iid"));
            return;
        }

        apiClient.updateMergeRequest(request.projectId, mergeRequestIid.intValue(), title, description);
        report.action = "update-merge-request";
        report.reason = "Updated GitLab merge request.";
        AutomationJsonSupport.print(out, textOutput, report, "Updated GitLab MR !" + mergeRequestIid);
    }

    void tagFromReleasePlan(GitlabTagRequest request) throws IOException, InterruptedException {
        boolean textOutput = AutomationJsonSupport.isText(request.format);
        AutomationJsonSupport.AutomationReport report = new AutomationJsonSupport.AutomationReport("gitlab-tag-from-plan");
        report.action = "tag-from-plan";
        report.execute = request.execute;
        report.dryRun = !request.execute;
        if (trimToNull(request.currentBranch) != null && trimToNull(request.releaseBranch) != null
            && request.currentBranch.equals(request.releaseBranch)) {
            report.skipped = true;
            report.reason = "Current branch matches the configured release branch.";
            AutomationJsonSupport.print(out, textOutput, report,
                "Current branch matches the configured release branch. Skip release tag.");
            return;
        }
        if (trimToNull(request.currentBranch) != null && trimToNull(request.baseBranch) != null
            && !request.currentBranch.equals(request.baseBranch)) {
            report.skipped = true;
            report.reason = "Current branch does not match the configured base branch.";
            AutomationJsonSupport.print(out, textOutput, report,
                "Current branch " + request.currentBranch + " does not match base branch "
                    + request.baseBranch + ". Skip release tag.");
            return;
        }
        String beforeSha = trimToNull(request.beforeSha);
        String currentSha = trimToNull(request.currentSha);
        if (beforeSha == null || currentSha == null || beforeSha.matches("0+")) {
            report.skipped = true;
            report.reason = "Missing previous SHA.";
            AutomationJsonSupport.print(out, textOutput, report, "Missing previous SHA. Skip release tag.");
            return;
        }

        if (!runtime.changedBetween(beforeSha, currentSha, CHANGESETS_DIR + "/" + RELEASE_PLAN_JSON)) {
            report.skipped = true;
            report.reason = "No release plan manifest change detected.";
            AutomationJsonSupport.print(out, textOutput, report,
                "No release plan manifest change detected. Skip release tag.");
            return;
        }

        ReleaseAutomationSupport.ReleaseDescriptor release = automationSupport.descriptorFromManifest();
        report.releaseVersion = release.releaseVersion;
        report.tagStrategy = release.tagStrategy.id;
        report.tags = release.tagNames();
        report.tag = release.releaseTargets.size() == 1 ? release.tagNames().get(0) : null;
        List<String> tagNames = release.tagNames();
        AutomationJsonSupport.printLines(out, textOutput, "Release tags: " + tagNames);

        String remoteUrl = apiClient.authenticatedRemoteUrl();
        for (String tagName : tagNames) {
            if (runtime.remoteTagExists(tagName, remoteUrl)) {
                report.skipped = true;
                report.reason = "Tag already exists remotely: " + tagName;
                AutomationJsonSupport.print(out, textOutput, report, "Tag already exists remotely. Skip.");
                return;
            }
        }

        if (!request.execute) {
            report.reason = "Dry-run only.";
            AutomationJsonSupport.print(out, textOutput, report,
                "Dry-run only. Use --execute true to create and push the release tag.");
            return;
        }

        for (String tagName : tagNames) {
            runtime.runGit("tag", tagName, currentSha);
            runtime.runGit("push", remoteUrl, "refs/tags/" + tagName);
        }
        report.action = "create-tag";
        report.reason = "Created and pushed tag.";
        AutomationJsonSupport.print(out, textOutput, report, "Created and pushed tag(s) " + tagNames);
    }

    void syncRelease(GitlabReleaseRequest request) throws IOException, InterruptedException {
        boolean textOutput = AutomationJsonSupport.isText(request.format);
        AutomationJsonSupport.AutomationReport report = new AutomationJsonSupport.AutomationReport("gitlab-release");
        report.action = "sync-release";
        report.projectId = request.projectId;
        report.execute = request.execute;
        report.dryRun = !request.execute;

        String tagName = trimToNull(request.tag);
        if (tagName == null) {
            throw new IllegalArgumentException("Missing GitLab tag. Pass --tag or set CI_COMMIT_TAG.");
        }
        if (trimToNull(request.projectId) == null) {
            throw new IllegalArgumentException("Missing GitLab project id. Pass --project-id or set CI_PROJECT_ID.");
        }
        ReleaseArtifactSupport.ReleaseTagInfo tagInfo = artifactSupport.describeTag(tagName);
        report.tag = tagInfo.tag;
        report.releaseVersion = tagInfo.releaseVersion;
        report.releaseModule = tagInfo.releaseModule;

        Path notesFile = artifactSupport.resolveReleaseNotesFile(request.releaseNotesFile);
        report.releaseNotesFile = notesFile.toString();
        Files.createDirectories(notesFile.getParent());
        new ReleaseNotesGenerator(repoRoot).writeReleaseNotes(tagName, notesFile);
        String description = new String(Files.readAllBytes(notesFile), StandardCharsets.UTF_8);
        String releaseName = tagInfo.releaseDisplayName();

        AutomationJsonSupport.printLines(out, textOutput,
            "GitLab host: " + firstNonBlank(trimToNull(request.gitlabHost), trimToNull(System.getenv("CI_SERVER_HOST"))),
            "Project ID: " + request.projectId,
            "Release tag: " + tagName,
            "Release version: " + report.releaseVersion,
            "Release module: " + (report.releaseModule == null ? "all" : report.releaseModule),
            "Release notes file: " + notesFile
        );

        boolean exists = apiClient.releaseExists(request.projectId, tagName);
        if (!request.execute) {
            report.reason = exists ? "Dry-run only. GitLab Release would be updated." : "Dry-run only. GitLab Release would be created.";
            AutomationJsonSupport.print(out, textOutput, report,
                "Dry-run only. Use --execute true to create/update the GitLab Release.");
            return;
        }

        if (exists) {
            apiClient.updateRelease(request.projectId, tagName, releaseName, description);
            report.action = "update-release";
            report.reason = "Updated GitLab Release.";
            AutomationJsonSupport.print(out, textOutput, report, "Updated GitLab Release " + tagName);
            return;
        }

        apiClient.createRelease(request.projectId, tagName, releaseName, description);
        report.action = "create-release";
        report.reason = "Created GitLab Release.";
        AutomationJsonSupport.print(out, textOutput, report, "Created GitLab Release " + tagName);
    }
}
