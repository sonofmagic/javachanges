---
"javachanges": minor
---

Add Maven plugin invocation support so `javachanges` can run directly through `mvn ...:run` without downloading the jar first.

- Bridge the existing CLI commands through a new Maven `run` goal.
- Default the plugin repository directory to `${project.basedir}` for shorter usage inside target repositories.
- Document the released-package plugin flow alongside the existing `java -jar` and source `exec:java` workflows.
