package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.firstNonBlank;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.releaseModuleFromTag;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.releaseVersionFromTag;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.requireEnv;

final class PublishPlanSupport {
    private final Path repoRoot;
    private final PublishRuntime runtime;
    private final VersionSupport versionSupport;

    PublishPlanSupport(Path repoRoot, PublishRuntime runtime, VersionSupport versionSupport) {
        this.repoRoot = repoRoot;
        this.runtime = runtime;
        this.versionSupport = versionSupport;
    }

    PublishTarget resolvePublishTarget(PublishRequest request) throws IOException, InterruptedException {
        String resolvedModule = request.module;
        if (request.snapshot) {
            versionSupport.assertSnapshot();
            if (request.snapshotVersionMode == SnapshotVersionMode.PLAIN) {
                return new PublishTarget(versionSupport.snapshotRevision(), resolvedModule,
                    SnapshotVersionMode.PLAIN, false);
            }
            String buildStamp = firstNonBlank(request.snapshotBuildStamp, runtime.snapshotBuildStamp());
            return new PublishTarget(versionSupport.resolveSnapshotPublishVersion(buildStamp), resolvedModule,
                SnapshotVersionMode.STAMPED, true);
        }

        versionSupport.assertReleaseTag(request.tag);
        String releaseVersion = releaseVersionFromTag(request.tag);
        String tagModule = releaseModuleFromTag(request.tag);
        if (resolvedModule == null) {
            resolvedModule = tagModule;
        } else if (tagModule != null && !resolvedModule.equals(tagModule)) {
            throw new IllegalStateException("显式指定的模块 " + resolvedModule + " 与 tag 中的模块 " + tagModule + " 不一致");
        }
        return new PublishTarget(releaseVersion, resolvedModule, null, false);
    }

    AutomationJsonSupport.AutomationReport buildReport(String command, PublishRequest request, PublishTarget publishTarget) {
        AutomationJsonSupport.AutomationReport report = new AutomationJsonSupport.AutomationReport(command);
        report.action = request.snapshot ? "publish-snapshot" : "publish-release";
        report.execute = request.execute;
        report.dryRun = !request.execute;
        report.releaseVersion = publishTarget.publishVersion;
        report.effectiveVersion = publishTarget.publishVersion;
        report.releaseModule = publishTarget.resolvedModule;
        report.snapshotVersionMode = publishTarget.snapshotVersionMode == null ? null : publishTarget.snapshotVersionMode.id;
        report.snapshotBuildStampApplied = publishTarget.snapshotBuildStampApplied;
        report.tag = request.tag;
        return report;
    }

    List<String> buildDeployCommand(PublishRequest request, PublishTarget publishTarget, MavenCommand mavenCommand,
                                    Path localMavenRepo) {
        String repositoryUrl = request.snapshot
            ? requireEnv("MAVEN_SNAPSHOT_REPOSITORY_URL")
            : requireEnv("MAVEN_RELEASE_REPOSITORY_URL");
        return buildDeployCommand(request, publishTarget, mavenCommand, localMavenRepo, repositoryUrl);
    }

    List<String> buildDeployCommand(PublishRequest request, PublishTarget publishTarget, MavenCommand mavenCommand,
                                    Path localMavenRepo, String repositoryUrl) {
        List<String> command = new ArrayList<String>();
        command.add(mavenCommand.command);
        command.add("--batch-mode");
        command.add("--errors");
        command.add("--show-version");
        command.add("-s");
        command.add(".m2/settings.xml");
        if (localMavenRepo != null) {
            command.add("-Dmaven.repo.local=" + localMavenRepo.toString());
        }
        if (publishTarget.publishVersion != null) {
            command.add("-Drevision=" + publishTarget.publishVersion);
        }
        if (publishTarget.resolvedModule != null) {
            command.add("-pl");
            command.add(":" + publishTarget.resolvedModule);
            command.add("-am");
        }
        if (request.snapshot) {
            command.add("-Dmaven.snapshot.repository.id=" + MavenSettingsWriter.snapshotServerId());
            command.add("-Dmaven.snapshot.repository.url=" + repositoryUrl);
        } else {
            command.add("-Dmaven.release.repository.id=" + MavenSettingsWriter.releaseServerId());
            command.add("-Dmaven.release.repository.url=" + repositoryUrl);
        }
        command.add("clean");
        command.add("deploy");
        return command;
    }

    Path repoRoot() {
        return repoRoot;
    }

    static final class PublishTarget {
        final String publishVersion;
        final String resolvedModule;
        final SnapshotVersionMode snapshotVersionMode;
        final boolean snapshotBuildStampApplied;

        PublishTarget(String publishVersion, String resolvedModule, SnapshotVersionMode snapshotVersionMode,
                      boolean snapshotBuildStampApplied) {
            this.publishVersion = publishVersion;
            this.resolvedModule = resolvedModule;
            this.snapshotVersionMode = snapshotVersionMode;
            this.snapshotBuildStampApplied = snapshotBuildStampApplied;
        }
    }
}
