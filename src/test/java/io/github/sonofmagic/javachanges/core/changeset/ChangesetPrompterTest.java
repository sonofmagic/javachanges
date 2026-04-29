package io.github.sonofmagic.javachanges.core.changeset;

import io.github.sonofmagic.javachanges.core.ReleaseLanguage;
import io.github.sonofmagic.javachanges.core.ReleaseLanguageContext;
import io.github.sonofmagic.javachanges.core.ReleaseLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChangesetPrompterTest {
    @Test
    void interactivePromptsUseCurrentLanguage(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot);
        Files.write(repoRoot.resolve("pom.xml"), singleModulePom().getBytes(StandardCharsets.UTF_8));

        InputStream originalIn = System.in;
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        System.setIn(new ByteArrayInputStream("interactive summary\npatch\nbody line\n.\n".getBytes(StandardCharsets.UTF_8)));
        ReleaseLanguageContext.set(ReleaseLanguage.ZH_CN);
        try {
            ChangesetInput input = ChangesetPrompter.resolveInput(
                repoRoot,
                Collections.<String, String>emptyMap(),
                new PrintStream(stdout, true),
                new PrintStream(new ByteArrayOutputStream(), true)
            );

            assertEquals("interactive summary", input.summary);
            assertEquals(ReleaseLevel.PATCH, input.release);
            assertEquals("body line", input.body);
            String output = new String(stdout.toByteArray(), StandardCharsets.UTF_8);
            assertTrue(output.contains("摘要: "));
            assertTrue(output.contains("发布级别 (patch/minor/major): "));
            assertTrue(output.contains("正文 (可选，输入单独的 `.` 行结束):"));
        } finally {
            ReleaseLanguageContext.clear();
            System.setIn(originalIn);
        }
    }

    @Test
    void interactivePromptsForModulesInMultiModuleRepository(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot.resolve("core"));
        Files.createDirectories(repoRoot.resolve("cli"));
        Files.write(repoRoot.resolve("pom.xml"), monorepoPom().getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve("core").resolve("pom.xml"), childModulePom("core").getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve("cli").resolve("pom.xml"), childModulePom("cli").getBytes(StandardCharsets.UTF_8));

        InputStream originalIn = System.in;
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        System.setIn(new ByteArrayInputStream("interactive summary\nminor\ncore,cli\nbody line\n.\n".getBytes(StandardCharsets.UTF_8)));
        try {
            ChangesetInput input = ChangesetPrompter.resolveInput(
                repoRoot,
                Collections.<String, String>emptyMap(),
                new PrintStream(stdout, true),
                new PrintStream(new ByteArrayOutputStream(), true)
            );

            assertEquals("interactive summary", input.summary);
            assertEquals(ReleaseLevel.MINOR, input.release);
            assertEquals(Arrays.asList("core", "cli"), input.modules);
            assertEquals("body line", input.body);
            String output = new String(stdout.toByteArray(), StandardCharsets.UTF_8);
            assertTrue(output.contains("Modules (comma-separated, or all; detected: core, cli): "));
        } finally {
            System.setIn(originalIn);
        }
    }

    private static String singleModulePom() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n"
            + "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
            + "  <modelVersion>4.0.0</modelVersion>\n"
            + "  <groupId>example</groupId>\n"
            + "  <artifactId>fixture-app</artifactId>\n"
            + "  <version>${revision}</version>\n"
            + "  <properties>\n"
            + "    <revision>1.2.3-SNAPSHOT</revision>\n"
            + "  </properties>\n"
            + "</project>\n";
    }

    private static String monorepoPom() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n"
            + "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
            + "  <modelVersion>4.0.0</modelVersion>\n"
            + "  <groupId>example</groupId>\n"
            + "  <artifactId>fixture-root</artifactId>\n"
            + "  <version>${revision}</version>\n"
            + "  <packaging>pom</packaging>\n"
            + "  <modules>\n"
            + "    <module>core</module>\n"
            + "    <module>cli</module>\n"
            + "  </modules>\n"
            + "  <properties>\n"
            + "    <revision>1.2.3-SNAPSHOT</revision>\n"
            + "  </properties>\n"
            + "</project>\n";
    }

    private static String childModulePom(String artifactId) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n"
            + "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
            + "  <modelVersion>4.0.0</modelVersion>\n"
            + "  <parent>\n"
            + "    <groupId>example</groupId>\n"
            + "    <artifactId>fixture-root</artifactId>\n"
            + "    <version>${revision}</version>\n"
            + "  </parent>\n"
            + "  <artifactId>" + artifactId + "</artifactId>\n"
            + "</project>\n";
    }
}
