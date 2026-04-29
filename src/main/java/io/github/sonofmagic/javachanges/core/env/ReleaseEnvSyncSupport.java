package io.github.sonofmagic.javachanges.core.env;

import io.github.sonofmagic.javachanges.core.ReleaseMessages;
import io.github.sonofmagic.javachanges.core.ReleaseTextUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

final class ReleaseEnvSyncSupport {
    private final PrintStream out;
    private final ReleaseEnvRuntime runtime;

    ReleaseEnvSyncSupport(java.nio.file.Path repoRoot, PrintStream out, ReleaseEnvRuntime runtime) {
        this.out = out;
        this.runtime = runtime;
    }

    void syncVars(SyncVarsRequest request, LoadedEnv env, String envPath) throws IOException, InterruptedException {
        out.println(ReleaseMessages.usingEnvFile(envPath));
        if (!request.execute) {
            out.println(ReleaseMessages.dryRunOnlyPrintsCommands());
        }

        if (request.execute) {
            if (ReleaseTextUtils.isBlank(request.repo)) {
                throw new IllegalArgumentException(ReleaseMessages.repoRequiredInExecuteMode());
            }
            if (request.platform.includesGithub()) {
                runtime.requireCommand("gh");
            }
            if (request.platform.includesGitlab()) {
                runtime.requireCommand("glab");
            }
        }

        if (request.platform.includesGithub()) {
            out.println();
            out.println(ReleaseMessages.githubCliCommandsHeading());
            for (EnvEntry entry : ReleaseEnvCatalog.GITHUB_ACTIONS_VARIABLES) {
                syncGithub(env, request, entry, false);
            }
            for (EnvEntry entry : ReleaseEnvCatalog.GITHUB_ACTIONS_SECRETS) {
                syncGithub(env, request, entry, true);
            }
        }

        if (request.platform.includesGitlab()) {
            out.println();
            out.println(ReleaseMessages.gitlabCliCommandsHeading());
            for (EnvEntry entry : ReleaseEnvCatalog.GITLAB_VARIABLES) {
                syncGitlab(env, request, entry);
            }
        }
    }

    private void syncGithub(LoadedEnv env, SyncVarsRequest request, EnvEntry entry, boolean secret)
        throws IOException, InterruptedException {
        EnvValue value = env.value(entry.name);
        if (!value.isReal()) {
            return;
        }
        List<String> command = new ArrayList<String>();
        command.add("gh");
        command.add(secret ? "secret" : "variable");
        command.add("set");
        command.add(entry.name);
        command.add("--body");
        command.add(value.raw);
        if (!ReleaseTextUtils.isBlank(request.repo)) {
            command.add("--repo");
            command.add(request.repo);
        }
        if (request.execute) {
            String displayCommand = ReleaseTextUtils.renderCommand(maskedCommand(command, "--body", value, secret));
            out.println(ReleaseMessages.runningCommand(displayCommand));
            int exitCode = runtime.runCommand(command);
            if (exitCode != 0) {
                throw new IllegalStateException(ReleaseMessages.commandFailed(displayCommand));
            }
            return;
        }
        String previewValue = secret ? value.renderMasked(request.showSecrets) : value.raw;
        out.println("gh " + (secret ? "secret" : "variable") + " set "
            + ReleaseTextUtils.padRight(entry.name, secret ? 36 : 34) + " --body " + previewValue
            + runtime.repoFlagPreview(request.repo));
    }

    private void syncGitlab(LoadedEnv env, SyncVarsRequest request, EnvEntry entry)
        throws IOException, InterruptedException {
        EnvValue value = env.value(entry.name);
        if (!value.isReal()) {
            return;
        }
        List<String> command = new ArrayList<String>();
        command.add("glab");
        command.add("variable");
        command.add("set");
        command.add(entry.name);
        command.add("--value");
        command.add(value.raw);
        if (entry.secret) {
            command.add("--masked");
        }
        if (entry.protectedValue) {
            command.add("--protected");
        }
        if (!ReleaseTextUtils.isBlank(request.repo)) {
            command.add("--repo");
            command.add(request.repo);
        }
        if (request.execute) {
            String displayCommand = ReleaseTextUtils.renderCommand(maskedCommand(command, "--value", value, entry.secret));
            out.println(ReleaseMessages.runningCommand(displayCommand));
            int exitCode = runtime.runCommand(command);
            if (exitCode != 0) {
                throw new IllegalStateException(ReleaseMessages.commandFailed(displayCommand));
            }
            return;
        }
        out.println("glab variable set " + ReleaseTextUtils.padRight(entry.name, 31) + " --value "
            + value.renderMasked(request.showSecrets)
            + (entry.secret ? " --masked" : "")
            + (entry.protectedValue ? " --protected" : "")
            + runtime.repoFlagPreview(request.repo));
    }

    private List<String> maskedCommand(List<String> command, String valueOption, EnvValue value, boolean secret) {
        if (!secret) {
            return command;
        }
        int valueOptionIndex = command.indexOf(valueOption);
        if (valueOptionIndex < 0 || valueOptionIndex + 1 >= command.size()) {
            return command;
        }
        List<String> masked = new ArrayList<String>(command);
        masked.set(valueOptionIndex + 1, value.renderMasked(false));
        return masked;
    }
}
