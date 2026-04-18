package io.github.sonofmagic.javachanges;

import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static io.github.sonofmagic.javachanges.ReleaseUtils.*;

public final class JavaChangesCli {

    private JavaChangesCli() {
    }

    public static void main(String[] args) throws Exception {
        try {
            new JavaChangesCliApp(args, System.out, System.err).run();
        } catch (Throwable throwable) {
            throwable.printStackTrace(System.err);
            if (throwable instanceof Exception) {
                throw (Exception) throwable;
            }
            throw new RuntimeException(throwable);
        }
    }
}

final class JavaChangesCliApp {
    private final String[] args;
    private final PrintStream out;
    private final PrintStream err;

    JavaChangesCliApp(String[] args, PrintStream out, PrintStream err) {
        this.args = args;
        this.out = out;
        this.err = err;
    }

    void run() throws Exception {
        if (args.length == 0) {
            usage();
            return;
        }

        String command = args[0];
        Map<String, String> options = parseOptions(args, 1);
        Path repoRoot = RepoFiles.resolveRepoRoot(options.get("directory"));
        RepoFiles.ensureChangesetReadme(repoRoot);

        if ("add".equals(command)) {
            ChangesetInput input = ChangesetPrompter.resolveInput(repoRoot, options, out, err);
            Path created = RepoFiles.writeChangeset(repoRoot, input);
            out.println("Created changeset: " + repoRoot.relativize(created));
            return;
        }

        if ("status".equals(command)) {
            printStatus(new ReleasePlanner(repoRoot).plan());
            return;
        }

        if ("plan".equals(command)) {
            ReleasePlan plan = new ReleasePlanner(repoRoot).plan();
            printStatus(plan);
            if ("true".equalsIgnoreCase(options.get("apply"))) {
                if (!plan.hasPendingChangesets()) {
                    out.println("No pending changesets to apply.");
                    return;
                }
                RepoFiles.applyPlan(repoRoot, plan);
                out.println();
                out.println("Applied release plan for v" + plan.getReleaseVersion());
            }
            return;
        }

        if ("manifest-field".equals(command)) {
            out.println(RepoFiles.readManifestField(repoRoot, requiredOption(options, "field")));
            return;
        }

        if ("version".equals(command)) {
            out.println(new VersionSupport(repoRoot).readRevision());
            return;
        }

        if ("release-version-from-tag".equals(command)) {
            out.println(releaseVersionFromTag(requiredOption(options, "tag")));
            return;
        }

        if ("release-module-from-tag".equals(command)) {
            String module = releaseModuleFromTag(requiredOption(options, "tag"));
            out.println(module == null ? "" : module);
            return;
        }

        if ("assert-module".equals(command)) {
            assertKnownModule(repoRoot, requiredOption(options, "module"));
            out.println("module ok");
            return;
        }

        if ("assert-snapshot".equals(command)) {
            new VersionSupport(repoRoot).assertSnapshot();
            out.println("snapshot ok");
            return;
        }

        if ("assert-release-tag".equals(command)) {
            new VersionSupport(repoRoot).assertReleaseTag(requiredOption(options, "tag"));
            out.println("release tag ok");
            return;
        }

        if ("module-selector-args".equals(command)) {
            out.println(moduleSelectorArgs(repoRoot, trimToNull(options.get("module"))));
            return;
        }

        if ("write-settings".equals(command)) {
            String output = requiredOption(options, "output");
            MavenSettingsWriter.write(Paths.get(output));
            out.println("Generated Maven settings: " + output);
            return;
        }

        if ("init-env".equals(command)) {
            new ReleaseEnvSupport(repoRoot, out).initEnv(InitEnvRequest.fromOptions(options));
            return;
        }

        if ("auth-help".equals(command)) {
            new ReleaseEnvSupport(repoRoot, out).printAuthHelp(platformOption(options));
            return;
        }

        if ("render-vars".equals(command)) {
            new ReleaseEnvSupport(repoRoot, out).renderVars(PlatformEnvRequest.fromOptions(options));
            return;
        }

        if ("doctor-local".equals(command)) {
            new ReleaseEnvSupport(repoRoot, out).doctorLocal(LocalDoctorRequest.fromOptions(options));
            return;
        }

        if ("doctor-platform".equals(command)) {
            new ReleaseEnvSupport(repoRoot, out).doctorPlatform(DoctorPlatformRequest.fromOptions(options));
            return;
        }

        if ("sync-vars".equals(command)) {
            new ReleaseEnvSupport(repoRoot, out).syncVars(SyncVarsRequest.fromOptions(options));
            return;
        }

        if ("audit-vars".equals(command)) {
            new ReleaseEnvSupport(repoRoot, out).auditVars(AuditVarsRequest.fromOptions(options));
            return;
        }

        if ("preflight".equals(command)) {
            new PublishSupport(repoRoot, out).preflight(PublishRequest.fromOptions(options, false));
            return;
        }

        if ("publish".equals(command)) {
            new PublishSupport(repoRoot, out).publish(PublishRequest.fromOptions(options, true));
            return;
        }

        if ("gitlab-release-plan".equals(command)) {
            new GitlabReleaseSupport(repoRoot, out).planMergeRequest(GitlabReleasePlanRequest.fromOptions(options));
            return;
        }

        if ("gitlab-tag-from-plan".equals(command)) {
            new GitlabReleaseSupport(repoRoot, out).tagFromReleasePlan(GitlabTagRequest.fromOptions(options));
            return;
        }

        if ("release-notes".equals(command)) {
            String tag = requiredOption(options, "tag");
            String output = requiredOption(options, "output");
            new ReleaseNotesGenerator(repoRoot).writeReleaseNotes(tag, repoRoot.resolve(output).normalize());
            out.println("Generated release notes: " + output);
            return;
        }

        usage();
    }

