package io.github.sonofmagic.javachanges.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PomModelSupportTest {

    @Test
    void readsRevisionAndArtifactIdFromNamespacedPom(@TempDir Path tempDir) throws Exception {
        Path pomPath = tempDir.resolve("pom.xml");
        Files.write(pomPath, namespacedPom().getBytes(StandardCharsets.UTF_8));

        assertEquals("2.3.4-SNAPSHOT", PomModelSupport.readRevision(pomPath));
        assertEquals(PomModelSupport.VersionSource.REVISION_PROPERTY, PomModelSupport.versionSource(pomPath));
        assertEquals("fixture-app", PomModelSupport.readArtifactId(pomPath));
    }

    @Test
    void readsModulePathsWithoutPickingParentArtifactId(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot.resolve("core"));
        Files.createDirectories(repoRoot.resolve("cli"));
        Files.write(repoRoot.resolve("pom.xml"), monorepoPom().getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve("core").resolve("pom.xml"), childModulePom("core").getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve("cli").resolve("pom.xml"), childModulePom("cli").getBytes(StandardCharsets.UTF_8));

        assertEquals(Arrays.asList("core", "cli"), PomModelSupport.readModulePaths(repoRoot.resolve("pom.xml")));
        assertEquals(Arrays.asList("core", "cli"), ReleaseModuleUtils.detectKnownModules(repoRoot));
    }

    @Test
    void writeRevisionUpdatesPomValue(@TempDir Path tempDir) throws Exception {
        Path pomPath = tempDir.resolve("pom.xml");
        Files.write(pomPath, namespacedPom().getBytes(StandardCharsets.UTF_8));

        PomModelSupport.writeRevision(pomPath, "2.3.4");

        assertEquals("2.3.4", PomModelSupport.readRevision(pomPath));
    }

    @Test
    void readsLiteralProjectVersionEvenWhenUnrelatedRevisionExists(@TempDir Path tempDir) throws Exception {
        Path pomPath = tempDir.resolve("pom.xml");
        Files.write(pomPath, literalPom("2.3.4-SNAPSHOT", true).getBytes(StandardCharsets.UTF_8));

        assertEquals("2.3.4-SNAPSHOT", PomModelSupport.readRevision(pomPath));
        assertEquals(PomModelSupport.VersionSource.PROJECT_VERSION, PomModelSupport.versionSource(pomPath));
    }

    @Test
    void writeRevisionUpdatesLiteralProjectVersionWithoutAddingRevision(@TempDir Path tempDir) throws Exception {
        Path pomPath = tempDir.resolve("pom.xml");
        Files.write(pomPath, literalPom("2.3.4-SNAPSHOT", false).getBytes(StandardCharsets.UTF_8));

        PomModelSupport.writeRevision(pomPath, "2.3.5-SNAPSHOT");

        String updated = new String(Files.readAllBytes(pomPath), StandardCharsets.UTF_8);
        assertEquals("2.3.5-SNAPSHOT", PomModelSupport.readRevision(pomPath));
        assertTrue(updated.contains("<version>2.3.5-SNAPSHOT</version>"));
        assertFalse(updated.contains("<revision>"));
    }

    @Test
    void writesLiteralVersionCopyWithoutChangingOriginalPom(@TempDir Path tempDir) throws Exception {
        Path pomPath = tempDir.resolve("pom.xml");
        Path publishPom = tempDir.resolve("publish-pom.xml");
        String original = literalPom("2.3.4-SNAPSHOT", false);
        Files.write(pomPath, original.getBytes(StandardCharsets.UTF_8));

        PomModelSupport.writeVersionCopy(pomPath, publishPom, "2.3.4");

        assertEquals("2.3.4-SNAPSHOT", PomModelSupport.readRevision(pomPath));
        assertEquals("2.3.4", PomModelSupport.readRevision(publishPom));
        assertEquals(original, new String(Files.readAllBytes(pomPath), StandardCharsets.UTF_8));
    }

    @Test
    void rejectsUnsupportedProjectVersionExpression(@TempDir Path tempDir) throws Exception {
        Path pomPath = tempDir.resolve("pom.xml");
        Files.write(pomPath, literalPom("${project.versionName}", false).getBytes(StandardCharsets.UTF_8));

        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> PomModelSupport.readRevision(pomPath));

        assertTrue(error.getMessage().contains("Unsupported Maven project version expression"));
        assertTrue(error.getMessage().contains("${revision}"));
    }

    @Test
    void rejectsPomWithoutDirectProjectVersion(@TempDir Path tempDir) throws Exception {
        Path pomPath = tempDir.resolve("pom.xml");
        Files.write(pomPath, literalPom("1.0.0-SNAPSHOT", false)
            .replace("    <version>1.0.0-SNAPSHOT</version>\n", "")
            .getBytes(StandardCharsets.UTF_8));

        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> PomModelSupport.readRevision(pomPath));

        assertTrue(error.getMessage().contains("Cannot find a direct project <version>"));
    }

    @Test
    void rejectsLiteralVersionForMultiModulePom(@TempDir Path tempDir) throws Exception {
        Path pomPath = tempDir.resolve("pom.xml");
        Files.write(pomPath, monorepoPom().replace("${revision}", "1.0.0-SNAPSHOT")
            .getBytes(StandardCharsets.UTF_8));

        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> PomModelSupport.readRevision(pomPath));

        assertTrue(error.getMessage().contains("supported only for single-module repositories"));
        assertTrue(error.getMessage().contains("${revision}"));
    }

    private static String namespacedPom() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n"
            + "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
            + "    <modelVersion>4.0.0</modelVersion>\n"
            + "    <groupId>example</groupId>\n"
            + "    <artifactId>fixture-app</artifactId>\n"
            + "    <version>${revision}</version>\n"
            + "    <properties>\n"
            + "        <revision>2.3.4-SNAPSHOT</revision>\n"
            + "    </properties>\n"
            + "</project>\n";
    }

    private static String literalPom(String version, boolean includeRevision) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\">\n"
            + "    <modelVersion>4.0.0</modelVersion>\n"
            + "    <groupId>example</groupId>\n"
            + "    <artifactId>fixture-app</artifactId>\n"
            + "    <version>" + version + "</version>\n"
            + (includeRevision ? "    <properties><revision>9.9.9-SNAPSHOT</revision></properties>\n" : "")
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
            + "        <revision>1.0.0-SNAPSHOT</revision>\n"
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
}
