# Examples Guide

[English](/examples-guide) | [简体中文](/zh-CN/examples-guide)

## 1. Overview

This guide explains how to use the checked-in example repository under `examples/basic-monorepo/`.

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
"core": minor
"api": minor
---

Add release notes generation workflow.

- Demonstrates a feature changeset for a two-module Maven monorepo.
- Shows how `javachanges plan` aggregates release metadata.
```

Read it as:

- `core` and `api` are Maven artifactIds
- `minor` is the release bump contributed by each package
- the first non-empty body line becomes the summary
- extra bullets become changelog detail

## 4. Snapshot walkthrough

Run the example from the `javachanges` source repository root:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="status --directory examples/basic-monorepo"
mvn -q -DskipTests compile exec:java -Dexec.args="plan --directory examples/basic-monorepo --apply true"
```

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
| `modules` | `core`, `api` |

## 5. GitHub Actions example

The example repository includes three GitHub Actions templates:

| File | Purpose |
| --- | --- |
| `examples/basic-monorepo/.github/workflows/ci.yml` | Builds the Maven repo and runs `status` |
| `examples/basic-monorepo/.github/workflows/release-plan.yml` | Applies the plan and opens a release PR |
| `examples/basic-monorepo/.github/workflows/publish.yml` | Runs `preflight`, writes Maven settings, and publishes from a tag |

These templates assume:

- `javachanges` is downloaded as a jar from Maven Central
- the pinned version is controlled by `JAVACHANGES_VERSION`
- Maven credentials come from GitHub Actions variables and secrets
- `actions/setup-java` uses `cache: maven`

This makes the example copy-friendly for a target repository that does not vendor the `javachanges` source tree.

## 6. GitLab CI example

`examples/basic-monorepo/.gitlab-ci.yml` mirrors the same lifecycle:

1. `verify`
2. `release-plan`
3. `tag`
4. `publish`

The template downloads the `javachanges` jar in `before_script`, reuses the Maven dependency cache, and then runs:

- `status` during validation
- `gitlab-release-plan --execute true` on the default branch
- `gitlab-tag-from-plan --execute true` after a release plan merge
- `preflight`, `write-settings`, and `publish --execute true` for tag pipelines

## 7. How to adapt the example

When copying the example into a real repository:

1. replace the example `groupId`, artifactIds, and module paths
2. update `.changesets/*.md` to match your real Maven artifactIds
3. replace repository URLs and credentials in `env/release.env.example`
4. pin the `JAVACHANGES_VERSION` you want CI to use
5. run `status` and `plan` locally before enabling automatic release jobs

## 8. Related guides

| Need | Document |
| --- | --- |
| Local setup and first release plan | [Getting Started](./getting-started.md) |
| CLI command details | [CLI Reference](./cli-reference.md) |
| Generated manifest fields | [Release Plan Manifest](./release-plan-manifest.md) |
| Full GitHub Actions setup | [GitHub Actions Usage Guide](./github-actions-guide.md) |
| Full GitLab CI/CD setup | [GitLab CI/CD Usage Guide](./gitlab-ci-guide.md) |
