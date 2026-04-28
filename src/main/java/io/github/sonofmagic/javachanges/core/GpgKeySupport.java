package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static io.github.sonofmagic.javachanges.core.ReleaseTextUtils.trimToNull;

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
        CommandResult result = runCommand(Arrays.asList(
            "gpg", "--batch", "--list-secret-keys", "--with-colons", "--fingerprint"
        ), null);
        if (result.exitCode != 0) {
            throw new IllegalStateException("Failed to inspect imported GPG secret keys: " + stderrOrStdout(result));
        }
        String[] lines = result.stdoutText().split("\\R");
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
        CommandResult result = runCommand(Arrays.asList(
            "gpg", "--batch", "--keyserver", keyserver, "--send-keys", fingerprint
        ), null);
        if (result.exitCode == 0) {
            out.println("Public key uploaded to " + keyserver);
            return;
        }
        err.println("Warning: unable to upload public key " + fingerprint + " to " + keyserver
            + ". Continuing to verification. Details: " + stderrOrStdout(result));
    }

    private boolean keyVisibleOnServer(String keyserver, String fingerprint) throws IOException, InterruptedException {
        Path tempHome = Files.createTempDirectory("javachanges-gpg-");
        ReleaseProcessUtils.restrictOwnerOnly(tempHome);
        try {
            CommandResult result = runCommand(Arrays.asList(
                "gpg", "--batch", "--homedir", tempHome.toString(), "--keyserver", keyserver, "--recv-keys", fingerprint
            ), tempHome);
            return result.exitCode == 0;
        } finally {
            ReleaseProcessUtils.deleteRecursively(tempHome);
        }
    }

    private CommandResult runCommand(List<String> command, Path customHome) throws IOException, InterruptedException {
        if (customHome != null) {
            return ReleaseProcessUtils.runCapture(workingDirectory, new ArrayList<String>(command),
                Collections.singletonMap("GNUPGHOME", customHome.toString()));
        }
        return ReleaseProcessUtils.runCapture(workingDirectory, new ArrayList<String>(command));
    }

    private static String stderrOrStdout(CommandResult result) {
        String value = trimToNull(result.stderrText());
        if (value != null) {
            return value;
        }
        value = trimToNull(result.stdoutText());
        return value == null ? "no process output" : value;
    }
}
