package io.github.sonofmagic.javachanges.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.ArrayList;
import java.util.List;

@Mojo(name = "init", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresProject = true)
public final class JavaChangesInitMojo extends AbstractJavaChangesMojo {

    @Parameter(property = "javachanges.config", defaultValue = "false")
    private boolean config;

    @Parameter(property = "javachanges.force", defaultValue = "false")
    private boolean force;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        List<String> args = new ArrayList<String>();
        JavaChangesMavenPluginSupport.addFlag(args, "--config", config);
        JavaChangesMavenPluginSupport.addFlag(args, "--force", force);
        executeStructuredGoal("init", "init", args.toArray(new String[0]));
    }
}
