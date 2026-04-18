package io.github.sonofmagic.javachanges;

import java.io.Console;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

import static io.github.sonofmagic.javachanges.ReleaseUtils.*;

enum Platform {
    GITHUB("github"),
    GITLAB("gitlab"),
    ALL("all");

    final String id;

    Platform(String id) {
        this.id = id;
    }

    static Platform parse(String value, Platform defaultValue) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return defaultValue;
        }
        for (Platform platform : values()) {
            if (platform.id.equalsIgnoreCase(normalized)) {
                return platform;
            }
        }
        throw new IllegalArgumentException("不支持的平台: " + value + "，可选值: github, gitlab, all");
    }

    boolean includesGithub() {
        return this == GITHUB || this == ALL;
    }

    boolean includesGitlab() {
        return this == GITLAB || this == ALL;
    }
}

final class InitEnvRequest {
    final String template;
    final String target;
    final boolean force;

    private InitEnvRequest(String template, String target, boolean force) {
        this.template = template;
        this.target = target;
        this.force = force;
    }

    static InitEnvRequest fromOptions(Map<String, String> options) {
        String target = trimToNull(options.get("target"));
        if (target == null) {
            target = trimToNull(options.get("path"));
        }
        return new InitEnvRequest(
            trimToNull(options.get("template")) == null ? "env/release.env.example" : trimToNull(options.get("template")),
            target == null ? "env/release.env.local" : target,
            isTrue(options.get("force"))
        );
    }
}

final class PlatformEnvRequest {
    final String envFile;
    final Platform platform;
    final boolean showSecrets;

    private PlatformEnvRequest(String envFile, Platform platform, boolean showSecrets) {
        this.envFile = envFile;
        this.platform = platform;
        this.showSecrets = showSecrets;
    }

    static PlatformEnvRequest fromOptions(Map<String, String> options) {
        return new PlatformEnvRequest(
            requiredOption(options, "env-file"),
            platformOption(options),
            isTrue(options.get("show-secrets"))
        );
    }
}

final class LocalDoctorRequest {
    final String envFile;
    final String githubRepo;
    final String gitlabRepo;

    private LocalDoctorRequest(String envFile, String githubRepo, String gitlabRepo) {
        this.envFile = envFile;
        this.githubRepo = githubRepo;
        this.gitlabRepo = gitlabRepo;
    }

    static LocalDoctorRequest fromOptions(Map<String, String> options) {
        return new LocalDoctorRequest(
            trimToNull(options.get("env-file")) == null ? "env/release.env.local" : trimToNull(options.get("env-file")),
            trimToNull(options.get("github-repo")),
            trimToNull(options.get("gitlab-repo"))
        );
    }
}

final class DoctorPlatformRequest {
    final String envFile;
    final Platform platform;
    final String githubRepo;
    final String gitlabRepo;

    private DoctorPlatformRequest(String envFile, Platform platform, String githubRepo, String gitlabRepo) {
        this.envFile = envFile;
        this.platform = platform;
        this.githubRepo = githubRepo;
        this.gitlabRepo = gitlabRepo;
    }

    static DoctorPlatformRequest fromOptions(Map<String, String> options) {
        return new DoctorPlatformRequest(
            requiredOption(options, "env-file"),
            platformOption(options),
            trimToNull(options.get("github-repo")),
            trimToNull(options.get("gitlab-repo"))
        );
    }
}

final class SyncVarsRequest {
    final String envFile;
    final Platform platform;
    final String repo;
    final boolean execute;
    final boolean showSecrets;

    private SyncVarsRequest(String envFile, Platform platform, String repo, boolean execute, boolean showSecrets) {
        this.envFile = envFile;
        this.platform = platform;
        this.repo = repo;
        this.execute = execute;
        this.showSecrets = showSecrets;
    }

    static SyncVarsRequest fromOptions(Map<String, String> options) {
        return new SyncVarsRequest(
            requiredOption(options, "env-file"),
            platformOption(options),
            trimToNull(options.get("repo")),
            isTrue(options.get("execute")),
            isTrue(options.get("show-secrets"))
        );
    }
}

final class AuditVarsRequest {
    final String envFile;
    final Platform platform;
    final String githubRepo;
    final String gitlabRepo;

    private AuditVarsRequest(String envFile, Platform platform, String githubRepo, String gitlabRepo) {
        this.envFile = envFile;
        this.platform = platform;
        this.githubRepo = githubRepo;
        this.gitlabRepo = gitlabRepo;
    }

