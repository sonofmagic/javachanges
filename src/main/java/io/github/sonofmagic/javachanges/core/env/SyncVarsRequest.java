package io.github.sonofmagic.javachanges.core.env;

import io.github.sonofmagic.javachanges.core.Platform;
import io.github.sonofmagic.javachanges.core.ReleaseTextUtils;

import java.util.Map;

public final class SyncVarsRequest {
    public final String envFile;
    public final Platform platform;
    public final String repo;
    public final boolean execute;
    public final boolean showSecrets;

    private SyncVarsRequest(String envFile, Platform platform, String repo, boolean execute, boolean showSecrets) {
        this.envFile = envFile;
        this.platform = platform;
        this.repo = repo;
        this.execute = execute;
        this.showSecrets = showSecrets;
    }

    public static SyncVarsRequest fromOptions(Map<String, String> options) {
        return new SyncVarsRequest(
            ReleaseTextUtils.requiredOption(options, "env-file"),
            ReleaseTextUtils.platformOption(options),
            ReleaseTextUtils.trimToNull(options.get("repo")),
            ReleaseTextUtils.isTrue(options.get("execute")),
            ReleaseTextUtils.isTrue(options.get("show-secrets"))
        );
    }
}
