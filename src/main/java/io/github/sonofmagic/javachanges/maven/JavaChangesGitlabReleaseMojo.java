package io.github.sonofmagic.javachanges.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.ArrayList;
import java.util.List;

@Mojo(name = "gitlab-release", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresProject = true)
public final class JavaChangesGitlabReleaseMojo extends AbstractJavaChangesMojo {

    @Parameter(property = "javachanges.tag")
    private String tag;

    @Parameter(property = "javachanges.projectId")
    private String projectId;

    @Parameter(property = "javachanges.gitlabHost")
    private String gitlabHost;

    @Parameter(property = "javachanges.releaseNotesFile")
    private String releaseNotesFile;

    @Parameter(property = "javachanges.execute", defaultValue = "false")
    private boolean execute;

    @Parameter(property = "javachanges.ignoreCatalogValidation", defaultValue = "false")
    private boolean ignoreCatalogValidation;

    @Parameter(property = "javachanges.format")
    private String format;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        List<String> args = new ArrayList<String>();
        JavaChangesMavenPluginSupport.addOption(args, "--tag", tag);
        JavaChangesMavenPluginSupport.addOption(args, "--project-id", projectId);
        JavaChangesMavenPluginSupport.addOption(args, "--gitlab-host", gitlabHost);
        JavaChangesMavenPluginSupport.addOption(args, "--release-notes-file", releaseNotesFile);
        JavaChangesMavenPluginSupport.addFlag(args, "--execute", execute);
        JavaChangesMavenPluginSupport.addFlag(args, "--ignore-catalog-validation", ignoreCatalogValidation);
        JavaChangesMavenPluginSupport.addOption(args, "--format", format);
        executeStructuredGoal("gitlab-release", "gitlab-release", args.toArray(new String[0]));
    }
}
