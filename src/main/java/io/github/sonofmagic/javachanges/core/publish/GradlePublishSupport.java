package io.github.sonofmagic.javachanges.core.publish;

import io.github.sonofmagic.javachanges.core.GradleCommand;
import io.github.sonofmagic.javachanges.core.OutputFormat;
import io.github.sonofmagic.javachanges.core.ReleaseModuleUtils;
import io.github.sonofmagic.javachanges.core.ReleaseMessages;
import io.github.sonofmagic.javachanges.core.ReleaseProcessUtils;
import io.github.sonofmagic.javachanges.core.ReleaseTextUtils;
import io.github.sonofmagic.javachanges.core.VersionSupport;
import io.github.sonofmagic.javachanges.core.automation.AutomationJsonSupport;
import io.github.sonofmagic.javachanges.gradle.GradleModelSupport;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class GradlePublishSupport {
    private static final String DEFAULT_TASK = "publish";
    private final Path repoRoot;
    private final PrintStream out;
    private final PublishRuntime runtime;
    private final PublishPlanSupport planSupport;

    public GradlePublishSupport(Path repoRoot, PrintStream out) {
        this.repoRoot = repoRoot;
        this.out = out;
        this.runtime = new PublishRuntime(repoRoot);
        VersionSupport versionSupport = new VersionSupport(repoRoot);
        this.planSupport = new PublishPlanSupport(repoRoot, runtime, versionSupport);
    }

    public void publish(PublishRequest request) throws IOException, InterruptedException {
        publish(request, DEFAULT_TASK);
    }

    public void publish(PublishRequest request, String task) throws IOException, InterruptedException {
        String resolvedTask = normalizeTask(task);
        PublishPlanSupport.PublishTarget publishTarget = planSupport.resolvePublishTarget(request);
        AutomationJsonSupport.AutomationReport report = planSupport.buildReport("gradle-publish", request, publishTarget);
        preflight(request);

        GradleCommand gradleCommand = ReleaseProcessUtils.resolveGradleCommand(repoRoot);
        if (gradleCommand == null) {
            throw new IllegalStateException(ReleaseMessages.noGradleCommandFound());
        }

        List<String> command = buildPublishCommand(publishTarget, gradleCommand, resolvedTask);
        if (request.format != OutputFormat.JSON) {
            out.println();
            out.println(ReleaseMessages.gradleDryRunOutputHeading());
            out.println(ReleaseMessages.gradleCommandLabel(gradleCommand.command, gradleCommand.source));
            out.println(ReleaseMessages.gradleTask(resolvedTask));
            out.println(ReleaseMessages.publishVersion(publishTarget.publishVersion));
            if (request.snapshot) {
                out.println(ReleaseMessages.snapshotVersionMode(publishTarget.snapshotVersionMode.id));
                out.println(ReleaseMessages.snapshotBuildStampApplied(publishTarget.snapshotBuildStampApplied));
            }
            out.println(ReleaseMessages.targetModule(publishTarget.resolvedModule));
            out.println();
            out.println(ReleaseMessages.commandToRun());
            out.println(ReleaseTextUtils.renderCommand(command));
        }

        if (!request.execute) {
            report.reason = ReleaseMessages.dryRunOnlyReason();
            if (request.format == OutputFormat.JSON) {
                out.println(report.toJson());
            } else {
                out.println();
                out.println(ReleaseMessages.dryRunOnlyGradlePublish());
            }
            return;
        }

        if (request.format != OutputFormat.JSON) {
            out.println();
            out.println(ReleaseMessages.runningGradleHeading());
        }
        int exitCode = ReleaseProcessUtils.runCommand(command, repoRoot);
        if (exitCode != 0) {
            throw new IllegalStateException(ReleaseMessages.gradlePublishFailed(exitCode));
        }
        report.reason = ReleaseMessages.gradlePublishCompletedReason();
        if (request.format == OutputFormat.JSON) {
            out.println(report.toJson());
        }
    }

    List<String> buildPublishCommand(PublishPlanSupport.PublishTarget publishTarget, GradleCommand gradleCommand) {
        return buildPublishCommand(publishTarget, gradleCommand, DEFAULT_TASK);
    }

    List<String> buildPublishCommand(PublishPlanSupport.PublishTarget publishTarget, GradleCommand gradleCommand,
                                     String task) {
        String resolvedTask = normalizeTask(task);
        List<String> command = new ArrayList<String>();
        command.add(gradleCommand.command);
        command.add("--no-daemon");
        if (publishTarget.resolvedModule == null) {
            command.add(resolvedTask);
        } else {
            ReleaseModuleUtils.assertKnownModule(repoRoot, publishTarget.resolvedModule);
            if (resolvedTask.indexOf(':') >= 0) {
                throw new IllegalArgumentException(ReleaseMessages.taskMustBeNameWhenModuleSet(task));
            }
            command.add(":" + publishTarget.resolvedModule + ":" + resolvedTask);
        }
        command.add("-P" + publishVersionPropertyName() + "=" + publishTarget.publishVersion);
        return command;
    }

    private String publishVersionPropertyName() {
        try {
            return GradleModelSupport.readVersionPropertyName(repoRoot.resolve("gradle.properties"));
        } catch (IOException exception) {
            return "version";
        }
    }

    private static String normalizeTask(String task) {
        String value = ReleaseTextUtils.trimToNull(task);
        if (value == null) {
            return DEFAULT_TASK;
        }
        if (!value.matches(":?[A-Za-z][A-Za-z0-9_.:-]*")) {
            throw new IllegalArgumentException(ReleaseMessages.unsupportedGradleTask(task));
        }
        return value;
    }

    private void preflight(PublishRequest request) throws IOException, InterruptedException {
        if (request.module != null) {
            ReleaseModuleUtils.assertKnownModule(repoRoot, request.module);
        }
        if (!request.allowDirty && runtime.hasDirtyWorktree()) {
            throw new IllegalStateException(ReleaseMessages.dirtyWorktree());
        }
    }
}
