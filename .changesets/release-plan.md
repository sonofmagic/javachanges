## Release Plan

| Field | Value |
| --- | --- |
| Release type | `minor` |
| Affected packages | `javachanges` |
| Release version | `v1.6.0` |
| Tag strategy | `whole-repo` |
| Planned tags | `v1.6.0` |
| Next snapshot | `1.6.0-SNAPSHOT` |

## Included Changesets

### Minor Changes

- **Add an optional `per-module` release tag strategy for release-plan automation.**
  - Release: `minor`
  - Packages: `javachanges`
  - Notes: Add an optional `per-module` release tag strategy for release-plan automation.
- **Add plain snapshot version mode so snapshot publishes can keep the original `-SNAPSHOT` revision while preserving the existing stamped default.**
  - Release: `minor`
  - Packages: `javachanges`
  - Notes: Add plain snapshot version mode so snapshot publishes can keep the original `-SNAPSHOT` revision while preserving the existing stamped default.

### Patch Changes

- **Improve generated release PR Markdown layout.**
  - Release: `patch`
  - Packages: `javachanges`
  - Notes: Improve generated release PR Markdown layout.

This PR was generated automatically from `.changesets/*.md` files.
Merging it will trigger an automatic tag push and then reuse the existing release workflows.
