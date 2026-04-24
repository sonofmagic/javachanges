package io.github.sonofmagic.javachanges.core.github;

import io.github.sonofmagic.javachanges.core.OutputFormat;
import io.github.sonofmagic.javachanges.core.RequestConfigSupport;
import io.github.sonofmagic.javachanges.core.ReleaseUtils;

import java.util.Map;

public final class GithubReleasePlanRequest {
    public final String githubRepo;
    public final String targetBranch;
    public final String releaseBranch;
    public final boolean execute;
    public final OutputFormat format;

    private GithubReleasePlanRequest(String githubRepo, String targetBranch, String releaseBranch,
                                     boolean execute, OutputFormat format) {
        this.githubRepo = githubRepo;
        this.targetBranch = targetBranch;
        this.releaseBranch = releaseBranch;
        this.execute = execute;
        this.format = format;
    }

    public static GithubReleasePlanRequest fromOptions(Map<String, String> options) {
        String repoRootOption = ReleaseUtils.trimToNull(options.get("directory"));
        String targetBranch = ReleaseUtils.firstNonBlank(
            ReleaseUtils.trimToNull(options.get("target-branch")),
            System.getenv("GITHUB_BASE_REF")
        );
        if (targetBranch == null) {
            targetBranch = RequestConfigSupport.readConfiguredBaseBranch(repoRootOption);
        }
        String releaseBranch = ReleaseUtils.trimToNull(options.get("release-branch"));
        if (releaseBranch == null) {
            releaseBranch = RequestConfigSupport.readConfiguredReleaseBranch(repoRootOption, targetBranch);
        }
        return new GithubReleasePlanRequest(
            ReleaseUtils.firstNonBlank(ReleaseUtils.trimToNull(options.get("github-repo")), System.getenv("GITHUB_REPOSITORY")),
            targetBranch,
            releaseBranch,
            ReleaseUtils.isTrue(options.get("execute")),
            OutputFormat.parse(options.get("format"), OutputFormat.TEXT)
        );
    }
}
