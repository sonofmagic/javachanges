package io.github.sonofmagic.javachanges.core.gitlab;

import io.github.sonofmagic.javachanges.core.OutputFormat;
import io.github.sonofmagic.javachanges.core.ReleaseUtils;

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
            ReleaseUtils.firstNonBlank(ReleaseUtils.trimToNull(options.get("tag")),
                ReleaseUtils.trimToNull(System.getenv("CI_COMMIT_TAG"))),
            ReleaseUtils.firstNonBlank(ReleaseUtils.trimToNull(options.get("project-id")),
                ReleaseUtils.trimToNull(System.getenv("CI_PROJECT_ID"))),
            ReleaseUtils.firstNonBlank(ReleaseUtils.trimToNull(options.get("gitlab-host")),
                ReleaseUtils.trimToNull(System.getenv("CI_SERVER_HOST"))),
            ReleaseUtils.trimToNull(options.get("release-notes-file")),
            ReleaseUtils.isTrue(options.get("execute")),
            OutputFormat.parse(options.get("format"), OutputFormat.TEXT)
        );
    }
}
