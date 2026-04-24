package io.github.sonofmagic.javachanges.core;

import io.github.sonofmagic.javachanges.core.automation.AutomationJsonSupport;
import io.github.sonofmagic.javachanges.core.env.ReleaseEnvSupport;
import io.github.sonofmagic.javachanges.core.github.GithubReleaseSupport;
import io.github.sonofmagic.javachanges.core.gitlab.GitlabReleaseSupport;
import io.github.sonofmagic.javachanges.core.publish.PublishSupport;
import picocli.CommandLine.ParentCommand;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static io.github.sonofmagic.javachanges.core.ReleaseTextUtils.trimToNull;

abstract class AbstractCliCommand implements Callable<Integer> {
    static final class CliOption {
        final String key;
        final String value;

        private CliOption(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Exception;
    }

    @FunctionalInterface
    interface ThrowingIntSupplier {
        int get() throws Exception;
    }

    @FunctionalInterface
    interface ThrowingBooleanSupplier {
        boolean get() throws Exception;
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

    final ReleaseEnvSupport envSupport() {
        return new ReleaseEnvSupport(repoRoot(), out());
    }

    final GithubReleaseSupport githubReleaseSupport() {
        return new GithubReleaseSupport(repoRoot(), out());
    }

    final GitlabReleaseSupport gitlabReleaseSupport() {
        return new GitlabReleaseSupport(repoRoot(), out());
    }

    final PublishSupport publishSupport() {
        return new PublishSupport(repoRoot(), out());
    }

    final Map<String, String> options() {
        return new LinkedHashMap<String, String>();
    }

    final Map<String, String> options(CliOption... entries) {
        Map<String, String> options = options();
        for (CliOption entry : entries) {
            if (entry != null) {
                putOption(options, entry.key, entry.value);
            }
        }
        return options;
    }

    final CliOption option(String key, String value) {
        return new CliOption(key, value);
    }

    final CliOption flag(String key, boolean value) {
        return value ? new CliOption(key, "true") : null;
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

    final int failure() {
        return 1;
    }

    final int runAutomationCommand(String command, OutputFormat format, ThrowingRunnable action) throws Exception {
        return runJsonCommand(command, format,
            (name, exception) -> AutomationJsonSupport.errorJson(name, exception),
            () -> {
                action.run();
                return success();
            });
    }

    final int runEnvJsonCommand(String command, OutputFormat format, ThrowingIntSupplier action) throws Exception {
        return runJsonCommand(command, format,
            (name, exception) -> ReleaseEnvSupport.errorJson(name, exception),
            action);
    }

    final int runEnvBooleanCommand(String command, OutputFormat format, ThrowingBooleanSupplier action) throws Exception {
        return runEnvJsonCommand(command, format, () -> action.get() ? success() : failure());
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
