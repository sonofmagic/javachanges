# javachanges

`javachanges` is a small Java CLI that brings a Changesets-like release planning workflow to Maven monorepos.

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
- a Maven monorepo with a root `pom.xml` and `<modules>`

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
- [Getting Started](docs/getting-started.md)
- [Use Cases](docs/use-cases.md)

## License

Apache-2.0
