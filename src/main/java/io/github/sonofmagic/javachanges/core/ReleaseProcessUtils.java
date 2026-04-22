package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

final class ReleaseProcessUtils {
    private ReleaseProcessUtils() {
    }

    static byte[] readAllBytes(InputStream inputStream) throws IOException {
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = inputStream.read(buffer)) >= 0) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    static String gitTextAllowEmpty(Path repoRoot, String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<String>();
        command.add("git");
        command.addAll(Arrays.asList(args));
        CommandResult result = runCapture(repoRoot, command);
        int exitCode = result.exitCode;
        if (exitCode != 0) {
            String error = result.stderrText().trim();
            if (!error.isEmpty()) {
                throw new IllegalStateException(error);
            }
            throw new IllegalStateException("git command failed");
        }
        return result.stdoutText();
    }

    static int runCommand(List<String> command, Path workingDirectory) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workingDirectory.toFile());
        builder.inheritIO();
        Process process = builder.start();
        return process.waitFor();
    }

    static CommandResult runCapture(Path workingDirectory, String... command) throws IOException, InterruptedException {
        return runCapture(workingDirectory, Arrays.asList(command));
    }

    static CommandResult runCapture(Path workingDirectory, List<String> command) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workingDirectory.toFile());
        Process process = builder.start();
        byte[] stdout = readAllBytes(process.getInputStream());
        byte[] stderr = readAllBytes(process.getErrorStream());
        int exitCode = process.waitFor();
        return new CommandResult(exitCode, stdout, stderr);
    }

    static String mavenWrapperPath() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win") ? "mvnw.cmd" : "./mvnw";
    }

    static MavenCommand resolveMavenCommand(Path repoRoot) throws IOException, InterruptedException {
        return resolveMavenCommand(repoRoot, new MavenCommandProbe() {
            @Override
            public boolean fileExists(Path path) {
                return java.nio.file.Files.exists(path);
            }

            @Override
            public boolean commandAvailable(Path workingDirectory, String... command) throws IOException, InterruptedException {
                return runCapture(workingDirectory, command).exitCode == 0;
            }
        });
    }

    static MavenCommand resolveMavenCommand(Path repoRoot, MavenCommandProbe probe) throws IOException, InterruptedException {
        Path wrapperPath = repoRoot.resolve(mavenWrapperPath());
        if (probe.fileExists(wrapperPath)) {
            return new MavenCommand(mavenWrapperPath(), "wrapper");
        }
        if (probe.commandAvailable(repoRoot, "mvn", "-q", "-version")) {
            return new MavenCommand("mvn", "system");
        }
        return null;
    }

    static void closeQuietly(InputStream inputStream) {
        try {
            inputStream.close();
        } catch (IOException ignored) {
        }
    }
}
