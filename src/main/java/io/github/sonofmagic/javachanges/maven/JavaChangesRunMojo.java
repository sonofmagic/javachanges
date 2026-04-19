package io.github.sonofmagic.javachanges.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "run", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresProject = true)
public final class JavaChangesRunMojo extends AbstractJavaChangesMojo {

    @Parameter(property = "javachanges.command")
    private String command;

    @Parameter
    private String[] arguments;

    @Parameter(property = "javachanges.args")
    private String args;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            executeRunGoal(args, command, arguments);
        } catch (IllegalArgumentException exception) {
            throw new MojoFailureException(exception.getMessage(), exception);
        }
    }
}
