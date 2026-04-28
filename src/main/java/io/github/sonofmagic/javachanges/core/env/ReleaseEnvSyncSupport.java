package io.github.sonofmagic.javachanges.core.env;

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
        out.println("使用 env 文件: " + envPath);
        if (!request.execute) {
            out.println("当前为 dry-run，只输出命令。传入 --execute true 后才会真正写入平台。");
        }

        if (request.execute) {
            if (ReleaseTextUtils.isBlank(request.repo)) {
                throw new IllegalArgumentException("执行模式下必须通过 --repo 指定仓库");
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
            out.println("== GitHub CLI 命令 ==");
            for (EnvEntry entry : ReleaseEnvCatalog.GITHUB_ACTIONS_VARIABLES) {
                syncGithub(env, request, entry, false);
            }
            for (EnvEntry entry : ReleaseEnvCatalog.GITHUB_ACTIONS_SECRETS) {
                syncGithub(env, request, entry, true);
            }
        }

        if (request.platform.includesGitlab()) {
            out.println();
            out.println("== GitLab CLI 命令 ==");
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
            out.println("执行: " + displayCommand);
            int exitCode = runtime.runCommand(command);
            if (exitCode != 0) {
                throw new IllegalStateException("命令执行失败: " + displayCommand);
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
            out.println("执行: " + displayCommand);
            int exitCode = runtime.runCommand(command);
            if (exitCode != 0) {
                throw new IllegalStateException("命令执行失败: " + displayCommand);
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
