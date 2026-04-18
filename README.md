# javachanges

[English](./README.md) | [简体中文](./README.zh-CN.md)

`javachanges` is a small Java CLI that brings a Changesets-like release planning workflow to Maven monorepos and single-module Maven repositories.

Documentation site: `https://javachanges.icebreaker.top`

It is designed for repositories that want:

- file-based release intents in `.changesets/*.md`
- reviewed release plans before version bumps land
- generated changelog and release notes
- CI-friendly publish checks and release helpers
- optional GitHub and GitLab variable sync helpers

## Status

This repository is the standalone source home for `javachanges`.

The current codebase focuses on:

- adding and validating changesets
- generating release plans
- updating the root `revision`
- generating changelog and release notes
- preparing Maven settings from environment variables
- release preflight and publish helpers
- GitHub and GitLab environment-variable auditing
- GitLab release-MR and tag automation helpers

## Quick Start

Requirements:

- Java 8+
- Maven 3.8+
- a git repository
- a Maven repository with a root `pom.xml`
- either `<modules>` in the root pom, or a single root artifact

Build the CLI:

```bash
mvn -q test
```

Run it against a target repository:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="status --directory /path/to/your/repo"
mvn -q -DskipTests compile exec:java -Dexec.args="add --directory /path/to/your/repo"
mvn -q -DskipTests compile exec:java -Dexec.args="plan --directory /path/to/your/repo"
```

## Changeset Format

The shortest recommended changeset only needs `release` and `summary`:

```md
---
release: minor
summary: add GitHub Actions release automation
---

- Add CI, release-plan PR creation, and publish workflows.
```

Defaults:

- `modules` defaults to `all`
- if `summary` is omitted, `javachanges` falls back to the first non-empty body line
- changelog sections are grouped by `release`: `major`, `minor`, `patch`

That means this shorter form also works:

```md
---
release: patch
---

Fix Windows path handling in release-notes generation.
```

Use the full format only when you need to override the defaults:

```md
---
release: minor
type: ci
modules: javachanges
summary: automate javachanges self-release publishing via GitHub Actions
---
```

Field reference:

- `release`
  Required. Controls the semver bump for the aggregated release plan.
  Allowed values: `patch`, `minor`, `major`.
  Typical guidance:
  `patch` for backwards-compatible fixes, docs, chores, CI, or small improvements.
  `minor` for backwards-compatible features.
  `major` for breaking changes.
- `summary`
  Optional but strongly recommended.
  Used in `status` output, release PR content, changelog sections, and generated release notes.
  Keep it short, imperative, and user-facing.
  If omitted, `javachanges` falls back to the first non-empty body line.
- `type`
  Optional.
  Used as extra classification metadata when you want to distinguish entries such as `ci`, `docs`, or `fix`.
  Allowed values: `feat`, `fix`, `docs`, `build`, `ci`, `test`, `refactor`, `perf`, `chore`, `other`.
  Changelog headings are primarily grouped by `release`, not by `type`.
  If you do not care about this extra label, you can skip it.
- `modules`
  Optional. Defaults to `all`.
  For Maven monorepos, this can be a comma-separated list of artifactIds such as `core, api`.
  For single-module repositories, you usually do not need to write it.
  Use it only when you want the release plan to record which Maven modules are affected.
- body
  Optional free-form Markdown below the frontmatter.
  The first non-empty line may be reused as the fallback summary.
  In changelog rendering, the first body line may appear after the summary to give extra context.

## Repository Layout

- `src/main/java`: the CLI source code
- `docs/`: public documentation pages
- `examples/basic-monorepo/`: a minimal example target repository
- `website/`: a simple static landing page that can be published as GitHub Pages
- `env/release.env.example`: a generic env template for release automation

## Commands

High-value commands:

- `add`
- `status`
- `plan`
- `write-settings`
- `init-env`
- `render-vars`
- `doctor-local`
- `doctor-platform`
- `sync-vars`
- `audit-vars`
- `preflight`
- `publish`
- `release-notes`

GitLab-specific helpers:

- `gitlab-release-plan`
- `gitlab-tag-from-plan`

## Docs

- [Overview](docs/index.md)
- [Overview (zh-CN)](docs/index.zh-CN.md)
- [Getting Started](docs/getting-started.md)
- [Getting Started (zh-CN)](docs/getting-started.zh-CN.md)
- [Development Guide](docs/development-guide.md)
- [Development Guide (zh-CN)](docs/development-guide.zh-CN.md)
- [GitHub Actions Release Flow](docs/github-actions-release.md)
- [GitHub Actions Release Flow (zh-CN)](docs/github-actions-release.zh-CN.md)
- [GitHub Actions Usage Guide](docs/github-actions-guide.md)
- [GitHub Actions Usage Guide (zh-CN)](docs/github-actions-guide.zh-CN.md)
- [GitLab CI/CD Usage Guide](docs/gitlab-ci-guide.md)
- [GitLab CI/CD Usage Guide (zh-CN)](docs/gitlab-ci-guide.zh-CN.md)
- [Publish To Maven Central](docs/publish-to-maven-central.md)
- [Publish To Maven Central (zh-CN)](docs/publish-to-maven-central.zh-CN.md)
- [Use Cases](docs/use-cases.md)
- [Use Cases (zh-CN)](docs/use-cases.zh-CN.md)

## License

Apache-2.0
