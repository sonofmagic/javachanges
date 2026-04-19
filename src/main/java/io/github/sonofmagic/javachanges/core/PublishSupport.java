package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.*;

final class PublishSupport {
    private final Path repoRoot;
    private final PrintStream out;
    private final VersionSupport versionSupport;
    private final ReleaseNotesGenerator releaseNotesGenerator;

    PublishSupport(Path repoRoot, PrintStream out) {
        this.repoRoot = repoRoot;
        this.out = out;
        this.versionSupport = new VersionSupport(repoRoot);
        this.releaseNotesGenerator = new ReleaseNotesGenerator(repoRoot);
    }

    void preflight(PublishRequest request) throws IOException, InterruptedException {
        if (request.module != null) {
            assertKnownModule(repoRoot, request.module);
        }

        if (!request.allowDirty && hasDirtyWorktree()) {
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
            if (gitRefExists(request.tag)) {
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
        Path localMavenRepo = ensureLocalMavenRepositoryDirectory();
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
            if (gitRefExists(request.tag)) {
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

    private boolean hasDirtyWorktree() throws IOException, InterruptedException {
        return !gitTextAllowEmpty(repoRoot, "status", "--short").trim().isEmpty();
    }

    private boolean gitRefExists(String ref) throws IOException, InterruptedException {
        List<String> command = Arrays.asList("git", "rev-parse", ref);
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(repoRoot.toFile());
        Process process = builder.start();
        readAllBytes(process.getInputStream());
        readAllBytes(process.getErrorStream());
        return process.waitFor() == 0;
    }

    private Path ensureLocalMavenRepositoryDirectory() throws IOException {
        Path defaultLocalRepo = repoRoot.resolve(".m2/repository").normalize();
        Files.createDirectories(defaultLocalRepo);

        String mavenOpts = trimToNull(System.getenv("MAVEN_OPTS"));
        if (mavenOpts == null) {
            return defaultLocalRepo;
        }

        Matcher matcher = Pattern.compile("(?:^|\\s)-Dmaven\\.repo\\.local=([^\\s]+)").matcher(mavenOpts);
        if (!matcher.find()) {
            return defaultLocalRepo;
        }

        String configuredPath = matcher.group(1);
        if ((configuredPath.startsWith("\"") && configuredPath.endsWith("\""))
            || (configuredPath.startsWith("'") && configuredPath.endsWith("'"))) {
            configuredPath = configuredPath.substring(1, configuredPath.length() - 1);
        }

        Path localRepoPath = Paths.get(configuredPath);
        if (!localRepoPath.isAbsolute()) {
            localRepoPath = repoRoot.resolve(localRepoPath).normalize();
        }
        Files.createDirectories(localRepoPath);
        return localRepoPath;
    }
}

final class MavenSettingsWriter {
    private MavenSettingsWriter() {
    }

    static void write(Path outputPath) throws IOException {
        String releaseUsername = firstNonBlank(
            System.getenv("MAVEN_RELEASE_REPOSITORY_USERNAME"),
            System.getenv("MAVEN_REPOSITORY_USERNAME")
        );
        String releasePassword = firstNonBlank(
            System.getenv("MAVEN_RELEASE_REPOSITORY_PASSWORD"),
            System.getenv("MAVEN_REPOSITORY_PASSWORD")
        );
        String snapshotUsername = firstNonBlank(
            System.getenv("MAVEN_SNAPSHOT_REPOSITORY_USERNAME"),
            System.getenv("MAVEN_REPOSITORY_USERNAME")
        );
        String snapshotPassword = firstNonBlank(
            System.getenv("MAVEN_SNAPSHOT_REPOSITORY_PASSWORD"),
            System.getenv("MAVEN_REPOSITORY_PASSWORD")
        );

        if (releaseUsername == null || releasePassword == null) {
            throw new IllegalStateException("缺少 release 仓库认证信息，请设置 MAVEN_RELEASE_REPOSITORY_USERNAME/MAVEN_RELEASE_REPOSITORY_PASSWORD 或通用 MAVEN_REPOSITORY_USERNAME/MAVEN_REPOSITORY_PASSWORD");
        }
        if (snapshotUsername == null || snapshotPassword == null) {
            throw new IllegalStateException("缺少 snapshot 仓库认证信息，请设置 MAVEN_SNAPSHOT_REPOSITORY_USERNAME/MAVEN_SNAPSHOT_REPOSITORY_PASSWORD 或通用 MAVEN_REPOSITORY_USERNAME/MAVEN_REPOSITORY_PASSWORD");
        }

        Path parent = outputPath.toAbsolutePath().normalize().getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        String xml = "<settings xmlns=\"http://maven.apache.org/SETTINGS/1.0.0\"\n"
            + "          xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "          xsi:schemaLocation=\"http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd\">\n"
            + "  <servers>\n"
            + "    <server>\n"
            + "      <id>" + xmlEscape(releaseServerId()) + "</id>\n"
            + "      <username>" + xmlEscape(releaseUsername) + "</username>\n"
            + "      <password>" + xmlEscape(releasePassword) + "</password>\n"
            + "    </server>\n"
            + "    <server>\n"
            + "      <id>" + xmlEscape(snapshotServerId()) + "</id>\n"
            + "      <username>" + xmlEscape(snapshotUsername) + "</username>\n"
            + "      <password>" + xmlEscape(snapshotPassword) + "</password>\n"
            + "    </server>\n"
            + "  </servers>\n"
            + "</settings>\n";
        Files.write(outputPath, Collections.singletonList(xml), StandardCharsets.UTF_8);
    }

    static String releaseServerId() {
        String id = trimToNull(System.getenv("MAVEN_RELEASE_REPOSITORY_ID"));
        return id == null ? "maven-releases" : id;
    }

    static String snapshotServerId() {
        String id = trimToNull(System.getenv("MAVEN_SNAPSHOT_REPOSITORY_ID"));
        return id == null ? "maven-snapshots" : id;
    }
}

final class VersionSupport {
    private final Path repoRoot;

    VersionSupport(Path repoRoot) {
        this.repoRoot = repoRoot;
    }

    String readRevision() throws IOException {
        String content = new String(Files.readAllBytes(repoRoot.resolve("pom.xml")), StandardCharsets.UTF_8);
        Pattern pattern = Pattern.compile("<revision>([^<]+)</revision>");
        Matcher matcher = pattern.matcher(content);
        if (!matcher.find()) {
            throw new IllegalStateException("未在 pom.xml 中找到 <revision> 配置");
        }
        return matcher.group(1).trim();
    }

    void assertSnapshot() throws IOException {
        String version = readRevision();
        if (!version.endsWith("-SNAPSHOT")) {
            throw new IllegalStateException("当前项目版本不是 SNAPSHOT: " + version);
        }
    }

    void assertReleaseTag(String tag) throws IOException {
        String version = readRevision();
        String releaseVersion = releaseVersionFromTag(tag);
        String module = releaseModuleFromTag(tag);
        String baseVersion = stripSnapshot(version);
        if (module != null) {
            assertKnownModule(repoRoot, module);
        }
        if (!baseVersion.equals(releaseVersion)) {
            throw new IllegalStateException("tag " + tag + " 与项目版本 " + version + " 不匹配，期望基础版本为 " + releaseVersion);
        }
    }
}
