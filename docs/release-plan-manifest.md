---
description: Understand the generated release-plan.json and release-plan.md files written by javachanges.
---

# javachanges Release Plan Manifest


## 1. Overview

When you run:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="plan --directory /path/to/repo --apply true"
```

`javachanges` can write two generated files:

| File | Purpose |
| --- | --- |
| `.changesets/release-plan.json` | Machine-readable release manifest |
| `.changesets/release-plan.md` | Human-readable release PR body |

These files are compatibility artifacts. New CI/CD automation should prefer
`--fresh true` and `--write-plan-files false` so the release branch does not
carry stale generated metadata.

## 2. Generation Timing

These files are generated when `plan --apply true` succeeds. Platform automation
can opt out with `github-release-plan --write-plan-files false` or
`gitlab-release-plan --write-plan-files false`.

That means:

- `status` does not write them
- `plan` without `--apply true` does not write them
- they represent the currently applied release plan, not just a preview
- they are ignored by this repository by default because fresh release metadata
  is derived from the current applied version state

## 3. `release-plan.json`

Typical shape:

```json
{
  "releaseVersion": "__JAVACHANGES_LATEST_RELEASE_VERSION__",
  "nextSnapshotVersion": "1.3.2-SNAPSHOT",
  "releaseLevel": "minor",
  "tagStrategy": "whole-repo",
  "tags": ["v__JAVACHANGES_LATEST_RELEASE_VERSION__"],
  "releaseTargets": [
    {
      "module": null,
      "tag": "v__JAVACHANGES_LATEST_RELEASE_VERSION__"
    }
  ],
  "generatedAt": "2026-04-19T12:34:56+08:00",
  "changesets": [
    {
      "file": "20260419-example.md",
      "release": "minor",
      "type": "other",
      "summary": "add release notes command",
      "modules": ["javachanges"]
    }
  ]
}
```

Field reference:

| Field | Type | Meaning |
| --- | --- | --- |
| `releaseVersion` | string | Final release version without the leading `v` |
| `nextSnapshotVersion` | string | Root version written back into `pom.xml` or `gradle.properties` after plan application |
| `releaseLevel` | string | Aggregated release type across all pending changesets |
| `tagStrategy` | string | `whole-repo` or `per-module` |
| `tags` | array | Planned tags derived from the selected tag strategy |
| `releaseTargets` | array | Structured tag targets with module and tag values |
| `generatedAt` | string | Timestamp when the manifest was generated |
| `changesets` | array | Included pending changesets that were consumed |

Changeset item fields:

| Field | Type | Meaning |
| --- | --- | --- |
| `file` | string | Original changeset filename |
| `release` | string | Declared release type for that changeset |
| `type` | string | Legacy compatibility field, often `other`, safe to ignore in new integrations |
| `summary` | string | User-facing summary derived from the changeset body or legacy frontmatter |
| `modules` | array | Maven artifactIds or Gradle project names affected by that changeset |

Release target item fields:

| Field | Type | Meaning |
| --- | --- | --- |
| `module` | string or `null` | Resolved module name for per-module tags, or `null` for whole-repo tags |
| `tag` | string | Final git tag to create for that target |

> Note: the JSON field is still named `modules` for compatibility with the current implementation, even though the user-facing docs now prefer the term `packages`.
>
> Note: if your changeset uses the official package-map format, the manifest may still emit `"type": "other"`. That field is not the release bump. Use `release` as the meaningful release signal.

## 4. `release-plan.md`

This file is the generated pull request body.

Typical content includes:

- release type
- affected packages
- release version
- next snapshot version
- included changesets

It is intended for:

- GitHub pull request bodies
- GitLab merge request descriptions
- human review before merging a release branch

## 5. Common Consumers

### 5.1 Local shell scripts

Read a field with:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="manifest-field --directory /path/to/repo --field releaseVersion"
mvn -q -DskipTests compile exec:java -Dexec.args="manifest-field --directory /path/to/repo --field releaseVersion --fresh true"
```

### 5.2 GitHub Actions

Typical uses:

- derive `releaseVersion` with `manifest-field --fresh true`
- derive the final release tag set with `github-tag-from-plan --fresh true`
- generate a transient PR body with `github-release-plan --write-plan-files false`
- for Gradle repositories, pass `releaseVersion` to `./gradlew publish -Pversion=...` if your publishing job needs an explicit release version

### 5.3 GitLab CI/CD

Typical uses:

- detect whether the version file or `CHANGELOG.md` changed when `gitlab-tag-from-plan --fresh true` is used
- generate the final tag set only when a new applied plan exists
- create or update the release-plan merge request body without committing generated plan files

## 6. Related Files Updated At The Same Time

When the manifest is generated, these files are usually updated in the same commit:

| File | Why it changes |
| --- | --- |
| `pom.xml` or `gradle.properties` | Root Maven `<revision>` or Gradle version advances to the next snapshot |
| `CHANGELOG.md` | A new release section is inserted |
| `.changesets/release-plan.json` | Machine-readable release data when compatibility manifest output is enabled |
| `.changesets/release-plan.md` | Human-readable release PR body when compatibility manifest output is enabled |

At the same time, the consumed `.changesets/*.md` entries are deleted.

## 7. Common Mistakes

| Problem | Cause | Fix |
| --- | --- | --- |
| `manifest-field` fails | the compatibility manifest was not written | use `manifest-field --fresh true` or generate and apply the release plan first |
| No tag is created in CI | release state did not change | confirm a release plan updated the version file and `CHANGELOG.md` |
| Release PR body is stale | generated plan files were committed and not regenerated | use `--write-plan-files false` so the body is regenerated transiently |
| Version mismatch in CI | workflow reads stale generated files | use `manifest-field --field releaseVersion --fresh true` |

## 8. Related Guides

| Need | Document |
| --- | --- |
| Command to read manifest fields | [CLI Reference](./cli-reference.md) |
| First-time use of `plan --apply true` | [Getting Started](./getting-started.md) |
| Gradle release handoff | [Gradle Usage Guide](./gradle-guide.md) |
| GitHub workflow consumption | [GitHub Actions Usage Guide](./github-actions-guide.md) |
| GitLab workflow consumption | [GitLab CI/CD Usage Guide](./gitlab-ci-guide.md) |
