package io.github.sonofmagic.javachanges.core.github;

import io.github.sonofmagic.javachanges.core.AutomationJsonSupport;
import io.github.sonofmagic.javachanges.core.ReleaseArtifactSupport;
import io.github.sonofmagic.javachanges.core.ReleaseAutomationSupport;
import io.github.sonofmagic.javachanges.core.ReleaseNotesGenerator;
import io.github.sonofmagic.javachanges.core.plan.ReleasePlan;
import io.github.sonofmagic.javachanges.core.plan.RepoFiles;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.CHANGESETS_DIR;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.firstNonBlank;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.trimToNull;

public final class GithubReleaseSupport {
    private final Path repoRoot;
    private final PrintStream out;
    private final GithubReleaseRuntime runtime;
    private final ReleaseArtifactSupport artifactSupport;
    private final ReleaseAutomationSupport automationSupport;

    public GithubReleaseSupport(Path repoRoot, PrintStream out) {
        this(repoRoot, out, new GithubReleaseRuntime(repoRoot));
    }

    public GithubReleaseSupport(Path repoRoot, PrintStream out, GithubReleaseRuntime runtime) {
        this.repoRoot = repoRoot;
        this.out = out;
        this.runtime = runtime;
        this.artifactSupport = new ReleaseArtifactSupport(repoRoot);
        this.automationSupport = new ReleaseAutomationSupport(repoRoot);
    }

    public void planPullRequest(GithubReleasePlanRequest request) throws IOException, InterruptedException {
        boolean textOutput = AutomationJsonSupport.isText(request.format);
        AutomationJsonSupport.AutomationReport report = new AutomationJsonSupport.AutomationReport("github-release-plan");
        report.action = "plan-pull-request";
        report.execute = request.execute;
        report.dryRun = !request.execute;
        if (trimToNull(request.githubRepo) == null) {
            throw new IllegalArgumentException("Missing GitHub repo. Pass --github-repo or set GITHUB_REPOSITORY.");
        }

        ReleasePlan plan = automationSupport.plan();
        ReleaseAutomationSupport.ReleaseDescriptor release = automationSupport.descriptorFromPlan(plan);
        report.releaseVersion = release.releaseVersion;
        if (!plan.hasPendingChangesets()) {
            report.skipped = true;
            report.reason = "No pending changesets.";
            AutomationJsonSupport.print(out, textOutput, report, "No pending changesets. Skip release PR.");
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

        if (!request.execute) {
            report.reason = "Dry-run only.";
            AutomationJsonSupport.print(out, textOutput, report,
                "Dry-run only. Use --execute true to create/update the GitHub PR.");
            return;
        }

        runtime.configureBotIdentity();
        runtime.runGit("switch", "-C", releaseBranch);
        RepoFiles.applyPlan(repoRoot, plan);
        runtime.runGit("add", "pom.xml", "CHANGELOG.md", CHANGESETS_DIR);
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
        if (trimToNull(prNumber) == null) {
            report.action = "create-pull-request";
            report.reason = "Created GitHub pull request.";
            runtime.createPullRequest(request.githubRepo, releaseBranch, targetBranch, title,
                automationSupport.releasePlanMarkdownFile());
            AutomationJsonSupport.print(out, textOutput, report, "Created GitHub PR for " + title);
            return;
        }

        report.action = "update-pull-request";
        report.reason = "Updated GitHub pull request.";
        runtime.updatePullRequest(request.githubRepo, prNumber, title,
            automationSupport.releasePlanMarkdownFile());
        AutomationJsonSupport.print(out, textOutput, report, "Updated GitHub PR #" + prNumber);
    }

    public void tagFromReleasePlan(GithubTagRequest request) throws IOException, InterruptedException {
        boolean textOutput = AutomationJsonSupport.isText(request.format);
        AutomationJsonSupport.AutomationReport report = new AutomationJsonSupport.AutomationReport("github-tag-from-plan");
        report.action = "tag-from-plan";
        report.execute = request.execute;
        report.dryRun = !request.execute;
        String currentSha = firstNonBlank(trimToNull(request.currentSha), runtime.headSha());
        ReleaseAutomationSupport.ReleaseDescriptor release = automationSupport.descriptorFromManifest();
        report.releaseVersion = release.releaseVersion;
        report.tagStrategy = release.tagStrategy.id;
        report.tags = release.tagNames();
        report.tag = release.releaseTargets.size() == 1 ? release.tagNames().get(0) : null;
        List<String> tagNames = release.tagNames();

        AutomationJsonSupport.printLines(out, textOutput,
            "Release tags: " + tagNames,
            "Target commit: " + currentSha
        );

        for (String tagName : tagNames) {
            if (runtime.remoteTagExists(tagName, "origin")) {
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
            runtime.createOrUpdateTag(tagName, currentSha);
            runtime.runGit("push", "origin", "refs/tags/" + tagName);
        }
        report.action = "create-tag";
        report.reason = "Created and pushed tag.";
        AutomationJsonSupport.print(out, textOutput, report, "Created and pushed tag(s) " + tagNames);
    }

    public void syncReleaseFromPlan(GithubReleasePublishRequest request) throws IOException, InterruptedException {
        boolean textOutput = AutomationJsonSupport.isText(request.format);
        AutomationJsonSupport.AutomationReport report = new AutomationJsonSupport.AutomationReport("github-release-from-plan");
        report.action = "sync-release";
        report.execute = request.execute;
        report.dryRun = !request.execute;
        ReleaseAutomationSupport.ReleaseDescriptor release = automationSupport.descriptorFromManifest();
        String releaseVersion = release.releaseVersion;
        String tagName = release.primaryTagName();
        report.releaseVersion = releaseVersion;
        report.tag = tagName;
        report.tagStrategy = release.tagStrategy.id;
        report.tags = release.tagNames();
        Path notesFile = artifactSupport.resolveReleaseNotesFile(request.releaseNotesFile);
        report.releaseNotesFile = notesFile.toString();
        new ReleaseNotesGenerator(repoRoot).writeReleaseNotes(tagName, notesFile);

        AutomationJsonSupport.printLines(out, textOutput,
            "Release version: " + releaseVersion,
            "Release tag: " + tagName,
            "Release notes file: " + notesFile
        );

        Path githubOutputFile = artifactSupport.resolveOptionalPath(request.githubOutputFile);
        if (githubOutputFile != null) {
            appendGithubOutputs(githubOutputFile, releaseVersion, tagName, notesFile);
            AutomationJsonSupport.printLines(out, textOutput, "GitHub output file: " + githubOutputFile);
        }

        if (!request.execute) {
            report.reason = "Dry-run only.";
            AutomationJsonSupport.print(out, textOutput, report,
                "Dry-run only. Use --execute true to create/update the GitHub Release.");
            return;
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
