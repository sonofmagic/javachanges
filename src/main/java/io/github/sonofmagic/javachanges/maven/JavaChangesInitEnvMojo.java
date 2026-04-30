package io.github.sonofmagic.javachanges.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.ArrayList;
import java.util.List;

@Mojo(name = "init-env", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresProject = true)
public final class JavaChangesInitEnvMojo extends AbstractJavaChangesMojo {

    @Parameter(property = "javachanges.template")
    private String template;

    @Parameter(property = "javachanges.target")
    private String target;

    @Parameter(property = "javachanges.force", defaultValue = "false")
    private boolean force;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        List<String> args = new ArrayList<String>();
        JavaChangesMavenPluginSupport.addOption(args, "--template", template);
        JavaChangesMavenPluginSupport.addOption(args, "--target", target);
        JavaChangesMavenPluginSupport.addFlag(args, "--force", force);
        executeStructuredGoal("init-env", "init-env", args.toArray(new String[0]));
    }
}
