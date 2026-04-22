---
"javachanges": minor
---

Add an optional `per-module` release tag strategy for release-plan automation.

`javachanges` can now read `tagStrategy` from `.changesets/config.json` or `.changesets/config.jsonc`.
When set to `per-module`, `plan --apply true` records planned module tags in the release manifest and
`github-tag-from-plan` / `gitlab-tag-from-plan` create one `artifactId/vX.Y.Z` tag per affected module.
The default behavior remains the existing whole-repo `vX.Y.Z` tag strategy.
