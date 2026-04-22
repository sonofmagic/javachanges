package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.firstNonBlank;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.isTrue;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.platformOption;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.requiredOption;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.trimToNull;

final class InitEnvRequest {
    final String template;
    final String target;
    final boolean force;

    private InitEnvRequest(String template, String target, boolean force) {
        this.template = template;
        this.target = target;
        this.force = force;
    }

    static InitEnvRequest fromOptions(Map<String, String> options) {
        String target = trimToNull(options.get("target"));
        if (target == null) {
            target = trimToNull(options.get("path"));
        }
        return new InitEnvRequest(
            trimToNull(options.get("template")) == null ? "env/release.env.example" : trimToNull(options.get("template")),
            target == null ? "env/release.env.local" : target,
            isTrue(options.get("force"))
        );
    }
}

final class PlatformEnvRequest {
    final String envFile;
    final Platform platform;
    final boolean showSecrets;
    final OutputFormat format;

    private PlatformEnvRequest(String envFile, Platform platform, boolean showSecrets, OutputFormat format) {
        this.envFile = envFile;
        this.platform = platform;
        this.showSecrets = showSecrets;
        this.format = format;
    }

    static PlatformEnvRequest fromOptions(Map<String, String> options) {
        return new PlatformEnvRequest(
            requiredOption(options, "env-file"),
            platformOption(options),
            isTrue(options.get("show-secrets")),
            OutputFormat.parse(options.get("format"), OutputFormat.TEXT)
        );
    }
}

final class LocalDoctorRequest {
    final String envFile;
    final String githubRepo;
    final String gitlabRepo;
    final OutputFormat format;

    private LocalDoctorRequest(String envFile, String githubRepo, String gitlabRepo, OutputFormat format) {
        this.envFile = envFile;
        this.githubRepo = githubRepo;
        this.gitlabRepo = gitlabRepo;
        this.format = format;
    }

    static LocalDoctorRequest fromOptions(Map<String, String> options) {
        return new LocalDoctorRequest(
            trimToNull(options.get("env-file")) == null ? "env/release.env.local" : trimToNull(options.get("env-file")),
            trimToNull(options.get("github-repo")),
            trimToNull(options.get("gitlab-repo")),
            OutputFormat.parse(options.get("format"), OutputFormat.TEXT)
        );
    }
}

final class DoctorPlatformRequest {
    final String envFile;
    final Platform platform;
    final String githubRepo;
    final String gitlabRepo;
    final OutputFormat format;

    private DoctorPlatformRequest(String envFile, Platform platform, String githubRepo, String gitlabRepo,
                                  OutputFormat format) {
        this.envFile = envFile;
        this.platform = platform;
        this.githubRepo = githubRepo;
        this.gitlabRepo = gitlabRepo;
        this.format = format;
    }

    static DoctorPlatformRequest fromOptions(Map<String, String> options) {
        return new DoctorPlatformRequest(
            requiredOption(options, "env-file"),
            platformOption(options),
            trimToNull(options.get("github-repo")),
            trimToNull(options.get("gitlab-repo")),
            OutputFormat.parse(options.get("format"), OutputFormat.TEXT)
        );
    }
}

final class SyncVarsRequest {
    final String envFile;
    final Platform platform;
    final String repo;
    final boolean execute;
    final boolean showSecrets;

    private SyncVarsRequest(String envFile, Platform platform, String repo, boolean execute, boolean showSecrets) {
        this.envFile = envFile;
        this.platform = platform;
        this.repo = repo;
        this.execute = execute;
        this.showSecrets = showSecrets;
    }

    static SyncVarsRequest fromOptions(Map<String, String> options) {
        return new SyncVarsRequest(
            requiredOption(options, "env-file"),
            platformOption(options),
            trimToNull(options.get("repo")),
            isTrue(options.get("execute")),
            isTrue(options.get("show-secrets"))
        );
    }
}

final class AuditVarsRequest {
    final String envFile;
    final Platform platform;
    final String githubRepo;
    final String gitlabRepo;
    final OutputFormat format;

    private AuditVarsRequest(String envFile, Platform platform, String githubRepo, String gitlabRepo,
                             OutputFormat format) {
        this.envFile = envFile;
        this.platform = platform;
        this.githubRepo = githubRepo;
        this.gitlabRepo = gitlabRepo;
        this.format = format;
    }

    static AuditVarsRequest fromOptions(Map<String, String> options) {
        return new AuditVarsRequest(
            requiredOption(options, "env-file"),
            platformOption(options),
            trimToNull(options.get("github-repo")),
            trimToNull(options.get("gitlab-repo")),
            OutputFormat.parse(options.get("format"), OutputFormat.TEXT)
        );
    }
}

