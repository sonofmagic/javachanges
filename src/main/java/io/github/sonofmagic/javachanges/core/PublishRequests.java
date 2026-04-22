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
            ChangesetConfigSupport.ChangesetConfig config =
                RequestConfigSupport.readConfiguredChangesetConfig(directoryOption);
            return currentBranch.equals(config.snapshotBranch());
        } catch (Exception ignored) {
            return "snapshot".equals(currentBranch);
        }
    }
}
