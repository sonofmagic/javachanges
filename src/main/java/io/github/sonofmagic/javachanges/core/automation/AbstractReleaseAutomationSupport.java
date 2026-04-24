package io.github.sonofmagic.javachanges.core.automation;

import io.github.sonofmagic.javachanges.core.OutputFormat;
import io.github.sonofmagic.javachanges.core.ReleaseAutomationSupport;
import io.github.sonofmagic.javachanges.core.plan.ReleasePlan;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;

public abstract class AbstractReleaseAutomationSupport {
    protected final Path repoRoot;
    protected final PrintStream out;
    protected final ReleaseArtifactSupport artifactSupport;
    protected final ReleaseAutomationSupport automationSupport;

    protected AbstractReleaseAutomationSupport(Path repoRoot, PrintStream out) {
        this.repoRoot = repoRoot;
        this.out = out;
        this.artifactSupport = new ReleaseArtifactSupport(repoRoot);
        this.automationSupport = new ReleaseAutomationSupport(repoRoot);
    }

    protected final boolean isTextOutput(OutputFormat format) {
        return AutomationJsonSupport.isText(format);
    }

    protected final AutomationJsonSupport.AutomationReport newAutomationReport(String command, String action, boolean execute) {
        AutomationJsonSupport.AutomationReport report = new AutomationJsonSupport.AutomationReport(command);
        report.action = action;
        report.execute = execute;
        report.dryRun = !execute;
        return report;
    }

    protected final ReleaseAutomationSupport.ReleaseDescriptor descriptorFromPlan(ReleasePlan plan,
                                                                                 AutomationJsonSupport.AutomationReport report) {
        ReleaseAutomationSupport.ReleaseDescriptor release = automationSupport.descriptorFromPlan(plan);
        applyReleaseDescriptor(report, release);
        return release;
    }

    protected final ReleaseAutomationSupport.ReleaseDescriptor descriptorFromManifest(
        AutomationJsonSupport.AutomationReport report) throws IOException {
        ReleaseAutomationSupport.ReleaseDescriptor release = automationSupport.descriptorFromManifest();
        applyReleaseDescriptor(report, release);
        return release;
    }

    protected final boolean skipWhenNoPendingChangesets(ReleasePlan plan, AutomationJsonSupport.AutomationReport report,
                                                        boolean textOutput, String message) {
        if (plan.hasPendingChangesets()) {
            return false;
        }
        report.skipped = true;
        report.reason = "No pending changesets.";
        AutomationJsonSupport.print(out, textOutput, report, message);
        return true;
    }

    protected final boolean skipDryRun(AutomationJsonSupport.AutomationReport report, boolean textOutput, String message) {
        if (report.execute) {
            return false;
        }
        report.reason = "Dry-run only.";
        AutomationJsonSupport.print(out, textOutput, report, message);
        return true;
    }

    protected final boolean skipWhenRemoteTagExists(AutomationJsonSupport.AutomationReport report, boolean textOutput,
                                                    String tagName) {
        report.skipped = true;
        report.reason = "Tag already exists remotely: " + tagName;
        AutomationJsonSupport.print(out, textOutput, report, "Tag already exists remotely. Skip.");
        return true;
    }

    protected final Path releasePlanMarkdownFile() {
        return automationSupport.releasePlanMarkdownFile();
    }

    private void applyReleaseDescriptor(AutomationJsonSupport.AutomationReport report,
                                        ReleaseAutomationSupport.ReleaseDescriptor release) {
        report.releaseVersion = release.releaseVersion;
        report.tagStrategy = release.tagStrategy.id;
        report.tags = release.tagNames();
        report.tag = release.releaseTargets.size() == 1 ? release.tagNames().get(0) : null;
    }
}
