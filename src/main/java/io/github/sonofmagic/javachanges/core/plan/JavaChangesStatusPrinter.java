package io.github.sonofmagic.javachanges.core.plan;

import io.github.sonofmagic.javachanges.core.ReleaseModuleUtils;
import io.github.sonofmagic.javachanges.core.ReleaseTextUtils;
import io.github.sonofmagic.javachanges.core.changeset.Changeset;

import java.io.PrintStream;

public final class JavaChangesStatusPrinter {
    private JavaChangesStatusPrinter() {
    }

    public static void printStatus(ReleasePlan plan, PrintStream out) {
        out.println("Repository: " + plan.getRepoRoot());
        out.println("Current revision: " + plan.getCurrentRevision());
        out.println(plan.getLatestWholeRepoTag() == null
            ? "Latest whole-repo tag: none"
            : "Latest whole-repo tag: " + plan.getLatestWholeRepoTag());
        out.println("Pending changesets: " + plan.getChangesets().size());

        if (!plan.hasPendingChangesets()) {
            out.println("Release plan: none");
            return;
        }

        out.println("Release plan:");
        out.println("- Release type: " + plan.getReleaseLevel().id);
        out.println("- Affected packages: " + ReleaseModuleUtils.joinModules(plan.getAffectedPackages()));
        out.println("- Release version: v" + plan.getReleaseVersion());
        out.println("- Next snapshot: " + plan.getNextSnapshotVersion());
        out.println();
        out.println("Changesets:");
        for (Changeset changeset : plan.getChangesets()) {
            String visibleType = ReleaseTextUtils.renderVisibleType(changeset.type);
            out.println("- " + changeset.fileName + " [" + changeset.release.id + "] "
                + "(packages: " + ReleaseModuleUtils.joinModules(changeset.modules) + ") "
                + (visibleType.isEmpty() ? "" : visibleType + ": ") + changeset.summary);
        }
    }
}
