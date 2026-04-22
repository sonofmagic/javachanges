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

    ReleaseDescriptor descriptorFromPlan(ReleasePlan plan) {
        return new ReleaseDescriptor(plan.getReleaseVersion());
    }

    ReleaseDescriptor descriptorFromManifest() throws IOException {
        return new ReleaseDescriptor(releaseVersionFromManifest());
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

    static final class ReleaseDescriptor {
        final String releaseVersion;

        ReleaseDescriptor(String releaseVersion) {
            this.releaseVersion = releaseVersion;
        }

        String commitMessage() {
            return "chore(release): apply changesets for v" + releaseVersion;
        }

        String githubPullRequestTitle() {
            return "chore(release): v" + releaseVersion;
        }

        String gitlabMergeRequestTitle() {
            return "chore(release): release v" + releaseVersion;
        }

        String wholeRepoTagName() {
            return "v" + releaseVersion;
        }
    }
}
