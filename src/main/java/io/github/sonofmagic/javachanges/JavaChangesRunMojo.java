package io.github.sonofmagic.javachanges;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

@Mojo(name = "run", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresProject = true)
public final class JavaChangesRunMojo extends AbstractMojo {

    @Parameter(property = "javachanges.command")
    private String command;

    @Parameter
    private String[] arguments;

    @Parameter(property = "javachanges.args")
    private String args;

    @Parameter(property = "javachanges.directory", defaultValue = "${project.basedir}")
    private File directory;

    @Parameter(property = "javachanges.skip", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping javachanges:run");
            return;
        }
        String directoryValue = directory == null ? null : directory.getAbsolutePath();
        String[] cliArgs;
        try {
            cliArgs = JavaChangesMavenPluginSupport.resolveCliArgs(directoryValue, command, arguments, args);
        } catch (IllegalArgumentException exception) {
            throw new MojoFailureException(exception.getMessage(), exception);
        }
        String effectiveCommand = ReleaseUtils.trimToNull(command);
        if (ReleaseUtils.trimToNull(args) != null) {
            getLog().info("Running javachanges with javachanges.args");
        } else {
            getLog().info("Running javachanges command: " + (effectiveCommand == null ? "status" : effectiveCommand));
        }
        int exitCode = JavaChangesCli.execute(cliArgs, System.out, System.err);
        if (exitCode != 0) {
            throw new MojoFailureException("javachanges failed with exit code " + exitCode);
        }
    }
}
