package io.github.sonofmagic.javachanges.core;

import java.util.Map;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.firstNonBlank;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.isTrue;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.trimToNull;

final class GitlabReleasePlanRequest {
    final String projectId;
    final String targetBranch;
    final String releaseBranch;
    final boolean execute;
    final OutputFormat format;

    private GitlabReleasePlanRequest(String projectId, String targetBranch, String releaseBranch,
                                     boolean execute, OutputFormat format) {
        this.projectId = projectId;
        this.targetBranch = targetBranch;
        this.releaseBranch = releaseBranch;
        this.execute = execute;
        this.format = format;
    }

    static GitlabReleasePlanRequest fromOptions(Map<String, String> options) {
        String repoRootOption = trimToNull(options.get("directory"));
        String targetBranch = firstNonBlank(trimToNull(options.get("target-branch")), System.getenv("CI_DEFAULT_BRANCH"));
        if (targetBranch == null) {
            targetBranch = RequestConfigSupport.readConfiguredBaseBranch(repoRootOption);
        }
        String releaseBranch = trimToNull(options.get("release-branch"));
        if (releaseBranch == null) {
            releaseBranch = RequestConfigSupport.readConfiguredReleaseBranch(repoRootOption, targetBranch);
        }
        return new GitlabReleasePlanRequest(
            firstNonBlank(trimToNull(options.get("project-id")), System.getenv("CI_PROJECT_ID")),
            targetBranch,
            releaseBranch,
            isTrue(options.get("execute")),
            OutputFormat.parse(options.get("format"), OutputFormat.TEXT)
        );
    }
}

final class GitlabTagRequest {
    final String beforeSha;
    final String currentSha;
    final boolean execute;
    final String baseBranch;
    final String releaseBranch;
    final String currentBranch;
    final OutputFormat format;

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

    static GitlabTagRequest fromOptions(Map<String, String> options) {
        String repoRootOption = trimToNull(options.get("directory"));
        ChangesetConfigSupport.ChangesetConfig config =
            RequestConfigSupport.readConfiguredChangesetConfigOrDefaults(repoRootOption);
        return new GitlabTagRequest(
            firstNonBlank(trimToNull(options.get("before-sha")), System.getenv("CI_COMMIT_BEFORE_SHA")),
            firstNonBlank(trimToNull(options.get("current-sha")), System.getenv("CI_COMMIT_SHA")),
            isTrue(options.get("execute")),
            config.baseBranch(),
            config.releaseBranch(),
            trimToNull(System.getenv("CI_COMMIT_BRANCH")),
            OutputFormat.parse(options.get("format"), OutputFormat.TEXT)
        );
    }
}

final class GitlabReleaseRequest {
    final String tag;
    final String projectId;
    final String gitlabHost;
    final String releaseNotesFile;
    final boolean execute;
    final OutputFormat format;

    private GitlabReleaseRequest(String tag, String projectId, String gitlabHost, String releaseNotesFile,
                                 boolean execute, OutputFormat format) {
        this.tag = tag;
        this.projectId = projectId;
        this.gitlabHost = gitlabHost;
        this.releaseNotesFile = releaseNotesFile;
        this.execute = execute;
        this.format = format;
    }

    static GitlabReleaseRequest fromOptions(Map<String, String> options) {
        return new GitlabReleaseRequest(
            firstNonBlank(trimToNull(options.get("tag")), trimToNull(System.getenv("CI_COMMIT_TAG"))),
            firstNonBlank(trimToNull(options.get("project-id")), trimToNull(System.getenv("CI_PROJECT_ID"))),
            firstNonBlank(trimToNull(options.get("gitlab-host")), trimToNull(System.getenv("CI_SERVER_HOST"))),
            trimToNull(options.get("release-notes-file")),
            isTrue(options.get("execute")),
            OutputFormat.parse(options.get("format"), OutputFormat.TEXT)
        );
    }
}
