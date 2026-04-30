package io.github.sonofmagic.javachanges.core.gitlab;

import io.github.sonofmagic.javachanges.core.BuildModelSupport;
import io.github.sonofmagic.javachanges.core.ChangesetPaths;
import io.github.sonofmagic.javachanges.core.ReleaseAutomationSupport;
import io.github.sonofmagic.javachanges.core.ReleaseMessages;
import io.github.sonofmagic.javachanges.core.ReleaseTextUtils;
import io.github.sonofmagic.javachanges.core.automation.AbstractReleaseAutomationSupport;
import io.github.sonofmagic.javachanges.core.automation.AutomationJsonSupport;
import io.github.sonofmagic.javachanges.core.automation.ReleaseArtifactSupport;
import io.github.sonofmagic.javachanges.core.automation.ReleaseNotesGenerator;
import io.github.sonofmagic.javachanges.core.plan.ReleasePlan;
import io.github.sonofmagic.javachanges.core.plan.RepoFiles;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class GitlabReleaseSupport extends AbstractReleaseAutomationSupport {
    private final GitlabReleaseRuntime runtime;
    private final GitlabMergeRequestClient apiClient;

    public GitlabReleaseSupport(Path repoRoot, PrintStream out) {
        this(repoRoot, out, new GitlabReleaseRuntime(repoRoot), new GitlabApiClient());
    }

    public GitlabReleaseSupport(Path repoRoot, PrintStream out, GitlabReleaseRuntime runtime, GitlabMergeRequestClient apiClient) {
        super(repoRoot, out);
        this.runtime = runtime;
        this.apiClient = apiClient;
    }

    public void planMergeRequest(GitlabReleasePlanRequest request) throws IOException, InterruptedException {
        boolean textOutput = isTextOutput(request.format);
        AutomationJsonSupport.AutomationReport report =
            newAutomationReport("gitlab-release-plan", "plan-merge-request", request.execute);
        report.projectId = request.projectId;
        if (ReleaseTextUtils.trimToNull(request.projectId) == null) {
            throw new IllegalArgumentException(ReleaseMessages.missingGitlabProjectId());
        }

        ReleasePlan plan = automationSupport.plan();
        ReleaseAutomationSupport.ReleaseDescriptor release = descriptorFromPlan(plan, report);
        if (skipWhenNoPendingChangesets(plan, report, textOutput, ReleaseMessages.noPendingChangesetsSkipReleaseMr())) {
            return;
        }

        String releaseBranch = request.releaseBranch;
        String targetBranch = request.targetBranch;
        String commitMessage = release.commitMessage();
        String title = release.gitlabMergeRequestTitle();

        AutomationJsonSupport.printLines(out, textOutput,
            ReleaseMessages.releaseBranchValue(releaseBranch),
            ReleaseMessages.targetBranchValue(targetBranch),
            ReleaseMessages.releaseVersionValueText(release.releaseVersion)
        );

        if (skipDryRun(report, textOutput, ReleaseMessages.dryRunGitlabMr())) {
            return;
        }

        runtime.configureBotIdentity();
        runtime.runGit("checkout", "-B", releaseBranch);
        RepoFiles.applyPlan(repoRoot, plan, request.writePlanFiles);
        String description = String.join("\n", plan.toPullRequestBodyLines()) + "\n";
        runtime.runGit(concat("add", BuildModelSupport.releasePlanGitAddPaths(repoRoot)));
        if (runtime.hasNoStagedChanges()) {
            report.skipped = true;
            report.reason = ReleaseMessages.noStagedReleasePlanChangesReason();
            AutomationJsonSupport.print(out, textOutput, report, ReleaseMessages.noStagedReleasePlanChangesSkipMr());
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
            report.reason = ReleaseMessages.createdGitlabMergeRequestReason();
            AutomationJsonSupport.print(out, textOutput, report,
                ReleaseMessages.createdGitlabMr(apiClient.requiredJsonInt(response, "iid")));
            return;
        }

        apiClient.updateMergeRequest(request.projectId, mergeRequestIid.intValue(), title, description);
        report.action = "update-merge-request";
        report.reason = ReleaseMessages.updatedGitlabMergeRequestReason();
        AutomationJsonSupport.print(out, textOutput, report, ReleaseMessages.updatedGitlabMr(mergeRequestIid));
    }

    private static String[] concat(String first, String[] rest) {
        String[] values = new String[rest.length + 1];
        values[0] = first;
        System.arraycopy(rest, 0, values, 1, rest.length);
        return values;
    }

    public void tagFromReleasePlan(GitlabTagRequest request) throws IOException, InterruptedException {
        boolean textOutput = isTextOutput(request.format);
        AutomationJsonSupport.AutomationReport report =
            newAutomationReport("gitlab-tag-from-plan", "tag-from-plan", request.execute);
        if (ReleaseTextUtils.trimToNull(request.currentBranch) != null
            && ReleaseTextUtils.trimToNull(request.releaseBranch) != null
            && request.currentBranch.equals(request.releaseBranch)) {
            report.skipped = true;
            report.reason = ReleaseMessages.currentBranchMatchesReleaseBranchReason();
            AutomationJsonSupport.print(out, textOutput, report, ReleaseMessages.currentBranchMatchesReleaseBranchSkipTag());
            return;
        }
        if (ReleaseTextUtils.trimToNull(request.currentBranch) != null
            && ReleaseTextUtils.trimToNull(request.baseBranch) != null
            && !request.currentBranch.equals(request.baseBranch)) {
            report.skipped = true;
            report.reason = ReleaseMessages.currentBranchDoesNotMatchBaseBranchReason();
            AutomationJsonSupport.print(out, textOutput, report,
                ReleaseMessages.currentBranchDoesNotMatchBaseBranchSkipTag(request.currentBranch, request.baseBranch));
            return;
        }
        String beforeSha = ReleaseTextUtils.trimToNull(request.beforeSha);
        String currentSha = ReleaseTextUtils.trimToNull(request.currentSha);
        if (beforeSha == null || currentSha == null || beforeSha.matches("0+")) {
            report.skipped = true;
            report.reason = ReleaseMessages.missingPreviousShaReason();
            AutomationJsonSupport.print(out, textOutput, report, ReleaseMessages.missingPreviousShaSkipTag());
            return;
        }

        ReleaseMetadataSource metadataSource = selectReleaseMetadataSource(request, beforeSha, currentSha);
        if (metadataSource == ReleaseMetadataSource.NONE) {
            report.skipped = true;
            report.reason = request.fresh
                ? ReleaseMessages.noFreshReleaseStateChangeDetectedReason()
                : ReleaseMessages.noReleaseMetadataChangeDetectedReason();
            AutomationJsonSupport.print(out, textOutput, report, ReleaseMessages.releaseChangeReasonSkipTag(report.reason));
            return;
        }

        ReleaseAutomationSupport.ReleaseDescriptor release = metadataSource == ReleaseMetadataSource.FRESH
            ? descriptorFromFreshPlan(report)
            : descriptorFromManifest(report);
        List<String> tagNames = release.tagNames();
        AutomationJsonSupport.printLines(out, textOutput, ReleaseMessages.releaseTagsValue(tagNames));

        String remoteUrl = apiClient.authenticatedRemoteUrl();
        for (String tagName : tagNames) {
            if (runtime.remoteTagExists(tagName, remoteUrl)) {
                skipWhenRemoteTagExists(report, textOutput, tagName);
                return;
            }
        }

        if (skipDryRun(report, textOutput, ReleaseMessages.dryRunReleaseTag())) {
            return;
        }

        for (String tagName : tagNames) {
            runtime.runGit("tag", tagName, currentSha);
            runtime.runGit("push", remoteUrl, "refs/tags/" + tagName);
        }
        report.action = "create-tag";
        report.reason = ReleaseMessages.createdAndPushedTagReason();
        AutomationJsonSupport.print(out, textOutput, report, ReleaseMessages.createdAndPushedTags(tagNames));
    }

    public void syncRelease(GitlabReleaseRequest request) throws IOException, InterruptedException {
        boolean textOutput = isTextOutput(request.format);
        AutomationJsonSupport.AutomationReport report =
            newAutomationReport("gitlab-release", "sync-release", request.execute);
        report.projectId = request.projectId;

        String tagName = ReleaseTextUtils.trimToNull(request.tag);
        if (tagName == null) {
            throw new IllegalArgumentException(ReleaseMessages.missingGitlabTag());
        }
        if (ReleaseTextUtils.trimToNull(request.projectId) == null) {
            throw new IllegalArgumentException(ReleaseMessages.missingGitlabProjectId());
        }
        ReleaseArtifactSupport.ReleaseTagInfo tagInfo = artifactSupport.describeTag(tagName);
        report.tag = tagInfo.tag;
        report.releaseVersion = tagInfo.releaseVersion;
        report.releaseModule = tagInfo.releaseModule;

        Path notesFile = artifactSupport.resolveReleaseNotesFile(request.releaseNotesFile);
        report.releaseNotesFile = notesFile.toString();
        String releaseName = tagInfo.releaseDisplayName();

        AutomationJsonSupport.printLines(out, textOutput,
            ReleaseMessages.gitlabHostValue(ReleaseTextUtils.firstNonBlank(
                ReleaseTextUtils.trimToNull(request.gitlabHost),
                ReleaseTextUtils.trimToNull(System.getenv("CI_SERVER_HOST")))),
            ReleaseMessages.projectIdValue(request.projectId),
            ReleaseMessages.releaseTagValue(tagName),
            ReleaseMessages.releaseVersionValueText(report.releaseVersion),
            ReleaseMessages.releaseModuleValue(report.releaseModule == null ? "all" : report.releaseModule),
            ReleaseMessages.releaseNotesFileValue(notesFile)
        );

        boolean exists = apiClient.releaseExists(request.projectId, tagName);
        if (!request.execute) {
            report.reason = exists
                ? ReleaseMessages.gitlabReleaseWouldBeUpdatedReason()
                : ReleaseMessages.gitlabReleaseWouldBeCreatedReason();
            AutomationJsonSupport.print(out, textOutput, report, ReleaseMessages.dryRunGitlabRelease());
            return;
        }

        Files.createDirectories(notesFile.getParent());
        new ReleaseNotesGenerator(repoRoot).writeReleaseNotes(tagName, notesFile);
        String description = new String(Files.readAllBytes(notesFile), StandardCharsets.UTF_8);

        if (exists) {
            apiClient.updateRelease(request.projectId, tagName, releaseName, description);
            report.action = "update-release";
            report.reason = ReleaseMessages.updatedGitlabReleaseReason();
            AutomationJsonSupport.print(out, textOutput, report, ReleaseMessages.updatedGitlabRelease(tagName));
            return;
        }

        apiClient.createRelease(request.projectId, tagName, releaseName, description);
        report.action = "create-release";
        report.reason = ReleaseMessages.createdGitlabReleaseReason();
        AutomationJsonSupport.print(out, textOutput, report, ReleaseMessages.createdGitlabRelease(tagName));
    }

    private ReleaseMetadataSource selectReleaseMetadataSource(GitlabTagRequest request, String beforeSha, String currentSha)
        throws IOException, InterruptedException {
        if (request.fresh) {
            return freshReleaseStateChangedBetween(beforeSha, currentSha)
                ? ReleaseMetadataSource.FRESH
                : ReleaseMetadataSource.NONE;
        }
        if (releasePlanManifestExists() && releasePlanManifestChangedBetween(beforeSha, currentSha)) {
            return ReleaseMetadataSource.MANIFEST;
        }
        if (freshReleaseStateChangedBetween(beforeSha, currentSha)) {
            return ReleaseMetadataSource.FRESH;
        }
        return ReleaseMetadataSource.NONE;
    }

    private boolean releasePlanManifestExists() {
        return Files.exists(repoRoot.resolve(ChangesetPaths.DIR).resolve(ChangesetPaths.RELEASE_PLAN_JSON));
    }

    private boolean releasePlanManifestChangedBetween(String beforeSha, String currentSha)
        throws IOException, InterruptedException {
        return runtime.changedBetween(beforeSha, currentSha,
            ChangesetPaths.DIR + "/" + ChangesetPaths.RELEASE_PLAN_JSON);
    }

    private boolean freshReleaseStateChangedBetween(String beforeSha, String currentSha)
        throws IOException, InterruptedException {
        for (String path : BuildModelSupport.releaseStateGitPaths(repoRoot)) {
            if (runtime.changedBetween(beforeSha, currentSha, path)) {
                return true;
            }
        }
        return false;
    }

    private enum ReleaseMetadataSource {
        MANIFEST,
        FRESH,
        NONE
    }
}
