package io.github.sonofmagic.javachanges.core;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentationConsistencyTest {
    private static final Pattern SNAPSHOT_PLUGIN_COORDINATE = Pattern.compile(
        "io\\.github\\.sonofmagic:javachanges:([^:\\s]+-SNAPSHOT)"
    );
    private static final Pattern CURRENT_SNAPSHOT_REFERENCE = Pattern.compile(
        "current `([^`]+-SNAPSHOT)`|当前 `([^`]+-SNAPSHOT)`"
    );
    private static final Pattern COMMAND_ANNOTATION = Pattern.compile("@Command\\(\\s*name\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern MARKDOWN_CODE = Pattern.compile("`([^`]+)`");

    @Test
    void readmeSnapshotPluginExamplesMatchProjectRevision() throws Exception {
        Path repoRoot = Paths.get("").toAbsolutePath().normalize();
        String revision = PomModelSupport.readRevision(repoRoot.resolve("pom.xml"));

        assertReadmeSnapshotExamplesMatch(repoRoot.resolve("README.md"), revision);
        assertReadmeSnapshotExamplesMatch(repoRoot.resolve("README.zh-CN.md"), revision);
    }

    @Test
    void readmeCommandSectionsListEveryCliCommand() throws Exception {
        Path repoRoot = Paths.get("").toAbsolutePath().normalize();
        Set<String> cliCommands = cliCommandNames(repoRoot);

        assertReadmeCommandSectionIncludes(repoRoot.resolve("README.md"), "## Commands", "## Docs", cliCommands);
        assertReadmeCommandSectionIncludes(repoRoot.resolve("README.zh-CN.md"), "## 命令", "## 文档", cliCommands);
    }

    @Test
    void cliReferenceMentionsEveryCliCommand() throws Exception {
        Path repoRoot = Paths.get("").toAbsolutePath().normalize();
        Set<String> cliCommands = cliCommandNames(repoRoot);

        assertMarkdownMentionsCommands(repoRoot.resolve("docs/cli-reference.md"), cliCommands);
        assertMarkdownMentionsCommands(repoRoot.resolve("docs/cli-reference.zh-CN.md"), cliCommands);
    }

    @Test
    void releaseRetryDocsMatchPublishReleaseWorkflowInput() throws Exception {
        Path repoRoot = Paths.get("").toAbsolutePath().normalize();
        String workflow = read(repoRoot.resolve(".github/workflows/publish-release.yml"));

        assertTrue(workflow.contains("workflow_dispatch:"), "publish-release.yml should support manual dispatch");
        assertTrue(workflow.contains("release_commit_sha:"), "publish-release.yml should accept the retry commit SHA");
        assertTrue(workflow.contains("inputs.release_commit_sha || github.event.workflow_run.head_sha"),
            "publish-release.yml should reuse the same commit expression for automatic and manual runs");
        assertTrue(workflow.contains(
            "ref: ${{ github.event_name == 'workflow_dispatch' && inputs.release_commit_sha || github.event.workflow_run.head_sha }}"
        ), "publish-release.yml should checkout the requested retry commit");

        assertMarkdownMentionsReleaseCommitSha(repoRoot.resolve("docs/github-actions-release.md"));
        assertMarkdownMentionsReleaseCommitSha(repoRoot.resolve("docs/github-actions-release.zh-CN.md"));
        assertMarkdownMentionsReleaseCommitSha(repoRoot.resolve("docs/troubleshooting-guide.md"));
        assertMarkdownMentionsReleaseCommitSha(repoRoot.resolve("docs/troubleshooting-guide.zh-CN.md"));
    }

    private static void assertReadmeSnapshotExamplesMatch(Path readme, String revision) throws Exception {
        String content = new String(Files.readAllBytes(readme), StandardCharsets.UTF_8);
        Matcher matcher = SNAPSHOT_PLUGIN_COORDINATE.matcher(content);
        int count = 0;
        while (matcher.find()) {
            count++;
            assertEquals(revision, matcher.group(1), readme + " contains stale snapshot plugin version");
        }
        assertTrue(count > 0, readme + " should include at least one snapshot plugin example");

        matcher = CURRENT_SNAPSHOT_REFERENCE.matcher(content);
        count = 0;
        while (matcher.find()) {
            count++;
            String snapshotVersion = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            assertEquals(revision, snapshotVersion, readme + " contains stale current snapshot version");
        }
        assertTrue(count > 0, readme + " should include at least one current snapshot reference");
    }

    private static void assertReadmeCommandSectionIncludes(Path readme, String startHeading, String endHeading,
                                                           Set<String> cliCommands) throws Exception {
        String content = new String(Files.readAllBytes(readme), StandardCharsets.UTF_8);
        int start = content.indexOf(startHeading);
        int end = content.indexOf(endHeading);
        assertTrue(start >= 0, readme + " should contain " + startHeading);
        assertTrue(end > start, readme + " should contain " + endHeading + " after " + startHeading);

        String commandsSection = content.substring(start, end);
        Set<String> documentedCommands = markdownCodeValues(commandsSection);
        Set<String> missingCommands = new TreeSet<String>(cliCommands);
        missingCommands.removeAll(documentedCommands);
        assertEquals(new TreeSet<String>(), missingCommands, readme + " Commands section is missing CLI commands");
    }

    private static void assertMarkdownMentionsCommands(Path document, Set<String> cliCommands) throws Exception {
        String content = read(document);
        Set<String> missingCommands = new TreeSet<String>(cliCommands);
        for (String command : cliCommands) {
            if (content.contains(command)) {
                missingCommands.remove(command);
            }
        }
        assertEquals(new TreeSet<String>(), missingCommands, document + " is missing CLI command references");
    }

    private static void assertMarkdownMentionsReleaseCommitSha(Path document) throws Exception {
        assertTrue(read(document).contains("release_commit_sha"),
            document + " should document the Publish Release retry input");
    }

    private static Set<String> cliCommandNames(Path repoRoot) throws Exception {
        final Set<String> commands = new TreeSet<String>();
        try (Stream<Path> files = Files.walk(repoRoot.resolve("src/main/java/io/github/sonofmagic/javachanges/core"))) {
            files.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
                try {
                    Matcher matcher = COMMAND_ANNOTATION.matcher(
                        new String(Files.readAllBytes(path), StandardCharsets.UTF_8)
                    );
                    while (matcher.find()) {
                        String command = matcher.group(1);
                        if (!"javachanges".equals(command)) {
                            commands.add(command);
                        }
                    }
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            });
        } catch (RuntimeException exception) {
            if (exception.getCause() instanceof Exception) {
                throw (Exception) exception.getCause();
            }
            throw exception;
        }
        return commands;
    }

    private static Set<String> markdownCodeValues(String content) {
        Matcher matcher = MARKDOWN_CODE.matcher(content);
        Set<String> values = new TreeSet<String>();
        while (matcher.find()) {
            values.add(matcher.group(1));
        }
        return values;
    }

    private static String read(Path path) throws Exception {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
