package io.github.sonofmagic.javachanges.core;

import io.github.sonofmagic.javachanges.core.changeset.Changeset;
import io.github.sonofmagic.javachanges.core.changeset.ChangesetFileSupport;
import io.github.sonofmagic.javachanges.core.changeset.ChangesetInput;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class RepoFiles {
    private RepoFiles() {
    }

    static Path resolveRepoRoot(String directoryOption) {
        return RepoRootResolver.resolveRepoRoot(directoryOption);
    }

    static void ensureChangesetReadme(Path repoRoot) throws IOException {
        ChangesetFileSupport.ensureChangesetReadme(repoRoot);
    }

    static Path writeChangeset(Path repoRoot, ChangesetInput input) throws IOException {
        return ChangesetFileSupport.writeChangeset(repoRoot, input);
    }

    static List<Changeset> loadChangesets(Path repoRoot) throws IOException {
        return ChangesetFileSupport.loadChangesets(repoRoot);
    }

    static String readManifestField(Path repoRoot, String field) throws IOException {
        return ChangesetFileSupport.readManifestField(repoRoot, field);
    }

    public static void applyPlan(Path repoRoot, ReleasePlan plan) throws IOException {
        ReleasePlanFiles.applyPlan(repoRoot, plan);
    }

    public static ChangesetConfigSupport.ChangesetConfig readChangesetConfig(Path repoRoot) throws IOException {
        return ChangesetConfigSupport.load(repoRoot);
    }
}
