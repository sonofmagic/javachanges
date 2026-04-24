package io.github.sonofmagic.javachanges.core.github;

import io.github.sonofmagic.javachanges.core.OutputFormat;
import io.github.sonofmagic.javachanges.core.ReleaseTextUtils;

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
        String releaseNotesFile = ReleaseTextUtils.trimToNull(options.get("release-notes-file"));
        return new GithubReleasePublishRequest(
            releaseNotesFile == null ? "target/release-notes.md" : releaseNotesFile,
            ReleaseTextUtils.firstNonBlank(ReleaseTextUtils.trimToNull(options.get("github-output-file")),
                System.getenv("GITHUB_OUTPUT")),
            ReleaseTextUtils.isTrue(options.get("execute")),
            OutputFormat.parse(options.get("format"), OutputFormat.TEXT)
        );
    }
}
