package io.github.sonofmagic.javachanges.core.env;

import io.github.sonofmagic.javachanges.core.OutputFormat;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class ReleaseEnvRenderSupport {
    private final PrintStream out;

    ReleaseEnvRenderSupport(PrintStream out) {
        this.out = out;
    }

    boolean renderVars(LoadedEnv env, PlatformEnvRequest request, String envPath) {
        if (request.format == OutputFormat.JSON) {
            out.println(renderVarsJson(env, request, envPath));
            return true;
        }
        out.println("使用 env 文件: " + envPath);
        if (!request.showSecrets) {
            out.println("敏感值默认已打码。传入 --show-secrets true 可显示原值。");
        }

        if (request.platform.includesGithub()) {
            out.println();
            out.println("== GitHub Actions Variables ==");
            printEnvEntries(env, ReleaseEnvCatalog.GITHUB_ACTIONS_VARIABLES, request.showSecrets);
            out.println();
            out.println("== GitHub Actions Secrets ==");
            printEnvEntries(env, ReleaseEnvCatalog.GITHUB_ACTIONS_SECRETS, request.showSecrets);
        }

        if (request.platform.includesGitlab()) {
            out.println();
            out.println("== GitLab CI/CD Variables ==");
            out.println("GITLAB_RELEASE_TOKEN                     OPTIONAL (fallback: CI_JOB_TOKEN)");
            for (EnvEntry entry : ReleaseEnvCatalog.GITLAB_VARIABLES) {
                if ("GITLAB_RELEASE_TOKEN".equals(entry.name)) {
                    continue;
                }
                EnvValue value = env.value(entry.name);
                String rendered = entry.secret ? value.renderMasked(request.showSecrets) : value.statusOrRaw();
                printStatus(entry.name, rendered);
            }
            printStatus("GITLAB_RELEASE_TOKEN", env.value("GITLAB_RELEASE_TOKEN").renderMasked(request.showSecrets));
        }
        return true;
    }

    String renderVarsJson(LoadedEnv env, PlatformEnvRequest request, String envPath) {
        List<ReleaseEnvJsonSupport.JsonSection> sections = new ArrayList<ReleaseEnvJsonSupport.JsonSection>();
        if (request.platform.includesGithub()) {
            sections.add(buildSection("GitHub Actions Variables", env, ReleaseEnvCatalog.GITHUB_ACTIONS_VARIABLES,
                request.showSecrets));
            sections.add(buildSection("GitHub Actions Secrets", env, ReleaseEnvCatalog.GITHUB_ACTIONS_SECRETS,
                request.showSecrets));
        }

        if (request.platform.includesGitlab()) {
            ReleaseEnvJsonSupport.JsonSection gitlab = new ReleaseEnvJsonSupport.JsonSection("GitLab CI/CD Variables");
            gitlab.add("GITLAB_RELEASE_TOKEN", "OPTIONAL (fallback: CI_JOB_TOKEN)");
            for (EnvEntry entry : ReleaseEnvCatalog.GITLAB_VARIABLES) {
                if ("GITLAB_RELEASE_TOKEN".equals(entry.name)) {
                    continue;
                }
                EnvValue value = env.value(entry.name);
                gitlab.add(entry.name, entry.secret ? value.renderMasked(request.showSecrets) : value.statusOrRaw());
            }
            gitlab.add("GITLAB_RELEASE_TOKEN", env.value("GITLAB_RELEASE_TOKEN").renderMasked(request.showSecrets));
            sections.add(gitlab);
        }
        return ReleaseEnvJsonSupport.commandReportJson("render-vars", true, envPath, request.platform.id,
            request.showSecrets, sections, Collections.<String>emptyList(), null);
    }

    private ReleaseEnvJsonSupport.JsonSection buildSection(String title, LoadedEnv env, List<EnvEntry> entries,
                                                           boolean showSecrets) {
        ReleaseEnvJsonSupport.JsonSection section = new ReleaseEnvJsonSupport.JsonSection(title);
        for (EnvEntry entry : entries) {
            EnvValue value = env.value(entry.name);
            section.add(entry.name, entry.secret ? value.renderMasked(showSecrets) : value.statusOrRaw());
        }
        return section;
    }

    private void printEnvEntries(LoadedEnv env, List<EnvEntry> entries, boolean showSecrets) {
        for (EnvEntry entry : entries) {
            EnvValue value = env.value(entry.name);
            String rendered = entry.secret ? value.renderMasked(showSecrets) : value.statusOrRaw();
            printStatus(entry.name, rendered);
        }
    }

    private void printStatus(String label, String value) {
        out.printf("%-40s %s%n", label, value);
    }
}
