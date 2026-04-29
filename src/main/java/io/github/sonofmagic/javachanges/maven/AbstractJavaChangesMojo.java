package io.github.sonofmagic.javachanges.maven;

import io.github.sonofmagic.javachanges.core.JavaChangesCli;
import io.github.sonofmagic.javachanges.core.ReleaseLanguage;
import io.github.sonofmagic.javachanges.core.ReleaseLanguageContext;
import io.github.sonofmagic.javachanges.core.ReleaseMessages;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

abstract class AbstractJavaChangesMojo extends AbstractMojo {

    @Parameter(property = "javachanges.directory", defaultValue = "${project.basedir}")
    private File directory;

    @Parameter(property = "javachanges.skip", defaultValue = "false")
    private boolean skip;

    @Parameter(property = "javachanges.language")
    private String language;

    protected final void executeStructuredGoal(String goalName, String command, String... arguments) throws MojoFailureException {
        if (skip) {
            getLog().info(ReleaseMessages.skippingJavachangesGoal(goalName));
            return;
        }
        getLog().info(ReleaseMessages.runningJavachangesGoal(goalName));
        execute(JavaChangesMavenPluginSupport.resolveStructuredCliArgs(directoryValue(), command, arguments));
    }

    protected final void executeRunGoal(String rawArgs, String command, String[] arguments) throws MojoFailureException {
        if (skip) {
            getLog().info(ReleaseMessages.skippingJavachangesRun());
            return;
        }
        if (JavaChangesMavenPluginSupport.trimToNull(rawArgs) != null) {
            getLog().info(ReleaseMessages.runningJavachangesArgs());
        } else {
            String effectiveCommand = JavaChangesMavenPluginSupport.trimToNull(command);
            getLog().info(ReleaseMessages.runningJavachangesCommand(effectiveCommand == null ? "status" : effectiveCommand));
        }
        execute(JavaChangesMavenPluginSupport.resolveCliArgs(directoryValue(), command, arguments, rawArgs));
    }

    protected final String directoryValue() {
        return directory == null ? null : directory.getAbsolutePath();
    }

    private void execute(String[] cliArgs) throws MojoFailureException {
        int exitCode = JavaChangesCli.execute(
            JavaChangesMavenPluginSupport.prependLanguageIfMissing(language, cliArgs),
            System.out,
            System.err
        );
        if (exitCode != 0) {
            ReleaseLanguageContext.set(mojoLanguage());
            try {
                throw new MojoFailureException(ReleaseMessages.javachangesFailed(exitCode));
            } finally {
                ReleaseLanguageContext.clear();
            }
        }
    }

    private ReleaseLanguage mojoLanguage() {
        String languageValue = JavaChangesMavenPluginSupport.trimToNull(language);
        try {
            return languageValue == null ? ReleaseLanguage.fromEnvironment() : ReleaseLanguage.parse(languageValue);
        } catch (IllegalArgumentException exception) {
            return ReleaseLanguage.EN;
        }
    }
}
