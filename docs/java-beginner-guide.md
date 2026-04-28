---
description: Learn Java, Maven, Gradle model parsing, CLI, and plugin concepts used in javachanges through a concrete real-world codebase.
---

# Java Concepts Guide For Beginners

## 1. Overview

This guide is not a "how to study the `javachanges` codebase" document.

Instead, it uses `javachanges` as a concrete case study to explain the Java concepts that appear in a real CLI and Maven plugin project:

| Area | What you learn |
| --- | --- |
| Java basics | classes, constructors, `final`, `static`, visibility |
| Type modeling | interfaces, enums, request/result models |
| Exceptions | argument errors, state errors, IO errors |
| Standard library | `Path`, `Files`, collections, regex, `ProcessBuilder` |
| Maven and Gradle models | `pom.xml`, `gradle.properties`, plugins, `revision`, Mojos |
| Testing | JUnit 5, temp directories, output assertions |
| Engineering | dry runs, file-driven workflows, small classes |

## 2. How To Use This Guide

Recommended order:

1. Read section 3 for core Java language concepts.
2. Read section 4 for the standard library APIs used heavily in this project.
3. Read section 5 and 6 for Maven, CLI design, and tests.
4. Read section 7 and 8 to connect syntax with engineering practice.

## 3. Core Java Language Concepts Used Here

### 3.1 Classes, fields, and constructors

Representative files:

- [`core/PublishSupport.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/PublishSupport.java)
- [`core/ReleasePlanner.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/ReleasePlanner.java)

Typical pattern:

```java
final class ReleasePlanner {
    private final Path repoRoot;

    ReleasePlanner(Path repoRoot) {
        this.repoRoot = repoRoot;
    }
}
```

This teaches:

| Syntax | Meaning |
| --- | --- |
| `class` | declares a class |
| `final class` | not meant to be subclassed |
| `private final` field | assigned once during construction |
| constructor | initializes object state |

### 3.2 `final`

`final` appears everywhere in this project.

| Usage | Meaning |
| --- | --- |
| `final class` | class should not be extended |
| `final` field | field should not be reassigned |
| `final` local/parameter | value should stay stable in scope |

### 3.3 Visibility

This project is a good example of keeping visibility tight.

| Modifier | Meaning |
| --- | --- |
| `public` | exposed outside the package |
| package-private | available inside the package only |
| `private` | available only inside the class |

The rule of thumb here is simple:

> expose only what really needs to be public.

### 3.4 Static utility classes

Examples:

- [`core/ReleaseUtils.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/ReleaseUtils.java)
- [`core/ReleaseTextUtils.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/ReleaseTextUtils.java)

Typical shape:

```java
final class ReleaseTextUtils {
    private ReleaseTextUtils() {
    }

    static String trimToNull(String value) {
        // ...
    }
}
```

### 3.5 Interfaces

See:

- [`core/MavenCommandModels.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/MavenCommandModels.java)

`MavenCommandProbe` is a small example of defining capability first and implementation second.

### 3.6 Enums

See:

- [`core/ReleaseTypes.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/ReleaseTypes.java)

Key enums:

| Enum | Purpose |
| --- | --- |
| `Platform` | github / gitlab / all |
| `OutputFormat` | text / json |
| `ReleaseLevel` | patch / minor / major |

### 3.7 Exceptions

Common exception types in this project:

| Exception | Typical meaning here |
| --- | --- |
| `IllegalArgumentException` | invalid user input |
| `IllegalStateException` | invalid runtime state |
| `IOException` | file or process IO problem |
| `InterruptedException` | waiting for a process was interrupted |

## 4. Standard Library APIs Worth Learning

### 4.1 `Path` and `Files`

Representative files:

- [`core/ChangesetFileSupport.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/ChangesetFileSupport.java)
- [`core/ReleasePlanFiles.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/ReleasePlanFiles.java)

| API | Purpose |
| --- | --- |
| `Path` | represents a filesystem path |
| `Files.exists(...)` | checks existence |
| `Files.readAllBytes(...)` | reads full file content |
| `Files.readAllLines(...)` | reads lines |
| `Files.write(...)` | writes files |
| `Files.createDirectories(...)` | creates directories |

### 4.2 Collections

| Type | Typical usage |
| --- | --- |
| `List` | changesets, args, modules |
| `Map` | frontmatter, JSON fields, env values |
| `Set` | deduplicated modules |

Frequent implementations:

- `ArrayList`
- `LinkedHashMap`
- `LinkedHashSet`

### 4.3 Strings and regex

This is a text-heavy project, so string operations and regex matter a lot.

Typical tasks:

