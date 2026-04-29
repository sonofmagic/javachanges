package io.github.sonofmagic.javachanges.core.publish;

import io.github.sonofmagic.javachanges.core.MavenCommand;
import io.github.sonofmagic.javachanges.core.MavenSettingsWriter;
import io.github.sonofmagic.javachanges.core.ReleaseMessages;
import io.github.sonofmagic.javachanges.core.ReleaseModuleUtils;
import io.github.sonofmagic.javachanges.core.ReleaseProcessUtils;
import io.github.sonofmagic.javachanges.core.ReleaseTextUtils;
import io.github.sonofmagic.javachanges.core.SnapshotVersionMode;
import io.github.sonofmagic.javachanges.core.VersionSupport;
import io.github.sonofmagic.javachanges.core.automation.AutomationJsonSupport;
import io.github.sonofmagic.javachanges.core.automation.ReleaseNotesGenerator;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class PublishSupport {
    private final Path repoRoot;
    private final PrintStream out;
    private final PublishRuntime runtime;
    private final VersionSupport versionSupport;
    private final ReleaseNotesGenerator releaseNotesGenerator;
    private final PublishPlanSupport planSupport;

    public PublishSupport(Path repoRoot, PrintStream out) {
        this.repoRoot = repoRoot;
        this.out = out;
        this.runtime = new PublishRuntime(repoRoot);
        this.versionSupport = new VersionSupport(repoRoot);
        this.releaseNotesGenerator = new ReleaseNotesGenerator(repoRoot);
        this.planSupport = new PublishPlanSupport(repoRoot, runtime, versionSupport);
    }

    public void preflight(PublishRequest request) throws IOException, InterruptedException {
        PublishPlanSupport.PublishTarget publishTarget = planSupport.resolvePublishTarget(request);
        AutomationJsonSupport.AutomationReport report = planSupport.buildReport("preflight", request, publishTarget);
        preflight(request, publishTarget, report);
        if (request.format == io.github.sonofmagic.javachanges.core.OutputFormat.JSON) {
            out.println(report.toJson());
        }
    }

    public void publish(PublishRequest request) throws IOException, InterruptedException {
        PublishPlanSupport.PublishTarget publishTarget = planSupport.resolvePublishTarget(request);
        AutomationJsonSupport.AutomationReport report = planSupport.buildReport("publish", request, publishTarget);
        preflight(request, publishTarget, report);

        Path localMavenRepo = request.execute
            ? runtime.ensureLocalMavenRepositoryDirectory()
            : runtime.localMavenRepositoryPath();
        boolean releaseNotesAvailable = !request.snapshot && runtime.gitRefExists(request.tag);
        if (request.execute) {
            Files.createDirectories(repoRoot.resolve(".m2"));
            Files.createDirectories(repoRoot.resolve("target"));
            MavenSettingsWriter.write(repoRoot.resolve(".m2/settings.xml"),
                request.snapshot ? MavenSettingsWriter.RepositoryMode.SNAPSHOT : MavenSettingsWriter.RepositoryMode.RELEASE);

            if (releaseNotesAvailable) {
                Path releaseNotesFile = repoRoot.resolve("target/release-notes.md");
                releaseNotesGenerator.writeReleaseNotes(request.tag, releaseNotesFile);
                report.releaseNotesFile = releaseNotesFile.toString();
            }
        }

        MavenCommand mavenCommand = ReleaseProcessUtils.resolveMavenCommand(repoRoot);
        if (mavenCommand == null) {
            throw new IllegalStateException(ReleaseMessages.noMavenCommandFound());
        }

        List<String> command = planSupport.buildDeployCommand(request, publishTarget, mavenCommand, localMavenRepo);

        if (request.format != io.github.sonofmagic.javachanges.core.OutputFormat.JSON) {
            out.println();
            out.println(ReleaseMessages.dryRunOutputHeading());
            out.println(request.execute
                ? ReleaseMessages.generatedMavenSettingsFile()
                : ReleaseMessages.mavenSettingsWillBeWritten());
            out.println(ReleaseMessages.mavenCommandLabel(mavenCommand.command, mavenCommand.source));
            if (localMavenRepo != null) {
                out.println(ReleaseMessages.localMavenRepository(localMavenRepo));
            }
            if (publishTarget.publishVersion != null) {
                out.println(request.snapshot
                    ? "snapshot publish version: " + publishTarget.publishVersion
                    : "publish version: " + publishTarget.publishVersion);
            }
            if (request.snapshot) {
                out.println("snapshot version mode: " + publishTarget.snapshotVersionMode.id);
                out.println("snapshot build stamp applied: " + publishTarget.snapshotBuildStampApplied);
            }
            if (releaseNotesAvailable) {
                out.println(request.execute
                    ? ReleaseMessages.generatedReleaseNotesFile()
                    : ReleaseMessages.releaseNotesWillBeWritten());
            }
            out.println(ReleaseMessages.targetModule(publishTarget.resolvedModule));
            out.println();
            out.println(ReleaseMessages.commandToRun());
            out.println(ReleaseTextUtils.renderCommand(command));
        }

        if (!request.execute) {
            report.reason = "Dry-run only.";
            if (request.format == io.github.sonofmagic.javachanges.core.OutputFormat.JSON) {
                out.println(report.toJson());
            } else {
                out.println();
                out.println(ReleaseMessages.dryRunOnlyMavenPublish());
            }
            return;
        }

        if (request.format != io.github.sonofmagic.javachanges.core.OutputFormat.JSON) {
            out.println();
            out.println(ReleaseMessages.runningMavenHeading());
        }
        int exitCode = ReleaseProcessUtils.runCommand(command, repoRoot);
        if (exitCode != 0) {
            throw new IllegalStateException(ReleaseMessages.mavenDeployFailed(exitCode));
        }
        report.action = request.snapshot ? "publish-snapshot" : "publish-release";
        report.reason = "Publish completed.";
        if (request.format == io.github.sonofmagic.javachanges.core.OutputFormat.JSON) {
            out.println(report.toJson());
        }
    }

    private void preflight(PublishRequest request, PublishPlanSupport.PublishTarget publishTarget,
                           AutomationJsonSupport.AutomationReport report) throws IOException, InterruptedException {
        Path tempDir = Files.createTempDirectory("javachanges-preflight-");
        ReleaseProcessUtils.restrictOwnerOnly(tempDir);
        try {
            preflight(request, publishTarget, report, tempDir);
        } finally {
            ReleaseProcessUtils.deleteRecursively(tempDir);
        }
    }

    private void preflight(PublishRequest request, PublishPlanSupport.PublishTarget publishTarget,
                           AutomationJsonSupport.AutomationReport report, Path tempDir)
        throws IOException, InterruptedException {
        if (request.module != null) {
            ReleaseModuleUtils.assertKnownModule(repoRoot, request.module);
        }

        if (!request.allowDirty && runtime.hasDirtyWorktree()) {
            throw new IllegalStateException(ReleaseMessages.dirtyWorktree());
        }

        if (request.format != io.github.sonofmagic.javachanges.core.OutputFormat.JSON) {
            out.println(ReleaseMessages.versionCheckHeading());
        }
        String currentVersion = versionSupport.readRevision();
        if (request.format != io.github.sonofmagic.javachanges.core.OutputFormat.JSON) {
            out.println(ReleaseMessages.currentRevisionValue(currentVersion));
            out.println();
            out.println(ReleaseMessages.publishModeCheckHeading());
        }

        if (request.snapshot) {
            if (request.format != io.github.sonofmagic.javachanges.core.OutputFormat.JSON) {
                out.println(ReleaseMessages.snapshotCheckPassed());
                out.println("snapshot version mode: " + publishTarget.snapshotVersionMode.id);
                out.println(publishTarget.snapshotVersionMode == SnapshotVersionMode.PLAIN
                    ? ReleaseMessages.plainSnapshotDescription()
                    : ReleaseMessages.stampedSnapshotDescription());
                out.println("snapshot publish version: " + publishTarget.publishVersion);
            }
        } else {
            if (request.format != io.github.sonofmagic.javachanges.core.OutputFormat.JSON) {
                out.println("release tag: " + request.tag);
                out.println("release version: " + publishTarget.publishVersion);
            }
        }

        if (request.format != io.github.sonofmagic.javachanges.core.OutputFormat.JSON) {
            out.println(publishTarget.resolvedModule == null ? "target module: all" : "target module: " + publishTarget.resolvedModule);
            out.println();
            out.println(ReleaseMessages.repositoryVariableCheckHeading());
        }
        if (request.snapshot) {
            String ignored = ReleaseTextUtils.requireEnv("MAVEN_SNAPSHOT_REPOSITORY_URL");
            if (request.format != io.github.sonofmagic.javachanges.core.OutputFormat.JSON) {
                out.println("MAVEN_SNAPSHOT_REPOSITORY_URL=" + ignored);
            }
        } else {
            String ignored = ReleaseTextUtils.requireEnv("MAVEN_RELEASE_REPOSITORY_URL");
            if (request.format != io.github.sonofmagic.javachanges.core.OutputFormat.JSON) {
                out.println("MAVEN_RELEASE_REPOSITORY_URL=" + ignored);
            }
        }

        if (request.format != io.github.sonofmagic.javachanges.core.OutputFormat.JSON) {
            out.println();
            out.println(ReleaseMessages.credentialCheckHeading());
        }
        Path settingsFile = tempDir.resolve("settings.xml");
        MavenSettingsWriter.write(settingsFile,
            request.snapshot ? MavenSettingsWriter.RepositoryMode.SNAPSHOT : MavenSettingsWriter.RepositoryMode.RELEASE);
        ReleaseProcessUtils.restrictOwnerOnly(settingsFile);
        if (request.format != io.github.sonofmagic.javachanges.core.OutputFormat.JSON) {
            out.println(ReleaseMessages.mavenSettingsGenerationPassed());
        }

        if (!request.snapshot) {
            if (request.format != io.github.sonofmagic.javachanges.core.OutputFormat.JSON) {
                out.println();
                out.println(ReleaseMessages.releaseNotesPreflightHeading());
            }
            if (runtime.gitRefExists(request.tag)) {
                Path releaseNotesFile = tempDir.resolve("release-notes.md");
                releaseNotesGenerator.writeReleaseNotes(request.tag, releaseNotesFile);
                if (request.format != io.github.sonofmagic.javachanges.core.OutputFormat.JSON) {
                    out.println(ReleaseMessages.releaseNotesGenerationPassed());
                }
            } else if (request.format != io.github.sonofmagic.javachanges.core.OutputFormat.JSON) {
                out.println(ReleaseMessages.localTagMissingSkipReleaseNotes(request.tag));
            }
        }

        report.reason = "Preflight checks passed.";
        if (request.format != io.github.sonofmagic.javachanges.core.OutputFormat.JSON) {
            out.println();
            out.println(ReleaseMessages.preflightChecksPassed());
        }
    }
}
