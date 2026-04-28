---
description: Complete command reference for javachanges, including release planning, environment, and publishing helpers.
---

# javachanges CLI Reference


## 1. Overview

This page is the command reference for `javachanges`.

Use it when you already understand the release workflow and need to look up:

- the command name
- required flags
- common argument combinations
- what each command reads or writes

## 2. Invocation Pattern

Current `main` branch Maven plugin invocation after local snapshot install:

```bash
mvn -q -DskipTests install
mvn io.github.sonofmagic:javachanges:__JAVACHANGES_CURRENT_SNAPSHOT_VERSION__:status
mvn io.github.sonofmagic:javachanges:__JAVACHANGES_CURRENT_SNAPSHOT_VERSION__:plan -Djavachanges.apply=true
mvn io.github.sonofmagic:javachanges:__JAVACHANGES_CURRENT_SNAPSHOT_VERSION__:add -Djavachanges.summary="add release notes command" -Djavachanges.release=minor
mvn io.github.sonofmagic:javachanges:__JAVACHANGES_CURRENT_SNAPSHOT_VERSION__:manifest-field -Djavachanges.field=releaseVersion
mvn io.github.sonofmagic:javachanges:__JAVACHANGES_CURRENT_SNAPSHOT_VERSION__:run -Djavachanges.args="release-notes --tag v1.2.3"
```

Source-driven invocation while developing this repository:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="status --directory /path/to/repo"
```

Common parts:

| Part | Meaning |
| --- | --- |
| `mvn io.github.sonofmagic:javachanges:__JAVACHANGES_CURRENT_SNAPSHOT_VERSION__:status` | Run the dedicated Maven plugin status goal |
| `mvn io.github.sonofmagic:javachanges:__JAVACHANGES_CURRENT_SNAPSHOT_VERSION__:plan -Djavachanges.apply=true` | Run the dedicated plan goal |
| `mvn io.github.sonofmagic:javachanges:__JAVACHANGES_CURRENT_SNAPSHOT_VERSION__:run -Djavachanges.args="..."` | Use the generic bridge goal for commands without a dedicated goal |
| `mvn -q -DskipTests compile exec:java` | Build the CLI and run the Java entrypoint |
| `-Dexec.args="..."` | Pass `javachanges` CLI arguments |
| `--directory /path/to/repo` | Target Maven or Gradle repository root, or a subdirectory inside it |

Plugin note:

- all dedicated goals and `javachanges:run` inject `--directory ${project.basedir}` automatically unless you already passed `--directory` explicitly
- for CI or external repositories, you can call the published plugin directly without a custom runner POM:

```bash
mvn -B io.github.sonofmagic:javachanges:1.4.1:run -Djavachanges.args="gitlab-release-plan --directory $CI_PROJECT_DIR --execute true"
```

If you declare the plugin in a target repository `pom.xml`, the shortest local form becomes:

```bash
mvn javachanges:status
mvn javachanges:plan -Djavachanges.apply=true
mvn javachanges:add -Djavachanges.summary="add release notes command" -Djavachanges.release=minor
mvn javachanges:manifest-field -Djavachanges.field=releaseVersion
```

Gradle repositories should use the CLI jar form:

```bash
mvn -q dependency:copy -Dartifact=io.github.sonofmagic:javachanges:__JAVACHANGES_LATEST_RELEASE_VERSION__ -DoutputDirectory=.javachanges
java -jar .javachanges/javachanges-__JAVACHANGES_LATEST_RELEASE_VERSION__.jar status --directory /path/to/gradle-repo
java -jar .javachanges/javachanges-__JAVACHANGES_LATEST_RELEASE_VERSION__.jar plan --directory /path/to/gradle-repo --apply true
```

Gradle detection requires `gradle.properties` with `version` or `revision`, plus `settings.gradle(.kts)` or `build.gradle(.kts)`.

> Note: some commands do not require a repository, for example `release-version-from-tag`.

## 3. Safely Wrapping `javachanges` Through Maven

When `javachanges` is executed through `exec-maven-plugin`, keep the entire CLI payload inside one `-Dexec.args=...` value. Do not define a reusable shell fragment that ends with bare `-Dexec.args=` and append the real command later, because the shell, Make, or CI runner may split the next token and Maven will then treat it as a lifecycle phase.

Recommended Makefile pattern:

```make
MVNW := ./mvnw
JAVACHANGES_MVN := $(MVNW) -q -DskipTests compile exec:java

