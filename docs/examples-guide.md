---
description: Walk through Maven and Gradle javachanges examples and map each file to the release flow.
---

# Examples Guide


## 1. Overview

This guide explains how to use the checked-in Maven example repository under `examples/basic-monorepo/` and how to translate the same flow to Gradle.

The example is intentionally small, but it covers the whole `javachanges` flow:

| Path | Purpose |
| --- | --- |
| `examples/basic-monorepo/pom.xml` | Root Maven monorepo with a shared `revision` |
| `examples/basic-monorepo/.changesets/` | Pending release intent files |
| `examples/basic-monorepo/snapshots/` | Curated outputs after `plan --apply true` |
| `examples/basic-monorepo/.github/workflows/` | Minimal GitHub Actions templates |
| `examples/basic-monorepo/.gitlab-ci.yml` | Minimal GitLab CI template |

Use this example when you want a concrete reference instead of reading each guide in isolation.

## 2. Repository shape

The example repository contains two Maven modules:

```text
examples/basic-monorepo/
├── .changesets/
├── .github/workflows/
├── env/
├── modules/
│   ├── api/
│   └── core/
├── snapshots/
├── .gitlab-ci.yml
├── CHANGELOG.md
└── pom.xml
```

Important assumptions:

- the root `pom.xml` owns the shared `revision`
- the root `pom.xml` lists `modules/core` and `modules/api`
- `CHANGELOG.md` exists before plan application
- pending release intent lives under `.changesets/*.md`

## 3. Example changeset format

The example changeset uses the official Changesets-style package map:

```md
---
"javachanges-basic-monorepo-core": minor
"javachanges-basic-monorepo-api": minor
---

Add release notes generation workflow.

- Demonstrates a feature changeset for a two-module Maven monorepo.
- Shows how `javachanges plan` aggregates release metadata.
```

Read it as:

- `javachanges-basic-monorepo-core` and `javachanges-basic-monorepo-api` are Maven artifactIds
- `minor` is the release bump contributed by each package
- the first non-empty body line becomes the summary
- extra bullets become changelog detail

## 4. Snapshot walkthrough

