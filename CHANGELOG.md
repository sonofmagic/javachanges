# Changelog

All notable changes to this project will be documented in this file.

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
