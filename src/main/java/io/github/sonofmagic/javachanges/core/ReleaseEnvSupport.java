package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;

final class ReleaseEnvSupport {
    private final ReleaseEnvRuntime runtime;
    private final ReleaseEnvRenderSupport renderSupport;
    private final ReleaseEnvAuditSupport auditSupport;
    private final ReleaseEnvSyncSupport syncSupport;
    private final ReleaseEnvInitSupport initSupport;
    private final ReleaseEnvAuthHelpSupport authHelpSupport;
    private final ReleaseEnvDoctorLocalSupport doctorLocalSupport;
    private final ReleaseEnvDoctorPlatformSupport doctorPlatformSupport;

    ReleaseEnvSupport(Path repoRoot, PrintStream out) {
        this(repoRoot, out, new ReleaseEnvRuntime(repoRoot));
    }

    ReleaseEnvSupport(Path repoRoot, PrintStream out, ReleaseEnvRuntime runtime) {
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

    void initEnv(InitEnvRequest request) throws IOException {
        initSupport.initEnv(request);
    }

    void printAuthHelp(Platform platform) {
        authHelpSupport.printAuthHelp(platform);
    }

    boolean renderVars(PlatformEnvRequest request) throws IOException {
        LoadedEnv env = LoadedEnv.load(runtime.resolveEnvFile(request.envFile));
        return renderSupport.renderVars(env, request, runtime.relativizePath(env.path));
    }

    boolean doctorLocal(LocalDoctorRequest request) throws IOException, InterruptedException {
        return doctorLocalSupport.doctorLocal(request);
    }

    boolean doctorPlatform(DoctorPlatformRequest request) throws IOException, InterruptedException {
        return doctorPlatformSupport.doctorPlatform(request);
    }

    void syncVars(SyncVarsRequest request) throws IOException, InterruptedException {
        LoadedEnv env = LoadedEnv.load(runtime.resolveEnvFile(request.envFile));
        syncSupport.syncVars(request, env, runtime.relativizePath(env.path));
    }

    boolean auditVars(AuditVarsRequest request) throws IOException, InterruptedException {
        LoadedEnv env = LoadedEnv.load(runtime.resolveEnvFile(request.envFile));
        try {
            return auditSupport.auditVars(request, env, runtime.relativizePath(env.path));
        } catch (ReleaseEnvAuditSupport.AuditPreconditionFailure exception) {
            return false;
        }
    }

    static String errorJson(String command, Exception exception) {
        return ReleaseEnvJsonSupport.errorJson(command, exception);
    }
}
