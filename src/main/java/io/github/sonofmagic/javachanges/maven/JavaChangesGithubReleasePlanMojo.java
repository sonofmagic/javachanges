package io.github.sonofmagic.javachanges.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.ArrayList;
import java.util.List;

@Mojo(name = "github-release-plan", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresProject = true)
public final class JavaChangesGithubReleasePlanMojo extends AbstractJavaChangesMojo {

    @Parameter(property = "javachanges.githubRepo")
    private String githubRepo;

    @Parameter(property = "javachanges.targetBranch")
    private String targetBranch;

    @Parameter(property = "javachanges.releaseBranch")
    private String releaseBranch;

    @Parameter(property = "javachanges.execute", defaultValue = "false")
    private boolean execute;

    @Parameter(property = "javachanges.writePlanFiles", defaultValue = "false")
    private boolean writePlanFiles;

    @Parameter(property = "javachanges.format")
    private String format;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        List<String> args = new ArrayList<String>();
        JavaChangesMavenPluginSupport.addOption(args, "--github-repo", githubRepo);
        JavaChangesMavenPluginSupport.addOption(args, "--target-branch", targetBranch);
        JavaChangesMavenPluginSupport.addOption(args, "--release-branch", releaseBranch);
        JavaChangesMavenPluginSupport.addFlag(args, "--execute", execute);
        JavaChangesMavenPluginSupport.addOption(args, "--write-plan-files", String.valueOf(writePlanFiles));
        JavaChangesMavenPluginSupport.addOption(args, "--format", format);
        executeStructuredGoal("github-release-plan", "github-release-plan", args.toArray(new String[0]));
    }
}