Run the example from the `javachanges` source repository root:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="status --directory examples/basic-monorepo"
mvn -q -DskipTests compile exec:java -Dexec.args="plan --directory examples/basic-monorepo --apply true"
```

When you run the example in-place under the `javachanges` source tree, Git-aware version calculations can still see the outer repository tags.
The curated `snapshots/` values below reflect the example after it has been copied into its own standalone Git repository.

The curated `snapshots/` directory shows the expected generated artifacts:

| Snapshot file | What it shows |
| --- | --- |
| `examples/basic-monorepo/snapshots/release-plan.json` | Machine-readable release metadata |
| `examples/basic-monorepo/snapshots/release-plan.md` | The release PR body |
| `examples/basic-monorepo/snapshots/CHANGELOG.after.md` | The changelog section produced for the release |
| `examples/basic-monorepo/snapshots/pom.after.xml` | The root `revision` after the release and next snapshot bump |

The release-plan snapshot corresponds to:

| Field | Example value |
| --- | --- |
| `releaseVersion` | `0.2.0` |
| `nextSnapshotVersion` | `0.2.0-SNAPSHOT` |
| `releaseLevel` | `minor` |
| `modules` | `javachanges-basic-monorepo-core`, `javachanges-basic-monorepo-api` |

## 5. GitHub Actions example

The example repository includes four GitHub Actions templates:

| File | Purpose |
| --- | --- |
| `examples/basic-monorepo/.github/workflows/ci.yml` | Builds the Maven repo and runs `status` |
| `examples/basic-monorepo/.github/workflows/release-plan.yml` | Creates or updates a release PR with `github-release-plan` |
| `examples/basic-monorepo/.github/workflows/tag-release.yml` | Tags the merged release commit with `github-tag-from-plan` |
| `examples/basic-monorepo/.github/workflows/publish.yml` | Publishes from the pushed release tag with `publish --execute true` |

These templates assume:

- `javachanges` is downloaded as a jar from Maven Central
- the pinned version is controlled by `JAVACHANGES_VERSION`
- Maven credentials come from GitHub Actions variables and secrets
- `actions/setup-java` uses `cache: maven`
- the example POM coordinates are unique enough to be publish-safe until you replace them

This makes the example copy-friendly for a target repository that does not vendor the `javachanges` source tree.

## 6. GitLab CI example

`examples/basic-monorepo/.gitlab-ci.yml` mirrors the same lifecycle:

1. `verify`
2. `release-plan`
3. `tag`
4. `publish`

The checked-in template now uses the official Maven plugin entrypoint directly, reuses the Maven dependency cache, and keeps each job to one `javachanges` command:

- `status` during validation
- `gitlab-release-plan --execute true` on the default branch
- `gitlab-tag-from-plan --execute true` after a release plan merge
- `publish --execute true` for both snapshot and tag pipelines, with preflight, tag detection, snapshot-branch detection, and settings generation handled inside the command
- `gitlab-release --execute true` to create or update the GitLab Release after a successful tag publish

## 7. Gradle equivalent

A Gradle repository uses the same `.changesets/`, `CHANGELOG.md`, and release-plan files. The build model files change:

```text
sample-gradle-monorepo/
├── .changesets/
├── modules/
│   ├── api/
│   └── core/
├── CHANGELOG.md
├── build.gradle.kts
├── gradle.properties
└── settings.gradle.kts
```

`gradle.properties`:

```properties
version=0.2.0-SNAPSHOT
```

`settings.gradle.kts`:

```kotlin
rootProject.name = "sample-gradle-monorepo"
include(":core", ":api")
```

Equivalent Gradle changeset:

```md
---
"core": minor
"api": minor
---

Add release notes generation workflow.
```

Run with the released CLI jar:

```bash
java -jar .javachanges/javachanges-__JAVACHANGES_LATEST_RELEASE_VERSION__.jar status --directory sample-gradle-monorepo
java -jar .javachanges/javachanges-__JAVACHANGES_LATEST_RELEASE_VERSION__.jar plan --directory sample-gradle-monorepo --apply true
```

The Gradle plan updates `gradle.properties` instead of `pom.xml`. GitHub/GitLab release-plan automation stages `gradle.properties`, `CHANGELOG.md`, and `.changesets/`.

For actual artifact publishing, keep using Gradle:

```bash
RELEASE_VERSION="$(java -jar .javachanges/javachanges-__JAVACHANGES_LATEST_RELEASE_VERSION__.jar manifest-field --directory sample-gradle-monorepo --field releaseVersion)"
cd sample-gradle-monorepo
./gradlew publish -Pversion="$RELEASE_VERSION"
```

## 8. How to adapt the example

When copying the example into a real repository:

1. replace the example `groupId`, artifactIds, and module paths
2. update `.changesets/*.md` to match your real Maven artifactIds or Gradle project names
3. replace repository URLs and credentials in `env/release.env.example`
4. pin the `JAVACHANGES_VERSION` you want CI to use
5. run `status` and `plan` locally before enabling automatic release jobs

## 9. Related guides

| Need | Document |
| --- | --- |
| Local setup and first release plan | [Getting Started](./getting-started.md) |
| Gradle repository setup | [Gradle Usage Guide](./gradle-guide.md) |
| CLI command details | [CLI Reference](./cli-reference.md) |
| Generated manifest fields | [Release Plan Manifest](./release-plan-manifest.md) |
| Full GitHub Actions setup | [GitHub Actions Usage Guide](./github-actions-guide.md) |
| Full GitLab CI/CD setup | [GitLab CI/CD Usage Guide](./gitlab-ci-guide.md) |
