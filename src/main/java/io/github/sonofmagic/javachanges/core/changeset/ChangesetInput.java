package io.github.sonofmagic.javachanges.core.changeset;

import io.github.sonofmagic.javachanges.core.ReleaseLevel;

import java.util.List;

public final class ChangesetInput {
    public final ReleaseLevel release;
    public final String type;
    public final List<String> modules;
    public final String summary;
    public final String body;

    public ChangesetInput(ReleaseLevel release, String type, List<String> modules, String summary, String body) {
        this.release = release;
        this.type = type;
        this.modules = modules;
        this.summary = summary;
        this.body = body;
    }
}
