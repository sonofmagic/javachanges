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
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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
    void languagePropertyIsPrependedWhenCliArgsDoNotSpecifyLanguage() {
        String[] args = JavaChangesMavenPluginSupport.prependLanguageIfMissing(
            "zh-CN",
            new String[]{"--directory", "/tmp/repo", "status"}
        );

        assertArrayEquals(new String[]{
            "--language", "zh-CN", "--directory", "/tmp/repo", "status"
        }, args);
    }

    @Test
    void explicitLanguageOptionWinsOverMavenLanguageProperty() {
        String[] original = new String[]{"--language", "en", "--directory", "/tmp/repo", "status"};
        String[] args = JavaChangesMavenPluginSupport.prependLanguageIfMissing("zh-CN", original);

        assertArrayEquals(original, args);
    }

    @Test
    void explicitLangAliasWinsOverMavenLanguageProperty() {
        String[] original = new String[]{"--lang", "en", "--directory", "/tmp/repo", "status"};
        String[] args = JavaChangesMavenPluginSupport.prependLanguageIfMissing("zh-CN", original);

        assertArrayEquals(original, args);
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

    @Test
    void pluginDescriptorIncludesWriteSettingsGoal() throws Exception {
        String descriptor = read(Files.newInputStream(Paths.get("target/classes/META-INF/maven/plugin.xml")));

        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("<goal>write-settings</goal>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("<name>settingsMode</name>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("${javachanges.settingsMode}</settingsMode>"));
    }

    @Test
    void pluginDescriptorCoversEveryCliCommand() throws Exception {
        Set<String> cliCommands = cliCommandNames();
        Set<String> pluginGoals = descriptorGoals();

        Set<String> missingGoals = new TreeSet<String>(cliCommands);
        missingGoals.removeAll(pluginGoals);
        assertEquals(new TreeSet<String>(), missingGoals, "Missing Maven plugin goals for CLI commands");

        Set<String> pluginOnlyGoals = new TreeSet<String>(pluginGoals);
        pluginOnlyGoals.removeAll(cliCommands);
        assertEquals(new TreeSet<String>(Arrays.asList("run")), pluginOnlyGoals);
    }

    @Test
    void mavenGuidesMentionEveryPluginGoal() throws Exception {
        Set<String> pluginGoals = descriptorGoals();

        assertDocumentMentionsGoals(Paths.get("docs/maven-guide.md"), pluginGoals);
        assertDocumentMentionsGoals(Paths.get("docs/maven-guide.zh-CN.md"), pluginGoals);
    }

    @Test
    void pluginDescriptorIncludesCiTemplateGoals() throws Exception {
        String descriptor = read(Files.newInputStream(Paths.get("target/classes/META-INF/maven/plugin.xml")));

        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("<goal>init-github-actions</goal>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("<goal>init-gitlab-ci</goal>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("<goal>init-gradle-tasks</goal>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("<name>gradleTasks</name>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("<name>applyGradleTasks</name>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("${javachanges.apply}</apply>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains(".github/workflows/javachanges-release.yml"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains(".gitlab-ci.yml"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("gradle/javachanges.gradle"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("${javachanges.buildTool}</buildTool>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("${javachanges.javachangesVersion}</javachangesVersion>"));
    }

    @Test
    void pluginDescriptorIncludesGpgPublicKeyGoal() throws Exception {
        String descriptor = read(Files.newInputStream(Paths.get("target/classes/META-INF/maven/plugin.xml")));

        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("<goal>ensure-gpg-public-key</goal>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("<name>primaryKeyserver</name>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("<name>secondaryKeyserver</name>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("<name>retryDelaySeconds</name>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("${javachanges.primaryKeyserver}</primaryKeyserver>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("${javachanges.retryDelaySeconds}</retryDelaySeconds>"));
    }

    @Test
    void pluginDescriptorIncludesEnvStarterGoals() throws Exception {
        String descriptor = read(Files.newInputStream(Paths.get("target/classes/META-INF/maven/plugin.xml")));

        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("<goal>init-env</goal>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("<goal>auth-help</goal>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("${javachanges.template}</template>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("${javachanges.target}</target>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("${javachanges.platform}</platform>"));
    }

    @Test
    void pluginDescriptorIncludesEnvOperationGoals() throws Exception {
        String descriptor = read(Files.newInputStream(Paths.get("target/classes/META-INF/maven/plugin.xml")));

        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("<goal>render-vars</goal>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("<goal>doctor-local</goal>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("<goal>doctor-platform</goal>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("<goal>sync-vars</goal>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("<goal>audit-vars</goal>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("${javachanges.envFile}</envFile>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("${javachanges.githubRepo}</githubRepo>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("${javachanges.gitlabRepo}</gitlabRepo>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("${javachanges.showSecrets}</showSecrets>"));
    }

    @Test
    void pluginDescriptorIncludesGithubReleaseGoals() throws Exception {
        String descriptor = read(Files.newInputStream(Paths.get("target/classes/META-INF/maven/plugin.xml")));

        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("<goal>github-release-plan</goal>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("<goal>github-tag-from-plan</goal>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("<goal>github-release-publish-state</goal>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("<goal>github-release-from-plan</goal>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("${javachanges.githubRepo}</githubRepo>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("${javachanges.targetBranch}</targetBranch>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("${javachanges.writePlanFiles}</writePlanFiles>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("${javachanges.githubOutputFile}</githubOutputFile>"));
        org.junit.jupiter.api.Assertions.assertTrue(
            descriptor.contains("${javachanges.requireReleaseApplyCommit}</requireReleaseApplyCommit>")
        );
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("${javachanges.releaseNotesFile}</releaseNotesFile>"));
    }

    @Test
    void pluginDescriptorIncludesGitlabReleaseGoals() throws Exception {
        String descriptor = read(Files.newInputStream(Paths.get("target/classes/META-INF/maven/plugin.xml")));

        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("<goal>gitlab-release-plan</goal>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("<goal>gitlab-tag-from-plan</goal>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("<goal>gitlab-release</goal>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("${javachanges.projectId}</projectId>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("${javachanges.beforeSha}</beforeSha>"));
        org.junit.jupiter.api.Assertions.assertTrue(
            descriptor.contains("${javachanges.fallbackFromReleaseCommit}</fallbackFromReleaseCommit>")
        );
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("${javachanges.gitlabHost}</gitlabHost>"));
        org.junit.jupiter.api.Assertions.assertTrue(
            descriptor.contains("${javachanges.ignoreCatalogValidation}</ignoreCatalogValidation>")
        );
    }

    @Test
    void pluginDescriptorIncludesCiHelperGoals() throws Exception {
        String descriptor = read(Files.newInputStream(Paths.get("target/classes/META-INF/maven/plugin.xml")));

        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("<goal>release-version-from-tag</goal>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("<goal>release-module-from-tag</goal>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("<goal>assert-module</goal>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("<goal>assert-snapshot</goal>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("<goal>assert-release-tag</goal>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("<goal>module-selector-args</goal>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("${javachanges.tag}</tag>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("${javachanges.module}</module>"));
    }

    @Test
    void pluginDescriptorIncludesGradlePublishGoal() throws Exception {
        String descriptor = read(Files.newInputStream(Paths.get("target/classes/META-INF/maven/plugin.xml")));

        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("<goal>gradle-publish</goal>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("${javachanges.task}</task>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("${javachanges.snapshotBuildStamp}</snapshotBuildStamp>"));
        org.junit.jupiter.api.Assertions.assertTrue(descriptor.contains("${javachanges.snapshotVersionMode}</snapshotVersionMode>"));
    }

    private static void run(Path workingDirectory, String... command) throws Exception {
        Process process = new ProcessBuilder(command)
            .directory(workingDirectory.toFile())
            .start();
        int exitCode = process.waitFor();
        assertEquals(0, exitCode, read(process.getErrorStream()));
    }

    private static Set<String> cliCommandNames() throws IOException {
        final Pattern commandPattern = Pattern.compile("@Command\\(\\s*name\\s*=\\s*\"([^\"]+)\"");
        final Set<String> commands = new TreeSet<String>();
        try (Stream<Path> files = Files.walk(Paths.get("src/main/java/io/github/sonofmagic/javachanges/core"))) {
            files.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
                try {
                    Matcher matcher = commandPattern.matcher(read(Files.newInputStream(path)));
                    while (matcher.find()) {
                        String name = matcher.group(1);
                        if (!"javachanges".equals(name)) {
                            commands.add(name);
                        }
                    }
                } catch (IOException exception) {
                    throw new RuntimeException(exception);
                }
            });
        } catch (RuntimeException exception) {
            if (exception.getCause() instanceof IOException) {
                throw (IOException) exception.getCause();
            }
            throw exception;
        }
        return commands;
    }

    private static Set<String> descriptorGoals() throws IOException {
        Pattern goalPattern = Pattern.compile("<goal>([^<]+)</goal>");
        Matcher matcher = goalPattern.matcher(read(Files.newInputStream(Paths.get("target/classes/META-INF/maven/plugin.xml"))));
        Set<String> goals = new TreeSet<String>();
        while (matcher.find()) {
            goals.add(matcher.group(1));
        }
        return goals;
    }

    private static void assertDocumentMentionsGoals(Path document, Set<String> pluginGoals) throws IOException {
        String content = read(Files.newInputStream(document));
        Set<String> missingGoals = new TreeSet<String>(pluginGoals);
        for (String goal : pluginGoals) {
            if (content.contains(goal)) {
                missingGoals.remove(goal);
            }
        }
        assertEquals(new TreeSet<String>(), missingGoals, document + " is missing Maven plugin goal references");
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
