package io.github.sonofmagic.javachanges;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaChangesCliTest {

    @Test
    void helpOutputListsCommands() {
        ExecutionResult result = execute("--help");

        assertEquals(0, result.exitCode);
        assertTrue(result.stdout.contains("Usage: javachanges"));
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
        assertTrue(content.contains("release: minor"));
        assertTrue(content.contains("summary: add picocli command parsing"));
        assertFalse(content.contains("type:"));
    }

    @Test
    void statusHidesOtherTypeLabel(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, true);
        writeChangeset(repoRoot,
            "hide-other-label.md",
            "---\n" +
                "release: patch\n" +
                "type: other\n" +
                "summary: hide the implicit other label\n" +
                "---\n" +
                "\n" +
                "Body.\n");

        ExecutionResult result = execute("status", "--directory", repoRoot.toString());

        assertEquals(0, result.exitCode);
        assertTrue(result.stdout.contains("[patch] hide the implicit other label"));
        assertFalse(result.stdout.contains("other:"));
    }

    @Test
    void planApplyUsesReleaseHeadings(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, true);
        writeChangeset(repoRoot,
            "minor-release.md",
            "---\n" +
                "release: minor\n" +
                "type: ci\n" +
                "summary: automate self release\n" +
                "---\n" +
                "\n" +
                "Body.\n");

        ExecutionResult result = execute("plan", "--directory", repoRoot.toString(), "--apply", "true");

        assertEquals(0, result.exitCode);
        assertTrue(result.stdout.contains("Applied release plan for v1.2.0"));
        assertTrue(read(repoRoot.resolve("CHANGELOG.md")).contains("### Minor Changes"));
        assertFalse(read(repoRoot.resolve("CHANGELOG.md")).contains("### Other"));
        assertTrue(read(repoRoot.resolve("pom.xml")).contains("<revision>1.2.0-SNAPSHOT</revision>"));
        assertTrue(Files.exists(repoRoot.resolve(".changesets").resolve("release-plan.json")));
        assertTrue(Files.exists(repoRoot.resolve(".changesets").resolve("release-plan.md")));
        assertFalse(Files.exists(repoRoot.resolve(".changesets").resolve("minor-release.md")));
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

    private static void writeChangeset(Path repoRoot, String fileName, String content) throws IOException {
        Path changesetsDir = repoRoot.resolve(".changesets");
        Files.createDirectories(changesetsDir);
        Files.write(changesetsDir.resolve(fileName), content.getBytes(StandardCharsets.UTF_8));
    }

    private static List<Path> listChangesetFiles(Path changesetsDir) throws IOException {
        try (java.util.stream.Stream<Path> stream = Files.list(changesetsDir)) {
            return stream
                .filter(path -> path.getFileName().toString().endsWith(".md"))
                .filter(path -> !"README.md".equals(path.getFileName().toString()))
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
