package io.github.sonofmagic.javachanges.core;

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

    @Override
    public Integer call() throws Exception {
        Map<String, String> options = options();
        putOption(options, "github-repo", githubRepo);
        putOption(options, "target-branch", targetBranch);
        putOption(options, "release-branch", releaseBranch);
        putFlag(options, "execute", execute);
        new GithubReleaseSupport(repoRoot(), out()).planPullRequest(GithubReleasePlanRequest.fromOptions(options));
        return success();
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

    @Override
    public Integer call() throws Exception {
        Map<String, String> options = options();
        putOption(options, "current-sha", currentSha);
        putFlag(options, "execute", execute);
        new GithubReleaseSupport(repoRoot(), out()).tagFromReleasePlan(GithubTagRequest.fromOptions(options));
        return success();
    }
}
