package io.github.sonofmagic.javachanges.core;

import io.github.sonofmagic.javachanges.core.automation.AutomationJsonSupport;
import io.github.sonofmagic.javachanges.core.publish.PublishPlanSupport;
import io.github.sonofmagic.javachanges.core.publish.PublishRequest;
import io.github.sonofmagic.javachanges.core.publish.PublishRuntime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublishPlanSupportTest {

    @Test
    void resolvePublishTargetUsesTagModuleWhenModuleNotExplicit(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, "fixture-app", "1.2.3-SNAPSHOT");
        PublishPlanSupport support = new PublishPlanSupport(
            repoRoot,
            new PublishRuntime(repoRoot),
            new VersionSupport(repoRoot)
        );

        Map<String, String> options = new LinkedHashMap<String, String>();
        options.put("tag", "fixture-app/v1.2.3");
        PublishRequest request = PublishRequest.fromOptions(options, true);

        PublishPlanSupport.PublishTarget target = support.resolvePublishTarget(request);

        assertEquals("1.2.3", target.publishVersion);
        assertEquals("fixture-app", target.resolvedModule);
    }

    @Test
    void buildDeployCommandIncludesSnapshotRevisionAndModule(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, "fixture-app", "1.2.3-SNAPSHOT");
        PublishPlanSupport support = new PublishPlanSupport(
            repoRoot,
            new PublishRuntime(repoRoot),
            new VersionSupport(repoRoot)
        );

        Map<String, String> options = new LinkedHashMap<String, String>();
        options.put("snapshot", "true");
        options.put("module", "fixture-app");
        options.put("snapshot-build-stamp", "20260422.120000.abc123");
        PublishRequest request = PublishRequest.fromOptions(options, true);
        PublishPlanSupport.PublishTarget target = support.resolvePublishTarget(request);

        List<String> command = support.buildDeployCommand(
            request,
            target,
            new MavenCommand("./mvnw", "wrapper"),
            repoRoot.resolve(".m2/repository"),
            "https://repo.example.com/snapshots"
        );

        assertTrue(command.contains("-Drevision=1.2.3-20260422.120000.abc123-SNAPSHOT"));
        assertTrue(command.contains("-pl"));
        assertTrue(command.contains(":fixture-app"));
        assertTrue(command.contains("-Dmaven.snapshot.repository.id=" + MavenSettingsWriter.snapshotServerId()));
        assertTrue(command.contains("-Dmaven.snapshot.repository.url=https://repo.example.com/snapshots"));
        assertTrue(command.contains("clean"));
        assertTrue(command.contains("deploy"));
    }

    @Test
    void resolvePublishTargetKeepsOriginalRevisionInPlainSnapshotMode(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, "fixture-app", "1.2.3-SNAPSHOT");
        PublishPlanSupport support = new PublishPlanSupport(
            repoRoot,
            new PublishRuntime(repoRoot),
            new VersionSupport(repoRoot)
        );

        Map<String, String> options = new LinkedHashMap<String, String>();
        options.put("snapshot", "true");
        options.put("snapshot-version-mode", "plain");
        PublishRequest request = PublishRequest.fromOptions(options, true);

        PublishPlanSupport.PublishTarget target = support.resolvePublishTarget(request);

        assertEquals("1.2.3-SNAPSHOT", target.publishVersion);
        assertEquals(SnapshotVersionMode.PLAIN, target.snapshotVersionMode);
        assertFalse(target.snapshotBuildStampApplied);
    }

    @Test
    void buildReportIncludesSnapshotModeMetadata(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createRepository(tempDir, "fixture-app", "1.2.3-SNAPSHOT");
        PublishPlanSupport support = new PublishPlanSupport(
            repoRoot,
            new PublishRuntime(repoRoot),
            new VersionSupport(repoRoot)
        );

        Map<String, String> options = new LinkedHashMap<String, String>();
        options.put("snapshot", "true");
        options.put("snapshot-version-mode", "plain");
        PublishRequest request = PublishRequest.fromOptions(options, true);

        PublishPlanSupport.PublishTarget target = support.resolvePublishTarget(request);
        AutomationJsonSupport.AutomationReport report = support.buildReport("publish", request, target);

        assertEquals("1.2.3-SNAPSHOT", report.releaseVersion);
        assertEquals("1.2.3-SNAPSHOT", report.effectiveVersion);
        assertEquals("plain", report.snapshotVersionMode);
        assertFalse(report.snapshotBuildStampApplied);
    }

    private static Path createRepository(Path tempDir, String artifactId, String revision) throws IOException {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot);
        Files.write(repoRoot.resolve("pom.xml"), pomXml(artifactId, revision).getBytes(StandardCharsets.UTF_8));
        return repoRoot;
    }

    private static String pomXml(String artifactId, String revision) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n"
            + "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
            + "    <modelVersion>4.0.0</modelVersion>\n"
            + "    <groupId>example</groupId>\n"
            + "    <artifactId>" + artifactId + "</artifactId>\n"
            + "    <version>${revision}</version>\n"
            + "    <properties>\n"
            + "        <revision>" + revision + "</revision>\n"
            + "    </properties>\n"
            + "</project>\n";
    }

}