final class PublishRequest {
    final boolean snapshot;
    final String tag;
    final boolean allowDirty;
    final boolean execute;
    final String module;
    final String snapshotBuildStamp;
    final OutputFormat format;

    private PublishRequest(boolean snapshot, String tag, boolean allowDirty, boolean execute, String module,
                           String snapshotBuildStamp, OutputFormat format) {
        this.snapshot = snapshot;
        this.tag = tag;
        this.allowDirty = allowDirty;
        this.execute = execute;
        this.module = module;
        this.snapshotBuildStamp = snapshotBuildStamp;
        this.format = format;
    }

    static PublishRequest fromOptions(Map<String, String> options, boolean supportExecute) {
        String directoryOption = trimToNull(options.get("directory"));
        boolean snapshot = isTrue(options.get("snapshot"));
        String tag = firstNonBlank(trimToNull(options.get("tag")), trimToNull(System.getenv("CI_COMMIT_TAG")));
        if (!snapshot && tag == null && shouldDefaultToSnapshot(directoryOption)) {
            snapshot = true;
        }
        if (!snapshot && tag == null) {
            throw new IllegalArgumentException("必须指定 --snapshot true 或 --tag <value>");
        }
        if (snapshot && tag != null) {
            throw new IllegalArgumentException("--snapshot 和 --tag 不能同时使用");
        }
        return new PublishRequest(
            snapshot,
            tag,
            isTrue(options.get("allow-dirty")),
            supportExecute && isTrue(options.get("execute")),
            trimToNull(options.get("module")),
            firstNonBlank(trimToNull(options.get("snapshot-build-stamp")),
                System.getenv("JAVACHANGES_SNAPSHOT_BUILD_STAMP")),
            OutputFormat.parse(options.get("format"), OutputFormat.TEXT)
        );
    }

    private static boolean shouldDefaultToSnapshot(String directoryOption) {
        String currentBranch = firstNonBlank(trimToNull(System.getenv("CI_COMMIT_BRANCH")),
            trimToNull(System.getenv("GITHUB_REF_NAME")));
        if (currentBranch == null) {
            return false;
        }
        try {
            ChangesetConfigSupport.ChangesetConfig config = readConfiguredChangesetConfig(directoryOption);
            return currentBranch.equals(config.snapshotBranch());
        } catch (Exception ignored) {
            return "snapshot".equals(currentBranch);
        }
    }

    static ChangesetConfigSupport.ChangesetConfig readConfiguredChangesetConfig(String directoryOption) throws IOException {
        Path configuredRoot = resolveConfigRoot(directoryOption);
        if (configuredRoot != null) {
            try {
                return RepoFiles.readChangesetConfig(configuredRoot);
            } catch (Exception ignored) {
            }
        }
        return RepoFiles.readChangesetConfig(RepoFiles.resolveRepoRoot(directoryOption));
    }

