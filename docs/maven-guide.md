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
mvn javachanges:setup
mvn javachanges:setup -Djavachanges.directory=/path/to/gradle-repo -Djavachanges.applyGradleTasks=true
mvn javachanges:status
mvn javachanges:next
mvn javachanges:add -Djavachanges.summary="add release notes command" -Djavachanges.release=minor
mvn javachanges:plan
mvn javachanges:plan -Djavachanges.apply=true
mvn javachanges:validate
mvn javachanges:init-gradle-tasks -Djavachanges.directory=/path/to/gradle-repo -Djavachanges.apply=true
mvn javachanges:init-env
mvn javachanges:auth-help -Djavachanges.platform=github
mvn javachanges:render-vars -Djavachanges.envFile=env/release.env.local -Djavachanges.platform=github
mvn javachanges:doctor-local -Djavachanges.envFile=env/release.env.local
mvn javachanges:doctor-platform -Djavachanges.envFile=env/release.env.local -Djavachanges.platform=github
mvn javachanges:audit-vars -Djavachanges.envFile=env/release.env.local -Djavachanges.platform=github
mvn javachanges:write-settings -Djavachanges.settingsMode=release
mvn javachanges:ensure-gpg-public-key
mvn javachanges:init-github-actions
mvn javachanges:github-release-plan -Djavachanges.githubRepo=owner/repo -Djavachanges.writePlanFiles=false
mvn javachanges:github-tag-from-plan -Djavachanges.fresh=true
mvn javachanges:github-release-publish-state -Djavachanges.fresh=true
mvn javachanges:github-release-from-plan -Djavachanges.fresh=true
mvn javachanges:init-gitlab-ci
mvn javachanges:gitlab-release-plan -Djavachanges.projectId=12345 -Djavachanges.writePlanFiles=false
mvn javachanges:gitlab-tag-from-plan -Djavachanges.fresh=true -Djavachanges.fallbackFromReleaseCommit=true
mvn javachanges:gitlab-release -Djavachanges.tag=v1.2.3
mvn javachanges:release-version-from-tag -Djavachanges.tag=core/v1.2.3
mvn javachanges:release-module-from-tag -Djavachanges.tag=core/v1.2.3
mvn javachanges:assert-module -Djavachanges.module=core
mvn javachanges:assert-snapshot
mvn javachanges:assert-release-tag -Djavachanges.tag=v1.2.3
mvn javachanges:doctor-publish -Djavachanges.tag=v1.2.3
mvn javachanges:gradle-publish -Djavachanges.directory=/path/to/gradle-repo -Djavachanges.tag=v1.2.3
mvn javachanges:manifest-field -Djavachanges.field=releaseVersion -Djavachanges.fresh=true
```

`javachanges:write-settings` writes `${project.basedir}/.m2/settings.xml` by default. Use `-Djavachanges.output=...` to choose another path and `-Djavachanges.settingsMode=all|release|snapshot` to control which server entries are written.

`javachanges:init-env` writes a local release env file from the example template. Use `-Djavachanges.target=...` to choose the destination, `-Djavachanges.template=...` to choose another template, and `-Djavachanges.force=true` to replace an existing file. Use `javachanges:auth-help` with `-Djavachanges.platform=github|gitlab|all` to print the required authentication variables.

The env review goals use Maven-style property names for common CLI options: `-Djavachanges.envFile=...`, `-Djavachanges.platform=github|gitlab|all`, `-Djavachanges.githubRepo=owner/repo`, and `-Djavachanges.gitlabRepo=group/project`. `javachanges:sync-vars` is a dry run by default; add `-Djavachanges.execute=true` only when you are ready to update the remote platform variables.

`javachanges:ensure-gpg-public-key` publishes the current signing public key to supported keyservers and waits until it can be fetched. Use `-Djavachanges.primaryKeyserver=...`, `-Djavachanges.secondaryKeyserver=...`, `-Djavachanges.attempts=...`, and `-Djavachanges.retryDelaySeconds=...` when the defaults need to match your release environment.

`javachanges:init-github-actions` writes `.github/workflows/javachanges-release.yml` by default, and `javachanges:init-gitlab-ci` writes `.gitlab-ci.yml` by default. Use `-Djavachanges.force=true` to replace an existing generated file, `-Djavachanges.buildTool=maven|gradle|auto` to choose the template, and `-Djavachanges.javachangesVersion=...` to pin the generated CI version.

`javachanges:init-gradle-tasks` writes `gradle/javachanges.gradle` for Gradle repositories. Use `-Djavachanges.apply=true` to append that script to the root `build.gradle` or `build.gradle.kts`; `javachanges:setup -Djavachanges.applyGradleTasks=true` does the same as part of first-time setup.

The GitHub release automation goals map directly to the CLI commands. Use `-Djavachanges.execute=true` only in CI or when you intentionally want to call `gh`; without it, release-plan, tag, and release goals stay in dry-run mode. Release-plan pull requests do not commit `.changesets/release-plan.*` files by default; use `-Djavachanges.writePlanFiles=true` only for compatibility with older manifest-based automation.

The GitLab release automation goals follow the same dry-run default. Use `-Djavachanges.execute=true` only when the command should call the GitLab API or push tags. `gitlab-tag-from-plan` also supports `-Djavachanges.fallbackFromReleaseCommit=true` for default-branch recovery from a merged `chore(release): release vX.Y.Z` commit.

`javachanges:gradle-publish` is available when you are invoking the Maven plugin from a Maven runner project but need to target a Gradle repository through `-Djavachanges.directory=...`. Gradle repositories without a Maven project should keep using the released CLI jar as shown in the Gradle guide.

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

For CI or scripts, add `-Djavachanges.noInteractive=true` so missing summary or release input fails instead of prompting. Add `-Djavachanges.format=json` when the script needs the created changeset path or next commands.

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
mvn javachanges:module-selector-args -Djavachanges.module=core
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
mvn javachanges:doctor-publish -Djavachanges.tag=v1.2.3
mvn javachanges:preflight -Djavachanges.tag=v1.2.3
```

When publish inputs are ready, execute the publish helper:

```bash
mvn javachanges:publish -Djavachanges.tag=v1.2.3 -Djavachanges.execute=true
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
