package io.github.sonofmagic.javachanges.core;

import io.github.sonofmagic.javachanges.core.changeset.Changeset;
import io.github.sonofmagic.javachanges.core.changeset.ChangesetFileSupport;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PendingChangesetConsistencyTest {

    @Test
    void pendingChangesetsUsePackageMapFrontmatterAndBodyText() throws Exception {
        Path repoRoot = Paths.get("").toAbsolutePath().normalize();
        List<Path> changesetFiles = ChangesetFileSupport.listPendingChangesetFiles(repoRoot);

        for (Path changesetFile : changesetFiles) {
            assertPackageMapShape(changesetFile);

            Changeset changeset = ChangesetFileSupport.parseChangeset(repoRoot, changesetFile);
            assertFalse(changeset.modules.isEmpty(), changesetFile + " should target at least one module");
            assertFalse(changeset.summary.trim().isEmpty(), changesetFile + " should have a summary");
            assertFalse(changeset.body.trim().isEmpty(), changesetFile + " should have body text");
        }
    }

    private static void assertPackageMapShape(Path changesetFile) throws Exception {
        List<String> lines = Files.readAllLines(changesetFile, StandardCharsets.UTF_8);
        assertTrue(lines.size() >= 4, changesetFile + " should contain frontmatter and body text");
        assertTrue("---".equals(lines.get(0)), changesetFile + " should start with frontmatter");

        int end = -1;
        for (int index = 1; index < lines.size(); index++) {
            if ("---".equals(lines.get(index))) {
                end = index;
                break;
            }
        }
        assertTrue(end > 1, changesetFile + " should contain package-map frontmatter entries");

        boolean hasPackageEntry = false;
        for (int index = 1; index < end; index++) {
            String line = lines.get(index);
            if (line.startsWith("type:")) {
                continue;
            }
            assertFalse(line.startsWith("release:"), changesetFile + " should use package-map frontmatter");
            assertFalse(line.startsWith("modules:"), changesetFile + " should use package-map frontmatter");
            assertFalse(line.startsWith("summary:"), changesetFile + " should put summary text in the body");
            assertTrue(
                line.matches("\"[^\"]+\": (patch|minor|major)"),
                changesetFile + " should use quoted package-map entries with patch, minor, or major releases"
            );
            hasPackageEntry = true;
        }
        assertTrue(hasPackageEntry, changesetFile + " should include at least one package-map entry");
    }
}
