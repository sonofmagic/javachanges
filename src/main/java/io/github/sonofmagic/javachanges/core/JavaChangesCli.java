package io.github.sonofmagic.javachanges.core;

import picocli.CommandLine;

import java.io.PrintStream;
import java.io.PrintWriter;

import static io.github.sonofmagic.javachanges.core.ReleaseTextUtils.trimToNull;

public final class JavaChangesCli {

    private JavaChangesCli() {
    }

    public static void main(String[] args) {
        int exitCode = execute(args, System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    public static int execute(String[] args, PrintStream out, PrintStream err) {
        JavaChangesCommand root = new JavaChangesCommand(out, err);
        CommandLine commandLine = new CommandLine(root);
        commandLine.setOut(new PrintWriter(out, true));
        commandLine.setErr(new PrintWriter(err, true));
        commandLine.setExecutionExceptionHandler(new CliExecutionExceptionHandler());
        return commandLine.execute(args);
    }

    static final class CliExecutionExceptionHandler implements CommandLine.IExecutionExceptionHandler {
        @Override
        public int handleExecutionException(Exception exception, CommandLine commandLine,
                                            CommandLine.ParseResult parseResult) {
            String message = trimToNull(exception.getMessage());
            if (message == null) {
                message = exception.getClass().getSimpleName();
            }
            commandLine.getErr().println(message);
            return commandLine.getCommandSpec().exitCodeOnExecutionException();
        }
    }
}
