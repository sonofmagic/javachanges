package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BuildModelSupport {
    private BuildModelSupport() {
    }

    public static boolean isSupportedRepository(Path repoRoot) {
        return detect(repoRoot) != null;
    }

    static BuildModel detect(Path repoRoot) {
        Path pomPath = repoRoot.resolve("pom.xml");
        if (Files.exists(pomPath)) {
            try {
                if (!ReleaseModuleUtils.detectMavenModules(repoRoot).isEmpty()) {
                    return BuildModel.maven(pomPath);
                }
            } catch (RuntimeException ignored) {
                // Keep probing for Gradle before reporting no supported build model.
            }
        }

        Path gradlePropertiesPath = repoRoot.resolve("gradle.properties");
        if (Files.exists(gradlePropertiesPath)
            && (GradleModelSupport.settingsFile(repoRoot) != null || GradleModelSupport.buildFile(repoRoot) != null)) {
            try {
                GradleModelSupport.readRevision(gradlePropertiesPath);
                List<String> modules = GradleModelSupport.detectModules(repoRoot);
                if (!modules.isEmpty()) {
                    return BuildModel.gradle(gradlePropertiesPath);
                }
            } catch (RuntimeException ignored) {
                // No supported Gradle version property.
            } catch (IOException ignored) {
                // Unreadable build model.
            }
        }

        return null;
    }

    public static String readRevision(Path repoRoot) throws IOException {
        BuildModel model = require(repoRoot);
        if (model.type == BuildType.MAVEN) {
            return PomModelSupport.readRevision(model.versionFile);
        }
        return GradleModelSupport.readRevision(model.versionFile);
    }

    public static void writeRevision(Path repoRoot, String revision) throws IOException {
        BuildModel model = require(repoRoot);
        if (model.type == BuildType.MAVEN) {
            PomModelSupport.writeRevision(model.versionFile, revision);
            return;
        }
        GradleModelSupport.writeRevision(model.versionFile, revision);
    }

    public static List<String> detectKnownModules(Path repoRoot) {
        BuildModel model = detect(repoRoot);
        if (model == null) {
            return Collections.emptyList();
        }
        if (model.type == BuildType.MAVEN) {
            return ReleaseModuleUtils.detectMavenModules(repoRoot);
        }
        try {
            return GradleModelSupport.detectModules(repoRoot);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to detect Gradle modules from " + repoRoot, exception);
        }
    }

    public static String revisionFileLabel(Path repoRoot) {
        BuildModel model = detect(repoRoot);
        if (model == null) {
            return "build model";
        }
        return repoRoot.relativize(model.versionFile).toString();
    }

    public static String[] releasePlanGitAddPaths(Path repoRoot) {
        List<String> paths = new ArrayList<String>();
        BuildModel model = detect(repoRoot);
        if (model != null) {
            paths.add(repoRoot.relativize(model.versionFile).toString());
        }
        paths.add("CHANGELOG.md");
        paths.add(ChangesetPaths.DIR);
        return paths.toArray(new String[0]);
    }

    public static String[] releaseStateGitPaths(Path repoRoot) {
        List<String> paths = new ArrayList<String>();
        BuildModel model = detect(repoRoot);
        if (model != null) {
            paths.add(repoRoot.relativize(model.versionFile).toString());
        }
        paths.add("CHANGELOG.md");
        return paths.toArray(new String[0]);
    }

    private static BuildModel require(Path repoRoot) {
        BuildModel model = detect(repoRoot);
        if (model == null) {
            throw new IllegalStateException("Cannot find supported Maven or Gradle build model in " + repoRoot);
        }
        return model;
    }

    enum BuildType {
        MAVEN,
        GRADLE
    }

    static final class BuildModel {
        final BuildType type;
        final Path versionFile;

        private BuildModel(BuildType type, Path versionFile) {
            this.type = type;
            this.versionFile = versionFile;
        }

        static BuildModel maven(Path versionFile) {
            return new BuildModel(BuildType.MAVEN, versionFile);
        }

        static BuildModel gradle(Path versionFile) {
            return new BuildModel(BuildType.GRADLE, versionFile);
        }
    }
}
