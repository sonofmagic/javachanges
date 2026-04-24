package io.github.sonofmagic.javachanges.core.env;

import io.github.sonofmagic.javachanges.core.OutputFormat;
import io.github.sonofmagic.javachanges.core.ReleaseTextUtils;

import java.util.Map;

public final class LocalDoctorRequest {
    public final String envFile;
    public final String githubRepo;
    public final String gitlabRepo;
    public final OutputFormat format;

    private LocalDoctorRequest(String envFile, String githubRepo, String gitlabRepo, OutputFormat format) {
        this.envFile = envFile;
        this.githubRepo = githubRepo;
        this.gitlabRepo = gitlabRepo;
        this.format = format;
    }

    public static LocalDoctorRequest fromOptions(Map<String, String> options) {
        return new LocalDoctorRequest(
            ReleaseTextUtils.trimToNull(options.get("env-file")) == null ? "env/release.env.local"
                : ReleaseTextUtils.trimToNull(options.get("env-file")),
            ReleaseTextUtils.trimToNull(options.get("github-repo")),
            ReleaseTextUtils.trimToNull(options.get("gitlab-repo")),
            OutputFormat.parse(options.get("format"), OutputFormat.TEXT)
        );
    }
}
