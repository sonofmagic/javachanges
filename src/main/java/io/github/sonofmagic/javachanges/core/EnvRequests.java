package io.github.sonofmagic.javachanges.core;

import java.util.Map;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.isTrue;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.platformOption;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.requiredOption;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.trimToNull;

final class InitEnvRequest {
    final String template;
    final String target;
    final boolean force;

    private InitEnvRequest(String template, String target, boolean force) {
        this.template = template;
        this.target = target;
        this.force = force;
    }

    static InitEnvRequest fromOptions(Map<String, String> options) {
        String target = trimToNull(options.get("target"));
        if (target == null) {
            target = trimToNull(options.get("path"));
        }
        return new InitEnvRequest(
            trimToNull(options.get("template")) == null ? "env/release.env.example" : trimToNull(options.get("template")),
            target == null ? "env/release.env.local" : target,
            isTrue(options.get("force"))
        );
    }
}

final class PlatformEnvRequest {
    final String envFile;
    final Platform platform;
    final boolean showSecrets;
    final OutputFormat format;

    private PlatformEnvRequest(String envFile, Platform platform, boolean showSecrets, OutputFormat format) {
        this.envFile = envFile;
        this.platform = platform;
        this.showSecrets = showSecrets;
        this.format = format;
    }

    static PlatformEnvRequest fromOptions(Map<String, String> options) {
        return new PlatformEnvRequest(
            requiredOption(options, "env-file"),
            platformOption(options),
            isTrue(options.get("show-secrets")),
            OutputFormat.parse(options.get("format"), OutputFormat.TEXT)
        );
    }
}

final class LocalDoctorRequest {
    final String envFile;
    final String githubRepo;
    final String gitlabRepo;
    final OutputFormat format;

    private LocalDoctorRequest(String envFile, String githubRepo, String gitlabRepo, OutputFormat format) {
        this.envFile = envFile;
        this.githubRepo = githubRepo;
        this.gitlabRepo = gitlabRepo;
        this.format = format;
    }

    static LocalDoctorRequest fromOptions(Map<String, String> options) {
        return new LocalDoctorRequest(
            trimToNull(options.get("env-file")) == null ? "env/release.env.local" : trimToNull(options.get("env-file")),
            trimToNull(options.get("github-repo")),
            trimToNull(options.get("gitlab-repo")),
            OutputFormat.parse(options.get("format"), OutputFormat.TEXT)
        );
    }
}

final class DoctorPlatformRequest {
    final String envFile;
    final Platform platform;
    final String githubRepo;
    final String gitlabRepo;
    final OutputFormat format;

    private DoctorPlatformRequest(String envFile, Platform platform, String githubRepo, String gitlabRepo,
                                  OutputFormat format) {
        this.envFile = envFile;
        this.platform = platform;
        this.githubRepo = githubRepo;
        this.gitlabRepo = gitlabRepo;
        this.format = format;
    }

    static DoctorPlatformRequest fromOptions(Map<String, String> options) {
        return new DoctorPlatformRequest(
            requiredOption(options, "env-file"),
            platformOption(options),
            trimToNull(options.get("github-repo")),
            trimToNull(options.get("gitlab-repo")),
            OutputFormat.parse(options.get("format"), OutputFormat.TEXT)
        );
    }
}

final class SyncVarsRequest {
    final String envFile;
    final Platform platform;
    final String repo;
    final boolean execute;
    final boolean showSecrets;

    private SyncVarsRequest(String envFile, Platform platform, String repo, boolean execute, boolean showSecrets) {
        this.envFile = envFile;
        this.platform = platform;
        this.repo = repo;
        this.execute = execute;
        this.showSecrets = showSecrets;
    }

    static SyncVarsRequest fromOptions(Map<String, String> options) {
        return new SyncVarsRequest(
            requiredOption(options, "env-file"),
            platformOption(options),
            trimToNull(options.get("repo")),
            isTrue(options.get("execute")),
            isTrue(options.get("show-secrets"))
        );
    }
}

final class AuditVarsRequest {
    final String envFile;
    final Platform platform;
    final String githubRepo;
    final String gitlabRepo;
    final OutputFormat format;

    private AuditVarsRequest(String envFile, Platform platform, String githubRepo, String gitlabRepo,
                             OutputFormat format) {
        this.envFile = envFile;
        this.platform = platform;
        this.githubRepo = githubRepo;
        this.gitlabRepo = gitlabRepo;
        this.format = format;
    }

    static AuditVarsRequest fromOptions(Map<String, String> options) {
        return new AuditVarsRequest(
            requiredOption(options, "env-file"),
            platformOption(options),
            trimToNull(options.get("github-repo")),
            trimToNull(options.get("gitlab-repo")),
            OutputFormat.parse(options.get("format"), OutputFormat.TEXT)
        );
    }
}
