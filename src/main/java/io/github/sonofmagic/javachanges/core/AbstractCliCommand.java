package io.github.sonofmagic.javachanges.core;

import picocli.CommandLine.ParentCommand;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.trimToNull;

abstract class AbstractCliCommand implements Callable<Integer> {
    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Exception;
    }

    @ParentCommand
    private JavaChangesCommand root;

    final PrintStream out() {
        return root.out();
    }

    final PrintStream err() {
        return root.err();
    }

    final Path repoRoot() {
        return root.repoRoot();
    }

    final Map<String, String> options() {
        return new LinkedHashMap<String, String>();
    }

    final void putOption(Map<String, String> options, String key, String value) {
        if (trimToNull(value) != null) {
            options.put(key, value);
        }
    }

    final void putFlag(Map<String, String> options, String key, boolean value) {
        if (value) {
            options.put(key, "true");
        }
    }

    final int success() {
        return 0;
    }

    final int runAutomationCommand(String command, OutputFormat format, ThrowingRunnable action) throws Exception {
        try {
            action.run();
            return success();
        } catch (Exception exception) {
            if (format == OutputFormat.JSON) {
                out().println(AutomationJsonSupport.errorJson(command, exception));
                return 1;
            }
            throw exception;
        }
    }
}
