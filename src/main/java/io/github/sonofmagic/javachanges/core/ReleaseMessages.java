package io.github.sonofmagic.javachanges.core;

import java.nio.file.Path;
import java.util.List;

public final class ReleaseMessages {
    private ReleaseMessages() {
    }

    public static ReleaseLanguage language() {
        return ReleaseLanguageContext.get();
    }

    public static boolean zh() {
        return language().isChinese();
    }

    private static String message(String key, Object... args) {
        return I18n.message(key, args);
    }

    public static String initialized(Path repoRoot) {
        return message("initialized", repoRoot);
    }

    public static String pathAction(String action) {
        if (!zh()) {
            return action;
        }
        if ("Created".equals(action)) {
            return message("pathAction.created");
        }
        if ("Replaced".equals(action)) {
            return message("pathAction.replaced");
        }
        if ("Kept".equals(action)) {
            return message("pathAction.kept");
        }
        if ("Skipped".equals(action)) {
            return message("pathAction.skipped");
        }
        return action;
    }

    public static String nextSteps() {
        return message("nextSteps");
    }

    public static String describeChangePlaceholder() {
        return message("describeChangePlaceholder");
    }

    public static String changesetSummaryPrompt() {
        return message("changesetSummaryPrompt");
    }

    public static String changesetReleaseLevelPrompt() {
        return message("changesetReleaseLevelPrompt");
    }

    public static String changesetBodyPrompt() {
        return message("changesetBodyPrompt");
    }

    public static String changesetReadme() {
        return I18n.template("changeset-readme.md");
    }

    public static String releaseLevelHeading(ReleaseLevel level) {
        if (level == ReleaseLevel.MAJOR) {
            return message("releaseLevelHeading.major");
        }
        if (level == ReleaseLevel.MINOR) {
            return message("releaseLevelHeading.minor");
        }
        return message("releaseLevelHeading.patch");
    }

    public static String changelogPackagesLabel() {
        return message("changelogPackagesLabel");
    }

    public static String noUserFacingChanges() {
        return message("noUserFacingChanges");
    }

    public static String releasePlanTitle() {
        return message("releasePlanTitle");
    }

    public static String releasePlanIntro() {
        return message("releasePlanIntro");
    }

    public static String fieldHeader() {
        return message("fieldHeader");
    }

    public static String valueHeader() {
        return message("valueHeader");
    }

    public static String releaseTypeField() {
        return message("releaseTypeField");
    }

    public static String affectedPackagesField() {
        return message("affectedPackagesField");
    }

    public static String releaseVersionField() {
        return message("releaseVersionField");
    }

    public static String tagStrategyField() {
        return message("tagStrategyField");
    }

    public static String plannedTagsField() {
        return message("plannedTagsField");
    }

    public static String nextSnapshotField() {
        return message("nextSnapshotField");
    }

    public static String includedChangesetsTitle() {
        return message("includedChangesetsTitle");
    }

    public static String releaseLabel() {
        return message("releaseLabel");
    }

    public static String packagesLabel() {
        return message("packagesLabel");
    }

    public static String typeLabel() {
        return message("typeLabel");
    }

    public static String notesLabel() {
        return message("notesLabel");
    }

    public static String whatHappensNextTitle() {
        return message("whatHappensNextTitle");
    }

    public static String mergeTriggersTagPush() {
        return message("mergeTriggersTagPush");
    }

    public static String workflowsReuseMetadata() {
        return message("workflowsReuseMetadata");
    }

    public static String repository() {
        return message("repository");
    }

    public static String currentRevision() {
        return message("currentRevision");
    }

    public static String latestWholeRepoTag() {
        return message("latestWholeRepoTag");
    }

    public static String none() {
        return message("none");
    }

    public static String pendingChangesets() {
        return message("pendingChangesets");
    }

    public static String releasePlan() {
        return message("releasePlan");
    }

    public static String changesets() {
        return message("changesets");
    }

    public static String releaseType() {
        return message("releaseType");
    }

    public static String affectedPackages() {
        return message("affectedPackages");
    }

    public static String releaseVersion() {
        return message("releaseVersion");
    }

    public static String nextSnapshot() {
        return message("nextSnapshot");
    }

    public static String createdChangeset(Path relativePath) {
        return message("createdChangeset", relativePath);
    }

    public static String releaseLevel() {
        return message("releaseLevel");
    }

    public static String noPendingChangesets() {
        return message("noPendingChangesets");
    }

