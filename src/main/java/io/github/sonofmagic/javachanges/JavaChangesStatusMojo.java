package io.github.sonofmagic.javachanges;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "status", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresProject = true)
public final class JavaChangesStatusMojo extends AbstractJavaChangesMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        executeStructuredGoal("status", "status");
    }
}
