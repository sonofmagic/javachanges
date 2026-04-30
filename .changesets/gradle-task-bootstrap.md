---
"javachanges": minor
---

Add init-gradle-tasks to generate and optionally apply Gradle task shortcuts for common javachanges workflows. Generated Gradle tasks include review-oriented aliases such as `javachangesStatusJson`, `javachangesApplyPlan`, and `javachangesRestorePlan`; can use a local CLI jar with `-Pjavachanges.jar=...`; pass JVM options with `-Pjavachanges.jvmArgs=...`; pass global CLI options through `-Pjavachanges.directory=...` and `-Pjavachanges.language=...`; override modeled options per command with properties like `-Pjavachanges.status.format=...`; append temporary raw CLI options through `-Pjavachanges.extraArgs=...` or command-scoped properties like `-Pjavachanges.status.extraArgs=...`; and fall back to `mavenCentral()` only when the build has no repositories configured.
