package io.github.sonofmagic.javachanges.core.gitlab;

import io.github.sonofmagic.javachanges.core.OutputFormat;
import io.github.sonofmagic.javachanges.core.ReleaseTextUtils;
import io.github.sonofmagic.javachanges.core.config.ChangesetConfigSupport;
import io.github.sonofmagic.javachanges.core.config.RequestConfigSupport;

import java.util.Map;

public final class GitlabTagRequest {
    public final String beforeSha;
    public final String currentSha;
    public final boolean execute;
    public final String baseBranch;
    public final String releaseBranch;
    public final String currentBranch;
    public final OutputFormat format;

    private GitlabTagRequest(String beforeSha, String currentSha, boolean execute,
                             String baseBranch, String releaseBranch, String currentBranch,
                             OutputFormat format) {
        this.beforeSha = beforeSha;
        this.currentSha = currentSha;
        this.execute = execute;
        this.baseBranch = baseBranch;
        this.releaseBranch = releaseBranch;
        this.currentBranch = currentBranch;
        this.format = format;
    }

    public static GitlabTagRequest fromOptions(Map<String, String> options) {
        String repoRootOption = ReleaseTextUtils.trimToNull(options.get("directory"));
        ChangesetConfigSupport.ChangesetConfig config =
            RequestConfigSupport.readConfiguredChangesetConfigOrDefaults(repoRootOption);
        return new GitlabTagRequest(
            ReleaseTextUtils.firstNonBlank(ReleaseTextUtils.trimToNull(options.get("before-sha")),
                System.getenv("CI_COMMIT_BEFORE_SHA")),
            ReleaseTextUtils.firstNonBlank(ReleaseTextUtils.trimToNull(options.get("current-sha")),
                System.getenv("CI_COMMIT_SHA")),
            ReleaseTextUtils.isTrue(options.get("execute")),
            config.baseBranch(),
            config.releaseBranch(),
            ReleaseTextUtils.trimToNull(System.getenv("CI_COMMIT_BRANCH")),
            OutputFormat.parse(options.get("format"), OutputFormat.TEXT)
        );
    }
}
