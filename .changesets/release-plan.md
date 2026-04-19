## Release Plan

- Release type: `minor`
- Affected packages: `javachanges`
- Release version: `v1.3.0`
- Next snapshot: `1.3.0-SNAPSHOT`

## Included Changesets

- `minor` `packages: javachanges` Add Maven plugin invocation support so `javachanges` can run directly through Maven goals instead of only `java -jar` or `exec:java`.
- `patch` `packages: javachanges` Update installation docs to use the published Maven Central release instead of source-only examples.

This PR was generated automatically from `.changesets/*.md` files.
Merging it will trigger an automatic tag push and then reuse the existing release workflows.
