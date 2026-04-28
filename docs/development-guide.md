---
description: Set up a local Java and Maven environment, run javachanges from source, and validate Maven or Gradle target repositories.
---

# javachanges Development Guide


## 1. Overview

`javachanges` is a Java CLI for Maven and Gradle repositories.

This repository is not currently distributed as:

- an `npm install` package
- a `brew install` formula
- a ready-made global `javachanges` executable

Instead, the current workflow is:

- install local prerequisites: JDK + Git
- clone the source repository
- compile and run the CLI directly with the Maven Wrapper

> Note: the repository includes Maven Wrapper scripts, so most source-repository commands should use `./mvnw` instead of a system Maven.

## 2. Requirements

### 2.1 Minimum requirements

According to [pom.xml](../pom.xml), the current project expects:

| Item | Requirement |
| --- | --- |
| JDK | Java 8+ |
| Maven | Wrapper-provided 3.9.15, or system Maven 3.8+ |
| Git | Required |
| Target repository | Maven root `pom.xml`, or Gradle `gradle.properties` plus Gradle settings/build files |

### 2.2 Recommended development environment

The repository currently develops and validates against Java 8:

| Scenario | Recommendation |
| --- | --- |
| Day-to-day development | JDK 8 + the repository Maven Wrapper |
| Local default environment | Match the Java 8 compiler target in `pom.xml` |
| Command execution | Use `./mvnw` from the repository root |

> Tip: since the repository targets Java 8, using Java 8 locally reduces environment drift.

## 3. Install local dependencies

### 3.1 macOS

With Homebrew:

```bash
# Install Java 8 (Corretto 8 is recommended)
brew install --cask corretto@8

# Maven is provided by ./mvnw in this repository
```

Then verify:

```bash
java -version
./mvnw -v
```

If you want new terminals to default to Java 8:

```bash
export JAVA_HOME="/Library/Java/JavaVirtualMachines/amazon-corretto-8.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
```

Then reopen the terminal or run:

```bash
source ~/.zshrc
```

### 3.2 Linux

Examples:

```bash
# Debian / Ubuntu
sudo apt install openjdk-8-jdk maven

# Fedora
sudo dnf install java-1.8.0-openjdk-devel maven
```

### 3.3 Windows

You can use:

- JDK: Amazon Corretto 8 / Temurin 8 / Oracle JDK 8
- Maven: official binary, Scoop, or Chocolatey

Verify with:

```bash
java -version
./mvnw -v
```

## 4. Get the source and build it

### 4.1 Clone the repository

```bash
git clone https://github.com/sonofmagic/javachanges.git
cd javachanges
```

### 4.2 Verify the build

Run:

```bash
./mvnw -q test
```

This mainly does two things:

| Action | Purpose |
| --- | --- |
| Download dependencies | Prime the local Maven cache |
| Compile the project | Confirm the source builds cleanly |

> Note: the repository now includes CLI-focused unit tests, so `./mvnw test` validates both compilation and command behavior.

### 4.3 Install into the local Maven repository (optional)

```bash
./mvnw install
```

This writes artifacts to `~/.m2/repository`, but it does not create a global `javachanges` command.

## 5. What “development mode” means here

This is a CLI project, not a web server. There is no typical:

- hot reload dev server
- `npm run dev`
- long-running background process

For this repository, “development mode” usually means:

1. edit files under `src/main/java`
2. recompile with Maven
3. run the CLI entrypoint through `exec:java`, or install the snapshot and use Maven plugin goals
4. inspect command output and iterate

### 5.2 Most common development command

```bash
./mvnw -q -DskipTests compile exec:java -Dexec.args="status --directory /path/to/your/repo"
```

| Segment | Meaning |
| --- | --- |
| `compile` | Compile the current source |
| `exec:java` | Run the `main` entrypoint directly |
| `-DskipTests` | Skip tests for faster iteration |
| `-Dexec.args="..."` | Pass CLI arguments |

### 5.3 Fastest plugin-style loop on the current branch

If you want to validate the developer-facing plugin UX itself, first install the current snapshot locally:

```bash
./mvnw -q -DskipTests install
```

Equivalent repository shortcut:

```bash
pnpm snapshot:install
```

Then run the dedicated plugin goals:

```bash
mvn io.github.sonofmagic:javachanges:__JAVACHANGES_CURRENT_SNAPSHOT_VERSION__:status
mvn io.github.sonofmagic:javachanges:__JAVACHANGES_CURRENT_SNAPSHOT_VERSION__:plan -Djavachanges.apply=true
mvn io.github.sonofmagic:javachanges:__JAVACHANGES_CURRENT_SNAPSHOT_VERSION__:add -Djavachanges.summary="add release notes command" -Djavachanges.release=minor
```

### 5.4 Entry class

The current CLI entry class is:

- [src/main/java/io/github/sonofmagic/javachanges/core/JavaChangesCli.java](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/JavaChangesCli.java)

You can also run or debug this class directly in IntelliJ IDEA or VS Code.

### 5.5 Local snapshot and docs deployment shortcuts

This repository also ships small local shortcuts so you do not need to keep retyping the longer commands:

```bash
pnpm snapshot:install
pnpm snapshot:preflight
pnpm snapshot:publish:local
pnpm docs:deploy:local
```

Use them like this:

