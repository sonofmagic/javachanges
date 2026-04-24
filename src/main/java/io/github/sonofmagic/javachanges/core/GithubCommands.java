package io.github.sonofmagic.javachanges.core;

import io.github.sonofmagic.javachanges.core.github.GithubReleasePlanRequest;
import io.github.sonofmagic.javachanges.core.github.GithubReleasePublishRequest;
import io.github.sonofmagic.javachanges.core.github.GithubReleaseSupport;
import io.github.sonofmagic.javachanges.core.github.GithubTagRequest;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Map;

@Command(name = "github-release-plan", mixinStandardHelpOptions = true,
    description = "Create or update a GitHub release-plan pull request.")
final class GithubReleasePlanCommand extends AbstractCliCommand {
    @Option(names = "--github-repo", description = "GitHub owner/repo.")
    private String githubRepo;

    @Option(names = "--target-branch", description = "Default branch to open the PR against.")
    private String targetBranch;

    @Option(names = "--release-branch", description = "Release plan branch name.")
    private String releaseBranch;

    @Option(names = "--execute", arity = "0..1", fallbackValue = "true", defaultValue = "false",
        description = "Call GitHub through gh instead of a dry run.")
    private boolean execute;

    @Option(names = "--format", description = "Output format: text or json.")
    private String format;

    @Override
    public Integer call() throws Exception {
        Map<String, String> options = options(
            option("github-repo", githubRepo),
            option("target-branch", targetBranch),
            option("release-branch", releaseBranch),
            flag("execute", execute),
            option("format", format)
        );
        GithubReleasePlanRequest request = GithubReleasePlanRequest.fromOptions(options);
        return runAutomationCommand("github-release-plan", request.format,
            () -> githubReleaseSupport().planPullRequest(request));
    }
}

@Command(name = "github-tag-from-plan", mixinStandardHelpOptions = true,
    description = "Tag and push a GitHub release from the generated release plan manifest.")
final class GithubTagFromPlanCommand extends AbstractCliCommand {
    @Option(names = "--current-sha", description = "Commit SHA to tag. Defaults to HEAD or GITHUB_SHA.")
    private String currentSha;

    @Option(names = "--execute", arity = "0..1", fallbackValue = "true", defaultValue = "false",
        description = "Push the release tag instead of a dry run.")
    private boolean execute;

    @Option(names = "--format", description = "Output format: text or json.")
    private String format;

    @Override
    public Integer call() throws Exception {
        Map<String, String> options = options(
            option("current-sha", currentSha),
            flag("execute", execute),
            option("format", format)
        );
        GithubTagRequest request = GithubTagRequest.fromOptions(options);
        return runAutomationCommand("github-tag-from-plan", request.format,
            () -> githubReleaseSupport().tagFromReleasePlan(request));
    }
}

@Command(name = "github-release-from-plan", mixinStandardHelpOptions = true,
    description = "Generate release notes and optionally create or update a GitHub Release from the release plan manifest.")
final class GithubReleaseFromPlanCommand extends AbstractCliCommand {
    @Option(names = "--release-notes-file",
        description = "Release notes output path. Relative paths resolve from the repository root.")
    private String releaseNotesFile;

    @Option(names = "--github-output-file",
        description = "Optional GitHub Actions output file. Defaults to GITHUB_OUTPUT when available.")
    private String githubOutputFile;

    @Option(names = "--execute", arity = "0..1", fallbackValue = "true", defaultValue = "false",
        description = "Create or update the GitHub Release through gh instead of a dry run.")
    private boolean execute;

    @Option(names = "--format", description = "Output format: text or json.")
    private String format;

    @Override
    public Integer call() throws Exception {
        Map<String, String> options = options(
            option("release-notes-file", releaseNotesFile),
            option("github-output-file", githubOutputFile),
            flag("execute", execute),
            option("format", format)
        );
        GithubReleasePublishRequest request = GithubReleasePublishRequest.fromOptions(options);
        return runAutomationCommand("github-release-from-plan", request.format,
            () -> githubReleaseSupport().syncReleaseFromPlan(request));
    }
}
