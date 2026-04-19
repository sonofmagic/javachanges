# javachanges Configuration Reference

[English](/configuration-reference) | [简体中文](/zh-CN/configuration-reference)

## 1. Overview

This page is the configuration catalog for `javachanges`.

Use it when you need one place to look up:

- changeset file structure
- CLI flags that affect behavior
- `env/release.env.example` variables
- GitHub Actions and GitLab CI/CD variable mapping
- publishing-related environment prerequisites

## 2. Configuration Surface Map

| Surface | Where it lives | Typical owner |
| --- | --- | --- |
| Changeset files | `.changesets/*.md` | Contributors |
| CLI invocation flags | command line / CI workflow YAML | Maintainers and CI authors |
| Release env template | `env/release.env.example` | Maintainers |
| Platform variables and secrets | GitHub Actions / GitLab CI/CD settings | Repository admins |
| Maven publish credentials | local env, CI secrets, generated `settings.xml` | Release maintainers |
| Repository version model | root `pom.xml` `<revision>` | Maintainers |

## 3. Changeset File Configuration

### 3.1 Recommended format

`javachanges` now defaults to the official Changesets-style package map:

```md
---
"javachanges": minor
---

Add GitHub Actions release automation.
```

Monorepo example:

```md
---
"core": minor
"cli": patch
---

Improve CLI parsing and release planning.
```

Rules:

| Part | Meaning |
| --- | --- |
| frontmatter key | Maven artifactId |
| frontmatter value | `patch`, `minor`, or `major` |
| markdown body | user-facing summary and notes |

### 3.2 Legacy compatibility

The older `javachanges` frontmatter is still accepted:

```md
---
release: minor
type: ci
modules: javachanges
summary: automate javachanges self-release publishing via GitHub Actions
---
```

Compatibility status:

| Field | Status |
| --- | --- |
| `release` | Legacy, still parsed |
| `modules` | Legacy, still parsed |
| `summary` | Legacy, still parsed |
| `type` | Legacy metadata, still parsed |

> Note: new files should use the official package-map format instead of the legacy fields above.

## 4. CLI Flags That Affect Configuration

### 4.1 `add`

| Flag | Meaning | Default |
| --- | --- | --- |
| `--summary` | First line of the generated markdown body | interactive prompt if omitted |
| `--release` | Release type to write for each selected package | interactive prompt if omitted |
| `--modules` | Comma-separated Maven artifactIds or `all` | `all` |
| `--body` | Extra markdown body after the summary | empty |
| `--type` | Legacy metadata only | omitted |

### 4.2 `plan`

| Flag | Meaning | Default |
| --- | --- | --- |
| `--apply` | Apply the calculated release plan and write files | `false` |
| `--directory` | Target repository path | current directory, resolved upward |

### 4.3 `preflight` and `publish`

| Flag | Meaning | Default |
| --- | --- | --- |
| `--snapshot` | Publish the current snapshot version | `false` |
| `--tag` | Publish a release tag like `v1.2.3` | none |
| `--module` | Restrict publish to one Maven artifactId | all packages |
| `--allow-dirty` | Skip dirty worktree protection | `false` |
| `--execute` | Run the final publish command instead of only printing it | `false` |

### 4.4 GitLab release commands

| Command | Flag | Default source |
| --- | --- | --- |
| `gitlab-release-plan` | `--project-id` | `CI_PROJECT_ID` |
| `gitlab-release-plan` | `--target-branch` | `CI_DEFAULT_BRANCH`, then `main` |
| `gitlab-release-plan` | `--release-branch` | `changeset-release/<target-branch>` |
| `gitlab-tag-from-plan` | `--before-sha` | `CI_COMMIT_BEFORE_SHA` |
| `gitlab-tag-from-plan` | `--current-sha` | `CI_COMMIT_SHA` |

## 5. `env/release.env.example`

Current template:

| Variable | Required | Meaning |
| --- | --- | --- |
| `MAVEN_RELEASE_REPOSITORY_URL` | Yes | Maven release repository URL |
| `MAVEN_SNAPSHOT_REPOSITORY_URL` | Yes | Maven snapshot repository URL |
| `MAVEN_RELEASE_REPOSITORY_ID` | Yes | Release server id used in Maven settings |
| `MAVEN_SNAPSHOT_REPOSITORY_ID` | Yes | Snapshot server id used in Maven settings |
| `MAVEN_REPOSITORY_USERNAME` | Yes unless release/snapshot credentials are split | Shared username fallback |
| `MAVEN_REPOSITORY_PASSWORD` | Yes unless release/snapshot credentials are split | Shared password fallback |
| `MAVEN_RELEASE_REPOSITORY_USERNAME` | No | Explicit release username override |
| `MAVEN_RELEASE_REPOSITORY_PASSWORD` | No | Explicit release password override |
| `MAVEN_SNAPSHOT_REPOSITORY_USERNAME` | No | Explicit snapshot username override |
| `MAVEN_SNAPSHOT_REPOSITORY_PASSWORD` | No | Explicit snapshot password override |
| `GITLAB_RELEASE_TOKEN` | No | Optional GitLab release token for some GitLab release scenarios |

Resolution rules used by the code:

| Setting | Fallback logic |
| --- | --- |
| Release username/password | `MAVEN_RELEASE_REPOSITORY_*`, then shared `MAVEN_REPOSITORY_*` |
| Snapshot username/password | `MAVEN_SNAPSHOT_REPOSITORY_*`, then shared `MAVEN_REPOSITORY_*` |
| Release server id | `MAVEN_RELEASE_REPOSITORY_ID`, then `maven-releases` |
| Snapshot server id | `MAVEN_SNAPSHOT_REPOSITORY_ID`, then `maven-snapshots` |

