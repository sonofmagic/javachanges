package io.github.sonofmagic.javachanges.core;

import java.util.Map;

public final class PublishDoctorRequest {
    public final String target;
    public final String mode;
    public final String tag;
    public final String module;
    public final String task;
    public final boolean allowDirty;
    public final String snapshotVersionMode;
    public final String snapshotBuildStamp;
    public final OutputFormat format;

    private PublishDoctorRequest(String target, String mode, String tag, String module, String task, boolean allowDirty,
                                 String snapshotVersionMode, String snapshotBuildStamp, OutputFormat format) {
        this.target = target;
        this.mode = mode;
        this.tag = tag;
        this.module = module;
        this.task = task;
        this.allowDirty = allowDirty;
        this.snapshotVersionMode = snapshotVersionMode;
        this.snapshotBuildStamp = snapshotBuildStamp;
        this.format = format;
    }

    public static PublishDoctorRequest fromOptions(Map<String, String> options) {
        return new PublishDoctorRequest(
            parseTarget(options.get("target")),
            parseMode(options.get("mode"), options.get("tag")),
            ReleaseTextUtils.trimToNull(options.get("tag")),
            ReleaseTextUtils.trimToNull(options.get("module")),
            ReleaseTextUtils.trimToNull(options.get("task")),
            Boolean.parseBoolean(String.valueOf(options.get("allow-dirty"))),
            ReleaseTextUtils.trimToNull(options.get("snapshot-version-mode")),
            ReleaseTextUtils.firstNonBlank(ReleaseTextUtils.trimToNull(options.get("snapshot-build-stamp")),
                System.getenv("JAVACHANGES_SNAPSHOT_BUILD_STAMP")),
            OutputFormat.parse(options.get("format"), OutputFormat.TEXT)
        );
    }

    private static String parseTarget(String rawValue) {
        String value = ReleaseTextUtils.trimToNull(rawValue);
        if (value == null) {
            return "maven-central";
        }
        if ("maven-central".equals(value)) {
            return value;
        }
        throw new IllegalArgumentException("Unsupported publish target: " + rawValue + ". Use maven-central.");
    }

    private static String parseMode(String rawValue, String rawTag) {
        String value = ReleaseTextUtils.trimToNull(rawValue);
        String tag = ReleaseTextUtils.trimToNull(rawTag);
        if (value == null) {
            return tag == null ? "auto" : "release";
        }
        if ("snapshot".equals(value) && tag != null) {
            throw new IllegalArgumentException("Unsupported publish mode: snapshot cannot be used with --tag.");
        }
        if ("auto".equals(value) || "snapshot".equals(value) || "release".equals(value)) {
            return value;
        }
        throw new IllegalArgumentException("Unsupported publish mode: " + rawValue + ". Use auto, snapshot, or release.");
    }
}
