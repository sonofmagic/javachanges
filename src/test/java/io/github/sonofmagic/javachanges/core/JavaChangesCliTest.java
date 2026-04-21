package io.github.sonofmagic.javachanges.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaChangesCliTest {

    @Test
    void helpOutputListsCommands() {
        ExecutionResult result = execute("--help");

        assertEquals(0, result.exitCode);
        assertTrue(result.stdout.contains("Usage: javachanges"));
        assertTrue(result.stdout.contains("github-release-plan"));
        assertTrue(result.stdout.contains("github-release-from-plan"));
        assertTrue(result.stdout.contains("github-tag-from-plan"));
        assertTrue(result.stdout.contains("release-version-from-tag"));
        assertTrue(result.stdout.contains("gitlab-release-plan"));
    }

    @Test
    void releaseVersionFromTagWorksWithoutRepository() {
        ExecutionResult result = execute("release-version-from-tag", "--tag", "v1.2.3");

        assertEquals(0, result.exitCode);
        assertEquals("1.2.3\n", result.stdout);
        assertEquals("", result.stderr);
    }

    @Test
    void addCreatesChangesetFile(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, false);

        ExecutionResult result = execute(
            "add",
            "--directory", repoRoot.toString(),
            "--release", "minor",
            "--summary", "add picocli command parsing"
        );

        assertEquals(0, result.exitCode);
        Path changesetsDir = repoRoot.resolve(".changesets");
        assertTrue(Files.exists(changesetsDir.resolve("README.md")));
        List<Path> files = listChangesetFiles(changesetsDir);
        assertEquals(1, files.size());
        String content = read(files.get(0));
        assertTrue(content.contains("\"fixture-app\": minor"));
        assertTrue(content.contains("\nadd picocli command parsing\n"));
        assertFalse(content.contains("release:"));
        assertFalse(content.contains("summary:"));
        assertFalse(content.contains("type:"));
    }

    @Test
    void statusHidesOtherTypeLabel(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, true);
        writeChangeset(repoRoot,
            "hide-other-label.md",
            "---\n" +
                "\"fixture-app\": patch\n" +
                "---\n" +
                "\n" +
                "hide the implicit other label\n");

        ExecutionResult result = execute("status", "--directory", repoRoot.toString());

        assertEquals(0, result.exitCode);
        assertTrue(result.stdout.contains("Release plan:"));
        assertTrue(result.stdout.contains("- Affected packages: fixture-app"));
        assertTrue(result.stdout.contains("[patch] (packages: fixture-app) hide the implicit other label"));
        assertFalse(result.stdout.contains("other:"));
    }

    @Test
    void planApplyUsesReleaseHeadings(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, true);
        writeChangeset(repoRoot,
            "minor-release.md",
            "---\n" +
                "\"fixture-app\": minor\n" +
                "---\n" +
                "\n" +
                "automate self release\n");

        ExecutionResult result = execute("plan", "--directory", repoRoot.toString(), "--apply", "true");

        assertEquals(0, result.exitCode);
        assertTrue(result.stdout.contains("Applied release plan for v1.2.0"));
        assertTrue(result.stdout.contains("- Release type: minor"));
        assertTrue(result.stdout.contains("- Affected packages: fixture-app"));
        assertTrue(read(repoRoot.resolve("CHANGELOG.md")).contains("### Minor Changes"));
        assertTrue(read(repoRoot.resolve("CHANGELOG.md")).contains("(packages: fixture-app)"));
        assertFalse(read(repoRoot.resolve("CHANGELOG.md")).contains("### Other"));
        assertTrue(read(repoRoot.resolve("pom.xml")).contains("<revision>1.2.0-SNAPSHOT</revision>"));
        assertTrue(Files.exists(repoRoot.resolve(".changesets").resolve("release-plan.json")));
        assertTrue(Files.exists(repoRoot.resolve(".changesets").resolve("release-plan.md")));
        assertFalse(Files.exists(repoRoot.resolve(".changesets").resolve("minor-release.md")));
    }

    @Test
    void legacyFrontmatterStillWorks(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, true);
        writeChangeset(repoRoot,
            "legacy-format.md",
            "---\n" +
                "release: patch\n" +
                "modules: fixture-app\n" +
                "summary: keep supporting the legacy format\n" +
                "---\n" +
                "\n" +
                "Compatibility body.\n");

        ExecutionResult result = execute("status", "--directory", repoRoot.toString());

        assertEquals(0, result.exitCode);
        assertTrue(result.stdout.contains("[patch] (packages: fixture-app) keep supporting the legacy format"));
    }

    @Test
    void officialPackageMapSupportsMultipleModules(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createMonorepo(tempDir, true);
        writeChangeset(repoRoot,
            "multi-module.md",
            "---\n" +
                "\"core\": minor\n" +
                "\"cli\": patch\n" +
                "---\n" +
                "\n" +
                "improve multi-module release planning\n");

        ExecutionResult result = execute("status", "--directory", repoRoot.toString());

        assertEquals(0, result.exitCode);
        assertTrue(result.stdout.contains("- Release type: minor"));
        assertTrue(result.stdout.contains("- Affected packages: core, cli"));
        assertTrue(result.stdout.contains("[minor] (packages: core, cli) improve multi-module release planning"));
    }

    @Test
    void statusIgnoresLocalizedChangesetReadmes(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, true);
        writeChangeset(repoRoot,
            "feature-release.md",
            "---\n" +
                "\"fixture-app\": minor\n" +
                "---\n" +
                "\n" +
                "ship localized changeset docs safely\n");
        writeChangeset(repoRoot,
            "README.zh-CN.md",
            "# Changesets\n\n这个文件不应该被当成 changeset。\n");

        ExecutionResult result = execute("status", "--directory", repoRoot.toString());

        assertEquals(0, result.exitCode);
        assertTrue(result.stdout.contains("ship localized changeset docs safely"));
        assertFalse(result.stdout.contains("Invalid changeset frontmatter"));
    }

    @Test
    void githubReleasePlanDryRunPrintsPlannedPullRequest(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, false);
        writeChangeset(repoRoot,
            "minor-release.md",
            "---\n" +
                "\"fixture-app\": minor\n" +
                "---\n" +
                "\n" +
                "publish a reviewed github release plan\n");

        ExecutionResult result = execute(
            "github-release-plan",
            "--directory", repoRoot.toString(),
            "--github-repo", "owner/repo"
        );

        assertEquals(0, result.exitCode);
        assertTrue(result.stdout.contains("GitHub repo: owner/repo"));
        assertTrue(result.stdout.contains("Release branch: changeset-release/main"));
        assertTrue(result.stdout.contains("Release version: 1.2.0"));
        assertTrue(result.stdout.contains("Dry-run only."));
    }

    @Test
    void githubTagFromPlanDryRunPrintsPlannedTag(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, true);
        run(repoRoot, "git", "config", "user.name", "tester");
        run(repoRoot, "git", "config", "user.email", "tester@example.com");
        run(repoRoot, "git", "add", "pom.xml");
        run(repoRoot, "git", "commit", "-qm", "init");
        writeChangeset(repoRoot,
            "minor-release.md",
            "---\n" +
                "\"fixture-app\": minor\n" +
                "---\n" +
                "\n" +
                "tag a github release from the manifest\n");
        ExecutionResult planResult = execute("plan", "--directory", repoRoot.toString(), "--apply", "true");
        assertEquals(0, planResult.exitCode);

        ExecutionResult result = execute("github-tag-from-plan", "--directory", repoRoot.toString());

        assertEquals(0, result.exitCode);
        assertTrue(result.stdout.contains("Release tag: v1.2.0"));
        assertTrue(result.stdout.contains("Dry-run only."));
    }

    @Test
    void githubReleaseFromPlanDryRunWritesNotesAndGithubOutputs(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, true);
        Path githubOutputFile = tempDir.resolve("github-output.txt");
        run(repoRoot, "git", "config", "user.name", "tester");
        run(repoRoot, "git", "config", "user.email", "tester@example.com");
        run(repoRoot, "git", "add", "pom.xml");
        run(repoRoot, "git", "commit", "-qm", "init");
        writeChangeset(repoRoot,
            "minor-release.md",
            "---\n" +
                "\"fixture-app\": minor\n" +
                "---\n" +
                "\n" +
                "sync github releases from the release manifest\n");
        ExecutionResult planResult = execute("plan", "--directory", repoRoot.toString(), "--apply", "true");
        assertEquals(0, planResult.exitCode);
        run(repoRoot, "git", "add", "pom.xml", "CHANGELOG.md", ".changesets");
        run(repoRoot, "git", "commit", "-qm", "release plan");
        run(repoRoot, "git", "tag", "v1.2.0");

        ExecutionResult result = execute(
            "github-release-from-plan",
            "--directory", repoRoot.toString(),
            "--release-notes-file", "target/release-notes.md",
            "--github-output-file", githubOutputFile.toString()
        );

        assertEquals(0, result.exitCode);
        assertTrue(result.stdout.contains("Release version: 1.2.0"));
        assertTrue(result.stdout.contains("Release tag: v1.2.0"));
        assertTrue(result.stdout.contains("GitHub output file: " + githubOutputFile));
        assertTrue(result.stdout.contains("Dry-run only."));
        assertTrue(read(repoRoot.resolve("target").resolve("release-notes.md")).contains("## 1.2.0 - "));
        assertTrue(read(githubOutputFile).contains("release_version=1.2.0"));
        assertTrue(read(githubOutputFile).contains("release_tag=v1.2.0"));
        assertTrue(read(githubOutputFile).contains("release_notes_file="));
    }

    @Test
    void doctorLocalFallsBackToSystemMavenWhenWrapperMissing(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, false);
        Path envDir = repoRoot.resolve("env");
        Files.createDirectories(envDir);
        Files.write(envDir.resolve("release.env.local"), envFile().getBytes(StandardCharsets.UTF_8));

        ExecutionResult result = execute(
            "doctor-local",
            "--directory", repoRoot.toString(),
            "--env-file", "env/release.env.local"
        );

        assertNotEquals(0, result.exitCode);
        assertTrue(result.stdout.matches("(?s).*\\./mvnw\\s+MISSING.*"));
        assertTrue(result.stdout.matches("(?s).*Maven command\\s+mvn \\(system\\).*"));
        assertTrue(result.stdout.matches("(?s).*mvn -q -version\\s+OK.*"));
    }

    @Test
    void renderVarsJsonReturnsMachineReadablePayload(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, false);
        Path envDir = repoRoot.resolve("env");
        Files.createDirectories(envDir);
        Files.write(envDir.resolve("release.env.local"), envFile().getBytes(StandardCharsets.UTF_8));

        ExecutionResult result = execute(
            "render-vars",
            "--directory", repoRoot.toString(),
            "--env-file", "env/release.env.local",
            "--platform", "github",
            "--format", "json"
        );

        assertEquals(0, result.exitCode);
        assertTrue(result.stdout.trim().startsWith("{"));
        assertTrue(result.stdout.contains("\"ok\":true"));
        assertTrue(result.stdout.contains("\"command\":\"render-vars\""));
        assertTrue(result.stdout.contains("\"platform\":\"github\""));
        assertTrue(result.stdout.contains("\"sections\":["));
        assertFalse(result.stdout.contains("== GitHub Actions Variables =="));
        assertEquals("", result.stderr);
    }

    @Test
    void doctorLocalJsonPrintsOnlyJson(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, false);
        Path envDir = repoRoot.resolve("env");
        Files.createDirectories(envDir);
        Files.write(envDir.resolve("release.env.local"), envFile().getBytes(StandardCharsets.UTF_8));

        ExecutionResult result = execute(
            "doctor-local",
            "--directory", repoRoot.toString(),
            "--env-file", "env/release.env.local",
            "--format", "json"
        );

        assertNotEquals(0, result.exitCode);
        assertTrue(result.stdout.trim().startsWith("{"));
        assertTrue(result.stdout.contains("\"ok\":false"));
        assertTrue(result.stdout.contains("\"command\":\"doctor-local\""));
        assertTrue(result.stdout.contains("\"sections\":["));
        assertTrue(result.stdout.contains("\"error\":\"本机发布环境未就绪\""));
        assertFalse(result.stdout.contains("== 本机运行时 =="));
        assertEquals("", result.stderr);
    }

    @Test
    void auditVarsJsonPrintsOnlyJson(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, false);
        Path envDir = repoRoot.resolve("env");
        Files.createDirectories(envDir);
        Files.write(envDir.resolve("release.env.local"), envFile().getBytes(StandardCharsets.UTF_8));

        ExecutionResult result = execute(
            "audit-vars",
            "--directory", repoRoot.toString(),
            "--env-file", "env/release.env.local",
            "--platform", "github",
            "--format", "json"
        );

        assertNotEquals(0, result.exitCode);
        assertTrue(result.stdout.trim().startsWith("{"));
        assertTrue(result.stdout.contains("\"ok\":false"));
        assertTrue(result.stdout.contains("\"command\":\"audit-vars\""));
        assertTrue(result.stdout.contains("\"platform\":\"github\""));
        assertTrue(result.stdout.contains("\"sections\":["));
        assertTrue(result.stdout.contains("\"error\":\"缺少仓库参数: GITHUB_REPO\""));
        assertTrue(result.stdout.contains("\"label\":\"GITHUB_REPO\""));
        assertFalse(result.stdout.contains("== GitHub Variables 审计 =="));
        assertEquals("", result.stderr);
    }

    @Test
    void resolveMavenCommandPrefersWrapper(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, false);
        Files.write(repoRoot.resolve(ReleaseUtils.mavenWrapperPath()), "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));

        MavenCommand command = ReleaseUtils.resolveMavenCommand(repoRoot, new MavenCommandProbe() {
            @Override
            public boolean fileExists(Path path) {
                return Files.exists(path);
            }

            @Override
            public boolean commandAvailable(Path workingDirectory, String... command) {
                return true;
            }
        });

        assertEquals(ReleaseUtils.mavenWrapperPath(), command.command);
        assertEquals("wrapper", command.source);
    }

    @Test
    void resolveMavenCommandFallsBackToSystemMaven(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, false);

        MavenCommand command = ReleaseUtils.resolveMavenCommand(repoRoot, new MavenCommandProbe() {
            @Override
            public boolean fileExists(Path path) {
                return false;
            }

            @Override
            public boolean commandAvailable(Path workingDirectory, String... command) {
                return true;
            }
        });

        assertEquals("mvn", command.command);
        assertEquals("system", command.source);
    }

    @Test
    void resolveMavenCommandReturnsNullWhenUnavailable(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, false);

        MavenCommand command = ReleaseUtils.resolveMavenCommand(repoRoot, new MavenCommandProbe() {
            @Override
            public boolean fileExists(Path path) {
                return false;
            }

            @Override
            public boolean commandAvailable(Path workingDirectory, String... command) {
                return false;
            }
        });

        assertNull(command);
    }

    @Test
    void unknownCommandReturnsNonZero() {
        ExecutionResult result = execute("unknown-command");

        assertNotEquals(0, result.exitCode);
    }

    private static ExecutionResult execute(String... args) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = JavaChangesCli.execute(
            args,
            new PrintStream(stdout, true),
            new PrintStream(stderr, true)
        );
        return new ExecutionResult(
            exitCode,
            new String(stdout.toByteArray(), StandardCharsets.UTF_8),
            new String(stderr.toByteArray(), StandardCharsets.UTF_8)
        );
    }

    private static Path createRepository(Path tempDir, boolean git) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot);
        Files.write(repoRoot.resolve("pom.xml"), singleModulePom().getBytes(StandardCharsets.UTF_8));
        if (git) {
            run(repoRoot, "git", "init", "-q");
        }
        return repoRoot;
    }

    private static Path createMonorepo(Path tempDir, boolean git) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot.resolve("core"));
        Files.createDirectories(repoRoot.resolve("cli"));
        Files.write(repoRoot.resolve("pom.xml"), monorepoPom().getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve("core").resolve("pom.xml"), childModulePom("core").getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve("cli").resolve("pom.xml"), childModulePom("cli").getBytes(StandardCharsets.UTF_8));
        if (git) {
            run(repoRoot, "git", "init", "-q");
        }
        return repoRoot;
    }

    private static String singleModulePom() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n"
            + "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
            + "    <modelVersion>4.0.0</modelVersion>\n"
            + "    <groupId>example</groupId>\n"
            + "    <artifactId>fixture-app</artifactId>\n"
            + "    <version>${revision}</version>\n"
            + "    <properties>\n"
            + "        <revision>1.1.1-SNAPSHOT</revision>\n"
            + "    </properties>\n"
            + "</project>\n";
    }

    private static String monorepoPom() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n"
            + "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
            + "    <modelVersion>4.0.0</modelVersion>\n"
            + "    <groupId>example</groupId>\n"
            + "    <artifactId>parent</artifactId>\n"
            + "    <version>${revision}</version>\n"
            + "    <packaging>pom</packaging>\n"
            + "    <properties>\n"
            + "        <revision>1.1.1-SNAPSHOT</revision>\n"
            + "    </properties>\n"
            + "    <modules>\n"
            + "        <module>core</module>\n"
            + "        <module>cli</module>\n"
            + "    </modules>\n"
            + "</project>\n";
    }

    private static String childModulePom(String artifactId) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n"
            + "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
            + "    <modelVersion>4.0.0</modelVersion>\n"
            + "    <parent>\n"
            + "        <groupId>example</groupId>\n"
            + "        <artifactId>parent</artifactId>\n"
            + "        <version>${revision}</version>\n"
            + "    </parent>\n"
            + "    <artifactId>" + artifactId + "</artifactId>\n"
            + "</project>\n";
    }

    private static String envFile() {
        return "MAVEN_RELEASE_REPOSITORY_URL=https://repo.example.com/maven-releases/\n"
            + "MAVEN_SNAPSHOT_REPOSITORY_URL=https://repo.example.com/maven-snapshots/\n"
            + "MAVEN_RELEASE_REPOSITORY_ID=maven-releases\n"
            + "MAVEN_SNAPSHOT_REPOSITORY_ID=maven-snapshots\n"
            + "MAVEN_REPOSITORY_USERNAME=replace-me\n"
            + "MAVEN_REPOSITORY_PASSWORD=replace-me\n";
    }

    private static void writeChangeset(Path repoRoot, String fileName, String content) throws IOException {
        Path changesetsDir = repoRoot.resolve(".changesets");
        Files.createDirectories(changesetsDir);
        Files.write(changesetsDir.resolve(fileName), content.getBytes(StandardCharsets.UTF_8));
    }

    private static List<Path> listChangesetFiles(Path changesetsDir) throws IOException {
        try (java.util.stream.Stream<Path> stream = Files.list(changesetsDir)) {
            return stream
                .filter(path -> path.getFileName().toString().endsWith(".md"))
                .filter(path -> !path.getFileName().toString().startsWith("README"))
                .collect(java.util.stream.Collectors.toList());
        }
    }

    private static String read(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private static void run(Path workingDirectory, String... command) throws Exception {
        Process process = new ProcessBuilder(command)
            .directory(workingDirectory.toFile())
            .start();
        int exitCode = process.waitFor();
        assertEquals(0, exitCode, "command failed");
    }

    private static final class ExecutionResult {
        private final int exitCode;
        private final String stdout;
        private final String stderr;

        private ExecutionResult(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }
    }
}
