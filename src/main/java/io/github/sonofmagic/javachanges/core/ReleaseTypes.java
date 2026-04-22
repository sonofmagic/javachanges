package io.github.sonofmagic.javachanges.core;

import java.util.Locale;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.trimToNull;

enum Platform {
    GITHUB("github"),
    GITLAB("gitlab"),
    ALL("all");

    final String id;

    Platform(String id) {
        this.id = id;
    }

    static Platform parse(String value, Platform defaultValue) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return defaultValue;
        }
        for (Platform platform : values()) {
            if (platform.id.equalsIgnoreCase(normalized)) {
                return platform;
            }
        }
        throw new IllegalArgumentException("不支持的平台: " + value + "，可选值: github, gitlab, all");
    }

    boolean includesGithub() {
        return this == GITHUB || this == ALL;
    }

    boolean includesGitlab() {
        return this == GITLAB || this == ALL;
    }
}

enum OutputFormat {
    TEXT("text"),
    JSON("json");

    final String id;

    OutputFormat(String id) {
        this.id = id;
    }

    static OutputFormat parse(String value, OutputFormat defaultValue) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return defaultValue;
        }
        for (OutputFormat format : values()) {
            if (format.id.equalsIgnoreCase(normalized)) {
                return format;
            }
        }
        throw new IllegalArgumentException("不支持的输出格式: " + value + "，可选值: text, json");
    }
}

enum SnapshotVersionMode {
    STAMPED("stamped"),
    PLAIN("plain");

    final String id;

    SnapshotVersionMode(String id) {
        this.id = id;
    }

    static SnapshotVersionMode parse(String value, SnapshotVersionMode defaultValue) {
        String normalized = trimToNull(value);
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

enum ReleaseLevel {
    PATCH("patch", 1),
    MINOR("minor", 2),
    MAJOR("major", 3);

    final String id;
    final int weight;

    ReleaseLevel(String id, int weight) {
        this.id = id;
        this.weight = weight;
    }

    static ReleaseLevel parse(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        for (ReleaseLevel level : values()) {
            if (level.id.equals(normalized)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unsupported release level: " + value);
    }
}

final class Semver {
    private final int major;
    private final int minor;
    private final int patch;

    private Semver(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    static Semver parse(String value) {
        String[] parts = value.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Unsupported version: " + value);
        }
        return new Semver(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }

    Semver bump(ReleaseLevel level) {
        if (level == ReleaseLevel.MAJOR) {
            return new Semver(major + 1, 0, 0);
        }
        if (level == ReleaseLevel.MINOR) {
            return new Semver(major, minor + 1, 0);
        }
        return new Semver(major, minor, patch + 1);
    }

    static Semver max(Semver left, Semver right) {
        return left.compareTo(right) >= 0 ? left : right;
    }

    private int compareTo(Semver other) {
        if (major != other.major) {
            return major - other.major;
        }
        if (minor != other.minor) {
            return minor - other.minor;
        }
        return patch - other.patch;
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
