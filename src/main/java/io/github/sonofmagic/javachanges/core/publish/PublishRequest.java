package io.github.sonofmagic.javachanges.core.publish;

import io.github.sonofmagic.javachanges.core.OutputFormat;
import io.github.sonofmagic.javachanges.core.SnapshotVersionMode;
import io.github.sonofmagic.javachanges.core.ReleaseTextUtils;
import io.github.sonofmagic.javachanges.core.config.ChangesetConfigSupport;
import io.github.sonofmagic.javachanges.core.config.RequestConfigSupport;

import java.util.Map;

public final class PublishRequest {
    public final boolean snapshot;
    public final String tag;
    public final boolean allowDirty;
    public final boolean execute;
    public final String module;
    public final SnapshotVersionMode snapshotVersionMode;
    public final String snapshotBuildStamp;
    public final OutputFormat format;

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

    public static PublishRequest fromOptions(Map<String, String> options, boolean supportExecute) {
        return fromOptions(options, supportExecute, currentBranchFromEnvironment());
    }

    public static PublishRequest fromOptions(Map<String, String> options, boolean supportExecute, String currentBranch) {
        String directoryOption = ReleaseTextUtils.trimToNull(options.get("directory"));
        boolean snapshot = ReleaseTextUtils.isTrue(options.get("snapshot"));
        String tag = ReleaseTextUtils.firstNonBlank(
            ReleaseTextUtils.trimToNull(options.get("tag")),
            ReleaseTextUtils.trimToNull(System.getenv("CI_COMMIT_TAG"))
        );
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
            ReleaseTextUtils.isTrue(options.get("allow-dirty")),
            supportExecute && ReleaseTextUtils.isTrue(options.get("execute")),
            ReleaseTextUtils.trimToNull(options.get("module")),
            resolveSnapshotVersionMode(options, directoryOption),
            ReleaseTextUtils.firstNonBlank(ReleaseTextUtils.trimToNull(options.get("snapshot-build-stamp")),
                System.getenv("JAVACHANGES_SNAPSHOT_BUILD_STAMP")),
            OutputFormat.parse(options.get("format"), OutputFormat.TEXT)
        );
    }

    public static SnapshotVersionMode resolveSnapshotVersionMode(Map<String, String> options, String directoryOption) {
        String cliValue = ReleaseTextUtils.trimToNull(options.get("snapshot-version-mode"));
        if (cliValue != null) {
            return SnapshotVersionMode.parse(cliValue, SnapshotVersionMode.STAMPED);
        }
        try {
            return RequestConfigSupport.readConfiguredChangesetConfig(directoryOption).snapshotVersionMode();
        } catch (Exception ignored) {
            return SnapshotVersionMode.STAMPED;
        }
    }

    public static boolean shouldDefaultToSnapshot(String currentBranch, String directoryOption) {
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
        return ReleaseTextUtils.firstNonBlank(ReleaseTextUtils.trimToNull(System.getenv("CI_COMMIT_BRANCH")),
            ReleaseTextUtils.trimToNull(System.getenv("GITHUB_REF_NAME")));
    }
}
