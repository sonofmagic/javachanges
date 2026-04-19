package io.github.sonofmagic.javachanges.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.ArrayList;
import java.util.List;

@Mojo(name = "add", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresProject = true)
public final class JavaChangesAddMojo extends AbstractJavaChangesMojo {

    @Parameter(property = "javachanges.summary")
    private String summary;

    @Parameter(property = "javachanges.release")
    private String release;

    @Parameter(property = "javachanges.type")
    private String type;

    @Parameter(property = "javachanges.modules")
    private String modules;

    @Parameter(property = "javachanges.body")
    private String body;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        List<String> args = new ArrayList<String>();
        JavaChangesMavenPluginSupport.addOption(args, "--summary", summary);
        JavaChangesMavenPluginSupport.addOption(args, "--release", release);
        JavaChangesMavenPluginSupport.addOption(args, "--type", type);
        JavaChangesMavenPluginSupport.addOption(args, "--modules", modules);
        JavaChangesMavenPluginSupport.addOption(args, "--body", body);
        executeStructuredGoal("add", "add", args.toArray(new String[0]));
    }
}
