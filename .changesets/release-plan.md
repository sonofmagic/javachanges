## Release Plan

| Field | Value |
| --- | --- |
| Release type | `minor` |
| Affected packages | `javachanges` |
| Release version | `v1.8.0` |
| Tag strategy | `whole-repo` |
| Planned tags | `v1.8.0` |
| Next snapshot | `1.8.0-SNAPSHOT` |

## Included Changesets

### Minor Changes

- **Add `gradle-publish` to render or execute Gradle-native publish commands from the same release and snapshot version metadata used by the Maven publish helper.**
  - Release: `minor`
  - Packages: `javachanges`
- **Add `init-github-actions` to generate Maven or Gradle GitHub Actions release workflows.**
  - Release: `minor`
  - Packages: `javachanges`
- **Add Gradle template support to `init-gitlab-ci`, including auto build-tool detection and generated pipelines that call `gradle-publish`.**
  - Release: `minor`
  - Packages: `javachanges`

### Patch Changes

- **Avoid duplicating official changeset summaries in generated changelog and release-plan notes, and support multiline Gradle `include(...)` module declarations.**
  - Release: `patch`
  - Packages: `javachanges`
- **Keep generated GitHub Actions and GitLab CI templates on the same default javachanges version.**
  - Release: `patch`
  - Packages: `javachanges`

This PR was generated automatically from `.changesets/*.md` files.
Merging it will trigger an automatic tag push and then reuse the existing release workflows.
