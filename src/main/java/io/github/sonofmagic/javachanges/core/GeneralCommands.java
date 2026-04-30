package io.github.sonofmagic.javachanges.core;

import io.github.sonofmagic.javachanges.core.automation.ReleaseNotesGenerator;
import io.github.sonofmagic.javachanges.core.changeset.ChangesetInput;
import io.github.sonofmagic.javachanges.core.changeset.ChangesetPrompter;
import io.github.sonofmagic.javachanges.core.env.InitEnvRequest;
import io.github.sonofmagic.javachanges.core.plan.JavaChangesStatusPrinter;
import io.github.sonofmagic.javachanges.core.plan.ReleasePlan;
import io.github.sonofmagic.javachanges.core.plan.ReleasePlanner;
import io.github.sonofmagic.javachanges.core.plan.RepoFiles;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
        description = "Replace existing generated setup files. Config files are only replaced when used with --config.")
    private boolean force;

    @Override
    public Integer call() throws Exception {
        Path repoRoot = repoRoot();
        Path changesetsDir = repoRoot.resolve(ChangesetPaths.DIR);
        Path readmePath = changesetsDir.resolve(ChangesetPaths.README);
        boolean hadReadme = Files.exists(readmePath);

        if (hadReadme && force) {
            RepoFiles.writeDefaultChangesetReadme(repoRoot);
        } else {
            RepoFiles.ensureChangesetReadme(repoRoot);
        }

        out().println(ReleaseMessages.initialized(repoRoot));
        out().println();
        printPathAction(readmeAction(hadReadme), repoRoot, readmePath);

        if (config) {
            Path configPath = configPath(changesetsDir);
            boolean hadConfig = Files.exists(configPath);
            if (!hadConfig || force) {
                Files.createDirectories(changesetsDir);
                Files.write(configPath, defaultConfig(configPath).getBytes(StandardCharsets.UTF_8));
                printPathAction(hadConfig ? "Replaced" : "Created", repoRoot, configPath);
            } else {
                printPathAction("Kept", repoRoot, configPath);
                out().println(ReleaseMessages.useForceToReplaceDefaultTemplate());
            }
        } else {
            out().println(ReleaseMessages.pathAction("Skipped") + ": " + ChangesetPaths.DIR + "/" + ChangesetPaths.CONFIG_JSONC
                + ReleaseMessages.useConfigToWriteDefaultTemplate());
        }

        String repoArg = CliOutputSupport.shellQuote(repoRoot.toString());
        out().println();
        out().println(ReleaseMessages.nextSteps());
        out().println("  javachanges modules --directory " + repoArg);
        out().println("  javachanges add --directory " + repoArg + " --summary \""
            + ReleaseMessages.describeChangePlaceholder() + "\" --release patch");
        out().println("  javachanges next --directory " + repoArg);
        return success();
    }

    private void printPathAction(String action, Path repoRoot, Path path) {
        out().println(ReleaseMessages.pathAction(action) + ": " + repoRoot.relativize(path));
    }

    private String readmeAction(boolean hadReadme) {
        if (!hadReadme) {
            return "Created";
        }
        return force ? "Replaced" : "Kept";
    }

    static Path configPath(Path changesetsDir) {
        Path jsonPath = changesetsDir.resolve(ChangesetPaths.CONFIG_JSON);
        if (Files.exists(jsonPath)) {
            return jsonPath;
        }
        return changesetsDir.resolve(ChangesetPaths.CONFIG_JSONC);
    }

    static String defaultConfig(Path configPath) {
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

@Command(name = "setup", mixinStandardHelpOptions = true,
    description = "Run first-time release workflow setup with safe defaults.")
final class SetupCommand extends AbstractCliCommand {
    @Option(names = "--force", arity = "0..1", fallbackValue = "true", defaultValue = "false",
        description = "Replace generated setup files when they already exist.")
    private boolean force;

    @Option(names = "--env", arity = "0..1", fallbackValue = "true", defaultValue = "false",
        description = "Also write env/release.env.example and env/release.env.local.")
    private boolean env;

    @Option(names = "--github-actions", arity = "0..1", fallbackValue = "true", defaultValue = "false",
        description = "Also write the GitHub Actions workflow template.")
    private boolean githubActions;

    @Option(names = "--gitlab-ci", arity = "0..1", fallbackValue = "true", defaultValue = "false",
        description = "Also write the GitLab CI template.")
    private boolean gitlabCi;

    @Option(names = "--javachanges-version", description = "Released javachanges version used by generated CI templates.")
    private String javachangesVersion;

    @Option(names = "--build-tool", description = "Build tool template for generated CI: auto, maven, or gradle.", defaultValue = "auto")
    private String buildTool;

    @Override
    public Integer call() throws Exception {
        Path repoRoot = repoRoot();
        BuildModelSupport.BuildModel model = BuildModelSupport.detect(repoRoot);
        if (model == null) {
            throw new IllegalStateException(ReleaseMessages.cannotFindSupportedBuildModel(repoRoot));
        }
        out().println(ReleaseMessages.setupStarted(repoRoot));
        setupChangesets(repoRoot);
        printDetectedBuild(repoRoot, model);
        if (env) {
            setupEnv(repoRoot);
        }
        if (githubActions) {
            runSetupChild(repoRoot, "init-github-actions", "--force", String.valueOf(force),
                "--build-tool", buildTool, "--javachanges-version", javachangesVersion);
        }
        if (gitlabCi) {
            runSetupChild(repoRoot, "init-gitlab-ci", "--force", String.valueOf(force),
                "--build-tool", buildTool, "--javachanges-version", javachangesVersion);
        }
        printSetupNextSteps(repoRoot);
        return success();
    }

    private void setupChangesets(Path repoRoot) throws Exception {
        Path changesetsDir = repoRoot.resolve(ChangesetPaths.DIR);
        Path readmePath = changesetsDir.resolve(ChangesetPaths.README);
        boolean hadReadme = Files.exists(readmePath);
        if (hadReadme && force) {
            RepoFiles.writeDefaultChangesetReadme(repoRoot);
        } else {
            RepoFiles.ensureChangesetReadme(repoRoot);
        }
        printPathAction(hadReadme ? (force ? "Replaced" : "Kept") : "Created", repoRoot, readmePath);

        Path configPath = InitCommand.configPath(changesetsDir);
        boolean hadConfig = Files.exists(configPath);
        if (!hadConfig || force) {
            Files.createDirectories(changesetsDir);
            Files.write(configPath, InitCommand.defaultConfig(configPath).getBytes(StandardCharsets.UTF_8));
            printPathAction(hadConfig ? "Replaced" : "Created", repoRoot, configPath);
        } else {
            printPathAction("Kept", repoRoot, configPath);
        }
    }

    private void setupEnv(Path repoRoot) throws Exception {
        Path envExample = repoRoot.resolve("env").resolve("release.env.example");
        boolean hadExample = Files.exists(envExample);
        if (!hadExample || force) {
            Files.createDirectories(envExample.getParent());
            Files.write(envExample, defaultEnvExample().getBytes(StandardCharsets.UTF_8));
            printPathAction(hadExample ? "Replaced" : "Created", repoRoot, envExample);
        } else {
            printPathAction("Kept", repoRoot, envExample);
        }
        envSupport().initEnv(InitEnvRequest.fromOptions(options(flag("force", force))));
    }

    private void printDetectedBuild(Path repoRoot, BuildModelSupport.BuildModel model) {
        out().println(ReleaseMessages.buildTool() + ": " + model.type.name().toLowerCase(java.util.Locale.ROOT));
        out().println(ReleaseMessages.versionFile() + ": " + repoRoot.relativize(model.versionFile));
        out().println(ReleaseMessages.modules() + ": " + ReleaseModuleUtils.joinModules(ReleaseModuleUtils.detectKnownModules(repoRoot)));
    }

    private void printPathAction(String action, Path repoRoot, Path path) {
        out().println(ReleaseMessages.pathAction(action) + ": " + repoRoot.relativize(path));
    }

    private void runSetupChild(Path repoRoot, String command, String... childArgs) {
        List<String> args = new ArrayList<String>();
        args.add("--language");
        args.add(ReleaseMessages.language().id);
        args.add("--directory");
        args.add(repoRoot.toString());
        args.add(command);
        for (int i = 0; i < childArgs.length; i += 2) {
            String value = i + 1 < childArgs.length ? ReleaseTextUtils.trimToNull(childArgs[i + 1]) : null;
            if (value != null) {
                args.add(childArgs[i]);
                args.add(value);
            }
        }
        int exitCode = JavaChangesCli.execute(args.toArray(new String[0]), out(), err());
        if (exitCode != 0) {
            throw new IllegalStateException(ReleaseMessages.javachangesFailed(exitCode));
        }
    }

    private void printSetupNextSteps(Path repoRoot) {
        String repoArg = CliOutputSupport.shellQuote(repoRoot.toString());
        out().println();
        out().println(ReleaseMessages.setupCompleted());
        out().println(ReleaseMessages.nextSteps());
        out().println("  javachanges validate --directory " + repoArg);
        out().println("  javachanges add --directory " + repoArg + " --summary \""
            + ReleaseMessages.describeChangePlaceholder() + "\" --release patch");
        out().println("  javachanges next --directory " + repoArg);
        if (!githubActions) {
            out().println("  javachanges init-github-actions --directory " + repoArg);
        }
        if (!gitlabCi) {
            out().println("  javachanges init-gitlab-ci --directory " + repoArg);
        }
    }

    private static String defaultEnvExample() {
        return "MAVEN_RELEASE_REPOSITORY_URL=https://repo.example.com/maven-releases/\n"
            + "MAVEN_SNAPSHOT_REPOSITORY_URL=https://repo.example.com/maven-snapshots/\n"
            + "MAVEN_RELEASE_REPOSITORY_ID=maven-releases\n"
            + "MAVEN_SNAPSHOT_REPOSITORY_ID=maven-snapshots\n"
            + "\n"
            + "MAVEN_REPOSITORY_USERNAME=replace-me\n"
            + "MAVEN_REPOSITORY_PASSWORD=replace-me\n"
            + "\n"
            + "# Optional Sonatype Central Portal token fallback\n"
            + "# MAVEN_CENTRAL_USERNAME=replace-me\n"
            + "# MAVEN_CENTRAL_PASSWORD=replace-me\n"
            + "\n"
            + "# Optional GitLab token for release creation\n"
            + "# GITLAB_RELEASE_TOKEN=replace-me\n";
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

    @Option(names = "--format", description = "Output format: text or json.")
    private String format;

    @Option(names = "--no-interactive", arity = "0..1", fallbackValue = "true", defaultValue = "false",
        description = "Fail instead of prompting when required changeset input is missing.")
    private boolean noInteractive;

    @Override
    public Integer call() throws Exception {
        Path repoRoot = repoRoot();
        OutputFormat outputFormat = OutputFormat.parse(format, OutputFormat.TEXT);
        return runJsonCommand("add", outputFormat,
            (name, exception) -> ReleaseWorkflowNextSteps.errorJson(name, exception),
            () -> {
                RepoFiles.ensureChangesetReadme(repoRoot);
                Map<String, String> options = options(
                    option("summary", summary),
                    option("release", release),
                    option("type", type),
                    option("modules", modules),
                    option("body", body),
                    flag("no-interactive", noInteractive)
                );
                ChangesetInput input = ChangesetPrompter.resolveInput(repoRoot, options, out(), err());
                Path created = RepoFiles.writeChangeset(repoRoot, input);
                if (outputFormat == OutputFormat.JSON) {
                    out().println(ReleaseWorkflowNextSteps.addJson(repoRoot, created, input));
                    return success();
                }
                String repoArg = CliOutputSupport.shellQuote(repoRoot.toString());
                out().println(ReleaseMessages.createdChangeset(repoRoot.relativize(created)));
                out().println(ReleaseMessages.releaseLevel() + ": " + input.release.id);
                out().println(ReleaseMessages.affectedPackages() + ": " + ReleaseModuleUtils.joinModules(input.modules));
                out().println();
                out().println(ReleaseMessages.nextSteps());
                out().println("  javachanges status --directory " + repoArg);
                out().println("  javachanges next --directory " + repoArg);
                return success();
            });
    }
}

@Command(name = "next", mixinStandardHelpOptions = true,
    description = "Suggest the next release workflow command for this repository.")
final class NextCommand extends AbstractCliCommand {
    @Option(names = "--format", description = "Output format: text or json.")
    private String format;

    @Override
    public Integer call() throws Exception {
        Path repoRoot = repoRoot();
        String repoArg = CliOutputSupport.shellQuote(repoRoot.toString());
        ReleasePlan plan = new ReleasePlanner(repoRoot).plan();
        if (OutputFormat.parse(format, OutputFormat.TEXT) == OutputFormat.JSON) {
            out().println(ReleaseWorkflowNextSteps.nextJson(repoRoot, plan));
            return success();
        }
        out().println(ReleaseMessages.nextStepFor(repoRoot));
        out().println();
        if (plan.hasPendingChangesets()) {
            out().println(ReleaseMessages.pendingChangesets() + ": " + plan.getChangesetCount());
            out().println(ReleaseMessages.plannedRelease() + ": v" + plan.getReleaseVersion());
            out().println(ReleaseMessages.affectedPackages() + ": " + ReleaseModuleUtils.joinModules(plan.getAffectedPackages()));
            out().println();
            out().println(ReleaseMessages.reviewPlan());
            out().println("  javachanges status --directory " + repoArg);
            out().println();
            out().println(ReleaseMessages.applyLocally());
            out().println("  javachanges plan --directory " + repoArg + " --apply true");
            out().println();
            out().println(ReleaseMessages.openGithubPr());
            out().println("  javachanges github-release-plan --directory " + repoArg + " --write-plan-files false --execute true");
            out().println();
            out().println(ReleaseMessages.openGitlabMr());
            out().println("  javachanges gitlab-release-plan --directory " + repoArg + " --write-plan-files false --execute true");
            return success();
        }
        out().println(ReleaseMessages.noPendingChangesets());
        out().println();
        out().println(ReleaseMessages.createdOne());
        out().println("  javachanges add --directory " + repoArg + " --summary \""
            + ReleaseMessages.describeChangePlaceholder() + "\" --release patch");
        out().println();
        out().println(ReleaseMessages.thenReviewPlan());
        out().println("  javachanges status --directory " + repoArg);
        return success();
    }
}

@Command(name = "status", mixinStandardHelpOptions = true,
    description = "Show the pending release plan.")
final class StatusCommand extends AbstractCliCommand {
    @Option(names = "--format", description = "Output format: text or json.")
    private String format;

    @Override
    public Integer call() throws Exception {
        Path repoRoot = repoRoot();
        ReleasePlan plan = new ReleasePlanner(repoRoot).plan();
        if (OutputFormat.parse(format, OutputFormat.TEXT) == OutputFormat.JSON) {
            out().println(ReleaseWorkflowNextSteps.statusJson(plan));
            return success();
        }
        JavaChangesStatusPrinter.printStatus(plan, out());
        ReleaseWorkflowNextSteps.printReviewNextSteps(out(), repoRoot, plan);
        return success();
    }
}

@Command(name = "validate", mixinStandardHelpOptions = true,
    description = "Check repository release readiness without modifying files.")
final class ValidateCommand extends AbstractCliCommand {
    @Option(names = "--format", description = "Output format: text or json.")
    private String format;

    @Option(names = "--check-dirty", arity = "0..1", fallbackValue = "true", defaultValue = "false",
        description = "Fail when the git working tree has uncommitted changes.")
    private boolean checkDirty;

    @Override
    public Integer call() throws Exception {
        OutputFormat outputFormat = OutputFormat.parse(format, OutputFormat.TEXT);
        return runJsonCommand("validate", outputFormat,
            (name, exception) -> ReleaseWorkflowNextSteps.errorJson(name, exception),
            () -> {
                RepoValidationSupport.Report report = RepoValidationSupport.validate(repoRoot(), checkDirty);
                if (outputFormat == OutputFormat.JSON) {
                    out().println(report.toJson());
                } else {
                    RepoValidationSupport.printText(report, out());
                }
                return report.ok() ? success() : failure();
            });
    }
}

@Command(name = "plan", mixinStandardHelpOptions = true,
    description = "Render the release plan and optionally apply it.")
final class PlanCommand extends AbstractCliCommand {
    @Option(names = "--apply", arity = "0..1", fallbackValue = "true", defaultValue = "false",
        description = "Apply the planned version, changelog, and manifest updates.")
    private boolean apply;

    @Option(names = "--format", description = "Output format: text or json.")
    private String format;

    @Option(names = "--restore", arity = "0..1", fallbackValue = "true", defaultValue = "false",
        description = "Restore files from .changesets/release-plan-backup.json.")
    private boolean restore;

    @Override
    public Integer call() throws Exception {
        Path repoRoot = repoRoot();
        OutputFormat outputFormat = OutputFormat.parse(format, OutputFormat.TEXT);
        if (restore) {
            return runRestore(repoRoot, outputFormat);
        }
        ReleasePlan plan = new ReleasePlanner(repoRoot).plan();
        if (outputFormat == OutputFormat.JSON) {
            return runJsonCommand("plan", outputFormat,
                (name, exception) -> ReleaseWorkflowNextSteps.errorJson(name, exception),
                () -> runJsonPlan(repoRoot, plan));
        }
        JavaChangesStatusPrinter.printStatus(plan, out());
        if (!apply) {
            ReleaseWorkflowNextSteps.printReviewNextSteps(out(), repoRoot, plan);
            return success();
        }
        if (!plan.hasPendingChangesets()) {
            out().println(ReleaseMessages.noPendingChangesetsToApply());
            ReleaseWorkflowNextSteps.printReviewNextSteps(out(), repoRoot, plan);
            return success();
        }
        Path backupPath = RepoFiles.writePlanBackup(repoRoot, plan);
        RepoFiles.applyPlan(repoRoot, plan);
        out().println();
        out().println(ReleaseMessages.appliedReleasePlan(plan.getReleaseVersion()));
        out().println(ReleaseMessages.releasePlanBackup(repoRoot.relativize(backupPath)));
        ReleaseWorkflowNextSteps.printAppliedNextSteps(out(), repoRoot, plan, backupPath);
        return success();
    }

    private int runJsonPlan(Path repoRoot, ReleasePlan plan) throws Exception {
        boolean applied = false;
        String reason = null;
        Path backupPath = null;
        if (apply && plan.hasPendingChangesets()) {
            backupPath = RepoFiles.writePlanBackup(repoRoot, plan);
            RepoFiles.applyPlan(repoRoot, plan);
            applied = true;
        } else if (apply) {
            reason = "no-pending-changesets";
        } else {
            reason = "dry-run";
        }
        out().println(ReleaseWorkflowNextSteps.planJson(repoRoot, plan, apply, applied, reason, backupPath));
        return success();
    }

    private int runRestore(Path repoRoot, OutputFormat outputFormat) throws Exception {
        return runJsonCommand("plan", outputFormat,
            (name, exception) -> ReleaseWorkflowNextSteps.errorJson(name, exception),
            () -> {
                Path backupPath = RepoFiles.restorePlanBackup(repoRoot);
                if (outputFormat == OutputFormat.JSON) {
                    out().println(ReleaseWorkflowNextSteps.planRestoreJson(repoRoot, backupPath));
                } else {
                    out().println(ReleaseMessages.restoredReleasePlanBackup(repoRoot.relativize(backupPath)));
                    out().println();
                    out().println(ReleaseMessages.nextSteps());
                    out().println("  javachanges status --directory " + CliOutputSupport.shellQuote(repoRoot.toString()));
                }
                return success();
            });
    }
}

final class ReleaseWorkflowNextSteps {
    private ReleaseWorkflowNextSteps() {
    }

    static void printReviewNextSteps(PrintStream out, Path repoRoot, ReleasePlan plan) {
        String repoArg = CliOutputSupport.shellQuote(repoRoot.toString());
        out.println();
        out.println(ReleaseMessages.nextSteps());
        if (plan.hasPendingChangesets()) {
            out.println("  javachanges plan --directory " + repoArg + " --apply true");
            out.println("  javachanges next --directory " + repoArg);
            return;
        }
        out.println("  javachanges add --directory " + repoArg + " --summary \""
            + ReleaseMessages.describeChangePlaceholder() + "\" --release patch");
        out.println("  javachanges next --directory " + repoArg);
    }

    static void printAppliedNextSteps(PrintStream out, Path repoRoot, ReleasePlan plan, Path backupPath) {
        String repoArg = CliOutputSupport.shellQuote(repoRoot.toString());
        out.println();
        out.println(ReleaseMessages.nextSteps());
        if (backupPath != null) {
            out.println("  " + ReleaseMessages.restoreReleasePlanBackupCommand());
            out.println("  javachanges plan --directory " + repoArg + " --restore true");
        }
        out.println("  git -C " + repoArg + " status --short");
        out.println("  git -C " + repoArg + " add "
            + CliOutputSupport.shellQuoteArgs(BuildModelSupport.releasePlanGitAddPaths(repoRoot)));
        out.println("  git -C " + repoArg + " commit -m "
            + CliOutputSupport.shellQuote("chore(release): v" + plan.getReleaseVersion()));
        out.println("  javachanges next --directory " + repoArg);
    }

    static String statusJson(ReleasePlan plan) {
        Map<String, Object> payload = commandPayload("status");
        payload.put("plan", plan.toStatusPayload());
        payload.put("nextCommands", reviewCommands(plan));
        return ReleaseJsonUtils.toPrettyJson(payload);
    }

    static String addJson(Path repoRoot, Path created, ChangesetInput input) {
        Map<String, Object> payload = commandPayload("add");
        payload.put("repository", repoRoot.toString());
        payload.put("createdChangeset", repoRoot.relativize(created).toString());
        payload.put("releaseLevel", input.release.id);
        payload.put("affectedPackages", new ArrayList<String>(input.modules));
        payload.put("summary", input.summary);
        payload.put("nextCommands", addCommands(repoRoot));
        return ReleaseJsonUtils.toPrettyJson(payload);
    }

    static String nextJson(Path repoRoot, ReleasePlan plan) {
        Map<String, Object> payload = commandPayload("next");
        payload.put("repository", repoRoot.toString());
        payload.put("hasPendingChangesets", Boolean.valueOf(plan.hasPendingChangesets()));
        payload.put("pendingChangesets", Integer.valueOf(plan.getChangesetCount()));
        payload.put("releaseVersion", plan.getReleaseVersion());
        payload.put("affectedPackages", new ArrayList<String>(plan.getAffectedPackages()));
        payload.put("nextCommands", nextCommands(plan));
        return ReleaseJsonUtils.toPrettyJson(payload);
    }

    static String modulesJson(Path repoRoot, BuildModelSupport.BuildModel model, List<String> modules) throws java.io.IOException {
        Map<String, Object> payload = commandPayload("modules");
        payload.put("repository", repoRoot.toString());
        payload.put("buildTool", model.type.name().toLowerCase(java.util.Locale.ROOT));
        payload.put("versionFile", BuildModelSupport.revisionFileLabel(repoRoot));
        payload.put("currentRevision", BuildModelSupport.readRevision(repoRoot));
        payload.put("modules", new ArrayList<String>(modules));
        payload.put("nextCommands", moduleCommands(repoRoot, modules));
        return ReleaseJsonUtils.toPrettyJson(payload);
    }

    static String versionJson(Path repoRoot, BuildModelSupport.BuildModel model, String revision) {
        Map<String, Object> payload = commandPayload("version");
        payload.put("repository", repoRoot.toString());
        payload.put("buildTool", model.type.name().toLowerCase(java.util.Locale.ROOT));
        payload.put("versionFile", BuildModelSupport.revisionFileLabel(repoRoot));
        payload.put("currentRevision", revision);
        payload.put("releaseVersion", ReleaseTextUtils.stripSnapshot(revision));
        payload.put("snapshot", Boolean.valueOf(revision.endsWith("-SNAPSHOT")));
        return ReleaseJsonUtils.toPrettyJson(payload);
    }

    static String releaseTagJson(String command, String tag, String releaseVersion, String releaseModule) {
        Map<String, Object> payload = commandPayload(command);
        payload.put("tag", tag);
        payload.put("releaseVersion", releaseVersion);
        payload.put("releaseModule", releaseModule);
        return ReleaseJsonUtils.toPrettyJson(payload);
    }

    static String planJson(Path repoRoot, ReleasePlan plan, boolean apply, boolean applied, String reason, Path backupPath) {
        Map<String, Object> payload = commandPayload("plan");
        payload.put("repository", repoRoot.toString());
        payload.put("apply", Boolean.valueOf(apply));
        payload.put("applied", Boolean.valueOf(applied));
        payload.put("restored", Boolean.FALSE);
        payload.put("reason", reason);
        payload.put("backupFile", backupPath == null ? null : repoRoot.relativize(backupPath).toString());
        payload.put("plan", plan.toStatusPayload());
        payload.put("nextCommands", applied ? appliedCommands(plan) : reviewCommands(plan));
        return ReleaseJsonUtils.toPrettyJson(payload);
    }

    static String planRestoreJson(Path repoRoot, Path backupPath) {
        Map<String, Object> payload = commandPayload("plan");
        payload.put("repository", repoRoot.toString());
        payload.put("apply", Boolean.FALSE);
        payload.put("applied", Boolean.FALSE);
        payload.put("restored", Boolean.TRUE);
        payload.put("backupFile", repoRoot.relativize(backupPath).toString());
        List<String> commands = new ArrayList<String>();
        commands.add("javachanges status --directory " + CliOutputSupport.shellQuote(repoRoot.toString()));
        payload.put("nextCommands", commands);
        return ReleaseJsonUtils.toPrettyJson(payload);
    }

    static String errorJson(String command, Exception exception) {
        Map<String, Object> payload = commandPayload(command);
        payload.put("ok", Boolean.FALSE);
        String message = trimToNull(exception.getMessage());
        payload.put("reason", message == null ? exception.getClass().getSimpleName() : message);
        return ReleaseJsonUtils.toJson(payload);
    }

    private static Map<String, Object> commandPayload(String command) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("ok", Boolean.TRUE);
        payload.put("command", command);
        return payload;
    }

    private static List<String> addCommands(Path repoRoot) {
        String repoArg = CliOutputSupport.shellQuote(repoRoot.toString());
        List<String> commands = new ArrayList<String>();
        commands.add("javachanges status --directory " + repoArg);
        commands.add("javachanges next --directory " + repoArg);
        return commands;
    }

    private static List<String> moduleCommands(Path repoRoot, List<String> modules) {
        String repoArg = CliOutputSupport.shellQuote(repoRoot.toString());
        List<String> commands = new ArrayList<String>();
        if (modules.isEmpty()) {
            return commands;
        }
        commands.add("javachanges add --directory " + repoArg
            + " --modules " + CliOutputSupport.shellQuote(modules.get(0))
            + " --summary \"" + ReleaseMessages.describeChangePlaceholder() + "\" --release patch");
        if (modules.size() > 1) {
            commands.add("javachanges add --directory " + repoArg
                + " --modules all --summary \"" + ReleaseMessages.describeChangePlaceholder() + "\" --release patch");
        }
        return commands;
    }

    private static List<String> nextCommands(ReleasePlan plan) {
        String repoArg = CliOutputSupport.shellQuote(plan.getRepoRoot().toString());
        List<String> commands = new ArrayList<String>();
        if (plan.hasPendingChangesets()) {
            commands.add("javachanges status --directory " + repoArg);
            commands.add("javachanges plan --directory " + repoArg + " --apply true");
            commands.add("javachanges github-release-plan --directory " + repoArg + " --write-plan-files false --execute true");
            commands.add("javachanges gitlab-release-plan --directory " + repoArg + " --write-plan-files false --execute true");
            return commands;
        }
        commands.add("javachanges add --directory " + repoArg + " --summary \""
            + ReleaseMessages.describeChangePlaceholder() + "\" --release patch");
        commands.add("javachanges status --directory " + repoArg);
        return commands;
    }

    private static List<String> reviewCommands(ReleasePlan plan) {
        String repoArg = CliOutputSupport.shellQuote(plan.getRepoRoot().toString());
        List<String> commands = new ArrayList<String>();
        if (plan.hasPendingChangesets()) {
            commands.add("javachanges plan --directory " + repoArg + " --apply true");
            commands.add("javachanges next --directory " + repoArg);
            return commands;
        }
        commands.add("javachanges add --directory " + repoArg + " --summary \""
            + ReleaseMessages.describeChangePlaceholder() + "\" --release patch");
        commands.add("javachanges next --directory " + repoArg);
        return commands;
    }

    private static List<String> appliedCommands(ReleasePlan plan) {
        String repoArg = CliOutputSupport.shellQuote(plan.getRepoRoot().toString());
        List<String> commands = new ArrayList<String>();
        commands.add("javachanges plan --directory " + repoArg + " --restore true");
        commands.add("git -C " + repoArg + " status --short");
        commands.add("git -C " + repoArg + " add "
            + CliOutputSupport.shellQuoteArgs(BuildModelSupport.releasePlanGitAddPaths(
                plan.getRepoRoot())));
        commands.add("git -C " + repoArg + " commit -m "
            + CliOutputSupport.shellQuote("chore(release): v" + plan.getReleaseVersion()));
        commands.add("javachanges next --directory " + repoArg);
        return commands;
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
    @Option(names = "--format", description = "Output format: text or json.")
    private String format;

    @Override
    public Integer call() throws Exception {
        Path repoRoot = repoRoot();
        OutputFormat outputFormat = OutputFormat.parse(format, OutputFormat.TEXT);
        return runJsonCommand("version", outputFormat,
            (name, exception) -> ReleaseWorkflowNextSteps.errorJson(name, exception),
            () -> {
                BuildModelSupport.BuildModel model = BuildModelSupport.detect(repoRoot);
                if (model == null) {
                    throw new IllegalStateException(ReleaseMessages.cannotFindSupportedBuildModel(repoRoot));
                }
                String revision = new VersionSupport(repoRoot).readRevision();
                if (outputFormat == OutputFormat.JSON) {
                    out().println(ReleaseWorkflowNextSteps.versionJson(repoRoot, model, revision));
                } else {
                    out().println(revision);
                }
                return success();
            });
    }
}

@Command(name = "modules", mixinStandardHelpOptions = true,
    description = "List the detected build tool, version file, revision, and modules.")
final class ModulesCommand extends AbstractCliCommand {
    @Option(names = "--format", description = "Output format: text or json.")
    private String format;

    @Override
    public Integer call() throws Exception {
        Path repoRoot = repoRoot();
        OutputFormat outputFormat = OutputFormat.parse(format, OutputFormat.TEXT);
        return runJsonCommand("modules", outputFormat,
            (name, exception) -> ReleaseWorkflowNextSteps.errorJson(name, exception),
            () -> {
                BuildModelSupport.BuildModel model = BuildModelSupport.detect(repoRoot);
                if (model == null) {
                    throw new IllegalStateException(ReleaseMessages.cannotFindSupportedBuildModel(repoRoot));
                }
                List<String> modules = ReleaseModuleUtils.detectKnownModules(repoRoot);
                if (outputFormat == OutputFormat.JSON) {
                    out().println(ReleaseWorkflowNextSteps.modulesJson(repoRoot, model, modules));
                    return success();
                }
                out().println(ReleaseMessages.repository() + ": " + repoRoot);
                out().println(ReleaseMessages.buildTool() + ": " + model.type.name().toLowerCase(java.util.Locale.ROOT));
                out().println(ReleaseMessages.versionFile() + ": " + BuildModelSupport.revisionFileLabel(repoRoot));
                out().println(ReleaseMessages.currentRevision() + ": " + BuildModelSupport.readRevision(repoRoot));
                out().println(ReleaseMessages.modules() + ":");
                for (String module : modules) {
                    out().println("  - " + module);
                }
                printNextSteps(repoRoot, modules);
                return success();
            });
    }

    private void printNextSteps(Path repoRoot, List<String> modules) {
        String repoArg = CliOutputSupport.shellQuote(repoRoot.toString());
        out().println();
        out().println(ReleaseMessages.nextSteps());
        out().println("  javachanges add --directory " + repoArg
            + " --modules " + CliOutputSupport.shellQuote(modules.get(0))
            + " --summary \"" + ReleaseMessages.describeChangePlaceholder() + "\" --release patch");
        if (modules.size() > 1) {
            out().println("  javachanges add --directory " + repoArg
                + " --modules all --summary \"" + ReleaseMessages.describeChangePlaceholder() + "\" --release patch");
        }
    }
}

@Command(name = "release-version-from-tag", mixinStandardHelpOptions = true,
    description = "Extract the version from a release tag.")
final class ReleaseVersionFromTagCommand extends AbstractCliCommand {
    @Option(names = "--tag", required = true, description = "Release tag like v1.2.3.")
    private String tag;

    @Option(names = "--format", description = "Output format: text or json.")
    private String format;

    @Override
    public Integer call() throws Exception {
        OutputFormat outputFormat = OutputFormat.parse(format, OutputFormat.TEXT);
        return runJsonCommand("release-version-from-tag", outputFormat,
            (name, exception) -> ReleaseWorkflowNextSteps.errorJson(name, exception),
            () -> {
                String releaseVersion = releaseVersionFromTag(tag);
                String releaseModule = releaseModuleFromTag(tag);
                if (outputFormat == OutputFormat.JSON) {
                    out().println(ReleaseWorkflowNextSteps.releaseTagJson("release-version-from-tag", tag, releaseVersion, releaseModule));
                } else {
                    out().println(releaseVersion);
                }
                return success();
            });
    }
}

@Command(name = "release-module-from-tag", mixinStandardHelpOptions = true,
    description = "Extract the module from a module release tag.")
final class ReleaseModuleFromTagCommand extends AbstractCliCommand {
    @Option(names = "--tag", required = true, description = "Module tag like sample-module/v1.2.3.")
    private String tag;

    @Option(names = "--format", description = "Output format: text or json.")
    private String format;

    @Override
    public Integer call() throws Exception {
        OutputFormat outputFormat = OutputFormat.parse(format, OutputFormat.TEXT);
        return runJsonCommand("release-module-from-tag", outputFormat,
            (name, exception) -> ReleaseWorkflowNextSteps.errorJson(name, exception),
            () -> {
                String releaseVersion = releaseVersionFromTag(tag);
                String releaseModule = releaseModuleFromTag(tag);
                if (outputFormat == OutputFormat.JSON) {
                    out().println(ReleaseWorkflowNextSteps.releaseTagJson("release-module-from-tag", tag, releaseVersion, releaseModule));
                } else {
                    out().println(releaseModule == null ? "" : releaseModule);
                }
                return success();
            });
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
        out().println(ReleaseMessages.moduleOk());
        return success();
    }
}

@Command(name = "assert-snapshot", mixinStandardHelpOptions = true,
    description = "Validate that the current revision is a SNAPSHOT.")
final class AssertSnapshotCommand extends AbstractCliCommand {
    @Override
    public Integer call() throws Exception {
        new VersionSupport(repoRoot()).assertSnapshot();
        out().println(ReleaseMessages.snapshotOk());
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
        out().println(ReleaseMessages.releaseTagOk());
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

    @Option(names = "--mode", description = "Settings mode: all, release, or snapshot. Defaults to all.")
    private String mode;

    @Override
    public Integer call() throws Exception {
        MavenSettingsWriter.RepositoryMode repositoryMode = repositoryMode();
        if (repositoryMode == null) {
            MavenSettingsWriter.write(Paths.get(output));
        } else {
            MavenSettingsWriter.write(Paths.get(output), repositoryMode);
        }
        out().println(ReleaseMessages.generatedMavenSettings(output));
        return success();
    }

    private MavenSettingsWriter.RepositoryMode repositoryMode() {
        String value = trimToNull(mode);
        if (value == null || "all".equalsIgnoreCase(value)) {
            return null;
        }
        if ("release".equalsIgnoreCase(value)) {
            return MavenSettingsWriter.RepositoryMode.RELEASE;
        }
        if ("snapshot".equalsIgnoreCase(value)) {
            return MavenSettingsWriter.RepositoryMode.SNAPSHOT;
        }
        throw new IllegalArgumentException(ReleaseMessages.unsupportedSettingsMode(value));
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
        out().println(ReleaseMessages.generatedReleaseNotes(output));
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
        out().println(ReleaseMessages.gpgPublicKeyOk(fingerprint));
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
