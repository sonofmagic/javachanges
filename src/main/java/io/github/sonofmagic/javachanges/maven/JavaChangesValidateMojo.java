package io.github.sonofmagic.javachanges.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.ArrayList;
import java.util.List;

@Mojo(name = "validate", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresProject = true)
public final class JavaChangesValidateMojo extends AbstractJavaChangesMojo {

    @Parameter(property = "javachanges.checkDirty", defaultValue = "false")
    private boolean checkDirty;

    @Parameter(property = "javachanges.format")
    private String format;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        List<String> args = new ArrayList<String>();
        JavaChangesMavenPluginSupport.addFlag(args, "--check-dirty", checkDirty);
        JavaChangesMavenPluginSupport.addOption(args, "--format", format);
        executeStructuredGoal("validate", "validate", args.toArray(new String[0]));
    }
}
