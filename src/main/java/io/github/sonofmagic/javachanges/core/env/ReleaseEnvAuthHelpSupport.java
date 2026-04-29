package io.github.sonofmagic.javachanges.core.env;

import io.github.sonofmagic.javachanges.core.Platform;
import io.github.sonofmagic.javachanges.core.ReleaseMessages;

import java.io.PrintStream;

final class ReleaseEnvAuthHelpSupport {
    private final PrintStream out;

    ReleaseEnvAuthHelpSupport(PrintStream out) {
        this.out = out;
    }

    void printAuthHelp(Platform platform) {
        if (platform.includesGithub()) {
            printGithubAuthHelp();
        }

        if (platform.includesGithub() && platform.includesGitlab()) {
            out.println();
        }

        if (platform.includesGitlab()) {
            printGitlabAuthHelp();
        }
    }

    private void printGithubAuthHelp() {
        out.println(ReleaseMessages.text("== GitHub CLI Login Guide ==", "== GitHub CLI 登录建议 =="));
        out.println();
        out.println(ReleaseMessages.text("1. Install or verify gh is available", "1. 安装或确认 gh 可用"));
        out.println("   gh --version");
        out.println();
        out.println(ReleaseMessages.text("2. Generate a local env template", "2. 生成本地 env 模板"));
        out.println("   make env-init");
        out.println();
        out.println(ReleaseMessages.text("3. Log in with a browser", "3. 使用浏览器登录"));
        out.println("   gh auth login --web --git-protocol ssh");
        out.println();
        out.println(ReleaseMessages.text("4. Check local readiness", "4. 检查本机 readiness"));
        out.println("   make readiness GITHUB_REPO=owner/repo");
        out.println();
        out.println(ReleaseMessages.text("5. Check login status", "5. 检查登录状态"));
        out.println("   gh auth status");
        out.println();
        out.println(ReleaseMessages.text("6. Verify the local doctor", "6. 验证本地 doctor"));
        out.println("   make doctor-github GITHUB_REPO=owner/repo");
        out.println();
        out.println(ReleaseMessages.text("7. Preview platform write commands", "7. 预览将写入的平台命令"));
        out.println("   make sync-github");
        out.println();
        out.println(ReleaseMessages.text("8. Write platform variables", "8. 真正写入平台变量"));
        out.println("   make sync-github-apply GITHUB_REPO=owner/repo");
        out.println();
        out.println(ReleaseMessages.text("9. Audit platform state", "9. 回读审计平台状态"));
        out.println("   make audit-github GITHUB_REPO=owner/repo");
        out.println();
        out.println(ReleaseMessages.text("Official docs:", "官方文档:"));
        out.println("  https://cli.github.com/manual/gh_auth_login");
    }

    private void printGitlabAuthHelp() {
        out.println(ReleaseMessages.text("== GitLab CLI Login Guide ==", "== GitLab CLI 登录建议 =="));
        out.println();
        out.println(ReleaseMessages.text("1. Install or verify glab is available", "1. 安装或确认 glab 可用"));
        out.println("   glab --version");
        out.println();
        out.println(ReleaseMessages.text("2. Generate a local env template", "2. 生成本地 env 模板"));
        out.println("   make env-init");
        out.println();
        out.println(ReleaseMessages.text("3. Log in to GitLab with a browser", "3. 使用浏览器登录 GitLab"));
        out.println("   glab auth login --hostname gitlab.example.com --web --git-protocol ssh --use-keyring");
        out.println();
        out.println(ReleaseMessages.text("4. Or log in with a token", "4. 或使用 token 登录"));
        out.println("   glab auth login --hostname gitlab.example.com --stdin < token.txt");
        out.println();
        out.println(ReleaseMessages.text("5. Check local readiness", "5. 检查本机 readiness"));
        out.println("   make readiness GITLAB_REPO=group/project");
        out.println();
        out.println(ReleaseMessages.text("6. Check login status", "6. 检查登录状态"));
        out.println("   glab auth status");
        out.println();
        out.println(ReleaseMessages.text("7. Verify the local doctor", "7. 验证本地 doctor"));
        out.println("   make doctor-gitlab GITLAB_REPO=group/project");
        out.println();
        out.println(ReleaseMessages.text("8. Preview platform write commands", "8. 预览将写入的平台命令"));
        out.println("   make sync-gitlab");
        out.println();
        out.println(ReleaseMessages.text("9. Write platform variables", "9. 真正写入平台变量"));
        out.println("   make sync-gitlab-apply GITLAB_REPO=group/project");
        out.println();
        out.println(ReleaseMessages.text("10. Audit platform state", "10. 回读审计平台状态"));
        out.println("    make audit-gitlab GITLAB_REPO=group/project");
        out.println();
        out.println(ReleaseMessages.text("Official docs:", "官方文档:"));
        out.println("  https://docs.gitlab.com/cli/auth/login/");
    }
}
