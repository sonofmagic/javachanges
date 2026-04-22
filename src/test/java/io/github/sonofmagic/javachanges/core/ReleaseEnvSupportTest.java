package io.github.sonofmagic.javachanges.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReleaseEnvSupportTest {

    @Test
    void doctorPlatformGitlabDetectsProtectedVariableAndSnapshotBranchMismatch(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot.resolve(".changesets"));
        Files.createDirectories(repoRoot.resolve("env"));
        Files.write(repoRoot.resolve(".changesets").resolve("config.json"), (
            "{\n" +
                "  \"snapshotBranch\": \"snapshot-prod\"\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve("env").resolve("release.env.local"), envFile().getBytes(StandardCharsets.UTF_8));

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        FakeReleaseEnvRuntime runtime = new FakeReleaseEnvRuntime(repoRoot);
        ReleaseEnvSupport support = new ReleaseEnvSupport(repoRoot, new PrintStream(stdout, true), runtime);
        Map<String, String> options = new LinkedHashMap<String, String>();
        options.put("env-file", "env/release.env.local");
        options.put("platform", "gitlab");
        options.put("gitlab-repo", "group/project");
        options.put("format", "json");

        boolean ok = support.doctorPlatform(DoctorPlatformRequest.fromOptions(options));
        String json = stdout.toString(StandardCharsets.UTF_8.name());

        assertFalse(ok);
        assertTrue(json.contains("\"ok\":false"));
        assertTrue(json.contains("UNPROTECTED_WITH_PROTECTED_VARIABLES"));
        assertTrue(json.contains("snapshot-prod"));
        assertTrue(json.contains("MAVEN_REPOSITORY_USERNAME"));
    }

    @Test
    void renderVarsJsonIncludesGitlabAndGithubSections(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot.resolve("env"));
        Files.write(repoRoot.resolve("env").resolve("release.env.local"), envFile().getBytes(StandardCharsets.UTF_8));

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ReleaseEnvSupport support = new ReleaseEnvSupport(repoRoot, new PrintStream(stdout, true));
        Map<String, String> options = new LinkedHashMap<String, String>();
        options.put("env-file", "env/release.env.local");
        options.put("platform", "all");
        options.put("format", "json");

        boolean ok = support.renderVars(PlatformEnvRequest.fromOptions(options));
        String json = stdout.toString(StandardCharsets.UTF_8.name());

        assertTrue(ok);
        assertTrue(json.contains("\"ok\":true"));
        assertTrue(json.contains("\"command\":\"render-vars\""));
        assertTrue(json.contains("GitHub Actions Variables"));
        assertTrue(json.contains("GitHub Actions Secrets"));
        assertTrue(json.contains("GitLab CI/CD Variables"));
        assertTrue(json.contains("OPTIONAL (fallback: CI_JOB_TOKEN)"));
    }

    @Test
    void auditVarsJsonIncludesGithubMatchAndSecretPresence(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot.resolve("env"));
        Files.write(repoRoot.resolve("env").resolve("release.env.local"), envFile().getBytes(StandardCharsets.UTF_8));

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        FakeReleaseEnvRuntime runtime = new FakeReleaseEnvRuntime(repoRoot);
        ReleaseEnvSupport support = new ReleaseEnvSupport(repoRoot, new PrintStream(stdout, true), runtime);
        Map<String, String> options = new LinkedHashMap<String, String>();
        options.put("env-file", "env/release.env.local");
        options.put("platform", "github");
        options.put("github-repo", "owner/repo");
        options.put("format", "json");

        boolean ok = support.auditVars(AuditVarsRequest.fromOptions(options));
        String json = stdout.toString(StandardCharsets.UTF_8.name());

        assertTrue(ok);
        assertTrue(json.contains("\"ok\":true"));
        assertTrue(json.contains("\"command\":\"audit-vars\""));
        assertTrue(json.contains("GitHub Audit Preconditions"));
        assertTrue(json.contains("GitHub Variables 审计"));
        assertTrue(json.contains("GitHub Secrets 审计"));
        assertTrue(json.contains("MATCH (2026-04-22T10:00:00Z)"));
        assertTrue(json.contains("PRESENT (2026-04-22T10:05:00Z)"));
    }

    @Test
    void syncVarsDryRunPrintsGithubAndGitlabCommands(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot.resolve("env"));
        Files.write(repoRoot.resolve("env").resolve("release.env.local"), envFile().getBytes(StandardCharsets.UTF_8));

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ReleaseEnvSupport support = new ReleaseEnvSupport(repoRoot, new PrintStream(stdout, true));
        Map<String, String> options = new LinkedHashMap<String, String>();
        options.put("env-file", "env/release.env.local");
        options.put("platform", "all");
        options.put("repo", "owner/repo");
        options.put("show-secrets", "true");

        support.syncVars(SyncVarsRequest.fromOptions(options));
        String text = stdout.toString(StandardCharsets.UTF_8.name());

        assertTrue(text.contains("== GitHub CLI 命令 =="));
        assertTrue(text.contains("gh variable set MAVEN_RELEASE_REPOSITORY_URL"));
        assertTrue(text.contains("gh secret set MAVEN_REPOSITORY_USERNAME"));
        assertTrue(text.contains("== GitLab CLI 命令 =="));
        assertTrue(text.contains("glab variable set MAVEN_REPOSITORY_USERNAME"));
        assertTrue(text.contains("--masked --protected"));
    }

    private static String envFile() {
        return ""
            + "MAVEN_RELEASE_REPOSITORY_URL=https://repo.example.com/releases\n"
            + "MAVEN_SNAPSHOT_REPOSITORY_URL=https://repo.example.com/snapshots\n"
            + "MAVEN_RELEASE_REPOSITORY_ID=maven-releases\n"
            + "MAVEN_SNAPSHOT_REPOSITORY_ID=maven-snapshots\n"
            + "MAVEN_REPOSITORY_USERNAME=ci-user\n"
            + "MAVEN_REPOSITORY_PASSWORD=ci-password\n"
            + "GITLAB_RELEASE_TOKEN=glrt-token\n";
    }

    private static final class FakeReleaseEnvRuntime extends ReleaseEnvRuntime {
        FakeReleaseEnvRuntime(Path repoRoot) {
            super(repoRoot);
        }

        @Override
        boolean commandExists(String command) {
            return true;
        }

        @Override
        void requireCommand(String command) {
        }

        @Override
        boolean runQuietly(java.util.List<String> command) {
            return true;
        }

        @Override
        CommandResult runAndCapture(java.util.List<String> command) throws IOException {
            String joined = command.toString();
            if (joined.contains("gh, variable, list")) {
                return json("["
                    + "{\"name\":\"MAVEN_RELEASE_REPOSITORY_URL\",\"value\":\"https://repo.example.com/releases\",\"updatedAt\":\"2026-04-22T10:00:00Z\"},"
                    + "{\"name\":\"MAVEN_SNAPSHOT_REPOSITORY_URL\",\"value\":\"https://repo.example.com/snapshots\",\"updatedAt\":\"2026-04-22T10:00:00Z\"},"
                    + "{\"name\":\"MAVEN_RELEASE_REPOSITORY_ID\",\"value\":\"maven-releases\",\"updatedAt\":\"2026-04-22T10:00:00Z\"},"
                    + "{\"name\":\"MAVEN_SNAPSHOT_REPOSITORY_ID\",\"value\":\"maven-snapshots\",\"updatedAt\":\"2026-04-22T10:00:00Z\"}"
                    + "]");
            }
            if (joined.contains("gh, secret, list")) {
                return json("["
                    + "{\"name\":\"MAVEN_REPOSITORY_USERNAME\",\"updatedAt\":\"2026-04-22T10:05:00Z\"},"
                    + "{\"name\":\"MAVEN_REPOSITORY_PASSWORD\",\"updatedAt\":\"2026-04-22T10:05:00Z\"}"
                    + "]");
            }
            if (joined.contains("/variables?per_page=100")) {
                return json("["
                    + "{\"key\":\"MAVEN_REPOSITORY_USERNAME\",\"protected\":true,\"masked\":true},"
                    + "{\"key\":\"MAVEN_REPOSITORY_PASSWORD\",\"protected\":true,\"masked\":true},"
                    + "{\"key\":\"GITLAB_RELEASE_TOKEN\",\"protected\":true,\"masked\":true}"
                    + "]");
            }
            if (joined.contains("/protected_branches?per_page=100")) {
                return json("[]");
            }
            throw new IllegalStateException("Unexpected command: " + Arrays.asList(command));
        }

        private CommandResult json(String value) {
            return new CommandResult(0, value.getBytes(StandardCharsets.UTF_8), new byte[0]);
        }
    }
}
