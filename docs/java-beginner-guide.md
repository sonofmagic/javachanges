# Java Beginner Guide To javachanges

## 1. Overview

This guide is for Java beginners who want to learn from the `javachanges` repository itself.

Instead of focusing only on command usage, it explains the main knowledge areas covered by this codebase:

| Topic | What you learn |
| --- | --- |
| Java basics | classes, objects, enums, interfaces, exceptions, collections |
| Maven project layout | `pom.xml`, `src/main/java`, `src/test/java` |
| CLI development | entry points, command parsing, exit codes |
| File processing | paths, file IO, Markdown/frontmatter parsing |
| Process execution | calling `git`, `mvn`, `gh`, `glab` |
| Release engineering | revision bumps, changelog generation, release notes |
| Maven plugin basics | Mojo classes and Maven goals |
| Testing | JUnit 5, temp directories, output assertions |

## 2. What This Repository Is

`javachanges` is a Java CLI and a Maven plugin for Maven repositories.

Its core workflow is:

```text
write .changesets/*.md
      ↓
parse changesets
      ↓
compute release plan
      ↓
update revision and changelog
      ↓
prepare publish settings and release automation
```

So this repository is a good example of a real-world Java tooling project:

- file-driven
- CLI-oriented
- Maven-based
- testable
- automation-friendly

## 3. Best Reading Order

Start with these files:

1. `src/main/java/io/github/sonofmagic/javachanges/core/cli/JavaChangesCli.java`
2. `src/main/java/io/github/sonofmagic/javachanges/core/cli/JavaChangesCommand.java`
3. `src/main/java/io/github/sonofmagic/javachanges/core/cli/GeneralCommands.java`
4. `src/main/java/io/github/sonofmagic/javachanges/core/release/ReleasePlanner.java`
5. `src/main/java/io/github/sonofmagic/javachanges/core/repo/RepoFiles.java`
6. `src/main/java/io/github/sonofmagic/javachanges/core/env/ReleaseEnvSupport.java`
7. `src/main/java/io/github/sonofmagic/javachanges/core/publish/PublishSupport.java`
8. `src/main/java/io/github/sonofmagic/javachanges/core/gitlab/GitlabReleaseSupport.java`

## 4. Main Knowledge Areas

### 4.1 Java classes and constructors

Many files are small service-style classes such as:

- `PublishSupport`
- `ReleasePlanner`
- `ReleaseNotesGenerator`
- `GitlabReleaseSupport`

These are good examples of how Java objects keep context in fields and perform one focused task.

### 4.2 Utility classes

The repository also contains utility classes such as:

- `ReleaseUtils`
- `ReleaseTextUtils`
- `ReleaseModuleUtils`
- `ReleaseJsonUtils`
- `ReleaseProcessUtils`

These show the classic Java pattern of:

- private constructor
- static methods
- no instance state

### 4.3 Enums

Look at `ReleaseTypes.java` for:

- `Platform`
- `OutputFormat`
- `ReleaseLevel`

These are useful examples of representing fixed choices safely instead of using raw strings everywhere.

### 4.4 File IO

The repository uses `Path` and `Files` heavily for:

- reading `pom.xml`
- reading and writing `.changesets/*.md`
- updating `CHANGELOG.md`
- generating `.m2/settings.xml`

### 4.5 Process execution

This project uses `ProcessBuilder` to execute:

- `git`
- `mvn`
- `gh`
- `glab`

That is one of the most practical Java skills for building CLI tools.

### 4.6 Maven concepts

From this repository you can learn:

| Maven concept | Where it appears |
| --- | --- |
| project coordinates | `pom.xml` |
| `revision` property | root version management |
| compiler plugin | Java 8 compilation |
| surefire plugin | tests |
| exec plugin | running the CLI in development |
| Maven plugin plugin | generating plugin metadata |

### 4.7 Maven plugin basics

The `src/main/java/io/github/sonofmagic/javachanges/maven` directory shows how Maven goals are implemented.

This is where you learn what a Mojo is and how a command can be exposed as:

```bash
mvn javachanges:status
```

### 4.8 Testing

The tests in `src/test/java` are good examples of how to test a Java CLI:

- use temp directories
- run commands programmatically
- assert stdout/stderr
- verify generated files

## 5. Current Source Layout

The `core` source tree is now grouped physically by responsibility:

| Directory | Responsibility |
| --- | --- |
| `core/cli` | CLI entrypoints and command classes |
| `core/env` | env files, doctor, render, sync, audit |
| `core/gitlab` | GitLab API and release automation |
| `core/publish` | Maven settings and publish flow |
| `core/release` | release planning and release notes |
| `core/repo` | changeset files and repository file updates |
| `core/util` | shared helper utilities |

## 6. Suggested Learning Path

1. Read the CLI entrypoint and command classes.
2. Learn how changesets are parsed and written.
3. Learn how release planning works.
4. Learn how file updates are applied.
5. Learn how publish helpers and GitLab automation are structured.
6. Read the tests and follow the execution flow end to end.

## 7. Good Beginner Exercises

| Exercise | Skill you practice |
| --- | --- |
| Add one extra output line | strings, CLI output, tests |
| Add a small command option | Picocli, request models, flow wiring |
| Add a JSON field | data flow and output contracts |
| Change changelog formatting slightly | text processing and tests |
| Extract one repeated helper | refactoring and code organization |

## 8. Related Docs

- [Getting Started](./getting-started.md)
- [Development Guide](./development-guide.md)
- [CLI Reference](./cli-reference.md)
- [GitHub Actions Usage Guide](./github-actions-guide.md)
- [GitLab CI/CD Usage Guide](./gitlab-ci-guide.md)
