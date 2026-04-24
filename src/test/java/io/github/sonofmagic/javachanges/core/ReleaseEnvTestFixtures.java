package io.github.sonofmagic.javachanges.core;

import io.github.sonofmagic.javachanges.core.env.ReleaseEnvRuntime;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

final class ReleaseEnvTestFixtures {
    private ReleaseEnvTestFixtures() {
    }

    static String supportEnvFile() {
        return ""
            + "MAVEN_RELEASE_REPOSITORY_URL=https://repo.example.com/releases\n"
            + "MAVEN_SNAPSHOT_REPOSITORY_URL=https://repo.example.com/snapshots\n"
            + "MAVEN_RELEASE_REPOSITORY_ID=maven-releases\n"
            + "MAVEN_SNAPSHOT_REPOSITORY_ID=maven-snapshots\n"
            + "MAVEN_REPOSITORY_USERNAME=ci-user\n"
            + "MAVEN_REPOSITORY_PASSWORD=ci-password\n"
            + "GITLAB_RELEASE_TOKEN=glrt-token\n";
    }

    static String cliEnvFile() {
        return ""
            + "MAVEN_RELEASE_REPOSITORY_URL=https://repo.example.com/maven-releases/\n"
            + "MAVEN_SNAPSHOT_REPOSITORY_URL=https://repo.example.com/maven-snapshots/\n"
            + "MAVEN_RELEASE_REPOSITORY_ID=maven-releases\n"
            + "MAVEN_SNAPSHOT_REPOSITORY_ID=maven-snapshots\n"
            + "MAVEN_REPOSITORY_USERNAME=replace-me\n"
            + "MAVEN_REPOSITORY_PASSWORD=replace-me\n";
    }

    static void writeLocalEnv(Path repoRoot, String content) throws IOException {
        Path envDir = repoRoot.resolve("env");
        Files.createDirectories(envDir);
        Files.write(envDir.resolve("release.env.local"), content.getBytes(StandardCharsets.UTF_8));
    }

    static final class FakeReleaseEnvRuntime extends ReleaseEnvRuntime {
        FakeReleaseEnvRuntime(Path repoRoot) {
            super(repoRoot);
        }

        @Override
        public boolean commandExists(String command) {
            return true;
        }

        @Override
        public void requireCommand(String command) {
        }

        @Override
        public boolean runQuietly(java.util.List<String> command) {
            return true;
        }

        @Override
        public boolean commandAvailable(String... command) {
            return true;
        }

        @Override
        public CommandResult runAndCapture(java.util.List<String> command) throws IOException {
            String joined = command.toString();
            if (joined.contains("gh, variable, list")) {
                return json("["
                    + "{\"name\":\"MAVEN_RELEASE_REPOSITORY_URL\",\"value\":\"https://repo.example.com/releases\",\"updatedAt\":\"2026-04-22T10:00:00Z\"},"
                    + "{\"name\":\"MAVEN_SNAPSHOT_REPOSITORY_URL\",\"value\":\"https://repo.example.com/snapshots\",\"updatedAt\":\"2026-04-22T10:00:00Z\"},"
                    + "{\"name\":\"MAVEN_RELEASE_REPOSITORY_ID\",\"value\":\"maven-releases\",\"updatedAt\":\"2026-04-22T10:00:00Z\"},"
                    + "{\"name\":\"MAVEN_SNAPSHOT_REPOSITORY_ID\",\"value\":\"maven-snapshots\",\"updatedAt\":\"2026-04-22T10:00:00Z\"}"
                    + "]");
            }
            if (joined.contains("gh, secret, list")) {
                return json("["
                    + "{\"name\":\"MAVEN_REPOSITORY_USERNAME\",\"updatedAt\":\"2026-04-22T10:05:00Z\"},"
                    + "{\"name\":\"MAVEN_REPOSITORY_PASSWORD\",\"updatedAt\":\"2026-04-22T10:05:00Z\"}"
                    + "]");
            }
            if (joined.contains("/variables?per_page=100")) {
                return json("["
                    + "{\"key\":\"MAVEN_REPOSITORY_USERNAME\",\"protected\":true,\"masked\":true},"
                    + "{\"key\":\"MAVEN_REPOSITORY_PASSWORD\",\"protected\":true,\"masked\":true},"
                    + "{\"key\":\"GITLAB_RELEASE_TOKEN\",\"protected\":true,\"masked\":true}"
                    + "]");
            }
            if (joined.contains("/protected_branches?per_page=100")) {
                return json("[]");
            }
            throw new IllegalStateException("Unexpected command: " + Arrays.asList(command));
        }

        private CommandResult json(String value) {
            return new CommandResult(0, value.getBytes(StandardCharsets.UTF_8), new byte[0]);
        }
    }
}