    public static String noPendingChangesetsToApply() {
        return message("noPendingChangesetsToApply");
    }

    public static String appliedReleasePlan(String version) {
        return message("appliedReleasePlan", version);
    }

    public static String createdOne() {
        return message("createdOne");
    }

    public static String reviewPlan() {
        return message("reviewPlan");
    }

    public static String thenReviewPlan() {
        return message("thenReviewPlan");
    }

    public static String applyLocally() {
        return message("applyLocally");
    }

    public static String openGithubPr() {
        return message("openGithubPr");
    }

    public static String openGitlabMr() {
        return message("openGitlabMr");
    }

    public static String nextStepFor(Path repoRoot) {
        return message("nextStepFor", repoRoot);
    }

    public static String plannedRelease() {
        return message("plannedRelease");
    }

    public static String unsupportedLanguage(String value) {
        return message("unsupportedLanguage", value);
    }

    public static String unsupportedOutputFormat(String value) {
        return message("unsupportedOutputFormat", value);
    }

    public static String unsupportedPlatform(String value) {
        return message("unsupportedPlatform", value);
    }

    public static String unsupportedReleaseLevel(String value) {
        return message("unsupportedReleaseLevel", value);
    }

    public static String unknownArgument(String value) {
        return message("unknownArgument", value);
    }

    public static String missingRequiredOption(String name) {
        return message("missingRequiredOption", name);
    }

    public static String missingEnv(String name) {
        return message("missingEnv", name);
    }

    public static String unknownModule(Path repoRoot, String module, List<String> knownModules) {
        return message("unknownModule", module, knownModules, repoRoot);
    }

    public static String unsupportedReleaseTag(String tag) {
        return message("unsupportedReleaseTag", tag);
    }

    public static String atLeastOneModuleRequired() {
        return message("atLeastOneModuleRequired");
    }

    public static String unsupportedChangeType(String type) {
        return message("unsupportedChangeType", type);
    }

    public static String invalidChangesetFrontmatter(Path path) {
        return message("invalidChangesetFrontmatter", path);
    }

    public static String invalidChangesetLine(Path path, String line) {
        return message("invalidChangesetLine", path, line);
    }

    public static String missingPackageReleaseEntries(Path path) {
        return message("missingPackageReleaseEntries", path);
    }

    public static String missingKeyIn(String key, Path path) {
        return message("missingKeyIn", key, path);
    }

    public static String missingReleasePlanManifest(Path manifest) {
        return message("core.missingReleasePlanManifest", manifest);
    }

    public static String missingFieldIn(String field, Path manifest) {
        return message("core.missingFieldIn", field, manifest);
    }

    public static String cannotFindRepoRoot(Path current) {
        return message("core.cannotFindRepoRoot", current);
    }

    public static String unsupportedSnapshotVersionMode(String value) {
        return message("core.unsupportedSnapshotVersionMode", value);
    }

    public static String unsupportedTagStrategy(String value) {
        return message("core.unsupportedTagStrategy", value);
    }

    public static String unsupportedVersion(String value) {
        return message("core.unsupportedVersion", value);
    }

    public static String missingVersionConfig() {
        return message("core.missingVersionConfig");
    }

    public static String notSnapshot(String version) {
        return message("core.notSnapshot", version);
    }

    public static String emptySnapshotBuildStamp() {
        return message("core.emptySnapshotBuildStamp");
    }

    public static String invalidSnapshotBuildStamp(String buildStamp) {
        return message("core.invalidSnapshotBuildStamp", buildStamp);
    }

    public static String releaseTagVersionMismatch(String tag, String version, String releaseVersion) {
        return message("core.releaseTagVersionMismatch", tag, version, releaseVersion);
    }

    public static String missingRepositoryCredentials(String mode, String specificUser, String specificPassword) {
        return message("core.missingRepositoryCredentials", mode, specificUser, specificPassword);
    }

    public static String outputPathMustStayInsideRepository(String optionName, String value) {
        return message("core.outputPathMustStayInsideRepository", optionName, value);
    }

    public static String cannotFindPomProperties(Path pomPath) {
        return message("build.cannotFindPomProperties", pomPath);
    }

    public static String cannotFindPomRevision(Path pomPath) {
        return message("build.cannotFindPomRevision", pomPath);
    }

    public static String failedToConfigureXmlParser(Path path) {
        return message("build.failedToConfigureXmlParser", path);
    }

    public static String failedToParsePom(Path path) {
        return message("build.failedToParsePom", path);
    }

