package io.github.sonofmagic.javachanges.core.publish;

import io.github.sonofmagic.javachanges.core.GradleCommand;
import io.github.sonofmagic.javachanges.core.OutputFormat;
import io.github.sonofmagic.javachanges.core.ReleaseModuleUtils;
import io.github.sonofmagic.javachanges.core.ReleaseProcessUtils;
import io.github.sonofmagic.javachanges.core.ReleaseTextUtils;
import io.github.sonofmagic.javachanges.core.VersionSupport;
import io.github.sonofmagic.javachanges.core.automation.AutomationJsonSupport;

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
            throw new IllegalStateException("未找到可用的 Gradle 命令，期望仓库内存在 "
                + ReleaseProcessUtils.gradleWrapperPath() + " 或系统中可用 gradle");
        }

        List<String> command = buildPublishCommand(publishTarget, gradleCommand, resolvedTask);
        if (request.format != OutputFormat.JSON) {
            out.println();
            out.println("== Gradle Publish Dry Run ==");
            out.println("Gradle command: " + gradleCommand.command + " (" + gradleCommand.source + ")");
            out.println("Gradle task: " + resolvedTask);
            out.println("publish version: " + publishTarget.publishVersion);
            if (request.snapshot) {
                out.println("snapshot version mode: " + publishTarget.snapshotVersionMode.id);
                out.println("snapshot build stamp applied: " + publishTarget.snapshotBuildStampApplied);
            }
            out.println(publishTarget.resolvedModule == null ? "target module: all" : "target module: " + publishTarget.resolvedModule);
            out.println();
            out.println("Command to run:");
            out.println(ReleaseTextUtils.renderCommand(command));
        }

        if (!request.execute) {
            report.reason = "Dry-run only.";
            if (request.format == OutputFormat.JSON) {
                out.println(report.toJson());
            } else {
                out.println();
                out.println("当前为 dry-run，未执行 Gradle。传入 --execute true 才会真正发布。");
            }
            return;
        }

        if (request.format != OutputFormat.JSON) {
            out.println();
            out.println("== 开始执行 Gradle ==");
        }
        int exitCode = ReleaseProcessUtils.runCommand(command, repoRoot);
        if (exitCode != 0) {
            throw new IllegalStateException("Gradle publish failed with exit code " + exitCode);
        }
        report.reason = "Gradle publish completed.";
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
                throw new IllegalArgumentException("--task must be a task name, not a project path, when --module is set: " + task);
            }
            command.add(":" + publishTarget.resolvedModule + ":" + resolvedTask);
        }
        command.add("-Pversion=" + publishTarget.publishVersion);
        return command;
    }

    private static String normalizeTask(String task) {
        String value = ReleaseTextUtils.trimToNull(task);
        if (value == null) {
            return DEFAULT_TASK;
        }
        if (!value.matches(":?[A-Za-z][A-Za-z0-9_.:-]*")) {
            throw new IllegalArgumentException("Unsupported Gradle task: " + task);
        }
        return value;
    }

    private void preflight(PublishRequest request) throws IOException, InterruptedException {
        if (request.module != null) {
            ReleaseModuleUtils.assertKnownModule(repoRoot, request.module);
        }
        if (!request.allowDirty && runtime.hasDirtyWorktree()) {
            throw new IllegalStateException("工作区存在未提交修改。若这是预期行为，可使用 --allow-dirty true 跳过检查。");
        }
    }
}
