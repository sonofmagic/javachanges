package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

final class ReleaseUtils {
    static final String CHANGESETS_DIR = ".changesets";
    static final String CHANGESETS_README = "README.md";
    static final String RELEASE_PLAN_JSON = "release-plan.json";
    static final String RELEASE_PLAN_MD = "release-plan.md";
    static final List<String> CHANGELOG_TYPE_ORDER = Collections.unmodifiableList(Arrays.asList(
        "breaking",
        "feat",
        "fix",
        "perf",
        "refactor",
        "build",
        "docs",
        "test",
        "ci",
        "chore",
        "other"
    ));

    private ReleaseUtils() {
    }

    static Platform platformOption(Map<String, String> options) {
        return ReleaseTextUtils.platformOption(options);
    }

    static Map<String, String> parseOptions(String[] args, int fromIndex) {
        return ReleaseTextUtils.parseOptions(args, fromIndex);
    }

    static byte[] readAllBytes(InputStream inputStream) throws IOException {
        return ReleaseProcessUtils.readAllBytes(inputStream);
    }

    static ReleaseLevel maxReleaseLevel(List<Changeset> changesets) {
        return ReleaseTextUtils.maxReleaseLevel(changesets);
    }

    static String stripSnapshot(String version) {
        return ReleaseTextUtils.stripSnapshot(version);
    }

    static List<String> detectKnownModules(Path repoRoot) {
        return ReleaseModuleUtils.detectKnownModules(repoRoot);
    }

    static void assertKnownModule(Path repoRoot, String module) {
        ReleaseModuleUtils.assertKnownModule(repoRoot, module);
    }

    static String moduleSelectorArgs(Path repoRoot, String module) {
        return ReleaseModuleUtils.moduleSelectorArgs(repoRoot, module);
    }

    static String releaseVersionFromTag(String tag) {
        return ReleaseModuleUtils.releaseVersionFromTag(tag);
    }

    static String releaseModuleFromTag(String tag) {
        return ReleaseModuleUtils.releaseModuleFromTag(tag);
    }

    static String requiredOption(Map<String, String> options, String name) {
        return ReleaseTextUtils.requiredOption(options, name);
    }

    static boolean isTrue(String value) {
        return ReleaseTextUtils.isTrue(value);
    }

    static String requireEnv(String name) {
        return ReleaseTextUtils.requireEnv(name);
    }

    static String firstNonBlank(String first, String second) {
        return ReleaseTextUtils.firstNonBlank(first, second);
    }

    static boolean isBlank(String value) {
        return ReleaseTextUtils.isBlank(value);
    }

    static String xmlEscape(String value) {
        return ReleaseTextUtils.xmlEscape(value);
    }

    static String gitTextAllowEmpty(Path repoRoot, String... args) throws IOException, InterruptedException {
        return ReleaseProcessUtils.gitTextAllowEmpty(repoRoot, args);
    }

    static int runCommand(List<String> command, Path workingDirectory) throws IOException, InterruptedException {
        return ReleaseProcessUtils.runCommand(command, workingDirectory);
    }

    static String renderCommand(List<String> command) {
        return ReleaseTextUtils.renderCommand(command);
    }

    static String shellEscape(String value) {
        return ReleaseTextUtils.shellEscape(value);
    }

    static String mavenWrapperPath() {
        return ReleaseProcessUtils.mavenWrapperPath();
    }

    static MavenCommand resolveMavenCommand(Path repoRoot) throws IOException, InterruptedException {
        return ReleaseProcessUtils.resolveMavenCommand(repoRoot);
    }

    static MavenCommand resolveMavenCommand(Path repoRoot, MavenCommandProbe probe) throws IOException, InterruptedException {
        return ReleaseProcessUtils.resolveMavenCommand(repoRoot, probe);
    }

    static Map<String, Map<String, String>> parseFlatJsonObjects(String json) {
        return ReleaseJsonUtils.parseFlatJsonObjects(json);
    }

    static List<String> parseModules(Path repoRoot, String rawModules) {
        return ReleaseModuleUtils.parseModules(repoRoot, rawModules);
    }

    static String normalizeType(String rawType) {
        return ReleaseModuleUtils.normalizeType(rawType);
    }

    static String joinModules(List<String> modules) {
        return ReleaseModuleUtils.joinModules(modules);
    }

    static String trimToNull(String value) {
        return ReleaseTextUtils.trimToNull(value);
    }

    static String trimTrailingBlankLines(List<String> lines) {
        return ReleaseTextUtils.trimTrailingBlankLines(lines);
    }

    static String changeTypeHeading(String type) {
        return ReleaseTextUtils.changeTypeHeading(type);
    }

    static String releaseLevelHeading(ReleaseLevel level) {
        return ReleaseTextUtils.releaseLevelHeading(level);
    }

    static String renderVisibleType(String type) {
        return ReleaseTextUtils.renderVisibleType(type);
    }

    static String firstBodyLine(String body) {
        return ReleaseTextUtils.firstBodyLine(body);
    }

    static String jsonEscape(String value) {
        return ReleaseJsonUtils.jsonEscape(value);
    }

    static String jsonUnescape(String value) {
        return ReleaseJsonUtils.jsonUnescape(value);
    }

    static boolean isPlaceholderValue(String value) {
        return ReleaseTextUtils.isPlaceholderValue(value);
    }

    static boolean isRequiredName(String name) {
        return ReleaseTextUtils.isRequiredName(name);
    }

    static String stripWrappingQuotes(String value) {
        return ReleaseTextUtils.stripWrappingQuotes(value);
    }

    static String padRight(String value, int width) {
        return ReleaseTextUtils.padRight(value, width);
    }

    static void closeQuietly(InputStream inputStream) {
        ReleaseProcessUtils.closeQuietly(inputStream);
    }
}
