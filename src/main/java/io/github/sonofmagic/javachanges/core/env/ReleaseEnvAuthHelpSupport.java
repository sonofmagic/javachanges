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
        for (String line : ReleaseMessages.githubAuthHelpLines()) {
            out.println(line);
        }
    }

    private void printGitlabAuthHelp() {
        for (String line : ReleaseMessages.gitlabAuthHelpLines()) {
            out.println(line);
        }
    }
}
