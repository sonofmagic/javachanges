package io.github.sonofmagic.javachanges.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReleaseProcessUtilsTest {

    @Test
    void runCaptureCollectsStdoutAndStderr(@TempDir Path tempDir) throws Exception {
        CommandResult result = ReleaseProcessUtils.runCapture(tempDir, outputCommand());

        assertEquals(0, result.exitCode);
        assertTrue(result.stdoutText().contains("stdout-line"));
        assertTrue(result.stderrText().contains("stderr-line"));
    }

    @Test
    void deleteRecursivelyRemovesNestedFiles(@TempDir Path tempDir) throws Exception {
        Path root = tempDir.resolve("root");
        Path nested = root.resolve("nested");
        Files.createDirectories(nested);
        Files.write(nested.resolve("settings.xml"), "secret".getBytes(StandardCharsets.UTF_8));

        ReleaseProcessUtils.deleteRecursively(root);

        assertTrue(Files.notExists(root));
    }

    @Test
    void restrictOwnerOnlyAcceptsExistingFile(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("settings.xml");
        Files.write(file, "secret".getBytes(StandardCharsets.UTF_8));

        ReleaseProcessUtils.restrictOwnerOnly(file);

        assertTrue(Files.exists(file));
    }

    private static List<String> outputCommand() {
        if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            return Arrays.asList("cmd", "/c", "(echo stdout-line) & (echo stderr-line 1>&2)");
        }
        return Arrays.asList("sh", "-c", "printf 'stdout-line\\n'; printf 'stderr-line\\n' >&2");
    }
}
