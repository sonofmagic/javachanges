package io.github.sonofmagic.javachanges.core.github;

import io.github.sonofmagic.javachanges.core.OutputFormat;
import io.github.sonofmagic.javachanges.core.ReleaseUtils;

import java.util.Map;

public final class GithubReleasePublishRequest {
    public final String releaseNotesFile;
    public final String githubOutputFile;
    public final boolean execute;
    public final OutputFormat format;

    private GithubReleasePublishRequest(String releaseNotesFile, String githubOutputFile,
                                        boolean execute, OutputFormat format) {
        this.releaseNotesFile = releaseNotesFile;
        this.githubOutputFile = githubOutputFile;
        this.execute = execute;
        this.format = format;
    }

    public static GithubReleasePublishRequest fromOptions(Map<String, String> options) {
        return new GithubReleasePublishRequest(
            ReleaseUtils.trimToNull(options.get("release-notes-file")) == null
                ? "target/release-notes.md"
                : ReleaseUtils.trimToNull(options.get("release-notes-file")),
            ReleaseUtils.firstNonBlank(ReleaseUtils.trimToNull(options.get("github-output-file")), System.getenv("GITHUB_OUTPUT")),
            ReleaseUtils.isTrue(options.get("execute")),
            OutputFormat.parse(options.get("format"), OutputFormat.TEXT)
        );
    }
}
