package io.github.sonofmagic.javachanges.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.ArrayList;
import java.util.List;

@Mojo(name = "release-module-from-tag", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresProject = true)
public final class JavaChangesReleaseModuleFromTagMojo extends AbstractJavaChangesMojo {

    @Parameter(property = "javachanges.tag", required = true)
    private String tag;

    @Parameter(property = "javachanges.format")
    private String format;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        List<String> args = new ArrayList<String>();
        JavaChangesMavenPluginSupport.addOption(args, "--tag", tag);
        JavaChangesMavenPluginSupport.addOption(args, "--format", format);
        executeStructuredGoal("release-module-from-tag", "release-module-from-tag", args.toArray(new String[0]));
    }
}
