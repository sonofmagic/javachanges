package io.github.sonofmagic.javachanges.core.env;

import io.github.sonofmagic.javachanges.core.ReleaseUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.trimToNull;

public final class ReleaseEnvJsonSupport {
    private ReleaseEnvJsonSupport() {
    }

    public static String errorJson(String command, Exception exception) {
        String message = trimToNull(exception.getMessage());
        if (message == null) {
            message = exception.getClass().getSimpleName();
        }
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("ok", Boolean.FALSE);
        payload.put("command", command);
        payload.put("error", message);
        return ReleaseUtils.toJson(payload);
    }

    public static String commandReportJson(String command, boolean ok, String envFile, String platform,
                                           boolean showSecrets, List<JsonSection> sections,
                                           List<String> suggestions, String error) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("ok", Boolean.valueOf(ok));
        payload.put("command", command);
        if (envFile != null) {
            payload.put("envFile", envFile);
        }
        if (platform != null) {
            payload.put("platform", platform);
        }
        payload.put("showSecrets", Boolean.valueOf(showSecrets));
        List<Map<String, Object>> renderedSections = new ArrayList<Map<String, Object>>();
        for (JsonSection section : sections) {
            renderedSections.add(section.toMap());
        }
        payload.put("sections", renderedSections);
        if (!suggestions.isEmpty()) {
            payload.put("suggestions", new ArrayList<String>(suggestions));
        }
        if (error != null) {
            payload.put("error", error);
        }
        return ReleaseUtils.toJson(payload);
    }

    public static final class JsonSection {
        private final String title;
        private final List<Map<String, String>> entries = new ArrayList<Map<String, String>>();

        public JsonSection(String title) {
            this.title = title;
        }

        public void add(String label, String value) {
            Map<String, String> entry = new LinkedHashMap<String, String>();
            entry.put("label", label);
            entry.put("value", value);
            entries.add(entry);
        }

        Map<String, Object> toMap() {
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("title", title);
            result.put("entries", new ArrayList<Map<String, String>>(entries));
            return result;
        }
    }
}