    public static String failedToWritePom(Path path) {
        return message("build.failedToWritePom", path);
    }

    public static String failedToDetectMavenModules(Path repoRoot) {
        return message("build.failedToDetectMavenModules", repoRoot);
    }

    public static String failedToDetectGradleModules(Path repoRoot) {
        return message("build.failedToDetectGradleModules", repoRoot);
    }

    public static String cannotFindSupportedBuildModel(Path repoRoot) {
        return message("build.cannotFindSupportedBuildModel", repoRoot);
    }

    public static String cannotFindGradleVersion(Path path) {
        return message("build.cannotFindGradleVersion", path);
    }

    public static String cannotParseGradleVersion(Path path) {
        return message("build.cannotParseGradleVersion", path);
    }

    public static String failedToParseJson() {
        return message("core.failedToParseJson");
    }

    public static String failedToWriteJson() {
        return message("core.failedToWriteJson");
    }

    public static String failedToReadChangesetConfig() {
        return message("core.failedToReadChangesetConfig");
    }

    public static String gitCommandFailed() {
        return message("platform.gitCommandFailed");
    }

    public static String gitCommandFailed(String command) {
        return message("platform.gitCommandFailedWithCommand", command);
    }

    public static String failedToCaptureProcessOutput() {
        return message("platform.failedToCaptureProcessOutput");
    }

    public static String gitTagFailed(String error) {
        return message("platform.gitTagFailed", error);
    }

    public static String gitReturnedEmptyOutput(Object args) {
        return message("platform.gitReturnedEmptyOutput", args);
    }

    public static String unexpectedGitLsRemoteOutput(String stdout) {
        return message("platform.unexpectedGitLsRemoteOutput", stdout);
    }

    public static String gitRevParseHeadFailed() {
        return message("platform.gitRevParseHeadFailed");
    }

    public static String currentHeadShaEmpty() {
        return message("platform.currentHeadShaEmpty");
    }

    public static String ghPrListFailed() {
        return message("platform.ghPrListFailed");
    }

    public static String ghCommandFailed(Object args) {
        return message("platform.ghCommandFailed", args);
    }

    public static String targetFileExists(Path target) {
        return message("platform.targetFileExists", target);
    }

    public static String generatedGithubActionsWorkflow(Path relativePath) {
        return message("platform.generatedGithubActionsWorkflow", relativePath);
    }

    public static String generatedGitlabCiTemplate(Path relativePath) {
        return message("platform.generatedGitlabCiTemplate", relativePath);
    }

    public static String unsupportedBuildTool(String buildTool) {
        return message("platform.unsupportedBuildTool", buildTool);
    }

    public static String javachangesFailed(int exitCode) {
        return message("platform.javachangesFailed", exitCode);
    }

    public static String unterminatedJavachangesArgsQuote() {
        return message("platform.unterminatedJavachangesArgsQuote");
    }

    public static String attemptsMustBePositive() {
        return message("platform.attemptsMustBePositive");
    }

    public static String retryDelayMustBeNonNegative() {
        return message("platform.retryDelayMustBeNonNegative");
    }

    public static String signingKeyNotVisible(String fingerprint, String primaryKeyserver, String secondaryKeyserver) {
        return message("platform.signingKeyNotVisible", fingerprint, primaryKeyserver, secondaryKeyserver);
    }

    public static String failedToInspectGpgSecretKeys(String detail) {
        return message("platform.failedToInspectGpgSecretKeys", detail);
    }

    public static String noImportedSecretKeyFingerprint() {
        return message("platform.noImportedSecretKeyFingerprint");
    }

    public static String missingGithubRepo() {
        return message("platform.missingGithubRepo");
    }

    public static String missingGitlabProjectId() {
        return message("platform.missingGitlabProjectId");
    }

    public static String missingGitlabTag() {
        return message("platform.missingGitlabTag");
    }

    public static String connectTimeoutMustBeNonNegative() {
        return message("platform.connectTimeoutMustBeNonNegative");
    }

    public static String readTimeoutMustBeNonNegative() {
        return message("platform.readTimeoutMustBeNonNegative");
    }

    public static String missingGitlabResponseField(String field, String json) {
        return message("platform.missingGitlabResponseField", field, json);
    }

    public static String gitlabApiNotFound(String method, String path) {
        return message("platform.gitlabApiNotFound", method, path);
    }

    public static String gitlabApiFailed(String method, String path, String detail) {
        return message("platform.gitlabApiFailed", method, path, detail);
    }

