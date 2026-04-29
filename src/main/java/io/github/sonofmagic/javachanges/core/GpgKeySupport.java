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
            throw new IllegalArgumentException(ReleaseMessages.attemptsMustBePositive());
        }
        if (retryDelaySeconds < 0) {
            throw new IllegalArgumentException(ReleaseMessages.retryDelayMustBeNonNegative());
        }

        String fingerprint = detectFingerprint();
        out.println(ReleaseMessages.detectedSigningKeyFingerprint(fingerprint));
        out.println(ReleaseMessages.publishingPublicKeyToKeyservers());

        sendKey(primaryKeyserver, fingerprint, out, err);
        sendKey(secondaryKeyserver, fingerprint, out, err);

        for (int attempt = 1; attempt <= attempts; attempt++) {
            if (keyVisibleOnServer(primaryKeyserver, fingerprint)) {
                out.println(ReleaseMessages.publicKeyVisibleOn(fingerprint, primaryKeyserver));
                return fingerprint;
            }
            if (keyVisibleOnServer(secondaryKeyserver, fingerprint)) {
                out.println(ReleaseMessages.publicKeyVisibleOn(fingerprint, secondaryKeyserver));
                return fingerprint;
            }
            if (attempt < attempts) {
                out.println(ReleaseMessages.publicKeyNotVisibleRetrying(
                    fingerprint, attempt, attempts, retryDelaySeconds));
                Thread.sleep(retryDelaySeconds * 1000L);
            }
        }

        throw new IllegalStateException(ReleaseMessages.signingKeyNotVisible(
            fingerprint, primaryKeyserver, secondaryKeyserver));
    }

    private String detectFingerprint() throws IOException, InterruptedException {
        CommandResult result = runCommand(Arrays.asList(
            "gpg", "--batch", "--list-secret-keys", "--with-colons", "--fingerprint"
        ), null);
        if (result.exitCode != 0) {
            throw new IllegalStateException(ReleaseMessages.failedToInspectGpgSecretKeys(stderrOrStdout(result)));
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
        throw new IllegalStateException(ReleaseMessages.noImportedSecretKeyFingerprint());
    }

    private void sendKey(String keyserver, String fingerprint, PrintStream out, PrintStream err)
        throws IOException, InterruptedException {
        CommandResult result = runCommand(Arrays.asList(
            "gpg", "--batch", "--keyserver", keyserver, "--send-keys", fingerprint
        ), null);
        if (result.exitCode == 0) {
            out.println(ReleaseMessages.publicKeyUploaded(keyserver));
            return;
        }
        err.println(ReleaseMessages.publicKeyUploadWarning(fingerprint, keyserver, stderrOrStdout(result)));
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
        return value == null ? ReleaseMessages.noProcessOutput() : value;
    }
}
