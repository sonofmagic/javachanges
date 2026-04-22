package io.github.sonofmagic.javachanges.core;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

import java.io.PrintStream;
import java.nio.file.Path;

@Command(
    name = "javachanges",
    mixinStandardHelpOptions = true,
    description = "Java Changesets for Maven repositories and release-plan workflows.",
    subcommands = {
        AddCommand.class,
        StatusCommand.class,
        PlanCommand.class,
        ManifestFieldCommand.class,
        VersionCommand.class,
        ReleaseVersionFromTagCommand.class,
        ReleaseModuleFromTagCommand.class,
        AssertModuleCommand.class,
        AssertSnapshotCommand.class,
        AssertReleaseTagCommand.class,
        ModuleSelectorArgsCommand.class,
        WriteSettingsCommand.class,
        InitEnvCommand.class,
        AuthHelpCommand.class,
        RenderVarsCommand.class,
        DoctorLocalCommand.class,
        DoctorPlatformCommand.class,
        SyncVarsCommand.class,
        AuditVarsCommand.class,
        PreflightCommand.class,
        PublishCommand.class,
        GithubReleasePlanCommand.class,
        GithubTagFromPlanCommand.class,
        GithubReleaseFromPlanCommand.class,
        GitlabReleasePlanCommand.class,
        GitlabTagFromPlanCommand.class,
        ReleaseNotesCommand.class,
        EnsureGpgPublicKeyCommand.class,
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

    PrintStream out() {
        return out;
    }

    PrintStream err() {
        return err;
    }

    Path repoRoot() {
        return RepoFiles.resolveRepoRoot(directory);
    }
}
