# Changelog

All notable changes to this project will be documented in this file.

## 1.12.0 - 2026-05-02

### Minor Changes

- Expand doctor-publish into a Maven and Gradle publish readiness doctor with module-aware checks, repository URL validation, release tag and worktree validation, Maven Central plugin/profile checks, effective snapshot version reporting, Gradle next-command support, shell-quoted next commands, and repair suggestions in text and JSON output. (packages: javachanges)
- Improve release workflow tooling with Gradle task bootstrap shortcuts, local CI simulation commands, expanded release dry-runs for doctor/preflight/publish checks, a directly executable `java -jar` CLI artifact, generated documentation placeholder checks, and safer GitHub release tag retry recovery. (packages: javachanges)

### Patch Changes

- Add dedicated Maven plugin goals for CI helpers, env setup and operations, GitHub and GitLab release automation, Gradle publishing, GPG public key publishing, CI template generation, and Maven settings generation. Env and settings helpers now also support Sonatype Central Portal credentials plus release-only and snapshot-only settings modes. (packages: javachanges)

## 1.11.0 - 2026-04-30

### Minor Changes

- Add reusable GitLab CI release helpers for release commit tag fallback and GitLab Catalog release-page tolerance. (packages: javachanges)

## 1.10.3 - 2026-04-30

### Patch Changes

- Allow release publishing to create the tag from a merged release apply commit after main CI succeeds. (packages: javachanges)
- Move GitHub release publish-state decisions into javachanges and simplify the repository release workflows. (packages: javachanges)

## 1.10.2 - 2026-04-30

### Patch Changes

- Avoid duplicate release publishing by running the publish workflow only after main CI succeeds. (packages: javachanges)

## 1.10.1 - 2026-04-30

### Patch Changes

- Allow GitLab release tagging to fall back to fresh release metadata when release-plan files are not committed. (packages: javachanges)

## 1.10.0 - 2026-04-29

### Minor Changes

- Add `add --format json` for scripts that need the created changeset path, affected packages, and next commands. (packages: javachanges)
- Add JSON output for the core status, plan, and next workflow commands. (packages: javachanges)
- Add a first-run setup command with safe defaults and optional env or CI template generation. (packages: javachanges)
- Add `modules --format json` and Maven goal format passthrough for module discovery and workflow status commands. (packages: javachanges)
- Write a release plan backup before applying and add a restore path for local recovery. (packages: javachanges)
- Add JSON output for release tag parsing helpers so scripts can read release version and module metadata together. (packages: javachanges)
- Add a validate command for local release readiness checks. (packages: javachanges)
- Add `version --format json` and Maven goal format passthrough for version metadata automation. (packages: javachanges)
- Improve `add` input handling with `--no-interactive` failures and detected module prompts for multi-module repositories. (packages: javachanges)

### Patch Changes

- Move release automation report reasons into the localized message bundles. (packages: javachanges)
- Move release automation text output into the localized message bundles. (packages: javachanges)
- Simplify localized message formatting so translators can write apostrophes naturally in message resources. (packages: javachanges)
- Move core version, config, and build model error messages into the localized message bundles. (packages: javachanges)
- Move release environment and platform doctor messages into the localized message bundles. (packages: javachanges)
- Move GitLab protection check headings into the localized message bundles. (packages: javachanges)
- Move GPG key publishing progress messages into the localized message bundles. (packages: javachanges)
- Move Maven plugin execution log messages into the localized message bundles. (packages: javachanges)
- Move Git, workflow, GPG, and platform API messages into the localized message bundles. (packages: javachanges)
- Move remaining publish dry-run labels and automation reasons into the localized message bundles. (packages: javachanges)
- Move publish preflight and execution messages into the localized message bundles. (packages: javachanges)
- Move generated changeset README and auth help text into localized UTF-8 templates. (packages: javachanges)
- Move release metadata, protected variable, and release notes section messages into the localized bundles. (packages: javachanges)
- Validate localized message placeholders across languages to catch incomplete translations earlier. (packages: javachanges)

## 1.9.0 - 2026-04-29

### Minor Changes

- Add fresh release metadata mode so automation can avoid committing generated release-plan files. (packages: javachanges)
- Add an init command for guided changeset setup. (packages: javachanges)
- Add a `modules` command and friendlier unknown-module guidance. (packages: javachanges)
- Add configurable output language support with English as the default and Chinese rendering for core CLI prompts, errors, and generated release Markdown. (packages: javachanges)
- Add dedicated Maven plugin goals for version, preflight, publish, and release-notes commands. (packages: javachanges)
- Add a `next` command that suggests the next release workflow step for the current repository. (packages: javachanges)

### Patch Changes

- Print review-oriented next steps after creating a changeset. (packages: javachanges)
- Echo the resolved release level and affected packages after creating a changeset. (packages: javachanges)
- Improve Gradle settings parsing for renamed projects and comments. (packages: javachanges)
- Reject generated output paths that escape the repository and fail fast on malformed changeset config. (packages: javachanges)
- Allow init force mode to refresh the generated changeset README. (packages: javachanges)
- Print copyable add examples after listing detected modules. (packages: javachanges)
- Show the GitLab release-plan command in next-step guidance. (packages: javachanges)
- Print review and commit next steps after applying a release plan. (packages: javachanges)
- Print apply-oriented next steps from plan dry-runs and empty apply attempts. (packages: javachanges)
- Show allowed release levels when changeset creation receives an unsupported value. (packages: javachanges)
- Write a more helpful changeset README with starter examples. (packages: javachanges)
- Make generated release pull request and merge request bodies easier to scan with emoji-labelled sections, fields, and next-step guidance. (packages: javachanges)
- Print next-step commands at the end of status output. (packages: javachanges)

## 1.8.0 - 2026-04-28

### Minor Changes

- Add `gradle-publish` to render or execute Gradle-native publish commands from the same release and snapshot version metadata used by the Maven publish helper. (packages: javachanges)
- Add `init-github-actions` to generate Maven or Gradle GitHub Actions release workflows. (packages: javachanges)
- Add Gradle template support to `init-gitlab-ci`, including auto build-tool detection and generated pipelines that call `gradle-publish`. (packages: javachanges)

### Patch Changes

- Avoid duplicating official changeset summaries in generated changelog and release-plan notes, and support multiline Gradle `include(...)` module declarations. (packages: javachanges)
- Keep generated GitHub Actions and GitLab CI templates on the same default javachanges version. (packages: javachanges)

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
