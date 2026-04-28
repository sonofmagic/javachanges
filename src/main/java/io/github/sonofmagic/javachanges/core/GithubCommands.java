package io.github.sonofmagic.javachanges.core;

import io.github.sonofmagic.javachanges.core.github.GithubReleasePlanRequest;
import io.github.sonofmagic.javachanges.core.github.GithubReleasePublishRequest;
import io.github.sonofmagic.javachanges.core.github.GithubReleaseSupport;
import io.github.sonofmagic.javachanges.core.github.GithubTagRequest;
import io.github.sonofmagic.javachanges.core.config.ChangesetConfigSupport;
import io.github.sonofmagic.javachanges.core.plan.RepoFiles;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Command(name = "github-release-plan", mixinStandardHelpOptions = true,
    description = "Create or update a GitHub release-plan pull request.")
final class GithubReleasePlanCommand extends AbstractCliCommand {
    @Option(names = "--github-repo", description = "GitHub owner/repo.")
    private String githubRepo;

    @Option(names = "--target-branch", description = "Default branch to open the PR against.")
    private String targetBranch;

    @Option(names = "--release-branch", description = "Release plan branch name.")
    private String releaseBranch;

    @Option(names = "--execute", arity = "0..1", fallbackValue = "true", defaultValue = "false",
        description = "Call GitHub through gh instead of a dry run.")
    private boolean execute;

    @Option(names = "--write-plan-files", arity = "0..1", fallbackValue = "true", defaultValue = "true",
        description = "Write .changesets/release-plan.json and .changesets/release-plan.md into the release branch.")
    private boolean writePlanFiles;

    @Option(names = "--format", description = "Output format: text or json.")
    private String format;

    @Override
    public Integer call() throws Exception {
        Map<String, String> options = options(
            option("github-repo", githubRepo),
            option("target-branch", targetBranch),
            option("release-branch", releaseBranch),
            flag("execute", execute),
            flag("write-plan-files", writePlanFiles),
            option("format", format)
        );
        GithubReleasePlanRequest request = GithubReleasePlanRequest.fromOptions(options);
        return runAutomationCommand("github-release-plan", request.format,
            () -> githubReleaseSupport().planPullRequest(request));
    }
}

@Command(name = "github-tag-from-plan", mixinStandardHelpOptions = true,
    description = "Tag and push a GitHub release from the generated release plan manifest.")
final class GithubTagFromPlanCommand extends AbstractCliCommand {
    @Option(names = "--current-sha", description = "Commit SHA to tag. Defaults to HEAD or GITHUB_SHA.")
    private String currentSha;

    @Option(names = "--execute", arity = "0..1", fallbackValue = "true", defaultValue = "false",
        description = "Push the release tag instead of a dry run.")
    private boolean execute;

    @Option(names = "--fresh", arity = "0..1", fallbackValue = "true", defaultValue = "false",
        description = "Derive release metadata from the current repository state instead of .changesets/release-plan.json.")
    private boolean fresh;

    @Option(names = "--format", description = "Output format: text or json.")
    private String format;

    @Override
    public Integer call() throws Exception {
        Map<String, String> options = options(
            option("current-sha", currentSha),
            flag("execute", execute),
            flag("fresh", fresh),
            option("format", format)
        );
        GithubTagRequest request = GithubTagRequest.fromOptions(options);
        return runAutomationCommand("github-tag-from-plan", request.format,
            () -> githubReleaseSupport().tagFromReleasePlan(request));
    }
}

@Command(name = "github-release-from-plan", mixinStandardHelpOptions = true,
    description = "Generate release notes and optionally create or update a GitHub Release from the release plan manifest.")
final class GithubReleaseFromPlanCommand extends AbstractCliCommand {
    @Option(names = "--release-notes-file",
        description = "Release notes output path. Relative paths resolve from the repository root.")
    private String releaseNotesFile;

    @Option(names = "--github-output-file",
        description = "Optional GitHub Actions output file. Defaults to GITHUB_OUTPUT when available.")
    private String githubOutputFile;

    @Option(names = "--execute", arity = "0..1", fallbackValue = "true", defaultValue = "false",
        description = "Create or update the GitHub Release through gh instead of a dry run.")
    private boolean execute;

