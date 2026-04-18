# Getting Started

## 1. Build the CLI

```bash
mvn -q test
```

## 2. Prepare a target repository

Your target repository should have:

- git initialized
- a root `pom.xml`
- a `<revision>` property
- a `CHANGELOG.md` file, or let `javachanges` create/update it during plan application
- either `<modules>` in the root pom, or a single root artifact

## 3. Create a changeset

Monorepo example:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="add --directory /path/to/repo --summary 'add release notes command' --release minor --type feat --modules core"
```

Single-module example:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="add --directory /path/to/repo --summary 'add release notes command' --release minor --type feat --modules your-artifact-id"
```

This writes a markdown file into `.changesets/`.

## 4. Inspect the plan

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="plan --directory /path/to/repo"
```

## 5. Apply the plan

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="plan --directory /path/to/repo --apply true"
```

That updates:

- the root `revision`
- `CHANGELOG.md`
- `.changesets/release-plan.json`
- `.changesets/release-plan.md`
