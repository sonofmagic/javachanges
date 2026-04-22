package io.github.sonofmagic.javachanges.core;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static io.github.sonofmagic.javachanges.core.ReleaseUtils.isBlank;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.padRight;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.renderCommand;
import static io.github.sonofmagic.javachanges.core.ReleaseUtils.runCommand;

final class ReleaseEnvSyncSupport {
    private final java.nio.file.Path repoRoot;
    private final PrintStream out;
    private final ReleaseEnvRuntime runtime;

    ReleaseEnvSyncSupport(java.nio.file.Path repoRoot, PrintStream out, ReleaseEnvRuntime runtime) {
        this.repoRoot = repoRoot;
        this.out = out;
        this.runtime = runtime;
    }

    void syncVars(SyncVarsRequest request, LoadedEnv env, String envPath) throws IOException, InterruptedException {
        out.println("使用 env 文件: " + envPath);
        if (!request.execute) {
            out.println("当前为 dry-run，只输出命令。传入 --execute true 后才会真正写入平台。");
        }

        if (request.execute) {
            if (isBlank(request.repo)) {
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
        if (!isBlank(request.repo)) {
            command.add("--repo");
            command.add(request.repo);
        }
        if (request.execute) {
            out.println("执行: " + renderCommand(command));
            int exitCode = runCommand(command, repoRoot);
            if (exitCode != 0) {
                throw new IllegalStateException("命令执行失败: " + renderCommand(command));
            }
            return;
        }
        String previewValue = secret ? value.renderMasked(request.showSecrets) : value.raw;
        out.println("gh " + (secret ? "secret" : "variable") + " set "
            + padRight(entry.name, secret ? 36 : 34) + " --body " + previewValue
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
        if (!isBlank(request.repo)) {
            command.add("--repo");
            command.add(request.repo);
        }
        if (request.execute) {
            out.println("执行: " + renderCommand(command));
            int exitCode = runCommand(command, repoRoot);
            if (exitCode != 0) {
                throw new IllegalStateException("命令执行失败: " + renderCommand(command));
            }
            return;
        }
        out.println("glab variable set " + padRight(entry.name, 31) + " --value "
            + value.renderMasked(request.showSecrets)
            + (entry.secret ? " --masked" : "")
            + (entry.protectedValue ? " --protected" : "")
            + runtime.repoFlagPreview(request.repo));
    }
}
