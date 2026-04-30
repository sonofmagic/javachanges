package io.github.sonofmagic.javachanges.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.ArrayList;
import java.util.List;

@Mojo(name = "ensure-gpg-public-key", defaultPhase = LifecyclePhase.NONE, threadSafe = true, requiresProject = true)
public final class JavaChangesEnsureGpgPublicKeyMojo extends AbstractJavaChangesMojo {

    @Parameter(property = "javachanges.primaryKeyserver", defaultValue = "hkps://keyserver.ubuntu.com")
    private String primaryKeyserver;

    @Parameter(property = "javachanges.secondaryKeyserver", defaultValue = "hkps://keys.openpgp.org")
    private String secondaryKeyserver;

    @Parameter(property = "javachanges.attempts", defaultValue = "12")
    private int attempts;

    @Parameter(property = "javachanges.retryDelaySeconds", defaultValue = "10")
    private int retryDelaySeconds;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        List<String> args = new ArrayList<String>();
        JavaChangesMavenPluginSupport.addOption(args, "--primary-keyserver", primaryKeyserver);
        JavaChangesMavenPluginSupport.addOption(args, "--secondary-keyserver", secondaryKeyserver);
        JavaChangesMavenPluginSupport.addOption(args, "--attempts", String.valueOf(attempts));
        JavaChangesMavenPluginSupport.addOption(args, "--retry-delay-seconds", String.valueOf(retryDelaySeconds));
        executeStructuredGoal("ensure-gpg-public-key", "ensure-gpg-public-key", args.toArray(new String[0]));
    }
}
