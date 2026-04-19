package io.github.sonofmagic.javachanges;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static io.github.sonofmagic.javachanges.ReleaseUtils.renderVisibleType;
import static io.github.sonofmagic.javachanges.ReleaseUtils.releaseModuleFromTag;
import static io.github.sonofmagic.javachanges.ReleaseUtils.releaseVersionFromTag;
import static io.github.sonofmagic.javachanges.ReleaseUtils.trimToNull;
import static io.github.sonofmagic.javachanges.ReleaseUtils.moduleSelectorArgs;
import static io.github.sonofmagic.javachanges.ReleaseUtils.assertKnownModule;

public final class JavaChangesCli {

    private JavaChangesCli() {
    }

    public static void main(String[] args) {
        int exitCode = execute(args, System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    static int execute(String[] args, PrintStream out, PrintStream err) {
        JavaChangesCommand root = new JavaChangesCommand(out, err);
        CommandLine commandLine = new CommandLine(root);
        commandLine.setOut(new PrintWriter(out, true));
        commandLine.setErr(new PrintWriter(err, true));
        commandLine.setExecutionExceptionHandler(new CliExecutionExceptionHandler());
        return commandLine.execute(args);
    }

    static final class CliExecutionExceptionHandler implements CommandLine.IExecutionExceptionHandler {
        @Override
        public int handleExecutionException(Exception exception, CommandLine commandLine,
                                            CommandLine.ParseResult parseResult) {
            String message = trimToNull(exception.getMessage());
            if (message == null) {
                message = exception.getClass().getSimpleName();
            }
            commandLine.getErr().println(message);
            return commandLine.getCommandSpec().exitCodeOnExecutionException();
        }
    }
}

@Command(
    name = "javachanges",
    mixinStandardHelpOptions = true,
    description = "Java Changesets for Maven repositories and release-plan workflows.",
    subcommands = {
        JavaChangesCommand.AddCommand.class,
        JavaChangesCommand.StatusCommand.class,
        JavaChangesCommand.PlanCommand.class,
        JavaChangesCommand.ManifestFieldCommand.class,
        JavaChangesCommand.VersionCommand.class,
        JavaChangesCommand.ReleaseVersionFromTagCommand.class,
        JavaChangesCommand.ReleaseModuleFromTagCommand.class,
        JavaChangesCommand.AssertModuleCommand.class,
        JavaChangesCommand.AssertSnapshotCommand.class,
        JavaChangesCommand.AssertReleaseTagCommand.class,
        JavaChangesCommand.ModuleSelectorArgsCommand.class,
        JavaChangesCommand.WriteSettingsCommand.class,
        JavaChangesCommand.InitEnvCommand.class,
        JavaChangesCommand.AuthHelpCommand.class,
        JavaChangesCommand.RenderVarsCommand.class,
        JavaChangesCommand.DoctorLocalCommand.class,
        JavaChangesCommand.DoctorPlatformCommand.class,
        JavaChangesCommand.SyncVarsCommand.class,
        JavaChangesCommand.AuditVarsCommand.class,
        JavaChangesCommand.PreflightCommand.class,
        JavaChangesCommand.PublishCommand.class,
        JavaChangesCommand.GitlabReleasePlanCommand.class,
        JavaChangesCommand.GitlabTagFromPlanCommand.class,
        JavaChangesCommand.ReleaseNotesCommand.class,
        HelpCommand.class
    }
)
final class JavaChangesCommand implements Runnable {
    private final PrintStream out;
    private final PrintStream err;

    @Spec
    private CommandSpec spec;

    @Option(
        names = "--directory",
        scope = CommandLine.ScopeType.INHERIT,
        description = "Repository root or subdirectory inside the repository."
    )
    private String directory;

    JavaChangesCommand(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
    }

    @Override
    public void run() {
        spec.commandLine().usage(out);
    }

    private Path repoRoot() {
        return RepoFiles.resolveRepoRoot(directory);
    }

    private static abstract class BaseCommand implements Callable<Integer> {
        @ParentCommand
        private JavaChangesCommand root;

        final PrintStream out() {
            return root.out;
        }

        final PrintStream err() {
            return root.err;
        }

        final Path repoRoot() {
            return root.repoRoot();
        }

        final Map<String, String> options() {
            return new LinkedHashMap<String, String>();
        }

        final void putOption(Map<String, String> options, String key, String value) {
            if (trimToNull(value) != null) {
                options.put(key, value);
            }
        }

        final void putFlag(Map<String, String> options, String key, boolean value) {
            if (value) {
                options.put(key, "true");
            }
        }

        final int success() {
            return 0;
        }
    }

    @Command(name = "add", mixinStandardHelpOptions = true,
        description = "Create a changeset file.")
    static final class AddCommand extends BaseCommand {
        @Option(names = "--summary", description = "Short user-facing release summary.")
        private String summary;

        @Option(names = "--release", description = "Release level: patch, minor, or major.")
        private String release;

        @Option(names = "--type",
            description = "Legacy change type metadata. Accepted for compatibility but not written in the default official-style format.")
        private String type;

        @Option(names = "--modules", description = "Comma-separated Maven artifactIds or all. Written as official Changesets package keys.")
        private String modules;

        @Option(names = "--body", description = "Optional Markdown body after the summary paragraph.")
        private String body;

        @Override
        public Integer call() throws Exception {
            Path repoRoot = repoRoot();
            RepoFiles.ensureChangesetReadme(repoRoot);
            Map<String, String> options = options();
            putOption(options, "summary", summary);
            putOption(options, "release", release);
            putOption(options, "type", type);
            putOption(options, "modules", modules);
            putOption(options, "body", body);
            ChangesetInput input = ChangesetPrompter.resolveInput(repoRoot, options, out(), err());
            Path created = RepoFiles.writeChangeset(repoRoot, input);
            out().println("Created changeset: " + repoRoot.relativize(created));
            return success();
        }
    }

    @Command(name = "status", mixinStandardHelpOptions = true,
        description = "Show the pending release plan.")
    static final class StatusCommand extends BaseCommand {
        @Override
        public Integer call() throws Exception {
            printStatus(new ReleasePlanner(repoRoot()).plan(), out());
            return success();
        }
    }

    @Command(name = "plan", mixinStandardHelpOptions = true,
        description = "Render the release plan and optionally apply it.")
    static final class PlanCommand extends BaseCommand {
        @Option(names = "--apply", arity = "0..1", fallbackValue = "true", defaultValue = "false",
            description = "Apply the planned version, changelog, and manifest updates.")
        private boolean apply;

        @Override
        public Integer call() throws Exception {
            Path repoRoot = repoRoot();
            ReleasePlan plan = new ReleasePlanner(repoRoot).plan();
            printStatus(plan, out());
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
            return success();
        }
    }

    @Command(name = "manifest-field", mixinStandardHelpOptions = true,
        description = "Read a field from .changesets/release-plan.json.")
    static final class ManifestFieldCommand extends BaseCommand {
        @Option(names = "--field", required = true, description = "Manifest field name.")
        private String field;

        @Override
        public Integer call() throws Exception {
            out().println(RepoFiles.readManifestField(repoRoot(), field));
            return success();
        }
    }

    @Command(name = "version", mixinStandardHelpOptions = true,
        description = "Print the current Maven revision.")
    static final class VersionCommand extends BaseCommand {
        @Override
        public Integer call() throws Exception {
            out().println(new VersionSupport(repoRoot()).readRevision());
            return success();
        }
    }

    @Command(name = "release-version-from-tag", mixinStandardHelpOptions = true,
        description = "Extract the version from a release tag.")
    static final class ReleaseVersionFromTagCommand extends BaseCommand {
        @Option(names = "--tag", required = true, description = "Release tag like v1.2.3.")
        private String tag;

        @Override
        public Integer call() {
            out().println(releaseVersionFromTag(tag));
            return success();
        }
    }

    @Command(name = "release-module-from-tag", mixinStandardHelpOptions = true,
        description = "Extract the Maven module from a module release tag.")
    static final class ReleaseModuleFromTagCommand extends BaseCommand {
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
    static final class AssertModuleCommand extends BaseCommand {
        @Option(names = "--module", required = true, description = "Maven artifactId to validate.")
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
    static final class AssertSnapshotCommand extends BaseCommand {
        @Override
        public Integer call() throws Exception {
            new VersionSupport(repoRoot()).assertSnapshot();
            out().println("snapshot ok");
            return success();
        }
    }

    @Command(name = "assert-release-tag", mixinStandardHelpOptions = true,
        description = "Validate that a tag matches the current repository revision.")
    static final class AssertReleaseTagCommand extends BaseCommand {
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
        description = "Print Maven -pl selector arguments for a module.")
    static final class ModuleSelectorArgsCommand extends BaseCommand {
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
    static final class WriteSettingsCommand extends BaseCommand {
        @Option(names = "--output", required = true, description = "Output path for settings.xml.")
        private String output;

        @Override
        public Integer call() throws Exception {
            MavenSettingsWriter.write(Paths.get(output));
            out().println("Generated Maven settings: " + output);
            return success();
        }
    }

    @Command(name = "init-env", mixinStandardHelpOptions = true,
        description = "Write a local release env file from the example template.")
    static final class InitEnvCommand extends BaseCommand {
        @Option(names = "--template", description = "Template env file path.")
        private String template;

        @Option(names = {"--target", "--path"}, description = "Target env file path.")
        private String target;

        @Option(names = "--force", arity = "0..1", fallbackValue = "true", defaultValue = "false",
            description = "Overwrite the target file when it already exists.")
        private boolean force;

        @Override
        public Integer call() throws Exception {
            Map<String, String> options = options();
            putOption(options, "template", template);
            putOption(options, "target", target);
            putFlag(options, "force", force);
            new ReleaseEnvSupport(repoRoot(), out()).initEnv(InitEnvRequest.fromOptions(options));
            return success();
        }
    }

    @Command(name = "auth-help", mixinStandardHelpOptions = true,
        description = "Show required authentication variables for GitHub and GitLab.")
    static final class AuthHelpCommand extends BaseCommand {
        @Option(names = "--platform", description = "github, gitlab, or all.")
        private String platform;

        @Override
        public Integer call() throws Exception {
            Map<String, String> options = options();
            putOption(options, "platform", platform);
            new ReleaseEnvSupport(repoRoot(), out()).printAuthHelp(ReleaseUtils.platformOption(options));
            return success();
        }
    }

    @Command(name = "render-vars", mixinStandardHelpOptions = true,
        description = "Render env variables for GitHub and GitLab.")
    static final class RenderVarsCommand extends BaseCommand {
        @Option(names = "--env-file", required = true, description = "Env file to render.")
        private String envFile;

        @Option(names = "--platform", description = "github, gitlab, or all.")
        private String platform;

        @Option(names = "--format", description = "Output format: text or json.")
        private String format;

        @Option(names = "--show-secrets", arity = "0..1", fallbackValue = "true", defaultValue = "false",
            description = "Show secret values instead of masking them.")
        private boolean showSecrets;

        @Override
        public Integer call() throws Exception {
            Map<String, String> options = options();
            putOption(options, "env-file", envFile);
            putOption(options, "platform", platform);
            putOption(options, "format", format);
            putFlag(options, "show-secrets", showSecrets);
            PlatformEnvRequest request = PlatformEnvRequest.fromOptions(options);
            try {
                return new ReleaseEnvSupport(repoRoot(), out()).renderVars(request) ? success() : 1;
            } catch (Exception exception) {
                if (request.format == OutputFormat.JSON) {
                    out().println(ReleaseEnvSupport.errorJson("render-vars", exception));
                    return 1;
                }
                throw exception;
            }
        }
    }

    @Command(name = "doctor-local", mixinStandardHelpOptions = true,
        description = "Validate local release prerequisites.")
    static final class DoctorLocalCommand extends BaseCommand {
        @Option(names = "--env-file", description = "Env file to validate.")
        private String envFile;

        @Option(names = "--github-repo", description = "GitHub owner/repo.")
        private String githubRepo;

        @Option(names = "--gitlab-repo", description = "GitLab group/project.")
        private String gitlabRepo;

        @Option(names = "--format", description = "Output format: text or json.")
        private String format;

        @Override
        public Integer call() throws Exception {
            Map<String, String> options = options();
            putOption(options, "env-file", envFile);
            putOption(options, "github-repo", githubRepo);
            putOption(options, "gitlab-repo", gitlabRepo);
            putOption(options, "format", format);
            LocalDoctorRequest request = LocalDoctorRequest.fromOptions(options);
            try {
                return new ReleaseEnvSupport(repoRoot(), out()).doctorLocal(request) ? success() : 1;
            } catch (Exception exception) {
                if (request.format == OutputFormat.JSON) {
                    out().println(ReleaseEnvSupport.errorJson("doctor-local", exception));
                    return 1;
                }
                throw exception;
            }
        }
    }

    @Command(name = "doctor-platform", mixinStandardHelpOptions = true,
        description = "Validate remote platform variables and auth.")
    static final class DoctorPlatformCommand extends BaseCommand {
        @Option(names = "--env-file", required = true, description = "Env file to validate.")
        private String envFile;

        @Option(names = "--platform", description = "github, gitlab, or all.")
        private String platform;

        @Option(names = "--github-repo", description = "GitHub owner/repo.")
        private String githubRepo;

        @Option(names = "--gitlab-repo", description = "GitLab group/project.")
        private String gitlabRepo;

        @Option(names = "--format", description = "Output format: text or json.")
        private String format;

        @Override
        public Integer call() throws Exception {
            Map<String, String> options = options();
            putOption(options, "env-file", envFile);
            putOption(options, "platform", platform);
            putOption(options, "github-repo", githubRepo);
            putOption(options, "gitlab-repo", gitlabRepo);
            putOption(options, "format", format);
            DoctorPlatformRequest request = DoctorPlatformRequest.fromOptions(options);
            try {
                return new ReleaseEnvSupport(repoRoot(), out()).doctorPlatform(request) ? success() : 1;
            } catch (Exception exception) {
                if (request.format == OutputFormat.JSON) {
                    out().println(ReleaseEnvSupport.errorJson("doctor-platform", exception));
                    return 1;
                }
                throw exception;
            }
        }
    }

    @Command(name = "sync-vars", mixinStandardHelpOptions = true,
        description = "Sync env variables to GitHub or GitLab.")
    static final class SyncVarsCommand extends BaseCommand {
        @Option(names = "--env-file", required = true, description = "Env file to sync from.")
        private String envFile;

        @Option(names = "--platform", description = "github, gitlab, or all.")
        private String platform;

        @Option(names = "--repo", description = "Repository override for platform sync.")
        private String repo;

        @Option(names = "--execute", arity = "0..1", fallbackValue = "true", defaultValue = "false",
            description = "Execute platform changes instead of a dry run.")
        private boolean execute;

        @Option(names = "--show-secrets", arity = "0..1", fallbackValue = "true", defaultValue = "false",
            description = "Show secret values instead of masking them.")
        private boolean showSecrets;

        @Override
        public Integer call() throws Exception {
            Map<String, String> options = options();
            putOption(options, "env-file", envFile);
            putOption(options, "platform", platform);
            putOption(options, "repo", repo);
            putFlag(options, "execute", execute);
            putFlag(options, "show-secrets", showSecrets);
            new ReleaseEnvSupport(repoRoot(), out()).syncVars(SyncVarsRequest.fromOptions(options));
            return success();
        }
    }

    @Command(name = "audit-vars", mixinStandardHelpOptions = true,
        description = "Audit env variables against remote platform state.")
    static final class AuditVarsCommand extends BaseCommand {
        @Option(names = "--env-file", required = true, description = "Env file to audit.")
        private String envFile;

        @Option(names = "--platform", description = "github, gitlab, or all.")
        private String platform;

        @Option(names = "--github-repo", description = "GitHub owner/repo.")
        private String githubRepo;

        @Option(names = "--gitlab-repo", description = "GitLab group/project.")
        private String gitlabRepo;

        @Option(names = "--format", description = "Output format: text or json.")
        private String format;

        @Override
        public Integer call() throws Exception {
            Map<String, String> options = options();
            putOption(options, "env-file", envFile);
            putOption(options, "platform", platform);
            putOption(options, "github-repo", githubRepo);
            putOption(options, "gitlab-repo", gitlabRepo);
            putOption(options, "format", format);
            AuditVarsRequest request = AuditVarsRequest.fromOptions(options);
            try {
                return new ReleaseEnvSupport(repoRoot(), out()).auditVars(request) ? success() : 1;
            } catch (Exception exception) {
                if (request.format == OutputFormat.JSON) {
                    out().println(ReleaseEnvSupport.errorJson("audit-vars", exception));
                    return 1;
                }
                throw exception;
            }
        }
    }

    @Command(name = "preflight", mixinStandardHelpOptions = true,
        description = "Render or execute the Maven publish preflight checks.")
    static final class PreflightCommand extends BaseCommand {
        @Option(names = "--snapshot", arity = "0..1", fallbackValue = "true", defaultValue = "false",
            description = "Publish the current snapshot instead of a release tag.")
        private boolean snapshot;

        @Option(names = "--tag", description = "Release tag such as v1.2.3.")
        private String tag;

        @Option(names = "--allow-dirty", arity = "0..1", fallbackValue = "true", defaultValue = "false",
            description = "Allow a dirty working tree.")
        private boolean allowDirty;

        @Option(names = "--module", description = "Target Maven module.")
        private String module;

        @Override
        public Integer call() throws Exception {
            Map<String, String> options = options();
            putFlag(options, "snapshot", snapshot);
            putOption(options, "tag", tag);
            putFlag(options, "allow-dirty", allowDirty);
            putOption(options, "module", module);
            new PublishSupport(repoRoot(), out()).preflight(PublishRequest.fromOptions(options, false));
            return success();
        }
    }

    @Command(name = "publish", mixinStandardHelpOptions = true,
        description = "Render or execute the Maven publish command.")
    static final class PublishCommand extends BaseCommand {
        @Option(names = "--snapshot", arity = "0..1", fallbackValue = "true", defaultValue = "false",
            description = "Publish the current snapshot instead of a release tag.")
        private boolean snapshot;

        @Option(names = "--tag", description = "Release tag such as v1.2.3.")
        private String tag;

        @Option(names = "--allow-dirty", arity = "0..1", fallbackValue = "true", defaultValue = "false",
            description = "Allow a dirty working tree.")
        private boolean allowDirty;

        @Option(names = "--execute", arity = "0..1", fallbackValue = "true", defaultValue = "false",
            description = "Run the publish command instead of a dry run.")
        private boolean execute;

        @Option(names = "--module", description = "Target Maven module.")
        private String module;

        @Override
        public Integer call() throws Exception {
            Map<String, String> options = options();
            putFlag(options, "snapshot", snapshot);
            putOption(options, "tag", tag);
            putFlag(options, "allow-dirty", allowDirty);
            putFlag(options, "execute", execute);
            putOption(options, "module", module);
            new PublishSupport(repoRoot(), out()).publish(PublishRequest.fromOptions(options, true));
            return success();
        }
    }

    @Command(name = "gitlab-release-plan", mixinStandardHelpOptions = true,
        description = "Create or update a GitLab release-plan merge request.")
    static final class GitlabReleasePlanCommand extends BaseCommand {
        @Option(names = "--project-id", description = "GitLab project ID.")
        private String projectId;

        @Option(names = "--target-branch", description = "Default branch to open the MR against.")
        private String targetBranch;

        @Option(names = "--release-branch", description = "Release plan branch name.")
        private String releaseBranch;

        @Option(names = "--execute", arity = "0..1", fallbackValue = "true", defaultValue = "false",
            description = "Call the GitLab API instead of a dry run.")
        private boolean execute;

        @Override
        public Integer call() throws Exception {
            Map<String, String> options = options();
            putOption(options, "project-id", projectId);
            putOption(options, "target-branch", targetBranch);
            putOption(options, "release-branch", releaseBranch);
            putFlag(options, "execute", execute);
            new GitlabReleaseSupport(repoRoot(), out()).planMergeRequest(GitlabReleasePlanRequest.fromOptions(options));
            return success();
        }
    }

    @Command(name = "gitlab-tag-from-plan", mixinStandardHelpOptions = true,
        description = "Tag a release from the generated release plan manifest.")
    static final class GitlabTagFromPlanCommand extends BaseCommand {
        @Option(names = "--before-sha", description = "Previous commit SHA.")
        private String beforeSha;

        @Option(names = "--current-sha", description = "Current commit SHA.")
        private String currentSha;

        @Option(names = "--execute", arity = "0..1", fallbackValue = "true", defaultValue = "false",
            description = "Push tags instead of a dry run.")
        private boolean execute;

        @Override
        public Integer call() throws Exception {
            Map<String, String> options = options();
            putOption(options, "before-sha", beforeSha);
            putOption(options, "current-sha", currentSha);
            putFlag(options, "execute", execute);
            new GitlabReleaseSupport(repoRoot(), out()).tagFromReleasePlan(GitlabTagRequest.fromOptions(options));
            return success();
        }
    }

    @Command(name = "release-notes", mixinStandardHelpOptions = true,
        description = "Generate release notes for a tag.")
    static final class ReleaseNotesCommand extends BaseCommand {
        @Option(names = "--tag", required = true, description = "Release tag like v1.2.3.")
        private String tag;

        @Option(names = "--output", required = true, description = "Output path relative to the repository root.")
        private String output;

        @Override
        public Integer call() throws Exception {
            Path repoRoot = repoRoot();
            new ReleaseNotesGenerator(repoRoot).writeReleaseNotes(tag, repoRoot.resolve(output).normalize());
            out().println("Generated release notes: " + output);
            return success();
        }
    }

    static void printStatus(ReleasePlan plan, PrintStream out) {
        out.println("Repository: " + plan.getRepoRoot());
        out.println("Current revision: " + plan.getCurrentRevision());
        out.println(plan.getLatestWholeRepoTag() == null
            ? "Latest whole-repo tag: none"
            : "Latest whole-repo tag: " + plan.getLatestWholeRepoTag());
        out.println("Pending changesets: " + plan.getChangesets().size());

        if (!plan.hasPendingChangesets()) {
            out.println("Release plan: none");
            return;
        }

        out.println("Release plan:");
        out.println("- Release type: " + plan.getReleaseLevel().id);
        out.println("- Affected packages: " + ReleaseUtils.joinModules(plan.getAffectedPackages()));
        out.println("- Release version: v" + plan.getReleaseVersion());
        out.println("- Next snapshot: " + plan.getNextSnapshotVersion());
        out.println();
        out.println("Changesets:");
        for (Changeset changeset : plan.getChangesets()) {
            String visibleType = renderVisibleType(changeset.type);
            out.println("- " + changeset.fileName + " [" + changeset.release.id + "] "
                + "(packages: " + ReleaseUtils.joinModules(changeset.modules) + ") "
                + (visibleType.isEmpty() ? "" : visibleType + ": ") + changeset.summary);
        }
    }
}
