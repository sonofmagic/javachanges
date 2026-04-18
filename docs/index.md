# javachanges

[English](./index.md) | [简体中文](./index.zh-CN.md)

`javachanges` is a release-planning CLI for Maven monorepos and single-module Maven repositories.

The workflow is intentionally simple:

1. contributors record intended changes in `.changesets/*.md`
2. CI or maintainers inspect a generated release plan
3. the plan updates the root version and changelog
4. publish helpers prepare Maven settings and deploy commands

The tool stays file-centric. It does not require a database or a hosted service.

## Core ideas

- Keep release intent in versioned files.
- Review release plans before publishing.
- Generate changelogs from structured metadata.
- Avoid shell-heavy release logic where possible.

## What the CLI assumes

- a Maven repository with a root `pom.xml`
- either `<modules>` in the root pom, or a single root artifact
- a root `revision` property used for versioning
- a `.changesets/` directory to store release notes-in-progress
