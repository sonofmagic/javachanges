package io.github.sonofmagic.javachanges.core;

import io.github.sonofmagic.javachanges.core.automation.ReleaseNotesGenerator;
import io.github.sonofmagic.javachanges.core.changeset.ChangesetInput;
import io.github.sonofmagic.javachanges.core.changeset.ChangesetPrompter;
import io.github.sonofmagic.javachanges.core.plan.JavaChangesStatusPrinter;
import io.github.sonofmagic.javachanges.core.plan.ReleasePlan;
import io.github.sonofmagic.javachanges.core.plan.ReleasePlanner;
import io.github.sonofmagic.javachanges.core.plan.RepoFiles;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static io.github.sonofmagic.javachanges.core.ReleaseModuleUtils.assertKnownModule;
import static io.github.sonofmagic.javachanges.core.ReleaseModuleUtils.moduleSelectorArgs;
import static io.github.sonofmagic.javachanges.core.ReleaseModuleUtils.releaseModuleFromTag;
import static io.github.sonofmagic.javachanges.core.ReleaseModuleUtils.releaseVersionFromTag;
import static io.github.sonofmagic.javachanges.core.ReleaseTextUtils.trimToNull;

@Command(name = "init", mixinStandardHelpOptions = true,
    description = "Initialize changeset files and print starter commands.")
final class InitCommand extends AbstractCliCommand {
    @Option(names = "--config", arity = "0..1", fallbackValue = "true", defaultValue = "false",
        description = "Also write .changesets/config.jsonc with the default release workflow settings.")
    private boolean config;

    @Option(names = "--force", arity = "0..1", fallbackValue = "true", defaultValue = "false",
        description = "Overwrite an existing changeset config file when used with --config.")
    private boolean force;

    @Override
    public Integer call() throws Exception {
        Path repoRoot = repoRoot();
        Path changesetsDir = repoRoot.resolve(ChangesetPaths.DIR);
        Path readmePath = changesetsDir.resolve(ChangesetPaths.README);
        boolean hadReadme = Files.exists(readmePath);

        RepoFiles.ensureChangesetReadme(repoRoot);

        out().println("Initialized javachanges in " + repoRoot);
        out().println();
        printPathAction(hadReadme ? "Kept" : "Created", repoRoot, readmePath);

        if (config) {
            Path configPath = configPath(changesetsDir);
            boolean hadConfig = Files.exists(configPath);
            if (!hadConfig || force) {
                Files.createDirectories(changesetsDir);
                Files.write(configPath, defaultConfig(configPath).getBytes(StandardCharsets.UTF_8));
                printPathAction(hadConfig ? "Replaced" : "Created", repoRoot, configPath);
            } else {
                printPathAction("Kept", repoRoot, configPath);
                out().println("  Use --force to replace it with the default template.");
            }
        } else {
            out().println("Skipped: " + ChangesetPaths.DIR + "/" + ChangesetPaths.CONFIG_JSONC
                + " (use --config to write the default template)");
        }

        String repoArg = CliOutputSupport.shellQuote(repoRoot.toString());
        out().println();
        out().println("Next steps:");
        out().println("  javachanges modules --directory " + repoArg);
        out().println("  javachanges add --directory " + repoArg + " --summary \"describe the change\" --release patch");
        out().println("  javachanges next --directory " + repoArg);
        return success();
    }

    private void printPathAction(String action, Path repoRoot, Path path) {
        out().println(action + ": " + repoRoot.relativize(path));
    }

    private static Path configPath(Path changesetsDir) {
        Path jsonPath = changesetsDir.resolve(ChangesetPaths.CONFIG_JSON);
        if (Files.exists(jsonPath)) {
            return jsonPath;
        }
        return changesetsDir.resolve(ChangesetPaths.CONFIG_JSONC);
    }

    private static String defaultConfig(Path configPath) {
        if (ChangesetPaths.CONFIG_JSON.equals(configPath.getFileName().toString())) {
            return "{\n"
                + "  \"baseBranch\": \"main\",\n"
                + "  \"releaseBranch\": \"changeset-release/main\",\n"
                + "  \"snapshotBranch\": \"snapshot\",\n"
                + "  \"snapshotVersionMode\": \"stamped\",\n"
                + "  \"tagStrategy\": \"whole-repo\"\n"
                + "}\n";
        }
        return "{\n"
            + "  // Default branch that receives reviewed release changesets.\n"
            + "  \"baseBranch\": \"main\",\n"
            + "\n"
            + "  // Branch used by release-plan automation.\n"
            + "  \"releaseBranch\": \"changeset-release/main\",\n"
            + "\n"
            + "  // Branch used for snapshot publishing.\n"
            + "  \"snapshotBranch\": \"snapshot\",\n"
            + "\n"
            + "  // Snapshot version strategy: stamped or plain.\n"
            + "  \"snapshotVersionMode\": \"stamped\",\n"
            + "\n"
            + "  // Release tag strategy: whole-repo or per-module.\n"
            + "  \"tagStrategy\": \"whole-repo\"\n"
            + "}\n";
    }
}

