package io.github.sonofmagic.javachanges.gradle;

import io.github.sonofmagic.javachanges.core.JavaChangesVersion;
import io.github.sonofmagic.javachanges.core.ReleaseMessages;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class GradleTasksSupport {
    public static final String DEFAULT_OUTPUT = "gradle/javachanges.gradle";
    private static final String TEMPLATE_RESOURCE =
        "io/github/sonofmagic/javachanges/gradle/javachanges.gradle.template";
    private static final String VERSION_PLACEHOLDER = "__JAVACHANGES_VERSION__";

    private GradleTasksSupport() {
    }

    public static String render(String javachangesVersion) {
        return template().replace(VERSION_PLACEHOLDER, JavaChangesVersion.releasedVersion(javachangesVersion));
    }

    public static Path applyToBuildFile(Path repoRoot, Path scriptPath) throws IOException {
        Path buildFile = GradleModelSupport.buildFile(repoRoot);
        if (buildFile == null) {
            throw new IllegalStateException(ReleaseMessages.cannotFindGradleBuildFile(repoRoot));
        }
        String relativeScriptPath = repoRoot.relativize(scriptPath).toString().replace('\\', '/');
        String applyLine = buildFile.getFileName().toString().endsWith(".kts")
            ? "apply(from = \"" + relativeScriptPath + "\")"
            : "apply from: \"" + relativeScriptPath + "\"";
        String content = new String(Files.readAllBytes(buildFile), StandardCharsets.UTF_8);
        if (content.contains(applyLine)) {
            return buildFile;
        }
        StringBuilder updated = new StringBuilder(content);
        if (updated.length() > 0 && updated.charAt(updated.length() - 1) != '\n') {
            updated.append('\n');
        }
        if (updated.length() > 0) {
            updated.append('\n');
        }
        updated.append(applyLine).append('\n');
        Files.write(buildFile, updated.toString().getBytes(StandardCharsets.UTF_8));
        return buildFile;
    }

    private static String template() {
        StringBuilder content = new StringBuilder();
        char[] buffer = new char[4096];
        try (
            InputStream inputStream = GradleTasksSupport.class.getClassLoader().getResourceAsStream(TEMPLATE_RESOURCE)
        ) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing Gradle tasks template: " + TEMPLATE_RESOURCE);
            }
            InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            try {
                int read;
                while ((read = reader.read(buffer)) >= 0) {
                    content.append(buffer, 0, read);
                }
            } finally {
                reader.close();
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        return content.toString();
    }
}
