package io.github.sonofmagic.javachanges.core.changeset;

import io.github.sonofmagic.javachanges.core.ReleaseLevel;

import java.nio.file.Path;
import java.util.List;

public final class Changeset {
    public final Path path;
    public final String fileName;
    public final ReleaseLevel release;
    public final String type;
    public final List<String> modules;
    public final String summary;
    public final String body;

    public Changeset(Path path, String fileName, ReleaseLevel release, String type,
                     List<String> modules, String summary, String body) {
        this.path = path;
        this.fileName = fileName;
        this.release = release;
        this.type = type;
        this.modules = modules;
        this.summary = summary;
        this.body = body;
    }
}
