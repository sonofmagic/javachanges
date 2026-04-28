## Release Plan

| Field | Value |
| --- | --- |
| Release type | `patch` |
| Affected packages | `javachanges` |
| Release version | `v1.6.1` |
| Tag strategy | `whole-repo` |
| Planned tags | `v1.6.1` |
| Next snapshot | `1.6.1-SNAPSHOT` |

## Included Changesets

### Patch Changes

- **Refine release-planning internals and simplify changeset path handling.**
  - Release: `patch`
  - Packages: `javachanges`
  - Notes: Refine release-planning internals and simplify changeset path handling.
- **Add Maven Wrapper scripts and update local development, CI, and snapshot helper commands to use the repository-pinned Maven runtime.**
  - Release: `patch`
  - Packages: `javachanges`
  - Notes: Add Maven Wrapper scripts and update local development, CI, and snapshot helper commands to use the repository-pinned Maven runtime.
- **Avoid writing real Maven settings or local repository directories during publish dry-runs.**
  - Release: `patch`
  - Packages: `javachanges`
  - Notes: Avoid writing real Maven settings or local repository directories during publish dry-runs.
- **Avoid writing release notes and GitHub output files during release dry-runs.**
  - Release: `patch`
  - Packages: `javachanges`
  - Notes: Avoid writing release notes and GitHub output files during release dry-runs.
- **Fail closed when remote release tag lookups cannot reach the configured Git remote.**
  - Release: `patch`
  - Packages: `javachanges`
  - Notes: Fail closed when remote release tag lookups cannot reach the configured Git remote.
- **Harden release automation by cleaning preflight credential files immediately and making GitLab API calls fail faster on network or empty-error responses.**
  - Release: `patch`
  - Packages: `javachanges`
  - Notes: Harden release automation by cleaning preflight credential files immediately and making GitLab API calls fail faster on network or empty-error responses.
- **Mask secret values in sync-vars execute logs while still passing real values to platform CLIs.**
  - Release: `patch`
  - Packages: `javachanges`
  - Notes: Mask secret values in sync-vars execute logs while still passing real values to platform CLIs.
- **Redact authenticated GitLab remote URLs from git failure messages.**
  - Release: `patch`
  - Packages: `javachanges`
  - Notes: Redact authenticated GitLab remote URLs from git failure messages.
- **Restrict generated Maven settings files to owner-only permissions when the filesystem supports POSIX permissions.**
  - Release: `patch`
  - Packages: `javachanges`
  - Notes: Restrict generated Maven settings files to owner-only permissions when the filesystem supports POSIX permissions.
- **Stabilize GPG public key checks by capturing process output concurrently and reusing the shared temporary-directory cleanup path.**
  - Release: `patch`
  - Packages: `javachanges`
  - Notes: Stabilize GPG public key checks by capturing process output concurrently and reusing the shared temporary-directory cleanup path.

This PR was generated automatically from `.changesets/*.md` files.
Merging it will trigger an automatic tag push and then reuse the existing release workflows.
