package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.readAllBytes;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.trimToNull;

final class GpgKeySupport {
    private final Path workingDirectory;

    GpgKeySupport(Path workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    String ensurePublicKeyDiscoverable(String primaryKeyserver,
                                       String secondaryKeyserver,
                                       int attempts,
                                       int retryDelaySeconds,
                                       PrintStream out,
                                       PrintStream err) throws IOException, InterruptedException {
        if (attempts < 1) {
            throw new IllegalArgumentException("--attempts must be greater than 0");
        }
        if (retryDelaySeconds < 0) {
            throw new IllegalArgumentException("--retry-delay-seconds must be 0 or greater");
        }

        String fingerprint = detectFingerprint();
        out.println("Detected signing key fingerprint: " + fingerprint);
        out.println("Publishing public key to supported keyservers if needed");

        sendKey(primaryKeyserver, fingerprint, out, err);
        sendKey(secondaryKeyserver, fingerprint, out, err);

        for (int attempt = 1; attempt <= attempts; attempt++) {
            if (keyVisibleOnServer(primaryKeyserver, fingerprint)) {
                out.println("Public key " + fingerprint + " is visible on " + primaryKeyserver);
                return fingerprint;
            }
            if (keyVisibleOnServer(secondaryKeyserver, fingerprint)) {
                out.println("Public key " + fingerprint + " is visible on " + secondaryKeyserver);
                return fingerprint;
            }
            if (attempt < attempts) {
                out.println("Public key " + fingerprint + " is not visible yet (attempt "
                    + attempt + "/" + attempts + "); retrying in " + retryDelaySeconds + "s");
                Thread.sleep(retryDelaySeconds * 1000L);
            }
        }

        throw new IllegalStateException("The signing key fingerprint " + fingerprint
            + " is still not visible from " + primaryKeyserver + " or " + secondaryKeyserver
            + ". Publish the public key to a supported keyserver and rerun the workflow.");
    }

    private String detectFingerprint() throws IOException, InterruptedException {
        ProcessResult result = runCommand(Arrays.asList(
            "gpg", "--batch", "--list-secret-keys", "--with-colons", "--fingerprint"
        ), null);
        if (result.exitCode != 0) {
            throw new IllegalStateException("Failed to inspect imported GPG secret keys: " + result.stderrOrStdout());
        }
        String[] lines = result.stdout.split("\\R");
        for (String line : lines) {
            if (line.startsWith("fpr:")) {
                String[] fields = line.split(":");
                if (fields.length > 9) {
                    String fingerprint = trimToNull(fields[9]);
                    if (fingerprint != null) {
                        return fingerprint;
                    }
                }
            }
        }
        throw new IllegalStateException("No imported secret key fingerprint was found after actions/setup-java.");
    }

    private void sendKey(String keyserver, String fingerprint, PrintStream out, PrintStream err)
        throws IOException, InterruptedException {
        ProcessResult result = runCommand(Arrays.asList(
            "gpg", "--batch", "--keyserver", keyserver, "--send-keys", fingerprint
        ), null);
        if (result.exitCode == 0) {
            out.println("Public key uploaded to " + keyserver);
            return;
        }
        err.println("Warning: unable to upload public key " + fingerprint + " to " + keyserver
            + ". Continuing to verification. Details: " + result.stderrOrStdout());
    }

    private boolean keyVisibleOnServer(String keyserver, String fingerprint) throws IOException, InterruptedException {
        Path tempHome = Files.createTempDirectory("javachanges-gpg-");
        try {
            ProcessResult result = runCommand(Arrays.asList(
                "gpg", "--batch", "--homedir", tempHome.toString(), "--keyserver", keyserver, "--recv-keys", fingerprint
            ), tempHome);
            return result.exitCode == 0;
        } finally {
            deleteRecursively(tempHome);
        }
    }

    private ProcessResult runCommand(List<String> command, Path customHome) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(new ArrayList<String>(command));
        builder.directory(workingDirectory.toFile());
        if (customHome != null) {
            builder.environment().put("GNUPGHOME", customHome.toString());
        }
        Process process = builder.start();
        byte[] stdout = readAllBytes(process.getInputStream());
        byte[] stderr = readAllBytes(process.getErrorStream());
        int exitCode = process.waitFor();
        return new ProcessResult(
            exitCode,
            new String(stdout, StandardCharsets.UTF_8).trim(),
            new String(stderr, StandardCharsets.UTF_8).trim()
        );
    }

    private void deleteRecursively(Path root) throws IOException {
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

    private static final class ProcessResult {
        private final int exitCode;
        private final String stdout;
        private final String stderr;

        private ProcessResult(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }

        private String stderrOrStdout() {
            String value = trimToNull(stderr);
            if (value != null) {
                return value;
            }
            value = trimToNull(stdout);
            return value == null ? "no process output" : value;
        }
    }
}
