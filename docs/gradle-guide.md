---
description: Use javachanges with Gradle single-project builds and multi-project builds.
---

# Gradle Usage Guide


## 1. What Gradle support covers

`javachanges` can plan releases for Gradle repositories without requiring a Maven `pom.xml`.

The Gradle path supports:

- repository root detection from `gradle.properties` plus `settings.gradle`, `settings.gradle.kts`, `build.gradle`, or `build.gradle.kts`
- current version reading from `gradle.properties`
- version updates during `plan --apply true`
- package detection from Gradle `include(...)` entries
- single-project repositories using the root project name
- changesets, status output, changelog generation, release-plan manifests, GitHub release PRs, and GitLab release MRs

The Maven publishing helper commands, `preflight` and `publish`, still render Maven deploy commands and Maven `settings.xml`. Use Gradle-native publishing tasks for Gradle artifacts, while using `javachanges` for planning, changelog, manifests, and release automation.

## 2. Required Gradle shape

A Gradle repository should keep the root version in `gradle.properties`:

```properties
version=1.4.0-SNAPSHOT
```

`javachanges` also accepts this fallback key:

```properties
revision=1.4.0-SNAPSHOT
```

Prefer `version` for new Gradle repositories because it matches the standard Gradle project property.

Single-project Gradle repositories should have one of:

```text
gradle.properties
settings.gradle
build.gradle
```

or:

```text
gradle.properties
settings.gradle.kts
build.gradle.kts
```

Multi-project Gradle repositories should declare included projects in `settings.gradle` or `settings.gradle.kts`.

Groovy DSL:

```groovy
rootProject.name = 'payments'
include 'api', 'core'
```

Kotlin DSL:

```kotlin
rootProject.name = "payments"
include(":api", ":core")
```

Nested project paths are supported. `include(":tools:cli")` is exposed to changesets as `cli`.

## 3. Install and run the CLI

Gradle repositories should use the released CLI jar. The Maven plugin is still useful for Maven repositories, but Gradle builds should call the CLI directly.

Download the jar:

```bash
mvn -q dependency:copy \
  -Dartifact=io.github.sonofmagic:javachanges:__JAVACHANGES_LATEST_RELEASE_VERSION__ \
  -DoutputDirectory=.javachanges
```

Set a helper variable:

```bash
export JAVACHANGES="java -jar .javachanges/javachanges-__JAVACHANGES_LATEST_RELEASE_VERSION__.jar"
```

Run against the Gradle repository:

```bash
$JAVACHANGES status --directory .
```

When developing `javachanges` itself from this source checkout, you can target a Gradle repository with:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="status --directory /path/to/gradle-repo"
```

## 4. Single-project Gradle workflow

Example repository:

```text
my-library/
├── .changesets/
├── CHANGELOG.md
├── build.gradle.kts
├── gradle.properties
└── settings.gradle.kts
```

`settings.gradle.kts`:

```kotlin
rootProject.name = "my-library"
```

`gradle.properties`:

```properties
version=0.8.0-SNAPSHOT
```

Create a patch changeset:

```bash
$JAVACHANGES add --directory . \
  --summary "fix generated release notes" \
  --release patch
```

The generated changeset uses the root project name:

```md
---
"my-library": patch
---

