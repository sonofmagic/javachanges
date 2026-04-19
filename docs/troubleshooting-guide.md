# javachanges Troubleshooting Guide


## 1. Overview

This guide collects the most common problems people hit when adopting `javachanges` in local development, GitHub Actions, GitLab CI/CD, and Maven publishing.

Use it when:

- `status` or `plan` prints unexpected release information
- CI says credentials or variables are missing
- Maven publishing fails even though the workflow looks correct
- the example workflows were copied but do not run in the target repository

## 2. Local repository and changeset problems

### 2.1 `No pending changesets`

| Symptom | Cause | Fix |
| --- | --- | --- |
| `status` shows `Pending changesets: 0` | `.changesets/*.md` does not exist, was already consumed, or is malformed | create a new changeset with `add`, then rerun `status` |

Checks:

```bash
find .changesets -maxdepth 1 -name '*.md'
mvn -q -DskipTests compile exec:java -Dexec.args="status --directory /path/to/repo"
```

### 2.2 `add` succeeds but `plan` does not affect the module you expected

| Symptom | Cause | Fix |
| --- | --- | --- |
| The release plan lists the wrong package or misses a package | the changeset uses the wrong Maven artifactId | align the frontmatter keys with the real artifactIds from `pom.xml` |

Good example:

```md
---
"core": minor
"api": patch
---

Improve release planning output.
```

### 2.3 `plan` does not write `release-plan.json`

| Symptom | Cause | Fix |
| --- | --- | --- |
| `.changesets/release-plan.json` is missing after `plan` | you ran preview mode only | rerun with `--apply true` |

