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

    private static final class RecordingRuntime extends GitlabReleaseRuntime {
        boolean configuredBotIdentity;
        boolean noStagedChanges;
        String remoteBranchHead;
        String checkedOutBranch;
        String addedPaths;
        String commitMessage;
        String remoteBranchHeadBranch;
        String pushExpectedOldSha;

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
            }
        }

        @Override
        String remoteBranchHead(String branchName, String remoteUrl) {
            remoteBranchHeadBranch = branchName;
            return remoteBranchHead;
        }

        @Override
        void pushReleaseBranch(String remoteUrl, String releaseBranch, String expectedOldSha) {
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
    }
}
