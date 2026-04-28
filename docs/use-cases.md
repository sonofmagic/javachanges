---
description: Practical release-planning and publishing scenarios where javachanges fits Maven and Gradle repositories.
---

# Use Cases


## Maven library monorepo

Use `javachanges` to manage version bumps across multiple artifacts while keeping a single reviewed release plan.

## Single-module Maven CLI or library

Use `javachanges` to keep a file-based release workflow even when the repository only has one publishable Maven artifact.

## Gradle multi-project build

Use `javachanges` to manage release intent across Gradle projects declared by `include(...)`, update `gradle.properties`, and generate a release-plan manifest before Gradle publishing runs.

For setup details, see [Gradle Usage Guide](./gradle-guide.md).

## Single-project Gradle library or application

Use `javachanges` when a Gradle repository has one root project and still needs reviewed release notes, changelog generation, and CI-friendly release tags.

## Internal platform release automation

Use `write-settings`, `render-vars`, `doctor-platform`, and `audit-vars` to standardize CI variables and Maven credentials across repositories.

For GitHub-based pipelines, see [GitHub Actions Usage Guide](./github-actions-guide.md).

## GitLab release MR flow

Use `gitlab-release-plan` and `gitlab-tag-from-plan` when you want release branches, merge requests, and tags to be generated from pending changesets.

For a full GitLab pipeline example, see [GitLab CI/CD Usage Guide](./gitlab-ci-guide.md).

## Safer publish dry-runs

Use `preflight` and `publish --execute false` to preview the exact Maven deploy command and generated settings before touching a real repository.

For Gradle artifacts, use the release-plan manifest as input to `./gradlew publish` and keep publication logic inside Gradle.
