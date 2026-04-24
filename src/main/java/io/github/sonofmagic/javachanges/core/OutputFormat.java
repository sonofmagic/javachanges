package io.github.sonofmagic.javachanges.core;

public enum OutputFormat {
    TEXT("text"),
    JSON("json");

    public final String id;

    OutputFormat(String id) {
        this.id = id;
    }

    public static OutputFormat parse(String value, OutputFormat defaultValue) {
        String normalized = ReleaseUtils.trimToNull(value);
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
