---
description: Stable and human-oriented javachanges outputs, including machine-readable JSON modes for automation.
---

# javachanges Output Contracts


## 1. Overview

This page explains which `javachanges` outputs are safe for automation, which ones are only intended for human review, and what the current output shape looks like.

Use it when you are:

- writing CI scripts around `javachanges`
- deciding whether to parse terminal output or read generated files
- trying to understand which fields are intended to stay stable

## 2. Stability map

| Output | Intended consumer | Stability guidance |
| --- | --- | --- |
| `manifest-field` stdout | scripts and CI | preferred machine-readable interface |
| `.changesets/release-plan.json` | scripts and CI | preferred machine-readable interface |
| `.changesets/release-plan.md` | pull request and merge request bodies | human-readable, structure may evolve |
| `status` stdout | local operators and reviewers | human-oriented, do not parse rigidly |
| `render-vars` stdout | local operators | human-oriented by default |
| `render-vars --format json` stdout | scripts and CI | machine-readable JSON contract |
| `doctor-local` stdout | local operators | human-oriented by default |
| `doctor-local --format json` stdout | scripts and CI | machine-readable JSON contract |
| `doctor-platform` stdout | local operators | human-oriented by default |
| `doctor-platform --format json` stdout | scripts and CI | machine-readable JSON contract |
| `sync-vars` dry-run stdout | local operators | preview text only, not a stable API |
| `audit-vars` stdout | local operators | human-oriented by default |
| `audit-vars --format json` stdout | scripts and CI | machine-readable JSON contract |
| `preflight --format json` stdout | scripts and CI | machine-readable publish-preflight contract |
| `publish --format json` stdout | scripts and CI | machine-readable publish contract |
| `gradle-publish --format json` stdout | scripts and CI | machine-readable Gradle publish contract |
| `github-release-plan --format json` stdout | scripts and CI | machine-readable GitHub release-plan contract |
| `github-tag-from-plan --format json` stdout | scripts and CI | machine-readable GitHub tag contract |
| `github-release-from-plan --format json` stdout | scripts and CI | machine-readable GitHub Release contract |
| `gitlab-release-plan --format json` stdout | scripts and CI | machine-readable GitLab release-plan contract |
| `gitlab-tag-from-plan --format json` stdout | scripts and CI | machine-readable GitLab tag contract |
| `gitlab-release --format json` stdout | scripts and CI | machine-readable GitLab Release contract |

Practical rule:

- if automation needs one value, prefer `manifest-field`
- if automation needs multiple values, prefer `.changesets/release-plan.json`
- do not build CI logic around terminal headings, spacing, or localized labels

## 3. `status` output

`status` prints a human-readable release summary.

Current command:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="status --directory /path/to/repo"
```

Current layout:

```text
Repository: /path/to/repo
Current revision: 0.1.0-SNAPSHOT
Latest whole-repo tag: none
Pending changesets: 1
Release plan:
- Release type: minor
- Affected packages: core, api
- Release version: v0.2.0
- Next snapshot: 0.2.0-SNAPSHOT

Changesets:
- 20260418-add-release-notes.md [minor] (packages: core, api) Add release notes generation workflow.
```

Important behavior:

- if there are no pending changesets, the command prints `Release plan: none`
- the command currently prints English headings
- visible change type text is omitted when the internal type is `other`

Automation guidance:

- do not parse the bullet text or spacing
- if you need `releaseVersion` or `releaseLevel`, read the manifest instead

## 4. `manifest-field` output

`manifest-field` is the narrowest machine-readable interface.

Command:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="manifest-field --directory /path/to/repo --field releaseVersion"
```

Current behavior:

- reads `.changesets/release-plan.json`
- prints the requested field value
- is intended for CI steps such as PR titles, tag names, or publish job metadata

Common fields:

| Field | Meaning |
| --- | --- |
| `releaseVersion` | Release version without the leading `v` |
| `nextSnapshotVersion` | Next snapshot version written back into `pom.xml` or `gradle.properties` |
| `releaseLevel` | Aggregated release level |

