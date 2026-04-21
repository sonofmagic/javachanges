# javachanges CLI Reference


## 1. Overview

This page is the command reference for `javachanges`.

Use it when you already understand the release workflow and need to look up:

- the command name
- required flags
- common argument combinations
- what each command reads or writes

## 2. Invocation Pattern

Current `main` branch Maven plugin invocation after local snapshot install:

```bash
mvn -q -DskipTests install
mvn io.github.sonofmagic:javachanges:__JAVACHANGES_CURRENT_SNAPSHOT_VERSION__:status
mvn io.github.sonofmagic:javachanges:__JAVACHANGES_CURRENT_SNAPSHOT_VERSION__:plan -Djavachanges.apply=true
mvn io.github.sonofmagic:javachanges:__JAVACHANGES_CURRENT_SNAPSHOT_VERSION__:add -Djavachanges.summary="add release notes command" -Djavachanges.release=minor
mvn io.github.sonofmagic:javachanges:__JAVACHANGES_CURRENT_SNAPSHOT_VERSION__:manifest-field -Djavachanges.field=releaseVersion
mvn io.github.sonofmagic:javachanges:__JAVACHANGES_CURRENT_SNAPSHOT_VERSION__:run -Djavachanges.args="release-notes --tag v1.2.3"
```

Source-driven invocation while developing this repository:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="status --directory /path/to/repo"
```

Common parts:

| Part | Meaning |
| --- | --- |
| `mvn io.github.sonofmagic:javachanges:__JAVACHANGES_CURRENT_SNAPSHOT_VERSION__:status` | Run the dedicated Maven plugin status goal |
| `mvn io.github.sonofmagic:javachanges:__JAVACHANGES_CURRENT_SNAPSHOT_VERSION__:plan -Djavachanges.apply=true` | Run the dedicated plan goal |
| `mvn io.github.sonofmagic:javachanges:__JAVACHANGES_CURRENT_SNAPSHOT_VERSION__:run -Djavachanges.args="..."` | Use the generic bridge goal for commands without a dedicated goal |
| `mvn -q -DskipTests compile exec:java` | Build the CLI and run the Java entrypoint |
| `-Dexec.args="..."` | Pass `javachanges` CLI arguments |
| `--directory /path/to/repo` | Target Maven repository root or a subdirectory inside it |

Plugin note:

- all dedicated goals and `javachanges:run` inject `--directory ${project.basedir}` automatically unless you already passed `--directory` explicitly

If you declare the plugin in a target repository `pom.xml`, the shortest local form becomes:

```bash
mvn javachanges:status
mvn javachanges:plan -Djavachanges.apply=true
mvn javachanges:add -Djavachanges.summary="add release notes command" -Djavachanges.release=minor
mvn javachanges:manifest-field -Djavachanges.field=releaseVersion
```

> Note: some commands do not require a repository, for example `release-version-from-tag`.

## 3. High-Value Commands

| Command | Purpose | Writes files |
| --- | --- | --- |
| `add` | Create a changeset | `.changesets/*.md` |
| `status` | Show the current release plan | No |
| `plan` | Render the current release plan | No |
| `plan --apply true` | Apply the plan and consume changesets | `pom.xml`, `CHANGELOG.md`, `.changesets/release-plan.json`, `.changesets/release-plan.md` |
| `manifest-field` | Read a field from the generated release manifest | No |
| `release-notes` | Generate release notes for a tag | target file |
| `preflight` | Render publish validation commands | No |
| `publish` | Render or execute the actual publish command | No |

## 4. Changeset Commands

### 4.1 `add`

Create a new changeset file.

Example:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="add --directory /path/to/repo --summary 'add release notes command' --release minor --modules core"
```

Behavior:

| Input | Meaning |
| --- | --- |
| `--summary` | First line of the generated markdown body |
| `--release` | `patch`, `minor`, or `major` |
| `--modules` | Comma-separated Maven artifactIds or `all` |
| `--body` | Optional extra markdown content after the summary |

Generated file shape:

````md
```md
---
"core": minor
---

add release notes command
```
````

Compatibility note:

- `add` still accepts legacy flag names such as `--release` and `--modules`
- the generated file itself uses the official Changesets-style package map

### 4.2 `status`

Show the current pending release plan.

Example:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="status --directory /path/to/repo"
```

Typical output includes:

- current root revision
- latest whole-repo tag
- pending changeset count
- release plan summary
- affected packages
- each pending changeset entry

### 4.3 `plan`

Render the release plan without writing files:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="plan --directory /path/to/repo"
```

Apply the release plan:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="plan --directory /path/to/repo --apply true"
```

When `--apply true` is used, `javachanges`:

1. updates the root `<revision>`
2. prepends a new section to `CHANGELOG.md`
3. writes `.changesets/release-plan.json`
4. writes `.changesets/release-plan.md`
5. deletes the consumed changeset files

### 4.4 `manifest-field`

Read a field from the generated release manifest.

Example:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="manifest-field --directory /path/to/repo --field releaseVersion"
```

Common fields:

| Field | Meaning |
| --- | --- |
| `releaseVersion` | Release version without the leading `v` |
| `nextSnapshotVersion` | Next root snapshot version |
| `releaseLevel` | Aggregated release type |

## 5. Repository And Version Commands

| Command | Purpose | Example |
| --- | --- | --- |
| `version` | Print the current root revision | `version --directory /path/to/repo` |
| `release-version-from-tag` | Extract `1.2.3` from `v1.2.3` or `core/v1.2.3` | `release-version-from-tag --tag v1.2.3` |
| `release-module-from-tag` | Extract the package/module name from `core/v1.2.3` | `release-module-from-tag --tag core/v1.2.3` |
| `assert-module` | Validate a Maven artifactId exists | `assert-module --directory /path/to/repo --module core` |
| `assert-snapshot` | Ensure the current revision is a snapshot | `assert-snapshot --directory /path/to/repo` |
| `assert-release-tag` | Ensure a tag matches the current repository version | `assert-release-tag --directory /path/to/repo --tag v1.2.3` |
| `module-selector-args` | Print Maven `-pl` selector args | `module-selector-args --directory /path/to/repo --module core` |

## 6. Environment And Settings Commands

| Command | Purpose |
| --- | --- |
| `write-settings` | Generate a Maven `settings.xml` file |
| `init-env` | Initialize a local release env file from the example template |
| `auth-help` | Print platform auth requirements |
| `render-vars` | Preview GitHub/GitLab variables and secrets, or emit JSON with `--format json` |
| `doctor-local` | Check local release prerequisites, or emit JSON with `--format json` |
| `doctor-platform` | Check remote platform readiness, or emit JSON with `--format json` |
| `sync-vars` | Sync variables to GitHub or GitLab |
| `audit-vars` | Compare local env values with remote platform state, or emit JSON with `--format json` |

Example:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="render-vars --directory /path/to/repo --env-file env/release.env.local --platform github"
```

Structured-output example:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="doctor-local --directory /path/to/repo --env-file env/release.env.local --format json"
```

Commands that currently support `--format json`:

| Command | Notes |
| --- | --- |
| `render-vars` | Includes `platform` and `showSecrets` in the payload |
| `doctor-local` | Includes section summaries, suggestions, and final error text on failure |
| `doctor-platform` | Includes `platform` and section summaries for env and CLI checks |
| `audit-vars` | Includes `platform`, audit sections, and final error text on failure |

Common flags for these commands:

| Flag | Used by | Meaning |
| --- | --- | --- |
| `--env-file` | all four | input env file path |
| `--platform` | `render-vars`, `doctor-platform`, `audit-vars` | `github`, `gitlab`, or `all` |
| `--show-secrets` | `render-vars` | reveal secret values instead of masking them |
| `--github-repo` | `doctor-local`, `doctor-platform`, `audit-vars` | optional GitHub `owner/repo` identifier |
| `--gitlab-repo` | `doctor-local`, `doctor-platform`, `audit-vars` | optional GitLab `group/project` identifier |
| `--format json` | all four | switch stdout from human text to machine-readable JSON |

JSON mode contract:

- stdout contains only one JSON object
- exit code `0` means success, non-zero means validation failure or execution error
- top-level fields may include `ok`, `command`, `envFile`, `platform`, `showSecrets`, `sections`, `suggestions`, and `error`

## 7. Publish Commands

### 7.1 `preflight`

Render the Maven validation flow before a real publish.

Snapshot example:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="preflight --directory /path/to/repo --snapshot"
```

Explicit snapshot build stamp:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="preflight --directory /path/to/repo --snapshot --snapshot-build-stamp 20260420.154500.ci001"
```

Release example:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="preflight --directory /path/to/repo --tag v1.2.3"
```

### 7.2 `publish`

Render the real publish command:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="publish --directory /path/to/repo --tag v1.2.3"
```

Actually execute it:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="publish --directory /path/to/repo --tag v1.2.3 --execute true"
```

Snapshot publishing resolves the root `1.2.3-SNAPSHOT` into a unique publish revision such as `1.2.3-20260420.154500.abc1234-SNAPSHOT`, then injects it through `-Drevision=`. You can override the generated build stamp with `--snapshot-build-stamp` or the `JAVACHANGES_SNAPSHOT_BUILD_STAMP` environment variable.

Important flags:

| Flag | Meaning |
| --- | --- |
| `--snapshot` | Publish the current snapshot instead of a release tag |
| `--snapshot-build-stamp` | Explicit snapshot publish stamp, overriding the default UTC timestamp + git short sha |
| `--tag` | Target release tag |
| `--module` | Restrict to one Maven artifactId |
| `--allow-dirty` | Allow a dirty working tree |
| `--execute true` | Run the final publish command instead of only printing it |

## 8. Platform Release Commands

### 8.1 GitHub Release Commands

| Command | Purpose |
| --- | --- |
| `github-release-plan` | Create or update a GitHub release-plan pull request |
| `github-tag-from-plan` | Create and push the final release tag from a generated release plan |
| `github-release-from-plan` | Generate release metadata and optionally create or update the GitHub Release |

Examples:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="github-release-plan --directory /path/to/repo --github-repo owner/repo --execute true"
mvn -q -DskipTests compile exec:java -Dexec.args="github-tag-from-plan --directory /path/to/repo --execute true"
mvn -q -DskipTests compile exec:java -Dexec.args="github-release-from-plan --directory /path/to/repo --release-notes-file target/release-notes.md --execute true"
```

### 8.2 GitLab Release Commands

| Command | Purpose |
| --- | --- |
| `gitlab-release-plan` | Create or update a GitLab release-plan merge request |
| `gitlab-tag-from-plan` | Create the final release tag from a generated release plan |

Examples:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="gitlab-release-plan --directory /path/to/repo --project-id 12345 --execute true"
mvn -q -DskipTests compile exec:java -Dexec.args="gitlab-tag-from-plan --directory /path/to/repo --execute true"
```

## 9. Help Output

You can always ask `picocli` for built-in help:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="--help"
mvn -q -DskipTests compile exec:java -Dexec.args="plan --help"
```

## 10. Related Guides

| Need | Document |
| --- | --- |
| First-time setup and local workflow | [Getting Started](./getting-started.md) |
| Local development workflow | [Development Guide](./development-guide.md) |
| Generated manifest files | [Release Plan Manifest](./release-plan-manifest.md) |
| GitHub Actions integration | [GitHub Actions Usage Guide](./github-actions-guide.md) |
| GitLab CI/CD integration | [GitLab CI/CD Usage Guide](./gitlab-ci-guide.md) |
