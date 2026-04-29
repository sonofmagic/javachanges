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
            getLog().info("Skipping javachanges:" + goalName);
            return;
        }
        getLog().info("Running javachanges:" + goalName);
        execute(JavaChangesMavenPluginSupport.resolveStructuredCliArgs(directoryValue(), command, arguments));
    }

    protected final void executeRunGoal(String rawArgs, String command, String[] arguments) throws MojoFailureException {
        if (skip) {
            getLog().info("Skipping javachanges:run");
            return;
        }
        if (JavaChangesMavenPluginSupport.trimToNull(rawArgs) != null) {
            getLog().info("Running javachanges with javachanges.args");
        } else {
            String effectiveCommand = JavaChangesMavenPluginSupport.trimToNull(command);
            getLog().info("Running javachanges command: " + (effectiveCommand == null ? "status" : effectiveCommand));
        }
        execute(JavaChangesMavenPluginSupport.resolveCliArgs(directoryValue(), command, arguments, rawArgs));
    }

    protected final String directoryValue() {
        return directory == null ? null : directory.getAbsolutePath();
    }

    private void execute(String[] cliArgs) throws MojoFailureException {
        int exitCode = JavaChangesCli.execute(withLanguage(cliArgs), System.out, System.err);
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

    private String[] withLanguage(String[] cliArgs) {
        String languageValue = JavaChangesMavenPluginSupport.trimToNull(language);
        if (languageValue == null || JavaChangesMavenPluginSupport.containsOption(cliArgs, "--language", "--lang")) {
            return cliArgs;
        }
        String[] result = new String[cliArgs.length + 2];
        result[0] = "--language";
        result[1] = languageValue;
        System.arraycopy(cliArgs, 0, result, 2, cliArgs.length);
        return result;
    }
}