@Command(name = "add", mixinStandardHelpOptions = true,
    description = "Create a changeset file.")
final class AddCommand extends AbstractCliCommand {
    @Option(names = "--summary", description = "Short user-facing release summary.")
    private String summary;

    @Option(names = "--release", description = "Release level: patch, minor, or major.")
    private String release;

    @Option(names = "--type",
        description = "Legacy change type metadata. Accepted for compatibility but not written in the default official-style format.")
    private String type;

    @Option(names = "--modules", description = "Comma-separated Maven artifactIds, Gradle project names, or all. Written as official Changesets package keys.")
    private String modules;

    @Option(names = "--body", description = "Optional Markdown body after the summary paragraph.")
    private String body;

    @Override
    public Integer call() throws Exception {
        Path repoRoot = repoRoot();
        RepoFiles.ensureChangesetReadme(repoRoot);
        Map<String, String> options = options(
            option("summary", summary),
            option("release", release),
            option("type", type),
            option("modules", modules),
            option("body", body)
        );
        ChangesetInput input = ChangesetPrompter.resolveInput(repoRoot, options, out(), err());
        Path created = RepoFiles.writeChangeset(repoRoot, input);
        String repoArg = CliOutputSupport.shellQuote(repoRoot.toString());
        out().println("Created changeset: " + repoRoot.relativize(created));
        out().println();
        out().println("Next steps:");
        out().println("  javachanges status --directory " + repoArg);
        out().println("  javachanges next --directory " + repoArg);
        return success();
    }
}

@Command(name = "next", mixinStandardHelpOptions = true,
    description = "Suggest the next release workflow command for this repository.")
final class NextCommand extends AbstractCliCommand {
    @Override
    public Integer call() throws Exception {
        Path repoRoot = repoRoot();
        String repoArg = CliOutputSupport.shellQuote(repoRoot.toString());
        ReleasePlan plan = new ReleasePlanner(repoRoot).plan();
        out().println("Next step for " + repoRoot + ":");
        out().println();
        if (plan.hasPendingChangesets()) {
            out().println("Pending changesets: " + plan.getChangesetCount());
            out().println("Planned release: v" + plan.getReleaseVersion());
            out().println("Affected packages: " + ReleaseModuleUtils.joinModules(plan.getAffectedPackages()));
            out().println();
            out().println("Review the plan:");
            out().println("  javachanges status --directory " + repoArg);
            out().println();
            out().println("Apply it locally:");
            out().println("  javachanges plan --directory " + repoArg + " --apply true");
            out().println();
            out().println("Or open an automated GitHub release PR:");
            out().println("  javachanges github-release-plan --directory " + repoArg + " --write-plan-files false --execute true");
            return success();
        }
        out().println("No pending changesets.");
        out().println();
        out().println("Create one:");
        out().println("  javachanges add --directory " + repoArg + " --summary \"describe the change\" --release patch");
        out().println();
        out().println("Then review the plan:");
        out().println("  javachanges status --directory " + repoArg);
        return success();
    }
}

@Command(name = "status", mixinStandardHelpOptions = true,
    description = "Show the pending release plan.")
final class StatusCommand extends AbstractCliCommand {
    @Override
    public Integer call() throws Exception {
        Path repoRoot = repoRoot();
        ReleasePlan plan = new ReleasePlanner(repoRoot).plan();
        JavaChangesStatusPrinter.printStatus(plan, out());
        printNextSteps(repoRoot, plan);
        return success();
    }

    private void printNextSteps(Path repoRoot, ReleasePlan plan) {
        String repoArg = CliOutputSupport.shellQuote(repoRoot.toString());
        out().println();
        out().println("Next steps:");
        if (plan.hasPendingChangesets()) {
            out().println("  javachanges plan --directory " + repoArg + " --apply true");
            out().println("  javachanges next --directory " + repoArg);
            return;
        }
        out().println("  javachanges add --directory " + repoArg + " --summary \"describe the change\" --release patch");
        out().println("  javachanges next --directory " + repoArg);
    }
}

