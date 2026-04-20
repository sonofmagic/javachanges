package io.github.sonofmagic.javachanges.core;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Map;

@Command(name = "preflight", mixinStandardHelpOptions = true,
    description = "Render or execute the Maven publish preflight checks.")
final class PreflightCommand extends AbstractCliCommand {
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

    @Option(names = "--snapshot-build-stamp",
        description = "Explicit snapshot build stamp used to derive the publish revision.")
    private String snapshotBuildStamp;

    @Override
    public Integer call() throws Exception {
        Map<String, String> options = options();
        putFlag(options, "snapshot", snapshot);
        putOption(options, "tag", tag);
        putFlag(options, "allow-dirty", allowDirty);
        putOption(options, "module", module);
        putOption(options, "snapshot-build-stamp", snapshotBuildStamp);
        new PublishSupport(repoRoot(), out()).preflight(PublishRequest.fromOptions(options, false));
        return success();
    }
}

@Command(name = "publish", mixinStandardHelpOptions = true,
    description = "Render or execute the Maven publish command.")
final class PublishCommand extends AbstractCliCommand {
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

    @Option(names = "--snapshot-build-stamp",
        description = "Explicit snapshot build stamp used to derive the publish revision.")
    private String snapshotBuildStamp;

    @Override
    public Integer call() throws Exception {
        Map<String, String> options = options();
        putFlag(options, "snapshot", snapshot);
        putOption(options, "tag", tag);
        putFlag(options, "allow-dirty", allowDirty);
        putFlag(options, "execute", execute);
        putOption(options, "module", module);
        putOption(options, "snapshot-build-stamp", snapshotBuildStamp);
        new PublishSupport(repoRoot(), out()).publish(PublishRequest.fromOptions(options, true));
        return success();
    }
}