## 5. `.changesets/release-plan.json`

This is the primary machine-readable release manifest.

Current shape:

```json
{
  "releaseVersion": "0.2.0",
  "nextSnapshotVersion": "0.2.0-SNAPSHOT",
  "releaseLevel": "minor",
  "tagStrategy": "whole-repo",
  "tags": ["v0.2.0"],
  "releaseTargets": [
    {
      "module": null,
      "tag": "v0.2.0"
    }
  ],
  "generatedAt": "2026-04-19T13:29:58.202943+08:00",
  "changesets": [
    {
      "file": "20260418-add-release-notes.md",
      "release": "minor",
      "type": "other",
      "summary": "Add release notes generation workflow.",
      "modules": ["core", "api"]
    }
  ]
}
```

Field contract:

| Field | Meaning |
| --- | --- |
| `releaseVersion` | final release version without the `v` prefix |
| `nextSnapshotVersion` | next root snapshot version after plan application |
| `releaseLevel` | aggregated release level across all included changesets |
| `tagStrategy` | configured tag strategy, currently `whole-repo` or `per-module` |
| `tags[]` | planned release tags |
| `releaseTargets[].module` | module associated with a planned tag, or `null` for whole-repo releases |
| `releaseTargets[].tag` | planned tag for this release target |
| `generatedAt` | manifest generation timestamp |
| `changesets[].file` | original changeset filename |
| `changesets[].release` | release bump for that changeset |
| `changesets[].type` | legacy compatibility field, often `other` |
| `changesets[].summary` | summary derived from the changeset body or legacy metadata |
| `changesets[].modules` | affected Maven artifactIds or Gradle project names |

Important caveats:

- the field is still called `modules` in JSON for compatibility, even though user-facing docs prefer `packages`
- `type` is not the release bump; `release` is the meaningful release signal

## 6. `.changesets/release-plan.md`

This file is designed for human review and PR or MR descriptions.

Current structure:

```md
## Release Plan

| Field | Value |
| --- | --- |
| Release type | `minor` |
| Affected packages | `core, api` |
| Release version | `v0.2.0` |
| Tag strategy | `whole-repo` |
| Planned tags | `v0.2.0` |
| Next snapshot | `0.2.0-SNAPSHOT` |

## Included Changesets

### Minor Changes

- **Add release notes generation workflow.**
  - Release: `minor`
  - Packages: `core, api`
```

Guidance:

- safe for PR bodies
- not a stable machine interface
- headings and sentence wording may evolve

## 7. `render-vars` output

`render-vars` shows which values will be treated as variables or secrets for GitHub or GitLab.

Current command:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="render-vars --env-file env/release.env.local --platform github"
```

Current GitHub layout:

```text
使用 env 文件: env/release.env.local
敏感值默认已打码。传入 --show-secrets true 可显示原值。

== GitHub Actions Variables ==
MAVEN_RELEASE_REPOSITORY_URL             https://repo.example.com/maven-releases/
MAVEN_SNAPSHOT_REPOSITORY_URL            https://repo.example.com/maven-snapshots/
MAVEN_RELEASE_REPOSITORY_ID              maven-releases
MAVEN_SNAPSHOT_REPOSITORY_ID             maven-snapshots

== GitHub Actions Secrets ==
MAVEN_REPOSITORY_USERNAME                PLACEHOLDER
MAVEN_REPOSITORY_PASSWORD                PLACEHOLDER
MAVEN_RELEASE_REPOSITORY_USERNAME        MISSING
```

Current GitLab layout:

```text
使用 env 文件: env/release.env.local
敏感值默认已打码。传入 --show-secrets true 可显示原值。

