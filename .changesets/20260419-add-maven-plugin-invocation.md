---
"javachanges": minor
---

Add Maven plugin invocation support so `javachanges` can run directly through Maven goals instead of only `java -jar` or `exec:java`.

- Add dedicated `status`, `plan`, `add`, and `manifest-field` Maven goals for the most common workflows.
- Keep a generic `run` goal as a bridge for commands that do not have a dedicated goal yet.
- Default the plugin repository directory to `${project.basedir}` for shorter usage inside target repositories.
- Document the plugin flow alongside the existing `java -jar` and source `exec:java` workflows.
