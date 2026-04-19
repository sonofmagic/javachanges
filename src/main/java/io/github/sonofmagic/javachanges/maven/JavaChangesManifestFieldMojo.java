package io.github.sonofmagic.javachanges.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "manifest-field", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresProject = true)
public final class JavaChangesManifestFieldMojo extends AbstractJavaChangesMojo {

    @Parameter(property = "javachanges.field", required = true)
    private String field;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        executeStructuredGoal("manifest-field", "manifest-field", "--field", field);
    }
}
