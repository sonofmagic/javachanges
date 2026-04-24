package io.github.sonofmagic.javachanges.core.env;

import io.github.sonofmagic.javachanges.core.OutputFormat;
import io.github.sonofmagic.javachanges.core.Platform;
import io.github.sonofmagic.javachanges.core.ReleaseTextUtils;

import java.util.Map;

public final class PlatformEnvRequest {
    public final String envFile;
    public final Platform platform;
    public final boolean showSecrets;
    public final OutputFormat format;

    private PlatformEnvRequest(String envFile, Platform platform, boolean showSecrets, OutputFormat format) {
        this.envFile = envFile;
        this.platform = platform;
        this.showSecrets = showSecrets;
        this.format = format;
    }

    public static PlatformEnvRequest fromOptions(Map<String, String> options) {
        return new PlatformEnvRequest(
            ReleaseTextUtils.requiredOption(options, "env-file"),
            ReleaseTextUtils.platformOption(options),
            ReleaseTextUtils.isTrue(options.get("show-secrets")),
            OutputFormat.parse(options.get("format"), OutputFormat.TEXT)
        );
    }
}
