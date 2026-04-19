## Release Plan

- Release type: `minor`
- Affected packages: `javachanges`
- Release version: `v1.2.0`
- Next snapshot: `1.2.0-SNAPSHOT`

## Included Changesets

- `patch` `packages: javachanges` Add `--format json` support to `audit-vars`.
- `patch` `packages: javachanges` Add a bilingual command cookbook for common release workflows.
- `patch` `packages: javachanges` Add a dedicated configuration reference for `javachanges`.
- `patch` `packages: javachanges` Add `--format json` output support for `render-vars`, `doctor-local`, and `doctor-platform`.
- `patch` `packages: javachanges` Add bilingual output contract documentation for CLI and manifest consumers.
- `patch` `packages: javachanges` Add troubleshooting documentation and clarify example CI placeholders.
- `patch` `packages: javachanges` Align the release plan and status output terminology more closely with official Changesets language.
- `minor` `packages: javachanges` Align the default changeset file format with the official Changesets package-map style.
- `patch` `packages: javachanges` Expand the checked-in example repository and add a bilingual examples guide.
- `patch` `packages: javachanges` Allow local doctor and publish flows to fall back to system Maven when no wrapper is present.
- `patch` `packages: javachanges` Fix the docs deployment workflow and remove duplicated locale switch links from documentation pages.
- `patch` `packages: javachanges` Improve the documentation structure and reference coverage.
- `patch` `packages: javachanges` Document and simplify docs deployment around Cloudflare Workers Builds.

This PR was generated automatically from `.changesets/*.md` files.
Merging it will trigger an automatic tag push and then reuse the existing release workflows.
