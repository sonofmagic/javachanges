package io.github.sonofmagic.javachanges.core;

import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;

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

    static boolean isText(OutputFormat format) {
        return format != OutputFormat.JSON;
    }

    static void print(PrintStream out, boolean textOutput, AutomationReport report, String textMessage) {
        if (textOutput) {
            out.println(textMessage);
        } else {
            out.println(report.toJson());
        }
    }

    static void printLines(PrintStream out, boolean textOutput, String... lines) {
        if (!textOutput) {
            return;
        }
        for (int i = 0; i < lines.length; i++) {
            out.println(lines[i]);
        }
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
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("ok", Boolean.valueOf(ok));
            payload.put("command", command);
            payload.put("action", action);
            payload.put("skipped", Boolean.valueOf(skipped));
            payload.put("reason", reason);
            payload.put("releaseVersion", releaseVersion);
            payload.put("releaseModule", releaseModule);
            payload.put("tag", tag);
            payload.put("releaseNotesFile", releaseNotesFile);
            payload.put("projectId", projectId);
            payload.put("execute", Boolean.valueOf(execute));
            payload.put("dryRun", Boolean.valueOf(dryRun));
            return ReleaseJsonUtils.toJson(payload);
        }
    }
}
