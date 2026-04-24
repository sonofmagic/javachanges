package io.github.sonofmagic.javachanges.core.env;

import io.github.sonofmagic.javachanges.core.ReleaseEnvJsonSupport;

import java.io.PrintStream;
import java.util.List;

final class ReleaseEnvDoctorSupport {
    private final PrintStream out;

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

    String commandReportJson(String command, boolean ok, String envFile, String platform,
                             List<ReleaseEnvJsonSupport.JsonSection> sections,
                             List<String> suggestions, String error) {
        return ReleaseEnvJsonSupport.commandReportJson(command, ok, envFile, platform, false, sections, suggestions, error);
    }
}
