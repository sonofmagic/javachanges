package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.*;

final class GithubReleaseSupport {
    private final Path repoRoot;
    private final PrintStream out;
    private final GithubReleaseRuntime runtime;
    private final ReleaseArtifactSupport artifactSupport;
    private final ReleaseAutomationSupport automationSupport;

    GithubReleaseSupport(Path repoRoot, PrintStream out) {
        this(repoRoot, out, new GithubReleaseRuntime(repoRoot));
    }

    GithubReleaseSupport(Path repoRoot, PrintStream out, GithubReleaseRuntime runtime) {
        this.repoRoot = repoRoot;
        this.out = out;
        this.runtime = runtime;
        this.artifactSupport = new ReleaseArtifactSupport(repoRoot);
        this.automationSupport = new ReleaseAutomationSupport(repoRoot);
    }

    void planPullRequest(GithubReleasePlanRequest request) throws IOException, InterruptedException {
        boolean textOutput = request.format != OutputFormat.JSON;
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
            if (textOutput) {
                out.println("No pending changesets. Skip release PR.");
            } else {
                out.println(report.toJson());
            }
            return;
        }

        String releaseBranch = request.releaseBranch;
        String targetBranch = request.targetBranch;
        String commitMessage = release.commitMessage();
        String title = release.githubPullRequestTitle();

        if (textOutput) {
            out.println("GitHub repo: " + request.githubRepo);
            out.println("Release branch: " + releaseBranch);
            out.println("Target branch: " + targetBranch);
            out.println("Release version: " + release.releaseVersion);
        }

        if (!request.execute) {
            report.reason = "Dry-run only.";
            if (textOutput) {
                out.println("Dry-run only. Use --execute true to create/update the GitHub PR.");
            } else {
                out.println(report.toJson());
            }
            return;
        }

        runtime.configureBotIdentity();
        runtime.runGit("switch", "-C", releaseBranch);
        RepoFiles.applyPlan(repoRoot, plan);
        runtime.runGit("add", "pom.xml", "CHANGELOG.md", CHANGESETS_DIR);
        if (runtime.hasNoStagedChanges()) {
            report.skipped = true;
            report.reason = "No staged release plan changes.";
            if (textOutput) {
                out.println("No staged release plan changes. Skip release PR update.");
            } else {
                out.println(report.toJson());
            }
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
            if (textOutput) {
                out.println("Created GitHub PR for " + title);
            } else {
                out.println(report.toJson());
            }
            return;
        }

        report.action = "update-pull-request";
        report.reason = "Updated GitHub pull request.";
        runtime.updatePullRequest(request.githubRepo, prNumber, title,
            automationSupport.releasePlanMarkdownFile());
        if (textOutput) {
            out.println("Updated GitHub PR #" + prNumber);
        } else {
            out.println(report.toJson());
        }
    }

    void tagFromReleasePlan(GithubTagRequest request) throws IOException, InterruptedException {
        boolean textOutput = request.format != OutputFormat.JSON;
        AutomationJsonSupport.AutomationReport report = new AutomationJsonSupport.AutomationReport("github-tag-from-plan");
        report.action = "tag-from-plan";
        report.execute = request.execute;
        report.dryRun = !request.execute;
        String currentSha = firstNonBlank(trimToNull(request.currentSha), runtime.headSha());
        ReleaseAutomationSupport.ReleaseDescriptor release = automationSupport.descriptorFromManifest();
        String tagName = release.wholeRepoTagName();
        report.releaseVersion = release.releaseVersion;
        report.tag = tagName;

        if (textOutput) {
            out.println("Release tag: " + tagName);
            out.println("Target commit: " + currentSha);
        }

        if (runtime.remoteTagExists(tagName, "origin")) {
            report.skipped = true;
            report.reason = "Tag already exists remotely.";
            if (textOutput) {
                out.println("Tag already exists remotely. Skip.");
            } else {
                out.println(report.toJson());
            }
            return;
        }

        if (!request.execute) {
            report.reason = "Dry-run only.";
            if (textOutput) {
                out.println("Dry-run only. Use --execute true to create and push the release tag.");
            } else {
                out.println(report.toJson());
            }
            return;
        }

        runtime.createOrUpdateTag(tagName, currentSha);
        runtime.runGit("push", "origin", "refs/tags/" + tagName);
        report.action = "create-tag";
        report.reason = "Created and pushed tag.";
        if (textOutput) {
            out.println("Created and pushed tag " + tagName);
        } else {
            out.println(report.toJson());
        }
    }

    void syncReleaseFromPlan(GithubReleasePublishRequest request) throws IOException, InterruptedException {
        boolean textOutput = request.format != OutputFormat.JSON;
        AutomationJsonSupport.AutomationReport report = new AutomationJsonSupport.AutomationReport("github-release-from-plan");
        report.action = "sync-release";
        report.execute = request.execute;
        report.dryRun = !request.execute;
        ReleaseAutomationSupport.ReleaseDescriptor release = automationSupport.descriptorFromManifest();
        String releaseVersion = release.releaseVersion;
        String tagName = release.wholeRepoTagName();
        report.releaseVersion = releaseVersion;
        report.tag = tagName;
        Path notesFile = artifactSupport.resolveReleaseNotesFile(request.releaseNotesFile);
        report.releaseNotesFile = notesFile.toString();
        new ReleaseNotesGenerator(repoRoot).writeReleaseNotes(tagName, notesFile);

        if (textOutput) {
            out.println("Release version: " + releaseVersion);
            out.println("Release tag: " + tagName);
            out.println("Release notes file: " + notesFile);
        }

        Path githubOutputFile = artifactSupport.resolveOptionalPath(request.githubOutputFile);
        if (githubOutputFile != null) {
            appendGithubOutputs(githubOutputFile, releaseVersion, tagName, notesFile);
            if (textOutput) {
                out.println("GitHub output file: " + githubOutputFile);
            }
        }

        if (!request.execute) {
            report.reason = "Dry-run only.";
            if (textOutput) {
                out.println("Dry-run only. Use --execute true to create/update the GitHub Release.");
            } else {
                out.println(report.toJson());
            }
            return;
        }

        boolean existed = runtime.releaseExists(tagName);
        if (existed) {
            report.action = "update-release";
            report.reason = "Updated GitHub Release.";
            runtime.updateRelease(tagName, notesFile);
            if (textOutput) {
                out.println("Updated GitHub Release " + tagName);
            } else {
                out.println(report.toJson());
            }
            return;
        }

        report.action = "create-release";
        report.reason = "Created GitHub Release.";
        runtime.createRelease(tagName, notesFile);
        if (textOutput) {
            out.println("Created GitHub Release " + tagName);
        } else {
            out.println(report.toJson());
        }
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
