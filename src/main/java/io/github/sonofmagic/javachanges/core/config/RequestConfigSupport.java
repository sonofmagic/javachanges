package io.github.sonofmagic.javachanges.core.config;

import io.github.sonofmagic.javachanges.core.ReleaseMessages;
import io.github.sonofmagic.javachanges.core.ReleaseTextUtils;
import io.github.sonofmagic.javachanges.core.plan.RepoFiles;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class RequestConfigSupport {
    private RequestConfigSupport() {
    }

    public static ChangesetConfigSupport.ChangesetConfig readConfiguredChangesetConfig(String directoryOption) throws IOException {
        Path configuredRoot = resolveConfigRoot(directoryOption);
        if (configuredRoot != null) {
            return RepoFiles.readChangesetConfig(configuredRoot);
        }
        return RepoFiles.readChangesetConfig(RepoFiles.resolveRepoRoot(directoryOption));
    }

    public static ChangesetConfigSupport.ChangesetConfig readConfiguredChangesetConfigOrDefaults(String directoryOption) {
        Path configuredRoot = resolveConfigRoot(directoryOption);
        if (configuredRoot != null) {
            return readConfigUnchecked(configuredRoot);
        }
        try {
            return RepoFiles.readChangesetConfig(RepoFiles.resolveRepoRoot(directoryOption));
        } catch (IllegalStateException exception) {
            return ChangesetConfigSupport.ChangesetConfig.defaults();
        } catch (IOException exception) {
            throw failedToReadConfig(exception);
        }
    }

    public static String readConfiguredBaseBranch(String directoryOption) {
        Path configuredRoot = resolveConfigRoot(directoryOption);
        if (configuredRoot != null) {
            return readConfigUnchecked(configuredRoot).baseBranch();
        }
        try {
            return RepoFiles.readChangesetConfig(RepoFiles.resolveRepoRoot(directoryOption)).baseBranch();
        } catch (IllegalStateException exception) {
            return "main";
        } catch (IOException exception) {
            throw failedToReadConfig(exception);
        }
    }

    public static String readConfiguredReleaseBranch(String directoryOption, String targetBranch) {
        Path configuredRoot = resolveConfigRoot(directoryOption);
        ChangesetConfigSupport.ChangesetConfig config = null;
        if (configuredRoot != null) {
            config = readConfigUnchecked(configuredRoot);
        } else {
            try {
                config = RepoFiles.readChangesetConfig(RepoFiles.resolveRepoRoot(directoryOption));
            } catch (IllegalStateException exception) {
                // Fall back when no repository is available.
            } catch (IOException exception) {
                throw failedToReadConfig(exception);
            }
        }
        if (config != null) {
            String configured = ReleaseTextUtils.trimToNull(config.releaseBranch());
            if (configured != null) {
                return configured;
            }
        }
        return "changeset-release/" + targetBranch;
    }

    private static ChangesetConfigSupport.ChangesetConfig readConfigUnchecked(Path repoRoot) {
        try {
            return RepoFiles.readChangesetConfig(repoRoot);
        } catch (IOException exception) {
            throw failedToReadConfig(exception);
        }
    }

    private static IllegalStateException failedToReadConfig(IOException exception) {
        return new IllegalStateException(ReleaseMessages.failedToReadChangesetConfig(), exception);
    }

    private static Path resolveConfigRoot(String directoryOption) {
        if (directoryOption == null) {
            return null;
        }
        return ChangesetConfigSupport.resolveConfigRoot(Paths.get(directoryOption));
    }
}
