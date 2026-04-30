package io.github.sonofmagic.javachanges.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PublishDoctorReport {
    public final String command = "doctor-publish";
    public final String target;
    public String mode;
    public String buildTool;
    public String module;
    public String gradleTask;
    public String currentRevision;
    public String publishVersion;
    public String snapshotVersionMode;
    public String snapshotBuildStamp;
    public boolean snapshotBuildStampApplied;
    public final List<Check> checks = new ArrayList<Check>();
    public final List<String> suggestions = new ArrayList<String>();
    public final List<String> nextCommands = new ArrayList<String>();

    public PublishDoctorReport(String target) {
        this.target = target;
    }

    public boolean ok() {
        for (Check check : checks) {
            if ("FAILED".equals(check.status)) {
                return false;
            }
        }
        return true;
    }

    public void ok(String section, String name, String message) {
        checks.add(new Check(section, name, "OK", message));
    }

    public void failed(String section, String name, String message) {
        checks.add(new Check(section, name, "FAILED", message));
    }

    public void failed(String section, String name, String message, String suggestion) {
        failed(section, name, message);
        suggest(suggestion);
    }

    public void suggest(String suggestion) {
        String normalized = ReleaseTextUtils.trimToNull(suggestion);
        if (normalized != null && !suggestions.contains(normalized)) {
            suggestions.add(normalized);
        }
    }

    public void warn(String section, String name, String message) {
        checks.add(new Check(section, name, "WARN", message));
    }

    public void skipped(String section, String name, String message) {
        checks.add(new Check(section, name, "SKIPPED", message));
    }

    public String reason() {
        return ok() ? "Publish readiness check passed." : "Publish readiness check failed.";
    }

    public String toJson() {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("ok", Boolean.valueOf(ok()));
        payload.put("command", command);
        payload.put("target", target);
        payload.put("mode", mode);
        payload.put("buildTool", buildTool);
        payload.put("module", module);
        payload.put("gradleTask", gradleTask);
        payload.put("currentRevision", currentRevision);
        payload.put("publishVersion", publishVersion);
        payload.put("snapshotVersionMode", snapshotVersionMode);
        payload.put("snapshotBuildStamp", snapshotBuildStamp);
        payload.put("snapshotBuildStampApplied", Boolean.valueOf(snapshotBuildStampApplied));
        payload.put("reason", reason());
        List<Map<String, Object>> renderedChecks = new ArrayList<Map<String, Object>>();
        for (Check check : checks) {
            renderedChecks.add(check.toMap());
        }
        payload.put("checks", renderedChecks);
        if (!suggestions.isEmpty()) {
            payload.put("suggestions", new ArrayList<String>(suggestions));
        }
        payload.put("nextCommands", new ArrayList<String>(nextCommands));
        return ReleaseJsonUtils.toJson(payload);
    }

    public static String errorJson(String command, Exception exception) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        String message = ReleaseTextUtils.trimToNull(exception.getMessage());
        payload.put("ok", Boolean.FALSE);
        payload.put("command", command);
        payload.put("reason", message == null ? exception.getClass().getSimpleName() : message);
        return ReleaseJsonUtils.toJson(payload);
    }

    public static final class Check {
        public final String section;
        public final String name;
        public final String status;
        public final String message;

        private Check(String section, String name, String status, String message) {
            this.section = section;
            this.name = name;
            this.status = status;
            this.message = message;
        }

        Map<String, Object> toMap() {
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("section", section);
            payload.put("name", name);
            payload.put("status", status);
            payload.put("message", message);
            return payload;
        }
    }
}
