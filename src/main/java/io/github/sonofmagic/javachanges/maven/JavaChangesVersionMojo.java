package io.github.sonofmagic.javachanges.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "version", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresProject = true)
public final class JavaChangesVersionMojo extends AbstractJavaChangesMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        executeStructuredGoal("version", "version");
    }
}
