package io.github.sonofmagic.javachanges.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.ArrayList;
import java.util.List;

@Mojo(name = "setup", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresProject = true)
public final class JavaChangesSetupMojo extends AbstractJavaChangesMojo {

    @Parameter(property = "javachanges.force", defaultValue = "false")
    private boolean force;

    @Parameter(property = "javachanges.env", defaultValue = "false")
    private boolean env;

    @Parameter(property = "javachanges.githubActions", defaultValue = "false")
    private boolean githubActions;

    @Parameter(property = "javachanges.gitlabCi", defaultValue = "false")
    private boolean gitlabCi;

    @Parameter(property = "javachanges.gradleTasks", defaultValue = "false")
    private boolean gradleTasks;

    @Parameter(property = "javachanges.applyGradleTasks", defaultValue = "false")
    private boolean applyGradleTasks;

    @Parameter(property = "javachanges.javachangesVersion")
    private String javachangesVersion;

    @Parameter(property = "javachanges.buildTool", defaultValue = "auto")
    private String buildTool;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        List<String> args = new ArrayList<String>();
        JavaChangesMavenPluginSupport.addFlag(args, "--force", force);
        JavaChangesMavenPluginSupport.addFlag(args, "--env", env);
        JavaChangesMavenPluginSupport.addFlag(args, "--github-actions", githubActions);
        JavaChangesMavenPluginSupport.addFlag(args, "--gitlab-ci", gitlabCi);
        JavaChangesMavenPluginSupport.addFlag(args, "--gradle-tasks", gradleTasks);
        JavaChangesMavenPluginSupport.addFlag(args, "--apply-gradle-tasks", applyGradleTasks);
        JavaChangesMavenPluginSupport.addOption(args, "--javachanges-version", javachangesVersion);
        JavaChangesMavenPluginSupport.addOption(args, "--build-tool", buildTool);
        executeStructuredGoal("setup", "setup", args.toArray(new String[0]));
    }
}
