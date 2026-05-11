package io.github.sonofmagic.javachanges.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.ArrayList;
import java.util.List;

@Mojo(name = "gitlab-release-plan", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresProject = true)
public final class JavaChangesGitlabReleasePlanMojo extends AbstractJavaChangesMojo {

    @Parameter(property = "javachanges.projectId")
    private String projectId;

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
        JavaChangesMavenPluginSupport.addOption(args, "--project-id", projectId);
        JavaChangesMavenPluginSupport.addOption(args, "--target-branch", targetBranch);
        JavaChangesMavenPluginSupport.addOption(args, "--release-branch", releaseBranch);
        JavaChangesMavenPluginSupport.addFlag(args, "--execute", execute);
        JavaChangesMavenPluginSupport.addOption(args, "--write-plan-files", String.valueOf(writePlanFiles));
        JavaChangesMavenPluginSupport.addOption(args, "--format", format);
        executeStructuredGoal("gitlab-release-plan", "gitlab-release-plan", args.toArray(new String[0]));
    }
}
