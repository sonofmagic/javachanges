package io.github.sonofmagic.javachanges.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

    private static List<String> outputCommand() {
        if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            return Arrays.asList("cmd", "/c", "(echo stdout-line) & (echo stderr-line 1>&2)");
        }
        return Arrays.asList("sh", "-c", "printf 'stdout-line\\n'; printf 'stderr-line\\n' >&2");
    }
}
