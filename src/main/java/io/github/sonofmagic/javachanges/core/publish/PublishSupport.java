package io.github.sonofmagic.javachanges.core.publish;

import io.github.sonofmagic.javachanges.core.MavenCommand;
import io.github.sonofmagic.javachanges.core.MavenSettingsWriter;
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

        Files.createDirectories(repoRoot.resolve(".m2"));
        Path localMavenRepo = runtime.ensureLocalMavenRepositoryDirectory();
        Files.createDirectories(repoRoot.resolve("target"));
        MavenSettingsWriter.write(repoRoot.resolve(".m2/settings.xml"),
            request.snapshot ? MavenSettingsWriter.RepositoryMode.SNAPSHOT : MavenSettingsWriter.RepositoryMode.RELEASE);

        if (!request.snapshot && runtime.gitRefExists(request.tag)) {
            Path releaseNotesFile = repoRoot.resolve("target/release-notes.md");
            releaseNotesGenerator.writeReleaseNotes(request.tag, releaseNotesFile);
            report.releaseNotesFile = releaseNotesFile.toString();
        }

        MavenCommand mavenCommand = ReleaseProcessUtils.resolveMavenCommand(repoRoot);
        if (mavenCommand == null) {
            throw new IllegalStateException("未找到可用的 Maven 命令，期望仓库内存在 "
                + ReleaseProcessUtils.mavenWrapperPath() + " 或系统中可用 mvn");
        }

        List<String> command = planSupport.buildDeployCommand(request, publishTarget, mavenCommand, localMavenRepo);

        if (request.format != io.github.sonofmagic.javachanges.core.OutputFormat.JSON) {
            out.println();
            out.println("== Dry Run 输出 ==");
            out.println("已生成 .m2/settings.xml");
            out.println("Maven 命令: " + mavenCommand.command + " (" + mavenCommand.source + ")");
            if (localMavenRepo != null) {
                out.println("本地 Maven 仓库: " + localMavenRepo);
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
            if (!request.snapshot && Files.exists(repoRoot.resolve("target/release-notes.md"))) {
                out.println("已生成 target/release-notes.md");
            }
            out.println(publishTarget.resolvedModule == null ? "目标模块: all" : "目标模块: " + publishTarget.resolvedModule);
            out.println();
            out.println("将执行的命令:");
            out.println(ReleaseTextUtils.renderCommand(command));
        }

        if (!request.execute) {
            report.reason = "Dry-run only.";
            if (request.format == io.github.sonofmagic.javachanges.core.OutputFormat.JSON) {
                out.println(report.toJson());
            } else {
                out.println();
                out.println("当前为 dry-run，未执行 Maven。传入 --execute true 才会真正发布。");
            }
            return;
        }

        if (request.format != io.github.sonofmagic.javachanges.core.OutputFormat.JSON) {
            out.println();
            out.println("== 开始执行 ==");
        }
        int exitCode = ReleaseProcessUtils.runCommand(command, repoRoot);
        if (exitCode != 0) {
            throw new IllegalStateException("Maven deploy failed with exit code " + exitCode);
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
            throw new IllegalStateException("工作区存在未提交修改。若这是预期行为，可使用 --allow-dirty true 跳过检查。");
        }

        if (request.format != io.github.sonofmagic.javachanges.core.OutputFormat.JSON) {
            out.println("== 版本检查 ==");
        }
        String currentVersion = versionSupport.readRevision();
        if (request.format != io.github.sonofmagic.javachanges.core.OutputFormat.JSON) {
            out.println("当前 revision: " + currentVersion);
            out.println();
            out.println("== 发布模式检查 ==");
        }

        if (request.snapshot) {
            if (request.format != io.github.sonofmagic.javachanges.core.OutputFormat.JSON) {
                out.println("snapshot 校验通过");
                out.println("snapshot version mode: " + publishTarget.snapshotVersionMode.id);
                out.println(publishTarget.snapshotVersionMode == SnapshotVersionMode.PLAIN
                    ? "plain snapshot: 项目版本号保持 pom.xml 中的原始 -SNAPSHOT revision"
                    : "stamped snapshot: 项目版本号会追加 build stamp 后再发布");
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
            out.println("== 仓库变量检查 ==");
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
            out.println("== 凭据检查 ==");
        }
        Path settingsFile = tempDir.resolve("settings.xml");
        MavenSettingsWriter.write(settingsFile,
            request.snapshot ? MavenSettingsWriter.RepositoryMode.SNAPSHOT : MavenSettingsWriter.RepositoryMode.RELEASE);
        ReleaseProcessUtils.restrictOwnerOnly(settingsFile);
        if (request.format != io.github.sonofmagic.javachanges.core.OutputFormat.JSON) {
            out.println("Maven settings 生成校验通过");
        }

        if (!request.snapshot) {
            if (request.format != io.github.sonofmagic.javachanges.core.OutputFormat.JSON) {
                out.println();
                out.println("== Release Notes 预检查 ==");
            }
            if (runtime.gitRefExists(request.tag)) {
                Path releaseNotesFile = tempDir.resolve("release-notes.md");
                releaseNotesGenerator.writeReleaseNotes(request.tag, releaseNotesFile);
                if (request.format != io.github.sonofmagic.javachanges.core.OutputFormat.JSON) {
                    out.println("release notes 生成校验通过");
                }
            } else if (request.format != io.github.sonofmagic.javachanges.core.OutputFormat.JSON) {
                out.println("本地尚未找到 tag " + request.tag + "，跳过 release notes 生成检查");
            }
        }

        report.reason = "Preflight checks passed.";
        if (request.format != io.github.sonofmagic.javachanges.core.OutputFormat.JSON) {
            out.println();
            out.println("发布前检查通过");
        }
    }
}
