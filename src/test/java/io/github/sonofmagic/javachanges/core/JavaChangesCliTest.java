package io.github.sonofmagic.javachanges.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaChangesCliTest {
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    @Test
    void helpOutputListsCommands() {
        ExecutionResult result = execute("--help");

        assertEquals(0, result.exitCode);
        assertTrue(result.stdout.contains("Usage: javachanges"));
        assertTrue(result.stdout.contains("init"));
        assertTrue(result.stdout.contains("setup"));
        assertTrue(result.stdout.contains("next"));
        assertTrue(result.stdout.contains("validate"));
        assertTrue(result.stdout.contains("modules"));
        assertTrue(result.stdout.contains("github-release-plan"));
        assertTrue(result.stdout.contains("github-release-from-plan"));
        assertTrue(result.stdout.contains("github-tag-from-plan"));
        assertTrue(result.stdout.contains("init-github-actions"));
        assertTrue(result.stdout.contains("release-version-from-tag"));
        assertTrue(result.stdout.contains("gitlab-release-plan"));
        assertTrue(result.stdout.contains("gitlab-release"));
        assertTrue(result.stdout.contains("init-gitlab-ci"));
        assertTrue(result.stdout.contains("doctor-publish"));
        assertTrue(result.stdout.contains("gradle-publish"));
        assertTrue(result.stdout.contains("init-gradle-tasks"));
    }

    @Test
    void releaseVersionFromTagWorksWithoutRepository() {
        ExecutionResult result = execute("release-version-from-tag", "--tag", "v1.2.3");

        assertEquals(0, result.exitCode);
        assertEquals("1.2.3\n", result.stdout);
        assertEquals("", result.stderr);
    }

    @Test
    void releaseVersionFromTagJsonReportsTagMetadata() {
        ExecutionResult result = execute("release-version-from-tag", "--tag", "core/v1.2.3", "--format", "json");

        assertEquals(0, result.exitCode);
        JsonNode root = ReleaseJsonUtils.readTree(result.stdout);
        assertTrue(root.get("ok").asBoolean());
        assertEquals("release-version-from-tag", root.get("command").asText());
        assertEquals("core/v1.2.3", root.get("tag").asText());
        assertEquals("1.2.3", root.get("releaseVersion").asText());
        assertEquals("core", root.get("releaseModule").asText());
        assertEquals("", result.stderr);
    }

    @Test
    void releaseModuleFromTagJsonReportsWholeRepoTag() {
        ExecutionResult result = execute("release-module-from-tag", "--tag", "v1.2.3", "--format", "json");

        assertEquals(0, result.exitCode);
        JsonNode root = ReleaseJsonUtils.readTree(result.stdout);
        assertTrue(root.get("ok").asBoolean());
        assertEquals("release-module-from-tag", root.get("command").asText());
        assertEquals("v1.2.3", root.get("tag").asText());
        assertEquals("1.2.3", root.get("releaseVersion").asText());
        assertTrue(root.get("releaseModule").isNull());
        assertEquals("", result.stderr);
    }

    @Test
    void releaseTagJsonReportsInvalidTag() {
        ExecutionResult result = execute("release-version-from-tag", "--tag", "not-a-tag", "--format", "json");

        assertNotEquals(0, result.exitCode);
        assertEquals("", result.stderr);
        JsonNode root = ReleaseJsonUtils.readTree(result.stdout);
        assertFalse(root.get("ok").asBoolean());
        assertEquals("release-version-from-tag", root.get("command").asText());
        assertEquals("Unsupported release tag: not-a-tag", root.get("reason").asText());
    }

    @Test
    void initCreatesChangesetReadmeAndPrintsNextSteps(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, false);

        ExecutionResult result = execute("init", "--directory", repoRoot.toString());

        assertEquals(0, result.exitCode);
        Path readmePath = repoRoot.resolve(".changesets").resolve("README.md");
        assertTrue(Files.exists(readmePath));
        String readme = read(readmePath);
        assertTrue(readme.contains("This directory stores pending release notes."));
        assertTrue(readme.contains("javachanges add --directory . --summary \"describe the change\" --release patch"));
        assertTrue(readme.contains("\"core\": minor"));
        assertTrue(readme.contains("javachanges plan --directory . --apply true"));
        assertFalse(Files.exists(repoRoot.resolve(".changesets").resolve("config.jsonc")));
        assertTrue(result.stdout.contains("Initialized javachanges in " + repoRoot));
        assertTrue(result.stdout.contains("Created: .changesets/README.md"));
        assertTrue(result.stdout.contains("use --config to write the default template"));
        assertTrue(result.stdout.contains("javachanges modules --directory " + repoRoot));
        assertTrue(result.stdout.contains("javachanges add --directory " + repoRoot));
        assertTrue(result.stdout.contains("javachanges next --directory " + repoRoot));
    }

    @Test
    void languageOptionLocalizesGeneratedReadmeAndCliText(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, false);

        ExecutionResult result = execute("--language", "zh-CN", "init", "--directory", repoRoot.toString());

        assertEquals(0, result.exitCode);
        String readme = read(repoRoot.resolve(".changesets").resolve("README.md"));
        assertTrue(readme.contains("这个目录保存待发布的 release notes。"));
        assertTrue(readme.contains("javachanges add --directory . --summary \"描述这次变更\" --release patch"));
        assertTrue(result.stdout.contains("已在 " + repoRoot + " 初始化 javachanges"));
        assertTrue(result.stdout.contains("已创建: .changesets/README.md"));
        assertTrue(result.stdout.contains("下一步:"));
    }

    @Test
    void initWithConfigWritesDefaultConfigTemplate(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, false);

        ExecutionResult result = execute("init", "--directory", repoRoot.toString(), "--config");

        assertEquals(0, result.exitCode);
        Path configPath = repoRoot.resolve(".changesets").resolve("config.jsonc");
        assertTrue(Files.exists(configPath));
        String config = read(configPath);
        assertTrue(config.contains("\"baseBranch\": \"main\""));
        assertTrue(config.contains("\"releaseBranch\": \"changeset-release/main\""));
        assertTrue(config.contains("\"snapshotBranch\": \"snapshot\""));
        assertTrue(config.contains("\"snapshotVersionMode\": \"stamped\""));
        assertTrue(config.contains("\"tagStrategy\": \"whole-repo\""));
        assertTrue(result.stdout.contains("Created: .changesets/config.jsonc"));
    }

    @Test
    void initKeepsExistingConfigUnlessForced(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, false);
        Path changesetsDir = repoRoot.resolve(".changesets");
        Files.createDirectories(changesetsDir);
        Path configPath = changesetsDir.resolve("config.jsonc");
        Files.write(configPath, "{ \"baseBranch\": \"develop\" }\n".getBytes(StandardCharsets.UTF_8));

        ExecutionResult keptResult = execute("init", "--directory", repoRoot.toString(), "--config");

        assertEquals(0, keptResult.exitCode);
        assertEquals("{ \"baseBranch\": \"develop\" }\n", read(configPath));
        assertTrue(keptResult.stdout.contains("Kept: .changesets/config.jsonc"));
        assertTrue(keptResult.stdout.contains("Use --force to replace it"));

        ExecutionResult forcedResult = execute("init", "--directory", repoRoot.toString(), "--config", "--force");

        assertEquals(0, forcedResult.exitCode);
        assertTrue(read(configPath).contains("\"baseBranch\": \"main\""));
        assertTrue(forcedResult.stdout.contains("Replaced: .changesets/config.jsonc"));
    }

    @Test
    void initKeepsExistingReadmeUnlessForced(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, false);
        Path changesetsDir = repoRoot.resolve(".changesets");
        Files.createDirectories(changesetsDir);
        Path readmePath = changesetsDir.resolve("README.md");
        Files.write(readmePath, "# Custom changeset policy\n".getBytes(StandardCharsets.UTF_8));

        ExecutionResult keptResult = execute("init", "--directory", repoRoot.toString());

        assertEquals(0, keptResult.exitCode);
        assertEquals("# Custom changeset policy\n", read(readmePath));
        assertTrue(keptResult.stdout.contains("Kept: .changesets/README.md"));

        ExecutionResult forcedResult = execute("init", "--directory", repoRoot.toString(), "--force");

        assertEquals(0, forcedResult.exitCode);
        assertTrue(read(readmePath).contains("This directory stores pending release notes."));
        assertTrue(read(readmePath).contains("javachanges modules --directory ."));
        assertTrue(forcedResult.stdout.contains("Replaced: .changesets/README.md"));
    }

    @Test
    void initUsesExistingJsonConfigPath(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, false);
        Path changesetsDir = repoRoot.resolve(".changesets");
        Files.createDirectories(changesetsDir);
        Path jsonConfigPath = changesetsDir.resolve("config.json");
        Files.write(jsonConfigPath, "{ \"baseBranch\": \"develop\" }\n".getBytes(StandardCharsets.UTF_8));

        ExecutionResult result = execute("init", "--directory", repoRoot.toString(), "--config", "--force");

        assertEquals(0, result.exitCode);
        assertTrue(read(jsonConfigPath).contains("\"baseBranch\": \"main\""));
        assertFalse(read(jsonConfigPath).contains("//"));
        assertFalse(Files.exists(changesetsDir.resolve("config.jsonc")));
        assertTrue(result.stdout.contains("Replaced: .changesets/config.json"));
    }

    @Test
    void setupCreatesMinimalReleaseWorkflowFiles(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, false);

        ExecutionResult result = execute("setup", "--directory", repoRoot.toString());

        assertEquals(0, result.exitCode);
        assertTrue(Files.exists(repoRoot.resolve(".changesets").resolve("README.md")));
        assertTrue(Files.exists(repoRoot.resolve(".changesets").resolve("config.jsonc")));
        assertFalse(Files.exists(repoRoot.resolve("env").resolve("release.env.local")));
        assertFalse(Files.exists(repoRoot.resolve(".github").resolve("workflows").resolve("javachanges-release.yml")));
        assertFalse(Files.exists(repoRoot.resolve(".gitlab-ci.yml")));
        assertTrue(result.stdout.contains("Setting up javachanges in " + repoRoot));
        assertTrue(result.stdout.contains("Created: .changesets/README.md"));
        assertTrue(result.stdout.contains("Created: .changesets/config.jsonc"));
        assertTrue(result.stdout.contains("Build tool: maven"));
        assertTrue(result.stdout.contains("Modules: fixture-app"));
        assertTrue(result.stdout.contains("Setup completed."));
        assertTrue(result.stdout.contains("javachanges validate --directory " + repoRoot));
        assertTrue(result.stdout.contains("javachanges init-github-actions --directory " + repoRoot));
        assertTrue(result.stdout.contains("javachanges init-gitlab-ci --directory " + repoRoot));
    }

    @Test
    void setupKeepsExistingGeneratedFilesUnlessForced(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, false);
        Path changesetsDir = repoRoot.resolve(".changesets");
        Files.createDirectories(changesetsDir);
        Path config = changesetsDir.resolve("config.jsonc");
        Files.write(config, "{ \"baseBranch\": \"develop\" }\n".getBytes(StandardCharsets.UTF_8));

        ExecutionResult result = execute("setup", "--directory", repoRoot.toString());

        assertEquals(0, result.exitCode);
        assertEquals("{ \"baseBranch\": \"develop\" }\n", read(config));
        assertTrue(result.stdout.contains("Kept: .changesets/config.jsonc"));
    }

    @Test
    void setupCanGenerateOptionalEnvAndCiTemplates(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, false);

        ExecutionResult result = execute(
            "setup",
            "--directory", repoRoot.toString(),
            "--env", "true",
            "--github-actions", "true",
            "--gitlab-ci", "true",
            "--javachanges-version", "1.2.3"
        );

        assertEquals(0, result.exitCode);
        assertTrue(Files.exists(repoRoot.resolve("env").resolve("release.env.example")));
        assertTrue(Files.exists(repoRoot.resolve("env").resolve("release.env.local")));
        assertTrue(Files.exists(repoRoot.resolve(".github").resolve("workflows").resolve("javachanges-release.yml")));
        assertTrue(Files.exists(repoRoot.resolve(".gitlab-ci.yml")));
        assertTrue(read(repoRoot.resolve(".github").resolve("workflows").resolve("javachanges-release.yml")).contains("JAVACHANGES_VERSION: \"1.2.3\""));
        assertTrue(read(repoRoot.resolve(".gitlab-ci.yml")).contains("JAVACHANGES_VERSION: \"1.2.3\""));
        assertTrue(result.stdout.contains("Created: env/release.env.example"));
        assertTrue(result.stdout.contains("Generated local env file: env/release.env.local"));
        assertTrue(result.stdout.contains("Generated GitHub Actions workflow: .github/workflows/javachanges-release.yml"));
        assertTrue(result.stdout.contains("Generated GitLab CI template: .gitlab-ci.yml"));
    }

    @Test
    void setupSuggestsGradleTasksForGradleRepository(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createGradleRepository(tempDir, false);

        ExecutionResult result = execute("setup", "--directory", repoRoot.toString());

        assertEquals(0, result.exitCode);
        assertFalse(Files.exists(repoRoot.resolve("gradle").resolve("javachanges.gradle")));
        assertTrue(result.stdout.contains("Build tool: gradle"));
        assertTrue(result.stdout.contains("javachanges init-gradle-tasks --directory " + repoRoot));
    }

    @Test
    void setupCanGenerateGradleTasks(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createGradleRepository(tempDir, false);

        ExecutionResult result = execute(
            "setup",
            "--directory", repoRoot.toString(),
            "--gradle-tasks", "true",
            "--javachanges-version", "1.11.0"
        );

        assertEquals(0, result.exitCode);
        Path script = repoRoot.resolve("gradle").resolve("javachanges.gradle");
        assertTrue(Files.exists(script));
        assertTrue(read(script).contains("orElse('1.11.0')"));
        assertTrue(result.stdout.contains("Generated Gradle javachanges tasks: gradle/javachanges.gradle"));
        assertFalse(result.stdout.contains("javachanges init-gradle-tasks --directory " + repoRoot));
    }

    @Test
    void setupCanApplyGeneratedGradleTasks(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createGradleRepository(tempDir, false);

        ExecutionResult result = execute(
            "setup",
            "--directory", repoRoot.toString(),
            "--apply-gradle-tasks", "true"
        );

        assertEquals(0, result.exitCode);
        assertTrue(Files.exists(repoRoot.resolve("gradle").resolve("javachanges.gradle")));
        assertTrue(read(repoRoot.resolve("build.gradle")).contains("apply from: \"gradle/javachanges.gradle\""));
        assertTrue(result.stdout.contains("Updated Gradle build file: build.gradle"));
        assertFalse(result.stdout.contains("javachanges init-gradle-tasks --directory " + repoRoot));
    }

    @Test
    void initGradleTasksWritesGradleScriptAndNextSteps(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createGradleRepository(tempDir, false);

        ExecutionResult result = execute(
            "init-gradle-tasks",
            "--directory", repoRoot.toString(),
            "--javachanges-version", "1.11.0"
        );

        assertEquals(0, result.exitCode);
        Path script = repoRoot.resolve("gradle").resolve("javachanges.gradle");
        assertTrue(Files.exists(script));
        String content = read(script);
        assertTrue(content.contains("io.github.sonofmagic:javachanges:${javachangesVersion.get()}"));
        assertTrue(content.contains("orElse('1.11.0')"));
        assertTrue(content.contains("javachangesStatus"));
        assertTrue(content.contains("javachangesGradlePublish"));
        assertTrue(content.contains("cliArgs.add('--directory')"));
        assertTrue(content.contains("cliArgs.add(javachangesDirectory())"));
        assertTrue(result.stdout.contains("Generated Gradle javachanges tasks: gradle/javachanges.gradle"));
        assertTrue(result.stdout.contains("apply(from = \"gradle/javachanges.gradle\")"));
        assertTrue(result.stdout.contains("./gradlew javachangesStatus"));
    }

    @Test
    void initGradleTasksCanApplyToGroovyBuildFile(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createGradleRepository(tempDir, false);

        ExecutionResult result = execute(
            "init-gradle-tasks",
            "--directory", repoRoot.toString(),
            "--apply", "true"
        );

        assertEquals(0, result.exitCode);
        String buildFile = read(repoRoot.resolve("build.gradle"));
        assertTrue(buildFile.contains("apply from: \"gradle/javachanges.gradle\""));
        assertTrue(result.stdout.contains("Updated Gradle build file: build.gradle"));
        assertFalse(result.stdout.contains("Add `apply(from = \"gradle/javachanges.gradle\")`"));

        ExecutionResult secondResult = execute(
            "init-gradle-tasks",
            "--directory", repoRoot.toString(),
            "--apply", "true",
            "--force", "true"
        );

        assertEquals(0, secondResult.exitCode);
        String appliedAgain = read(repoRoot.resolve("build.gradle"));
        assertEquals(appliedAgain.indexOf("apply from: \"gradle/javachanges.gradle\""),
            appliedAgain.lastIndexOf("apply from: \"gradle/javachanges.gradle\""));
    }

    @Test
    void initGradleTasksCanApplyToKotlinBuildFile(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createGradleRepository(tempDir, false);
        Files.delete(repoRoot.resolve("build.gradle"));
        Files.write(repoRoot.resolve("build.gradle.kts"), "plugins { `java-library` }\n".getBytes(StandardCharsets.UTF_8));

        ExecutionResult result = execute(
            "init-gradle-tasks",
            "--directory", repoRoot.toString(),
            "--apply", "true"
        );

        assertEquals(0, result.exitCode);
        assertTrue(read(repoRoot.resolve("build.gradle.kts")).contains("apply(from = \"gradle/javachanges.gradle\")"));
        assertTrue(result.stdout.contains("Updated Gradle build file: build.gradle.kts"));
    }

    @Test
    void initGradleTasksApplyRequiresRootBuildFile(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createGradleRepository(tempDir, false);
        Files.delete(repoRoot.resolve("build.gradle"));

        ExecutionResult result = execute(
            "init-gradle-tasks",
            "--directory", repoRoot.toString(),
            "--apply", "true"
        );

        assertNotEquals(0, result.exitCode);
        assertFalse(Files.exists(repoRoot.resolve("gradle").resolve("javachanges.gradle")));
        assertTrue(result.stderr.contains("Cannot find build.gradle or build.gradle.kts"));
    }

    @Test
    void initGradleTasksPreservesExistingFileUnlessForced(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createGradleRepository(tempDir, false);
        Path script = repoRoot.resolve("gradle").resolve("javachanges.gradle");
        Files.createDirectories(script.getParent());
        Files.write(script, "custom\n".getBytes(StandardCharsets.UTF_8));

        ExecutionResult keptResult = execute("init-gradle-tasks", "--directory", repoRoot.toString());

        assertNotEquals(0, keptResult.exitCode);
        assertEquals("custom\n", read(script));

        ExecutionResult forcedResult = execute(
            "init-gradle-tasks",
            "--directory", repoRoot.toString(),
            "--force"
        );

        assertEquals(0, forcedResult.exitCode);
        assertTrue(read(script).contains("javachangesStatus"));
    }

    @Test
    void initQuotesDirectoryWithSpaces(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo with space");
        Files.createDirectories(repoRoot);
        Files.write(repoRoot.resolve("pom.xml"), singleModulePom().getBytes(StandardCharsets.UTF_8));

        ExecutionResult result = execute("init", "--directory", repoRoot.toString());

        assertEquals(0, result.exitCode);
        assertTrue(result.stdout.contains("javachanges add --directory '" + repoRoot + "'"));
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
        assertTrue(read(changesetsDir.resolve("README.md")).contains("Supported release levels are `patch`, `minor`, and `major`."));
        List<Path> files = listChangesetFiles(changesetsDir);
        assertEquals(1, files.size());
        String content = read(files.get(0));
        assertTrue(content.contains("\"fixture-app\": minor"));
        assertTrue(content.contains("\nadd picocli command parsing\n"));
        assertFalse(content.contains("release:"));
        assertFalse(content.contains("summary:"));
        assertFalse(content.contains("type:"));
        assertTrue(result.stdout.contains("Created changeset: .changesets/"));
        assertTrue(result.stdout.contains("Release level: minor"));
        assertTrue(result.stdout.contains("Affected packages: fixture-app"));
        assertTrue(result.stdout.contains("Next steps:"));
        assertTrue(result.stdout.contains("javachanges status --directory " + repoRoot));
        assertTrue(result.stdout.contains("javachanges next --directory " + repoRoot));
    }

    @Test
    void addJsonReportsCreatedChangeset(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, false);

        ExecutionResult result = execute(
            "add",
            "--directory", repoRoot.toString(),
            "--release", "minor",
            "--summary", "add machine readable changeset output",
            "--format", "json",
            "--no-interactive", "true"
        );

        assertEquals(0, result.exitCode);
        JsonNode root = ReleaseJsonUtils.readTree(result.stdout);
        assertTrue(root.get("ok").asBoolean());
        assertEquals("add", root.get("command").asText());
        assertEquals(repoRoot.toString(), root.get("repository").asText());
        assertEquals("minor", root.get("releaseLevel").asText());
        assertEquals("fixture-app", root.get("affectedPackages").get(0).asText());
        assertEquals("add machine readable changeset output", root.get("summary").asText());
        assertTrue(root.get("createdChangeset").asText().startsWith(".changesets/"));
        assertEquals("javachanges status --directory " + repoRoot, root.get("nextCommands").get(0).asText());
        assertEquals("javachanges next --directory " + repoRoot, root.get("nextCommands").get(1).asText());
        assertEquals(1, listChangesetFiles(repoRoot.resolve(".changesets")).size());
    }

    @Test
    void addQuotesDirectoryWithSpacesInNextSteps(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo with space");
        Files.createDirectories(repoRoot);
        Files.write(repoRoot.resolve("pom.xml"), singleModulePom().getBytes(StandardCharsets.UTF_8));

        ExecutionResult result = execute(
            "add",
            "--directory", repoRoot.toString(),
            "--release", "patch",
            "--summary", "quote add next steps"
        );

        assertEquals(0, result.exitCode);
        assertTrue(result.stdout.contains("javachanges status --directory '" + repoRoot + "'"));
        assertTrue(result.stdout.contains("javachanges next --directory '" + repoRoot + "'"));
    }

    @Test
    void addRejectsInvalidReleaseLevelWithAllowedValues(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, false);

        ExecutionResult result = execute(
            "add",
            "--directory", repoRoot.toString(),
            "--release", "feature",
            "--summary", "reject invalid release levels"
        );

        assertNotEquals(0, result.exitCode);
        assertTrue(result.stderr.contains("Unsupported release level: feature. Use patch, minor, or major."));
    }

    @Test
    void addNoInteractiveRejectsMissingInput(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, false);

        ExecutionResult result = execute(
            "add",
            "--directory", repoRoot.toString(),
            "--no-interactive", "true",
            "--release", "patch"
        );

        assertNotEquals(0, result.exitCode);
        assertTrue(result.stderr.contains(
            "Missing changeset input in --no-interactive mode. Pass --summary and --release, or set CHANGESET_SUMMARY and CHANGESET_RELEASE."
        ));
        assertEquals(0, listChangesetFiles(repoRoot.resolve(".changesets")).size());
    }

    @Test
    void addJsonReportsMissingNoInteractiveInput(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, false);

        ExecutionResult result = execute(
            "add",
            "--directory", repoRoot.toString(),
            "--format", "json",
            "--no-interactive", "true",
            "--release", "patch"
        );

        assertNotEquals(0, result.exitCode);
        assertEquals("", result.stderr);
        JsonNode root = ReleaseJsonUtils.readTree(result.stdout);
        assertFalse(root.get("ok").asBoolean());
        assertEquals("add", root.get("command").asText());
        assertEquals(
            "Missing changeset input in --no-interactive mode. Pass --summary and --release, or set CHANGESET_SUMMARY and CHANGESET_RELEASE.",
            root.get("reason").asText()
        );
        assertEquals(0, listChangesetFiles(repoRoot.resolve(".changesets")).size());
    }

    @Test
    void nextSuggestsCreatingChangesetWhenNoneArePending(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, false);

        ExecutionResult result = execute("next", "--directory", repoRoot.toString());

        assertEquals(0, result.exitCode);
        assertTrue(result.stdout.contains("No pending changesets."));
        assertTrue(result.stdout.contains("javachanges add --directory " + repoRoot));
        assertTrue(result.stdout.contains("--summary \"describe the change\" --release patch"));
    }

    @Test
    void nextQuotesDirectoryWithSpaces(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo with space");
        Files.createDirectories(repoRoot);
        Files.write(repoRoot.resolve("pom.xml"), singleModulePom().getBytes(StandardCharsets.UTF_8));

        ExecutionResult result = execute("next", "--directory", repoRoot.toString());

        assertEquals(0, result.exitCode);
        assertTrue(result.stdout.contains("javachanges add --directory '" + repoRoot + "'"));
    }

    @Test
    void nextSuggestsReviewAndApplyWhenChangesetsArePending(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, true);
        writeChangeset(repoRoot,
            "minor-release.md",
            "---\n" +
                "\"fixture-app\": minor\n" +
                "---\n" +
                "\n" +
                "guide users to the next command\n");

        ExecutionResult result = execute("next", "--directory", repoRoot.toString());

        assertEquals(0, result.exitCode);
        assertTrue(result.stdout.contains("Pending changesets: 1"));
        assertTrue(result.stdout.contains("Planned release: v1.2.0"));
        assertTrue(result.stdout.contains("Affected packages: fixture-app"));
        assertTrue(result.stdout.contains("javachanges status --directory " + repoRoot));
        assertTrue(result.stdout.contains("javachanges plan --directory " + repoRoot + " --apply true"));
        assertTrue(result.stdout.contains("javachanges github-release-plan --directory " + repoRoot));
        assertTrue(result.stdout.contains("javachanges gitlab-release-plan --directory " + repoRoot));
    }

    @Test
    void versionJsonReportsBuildModel(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, false);

        ExecutionResult result = execute("version", "--directory", repoRoot.toString(), "--format", "json");

        assertEquals(0, result.exitCode);
        JsonNode root = ReleaseJsonUtils.readTree(result.stdout);
        assertTrue(root.get("ok").asBoolean());
        assertEquals("version", root.get("command").asText());
        assertEquals(repoRoot.toString(), root.get("repository").asText());
        assertEquals("maven", root.get("buildTool").asText());
        assertEquals("pom.xml", root.get("versionFile").asText());
        assertEquals("1.1.1-SNAPSHOT", root.get("currentRevision").asText());
        assertEquals("1.1.1", root.get("releaseVersion").asText());
        assertTrue(root.get("snapshot").asBoolean());
    }

    @Test
    void modulesListsMavenBuildModel(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createMonorepo(tempDir, false);

        ExecutionResult result = execute("modules", "--directory", repoRoot.toString());

        assertEquals(0, result.exitCode);
        assertTrue(result.stdout.contains("Build tool: maven"));
        assertTrue(result.stdout.contains("Version file: pom.xml"));
        assertTrue(result.stdout.contains("Current revision: 1.1.1-SNAPSHOT"));
        assertTrue(result.stdout.contains("  - core"));
        assertTrue(result.stdout.contains("  - cli"));
        assertTrue(result.stdout.contains("Next steps:"));
        assertTrue(result.stdout.contains("javachanges add --directory " + repoRoot
            + " --modules core --summary \"describe the change\" --release patch"));
        assertTrue(result.stdout.contains("javachanges add --directory " + repoRoot
            + " --modules all --summary \"describe the change\" --release patch"));
    }

    @Test
    void modulesJsonListsBuildModel(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createMonorepo(tempDir, false);

        ExecutionResult result = execute("modules", "--directory", repoRoot.toString(), "--format", "json");

        assertEquals(0, result.exitCode);
        JsonNode root = ReleaseJsonUtils.readTree(result.stdout);
        assertTrue(root.get("ok").asBoolean());
        assertEquals("modules", root.get("command").asText());
        assertEquals(repoRoot.toString(), root.get("repository").asText());
        assertEquals("maven", root.get("buildTool").asText());
        assertEquals("pom.xml", root.get("versionFile").asText());
        assertEquals("1.1.1-SNAPSHOT", root.get("currentRevision").asText());
        assertEquals("core", root.get("modules").get(0).asText());
        assertEquals("cli", root.get("modules").get(1).asText());
        assertEquals("javachanges add --directory " + repoRoot
            + " --modules core --summary \"describe the change\" --release patch",
            root.get("nextCommands").get(0).asText());
        assertEquals("javachanges add --directory " + repoRoot
            + " --modules all --summary \"describe the change\" --release patch",
            root.get("nextCommands").get(1).asText());
    }

    @Test
    void modulesListsGradleBuildModel(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createGradleRepository(tempDir, false);

        ExecutionResult result = execute("modules", "--directory", repoRoot.toString());

        assertEquals(0, result.exitCode);
        assertTrue(result.stdout.contains("Build tool: gradle"));
        assertTrue(result.stdout.contains("Version file: gradle.properties"));
        assertTrue(result.stdout.contains("Current revision: 1.1.1-SNAPSHOT"));
        assertTrue(result.stdout.contains("  - fixture-app"));
        assertTrue(result.stdout.contains("Next steps:"));
        assertTrue(result.stdout.contains("javachanges add --directory " + repoRoot
            + " --modules fixture-app --summary \"describe the change\" --release patch"));
        assertFalse(result.stdout.contains("--modules all"));
    }

    @Test
    void unknownModuleSuggestsModulesCommand(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createMonorepo(tempDir, false);

        ExecutionResult result = execute(
            "assert-module",
            "--directory", repoRoot.toString(),
            "--module", "missing"
        );

        assertNotEquals(0, result.exitCode);
        assertTrue(result.stderr.contains("Unknown module: missing, allowed: [core, cli]"));
        assertTrue(result.stderr.contains("javachanges modules --directory " + repoRoot));
    }

    @Test
    void statusSuggestsCreatingChangesetWhenNoneArePending(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, false);

        ExecutionResult result = execute("status", "--directory", repoRoot.toString());

        assertEquals(0, result.exitCode);
        assertTrue(result.stdout.contains("Release plan: none"));
        assertTrue(result.stdout.contains("Next steps:"));
        assertTrue(result.stdout.contains("javachanges add --directory " + repoRoot
            + " --summary \"describe the change\" --release patch"));
        assertTrue(result.stdout.contains("javachanges next --directory " + repoRoot));
    }

    @Test
    void statusJsonReportsNoPendingChangesets(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, false);

        ExecutionResult result = execute("status", "--directory", repoRoot.toString(), "--format", "json");

        assertEquals(0, result.exitCode);
        JsonNode root = ReleaseJsonUtils.readTree(result.stdout);
        assertTrue(root.get("ok").asBoolean());
        assertEquals("status", root.get("command").asText());
        JsonNode plan = root.get("plan");
        assertEquals(repoRoot.toString(), plan.get("repository").asText());
        assertFalse(plan.get("hasPendingChangesets").asBoolean());
        assertEquals(0, plan.get("pendingChangesets").asInt());
        assertTrue(plan.get("releaseVersion").isNull());
        assertEquals("1.1.1-SNAPSHOT", plan.get("nextSnapshotVersion").asText());
        assertEquals("javachanges add --directory " + repoRoot
            + " --summary \"describe the change\" --release patch", root.get("nextCommands").get(0).asText());
    }

    @Test
    void validatePassesForReleaseReadyRepository(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, true);
        writeChangeset(repoRoot,
            "minor-release.md",
            "---\n" +
                "\"fixture-app\": minor\n" +
                "---\n" +
                "\n" +
                "validate local release readiness\n");

        ExecutionResult result = execute("validate", "--directory", repoRoot.toString());

        assertEquals(0, result.exitCode);
        assertTrue(result.stdout.contains("Validation passed for " + repoRoot));
        assertTrue(result.stdout.contains("Checks:"));
        assertTrue(result.stdout.contains("- Current revision: 1.1.1-SNAPSHOT"));
        assertTrue(result.stdout.contains("- Pending changesets: 1"));
        assertTrue(result.stdout.contains("- Release version: v1.2.0"));
        assertTrue(result.stdout.contains("- Planned tags: v1.2.0"));
    }

    @Test
    void validateJsonReportsIssues(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, false);

        ExecutionResult result = execute("validate", "--directory", repoRoot.toString(), "--format", "json");

        assertNotEquals(0, result.exitCode);
        JsonNode root = ReleaseJsonUtils.readTree(result.stdout);
        assertFalse(root.get("ok").asBoolean());
        assertEquals("validate", root.get("command").asText());
        assertEquals(repoRoot.toString(), root.get("repository").asText());
        assertEquals("GIT_REPOSITORY", root.get("issues").get(0).get("code").asText());
    }

    @Test
    void validateReportsInvalidChangeset(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, true);
        writeChangeset(repoRoot, "broken.md", "not frontmatter\n");

        ExecutionResult result = execute("validate", "--directory", repoRoot.toString());

        assertNotEquals(0, result.exitCode);
        assertTrue(result.stdout.contains("Validation failed for " + repoRoot));
        assertTrue(result.stdout.contains("[CHANGESET]"));
        assertTrue(result.stdout.contains(".changesets/broken.md"));
    }

    @Test
    void validateReportsExistingPlannedTag(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, true);
        run(repoRoot, "git", "config", "user.name", "tester");
        run(repoRoot, "git", "config", "user.email", "tester@example.com");
        run(repoRoot, "git", "add", "pom.xml");
        run(repoRoot, "git", "commit", "-qm", "init");
        Files.createDirectories(repoRoot.resolve(".changesets"));
        Files.write(repoRoot.resolve(".changesets").resolve("config.jsonc"),
            ("{\n" +
                "  \"tagStrategy\": \"per-module\"\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));
        writeChangeset(repoRoot,
            "patch-release.md",
            "---\n" +
                "\"fixture-app\": patch\n" +
                "---\n" +
                "\n" +
                "catch duplicate tags before release\n");
        run(repoRoot, "git", "tag", "fixture-app/v1.1.2");

        ExecutionResult result = execute("validate", "--directory", repoRoot.toString());

        assertNotEquals(0, result.exitCode);
        assertTrue(result.stdout.contains("[RELEASE_TAG_EXISTS] Local release tag already exists: fixture-app/v1.1.2"));
    }

    @Test
    void validateCheckDirtyReportsDirtyWorktree(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, true);
        Files.write(repoRoot.resolve("untracked.txt"), "dirty\n".getBytes(StandardCharsets.UTF_8));

        ExecutionResult result = execute("validate", "--directory", repoRoot.toString(), "--check-dirty", "true");

        assertNotEquals(0, result.exitCode);
        assertTrue(result.stdout.contains("[DIRTY_WORKTREE]"));
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
        assertTrue(result.stdout.contains("Next steps:"));
        assertTrue(result.stdout.contains("javachanges plan --directory " + repoRoot + " --apply true"));
        assertTrue(result.stdout.contains("javachanges next --directory " + repoRoot));
        assertFalse(result.stdout.contains("other:"));
    }

    @Test
    void nextJsonReportsPendingReleaseCommands(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, true);
        writeChangeset(repoRoot,
            "minor-release.md",
            "---\n" +
                "\"fixture-app\": minor\n" +
                "---\n" +
                "\n" +
                "guide json automation\n");

        ExecutionResult result = execute("next", "--directory", repoRoot.toString(), "--format", "json");

        assertEquals(0, result.exitCode);
        JsonNode root = ReleaseJsonUtils.readTree(result.stdout);
        assertEquals("next", root.get("command").asText());
        assertTrue(root.get("hasPendingChangesets").asBoolean());
        assertEquals(1, root.get("pendingChangesets").asInt());
        assertEquals("1.2.0", root.get("releaseVersion").asText());
        assertEquals("fixture-app", root.get("affectedPackages").get(0).asText());
        assertEquals("javachanges status --directory " + repoRoot, root.get("nextCommands").get(0).asText());
        assertEquals("javachanges plan --directory " + repoRoot + " --apply true",
            root.get("nextCommands").get(1).asText());
    }

    @Test
    void planDryRunSuggestsApplyWhenChangesetsArePending(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, true);
        writeChangeset(repoRoot,
            "minor-release.md",
            "---\n" +
                "\"fixture-app\": minor\n" +
                "---\n" +
                "\n" +
                "preview release plan next steps\n");

        ExecutionResult result = execute("plan", "--directory", repoRoot.toString());

        assertEquals(0, result.exitCode);
        assertTrue(result.stdout.contains("- Release type: minor"));
        assertTrue(result.stdout.contains("Next steps:"));
        assertTrue(result.stdout.contains("javachanges plan --directory " + repoRoot + " --apply true"));
        assertTrue(result.stdout.contains("javachanges next --directory " + repoRoot));
        assertTrue(Files.exists(repoRoot.resolve(".changesets").resolve("minor-release.md")));
    }

    @Test
    void planJsonDryRunReportsPlanWithoutApplying(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, true);
        writeChangeset(repoRoot,
            "minor-release.md",
            "---\n" +
                "\"fixture-app\": minor\n" +
                "---\n" +
                "\n" +
                "preview json release plan\n");

        ExecutionResult result = execute("plan", "--directory", repoRoot.toString(), "--format", "json");

        assertEquals(0, result.exitCode);
        JsonNode root = ReleaseJsonUtils.readTree(result.stdout);
        assertEquals("plan", root.get("command").asText());
        assertFalse(root.get("apply").asBoolean());
        assertFalse(root.get("applied").asBoolean());
        assertEquals("dry-run", root.get("reason").asText());
        assertEquals("minor", root.get("plan").get("releaseLevel").asText());
        assertEquals("1.2.0", root.get("plan").get("releaseVersion").asText());
        assertTrue(Files.exists(repoRoot.resolve(".changesets").resolve("minor-release.md")));
    }

    @Test
    void planApplySuggestsCreatingChangesetWhenNoneArePending(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, false);

        ExecutionResult result = execute("plan", "--directory", repoRoot.toString(), "--apply", "true");

        assertEquals(0, result.exitCode);
        assertTrue(result.stdout.contains("Release plan: none"));
        assertTrue(result.stdout.contains("No pending changesets to apply."));
        assertTrue(result.stdout.contains("Next steps:"));
        assertTrue(result.stdout.contains("javachanges add --directory " + repoRoot
            + " --summary \"describe the change\" --release patch"));
        assertTrue(result.stdout.contains("javachanges next --directory " + repoRoot));
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
        assertTrue(result.stdout.contains("Recovery backup: .changesets/release-plan-backup.json"));
        assertTrue(result.stdout.contains("javachanges plan --directory " + repoRoot + " --restore true"));
        assertTrue(result.stdout.contains("Next steps:"));
        assertTrue(result.stdout.contains("git -C " + repoRoot + " status --short"));
        assertTrue(result.stdout.contains("git -C " + repoRoot + " add pom.xml CHANGELOG.md .changesets"));
        assertTrue(result.stdout.contains("git -C " + repoRoot + " commit -m 'chore(release): v1.2.0'"));
        assertTrue(result.stdout.contains("javachanges next --directory " + repoRoot));
        assertTrue(result.stdout.contains("- Release type: minor"));
        assertTrue(result.stdout.contains("- Affected packages: fixture-app"));
        assertTrue(read(repoRoot.resolve("CHANGELOG.md")).contains("### Minor Changes"));
        assertTrue(read(repoRoot.resolve("CHANGELOG.md")).contains("(packages: fixture-app)"));
        assertFalse(read(repoRoot.resolve("CHANGELOG.md")).contains("### Other"));
        assertEquals(1, countOccurrences(read(repoRoot.resolve("CHANGELOG.md")), "automate self release"));
        assertTrue(read(repoRoot.resolve("pom.xml")).contains("<revision>1.2.0-SNAPSHOT</revision>"));
        assertTrue(Files.exists(repoRoot.resolve(".changesets").resolve("release-plan.json")));
        assertTrue(Files.exists(repoRoot.resolve(".changesets").resolve("release-plan.md")));
        assertTrue(Files.exists(repoRoot.resolve(".changesets").resolve("release-plan-backup.json")));
        String releasePlanMarkdown = read(repoRoot.resolve(".changesets").resolve("release-plan.md"));
        assertTrue(releasePlanMarkdown.contains("| Field | Value |"));
        assertTrue(releasePlanMarkdown.contains("| 🏷️ Release version | `v1.2.0` |"));
        assertTrue(releasePlanMarkdown.contains("### ✨ Minor Changes"));
        assertTrue(releasePlanMarkdown.contains("- **automate self release**"));
        assertTrue(releasePlanMarkdown.contains("  - 📦 Packages: `fixture-app`"));
        assertTrue(releasePlanMarkdown.contains("## What happens next ✅"));
        assertFalse(releasePlanMarkdown.contains("  - 📝 Notes: automate self release"));
        assertFalse(Files.exists(repoRoot.resolve(".changesets").resolve("minor-release.md")));
    }

    @Test
    void planRestoreRevertsAppliedReleasePlan(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, true);
        Path changesetPath = writeChangeset(repoRoot,
            "minor-release.md",
            "---\n" +
                "\"fixture-app\": minor\n" +
                "---\n" +
                "\n" +
                "restore applied release plan\n");

        ExecutionResult applyResult = execute("plan", "--directory", repoRoot.toString(), "--apply", "true");
        assertEquals(0, applyResult.exitCode);
        assertFalse(Files.exists(changesetPath));
        assertTrue(read(repoRoot.resolve("pom.xml")).contains("<revision>1.2.0-SNAPSHOT</revision>"));
        assertTrue(Files.exists(repoRoot.resolve(".changesets").resolve("release-plan.json")));

        ExecutionResult restoreResult = execute("plan", "--directory", repoRoot.toString(), "--restore", "true");

        assertEquals(0, restoreResult.exitCode);
        assertTrue(restoreResult.stdout.contains("Restored release plan backup: .changesets/release-plan-backup.json"));
        assertTrue(read(repoRoot.resolve("pom.xml")).contains("<revision>1.1.1-SNAPSHOT</revision>"));
        assertFalse(Files.exists(repoRoot.resolve("CHANGELOG.md")));
        assertFalse(Files.exists(repoRoot.resolve(".changesets").resolve("release-plan.json")));
        assertFalse(Files.exists(repoRoot.resolve(".changesets").resolve("release-plan.md")));
        assertTrue(Files.exists(changesetPath));
        assertTrue(read(changesetPath).contains("restore applied release plan"));
    }

    @Test
    void planJsonApplyReportsAppliedPlan(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, true);
        writeChangeset(repoRoot,
            "minor-release.md",
            "---\n" +
                "\"fixture-app\": minor\n" +
                "---\n" +
                "\n" +
                "apply json release plan\n");

        ExecutionResult result = execute("plan", "--directory", repoRoot.toString(), "--apply", "true", "--format", "json");

        assertEquals(0, result.exitCode);
        JsonNode root = ReleaseJsonUtils.readTree(result.stdout);
        assertEquals("plan", root.get("command").asText());
        assertTrue(root.get("apply").asBoolean());
        assertTrue(root.get("applied").asBoolean());
        assertFalse(root.get("restored").asBoolean());
        assertTrue(root.get("reason").isNull());
        assertEquals(".changesets/release-plan-backup.json", root.get("backupFile").asText());
        assertEquals("1.2.0", root.get("plan").get("releaseVersion").asText());
        assertEquals("javachanges plan --directory " + repoRoot + " --restore true",
            root.get("nextCommands").get(0).asText());
        assertTrue(read(repoRoot.resolve("pom.xml")).contains("<revision>1.2.0-SNAPSHOT</revision>"));
        assertFalse(Files.exists(repoRoot.resolve(".changesets").resolve("minor-release.md")));
    }

    @Test
    void planJsonRestoreReportsRestoredBackup(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, true);
        writeChangeset(repoRoot,
            "minor-release.md",
            "---\n" +
                "\"fixture-app\": minor\n" +
                "---\n" +
                "\n" +
                "restore json release plan\n");
        ExecutionResult applyResult = execute("plan", "--directory", repoRoot.toString(), "--apply", "true");
        assertEquals(0, applyResult.exitCode);

        ExecutionResult result = execute("plan", "--directory", repoRoot.toString(), "--restore", "true", "--format", "json");

        assertEquals(0, result.exitCode);
        JsonNode root = ReleaseJsonUtils.readTree(result.stdout);
        assertEquals("plan", root.get("command").asText());
        assertTrue(root.get("restored").asBoolean());
        assertEquals(".changesets/release-plan-backup.json", root.get("backupFile").asText());
        assertTrue(read(repoRoot.resolve("pom.xml")).contains("<revision>1.1.1-SNAPSHOT</revision>"));
    }

    @Test
    void languageOptionLocalizesStatusAndReleasePlanMarkdown(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, true);
        writeChangeset(repoRoot,
            "minor-release.md",
            "---\n" +
                "\"fixture-app\": minor\n" +
                "---\n" +
                "\n" +
                "支持中文发布计划\n");

        ExecutionResult result = execute("--language", "zh-CN", "plan", "--directory", repoRoot.toString(), "--apply", "true");

        assertEquals(0, result.exitCode);
        assertTrue(result.stdout.contains("发布计划:"));
        assertTrue(result.stdout.contains("- 发布类型: minor"));
        assertTrue(result.stdout.contains("已应用 v1.2.0 的发布计划"));
        String releasePlanMarkdown = read(repoRoot.resolve(".changesets").resolve("release-plan.md"));
        assertTrue(releasePlanMarkdown.contains("## 发布计划 🚀"));
        assertTrue(releasePlanMarkdown.contains("| 🏷️ 发布版本 | `v1.2.0` |"));
        assertTrue(releasePlanMarkdown.contains("### ✨ 功能变更"));
        assertTrue(releasePlanMarkdown.contains("  - 📦 包: `fixture-app`"));
        assertTrue(read(repoRoot.resolve("CHANGELOG.md")).contains("### 功能变更"));
        assertTrue(read(repoRoot.resolve("CHANGELOG.md")).contains("(包: fixture-app)"));
    }

    @Test
    void planApplyUpdatesGradleProperties(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createGradleRepository(tempDir, true);
        writeChangeset(repoRoot,
            "minor-release.md",
            "---\n" +
                "\"fixture-app\": minor\n" +
                "---\n" +
                "\n" +
                "support gradle release plans\n");

        ExecutionResult result = execute("plan", "--directory", repoRoot.toString(), "--apply", "true");

        assertEquals(0, result.exitCode);
        assertTrue(result.stdout.contains("Applied release plan for v1.2.0"));
        assertTrue(result.stdout.contains("git -C " + repoRoot + " add gradle.properties CHANGELOG.md .changesets"));
        assertTrue(read(repoRoot.resolve("gradle.properties")).contains("version=1.2.0-SNAPSHOT"));
        assertTrue(read(repoRoot.resolve("CHANGELOG.md")).contains("support gradle release plans"));
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
    void githubReleasePlanFailsOnMalformedChangesetConfig(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, false);
        Files.createDirectories(repoRoot.resolve(".changesets"));
        Files.write(repoRoot.resolve(".changesets").resolve("config.json"),
            "{ invalid json\n".getBytes(StandardCharsets.UTF_8));

        ExecutionResult result = execute(
            "github-release-plan",
            "--directory", repoRoot.toString(),
            "--github-repo", "owner/repo"
        );

        assertNotEquals(0, result.exitCode);
        assertEquals("", result.stdout);
        assertTrue(result.stderr.contains("Failed to parse JSON"));
    }

    @Test
    void releaseNotesRejectsOutputOutsideRepository(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, false);

        ExecutionResult result = execute(
            "release-notes",
            "--directory", repoRoot.toString(),
            "--tag", "v1.2.3",
            "--output", "../release-notes.md"
        );

        assertNotEquals(0, result.exitCode);
        assertTrue(result.stderr.contains("--output must stay inside repository: ../release-notes.md"));
        assertFalse(Files.exists(tempDir.resolve("release-notes.md")));
    }

    @Test
    void publishDryRunDoesNotWriteMavenSettings(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, false);
        Files.write(repoRoot.resolve(ReleaseProcessUtils.mavenWrapperPath()),
            "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));

        Map<String, String> environment = new LinkedHashMap<String, String>();
        environment.put("MAVEN_SNAPSHOT_REPOSITORY_URL", "https://repo.example.com/snapshots");
        environment.put("MAVEN_SNAPSHOT_REPOSITORY_USERNAME", "tester");
        environment.put("MAVEN_SNAPSHOT_REPOSITORY_PASSWORD", "secret");

        ExecutionResult result = executeProcess(environment,
            "publish",
            "--directory", repoRoot.toString(),
            "--snapshot", "true",
            "--snapshot-build-stamp", "20260428.000000.test",
            "--allow-dirty", "true"
        );

        assertEquals(0, result.exitCode, result.stderr);
        assertTrue(result.stdout.contains("Maven settings generation check passed; execution will write .m2/settings.xml"));
        assertTrue(result.stdout.contains("-s .m2/settings.xml"));
        assertFalse(Files.exists(repoRoot.resolve(".m2/settings.xml")));
        assertFalse(Files.exists(repoRoot.resolve(".m2/repository")));
    }

    @Test
    void preflightAcceptsMavenCentralCredentialsForRelease(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, false);
        Map<String, String> environment = new LinkedHashMap<String, String>();
        environment.put("MAVEN_RELEASE_REPOSITORY_URL", "https://repo.example.com/releases");
        environment.put("MAVEN_CENTRAL_USERNAME", "central-user");
        environment.put("MAVEN_CENTRAL_PASSWORD", "central-secret");

        ExecutionResult result = executeProcess(environment,
            "preflight",
            "--directory", repoRoot.toString(),
            "--tag", "v1.1.1",
            "--allow-dirty", "true",
            "--format", "json"
        );

        assertEquals(0, result.exitCode, result.stdout + result.stderr);
        JsonNode root = ReleaseJsonUtils.readTree(result.stdout);
        assertTrue(root.get("ok").asBoolean());
        assertEquals("publish-release", root.get("action").asText());
    }

    @Test
    void writeSettingsIncludesCentralServerWhenCentralCredentialsAreSet(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("settings.xml");
        Map<String, String> environment = new LinkedHashMap<String, String>();
        environment.put("MAVEN_CENTRAL_USERNAME", "central-user");
        environment.put("MAVEN_CENTRAL_PASSWORD", "central-secret");

        ExecutionResult result = executeProcess(environment,
            "write-settings",
            "--output", output.toString()
        );

        assertEquals(0, result.exitCode, result.stdout + result.stderr);
        String settings = read(output);
        assertTrue(settings.contains("<id>central</id>"));
        assertTrue(settings.contains("<id>maven-releases</id>"));
        assertTrue(settings.contains("<id>maven-snapshots</id>"));
        assertTrue(settings.contains("<username>central-user</username>"));
    }

    @Test
    void writeSettingsDoesNotDuplicateCentralServerWhenReleaseIdIsCentral(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("settings.xml");
        Map<String, String> environment = new LinkedHashMap<String, String>();
        environment.put("MAVEN_RELEASE_REPOSITORY_ID", "central");
        environment.put("MAVEN_CENTRAL_USERNAME", "central-user");
        environment.put("MAVEN_CENTRAL_PASSWORD", "central-secret");

        ExecutionResult result = executeProcess(environment,
            "write-settings",
            "--output", output.toString()
        );

        assertEquals(0, result.exitCode, result.stdout + result.stderr);
        String settings = read(output);
        assertEquals(1, countOccurrences(settings, "<id>central</id>"));
        assertTrue(settings.contains("<id>maven-snapshots</id>"));
    }

    @Test
    void writeSettingsReleaseModeOnlyRequiresReleaseCredentials(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("settings.xml");
        Map<String, String> environment = new LinkedHashMap<String, String>();
        environment.put("MAVEN_RELEASE_REPOSITORY_USERNAME", "release-user");
        environment.put("MAVEN_RELEASE_REPOSITORY_PASSWORD", "release-secret");

        ExecutionResult result = executeProcess(environment,
            "write-settings",
            "--output", output.toString(),
            "--mode", "release"
        );

        assertEquals(0, result.exitCode, result.stdout + result.stderr);
        String settings = read(output);
        assertTrue(settings.contains("<id>maven-releases</id>"));
        assertTrue(settings.contains("<username>release-user</username>"));
        assertFalse(settings.contains("<id>maven-snapshots</id>"));
    }

    @Test
    void writeSettingsSnapshotModeOnlyRequiresSnapshotCredentials(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("settings.xml");
        Map<String, String> environment = new LinkedHashMap<String, String>();
        environment.put("MAVEN_SNAPSHOT_REPOSITORY_USERNAME", "snapshot-user");
        environment.put("MAVEN_SNAPSHOT_REPOSITORY_PASSWORD", "snapshot-secret");

        ExecutionResult result = executeProcess(environment,
            "write-settings",
            "--output", output.toString(),
            "--mode", "snapshot"
        );

        assertEquals(0, result.exitCode, result.stdout + result.stderr);
        String settings = read(output);
        assertFalse(settings.contains("<id>maven-releases</id>"));
        assertTrue(settings.contains("<id>maven-snapshots</id>"));
        assertTrue(settings.contains("<username>snapshot-user</username>"));
    }

    @Test
    void writeSettingsRejectsUnsupportedMode(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("settings.xml");

        ExecutionResult result = execute(
            "write-settings",
            "--output", output.toString(),
            "--mode", "staging"
        );

        assertNotEquals(0, result.exitCode);
        assertTrue(result.stderr.contains("Unsupported settings mode: staging. Use all, release, or snapshot."));
    }

    @Test
    void gradlePublishDryRunRendersPublishCommand(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createGradleRepository(tempDir, false);
        Files.write(repoRoot.resolve(ReleaseProcessUtils.gradleWrapperPath()),
            "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));

        ExecutionResult result = execute(
            "gradle-publish",
            "--directory", repoRoot.toString(),
            "--snapshot", "true",
            "--module", "fixture-app",
            "--snapshot-build-stamp", "20260428.000000.test",
            "--allow-dirty", "true"
        );

        assertEquals(0, result.exitCode, result.stderr);
        assertTrue(result.stdout.contains("Gradle command: " + ReleaseProcessUtils.gradleWrapperPath() + " (wrapper)"));
        assertTrue(result.stdout.contains("Gradle task: publish"));
        assertTrue(result.stdout.contains("publish version: 1.1.1-20260428.000000.test-SNAPSHOT"));
        assertTrue(result.stdout.contains("target module: fixture-app"));
        assertTrue(result.stdout.contains(ReleaseProcessUtils.gradleWrapperPath()
            + " --no-daemon :fixture-app:publish -Pversion=1.1.1-20260428.000000.test-SNAPSHOT"));
        assertTrue(result.stdout.contains("Dry-run only. Pass --execute true to run Gradle publish."));
    }

    @Test
    void gradlePublishDryRunRendersCustomTask(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createGradleRepository(tempDir, false);
        Files.write(repoRoot.resolve(ReleaseProcessUtils.gradleWrapperPath()),
            "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));

        ExecutionResult result = execute(
            "gradle-publish",
            "--directory", repoRoot.toString(),
            "--snapshot", "true",
            "--module", "fixture-app",
            "--task", "publishAllPublicationsToMavenRepository",
            "--snapshot-version-mode", "plain",
            "--allow-dirty", "true"
        );

        assertEquals(0, result.exitCode, result.stderr);
        assertTrue(result.stdout.contains("Gradle task: publishAllPublicationsToMavenRepository"));
        assertTrue(result.stdout.contains(ReleaseProcessUtils.gradleWrapperPath()
            + " --no-daemon :fixture-app:publishAllPublicationsToMavenRepository -Pversion=1.1.1-SNAPSHOT"));
    }

    @Test
    void githubTagFromPlanDryRunPrintsPlannedTag(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, true);
        Path remoteRepo = tempDir.resolve("remote.git");
        run(repoRoot, "git", "init", "-q", "--bare", remoteRepo.toString());
        run(repoRoot, "git", "remote", "add", "origin", remoteRepo.toString());
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
        assertTrue(result.stdout.contains("Release tags: [v1.2.0]"));
        assertTrue(result.stdout.contains("Dry-run only."));
    }

    @Test
    void githubReleaseFromPlanDryRunDoesNotWriteNotesOrGithubOutputs(@TempDir Path tempDir) throws Exception {
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
        assertTrue(result.stdout.contains("GitHub output file: " + githubOutputFile + " (execute only)"));
        assertTrue(result.stdout.contains("Dry-run only."));
        assertFalse(Files.exists(repoRoot.resolve("target").resolve("release-notes.md")));
        assertFalse(Files.exists(githubOutputFile));
    }

    @Test
    void initGitlabCiWritesMinimalTemplate(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, false);
        Files.createDirectories(repoRoot.resolve(".changesets"));
        Files.write(repoRoot.resolve(".changesets").resolve("config.json"), (
            "{\n" +
                "  \"baseBranch\": \"develop\",\n" +
                "  \"snapshotBranch\": \"snapshot-dev\"\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));

        ExecutionResult result = execute(
            "init-gitlab-ci",
            "--directory", repoRoot.toString(),
            "--output", ".gitlab-ci.generated.yml",
            "--javachanges-version", "1.4.1"
        );

        assertEquals(0, result.exitCode);
        String yaml = read(repoRoot.resolve(".gitlab-ci.generated.yml"));
        JsonNode root = assertYamlObject(yaml);
        assertTrue(root.has("stages"));
        assertTrue(root.has("release_plan_mr"));
        assertTrue(root.has("release_tag"));
        assertTrue(root.has("publish_release"));
        assertTrue(yaml.contains("JAVACHANGES_VERSION: \"1.4.1\""));
        assertTrue(yaml.contains("run_javachanges()"));
        assertTrue(yaml.contains("mvn --batch-mode --non-recursive \"io.github.sonofmagic:javachanges:${JAVACHANGES_VERSION}:run\""));
        assertTrue(yaml.contains("run_javachanges \"gitlab-release-plan --write-plan-files false\""));
        assertTrue(yaml.contains("run_javachanges \"gitlab-tag-from-plan --fresh true --fallback-from-release-commit true\""));
        assertTrue(yaml.contains("run_javachanges \"publish\""));
        assertTrue(yaml.contains("run_javachanges \"gitlab-release --ignore-catalog-validation true\""));
        assertTrue(yaml.contains("$CI_COMMIT_BRANCH == \"develop\""));
        assertTrue(yaml.contains("$CI_COMMIT_BRANCH == \"snapshot-dev\""));
    }

    @Test
    void initCiTemplatesShareDefaultJavachangesVersion(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, false);

        ExecutionResult gitlabResult = execute(
            "init-gitlab-ci",
            "--directory", repoRoot.toString(),
            "--output", ".gitlab-ci.generated.yml"
        );
        ExecutionResult githubResult = execute(
            "init-github-actions",
            "--directory", repoRoot.toString(),
            "--output", ".github/workflows/generated-release.yml"
        );

        assertEquals(0, gitlabResult.exitCode);
        assertEquals(0, githubResult.exitCode);
        String expectedVersion = "JAVACHANGES_VERSION: \"" + JavaChangesVersion.FALLBACK_RELEASED_VERSION + "\"";
        assertTrue(read(repoRoot.resolve(".gitlab-ci.generated.yml")).contains(expectedVersion));
        assertTrue(read(repoRoot.resolve(".github/workflows/generated-release.yml")).contains(expectedVersion));
    }

    @Test
    void initCiTemplatesRejectOutputOutsideRepository(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, false);

        ExecutionResult gitlabResult = execute(
            "init-gitlab-ci",
            "--directory", repoRoot.toString(),
            "--output", "../generated.yml"
        );
        ExecutionResult githubResult = execute(
            "init-github-actions",
            "--directory", repoRoot.toString(),
            "--output", "../generated.yml"
        );

        assertNotEquals(0, gitlabResult.exitCode);
        assertNotEquals(0, githubResult.exitCode);
        assertTrue(gitlabResult.stderr.contains("--output must stay inside repository: ../generated.yml"));
        assertTrue(githubResult.stderr.contains("--output must stay inside repository: ../generated.yml"));
        assertFalse(Files.exists(tempDir.resolve("generated.yml")));
    }

    @Test
    void initGithubActionsWritesMavenWorkflow(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, false);
        Files.createDirectories(repoRoot.resolve(".changesets"));
        Files.write(repoRoot.resolve(".changesets").resolve("config.json"), (
            "{\n" +
                "  \"baseBranch\": \"develop\",\n" +
                "  \"releaseBranch\": \"changeset-release/develop\"\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));

        ExecutionResult result = execute(
            "init-github-actions",
            "--directory", repoRoot.toString(),
            "--output", ".github/workflows/generated-release.yml",
            "--javachanges-version", "1.8.0",
            "--build-tool", "maven"
        );

        assertEquals(0, result.exitCode);
        String yaml = read(repoRoot.resolve(".github/workflows/generated-release.yml"));
        JsonNode root = assertYamlObject(yaml);
        assertTrue(root.has("name"));
        assertTrue(root.has("jobs"));
        assertTrue(root.get("jobs").has("release-plan"));
        assertTrue(root.get("jobs").has("publish"));
        assertTrue(yaml.contains("JAVACHANGES_VERSION: \"1.8.0\""));
        assertTrue(yaml.contains("pull-requests: write"));
        assertTrue(yaml.contains("mvn -B io.github.sonofmagic:javachanges:${JAVACHANGES_VERSION}:run"));
        assertTrue(yaml.contains("github-release-plan --directory $GITHUB_WORKSPACE --write-plan-files false --execute true"));
        assertTrue(yaml.contains("github-release-publish-state --directory $GITHUB_WORKSPACE --fresh true"));
        assertTrue(yaml.contains("github-tag-from-plan --directory $GITHUB_WORKSPACE --fresh true"));
        assertTrue(yaml.contains("publish --directory $GITHUB_WORKSPACE --tag ${{ steps.release.outputs.release_tag }} --execute true"));
        assertTrue(yaml.contains("github-release-from-plan --directory $GITHUB_WORKSPACE --fresh true"));
        assertTrue(yaml.contains("if: steps.release.outputs.should_publish == 'true'"));
        assertTrue(yaml.contains("github.event.pull_request.base.ref == 'develop'"));
        assertTrue(yaml.contains("github.event.pull_request.head.ref == 'changeset-release/develop'"));
        assertFalse(yaml.contains("gradle-publish"));
    }

    @Test
    void initGithubActionsWritesGradleWorkflow(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createGradleRepository(tempDir, false);
        Files.createDirectories(repoRoot.resolve(".changesets"));
        Files.write(repoRoot.resolve(".changesets").resolve("config.json"), (
            "{\n" +
                "  \"baseBranch\": \"develop\",\n" +
                "  \"releaseBranch\": \"changeset-release/develop\"\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));

        ExecutionResult result = execute(
            "init-github-actions",
            "--directory", repoRoot.toString(),
            "--output", ".github/workflows/generated-release.yml",
            "--javachanges-version", "1.8.0",
            "--build-tool", "gradle"
        );

        assertEquals(0, result.exitCode);
        String yaml = read(repoRoot.resolve(".github/workflows/generated-release.yml"));
        JsonNode root = assertYamlObject(yaml);
        assertTrue(root.has("name"));
        assertTrue(root.has("jobs"));
        assertTrue(root.get("jobs").has("release-plan"));
        assertTrue(root.get("jobs").has("publish"));
        assertTrue(yaml.contains("JAVACHANGES_JAR: .javachanges/javachanges-${{ env.JAVACHANGES_VERSION }}.jar"));
        assertTrue(yaml.contains("distribution: temurin"));
        assertTrue(yaml.contains("cache: gradle"));
        assertTrue(yaml.contains("./gradlew --no-daemon build"));
        assertTrue(yaml.contains("curl -fsSL"));
        assertTrue(yaml.contains("github-release-plan --directory \"$GITHUB_WORKSPACE\" --write-plan-files false --execute true"));
        assertTrue(yaml.contains("github-release-publish-state --directory \"$GITHUB_WORKSPACE\" --fresh true"));
        assertTrue(yaml.contains("gradle-publish --directory \"$GITHUB_WORKSPACE\" --tag \"${{ steps.release.outputs.release_tag }}\" --execute true"));
        assertTrue(yaml.contains("github-release-from-plan --directory \"$GITHUB_WORKSPACE\" --fresh true"));
        assertTrue(yaml.contains("if: steps.release.outputs.should_publish == 'true'"));
        assertTrue(yaml.contains("github.event.pull_request.base.ref == 'develop'"));
        assertTrue(yaml.contains("github.event.pull_request.head.ref == 'changeset-release/develop'"));
        assertFalse(yaml.contains("io.github.sonofmagic:javachanges:${JAVACHANGES_VERSION}:run"));
    }

    @Test
    void initGitlabCiWritesGradleTemplate(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createGradleRepository(tempDir, false);
        Files.createDirectories(repoRoot.resolve(".changesets"));
        Files.write(repoRoot.resolve(".changesets").resolve("config.json"), (
            "{\n" +
                "  \"baseBranch\": \"develop\",\n" +
                "  \"snapshotBranch\": \"snapshot-dev\"\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));

        ExecutionResult result = execute(
            "init-gitlab-ci",
            "--directory", repoRoot.toString(),
            "--output", ".gitlab-ci.gradle.yml",
            "--javachanges-version", "1.8.0",
            "--build-tool", "gradle"
        );

        assertEquals(0, result.exitCode);
        String yaml = read(repoRoot.resolve(".gitlab-ci.gradle.yml"));
        JsonNode root = assertYamlObject(yaml);
        assertTrue(root.has("stages"));
        assertTrue(root.has("release_plan_mr"));
        assertTrue(root.has("release_tag"));
        assertTrue(root.has("publish_release"));
        assertTrue(yaml.contains("image: eclipse-temurin:17"));
        assertTrue(yaml.contains("GRADLE_USER_HOME: \"$CI_PROJECT_DIR/.gradle\""));
        assertTrue(yaml.contains("curl -fsSL"));
        assertTrue(yaml.contains("./gradlew --no-daemon build"));
        assertTrue(yaml.contains("gitlab-release-plan"));
        assertTrue(yaml.contains("--write-plan-files false"));
        assertTrue(yaml.contains("gitlab-tag-from-plan --directory \"$CI_PROJECT_DIR\" --fresh true --fallback-from-release-commit true --execute true"));
        assertTrue(yaml.contains("gradle-publish --directory \"$CI_PROJECT_DIR\" --execute true"));
        assertTrue(yaml.contains("gitlab-release --directory \"$CI_PROJECT_DIR\" --ignore-catalog-validation true --execute true"));
        assertTrue(yaml.contains("$CI_COMMIT_BRANCH == \"develop\""));
        assertTrue(yaml.contains("$CI_COMMIT_BRANCH == \"snapshot-dev\""));
        assertFalse(yaml.contains("io.github.sonofmagic:javachanges:${JAVACHANGES_VERSION}:run"));
        assertFalse(yaml.contains("publish --directory $CI_PROJECT_DIR --execute true"));
    }

    @Test
    void preflightHelpListsSnapshotVersionModeFlag() {
        ExecutionResult result = execute("preflight", "--help");

        assertEquals(0, result.exitCode);
        assertTrue(result.stdout.contains("--snapshot-version-mode"));
        assertTrue(result.stdout.contains("plain or stamped"));
    }

    @Test
    void doctorPublishJsonReportsMissingMavenCentralReadiness(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, false);
        Files.write(repoRoot.resolve(ReleaseProcessUtils.mavenWrapperPath()), "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));

        ExecutionResult result = executeProcess(publishDoctorEnv(),
            "doctor-publish",
            "--directory", repoRoot.toString(),
            "--format", "json"
        );

        assertNotEquals(0, result.exitCode);
        assertEquals("", result.stderr);
        JsonNode root = ReleaseJsonUtils.readTree(result.stdout);
        assertFalse(root.get("ok").asBoolean());
        assertEquals("doctor-publish", root.get("command").asText());
        assertEquals("maven-central", root.get("target").asText());
        assertEquals("snapshot", root.get("mode").asText());
        assertTrue(hasCheck(root, "Maven POM", "Central metadata", "FAILED"));
        assertTrue(hasCheck(root, "Maven POM", "profile central-snapshot-publish", "FAILED"));
        assertTrue(hasSuggestion(root, "Maven Central metadata"));
        assertTrue(hasSuggestion(root, "central-snapshot-publish"));
    }

    @Test
    void doctorPublishJsonPassesForCentralReadyMavenProject(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot);
        Files.write(repoRoot.resolve("pom.xml"), centralReadyPom().getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve(ReleaseProcessUtils.mavenWrapperPath()), "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));
        commitAll(repoRoot);

        ExecutionResult result = executeProcess(publishDoctorEnv(),
            "doctor-publish",
            "--directory", repoRoot.toString(),
            "--format", "json"
        );

        assertEquals(0, result.exitCode, result.stdout + result.stderr);
        JsonNode root = ReleaseJsonUtils.readTree(result.stdout);
        assertTrue(root.get("ok").asBoolean());
        assertEquals("snapshot", root.get("mode").asText());
        assertEquals("maven", root.get("buildTool").asText());
        assertEquals("1.2.3-SNAPSHOT", root.get("currentRevision").asText());
        assertEquals("stamped", root.get("snapshotVersionMode").asText());
        assertTrue(root.get("snapshotBuildStampApplied").asBoolean());
        assertTrue(root.get("publishVersion").asText().matches("1\\.2\\.3-[0-9]{8}\\.[0-9]{6}\\.[A-Za-z0-9]+-SNAPSHOT"));
        assertTrue(root.get("nextCommands").get(0).asText().contains("--snapshot-version-mode stamped"));
        assertTrue(root.get("nextCommands").get(0).asText().contains("--snapshot-build-stamp " + root.get("snapshotBuildStamp").asText()));
        assertTrue(hasCheck(root, "Maven POM", "Central metadata", "OK"));
        assertTrue(hasCheck(root, "Credentials", "GPG signing", "OK"));
        assertFalse(root.has("suggestions"));
    }

    @Test
    void doctorPublishJsonQuotesNextCommandsForRepositoryPathWithSpaces(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo with spaces");
        Files.createDirectories(repoRoot);
        Files.write(repoRoot.resolve("pom.xml"), centralReadyPom().getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve(ReleaseProcessUtils.mavenWrapperPath()), "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));
        commitAll(repoRoot);

        ExecutionResult result = executeProcess(publishDoctorEnv(),
            "doctor-publish",
            "--directory", repoRoot.toString(),
            "--format", "json"
        );

        assertEquals(0, result.exitCode, result.stdout + result.stderr);
        JsonNode root = ReleaseJsonUtils.readTree(result.stdout);
        assertTrue(root.get("ok").asBoolean());
        assertTrue(root.get("nextCommands").get(0).asText()
            .contains("--directory '" + repoRoot + "'"));
        assertTrue(root.get("nextCommands").get(1).asText()
            .contains("--directory '" + repoRoot + "'"));
    }

    @Test
    void doctorPublishJsonSupportsExplicitSnapshotBuildStamp(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot);
        Files.write(repoRoot.resolve("pom.xml"), centralReadyPom().getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve(ReleaseProcessUtils.mavenWrapperPath()), "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));
        commitAll(repoRoot);

        ExecutionResult result = executeProcess(publishDoctorEnv(),
            "doctor-publish",
            "--directory", repoRoot.toString(),
            "--snapshot-build-stamp", "20260430.120000.ci001",
            "--format", "json"
        );

        assertEquals(0, result.exitCode, result.stdout + result.stderr);
        JsonNode root = ReleaseJsonUtils.readTree(result.stdout);
        assertTrue(root.get("ok").asBoolean());
        assertEquals("stamped", root.get("snapshotVersionMode").asText());
        assertEquals("20260430.120000.ci001", root.get("snapshotBuildStamp").asText());
        assertEquals("1.2.3-20260430.120000.ci001-SNAPSHOT", root.get("publishVersion").asText());
        assertTrue(root.get("nextCommands").get(0).asText().contains("--snapshot-build-stamp 20260430.120000.ci001"));
    }

    @Test
    void doctorPublishJsonSupportsPlainSnapshotMode(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot);
        Files.write(repoRoot.resolve("pom.xml"), centralReadyPom().getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve(ReleaseProcessUtils.mavenWrapperPath()), "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));
        commitAll(repoRoot);

        ExecutionResult result = executeProcess(publishDoctorEnv(),
            "doctor-publish",
            "--directory", repoRoot.toString(),
            "--snapshot-version-mode", "plain",
            "--format", "json"
        );

        assertEquals(0, result.exitCode, result.stdout + result.stderr);
        JsonNode root = ReleaseJsonUtils.readTree(result.stdout);
        assertTrue(root.get("ok").asBoolean());
        assertEquals("plain", root.get("snapshotVersionMode").asText());
        assertFalse(root.get("snapshotBuildStampApplied").asBoolean());
        assertTrue(root.get("snapshotBuildStamp").isNull());
        assertEquals("1.2.3-SNAPSHOT", root.get("publishVersion").asText());
        assertTrue(root.get("nextCommands").get(0).asText().contains("--snapshot-version-mode plain"));
        assertFalse(root.get("nextCommands").get(0).asText().contains("--snapshot-build-stamp"));
    }

    @Test
    void doctorPublishJsonSupportsMavenModuleNextCommands(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot);
        Files.write(repoRoot.resolve("pom.xml"), centralReadyPom().getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve(ReleaseProcessUtils.mavenWrapperPath()), "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));
        commitAll(repoRoot);

        ExecutionResult result = executeProcess(publishDoctorEnv(),
            "doctor-publish",
            "--directory", repoRoot.toString(),
            "--module", "fixture-app",
            "--format", "json"
        );

        assertEquals(0, result.exitCode, result.stdout + result.stderr);
        JsonNode root = ReleaseJsonUtils.readTree(result.stdout);
        assertTrue(root.get("ok").asBoolean());
        assertEquals("fixture-app", root.get("module").asText());
        assertTrue(hasCheck(root, "Project", "target module", "OK"));
        assertTrue(root.get("nextCommands").get(0).asText().contains("--module fixture-app"));
        assertTrue(root.get("nextCommands").get(1).asText().contains("--module fixture-app"));
    }

    @Test
    void doctorPublishJsonUsesPerModuleReleaseTagForModule(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot.resolve(".changesets"));
        Files.write(repoRoot.resolve("pom.xml"), centralReadyPom().getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve(".changesets").resolve("config.json"),
            "{\n  \"tagStrategy\": \"per-module\"\n}\n".getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve(ReleaseProcessUtils.mavenWrapperPath()), "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));
        commitAll(repoRoot);

        ExecutionResult result = executeProcess(publishDoctorReleaseEnv(),
            "doctor-publish",
            "--directory", repoRoot.toString(),
            "--mode", "release",
            "--module", "fixture-app",
            "--format", "json"
        );

        assertEquals(0, result.exitCode, result.stdout + result.stderr);
        JsonNode root = ReleaseJsonUtils.readTree(result.stdout);
        assertTrue(root.get("ok").asBoolean());
        assertEquals("release", root.get("mode").asText());
        assertTrue(root.get("nextCommands").get(0).asText().contains("--tag fixture-app/v1.2.3 --module fixture-app"));
        assertTrue(root.get("nextCommands").get(1).asText().contains("--tag fixture-app/v1.2.3 --module fixture-app"));
    }

    @Test
    void doctorPublishJsonUsesExplicitReleaseTag(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot);
        Files.write(repoRoot.resolve("pom.xml"), centralReadyPom().getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve(ReleaseProcessUtils.mavenWrapperPath()), "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));
        commitAll(repoRoot);

        ExecutionResult result = executeProcess(publishDoctorReleaseEnv(),
            "doctor-publish",
            "--directory", repoRoot.toString(),
            "--tag", "v1.2.3",
            "--format", "json"
        );

        assertEquals(0, result.exitCode, result.stdout + result.stderr);
        JsonNode root = ReleaseJsonUtils.readTree(result.stdout);
        assertTrue(root.get("ok").asBoolean());
        assertEquals("release", root.get("mode").asText());
        assertEquals("v1.2.3", root.get("tag").asText());
        assertEquals("1.2.3", root.get("publishVersion").asText());
        assertTrue(hasCheck(root, "Project", "release tag", "OK"));
        assertTrue(root.get("nextCommands").get(0).asText().contains("--tag v1.2.3"));
        assertTrue(root.get("nextCommands").get(1).asText().contains("--tag v1.2.3"));
    }

    @Test
    void doctorPublishJsonInfersModuleFromReleaseTag(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot);
        Files.write(repoRoot.resolve("pom.xml"), centralReadyPom().getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve(ReleaseProcessUtils.mavenWrapperPath()), "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));
        commitAll(repoRoot);

        ExecutionResult result = executeProcess(publishDoctorReleaseEnv(),
            "doctor-publish",
            "--directory", repoRoot.toString(),
            "--tag", "fixture-app/v1.2.3",
            "--format", "json"
        );

        assertEquals(0, result.exitCode, result.stdout + result.stderr);
        JsonNode root = ReleaseJsonUtils.readTree(result.stdout);
        assertTrue(root.get("ok").asBoolean());
        assertEquals("fixture-app/v1.2.3", root.get("tag").asText());
        assertEquals("fixture-app", root.get("module").asText());
        assertEquals("1.2.3", root.get("publishVersion").asText());
        assertTrue(hasCheck(root, "Project", "target module", "OK"));
        assertTrue(root.get("nextCommands").get(0).asText().contains("--tag fixture-app/v1.2.3 --module fixture-app"));
        assertTrue(root.get("nextCommands").get(1).asText().contains("--tag fixture-app/v1.2.3 --module fixture-app"));
    }

    @Test
    void doctorPublishJsonRejectsMismatchedModuleReleaseTag(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot);
        Files.write(repoRoot.resolve("pom.xml"), centralReadyPom().getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve(ReleaseProcessUtils.mavenWrapperPath()), "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));
        commitAll(repoRoot);

        ExecutionResult result = executeProcess(publishDoctorReleaseEnv(),
            "doctor-publish",
            "--directory", repoRoot.toString(),
            "--tag", "fixture-app/v1.2.3",
            "--module", "other-module",
            "--format", "json"
        );

        assertNotEquals(0, result.exitCode);
        JsonNode root = ReleaseJsonUtils.readTree(result.stdout);
        assertFalse(root.get("ok").asBoolean());
        assertEquals("fixture-app/v1.2.3", root.get("tag").asText());
        assertEquals("other-module", root.get("module").asText());
        assertTrue(hasCheck(root, "Project", "release tag", "FAILED"));
        assertTrue(hasSuggestion(root, "Use a tag that matches"));
    }

    @Test
    void doctorPublishJsonRejectsReleaseTagVersionMismatch(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot);
        Files.write(repoRoot.resolve("pom.xml"), centralReadyPom().getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve(ReleaseProcessUtils.mavenWrapperPath()), "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));
        commitAll(repoRoot);

        ExecutionResult result = executeProcess(publishDoctorReleaseEnv(),
            "doctor-publish",
            "--directory", repoRoot.toString(),
            "--tag", "v9.9.9",
            "--format", "json"
        );

        assertNotEquals(0, result.exitCode);
        JsonNode root = ReleaseJsonUtils.readTree(result.stdout);
        assertFalse(root.get("ok").asBoolean());
        assertEquals("v9.9.9", root.get("tag").asText());
        assertEquals("9.9.9", root.get("publishVersion").asText());
        assertTrue(hasCheck(root, "Project", "release tag", "FAILED"));
        assertTrue(hasSuggestion(root, "v1.2.3"));
    }

    @Test
    void doctorPublishJsonRejectsSnapshotModeWithTag() {
        ExecutionResult result = execute(
            "doctor-publish",
            "--mode", "snapshot",
            "--tag", "v1.2.3",
            "--format", "json"
        );

        assertNotEquals(0, result.exitCode);
        assertEquals("", result.stderr);
        JsonNode root = ReleaseJsonUtils.readTree(result.stdout);
        assertFalse(root.get("ok").asBoolean());
        assertEquals("doctor-publish", root.get("command").asText());
        assertTrue(root.get("reason").asText().contains("snapshot cannot be used with --tag"));
    }

    @Test
    void doctorPublishJsonRejectsUnknownModule(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot);
        Files.write(repoRoot.resolve("pom.xml"), centralReadyPom().getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve(ReleaseProcessUtils.mavenWrapperPath()), "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));
        commitAll(repoRoot);

        ExecutionResult result = executeProcess(publishDoctorEnv(),
            "doctor-publish",
            "--directory", repoRoot.toString(),
            "--module", "missing-module",
            "--format", "json"
        );

        assertNotEquals(0, result.exitCode);
        JsonNode root = ReleaseJsonUtils.readTree(result.stdout);
        assertFalse(root.get("ok").asBoolean());
        assertEquals("missing-module", root.get("module").asText());
        assertTrue(hasCheck(root, "Project", "target module", "FAILED"));
        assertTrue(hasSuggestion(root, "javachanges modules"));
    }

    @Test
    void doctorPublishJsonReportsDirtyWorktree(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot);
        Files.write(repoRoot.resolve("pom.xml"), centralReadyPom().getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve(ReleaseProcessUtils.mavenWrapperPath()), "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));
        commitAll(repoRoot);
        Files.write(repoRoot.resolve("untracked.txt"), "dirty\n".getBytes(StandardCharsets.UTF_8));

        ExecutionResult result = executeProcess(publishDoctorEnv(),
            "doctor-publish",
            "--directory", repoRoot.toString(),
            "--format", "json"
        );

        assertNotEquals(0, result.exitCode);
        JsonNode root = ReleaseJsonUtils.readTree(result.stdout);
        assertFalse(root.get("ok").asBoolean());
        assertTrue(hasCheck(root, "Git", "worktree", "FAILED"));
        assertTrue(hasSuggestion(root, "Commit, stash, or clean"));
    }

    @Test
    void doctorPublishJsonAllowsDirtyWorktreeWhenRequested(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot);
        Files.write(repoRoot.resolve("pom.xml"), centralReadyPom().getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve(ReleaseProcessUtils.mavenWrapperPath()), "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));
        commitAll(repoRoot);
        Files.write(repoRoot.resolve("untracked.txt"), "dirty\n".getBytes(StandardCharsets.UTF_8));

        ExecutionResult result = executeProcess(publishDoctorEnv(),
            "doctor-publish",
            "--directory", repoRoot.toString(),
            "--allow-dirty", "true",
            "--format", "json"
        );

        assertEquals(0, result.exitCode, result.stdout + result.stderr);
        JsonNode root = ReleaseJsonUtils.readTree(result.stdout);
        assertTrue(root.get("ok").asBoolean());
        assertTrue(hasCheck(root, "Git", "worktree", "SKIPPED"));
        assertTrue(root.get("nextCommands").get(0).asText().contains("--allow-dirty true"));
        assertTrue(root.get("nextCommands").get(1).asText().contains("--allow-dirty true"));
    }

    @Test
    void doctorPublishJsonReportsInvalidMavenRepositoryUrl(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot);
        Files.write(repoRoot.resolve("pom.xml"), centralReadyPom().getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve(ReleaseProcessUtils.mavenWrapperPath()), "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));
        commitAll(repoRoot);
        Map<String, String> environment = publishDoctorEnv();
        environment.put("MAVEN_SNAPSHOT_REPOSITORY_URL", "not-a-url");

        ExecutionResult result = executeProcess(environment,
            "doctor-publish",
            "--directory", repoRoot.toString(),
            "--format", "json"
        );

        assertNotEquals(0, result.exitCode);
        JsonNode root = ReleaseJsonUtils.readTree(result.stdout);
        assertFalse(root.get("ok").asBoolean());
        assertTrue(hasCheck(root, "Credentials", "snapshot repository URL", "FAILED"));
        assertTrue(hasSuggestion(root, "absolute repository URL"));
    }

    @Test
    void doctorPublishJsonReportsIncompleteMavenProfileGoals(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot);
        String pom = centralReadyPom().replace("<goal>jar-no-fork</goal>", "");
        Files.write(repoRoot.resolve("pom.xml"), pom.getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve(ReleaseProcessUtils.mavenWrapperPath()), "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));
        commitAll(repoRoot);

        ExecutionResult result = executeProcess(publishDoctorEnv(),
            "doctor-publish",
            "--directory", repoRoot.toString(),
            "--format", "json"
        );

        assertNotEquals(0, result.exitCode);
        JsonNode root = ReleaseJsonUtils.readTree(result.stdout);
        assertFalse(root.get("ok").asBoolean());
        assertTrue(hasCheck(root, "Maven POM", "central-snapshot-publish maven-source-plugin goals", "FAILED"));
        assertTrue(hasSuggestion(root, "maven-source-plugin"));
    }

    @Test
    void doctorPublishJsonReportsIncompleteCentralPublishingPluginConfiguration(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot);
        String pom = centralReadyPom().replace("<extensions>true</extensions>", "");
        Files.write(repoRoot.resolve("pom.xml"), pom.getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve(ReleaseProcessUtils.mavenWrapperPath()), "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));
        commitAll(repoRoot);

        ExecutionResult result = executeProcess(publishDoctorEnv(),
            "doctor-publish",
            "--directory", repoRoot.toString(),
            "--format", "json"
        );

        assertNotEquals(0, result.exitCode);
        JsonNode root = ReleaseJsonUtils.readTree(result.stdout);
        assertFalse(root.get("ok").asBoolean());
        assertTrue(hasCheck(root, "Maven POM",
            "central-snapshot-publish central-publishing-maven-plugin extensions", "FAILED"));
        assertTrue(hasSuggestion(root, "Maven extension"));
    }

    @Test
    void doctorPublishJsonReportsMissingGradlePublishReadiness(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createGradleRepository(tempDir, false);
        Files.write(repoRoot.resolve(ReleaseProcessUtils.gradleWrapperPath()), "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));

        ExecutionResult result = executeProcess(publishDoctorEnv(),
            "doctor-publish",
            "--directory", repoRoot.toString(),
            "--format", "json"
        );

        assertNotEquals(0, result.exitCode);
        JsonNode root = ReleaseJsonUtils.readTree(result.stdout);
        assertFalse(root.get("ok").asBoolean());
        assertEquals("gradle", root.get("buildTool").asText());
        assertTrue(hasCheck(root, "Gradle", "publish configuration", "FAILED"));
        assertTrue(hasSuggestion(root, "maven-publish"));
    }

    @Test
    void doctorPublishJsonPassesForGradlePublishProject(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createGradleRepository(tempDir, false);
        Files.write(repoRoot.resolve("build.gradle"), gradlePublishBuildFile().getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve(ReleaseProcessUtils.gradleWrapperPath()), "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));
        commitAll(repoRoot);

        ExecutionResult result = executeProcess(publishDoctorEnv(),
            "doctor-publish",
            "--directory", repoRoot.toString(),
            "--format", "json"
        );

        assertEquals(0, result.exitCode, result.stdout + result.stderr);
        JsonNode root = ReleaseJsonUtils.readTree(result.stdout);
        assertTrue(root.get("ok").asBoolean());
        assertEquals("gradle", root.get("buildTool").asText());
        assertEquals("publish", root.get("gradleTask").asText());
        assertTrue(hasCheck(root, "Gradle", "publish configuration", "OK"));
        assertTrue(hasCheck(root, "Gradle", "publish task", "OK"));
        assertTrue(hasCheck(root, "Credentials", "Gradle signing", "OK"));
        assertFalse(root.has("suggestions"));
        assertTrue(root.get("nextCommands").get(0).asText().contains("gradle-publish"));
    }

    @Test
    void doctorPublishJsonIncludesCustomGradleTask(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createGradleRepository(tempDir, false);
        Files.write(repoRoot.resolve("build.gradle"), gradlePublishBuildFile().getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve(ReleaseProcessUtils.gradleWrapperPath()), "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));
        commitAll(repoRoot);

        ExecutionResult result = executeProcess(publishDoctorEnv(),
            "doctor-publish",
            "--directory", repoRoot.toString(),
            "--task", "publishAllPublicationsToMavenRepository",
            "--format", "json"
        );

        assertEquals(0, result.exitCode, result.stdout + result.stderr);
        JsonNode root = ReleaseJsonUtils.readTree(result.stdout);
        assertTrue(root.get("ok").asBoolean());
        assertEquals("publishAllPublicationsToMavenRepository", root.get("gradleTask").asText());
        assertTrue(root.get("nextCommands").get(0).asText().contains("--task publishAllPublicationsToMavenRepository"));
    }

    @Test
    void doctorPublishJsonRejectsQualifiedGradleTaskWithModule(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createGradleRepository(tempDir, false);
        Files.write(repoRoot.resolve("build.gradle"), gradlePublishBuildFile().getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve(ReleaseProcessUtils.gradleWrapperPath()), "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));
        commitAll(repoRoot);

        ExecutionResult result = executeProcess(publishDoctorEnv(),
            "doctor-publish",
            "--directory", repoRoot.toString(),
            "--module", "fixture-app",
            "--task", ":fixture-app:publish",
            "--format", "json"
        );

        assertNotEquals(0, result.exitCode);
        JsonNode root = ReleaseJsonUtils.readTree(result.stdout);
        assertFalse(root.get("ok").asBoolean());
        assertEquals(":fixture-app:publish", root.get("gradleTask").asText());
        assertTrue(hasCheck(root, "Gradle", "publish task", "FAILED"));
        assertTrue(hasSuggestion(root, "Pass a task name"));
    }

    @Test
    void doctorPublishJsonReportsMissingGradleRepositoryUrl(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createGradleRepository(tempDir, false);
        Files.write(repoRoot.resolve("build.gradle"), gradlePublishBuildFile().getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve(ReleaseProcessUtils.gradleWrapperPath()), "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));
        commitAll(repoRoot);

        ExecutionResult result = executeProcess(publishDoctorEnvWithoutRepositoryUrls(),
            "doctor-publish",
            "--directory", repoRoot.toString(),
            "--format", "json"
        );

        assertNotEquals(0, result.exitCode);
        JsonNode root = ReleaseJsonUtils.readTree(result.stdout);
        assertFalse(root.get("ok").asBoolean());
        assertTrue(hasCheck(root, "Credentials", "Gradle snapshot repository URL", "FAILED"));
        assertTrue(hasSuggestion(root, "ORG_GRADLE_PROJECT_mavenSnapshotRepositoryUrl"));
    }

    @Test
    void doctorPublishJsonReportsInvalidGradleRepositoryUrl(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createGradleRepository(tempDir, false);
        Files.write(repoRoot.resolve("build.gradle"), gradlePublishBuildFile().getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve(ReleaseProcessUtils.gradleWrapperPath()), "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));
        commitAll(repoRoot);
        Map<String, String> environment = publishDoctorEnvWithoutRepositoryUrls();
        environment.put("ORG_GRADLE_PROJECT_mavenSnapshotRepositoryUrl", "repo.example.com/snapshots");

        ExecutionResult result = executeProcess(environment,
            "doctor-publish",
            "--directory", repoRoot.toString(),
            "--format", "json"
        );

        assertNotEquals(0, result.exitCode);
        JsonNode root = ReleaseJsonUtils.readTree(result.stdout);
        assertFalse(root.get("ok").asBoolean());
        assertTrue(hasCheck(root, "Credentials", "Gradle snapshot repository URL", "FAILED"));
        assertTrue(hasSuggestion(root, "absolute repository URL"));
    }

    @Test
    void doctorPublishJsonAcceptsGradleProjectRepositoryUrl(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createGradleRepository(tempDir, false);
        Files.write(repoRoot.resolve("build.gradle"), gradlePublishBuildFile().getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve(ReleaseProcessUtils.gradleWrapperPath()), "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));
        commitAll(repoRoot);

        ExecutionResult result = executeProcess(gradlePublishDoctorEnv(),
            "doctor-publish",
            "--directory", repoRoot.toString(),
            "--format", "json"
        );

        assertEquals(0, result.exitCode, result.stdout + result.stderr);
        JsonNode root = ReleaseJsonUtils.readTree(result.stdout);
        assertTrue(root.get("ok").asBoolean());
        assertTrue(hasCheck(root, "Credentials", "Gradle snapshot repository URL", "OK"));
    }

    @Test
    void doctorLocalFallsBackToSystemMavenWhenWrapperMissing(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, false);
        ReleaseEnvTestFixtures.writeLocalEnv(repoRoot, ReleaseEnvTestFixtures.cliEnvFile());

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
        ReleaseEnvTestFixtures.writeLocalEnv(repoRoot, ReleaseEnvTestFixtures.cliEnvFile());

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
        ReleaseEnvTestFixtures.writeLocalEnv(repoRoot, ReleaseEnvTestFixtures.cliEnvFile());

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
        assertTrue(result.stdout.contains("\"error\":\"Local release environment is not ready\""));
        assertFalse(result.stdout.contains("== Local Runtime =="));
        assertEquals("", result.stderr);
    }

    @Test
    void auditVarsJsonPrintsOnlyJson(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, false);
        ReleaseEnvTestFixtures.writeLocalEnv(repoRoot, ReleaseEnvTestFixtures.cliEnvFile());

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
        assertTrue(result.stdout.contains("\"error\":\"Missing repository argument: GITHUB_REPO\""));
        assertTrue(result.stdout.contains("\"label\":\"GITHUB_REPO\""));
        assertFalse(result.stdout.contains("== GitHub Variables Audit =="));
        assertEquals("", result.stderr);
    }

    @Test
    void resolveMavenCommandPrefersWrapper(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, false);
        Files.write(repoRoot.resolve(ReleaseProcessUtils.mavenWrapperPath()), "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));

        MavenCommand command = ReleaseProcessUtils.resolveMavenCommand(repoRoot, new MavenCommandProbe() {
            @Override
            public boolean fileExists(Path path) {
                return Files.exists(path);
            }

            @Override
            public boolean commandAvailable(Path workingDirectory, String... command) {
                return true;
            }
        });

        assertEquals(ReleaseProcessUtils.mavenWrapperPath(), command.command);
        assertEquals("wrapper", command.source);
    }

    @Test
    void resolveMavenCommandFallsBackToSystemMaven(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, false);

        MavenCommand command = ReleaseProcessUtils.resolveMavenCommand(repoRoot, new MavenCommandProbe() {
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

        MavenCommand command = ReleaseProcessUtils.resolveMavenCommand(repoRoot, new MavenCommandProbe() {
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

    private static ExecutionResult executeProcess(Map<String, String> environment, String... args) throws Exception {
        List<String> command = new ArrayList<String>();
        command.add(javaCommand());
        command.add("-cp");
        command.add(System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")));
        command.add(JavaChangesCli.class.getName());
        command.addAll(Arrays.asList(args));

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.environment().remove("MAVEN_OPTS");
        builder.environment().putAll(environment);
        Process process = builder.start();
        int exitCode = process.waitFor();
        return new ExecutionResult(
            exitCode,
            new String(readAll(process.getInputStream()), StandardCharsets.UTF_8),
            new String(readAll(process.getErrorStream()), StandardCharsets.UTF_8)
        );
    }

    private static String javaCommand() {
        String binary = System.getProperty("os.name", "").toLowerCase().contains("win") ? "java.exe" : "java";
        return Paths.get(System.getProperty("java.home"), "bin", binary).toString();
    }

    private static byte[] readAll(InputStream inputStream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
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

    private static Path createGradleRepository(Path tempDir, boolean git) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot);
        Files.write(repoRoot.resolve("settings.gradle"), "rootProject.name = 'fixture-app'\n".getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve("build.gradle"), "plugins { id 'java-library' }\n".getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve("gradle.properties"), "version=1.1.1-SNAPSHOT\n".getBytes(StandardCharsets.UTF_8));
        if (git) {
            run(repoRoot, "git", "init", "-q");
        }
        return repoRoot;
    }

    private static void commitAll(Path repoRoot) throws Exception {
        run(repoRoot, "git", "init", "-q");
        run(repoRoot, "git", "config", "user.name", "tester");
        run(repoRoot, "git", "config", "user.email", "tester@example.com");
        run(repoRoot, "git", "add", ".");
        run(repoRoot, "git", "commit", "-q", "-m", "initial");
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

    private static String centralReadyPom() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\">\n"
            + "  <modelVersion>4.0.0</modelVersion>\n"
            + "  <groupId>example</groupId>\n"
            + "  <artifactId>fixture-app</artifactId>\n"
            + "  <version>${revision}</version>\n"
            + "  <name>fixture-app</name>\n"
            + "  <description>Fixture app.</description>\n"
            + "  <url>https://example.com/fixture-app</url>\n"
            + "  <properties><revision>1.2.3-SNAPSHOT</revision></properties>\n"
            + "  <licenses><license><name>Apache License 2.0</name><url>https://www.apache.org/licenses/LICENSE-2.0.txt</url></license></licenses>\n"
            + "  <developers><developer><id>tester</id><name>Tester</name></developer></developers>\n"
            + "  <scm><connection>scm:git:https://example.com/fixture-app.git</connection><url>https://example.com/fixture-app</url></scm>\n"
            + "  <profiles>\n"
            + centralProfile("central-publish")
            + centralProfile("central-snapshot-publish")
            + "  </profiles>\n"
            + "</project>\n";
    }

    private static String centralProfile(String id) {
        return "    <profile>\n"
            + "      <id>" + id + "</id>\n"
            + "      <build><plugins>\n"
            + "        <plugin><artifactId>flatten-maven-plugin</artifactId><executions><execution><goals><goal>flatten</goal></goals></execution></executions></plugin>\n"
            + "        <plugin><artifactId>maven-source-plugin</artifactId><executions><execution><goals><goal>jar-no-fork</goal></goals></execution></executions></plugin>\n"
            + "        <plugin><artifactId>maven-javadoc-plugin</artifactId><executions><execution><goals><goal>jar</goal></goals></execution></executions></plugin>\n"
            + "        <plugin><artifactId>maven-gpg-plugin</artifactId><executions><execution><goals><goal>sign</goal></goals></execution></executions></plugin>\n"
            + centralPublishingPlugin(id)
            + "      </plugins></build>\n"
            + "    </profile>\n";
    }

    private static String centralPublishingPlugin(String profileId) {
        String snapshotConfiguration = "central-snapshot-publish".equals(profileId)
            ? "<centralSnapshotsUrl>https://repo.example.com/snapshots</centralSnapshotsUrl>"
            : "";
        return "        <plugin><artifactId>central-publishing-maven-plugin</artifactId>"
            + "<extensions>true</extensions>"
            + "<configuration><publishingServerId>central</publishingServerId>"
            + snapshotConfiguration
            + "</configuration></plugin>\n";
    }

    private static String gradlePublishBuildFile() {
        return "plugins {\n"
            + "  id 'java-library'\n"
            + "  id 'maven-publish'\n"
            + "  id 'signing'\n"
            + "}\n"
            + "publishing {\n"
            + "  publications { mavenJava(MavenPublication) { from components.java } }\n"
            + "}\n"
            + "signing { sign publishing.publications.mavenJava }\n";
    }

    private static Map<String, String> publishDoctorEnv() {
        Map<String, String> environment = new LinkedHashMap<String, String>();
        environment.put("MAVEN_SNAPSHOT_REPOSITORY_URL", "https://repo.example.com/snapshots");
        environment.put("MAVEN_CENTRAL_USERNAME", "tester");
        environment.put("MAVEN_CENTRAL_PASSWORD", "secret");
        environment.put("MAVEN_GPG_PRIVATE_KEY", "private-key");
        environment.put("MAVEN_GPG_PASSPHRASE", "passphrase");
        return environment;
    }

    private static Map<String, String> publishDoctorReleaseEnv() {
        Map<String, String> environment = publishDoctorEnv();
        environment.put("MAVEN_RELEASE_REPOSITORY_URL", "https://repo.example.com/releases");
        return environment;
    }

    private static Map<String, String> publishDoctorEnvWithoutRepositoryUrls() {
        Map<String, String> environment = publishDoctorEnv();
        environment.remove("MAVEN_SNAPSHOT_REPOSITORY_URL");
        environment.remove("MAVEN_RELEASE_REPOSITORY_URL");
        return environment;
    }

    private static Map<String, String> gradlePublishDoctorEnv() {
        Map<String, String> environment = publishDoctorEnvWithoutRepositoryUrls();
        environment.put("ORG_GRADLE_PROJECT_mavenSnapshotRepositoryUrl", "https://repo.example.com/gradle-snapshots");
        return environment;
    }

    private static boolean hasCheck(JsonNode root, String section, String name, String status) {
        for (JsonNode check : root.get("checks")) {
            if (section.equals(check.get("section").asText())
                && name.equals(check.get("name").asText())
                && status.equals(check.get("status").asText())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasSuggestion(JsonNode root, String text) {
        if (!root.has("suggestions")) {
            return false;
        }
        for (JsonNode suggestion : root.get("suggestions")) {
            if (suggestion.asText().contains(text)) {
                return true;
            }
        }
        return false;
    }

    private static Path writeChangeset(Path repoRoot, String fileName, String content) throws IOException {
        Path changesetsDir = repoRoot.resolve(".changesets");
        Files.createDirectories(changesetsDir);
        Path path = changesetsDir.resolve(fileName);
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
        return path;
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

    private static JsonNode assertYamlObject(String yaml) throws IOException {
        JsonNode root = YAML_MAPPER.readTree(yaml);
        assertTrue(root != null && root.isObject(), "generated YAML should parse as an object");
        return root;
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
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
