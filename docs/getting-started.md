# Getting Started

## 1. Install from Maven Central

Published coordinates:

- GroupId: `io.github.sonofmagic`
- ArtifactId: `javachanges`
- Current release: `1.2.0`
- Maven Central page: `https://central.sonatype.com/artifact/io.github.sonofmagic/javachanges`
- Direct jar URL: `https://repo1.maven.org/maven2/io/github/sonofmagic/javachanges/1.2.0/javachanges-1.2.0.jar`

Download the released jar:

```bash
mvn -q dependency:copy -Dartifact=io.github.sonofmagic:javachanges:1.2.0 -DoutputDirectory=.javachanges
```

Run the CLI:

```bash
java -jar .javachanges/javachanges-1.2.0.jar --help
```

Run the released package as a Maven plugin:

```bash
mvn io.github.sonofmagic:javachanges:1.2.0:run -Djavachanges.command=status
mvn io.github.sonofmagic:javachanges:1.2.0:run -Djavachanges.args="add --summary 'add release notes command' --release minor"
```

Notes:

- `javachanges:run` defaults `--directory` to `${project.basedir}`
- `-Djavachanges.command=status` is the shortest structured form
- `-Djavachanges.args="..."` lets you pass raw CLI flags when you need the full command line shape

## 2. Prepare a target repository

Your target repository should have:

- git initialized
- a root `pom.xml`
- a `<revision>` property
- a `CHANGELOG.md` file, or let `javachanges` create/update it during plan application
- either `<modules>` in the root pom, or a single root artifact

## 3. Create a changeset

Monorepo example:

```bash
java -jar .javachanges/javachanges-1.2.0.jar add --directory /path/to/repo --summary "add release notes command" --release minor --modules core
```

Single-module example:

```bash
java -jar .javachanges/javachanges-1.2.0.jar add --directory /path/to/repo --summary "add release notes command" --release minor
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

## 4. Inspect the plan

```bash
java -jar .javachanges/javachanges-1.2.0.jar plan --directory /path/to/repo
```

## 5. Apply the plan

```bash
java -jar .javachanges/javachanges-1.2.0.jar plan --directory /path/to/repo --apply true
```

That updates:

- the root `revision`
- `CHANGELOG.md`
- `.changesets/release-plan.json`
- `.changesets/release-plan.md`

## 6. Running from source during development

If you are working on the `javachanges` repository itself, use the source-driven development flow instead:

```bash
mvn -q test
mvn -q -DskipTests compile exec:java -Dexec.args="status --directory /path/to/your/repo"
```

For the full development workflow, see [Development Guide](./development-guide.md).
