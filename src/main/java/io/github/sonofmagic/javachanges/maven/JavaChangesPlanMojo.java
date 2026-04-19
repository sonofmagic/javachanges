package io.github.sonofmagic.javachanges.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.ArrayList;
import java.util.List;

@Mojo(name = "plan", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresProject = true)
public final class JavaChangesPlanMojo extends AbstractJavaChangesMojo {

    @Parameter(property = "javachanges.apply", defaultValue = "false")
    private boolean apply;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        List<String> args = new ArrayList<String>();
        JavaChangesMavenPluginSupport.addFlag(args, "--apply", apply);
        executeStructuredGoal("plan", "plan", args.toArray(new String[0]));
    }
}
