package io.github.sonofmagic.javachanges.core.automation;

import io.github.sonofmagic.javachanges.core.CommandResult;
import io.github.sonofmagic.javachanges.core.ReleaseModuleUtils;
import io.github.sonofmagic.javachanges.core.ReleaseProcessUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ReleaseNotesGenerator {
    private final Path repoRoot;

    public ReleaseNotesGenerator(Path repoRoot) {
        this.repoRoot = repoRoot;
    }

    public void writeReleaseNotes(String tag, Path outputPath) throws IOException, InterruptedException {
        String releaseVersion = ReleaseModuleUtils.releaseVersionFromTag(tag);
        String releaseDate = gitSingleLine("log", "-1", "--format=%cs", tag);

        String changelogSection = readChangelogSection(repoRoot.resolve("CHANGELOG.md"), releaseVersion);
        Files.createDirectories(outputPath.getParent());
        if (changelogSection != null) {
            Files.write(outputPath, Collections.singletonList(changelogSection), StandardCharsets.UTF_8);
            return;
        }

        String releaseModule = ReleaseModuleUtils.releaseModuleFromTag(tag);
        String tagMatch = releaseModule == null ? "v*" : releaseModule + "/v*";
        String previousTag = gitSingleLineAllowEmpty("describe", "--tags", "--abbrev=0", "--match", tagMatch, tag + "^");
        String commitRange = previousTag == null || previousTag.isEmpty() ? tag : previousTag + ".." + tag;
        String gitLog = gitText("log", "--no-merges", "--reverse", "--format=%h%x1f%s%x1f%b%x1e", commitRange);
        String rendered = renderGitHistoryReleaseNotes(releaseVersion, releaseDate, gitLog);
        Files.write(outputPath, Collections.singletonList(rendered), StandardCharsets.UTF_8);
    }

    private String readChangelogSection(Path changelogPath, String releaseVersion) throws IOException {
        if (!Files.exists(changelogPath)) {
            return null;
        }
        List<String> lines = Files.readAllLines(changelogPath, StandardCharsets.UTF_8);
        String heading = "## " + releaseVersion + " - ";
        StringBuilder builder = new StringBuilder();
        boolean capture = false;
        for (String line : lines) {
            if (line.startsWith(heading)) {
                capture = true;
            }
            if (!capture) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(line);
            if (capture && line.startsWith("## ") && !line.startsWith(heading) && builder.length() > 0) {
                break;
            }
        }
        String value = builder.toString().trim();
        if (value.isEmpty()) {
            return null;
        }
        String[] sections = value.split("\\n(?=## )");
        return sections.length == 0 ? null : sections[0].trim();
    }

    private String renderGitHistoryReleaseNotes(String releaseVersion, String releaseDate, String gitLog) {
        Map<String, List<String>> sections = new LinkedHashMap<String, List<String>>();
        for (String key : Arrays.asList("Breaking Changes", "Features", "Fixes", "Performance",
            "Refactoring", "Build", "Documentation", "Tests", "CI", "Chores", "Other")) {
            sections.put(key, new ArrayList<String>());
        }

        String[] records = gitLog.split("\u001e");
        for (String record : records) {
            String trimmedRecord = record.trim();
            if (trimmedRecord.isEmpty()) {
                continue;
            }
            String[] fields = trimmedRecord.split("\u001f", 3);
            String sha = fields.length > 0 ? fields[0].trim() : "";
            String subject = fields.length > 1 ? fields[1].trim() : "";
            String body = fields.length > 2 ? fields[2].replace("\r", "") : "";
            if (subject.isEmpty() || subject.startsWith("Merge ")) {
                continue;
            }
            if (subject.startsWith("chore(release): ")) {
                continue;
            }

            String description = normalizeCommitDescription(subject);
            String section = commitSection(subject);
            sections.get(section).add(description + " (`" + sha + "`)");
            if (subject.contains("!:") || body.contains("BREAKING CHANGE:")) {
                sections.get("Breaking Changes").add(description + " (`" + sha + "`)");
            }
        }

        StringBuilder builder = new StringBuilder();
        builder.append("## ").append(releaseVersion).append(" - ").append(releaseDate).append("\n\n");
        boolean found = false;
        for (Map.Entry<String, List<String>> entry : sections.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            builder.append("### ").append(entry.getKey()).append("\n\n");
            for (String line : entry.getValue()) {
                builder.append("- ").append(line).append("\n");
            }
            builder.append("\n");
            found = true;
        }
        if (!found) {
            builder.append("- No user-facing changes were recorded for this release.\n");
        }
        return builder.toString().trim();
    }

    private String commitSection(String subject) {
        if (subject.matches("^feat(\\([^)]+\\))?!?: .+")) {
            return "Features";
        }
        if (subject.matches("^fix(\\([^)]+\\))?!?: .+")) {
            return "Fixes";
        }
        if (subject.matches("^perf(\\([^)]+\\))?!?: .+")) {
            return "Performance";
        }
        if (subject.matches("^refactor(\\([^)]+\\))?!?: .+")) {
            return "Refactoring";
        }
        if (subject.matches("^(build|deps)(\\([^)]+\\))?!?: .+")) {
            return "Build";
        }
        if (subject.matches("^docs(\\([^)]+\\))?!?: .+")) {
            return "Documentation";
        }
        if (subject.matches("^test(\\([^)]+\\))?!?: .+")) {
            return "Tests";
        }
        if (subject.matches("^ci(\\([^)]+\\))?!?: .+")) {
            return "CI";
        }
        if (subject.matches("^chore(\\([^)]+\\))?!?: .+")) {
            return "Chores";
        }
        return "Other";
    }

    private String normalizeCommitDescription(String subject) {
        return subject.replaceFirst("^[[:alpha:]]+(\\([^)]+\\))?!?:\\s*", "").trim();
    }

    private String gitSingleLine(String... args) throws IOException, InterruptedException {
        String value = gitText(args).trim();
        if (value.isEmpty()) {
            throw new IllegalStateException("git returned empty output for " + Arrays.asList(args));
        }
        return value.split("\\r?\\n")[0].trim();
    }

    private String gitSingleLineAllowEmpty(String... args) throws IOException, InterruptedException {
        return gitTextAllowEmpty(args).trim();
    }

    private String gitText(String... args) throws IOException, InterruptedException {
        String value = gitTextAllowEmpty(args);
        if (value.trim().isEmpty()) {
            throw new IllegalStateException("git returned empty output for " + Arrays.asList(args));
        }
        return value;
    }

    private String gitTextAllowEmpty(String... args) throws IOException, InterruptedException {
        String[] command = new String[args.length + 1];
        command[0] = "git";
        System.arraycopy(args, 0, command, 1, args.length);
        CommandResult result = ReleaseProcessUtils.runCapture(repoRoot, command);
        if (result.exitCode != 0) {
            String error = result.stderrText().trim();
            if (error.isEmpty()) {
                error = "git command failed";
            }
            if (Arrays.asList(args).contains("describe")) {
                return "";
            }
            throw new IllegalStateException(error);
        }
        return result.stdoutText();
    }
}
