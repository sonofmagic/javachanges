package io.github.sonofmagic.javachanges.gradle;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GradleTasksSupportTest {
    private static final Pattern COMMAND_ANNOTATION = Pattern.compile("@Command\\(\\s*name\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern GRADLE_TASK_REGISTRATION = Pattern.compile(
        "registerJavaChangesTask\\('([^']+)', '([^']+)'"
    );

    @Test
    void generatedGradleTasksCoverEveryCliCommand() throws Exception {
        Set<String> cliCommands = cliCommandNames();
        Set<String> gradleCommands = registeredGradleCommands();

        Set<String> missingCommands = new TreeSet<String>(cliCommands);
        missingCommands.removeAll(gradleCommands);

        assertEquals(new TreeSet<String>(), missingCommands, "Missing generated Gradle tasks for CLI commands");
    }

    @Test
    void generatedGradleTaskNamesAreUnique() {
        String script = GradleTasksSupport.render("1.11.0");
        Matcher matcher = GRADLE_TASK_REGISTRATION.matcher(script);
        Set<String> taskNames = new TreeSet<String>();
        int count = 0;
        while (matcher.find()) {
            count++;
            assertTrue(taskNames.add(matcher.group(1)), "Duplicate generated Gradle task: " + matcher.group(1));
        }

        assertTrue(count > 20, "Expected generated Gradle task shortcuts");
    }

    @Test
    void generatedGradleScriptReplacesVersionPlaceholder() {
        String script = GradleTasksSupport.render("1.11.0");

        assertTrue(script.contains("orElse('1.11.0')"));
        assertTrue(!script.contains("__JAVACHANGES_VERSION__"));
    }

    @Test
    void generatedGradleScriptExposesGradleTaskBootstrapFlags() {
        String script = GradleTasksSupport.render("1.11.0");

        assertTrue(script.contains("addFlag(args, 'gradleTasks', '--gradle-tasks')"));
        assertTrue(script.contains("addFlag(args, 'applyGradleTasks', '--apply-gradle-tasks')"));
        assertTrue(script.contains("addFlag(args, 'apply', '--apply')"));
    }

    @Test
    void generatedGradleScriptSupportsLocalCliJarAndRepositoryFallback() {
        String script = GradleTasksSupport.render("1.11.0");

        assertTrue(script.contains("def javachangesJar = javachangesProperty('jar')"));
        assertTrue(script.contains("dependencies.add('javachangesCli', files(javachangesJar))"));
        assertTrue(script.contains("repositories.mavenCentral()"));
        assertTrue(script.contains("io.github.sonofmagic:javachanges:${javachangesVersion.get()}"));
    }

    @Test
    void generatedGradleScriptSupportsGlobalCliOptions() {
        String script = GradleTasksSupport.render("1.11.0");

        assertTrue(script.contains("def javachangesTaskProperty = { name ->"));
        assertTrue(script.contains("def scoped = javachangesProperty(\"${command}.${name}\")"));
        assertTrue(script.contains("def javachangesDirectory = {"));
        assertTrue(script.contains("javachangesTaskProperty('directory') ?: rootProject.projectDir.absolutePath"));
        assertTrue(script.contains("addOption(cliArgs, 'language', '--language')"));
        assertTrue(script.contains("javachangesCommand.set(command)"));
        assertTrue(script.contains("javachangesCommand.remove()"));
        assertTrue(script.contains("cliArgs.add('--directory')"));
        assertTrue(script.contains("cliArgs.add(javachangesDirectory())"));
    }

    @Test
    void generatedGradleScriptSupportsExtraCliArgs() {
        String script = GradleTasksSupport.render("1.11.0");

        assertTrue(script.contains("def tokenizeJavaChangesArgs = { value ->"));
        assertTrue(script.contains("def javachangesExtraArgs = { command ->"));
        assertTrue(script.contains("javachangesProperty(\"${command}.extraArgs\")"));
        assertTrue(script.contains("Unterminated quoted argument in -Pjavachanges.extraArgs"));
        assertTrue(script.contains("cliArgs.addAll(javachangesExtraArgs(command))"));
    }

    @Test
    void generatedGradleScriptProvidesTaskOrientedAliases() {
        String script = GradleTasksSupport.render("1.11.0");

        assertTrue(script.contains("registerJavaChangesTask('javachangesStatusJson', 'status'"));
        assertTrue(script.contains("registerJavaChangesTask('javachangesApplyPlan', 'plan'"));
        assertTrue(script.contains("registerJavaChangesTask('javachangesRestorePlan', 'plan'"));
    }

    @Test
    void generatedGradleScriptSupportsJvmArgs() {
        String script = GradleTasksSupport.render("1.11.0");

        assertTrue(script.contains("def cliJvmArgs = tokenizeJavaChangesArgs(javachangesTaskProperty('jvmArgs'))"));
        assertTrue(script.contains("task.jvmArgs(cliJvmArgs)"));
    }

    private static Set<String> registeredGradleCommands() {
        String script = GradleTasksSupport.render("1.11.0");
        Matcher matcher = GRADLE_TASK_REGISTRATION.matcher(script);
        Set<String> commands = new TreeSet<String>();
        while (matcher.find()) {
            commands.add(matcher.group(2));
        }
        return commands;
    }

    private static Set<String> cliCommandNames() throws Exception {
        final Set<String> commands = new TreeSet<String>();
        try (Stream<Path> files = Files.walk(sourceRoot())) {
            files.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
                try {
                    Matcher matcher = COMMAND_ANNOTATION.matcher(
                        new String(Files.readAllBytes(path), StandardCharsets.UTF_8)
                    );
                    while (matcher.find()) {
                        String command = matcher.group(1);
                        if (!"javachanges".equals(command)) {
                            commands.add(command);
                        }
                    }
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            });
        } catch (RuntimeException exception) {
            if (exception.getCause() instanceof Exception) {
                throw (Exception) exception.getCause();
            }
            throw exception;
        }
        return commands;
    }

    private static Path sourceRoot() {
        return Paths.get("").toAbsolutePath().normalize()
            .resolve("src/main/java/io/github/sonofmagic/javachanges/core");
    }
}
