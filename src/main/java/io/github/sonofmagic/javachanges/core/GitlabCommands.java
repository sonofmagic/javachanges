package io.github.sonofmagic.javachanges.core;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Map;

@Command(name = "gitlab-release-plan", mixinStandardHelpOptions = true,
    description = "Create or update a GitLab release-plan merge request.")
final class GitlabReleasePlanCommand extends AbstractCliCommand {
    @Option(names = "--project-id", description = "GitLab project ID.")
    private String projectId;

    @Option(names = "--target-branch", description = "Default branch to open the MR against.")
    private String targetBranch;

    @Option(names = "--release-branch", description = "Release plan branch name.")
    private String releaseBranch;

    @Option(names = "--execute", arity = "0..1", fallbackValue = "true", defaultValue = "false",
        description = "Call the GitLab API instead of a dry run.")
    private boolean execute;

    @Override
    public Integer call() throws Exception {
        Map<String, String> options = options();
        putOption(options, "project-id", projectId);
        putOption(options, "target-branch", targetBranch);
        putOption(options, "release-branch", releaseBranch);
        putFlag(options, "execute", execute);
        new GitlabReleaseSupport(repoRoot(), out()).planMergeRequest(GitlabReleasePlanRequest.fromOptions(options));
        return success();
    }
}

@Command(name = "gitlab-tag-from-plan", mixinStandardHelpOptions = true,
    description = "Tag a release from the generated release plan manifest.")
final class GitlabTagFromPlanCommand extends AbstractCliCommand {
    @Option(names = "--before-sha", description = "Previous commit SHA.")
    private String beforeSha;

    @Option(names = "--current-sha", description = "Current commit SHA.")
    private String currentSha;

    @Option(names = "--execute", arity = "0..1", fallbackValue = "true", defaultValue = "false",
        description = "Push tags instead of a dry run.")
    private boolean execute;

    @Override
    public Integer call() throws Exception {
        Map<String, String> options = options();
        putOption(options, "before-sha", beforeSha);
        putOption(options, "current-sha", currentSha);
        putFlag(options, "execute", execute);
        new GitlabReleaseSupport(repoRoot(), out()).tagFromReleasePlan(GitlabTagRequest.fromOptions(options));
        return success();
    }
}
