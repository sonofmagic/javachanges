package io.github.sonofmagic.javachanges.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.ArrayList;
import java.util.List;

@Mojo(name = "github-release-from-plan", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresProject = true)
public final class JavaChangesGithubReleaseFromPlanMojo extends AbstractJavaChangesMojo {

    @Parameter(property = "javachanges.releaseNotesFile")
    private String releaseNotesFile;

    @Parameter(property = "javachanges.githubOutputFile")
    private String githubOutputFile;

    @Parameter(property = "javachanges.execute", defaultValue = "false")
    private boolean execute;

    @Parameter(property = "javachanges.fresh", defaultValue = "false")
    private boolean fresh;

    @Parameter(property = "javachanges.format")
    private String format;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        List<String> args = new ArrayList<String>();
        JavaChangesMavenPluginSupport.addOption(args, "--release-notes-file", releaseNotesFile);
        JavaChangesMavenPluginSupport.addOption(args, "--github-output-file", githubOutputFile);
        JavaChangesMavenPluginSupport.addFlag(args, "--execute", execute);
        JavaChangesMavenPluginSupport.addFlag(args, "--fresh", fresh);
        JavaChangesMavenPluginSupport.addOption(args, "--format", format);
        executeStructuredGoal("github-release-from-plan", "github-release-from-plan", args.toArray(new String[0]));
    }
}
