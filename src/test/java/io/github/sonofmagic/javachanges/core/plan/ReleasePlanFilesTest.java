package io.github.sonofmagic.javachanges.core.plan;

import io.github.sonofmagic.javachanges.core.BuildModelSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReleasePlanFilesTest {

    @Test
    void applyPlanUpdatesJavachangesSnapshotReadmeExamples(@TempDir Path tempDir) throws Exception {
        Path repoRoot = tempDir.resolve("repo");
        Files.createDirectories(repoRoot.resolve(".changesets"));
        Files.write(repoRoot.resolve("pom.xml"), singleModulePom("1.0.0-SNAPSHOT").getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve("CHANGELOG.md"), "# Changelog\n\n".getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve("README.md"), (
            "On the current `1.0.0-SNAPSHOT` branch:\n" +
                "mvn io.github.sonofmagic:javachanges:1.0.0-SNAPSHOT:status\n"
        ).getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve("README.zh-CN.md"), (
            "\u5f53\u524d `1.0.0-SNAPSHOT`:\n" +
                "mvn io.github.sonofmagic:javachanges:1.0.0-SNAPSHOT:status\n"
        ).getBytes(StandardCharsets.UTF_8));
        Files.write(repoRoot.resolve(".changesets").resolve("fix-doc-sync.md"), (
            "---\n" +
                "\"javachanges\": patch\n" +
                "---\n" +
                "\n" +
                "Keep snapshot docs in sync.\n"
        ).getBytes(StandardCharsets.UTF_8));

        ReleasePlan plan = new ReleasePlanner(repoRoot).plan();
        ReleasePlanFiles.applyPlan(repoRoot, plan, false);

        assertReadmeSnapshot(repoRoot.resolve("README.md"));
        assertReadmeSnapshot(repoRoot.resolve("README.zh-CN.md"));
        List<String> addPaths = Arrays.asList(BuildModelSupport.releasePlanGitAddPaths(repoRoot));
        assertTrue(addPaths.contains("README.md"));
        assertTrue(addPaths.contains("README.zh-CN.md"));
    }

    private static void assertReadmeSnapshot(Path path) throws Exception {
        String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        assertTrue(content.contains("1.0.1-SNAPSHOT"));
        assertFalse(content.contains("1.0.0-SNAPSHOT"));
    }

    private static String singleModulePom(String revision) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\">\n"
            + "    <modelVersion>4.0.0</modelVersion>\n"
            + "    <groupId>example</groupId>\n"
            + "    <artifactId>javachanges</artifactId>\n"
            + "    <version>${revision}</version>\n"
            + "    <properties>\n"
            + "        <revision>" + revision + "</revision>\n"
            + "    </properties>\n"
            + "</project>\n";
    }
}
