# Changesets

This directory stores pending release intent for the Gradle example.

Use Gradle project names from `settings.gradle.kts` as package keys:

```md
---
"core": minor
"api": minor
---

Add release notes generation workflow.
```

Run from the `javachanges` source repository root:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="status --directory examples/basic-gradle-monorepo"
```
