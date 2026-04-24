package io.github.sonofmagic.javachanges.core.env;

import io.github.sonofmagic.javachanges.core.ReleaseTextUtils;

public final class EnvValue {
    public final String raw;
    public final boolean missing;
    public final boolean placeholder;

    private EnvValue(String raw, boolean missing, boolean placeholder) {
        this.raw = raw;
        this.missing = missing;
        this.placeholder = placeholder;
    }

    public static EnvValue of(String value) {
        String normalized = ReleaseTextUtils.trimToNull(value);
        if (normalized == null) {
            return missing();
        }
        if (ReleaseTextUtils.isPlaceholderValue(normalized)) {
            return new EnvValue(normalized, false, true);
        }
        return new EnvValue(normalized, false, false);
    }

    public static EnvValue missing() {
        return new EnvValue("", true, false);
    }

    public boolean isReal() {
        return !missing && !placeholder;
    }

    public String statusOrRaw() {
        if (missing) {
            return "MISSING";
        }
        if (placeholder) {
            return "PLACEHOLDER";
        }
        return raw;
    }

    public String renderMasked(boolean showSecrets) {
        if (missing) {
            return "MISSING";
        }
        if (placeholder) {
            return "PLACEHOLDER";
        }
        if (showSecrets) {
            return raw;
        }
        if (raw.length() <= 4) {
            return "****";
        }
        return raw.substring(0, 2) + "****" + raw.substring(raw.length() - 2);
    }
}
