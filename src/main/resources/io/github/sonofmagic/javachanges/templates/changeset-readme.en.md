# Changesets

This directory stores pending release notes. Add one changeset for each user-visible change.

Create a changeset:

```bash
javachanges add --directory . --summary "describe the change" --release patch
```

For multi-module repositories, use detected module names:

```bash
javachanges modules --directory .
javachanges add --directory . --modules core --summary "describe the change" --release patch
```

Changesets use the official package-map frontmatter shape:

```md
---
"core": minor
---

Describe the user-visible change.
```

Supported release levels are `patch`, `minor`, and `major`.

Review and apply the release plan:

```bash
javachanges status --directory .
javachanges plan --directory . --apply true
```
