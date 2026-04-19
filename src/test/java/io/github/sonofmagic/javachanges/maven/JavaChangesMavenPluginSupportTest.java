package io.github.sonofmagic.javachanges.maven;

import io.github.sonofmagic.javachanges.core.JavaChangesCli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JavaChangesMavenPluginSupportTest {

    @Test
    void rawArgsSupportQuotedValuesAndDirectoryInjection() {
        String[] args = JavaChangesMavenPluginSupport.resolveCliArgs(
            "/tmp/repo",
            null,
            null,
            "add --summary \"release notes\" --release minor"
        );

        assertArrayEquals(new String[]{
            "--directory", "/tmp/repo",
            "add", "--summary", "release notes", "--release", "minor"
        }, args);
    }

    @Test
    void explicitDirectoryIsNotDuplicated() {
        String[] args = JavaChangesMavenPluginSupport.resolveCliArgs(
            "/tmp/repo",
            null,
            null,
            "status --directory /custom/repo"
        );

        assertArrayEquals(new String[]{
            "status", "--directory", "/custom/repo"
        }, args);
    }

    @Test
    void structuredConfigurationDefaultsToStatus() {
        String[] args = JavaChangesMavenPluginSupport.resolveStructuredCliArgs(
            "/tmp/repo",
            null
        );

        assertArrayEquals(new String[]{
            "--directory", "/tmp/repo", "status"
        }, args);
    }

    @Test
    void structuredConfigurationAppendsArguments() {
        String[] args = JavaChangesMavenPluginSupport.resolveStructuredCliArgs(
            "/tmp/repo",
            "plan",
            "--apply", "true"
        );

        assertArrayEquals(new String[]{
            "--directory", "/tmp/repo", "plan", "--apply", "true"
        }, args);
    }

    @Test
    void tokenizeRejectsUnterminatedQuotes() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> JavaChangesMavenPluginSupport.tokenize("add --summary \"broken")
        );

        assertEquals("Unterminated quoted argument in javachanges.args", exception.getMessage());
    }

    @Test
    void rawArgsRespectExplicitDirectory() {
        String[] args = JavaChangesMavenPluginSupport.resolveRawCliArgs(
            "/tmp/repo",
            "status --directory /custom/repo"
        );

        assertArrayEquals(new String[]{
            "status", "--directory", "/custom/repo"
        }, args);
    }

    @Test
    void structuredArgsIgnoreBlankArguments() {
        String[] args = JavaChangesMavenPluginSupport.resolveStructuredCliArgs(
            "/tmp/repo",
            "add",
            "--summary",
            "release notes",
            "   ",
            null,
            "--release",
            "minor"
        );

        assertArrayEquals(new String[]{
            "--directory", "/tmp/repo",
            "add", "--summary", "release notes", "--release", "minor"
        }, args);
    }

    @Test
    void resolvedPluginArgsCanDriveCliStatus(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot);
        Files.write(
            repoRoot.resolve("pom.xml"),
            Arrays.asList(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"",
                "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"",
                "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd\">",
                "  <modelVersion>4.0.0</modelVersion>",
                "  <groupId>example</groupId>",
                "  <artifactId>fixture-app</artifactId>",
                "  <version>${revision}</version>",
                "  <properties>",
                "    <revision>1.1.1-SNAPSHOT</revision>",
                "  </properties>",
                "</project>"
            ),
            StandardCharsets.UTF_8
        );
        Files.createDirectories(repoRoot.resolve(".changesets"));
        Files.write(
            repoRoot.resolve(".changesets").resolve("change.md"),
            Arrays.asList(
                "---",
                "\"fixture-app\": patch",
                "---",
                "",
                "plugin invocation works"
            ),
            StandardCharsets.UTF_8
        );
        run(repoRoot, "git", "init", "-q");

        List<String> cliArgs = Arrays.asList(JavaChangesMavenPluginSupport.resolveCliArgs(
            repoRoot.toString(),
            "status",
            null,
            null
        ));
        ExecutionResult result = execute(cliArgs.toArray(new String[0]));

        assertEquals(0, result.exitCode);
        org.junit.jupiter.api.Assertions.assertTrue(result.stdout.contains("plugin invocation works"));
    }

    private static void run(Path workingDirectory, String... command) throws Exception {
        Process process = new ProcessBuilder(command)
            .directory(workingDirectory.toFile())
            .start();
        int exitCode = process.waitFor();
        assertEquals(0, exitCode, read(process.getErrorStream()));
    }

    private static String read(java.io.InputStream inputStream) throws IOException {
        byte[] bytes = new byte[8192];
        StringBuilder content = new StringBuilder();
        int count;
        while ((count = inputStream.read(bytes)) != -1) {
            content.append(new String(bytes, 0, count, StandardCharsets.UTF_8));
        }
        return content.toString();
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
