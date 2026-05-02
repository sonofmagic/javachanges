package io.github.sonofmagic.javachanges.core.github;

import io.github.sonofmagic.javachanges.core.BuildModelSupport;
import io.github.sonofmagic.javachanges.core.ReleaseAutomationSupport;
import io.github.sonofmagic.javachanges.core.ReleaseMessages;
import io.github.sonofmagic.javachanges.core.ReleaseTextUtils;
import io.github.sonofmagic.javachanges.core.automation.AbstractReleaseAutomationSupport;
import io.github.sonofmagic.javachanges.core.automation.AutomationJsonSupport;
import io.github.sonofmagic.javachanges.core.automation.ReleaseNotesGenerator;
import io.github.sonofmagic.javachanges.core.plan.ReleasePlan;
import io.github.sonofmagic.javachanges.core.plan.RepoFiles;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GithubReleaseSupport extends AbstractReleaseAutomationSupport {
    private final GithubReleaseRuntime runtime;

    public GithubReleaseSupport(Path repoRoot, PrintStream out) {
        this(repoRoot, out, new GithubReleaseRuntime(repoRoot));
    }

    public GithubReleaseSupport(Path repoRoot, PrintStream out, GithubReleaseRuntime runtime) {
        super(repoRoot, out);
        this.runtime = runtime;
    }

    public void planPullRequest(GithubReleasePlanRequest request) throws IOException, InterruptedException {
        boolean textOutput = isTextOutput(request.format);
        AutomationJsonSupport.AutomationReport report =
            newAutomationReport("github-release-plan", "plan-pull-request", request.execute);
        if (ReleaseTextUtils.trimToNull(request.githubRepo) == null) {
            throw new IllegalArgumentException(ReleaseMessages.missingGithubRepo());
        }

        ReleasePlan plan = automationSupport.plan();
        ReleaseAutomationSupport.ReleaseDescriptor release = descriptorFromPlan(plan, report);
        if (skipWhenNoPendingChangesets(plan, report, textOutput, ReleaseMessages.noPendingChangesetsSkipReleasePr())) {
            return;
        }

        String releaseBranch = request.releaseBranch;
        String targetBranch = request.targetBranch;
        String commitMessage = release.commitMessage();
        String title = release.githubPullRequestTitle();

        AutomationJsonSupport.printLines(out, textOutput,
            ReleaseMessages.githubRepoValue(request.githubRepo),
            ReleaseMessages.releaseBranchValue(releaseBranch),
            ReleaseMessages.targetBranchValue(targetBranch),
            ReleaseMessages.releaseVersionValueText(release.releaseVersion)
        );

        if (skipDryRun(report, textOutput, ReleaseMessages.dryRunGithubPr())) {
            return;
        }

        runtime.configureBotIdentity();
        runtime.runGit("switch", "-C", releaseBranch);
        RepoFiles.applyPlan(repoRoot, plan, request.writePlanFiles);
        Path pullRequestBodyFile = request.writePlanFiles
            ? releasePlanMarkdownFile()
            : writeTransientReleasePlanBody(plan);
        runtime.runGit(concat("add", BuildModelSupport.releasePlanGitAddPaths(repoRoot)));
        if (runtime.hasNoStagedChanges()) {
            report.skipped = true;
            report.reason = ReleaseMessages.noStagedReleasePlanChangesReason();
            AutomationJsonSupport.print(out, textOutput, report, ReleaseMessages.noStagedReleasePlanChangesSkipPr());
            return;
        }
        runtime.runGit("commit", "-m", commitMessage);
        runtime.runGit("push", "--force-with-lease", "origin", "HEAD:" + releaseBranch);

        String prNumber = runtime.findOpenPullRequestNumber(request.githubRepo, releaseBranch, targetBranch);
        if (ReleaseTextUtils.trimToNull(prNumber) == null) {
            report.action = "create-pull-request";
            report.reason = ReleaseMessages.createdGithubPullRequestReason();
            runtime.createPullRequest(request.githubRepo, releaseBranch, targetBranch, title,
                pullRequestBodyFile);
            AutomationJsonSupport.print(out, textOutput, report, ReleaseMessages.createdGithubPrFor(title));
            return;
        }

        report.action = "update-pull-request";
        report.reason = ReleaseMessages.updatedGithubPullRequestReason();
        runtime.updatePullRequest(request.githubRepo, prNumber, title,
            pullRequestBodyFile);
        AutomationJsonSupport.print(out, textOutput, report, ReleaseMessages.updatedGithubPr(prNumber));
    }

    private static String[] concat(String first, String[] rest) {
        String[] values = new String[rest.length + 1];
        values[0] = first;
        System.arraycopy(rest, 0, values, 1, rest.length);
        return values;
    }

    public void tagFromReleasePlan(GithubTagRequest request) throws IOException, InterruptedException {
        boolean textOutput = isTextOutput(request.format);
        AutomationJsonSupport.AutomationReport report =
            newAutomationReport("github-tag-from-plan", "tag-from-plan", request.execute);
        String currentSha = ReleaseTextUtils.firstNonBlank(ReleaseTextUtils.trimToNull(request.currentSha),
            runtime.headSha());
        ReleaseAutomationSupport.ReleaseDescriptor release = request.fresh
            ? descriptorFromFreshPlan(report)
            : descriptorFromManifest(report);
        List<String> tagNames = release.tagNames();

        AutomationJsonSupport.printLines(out, textOutput,
            ReleaseMessages.releaseTagsValue(tagNames),
            ReleaseMessages.targetCommitValue(currentSha)
        );

        List<String> missingTags = new ArrayList<String>();
        for (String tagName : tagNames) {
            String tagSha = runtime.remoteTagSha(tagName, "origin");
            if (tagSha == null) {
                missingTags.add(tagName);
                continue;
            }
            if (!tagSha.equals(currentSha)) {
                throw new IllegalStateException(ReleaseMessages.githubTagPointsAtDifferentCommit(tagName,
                    tagSha, currentSha));
            }
        }

        if (missingTags.isEmpty()) {
            skipWhenRemoteTagsAlreadyAtTargetCommit(report, textOutput, tagNames);
            return;
        }

        if (skipDryRun(report, textOutput, ReleaseMessages.dryRunReleaseTag())) {
            return;
        }

        for (String tagName : missingTags) {
            runtime.createOrUpdateTag(tagName, currentSha);
            runtime.runGit("push", "origin", "refs/tags/" + tagName);
        }
        report.action = "create-tag";
        report.reason = ReleaseMessages.createdAndPushedTagReason();
        AutomationJsonSupport.print(out, textOutput, report, ReleaseMessages.createdAndPushedTags(missingTags));
    }

    public void publishState(GithubReleasePublishStateRequest request) throws IOException, InterruptedException {
        boolean textOutput = isTextOutput(request.format);
        AutomationJsonSupport.AutomationReport report =
            newAutomationReport("github-release-publish-state", "resolve-publish-state", false);
        String currentSha = ReleaseTextUtils.firstNonBlank(ReleaseTextUtils.trimToNull(request.currentSha),
            runtime.headSha());
        ReleaseAutomationSupport.ReleaseDescriptor release = request.fresh
            ? descriptorFromFreshPlan(report)
            : descriptorFromManifest(report);
        String releaseVersion = release.releaseVersion;
        String tagName = release.primaryTagName();
        Path githubOutputFile = artifactSupport.resolveOptionalPath(request.githubOutputFile);

        AutomationJsonSupport.printLines(out, textOutput,
            ReleaseMessages.releaseVersionValueText(releaseVersion),
            ReleaseMessages.releaseTagValue(tagName),
            ReleaseMessages.targetCommitValue(currentSha)
        );
        if (githubOutputFile != null) {
            AutomationJsonSupport.printLines(out, textOutput, ReleaseMessages.githubOutputFileValue(githubOutputFile));
        }

        boolean shouldPublish = true;
        String reason = ReleaseMessages.githubReleasePublishShouldContinueReason();
        if (runtime.releaseExists(tagName)) {
            shouldPublish = false;
            reason = ReleaseMessages.githubReleaseAlreadyExistsSkipReason(tagName);
        } else {
            String headSubject = runtime.headSubject();
            String expectedSubject = "chore(release): apply changesets for " + tagName;
            if (request.requireReleaseApplyCommit && !expectedSubject.equals(headSubject)) {
                shouldPublish = false;
                reason = ReleaseMessages.githubHeadNotReleaseApplyCommitSkipReason(tagName);
            } else {
                String tagSha = runtime.remoteTagSha(tagName, "origin");
                if (tagSha != null && !tagSha.equals(currentSha)) {
                    throw new IllegalStateException(ReleaseMessages.githubTagPointsAtDifferentCommit(tagName,
                        tagSha, currentSha));
                }
            }
        }

        report.skipped = !shouldPublish;
        report.reason = reason;
        if (githubOutputFile != null) {
            Map<String, String> outputs = new LinkedHashMap<String, String>();
            outputs.put("release_version", releaseVersion);
            outputs.put("release_tag", tagName);
            outputs.put("should_publish", Boolean.toString(shouldPublish));
            appendGithubOutputs(githubOutputFile, outputs);
        }
        AutomationJsonSupport.print(out, textOutput, report, reason);
    }

    public void syncReleaseFromPlan(GithubReleasePublishRequest request) throws IOException, InterruptedException {
        boolean textOutput = isTextOutput(request.format);
        AutomationJsonSupport.AutomationReport report =
            newAutomationReport("github-release-from-plan", "sync-release", request.execute);
        ReleaseAutomationSupport.ReleaseDescriptor release = request.fresh
            ? descriptorFromFreshPlan(report)
            : descriptorFromManifest(report);
        String releaseVersion = release.releaseVersion;
        String tagName = release.primaryTagName();
        Path notesFile = artifactSupport.resolveReleaseNotesFile(request.releaseNotesFile);
        report.releaseNotesFile = notesFile.toString();

        AutomationJsonSupport.printLines(out, textOutput,
            ReleaseMessages.releaseVersionValueText(releaseVersion),
            ReleaseMessages.releaseTagValue(tagName),
            ReleaseMessages.releaseNotesFileValue(notesFile)
        );

        Path githubOutputFile = artifactSupport.resolveOptionalPath(request.githubOutputFile);
        if (githubOutputFile != null) {
            AutomationJsonSupport.printLines(out, textOutput,
                request.execute
                    ? ReleaseMessages.githubOutputFileValue(githubOutputFile)
                    : ReleaseMessages.githubOutputFileExecuteOnly(githubOutputFile));
        }

        if (skipDryRun(report, textOutput, ReleaseMessages.dryRunGithubRelease())) {
            return;
        }

        new ReleaseNotesGenerator(repoRoot).writeReleaseNotes(tagName, notesFile);
        if (githubOutputFile != null) {
            Map<String, String> outputs = new LinkedHashMap<String, String>();
            outputs.put("release_version", releaseVersion);
            outputs.put("release_tag", tagName);
            outputs.put("release_notes_file", notesFile.toString());
            appendGithubOutputs(githubOutputFile, outputs);
        }

        boolean existed = runtime.releaseExists(tagName);
        if (existed) {
            report.action = "update-release";
            report.reason = ReleaseMessages.updatedGithubReleaseReason();
            runtime.updateRelease(tagName, notesFile);
            AutomationJsonSupport.print(out, textOutput, report, ReleaseMessages.updatedGithubRelease(tagName));
            return;
        }

        report.action = "create-release";
        report.reason = ReleaseMessages.createdGithubReleaseReason();
        runtime.createRelease(tagName, notesFile);
        AutomationJsonSupport.print(out, textOutput, report, ReleaseMessages.createdGithubRelease(tagName));
    }

    private void appendGithubOutputs(Path outputFile, Map<String, String> outputs) throws IOException {
        Path parent = outputFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> output : outputs.entrySet()) {
            builder.append(output.getKey()).append('=').append(output.getValue()).append('\n');
        }
        Files.write(outputFile, builder.toString().getBytes(StandardCharsets.UTF_8),
            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private Path writeTransientReleasePlanBody(ReleasePlan plan) throws IOException {
        Path bodyFile = repoRoot.resolve("target").resolve("javachanges-release-plan.md");
        Files.createDirectories(bodyFile.getParent());
        Files.write(bodyFile, plan.toPullRequestBodyLines(), StandardCharsets.UTF_8);
        return bodyFile;
    }

}