    static AuditVarsRequest fromOptions(Map<String, String> options) {
        return new AuditVarsRequest(
            requiredOption(options, "env-file"),
            platformOption(options),
            trimToNull(options.get("github-repo")),
            trimToNull(options.get("gitlab-repo"))
        );
    }
}

final class EnvEntry {
    final String name;
    final boolean secret;
    final boolean protectedValue;
    final boolean required;

    EnvEntry(String name, boolean secret, boolean protectedValue) {
        this(name, secret, protectedValue, isRequiredName(name));
    }

    EnvEntry(String name, boolean secret, boolean protectedValue, boolean required) {
        this.name = name;
        this.secret = secret;
        this.protectedValue = protectedValue;
        this.required = required;
    }
}

final class EnvValue {
    final String raw;
    final boolean missing;
    final boolean placeholder;

    private EnvValue(String raw, boolean missing, boolean placeholder) {
        this.raw = raw;
        this.missing = missing;
        this.placeholder = placeholder;
    }

    static EnvValue of(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return missing();
        }
        if (isPlaceholderValue(normalized)) {
            return new EnvValue(normalized, false, true);
        }
        return new EnvValue(normalized, false, false);
    }

    static EnvValue missing() {
        return new EnvValue("", true, false);
    }

    boolean isReal() {
        return !missing && !placeholder;
    }

    String statusOrRaw() {
        if (missing) {
            return "MISSING";
        }
        if (placeholder) {
            return "PLACEHOLDER";
        }
        return raw;
    }

    String renderMasked(boolean showSecrets) {
        if (missing) {
            return "MISSING";
        }
        if (placeholder) {
            return "PLACEHOLDER";
        }
        if (showSecrets) {
            return raw;
        }
        if (raw.length() <= 4) {
            return "****";
        }
        return raw.substring(0, 2) + "****" + raw.substring(raw.length() - 2);
    }
}

final class LoadedEnv {
    final Path path;
    final Map<String, String> values;

    private LoadedEnv(Path path, Map<String, String> values) {
        this.path = path;
        this.values = values;
    }

    static LoadedEnv load(Path path) throws IOException {
        return parse(new String(Files.readAllBytes(path), StandardCharsets.UTF_8), path);
    }

    static LoadedEnv parse(String content, Path path) {
        Map<String, String> values = new LinkedHashMap<String, String>();
        String[] lines = content.split("\\r?\\n");
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("export ")) {
                line = line.substring("export ".length()).trim();
            }
            int separator = line.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            String key = line.substring(0, separator).trim();
            String value = line.substring(separator + 1).trim();
            values.put(key, stripWrappingQuotes(value));
        }
        return new LoadedEnv(path, values);
    }

    EnvValue value(String key) {
        return EnvValue.of(values.get(key));
    }
}

final class CommandResult {
    final int exitCode;
    private final byte[] stdout;
    private final byte[] stderr;

    CommandResult(int exitCode, byte[] stdout, byte[] stderr) {
        this.exitCode = exitCode;
        this.stdout = stdout;
        this.stderr = stderr;
    }

