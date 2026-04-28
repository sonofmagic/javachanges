# basic-gradle-monorepo example

[English](./README.md) | [简体中文](./README.zh-CN.md)

This directory is a copy-ready Gradle example repository for `javachanges`.

It demonstrates a two-project Gradle monorepo with:

- official Changesets-style `.changesets/*.md` files using Gradle project names
- generated release-plan snapshots
- minimal GitHub Actions and GitLab CI templates
- Gradle-native publishing handoff through the release-plan manifest

## Layout

| Path | Purpose |
| --- | --- |
| `settings.gradle.kts` | Declares the root project and included Gradle projects |
| `gradle.properties` | Holds the root `version` read and updated by `javachanges` |
| `build.gradle.kts` | Shared Java and `maven-publish` setup for the example projects |
| `modules/core/build.gradle.kts` | Example `core` Gradle project |
| `modules/api/build.gradle.kts` | Example `api` Gradle project depending on `core` |
| `.changesets/20260418-add-release-notes.md` | Pending release intent in package-map format |
| `snapshots/` | Generated outputs after `plan --apply true` |
| `.github/workflows/` | Minimal GitHub Actions examples |
| `.gitlab-ci.yml` | Minimal GitLab CI example |
| `env/release.env.example` | Example publishing variables |

## Try the example from this source repository

Run these commands from the `javachanges` repository root:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="status --directory examples/basic-gradle-monorepo"
mvn -q -DskipTests compile exec:java -Dexec.args="plan --directory examples/basic-gradle-monorepo"
```

If you want to inspect the generated files without mutating the example tree, review the curated files under `snapshots/`.
The checked-in snapshots reflect the example after it has been copied into its own standalone Git repository. When you run it inside the `javachanges` source tree, the outer repository tags are still visible to Git-based release calculations.

## Snapshot files

| Path | Meaning |
| --- | --- |
| `snapshots/release-plan.json` | Machine-readable release manifest |
| `snapshots/release-plan.md` | Release PR body |
| `snapshots/CHANGELOG.after.md` | Expected changelog after plan application |
| `snapshots/gradle.properties.after` | Expected `gradle.properties` after the version advances |

## CI templates

The checked-in workflow files assume `javachanges` has been published and can be downloaded as an executable jar from Maven Central.

| Path | Purpose |
| --- | --- |
| `.github/workflows/ci.yml` | Build the Gradle repo and print pending release state |
| `.github/workflows/release-plan.yml` | Generate a reviewed release-plan pull request |
| `.github/workflows/tag-release.yml` | Tag and push the merged release commit with `github-tag-from-plan` |
| `.github/workflows/publish.yml` | Publish from the pushed release tag by reading the manifest and running `gradle publish` |
| `.gitlab-ci.yml` | Validate, create a release MR, tag from the plan, and publish with Gradle |

Replace `JAVACHANGES_VERSION`, repository URLs, credentials, and the example Gradle project metadata before copying these templates into a real repository.
