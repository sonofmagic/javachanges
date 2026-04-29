package io.github.sonofmagic.javachanges.core;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceLocalizationConsistencyTest {
    private static final Pattern HAN = Pattern.compile("\\p{IsHan}");

    @Test
    void productionJavaSourcesDoNotInlineChineseLocalizedText() throws Exception {
        List<String> violations = new ArrayList<String>();
        for (Path path : sourceFiles()) {
            String content = read(path);
            if (HAN.matcher(content).find()) {
                violations.add(relative(path));
            }
        }

        assertTrue(
            violations.isEmpty(),
            "Chinese localized text should live in message bundles or templates: " + violations
        );
    }

    @Test
    void productionJavaSourcesDoNotUseInlineDualLanguageHelper() throws Exception {
        List<String> violations = new ArrayList<String>();
        for (Path path : sourceFiles()) {
            String content = read(path);
            if (content.contains("ReleaseMessages.text(") || content.contains("return text(")) {
                violations.add(relative(path));
            }
        }

        assertTrue(
            violations.isEmpty(),
            "Use named ReleaseMessages methods instead of ReleaseMessages.text(...): " + violations
        );
    }

    private static List<Path> sourceFiles() throws Exception {
        Path root = sourceRoot();
        List<Path> files = new ArrayList<Path>();
        Stream<Path> stream = Files.walk(root);
        try {
            stream.filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".java"))
                .forEach(files::add);
        } finally {
            stream.close();
        }
        return files;
    }

    private static Path sourceRoot() {
        return repoRoot().resolve("src/main/java/io/github/sonofmagic/javachanges");
    }

    private static String read(Path path) throws Exception {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private static String relative(Path path) {
        return repoRoot().relativize(path).toString();
    }

    private static Path repoRoot() {
        return Paths.get("").toAbsolutePath().normalize();
    }
}
