package io.github.sonofmagic.javachanges.core;

import io.github.sonofmagic.javachanges.core.gitlab.GitlabReleaseRuntime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GitlabReleaseRuntimeTest {

    @Test
    void remoteTagExistsFailsClosedWhenRemoteLookupFails(@TempDir Path tempDir) {
        GitlabReleaseRuntime runtime = new GitlabReleaseRuntime(tempDir);
        Path missingRemote = tempDir.resolve("missing.git");

        assertThrows(IllegalStateException.class,
            () -> runtime.remoteTagExists("v1.2.0", missingRemote.toString()));
    }

    @Test
    void pushReleaseBranchUsesExplicitLeaseAgainstExistingRemoteBranch(@TempDir Path tempDir) throws Exception {
        Path remoteRepo = tempDir.resolve("remote.git");
        run(tempDir, "git", "init", "-q", "--bare", remoteRepo.toString());

        Path seedRepo = tempDir.resolve("seed");
        run(tempDir, "git", "init", "-q", "-b", "main", seedRepo.toString());
        run(seedRepo, "git", "config", "user.name", "tester");
        run(seedRepo, "git", "config", "user.email", "tester@example.com");
        Files.write(seedRepo.resolve("pom.xml"), "<project/>\n".getBytes(StandardCharsets.UTF_8));
        run(seedRepo, "git", "add", "pom.xml");
        run(seedRepo, "git", "commit", "-qm", "init main");
        run(seedRepo, "git", "remote", "add", "origin", remoteRepo.toString());
        run(seedRepo, "git", "push", "-q", "origin", "main");

        Path workerRepo = tempDir.resolve("worker");
        run(tempDir, "git", "clone", "-q", "-b", "main", remoteRepo.toString(), workerRepo.toString());
        run(workerRepo, "git", "config", "user.name", "tester");
        run(workerRepo, "git", "config", "user.email", "tester@example.com");

        run(seedRepo, "git", "checkout", "-q", "-B", "changeset-release/main");
        Files.write(seedRepo.resolve("stale.txt"), "stale\n".getBytes(StandardCharsets.UTF_8));
        run(seedRepo, "git", "add", "stale.txt");
        run(seedRepo, "git", "commit", "-qm", "stale release branch");
        run(seedRepo, "git", "push", "-q", "origin", "HEAD:refs/heads/changeset-release/main");

        run(workerRepo, "git", "checkout", "-q", "-B", "changeset-release/main");
        Files.write(workerRepo.resolve("fresh.txt"), "fresh\n".getBytes(StandardCharsets.UTF_8));
        run(workerRepo, "git", "add", "fresh.txt");
        run(workerRepo, "git", "commit", "-qm", "fresh release branch");

        GitlabReleaseRuntime runtime = new GitlabReleaseRuntime(workerRepo);
        String remoteHead = runtime.remoteBranchHead("changeset-release/main", remoteRepo.toString());
        runtime.pushReleaseBranch(remoteRepo.toString(), "changeset-release/main", remoteHead);

        String updatedRemoteHead = gitText(workerRepo, "ls-remote", "--heads", remoteRepo.toString(),
            "refs/heads/changeset-release/main");
        String localHead = gitText(workerRepo, "rev-parse", "HEAD");
        assertEquals(localHead.trim(), updatedRemoteHead.split("\\s+")[0]);
    }

    private static void run(Path workingDirectory, String... command) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(Arrays.asList(command));
        builder.directory(workingDirectory.toFile());
        Process process = builder.start();
        byte[] stdout = ReleaseProcessUtils.readAllBytes(process.getInputStream());
        byte[] stderr = ReleaseProcessUtils.readAllBytes(process.getErrorStream());
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("Command failed: " + Arrays.asList(command)
                + "\nstdout: " + new String(stdout, StandardCharsets.UTF_8)
                + "\nstderr: " + new String(stderr, StandardCharsets.UTF_8));
        }
    }

    private static String gitText(Path workingDirectory, String... gitArgs) throws IOException, InterruptedException {
        String[] command = new String[gitArgs.length + 1];
        command[0] = "git";
        System.arraycopy(gitArgs, 0, command, 1, gitArgs.length);
        ProcessBuilder builder = new ProcessBuilder(Arrays.asList(command));
        builder.directory(workingDirectory.toFile());
        Process process = builder.start();
        byte[] stdout = ReleaseProcessUtils.readAllBytes(process.getInputStream());
        byte[] stderr = ReleaseProcessUtils.readAllBytes(process.getErrorStream());
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("Command failed: " + Arrays.asList(command)
                + "\nstdout: " + new String(stdout, StandardCharsets.UTF_8)
                + "\nstderr: " + new String(stderr, StandardCharsets.UTF_8));
        }
        return new String(stdout, StandardCharsets.UTF_8).trim();
    }
}
