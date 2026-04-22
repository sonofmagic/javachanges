package io.github.sonofmagic.javachanges.core;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.trimToNull;

enum ReleaseTagStrategy {
    WHOLE_REPO("whole-repo"),
    PER_MODULE("per-module");

    final String id;

    ReleaseTagStrategy(String id) {
        this.id = id;
    }

    static ReleaseTagStrategy parse(String value, ReleaseTagStrategy defaultValue) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return defaultValue;
        }
        if (WHOLE_REPO.id.equals(normalized)) {
            return WHOLE_REPO;
        }
        if (PER_MODULE.id.equals(normalized)) {
            return PER_MODULE;
        }
        throw new IllegalArgumentException("Unsupported tag strategy: " + value);
    }
}
