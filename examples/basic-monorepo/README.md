# basic-monorepo example

[English](./README.md) | [简体中文](./README.zh-CN.md)

This directory is a copy-ready example repository for `javachanges`.

It demonstrates a two-module Maven monorepo with:

- official Changesets-style `.changesets/*.md` files
- generated release-plan snapshots
- minimal GitHub Actions and GitLab CI templates
- a small `env/release.env.example` template for repository publishing

## Layout

| Path | Purpose |
| --- | --- |
| `pom.xml` | Root Maven aggregator with a shared `revision` |
| `modules/core/pom.xml` | Example `core` module |
| `modules/api/pom.xml` | Example `api` module |
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

## Snapshot files

| Path | Meaning |
| --- | --- |
| `snapshots/release-plan.json` | Machine-readable release manifest |
| `snapshots/release-plan.md` | Release PR body |
| `snapshots/CHANGELOG.after.md` | Expected changelog after plan application |
| `snapshots/pom.after.xml` | Expected root `pom.xml` after the root revision advances |

## CI templates

The checked-in workflow files assume `javachanges` has been published and can be downloaded as an executable jar from Maven Central.

| Path | Purpose |
| --- | --- |
| `.github/workflows/ci.yml` | Build the Maven repo and print pending release state |
| `.github/workflows/release-plan.yml` | Generate a reviewed release-plan pull request |
| `.github/workflows/publish.yml` | Run `preflight`, generate `.m2/settings.xml`, and publish |
| `.gitlab-ci.yml` | Validate, create a release MR, tag from the plan, and publish |

Update `JAVACHANGES_VERSION`, repository URLs, and credentials before copying these templates into a real repository.
