package io.github.sonofmagic.javachanges.core;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.assertKnownModule;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.moduleSelectorArgs;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.releaseModuleFromTag;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.releaseVersionFromTag;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.trimToNull;

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

    @Option(names = "--modules", description = "Comma-separated Maven artifactIds or all. Written as official Changesets package keys.")
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
        out().println("Created changeset: " + repoRoot.relativize(created));
        return success();
    }
}

@Command(name = "status", mixinStandardHelpOptions = true,
    description = "Show the pending release plan.")
final class StatusCommand extends AbstractCliCommand {
    @Override
    public Integer call() throws Exception {
        JavaChangesStatusPrinter.printStatus(new ReleasePlanner(repoRoot()).plan(), out());
        return success();
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
        return success();
    }
}

@Command(name = "manifest-field", mixinStandardHelpOptions = true,
    description = "Read a field from .changesets/release-plan.json.")
final class ManifestFieldCommand extends AbstractCliCommand {
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
final class VersionCommand extends AbstractCliCommand {
    @Override
    public Integer call() throws Exception {
        out().println(new VersionSupport(repoRoot()).readRevision());
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
    description = "Extract the Maven module from a module release tag.")
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
    description = "Print Maven -pl selector arguments for a module.")
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
        new ReleaseNotesGenerator(repoRoot).writeReleaseNotes(tag, repoRoot.resolve(output).normalize());
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
