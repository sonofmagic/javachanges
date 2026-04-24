package io.github.sonofmagic.javachanges.core.env;

import io.github.sonofmagic.javachanges.core.ReleaseTextUtils;

import java.util.Map;

public final class InitEnvRequest {
    public final String template;
    public final String target;
    public final boolean force;

    private InitEnvRequest(String template, String target, boolean force) {
        this.template = template;
        this.target = target;
        this.force = force;
    }

    public static InitEnvRequest fromOptions(Map<String, String> options) {
        String target = ReleaseTextUtils.trimToNull(options.get("target"));
        if (target == null) {
            target = ReleaseTextUtils.trimToNull(options.get("path"));
        }
        return new InitEnvRequest(
            ReleaseTextUtils.trimToNull(options.get("template")) == null ? "env/release.env.example"
                : ReleaseTextUtils.trimToNull(options.get("template")),
            target == null ? "env/release.env.local" : target,
            ReleaseTextUtils.isTrue(options.get("force"))
        );
    }
}
