---
"javachanges": patch
---

Make GitHub release tag creation safer to retry by accepting existing tags that already point at the target commit, rejecting tags that point elsewhere, and creating only missing per-module tags.