@Command(name = "plan", mixinStandardHelpOptions = true,
    description = "Render the release plan and optionally apply it.")
final class PlanCommand extends AbstractCliCommand {
    @Option(names = "--apply", arity = "0..1", fallbackValue = "true", defaultValue = "false",
        description = "Apply the planned version, changelog, and manifest updates.")
    private boolean apply;

    @Override
    public Integer call() throws Exception {
        Path repoRoot = repoRoot();
        ReleasePlan plan = new ReleasePlanner(repoRoot).plan();
        JavaChangesStatusPrinter.printStatus(plan, out());
        if (!apply) {
            return success();
        }
        if (!plan.hasPendingChangesets()) {
            out().println("No pending changesets to apply.");
            return success();
        }
        RepoFiles.applyPlan(repoRoot, plan);
        out().println();
        out().println("Applied release plan for v" + plan.getReleaseVersion());
        printAppliedNextSteps(repoRoot, plan);
        return success();
    }

    private void printAppliedNextSteps(Path repoRoot, ReleasePlan plan) {
        String repoArg = CliOutputSupport.shellQuote(repoRoot.toString());
        out().println();
        out().println("Next steps:");
        out().println("  git -C " + repoArg + " status --short");
        out().println("  git -C " + repoArg + " add "
            + CliOutputSupport.shellQuoteArgs(BuildModelSupport.releasePlanGitAddPaths(repoRoot)));
        out().println("  git -C " + repoArg + " commit -m "
            + CliOutputSupport.shellQuote("chore(release): v" + plan.getReleaseVersion()));
        out().println("  javachanges next --directory " + repoArg);
    }
}

@Command(name = "manifest-field", mixinStandardHelpOptions = true,
    description = "Read a field from .changesets/release-plan.json.")
final class ManifestFieldCommand extends AbstractCliCommand {
    @Option(names = "--field", required = true, description = "Manifest field name.")
    private String field;

    @Option(names = "--fresh", arity = "0..1", fallbackValue = "true", defaultValue = "false",
        description = "Derive the field from the current repository state instead of .changesets/release-plan.json.")
    private boolean fresh;

    @Override
    public Integer call() throws Exception {
        out().println(new ReleaseAutomationSupport(repoRoot()).readManifestField(field, fresh));
        return success();
    }
}

@Command(name = "version", mixinStandardHelpOptions = true,
    description = "Print the current Maven or Gradle revision.")
final class VersionCommand extends AbstractCliCommand {
    @Override
    public Integer call() throws Exception {
        out().println(new VersionSupport(repoRoot()).readRevision());
        return success();
    }
}

@Command(name = "modules", mixinStandardHelpOptions = true,
    description = "List the detected build tool, version file, revision, and modules.")
final class ModulesCommand extends AbstractCliCommand {
    @Override
    public Integer call() throws Exception {
        Path repoRoot = repoRoot();
        BuildModelSupport.BuildModel model = BuildModelSupport.detect(repoRoot);
        if (model == null) {
            throw new IllegalStateException("Cannot find supported Maven or Gradle build model in " + repoRoot);
        }
        List<String> modules = ReleaseModuleUtils.detectKnownModules(repoRoot);
        out().println("Repository: " + repoRoot);
        out().println("Build tool: " + model.type.name().toLowerCase(java.util.Locale.ROOT));
        out().println("Version file: " + BuildModelSupport.revisionFileLabel(repoRoot));
        out().println("Current revision: " + BuildModelSupport.readRevision(repoRoot));
        out().println("Modules:");
        for (String module : modules) {
            out().println("  - " + module);
        }
        return success();
    }
}

@Command(name = "release-version-from-tag", mixinStandardHelpOptions = true,
    description = "Extract the version from a release tag.")
final class ReleaseVersionFromTagCommand extends AbstractCliCommand {
    @Option(names = "--tag", required = true, description = "Release tag like v1.2.3.")
    private String tag;

    @Override
    public Integer call() {
        out().println(releaseVersionFromTag(tag));
        return success();
    }
}

@Command(name = "release-module-from-tag", mixinStandardHelpOptions = true,
    description = "Extract the module from a module release tag.")
final class ReleaseModuleFromTagCommand extends AbstractCliCommand {
    @Option(names = "--tag", required = true, description = "Module tag like sample-module/v1.2.3.")
    private String tag;

    @Override
    public Integer call() {
        String module = releaseModuleFromTag(tag);
        out().println(module == null ? "" : module);
        return success();
    }
}

