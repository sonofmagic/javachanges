---
description: Use javachanges in GitLab CI/CD for validation, release merge requests, tag creation, and Maven or Gradle publishing.
---

# javachanges GitLab CI/CD Usage Guide


## 1. Overview

This guide explains how to use `javachanges` in GitLab CI/CD for:

1. regular validation
2. GitLab CI/CD variable management
3. release merge request generation
4. release tag creation from the generated release plan
5. Maven or Gradle publishing in tag pipelines
6. Maven and Gradle dependency caching

`javachanges` now has four GitLab-specific commands:

| Command | Purpose |
| --- | --- |
| `gitlab-release-plan` | Create or update a release branch and release merge request |
| `gitlab-tag-from-plan` | Create and push the final release tag after the release plan lands |
| `gitlab-release` | Generate release notes and create or update the GitLab Release for the current CI tag |
| `init-gitlab-ci` | Generate the minimal GitLab CI file that wires release-plan, tag, publish, and GitLab Release jobs |

## 2. What `javachanges` Can Do In GitLab CI/CD

Recommended command mapping:

| Goal | Command |
| --- | --- |
| Check pending release state | `status` |
| Apply a release plan locally or in CI | `plan --apply true` |
| Generate Maven settings from env vars | `write-settings` |
| Preview GitLab variables from a local env file | `render-vars --platform gitlab` |
| Check platform readiness | `doctor-local`, `doctor-platform` |
| Sync GitLab variables through `glab` | `sync-vars --platform gitlab` |
| Audit GitLab variables through `glab variable export` | `audit-vars --platform gitlab` |
| Create or update a GitLab release MR | `gitlab-release-plan --write-plan-files false --execute true` |
| Create and push a release tag after a release plan merge | `gitlab-tag-from-plan --fresh true --execute true` |
| Validate a publish | `preflight` |
| Run the real Maven deploy command | `publish --execute true` |
| Run the real Gradle publish task | `gradle-publish --execute true` |
| Create or update a GitLab Release from the tag pipeline | `gitlab-release --execute true` |

## 3. Variable Model

### 3.1 Shared Maven repository variables

`javachanges` understands these values from `env/release.env.example`:

| Variable | Required | Meaning |
| --- | --- | --- |
| `MAVEN_RELEASE_REPOSITORY_URL` | Yes | Release repository URL |
| `MAVEN_SNAPSHOT_REPOSITORY_URL` | Yes | Snapshot repository URL |
| `MAVEN_RELEASE_REPOSITORY_ID` | Yes | Release repository id |
| `MAVEN_SNAPSHOT_REPOSITORY_ID` | Yes | Snapshot repository id |
| `MAVEN_REPOSITORY_USERNAME` | Yes, unless explicit split credentials are used | Shared username |
| `MAVEN_REPOSITORY_PASSWORD` | Yes, unless explicit split credentials are used | Shared password |
| `MAVEN_RELEASE_REPOSITORY_USERNAME` | Optional | Explicit release username |
| `MAVEN_RELEASE_REPOSITORY_PASSWORD` | Optional | Explicit release password |
| `MAVEN_SNAPSHOT_REPOSITORY_USERNAME` | Optional | Explicit snapshot username |
| `MAVEN_SNAPSHOT_REPOSITORY_PASSWORD` | Optional | Explicit snapshot password |
| `GITLAB_RELEASE_TOKEN` | Optional | Extra token for GitLab release creation flows outside CI job token fallback |

When syncing to GitLab with `sync-vars`, secret values are written as masked and protected variables.

### 3.2 Extra variables for GitLab release branch and MR automation

`gitlab-release-plan` also depends on these runtime values:

| Variable | Source |
| --- | --- |
| `CI_PROJECT_ID` | GitLab built-in CI variable or `--project-id` |
| `CI_DEFAULT_BRANCH` | GitLab built-in CI variable |
| `CI_SERVER_HOST` | GitLab built-in CI variable |
| `CI_SERVER_URL` | GitLab built-in CI variable |
| `CI_PROJECT_PATH` | GitLab built-in CI variable |
| `GITLAB_RELEASE_BOT_USERNAME` | Project variable you provide |
| `GITLAB_RELEASE_BOT_TOKEN` | Project variable you provide |

