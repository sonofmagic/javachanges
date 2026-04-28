---
description: Use javachanges with Maven single-module repositories and Maven multi-module builds.
---

# Maven Usage Guide


## 1. What Maven support covers

`javachanges` can plan releases for Maven repositories through either the Maven plugin or the released CLI jar.

The Maven path supports:

- repository root detection from the root `pom.xml`
- current version reading from the root `<revision>` property
- version updates during `plan --apply true`
- package detection from Maven `<modules>` entries
- single-module repositories using the root `artifactId`
- changesets, status output, changelog generation, release-plan manifests, GitHub release PRs, GitLab release MRs, preflight checks, and Maven publish helpers

For day-to-day Maven repository usage, prefer the Maven plugin because it keeps commands short and defaults `--directory` to the current Maven project's `${project.basedir}`.

## 2. Required Maven shape

A Maven repository should keep the root version in a `revision` property:

```xml
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.example</groupId>
  <artifactId>payments</artifactId>
  <version>${revision}</version>

  <properties>
    <revision>1.4.0-SNAPSHOT</revision>
  </properties>
</project>
```

Multi-module Maven repositories should declare modules in the root `pom.xml`:

```xml
<modules>
  <module>modules/api</module>
  <module>modules/core</module>
</modules>
```

Each module should have its own `artifactId`. That `artifactId` is the package key used in changeset files.

## 3. Install and run the Maven plugin

Add the plugin to the target repository `pom.xml`:

```xml
<plugin>
  <groupId>io.github.sonofmagic</groupId>
  <artifactId>javachanges</artifactId>
  <version>__JAVACHANGES_LATEST_RELEASE_VERSION__</version>
</plugin>
```

Then run the shortest local goals inside the Maven repository:

```bash
mvn javachanges:status
mvn javachanges:add -Djavachanges.summary="add release notes command" -Djavachanges.release=minor
mvn javachanges:plan
mvn javachanges:plan -Djavachanges.apply=true
mvn javachanges:manifest-field -Djavachanges.field=releaseVersion
```

Use the generic `run` goal for commands that do not have a dedicated Maven goal yet:

```bash
mvn javachanges:run -Djavachanges.args="release-notes --tag v1.2.3"
```

## 4. Use the released CLI without changing `pom.xml`

If you cannot add the plugin yet, download the CLI jar from Maven Central:

```bash
mvn -q dependency:copy \
  -Dartifact=io.github.sonofmagic:javachanges:__JAVACHANGES_LATEST_RELEASE_VERSION__ \
  -DoutputDirectory=.javachanges
```

Set a helper variable:

```bash
export JAVACHANGES="java -jar .javachanges/javachanges-__JAVACHANGES_LATEST_RELEASE_VERSION__.jar"
```

Run against the Maven repository:

```bash
$JAVACHANGES status --directory .
$JAVACHANGES add --directory . --summary "add release notes command" --release minor
$JAVACHANGES plan --directory . --apply true
```

## 5. Single-module Maven workflow

Example repository:

```text
my-library/
├── .changesets/
├── CHANGELOG.md
└── pom.xml
```

`pom.xml`:

```xml
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.example</groupId>
  <artifactId>my-library</artifactId>
  <version>${revision}</version>
  <properties>
    <revision>0.8.0-SNAPSHOT</revision>
  </properties>
</project>
```

Create a patch changeset:

```bash
mvn javachanges:add \
  -Djavachanges.summary="fix generated release notes" \
  -Djavachanges.release=patch
```

The generated changeset uses the root `artifactId`:

```md
---
"my-library": patch
---

fix generated release notes
```

Inspect and apply:

```bash
mvn javachanges:status
mvn javachanges:plan
mvn javachanges:plan -Djavachanges.apply=true
```

After apply:

- `pom.xml` advances the `revision` property to the next snapshot version
- `CHANGELOG.md` gets a new release section
- `.changesets/release-plan.json` is written
- `.changesets/release-plan.md` is written
- consumed `.changesets/*.md` files are deleted

## 6. Multi-module Maven workflow

Example root `pom.xml`:

