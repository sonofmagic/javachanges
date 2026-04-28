package io.github.sonofmagic.javachanges.core;

import io.github.sonofmagic.javachanges.core.config.ChangesetConfigSupport;
import io.github.sonofmagic.javachanges.core.gitlab.GitlabReleaseSupport;
import io.github.sonofmagic.javachanges.core.gitlab.GitlabReleasePlanRequest;
import io.github.sonofmagic.javachanges.core.gitlab.GitlabReleaseRequest;
import io.github.sonofmagic.javachanges.core.gitlab.GitlabTagRequest;
import io.github.sonofmagic.javachanges.core.plan.RepoFiles;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Command(name = "gitlab-release-plan", mixinStandardHelpOptions = true,
    description = "Create or update a GitLab release-plan merge request.")
final class GitlabReleasePlanCommand extends AbstractCliCommand {
    @Option(names = "--project-id", description = "GitLab project ID.")
    private String projectId;

    @Option(names = "--target-branch", description = "Default branch to open the MR against.")
    private String targetBranch;

    @Option(names = "--release-branch", description = "Release plan branch name.")
    private String releaseBranch;

    @Option(names = "--execute", arity = "0..1", fallbackValue = "true", defaultValue = "false",
        description = "Call the GitLab API instead of a dry run.")
    private boolean execute;

    @Option(names = "--format", description = "Output format: text or json.")
    private String format;

    @Override
    public Integer call() throws Exception {
        Map<String, String> options = options(
            option("project-id", projectId),
            option("target-branch", targetBranch),
            option("release-branch", releaseBranch),
            flag("execute", execute),
            option("format", format)
        );
        GitlabReleasePlanRequest request = GitlabReleasePlanRequest.fromOptions(options);
        return runAutomationCommand("gitlab-release-plan", request.format,
            () -> gitlabReleaseSupport().planMergeRequest(request));
    }
}

@Command(name = "gitlab-tag-from-plan", mixinStandardHelpOptions = true,
    description = "Tag a release from the generated release plan manifest.")
final class GitlabTagFromPlanCommand extends AbstractCliCommand {
    @Option(names = "--before-sha", description = "Previous commit SHA.")
    private String beforeSha;

    @Option(names = "--current-sha", description = "Current commit SHA.")
    private String currentSha;

    @Option(names = "--execute", arity = "0..1", fallbackValue = "true", defaultValue = "false",
        description = "Push tags instead of a dry run.")
    private boolean execute;

    @Option(names = "--format", description = "Output format: text or json.")
    private String format;

    @Override
    public Integer call() throws Exception {
        Map<String, String> options = options(
            option("before-sha", beforeSha),
            option("current-sha", currentSha),
            flag("execute", execute),
            option("format", format)
        );
        GitlabTagRequest request = GitlabTagRequest.fromOptions(options);
        return runAutomationCommand("gitlab-tag-from-plan", request.format,
            () -> gitlabReleaseSupport().tagFromReleasePlan(request));
    }
}

@Command(name = "gitlab-release", mixinStandardHelpOptions = true,
    description = "Generate release notes and optionally create or update a GitLab Release for the current CI tag.")
final class GitlabReleaseCommand extends AbstractCliCommand {
    @Option(names = "--tag", description = "Release tag such as v1.2.3 or artifactId/v1.2.3. Defaults to CI_COMMIT_TAG.")
    private String tag;

    @Option(names = "--project-id", description = "GitLab project ID. Defaults to CI_PROJECT_ID.")
    private String projectId;

    @Option(names = "--gitlab-host", description = "GitLab host. Defaults to CI_SERVER_HOST.")
    private String gitlabHost;

    @Option(names = "--release-notes-file",
        description = "Release notes output path. Relative paths resolve from the repository root.")
    private String releaseNotesFile;

    @Option(names = "--execute", arity = "0..1", fallbackValue = "true", defaultValue = "false",
        description = "Call the GitLab Releases API instead of a dry run.")
    private boolean execute;

    @Option(names = "--format", description = "Output format: text or json.")
    private String format;

    @Override
    public Integer call() throws Exception {
        Map<String, String> options = options(
            option("tag", tag),
            option("project-id", projectId),
            option("gitlab-host", gitlabHost),
            option("release-notes-file", releaseNotesFile),
            flag("execute", execute),
            option("format", format)
        );
        GitlabReleaseRequest request = GitlabReleaseRequest.fromOptions(options);
        return runAutomationCommand("gitlab-release", request.format,
            () -> gitlabReleaseSupport().syncRelease(request));
    }
}