Correct command:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="plan --directory /path/to/repo --apply true"
```

### 2.4 `manifest-field` fails

| Symptom | Cause | Fix |
| --- | --- | --- |
| `manifest-field` cannot read a field | no applied release plan exists yet | apply the plan first, then read the field |

## 3. Java and Maven environment problems

### 3.1 Build fails or behaves differently across machines

| Symptom | Cause | Fix |
| --- | --- | --- |
| local results differ from CI | Java version drift | standardize on Java 8 for this repository |

Verify:

```bash
java -version
mvn -v
```

Expected direction:

- Java 8
- Maven 3.8+

### 3.2 `exec:java` fails or the CLI does not start

| Symptom | Cause | Fix |
| --- | --- | --- |
| Maven cannot run `exec:java` | dependencies were not built yet, or the build is broken | run `mvn -q test` first |

### 3.3 `doctor-local` says `./mvnw` is missing

| Symptom | Cause | Fix |
| --- | --- | --- |
| the runtime section shows `./mvnw MISSING` | the target repository does not vendor a Maven wrapper | this is fine if a system `mvn` is available and `Maven command` resolves to `mvn (system)` |

What to check:

- if `Maven command` is `mvn (system)`, the fallback worked
- if both wrapper and system Maven are unavailable, install Maven or add a wrapper

### 3.4 Example workflow cannot download `javachanges`

| Symptom | Cause | Fix |
| --- | --- | --- |
| Maven dependency copy fails for `io.github.sonofmagic:javachanges` | `JAVACHANGES_VERSION` is still the placeholder value | replace it with a real published version before enabling CI |

The example templates intentionally use:

```yaml
JAVACHANGES_VERSION: "REPLACE_WITH_PUBLISHED_VERSION"
```

That is a reminder, not a runnable default.

## 4. GitHub Actions problems

### 4.1 `sync-vars` or `audit-vars` says values are missing

| Symptom | Cause | Fix |
| --- | --- | --- |
| repository variables or secrets are missing | `env/release.env.local` still contains placeholders, or the repository name is wrong | replace placeholder values, then rerun `render-vars`, `sync-vars`, and `audit-vars` |

Suggested order:

1. `init-env`
2. fill the env file
3. `render-vars --platform github`
4. `sync-vars --platform github --execute true`
5. `audit-vars --platform github`

### 4.2 Release PR workflow keeps changing unrelated files

| Symptom | Cause | Fix |
| --- | --- | --- |
| the release branch includes extra noise | the workflow stages too much or runs additional generators | limit the commit scope to `pom.xml`, `CHANGELOG.md`, and `.changesets` |

### 4.3 Maven downloads still happen even with cache enabled

| Symptom | Cause | Fix |
| --- | --- | --- |
| the workflow still downloads dependencies | first cache warmup or a changed dependency graph | let one successful run warm the cache; this is normal |

## 5. GitLab CI/CD problems

### 5.1 Release MR is never created

| Symptom | Cause | Fix |
| --- | --- | --- |
| `gitlab-release-plan` skips | no pending changesets, or missing GitLab auth variables | add a new changeset and verify `GITLAB_RELEASE_BOT_USERNAME` / `GITLAB_RELEASE_BOT_TOKEN` |

### 5.2 Release tag job does nothing

| Symptom | Cause | Fix |
| --- | --- | --- |
| `gitlab-tag-from-plan` skips tagging | `.changesets/release-plan.json` did not change, or `CI_COMMIT_BEFORE_SHA` is unusable | inspect the default branch pipeline and confirm a new applied plan was committed |

## 6. Publish and credentials problems

### 6.1 `preflight` or `publish` says credentials are missing

| Symptom | Cause | Fix |
| --- | --- | --- |
| publish commands fail before deploy | required repository URLs, ids, or credentials are not set | populate the env file or CI secrets, then rerun `doctor-local` or `doctor-platform` |

Minimum shared variables:

| Name |
| --- |
| `MAVEN_RELEASE_REPOSITORY_URL` |
| `MAVEN_SNAPSHOT_REPOSITORY_URL` |
| `MAVEN_RELEASE_REPOSITORY_ID` |
| `MAVEN_SNAPSHOT_REPOSITORY_ID` |
| `MAVEN_REPOSITORY_USERNAME` |
| `MAVEN_REPOSITORY_PASSWORD` |

### 6.2 Publish fails on a dirty worktree

| Symptom | Cause | Fix |
| --- | --- | --- |
| `preflight` or `publish` rejects the repository | the repo has local edits or generated files | commit or clean the tree before publishing |

Use `--allow-dirty true` only when you intentionally accept that risk.

### 6.3 Maven Central publish fails with missing jars or signatures

| Symptom | Cause | Fix |
| --- | --- | --- |
| `sources.jar`, `javadoc.jar`, or signatures are missing | the `central-publish` profile or GPG setup is incomplete | rerun the Central verification steps from the Maven Central guide |

Verification command:

```bash
mvn -Pcentral-publish -Dgpg.skip=true verify
```

## 7. Release plan manifest confusion

### 7.1 `type: other` appears in `release-plan.json`

| Symptom | Cause | Fix |
| --- | --- | --- |
| the manifest contains `"type": "other"` | the field is a legacy compatibility field retained by the current implementation | ignore it in new integrations and key off `release`, `summary`, and `modules` instead |

If you use the official package-map changeset format, `type` is not the release level. The meaningful release signal is `release`.

## 8. Fast checklist

| Check | Command or file |
| --- | --- |
| Java version | `java -version` |
| Maven version | `mvn -v` |
| Pending changesets | `status` |
| Applied manifest exists | `.changesets/release-plan.json` |
| CI variables look correct | `render-vars` / `audit-vars` |
| Publish inputs are valid | `doctor-local` / `preflight` |
| Example workflow placeholder replaced | `JAVACHANGES_VERSION` |

## 9. Related guides

| Need | Document |
| --- | --- |
| Local setup | [Development Guide](./development-guide.md) |
| Example repository walkthrough | [Examples Guide](./examples-guide.md) |
| Full CLI command list | [CLI Reference](./cli-reference.md) |
| Generated manifest fields | [Release Plan Manifest](./release-plan-manifest.md) |
| GitHub Actions automation | [GitHub Actions Usage Guide](./github-actions-guide.md) |
| GitLab CI/CD automation | [GitLab CI/CD Usage Guide](./gitlab-ci-guide.md) |
