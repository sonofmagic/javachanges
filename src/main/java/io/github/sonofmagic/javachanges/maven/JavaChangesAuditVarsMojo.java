package io.github.sonofmagic.javachanges.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.ArrayList;
import java.util.List;

@Mojo(name = "audit-vars", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresProject = true)
public final class JavaChangesAuditVarsMojo extends AbstractJavaChangesMojo {

    @Parameter(property = "javachanges.envFile", required = true)
    private String envFile;

    @Parameter(property = "javachanges.platform")
    private String platform;

    @Parameter(property = "javachanges.githubRepo")
    private String githubRepo;

    @Parameter(property = "javachanges.gitlabRepo")
    private String gitlabRepo;

    @Parameter(property = "javachanges.format")
    private String format;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        List<String> args = new ArrayList<String>();
        JavaChangesMavenPluginSupport.addOption(args, "--env-file", envFile);
        JavaChangesMavenPluginSupport.addOption(args, "--platform", platform);
        JavaChangesMavenPluginSupport.addOption(args, "--github-repo", githubRepo);
        JavaChangesMavenPluginSupport.addOption(args, "--gitlab-repo", gitlabRepo);
        JavaChangesMavenPluginSupport.addOption(args, "--format", format);
        executeStructuredGoal("audit-vars", "audit-vars", args.toArray(new String[0]));
    }
}
