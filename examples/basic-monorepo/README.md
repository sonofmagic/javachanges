# basic-monorepo example

[English](./README.md) | [简体中文](./README.zh-CN.md)

This directory is a copy-ready example repository for `javachanges`.

It demonstrates a two-module Maven monorepo with:

- official Changesets-style `.changesets/*.md` files
- generated release-plan snapshots
- minimal GitHub Actions and GitLab CI templates that publish through CI/CD
- a small `env/release.env.example` template for repository publishing

## Layout

| Path | Purpose |
| --- | --- |
| `pom.xml` | Root Maven aggregator with a shared `revision` and publish-ready `distributionManagement` |
| `modules/core/pom.xml` | Example `javachanges-basic-monorepo-core` module |
| `modules/api/pom.xml` | Example `javachanges-basic-monorepo-api` module |
| `.changesets/20260418-add-release-notes.md` | Pending release intent in package-map format |
| `snapshots/` | Generated outputs after `plan --apply true` |
| `.github/workflows/` | Minimal GitHub Actions examples |
| `.gitlab-ci.yml` | Minimal GitLab CI example |
| `env/release.env.example` | Example Maven publishing variables |

## Try the example from this source repository

Run these commands from the `javachanges` repository root:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="status --directory examples/basic-monorepo"
mvn -q -DskipTests compile exec:java -Dexec.args="plan --directory examples/basic-monorepo"
```

If you want to inspect the generated files without mutating the example tree, review the curated files under `snapshots/`.
The checked-in snapshots reflect the example after it has been copied into its own standalone Git repository. When you run it inside the `javachanges` source tree, the outer repository tags are still visible to Git-based release calculations.

## Snapshot files

| Path | Meaning |
| --- | --- |
| `snapshots/release-plan.json` | Machine-readable release manifest |
| `snapshots/release-plan.md` | Release PR body |
| `snapshots/CHANGELOG.after.md` | Expected changelog after plan application |
| `snapshots/pom.after.xml` | Expected root `pom.xml` after the root revision advances |

## CI templates

The checked-in workflow files assume `javachanges` has been published and can be downloaded as an executable jar from Maven Central.
The example coordinates are intentionally unique enough to be publish-safe, but you should still replace them when copying this repository into a real project.

| Path | Purpose |
| --- | --- |
| `.github/workflows/ci.yml` | Build the Maven repo and print pending release state |
| `.github/workflows/release-plan.yml` | Generate a reviewed release-plan pull request |
| `.github/workflows/tag-release.yml` | Tag and push the merged release commit with `github-tag-from-plan` |
| `.github/workflows/publish.yml` | Publish from the pushed release tag with `publish --execute true` |
| `.gitlab-ci.yml` | Validate, create a release MR, tag from the plan, and publish |

Replace `JAVACHANGES_VERSION`, repository URLs, credentials, and the example Maven coordinates before copying these templates into a real repository.