@Command(name = "init-gitlab-ci", mixinStandardHelpOptions = true,
    description = "Write a minimal GitLab CI template that runs javachanges release-plan, tag, publish, and GitLab Release jobs.")
final class InitGitlabCiCommand extends AbstractCliCommand {
    @Option(names = "--output", description = "Target GitLab CI file path.", defaultValue = ".gitlab-ci.yml")
    private String output;

    @Option(names = "--force", arity = "0..1", fallbackValue = "true", defaultValue = "false",
        description = "Overwrite the target file when it already exists.")
    private boolean force;

    @Option(names = "--javachanges-version",
        description = "Released javachanges Maven plugin version used by the generated pipeline.")
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
        out().println("Generated GitLab CI template: " + repoRoot.relativize(target));
        return success();
    }

    private String effectiveVersion() {
        return JavaChangesVersion.releasedVersion(javachangesVersion);
    }

    private String renderTemplate(Path repoRoot, ChangesetConfigSupport.ChangesetConfig config, String version, String buildTool) {
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
        String snapshotBranch = config.snapshotBranch();
        return ""
            + "stages:\n"
            + "  - verify\n"
            + "  - release-plan\n"
            + "  - tag\n"
            + "  - publish\n"
            + "\n"
            + "default:\n"
            + "  image: maven:3.9.9-eclipse-temurin-8\n"
            + "  cache:\n"
            + "    key:\n"
            + "      files:\n"
            + "        - pom.xml\n"
            + "    paths:\n"
            + "      - .m2/repository\n"
            + "\n"
            + "variables:\n"
            + "  MAVEN_OPTS: \"-Dmaven.repo.local=.m2/repository\"\n"
            + "  JAVACHANGES_VERSION: \"" + version + "\"\n"
            + "\n"
            + "verify:\n"
            + "  stage: verify\n"
            + "  script:\n"
            + "    - mvn -B verify\n"
            + "    - >\n"
            + "      mvn -B io.github.sonofmagic:javachanges:${JAVACHANGES_VERSION}:run\n"
            + "      -Djavachanges.args=\"status --directory $CI_PROJECT_DIR\"\n"
            + "  rules:\n"
            + "    - if: $CI_PIPELINE_SOURCE == \"merge_request_event\"\n"
            + "    - if: $CI_COMMIT_BRANCH\n"
            + "\n"
            + "release_plan_mr:\n"
            + "  stage: release-plan\n"
            + "  script:\n"
            + "    - >\n"
            + "      mvn -B io.github.sonofmagic:javachanges:${JAVACHANGES_VERSION}:run\n"
            + "      -Djavachanges.args=\"gitlab-release-plan --directory $CI_PROJECT_DIR --execute true\"\n"
            + "  rules:\n"
            + "    - if: $CI_COMMIT_BRANCH == \"" + baseBranch + "\"\n"
            + "\n"
            + "release_tag:\n"
            + "  stage: tag\n"
            + "  script:\n"
            + "    - >\n"
            + "      mvn -B io.github.sonofmagic:javachanges:${JAVACHANGES_VERSION}:run\n"
            + "      -Djavachanges.args=\"gitlab-tag-from-plan --directory $CI_PROJECT_DIR --execute true\"\n"
            + "  rules:\n"
            + "    - if: $CI_COMMIT_BRANCH == \"" + baseBranch + "\"\n"
            + "\n"
            + "publish_snapshot:\n"
            + "  stage: publish\n"
            + "  script:\n"
            + "    - >\n"
            + "      mvn -B io.github.sonofmagic:javachanges:${JAVACHANGES_VERSION}:run\n"
            + "      -Djavachanges.args=\"publish --directory $CI_PROJECT_DIR --execute true\"\n"
            + "  rules:\n"
            + "    - if: $CI_COMMIT_BRANCH == \"" + snapshotBranch + "\"\n"
            + "\n"
            + "publish_release:\n"
            + "  stage: publish\n"
            + "  script:\n"
            + "    - >\n"
            + "      mvn -B io.github.sonofmagic:javachanges:${JAVACHANGES_VERSION}:run\n"
            + "      -Djavachanges.args=\"publish --directory $CI_PROJECT_DIR --execute true\"\n"
            + "    - >\n"
            + "      mvn -B io.github.sonofmagic:javachanges:${JAVACHANGES_VERSION}:run\n"
            + "      -Djavachanges.args=\"gitlab-release --directory $CI_PROJECT_DIR --execute true\"\n"
            + "  rules:\n"
            + "    - if: $CI_COMMIT_TAG\n";
    }

    private String renderGradleTemplate(ChangesetConfigSupport.ChangesetConfig config, String version) {
        String baseBranch = config.baseBranch();
        String snapshotBranch = config.snapshotBranch();
        return ""
            + "stages:\n"
            + "  - verify\n"
            + "  - release-plan\n"
            + "  - tag\n"
            + "  - publish\n"
            + "\n"
            + "default:\n"
            + "  image: eclipse-temurin:17\n"
            + "  cache:\n"
            + "    key:\n"
            + "      files:\n"
            + "        - gradle.properties\n"
            + "        - settings.gradle.kts\n"
            + "    paths:\n"
            + "      - .gradle/caches\n"
            + "      - .gradle/wrapper\n"
            + "      - .javachanges\n"
            + "\n"
            + "variables:\n"
            + "  GRADLE_USER_HOME: \"$CI_PROJECT_DIR/.gradle\"\n"
            + "  JAVACHANGES_VERSION: \"" + version + "\"\n"
            + "\n"
            + "before_script:\n"
            + "  - ./gradlew --version\n"
            + "  - mkdir -p .javachanges\n"
            + "  - >\n"
            + "    test -f \".javachanges/javachanges-${JAVACHANGES_VERSION}.jar\" ||\n"
            + "    curl -fsSL\n"
            + "    \"https://repo1.maven.org/maven2/io/github/sonofmagic/javachanges/${JAVACHANGES_VERSION}/javachanges-${JAVACHANGES_VERSION}.jar\"\n"
            + "    -o \".javachanges/javachanges-${JAVACHANGES_VERSION}.jar\"\n"
            + "\n"
            + "verify:\n"
            + "  stage: verify\n"
            + "  script:\n"
            + "    - ./gradlew --no-daemon build\n"
            + "    - java -jar \".javachanges/javachanges-${JAVACHANGES_VERSION}.jar\" status --directory \"$CI_PROJECT_DIR\"\n"
            + "  rules:\n"
            + "    - if: $CI_PIPELINE_SOURCE == \"merge_request_event\"\n"
            + "    - if: $CI_COMMIT_BRANCH\n"
            + "\n"
            + "release_plan_mr:\n"
            + "  stage: release-plan\n"
            + "  script:\n"
            + "    - >\n"
            + "      java -jar \".javachanges/javachanges-${JAVACHANGES_VERSION}.jar\"\n"
            + "      gitlab-release-plan\n"
            + "      --directory \"$CI_PROJECT_DIR\"\n"
            + "      --project-id \"$CI_PROJECT_ID\"\n"
            + "      --execute true\n"
            + "  rules:\n"
            + "    - if: $CI_COMMIT_BRANCH == \"" + baseBranch + "\"\n"
            + "\n"
            + "release_tag:\n"
            + "  stage: tag\n"
            + "  script:\n"
            + "    - java -jar \".javachanges/javachanges-${JAVACHANGES_VERSION}.jar\" gitlab-tag-from-plan --directory \"$CI_PROJECT_DIR\" --execute true\n"
            + "  rules:\n"
            + "    - if: $CI_COMMIT_BRANCH == \"" + baseBranch + "\"\n"
            + "\n"
            + "publish_snapshot:\n"
            + "  stage: publish\n"
            + "  script:\n"
            + "    - java -jar \".javachanges/javachanges-${JAVACHANGES_VERSION}.jar\" gradle-publish --directory \"$CI_PROJECT_DIR\" --execute true\n"
            + "  rules:\n"
            + "    - if: $CI_COMMIT_BRANCH == \"" + snapshotBranch + "\"\n"
            + "\n"
            + "publish_release:\n"
            + "  stage: publish\n"
            + "  script:\n"
            + "    - java -jar \".javachanges/javachanges-${JAVACHANGES_VERSION}.jar\" gradle-publish --directory \"$CI_PROJECT_DIR\" --execute true\n"
            + "    - java -jar \".javachanges/javachanges-${JAVACHANGES_VERSION}.jar\" gitlab-release --directory \"$CI_PROJECT_DIR\" --execute true\n"
            + "  rules:\n"
            + "    - if: $CI_COMMIT_TAG\n";
    }
}
