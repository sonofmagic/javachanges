package io.github.sonofmagic.javachanges.core.gitlab;

import io.github.sonofmagic.javachanges.core.OutputFormat;
import io.github.sonofmagic.javachanges.core.RequestConfigSupport;
import io.github.sonofmagic.javachanges.core.ReleaseUtils;

import java.util.Map;

public final class GitlabReleasePlanRequest {
    public final String projectId;
    public final String targetBranch;
    public final String releaseBranch;
    public final boolean execute;
    public final OutputFormat format;

    private GitlabReleasePlanRequest(String projectId, String targetBranch, String releaseBranch,
                                     boolean execute, OutputFormat format) {
        this.projectId = projectId;
        this.targetBranch = targetBranch;
        this.releaseBranch = releaseBranch;
        this.execute = execute;
        this.format = format;
    }

    public static GitlabReleasePlanRequest fromOptions(Map<String, String> options) {
        String repoRootOption = ReleaseUtils.trimToNull(options.get("directory"));
        String targetBranch = ReleaseUtils.firstNonBlank(
            ReleaseUtils.trimToNull(options.get("target-branch")),
            System.getenv("CI_DEFAULT_BRANCH")
        );
        if (targetBranch == null) {
            targetBranch = RequestConfigSupport.readConfiguredBaseBranch(repoRootOption);
        }
        String releaseBranch = ReleaseUtils.trimToNull(options.get("release-branch"));
        if (releaseBranch == null) {
            releaseBranch = RequestConfigSupport.readConfiguredReleaseBranch(repoRootOption, targetBranch);
        }
        return new GitlabReleasePlanRequest(
            ReleaseUtils.firstNonBlank(ReleaseUtils.trimToNull(options.get("project-id")), System.getenv("CI_PROJECT_ID")),
            targetBranch,
            releaseBranch,
            ReleaseUtils.isTrue(options.get("execute")),
            OutputFormat.parse(options.get("format"), OutputFormat.TEXT)
        );
    }
}
