package io.github.sonofmagic.javachanges.core.env;

import io.github.sonofmagic.javachanges.core.gitlab.GitlabProtectionSupport;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;

public final class ReleaseEnvSupport {
    private final ReleaseEnvRuntime runtime;
    private final ReleaseEnvRenderSupport renderSupport;
    private final ReleaseEnvAuditSupport auditSupport;
    private final ReleaseEnvSyncSupport syncSupport;
    private final ReleaseEnvInitSupport initSupport;
    private final ReleaseEnvAuthHelpSupport authHelpSupport;
    private final ReleaseEnvDoctorLocalSupport doctorLocalSupport;
    private final ReleaseEnvDoctorPlatformSupport doctorPlatformSupport;

    public ReleaseEnvSupport(Path repoRoot, PrintStream out) {
        this(repoRoot, out, new ReleaseEnvRuntime(repoRoot));
    }

    public ReleaseEnvSupport(Path repoRoot, PrintStream out, ReleaseEnvRuntime runtime) {
        this.runtime = runtime;
        GitlabProtectionSupport gitlabProtectionSupport = new GitlabProtectionSupport(runtime, out);
        this.renderSupport = new ReleaseEnvRenderSupport(out);
        this.auditSupport = new ReleaseEnvAuditSupport(repoRoot, out, runtime);
        this.syncSupport = new ReleaseEnvSyncSupport(repoRoot, out, runtime);
        this.initSupport = new ReleaseEnvInitSupport(runtime, out);
        this.authHelpSupport = new ReleaseEnvAuthHelpSupport(out);
        this.doctorLocalSupport = new ReleaseEnvDoctorLocalSupport(repoRoot, out, runtime);
        this.doctorPlatformSupport = new ReleaseEnvDoctorPlatformSupport(repoRoot, out, runtime, gitlabProtectionSupport);
    }

    public void initEnv(InitEnvRequest request) throws IOException {
        initSupport.initEnv(request);
    }

    public void printAuthHelp(io.github.sonofmagic.javachanges.core.Platform platform) {
        authHelpSupport.printAuthHelp(platform);
    }

    public boolean renderVars(PlatformEnvRequest request) throws IOException {
        LoadedEnv env = LoadedEnv.load(runtime.resolveEnvFile(request.envFile));
        return renderSupport.renderVars(env, request, runtime.relativizePath(env.path));
    }

    public boolean doctorLocal(LocalDoctorRequest request) throws IOException, InterruptedException {
        return doctorLocalSupport.doctorLocal(request);
    }

    public boolean doctorPlatform(DoctorPlatformRequest request) throws IOException, InterruptedException {
        return doctorPlatformSupport.doctorPlatform(request);
    }

    public void syncVars(SyncVarsRequest request) throws IOException, InterruptedException {
        LoadedEnv env = LoadedEnv.load(runtime.resolveEnvFile(request.envFile));
        syncSupport.syncVars(request, env, runtime.relativizePath(env.path));
    }

    public boolean auditVars(AuditVarsRequest request) throws IOException, InterruptedException {
        LoadedEnv env = LoadedEnv.load(runtime.resolveEnvFile(request.envFile));
        try {
            return auditSupport.auditVars(request, env, runtime.relativizePath(env.path));
        } catch (ReleaseEnvAuditSupport.AuditPreconditionFailure exception) {
            return false;
        }
    }

    public static String errorJson(String command, Exception exception) {
        return ReleaseEnvJsonSupport.errorJson(command, exception);
    }
}
