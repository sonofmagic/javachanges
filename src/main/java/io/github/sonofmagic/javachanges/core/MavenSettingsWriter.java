package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.firstNonBlank;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.trimToNull;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.xmlEscape;

final class MavenSettingsWriter {
    private MavenSettingsWriter() {
    }

    static void write(Path outputPath) throws IOException {
        String releaseUsername = firstNonBlank(
            System.getenv("MAVEN_RELEASE_REPOSITORY_USERNAME"),
            System.getenv("MAVEN_REPOSITORY_USERNAME")
        );
        String releasePassword = firstNonBlank(
            System.getenv("MAVEN_RELEASE_REPOSITORY_PASSWORD"),
            System.getenv("MAVEN_REPOSITORY_PASSWORD")
        );
        String snapshotUsername = firstNonBlank(
            System.getenv("MAVEN_SNAPSHOT_REPOSITORY_USERNAME"),
            System.getenv("MAVEN_REPOSITORY_USERNAME")
        );
        String snapshotPassword = firstNonBlank(
            System.getenv("MAVEN_SNAPSHOT_REPOSITORY_PASSWORD"),
            System.getenv("MAVEN_REPOSITORY_PASSWORD")
        );

        if (releaseUsername == null || releasePassword == null) {
            throw new IllegalStateException("缺少 release 仓库认证信息，请设置 MAVEN_RELEASE_REPOSITORY_USERNAME/MAVEN_RELEASE_REPOSITORY_PASSWORD 或通用 MAVEN_REPOSITORY_USERNAME/MAVEN_REPOSITORY_PASSWORD");
        }
        if (snapshotUsername == null || snapshotPassword == null) {
            throw new IllegalStateException("缺少 snapshot 仓库认证信息，请设置 MAVEN_SNAPSHOT_REPOSITORY_USERNAME/MAVEN_SNAPSHOT_REPOSITORY_PASSWORD 或通用 MAVEN_REPOSITORY_USERNAME/MAVEN_REPOSITORY_PASSWORD");
        }

        Path parent = outputPath.toAbsolutePath().normalize().getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        String xml = "<settings xmlns=\"http://maven.apache.org/SETTINGS/1.0.0\"\n"
            + "          xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "          xsi:schemaLocation=\"http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd\">\n"
            + "  <servers>\n"
            + "    <server>\n"
            + "      <id>" + xmlEscape(releaseServerId()) + "</id>\n"
            + "      <username>" + xmlEscape(releaseUsername) + "</username>\n"
            + "      <password>" + xmlEscape(releasePassword) + "</password>\n"
            + "    </server>\n"
            + "    <server>\n"
            + "      <id>" + xmlEscape(snapshotServerId()) + "</id>\n"
            + "      <username>" + xmlEscape(snapshotUsername) + "</username>\n"
            + "      <password>" + xmlEscape(snapshotPassword) + "</password>\n"
            + "    </server>\n"
            + "  </servers>\n"
            + "</settings>\n";
        Files.write(outputPath, Collections.singletonList(xml), StandardCharsets.UTF_8);
    }

    static String releaseServerId() {
        String id = trimToNull(System.getenv("MAVEN_RELEASE_REPOSITORY_ID"));
        return id == null ? "maven-releases" : id;
    }

    static String snapshotServerId() {
        String id = trimToNull(System.getenv("MAVEN_SNAPSHOT_REPOSITORY_ID"));
        return id == null ? "maven-snapshots" : id;
    }
}
