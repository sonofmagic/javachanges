package io.github.sonofmagic.javachanges.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PomModelSupportTest {

    @Test
    void readsRevisionAndArtifactIdFromNamespacedPom(@TempDir Path tempDir) throws Exception {
        Path pomPath = tempDir.resolve("pom.xml");
        Files.write(pomPath, namespacedPom().getBytes(StandardCharsets.UTF_8));

        assertEquals("2.3.4-SNAPSHOT", PomModelSupport.readRevision(pomPath));
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
