# Getting Started

## 1. Recommended: use the Maven plugin inside the target repository

Published coordinates:

- GroupId: `io.github.sonofmagic`
- ArtifactId: `javachanges`
- Current release: `__JAVACHANGES_LATEST_RELEASE_VERSION__`
- Maven Central page: `__JAVACHANGES_CENTRAL_OVERVIEW_URL__`
- CLI jar URL: `https://repo1.maven.org/maven2/io/github/sonofmagic/javachanges/__JAVACHANGES_LATEST_RELEASE_VERSION__/javachanges-__JAVACHANGES_LATEST_RELEASE_VERSION__.jar`

Add the plugin to the target repository `pom.xml`:

```xml
<plugin>
  <groupId>io.github.sonofmagic</groupId>
  <artifactId>javachanges</artifactId>
  <version>__JAVACHANGES_LATEST_RELEASE_VERSION__</version>
</plugin>
```

Then inside that repository, use the shortest local form:

```bash
mvn javachanges:status
mvn javachanges:plan -Djavachanges.apply=true
mvn javachanges:add -Djavachanges.summary="add release notes command" -Djavachanges.release=minor
mvn javachanges:manifest-field -Djavachanges.field=releaseVersion
```

Notes:

- this is the recommended day-to-day usage for target repositories
- the plugin defaults `--directory` to the current Maven project's `${project.basedir}`
- the generic `run` goal still exists for commands that do not have a dedicated goal yet

## 2. Alternative: use the released CLI when you cannot edit `pom.xml`

Download the released jar:

```bash
mvn -q dependency:copy -Dartifact=io.github.sonofmagic:javachanges:__JAVACHANGES_LATEST_RELEASE_VERSION__ -DoutputDirectory=.javachanges
```

Run the CLI help:

```bash
java -jar .javachanges/javachanges-__JAVACHANGES_LATEST_RELEASE_VERSION__.jar --help
```

Run it against a target repository:

```bash
java -jar .javachanges/javachanges-__JAVACHANGES_LATEST_RELEASE_VERSION__.jar status --directory /path/to/repo
java -jar .javachanges/javachanges-__JAVACHANGES_LATEST_RELEASE_VERSION__.jar add --directory /path/to/repo --summary "add release notes command" --release minor
java -jar .javachanges/javachanges-__JAVACHANGES_LATEST_RELEASE_VERSION__.jar plan --directory /path/to/repo
```

Notes:

- prefer the Maven plugin for day-to-day repository usage because it keeps commands short and auto-detects the current project directory
- keep the released CLI for temporary usage against repositories where you cannot add the plugin yet

## 3. Working on the current `main` branch

```bash
mvn -q -DskipTests install
mvn io.github.sonofmagic:javachanges:__JAVACHANGES_CURRENT_SNAPSHOT_VERSION__:status
mvn io.github.sonofmagic:javachanges:__JAVACHANGES_CURRENT_SNAPSHOT_VERSION__:plan -Djavachanges.apply=true
mvn io.github.sonofmagic:javachanges:__JAVACHANGES_CURRENT_SNAPSHOT_VERSION__:add -Djavachanges.summary="add release notes command" -Djavachanges.release=minor
mvn io.github.sonofmagic:javachanges:__JAVACHANGES_CURRENT_SNAPSHOT_VERSION__:manifest-field -Djavachanges.field=releaseVersion
```

Notes:

- dedicated goals now exist for `status`, `plan`, `add`, and `manifest-field`
- `javachanges:run` is still available with `-Djavachanges.args="..."`

## 4. Prepare a target repository

Your target repository should have:

- git initialized
- a root `pom.xml`
- a `<revision>` property
- a `CHANGELOG.md` file, or let `javachanges` create/update it during plan application
- either `<modules>` in the root pom, or a single root artifact

## 5. Create a changeset

Monorepo example:

```bash
mvn javachanges:add -Djavachanges.summary="add release notes command" -Djavachanges.release=minor -Djavachanges.modules=core
```

Single-module example:

```bash
mvn javachanges:add -Djavachanges.summary="add release notes command" -Djavachanges.release=minor
```

This writes a markdown file into `.changesets/`.

Shortest hand-written format:

```md
---
"your-artifact-id": patch
---

Fix release-notes rendering.
```

Monorepo example:

```md
---
"core": minor
"cli": patch
---

Improve CLI parsing and release planning.
```

Notes:

- `javachanges add` writes this official Changesets-style package map by default
- the first non-empty body line becomes the summary used by `status`, changelogs, and release notes
- legacy `release` / `modules` / `summary` frontmatter is still read for compatibility, but new files should use the package-map form
- changelog sections are grouped by the aggregated release level: `major`, `minor`, `patch`

## 6. Inspect the plan

```bash
mvn javachanges:plan
```

## 7. Apply the plan

```bash
mvn javachanges:plan -Djavachanges.apply=true
```

That updates:

- the root `revision`
- `CHANGELOG.md`
- `.changesets/release-plan.json`
- `.changesets/release-plan.md`

## 8. Running from source during development

If you are working on the `javachanges` repository itself, use the source-driven development flow instead:

```bash
mvn -q test
mvn -q -DskipTests compile exec:java -Dexec.args="status --directory /path/to/your/repo"
```

For the full development workflow, see [Development Guide](./development-guide.md).
