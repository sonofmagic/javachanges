package io.github.sonofmagic.javachanges.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.ArrayList;
import java.util.List;

@Mojo(name = "gradle-publish", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresProject = true)
public final class JavaChangesGradlePublishMojo extends AbstractJavaChangesMojo {

    @Parameter(property = "javachanges.snapshot", defaultValue = "false")
    private boolean snapshot;

    @Parameter(property = "javachanges.tag")
    private String tag;

    @Parameter(property = "javachanges.allowDirty", defaultValue = "false")
    private boolean allowDirty;

    @Parameter(property = "javachanges.execute", defaultValue = "false")
    private boolean execute;

    @Parameter(property = "javachanges.module")
    private String module;

    @Parameter(property = "javachanges.task")
    private String task;

    @Parameter(property = "javachanges.snapshotBuildStamp")
    private String snapshotBuildStamp;

    @Parameter(property = "javachanges.snapshotVersionMode")
    private String snapshotVersionMode;

    @Parameter(property = "javachanges.format")
    private String format;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        List<String> args = new ArrayList<String>();
        JavaChangesMavenPluginSupport.addFlag(args, "--snapshot", snapshot);
        JavaChangesMavenPluginSupport.addOption(args, "--tag", tag);
        JavaChangesMavenPluginSupport.addFlag(args, "--allow-dirty", allowDirty);
        JavaChangesMavenPluginSupport.addFlag(args, "--execute", execute);
        JavaChangesMavenPluginSupport.addOption(args, "--module", module);
        JavaChangesMavenPluginSupport.addOption(args, "--task", task);
        JavaChangesMavenPluginSupport.addOption(args, "--snapshot-build-stamp", snapshotBuildStamp);
        JavaChangesMavenPluginSupport.addOption(args, "--snapshot-version-mode", snapshotVersionMode);
        JavaChangesMavenPluginSupport.addOption(args, "--format", format);
        executeStructuredGoal("gradle-publish", "gradle-publish", args.toArray(new String[0]));
    }
}
