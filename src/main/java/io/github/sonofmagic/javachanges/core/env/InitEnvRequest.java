package io.github.sonofmagic.javachanges.core.env;

import io.github.sonofmagic.javachanges.core.ReleaseUtils;

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
        String target = ReleaseUtils.trimToNull(options.get("target"));
        if (target == null) {
            target = ReleaseUtils.trimToNull(options.get("path"));
        }
        return new InitEnvRequest(
            ReleaseUtils.trimToNull(options.get("template")) == null ? "env/release.env.example"
                : ReleaseUtils.trimToNull(options.get("template")),
            target == null ? "env/release.env.local" : target,
            ReleaseUtils.isTrue(options.get("force"))
        );
    }
}