== GitLab CI/CD Variables ==
GITLAB_RELEASE_TOKEN                     OPTIONAL (fallback: CI_JOB_TOKEN)
MAVEN_RELEASE_REPOSITORY_URL             https://repo.example.com/maven-releases/
MAVEN_REPOSITORY_USERNAME                PL****ER
```

Value states currently used:

| State | Meaning |
| --- | --- |
| raw value | real configured value |
| `MISSING` | key is absent |
| `PLACEHOLDER` | key still uses `replace-me` style placeholder data |
| masked value like `ab****yz` | secret is present but hidden |

Automation guidance:

- treat this as an operator preview only
- use the env file itself as the source of truth, not the rendered table text

JSON mode:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="render-vars --env-file env/release.env.local --platform github --format json"
```

Current JSON shape:

```json
{
  "ok": true,
  "command": "render-vars",
  "envFile": "env/release.env.local",
  "platform": "github",
  "showSecrets": false,
  "sections": [
    {
      "title": "GitHub Actions Variables",
      "entries": [
        {
          "label": "MAVEN_RELEASE_REPOSITORY_URL",
          "value": "https://repo.example.com/maven-releases/"
        }
      ]
    }
  ]
}
```

Contract notes:

- stdout contains only the JSON object
- exit code `0` means the command succeeded
- `sections[].entries[].label` is the field name and `value` is the rendered value or status word

## 8. `doctor-local` output

`doctor-local` validates local runtime prerequisites, env completeness, CLI auth, and optional repository identifiers.

Current command:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="doctor-local --env-file env/release.env.local"
```

Current section layout:

```text
== 本机运行时 ==
java -version                            OK
./mvnw                                   MISSING
mvn                                      OK
Maven command                           mvn (system)
mvn -q -version                         OK

== 本地 env 文件 ==
env/release.env.local                    OK
MAVEN_REPOSITORY_USERNAME                PLACEHOLDER

== 平台 CLI ==
gh                                       OK
gh auth status                           FAILED
glab                                     MISSING

== 仓库标识 ==
GITHUB_REPO                              NOT_SET
GITLAB_REPO                              NOT_SET
```

Current status words:

| Status | Meaning |
| --- | --- |
| `OK` | the check passed |
| `MISSING` | the required tool, file, or value does not exist |
| `FAILED` | the command exists but the validation failed |
| `SKIPPED` | a dependent check was skipped |
| `PLACEHOLDER` | the value still uses placeholder data |
| `OPTIONAL` | the key is absent but optional |
| `NOT_SET` | an optional repo identifier flag was not provided |
| `INVALID` | a provided repository identifier is malformed |

Important current behavior:

- the implementation prefers `./mvnw` when it exists
- if the wrapper is absent, it falls back to a system-installed `mvn`
- failure ends with a human-readable checklist and a thrown error

JSON mode:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="doctor-local --env-file env/release.env.local --format json"
```

Current JSON behavior:

- stdout contains only one JSON object
- exit code `0` means all local checks passed
- non-zero exit code means at least one required check failed
- the payload includes `sections`, and may include `suggestions` plus final `error`

## 9. `doctor-platform` and `audit-vars`

`doctor-platform` validates local env values and authenticated platform access before sync or audit work.

Current section layout:

```text
使用 env 文件: env/release.env.local

== 本地 env 检查 ==
MAVEN_RELEASE_REPOSITORY_URL             OK
MAVEN_REPOSITORY_USERNAME                PLACEHOLDER

== GitHub CLI 检查 ==
gh                                       OK
gh auth status                           FAILED
```

`audit-vars` compares local values with remote platform state.

Current audit result words:

| Status | Meaning |
| --- | --- |
| `MATCH` | remote value matches local value |
| `PRESENT` | remote secret exists |
| `REMOTE_ONLY` | remote value exists but local input is absent or placeholder |
| `SKIPPED` | nothing meaningful to compare |
| `MISSING_REMOTE` | local real value exists but remote value is missing |
| `MISMATCH` | local and remote values differ |

`doctor-platform --format json` behaves similarly to `doctor-local --format json`:

- stdout contains only one JSON object
- exit code `0` means the env and selected platform checks passed
- non-zero exit code means auth, repo identifiers, or required env values failed validation
- the payload includes `platform`, `sections`, and optional final `error`

