package io.github.sonofmagic.javachanges.core.publish;

import io.github.sonofmagic.javachanges.core.BuildModelSupport;
import io.github.sonofmagic.javachanges.core.MavenCommand;
import io.github.sonofmagic.javachanges.core.MavenSettingsWriter;
import io.github.sonofmagic.javachanges.core.PomModelSupport;
import io.github.sonofmagic.javachanges.core.ReleaseMessages;
import io.github.sonofmagic.javachanges.core.ReleaseModuleUtils;
import io.github.sonofmagic.javachanges.core.ReleaseTextUtils;
import io.github.sonofmagic.javachanges.core.SnapshotVersionMode;
import io.github.sonofmagic.javachanges.core.VersionSupport;
import io.github.sonofmagic.javachanges.core.automation.AutomationJsonSupport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class PublishPlanSupport {
    public static final String TEMPORARY_PUBLISH_POM = ".javachanges-publish-pom.xml";
    private final Path repoRoot;
    private final PublishRuntime runtime;
    private final VersionSupport versionSupport;

    public PublishPlanSupport(Path repoRoot, PublishRuntime runtime, VersionSupport versionSupport) {
        this.repoRoot = repoRoot;
        this.runtime = runtime;
        this.versionSupport = versionSupport;
    }

    public PublishTarget resolvePublishTarget(PublishRequest request) throws IOException, InterruptedException {
        String resolvedModule = request.module;
        if (request.snapshot) {
            versionSupport.assertSnapshot();
            if (request.snapshotVersionMode == SnapshotVersionMode.PLAIN) {
                return publishTarget(versionSupport.snapshotRevision(), resolvedModule,
                    SnapshotVersionMode.PLAIN, false);
            }
            String buildStamp = ReleaseTextUtils.firstNonBlank(request.snapshotBuildStamp, runtime.snapshotBuildStamp());
            return publishTarget(versionSupport.resolveSnapshotPublishVersion(buildStamp), resolvedModule,
                SnapshotVersionMode.STAMPED, true);
        }

        versionSupport.assertReleaseTag(request.tag);
        String releaseVersion = ReleaseModuleUtils.releaseVersionFromTag(request.tag);
        String tagModule = ReleaseModuleUtils.releaseModuleFromTag(request.tag);
        if (resolvedModule == null) {
            resolvedModule = tagModule;
        } else if (tagModule != null && !resolvedModule.equals(tagModule)) {
            throw new IllegalStateException(ReleaseMessages.explicitModuleDoesNotMatchTagModule(resolvedModule, tagModule));
        }
        return publishTarget(releaseVersion, resolvedModule, null, false);
    }

    private PublishTarget publishTarget(String publishVersion, String resolvedModule,
                                        SnapshotVersionMode snapshotVersionMode,
                                        boolean snapshotBuildStampApplied) throws IOException {
        PomModelSupport.VersionSource versionSource = versionSupport.mavenVersionSource();
        boolean temporaryPomRequired = versionSource == PomModelSupport.VersionSource.PROJECT_VERSION
            && !publishVersion.equals(versionSupport.readRevision());
        return new PublishTarget(publishVersion, resolvedModule, snapshotVersionMode,
            snapshotBuildStampApplied, versionSource, temporaryPomRequired);
    }

    public AutomationJsonSupport.AutomationReport buildReport(String command, PublishRequest request, PublishTarget publishTarget) {
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

    public List<String> buildDeployCommand(PublishRequest request, PublishTarget publishTarget, MavenCommand mavenCommand,
                                           Path localMavenRepo) {
        String repositoryUrl = request.snapshot
            ? ReleaseTextUtils.requireEnv("MAVEN_SNAPSHOT_REPOSITORY_URL")
            : ReleaseTextUtils.requireEnv("MAVEN_RELEASE_REPOSITORY_URL");
        return buildDeployCommand(request, publishTarget, mavenCommand, localMavenRepo, repositoryUrl);
    }

    public List<String> buildDeployCommand(PublishRequest request, PublishTarget publishTarget, MavenCommand mavenCommand,
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
        if (publishTarget.versionSource == PomModelSupport.VersionSource.REVISION_PROPERTY
            && publishTarget.publishVersion != null) {
            command.add("-Drevision=" + publishTarget.publishVersion);
        }
        if (publishTarget.temporaryPomRequired) {
            command.add("-f");
            command.add(TEMPORARY_PUBLISH_POM);
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

    public Path prepareTemporaryPublishPom(PublishTarget publishTarget) throws IOException {
        if (!publishTarget.temporaryPomRequired) {
            return null;
        }
        Path path = repoRoot.resolve(TEMPORARY_PUBLISH_POM);
        if (Files.exists(path)) {
            throw new IllegalStateException(ReleaseMessages.temporaryMavenPublishPomExists(path));
        }
        try {
            BuildModelSupport.writeMavenVersionCopy(repoRoot, path, publishTarget.publishVersion);
            return path;
        } catch (IOException exception) {
            Files.deleteIfExists(path);
            throw exception;
        } catch (RuntimeException exception) {
            Files.deleteIfExists(path);
            throw exception;
        }
    }

    public static final class PublishTarget {
        public final String publishVersion;
        public final String resolvedModule;
        public final SnapshotVersionMode snapshotVersionMode;
        public final boolean snapshotBuildStampApplied;
        public final PomModelSupport.VersionSource versionSource;
        public final boolean temporaryPomRequired;

        PublishTarget(String publishVersion, String resolvedModule, SnapshotVersionMode snapshotVersionMode,
                      boolean snapshotBuildStampApplied, PomModelSupport.VersionSource versionSource,
                      boolean temporaryPomRequired) {
            this.publishVersion = publishVersion;
            this.resolvedModule = resolvedModule;
            this.snapshotVersionMode = snapshotVersionMode;
            this.snapshotBuildStampApplied = snapshotBuildStampApplied;
            this.versionSource = versionSource;
            this.temporaryPomRequired = temporaryPomRequired;
        }
    }
}
