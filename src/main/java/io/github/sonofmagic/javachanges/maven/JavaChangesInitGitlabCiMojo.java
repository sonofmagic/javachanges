package io.github.sonofmagic.javachanges.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.ArrayList;
import java.util.List;

@Mojo(name = "init-gitlab-ci", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresProject = true)
public final class JavaChangesInitGitlabCiMojo extends AbstractJavaChangesMojo {

    @Parameter(property = "javachanges.output", defaultValue = ".gitlab-ci.yml")
    private String output;

    @Parameter(property = "javachanges.force", defaultValue = "false")
    private boolean force;

    @Parameter(property = "javachanges.javachangesVersion")
    private String javachangesVersion;

    @Parameter(property = "javachanges.buildTool", defaultValue = "auto")
    private String buildTool;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        List<String> args = new ArrayList<String>();
        JavaChangesMavenPluginSupport.addOption(args, "--output", output);
        JavaChangesMavenPluginSupport.addFlag(args, "--force", force);
        JavaChangesMavenPluginSupport.addOption(args, "--javachanges-version", javachangesVersion);
        JavaChangesMavenPluginSupport.addOption(args, "--build-tool", buildTool);
        executeStructuredGoal("init-gitlab-ci", "init-gitlab-ci", args.toArray(new String[0]));
    }
}
