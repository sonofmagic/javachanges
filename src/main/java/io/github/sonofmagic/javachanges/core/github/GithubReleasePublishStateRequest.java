package io.github.sonofmagic.javachanges.core.github;

import io.github.sonofmagic.javachanges.core.OutputFormat;
import io.github.sonofmagic.javachanges.core.ReleaseTextUtils;

import java.util.Map;

public final class GithubReleasePublishStateRequest {
    public final String currentSha;
    public final String githubOutputFile;
    public final boolean fresh;
    public final boolean requireReleaseApplyCommit;
    public final OutputFormat format;

    private GithubReleasePublishStateRequest(String currentSha, String githubOutputFile, boolean fresh,
                                             boolean requireReleaseApplyCommit, OutputFormat format) {
        this.currentSha = currentSha;
        this.githubOutputFile = githubOutputFile;
        this.fresh = fresh;
        this.requireReleaseApplyCommit = requireReleaseApplyCommit;
        this.format = format;
    }

    public static GithubReleasePublishStateRequest fromOptions(Map<String, String> options) {
        return new GithubReleasePublishStateRequest(
            ReleaseTextUtils.trimToNull(options.get("current-sha")),
            ReleaseTextUtils.firstNonBlank(ReleaseTextUtils.trimToNull(options.get("github-output-file")),
                System.getenv("GITHUB_OUTPUT")),
            ReleaseTextUtils.isTrue(options.get("fresh")),
            !ReleaseTextUtils.isFalse(options.get("require-release-apply-commit")),
            OutputFormat.parse(options.get("format"), OutputFormat.TEXT)
        );
    }
}
