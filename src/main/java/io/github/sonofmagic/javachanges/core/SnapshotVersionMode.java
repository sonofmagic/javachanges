package io.github.sonofmagic.javachanges.core;

public enum SnapshotVersionMode {
    STAMPED("stamped"),
    PLAIN("plain");

    public final String id;

    SnapshotVersionMode(String id) {
        this.id = id;
    }

    public static SnapshotVersionMode parse(String value, SnapshotVersionMode defaultValue) {
        String normalized = ReleaseUtils.trimToNull(value);
        if (normalized == null) {
            return defaultValue;
        }
        for (SnapshotVersionMode mode : values()) {
            if (mode.id.equalsIgnoreCase(normalized)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("不支持的 snapshot version mode: " + value + "，可选值: plain, stamped");
    }
}
