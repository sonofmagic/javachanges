package io.github.sonofmagic.javachanges.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.ArrayList;
import java.util.List;

@Mojo(name = "github-tag-from-plan", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresProject = true)
public final class JavaChangesGithubTagFromPlanMojo extends AbstractJavaChangesMojo {

    @Parameter(property = "javachanges.currentSha")
    private String currentSha;

    @Parameter(property = "javachanges.execute", defaultValue = "false")
    private boolean execute;

    @Parameter(property = "javachanges.fresh", defaultValue = "false")
    private boolean fresh;

    @Parameter(property = "javachanges.format")
    private String format;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        List<String> args = new ArrayList<String>();
        JavaChangesMavenPluginSupport.addOption(args, "--current-sha", currentSha);
        JavaChangesMavenPluginSupport.addFlag(args, "--execute", execute);
        JavaChangesMavenPluginSupport.addFlag(args, "--fresh", fresh);
        JavaChangesMavenPluginSupport.addOption(args, "--format", format);
        executeStructuredGoal("github-tag-from-plan", "github-tag-from-plan", args.toArray(new String[0]));
    }
}