`gitlab-tag-from-plan` additionally needs:

| Option or variable | Meaning |
| --- | --- |
| `--before-sha` or `CI_COMMIT_BEFORE_SHA` | Previous commit SHA |
| `--current-sha` or `CI_COMMIT_SHA` | Current commit SHA |

## 4. Local Preparation

### 4.1 Build the CLI

```bash
mvn -q test
```

### 4.2 Initialize a local env file

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="init-env --target env/release.env.local"
```

### 4.3 Preview GitLab variables

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="render-vars --env-file env/release.env.local --platform gitlab"
```

### 4.4 Check local readiness

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="doctor-local --env-file env/release.env.local --gitlab-repo group/project"
```

### 4.5 Sync GitLab variables with `glab`

Dry-run:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="sync-vars --env-file env/release.env.local --platform gitlab --repo group/project"
```

Apply:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="sync-vars --env-file env/release.env.local --platform gitlab --repo group/project --execute true"
```

Audit:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="audit-vars --env-file env/release.env.local --platform gitlab --gitlab-repo group/project"
```

## 5. Recommended GitLab CI/CD Pipeline Topology

Recommended stages:

1. `verify`
2. `release-plan`
3. `tag`
4. `publish`

## 6. Minimal `.gitlab-ci.yml`

If you want the shortest stable setup, let `javachanges` generate it:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="init-gitlab-ci --directory /path/to/repo --output .gitlab-ci.yml --force true"
```

If you prefer to call the released Maven plugin directly from a business repository, the shortest runnable form is:

```bash
mvn -B io.github.sonofmagic:javachanges:__JAVACHANGES_LATEST_RELEASE_VERSION__:run -Djavachanges.args="gitlab-release-plan --directory $CI_PROJECT_DIR --write-plan-files false --execute true"
```

Generated template shape:

```yaml
stages:
  - verify
  - release-plan
  - tag
  - publish

default:
  image: maven:3.9.9-eclipse-temurin-8
  cache:
    key:
      files:
        - pom.xml
    paths:
      - .m2/repository

variables:
  MAVEN_OPTS: "-Dmaven.repo.local=.m2/repository"
  JAVACHANGES_VERSION: "__JAVACHANGES_LATEST_RELEASE_VERSION__"

verify:
  stage: verify
  script:
    - mvn -B verify
    - >
      mvn -B io.github.sonofmagic:javachanges:${JAVACHANGES_VERSION}:run
      -Djavachanges.args="status --directory $CI_PROJECT_DIR"
  rules:
    - if: $CI_PIPELINE_SOURCE == "merge_request_event"
    - if: $CI_COMMIT_BRANCH

release_plan_mr:
  stage: release-plan
  script:
    - >
      mvn -B io.github.sonofmagic:javachanges:${JAVACHANGES_VERSION}:run
      -Djavachanges.args="gitlab-release-plan --directory $CI_PROJECT_DIR --write-plan-files false --execute true"
  rules:
    - if: $CI_COMMIT_BRANCH == "main"

release_tag:
  stage: tag
  script:
    - >
      mvn -B io.github.sonofmagic:javachanges:${JAVACHANGES_VERSION}:run
      -Djavachanges.args="gitlab-tag-from-plan --directory $CI_PROJECT_DIR --fresh true --execute true"
  rules:
    - if: $CI_COMMIT_BRANCH == "main"

publish_snapshot:
  stage: publish
  script:
    - >
      mvn -B io.github.sonofmagic:javachanges:${JAVACHANGES_VERSION}:run
      -Djavachanges.args="publish --directory $CI_PROJECT_DIR --execute true"
  rules:
    - if: $CI_COMMIT_BRANCH == "snapshot"

