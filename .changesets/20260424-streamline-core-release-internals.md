---
"javachanges": patch
---

Refine release-planning internals and simplify changeset path handling.

This refactor removes the old `ReleaseUtils` facade, splits version planning into focused helpers, and centralizes `.changesets` path constants without changing the existing release workflow behavior.