jc-version:
	$(JAVACHANGES_MVN) -Dexec.args="version --directory $(CURDIR)"
```

Recommended parameterized Makefile pattern:

```make
MVNW := ./mvnw
JAVACHANGES_MVN := $(MVNW) -q -DskipTests compile exec:java

define RUN_JAVACHANGES
$(JAVACHANGES_MVN) -Dexec.args="$(1) --directory $(CURDIR)"
endef

jc-status:
	$(call RUN_JAVACHANGES,status)
```

Recommended GitLab CI pattern:

```yaml
script:
  - >
    ./mvnw -q -DskipTests compile exec:java
    -Dexec.args="version --directory $CI_PROJECT_DIR"
```

Not recommended:

```make
JAVACHANGES = ./mvnw -q -DskipTests compile exec:java -Dexec.args=

jc-version:
	$(JAVACHANGES) "version --directory $(CURDIR)"
```

Why it breaks:

- `-Dexec.args=` is already complete before the next token is appended.
- The quoted `version --directory ...` token is no longer attached to the system property assignment.
- Maven receives that token as a positional argument and may parse it as a lifecycle phase, causing errors such as `Unknown lifecycle phase "version --directory ..."`.

Rules of thumb:

- keep `-Dexec.args="..."` in the final command line, not in a prefix variable
- prefer one shell command per invocation
- if you need reuse, wrap the whole command in a Make function or shell script
- in CI YAML, prefer one folded scalar command instead of concatenating fragments across variables

## 4. High-Value Commands

| Command | Purpose | Writes files |
| --- | --- | --- |
| `add` | Create a changeset | `.changesets/*.md` |
| `status` | Show the current release plan | No |
| `plan` | Render the current release plan | No |
| `plan --apply true` | Apply the plan and consume changesets | `pom.xml` or `gradle.properties`, `CHANGELOG.md`, `.changesets/release-plan.json`, `.changesets/release-plan.md` |
| `manifest-field` | Read a field from the generated release manifest | No |
| `release-notes` | Generate release notes for a tag | target file |
| `ensure-gpg-public-key` | Publish and verify the current signing public key on supported keyservers | No |
| `preflight` | Render publish validation commands | No |
| `publish` | Render or execute the Maven publish command | No |
| `gradle-publish` | Render or execute the Gradle publish command | No |

## 5. Changeset Commands

### 5.1 `add`

Create a new changeset file.

Example:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="add --directory /path/to/repo --summary 'add release notes command' --release minor --modules core"
```

Behavior:

| Input | Meaning |
| --- | --- |
| `--summary` | First line of the generated markdown body |
| `--release` | `patch`, `minor`, or `major` |
| `--modules` | Comma-separated Maven artifactIds, Gradle project names, or `all` |
| `--body` | Optional extra markdown content after the summary |

Generated file shape:

````md
```md
---
"core": minor
---

add release notes command
```
````

Compatibility note:

- `add` still accepts legacy flag names such as `--release` and `--modules`
- the generated file itself uses the official Changesets-style package map

### 5.2 `status`

Show the current pending release plan.

Example:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="status --directory /path/to/repo"
```

Typical output includes:

- current root revision
- latest whole-repo tag
- pending changeset count
- release plan summary
- affected packages
- each pending changeset entry

### 5.3 `plan`

Render the release plan without writing files:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="plan --directory /path/to/repo"
```

Apply the release plan:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="plan --directory /path/to/repo --apply true"
```

When `--apply true` is used, `javachanges`:

1. updates the root Maven `<revision>` or Gradle `gradle.properties` version
2. prepends a new section to `CHANGELOG.md`
3. writes `.changesets/release-plan.json`
4. writes `.changesets/release-plan.md`
5. deletes the consumed changeset files

### 5.4 `manifest-field`

Read a field from the generated release manifest.

Example:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="manifest-field --directory /path/to/repo --field releaseVersion"
```

Common fields:

| Field | Meaning |
| --- | --- |
| `releaseVersion` | Release version without the leading `v` |
| `nextSnapshotVersion` | Next root snapshot version |
| `releaseLevel` | Aggregated release type |

## 6. Repository And Version Commands

| Command | Purpose | Example |
| --- | --- | --- |
| `version` | Print the current root revision | `version --directory /path/to/repo` |
| `release-version-from-tag` | Extract `1.2.3` from `v1.2.3` or `core/v1.2.3` | `release-version-from-tag --tag v1.2.3` |
| `release-module-from-tag` | Extract the package/module name from `core/v1.2.3` | `release-module-from-tag --tag core/v1.2.3` |
| `assert-module` | Validate a Maven artifactId or Gradle project name exists | `assert-module --directory /path/to/repo --module core` |
| `assert-snapshot` | Ensure the current revision is a snapshot | `assert-snapshot --directory /path/to/repo` |
| `assert-release-tag` | Ensure a tag matches the current repository version | `assert-release-tag --directory /path/to/repo --tag v1.2.3` |
| `module-selector-args` | Print build-tool selector args | `module-selector-args --directory /path/to/repo --module core` |

For Maven repositories, `module-selector-args --module core` prints Maven `-pl :core -am`.
For Gradle repositories, it prints the Gradle project selector `:core`.

## 7. Environment And Settings Commands

| Command | Purpose |
| --- | --- |
| `write-settings` | Generate a Maven `settings.xml` file |
| `init-env` | Initialize a local release env file from the example template |
| `auth-help` | Print platform auth requirements |
| `render-vars` | Preview GitHub/GitLab variables and secrets, or emit JSON with `--format json` |
| `doctor-local` | Check local release prerequisites, or emit JSON with `--format json` |
| `doctor-platform` | Check remote platform readiness, or emit JSON with `--format json` |
| `sync-vars` | Sync variables to GitHub or GitLab |
| `audit-vars` | Compare local env values with remote platform state, or emit JSON with `--format json` |
| `ensure-gpg-public-key` | Upload the current public signing key and wait until a supported keyserver can fetch it |

Example:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="render-vars --directory /path/to/repo --env-file env/release.env.local --platform github"
```

Structured-output example:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="doctor-local --directory /path/to/repo --env-file env/release.env.local --format json"
```

Commands that currently support `--format json`:

| Command | Notes |
| --- | --- |
| `render-vars` | Includes `platform` and `showSecrets` in the payload |
| `doctor-local` | Includes section summaries, suggestions, and final error text on failure |
| `doctor-platform` | Includes `platform` and section summaries for env and CLI checks |
| `audit-vars` | Includes `platform`, audit sections, and final error text on failure |
| `preflight` | Includes publish action metadata plus snapshot mode fields such as `snapshotVersionMode`, `effectiveVersion`, and `snapshotBuildStampApplied` |
| `publish` | Includes publish action metadata such as tag, module, release version, and release notes file |
| `gradle-publish` | Includes Gradle publish action metadata such as tag, module, release version, and snapshot mode |
| `github-release-plan` | Includes action, skip reason, and release version |
| `github-tag-from-plan` | Includes action, skip reason, release version, and tag |
| `github-release-from-plan` | Includes action, tag, release version, and release notes file |
| `gitlab-release-plan` | Includes action, skip reason, release version, and project id |
| `gitlab-tag-from-plan` | Includes action, skip reason, release version, module, and tag |
| `gitlab-release` | Includes action, project id, tag, module, release version, and release notes file |

Common flags for these commands:

| Flag | Used by | Meaning |
| --- | --- | --- |
| `--env-file` | all four | input env file path |
| `--platform` | `render-vars`, `doctor-platform`, `audit-vars` | `github`, `gitlab`, or `all` |
| `--show-secrets` | `render-vars` | reveal secret values instead of masking them |
| `--github-repo` | `doctor-local`, `doctor-platform`, `audit-vars` | optional GitHub `owner/repo` identifier |
| `--gitlab-repo` | `doctor-local`, `doctor-platform`, `audit-vars` | optional GitLab `group/project` identifier |
| `--format json` | all four | switch stdout from human text to machine-readable JSON |

### 7.1 `ensure-gpg-public-key`

Use this command in CI after GPG import and before a Maven Central publish:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="ensure-gpg-public-key --directory /path/to/repo"
```

What it does:

- reads the imported secret key fingerprint from `gpg`
- attempts to publish the public key to `hkps://keyserver.ubuntu.com` and `hkps://keys.openpgp.org`
- retries until at least one supported keyserver can fetch the key

Useful flags:

| Flag | Meaning |
| --- | --- |
| `--primary-keyserver` | Override the first keyserver URL |
| `--secondary-keyserver` | Override the fallback keyserver URL |
| `--attempts` | Maximum discovery attempts before failure |
| `--retry-delay-seconds` | Delay between discovery attempts |

JSON mode contract:

- stdout contains only one JSON object
- exit code `0` means success, non-zero means validation failure or execution error
- top-level fields may include `ok`, `command`, `envFile`, `platform`, `showSecrets`, `sections`, `suggestions`, and `error`

## 8. Publish Commands

### 8.1 `preflight`

Render the Maven validation flow before a real publish.

Snapshot example:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="preflight --directory /path/to/repo --snapshot"
```

Plain snapshot example:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="preflight --directory /path/to/repo --snapshot --snapshot-version-mode plain"
```

Explicit snapshot build stamp:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="preflight --directory /path/to/repo --snapshot --snapshot-build-stamp 20260420.154500.ci001"
```

Release example:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="preflight --directory /path/to/repo --tag v1.2.3"
```

GitLab snapshot branch example with config-driven defaults:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="preflight --directory $CI_PROJECT_DIR"
```

In plain snapshot mode, `preflight` prints that it is using `plain snapshot` mode and keeps the effective publish version at the original `pom.xml` revision such as `1.2.3-SNAPSHOT`.

### 8.2 `publish`

Render the real Maven publish command:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="publish --directory /path/to/repo --tag v1.2.3"
```

Actually execute it:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="publish --directory /path/to/repo --tag v1.2.3 --execute true"
```

Snapshot publishing resolves the root `1.2.3-SNAPSHOT` into a unique publish revision such as `1.2.3-20260420.154500.abc1234-SNAPSHOT`, then injects it through `-Drevision=`. You can override the generated build stamp with `--snapshot-build-stamp` or the `JAVACHANGES_SNAPSHOT_BUILD_STAMP` environment variable.

If you pass `--snapshot-version-mode plain`, `publish` keeps the effective Maven version at the original snapshot revision such as `1.2.3-SNAPSHOT` instead of rewriting it to a stamped value. `preflight` and `publish` both print the active snapshot mode so CI logs show whether the run is `plain` or `stamped`.

Important Maven repository note:

- plain mode means the project version stays `1.2.3-SNAPSHOT`
- Maven and Nexus snapshot repositories still usually expand uploaded artifact filenames to timestamped snapshot files
- that timestamped filename expansion is standard Maven snapshot repository behavior, not a second rewrite by `javachanges`

GitLab CI defaults:

- if `CI_COMMIT_TAG` is present, `publish` uses it automatically, so a tag job can just run `publish --execute true`
- if the current branch matches `.changesets/config.json` or `.changesets/config.jsonc` `snapshotBranch`, `publish` and `preflight` default to snapshot mode
- if the repository config also sets `"snapshotVersionMode": "plain"`, the same GitLab snapshot-branch flow automatically uses plain snapshot mode
- this removes the need for CI shell wrappers that manually branch on tags vs snapshot branches

Important flags:

| Flag | Meaning |
| --- | --- |
| `--snapshot` | Publish the current snapshot instead of a release tag |
| `--snapshot-version-mode` | Snapshot version strategy: `stamped` or `plain` |
| `--snapshot-build-stamp` | Explicit snapshot publish stamp, overriding the default UTC timestamp + git short sha |
| `--tag` | Target release tag |
| `--module` | Restrict to one Maven artifactId or Gradle project name |
| `--allow-dirty` | Allow a dirty working tree |
| `--execute true` | Run the final publish command instead of only printing it |

### 8.3 `gradle-publish`

Render the Gradle publish command:

```bash
java -jar .javachanges/javachanges-__JAVACHANGES_LATEST_RELEASE_VERSION__.jar gradle-publish --directory /path/to/repo --tag v1.2.3
```

Actually execute it:

```bash
java -jar .javachanges/javachanges-__JAVACHANGES_LATEST_RELEASE_VERSION__.jar gradle-publish --directory /path/to/repo --tag v1.2.3 --execute true
```

Snapshot example:

```bash
java -jar .javachanges/javachanges-__JAVACHANGES_LATEST_RELEASE_VERSION__.jar gradle-publish --directory /path/to/repo --snapshot true
```

Custom task example:

```bash
java -jar .javachanges/javachanges-__JAVACHANGES_LATEST_RELEASE_VERSION__.jar gradle-publish --directory /path/to/repo --tag v1.2.3 --task publishAllPublicationsToMavenRepository
```

`gradle-publish` resolves the same release or snapshot version as `publish`, then renders `./gradlew --no-daemon publish -Pversion=...`. If `--module api` is provided, it renders `./gradlew --no-daemon :api:publish -Pversion=...`. Use `--task` to replace the final Gradle task name.

Important Gradle repository note:

- this command does not generate Maven `settings.xml`
- repository URLs and credentials should stay in the Gradle build or CI environment
- pass `--task` when the publication task is not named `publish`

## 9. Platform Release Commands

### 9.1 GitHub Release Commands

| Command | Purpose |
| --- | --- |
| `github-release-plan` | Create or update a GitHub release-plan pull request |
| `github-tag-from-plan` | Create and push the final release tag from a generated release plan |
| `github-release-from-plan` | Generate release metadata and optionally create or update the GitHub Release |
| `init-github-actions` | Write a minimal GitHub Actions workflow that wires release-plan, tag, publish, and GitHub Release jobs |

Examples:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="github-release-plan --directory /path/to/repo --github-repo owner/repo --execute true"
mvn -q -DskipTests compile exec:java -Dexec.args="github-tag-from-plan --directory /path/to/repo --execute true"
mvn -q -DskipTests compile exec:java -Dexec.args="github-release-from-plan --directory /path/to/repo --release-notes-file target/release-notes.md --execute true"
mvn -q -DskipTests compile exec:java -Dexec.args="github-release-from-plan --directory /path/to/repo --format json"
mvn -q -DskipTests compile exec:java -Dexec.args="init-github-actions --directory /path/to/repo --output .github/workflows/javachanges-release.yml --force true"
mvn -q -DskipTests compile exec:java -Dexec.args="init-github-actions --directory /path/to/gradle-repo --build-tool gradle --output .github/workflows/javachanges-release.yml --force true"
```

The GitHub release-plan, tag, and release commands also support `--format json` for CI-safe machine-readable output.

### 9.2 GitLab Release Commands

| Command | Purpose |
| --- | --- |
| `gitlab-release-plan` | Create or update a GitLab release-plan merge request |
| `gitlab-tag-from-plan` | Create the final release tag from a generated release plan |
| `gitlab-release` | Generate release notes and create or update the GitLab Release for the current tag |
| `init-gitlab-ci` | Write a minimal GitLab CI template that wires release-plan, tag, publish, and GitLab Release jobs |

Examples:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="gitlab-release-plan --directory /path/to/repo --project-id 12345 --execute true"
mvn -q -DskipTests compile exec:java -Dexec.args="gitlab-tag-from-plan --directory /path/to/repo --execute true"
mvn -q -DskipTests compile exec:java -Dexec.args="gitlab-release --directory /path/to/repo --execute true"
mvn -q -DskipTests compile exec:java -Dexec.args="init-gitlab-ci --directory /path/to/repo --output .gitlab-ci.yml --force true"
mvn -q -DskipTests compile exec:java -Dexec.args="init-gitlab-ci --directory /path/to/gradle-repo --build-tool gradle --output .gitlab-ci.yml --force true"
```

## 10. Help Output

You can always ask `picocli` for built-in help:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="--help"
mvn -q -DskipTests compile exec:java -Dexec.args="plan --help"
```

## 11. Related Guides

| Need | Document |
| --- | --- |
| First-time setup and local workflow | [Getting Started](./getting-started.md) |
| Local development workflow | [Development Guide](./development-guide.md) |
| Generated manifest files | [Release Plan Manifest](./release-plan-manifest.md) |
| GitHub Actions integration | [GitHub Actions Usage Guide](./github-actions-guide.md) |
| GitLab CI/CD integration | [GitLab CI/CD Usage Guide](./gitlab-ci-guide.md) |
