package io.github.sonofmagic.javachanges.core.env;

import io.github.sonofmagic.javachanges.core.MavenCommand;
import io.github.sonofmagic.javachanges.core.OutputFormat;
import io.github.sonofmagic.javachanges.core.ReleaseMessages;
import io.github.sonofmagic.javachanges.core.ReleaseProcessUtils;
import io.github.sonofmagic.javachanges.core.ReleaseTextUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class ReleaseEnvDoctorLocalSupport {
    private final Path repoRoot;
    private final PrintStream out;
    private final ReleaseEnvRuntime runtime;
    private final ReleaseEnvDoctorSupport doctorSupport;

    ReleaseEnvDoctorLocalSupport(Path repoRoot, PrintStream out, ReleaseEnvRuntime runtime) {
        this.repoRoot = repoRoot;
        this.out = out;
        this.runtime = runtime;
        this.doctorSupport = new ReleaseEnvDoctorSupport(out);
    }

    boolean doctorLocal(LocalDoctorRequest request) throws IOException, InterruptedException {
        boolean textOutput = request.format != OutputFormat.JSON;
        DoctorLocalState state = new DoctorLocalState();
        List<ReleaseEnvJsonSupport.JsonSection> sections = new ArrayList<ReleaseEnvJsonSupport.JsonSection>();
        ReleaseEnvJsonSupport.JsonSection runtimeSection =
            new ReleaseEnvJsonSupport.JsonSection(ReleaseMessages.localRuntime());
        ReleaseEnvJsonSupport.JsonSection envSection =
            new ReleaseEnvJsonSupport.JsonSection(ReleaseMessages.localEnvFile());
        ReleaseEnvJsonSupport.JsonSection cliSection =
            new ReleaseEnvJsonSupport.JsonSection(ReleaseMessages.platformCli());
        ReleaseEnvJsonSupport.JsonSection repoSection =
            new ReleaseEnvJsonSupport.JsonSection(ReleaseMessages.repositoryIdentifiers());
        sections.add(runtimeSection);
        sections.add(envSection);
        sections.add(cliSection);
        sections.add(repoSection);

        checkRuntime(textOutput, runtimeSection, state);
        Path envPath = checkEnvFile(request, textOutput, envSection, state);
        checkPlatformCli(textOutput, cliSection, state);
        checkRepoIdentifiers(request, textOutput, repoSection, state);

        if (textOutput) {
            out.println();
        }
        List<String> suggestions = new ArrayList<String>();
        if (!state.failed) {
            suggestions.add("make doctor-all GITHUB_REPO=owner/repo GITLAB_REPO=group/project");
            suggestions.add("make sync-all");
            if (textOutput) {
                out.println(ReleaseMessages.localReleaseEnvironmentPassed());
                out.println(ReleaseMessages.suggestedNextSteps());
                out.println("  make doctor-all GITHUB_REPO=owner/repo GITLAB_REPO=group/project");
                out.println("  make sync-all");
            }
            if (request.format == OutputFormat.JSON) {
                out.println(doctorSupport.commandReportJson("doctor-local", true, runtime.relativizePath(envPath),
                    null, sections, suggestions, null));
            }
            return true;
        }

        if (textOutput) {
            out.println(ReleaseMessages.localReleaseEnvironmentNotReadyIntro());
        }
        if (!state.envPresent) {
            String suggestion = ReleaseMessages.runEnvInitSuggestion();
            suggestions.add(suggestion);
            if (textOutput) {
                out.println("  1. " + suggestion);
            }
        }
        if (state.envNeedsEdit) {
            String suggestion = ReleaseMessages.editLocalEnvSuggestion();
            suggestions.add(suggestion);
            if (textOutput) {
                out.println("  2. " + suggestion);
            }
        }
        if (state.javaMissing || state.mavenMissing || state.mavenFailed) {
            String suggestion = ReleaseMessages.installJavaMavenSuggestion();
            suggestions.add(suggestion);
            if (textOutput) {
                out.println("  3. " + suggestion);
            }
        }
        if (state.ghMissing || state.ghAuthFailed || state.glabMissing || state.glabAuthFailed) {
            String suggestion = ReleaseMessages.runAuthHelpSuggestion();
            suggestions.add(suggestion);
            if (textOutput) {
                out.println("  4. " + suggestion);
            }
        }
        String finalSuggestion = ReleaseMessages.runPlatformDoctorSuggestion();
        suggestions.add(finalSuggestion);
        if (textOutput) {
            out.println("  5. " + finalSuggestion);
        }
        String failure = ReleaseMessages.localReleaseEnvironmentNotReady();
        if (request.format == OutputFormat.JSON) {
            out.println(doctorSupport.commandReportJson("doctor-local", false, runtime.relativizePath(envPath),
                null, sections, suggestions, failure));
            return false;
        }
        throw new IllegalStateException(failure);
    }

    private void checkRuntime(boolean textOutput, ReleaseEnvJsonSupport.JsonSection runtimeSection,
                              DoctorLocalState state) throws IOException, InterruptedException {
        if (textOutput) {
            out.println(ReleaseMessages.heading(ReleaseMessages.localRuntime()));
        }
        if (runtime.commandAvailable("java", "-version")) {
            doctorSupport.recordStatus(textOutput, runtimeSection, "java -version", "OK");
        } else {
            doctorSupport.recordStatus(textOutput, runtimeSection, "java", "MISSING");
            state.javaMissing = true;
            state.failed = true;
        }

        Path mvnw = repoRoot.resolve(ReleaseProcessUtils.mavenWrapperPath());
        boolean wrapperPresent = Files.exists(mvnw);
        if (wrapperPresent) {
            doctorSupport.recordStatus(textOutput, runtimeSection, ReleaseProcessUtils.mavenWrapperPath(), "OK");
        } else {
            doctorSupport.recordStatus(textOutput, runtimeSection, ReleaseProcessUtils.mavenWrapperPath(), "MISSING");
        }

        boolean systemMavenPresent = runtime.commandExists("mvn");
        if (!wrapperPresent) {
            doctorSupport.recordStatus(textOutput, runtimeSection, "mvn", systemMavenPresent ? "OK" : "MISSING");
        }

        MavenCommand mavenCommand = ReleaseProcessUtils.resolveMavenCommand(repoRoot);
        if (mavenCommand == null) {
            doctorSupport.recordStatus(textOutput, runtimeSection, "Maven command", "MISSING");
            state.mavenMissing = true;
            state.failed = true;
        } else {
            doctorSupport.recordStatus(textOutput, runtimeSection, "Maven command",
                mavenCommand.command + " (" + mavenCommand.source + ")");
        }

        if (!state.javaMissing && mavenCommand != null) {
            if (runtime.commandAvailable(mavenCommand.command, "-q", "-version")) {
                doctorSupport.recordStatus(textOutput, runtimeSection, mavenCommand.versionLabel(), "OK");
            } else {
                doctorSupport.recordStatus(textOutput, runtimeSection, mavenCommand.versionLabel(), "FAILED");
                state.mavenFailed = true;
                state.failed = true;
            }
        } else {
            String versionLabel = mavenCommand == null ? "maven -q -version" : mavenCommand.versionLabel();
            doctorSupport.recordStatus(textOutput, runtimeSection, versionLabel, "SKIPPED");
        }
    }

    private Path checkEnvFile(LocalDoctorRequest request, boolean textOutput,
                              ReleaseEnvJsonSupport.JsonSection envSection, DoctorLocalState state) throws IOException {
        if (textOutput) {
            out.println();
            out.println(ReleaseMessages.heading(ReleaseMessages.localEnvFile()));
        }
        Path envPath = runtime.resolvePath(request.envFile);
        if (Files.exists(envPath)) {
            state.envPresent = true;
            doctorSupport.recordStatus(textOutput, envSection, runtime.relativizePath(envPath), "OK");
        } else {
            doctorSupport.recordStatus(textOutput, envSection, runtime.relativizePath(envPath), "MISSING");
            state.failed = true;
        }

        if (state.envPresent && !runtime.isExampleFile(envPath)) {
            LoadedEnv env = LoadedEnv.load(envPath);
            ReleaseEnvDoctorSupport.EnvStatusSummary summary =
                doctorSupport.recordCommonEnvStatuses(env, textOutput, envSection);
            state.failed = state.failed || summary.failed;
            state.envNeedsEdit = state.envNeedsEdit || summary.needsEdit;
        } else if (state.envPresent) {
            doctorSupport.recordStatus(textOutput, envSection, "env file type", "INVALID");
            state.failed = true;
        } else {
            doctorSupport.recordStatus(textOutput, envSection, "env values", "SKIPPED");
        }
        return envPath;
    }

    private void checkPlatformCli(boolean textOutput, ReleaseEnvJsonSupport.JsonSection cliSection,
                                  DoctorLocalState state) throws IOException, InterruptedException {
        if (textOutput) {
            out.println();
            out.println(ReleaseMessages.heading(ReleaseMessages.platformCli()));
        }
        if (runtime.commandExists("gh")) {
            doctorSupport.recordStatus(textOutput, cliSection, "gh", "OK");
            if (runtime.runQuietly(Arrays.asList("gh", "auth", "status"))) {
                doctorSupport.recordStatus(textOutput, cliSection, "gh auth status", "OK");
            } else {
                doctorSupport.recordStatus(textOutput, cliSection, "gh auth status", "FAILED");
                state.ghAuthFailed = true;
                state.failed = true;
            }
        } else {
            doctorSupport.recordStatus(textOutput, cliSection, "gh", "MISSING");
            state.ghMissing = true;
            state.failed = true;
        }

        if (runtime.commandExists("glab")) {
            doctorSupport.recordStatus(textOutput, cliSection, "glab", "OK");
            if (runtime.runQuietly(Arrays.asList("glab", "auth", "status"))) {
                doctorSupport.recordStatus(textOutput, cliSection, "glab auth status", "OK");
            } else {
                doctorSupport.recordStatus(textOutput, cliSection, "glab auth status", "FAILED");
                state.glabAuthFailed = true;
                state.failed = true;
            }
        } else {
            doctorSupport.recordStatus(textOutput, cliSection, "glab", "MISSING");
            state.glabMissing = true;
            state.failed = true;
        }
    }

    private void checkRepoIdentifiers(LocalDoctorRequest request, boolean textOutput,
                                      ReleaseEnvJsonSupport.JsonSection repoSection, DoctorLocalState state) {
        if (textOutput) {
            out.println();
            out.println(ReleaseMessages.heading(ReleaseMessages.repositoryIdentifiers()));
        }
        doctorSupport.recordStatus(textOutput, repoSection, "GITHUB_REPO", doctorSupport.repoStatusValue(request.githubRepo));
        doctorSupport.recordStatus(textOutput, repoSection, "GITLAB_REPO", doctorSupport.repoStatusValue(request.gitlabRepo));
        if (!ReleaseTextUtils.isBlank(request.githubRepo) && !doctorSupport.isValidRepoIdentifier(request.githubRepo)) {
            state.failed = true;
        }
        if (!ReleaseTextUtils.isBlank(request.gitlabRepo) && !doctorSupport.isValidRepoIdentifier(request.gitlabRepo)) {
            state.failed = true;
        }
    }

    private static final class DoctorLocalState {
        private boolean failed;
        private boolean envPresent;
        private boolean envNeedsEdit;
        private boolean javaMissing;
        private boolean mavenMissing;
        private boolean mavenFailed;
        private boolean ghMissing;
        private boolean ghAuthFailed;
        private boolean glabMissing;
        private boolean glabAuthFailed;
    }
}