    private void printStatus(ReleasePlan plan) {
        out.println("Repository: " + plan.getRepoRoot());
        out.println("Current revision: " + plan.getCurrentRevision());
        out.println(plan.getLatestWholeRepoTag() == null
            ? "Latest whole-repo tag: none"
            : "Latest whole-repo tag: " + plan.getLatestWholeRepoTag());
        out.println("Pending changesets: " + plan.getChangesets().size());

        if (!plan.hasPendingChangesets()) {
            out.println("Next release: none");
            return;
        }

        out.println("Release bump: " + plan.getReleaseLevel().id);
        out.println("Next release: v" + plan.getReleaseVersion());
        out.println("Next snapshot: " + plan.getNextSnapshotVersion());
        out.println();
        for (Changeset changeset : plan.getChangesets()) {
            out.println("- " + changeset.fileName + " [" + changeset.release.id + "] "
                + changeset.type + ": " + changeset.summary);
        }
    }

    private void usage() {
        out.println("Usage:");
        out.println("  javachanges add [--summary text --release patch|minor|major --type feat|fix|docs|build|ci|test|refactor|perf|chore|other --modules a,b]");
        out.println("  javachanges status");
        out.println("  javachanges plan [--apply true]");
        out.println("  javachanges manifest-field --field releaseVersion");
        out.println("  javachanges version");
        out.println("  javachanges release-version-from-tag --tag v1.2.3");
        out.println("  javachanges release-module-from-tag --tag sample-module/v1.2.3");
        out.println("  javachanges assert-snapshot");
        out.println("  javachanges assert-release-tag --tag v1.2.3");
        out.println("  javachanges write-settings --output .m2/settings.xml");
        out.println("  javachanges init-env [--template env/release.env.example --target env/release.env.local --force true]");
        out.println("  javachanges auth-help [--platform github|gitlab|all]");
        out.println("  javachanges render-vars --env-file env/release.env.local [--platform github|gitlab|all] [--show-secrets true]");
        out.println("  javachanges doctor-local --env-file env/release.env.local [--github-repo owner/repo] [--gitlab-repo group/project]");
        out.println("  javachanges doctor-platform --env-file env/release.env.local [--platform github|gitlab|all]");
        out.println("  javachanges sync-vars --env-file env/release.env.local [--platform github|gitlab|all] [--repo owner/repo] [--execute true]");
        out.println("  javachanges audit-vars --env-file env/release.env.local [--platform github|gitlab|all]");
        out.println("  javachanges preflight --snapshot [--module sample-module] [--allow-dirty true]");
        out.println("  javachanges publish --tag v1.2.3 [--execute true]");
        out.println("  javachanges gitlab-release-plan [--project-id 12345 --target-branch main --release-branch changeset-release/main --execute true]");
        out.println("  javachanges gitlab-tag-from-plan [--before-sha <sha> --current-sha <sha> --execute true]");
        out.println("  javachanges release-notes --tag v1.2.3 --output target/release-notes.md");
        out.println();
        out.println("Options:");
        out.println("  --directory <path>   Repository root or subdirectory inside the repository.");
    }
}
