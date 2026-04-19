---
"javachanges": minor
---

Align the default changeset file format with the official Changesets package-map style.

- Write new changesets as `"artifactId": patch|minor|major` entries instead of the older `release/modules/summary` frontmatter.
- Keep reading legacy `javachanges` frontmatter for backwards compatibility.
- Update the README and getting-started guides to recommend the official format for new files.