    @Option(names = "--fresh", arity = "0..1", fallbackValue = "true", defaultValue = "false",
        description = "Derive release metadata from the current repository state instead of .changesets/release-plan.json.")
    private boolean fresh;

    @Option(names = "--format", description = "Output format: text or json.")
    private String format;

    @Override
    public Integer call() throws Exception {
        Map<String, String> options = options(
            option("release-notes-file", releaseNotesFile),
            option("github-output-file", githubOutputFile),
            flag("execute", execute),
            flag("fresh", fresh),
            option("format", format)
        );
        GithubReleasePublishRequest request = GithubReleasePublishRequest.fromOptions(options);
        return runAutomationCommand("github-release-from-plan", request.format,
            () -> githubReleaseSupport().syncReleaseFromPlan(request));
    }
}

@Command(name = "init-github-actions", mixinStandardHelpOptions = true,
    description = "Write a minimal GitHub Actions workflow that runs javachanges release-plan, tag, publish, and GitHub Release jobs.")
final class InitGithubActionsCommand extends AbstractCliCommand {
    @Option(names = "--output", description = "Target GitHub Actions workflow path.",
        defaultValue = ".github/workflows/javachanges-release.yml")
    private String output;

    @Option(names = "--force", arity = "0..1", fallbackValue = "true", defaultValue = "false",
        description = "Overwrite the target file when it already exists.")
    private boolean force;

    @Option(names = "--javachanges-version",
        description = "Released javachanges version used by the generated workflow.")
    private String javachangesVersion;

    @Option(names = "--build-tool", description = "Build tool template: auto, maven, or gradle.", defaultValue = "auto")
    private String buildTool;

    @Override
    public Integer call() throws Exception {
        Path repoRoot = repoRoot();
        Path target = repoRoot.resolve(output).normalize();
        if (Files.exists(target) && !force) {
            throw new IllegalStateException("Target file already exists. Pass --force true to overwrite: " + target);
        }
        ChangesetConfigSupport.ChangesetConfig config = RepoFiles.readChangesetConfig(repoRoot);
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(target, renderTemplate(repoRoot, config, effectiveVersion(), buildTool).getBytes(StandardCharsets.UTF_8));
        out().println("Generated GitHub Actions workflow: " + repoRoot.relativize(target));
        return success();
    }

    private String effectiveVersion() {
        return JavaChangesVersion.releasedVersion(javachangesVersion);
    }

    private String renderTemplate(Path repoRoot, ChangesetConfigSupport.ChangesetConfig config, String version,
                                  String buildTool) {
        String resolvedBuildTool = resolveBuildTool(repoRoot, buildTool);
        if ("gradle".equals(resolvedBuildTool)) {
            return renderGradleTemplate(config, version);
        }
        return renderMavenTemplate(config, version);
    }

    private String resolveBuildTool(Path repoRoot, String buildTool) {
        String explicit = ReleaseTextUtils.trimToNull(buildTool);
        if (explicit == null || "auto".equalsIgnoreCase(explicit)) {
            BuildModelSupport.BuildModel model = BuildModelSupport.detect(repoRoot);
            return model != null && model.type == BuildModelSupport.BuildType.GRADLE ? "gradle" : "maven";
        }
        String normalized = explicit.toLowerCase(java.util.Locale.ROOT);
        if (!"maven".equals(normalized) && !"gradle".equals(normalized)) {
            throw new IllegalArgumentException("Unsupported build tool: " + buildTool);
        }
        return normalized;
    }

