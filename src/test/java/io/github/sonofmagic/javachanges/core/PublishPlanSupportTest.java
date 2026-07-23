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

    @Test
    void literalPlainSnapshotUsesOriginalPomWithoutRevisionOverride(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createLiteralRepository(tempDir, "fixture-app", "1.2.3-SNAPSHOT");
        PublishPlanSupport support = support(repoRoot);
        Map<String, String> options = new LinkedHashMap<String, String>();
        options.put("snapshot", "true");
        options.put("snapshot-version-mode", "plain");
        PublishRequest request = PublishRequest.fromOptions(options, true);

        PublishPlanSupport.PublishTarget target = support.resolvePublishTarget(request);
        List<String> command = support.buildDeployCommand(request, target,
            new MavenCommand("mvn", "system"), null, "https://repo.example.com/snapshots");

        assertEquals(PomModelSupport.VersionSource.PROJECT_VERSION, target.versionSource);
        assertFalse(target.temporaryPomRequired);
        assertFalse(command.contains("-Drevision=1.2.3-SNAPSHOT"));
        assertFalse(command.contains("-f"));
    }

    @Test
    void literalStampedSnapshotUsesTemporaryPomAndPreservesOriginal(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createLiteralRepository(tempDir, "fixture-app", "1.2.3-SNAPSHOT");
        Path pomPath = repoRoot.resolve("pom.xml");
        String original = new String(Files.readAllBytes(pomPath), StandardCharsets.UTF_8);
        PublishPlanSupport support = support(repoRoot);
        Map<String, String> options = new LinkedHashMap<String, String>();
        options.put("snapshot", "true");
        options.put("snapshot-build-stamp", "20260422.120000.abc123");
        PublishRequest request = PublishRequest.fromOptions(options, true);

        PublishPlanSupport.PublishTarget target = support.resolvePublishTarget(request);
        List<String> command = support.buildDeployCommand(request, target,
            new MavenCommand("mvn", "system"), null, "https://repo.example.com/snapshots");
        Path temporaryPom = support.prepareTemporaryPublishPom(target);

        assertTrue(target.temporaryPomRequired);
        assertTrue(command.contains("-f"));
        assertTrue(command.contains(PublishPlanSupport.TEMPORARY_PUBLISH_POM));
        assertFalse(command.contains("-Drevision=1.2.3-20260422.120000.abc123-SNAPSHOT"));
        assertEquals("1.2.3-20260422.120000.abc123-SNAPSHOT", PomModelSupport.readRevision(temporaryPom));
        assertEquals(original, new String(Files.readAllBytes(pomPath), StandardCharsets.UTF_8));
        Files.delete(temporaryPom);
    }

    @Test
    void literalReleaseUsesTemporaryPom(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createLiteralRepository(tempDir, "fixture-app", "1.2.3-SNAPSHOT");
        PublishPlanSupport support = support(repoRoot);
        Map<String, String> options = new LinkedHashMap<String, String>();
        options.put("tag", "v1.2.3");
        PublishRequest request = PublishRequest.fromOptions(options, true);

        PublishPlanSupport.PublishTarget target = support.resolvePublishTarget(request);
        Path temporaryPom = support.prepareTemporaryPublishPom(target);

        assertTrue(target.temporaryPomRequired);
        assertEquals("1.2.3", PomModelSupport.readRevision(temporaryPom));
        assertEquals("1.2.3-SNAPSHOT", PomModelSupport.readRevision(repoRoot.resolve("pom.xml")));
        Files.delete(temporaryPom);
    }

    @Test
    void releaseQualifiedProjectDerivesPlainSnapshotFromPendingChangeset(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createLiteralRepository(tempDir, "fixture-app", "1.2.2-RELEASE");
        writeReleaseQualifiedConfigAndPatch(repoRoot);
        Path pomPath = repoRoot.resolve("pom.xml");
        String original = new String(Files.readAllBytes(pomPath), StandardCharsets.UTF_8);
        PublishPlanSupport support = support(repoRoot);
        Map<String, String> options = new LinkedHashMap<String, String>();
        options.put("snapshot", "true");
        options.put("snapshot-version-mode", "plain");
        PublishRequest request = PublishRequest.fromOptions(options, true);

        PublishPlanSupport.PublishTarget target = support.resolvePublishTarget(request);
        Path temporaryPom = support.prepareTemporaryPublishPom(target);

        assertEquals("1.2.3-SNAPSHOT", target.publishVersion);
        assertTrue(target.temporaryPomRequired);
        assertEquals("1.2.3-SNAPSHOT", PomModelSupport.readRevision(temporaryPom));
        assertEquals(original, new String(Files.readAllBytes(pomPath), StandardCharsets.UTF_8));
        Files.delete(temporaryPom);
    }

    @Test
    void configuredReleaseSuffixAppliesToTaggedReleaseArtifact(@TempDir Path tempDir) throws Exception {
        Path repoRoot = createLiteralRepository(tempDir, "fixture-app", "1.2.3-SNAPSHOT");
        Path changesetsDir = repoRoot.resolve(".changesets");
        Files.createDirectories(changesetsDir);
        Files.write(changesetsDir.resolve("config.jsonc"),
            "{\n  \"releaseVersionSuffix\": \"-RELEASE\"\n}\n".getBytes(StandardCharsets.UTF_8));
        PublishPlanSupport support = support(repoRoot);
        Map<String, String> options = new LinkedHashMap<String, String>();
        options.put("tag", "v1.2.3");
        PublishRequest request = PublishRequest.fromOptions(options, true);

        PublishPlanSupport.PublishTarget target = support.resolvePublishTarget(request);
        Path temporaryPom = support.prepareTemporaryPublishPom(target);

        assertEquals("1.2.3-RELEASE", target.publishVersion);
        assertTrue(target.temporaryPomRequired);
        assertEquals("1.2.3-RELEASE", PomModelSupport.readRevision(temporaryPom));
        assertEquals("1.2.3-SNAPSHOT", PomModelSupport.readRevision(repoRoot.resolve("pom.xml")));
        Files.delete(temporaryPom);
    }

    private static PublishPlanSupport support(Path repoRoot) {
        return new PublishPlanSupport(repoRoot, new PublishRuntime(repoRoot), new VersionSupport(repoRoot));
    }

    private static void writeReleaseQualifiedConfigAndPatch(Path repoRoot) throws IOException {
        Path changesetsDir = repoRoot.resolve(".changesets");
        Files.createDirectories(changesetsDir);
        Files.write(changesetsDir.resolve("config.jsonc"), (
            "{\n" +
                "  \"snapshotVersionMode\": \"plain\",\n" +
                "  \"releaseVersionSuffix\": \"-RELEASE\"\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));
        Files.write(changesetsDir.resolve("patch.md"), (
            "---\n" +
                "\"fixture-app\": patch\n" +
                "---\n\n" +
                "Derive the next snapshot from the pending change.\n").getBytes(StandardCharsets.UTF_8));
    }

    private static Path createRepository(Path tempDir, String artifactId, String revision) throws IOException {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot);
        Files.write(repoRoot.resolve("pom.xml"), pomXml(artifactId, revision).getBytes(StandardCharsets.UTF_8));
        return repoRoot;
    }

    private static Path createLiteralRepository(Path tempDir, String artifactId, String version) throws IOException {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot);
        String pom = pomXml(artifactId, version)
            .replace("<version>${revision}</version>", "<version>" + version + "</version>")
            .replace("    <properties>\n        <revision>" + version + "</revision>\n    </properties>\n", "");
        Files.write(repoRoot.resolve("pom.xml"), pom.getBytes(StandardCharsets.UTF_8));
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
