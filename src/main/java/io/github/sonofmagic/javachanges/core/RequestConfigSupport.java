package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

final class RequestConfigSupport {
    private RequestConfigSupport() {
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

    static ChangesetConfigSupport.ChangesetConfig readConfiguredChangesetConfigOrDefaults(String directoryOption) {
        try {
            return readConfiguredChangesetConfig(directoryOption);
        } catch (Exception ignored) {
            return ChangesetConfigSupport.ChangesetConfig.defaults();
        }
    }

    static String readConfiguredBaseBranch(String directoryOption) {
        try {
            return readConfiguredChangesetConfig(directoryOption).baseBranch();
        } catch (Exception ignored) {
            return "main";
        }
    }

    static String readConfiguredReleaseBranch(String directoryOption, String targetBranch) {
        try {
            ChangesetConfigSupport.ChangesetConfig config = readConfiguredChangesetConfig(directoryOption);
            String configured = ReleaseUtils.trimToNull(config.releaseBranch());
            if (configured != null) {
                return configured;
            }
        } catch (Exception ignored) {
        }
        return "changeset-release/" + targetBranch;
    }

    private static Path resolveConfigRoot(String directoryOption) {
        if (directoryOption == null) {
            return null;
        }
        return ChangesetConfigSupport.resolveConfigRoot(Paths.get(directoryOption));
    }
}
