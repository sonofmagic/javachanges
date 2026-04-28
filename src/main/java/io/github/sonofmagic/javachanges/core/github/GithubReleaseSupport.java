package io.github.sonofmagic.javachanges.core.github;

import io.github.sonofmagic.javachanges.core.BuildModelSupport;
import io.github.sonofmagic.javachanges.core.ReleaseAutomationSupport;
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
import java.util.List;

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
            throw new IllegalArgumentException("Missing GitHub repo. Pass --github-repo or set GITHUB_REPOSITORY.");
        }

        ReleasePlan plan = automationSupport.plan();
        ReleaseAutomationSupport.ReleaseDescriptor release = descriptorFromPlan(plan, report);
        if (skipWhenNoPendingChangesets(plan, report, textOutput, "No pending changesets. Skip release PR.")) {
            return;
        }

        String releaseBranch = request.releaseBranch;
        String targetBranch = request.targetBranch;
        String commitMessage = release.commitMessage();
        String title = release.githubPullRequestTitle();

        AutomationJsonSupport.printLines(out, textOutput,
            "GitHub repo: " + request.githubRepo,
            "Release branch: " + releaseBranch,
            "Target branch: " + targetBranch,
            "Release version: " + release.releaseVersion
        );

        if (skipDryRun(report, textOutput, "Dry-run only. Use --execute true to create/update the GitHub PR.")) {
            return;
        }

        runtime.configureBotIdentity();
        runtime.runGit("switch", "-C", releaseBranch);
        RepoFiles.applyPlan(repoRoot, plan);
        runtime.runGit(concat("add", BuildModelSupport.releasePlanGitAddPaths(repoRoot)));
        if (runtime.hasNoStagedChanges()) {
            report.skipped = true;
            report.reason = "No staged release plan changes.";
            AutomationJsonSupport.print(out, textOutput, report,
                "No staged release plan changes. Skip release PR update.");
            return;
        }
        runtime.runGit("commit", "-m", commitMessage);
        runtime.runGit("push", "--force-with-lease", "origin", "HEAD:" + releaseBranch);

        String prNumber = runtime.findOpenPullRequestNumber(request.githubRepo, releaseBranch, targetBranch);
        if (ReleaseTextUtils.trimToNull(prNumber) == null) {
            report.action = "create-pull-request";
            report.reason = "Created GitHub pull request.";
            runtime.createPullRequest(request.githubRepo, releaseBranch, targetBranch, title,
                releasePlanMarkdownFile());
            AutomationJsonSupport.print(out, textOutput, report, "Created GitHub PR for " + title);
            return;
        }

        report.action = "update-pull-request";
        report.reason = "Updated GitHub pull request.";
        runtime.updatePullRequest(request.githubRepo, prNumber, title,
            releasePlanMarkdownFile());
        AutomationJsonSupport.print(out, textOutput, report, "Updated GitHub PR #" + prNumber);
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
        ReleaseAutomationSupport.ReleaseDescriptor release = descriptorFromManifest(report);
        List<String> tagNames = release.tagNames();

        AutomationJsonSupport.printLines(out, textOutput,
            "Release tags: " + tagNames,
            "Target commit: " + currentSha
        );

        for (String tagName : tagNames) {
            if (runtime.remoteTagExists(tagName, "origin")) {
                skipWhenRemoteTagExists(report, textOutput, tagName);
                return;
            }
        }

        if (skipDryRun(report, textOutput, "Dry-run only. Use --execute true to create and push the release tag.")) {
            return;
        }

        for (String tagName : tagNames) {
            runtime.createOrUpdateTag(tagName, currentSha);
            runtime.runGit("push", "origin", "refs/tags/" + tagName);
        }
        report.action = "create-tag";
        report.reason = "Created and pushed tag.";
        AutomationJsonSupport.print(out, textOutput, report, "Created and pushed tag(s) " + tagNames);
    }

    public void syncReleaseFromPlan(GithubReleasePublishRequest request) throws IOException, InterruptedException {
        boolean textOutput = isTextOutput(request.format);
        AutomationJsonSupport.AutomationReport report =
            newAutomationReport("github-release-from-plan", "sync-release", request.execute);
        ReleaseAutomationSupport.ReleaseDescriptor release = descriptorFromManifest(report);
        String releaseVersion = release.releaseVersion;
        String tagName = release.primaryTagName();
        Path notesFile = artifactSupport.resolveReleaseNotesFile(request.releaseNotesFile);
        report.releaseNotesFile = notesFile.toString();

        AutomationJsonSupport.printLines(out, textOutput,
            "Release version: " + releaseVersion,
            "Release tag: " + tagName,
            "Release notes file: " + notesFile
        );

        Path githubOutputFile = artifactSupport.resolveOptionalPath(request.githubOutputFile);
        if (githubOutputFile != null) {
            AutomationJsonSupport.printLines(out, textOutput,
                request.execute
                    ? "GitHub output file: " + githubOutputFile
                    : "GitHub output file: " + githubOutputFile + " (execute only)");
        }

        if (skipDryRun(report, textOutput, "Dry-run only. Use --execute true to create/update the GitHub Release.")) {
            return;
        }

        new ReleaseNotesGenerator(repoRoot).writeReleaseNotes(tagName, notesFile);
        if (githubOutputFile != null) {
            appendGithubOutputs(githubOutputFile, releaseVersion, tagName, notesFile);
        }

        boolean existed = runtime.releaseExists(tagName);
        if (existed) {
            report.action = "update-release";
            report.reason = "Updated GitHub Release.";
            runtime.updateRelease(tagName, notesFile);
            AutomationJsonSupport.print(out, textOutput, report, "Updated GitHub Release " + tagName);
            return;
        }

        report.action = "create-release";
        report.reason = "Created GitHub Release.";
        runtime.createRelease(tagName, notesFile);
        AutomationJsonSupport.print(out, textOutput, report, "Created GitHub Release " + tagName);
    }

    private void appendGithubOutputs(Path outputFile, String releaseVersion, String tagName, Path notesFile) throws IOException {
        Path parent = outputFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        StringBuilder builder = new StringBuilder();
        builder.append("release_version=").append(releaseVersion).append('\n');
        builder.append("release_tag=").append(tagName).append('\n');
        builder.append("release_notes_file=").append(notesFile.toString()).append('\n');
        Files.write(outputFile, builder.toString().getBytes(StandardCharsets.UTF_8),
            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

}
