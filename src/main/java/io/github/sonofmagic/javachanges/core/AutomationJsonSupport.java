package io.github.sonofmagic.javachanges.core;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.jsonEscape;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.trimToNull;

final class AutomationJsonSupport {
    private AutomationJsonSupport() {
    }

    static String errorJson(String command, Exception exception) {
        String message = trimToNull(exception.getMessage());
        if (message == null) {
            message = exception.getClass().getSimpleName();
        }
        AutomationReport report = new AutomationReport(command);
        report.ok = false;
        report.skipped = false;
        report.reason = message;
        report.action = "error";
        return report.toJson();
    }

    static final class AutomationReport {
        boolean ok = true;
        final String command;
        String action;
        boolean skipped;
        String reason;
        String releaseVersion;
        String releaseModule;
        String tag;
        String releaseNotesFile;
        String projectId;
        boolean execute;
        boolean dryRun;

        AutomationReport(String command) {
            this.command = command;
        }

        String toJson() {
            StringBuilder builder = new StringBuilder();
            builder.append("{");
            builder.append("\"ok\":").append(ok);
            builder.append(",\"command\":\"").append(jsonEscape(command)).append("\"");
            appendString(builder, "action", action);
            builder.append(",\"skipped\":").append(skipped);
            appendString(builder, "reason", reason);
            appendString(builder, "releaseVersion", releaseVersion);
            appendString(builder, "releaseModule", releaseModule);
            appendString(builder, "tag", tag);
            appendString(builder, "releaseNotesFile", releaseNotesFile);
            appendString(builder, "projectId", projectId);
            builder.append(",\"execute\":").append(execute);
            builder.append(",\"dryRun\":").append(dryRun);
            builder.append("}");
            return builder.toString();
        }

        private void appendString(StringBuilder builder, String name, String value) {
            builder.append(",\"").append(name).append("\":");
            if (value == null) {
                builder.append("null");
                return;
            }
            builder.append("\"").append(jsonEscape(value)).append("\"");
        }
    }
}
