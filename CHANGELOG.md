# Changelog

All notable changes to this project will be documented in this file.

## 1.7.0 - 2026-04-28

### Minor Changes

- Support Gradle repositories in release planning, and add Gradle usage docs plus a copy-ready Gradle example repository. (packages: javachanges) Support Gradle repositories in release planning, and add Gradle usage docs plus a copy-ready Gradle example repository.

## 1.6.1 - 2026-04-28

### Patch Changes

- Refine release-planning internals and simplify changeset path handling. (packages: javachanges) Refine release-planning internals and simplify changeset path handling.
- Add Maven Wrapper scripts and update local development, CI, and snapshot helper commands to use the repository-pinned Maven runtime. (packages: javachanges) Add Maven Wrapper scripts and update local development, CI, and snapshot helper commands to use the repository-pinned Maven runtime.
- Avoid writing real Maven settings or local repository directories during publish dry-runs. (packages: javachanges) Avoid writing real Maven settings or local repository directories during publish dry-runs.
- Avoid writing release notes and GitHub output files during release dry-runs. (packages: javachanges) Avoid writing release notes and GitHub output files during release dry-runs.
- Fail closed when remote release tag lookups cannot reach the configured Git remote. (packages: javachanges) Fail closed when remote release tag lookups cannot reach the configured Git remote.
- Harden release automation by cleaning preflight credential files immediately and making GitLab API calls fail faster on network or empty-error responses. (packages: javachanges) Harden release automation by cleaning preflight credential files immediately and making GitLab API calls fail faster on network or empty-error responses.
- Mask secret values in sync-vars execute logs while still passing real values to platform CLIs. (packages: javachanges) Mask secret values in sync-vars execute logs while still passing real values to platform CLIs.
- Redact authenticated GitLab remote URLs from git failure messages. (packages: javachanges) Redact authenticated GitLab remote URLs from git failure messages.
- Restrict generated Maven settings files to owner-only permissions when the filesystem supports POSIX permissions. (packages: javachanges) Restrict generated Maven settings files to owner-only permissions when the filesystem supports POSIX permissions.
- Stabilize GPG public key checks by capturing process output concurrently and reusing the shared temporary-directory cleanup path. (packages: javachanges) Stabilize GPG public key checks by capturing process output concurrently and reusing the shared temporary-directory cleanup path.

## 1.6.0 - 2026-04-22

### Minor Changes

- Add an optional `per-module` release tag strategy for release-plan automation. (packages: javachanges) Add an optional `per-module` release tag strategy for release-plan automation.
- Add plain snapshot version mode so snapshot publishes can keep the original `-SNAPSHOT` revision while preserving the existing stamped default. (packages: javachanges) Add plain snapshot version mode so snapshot publishes can keep the original `-SNAPSHOT` revision while preserving the existing stamped default.

### Patch Changes

- Improve generated release PR Markdown layout. (packages: javachanges) Improve generated release PR Markdown layout.

## 1.5.0 - 2026-04-22

### Minor Changes

- Commandize the GitLab CI/CD release flow so business repositories can keep `.gitlab-ci.yml` minimal. (packages: javachanges) Commandize the GitLab CI/CD release flow so business repositories can keep `.gitlab-ci.yml` minimal.

### Patch Changes

- Add `--format json` support for `github-release-plan`, `github-tag-from-plan`, and `github-release-from-plan`. (packages: javachanges) Add `--format json` support for `github-release-plan`, `github-tag-from-plan`, and `github-release-from-plan`.
- Harden machine-readable JSON handling by replacing more hand-written JSON formatting and parsing with Jackson-based structured serialization. (packages: javachanges) Harden machine-readable JSON handling by replacing more hand-written JSON formatting and parsing with Jackson-based structured serialization.
- Refactor release automation request parsing and process execution helpers to reduce duplicated CLI runtime code. (packages: javachanges) Refactor release automation request parsing and process execution helpers to reduce duplicated CLI runtime code.

## 1.4.1 - 2026-04-22

### Patch Changes

- Make `gitlab-release-plan --execute true` more idempotent when a stale remote (packages: javachanges) Make `gitlab-release-plan --execute true` more idempotent when a stale remote

## 1.4.0 - 2026-04-21

### Minor Changes

- simplify GitHub release metadata sync in Actions workflows (packages: javachanges) simplify GitHub release metadata sync in Actions workflows
- add unique snapshot publish revisions and split GitHub Actions snapshot/release publishing flows, including snapshot branch publishing (packages: javachanges) add unique snapshot publish revisions and split GitHub Actions snapshot/release publishing flows, including snapshot branch publishing

## 1.3.1 - 2026-04-19

### Patch Changes

- Clarify which Maven Central POM metadata fields improve the Sonatype Central artifact page. (packages: javachanges) Clarify which Maven Central POM metadata fields improve the Sonatype Central artifact page.

## 1.3.0 - 2026-04-19

### Minor Changes

- Add Maven plugin invocation support so `javachanges` can run directly through Maven goals instead of only `java -jar` or `exec:java`. (packages: javachanges) Add Maven plugin invocation support so `javachanges` can run directly through Maven goals instead of only `java -jar` or `exec:java`.

### Patch Changes

- Update installation docs to use the published Maven Central release instead of source-only examples. (packages: javachanges) Update installation docs to use the published Maven Central release instead of source-only examples.

## 1.2.0 - 2026-04-19

### Minor Changes

- Align the default changeset file format with the official Changesets package-map style. (packages: javachanges) Align the default changeset file format with the official Changesets package-map style.

### Patch Changes

- Add `--format json` support to `audit-vars`. (packages: javachanges) Add `--format json` support to `audit-vars`.
- Add a bilingual command cookbook for common release workflows. (packages: javachanges) Add a bilingual command cookbook for common release workflows.
- Add a dedicated configuration reference for `javachanges`. (packages: javachanges) Add a dedicated configuration reference for `javachanges`.
- Add `--format json` output support for `render-vars`, `doctor-local`, and `doctor-platform`. (packages: javachanges) Add `--format json` output support for `render-vars`, `doctor-local`, and `doctor-platform`.
- Add bilingual output contract documentation for CLI and manifest consumers. (packages: javachanges) Add bilingual output contract documentation for CLI and manifest consumers.
- Add troubleshooting documentation and clarify example CI placeholders. (packages: javachanges) Add troubleshooting documentation and clarify example CI placeholders.
- Align the release plan and status output terminology more closely with official Changesets language. (packages: javachanges) Align the release plan and status output terminology more closely with official Changesets language.
- Expand the checked-in example repository and add a bilingual examples guide. (packages: javachanges) Expand the checked-in example repository and add a bilingual examples guide.
- Allow local doctor and publish flows to fall back to system Maven when no wrapper is present. (packages: javachanges) Allow local doctor and publish flows to fall back to system Maven when no wrapper is present.
- Fix the docs deployment workflow and remove duplicated locale switch links from documentation pages. (packages: javachanges) Fix the docs deployment workflow and remove duplicated locale switch links from documentation pages.
- Improve the documentation structure and reference coverage. (packages: javachanges) Improve the documentation structure and reference coverage.
- Document and simplify docs deployment around Cloudflare Workers Builds. (packages: javachanges) Document and simplify docs deployment around Cloudflare Workers Builds.

## 1.1.1 - 2026-04-18

### Patch Changes

- fix Maven Central publishing for CI-friendly revision versions (modules: javachanges)

## 1.1.0 - 2026-04-18

### Minor Changes

- automate javachanges self-release publishing via GitHub Actions (modules: javachanges) - Add GitHub Actions workflows for CI, release-plan PR generation, and Maven Central publishing.
