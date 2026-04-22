---
description: Understand the generated release-plan.json and release-plan.md files written by javachanges.
---

# javachanges Release Plan Manifest


## 1. Overview

When you run:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="plan --directory /path/to/repo --apply true"
```

`javachanges` writes two generated files:

| File | Purpose |
| --- | --- |
| `.changesets/release-plan.json` | Machine-readable release manifest |
| `.changesets/release-plan.md` | Human-readable release PR body |

This page explains what is inside those files and how CI/CD usually consumes them.

## 2. Generation Timing

These files are generated only when `plan --apply true` succeeds.

That means:

- `status` does not write them
- `plan` without `--apply true` does not write them
- they represent the currently applied release plan, not just a preview

## 3. `release-plan.json`

Typical shape:

```json
{
  "releaseVersion": "__JAVACHANGES_LATEST_RELEASE_VERSION__",
  "nextSnapshotVersion": "1.3.2-SNAPSHOT",
  "releaseLevel": "minor",
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
| `nextSnapshotVersion` | string | Root version written back into `pom.xml` after plan application |
| `releaseLevel` | string | Aggregated release type across all pending changesets |
| `generatedAt` | string | Timestamp when the manifest was generated |
| `changesets` | array | Included pending changesets that were consumed |

Changeset item fields:

| Field | Type | Meaning |
| --- | --- | --- |
| `file` | string | Original changeset filename |
| `release` | string | Declared release type for that changeset |
| `type` | string | Legacy compatibility field, often `other`, safe to ignore in new integrations |
| `summary` | string | User-facing summary derived from the changeset body or legacy frontmatter |
| `modules` | array | Maven artifactIds affected by that changeset |

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
```

### 5.2 GitHub Actions

Typical uses:

- read `releaseVersion` for the PR title
- read `releaseVersion` when creating the final release tag
- attach `.changesets/release-plan.md` as the PR body

### 5.3 GitLab CI/CD

Typical uses:

- detect whether `release-plan.json` changed between commits
- generate the final tag only when a new applied plan exists
- create or update the release-plan merge request body

## 6. Related Files Updated At The Same Time

When the manifest is generated, these files are usually updated in the same commit:

| File | Why it changes |
| --- | --- |
| `pom.xml` | Root `<revision>` advances to the next snapshot |
| `CHANGELOG.md` | A new release section is inserted |
| `.changesets/release-plan.json` | Machine-readable release data |
| `.changesets/release-plan.md` | Human-readable release PR body |

At the same time, the consumed `.changesets/*.md` entries are deleted.

## 7. Common Mistakes

| Problem | Cause | Fix |
| --- | --- | --- |
| `manifest-field` fails | `plan --apply true` has not been run yet | generate and apply the release plan first |
| No tag is created in CI | `release-plan.json` did not change | confirm a new plan was applied and committed |
| Release PR body is stale | `.changesets/release-plan.md` was not regenerated | rerun the release-plan job |
| Version mismatch in CI | workflow reads `pom.xml` instead of the manifest | use `manifest-field --field releaseVersion` |

## 8. Related Guides

| Need | Document |
| --- | --- |
| Command to read manifest fields | [CLI Reference](./cli-reference.md) |
| First-time use of `plan --apply true` | [Getting Started](./getting-started.md) |
| GitHub workflow consumption | [GitHub Actions Usage Guide](./github-actions-guide.md) |
| GitLab workflow consumption | [GitLab CI/CD Usage Guide](./gitlab-ci-guide.md) |
