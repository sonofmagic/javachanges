package io.github.sonofmagic.javachanges.core;

import io.github.sonofmagic.javachanges.core.changeset.Changeset;
import io.github.sonofmagic.javachanges.core.changeset.ChangesetFileSupport;
import io.github.sonofmagic.javachanges.core.config.ChangesetConfigSupport;
import io.github.sonofmagic.javachanges.core.plan.ReleasePlan;
import io.github.sonofmagic.javachanges.core.plan.ReleasePlanner;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class RepoValidationSupport {
    private RepoValidationSupport() {
    }

    static Report validate(Path repoRoot, boolean checkDirty) throws IOException, InterruptedException {
        Report report = new Report(repoRoot, checkDirty);
        checkBuildModel(repoRoot, report);
        boolean gitRepository = checkGitRepository(repoRoot, report);
        checkChangesetConfig(repoRoot, report);
        checkChangesets(repoRoot, report);
        if (checkDirty && gitRepository) {
            checkDirtyWorktree(repoRoot, report);
        }
        if (!report.hasErrors()) {
            checkReleasePlan(repoRoot, gitRepository, report);
        }
        return report;
    }

    private static void checkBuildModel(Path repoRoot, Report report) {
        BuildModelSupport.BuildModel model = BuildModelSupport.detect(repoRoot);
        if (model == null) {
            report.error("BUILD_MODEL", ReleaseMessages.cannotFindSupportedBuildModel(repoRoot), null);
            return;
        }
        report.buildTool = model.type.name().toLowerCase(java.util.Locale.ROOT);
        report.versionFile = repoRoot.relativize(model.versionFile).toString();
        try {
            String revision = BuildModelSupport.readRevision(repoRoot);
            report.currentRevision = revision;
            Semver.parse(ReleaseTextUtils.stripSnapshot(revision));
            if (!revision.endsWith("-SNAPSHOT")) {
                report.error("VERSION_NOT_SNAPSHOT", ReleaseMessages.notSnapshot(revision), report.versionFile);
            }
        } catch (RuntimeException exception) {
            report.error("VERSION", message(exception), report.versionFile);
        } catch (IOException exception) {
            report.error("VERSION", message(exception), report.versionFile);
        }
    }

    private static boolean checkGitRepository(Path repoRoot, Report report) throws IOException, InterruptedException {
        CommandResult result = ReleaseProcessUtils.runCapture(repoRoot, "git", "rev-parse", "--is-inside-work-tree");
        if (result.exitCode == 0 && "true".equals(result.stdoutText().trim())) {
            report.gitRepository = true;
            return true;
        }
        report.error("GIT_REPOSITORY", ReleaseMessages.notGitRepository(repoRoot), null);
        return false;
    }

    private static void checkChangesetConfig(Path repoRoot, Report report) {
        try {
            ChangesetConfigSupport.ChangesetConfig config = ChangesetConfigSupport.load(repoRoot);
            report.tagStrategy = config.tagStrategy().id;
        } catch (RuntimeException exception) {
            report.error("CHANGESET_CONFIG", message(exception), ChangesetPaths.DIR);
        } catch (IOException exception) {
            report.error("CHANGESET_CONFIG", message(exception), ChangesetPaths.DIR);
        }
    }

    private static void checkChangesets(Path repoRoot, Report report) throws IOException {
        List<Path> paths = ChangesetFileSupport.listPendingChangesetFiles(repoRoot);
        report.pendingChangesets = paths.size();
        for (Path path : paths) {
            try {
                Changeset changeset = ChangesetFileSupport.parseChangeset(repoRoot, path);
                for (String module : changeset.modules) {
                    if (!report.affectedPackages.contains(module)) {
                        report.affectedPackages.add(module);
                    }
                }
            } catch (RuntimeException exception) {
                report.error("CHANGESET", message(exception), relative(repoRoot, path));
            } catch (IOException exception) {
                report.error("CHANGESET", message(exception), relative(repoRoot, path));
            }
        }
    }

    private static void checkDirtyWorktree(Path repoRoot, Report report) throws IOException, InterruptedException {
        CommandResult result = ReleaseProcessUtils.runCapture(repoRoot, "git", "status", "--porcelain");
        if (result.exitCode != 0) {
            report.error("WORKTREE", result.stderrText().trim(), null);
            return;
        }
        if (!result.stdoutText().trim().isEmpty()) {
            report.error("DIRTY_WORKTREE", ReleaseMessages.dirtyWorktree(), null);
        }
    }

    private static void checkReleasePlan(Path repoRoot, boolean gitRepository, Report report) throws IOException, InterruptedException {
        ReleasePlan plan = new ReleasePlanner(repoRoot).plan();
        report.releaseVersion = plan.getReleaseVersion();
        report.nextSnapshotVersion = plan.getNextSnapshotVersion();
        report.plannedTags.addAll(plan.getPlannedTags());
        if (!gitRepository || report.plannedTags.isEmpty()) {
            return;
        }
        for (String tag : report.plannedTags) {
            CommandResult result = ReleaseProcessUtils.runCapture(repoRoot, "git", "tag", "--list", tag);
            if (result.exitCode != 0) {
                report.error("RELEASE_TAG", result.stderrText().trim(), null);
                continue;
            }
            if (!result.stdoutText().trim().isEmpty()) {
                report.error("RELEASE_TAG_EXISTS", ReleaseMessages.localTagAlreadyExists(tag), null);
            }
        }
    }

    static void printText(Report report, PrintStream out) {
        out.println(report.ok() ? ReleaseMessages.validationPassed(report.repoRoot) : ReleaseMessages.validationFailed(report.repoRoot));
        out.println(ReleaseMessages.validationChecksHeading());
        printCheck(out, ReleaseMessages.buildTool(), report.buildTool);
        printCheck(out, ReleaseMessages.versionFile(), report.versionFile);
        printCheck(out, ReleaseMessages.currentRevision(), report.currentRevision);
        printCheck(out, ReleaseMessages.pendingChangesets(), String.valueOf(report.pendingChangesets));
        printCheck(out, ReleaseMessages.releaseVersion(), report.releaseVersion == null ? ReleaseMessages.none() : "v" + report.releaseVersion);
        printCheck(out, ReleaseMessages.nextSnapshot(), report.nextSnapshotVersion);
        printCheck(out, ReleaseMessages.tagStrategyField(), report.tagStrategy);
        printCheck(out, ReleaseMessages.plannedTagsField(), report.plannedTags.isEmpty()
            ? ReleaseMessages.none()
            : ReleaseModuleUtils.joinModules(report.plannedTags));
        printIssues(out, ReleaseMessages.validationErrorsHeading(), report.errors());
        printIssues(out, ReleaseMessages.validationWarningsHeading(), report.warnings());
    }

    private static void printCheck(PrintStream out, String label, String value) {
        if (ReleaseTextUtils.trimToNull(value) != null) {
            out.println("- " + label + ": " + value);
        }
    }

    private static void printIssues(PrintStream out, String heading, List<Issue> issues) {
        if (issues.isEmpty()) {
            return;
        }
        out.println();
        out.println(heading);
        for (Issue issue : issues) {
            String suffix = issue.path == null ? "" : " (" + issue.path + ")";
            out.println("- [" + issue.code + "] " + issue.message + suffix);
        }
    }

    private static String message(Exception exception) {
        String message = ReleaseTextUtils.trimToNull(exception.getMessage());
        return message == null ? exception.getClass().getSimpleName() : message;
    }

    private static String relative(Path repoRoot, Path path) {
        return repoRoot.relativize(path).toString();
    }

    static final class Report {
        final Path repoRoot;
        final boolean checkDirty;
        boolean gitRepository;
        String buildTool;
        String versionFile;
        String currentRevision;
        String releaseVersion;
        String nextSnapshotVersion;
        String tagStrategy;
        int pendingChangesets;
        final List<String> affectedPackages = new ArrayList<String>();
        final List<String> plannedTags = new ArrayList<String>();
        final List<Issue> issues = new ArrayList<Issue>();

        Report(Path repoRoot, boolean checkDirty) {
            this.repoRoot = repoRoot;
            this.checkDirty = checkDirty;
        }

        boolean ok() {
            return !hasErrors();
        }

        boolean hasErrors() {
            return !errors().isEmpty();
        }

        void error(String code, String message, String path) {
            issues.add(new Issue("ERROR", code, message, path));
        }

        @SuppressWarnings("unused")
        void warning(String code, String message, String path) {
            issues.add(new Issue("WARNING", code, message, path));
        }

        List<Issue> errors() {
            return issues("ERROR");
        }

        List<Issue> warnings() {
            return issues("WARNING");
        }

        private List<Issue> issues(String severity) {
            List<Issue> result = new ArrayList<Issue>();
            for (Issue issue : issues) {
                if (severity.equals(issue.severity)) {
                    result.add(issue);
                }
            }
            return result;
        }

        String toJson() {
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("ok", Boolean.valueOf(ok()));
            payload.put("command", "validate");
            payload.put("repository", repoRoot.toString());
            payload.put("checkDirty", Boolean.valueOf(checkDirty));
            payload.put("gitRepository", Boolean.valueOf(gitRepository));
            payload.put("buildTool", buildTool);
            payload.put("versionFile", versionFile);
            payload.put("currentRevision", currentRevision);
            payload.put("pendingChangesets", Integer.valueOf(pendingChangesets));
            payload.put("releaseVersion", releaseVersion);
            payload.put("nextSnapshotVersion", nextSnapshotVersion);
            payload.put("tagStrategy", tagStrategy);
            payload.put("affectedPackages", new ArrayList<String>(affectedPackages));
            payload.put("plannedTags", new ArrayList<String>(plannedTags));
            List<Map<String, Object>> renderedIssues = new ArrayList<Map<String, Object>>();
            for (Issue issue : issues) {
                renderedIssues.add(issue.toPayload());
            }
            payload.put("issues", renderedIssues);
            return ReleaseJsonUtils.toPrettyJson(payload);
        }
    }

    static final class Issue {
        final String severity;
        final String code;
        final String message;
        final String path;

        Issue(String severity, String code, String message, String path) {
            this.severity = severity;
            this.code = code;
            this.message = message;
            this.path = path;
        }

        Map<String, Object> toPayload() {
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("severity", severity);
            payload.put("code", code);
            payload.put("message", message);
            payload.put("path", path);
            return payload;
        }
    }
}
