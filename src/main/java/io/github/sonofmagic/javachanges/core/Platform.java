package io.github.sonofmagic.javachanges.core;

public enum Platform {
    GITHUB("github"),
    GITLAB("gitlab"),
    ALL("all");

    public final String id;

    Platform(String id) {
        this.id = id;
    }

    public static Platform parse(String value, Platform defaultValue) {
        String normalized = ReleaseTextUtils.trimToNull(value);
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

    public boolean includesGithub() {
        return this == GITHUB || this == ALL;
    }

    public boolean includesGitlab() {
        return this == GITLAB || this == ALL;
    }
}
