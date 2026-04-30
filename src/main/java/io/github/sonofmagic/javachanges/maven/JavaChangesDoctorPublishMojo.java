package io.github.sonofmagic.javachanges.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.ArrayList;
import java.util.List;

@Mojo(name = "doctor-publish", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresProject = true)
public final class JavaChangesDoctorPublishMojo extends AbstractJavaChangesMojo {

    @Parameter(property = "javachanges.target")
    private String target;

    @Parameter(property = "javachanges.mode")
    private String mode;

    @Parameter(property = "javachanges.allowDirty", defaultValue = "false")
    private boolean allowDirty;

    @Parameter(property = "javachanges.format")
    private String format;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        List<String> args = new ArrayList<String>();
        JavaChangesMavenPluginSupport.addOption(args, "--target", target);
        JavaChangesMavenPluginSupport.addOption(args, "--mode", mode);
        JavaChangesMavenPluginSupport.addFlag(args, "--allow-dirty", allowDirty);
        JavaChangesMavenPluginSupport.addOption(args, "--format", format);
        executeStructuredGoal("doctor-publish", "doctor-publish", args.toArray(new String[0]));
    }
}