publish_release:
  stage: publish
  script:
    - >
      mvn -B io.github.sonofmagic:javachanges:${JAVACHANGES_VERSION}:run
      -Djavachanges.args="publish --directory $CI_PROJECT_DIR --execute true"
    - >
      mvn -B io.github.sonofmagic:javachanges:${JAVACHANGES_VERSION}:run
      -Djavachanges.args="gitlab-release --directory $CI_PROJECT_DIR --execute true"
  rules:
    - if: $CI_COMMIT_TAG
```

How the example works:

| Job | Purpose |
| --- | --- |
| `verify` | Validates the repository and prints release state |
| `release_plan_mr` | Creates or updates the release branch and merge request |
| `release_tag` | Creates the final tag after the release plan manifest has changed on the default branch |
| `publish_snapshot` | Publishes from the configured snapshot branch without extra shell branch parsing |
| `publish_release` | Publishes from the final Git tag and creates or updates the GitLab Release |

If `.changesets/config.json` or `.changesets/config.jsonc` contains:

```jsonc
{
  "snapshotBranch": "snapshot",
  "snapshotVersionMode": "plain"
}
```

then the same `publish --directory $CI_PROJECT_DIR --execute true` snapshot job automatically switches to plain snapshot mode on that branch. No extra `if` block or custom `mvn deploy` split is required in the business repository.

### 6.1 Minimal Gradle `.gitlab-ci.yml`

For Gradle repositories, use the CLI jar directly and let Gradle own artifact publishing:

```yaml
stages:
  - verify
  - release-plan
  - tag
  - publish

default:
  image: eclipse-temurin:17
  cache:
    key:
      files:
        - gradle.properties
        - settings.gradle.kts
    paths:
      - .gradle/caches
      - .gradle/wrapper
      - .javachanges

variables:
  GRADLE_USER_HOME: "$CI_PROJECT_DIR/.gradle"
  JAVACHANGES_VERSION: "__JAVACHANGES_LATEST_RELEASE_VERSION__"

before_script:
  - ./gradlew --version
  - mkdir -p .javachanges
  - >
    test -f ".javachanges/javachanges-${JAVACHANGES_VERSION}.jar" ||
    curl -fsSL
    "https://repo1.maven.org/maven2/io/github/sonofmagic/javachanges/${JAVACHANGES_VERSION}/javachanges-${JAVACHANGES_VERSION}.jar"
    -o ".javachanges/javachanges-${JAVACHANGES_VERSION}.jar"

verify:
  stage: verify
  script:
    - ./gradlew --no-daemon build
    - java -jar ".javachanges/javachanges-${JAVACHANGES_VERSION}.jar" status --directory "$CI_PROJECT_DIR"
  rules:
    - if: $CI_PIPELINE_SOURCE == "merge_request_event"
    - if: $CI_COMMIT_BRANCH

release_plan_mr:
  stage: release-plan
  script:
    - >
      java -jar ".javachanges/javachanges-${JAVACHANGES_VERSION}.jar"
      gitlab-release-plan
      --directory "$CI_PROJECT_DIR"
      --project-id "$CI_PROJECT_ID"
      --write-plan-files false
      --execute true
  rules:
    - if: $CI_COMMIT_BRANCH == "main"

release_tag:
  stage: tag
  script:
    - java -jar ".javachanges/javachanges-${JAVACHANGES_VERSION}.jar" gitlab-tag-from-plan --directory "$CI_PROJECT_DIR" --fresh true --execute true
  rules:
    - if: $CI_COMMIT_BRANCH == "main"

publish_release:
  stage: publish
  script:
    - java -jar ".javachanges/javachanges-${JAVACHANGES_VERSION}.jar" gradle-publish --directory "$CI_PROJECT_DIR" --execute true
    - java -jar ".javachanges/javachanges-${JAVACHANGES_VERSION}.jar" gitlab-release --directory "$CI_PROJECT_DIR" --execute true
  rules:
    - if: $CI_COMMIT_TAG
