package io.github.sonofmagic.javachanges.core.plan;

import io.github.sonofmagic.javachanges.core.ReleaseModuleUtils;
import io.github.sonofmagic.javachanges.core.ReleaseMessages;
import io.github.sonofmagic.javachanges.core.ReleaseTextUtils;
import io.github.sonofmagic.javachanges.core.changeset.Changeset;

import java.io.PrintStream;

public final class JavaChangesStatusPrinter {
    private JavaChangesStatusPrinter() {
    }

    public static void printStatus(ReleasePlan plan, PrintStream out) {
        out.println(ReleaseMessages.repository() + ": " + plan.getRepoRoot());
        out.println(ReleaseMessages.currentRevision() + ": " + plan.getCurrentRevision());
        out.println(plan.getLatestWholeRepoTag() == null
            ? ReleaseMessages.latestWholeRepoTag() + ": " + ReleaseMessages.none()
            : ReleaseMessages.latestWholeRepoTag() + ": " + plan.getLatestWholeRepoTag());
        out.println(ReleaseMessages.pendingChangesets() + ": " + plan.getChangesets().size());

        if (!plan.hasPendingChangesets()) {
            out.println(ReleaseMessages.releasePlan() + ": " + ReleaseMessages.none());
            return;
        }

        out.println(ReleaseMessages.releasePlan() + ":");
        out.println("- " + ReleaseMessages.releaseType() + ": " + plan.getReleaseLevel().id);
        out.println("- " + ReleaseMessages.affectedPackages() + ": " + ReleaseModuleUtils.joinModules(plan.getAffectedPackages()));
        out.println("- " + ReleaseMessages.releaseVersion() + ": v" + plan.getReleaseVersion());
        out.println("- " + ReleaseMessages.nextSnapshot() + ": " + plan.getNextSnapshotVersion());
        out.println();
        out.println(ReleaseMessages.changesets() + ":");
        for (Changeset changeset : plan.getChangesets()) {
            String visibleType = ReleaseTextUtils.renderVisibleType(changeset.type);
            out.println("- " + changeset.fileName + " [" + changeset.release.id + "] "
                + "(" + ReleaseMessages.changelogPackagesLabel() + ": " + ReleaseModuleUtils.joinModules(changeset.modules) + ") "
                + (visibleType.isEmpty() ? "" : visibleType + ": ") + changeset.summary);
        }
    }
}
