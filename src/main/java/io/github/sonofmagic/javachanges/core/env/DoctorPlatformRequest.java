package io.github.sonofmagic.javachanges.core.env;

import io.github.sonofmagic.javachanges.core.OutputFormat;
import io.github.sonofmagic.javachanges.core.Platform;
import io.github.sonofmagic.javachanges.core.ReleaseTextUtils;

import java.util.Map;

public final class DoctorPlatformRequest {
    public final String envFile;
    public final Platform platform;
    public final String githubRepo;
    public final String gitlabRepo;
    public final OutputFormat format;

    private DoctorPlatformRequest(String envFile, Platform platform, String githubRepo, String gitlabRepo,
                                  OutputFormat format) {
        this.envFile = envFile;
        this.platform = platform;
        this.githubRepo = githubRepo;
        this.gitlabRepo = gitlabRepo;
        this.format = format;
    }

    public static DoctorPlatformRequest fromOptions(Map<String, String> options) {
        return new DoctorPlatformRequest(
            ReleaseTextUtils.requiredOption(options, "env-file"),
            ReleaseTextUtils.platformOption(options),
            ReleaseTextUtils.trimToNull(options.get("github-repo")),
            ReleaseTextUtils.trimToNull(options.get("gitlab-repo")),
            OutputFormat.parse(options.get("format"), OutputFormat.TEXT)
        );
    }
}