    public static String failedToEncodeUrlComponent() {
        return message("platform.failedToEncodeUrlComponent");
    }

    public static String failedToEncodeGitlabProjectPath() {
        return message("platform.failedToEncodeGitlabProjectPath");
    }

    public static String createGitlabProtectedVariable(String name) {
        return message("release.createGitlabProtectedVariable", name);
    }

    public static String markGitlabVariableProtected(String name) {
        return message("release.markGitlabVariableProtected", name);
    }

    public static String protectGitlabBranchForProtectedVariables(String branch) {
        return message("release.protectGitlabBranchForProtectedVariables", branch);
    }

    public static String removeProtectedVariableFlagWithLowerSecurity() {
        return message("release.removeProtectedVariableFlagWithLowerSecurity");
    }

    public static String protectGitlabBranchForSnapshot(String branch) {
        return message("release.protectGitlabBranchForSnapshot", branch);
    }

    public static String freshMetadataCannotInferPerModuleTargets() {
        return message("release.freshMetadataCannotInferPerModuleTargets");
    }

    public static String multipleTagsRequireExplicitTagCommand() {
        return message("release.multipleTagsRequireExplicitTagCommand");
    }

    public static String missingReleaseManifestField(String field) {
        return message("release.missingReleaseManifestField", field);
    }

    public static String missingFieldInSource(String field, String source) {
        return message("release.missingFieldInSource", field, source);
    }

    public static String releaseNotesSection(String section) {
        if ("Breaking Changes".equals(section)) {
            return message("release.section.breakingChanges");
        }
        if ("Features".equals(section)) {
            return message("release.section.features");
        }
        if ("Fixes".equals(section)) {
            return message("release.section.fixes");
        }
        if ("Performance".equals(section)) {
            return message("release.section.performance");
        }
        if ("Refactoring".equals(section)) {
            return message("release.section.refactoring");
        }
        if ("Build".equals(section)) {
            return message("release.section.build");
        }
        if ("Documentation".equals(section)) {
            return message("release.section.documentation");
        }
        if ("Tests".equals(section)) {
            return message("release.section.tests");
        }
        if ("CI".equals(section)) {
            return message("release.section.ci");
        }
        if ("Chores".equals(section)) {
            return message("release.section.chores");
        }
        return message("release.section.other");
    }

    public static String[] githubAuthHelpLines() {
        return I18n.templateLines("github-auth-help.txt");
    }

    public static String[] gitlabAuthHelpLines() {
        return I18n.templateLines("gitlab-auth-help.txt");
    }

    public static String usingEnvFile(String envPath) {
        return message("env.usingEnvFile", envPath);
    }

    public static String sensitiveValuesMaskedByDefault() {
        return message("env.sensitiveValuesMaskedByDefault");
    }

    public static String dryRunOnlyPrintsCommands() {
        return message("env.dryRunOnlyPrintsCommands");
    }

    public static String repoRequiredInExecuteMode() {
        return message("env.repoRequiredInExecuteMode");
    }

    public static String githubCliCommandsHeading() {
        return message("env.githubCliCommandsHeading");
    }

    public static String gitlabCliCommandsHeading() {
        return message("env.gitlabCliCommandsHeading");
    }

    public static String runningCommand(String displayCommand) {
        return message("env.runningCommand", displayCommand);
    }

    public static String commandFailed(String displayCommand) {
        return message("env.commandFailed", displayCommand);
    }

    public static String templateFileNotFound(String path) {
        return message("env.templateFileNotFound", path);
    }

    public static String targetFileCannotBeExample(String path) {
        return message("env.targetFileCannotBeExample", path);
    }

    public static String targetFileKept(String path) {
        return message("env.targetFileKept", path);
    }

    public static String recreateEnvFileCommand(String path) {
        return message("env.recreateEnvFileCommand", path);
    }

    public static String generatedLocalEnvFile(String path) {
        return message("env.generatedLocalEnvFile", path);
    }

    public static String editEnvFileNextStep() {
        return message("env.editEnvFileNextStep");
    }

    public static String platformVariableAuditFailed() {
        return message("env.platformVariableAuditFailed");
    }

    public static String platformVariableAuditPassed() {
        return message("env.platformVariableAuditPassed");
    }

    public static String githubVariablesAudit() {
        return message("env.githubVariablesAudit");
    }

    public static String githubSecretsAudit() {
        return message("env.githubSecretsAudit");
    }

