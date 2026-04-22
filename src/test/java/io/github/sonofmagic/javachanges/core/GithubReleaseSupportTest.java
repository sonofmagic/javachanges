package io.github.sonofmagic.javachanges.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GithubReleaseSupportTest {

    @Test
    void planPullRequestCreatesPullRequestWhenNoOpenPr(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir);
        RecordingRuntime runtime = new RecordingRuntime(repoRoot);
        runtime.noStagedChanges = false;
        runtime.openPullRequestNumber = null;
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();

        new GithubReleaseSupport(repoRoot, new PrintStream(stdout, true), runtime)
            .planPullRequest(planRequest());

        assertTrue(runtime.configuredBotIdentity);
        assertEquals("changeset-release/main", runtime.switchedBranch);
        assertEquals("pom.xml,CHANGELOG.md,.changesets", runtime.addedPaths);
        assertEquals("chore(release): apply changesets for v1.2.0", runtime.commitMessage);
        assertEquals("owner/repo", runtime.prRepo);
        assertEquals("changeset-release/main", runtime.prHeadBranch);
        assertEquals("main", runtime.prBaseBranch);
        assertEquals("chore(release): v1.2.0", runtime.prTitle);
        assertTrue(runtime.prBodyFile.endsWith(".changesets/release-plan.md"));
        assertTrue(stdout.toString(StandardCharsets.UTF_8.name()).contains("Created GitHub PR for chore(release): v1.2.0"));
    }

    @Test
    void tagFromReleasePlanCreatesAndPushesTag(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir);
        Files.write(
            repoRoot.resolve(".changesets").resolve("release-plan.json"),
            "{\"releaseVersion\":\"1.2.0\"}\n".getBytes(StandardCharsets.UTF_8)
        );
        RecordingRuntime runtime = new RecordingRuntime(repoRoot);
        runtime.headSha = "abc1234";
        runtime.remoteTagExists = false;
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();

        Map<String, String> options = new HashMap<String, String>();
        options.put("current-sha", "abc1234");
        options.put("execute", "true");

        new GithubReleaseSupport(repoRoot, new PrintStream(stdout, true), runtime)
            .tagFromReleasePlan(GithubTagRequest.fromOptions(options));

        assertEquals("v1.2.0", runtime.tagName);
        assertEquals("abc1234", runtime.tagSha);
        assertEquals("origin", runtime.pushedRemote);
        assertEquals("refs/tags/v1.2.0", runtime.pushedRef);
        assertTrue(stdout.toString(StandardCharsets.UTF_8.name()).contains("Created and pushed tag v1.2.0"));
    }

    @Test
    void syncReleaseFromPlanUpdatesExistingReleaseAndWritesGithubOutputs(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir);
        run(repoRoot, "git", "init", "-q", "-b", "main");
        run(repoRoot, "git", "config", "user.name", "tester");
        run(repoRoot, "git", "config", "user.email", "tester@example.com");
        run(repoRoot, "git", "add", "pom.xml", "CHANGELOG.md", ".changesets");
        run(repoRoot, "git", "commit", "-qm", "init");
        Files.write(
            repoRoot.resolve(".changesets").resolve("release-plan.json"),
            "{\"releaseVersion\":\"1.2.0\"}\n".getBytes(StandardCharsets.UTF_8)
        );
        run(repoRoot, "git", "tag", "v1.2.0");
        Path githubOutput = tempDir.resolve("github-output.txt");
        RecordingRuntime runtime = new RecordingRuntime(repoRoot);
        runtime.releaseExists = true;
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();

        Map<String, String> options = new HashMap<String, String>();
        options.put("release-notes-file", "target/release-notes.md");
        options.put("github-output-file", githubOutput.toString());
        options.put("execute", "true");

        new GithubReleaseSupport(repoRoot, new PrintStream(stdout, true), runtime)
            .syncReleaseFromPlan(GithubReleasePublishRequest.fromOptions(options));

        assertEquals("v1.2.0", runtime.updatedReleaseTag);
        assertTrue(runtime.updatedReleaseNotesFile.endsWith("target/release-notes.md"));
        assertTrue(Files.exists(repoRoot.resolve("target").resolve("release-notes.md")));
        assertTrue(new String(Files.readAllBytes(githubOutput), StandardCharsets.UTF_8).contains("release_tag=v1.2.0"));
        assertTrue(stdout.toString(StandardCharsets.UTF_8.name()).contains("Updated GitHub Release v1.2.0"));
    }

    @Test
    void planPullRequestJsonIncludesMachineReadableFields(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir);
        RecordingRuntime runtime = new RecordingRuntime(repoRoot);
        runtime.noStagedChanges = false;
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();

        Map<String, String> options = new HashMap<String, String>();
        options.put("github-repo", "owner/repo");
        options.put("target-branch", "main");
        options.put("release-branch", "changeset-release/main");
        options.put("format", "json");

        new GithubReleaseSupport(repoRoot, new PrintStream(stdout, true), runtime)
            .planPullRequest(GithubReleasePlanRequest.fromOptions(options));

        String json = stdout.toString(StandardCharsets.UTF_8.name());
        assertTrue(json.contains("\"ok\":true"));
        assertTrue(json.contains("\"command\":\"github-release-plan\""));
        assertTrue(json.contains("\"action\":\"plan-pull-request\""));
        assertTrue(json.contains("\"reason\":\"Dry-run only.\""));
        assertTrue(json.contains("\"releaseVersion\":\"1.2.0\""));
    }

    @Test
    void tagFromReleasePlanJsonIncludesMachineReadableFields(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir);
        Files.write(
            repoRoot.resolve(".changesets").resolve("release-plan.json"),
            "{\"releaseVersion\":\"1.2.0\"}\n".getBytes(StandardCharsets.UTF_8)
        );
        RecordingRuntime runtime = new RecordingRuntime(repoRoot);
        runtime.headSha = "abc1234";
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();

        Map<String, String> options = new HashMap<String, String>();
        options.put("format", "json");

        new GithubReleaseSupport(repoRoot, new PrintStream(stdout, true), runtime)
            .tagFromReleasePlan(GithubTagRequest.fromOptions(options));

        String json = stdout.toString(StandardCharsets.UTF_8.name());
        assertTrue(json.contains("\"ok\":true"));
        assertTrue(json.contains("\"command\":\"github-tag-from-plan\""));
        assertTrue(json.contains("\"tag\":\"v1.2.0\""));
        assertTrue(json.contains("\"releaseVersion\":\"1.2.0\""));
        assertTrue(json.contains("\"reason\":\"Dry-run only.\""));
    }

    @Test
    void syncReleaseFromPlanJsonIncludesMachineReadableFields(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir);
        run(repoRoot, "git", "init", "-q", "-b", "main");
        run(repoRoot, "git", "config", "user.name", "tester");
        run(repoRoot, "git", "config", "user.email", "tester@example.com");
        run(repoRoot, "git", "add", "pom.xml", "CHANGELOG.md", ".changesets");
        run(repoRoot, "git", "commit", "-qm", "init");
        Files.write(
            repoRoot.resolve(".changesets").resolve("release-plan.json"),
            "{\"releaseVersion\":\"1.2.0\"}\n".getBytes(StandardCharsets.UTF_8)
        );
        run(repoRoot, "git", "tag", "v1.2.0");
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();

        Map<String, String> options = new HashMap<String, String>();
        options.put("format", "json");

        new GithubReleaseSupport(repoRoot, new PrintStream(stdout, true), new RecordingRuntime(repoRoot))
            .syncReleaseFromPlan(GithubReleasePublishRequest.fromOptions(options));

        String json = stdout.toString(StandardCharsets.UTF_8.name());
        assertTrue(json.contains("\"ok\":true"));
        assertTrue(json.contains("\"command\":\"github-release-from-plan\""));
        assertTrue(json.contains("\"tag\":\"v1.2.0\""));
        assertTrue(json.contains("\"releaseVersion\":\"1.2.0\""));
        assertTrue(json.contains("\"releaseNotesFile\":"));
        assertTrue(json.contains("\"reason\":\"Dry-run only.\""));
    }

    private static Path createRepository(Path tempDir) throws IOException {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot.resolve(".changesets"));
        Files.write(repoRoot.resolve("pom.xml"), singleModulePom().getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve("CHANGELOG.md"), "# Changelog\n\n".getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve(".changesets").resolve("README.md"), "# Changesets\n".getBytes(StandardCharsets.UTF_8));
        Files.write(
            repoRoot.resolve(".changesets").resolve("minor-release.md"),
            ("---\n" +
                "\"fixture-app\": minor\n" +
                "---\n" +
                "\n" +
                "publish a reviewed github release plan\n").getBytes(StandardCharsets.UTF_8)
        );
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

    private static GithubReleasePlanRequest planRequest() {
        Map<String, String> options = new HashMap<String, String>();
        options.put("github-repo", "owner/repo");
        options.put("target-branch", "main");
        options.put("release-branch", "changeset-release/main");
        options.put("execute", "true");
        return GithubReleasePlanRequest.fromOptions(options);
    }

    private static void run(Path workingDirectory, String... command) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workingDirectory.toFile());
        Process process = builder.start();
        byte[] stdout = ReleaseUtils.readAllBytes(process.getInputStream());
        byte[] stderr = ReleaseUtils.readAllBytes(process.getErrorStream());
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("Command failed with exit code " + exitCode
                + "\nstdout: " + new String(stdout, StandardCharsets.UTF_8)
                + "\nstderr: " + new String(stderr, StandardCharsets.UTF_8));
        }
    }

    private static final class RecordingRuntime extends GithubReleaseRuntime {
        boolean configuredBotIdentity;
        boolean noStagedChanges;
        String openPullRequestNumber;
        String switchedBranch;
        String addedPaths;
        String commitMessage;
        String prRepo;
        String prHeadBranch;
        String prBaseBranch;
        String prTitle;
        String prBodyFile;
        String headSha;
        boolean remoteTagExists;
        String tagName;
        String tagSha;
        String pushedRemote;
        String pushedRef;
        boolean releaseExists;
        String updatedReleaseTag;
        String updatedReleaseNotesFile;

        RecordingRuntime(Path repoRoot) {
            super(repoRoot);
        }

        @Override
        void configureBotIdentity() {
            configuredBotIdentity = true;
        }

        @Override
        boolean hasNoStagedChanges() {
            return noStagedChanges;
        }

        @Override
        void runGit(String... args) {
            if (args.length >= 3 && "switch".equals(args[0]) && "-C".equals(args[1])) {
                switchedBranch = args[2];
                return;
            }
            if (args.length >= 4 && "add".equals(args[0])) {
                addedPaths = args[1] + "," + args[2] + "," + args[3];
                return;
            }
            if (args.length >= 3 && "commit".equals(args[0]) && "-m".equals(args[1])) {
                commitMessage = args[2];
                return;
            }
            if (args.length >= 3 && "push".equals(args[0])) {
                pushedRemote = args[1];
                pushedRef = args[2];
            }
        }

        @Override
        String findOpenPullRequestNumber(String githubRepo, String headBranch, String baseBranch) {
            prRepo = githubRepo;
            prHeadBranch = headBranch;
            prBaseBranch = baseBranch;
            return openPullRequestNumber;
        }

        @Override
        void createPullRequest(String githubRepo, String headBranch, String baseBranch, String title, Path bodyFile) {
            prRepo = githubRepo;
            prHeadBranch = headBranch;
            prBaseBranch = baseBranch;
            prTitle = title;
            prBodyFile = bodyFile.toString().replace('\\', '/');
        }

        @Override
        void updatePullRequest(String githubRepo, String prNumber, String title, Path bodyFile) {
            prRepo = githubRepo;
            prTitle = title;
            prBodyFile = bodyFile.toString().replace('\\', '/');
            openPullRequestNumber = prNumber;
        }

        @Override
        String headSha() {
            return headSha;
        }

        @Override
        boolean remoteTagExists(String tagName, String remoteName) {
            return remoteTagExists;
        }

        @Override
        void createOrUpdateTag(String tagName, String sha) {
            this.tagName = tagName;
            this.tagSha = sha;
        }

        @Override
        boolean releaseExists(String tagName) {
            return releaseExists;
        }

        @Override
        void updateRelease(String tagName, Path notesFile) {
            updatedReleaseTag = tagName;
            updatedReleaseNotesFile = notesFile.toString();
        }
    }
}