```

The release-plan job stages `gradle.properties`, `CHANGELOG.md`, and `.changesets/` for Gradle repositories. `gradle-publish` renders and executes the Gradle `publish` task with the release or snapshot version resolved from the same manifest. For a simpler jar download step, you can also use `mvn dependency:copy` if Maven is available in your runner image.

## 7. Safe `script:` Patterns For GitLab CI

Recommended:

- Keep each `script:` item to one command when possible.
- Use YAML folded scalars like `- >` for long single commands that need line wrapping.
- Prefer direct `mvn ...` or `java -jar ...` invocation over inline shell program generation.
- Prefer the official Maven plugin entrypoint over custom shell wrappers or ad hoc runner POM files in business repositories.
- If CI must write a file, prefer `printf`, `echo`, or a checked-in script under `scripts/`.
- Quote GitLab variables explicitly, for example `"$CI_PROJECT_DIR"` and `"$CI_COMMIT_TAG"`.

Not recommended:

- `script: - |` blocks that contain shell heredoc such as `cat <<EOF`.
- Heredoc bodies whose lines can be misread as YAML keys or list items after indentation changes.
- Large inline shell programs embedded directly in `.gitlab-ci.yml` when the same logic can live in a repository script.

Why this pitfall is common:

- GitLab parses `.gitlab-ci.yml` as YAML before the shell runs.
- Heredoc syntax needs indentation that remains valid for both YAML and the shell terminator.
- A small reindent can make YAML treat heredoc content as a new mapping key, which causes errors such as `could not find expected ':' while scanning a simple key`.
- This is easy to trigger when users copy a working shell snippet into `script: - |` and then adjust indentation by hand.

Recommended pattern:

```yaml
release_tag:
  stage: tag
  script:
    - mvn -B -DskipTests compile
    - >
      mvn -B -DskipTests compile exec:java
      -Dexec.args="gitlab-tag-from-plan --directory $CI_PROJECT_DIR --fresh true --before-sha $CI_COMMIT_BEFORE_SHA --current-sha $CI_COMMIT_SHA --execute true"
```

Avoid:

```yaml
release_tag:
  stage: tag
  script:
    - |
      cat <<EOF > release.env
      CI_PROJECT_DIR=$CI_PROJECT_DIR
      CI_COMMIT_SHA=$CI_COMMIT_SHA
      EOF
      mvn -B -DskipTests compile exec:java -Dexec.args="gitlab-tag-from-plan --directory $CI_PROJECT_DIR --fresh true --execute true"
```

Safer file-generation pattern:

```yaml
write_env:
  script:
    - printf 'CI_PROJECT_DIR=%s\nCI_COMMIT_SHA=%s\n' "$CI_PROJECT_DIR" "$CI_COMMIT_SHA" > release.env
```

For longer setup flows, move the shell into a repository script:

```yaml
release_plan_mr:
  script:
    - ./scripts/gitlab-release-plan.sh
