package io.github.sonofmagic.javachanges.core.env;

import io.github.sonofmagic.javachanges.core.OutputFormat;
import io.github.sonofmagic.javachanges.core.ReleaseUtils;

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
            ReleaseUtils.trimToNull(options.get("env-file")) == null ? "env/release.env.local"
                : ReleaseUtils.trimToNull(options.get("env-file")),
            ReleaseUtils.trimToNull(options.get("github-repo")),
            ReleaseUtils.trimToNull(options.get("gitlab-repo")),
            OutputFormat.parse(options.get("format"), OutputFormat.TEXT)
        );
    }
}
