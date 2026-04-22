package io.github.sonofmagic.javachanges.core;

import java.util.Map;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.firstNonBlank;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.isTrue;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.trimToNull;

final class GithubReleasePlanRequest {
    final String githubRepo;
    final String targetBranch;
    final String releaseBranch;
    final boolean execute;
    final OutputFormat format;

    private GithubReleasePlanRequest(String githubRepo, String targetBranch, String releaseBranch,
                                     boolean execute, OutputFormat format) {
        this.githubRepo = githubRepo;
        this.targetBranch = targetBranch;
        this.releaseBranch = releaseBranch;
        this.execute = execute;
        this.format = format;
    }

    static GithubReleasePlanRequest fromOptions(Map<String, String> options) {
        String repoRootOption = trimToNull(options.get("directory"));
        String targetBranch = firstNonBlank(trimToNull(options.get("target-branch")), System.getenv("GITHUB_BASE_REF"));
        if (targetBranch == null) {
            targetBranch = RequestConfigSupport.readConfiguredBaseBranch(repoRootOption);
        }
        String releaseBranch = trimToNull(options.get("release-branch"));
        if (releaseBranch == null) {
            releaseBranch = RequestConfigSupport.readConfiguredReleaseBranch(repoRootOption, targetBranch);
        }
        return new GithubReleasePlanRequest(
            firstNonBlank(trimToNull(options.get("github-repo")), System.getenv("GITHUB_REPOSITORY")),
            targetBranch,
            releaseBranch,
            isTrue(options.get("execute")),
            OutputFormat.parse(options.get("format"), OutputFormat.TEXT)
        );
    }
}

final class GithubTagRequest {
    final String currentSha;
    final boolean execute;
    final OutputFormat format;

    private GithubTagRequest(String currentSha, boolean execute, OutputFormat format) {
        this.currentSha = currentSha;
        this.execute = execute;
        this.format = format;
    }

    static GithubTagRequest fromOptions(Map<String, String> options) {
        return new GithubTagRequest(
            firstNonBlank(trimToNull(options.get("current-sha")), System.getenv("GITHUB_SHA")),
            isTrue(options.get("execute")),
            OutputFormat.parse(options.get("format"), OutputFormat.TEXT)
        );
    }
}

final class GithubReleasePublishRequest {
    final String releaseNotesFile;
    final String githubOutputFile;
    final boolean execute;
    final OutputFormat format;

    private GithubReleasePublishRequest(String releaseNotesFile, String githubOutputFile,
                                       boolean execute, OutputFormat format) {
        this.releaseNotesFile = releaseNotesFile;
        this.githubOutputFile = githubOutputFile;
        this.execute = execute;
        this.format = format;
    }

    static GithubReleasePublishRequest fromOptions(Map<String, String> options) {
        return new GithubReleasePublishRequest(
            trimToNull(options.get("release-notes-file")) == null
                ? "target/release-notes.md"
                : trimToNull(options.get("release-notes-file")),
            firstNonBlank(trimToNull(options.get("github-output-file")), System.getenv("GITHUB_OUTPUT")),
            isTrue(options.get("execute")),
            OutputFormat.parse(options.get("format"), OutputFormat.TEXT)
        );
    }
}