```

## 8. How The GitLab-specific Commands Behave

### 8.1 `gitlab-release-plan`

Default behavior:

| Input | Default |
| --- | --- |
| `--project-id` | `CI_PROJECT_ID` |
| `--target-branch` | `CI_DEFAULT_BRANCH`, or `main` if absent |
| `--release-branch` | `changeset-release/<target-branch>` |

Important behavior:

| Condition | Result |
| --- | --- |
| No pending changesets | Skips the release MR |
| `--execute true` missing | Dry-run only |
| Release plan produces no staged file changes | Skips MR update |
| Open release MR already exists | Updates it instead of creating a new one |
| Remote `changeset-release/*` branch already exists | Reuses the branch by resolving its current remote SHA, then pushes with an explicit `--force-with-lease` |

Notes:

- `gitlab-release-plan` treats `changeset-release/<target-branch>` as an automation-owned branch.
- If the remote branch exists but no open MR matches it, the command still refreshes that branch and then creates a new MR.
- This keeps repeated default-branch pipelines idempotent without requiring manual branch deletion.

### 8.2 `gitlab-tag-from-plan`

Important behavior:

| Condition | Result |
| --- | --- |
| `beforeSha` missing or all zeros | Skips tagging |
| release state did not change between commits | Skips tagging |
| Tag already exists remotely | Skips tagging |
| `--execute true` missing | Dry-run only |

## 9. Generic Maven Publish In GitLab CI/CD

The generic `publish` helper uses:

1. `preflight` logic to verify revision, tag, and credentials
2. `write-settings` logic to generate `.m2/settings.xml`
3. repository variables such as `MAVEN_RELEASE_REPOSITORY_URL`
4. credentials from your GitLab CI/CD variables

Snapshot mode behavior:

- default behavior stays `stamped`, which rewrites `1.2.3-SNAPSHOT` to a unique stamped revision before deploy
- if the configured `snapshotBranch` matches the current branch and `snapshotVersionMode` is `plain`, `publish --execute true` keeps the effective version at the original `1.2.3-SNAPSHOT`
- `preflight` and `publish` logs now print the resolved snapshot mode so pipeline logs make the choice explicit
- even in plain mode, Maven snapshot repositories still normally produce timestamped artifact filenames on the server side; that is repository-standard snapshot expansion, not a second rewrite by `javachanges`

Typical tag-pipeline split:

```yaml
publish_preflight:
  stage: publish
  script:
    - mvn -B -DskipTests compile
    - >
      mvn -B -DskipTests compile exec:java
      -Dexec.args="preflight --directory $CI_PROJECT_DIR --tag $CI_COMMIT_TAG"
  rules:
    - if: $CI_COMMIT_TAG

publish_execute:
  stage: publish
  script:
    - mvn -B -DskipTests compile
    - >
      mvn -B -DskipTests compile exec:java
      -Dexec.args="publish --directory $CI_PROJECT_DIR --tag $CI_COMMIT_TAG --execute true"
  rules:
    - if: $CI_COMMIT_TAG
```

## 10. Maven Cache Behavior In GitLab CI/CD

Recommended cache:

```yaml
cache:
  key:
    files:
      - pom.xml
  paths:
    - .m2/repository
```

Recommended runtime option:

```yaml
variables:
  MAVEN_OPTS: "-Dmaven.repo.local=.m2/repository"
```

What this improves:

| Cached well | Not solved by GitLab cache alone |
| --- | --- |
| Maven dependencies | Git clone/fetch cost |
| Maven plugins | JDK image pull time |
| Repeat pipelines with the same `pom.xml` | GitLab API calls for release MR creation |
| Reuse across jobs on shared cache backends | Remote repository publishing latency |

Important behavior:

| Situation | Result |
| --- | --- |
| New cache key | First pipeline still downloads |
| `pom.xml` changes | Cache key may change |
| Different runners without shared cache | Cache reuse may be weak |
| Shared/distributed GitLab cache configured | Cross-runner reuse improves |

## 11. Optional Hygiene And Secret Scanning

If you add a hygiene or secret-scanning job to the same repository, distinguish two different outcomes:

- A real secret hit means repository content contains a value that looks like an actual credential or private key.
- A rule self-hit means the scanner matched its own detection patterns, such as `ghp_`, `glpat-`, `AKIA`, or `BEGIN PRIVATE KEY`, inside `.gitlab-ci.yml`, `Makefile`, or another rule-definition file.

Recommended default strategy:

1. keep secret-detection patterns in a dedicated file such as `.hygiene/secret-patterns.txt`
2. exclude that file, plus `.gitlab-ci.yml` and `Makefile`, from content scanning
3. scan source, docs, scripts, and config that may carry real secrets
4. use allowlist comments only for one-off reviewed exceptions

Why this is the safest default:

- scanner configuration is not business content and should not be scanned like application files
- excluding one dedicated rules file is easier to reason about than scattering regex literals through CI YAML
- it avoids fragile pattern splitting that makes rule maintenance harder

Recommended example:

```yaml
hygiene:
  stage: verify
  script:
    - ./scripts/secret-scan.sh
  rules:
    - if: $CI_COMMIT_BRANCH
```

```bash
# scripts/secret-scan.sh
set -eu

scanner scan \
  --rules .hygiene/secret-patterns.txt \
  --exclude .hygiene/secret-patterns.txt \
  --exclude .gitlab-ci.yml \
  --exclude Makefile \
  .
```

Option comparison:

| Option | Pros | Cons | Recommendation |
| --- | --- | --- | --- |
| Exclude `.gitlab-ci.yml` / `Makefile` only | simple and quick | still fails if rules move into another scanned file | useful as a minimum stopgap |
| Move rules to a dedicated file and exclude it | clear ownership, easiest to explain, stable over time | needs one extra checked-in file | recommended default |
| Split patterns into fragments and concatenate them | can avoid literal self-matches without file exclusions | hurts readability, easier to break, may reduce portability across tools | avoid by default |
| Use allowlist comments | precise for a few reviewed lines | noisy, tool-specific, easy to overuse until real hits get hidden | keep for exceptional cases only |

Avoid:

- defining detector regex literals directly in `.gitlab-ci.yml`
- defining the same literals inline in `Makefile` targets
- treating allowlists as the primary suppression mechanism for scanner-owned files

## 12. Common Mistakes

| Problem | Cause | Fix |
| --- | --- | --- |
| Release MR job fails to push | `GITLAB_RELEASE_BOT_TOKEN` or `GITLAB_RELEASE_BOT_USERNAME` missing | add the bot credentials as project variables |
| Release MR job fails with `stale info` | another process updated `changeset-release/*` after javachanges resolved the remote SHA | rerun the pipeline; if the branch is shared by other automation, stop sharing that branch name |
| Release tag job never tags | release state did not change or `CI_COMMIT_BEFORE_SHA` is unusable | inspect the branch pipeline, version file, and changelog |
| Snapshot publish job cannot see Maven credentials or `GITLAB_RELEASE_TOKEN` | the variables are protected but the configured `snapshotBranch` is not a protected branch | protect the `snapshotBranch`, then rerun `doctor-platform --platform gitlab` and the pipeline |
| GitLab rejects the pipeline before any job starts with `could not find expected ':' while scanning a simple key` | heredoc or other multiline shell content broke YAML indentation rules | replace `script: - |` heredoc blocks with `- >`, `printf`, or a checked-in shell script |
| Hygiene or secret scan fails on `.gitlab-ci.yml` or `Makefile`, but no real credential was added | the scanner matched rule literals inside its own configuration | move patterns into a dedicated rules file and exclude scanner-owned files from scanning |
| `sync-vars` does nothing | env file still contains placeholders | replace `replace-me` values first |
| `audit-vars` fails with `MISMATCH` | local env and remote project variables diverged | resync or deliberately update one side |
| Publish job fails on missing Maven credentials | project variables were not configured | sync the variables with `glab`, then rerun |

## 13. Recommended Documentation Split

Use these docs together:

| Need | Document |
| --- | --- |
| Generic release commands and local preparation | [Development Guide](./development-guide.md) |
| GitHub-based self-release flow in this repository | [GitHub Actions Release Flow](./github-actions-release.md) |
| Maven Central-specific publishing | [Publish To Maven Central](./publish-to-maven-central.md) |

## 14. Summary

The practical GitLab CI/CD path is:

1. validate with `status`
2. create or update a release MR with `gitlab-release-plan`
3. create the final tag with `gitlab-tag-from-plan`
4. sync and audit GitLab variables with `sync-vars` and `audit-vars`
5. publish snapshots and tags with the same `publish --execute true` command
6. create or update the GitLab Release with `gitlab-release`

## 15. References

- GitLab CI/CD YAML syntax: https://docs.gitlab.com/ci/yaml/
- GitLab CI/CD caching: https://docs.gitlab.com/ci/caching/
- `glab auth login`: https://docs.gitlab.com/cli/auth/login/
- `glab variable set`: https://docs.gitlab.com/cli/variable/set/
- `glab variable export`: https://docs.gitlab.com/cli/variable/export/