    public static String gitlabVariablesAudit() {
        return message("env.gitlabVariablesAudit");
    }

    public static String heading(String label) {
        return "== " + label + " ==";
    }

    public static String missingRepositoryArgument(String name) {
        return message("env.missingRepositoryArgument", name);
    }

    public static String cliNotFound(String command) {
        return message("env.cliNotFound", command);
    }

    public static String authStatusFailed(String command) {
        return message("env.authStatusFailed", command);
    }

    public static String localRuntime() {
        return message("env.localRuntime");
    }

    public static String localEnvFile() {
        return message("env.localEnvFile");
    }

    public static String platformCli() {
        return message("env.platformCli");
    }

    public static String repositoryIdentifiers() {
        return message("env.repositoryIdentifiers");
    }

    public static String localEnvCheck() {
        return message("env.localEnvCheck");
    }

    public static String githubCliCheck() {
        return message("env.githubCliCheck");
    }

    public static String gitlabCliCheck() {
        return message("env.gitlabCliCheck");
    }

    public static String localReleaseEnvironmentPassed() {
        return message("env.localReleaseEnvironmentPassed");
    }

    public static String suggestedNextSteps() {
        return message("env.suggestedNextSteps");
    }

    public static String localReleaseEnvironmentNotReadyIntro() {
        return message("env.localReleaseEnvironmentNotReadyIntro");
    }

    public static String runEnvInitSuggestion() {
        return message("env.runEnvInitSuggestion");
    }

    public static String editLocalEnvSuggestion() {
        return message("env.editLocalEnvSuggestion");
    }

    public static String installJavaMavenSuggestion() {
        return message("env.installJavaMavenSuggestion");
    }

    public static String runAuthHelpSuggestion() {
        return message("env.runAuthHelpSuggestion");
    }

    public static String runPlatformDoctorSuggestion() {
        return message("env.runPlatformDoctorSuggestion");
    }

    public static String localReleaseEnvironmentNotReady() {
        return message("env.localReleaseEnvironmentNotReady");
    }

    public static String doctorPlatformFailed() {
        return message("env.doctorPlatformFailed");
    }

    public static String doctorCheckCompleted() {
        return message("env.doctorCheckCompleted");
    }

    public static String missingSnapshotOrTag() {
        return message("publish.missingSnapshotOrTag");
    }

    public static String snapshotAndTagMutuallyExclusive() {
        return message("publish.snapshotAndTagMutuallyExclusive");
    }

    public static String useForceToReplaceDefaultTemplate() {
        return message("publish.useForceToReplaceDefaultTemplate");
    }

    public static String useConfigToWriteDefaultTemplate() {
        return message("publish.useConfigToWriteDefaultTemplate");
    }

    public static String buildTool() {
        return message("publish.buildTool");
    }

    public static String versionFile() {
        return message("publish.versionFile");
    }

    public static String modules() {
        return message("publish.modules");
    }

    public static String moduleOk() {
        return message("publish.moduleOk");
    }

    public static String snapshotOk() {
        return message("publish.snapshotOk");
    }

    public static String releaseTagOk() {
        return message("publish.releaseTagOk");
    }

    public static String generatedMavenSettings(String output) {
        return message("publish.generatedMavenSettings", output);
    }

    public static String generatedReleaseNotes(String output) {
        return message("publish.generatedReleaseNotes", output);
    }

    public static String gpgPublicKeyOk(String fingerprint) {
        return message("publish.gpgPublicKeyOk", fingerprint);
    }

    public static String envFileNotFound(String path) {
        return message("publish.envFileNotFound", path);
    }

    public static String doNotUseExampleEnvFile(String path) {
        return message("publish.doNotUseExampleEnvFile", path);
    }

    public static String explicitModuleDoesNotMatchTagModule(String resolvedModule, String tagModule) {
        return message("publish.explicitModuleDoesNotMatchTagModule", resolvedModule, tagModule);
    }

    public static String noMavenCommandFound() {
        return message("publish.noMavenCommandFound", ReleaseProcessUtils.mavenWrapperPath());
    }

    public static String noGradleCommandFound() {
        return message("publish.noGradleCommandFound", ReleaseProcessUtils.gradleWrapperPath());
    }

    public static String dryRunOutputHeading() {
        return message("publish.dryRunOutputHeading");
    }

    public static String generatedMavenSettingsFile() {
        return message("publish.generatedMavenSettingsFile");
    }

