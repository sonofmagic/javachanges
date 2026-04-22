---
description: Practical release-planning and publishing scenarios where javachanges fits Maven repositories and CI systems.
---

# Use Cases


## Maven library monorepo

Use `javachanges` to manage version bumps across multiple artifacts while keeping a single reviewed release plan.

## Single-module Maven CLI or library

Use `javachanges` to keep a file-based release workflow even when the repository only has one publishable Maven artifact.

## Internal platform release automation

Use `write-settings`, `render-vars`, `doctor-platform`, and `audit-vars` to standardize CI variables and Maven credentials across repositories.

For GitHub-based pipelines, see [GitHub Actions Usage Guide](./github-actions-guide.md).

## GitLab release MR flow

Use `gitlab-release-plan` and `gitlab-tag-from-plan` when you want release branches, merge requests, and tags to be generated from pending changesets.

For a full GitLab pipeline example, see [GitLab CI/CD Usage Guide](./gitlab-ci-guide.md).

## Safer publish dry-runs

Use `preflight` and `publish --execute false` to preview the exact Maven deploy command and generated settings before touching a real repository.
