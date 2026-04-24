package io.github.sonofmagic.javachanges.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReleaseEnvSupportTest {

    @Test
    void initEnvCopiesTemplateAndPrintsNextStep(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot.resolve("env"));
        Files.write(repoRoot.resolve("env").resolve("release.env.example"),
            ReleaseEnvTestFixtures.supportEnvFile().getBytes(StandardCharsets.UTF_8));

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ReleaseEnvSupport support = new ReleaseEnvSupport(repoRoot, new PrintStream(stdout, true));
        Map<String, String> options = new LinkedHashMap<String, String>();
        options.put("template", "env/release.env.example");
        options.put("target", "env/release.env.local");

        support.initEnv(InitEnvRequest.fromOptions(options));

        String text = stdout.toString(StandardCharsets.UTF_8.name());
        assertTrue(Files.exists(repoRoot.resolve("env").resolve("release.env.local")));
        assertTrue(text.contains("已生成本地 env 文件: env/release.env.local"));
        assertTrue(text.contains("下一步请编辑真实仓库地址和凭据，然后执行: make readiness"));
    }

    @Test
    void authHelpPrintsGithubAndGitlabGuidance(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot);

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ReleaseEnvSupport support = new ReleaseEnvSupport(repoRoot, new PrintStream(stdout, true));

        support.printAuthHelp(Platform.ALL);

        String text = stdout.toString(StandardCharsets.UTF_8.name());
        assertTrue(text.contains("== GitHub CLI 登录建议 =="));
        assertTrue(text.contains("gh auth login --web --git-protocol ssh"));
        assertTrue(text.contains("== GitLab CLI 登录建议 =="));
        assertTrue(text.contains("glab auth login --hostname gitlab.example.com --web --git-protocol ssh --use-keyring"));
    }

    @Test
    void doctorPlatformGitlabDetectsProtectedVariableAndSnapshotBranchMismatch(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot.resolve(".changesets"));
        Files.write(repoRoot.resolve(".changesets").resolve("config.json"), (
            "{\n" +
                "  \"snapshotBranch\": \"snapshot-prod\"\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));
        ReleaseEnvTestFixtures.writeLocalEnv(repoRoot, ReleaseEnvTestFixtures.supportEnvFile());

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ReleaseEnvTestFixtures.FakeReleaseEnvRuntime runtime = new ReleaseEnvTestFixtures.FakeReleaseEnvRuntime(repoRoot);
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
        ReleaseEnvTestFixtures.writeLocalEnv(repoRoot, ReleaseEnvTestFixtures.supportEnvFile());

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
        ReleaseEnvTestFixtures.writeLocalEnv(repoRoot, ReleaseEnvTestFixtures.supportEnvFile());

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ReleaseEnvTestFixtures.FakeReleaseEnvRuntime runtime = new ReleaseEnvTestFixtures.FakeReleaseEnvRuntime(repoRoot);
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
        ReleaseEnvTestFixtures.writeLocalEnv(repoRoot, ReleaseEnvTestFixtures.supportEnvFile());

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
}