`audit-vars --format json` now provides a machine-readable contract as well:

- stdout contains only one JSON object
- exit code `0` means all audited remote values matched the expected local state
- non-zero exit code means at least one audited item ended in `MISSING_REMOTE` or `MISMATCH`, or a platform precondition failed
- the payload includes `platform`, `sections`, and optional final `error`

GitLab-specific additions:

- `doctor-platform --platform gitlab --format json` now includes protected-variable and protected-branch sections
- if protected variables exist but the configured `snapshotBranch` is not protected, the command fails explicitly instead of silently passing

## 10. Publish And GitLab Release JSON

`preflight`, `publish`, `gitlab-release-plan`, `gitlab-tag-from-plan`, and `gitlab-release` now expose a shared machine-readable top-level contract.

Current common fields:

| Field | Meaning |
| --- | --- |
| `ok` | whether the command succeeded |
| `command` | command name |
| `action` | action taken or planned |
| `skipped` | whether the command intentionally skipped work |
| `reason` | human-readable reason for skip, dry-run, or success summary |
| `releaseVersion` | resolved release version without extra parsing |
| `effectiveVersion` | actual version passed into the publish flow, including snapshot mode decisions |
| `releaseModule` | resolved module or `null` for whole-repo work |
| `tag` | release tag when relevant |
| `tagStrategy` | resolved tag strategy when relevant |
| `tags` | resolved release tag list when relevant |
| `releaseNotesFile` | generated or consumed notes file path when relevant |
| `projectId` | GitLab project id when relevant |
| `snapshotVersionMode` | snapshot version mode when the command is operating on a snapshot |
| `snapshotBuildStampApplied` | whether javachanges applied a stamped snapshot build suffix |

Example:

```json
{
  "ok": true,
  "command": "gitlab-release",
  "action": "create-release",
  "skipped": false,
  "reason": "Created GitLab Release.",
  "releaseVersion": "1.2.3",
  "effectiveVersion": "1.2.3",
  "releaseModule": "core",
  "tag": "core/v1.2.3",
  "tagStrategy": null,
  "tags": null,
  "releaseNotesFile": "/path/to/repo/target/release-notes.md",
  "projectId": "12345",
  "execute": true,
  "dryRun": false,
  "snapshotVersionMode": null,
  "snapshotBuildStampApplied": false
}
```

Snapshot-specific example:

```json
{
  "ok": true,
  "command": "preflight",
  "action": "publish-snapshot",
  "skipped": false,
  "reason": "Preflight checks passed.",
  "releaseVersion": "1.2.3-SNAPSHOT",
  "effectiveVersion": "1.2.3-SNAPSHOT",
  "releaseModule": null,
  "tag": null,
  "releaseNotesFile": null,
  "projectId": null,
  "execute": false,
  "dryRun": true,
  "snapshotVersionMode": "plain",
  "snapshotBuildStampApplied": false
}
```

## 11. Automation recommendations

For CI and scripting:

1. use `plan --apply true` to generate the manifest
2. read `releaseVersion` and `releaseLevel` through `manifest-field` or `release-plan.json`
3. if scripts need structured diagnostics, use `render-vars --format json`, `doctor-local --format json`, `doctor-platform --format json`, or `audit-vars --format json`
4. keep parsing generated manifests for release planning data, and use diagnostic JSON only for environment validation flows

Avoid:

- parsing aligned column spacing
- parsing localized headings like `== 本地 env 检查 ==`
- depending on the exact prose of failure summaries

## 12. Related guides

| Need | Document |
| --- | --- |
| Full command list | [CLI Reference](./cli-reference.md) |
| Generated manifest fields | [Release Plan Manifest](./release-plan-manifest.md) |
| Practical command sequences | [Command Cookbook](./command-cookbook.md) |
| Failure diagnosis | [Troubleshooting Guide](./troubleshooting-guide.md) |