```xml
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.example</groupId>
  <artifactId>payments-parent</artifactId>
  <version>${revision}</version>
  <packaging>pom</packaging>

  <modules>
    <module>modules/api</module>
    <module>modules/core</module>
    <module>tools/cli</module>
  </modules>

  <properties>
    <revision>1.2.0-SNAPSHOT</revision>
  </properties>
</project>
```

Detected package keys come from module `artifactId` values:

| Module path | Example artifactId | Changeset package key |
| --- | --- | --- |
| `modules/api` | `api` | `api` |
| `modules/core` | `core` | `core` |
| `tools/cli` | `payments-cli` | `payments-cli` |

Create one changeset affecting two modules:

```bash
mvn javachanges:add \
  -Djavachanges.summary="add payment retry metadata" \
  -Djavachanges.release=minor \
  -Djavachanges.modules=api,core
```

Hand-written format:

```md
---
"api": minor
"core": minor
---

add payment retry metadata
```

Use `-Djavachanges.modules=all` when the changeset affects every detected Maven package:

```bash
mvn javachanges:add \
  -Djavachanges.summary="standardize publication metadata" \
  -Djavachanges.release=patch \
  -Djavachanges.modules=all
```

When a downstream job needs to publish or test one Maven module, ask `javachanges` for Maven selector arguments:

```bash
mvn javachanges:run -Djavachanges.args="module-selector-args --module core"
```

For Maven repositories, this renders arguments such as:

```text
-pl :core -am
```

## 7. CI release-plan automation

GitHub Actions and GitLab CI can run the same planning commands as local usage.

GitHub:

```bash
java -jar .javachanges/javachanges-__JAVACHANGES_LATEST_RELEASE_VERSION__.jar \
  github-release-plan \
  --directory "$GITHUB_WORKSPACE" \
  --github-repo "$GITHUB_REPOSITORY" \
  --execute true
```

GitLab:

```bash
java -jar .javachanges/javachanges-__JAVACHANGES_LATEST_RELEASE_VERSION__.jar \
  gitlab-release-plan \
  --directory "$CI_PROJECT_DIR" \
  --project-id "$CI_PROJECT_ID" \
  --execute true
```

Release-plan automation stages `pom.xml`, `CHANGELOG.md`, and `.changesets/` for Maven repositories.

Tagging from an applied plan:

```bash
mvn javachanges:run -Djavachanges.args="github-tag-from-plan --execute true"
mvn javachanges:run -Djavachanges.args="gitlab-tag-from-plan --execute true"
```

## 8. Publishing Maven artifacts

Use `preflight` before enabling real publish execution:

```bash
mvn javachanges:run -Djavachanges.args="preflight --tag v1.2.3"
```

When publish inputs are ready, execute the publish helper:

```bash
mvn javachanges:run -Djavachanges.args="publish --tag v1.2.3 --execute true"
```

The helper renders Maven deploy commands and can write Maven `settings.xml` from environment variables. For full Central release setup, see [Publish To Maven Central](./publish-to-maven-central.md).

## 9. Common mistakes

| Symptom | Cause | Fix |
| --- | --- | --- |
| `Cannot find repository root` | no root `pom.xml` can be found | run from inside the Maven repository or pass `--directory` |
| `Cannot find version or revision` | root `pom.xml` does not define `<revision>` | add `<revision>1.0.0-SNAPSHOT</revision>` under root `<properties>` |
| `Unknown module` | changeset key does not match a detected module `artifactId` | use the module `artifactId`, not the folder name unless they match |
| version updates the wrong file | command was pointed at the wrong repository root | check the plugin basedir or pass an explicit `--directory` with the CLI |

## 10. Related guides

| Need | Document |
| --- | --- |
| First setup flow | [Getting Started](./getting-started.md) |
| Gradle repository flow | [Gradle Usage Guide](./gradle-guide.md) |
| Command details | [CLI Reference](./cli-reference.md) |
| Copy-ready command sequences | [Command Cookbook](./command-cookbook.md) |
| Generated release manifest | [Release Plan Manifest](./release-plan-manifest.md) |
| Maven Central publishing | [Publish To Maven Central](./publish-to-maven-central.md) |
| CI release PR/MR automation | [GitHub Actions Usage Guide](./github-actions-guide.md) and [GitLab CI/CD Usage Guide](./gitlab-ci-guide.md) |
