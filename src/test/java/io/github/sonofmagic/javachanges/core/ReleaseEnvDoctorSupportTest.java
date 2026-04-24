package io.github.sonofmagic.javachanges.core;

import io.github.sonofmagic.javachanges.core.env.DoctorPlatformRequest;
import io.github.sonofmagic.javachanges.core.env.LocalDoctorRequest;
import io.github.sonofmagic.javachanges.core.env.ReleaseEnvSupport;
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

class ReleaseEnvDoctorSupportTest {

    @Test
    void doctorLocalJsonReportsMissingEnvFile(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot);

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ReleaseEnvSupport support = new ReleaseEnvSupport(repoRoot, new PrintStream(stdout, true),
            new ReleaseEnvTestFixtures.FakeReleaseEnvRuntime(repoRoot));
        Map<String, String> options = new LinkedHashMap<String, String>();
        options.put("env-file", "env/release.env.local");
        options.put("format", "json");

        boolean ok = support.doctorLocal(LocalDoctorRequest.fromOptions(options));
        String json = stdout.toString(StandardCharsets.UTF_8.name());

        assertFalse(ok);
        assertTrue(json.contains("\"ok\":false"));
        assertTrue(json.contains("\"command\":\"doctor-local\""));
        assertTrue(json.contains("\"error\":\"本机发布环境未就绪\""));
        assertTrue(json.contains("env/release.env.local"));
    }

    @Test
    void doctorPlatformGitlabJsonReportsProtectionMismatch(@TempDir Path tempDir) throws Exception {
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
        assertTrue(json.contains("\"command\":\"doctor-platform\""));
        assertTrue(json.contains("UNPROTECTED_WITH_PROTECTED_VARIABLES"));
        assertTrue(json.contains("snapshot-prod"));
    }
}