    private static Path resolveConfigRoot(String directoryOption) {
        if (directoryOption == null) {
            return null;
        }
        return ChangesetConfigSupport.resolveConfigRoot(Paths.get(directoryOption));
    }
}

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
            targetBranch = readConfiguredBaseBranch(repoRootOption);
        }
        String releaseBranch = trimToNull(options.get("release-branch"));
        if (releaseBranch == null) {
            releaseBranch = readConfiguredReleaseBranch(repoRootOption, targetBranch);
        }
        return new GitlabReleasePlanRequest(
            firstNonBlank(trimToNull(options.get("project-id")), System.getenv("CI_PROJECT_ID")),
            targetBranch,
            releaseBranch,
            isTrue(options.get("execute")),
            OutputFormat.parse(options.get("format"), OutputFormat.TEXT)
        );
    }

    private static String readConfiguredBaseBranch(String directoryOption) {
        try {
            return readConfiguredChangesetConfig(directoryOption).baseBranch();
        } catch (Exception ignored) {
            return "main";
        }
    }

    private static String readConfiguredReleaseBranch(String directoryOption, String targetBranch) {
        try {
            ChangesetConfigSupport.ChangesetConfig config = readConfiguredChangesetConfig(directoryOption);
            String configured = trimToNull(config.releaseBranch());
            if (configured != null) {
                return configured;
            }
        } catch (Exception ignored) {
        }
        return "changeset-release/" + targetBranch;
    }

    static ChangesetConfigSupport.ChangesetConfig readConfiguredChangesetConfig(String directoryOption) throws IOException {
        Path configuredRoot = resolveConfigRoot(directoryOption);
        if (configuredRoot != null) {
            try {
                return RepoFiles.readChangesetConfig(configuredRoot);
            } catch (Exception ignored) {
            }
        }
        return RepoFiles.readChangesetConfig(RepoFiles.resolveRepoRoot(directoryOption));
    }

    private static Path resolveConfigRoot(String directoryOption) {
        if (directoryOption == null) {
            return null;
        }
        return ChangesetConfigSupport.resolveConfigRoot(Paths.get(directoryOption));
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
        ChangesetConfigSupport.ChangesetConfig config = readConfiguredChangesetConfigOrDefaults(repoRootOption);
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

    private static ChangesetConfigSupport.ChangesetConfig readConfiguredChangesetConfigOrDefaults(String directoryOption) {
        try {
            return GitlabReleasePlanRequest.readConfiguredChangesetConfig(directoryOption);
        } catch (Exception ignored) {
            return ChangesetConfigSupport.ChangesetConfig.defaults();
        }
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

final class GithubReleasePlanRequest {
    final String githubRepo;
    final String targetBranch;
    final String releaseBranch;
    final boolean execute;

    private GithubReleasePlanRequest(String githubRepo, String targetBranch, String releaseBranch, boolean execute) {
        this.githubRepo = githubRepo;
        this.targetBranch = targetBranch;
        this.releaseBranch = releaseBranch;
        this.execute = execute;
    }

    static GithubReleasePlanRequest fromOptions(Map<String, String> options) {
        String repoRootOption = trimToNull(options.get("directory"));
        String targetBranch = firstNonBlank(trimToNull(options.get("target-branch")), System.getenv("GITHUB_BASE_REF"));
        if (targetBranch == null) {
            targetBranch = readConfiguredBaseBranch(repoRootOption);
        }
        String releaseBranch = trimToNull(options.get("release-branch"));
        if (releaseBranch == null) {
            releaseBranch = readConfiguredReleaseBranch(repoRootOption, targetBranch);
        }
        return new GithubReleasePlanRequest(
            firstNonBlank(trimToNull(options.get("github-repo")), System.getenv("GITHUB_REPOSITORY")),
            targetBranch,
            releaseBranch,
            isTrue(options.get("execute"))
        );
    }

    private static String readConfiguredBaseBranch(String directoryOption) {
        try {
            return readConfiguredChangesetConfig(directoryOption).baseBranch();
        } catch (Exception ignored) {
            return "main";
        }
    }

    private static String readConfiguredReleaseBranch(String directoryOption, String targetBranch) {
        try {
            ChangesetConfigSupport.ChangesetConfig config = readConfiguredChangesetConfig(directoryOption);
            String configured = trimToNull(config.releaseBranch());
            if (configured != null) {
                return configured;
            }
        } catch (Exception ignored) {
        }
        return "changeset-release/" + targetBranch;
    }

    private static ChangesetConfigSupport.ChangesetConfig readConfiguredChangesetConfig(String directoryOption) throws IOException {
        Path configuredRoot = resolveConfigRoot(directoryOption);
        if (configuredRoot != null) {
            try {
                return RepoFiles.readChangesetConfig(configuredRoot);
            } catch (Exception ignored) {
            }
        }
        return RepoFiles.readChangesetConfig(RepoFiles.resolveRepoRoot(directoryOption));
    }

    private static Path resolveConfigRoot(String directoryOption) {
        if (directoryOption == null) {
            return null;
        }
        return ChangesetConfigSupport.resolveConfigRoot(Paths.get(directoryOption));
    }
}

final class GithubTagRequest {
    final String currentSha;
    final boolean execute;

    private GithubTagRequest(String currentSha, boolean execute) {
        this.currentSha = currentSha;
        this.execute = execute;
    }

    static GithubTagRequest fromOptions(Map<String, String> options) {
        return new GithubTagRequest(
            firstNonBlank(trimToNull(options.get("current-sha")), System.getenv("GITHUB_SHA")),
            isTrue(options.get("execute"))
        );
    }
}

final class GithubReleasePublishRequest {
    final String releaseNotesFile;
    final String githubOutputFile;
    final boolean execute;

    private GithubReleasePublishRequest(String releaseNotesFile, String githubOutputFile, boolean execute) {
        this.releaseNotesFile = releaseNotesFile;
        this.githubOutputFile = githubOutputFile;
        this.execute = execute;
    }

    static GithubReleasePublishRequest fromOptions(Map<String, String> options) {
        return new GithubReleasePublishRequest(
            trimToNull(options.get("release-notes-file")) == null
                ? "target/release-notes.md"
                : trimToNull(options.get("release-notes-file")),
            firstNonBlank(trimToNull(options.get("github-output-file")), System.getenv("GITHUB_OUTPUT")),
            isTrue(options.get("execute"))
        );
    }
}
