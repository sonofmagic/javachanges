package io.github.sonofmagic.javachanges.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.ArrayList;
import java.util.List;

@Mojo(name = "init-gradle-tasks", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresProject = true)
public final class JavaChangesInitGradleTasksMojo extends AbstractJavaChangesMojo {

    @Parameter(property = "javachanges.output", defaultValue = "gradle/javachanges.gradle")
    private String output;

    @Parameter(property = "javachanges.force", defaultValue = "false")
    private boolean force;

    @Parameter(property = "javachanges.apply", defaultValue = "false")
    private boolean apply;

    @Parameter(property = "javachanges.javachangesVersion")
    private String javachangesVersion;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        List<String> args = new ArrayList<String>();
        JavaChangesMavenPluginSupport.addOption(args, "--output", output);
        JavaChangesMavenPluginSupport.addFlag(args, "--force", force);
        JavaChangesMavenPluginSupport.addFlag(args, "--apply", apply);
        JavaChangesMavenPluginSupport.addOption(args, "--javachanges-version", javachangesVersion);
        executeStructuredGoal("init-gradle-tasks", "init-gradle-tasks", args.toArray(new String[0]));
    }
}
