package io.github.sonofmagic.javachanges.core;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Map;

@Command(name = "init-env", mixinStandardHelpOptions = true,
    description = "Write a local release env file from the example template.")
final class InitEnvCommand extends AbstractCliCommand {
    @Option(names = "--template", description = "Template env file path.")
    private String template;

    @Option(names = {"--target", "--path"}, description = "Target env file path.")
    private String target;

    @Option(names = "--force", arity = "0..1", fallbackValue = "true", defaultValue = "false",
        description = "Overwrite the target file when it already exists.")
    private boolean force;

    @Override
    public Integer call() throws Exception {
        Map<String, String> options = options(
            option("template", template),
            option("target", target),
            flag("force", force)
        );
        new ReleaseEnvSupport(repoRoot(), out()).initEnv(InitEnvRequest.fromOptions(options));
        return success();
    }
}

@Command(name = "auth-help", mixinStandardHelpOptions = true,
    description = "Show required authentication variables for GitHub and GitLab.")
final class AuthHelpCommand extends AbstractCliCommand {
    @Option(names = "--platform", description = "github, gitlab, or all.")
    private String platform;

    @Override
    public Integer call() throws Exception {
        Map<String, String> options = options(option("platform", platform));
        new ReleaseEnvSupport(repoRoot(), out()).printAuthHelp(ReleaseUtils.platformOption(options));
        return success();
    }
}

@Command(name = "render-vars", mixinStandardHelpOptions = true,
    description = "Render env variables for GitHub and GitLab.")
final class RenderVarsCommand extends AbstractCliCommand {
    @Option(names = "--env-file", required = true, description = "Env file to render.")
    private String envFile;

    @Option(names = "--platform", description = "github, gitlab, or all.")
    private String platform;

    @Option(names = "--format", description = "Output format: text or json.")
    private String format;

    @Option(names = "--show-secrets", arity = "0..1", fallbackValue = "true", defaultValue = "false",
        description = "Show secret values instead of masking them.")
    private boolean showSecrets;

    @Override
    public Integer call() throws Exception {
        Map<String, String> options = options(
            option("env-file", envFile),
            option("platform", platform),
            option("format", format),
            flag("show-secrets", showSecrets)
        );
        final PlatformEnvRequest request = PlatformEnvRequest.fromOptions(options);
        return runEnvJsonCommand("render-vars", request.format, new ThrowingIntSupplier() {
            @Override
            public int get() throws Exception {
                return new ReleaseEnvSupport(repoRoot(), out()).renderVars(request) ? success() : 1;
            }
        });
    }
}

@Command(name = "doctor-local", mixinStandardHelpOptions = true,
    description = "Validate local release prerequisites.")
final class DoctorLocalCommand extends AbstractCliCommand {
    @Option(names = "--env-file", description = "Env file to validate.")
    private String envFile;

    @Option(names = "--github-repo", description = "GitHub owner/repo.")
    private String githubRepo;

    @Option(names = "--gitlab-repo", description = "GitLab group/project.")
    private String gitlabRepo;

    @Option(names = "--format", description = "Output format: text or json.")
    private String format;

    @Override
    public Integer call() throws Exception {
        Map<String, String> options = options(
            option("env-file", envFile),
            option("github-repo", githubRepo),
            option("gitlab-repo", gitlabRepo),
            option("format", format)
        );
        final LocalDoctorRequest request = LocalDoctorRequest.fromOptions(options);
        return runEnvJsonCommand("doctor-local", request.format, new ThrowingIntSupplier() {
            @Override
            public int get() throws Exception {
                return new ReleaseEnvSupport(repoRoot(), out()).doctorLocal(request) ? success() : 1;
            }
        });
    }
}

@Command(name = "doctor-platform", mixinStandardHelpOptions = true,
    description = "Validate remote platform variables and auth.")
final class DoctorPlatformCommand extends AbstractCliCommand {
    @Option(names = "--env-file", required = true, description = "Env file to validate.")
    private String envFile;

    @Option(names = "--platform", description = "github, gitlab, or all.")
    private String platform;

    @Option(names = "--github-repo", description = "GitHub owner/repo.")
    private String githubRepo;

    @Option(names = "--gitlab-repo", description = "GitLab group/project.")
    private String gitlabRepo;

    @Option(names = "--format", description = "Output format: text or json.")
    private String format;

    @Override
    public Integer call() throws Exception {
        Map<String, String> options = options(
            option("env-file", envFile),
            option("platform", platform),
            option("github-repo", githubRepo),
            option("gitlab-repo", gitlabRepo),
            option("format", format)
        );
        final DoctorPlatformRequest request = DoctorPlatformRequest.fromOptions(options);
        return runEnvJsonCommand("doctor-platform", request.format, new ThrowingIntSupplier() {
            @Override
            public int get() throws Exception {
                return new ReleaseEnvSupport(repoRoot(), out()).doctorPlatform(request) ? success() : 1;
            }
        });
    }
}

@Command(name = "sync-vars", mixinStandardHelpOptions = true,
    description = "Sync env variables to GitHub or GitLab.")
final class SyncVarsCommand extends AbstractCliCommand {
    @Option(names = "--env-file", required = true, description = "Env file to sync from.")
    private String envFile;

    @Option(names = "--platform", description = "github, gitlab, or all.")
    private String platform;

    @Option(names = "--repo", description = "Repository override for platform sync.")
    private String repo;

    @Option(names = "--execute", arity = "0..1", fallbackValue = "true", defaultValue = "false",
        description = "Execute platform changes instead of a dry run.")
    private boolean execute;

    @Option(names = "--show-secrets", arity = "0..1", fallbackValue = "true", defaultValue = "false",
        description = "Show secret values instead of masking them.")
    private boolean showSecrets;

    @Override
    public Integer call() throws Exception {
        Map<String, String> options = options(
            option("env-file", envFile),
            option("platform", platform),
            option("repo", repo),
            flag("execute", execute),
            flag("show-secrets", showSecrets)
        );
        new ReleaseEnvSupport(repoRoot(), out()).syncVars(SyncVarsRequest.fromOptions(options));
        return success();
    }
}

@Command(name = "audit-vars", mixinStandardHelpOptions = true,
    description = "Audit env variables against remote platform state.")
final class AuditVarsCommand extends AbstractCliCommand {
    @Option(names = "--env-file", required = true, description = "Env file to audit.")
    private String envFile;

    @Option(names = "--platform", description = "github, gitlab, or all.")
    private String platform;

    @Option(names = "--github-repo", description = "GitHub owner/repo.")
    private String githubRepo;

    @Option(names = "--gitlab-repo", description = "GitLab group/project.")
    private String gitlabRepo;

    @Option(names = "--format", description = "Output format: text or json.")
    private String format;

    @Override
    public Integer call() throws Exception {
        Map<String, String> options = options(
            option("env-file", envFile),
            option("platform", platform),
            option("github-repo", githubRepo),
            option("gitlab-repo", gitlabRepo),
            option("format", format)
        );
        final AuditVarsRequest request = AuditVarsRequest.fromOptions(options);
        return runEnvJsonCommand("audit-vars", request.format, new ThrowingIntSupplier() {
            @Override
            public int get() throws Exception {
                return new ReleaseEnvSupport(repoRoot(), out()).auditVars(request) ? success() : 1;
            }
        });
    }
}
