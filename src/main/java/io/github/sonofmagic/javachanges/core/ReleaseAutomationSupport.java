package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.nio.file.Path;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.CHANGESETS_DIR;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.RELEASE_PLAN_MD;

final class ReleaseAutomationSupport {
    private final Path repoRoot;

    ReleaseAutomationSupport(Path repoRoot) {
        this.repoRoot = repoRoot;
    }

    ReleasePlan plan() throws IOException, InterruptedException {
        return new ReleasePlanner(repoRoot).plan();
    }

    String releaseVersionFromManifest() throws IOException {
        return RepoFiles.readManifestField(repoRoot, "releaseVersion");
    }

    String wholeRepoTagFromManifest() throws IOException {
        return "v" + releaseVersionFromManifest();
    }

    Path releasePlanMarkdownFile() {
        return repoRoot.resolve(CHANGESETS_DIR).resolve(RELEASE_PLAN_MD);
    }
}
