package io.github.sonofmagic.javachanges.core.gitlab;

import io.github.sonofmagic.javachanges.core.OutputFormat;
import io.github.sonofmagic.javachanges.core.ReleaseTextUtils;

import java.util.Map;

public final class GitlabReleaseRequest {
    public final String tag;
    public final String projectId;
    public final String gitlabHost;
    public final String releaseNotesFile;
    public final boolean execute;
    public final OutputFormat format;

    private GitlabReleaseRequest(String tag, String projectId, String gitlabHost, String releaseNotesFile,
                                 boolean execute, OutputFormat format) {
        this.tag = tag;
        this.projectId = projectId;
        this.gitlabHost = gitlabHost;
        this.releaseNotesFile = releaseNotesFile;
        this.execute = execute;
        this.format = format;
    }

    public static GitlabReleaseRequest fromOptions(Map<String, String> options) {
        return new GitlabReleaseRequest(
            ReleaseTextUtils.firstNonBlank(ReleaseTextUtils.trimToNull(options.get("tag")),
                ReleaseTextUtils.trimToNull(System.getenv("CI_COMMIT_TAG"))),
            ReleaseTextUtils.firstNonBlank(ReleaseTextUtils.trimToNull(options.get("project-id")),
                ReleaseTextUtils.trimToNull(System.getenv("CI_PROJECT_ID"))),
            ReleaseTextUtils.firstNonBlank(ReleaseTextUtils.trimToNull(options.get("gitlab-host")),
                ReleaseTextUtils.trimToNull(System.getenv("CI_SERVER_HOST"))),
            ReleaseTextUtils.trimToNull(options.get("release-notes-file")),
            ReleaseTextUtils.isTrue(options.get("execute")),
            OutputFormat.parse(options.get("format"), OutputFormat.TEXT)
        );
    }
}
