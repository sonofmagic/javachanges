package io.github.sonofmagic.javachanges.core.env;

import io.github.sonofmagic.javachanges.core.ReleaseTextUtils;

public final class EnvEntry {
    public final String name;
    public final boolean secret;
    public final boolean protectedValue;
    public final boolean required;

    public EnvEntry(String name, boolean secret, boolean protectedValue) {
        this(name, secret, protectedValue, ReleaseTextUtils.isRequiredName(name));
    }

    public EnvEntry(String name, boolean secret, boolean protectedValue, boolean required) {
        this.name = name;
        this.secret = secret;
        this.protectedValue = protectedValue;
        this.required = required;
    }
}
