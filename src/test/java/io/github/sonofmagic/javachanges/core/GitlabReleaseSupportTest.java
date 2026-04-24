package io.github.sonofmagic.javachanges.core;

import io.github.sonofmagic.javachanges.core.gitlab.GitlabMergeRequestClient;
import io.github.sonofmagic.javachanges.core.gitlab.GitlabReleasePlanRequest;
import io.github.sonofmagic.javachanges.core.gitlab.GitlabReleaseRequest;
import io.github.sonofmagic.javachanges.core.gitlab.GitlabReleaseRuntime;
import io.github.sonofmagic.javachanges.core.gitlab.GitlabReleaseSupport;
import io.github.sonofmagic.javachanges.core.gitlab.GitlabTagRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitlabReleaseSupportTest {

    @Test
    void planMergeRequestOverwritesExistingRemoteBranchAndCreatesMrWhenNoOpenMr(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir);
        RecordingRuntime runtime = new RecordingRuntime(repoRoot);
        runtime.noStagedChanges = false;
        runtime.remoteBranchHead = "abc123";
        FakeGitlabApiClient api = new FakeGitlabApiClient();
        api.remoteUrl = "https://bot:token@example.com/group/repo.git";
        api.openMergeRequestIid = null;
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();

        new GitlabReleaseSupport(repoRoot, new PrintStream(stdout, true), runtime, api)
            .planMergeRequest(planRequest());

        assertTrue(runtime.configuredBotIdentity);
        assertEquals("changeset-release/main", runtime.checkedOutBranch);
        assertEquals("pom.xml,CHANGELOG.md,.changesets", runtime.addedPaths);
        assertEquals("chore(release): apply changesets for v1.2.0", runtime.commitMessage);
        assertEquals("changeset-release/main", runtime.remoteBranchHeadBranch);
        assertEquals("abc123", runtime.pushExpectedOldSha);
        assertEquals("changeset-release/main", api.createdSourceBranch);
        assertEquals("main", api.createdTargetBranch);
        assertEquals("chore(release): release v1.2.0", api.createdTitle);
        assertTrue(api.createdDescription.contains("## Release Plan"));
        assertTrue(stdout.toString(StandardCharsets.UTF_8.name()).contains("Created GitLab MR !42"));
    }

    @Test
    void planMergeRequestUpdatesExistingMrAndPushesWithoutLeaseWhenRemoteBranchDoesNotExist(@TempDir Path tempDir)
        throws Exception {
        Path repoRoot = createRepository(tempDir);
        RecordingRuntime runtime = new RecordingRuntime(repoRoot);
        runtime.noStagedChanges = false;
        runtime.remoteBranchHead = null;
        FakeGitlabApiClient api = new FakeGitlabApiClient();
        api.remoteUrl = "https://bot:token@example.com/group/repo.git";
        api.openMergeRequestIid = Integer.valueOf(7);
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();

        new GitlabReleaseSupport(repoRoot, new PrintStream(stdout, true), runtime, api)
            .planMergeRequest(planRequest());

        assertEquals(null, runtime.pushExpectedOldSha);
        assertEquals(Integer.valueOf(7), api.updatedMergeRequestIid);
        assertEquals("chore(release): release v1.2.0", api.updatedTitle);
        assertTrue(api.updatedDescription.contains("## Release Plan"));
        assertTrue(stdout.toString(StandardCharsets.UTF_8.name()).contains("Updated GitLab MR !7"));
    }

    @Test
    void syncReleaseWritesReleaseNotesAndCreatesGitlabRelease(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir);
        run(repoRoot, "git", "init", "-q", "-b", "main");
        run(repoRoot, "git", "config", "user.name", "tester");
        run(repoRoot, "git", "config", "user.email", "tester@example.com");
        run(repoRoot, "git", "add", "pom.xml", "CHANGELOG.md", ".changesets");
        run(repoRoot, "git", "commit", "-qm", "init");
        run(repoRoot, "git", "tag", "fixture-app/v1.1.1");
        FakeGitlabApiClient api = new FakeGitlabApiClient();
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();

        Map<String, String> options = new HashMap<String, String>();
        options.put("project-id", "123");
        options.put("tag", "fixture-app/v1.1.1");
        options.put("execute", "true");

        new GitlabReleaseSupport(repoRoot, new PrintStream(stdout, true), new RecordingRuntime(repoRoot), api)
            .syncRelease(GitlabReleaseRequest.fromOptions(options));

        assertEquals("123", api.releaseProjectId);
        assertEquals("fixture-app/v1.1.1", api.releaseTag);
        assertTrue(api.releaseDescription.contains("## 1.1.1 - "));
        assertTrue(stdout.toString(StandardCharsets.UTF_8.name()).contains("Created GitLab Release fixture-app/v1.1.1"));
        assertTrue(Files.exists(repoRoot.resolve("target").resolve("release-notes.md")));
    }

    @Test
    void syncReleaseJsonIncludesMachineReadableFields(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir);
        run(repoRoot, "git", "init", "-q", "-b", "main");
        run(repoRoot, "git", "config", "user.name", "tester");
        run(repoRoot, "git", "config", "user.email", "tester@example.com");
        run(repoRoot, "git", "add", "pom.xml", "CHANGELOG.md", ".changesets");
        run(repoRoot, "git", "commit", "-qm", "init");
        run(repoRoot, "git", "tag", "fixture-app/v1.1.1");
        FakeGitlabApiClient api = new FakeGitlabApiClient();
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();

        Map<String, String> options = new HashMap<String, String>();
        options.put("project-id", "123");
        options.put("tag", "fixture-app/v1.1.1");
        options.put("format", "json");

        new GitlabReleaseSupport(repoRoot, new PrintStream(stdout, true), new RecordingRuntime(repoRoot), api)
            .syncRelease(GitlabReleaseRequest.fromOptions(options));

        String json = stdout.toString(StandardCharsets.UTF_8.name());
        assertTrue(json.contains("\"ok\":true"));
        assertTrue(json.contains("\"command\":\"gitlab-release\""));
        assertTrue(json.contains("\"releaseVersion\":\"1.1.1\""));
        assertTrue(json.contains("\"releaseModule\":\"fixture-app\""));
        assertTrue(json.contains("\"tag\":\"fixture-app/v1.1.1\""));
        assertTrue(json.contains("\"projectId\":\"123\""));
        assertTrue(json.contains("\"releaseNotesFile\":"));
    }

    @Test
    void tagFromReleasePlanCreatesPerModuleTags(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir);
        Files.write(
            repoRoot.resolve(".changesets").resolve("release-plan.json"),
            ("{\n" +
                "  \"releaseVersion\": \"1.2.0\",\n" +
                "  \"tagStrategy\": \"per-module\",\n" +
                "  \"releaseTargets\": [\n" +
                "    {\"module\": \"fixture-app\", \"tag\": \"fixture-app/v1.2.0\"},\n" +
                "    {\"module\": \"fixture-starter\", \"tag\": \"fixture-starter/v1.2.0\"}\n" +
                "  ]\n" +
                "}\n").getBytes(StandardCharsets.UTF_8)
        );
        RecordingRuntime runtime = new RecordingRuntime(repoRoot);
        FakeGitlabApiClient api = new FakeGitlabApiClient();
        api.remoteUrl = "https://bot:token@example.com/group/repo.git";
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();

        Map<String, String> options = new HashMap<String, String>();
        options.put("before-sha", "abc111");
        options.put("current-sha", "abc222");
        options.put("execute", "true");

        new GitlabReleaseSupport(repoRoot, new PrintStream(stdout, true), runtime, api)
            .tagFromReleasePlan(GitlabTagRequest.fromOptions(options));

        assertEquals(2, runtime.createdTags.size());
        assertEquals("tag fixture-app/v1.2.0 abc222", runtime.createdTags.get(0));
        assertEquals("push https://bot:token@example.com/group/repo.git refs/tags/fixture-app/v1.2.0", runtime.pushes.get(0));
        assertTrue(stdout.toString(StandardCharsets.UTF_8.name()).contains("fixture-starter/v1.2.0"));
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
                "publish a reviewed gitlab release plan\n").getBytes(StandardCharsets.UTF_8)
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

    private static GitlabReleasePlanRequest planRequest() {
        Map<String, String> options = new HashMap<String, String>();
        options.put("project-id", "123");
        options.put("target-branch", "main");
        options.put("release-branch", "changeset-release/main");
        options.put("execute", "true");
        return GitlabReleasePlanRequest.fromOptions(options);
    }

    private static void run(Path workingDirectory, String... command) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workingDirectory.toFile());
        Process process = builder.start();
        byte[] stdout = ReleaseProcessUtils.readAllBytes(process.getInputStream());
        byte[] stderr = ReleaseProcessUtils.readAllBytes(process.getErrorStream());
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("Command failed with exit code " + exitCode
                + "\nstdout: " + new String(stdout, StandardCharsets.UTF_8)
                + "\nstderr: " + new String(stderr, StandardCharsets.UTF_8));
        }
    }

    private static final class RecordingRuntime extends GitlabReleaseRuntime {
        boolean configuredBotIdentity;
        boolean noStagedChanges;
        String remoteBranchHead;
        String checkedOutBranch;
        String addedPaths;
        String commitMessage;
        String remoteBranchHeadBranch;
        String pushExpectedOldSha;
        List<String> createdTags = new ArrayList<String>();
        List<String> pushes = new ArrayList<String>();

        RecordingRuntime(Path repoRoot) {
            super(repoRoot);
        }

        @Override
        public void configureBotIdentity() {
            configuredBotIdentity = true;
        }

        @Override
        public boolean hasNoStagedChanges() {
            return noStagedChanges;
        }

        @Override
        public void runGit(String... args) {
            if (args.length >= 3 && "checkout".equals(args[0]) && "-B".equals(args[1])) {
                checkedOutBranch = args[2];
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
            if (args.length >= 3 && "tag".equals(args[0])) {
                createdTags.add(args[0] + " " + args[1] + " " + args[2]);
                return;
            }
            if (args.length >= 3 && "push".equals(args[0])) {
                pushes.add(args[0] + " " + args[1] + " " + args[2]);
            }
        }

        @Override
        public String remoteBranchHead(String branchName, String remoteUrl) {
            remoteBranchHeadBranch = branchName;
            return remoteBranchHead;
        }

        @Override
        public void pushReleaseBranch(String remoteUrl, String releaseBranch, String expectedOldSha) {
            pushExpectedOldSha = expectedOldSha;
        }
    }

    private static final class FakeGitlabApiClient implements GitlabMergeRequestClient {
        String remoteUrl;
        Integer openMergeRequestIid;
        String createdSourceBranch;
        String createdTargetBranch;
        String createdTitle;
        String createdDescription;
        Integer updatedMergeRequestIid;
        String updatedTitle;
        String updatedDescription;
        String releaseProjectId;
        String releaseTag;
        String releaseName;
        String releaseDescription;
        boolean releaseExists;

        @Override
        public Integer findOpenMergeRequestIid(String projectId, String sourceBranch, String targetBranch) {
            return openMergeRequestIid;
        }

        @Override
        public String createMergeRequest(String projectId, String sourceBranch, String targetBranch,
                                         String title, String description) {
            createdSourceBranch = sourceBranch;
            createdTargetBranch = targetBranch;
            createdTitle = title;
            createdDescription = description;
            return "{\"iid\":42}";
        }

        @Override
        public void updateMergeRequest(String projectId, int mergeRequestIid, String title, String description) {
            updatedMergeRequestIid = Integer.valueOf(mergeRequestIid);
            updatedTitle = title;
            updatedDescription = description;
        }

        @Override
        public String authenticatedRemoteUrl() {
            return remoteUrl;
        }

        @Override
        public int requiredJsonInt(String json, String field) {
            return 42;
        }

        @Override
        public boolean releaseExists(String projectId, String tagName) {
            return releaseExists;
        }

        @Override
        public void createRelease(String projectId, String tagName, String releaseName, String description) {
            this.releaseProjectId = projectId;
            this.releaseTag = tagName;
            this.releaseName = releaseName;
            this.releaseDescription = description;
        }

        @Override
        public void updateRelease(String projectId, String tagName, String releaseName, String description) {
            this.releaseProjectId = projectId;
            this.releaseTag = tagName;
            this.releaseName = releaseName;
            this.releaseDescription = description;
        }
    }
}
