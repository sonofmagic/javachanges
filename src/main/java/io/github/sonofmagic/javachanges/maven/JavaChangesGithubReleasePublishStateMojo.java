package io.github.sonofmagic.javachanges.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.ArrayList;
import java.util.List;

@Mojo(name = "github-release-publish-state", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresProject = true)
public final class JavaChangesGithubReleasePublishStateMojo extends AbstractJavaChangesMojo {

    @Parameter(property = "javachanges.currentSha")
    private String currentSha;

    @Parameter(property = "javachanges.githubOutputFile")
    private String githubOutputFile;

    @Parameter(property = "javachanges.fresh", defaultValue = "false")
    private boolean fresh;

    @Parameter(property = "javachanges.requireReleaseApplyCommit", defaultValue = "true")
    private boolean requireReleaseApplyCommit;

    @Parameter(property = "javachanges.format")
    private String format;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        List<String> args = new ArrayList<String>();
        JavaChangesMavenPluginSupport.addOption(args, "--current-sha", currentSha);
        JavaChangesMavenPluginSupport.addOption(args, "--github-output-file", githubOutputFile);
        JavaChangesMavenPluginSupport.addFlag(args, "--fresh", fresh);
        JavaChangesMavenPluginSupport.addOption(args, "--require-release-apply-commit",
            String.valueOf(requireReleaseApplyCommit));
        JavaChangesMavenPluginSupport.addOption(args, "--format", format);
        executeStructuredGoal("github-release-publish-state", "github-release-publish-state",
            args.toArray(new String[0]));
    }
}
