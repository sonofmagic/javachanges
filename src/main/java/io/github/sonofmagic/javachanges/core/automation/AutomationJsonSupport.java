package io.github.sonofmagic.javachanges.core.automation;

import io.github.sonofmagic.javachanges.core.OutputFormat;
import io.github.sonofmagic.javachanges.core.ReleaseUtils;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.trimToNull;

public final class AutomationJsonSupport {
    private AutomationJsonSupport() {
    }

    public static String errorJson(String command, Exception exception) {
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

    public static boolean isText(OutputFormat format) {
        return format != OutputFormat.JSON;
    }

    public static void print(PrintStream out, boolean textOutput, AutomationReport report, String textMessage) {
        if (textOutput) {
            out.println(textMessage);
        } else {
            out.println(report.toJson());
        }
    }

    public static void printLines(PrintStream out, boolean textOutput, String... lines) {
        if (!textOutput) {
            return;
        }
        for (int i = 0; i < lines.length; i++) {
            out.println(lines[i]);
        }
    }

    public static final class AutomationReport {
        public boolean ok = true;
        public final String command;
        public String action;
        public boolean skipped;
        public String reason;
        public String releaseVersion;
        public String effectiveVersion;
        public String releaseModule;
        public String tag;
        public String tagStrategy;
        public List<String> tags;
        public String releaseNotesFile;
        public String projectId;
        public boolean execute;
        public boolean dryRun;
        public String snapshotVersionMode;
        public boolean snapshotBuildStampApplied;

        public AutomationReport(String command) {
            this.command = command;
        }

        public String toJson() {
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("ok", Boolean.valueOf(ok));
            payload.put("command", command);
            payload.put("action", action);
            payload.put("skipped", Boolean.valueOf(skipped));
            payload.put("reason", reason);
            payload.put("releaseVersion", releaseVersion);
            payload.put("effectiveVersion", effectiveVersion);
            payload.put("releaseModule", releaseModule);
            payload.put("tag", tag);
            payload.put("tagStrategy", tagStrategy);
            payload.put("tags", tags == null ? null : new ArrayList<String>(tags));
            payload.put("releaseNotesFile", releaseNotesFile);
            payload.put("projectId", projectId);
            payload.put("execute", Boolean.valueOf(execute));
            payload.put("dryRun", Boolean.valueOf(dryRun));
            payload.put("snapshotVersionMode", snapshotVersionMode);
            payload.put("snapshotBuildStampApplied", Boolean.valueOf(snapshotBuildStampApplied));
            return ReleaseUtils.toJson(payload);
        }
    }
}
