package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class ReleaseProcessUtils {
    private ReleaseProcessUtils() {
    }

    public static byte[] readAllBytes(InputStream inputStream) throws IOException {
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = inputStream.read(buffer)) >= 0) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    public static String gitTextAllowEmpty(Path repoRoot, String... args) throws IOException, InterruptedException {
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

    public static int runCommand(List<String> command, Path workingDirectory) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workingDirectory.toFile());
        builder.inheritIO();
        Process process = builder.start();
        return process.waitFor();
    }

    public static CommandResult runCapture(Path workingDirectory, String... command) throws IOException, InterruptedException {
        return runCapture(workingDirectory, Arrays.asList(command));
    }

    public static CommandResult runCapture(Path workingDirectory, List<String> command) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workingDirectory.toFile());
        return runCapture(builder);
    }

    public static CommandResult runCapture(Path workingDirectory, List<String> command, Map<String, String> environment)
        throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workingDirectory.toFile());
        if (environment != null) {
            builder.environment().putAll(environment);
        }
        return runCapture(builder);
    }

    private static CommandResult runCapture(ProcessBuilder builder) throws IOException, InterruptedException {
        Process process = builder.start();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<byte[]> stdoutFuture = executor.submit(readStream(process.getInputStream()));
            Future<byte[]> stderrFuture = executor.submit(readStream(process.getErrorStream()));
            int exitCode = process.waitFor();
            byte[] stdout = awaitBytes(stdoutFuture);
            byte[] stderr = awaitBytes(stderrFuture);
            return new CommandResult(exitCode, stdout, stderr);
        } finally {
            executor.shutdownNow();
        }
    }

    public static String mavenWrapperPath() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win") ? "mvnw.cmd" : "./mvnw";
    }

    public static String gradleWrapperPath() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win") ? "gradlew.bat" : "./gradlew";
    }

    public static MavenCommand resolveMavenCommand(Path repoRoot) throws IOException, InterruptedException {
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

    public static MavenCommand resolveMavenCommand(Path repoRoot, MavenCommandProbe probe) throws IOException, InterruptedException {
        Path wrapperPath = repoRoot.resolve(mavenWrapperPath());
        if (probe.fileExists(wrapperPath)) {
            return new MavenCommand(mavenWrapperPath(), "wrapper");
        }
        if (probe.commandAvailable(repoRoot, "mvn", "-q", "-version")) {
            return new MavenCommand("mvn", "system");
        }
        return null;
    }

    public static GradleCommand resolveGradleCommand(Path repoRoot) throws IOException, InterruptedException {
        Path wrapperPath = repoRoot.resolve(gradleWrapperPath());
        if (Files.exists(wrapperPath)) {
            return new GradleCommand(gradleWrapperPath(), "wrapper");
        }
        if (runCapture(repoRoot, "gradle", "-q", "--version").exitCode == 0) {
            return new GradleCommand("gradle", "system");
        }
        return null;
    }

    public static void closeQuietly(InputStream inputStream) {
        try {
            inputStream.close();
        } catch (IOException ignored) {
        }
    }

    public static void deleteRecursively(Path root) throws IOException {
        if (root == null || !Files.exists(root)) {
            return;
        }
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void restrictOwnerOnly(Path path) throws IOException {
        Set<PosixFilePermission> permissions = Files.isDirectory(path)
            ? PosixFilePermissions.fromString("rwx------")
            : PosixFilePermissions.fromString("rw-------");
        try {
            Files.setPosixFilePermissions(path, permissions);
        } catch (UnsupportedOperationException ignored) {
            // Non-POSIX filesystems do not support owner-only permissions.
        }
    }

    private static Callable<byte[]> readStream(final InputStream inputStream) {
        return new Callable<byte[]>() {
            @Override
            public byte[] call() {
                try {
                    return readAllBytes(inputStream);
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
            }
        };
    }

    private static byte[] awaitBytes(Future<byte[]> future) throws IOException, InterruptedException {
        try {
            return future.get();
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof UncheckedIOException) {
                throw ((UncheckedIOException) cause).getCause();
            }
            throw new IllegalStateException("Failed to capture process output", cause);
        }
    }
}
