package io.github.sonofmagic.javachanges.core.env;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import io.github.sonofmagic.javachanges.core.ReleaseTextUtils;

final class ReleaseEnvDoctorSupport {
    private final PrintStream out;

    static final class EnvStatusSummary {
        final boolean failed;
        final boolean needsEdit;

        EnvStatusSummary(boolean failed, boolean needsEdit) {
            this.failed = failed;
            this.needsEdit = needsEdit;
        }
    }

    ReleaseEnvDoctorSupport(PrintStream out) {
        this.out = out;
    }

    void recordStatus(boolean textOutput, ReleaseEnvJsonSupport.JsonSection section, String label, String value) {
        if (textOutput) {
            out.printf("%-40s %s%n", label, value);
        }
        if (section != null) {
            section.add(label, value);
        }
    }

    String requiredStatus(EnvValue value, boolean required) {
        if (value.missing) {
            return required ? "MISSING" : "OPTIONAL";
        }
        if (value.placeholder) {
            return "PLACEHOLDER";
        }
        return "OK";
    }

    EnvStatusSummary recordCommonEnvStatuses(LoadedEnv env, boolean textOutput,
                                             ReleaseEnvJsonSupport.JsonSection section) {
        boolean failed = false;
        boolean needsEdit = false;
        for (EnvEntry entry : ReleaseEnvCatalog.COMMON_VARIABLES) {
            String status = requiredStatus(env.value(entry.name), entry.required);
            recordStatus(textOutput, section, entry.name, status);
            if ("MISSING".equals(status) || "PLACEHOLDER".equals(status)) {
                if (entry.required) {
                    failed = true;
                }
                if (!"OPTIONAL".equals(status)) {
                    needsEdit = true;
                }
            }
        }
        String gitlabTokenStatus = requiredStatus(env.value("GITLAB_RELEASE_TOKEN"), false);
        recordStatus(textOutput, section, "GITLAB_RELEASE_TOKEN", gitlabTokenStatus);
        return new EnvStatusSummary(failed, needsEdit);
    }

    String repoStatusValue(String value) {
        if (ReleaseTextUtils.isBlank(value)) {
            return "NOT_SET";
        }
        if (value.contains("/")) {
            return value;
        }
        return "INVALID";
    }

    boolean isValidRepoIdentifier(String value) {
        return !ReleaseTextUtils.isBlank(value) && value.contains("/");
    }

    String commandReportJson(String command, boolean ok, String envFile, String platform,
                             List<ReleaseEnvJsonSupport.JsonSection> sections,
                             List<String> suggestions, String error) {
        return ReleaseEnvJsonSupport.commandReportJson(command, ok, envFile, platform, false,
            new ArrayList<ReleaseEnvJsonSupport.JsonSection>(sections), new ArrayList<String>(suggestions), error);
    }
}