| Command | Purpose |
| --- | --- |
| `pnpm snapshot:install` | Install the current `__JAVACHANGES_CURRENT_SNAPSHOT_VERSION__` into the repository-local `.m2/repository` |
| `pnpm snapshot:preflight` | Run `preflight --snapshot` against the current repository with a local build stamp and the repository-local Maven cache |
| `pnpm snapshot:publish:local` | Publish a unique snapshot revision from this repository through `central-publishing-maven-plugin` |
| `pnpm docs:deploy:local` | Rebuild `website/dist` and serve it locally through Wrangler |

## 6. Common development commands

### 6.1 Show status

```bash
./mvnw -q -DskipTests compile exec:java -Dexec.args="status --directory /path/to/your/repo"
```

### 6.2 Add a changeset

```bash
./mvnw -q -DskipTests compile exec:java -Dexec.args="add --directory /path/to/your/repo --summary 'add release notes command' --release minor"
```

That command now writes an official Changesets-style file, for example:

````md
```md
---
"your-artifact-id": minor
---

add release notes command
```
````

### 6.3 Generate a release plan

```bash
./mvnw -q -DskipTests compile exec:java -Dexec.args="plan --directory /path/to/your/repo"
```

### 6.4 Apply a release plan

```bash
./mvnw -q -DskipTests compile exec:java -Dexec.args="plan --directory /path/to/your/repo --apply true"
```

### 6.5 Print help

```bash
./mvnw -q -DskipTests compile exec:java -Dexec.args="help"
```

High-value commands include:

| Command | Purpose |
| --- | --- |
| `add` | Create a changeset |
| `status` | Show pending release state |
| `plan` | Generate or apply a release plan |
| `write-settings` | Generate Maven settings |
| `init-env` | Initialize release env templates |
| `render-vars` | Render platform variables |
| `doctor-local` | Check local environment |
| `doctor-platform` | Check platform variables |
| `sync-vars` | Sync platform variables |
| `audit-vars` | Audit platform variables |
| `preflight` | Run pre-publish checks |
| `publish` | Assist publishing |
| `ensure-gpg-public-key` | Publish and verify the current signing public key on supported keyservers |
| `release-notes` | Generate release notes |

## 7. Recommended local workflow

### 7.1 Iteration loop

1. Install JDK and Git
2. Run `./mvnw -q test`
3. Prepare a Maven or Gradle repository for testing
4. Edit `src/main/java`
5. Validate behavior with `./mvnw -q -DskipTests compile exec:java -Dexec.args="..."`
6. Run `./mvnw test` or `./mvnw package` before finalizing

### 7.2 Debugging options

| Method | Best for |
| --- | --- |
| `./mvnw ... exec:java` | Closest to real CLI usage |
| Run `JavaChangesCli` in an IDE | Breakpoint debugging |
| `./mvnw package` then verify | Packaging validation |

## 8. FAQ

### 8.1 `./mvnw: Permission denied`

Cause: the Maven Wrapper script is not executable after checkout.

Fix:

1. run `chmod +x ./mvnw`
2. rerun `./mvnw -v`
3. commit the executable bit if your Git client removed it

### 8.2 `Unable to locate a Java Runtime`

Cause: no JDK is installed, or `JAVA_HOME` / `PATH` is wrong.

Fix:

1. install a JDK
2. run `java -version`
3. run `./mvnw -v`

### 8.3 Target repository structure errors

The target repository must have at least:

| Requirement | Meaning |
| --- | --- |
| Git repository | Must be initialized |
| Maven version model | root `pom.xml` with `<revision>` |
| Gradle version model | `gradle.properties` with `version` or `revision` |
| Module model | Maven `<modules>`, Gradle `include(...)`, or a single root artifact/project |
| `.changesets/` | Stores changesets |

### 8.4 Why there is no hot reload

Because this is a Java CLI project, not a long-running frontend or backend service. The normal flow is to rerun commands after editing:

```bash
./mvnw -q -DskipTests compile exec:java -Dexec.args="status --directory /path/to/your/repo"
```

### 8.5 Can I install `javachanges` as a global command today?

Not from this source repository directly.

Current supported workflows are:

- run from source with `./mvnw ... exec:java`
- build the jar with Maven and run `java -jar ...`
- consume a published jar from Maven Central in CI after a release exists

### 8.6 What is the safest way to test release behavior?

Use this order:

1. `status`
2. `plan`
3. `plan --apply true` in a disposable test repository
4. `preflight`
5. `publish` without `--execute true`

That lets you inspect the release flow before any real deployment happens.

## 9. Summary

You can think of this repository as “the source repository of a Java CLI that is run through Maven.”

Shortest path:

| Goal | Command |
| --- | --- |
| Install local dependencies | `brew install --cask corretto@8` |
| Verify environment | `java -version && ./mvnw -v` |
| Clone the project | `git clone https://github.com/sonofmagic/javachanges.git` |
| Initial build | `./mvnw -q test` |
| Enter development mode | `./mvnw -q -DskipTests compile exec:java -Dexec.args="status --directory /path/to/your/repo"` |

## 10. References

| Resource | Link |
| --- | --- |
| Maven install docs | https://maven.apache.org/install.html |
| Maven run docs | https://maven.apache.org/run.html |
| Amazon Corretto 8 downloads | https://docs.aws.amazon.com/corretto/latest/corretto-8-ug/downloads-list.html |
| Amazon Corretto 8 macOS install | https://docs.aws.amazon.com/corretto/latest/corretto-8-ug/macos-install.html |
| Project overview | [README.md](https://github.com/sonofmagic/javachanges/blob/main/README.md) |
| Getting started | [getting-started.md](./getting-started.md) |
| Troubleshooting | [troubleshooting-guide.md](./troubleshooting-guide.md) |
