package io.github.sonofmagic.javachanges.core;

import io.github.sonofmagic.javachanges.core.publish.PublishRequest;
import io.github.sonofmagic.javachanges.core.publish.PublishSupport;
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

    @Option(names = "--snapshot-version-mode",
        description = "Snapshot version mode: plain or stamped.")
    private String snapshotVersionMode;

    @Option(names = "--format", description = "Output format: text or json.")
    private String format;

    @Override
    public Integer call() throws Exception {
        Map<String, String> options = options(
            flag("snapshot", snapshot),
            option("tag", tag),
            flag("allow-dirty", allowDirty),
            option("module", module),
            option("snapshot-build-stamp", snapshotBuildStamp),
            option("snapshot-version-mode", snapshotVersionMode),
            option("format", format)
        );
        PublishRequest request = PublishRequest.fromOptions(options, false);
        return runAutomationCommand("preflight", request.format,
            () -> publishSupport().preflight(request));
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

    @Option(names = "--snapshot-version-mode",
        description = "Snapshot version mode: plain or stamped.")
    private String snapshotVersionMode;

    @Option(names = "--format", description = "Output format: text or json.")
    private String format;

    @Override
    public Integer call() throws Exception {
        Map<String, String> options = options(
            flag("snapshot", snapshot),
            option("tag", tag),
            flag("allow-dirty", allowDirty),
            flag("execute", execute),
            option("module", module),
            option("snapshot-build-stamp", snapshotBuildStamp),
            option("snapshot-version-mode", snapshotVersionMode),
            option("format", format)
        );
        PublishRequest request = PublishRequest.fromOptions(options, true);
        return runAutomationCommand("publish", request.format,
            () -> publishSupport().publish(request));
    }
}

@Command(name = "gradle-publish", mixinStandardHelpOptions = true,
    description = "Render or execute the Gradle publish command.")
final class GradlePublishCommand extends AbstractCliCommand {
    @Option(names = "--snapshot", arity = "0..1", fallbackValue = "true", defaultValue = "false",
        description = "Publish the current snapshot instead of a release tag.")
    private boolean snapshot;

    @Option(names = "--tag", description = "Release tag such as v1.2.3.")
    private String tag;

    @Option(names = "--allow-dirty", arity = "0..1", fallbackValue = "true", defaultValue = "false",
        description = "Allow a dirty working tree.")
    private boolean allowDirty;

    @Option(names = "--execute", arity = "0..1", fallbackValue = "true", defaultValue = "false",
        description = "Run the Gradle publish command instead of a dry run.")
    private boolean execute;

    @Option(names = "--module", description = "Target Gradle project name.")
    private String module;

    @Option(names = "--task", description = "Gradle publish task name. Defaults to publish.")
    private String task;

    @Option(names = "--snapshot-build-stamp",
        description = "Explicit snapshot build stamp used to derive the publish version.")
    private String snapshotBuildStamp;

    @Option(names = "--snapshot-version-mode",
        description = "Snapshot version mode: plain or stamped.")
    private String snapshotVersionMode;

    @Option(names = "--format", description = "Output format: text or json.")
    private String format;

    @Override
    public Integer call() throws Exception {
        Map<String, String> options = options(
            flag("snapshot", snapshot),
            option("tag", tag),
            flag("allow-dirty", allowDirty),
            flag("execute", execute),
            option("module", module),
            option("snapshot-build-stamp", snapshotBuildStamp),
            option("snapshot-version-mode", snapshotVersionMode),
            option("format", format)
        );
        PublishRequest request = PublishRequest.fromOptions(options, true);
        return runAutomationCommand("gradle-publish", request.format,
            () -> gradlePublishSupport().publish(request, task));
    }
}
