---
release: minor
summary: improve javachanges CLI ergonomics and self-release documentation
---

- Replace the hand-written CLI dispatcher with picocli subcommands and built-in help output.
- Keep existing flag forms such as `--apply true`, `--execute true`, and `--snapshot` working.
- Group changelog output by release level so the default `other` metadata does not leak into user-facing sections.
- Expand the README and getting-started docs around the minimal changeset format and self-release workflow.
