package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.*;

final class PublishSupport {
    private final Path repoRoot;
    private final PrintStream out;
    private final PublishRuntime runtime;
    private final VersionSupport versionSupport;
    private final ReleaseNotesGenerator releaseNotesGenerator;

    PublishSupport(Path repoRoot, PrintStream out) {
        this.repoRoot = repoRoot;
        this.out = out;
        this.runtime = new PublishRuntime(repoRoot);
        this.versionSupport = new VersionSupport(repoRoot);
        this.releaseNotesGenerator = new ReleaseNotesGenerator(repoRoot);
    }

    void preflight(PublishRequest request) throws IOException, InterruptedException {
        if (request.module != null) {
            assertKnownModule(repoRoot, request.module);
        }

        if (!request.allowDirty && runtime.hasDirtyWorktree()) {
            throw new IllegalStateException("工作区存在未提交修改。若这是预期行为，可使用 --allow-dirty true 跳过检查。");
        }

        out.println("== 版本检查 ==");
        String currentVersion = versionSupport.readRevision();
        out.println("当前 revision: " + currentVersion);
        out.println();
        out.println("== 发布模式检查 ==");

        String releaseVersion = null;
        String resolvedModule = request.module;
        if (request.snapshot) {
            versionSupport.assertSnapshot();
            out.println("snapshot 校验通过");
        } else {
            versionSupport.assertReleaseTag(request.tag);
            releaseVersion = releaseVersionFromTag(request.tag);
            String tagModule = releaseModuleFromTag(request.tag);
            if (resolvedModule == null) {
                resolvedModule = tagModule;
            } else if (tagModule != null && !resolvedModule.equals(tagModule)) {
                throw new IllegalStateException("显式指定的模块 " + resolvedModule + " 与 tag 中的模块 " + tagModule + " 不一致");
            }
            out.println("release tag: " + request.tag);
            out.println("release version: " + releaseVersion);
        }

        out.println(resolvedModule == null ? "target module: all" : "target module: " + resolvedModule);
        out.println();
        out.println("== 仓库变量检查 ==");
        out.println("MAVEN_RELEASE_REPOSITORY_URL=" + requireEnv("MAVEN_RELEASE_REPOSITORY_URL"));
        out.println("MAVEN_SNAPSHOT_REPOSITORY_URL=" + requireEnv("MAVEN_SNAPSHOT_REPOSITORY_URL"));

        out.println();
        out.println("== 凭据检查 ==");
        MavenSettingsWriter.write(Paths.get("/tmp/javachanges-preflight-settings.xml"));
        out.println("Maven settings 生成校验通过");

        if (!request.snapshot) {
            out.println();
            out.println("== Release Notes 预检查 ==");
            if (runtime.gitRefExists(request.tag)) {
                releaseNotesGenerator.writeReleaseNotes(request.tag, Paths.get("/tmp/javachanges-release-notes.md"));
                out.println("release notes 生成校验通过");
            } else {
                out.println("本地尚未找到 tag " + request.tag + "，跳过 release notes 生成检查");
            }
        }

        out.println();
        out.println("发布前检查通过");
    }

    void publish(PublishRequest request) throws IOException, InterruptedException {
        preflight(request);

        Files.createDirectories(repoRoot.resolve(".m2"));
        Path localMavenRepo = runtime.ensureLocalMavenRepositoryDirectory();
        Files.createDirectories(repoRoot.resolve("target"));
        MavenSettingsWriter.write(repoRoot.resolve(".m2/settings.xml"));

        String releaseVersion = null;
        String resolvedModule = request.module;
        if (!request.snapshot) {
            releaseVersion = releaseVersionFromTag(request.tag);
            String tagModule = releaseModuleFromTag(request.tag);
            if (resolvedModule == null) {
                resolvedModule = tagModule;
            }
            if (runtime.gitRefExists(request.tag)) {
                releaseNotesGenerator.writeReleaseNotes(request.tag, repoRoot.resolve("target/release-notes.md"));
            }
        }

        MavenCommand mavenCommand = resolveMavenCommand(repoRoot);
        if (mavenCommand == null) {
            throw new IllegalStateException("未找到可用的 Maven 命令，期望仓库内存在 " + mavenWrapperPath() + " 或系统中可用 mvn");
        }

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
        if (!request.snapshot) {
            command.add("-Drevision=" + releaseVersion);
        }
        if (resolvedModule != null) {
            command.add("-pl");
            command.add(":" + resolvedModule);
            command.add("-am");
        }
        command.add("-Dmaven.release.repository.id=" + MavenSettingsWriter.releaseServerId());
        command.add("-Dmaven.release.repository.url=" + requireEnv("MAVEN_RELEASE_REPOSITORY_URL"));
        command.add("-Dmaven.snapshot.repository.id=" + MavenSettingsWriter.snapshotServerId());
        command.add("-Dmaven.snapshot.repository.url=" + requireEnv("MAVEN_SNAPSHOT_REPOSITORY_URL"));
        command.add("clean");
        command.add("deploy");

        out.println();
        out.println("== Dry Run 输出 ==");
        out.println("已生成 .m2/settings.xml");
        out.println("Maven 命令: " + mavenCommand.command + " (" + mavenCommand.source + ")");
        if (localMavenRepo != null) {
            out.println("本地 Maven 仓库: " + localMavenRepo);
        }
        if (!request.snapshot && Files.exists(repoRoot.resolve("target/release-notes.md"))) {
            out.println("已生成 target/release-notes.md");
        }
        out.println(resolvedModule == null ? "目标模块: all" : "目标模块: " + resolvedModule);
        out.println();
        out.println("将执行的命令:");
        out.println(renderCommand(command));

        if (!request.execute) {
            out.println();
            out.println("当前为 dry-run，未执行 Maven。传入 --execute true 才会真正发布。");
            return;
        }

        out.println();
        out.println("== 开始执行 ==");
        int exitCode = runCommand(command, repoRoot);
        if (exitCode != 0) {
            throw new IllegalStateException("Maven deploy failed with exit code " + exitCode);
        }
    }
}
