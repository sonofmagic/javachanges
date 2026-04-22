---
"javachanges": patch
---

Make `gitlab-release-plan --execute true` more idempotent when a stale remote
`changeset-release/<default-branch>` branch still exists, and document the
repository rule that every shipped feature change must include a changeset.