@Command(name = "assert-module", mixinStandardHelpOptions = true,
    description = "Validate that a module exists in the repository.")
final class AssertModuleCommand extends AbstractCliCommand {
    @Option(names = "--module", required = true, description = "Maven artifactId or Gradle project name to validate.")
    private String module;

    @Override
    public Integer call() {
        assertKnownModule(repoRoot(), module);
        out().println("module ok");
        return success();
    }
}

@Command(name = "assert-snapshot", mixinStandardHelpOptions = true,
    description = "Validate that the current revision is a SNAPSHOT.")
final class AssertSnapshotCommand extends AbstractCliCommand {
    @Override
    public Integer call() throws Exception {
        new VersionSupport(repoRoot()).assertSnapshot();
        out().println("snapshot ok");
        return success();
    }
}

@Command(name = "assert-release-tag", mixinStandardHelpOptions = true,
    description = "Validate that a tag matches the current repository revision.")
final class AssertReleaseTagCommand extends AbstractCliCommand {
    @Option(names = "--tag", required = true, description = "Release tag to validate.")
    private String tag;

    @Override
    public Integer call() throws Exception {
        new VersionSupport(repoRoot()).assertReleaseTag(tag);
        out().println("release tag ok");
        return success();
    }
}

@Command(name = "module-selector-args", mixinStandardHelpOptions = true,
    description = "Print build-tool module selector arguments for a module.")
final class ModuleSelectorArgsCommand extends AbstractCliCommand {
    @Option(names = "--module", description = "Maven artifactId or all.")
    private String module;

    @Override
    public Integer call() {
        out().println(moduleSelectorArgs(repoRoot(), trimToNull(module)));
        return success();
    }
}

@Command(name = "write-settings", mixinStandardHelpOptions = true,
    description = "Generate a Maven settings.xml file from environment variables.")
final class WriteSettingsCommand extends AbstractCliCommand {
    @Option(names = "--output", required = true, description = "Output path for settings.xml.")
    private String output;

    @Override
    public Integer call() throws Exception {
        MavenSettingsWriter.write(Paths.get(output));
        out().println("Generated Maven settings: " + output);
        return success();
    }
}

@Command(name = "release-notes", mixinStandardHelpOptions = true,
    description = "Generate release notes for a tag.")
final class ReleaseNotesCommand extends AbstractCliCommand {
    @Option(names = "--tag", required = true, description = "Release tag like v1.2.3.")
    private String tag;

    @Option(names = "--output", required = true, description = "Output path relative to the repository root.")
    private String output;

    @Override
    public Integer call() throws Exception {
        Path repoRoot = repoRoot();
        Path target = RepoPathSupport.resolveOutputPath(repoRoot, output, "--output");
        new ReleaseNotesGenerator(repoRoot).writeReleaseNotes(tag, target);
        out().println("Generated release notes: " + output);
        return success();
    }
}

@Command(name = "ensure-gpg-public-key", mixinStandardHelpOptions = true,
    description = "Publish the current signing public key and wait until a supported keyserver can fetch it.")
final class EnsureGpgPublicKeyCommand extends AbstractCliCommand {
    @Option(names = "--primary-keyserver", defaultValue = "hkps://keyserver.ubuntu.com",
        description = "Primary keyserver used for upload and lookup.")
    private String primaryKeyserver;

    @Option(names = "--secondary-keyserver", defaultValue = "hkps://keys.openpgp.org",
        description = "Secondary keyserver used for upload and lookup.")
    private String secondaryKeyserver;

    @Option(names = "--attempts", defaultValue = "12",
        description = "Maximum number of discovery attempts before failing.")
    private int attempts;

    @Option(names = "--retry-delay-seconds", defaultValue = "10",
        description = "Delay between discovery attempts in seconds.")
    private int retryDelaySeconds;

    @Override
    public Integer call() throws Exception {
        GpgKeySupport support = new GpgKeySupport(repoRoot());
        String fingerprint = support.ensurePublicKeyDiscoverable(
            primaryKeyserver,
            secondaryKeyserver,
            attempts,
            retryDelaySeconds,
            out(),
            err()
        );
        out().println("gpg public key ok: " + fingerprint);
        return success();
    }
}

final class CliOutputSupport {
    private CliOutputSupport() {
    }

    static String shellQuote(String value) {
        if (value.matches("[A-Za-z0-9_./:-]+")) {
            return value;
        }
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }

    static String shellQuoteArgs(String[] values) {
        StringBuilder result = new StringBuilder();
        for (String value : values) {
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(shellQuote(value));
        }
        return result.toString();
    }
}
