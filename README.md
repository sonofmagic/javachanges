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

Recommended for target repositories: declare the Maven plugin and run the short local goals:

```xml
<plugin>
  <groupId>io.github.sonofmagic</groupId>
  <artifactId>javachanges</artifactId>
  <version>1.3.1</version>
</plugin>
```

Then inside that repository:

```bash
mvn javachanges:status
mvn javachanges:plan -Djavachanges.apply=true
mvn javachanges:add -Djavachanges.summary="add release notes command" -Djavachanges.release=minor
mvn javachanges:manifest-field -Djavachanges.field=releaseVersion
mvn javachanges:run -Djavachanges.args="release-notes --tag v1.2.3"
```

The plugin defaults `--directory` to the current Maven project's `${project.basedir}`, so if you run it inside the target repository you usually do not need to pass `--directory` explicitly. The generic `run` goal still exists for commands that do not have a dedicated goal yet.

If you cannot modify the target repository `pom.xml`, use the released CLI from Maven Central instead:

```bash
mvn -q dependency:copy -Dartifact=io.github.sonofmagic:javachanges:1.3.1 -DoutputDirectory=.javachanges
java -jar .javachanges/javachanges-1.3.1.jar --help
```

On the current `main` branch, after installing the snapshot locally, you can also run `javachanges` as a Maven plugin:

```bash
mvn -q -DskipTests install
mvn io.github.sonofmagic:javachanges:1.3.1-SNAPSHOT:status
mvn io.github.sonofmagic:javachanges:1.3.1-SNAPSHOT:plan -Djavachanges.apply=true
mvn io.github.sonofmagic:javachanges:1.3.1-SNAPSHOT:add -Djavachanges.summary="add release notes command" -Djavachanges.release=minor
mvn io.github.sonofmagic:javachanges:1.3.1-SNAPSHOT:manifest-field -Djavachanges.field=releaseVersion
```

Repository-local shortcuts for working on `javachanges` itself:

```bash
pnpm snapshot:install
pnpm snapshot:preflight
pnpm snapshot:publish:local
pnpm docs:deploy:local
```

These map to the same phases used elsewhere in the docs:

- `snapshot:install` installs the current `1.3.1-SNAPSHOT` into local Maven
- `snapshot:preflight` previews a local snapshot publish with `local.dev.001`
- `snapshot:publish:local` publishes a unique snapshot through `central-publishing-maven-plugin`
- `docs:deploy:local` rebuilds `website/dist` and serves it through Wrangler locally

The snapshot shortcuts pin Maven's local repository to `.m2/repository` inside this repository so local bootstrap does not depend on a writable global `~/.m2`.

Published package:

- Maven Central page: `https://central.sonatype.com/artifact/io.github.sonofmagic/javachanges`
- CLI jar URL: `https://repo1.maven.org/maven2/io/github/sonofmagic/javachanges/1.3.1/javachanges-1.3.1.jar`

Released CLI examples against a target repository:

```bash
java -jar .javachanges/javachanges-1.3.1.jar status --directory /path/to/your/repo
java -jar .javachanges/javachanges-1.3.1.jar add --directory /path/to/your/repo
java -jar .javachanges/javachanges-1.3.1.jar plan --directory /path/to/your/repo
```

If you want to work on this repository itself from source, see [Development Guide](docs/development-guide.md).

## Changeset Format

`javachanges` now defaults to the same core markdown shape used by Node.js Changesets:

```md
---
"javachanges": minor
---

Add GitHub Actions release automation.
```

Monorepo example:

```md
---
"core": minor
"cli": patch
---

Improve CLI parsing and release planning.
```

How to read it:

- each frontmatter key is a Maven artifactId
- each value is the semver bump for that artifact: `patch`, `minor`, `major`
- the markdown body is the user-facing summary and notes

Defaults and behavior:

- `javachanges add` writes the official package-map style by default
- if you use `--modules all`, `javachanges` writes all detected Maven artifactIds
- the first non-empty body line becomes the summary shown in `status`, release PRs, changelogs, and release notes
- changelog sections are grouped by the aggregated release level: `major`, `minor`, `patch`

The shortest hand-written form for a single-module repository is:

```md
---
"javachanges": patch
---

Fix Windows path handling in release-notes generation.
```

Compatibility:

- the old `release` / `modules` / `summary` / `type` frontmatter format is still accepted when reading existing files
- new files should use the official package-map style above

Legacy format example:

```md
---
release: minor
type: ci
modules: javachanges
summary: automate javachanges self-release publishing via GitHub Actions
---
```

Field reference:

- `"artifactId"`
  Required in the new format.
  Each frontmatter key is a Maven artifactId, usually quoted to match official Changesets style.
  For single-module repositories, this is typically the root artifactId.
  For monorepos, add one entry per affected module.
- `patch` / `minor` / `major`
  Required value for each artifactId entry.
  Controls the semver bump contributed by that module.
  Allowed values: `patch`, `minor`, `major`.
  Typical guidance:
  `patch` for backwards-compatible fixes, docs, chores, CI, or small improvements.
  `minor` for backwards-compatible features.
  `major` for breaking changes.
- body
  Recommended markdown body below the frontmatter.
  The first non-empty line is reused as the summary.
  Additional paragraphs or bullets can hold rollout notes, migration details, or context.
- `type`
  Legacy optional metadata field, only for backwards compatibility with older `javachanges` files.
  New files should generally omit it.
- `release`, `modules`, `summary`
  Legacy fields from the older `javachanges` format.
  Still accepted for parsing existing files, but no longer recommended for new changesets.

## Repository Layout

- `src/main/java`: the CLI source code
- `docs/`: public documentation pages
- `examples/basic-monorepo/`: a minimal example target repository with CI templates and generated release-plan snapshots
- `website/`: the VitePress site published as static assets with Workers + Wrangler
- Cloudflare can connect this repository directly through Workers Builds, so GitHub does not need a dedicated docs deploy workflow
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
- [Examples Guide](docs/examples-guide.md)
- [Examples Guide (zh-CN)](docs/examples-guide.zh-CN.md)
- [Command Cookbook](docs/command-cookbook.md)
- [Command Cookbook (zh-CN)](docs/command-cookbook.zh-CN.md)
- [Configuration Reference](docs/configuration-reference.md)
- [Configuration Reference (zh-CN)](docs/configuration-reference.zh-CN.md)
- [CLI Reference](docs/cli-reference.md)
- [CLI Reference (zh-CN)](docs/cli-reference.zh-CN.md)
- [Output Contracts](docs/output-contracts.md)
- [Output Contracts (zh-CN)](docs/output-contracts.zh-CN.md)
- [Development Guide](docs/development-guide.md)
- [Development Guide (zh-CN)](docs/development-guide.zh-CN.md)
- [Release Plan Manifest](docs/release-plan-manifest.md)
- [Release Plan Manifest (zh-CN)](docs/release-plan-manifest.zh-CN.md)
- [Troubleshooting Guide](docs/troubleshooting-guide.md)
- [Troubleshooting Guide (zh-CN)](docs/troubleshooting-guide.zh-CN.md)
- [Cloudflare Workers Builds](docs/cloudflare-workers-builds.md)
- [Cloudflare Workers Builds (zh-CN)](docs/cloudflare-workers-builds.zh-CN.md)
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
