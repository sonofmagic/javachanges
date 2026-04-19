package io.github.sonofmagic.javachanges.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.jsonEscape;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.trimToNull;

final class ReleaseEnvJsonSupport {
    private ReleaseEnvJsonSupport() {
    }

    static String errorJson(String command, Exception exception) {
        String message = trimToNull(exception.getMessage());
        if (message == null) {
            message = exception.getClass().getSimpleName();
        }
        return "{\"ok\":false,\"command\":\"" + jsonEscape(command) + "\",\"error\":\""
            + jsonEscape(message) + "\"}";
    }

    static String commandReportJson(String command, boolean ok, String envFile, String platform,
                                    boolean showSecrets, List<JsonSection> sections,
                                    List<String> suggestions, String error) {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append("\"ok\":").append(ok);
        builder.append(",\"command\":\"").append(jsonEscape(command)).append("\"");
        if (envFile != null) {
            builder.append(",\"envFile\":\"").append(jsonEscape(envFile)).append("\"");
        }
        if (platform != null) {
            builder.append(",\"platform\":\"").append(jsonEscape(platform)).append("\"");
        }
        builder.append(",\"showSecrets\":").append(showSecrets);
        builder.append(",\"sections\":[");
        for (int i = 0; i < sections.size(); i++) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append(sections.get(i).toJson());
        }
        builder.append("]");
        if (!suggestions.isEmpty()) {
            builder.append(",\"suggestions\":[");
            for (int i = 0; i < suggestions.size(); i++) {
                if (i > 0) {
                    builder.append(",");
                }
                builder.append("\"").append(jsonEscape(suggestions.get(i))).append("\"");
            }
            builder.append("]");
        }
        if (error != null) {
            builder.append(",\"error\":\"").append(jsonEscape(error)).append("\"");
        }
        builder.append("}");
        return builder.toString();
    }

    static final class JsonSection {
        private final String title;
        private final List<Map<String, String>> entries = new ArrayList<Map<String, String>>();

        JsonSection(String title) {
            this.title = title;
        }

        void add(String label, String value) {
            Map<String, String> entry = new LinkedHashMap<String, String>();
            entry.put("label", label);
            entry.put("value", value);
            entries.add(entry);
        }

        String toJson() {
            StringBuilder builder = new StringBuilder();
            builder.append("{\"title\":\"").append(jsonEscape(title)).append("\",\"entries\":[");
            for (int i = 0; i < entries.size(); i++) {
                if (i > 0) {
                    builder.append(",");
                }
                Map<String, String> entry = entries.get(i);
                builder.append("{\"label\":\"").append(jsonEscape(entry.get("label"))).append("\",");
                builder.append("\"value\":\"").append(jsonEscape(entry.get("value"))).append("\"}");
            }
            builder.append("]}");
            return builder.toString();
        }
    }
}
