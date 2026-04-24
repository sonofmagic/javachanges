package io.github.sonofmagic.javachanges.core;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.sonofmagic.javachanges.core.config.RepoRootResolver;
import io.github.sonofmagic.javachanges.core.changeset.Changeset;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class ReleaseUtils {
    public static final String CHANGESETS_DIR = ".changesets";
    public static final String CHANGESETS_README = "README.md";
    public static final String RELEASE_PLAN_JSON = "release-plan.json";
    public static final String RELEASE_PLAN_MD = "release-plan.md";
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

    public static Platform platformOption(Map<String, String> options) {
        return ReleaseTextUtils.platformOption(options);
    }

    static Map<String, String> parseOptions(String[] args, int fromIndex) {
        return ReleaseTextUtils.parseOptions(args, fromIndex);
    }

    public static byte[] readAllBytes(InputStream inputStream) throws IOException {
        return ReleaseProcessUtils.readAllBytes(inputStream);
    }

    public static Path resolveRepoRoot(String directoryOption) {
        return RepoRootResolver.resolveRepoRoot(directoryOption);
    }

    public static String readRevision(Path pomPath) throws IOException {
        return PomModelSupport.readRevision(pomPath);
    }

    public static void writeRevision(Path pomPath, String revision) throws IOException {
        PomModelSupport.writeRevision(pomPath, revision);
    }

    public static ReleaseLevel maxReleaseLevel(List<Changeset> changesets) {
        return ReleaseTextUtils.maxReleaseLevel(changesets);
    }

    public static String stripSnapshot(String version) {
        return ReleaseTextUtils.stripSnapshot(version);
    }

    public static List<String> detectKnownModules(Path repoRoot) {
        return ReleaseModuleUtils.detectKnownModules(repoRoot);
    }

    public static void assertKnownModule(Path repoRoot, String module) {
        ReleaseModuleUtils.assertKnownModule(repoRoot, module);
    }

    static String moduleSelectorArgs(Path repoRoot, String module) {
        return ReleaseModuleUtils.moduleSelectorArgs(repoRoot, module);
    }

    public static String releaseVersionFromTag(String tag) {
        return ReleaseModuleUtils.releaseVersionFromTag(tag);
    }

    public static String releaseModuleFromTag(String tag) {
        return ReleaseModuleUtils.releaseModuleFromTag(tag);
    }

    public static String releaseVersionForChanges(String currentRevision, String latestTag, ReleaseLevel releaseLevel) {
        Semver currentBaseVersion = Semver.parse(stripSnapshot(currentRevision));
        Semver latestTagVersion = latestTag == null ? currentBaseVersion : Semver.parse(latestTag.substring(1));
        Semver bumpedFromTag = latestTag == null ? currentBaseVersion.bump(releaseLevel) : latestTagVersion.bump(releaseLevel);
        return Semver.max(currentBaseVersion, bumpedFromTag).toString();
    }

    public static String requiredOption(Map<String, String> options, String name) {
        return ReleaseTextUtils.requiredOption(options, name);
    }

    public static boolean isTrue(String value) {
        return ReleaseTextUtils.isTrue(value);
    }

    public static String requireEnv(String name) {
        return ReleaseTextUtils.requireEnv(name);
    }

    public static String firstNonBlank(String first, String second) {
        return ReleaseTextUtils.firstNonBlank(first, second);
    }

    public static boolean isBlank(String value) {
        return ReleaseTextUtils.isBlank(value);
    }

    static String xmlEscape(String value) {
        return ReleaseTextUtils.xmlEscape(value);
    }

    public static String gitTextAllowEmpty(Path repoRoot, String... args) throws IOException, InterruptedException {
        return ReleaseProcessUtils.gitTextAllowEmpty(repoRoot, args);
    }

    public static int runCommand(List<String> command, Path workingDirectory) throws IOException, InterruptedException {
        return ReleaseProcessUtils.runCommand(command, workingDirectory);
    }

    public static CommandResult runCapture(Path workingDirectory, String... command) throws IOException, InterruptedException {
        return ReleaseProcessUtils.runCapture(workingDirectory, command);
    }

    public static String renderCommand(List<String> command) {
        return ReleaseTextUtils.renderCommand(command);
    }

    static String shellEscape(String value) {
        return ReleaseTextUtils.shellEscape(value);
    }

    public static String mavenWrapperPath() {
        return ReleaseProcessUtils.mavenWrapperPath();
    }

    public static MavenCommand resolveMavenCommand(Path repoRoot) throws IOException, InterruptedException {
        return ReleaseProcessUtils.resolveMavenCommand(repoRoot);
    }

    static MavenCommand resolveMavenCommand(Path repoRoot, MavenCommandProbe probe) throws IOException, InterruptedException {
        return ReleaseProcessUtils.resolveMavenCommand(repoRoot, probe);
    }

    public static Map<String, Map<String, String>> parseFlatJsonObjects(String json) {
        return ReleaseJsonUtils.parseFlatJsonObjects(json);
    }

    public static List<String> parseModules(Path repoRoot, String rawModules) {
        return ReleaseModuleUtils.parseModules(repoRoot, rawModules);
    }

    public static String normalizeType(String rawType) {
        return ReleaseModuleUtils.normalizeType(rawType);
    }

    public static String joinModules(List<String> modules) {
        return ReleaseModuleUtils.joinModules(modules);
    }

    public static String trimToNull(String value) {
        return ReleaseTextUtils.trimToNull(value);
    }

    public static String trimTrailingBlankLines(List<String> lines) {
        return ReleaseTextUtils.trimTrailingBlankLines(lines);
    }

    static String changeTypeHeading(String type) {
        return ReleaseTextUtils.changeTypeHeading(type);
    }

    public static String releaseLevelHeading(ReleaseLevel level) {
        return ReleaseTextUtils.releaseLevelHeading(level);
    }

    public static String renderVisibleType(String type) {
        return ReleaseTextUtils.renderVisibleType(type);
    }

    public static String firstBodyLine(String body) {
        return ReleaseTextUtils.firstBodyLine(body);
    }

    static String jsonEscape(String value) {
        return ReleaseJsonUtils.jsonEscape(value);
    }

    static String jsonUnescape(String value) {
        return ReleaseJsonUtils.jsonUnescape(value);
    }

    public static JsonNode readJsonTree(String json) {
        return ReleaseJsonUtils.readTree(json);
    }

    public static String toPrettyJson(Object value) {
        return ReleaseJsonUtils.toPrettyJson(value);
    }

    public static boolean isPlaceholderValue(String value) {
        return ReleaseTextUtils.isPlaceholderValue(value);
    }

    public static boolean isRequiredName(String name) {
        return ReleaseTextUtils.isRequiredName(name);
    }

    public static String stripWrappingQuotes(String value) {
        return ReleaseTextUtils.stripWrappingQuotes(value);
    }

    public static String padRight(String value, int width) {
        return ReleaseTextUtils.padRight(value, width);
    }

    static void closeQuietly(InputStream inputStream) {
        ReleaseProcessUtils.closeQuietly(inputStream);
    }
}