    private String renderMavenTemplate(ChangesetConfigSupport.ChangesetConfig config, String version) {
        String baseBranch = config.baseBranch();
        String releaseBranch = config.releaseBranch();
        return ""
            + "name: javachanges Release\n"
            + "\n"
            + "on:\n"
            + "  push:\n"
            + "    branches:\n"
            + "      - " + baseBranch + "\n"
            + "  pull_request:\n"
            + "    types:\n"
            + "      - closed\n"
            + "  workflow_dispatch:\n"
            + "\n"
            + "permissions:\n"
            + "  contents: write\n"
            + "  pull-requests: write\n"
            + "\n"
            + "env:\n"
            + "  JAVACHANGES_VERSION: \"" + version + "\"\n"
            + "\n"
            + "jobs:\n"
            + "  release-plan:\n"
            + "    if: github.event_name == 'workflow_dispatch' || (github.event_name == 'push' && github.actor != 'github-actions[bot]')\n"
            + "    runs-on: ubuntu-latest\n"
            + "    steps:\n"
            + "      - uses: actions/checkout@v5\n"
            + "        with:\n"
            + "          fetch-depth: 0\n"
            + "      - uses: actions/setup-java@v5\n"
            + "        with:\n"
            + "          distribution: corretto\n"
            + "          java-version: '8'\n"
            + "          cache: maven\n"
            + "          cache-dependency-path: pom.xml\n"
            + "      - name: Create or update release pull request\n"
            + "        env:\n"
            + "          GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}\n"
            + "        run: >\n"
            + "          mvn -B io.github.sonofmagic:javachanges:${JAVACHANGES_VERSION}:run\n"
            + "          -Djavachanges.args=\"github-release-plan --directory $GITHUB_WORKSPACE --write-plan-files false --execute true\"\n"
            + "\n"
            + "  publish:\n"
            + "    if: github.event_name == 'pull_request' && github.event.pull_request.merged == true && github.event.pull_request.base.ref == '" + baseBranch + "' && github.event.pull_request.head.ref == '" + releaseBranch + "'\n"
            + "    runs-on: ubuntu-latest\n"
            + "    steps:\n"
            + "      - uses: actions/checkout@v5\n"
            + "        with:\n"
            + "          ref: ${{ github.event.pull_request.merge_commit_sha }}\n"
            + "          fetch-depth: 0\n"
            + "      - uses: actions/setup-java@v5\n"
            + "        with:\n"
            + "          distribution: corretto\n"
            + "          java-version: '8'\n"
            + "          cache: maven\n"
            + "          cache-dependency-path: pom.xml\n"
            + "      - name: Create and push release tag\n"
            + "        env:\n"
            + "          GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}\n"
            + "        run: >\n"
            + "          mvn -B io.github.sonofmagic:javachanges:${JAVACHANGES_VERSION}:run\n"
            + "          -Djavachanges.args=\"github-tag-from-plan --directory $GITHUB_WORKSPACE --fresh true --current-sha $(git rev-parse HEAD) --execute true\"\n"
            + "      - name: Read release version\n"
            + "        id: release_meta\n"
            + "        run: |\n"
            + "          release_version=\"$(mvn -B -q io.github.sonofmagic:javachanges:${JAVACHANGES_VERSION}:run -Djavachanges.args=\\\"manifest-field --directory $GITHUB_WORKSPACE --field releaseVersion --fresh true\\\")\"\n"
            + "          echo \"release_version=$release_version\" >> \"$GITHUB_OUTPUT\"\n"
            + "      - name: Publish Maven artifacts\n"
            + "        run: >\n"
            + "          mvn -B io.github.sonofmagic:javachanges:${JAVACHANGES_VERSION}:run\n"
            + "          -Djavachanges.args=\"publish --directory $GITHUB_WORKSPACE --tag v${{ steps.release_meta.outputs.release_version }} --execute true\"\n"
            + "      - name: Create GitHub release\n"
            + "        env:\n"
            + "          GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}\n"
            + "        run: >\n"
            + "          mvn -B io.github.sonofmagic:javachanges:${JAVACHANGES_VERSION}:run\n"
            + "          -Djavachanges.args=\"github-release-from-plan --directory $GITHUB_WORKSPACE --fresh true --release-notes-file target/release-notes.md --execute true\"\n";
    }

