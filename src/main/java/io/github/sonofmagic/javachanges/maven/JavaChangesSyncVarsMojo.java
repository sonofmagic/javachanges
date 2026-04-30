package io.github.sonofmagic.javachanges.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.ArrayList;
import java.util.List;

@Mojo(name = "sync-vars", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresProject = true)
public final class JavaChangesSyncVarsMojo extends AbstractJavaChangesMojo {

    @Parameter(property = "javachanges.envFile", required = true)
    private String envFile;

    @Parameter(property = "javachanges.platform")
    private String platform;

    @Parameter(property = "javachanges.repo")
    private String repo;

    @Parameter(property = "javachanges.execute", defaultValue = "false")
    private boolean execute;

    @Parameter(property = "javachanges.showSecrets", defaultValue = "false")
    private boolean showSecrets;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        List<String> args = new ArrayList<String>();
        JavaChangesMavenPluginSupport.addOption(args, "--env-file", envFile);
        JavaChangesMavenPluginSupport.addOption(args, "--platform", platform);
        JavaChangesMavenPluginSupport.addOption(args, "--repo", repo);
        JavaChangesMavenPluginSupport.addFlag(args, "--execute", execute);
        JavaChangesMavenPluginSupport.addFlag(args, "--show-secrets", showSecrets);
        executeStructuredGoal("sync-vars", "sync-vars", args.toArray(new String[0]));
    }
}