    String stdoutText() {
        return new String(stdout, StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unused")
    String stderrText() {
        return new String(stderr, StandardCharsets.UTF_8);
    }
}

final class AuditOutcome {
    final String message;
    private final boolean failure;

    private AuditOutcome(String message, boolean failure) {
        this.message = message;
        this.failure = failure;
    }

    static AuditOutcome success(String message) {
        return new AuditOutcome(message, false);
    }

    static AuditOutcome failure(String message) {
        return new AuditOutcome(message, true);
    }

    boolean isFailure() {
        return failure;
    }
}

final class PublishRequest {
    final boolean snapshot;
    final String tag;
    final boolean allowDirty;
    final boolean execute;
    final String module;

    private PublishRequest(boolean snapshot, String tag, boolean allowDirty, boolean execute, String module) {
        this.snapshot = snapshot;
        this.tag = tag;
        this.allowDirty = allowDirty;
        this.execute = execute;
        this.module = module;
    }

    static PublishRequest fromOptions(Map<String, String> options, boolean supportExecute) {
        boolean snapshot = isTrue(options.get("snapshot"));
        String tag = trimToNull(options.get("tag"));
        if (!snapshot && tag == null) {
            throw new IllegalArgumentException("必须指定 --snapshot true 或 --tag <value>");
        }
        if (snapshot && tag != null) {
            throw new IllegalArgumentException("--snapshot 和 --tag 不能同时使用");
        }
        return new PublishRequest(
            snapshot,
            tag,
            isTrue(options.get("allow-dirty")),
            supportExecute && isTrue(options.get("execute")),
            trimToNull(options.get("module"))
        );
    }
}

final class GitlabReleasePlanRequest {
    final String projectId;
    final String targetBranch;
    final String releaseBranch;
    final boolean execute;

    private GitlabReleasePlanRequest(String projectId, String targetBranch, String releaseBranch, boolean execute) {
        this.projectId = projectId;
        this.targetBranch = targetBranch;
        this.releaseBranch = releaseBranch;
        this.execute = execute;
    }

    static GitlabReleasePlanRequest fromOptions(Map<String, String> options) {
        String targetBranch = firstNonBlank(trimToNull(options.get("target-branch")), System.getenv("CI_DEFAULT_BRANCH"));
        if (targetBranch == null) {
            targetBranch = "main";
        }
        String releaseBranch = trimToNull(options.get("release-branch"));
        if (releaseBranch == null) {
            releaseBranch = "changeset-release/" + targetBranch;
        }
        return new GitlabReleasePlanRequest(
            firstNonBlank(trimToNull(options.get("project-id")), System.getenv("CI_PROJECT_ID")),
            targetBranch,
            releaseBranch,
            isTrue(options.get("execute"))
        );
    }
}

final class GitlabTagRequest {
    final String beforeSha;
    final String currentSha;
    final boolean execute;

    private GitlabTagRequest(String beforeSha, String currentSha, boolean execute) {
        this.beforeSha = beforeSha;
        this.currentSha = currentSha;
        this.execute = execute;
    }

    static GitlabTagRequest fromOptions(Map<String, String> options) {
        return new GitlabTagRequest(
            firstNonBlank(trimToNull(options.get("before-sha")), System.getenv("CI_COMMIT_BEFORE_SHA")),
            firstNonBlank(trimToNull(options.get("current-sha")), System.getenv("CI_COMMIT_SHA")),
            isTrue(options.get("execute"))
        );
    }
}

final class ReleasePlan {
    private final Path repoRoot;
    private final String currentRevision;
    private final String latestWholeRepoTag;
    private final List<Changeset> changesets;
    private final ReleaseLevel releaseLevel;
    private final String releaseVersion;
    private final String nextSnapshotVersion;

    ReleasePlan(Path repoRoot, String currentRevision, String latestWholeRepoTag,
                List<Changeset> changesets, ReleaseLevel releaseLevel,
                String releaseVersion, String nextSnapshotVersion) {
        this.repoRoot = repoRoot;
        this.currentRevision = currentRevision;
        this.latestWholeRepoTag = latestWholeRepoTag;
        this.changesets = changesets;
        this.releaseLevel = releaseLevel;
        this.releaseVersion = releaseVersion;
        this.nextSnapshotVersion = nextSnapshotVersion;
    }

    Path getRepoRoot() {
        return repoRoot;
    }

    String getCurrentRevision() {
        return currentRevision;
    }

    String getLatestWholeRepoTag() {
        return latestWholeRepoTag;
    }

    List<Changeset> getChangesets() {
        return changesets;
    }

    boolean hasPendingChangesets() {
        return !changesets.isEmpty();
    }

    ReleaseLevel getReleaseLevel() {
        return releaseLevel;
    }

    String getReleaseVersion() {
        return releaseVersion;
    }

    String getNextSnapshotVersion() {
        return nextSnapshotVersion;
    }

    String renderChangelogSection() {
        StringBuilder builder = new StringBuilder();
        builder.append("## ").append(releaseVersion).append(" - ")
            .append(LocalDate.now().toString()).append("\n\n");

        Map<ReleaseLevel, List<Changeset>> grouped = new LinkedHashMap<ReleaseLevel, List<Changeset>>();
        grouped.put(ReleaseLevel.MAJOR, new ArrayList<Changeset>());
        grouped.put(ReleaseLevel.MINOR, new ArrayList<Changeset>());
        grouped.put(ReleaseLevel.PATCH, new ArrayList<Changeset>());
        for (Changeset changeset : changesets) {
            grouped.get(changeset.release).add(changeset);
        }

        boolean wroteSection = false;
        for (ReleaseLevel level : Arrays.asList(ReleaseLevel.MAJOR, ReleaseLevel.MINOR, ReleaseLevel.PATCH)) {
            List<Changeset> levelChangesets = grouped.get(level);
            if (levelChangesets == null || levelChangesets.isEmpty()) {
                continue;
            }
            builder.append("### ").append(releaseLevelHeading(level)).append("\n\n");
            for (Changeset changeset : levelChangesets) {
                builder.append("- ").append(changeset.summary);
                builder.append(" (modules: ").append(joinModules(changeset.modules)).append(")");
                if (!changeset.body.isEmpty()) {
                    builder.append(" ");
                    builder.append(firstBodyLine(changeset.body));
                }
                builder.append("\n");
            }
            builder.append("\n");
            wroteSection = true;
        }

        if (!wroteSection) {
            builder.append("- No user-facing changes were recorded for this release.\n\n");
        }

        return builder.toString().trim() + "\n";
    }

    List<String> toPullRequestBodyLines() {
        List<String> lines = new ArrayList<String>();
        lines.add("## Release Plan");
        lines.add("");
        lines.add("- Release version: `v" + releaseVersion + "`");
        lines.add("- Next snapshot: `" + nextSnapshotVersion + "`");
        lines.add("- Release level: `" + releaseLevel.id + "`");
        lines.add("");
        lines.add("## Included Changesets");
        lines.add("");
        for (Changeset changeset : changesets) {
            String visibleType = renderVisibleType(changeset.type);
            lines.add("- `" + changeset.release.id + "` "
                + (visibleType.isEmpty() ? "" : "`" + visibleType + "` ")
                + changeset.summary + " (`" + joinModules(changeset.modules) + "`)");
        }
        lines.add("");
        lines.add("This PR was generated automatically from `.changesets/*.md` files.");
        lines.add("Merging it will trigger an automatic tag push and then reuse the existing release workflows.");
        return lines;
    }

    String toJson() {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        builder.append("  \"releaseVersion\": \"").append(jsonEscape(releaseVersion)).append("\",\n");
        builder.append("  \"nextSnapshotVersion\": \"").append(jsonEscape(nextSnapshotVersion)).append("\",\n");
        builder.append("  \"releaseLevel\": \"").append(jsonEscape(releaseLevel.id)).append("\",\n");
        builder.append("  \"generatedAt\": \"").append(jsonEscape(OffsetDateTime.now().toString())).append("\",\n");
        builder.append("  \"changesets\": [\n");
        for (int i = 0; i < changesets.size(); i++) {
            Changeset changeset = changesets.get(i);
            builder.append("    {\n");
            builder.append("      \"file\": \"").append(jsonEscape(changeset.fileName)).append("\",\n");
            builder.append("      \"release\": \"").append(jsonEscape(changeset.release.id)).append("\",\n");
            builder.append("      \"type\": \"").append(jsonEscape(changeset.type)).append("\",\n");
            builder.append("      \"summary\": \"").append(jsonEscape(changeset.summary)).append("\",\n");
            builder.append("      \"modules\": [");
            for (int moduleIndex = 0; moduleIndex < changeset.modules.size(); moduleIndex++) {
                if (moduleIndex > 0) {
                    builder.append(", ");
                }
                builder.append("\"").append(jsonEscape(changeset.modules.get(moduleIndex))).append("\"");
            }
            builder.append("]\n");
            builder.append("    }");
            if (i + 1 < changesets.size()) {
                builder.append(",");
            }
            builder.append("\n");
        }
        builder.append("  ]\n");
        builder.append("}\n");
        return builder.toString();
    }
}

final class ChangesetInput {
    final ReleaseLevel release;
    final String type;
    final List<String> modules;
    final String summary;
    final String body;

    ChangesetInput(ReleaseLevel release, String type, List<String> modules, String summary, String body) {
        this.release = release;
        this.type = type;
        this.modules = modules;
        this.summary = summary;
        this.body = body;
    }
}

final class Changeset {
    final Path path;
    final String fileName;
    final ReleaseLevel release;
    final String type;
    final List<String> modules;
    final String summary;
    final String body;

    Changeset(Path path, String fileName, ReleaseLevel release, String type,
              List<String> modules, String summary, String body) {
        this.path = path;
        this.fileName = fileName;
        this.release = release;
        this.type = type;
        this.modules = modules;
        this.summary = summary;
        this.body = body;
    }
}

enum ReleaseLevel {
    PATCH("patch", 1),
    MINOR("minor", 2),
    MAJOR("major", 3);

    final String id;
    final int weight;

    ReleaseLevel(String id, int weight) {
        this.id = id;
        this.weight = weight;
    }

    static ReleaseLevel parse(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        for (ReleaseLevel level : values()) {
            if (level.id.equals(normalized)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unsupported release level: " + value);
    }
}

final class Semver {
    private final int major;
    private final int minor;
    private final int patch;

    private Semver(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    static Semver parse(String value) {
        String[] parts = value.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Unsupported version: " + value);
        }
        return new Semver(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }

    Semver bump(ReleaseLevel level) {
        if (level == ReleaseLevel.MAJOR) {
            return new Semver(major + 1, 0, 0);
        }
        if (level == ReleaseLevel.MINOR) {
            return new Semver(major, minor + 1, 0);
        }
        return new Semver(major, minor, patch + 1);
    }

    static Semver max(Semver left, Semver right) {
        return left.compareTo(right) >= 0 ? left : right;
    }

    private int compareTo(Semver other) {
        if (major != other.major) {
            return major - other.major;
        }
        if (minor != other.minor) {
            return minor - other.minor;
        }
        return patch - other.patch;
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}

final class ChangesetPrompter {
    private ChangesetPrompter() {
    }

    static ChangesetInput resolveInput(Path repoRoot, Map<String, String> options, PrintStream out, PrintStream err) {
        String summary = trimToNull(options.get("summary"));
        String release = trimToNull(options.get("release"));
        String type = trimToNull(options.get("type"));
        String modules = trimToNull(options.get("modules"));
        String body = trimToNull(options.get("body"));

        if (summary == null) {
            summary = trimToNull(System.getenv("CHANGESET_SUMMARY"));
        }
        if (release == null) {
            release = trimToNull(System.getenv("CHANGESET_RELEASE"));
        }
        if (type == null) {
            type = trimToNull(System.getenv("CHANGESET_TYPE"));
        }
        if (modules == null) {
            modules = trimToNull(System.getenv("CHANGESET_MODULES"));
        }
        if (body == null) {
            body = trimToNull(System.getenv("CHANGESET_BODY"));
        }

        if (summary != null && release != null) {
            return new ChangesetInput(
                ReleaseLevel.parse(release),
                normalizeType(type == null ? "other" : type),
                parseModules(repoRoot, modules == null ? "all" : modules),
                summary,
                body == null ? "" : body
            );
        }

        Console console = System.console();
        Scanner scanner = console == null ? new Scanner(System.in, "UTF-8") : null;

        summary = summary != null ? summary : prompt(console, scanner, out, "Summary");
        release = release != null ? release : prompt(console, scanner, out, "Release level (patch/minor/major)");
        type = type == null ? "other" : type;
        modules = modules == null ? "all" : modules;

        if (body == null) {
            out.println("Body (optional, finish with a single `.` line):");
            body = readMultiline(console, scanner);
        }

        return new ChangesetInput(
            ReleaseLevel.parse(release),
            normalizeType(type),
            parseModules(repoRoot, modules),
            summary,
            body
        );
    }

    private static String prompt(Console console, Scanner scanner, PrintStream out, String label) {
        String value;
        do {
            if (console != null) {
                value = console.readLine("%s: ", label);
            } else {
                out.print(label + ": ");
                out.flush();
                value = scanner.nextLine();
            }
            value = trimToNull(value);
        } while (value == null);
        return value;
    }

    private static String readMultiline(Console console, Scanner scanner) {
        StringBuilder builder = new StringBuilder();
        while (true) {
            String line = console != null ? console.readLine() : scanner.nextLine();
            if (".".equals(line)) {
                break;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(line);
        }
        return builder.toString().trim();
    }
}

final class Slug {
    private Slug() {
    }

    static String slugify(String text) {
        StringBuilder builder = new StringBuilder();
        char previous = '-';
        for (int i = 0; i < text.length(); i++) {
            char current = Character.toLowerCase(text.charAt(i));
            if ((current >= 'a' && current <= 'z') || (current >= '0' && current <= '9')) {
                builder.append(current);
                previous = current;
            } else if (previous != '-') {
                builder.append('-');
                previous = '-';
            }
        }
        String slug = builder.toString();
        while (slug.startsWith("-")) {
            slug = slug.substring(1);
        }
        while (slug.endsWith("-")) {
            slug = slug.substring(0, slug.length() - 1);
        }
        if (slug.isEmpty()) {
            return "changeset";
        }
        return slug.length() > 48 ? slug.substring(0, 48) : slug;
    }
}
