package io.github.sonofmagic.javachanges.core;

import java.util.Map;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.firstNonBlank;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.isTrue;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.trimToNull;

final class PublishRequest {
    final boolean snapshot;
    final String tag;
    final boolean allowDirty;
    final boolean execute;
    final String module;
    final SnapshotVersionMode snapshotVersionMode;
    final String snapshotBuildStamp;
    final OutputFormat format;

    private PublishRequest(boolean snapshot, String tag, boolean allowDirty, boolean execute, String module,
                           SnapshotVersionMode snapshotVersionMode, String snapshotBuildStamp, OutputFormat format) {
        this.snapshot = snapshot;
        this.tag = tag;
        this.allowDirty = allowDirty;
        this.execute = execute;
        this.module = module;
        this.snapshotVersionMode = snapshotVersionMode;
        this.snapshotBuildStamp = snapshotBuildStamp;
        this.format = format;
    }

    static PublishRequest fromOptions(Map<String, String> options, boolean supportExecute) {
        return fromOptions(options, supportExecute, currentBranchFromEnvironment());
    }

    static PublishRequest fromOptions(Map<String, String> options, boolean supportExecute, String currentBranch) {
        String directoryOption = trimToNull(options.get("directory"));
        boolean snapshot = isTrue(options.get("snapshot"));
        String tag = firstNonBlank(trimToNull(options.get("tag")), trimToNull(System.getenv("CI_COMMIT_TAG")));
        if (!snapshot && tag == null && shouldDefaultToSnapshot(currentBranch, directoryOption)) {
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
            resolveSnapshotVersionMode(options, directoryOption),
            firstNonBlank(trimToNull(options.get("snapshot-build-stamp")),
                System.getenv("JAVACHANGES_SNAPSHOT_BUILD_STAMP")),
            OutputFormat.parse(options.get("format"), OutputFormat.TEXT)
        );
    }

    static SnapshotVersionMode resolveSnapshotVersionMode(Map<String, String> options, String directoryOption) {
        String cliValue = trimToNull(options.get("snapshot-version-mode"));
        if (cliValue != null) {
            return SnapshotVersionMode.parse(cliValue, SnapshotVersionMode.STAMPED);
        }
        try {
            return RequestConfigSupport.readConfiguredChangesetConfig(directoryOption).snapshotVersionMode();
        } catch (Exception ignored) {
            return SnapshotVersionMode.STAMPED;
        }
    }

    private static boolean shouldDefaultToSnapshot(String directoryOption) {
        return shouldDefaultToSnapshot(currentBranchFromEnvironment(), directoryOption);
    }

    static boolean shouldDefaultToSnapshot(String currentBranch, String directoryOption) {
        if (currentBranch == null) {
            return false;
        }
        try {
            ChangesetConfigSupport.ChangesetConfig config =
                RequestConfigSupport.readConfiguredChangesetConfig(directoryOption);
            return currentBranch.equals(config.snapshotBranch());
        } catch (Exception ignored) {
            return "snapshot".equals(currentBranch);
        }
    }

    private static String currentBranchFromEnvironment() {
        return firstNonBlank(trimToNull(System.getenv("CI_COMMIT_BRANCH")),
            trimToNull(System.getenv("GITHUB_REF_NAME")));
    }
}
