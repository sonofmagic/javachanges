package io.github.sonofmagic.javachanges.gradle;

import io.github.sonofmagic.javachanges.core.BuildModelSupport;
import io.github.sonofmagic.javachanges.core.ReleaseModuleUtils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GradleModelSupportTest {

    @Test
    void readsAndWritesVersionFromGradleProperties(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot);
        Files.write(repoRoot.resolve("settings.gradle"), "rootProject.name = 'fixture-app'\n".getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve("gradle.properties"), (
            "org.gradle.jvmargs=-Xmx1g\n" +
                "version=1.2.3-SNAPSHOT\n"
        ).getBytes(StandardCharsets.UTF_8));

        assertEquals("1.2.3-SNAPSHOT", BuildModelSupport.readRevision(repoRoot));

        BuildModelSupport.writeRevision(repoRoot, "1.2.4-SNAPSHOT");

        assertEquals("1.2.4-SNAPSHOT", BuildModelSupport.readRevision(repoRoot));
    }

    @Test
    void readsRevisionFallbackFromGradleProperties(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot);
        Files.write(repoRoot.resolve("settings.gradle.kts"), "rootProject.name = \"fixture-app\"\n".getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve("gradle.properties"), "revision: 2.0.0-SNAPSHOT\n".getBytes(StandardCharsets.UTF_8));

        assertEquals("2.0.0-SNAPSHOT", BuildModelSupport.readRevision(repoRoot));
    }

    @Test
    void readsAndWritesAndroidVersionNameFromGradleProperties(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot);
        Files.write(repoRoot.resolve("settings.gradle"), "rootProject.name = 'fixture-app'\n".getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve("gradle.properties"), (
            "GROUP=com.example\n" +
                "VERSION_NAME=3.0.0-SNAPSHOT\n"
        ).getBytes(StandardCharsets.UTF_8));

        assertEquals("3.0.0-SNAPSHOT", BuildModelSupport.readRevision(repoRoot));

        BuildModelSupport.writeRevision(repoRoot, "3.0.1-SNAPSHOT");

        assertEquals("3.0.1-SNAPSHOT", BuildModelSupport.readRevision(repoRoot));
    }

    @Test
    void prefersGradleModelWhenPomAndAndroidGradleMetadataCoexist(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot);
        Files.write(repoRoot.resolve("pom.xml"), (
            "<project><modelVersion>4.0.0</modelVersion><groupId>example</groupId>" +
                "<artifactId>fixture-pom</artifactId><version>${revision}</version>" +
                "<properties><revision>1.0.0-SNAPSHOT</revision></properties></project>\n"
        ).getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve("settings.gradle"), (
            "rootProject.name = 'fixture-app'\n" +
                "include ':android-sdk'\n"
        ).getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve("gradle.properties"), "VERSION_NAME=3.0.0-SNAPSHOT\n".getBytes(StandardCharsets.UTF_8));

        assertEquals("3.0.0-SNAPSHOT", BuildModelSupport.readRevision(repoRoot));
        assertEquals(Arrays.asList("android-sdk"), ReleaseModuleUtils.detectKnownModules(repoRoot));
    }

    @Test
    void detectsGradleModulesFromSettings(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot);
        Files.write(repoRoot.resolve("settings.gradle.kts"), (
            "rootProject.name = \"fixture-parent\"\n" +
                "include(\":core\", \"tools:cli\")\n"
        ).getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve("gradle.properties"), "version=1.0.0-SNAPSHOT\n".getBytes(StandardCharsets.UTF_8));

        assertEquals(Arrays.asList("core", "cli"), ReleaseModuleUtils.detectKnownModules(repoRoot));
    }

    @Test
    void detectsGradleModulesFromMultilineSettingsInclude(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot);
        Files.write(repoRoot.resolve("settings.gradle.kts"), (
            "rootProject.name = \"fixture-parent\"\n" +
                "include(\n" +
                "    \":core\",\n" +
                "    \":tools:cli\",\n" +
                ")\n"
        ).getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve("gradle.properties"), "version=1.0.0-SNAPSHOT\n".getBytes(StandardCharsets.UTF_8));

        assertEquals(Arrays.asList("core", "cli"), ReleaseModuleUtils.detectKnownModules(repoRoot));
    }

    @Test
    void detectsGradleRenamedProjectsFromSettings(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot);
        Files.write(repoRoot.resolve("settings.gradle.kts"), (
            "rootProject.name = \"fixture-parent\"\n" +
                "include(\":api\", \":tools:cli\")\n" +
                "project(\":api\").name = \"public-api\"\n" +
                "project(\":tools:cli\").name = \"command-line\"\n"
        ).getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve("gradle.properties"), "version=1.0.0-SNAPSHOT\n".getBytes(StandardCharsets.UTF_8));

        assertEquals(Arrays.asList("public-api", "command-line"), ReleaseModuleUtils.detectKnownModules(repoRoot));
    }

    @Test
    void ignoresGradleIncludesInsideComments(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot);
        Files.write(repoRoot.resolve("settings.gradle.kts"), (
            "rootProject.name = \"fixture-parent\"\n" +
                "include(\":core\")\n" +
                "include(\":url//safe\")\n" +
                "// include(\":commented-line\")\n" +
                "/*\n" +
                "include(\":commented-block\")\n" +
                "*/\n"
        ).getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve("gradle.properties"), "version=1.0.0-SNAPSHOT\n".getBytes(StandardCharsets.UTF_8));

        assertEquals(Arrays.asList("core", "url//safe"), ReleaseModuleUtils.detectKnownModules(repoRoot));
    }

    @Test
    void rendersGradleModuleSelector(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot);
        Files.write(repoRoot.resolve("settings.gradle"), "include ':core'\n".getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve("gradle.properties"), "version=1.0.0-SNAPSHOT\n".getBytes(StandardCharsets.UTF_8));

        assertEquals(":core", ReleaseModuleUtils.moduleSelectorArgs(repoRoot, "core"));
    }

    @Test
    void stagesGradleVersionFileForReleasePlans(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot);
        Files.write(repoRoot.resolve("settings.gradle"), "rootProject.name = 'fixture-app'\n".getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve("gradle.properties"), "version=1.0.0-SNAPSHOT\n".getBytes(StandardCharsets.UTF_8));

        assertEquals(Arrays.asList("gradle.properties", "CHANGELOG.md", ".changesets"),
            Arrays.asList(BuildModelSupport.releasePlanGitAddPaths(repoRoot)));
    }
}
