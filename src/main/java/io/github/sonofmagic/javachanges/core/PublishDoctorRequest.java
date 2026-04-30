package io.github.sonofmagic.javachanges.core;

import java.util.Map;

public final class PublishDoctorRequest {
    public final String target;
    public final String mode;
    public final OutputFormat format;

    private PublishDoctorRequest(String target, String mode, OutputFormat format) {
        this.target = target;
        this.mode = mode;
        this.format = format;
    }

    public static PublishDoctorRequest fromOptions(Map<String, String> options) {
        return new PublishDoctorRequest(
            parseTarget(options.get("target")),
            parseMode(options.get("mode")),
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

    private static String parseMode(String rawValue) {
        String value = ReleaseTextUtils.trimToNull(rawValue);
        if (value == null) {
            return "auto";
        }
        if ("auto".equals(value) || "snapshot".equals(value) || "release".equals(value)) {
            return value;
        }
        throw new IllegalArgumentException("Unsupported publish mode: " + rawValue + ". Use auto, snapshot, or release.");
    }
}
