package io.github.sonofmagic.javachanges.core;

import java.util.Arrays;
import java.util.List;

final class ReleaseEnvCatalog {
    static final List<EnvEntry> COMMON_VARIABLES = Arrays.asList(
        new EnvEntry("MAVEN_RELEASE_REPOSITORY_URL", false, false),
        new EnvEntry("MAVEN_SNAPSHOT_REPOSITORY_URL", false, false),
        new EnvEntry("MAVEN_RELEASE_REPOSITORY_ID", false, false),
        new EnvEntry("MAVEN_SNAPSHOT_REPOSITORY_ID", false, false),
        new EnvEntry("MAVEN_REPOSITORY_USERNAME", true, false),
        new EnvEntry("MAVEN_REPOSITORY_PASSWORD", true, false),
        new EnvEntry("MAVEN_RELEASE_REPOSITORY_USERNAME", true, false),
        new EnvEntry("MAVEN_RELEASE_REPOSITORY_PASSWORD", true, false),
        new EnvEntry("MAVEN_SNAPSHOT_REPOSITORY_USERNAME", true, false),
        new EnvEntry("MAVEN_SNAPSHOT_REPOSITORY_PASSWORD", true, false)
    );

    static final List<EnvEntry> GITHUB_ACTIONS_VARIABLES = Arrays.asList(
        new EnvEntry("MAVEN_RELEASE_REPOSITORY_URL", false, false),
        new EnvEntry("MAVEN_SNAPSHOT_REPOSITORY_URL", false, false),
        new EnvEntry("MAVEN_RELEASE_REPOSITORY_ID", false, false),
        new EnvEntry("MAVEN_SNAPSHOT_REPOSITORY_ID", false, false)
    );

    static final List<EnvEntry> GITHUB_ACTIONS_SECRETS = Arrays.asList(
        new EnvEntry("MAVEN_REPOSITORY_USERNAME", true, false),
        new EnvEntry("MAVEN_REPOSITORY_PASSWORD", true, false),
        new EnvEntry("MAVEN_RELEASE_REPOSITORY_USERNAME", true, false),
        new EnvEntry("MAVEN_RELEASE_REPOSITORY_PASSWORD", true, false),
        new EnvEntry("MAVEN_SNAPSHOT_REPOSITORY_USERNAME", true, false),
        new EnvEntry("MAVEN_SNAPSHOT_REPOSITORY_PASSWORD", true, false)
    );

    static final List<EnvEntry> GITLAB_VARIABLES = Arrays.asList(
        new EnvEntry("MAVEN_RELEASE_REPOSITORY_URL", false, false),
        new EnvEntry("MAVEN_SNAPSHOT_REPOSITORY_URL", false, false),
        new EnvEntry("MAVEN_RELEASE_REPOSITORY_ID", false, false),
        new EnvEntry("MAVEN_SNAPSHOT_REPOSITORY_ID", false, false),
        new EnvEntry("MAVEN_REPOSITORY_USERNAME", true, true),
        new EnvEntry("MAVEN_REPOSITORY_PASSWORD", true, true),
        new EnvEntry("MAVEN_RELEASE_REPOSITORY_USERNAME", true, true),
        new EnvEntry("MAVEN_RELEASE_REPOSITORY_PASSWORD", true, true),
        new EnvEntry("MAVEN_SNAPSHOT_REPOSITORY_USERNAME", true, true),
        new EnvEntry("MAVEN_SNAPSHOT_REPOSITORY_PASSWORD", true, true),
        new EnvEntry("GITLAB_RELEASE_TOKEN", true, true)
    );

    private ReleaseEnvCatalog() {
    }
}
