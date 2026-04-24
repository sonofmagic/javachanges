package io.github.sonofmagic.javachanges.core;

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
