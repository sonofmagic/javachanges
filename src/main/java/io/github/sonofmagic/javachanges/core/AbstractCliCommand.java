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

    @FunctionalInterface
    interface ThrowingIntSupplier {
        int get() throws Exception;
    }

    @FunctionalInterface
    interface ErrorJsonRenderer {
        String render(String command, Exception exception);
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
        return runJsonCommand(command, format, new ErrorJsonRenderer() {
            @Override
            public String render(String name, Exception exception) {
                return AutomationJsonSupport.errorJson(name, exception);
            }
        }, new ThrowingIntSupplier() {
            @Override
            public int get() throws Exception {
                action.run();
                return success();
            }
        });
    }

    final int runEnvJsonCommand(String command, OutputFormat format, ThrowingIntSupplier action) throws Exception {
        return runJsonCommand(command, format, new ErrorJsonRenderer() {
            @Override
            public String render(String name, Exception exception) {
                return ReleaseEnvSupport.errorJson(name, exception);
            }
        }, action);
    }

    final int runJsonCommand(String command, OutputFormat format, ErrorJsonRenderer errorJsonRenderer,
                             ThrowingIntSupplier action) throws Exception {
        try {
            return action.get();
        } catch (Exception exception) {
            if (format == OutputFormat.JSON) {
                out().println(errorJsonRenderer.render(command, exception));
                return 1;
            }
            throw exception;
        }
    }
}
