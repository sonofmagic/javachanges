## Release Plan

- Release type: `minor`
- Affected packages: `javachanges`
- Release version: `v1.5.0`
- Next snapshot: `1.5.0-SNAPSHOT`

## Included Changesets

- `patch` `packages: javachanges` Add `--format json` support for `github-release-plan`, `github-tag-from-plan`, and `github-release-from-plan`.
- `minor` `packages: javachanges` Commandize the GitLab CI/CD release flow so business repositories can keep `.gitlab-ci.yml` minimal.
- `patch` `packages: javachanges` Harden machine-readable JSON handling by replacing more hand-written JSON formatting and parsing with Jackson-based structured serialization.
- `patch` `packages: javachanges` Refactor release automation request parsing and process execution helpers to reduce duplicated CLI runtime code.

This PR was generated automatically from `.changesets/*.md` files.
Merging it will trigger an automatic tag push and then reuse the existing release workflows.
