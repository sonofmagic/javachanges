---
"javachanges": minor
---

Align the default changeset file format with the official Changesets package-map style.

- Replace the hand-written CLI dispatcher with picocli subcommands and built-in help output.
- Keep existing flag forms such as `--apply true`, `--execute true`, and `--snapshot` working.
- Group changelog output by release level so the default `other` metadata does not leak into user-facing sections.
- Write new changesets as `"artifactId": patch|minor|major` entries instead of the older `release/modules/summary` frontmatter.
- Keep reading legacy `javachanges` frontmatter for backwards compatibility.
- Update the README and getting-started guides around the official format and self-release workflow.