    private String renderGradleTemplate(ChangesetConfigSupport.ChangesetConfig config, String version) {
        String baseBranch = config.baseBranch();
        String releaseBranch = config.releaseBranch();
        return ""
            + "name: javachanges Release\n"
            + "\n"
            + "on:\n"
            + "  push:\n"
            + "    branches:\n"
            + "      - " + baseBranch + "\n"
            + "  pull_request:\n"
            + "    types:\n"
            + "      - closed\n"
            + "  workflow_dispatch:\n"
            + "\n"
            + "permissions:\n"
            + "  contents: write\n"
            + "  pull-requests: write\n"
            + "\n"
            + "env:\n"
            + "  JAVACHANGES_VERSION: \"" + version + "\"\n"
            + "  JAVACHANGES_JAR: .javachanges/javachanges-${{ env.JAVACHANGES_VERSION }}.jar\n"
            + "\n"
            + "jobs:\n"
            + "  release-plan:\n"
            + "    if: github.event_name == 'workflow_dispatch' || (github.event_name == 'push' && github.actor != 'github-actions[bot]')\n"
            + "    runs-on: ubuntu-latest\n"
            + "    steps:\n"
            + "      - uses: actions/checkout@v5\n"
            + "        with:\n"
            + "          fetch-depth: 0\n"
            + "      - uses: actions/setup-java@v5\n"
            + "        with:\n"
            + "          distribution: temurin\n"
            + "          java-version: '17'\n"
            + "          cache: gradle\n"
            + "      - name: Build\n"
            + "        run: ./gradlew --no-daemon build\n"
            + "      - name: Download javachanges\n"
            + "        run: |\n"
            + "          mkdir -p .javachanges\n"
            + "          curl -fsSL \"https://repo1.maven.org/maven2/io/github/sonofmagic/javachanges/${JAVACHANGES_VERSION}/javachanges-${JAVACHANGES_VERSION}.jar\" -o \"$JAVACHANGES_JAR\"\n"
            + "      - name: Create or update release pull request\n"
            + "        env:\n"
            + "          GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}\n"
            + "        run: java -jar \"$JAVACHANGES_JAR\" github-release-plan --directory \"$GITHUB_WORKSPACE\" --write-plan-files false --execute true\n"
            + "\n"
            + "  publish:\n"
            + "    if: github.event_name == 'pull_request' && github.event.pull_request.merged == true && github.event.pull_request.base.ref == '" + baseBranch + "' && github.event.pull_request.head.ref == '" + releaseBranch + "'\n"
            + "    runs-on: ubuntu-latest\n"
            + "    steps:\n"
            + "      - uses: actions/checkout@v5\n"
            + "        with:\n"
            + "          ref: ${{ github.event.pull_request.merge_commit_sha }}\n"
            + "          fetch-depth: 0\n"
            + "      - uses: actions/setup-java@v5\n"
            + "        with:\n"
            + "          distribution: temurin\n"
            + "          java-version: '17'\n"
            + "          cache: gradle\n"
            + "      - name: Download javachanges\n"
            + "        run: |\n"
            + "          mkdir -p .javachanges\n"
            + "          curl -fsSL \"https://repo1.maven.org/maven2/io/github/sonofmagic/javachanges/${JAVACHANGES_VERSION}/javachanges-${JAVACHANGES_VERSION}.jar\" -o \"$JAVACHANGES_JAR\"\n"
            + "      - name: Create and push release tag\n"
            + "        env:\n"
            + "          GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}\n"
            + "        run: java -jar \"$JAVACHANGES_JAR\" github-tag-from-plan --directory \"$GITHUB_WORKSPACE\" --fresh true --current-sha \"$(git rev-parse HEAD)\" --execute true\n"
            + "      - name: Read release version\n"
            + "        id: release_meta\n"
            + "        run: |\n"
            + "          release_version=\"$(java -jar \"$JAVACHANGES_JAR\" manifest-field --directory \"$GITHUB_WORKSPACE\" --field releaseVersion --fresh true)\"\n"
            + "          echo \"release_version=$release_version\" >> \"$GITHUB_OUTPUT\"\n"
            + "      - name: Publish Gradle artifacts\n"
            + "        run: java -jar \"$JAVACHANGES_JAR\" gradle-publish --directory \"$GITHUB_WORKSPACE\" --tag \"v${{ steps.release_meta.outputs.release_version }}\" --execute true\n"
            + "      - name: Create GitHub release\n"
            + "        env:\n"
            + "          GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}\n"
            + "        run: java -jar \"$JAVACHANGES_JAR\" github-release-from-plan --directory \"$GITHUB_WORKSPACE\" --fresh true --release-notes-file target/release-notes.md --execute true\n";
    }
}
