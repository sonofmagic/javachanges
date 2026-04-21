# javachanges Command Cookbook


## 1. Overview

This page is the practical companion to the CLI reference.

Use it when you do not want to piece together commands from multiple pages and instead want one copy-ready sequence for a common workflow.

The recipes below assume:

- `javachanges` is run from source with Maven
- your target repository has a root `pom.xml`
- the target repository uses a root `<revision>`

## 2. Shared setup

Set a reusable repository path first:

```bash
export REPO=/path/to/your/repo
```

Then reuse `"$REPO"` in the recipes below.

If you are testing against the checked-in sample repository:

```bash
export REPO=examples/basic-monorepo
```

## 3. Recipe: single-module repository

Use this when the repository publishes one Maven artifact from the root project.

### 3.1 Add a patch changeset

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="add --directory $REPO --summary 'fix release note rendering' --release patch"
```

### 3.2 Inspect the pending release

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="status --directory $REPO"
mvn -q -DskipTests compile exec:java -Dexec.args="plan --directory $REPO"
```

### 3.3 Apply the release plan

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="plan --directory $REPO --apply true"
```

### 3.4 Read the generated release version

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="manifest-field --directory $REPO --field releaseVersion"
```

Expected outputs after `plan --apply true`:

- `pom.xml` revision advances to the next snapshot
- `CHANGELOG.md` gets a new release section
- `.changesets/release-plan.json` is written
- `.changesets/release-plan.md` is written

## 4. Recipe: Maven monorepo

Use this when one repository contains multiple Maven modules and you want one reviewed release plan across them.

### 4.1 Add one changeset affecting multiple packages

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="add --directory $REPO --summary 'add release notes workflow' --release minor --modules core,api"
```

### 4.2 Check the affected packages

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="status --directory $REPO"
```

Look for:

- `Affected packages`
- `Release type`
- the pending changeset filename and summary

### 4.3 Apply the plan and inspect the manifest

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="plan --directory $REPO --apply true"
mvn -q -DskipTests compile exec:java -Dexec.args="manifest-field --directory $REPO --field releaseLevel"
```

### 4.4 Restrict later publish work to one module

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="module-selector-args --directory $REPO --module core"
```

That is useful when a downstream workflow needs Maven `-pl` arguments for one package.

## 5. Recipe: prepare CI variables from an env file

Use this when you want to standardize repository publishing variables before enabling GitHub Actions or GitLab CI/CD.

### 5.1 Initialize a local env file

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="init-env --target env/release.env.local"
```

### 5.2 Preview required GitHub variables and secrets

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="render-vars --directory $REPO --env-file env/release.env.local --platform github"
```

### 5.3 Preview required GitLab variables

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="render-vars --directory $REPO --env-file env/release.env.local --platform gitlab"
```

### 5.4 Validate local release inputs

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="doctor-local --directory $REPO --env-file env/release.env.local"
```

Use `doctor-platform` after syncing real repository variables.

### 5.5 Emit machine-readable diagnostics for CI glue

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="render-vars --directory $REPO --env-file env/release.env.local --platform github --format json"
mvn -q -DskipTests compile exec:java -Dexec.args="doctor-local --directory $REPO --env-file env/release.env.local --format json"
mvn -q -DskipTests compile exec:java -Dexec.args="audit-vars --directory $REPO --env-file env/release.env.local --platform github --github-repo owner/repo --format json"
```

Use these when a shell step needs structured diagnostics instead of parsing aligned terminal columns.

## 6. Recipe: GitHub Actions release PR flow

Use this when `main` accumulates `.changesets/*.md` files and a workflow should open one reviewed release PR.

### 6.1 Local dry-run before CI

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="status --directory $REPO"
mvn -q -DskipTests compile exec:java -Dexec.args="plan --directory $REPO --apply true"
```

### 6.2 Fields commonly consumed in GitHub Actions

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="manifest-field --directory $REPO --field releaseVersion"
mvn -q -DskipTests compile exec:java -Dexec.args="manifest-field --directory $REPO --field releaseLevel"
```

### 6.3 What the workflow usually commits

Commit only these generated files:

- `pom.xml`
- `CHANGELOG.md`
- `.changesets/release-plan.json`
- `.changesets/release-plan.md`

For a full workflow file, see [GitHub Actions Usage Guide](./github-actions-guide.md) and [Examples Guide](./examples-guide.md).

## 7. Recipe: GitLab release MR and tag flow

Use this when GitLab should manage the release branch, merge request, and final tag from changesets.

### 7.1 Create or update the release MR

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="gitlab-release-plan --directory $REPO --project-id 12345 --execute true"
```

### 7.2 Create the final tag from the applied plan

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="gitlab-tag-from-plan --directory $REPO --before-sha <before-sha> --current-sha <current-sha> --execute true"
```

Typical CI variable mapping:

| Input | Common source |
| --- | --- |
| `--project-id` | `CI_PROJECT_ID` |
| `--before-sha` | `CI_COMMIT_BEFORE_SHA` |
| `--current-sha` | `CI_COMMIT_SHA` |

For the full pipeline layout, see [GitLab CI/CD Usage Guide](./gitlab-ci-guide.md).

## 8. Recipe: safe local publish dry-run

Use this before enabling real publish execution.

### 8.1 Validate snapshot publishing

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="preflight --directory $REPO --snapshot"
```

### 8.2 Validate a tagged release publish

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="preflight --directory $REPO --tag v1.2.3"
```

### 8.3 Render the actual publish command without executing it

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="publish --directory $REPO --tag v1.2.3"
```

Only add `--execute true` after the printed command, generated settings, and credentials all look correct.

## 9. Recipe: this repository publishing to Maven Central

This recipe is specific to the `javachanges` repository itself, not every downstream repository.

### 9.1 Verify Central publishing prerequisites

```bash
mvn -Pcentral-publish -Dgpg.skip=true verify
```

### 9.2 Run the real deployment

```bash
mvn -Pcentral-publish clean deploy
```

### 9.3 Publish a repository-local snapshot through Central

```bash
pnpm snapshot:publish:local
```

If you only need the generic repository publish helper instead of Sonatype Central publishing, use the `preflight` and `publish` recipes above.

## 10. Quick decision map

| Goal | Start here |
| --- | --- |
| One root artifact, local release plan | Section 3 |
| Monorepo package release planning | Section 4 |
| CI variable preparation | Section 5 |
| GitHub release PR automation | Section 6 |
| GitLab release MR and tag automation | Section 7 |
| Safe publish rehearsal | Section 8 |
| This repository's Central release | Section 9 |

## 11. Related guides

| Need | Document |
| --- | --- |
| Full command catalog | [CLI Reference](./cli-reference.md) |
| Example repository structure | [Examples Guide](./examples-guide.md) |
| Variable catalog | [Configuration Reference](./configuration-reference.md) |
| Applied manifest fields | [Release Plan Manifest](./release-plan-manifest.md) |
| Failure diagnosis | [Troubleshooting Guide](./troubleshooting-guide.md) |