## 6. GitHub Actions Variable Mapping

### 6.1 Recommended GitHub Actions variables

These are treated as non-secret variables:

| Name |
| --- |
| `MAVEN_RELEASE_REPOSITORY_URL` |
| `MAVEN_SNAPSHOT_REPOSITORY_URL` |
| `MAVEN_RELEASE_REPOSITORY_ID` |
| `MAVEN_SNAPSHOT_REPOSITORY_ID` |

### 6.2 Recommended GitHub Actions secrets

| Name |
| --- |
| `MAVEN_REPOSITORY_USERNAME` |
| `MAVEN_REPOSITORY_PASSWORD` |
| `MAVEN_RELEASE_REPOSITORY_USERNAME` |
| `MAVEN_RELEASE_REPOSITORY_PASSWORD` |
| `MAVEN_SNAPSHOT_REPOSITORY_USERNAME` |
| `MAVEN_SNAPSHOT_REPOSITORY_PASSWORD` |

### 6.3 Repository-specific Maven Central release secrets

The self-release workflow in this repository also requires:

| Secret | Purpose |
| --- | --- |
| `MAVEN_CENTRAL_USERNAME` | Sonatype Central Portal token username |
| `MAVEN_CENTRAL_PASSWORD` | Sonatype Central Portal token password |
| `MAVEN_GPG_PRIVATE_KEY` | ASCII-armored private key |
| `MAVEN_GPG_PASSPHRASE` | GPG key passphrase |

## 7. GitLab CI/CD Variable Mapping

### 7.1 Variables synced from the env file

When `sync-vars --platform gitlab` is used, these values are written as GitLab variables:

| Name | Secret | Protected |
| --- | --- | --- |
| `MAVEN_RELEASE_REPOSITORY_URL` | No | No |
| `MAVEN_SNAPSHOT_REPOSITORY_URL` | No | No |
| `MAVEN_RELEASE_REPOSITORY_ID` | No | No |
| `MAVEN_SNAPSHOT_REPOSITORY_ID` | No | No |
| `MAVEN_REPOSITORY_USERNAME` | Yes | Yes |
| `MAVEN_REPOSITORY_PASSWORD` | Yes | Yes |
| `MAVEN_RELEASE_REPOSITORY_USERNAME` | Yes | Yes |
| `MAVEN_RELEASE_REPOSITORY_PASSWORD` | Yes | Yes |
| `MAVEN_SNAPSHOT_REPOSITORY_USERNAME` | Yes | Yes |
| `MAVEN_SNAPSHOT_REPOSITORY_PASSWORD` | Yes | Yes |
| `GITLAB_RELEASE_TOKEN` | Yes | Yes |

### 7.2 Extra GitLab CI runtime variables

These are expected by the GitLab release automation:

| Variable | Source |
| --- | --- |
| `CI_PROJECT_ID` | GitLab built-in variable |
| `CI_DEFAULT_BRANCH` | GitLab built-in variable |
| `CI_SERVER_HOST` | GitLab built-in variable |
| `CI_SERVER_URL` | GitLab built-in variable |
| `CI_PROJECT_PATH` | GitLab built-in variable |
| `CI_COMMIT_BEFORE_SHA` | GitLab built-in variable |
| `CI_COMMIT_SHA` | GitLab built-in variable |
| `GITLAB_RELEASE_BOT_USERNAME` | Project variable you provide |
| `GITLAB_RELEASE_BOT_TOKEN` | Project variable you provide |

## 8. Maven Publish Runtime Configuration

### 8.1 Repository version model

Recommended root `pom.xml` pattern:

```xml
<version>${revision}</version>
```

And the mutable version field:

```xml
<revision>1.2.3-SNAPSHOT</revision>
```

### 8.2 Local Maven repository override

`publish` and `preflight` recognize `MAVEN_OPTS` values like:

```bash
export MAVEN_OPTS="-Dmaven.repo.local=.m2/repository"
```

Effect:

| Setting | Result |
| --- | --- |
| no `MAVEN_OPTS` override | uses `.m2/repository` inside the target repo |
| relative `maven.repo.local` | resolved relative to the target repo |
| absolute `maven.repo.local` | used as-is |

## 9. Recommended Defaults By Scenario

### 9.1 Single-module library

| Surface | Recommendation |
| --- | --- |
| Changeset file | one package key matching the root artifactId |
| `--modules` | omit it or use `all` |
| release tagging | `v1.2.3` |
| publish target | all packages |

### 9.2 Maven monorepo

| Surface | Recommendation |
| --- | --- |
| Changeset file | one frontmatter entry per affected artifactId |
| `--modules` | pass explicit artifactIds during `add` |
| release tagging | whole-repo `v1.2.3` unless you intentionally use module tags elsewhere |
| publish target | all packages, or one `--module` when needed |

## 10. Related Guides

| Need | Document |
| --- | --- |
| Command-by-command syntax | [CLI Reference](./cli-reference.md) |
| Generated manifest files | [Release Plan Manifest](./release-plan-manifest.md) |
| GitHub workflow patterns | [GitHub Actions Usage Guide](./github-actions-guide.md) |
| GitLab workflow patterns | [GitLab CI/CD Usage Guide](./gitlab-ci-guide.md) |
| Maven Central self-release flow | [Publish To Maven Central](./publish-to-maven-central.md) |