    public static String mavenSettingsWillBeWritten() {
        return message("publish.mavenSettingsWillBeWritten");
    }

    public static String mavenCommandLabel(String command, String source) {
        return message("publish.mavenCommandLabel", command, source);
    }

    public static String localMavenRepository(Path path) {
        return message("publish.localMavenRepository", path);
    }

    public static String publishVersion(String version) {
        return message("publish.publishVersion", version);
    }

    public static String snapshotPublishVersion(String version) {
        return message("publish.snapshotPublishVersion", version);
    }

    public static String snapshotVersionMode(String mode) {
        return message("publish.snapshotVersionMode", mode);
    }

    public static String snapshotBuildStampApplied(boolean applied) {
        return message("publish.snapshotBuildStampApplied", applied);
    }

    public static String releaseTag(String tag) {
        return message("publish.releaseTag", tag);
    }

    public static String releaseVersionValue(String version) {
        return message("publish.releaseVersionValue", version);
    }

    public static String gradleDryRunOutputHeading() {
        return message("publish.gradleDryRunOutputHeading");
    }

    public static String gradleCommandLabel(String command, String source) {
        return message("publish.gradleCommandLabel", command, source);
    }

    public static String gradleTask(String task) {
        return message("publish.gradleTask", task);
    }

    public static String generatedReleaseNotesFile() {
        return message("publish.generatedReleaseNotesFile");
    }

    public static String releaseNotesWillBeWritten() {
        return message("publish.releaseNotesWillBeWritten");
    }

    public static String targetModule(String module) {
        return module == null ? message("publish.targetModuleAll") : message("publish.targetModule", module);
    }

    public static String commandToRun() {
        return message("publish.commandToRun");
    }

    public static String dryRunOnlyMavenPublish() {
        return message("publish.dryRunOnlyMavenPublish");
    }

    public static String dryRunOnlyGradlePublish() {
        return message("publish.dryRunOnlyGradlePublish");
    }

    public static String dryRunOnlyReason() {
        return message("publish.reasonDryRunOnly");
    }

    public static String publishCompletedReason() {
        return message("publish.reasonPublishCompleted");
    }

    public static String gradlePublishCompletedReason() {
        return message("publish.reasonGradlePublishCompleted");
    }

    public static String preflightChecksPassedReason() {
        return message("publish.reasonPreflightChecksPassed");
    }

    public static String runningMavenHeading() {
        return message("publish.runningMavenHeading");
    }

    public static String runningGradleHeading() {
        return message("publish.runningGradleHeading");
    }

    public static String mavenDeployFailed(int exitCode) {
        return message("publish.mavenDeployFailed", exitCode);
    }

    public static String gradlePublishFailed(int exitCode) {
        return message("publish.gradlePublishFailed", exitCode);
    }

    public static String dirtyWorktree() {
        return message("publish.dirtyWorktree");
    }

    public static String versionCheckHeading() {
        return message("publish.versionCheckHeading");
    }

    public static String currentRevisionValue(String value) {
        return message("publish.currentRevisionValue", value);
    }

    public static String publishModeCheckHeading() {
        return message("publish.publishModeCheckHeading");
    }

    public static String snapshotCheckPassed() {
        return message("publish.snapshotCheckPassed");
    }

    public static String plainSnapshotDescription() {
        return message("publish.plainSnapshotDescription");
    }

    public static String stampedSnapshotDescription() {
        return message("publish.stampedSnapshotDescription");
    }

    public static String repositoryVariableCheckHeading() {
        return message("publish.repositoryVariableCheckHeading");
    }

    public static String credentialCheckHeading() {
        return message("publish.credentialCheckHeading");
    }

    public static String mavenSettingsGenerationPassed() {
        return message("publish.mavenSettingsGenerationPassed");
    }

    public static String releaseNotesPreflightHeading() {
        return message("publish.releaseNotesPreflightHeading");
    }

    public static String releaseNotesGenerationPassed() {
        return message("publish.releaseNotesGenerationPassed");
    }

    public static String localTagMissingSkipReleaseNotes(String tag) {
        return message("publish.localTagMissingSkipReleaseNotes", tag);
    }

    public static String preflightChecksPassed() {
        return message("publish.preflightChecksPassed");
    }

    public static String taskMustBeNameWhenModuleSet(String task) {
        return message("publish.taskMustBeNameWhenModuleSet", task);
    }

    public static String unsupportedGradleTask(String task) {
        return message("publish.unsupportedGradleTask", task);
    }
}