fix generated release notes
```

Inspect and apply:

```bash
$JAVACHANGES status --directory .
$JAVACHANGES plan --directory .
$JAVACHANGES plan --directory . --apply true
```

After apply:

- `gradle.properties` advances to the next snapshot version
- `CHANGELOG.md` gets a new release section
- `.changesets/release-plan.json` is written
- `.changesets/release-plan.md` is written
- consumed `.changesets/*.md` files are deleted

## 5. Multi-project Gradle workflow

Example `settings.gradle.kts`:

```kotlin
rootProject.name = "payments"
include(":api", ":core", ":tools:cli")
```

Detected package names:

| Gradle project path | Changeset package key |
| --- | --- |
| `:api` | `api` |
| `:core` | `core` |
| `:tools:cli` | `cli` |

Create one changeset affecting two projects:

```bash
$JAVACHANGES add --directory . \
  --summary "add payment retry metadata" \
  --release minor \
  --modules api,core
```

Hand-written format:

```md
---
"api": minor
"core": minor
---

add payment retry metadata
```

Use `--modules all` when the changeset affects every detected Gradle project:

```bash
$JAVACHANGES add --directory . \
  --summary "standardize Gradle publication metadata" \
  --release patch \
  --modules all
```

## 6. CI release-plan automation

In GitHub Actions or GitLab CI, use the same CLI commands as Maven repositories for planning:

```bash
java -jar .javachanges/javachanges-__JAVACHANGES_LATEST_RELEASE_VERSION__.jar \
  github-release-plan \
  --directory "$GITHUB_WORKSPACE" \
  --github-repo "$GITHUB_REPOSITORY" \
  --execute true
```

For GitLab:

```bash
java -jar .javachanges/javachanges-__JAVACHANGES_LATEST_RELEASE_VERSION__.jar \
  gitlab-release-plan \
  --directory "$CI_PROJECT_DIR" \
  --project-id "$CI_PROJECT_ID" \
  --execute true
```

Release-plan automation stages `gradle.properties`, `CHANGELOG.md`, and `.changesets/` for Gradle repositories.

Tagging from an applied plan can use fresh metadata:

```bash
$JAVACHANGES github-tag-from-plan --directory . --fresh true --execute true
$JAVACHANGES gitlab-tag-from-plan --directory . --fresh true --execute true
```

## 7. Publishing Gradle artifacts

Use `gradle-publish` as the dry-run handoff point to Gradle-native publishing:

```bash
$JAVACHANGES gradle-publish --directory . --tag v1.4.0
```

Execute the rendered Gradle command only after reviewing it:

```bash
$JAVACHANGES gradle-publish --directory . --tag v1.4.0 --execute true
```

Snapshot publishing works the same way:

```bash
$JAVACHANGES gradle-publish --directory . --snapshot true
```

For a single Gradle project, pass the detected project name:

```bash
$JAVACHANGES gradle-publish --directory . --snapshot true --module api
```

The command renders `./gradlew --no-daemon publish -Pversion=...`, or `./gradlew --no-daemon :api:publish -Pversion=...` when `--module api` is set. It does not manage Gradle repository credentials; keep credentials and publication repositories in the Gradle build or CI environment.

If your Gradle publication task has a different name, pass `--task`:

```bash
$JAVACHANGES gradle-publish --directory . --tag v1.4.0 --task publishAllPublicationsToMavenRepository
```

If your Gradle build already reads `version` from `gradle.properties`, the applied release plan has already updated that file to the next snapshot. Use fresh release metadata for release tags and release notes, and keep the actual publication logic inside your Gradle build.

## 8. Common mistakes

| Symptom | Cause | Fix |
| --- | --- | --- |
| `Cannot find repository root` | missing `gradle.properties`, or no Gradle settings/build file exists | add `gradle.properties` and `settings.gradle(.kts)` or `build.gradle(.kts)` |
| `Cannot find version or revision` | `gradle.properties` has no supported version key | add `version=1.0.0-SNAPSHOT` |
| `Unknown module` | changeset key does not match detected project names | use the final segment of `include(...)`, such as `cli` for `:tools:cli` |
| `publish` renders Maven deploy | `preflight` and `publish` are Maven-specific helpers | use `gradle-publish` for Gradle artifact publishing |

## 9. Related guides

| Need | Document |
| --- | --- |
| First setup flow | [Getting Started](./getting-started.md) |
| Maven repository flow | [Maven Usage Guide](./maven-guide.md) |
| Command details | [CLI Reference](./cli-reference.md) |
| Copy-ready command sequences | [Command Cookbook](./command-cookbook.md) |
| Generated release manifest | [Release Plan Manifest](./release-plan-manifest.md) |
| CI release PR/MR automation | [GitHub Actions Usage Guide](./github-actions-guide.md) and [GitLab CI/CD Usage Guide](./gitlab-ci-guide.md) |
