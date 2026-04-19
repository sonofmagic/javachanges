package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.assertKnownModule;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.releaseModuleFromTag;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.releaseVersionFromTag;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.stripSnapshot;

final class VersionSupport {
    private final Path repoRoot;

    VersionSupport(Path repoRoot) {
        this.repoRoot = repoRoot;
    }

    String readRevision() throws IOException {
        String content = new String(Files.readAllBytes(repoRoot.resolve("pom.xml")), StandardCharsets.UTF_8);
        Pattern pattern = Pattern.compile("<revision>([^<]+)</revision>");
        Matcher matcher = pattern.matcher(content);
        if (!matcher.find()) {
            throw new IllegalStateException("未在 pom.xml 中找到 <revision> 配置");
        }
        return matcher.group(1).trim();
    }

    void assertSnapshot() throws IOException {
        String version = readRevision();
        if (!version.endsWith("-SNAPSHOT")) {
            throw new IllegalStateException("当前项目版本不是 SNAPSHOT: " + version);
        }
    }

    void assertReleaseTag(String tag) throws IOException {
        String version = readRevision();
        String releaseVersion = releaseVersionFromTag(tag);
        String module = releaseModuleFromTag(tag);
        String baseVersion = stripSnapshot(version);
        if (module != null) {
            assertKnownModule(repoRoot, module);
        }
        if (!baseVersion.equals(releaseVersion)) {
            throw new IllegalStateException("tag " + tag + " 与项目版本 " + version + " 不匹配，期望基础版本为 " + releaseVersion);
        }
    }
}
