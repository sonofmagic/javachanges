package io.github.sonofmagic.javachanges.core;

import java.util.Locale;

public enum ReleaseLevel {
    PATCH("patch", 1),
    MINOR("minor", 2),
    MAJOR("major", 3);

    public final String id;
    public final int weight;

    ReleaseLevel(String id, int weight) {
        this.id = id;
        this.weight = weight;
    }

    public static ReleaseLevel parse(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        for (ReleaseLevel level : values()) {
            if (level.id.equals(normalized)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unsupported release level: " + value);
    }
}
