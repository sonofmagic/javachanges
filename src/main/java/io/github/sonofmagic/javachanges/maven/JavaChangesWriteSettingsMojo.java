package io.github.sonofmagic.javachanges.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Mojo(name = "write-settings", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresProject = true)
public final class JavaChangesWriteSettingsMojo extends AbstractJavaChangesMojo {

    @Parameter(property = "javachanges.output", defaultValue = "${project.basedir}/.m2/settings.xml")
    private File output;

    @Parameter(property = "javachanges.settingsMode")
    private String settingsMode;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        List<String> args = new ArrayList<String>();
        JavaChangesMavenPluginSupport.addOption(args, "--output", outputPath());
        JavaChangesMavenPluginSupport.addOption(args, "--mode", settingsMode);
        executeStructuredGoal("write-settings", "write-settings", args.toArray(new String[0]));
    }

    private String outputPath() {
        return output == null ? null : output.getPath();
    }
}
