package io.github.sonofmagic.javachanges.core.github;

import io.github.sonofmagic.javachanges.core.OutputFormat;
import io.github.sonofmagic.javachanges.core.ReleaseTextUtils;

import java.util.Map;

public final class GithubTagRequest {
    public final String currentSha;
    public final boolean execute;
    public final OutputFormat format;

    private GithubTagRequest(String currentSha, boolean execute, OutputFormat format) {
        this.currentSha = currentSha;
        this.execute = execute;
        this.format = format;
    }

    public static GithubTagRequest fromOptions(Map<String, String> options) {
        return new GithubTagRequest(
            ReleaseTextUtils.firstNonBlank(ReleaseTextUtils.trimToNull(options.get("current-sha")),
                System.getenv("GITHUB_SHA")),
            ReleaseTextUtils.isTrue(options.get("execute")),
            OutputFormat.parse(options.get("format"), OutputFormat.TEXT)
        );
    }
}