| Task | Example |
| --- | --- |
| extract `<revision>` | from `pom.xml` |
| extract Gradle `version` | from `gradle.properties` |
| parse tags | `v1.2.3` or `module/v1.2.3` |
| parse simple JSON | lightweight machine-readable output |
| rewrite XML fragments | updating `revision` |

### 4.4 `ProcessBuilder`

Representative files:

- [`core/ReleaseProcessUtils.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/ReleaseProcessUtils.java)
- [`core/PublishRuntime.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/PublishRuntime.java)

This project uses `ProcessBuilder` to run tools like `git`, `mvn`, `gh`, and `glab`.

## 5. Maven Concepts You Learn From This Project

### 5.1 Basic project structure

| Path | Meaning |
| --- | --- |
| `pom.xml` | Maven project descriptor |
| `src/main/java` | production code |
| `src/test/java` | test code |
| `target` | build output |

### 5.2 Important `pom.xml` concepts

| Concept | Meaning |
| --- | --- |
| coordinates | `groupId`, `artifactId`, `version` |
| `revision` | centralized version property |
| compiler plugin | controls Java version |
| surefire plugin | runs tests |
| plugin plugin | generates Maven plugin metadata |

### 5.3 Maven plugin and Mojo

The Maven plugin implementation lives in:

- [`src/main/java/io/github/sonofmagic/javachanges/maven`](https://github.com/sonofmagic/javachanges/tree/main/src/main/java/io/github/sonofmagic/javachanges/maven)

Simple mapping:

| Term | Meaning |
| --- | --- |
| Maven goal | `mvn javachanges:status` |
| Mojo class | Java implementation of that goal |

## 6. CLI and Testing Concepts

### 6.1 CLI entrypoint

Main entrypoint:

- [`core/JavaChangesCli.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/JavaChangesCli.java)

Typical flow:

1. receive `String[] args`
2. create the root command
3. parse arguments
4. execute command logic
5. return an exit code

### 6.2 Picocli

This project uses Picocli for argument parsing.

Key annotations:

| Annotation | Purpose |
| --- | --- |
| `@Command` | defines a command |
| `@Option` | defines an option |
| `@ParentCommand` | gives child commands access to the parent |
| `@Spec` | exposes command metadata |

### 6.3 JUnit 5

Representative tests:

- [`src/test/java/io/github/sonofmagic/javachanges/core/JavaChangesCliTest.java`](https://github.com/sonofmagic/javachanges/blob/main/src/test/java/io/github/sonofmagic/javachanges/core/JavaChangesCliTest.java)
- [`src/test/java/io/github/sonofmagic/javachanges/maven/JavaChangesMavenPluginSupportTest.java`](https://github.com/sonofmagic/javachanges/blob/main/src/test/java/io/github/sonofmagic/javachanges/maven/JavaChangesMavenPluginSupportTest.java)

Important testing ideas:

| Topic | Meaning |
| --- | --- |
| `@TempDir` | creates temp directories automatically |
| direct method execution | test logic without starting a real shell |
| stdout/stderr assertions | verify CLI output |
| file assertions | verify generated files |

## 7. Engineering Habits This Project Teaches

| Habit | Why it matters |
| --- | --- |
| dry-run first | safer automation |
| file-driven workflow | easy to review and automate |
| small focused classes | easier maintenance |
| explicit exceptions | clearer failure modes |
| structured command layering | easier testing and reuse |

## 8. Good Practice Exercises

| Exercise | What you practice |
| --- | --- |
| add one more status line | strings, CLI output, tests |
| add a small option | Picocli, models, command wiring |
| tweak changelog formatting | text processing, file writing |
| add one JSON field | maps, output generation, tests |
| extract repeated logic | refactoring and utility methods |

## 9. Summary

The main value of this project for a beginner is not its release workflow by itself.

Its value is that it shows Java being used for real engineering tasks:

| Skill | Keywords |
| --- | --- |
| object-oriented structure | classes, constructors, visibility |
| type-safe modeling | interfaces, enums, request/result models |
| filesystem work | `Path`, `Files` |
| text processing | strings, regex, `StringBuilder` |
| command execution | `ProcessBuilder` |
| build tooling | Maven, plugins, Mojos |
| testing | JUnit 5, temp dirs, assertions |

## 10. References

| Resource | Purpose |
| --- | --- |
| [Java 8 API Docs](https://docs.oracle.com/javase/8/docs/) | API reference |
| [The Java Tutorials](https://docs.oracle.com/javase/tutorial/) | official Java tutorials |
| [Maven Guides](https://maven.apache.org/guides/) | Maven concepts and plugins |
| [Picocli Documentation](https://picocli.info/) | CLI parsing |
| [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/) | testing |
