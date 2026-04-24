package io.github.sonofmagic.javachanges.core.env;

import io.github.sonofmagic.javachanges.core.OutputFormat;
import io.github.sonofmagic.javachanges.core.Platform;
import io.github.sonofmagic.javachanges.core.ReleaseUtils;

import java.util.Map;

public final class AuditVarsRequest {
    public final String envFile;
    public final Platform platform;
    public final String githubRepo;
    public final String gitlabRepo;
    public final OutputFormat format;

    private AuditVarsRequest(String envFile, Platform platform, String githubRepo, String gitlabRepo,
                             OutputFormat format) {
        this.envFile = envFile;
        this.platform = platform;
        this.githubRepo = githubRepo;
        this.gitlabRepo = gitlabRepo;
        this.format = format;
    }

    public static AuditVarsRequest fromOptions(Map<String, String> options) {
        return new AuditVarsRequest(
            ReleaseUtils.requiredOption(options, "env-file"),
            ReleaseUtils.platformOption(options),
            ReleaseUtils.trimToNull(options.get("github-repo")),
            ReleaseUtils.trimToNull(options.get("gitlab-repo")),
            OutputFormat.parse(options.get("format"), OutputFormat.TEXT)
        );
    }
}
