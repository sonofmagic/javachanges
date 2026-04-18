# javachanges

`javachanges` is a release-planning CLI for Maven monorepos.

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

- a Maven monorepo with a root `pom.xml`
- a `<modules>` section in that root pom
- a root `revision` property used for versioning
- a `.changesets/` directory to store release notes-in-progress

