package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static io.github.sonofmagic.javachanges.core.ReleaseTextUtils.trimToNull;
import static io.github.sonofmagic.javachanges.core.ReleaseTextUtils.xmlEscape;

public final class MavenSettingsWriter {
    public enum RepositoryMode {
        RELEASE,
        SNAPSHOT
    }

    private MavenSettingsWriter() {
    }

    public static void write(Path outputPath) throws IOException {
        write(outputPath, true, true);
    }

    public static void write(Path outputPath, RepositoryMode mode) throws IOException {
        write(outputPath, mode == RepositoryMode.RELEASE, mode == RepositoryMode.SNAPSHOT);
    }

    private static void write(Path outputPath, boolean includeRelease, boolean includeSnapshot) throws IOException {
        String releaseUsername = firstNonBlank(
            System.getenv("MAVEN_RELEASE_REPOSITORY_USERNAME"),
            System.getenv("MAVEN_CENTRAL_USERNAME"),
            System.getenv("MAVEN_REPOSITORY_USERNAME")
        );
        String releasePassword = firstNonBlank(
            System.getenv("MAVEN_RELEASE_REPOSITORY_PASSWORD"),
            System.getenv("MAVEN_CENTRAL_PASSWORD"),
            System.getenv("MAVEN_REPOSITORY_PASSWORD")
        );
        String snapshotUsername = firstNonBlank(
            System.getenv("MAVEN_SNAPSHOT_REPOSITORY_USERNAME"),
            System.getenv("MAVEN_CENTRAL_USERNAME"),
            System.getenv("MAVEN_REPOSITORY_USERNAME")
        );
        String snapshotPassword = firstNonBlank(
            System.getenv("MAVEN_SNAPSHOT_REPOSITORY_PASSWORD"),
            System.getenv("MAVEN_CENTRAL_PASSWORD"),
            System.getenv("MAVEN_REPOSITORY_PASSWORD")
        );
        String centralUsername = trimToNull(System.getenv("MAVEN_CENTRAL_USERNAME"));
        String centralPassword = trimToNull(System.getenv("MAVEN_CENTRAL_PASSWORD"));

        if (includeRelease && (releaseUsername == null || releasePassword == null)) {
            throw new IllegalStateException(ReleaseMessages.missingRepositoryCredentials(
                "release",
                "MAVEN_RELEASE_REPOSITORY_USERNAME",
                "MAVEN_RELEASE_REPOSITORY_PASSWORD"
            ));
        }
        if (includeSnapshot && (snapshotUsername == null || snapshotPassword == null)) {
            throw new IllegalStateException(ReleaseMessages.missingRepositoryCredentials(
                "snapshot",
                "MAVEN_SNAPSHOT_REPOSITORY_USERNAME",
                "MAVEN_SNAPSHOT_REPOSITORY_PASSWORD"
            ));
        }

        Path parent = outputPath.toAbsolutePath().normalize().getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        StringBuilder xml = new StringBuilder();
        xml.append("<settings xmlns=\"http://maven.apache.org/SETTINGS/1.0.0\"\n");
        xml.append("          xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        xml.append("          xsi:schemaLocation=\"http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd\">\n");
        xml.append("  <servers>\n");
        if (includeRelease) {
            appendServer(xml, releaseServerId(), releaseUsername, releasePassword);
        }
        if (includeSnapshot) {
            appendServer(xml, snapshotServerId(), snapshotUsername, snapshotPassword);
        }
        if (centralUsername != null && centralPassword != null && shouldWriteCentralServer(includeRelease, includeSnapshot)) {
            appendServer(xml, "central", centralUsername, centralPassword);
        }
        xml.append("  </servers>\n");
        xml.append("</settings>\n");
        Files.write(outputPath, Collections.singletonList(xml.toString()), StandardCharsets.UTF_8);
        ReleaseProcessUtils.restrictOwnerOnly(outputPath);
    }

    public static String releaseServerId() {
        String id = trimToNull(System.getenv("MAVEN_RELEASE_REPOSITORY_ID"));
        return id == null ? "maven-releases" : id;
    }

    public static String snapshotServerId() {
        String id = trimToNull(System.getenv("MAVEN_SNAPSHOT_REPOSITORY_ID"));
        return id == null ? "maven-snapshots" : id;
    }

    private static String firstNonBlank(String first, String second, String third) {
        return ReleaseTextUtils.firstNonBlank(first, ReleaseTextUtils.firstNonBlank(second, third));
    }

    private static boolean shouldWriteCentralServer(boolean includeRelease, boolean includeSnapshot) {
        boolean releaseAlreadyCentral = includeRelease && "central".equals(releaseServerId());
        boolean snapshotAlreadyCentral = includeSnapshot && "central".equals(snapshotServerId());
        return !releaseAlreadyCentral && !snapshotAlreadyCentral;
    }

    private static void appendServer(StringBuilder xml, String id, String username, String password) {
        xml.append("    <server>\n");
        xml.append("      <id>").append(xmlEscape(id)).append("</id>\n");
        xml.append("      <username>").append(xmlEscape(username)).append("</username>\n");
        xml.append("      <password>").append(xmlEscape(password)).append("</password>\n");
        xml.append("    </server>\n");
    }
}
