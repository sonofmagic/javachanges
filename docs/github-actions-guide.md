# javachanges GitHub Actions Usage Guide


## 1. Overview

This guide explains how to use `javachanges` in GitHub Actions for:

1. regular CI validation
2. release-plan generation
3. GitHub Actions variable and secret management
4. publish preflight and real publishing
5. Maven dependency caching

The examples in this guide use direct CLI commands such as:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="status --directory $PWD"
```

> Note: this repository currently documents direct `javachanges` CLI usage. It does not ship a Makefile wrapper in the repository root.

## 2. What `javachanges` Can Do In GitHub Actions

Recommended command mapping:

| Goal | Command |
| --- | --- |
| Check pending release state | `status` |
| Apply a release plan | `plan --apply true` |
| Read `releaseVersion` from the manifest | `manifest-field --field releaseVersion` |
| Generate Maven settings from env vars | `write-settings --output .m2/settings.xml` |
| Render required GitHub variables and secrets | `render-vars --env-file env/release.env.local --platform github` |
| Check local/platform readiness | `doctor-local`, `doctor-platform` |
| Sync GitHub Actions variables and secrets | `sync-vars --platform github` |
| Audit remote GitHub Actions variables and secrets | `audit-vars --platform github` |
| Validate a release publish locally or in CI | `preflight` |
| Run the actual Maven deploy command | `publish --execute true` |
| Generate release notes | `release-notes --tag vX.Y.Z --output target/release-notes.md` |

## 3. Recommended Repository Layout

For a GitHub-based release workflow, keep these files under version control:

| Path | Purpose |
| --- | --- |
| `.changesets/*.md` | Pending release intent |
| `.changesets/release-plan.json` | Generated release manifest |
| `.changesets/release-plan.md` | Generated release PR body |
| `CHANGELOG.md` | Generated changelog |
| `env/release.env.example` | Variable template for repository publishing |
| `.github/workflows/*.yml` | CI and release workflows |

## 4. Local Preparation Before Touching GitHub Actions

### 4.1 Build the CLI

```bash
mvn -q test
```

### 4.2 Initialize a local env file

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="init-env --target env/release.env.local"
```

Template fields in `env/release.env.example`:

| Variable | Meaning |
| --- | --- |
| `MAVEN_RELEASE_REPOSITORY_URL` | Release repository URL |
| `MAVEN_SNAPSHOT_REPOSITORY_URL` | Snapshot repository URL |
| `MAVEN_RELEASE_REPOSITORY_ID` | Release server id used in Maven settings |
| `MAVEN_SNAPSHOT_REPOSITORY_ID` | Snapshot server id used in Maven settings |
| `MAVEN_REPOSITORY_USERNAME` | Shared username fallback |
| `MAVEN_REPOSITORY_PASSWORD` | Shared password fallback |
| `MAVEN_RELEASE_REPOSITORY_USERNAME` | Optional explicit release username |
| `MAVEN_RELEASE_REPOSITORY_PASSWORD` | Optional explicit release password |
| `MAVEN_SNAPSHOT_REPOSITORY_USERNAME` | Optional explicit snapshot username |
| `MAVEN_SNAPSHOT_REPOSITORY_PASSWORD` | Optional explicit snapshot password |
| `JAVACHANGES_SNAPSHOT_BUILD_STAMP` | Optional explicit snapshot publish stamp for CI |

### 4.3 Check your local environment

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="doctor-local --env-file env/release.env.local --github-repo owner/repo"
```

### 4.4 Preview GitHub variables and secrets

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="render-vars --env-file env/release.env.local --platform github"
```

### 4.5 Sync GitHub variables and secrets with `gh`

Dry-run:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="sync-vars --env-file env/release.env.local --platform github --repo owner/repo"
```

Apply:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="sync-vars --env-file env/release.env.local --platform github --repo owner/repo --execute true"
```

Audit:

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="audit-vars --env-file env/release.env.local --platform github --github-repo owner/repo"
```

`sync-vars` writes:

| Remote type | Names |
| --- | --- |
| GitHub Actions variables | `MAVEN_RELEASE_REPOSITORY_URL`, `MAVEN_SNAPSHOT_REPOSITORY_URL`, `MAVEN_RELEASE_REPOSITORY_ID`, `MAVEN_SNAPSHOT_REPOSITORY_ID` |
| GitHub Actions secrets | `MAVEN_REPOSITORY_USERNAME`, `MAVEN_REPOSITORY_PASSWORD`, `MAVEN_RELEASE_REPOSITORY_USERNAME`, `MAVEN_RELEASE_REPOSITORY_PASSWORD`, `MAVEN_SNAPSHOT_REPOSITORY_USERNAME`, `MAVEN_SNAPSHOT_REPOSITORY_PASSWORD` |

## 5. GitHub Actions Workflow Patterns

### 5.1 CI-only validation

Use this when you want pull requests to validate build health and pending release state.

```yaml
name: CI

on:
  push:
    branches: [main]
  pull_request:

permissions:
  contents: read

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v5
        with:
          fetch-depth: 0

      - uses: actions/setup-java@v5
        with:
          distribution: corretto
          java-version: '8'
          cache: maven
          cache-dependency-path: pom.xml

      - name: Verify build
        run: mvn -B verify

      - name: Inspect release state
        run: mvn -B -DskipTests compile exec:java -Dexec.args="status --directory $GITHUB_WORKSPACE"
```

### 5.2 Release-plan pull request automation

Use this pattern when `main` should accumulate `.changesets/*.md` files and produce a reviewed release PR.

```yaml
name: Release Plan

on:
  push:
    branches: [main]
  workflow_dispatch:

permissions:
  contents: write
  pull-requests: write

jobs:
  release-pr:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v5
        with:
          fetch-depth: 0

      - uses: actions/setup-java@v5
        with:
          distribution: corretto
          java-version: '8'
          cache: maven
          cache-dependency-path: pom.xml

      - name: Build CLI
        run: mvn -B -DskipTests compile

      - name: Apply release plan
        run: mvn -B -DskipTests compile exec:java -Dexec.args="plan --directory $GITHUB_WORKSPACE --apply true"

      - name: Read release version
        if: hashFiles('.changesets/release-plan.json') != ''
        id: release_version
        run: |
          value="$(mvn -q -DskipTests compile exec:java -Dexec.args="manifest-field --directory $GITHUB_WORKSPACE --field releaseVersion" | tail -n 1)"
          echo "value=$value" >> "$GITHUB_OUTPUT"

      - name: Commit release plan
        if: hashFiles('.changesets/release-plan.json') != ''
        env:
          GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          RELEASE_VERSION: ${{ steps.release_version.outputs.value }}
        run: |
          git config user.name "github-actions[bot]"
          git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
          git switch -C changeset-release/main
          git add pom.xml CHANGELOG.md .changesets
          git commit -m "chore(release): apply changesets for v${RELEASE_VERSION}"
          git push --force-with-lease origin HEAD:changeset-release/main
          gh pr create \
            --base main \
            --head changeset-release/main \
            --title "chore(release): v${RELEASE_VERSION}" \
            --body-file .changesets/release-plan.md
```

### 5.3 Snapshot publish with `javachanges publish`

Use this when pushes to a dedicated `snapshot` branch should publish unique snapshots into your own snapshot repository.

```yaml
name: Publish Snapshot

on:
  push:
    branches:
      - snapshot
  workflow_dispatch:
    inputs:
      snapshot_build_stamp:
        description: Optional explicit snapshot build stamp
        required: false
        type: string

permissions:
  contents: read

jobs:
  publish-snapshot:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v5
        with:
          fetch-depth: 0

      - uses: actions/setup-java@v5
        with:
          distribution: corretto
          java-version: '8'
          cache: maven
          cache-dependency-path: pom.xml

      - name: Build CLI
        run: mvn -B -DskipTests compile

      - name: Resolve snapshot build stamp
        id: snapshot_build_stamp
        env:
          INPUT_SNAPSHOT_BUILD_STAMP: ${{ inputs.snapshot_build_stamp }}
        run: |
          if [ -n "$INPUT_SNAPSHOT_BUILD_STAMP" ]; then
            value="$INPUT_SNAPSHOT_BUILD_STAMP"
          else
            value="${GITHUB_RUN_ID}.${GITHUB_RUN_ATTEMPT}.$(git rev-parse --short HEAD)"
          fi
          echo "value=$value" >> "$GITHUB_OUTPUT"

      - name: Preflight snapshot publish
        env:
          JAVACHANGES_SNAPSHOT_BUILD_STAMP: ${{ steps.snapshot_build_stamp.outputs.value }}
          MAVEN_SNAPSHOT_REPOSITORY_URL: ${{ vars.MAVEN_SNAPSHOT_REPOSITORY_URL }}
          MAVEN_SNAPSHOT_REPOSITORY_ID: ${{ vars.MAVEN_SNAPSHOT_REPOSITORY_ID }}
          MAVEN_REPOSITORY_USERNAME: ${{ secrets.MAVEN_REPOSITORY_USERNAME }}
          MAVEN_REPOSITORY_PASSWORD: ${{ secrets.MAVEN_REPOSITORY_PASSWORD }}
          MAVEN_SNAPSHOT_REPOSITORY_USERNAME: ${{ secrets.MAVEN_SNAPSHOT_REPOSITORY_USERNAME }}
          MAVEN_SNAPSHOT_REPOSITORY_PASSWORD: ${{ secrets.MAVEN_SNAPSHOT_REPOSITORY_PASSWORD }}
        run: |
          mvn -B -DskipTests compile exec:java \
            -Dexec.args="preflight --directory $GITHUB_WORKSPACE --snapshot"

      - name: Publish snapshot
        env:
          JAVACHANGES_SNAPSHOT_BUILD_STAMP: ${{ steps.snapshot_build_stamp.outputs.value }}
          MAVEN_SNAPSHOT_REPOSITORY_URL: ${{ vars.MAVEN_SNAPSHOT_REPOSITORY_URL }}
          MAVEN_SNAPSHOT_REPOSITORY_ID: ${{ vars.MAVEN_SNAPSHOT_REPOSITORY_ID }}
          MAVEN_REPOSITORY_USERNAME: ${{ secrets.MAVEN_REPOSITORY_USERNAME }}
          MAVEN_REPOSITORY_PASSWORD: ${{ secrets.MAVEN_REPOSITORY_PASSWORD }}
          MAVEN_SNAPSHOT_REPOSITORY_USERNAME: ${{ secrets.MAVEN_SNAPSHOT_REPOSITORY_USERNAME }}
          MAVEN_SNAPSHOT_REPOSITORY_PASSWORD: ${{ secrets.MAVEN_SNAPSHOT_REPOSITORY_PASSWORD }}
        run: |
          mvn -B -DskipTests compile exec:java \
            -Dexec.args="publish --directory $GITHUB_WORKSPACE --snapshot --execute true"
```

This publishes a unique revision such as `1.2.3-123456789.1.abc1234-SNAPSHOT`, instead of repeatedly deploying the raw `1.2.3-SNAPSHOT`. A common repository policy is to reserve `main` for release planning and merge development that should produce published snapshots into a separate `snapshot` branch.

### 5.4 Generic release publish with `javachanges publish`

Use this when the target repository publishes tagged releases into your own release repository and you want `javachanges` to:

1. validate the tag and version state
2. generate `.m2/settings.xml`
3. render the exact Maven `deploy` command
4. optionally execute the real deploy

```yaml
name: Publish Release

on:
  push:
    tags:
      - 'v*'

permissions:
  contents: read

jobs:
  publish-release:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v5
        with:
          fetch-depth: 0

      - uses: actions/setup-java@v5
        with:
          distribution: corretto
          java-version: '8'
          cache: maven
          cache-dependency-path: pom.xml

      - name: Build CLI
        run: mvn -B -DskipTests compile

      - name: Preflight
        env:
          MAVEN_RELEASE_REPOSITORY_URL: ${{ vars.MAVEN_RELEASE_REPOSITORY_URL }}
          MAVEN_REPOSITORY_USERNAME: ${{ secrets.MAVEN_REPOSITORY_USERNAME }}
          MAVEN_REPOSITORY_PASSWORD: ${{ secrets.MAVEN_REPOSITORY_PASSWORD }}
          MAVEN_RELEASE_REPOSITORY_USERNAME: ${{ secrets.MAVEN_RELEASE_REPOSITORY_USERNAME }}
          MAVEN_RELEASE_REPOSITORY_PASSWORD: ${{ secrets.MAVEN_RELEASE_REPOSITORY_PASSWORD }}
        run: |
          mvn -B -DskipTests compile exec:java \
            -Dexec.args="preflight --directory $GITHUB_WORKSPACE --tag ${GITHUB_REF_NAME}"

      - name: Publish
        env:
          MAVEN_RELEASE_REPOSITORY_URL: ${{ vars.MAVEN_RELEASE_REPOSITORY_URL }}
          MAVEN_REPOSITORY_USERNAME: ${{ secrets.MAVEN_REPOSITORY_USERNAME }}
          MAVEN_REPOSITORY_PASSWORD: ${{ secrets.MAVEN_REPOSITORY_PASSWORD }}
          MAVEN_RELEASE_REPOSITORY_USERNAME: ${{ secrets.MAVEN_RELEASE_REPOSITORY_USERNAME }}
          MAVEN_RELEASE_REPOSITORY_PASSWORD: ${{ secrets.MAVEN_RELEASE_REPOSITORY_PASSWORD }}
        run: |
          mvn -B -DskipTests compile exec:java \
            -Dexec.args="publish --directory $GITHUB_WORKSPACE --tag ${GITHUB_REF_NAME} --execute true"
```

> Note: these generic `publish` flows use the repository URLs and credentials from `env/release.env.example`.  
> If you are publishing to Maven Central with a `central-publish` Maven profile, see [Publish To Maven Central](./publish-to-maven-central.md) and [GitHub Actions Release Flow](./github-actions-release.md).

## 6. Maven Cache Behavior In GitHub Actions

The recommended configuration is:

```yaml
- uses: actions/setup-java@v5
  with:
    distribution: corretto
    java-version: '8'
    cache: maven
    cache-dependency-path: pom.xml
```

What this helps with:

| Cached well | Not solved by Maven cache |
| --- | --- |
| Maven dependencies in `~/.m2/repository` | `git checkout` and `git fetch` |
| Maven plugins and plugin dependencies | JDK download and setup |
| Repeat builds with the same `pom.xml` hash | GPG key import |
| Cross-workflow reuse of the same dependency graph | Sonatype or custom repository publish latency |

Important behavior:

| Situation | Result |
| --- | --- |
| First run of a new cache key | Downloads still happen |
| `pom.xml` changes | A new cache key may need fresh downloads |
| GitHub-hosted runner starts clean | Cache still restores from GitHub storage, not from previous local disk state |
| You cache `target/` instead of Maven repo | Usually a bad trade-off for Java library CI |

## 7. Recommended GitHub Actions Checks

Use this order:

1. `mvn -B verify`
2. `javachanges status`
3. `javachanges plan --apply true` only in the release-plan workflow
4. `javachanges preflight --snapshot` before any snapshot deploy
5. `javachanges publish --snapshot --execute true` only in a snapshot workflow
6. `javachanges preflight --tag ...` before any release deploy
7. `javachanges publish --execute true` only in a release workflow

## 8. Common Mistakes

| Problem | Cause | Fix |
| --- | --- | --- |
| Release PR keeps changing unexpectedly | release workflow edits files outside `.changesets`, `pom.xml`, or `CHANGELOG.md` | limit the release-plan commit scope |
| Publish job says repository credentials are missing | required vars or secrets were never synced | run `render-vars`, `sync-vars`, then `audit-vars` |
| Snapshot workflow keeps producing the same visible version | the build stamp was fixed or never updated | set `JAVACHANGES_SNAPSHOT_BUILD_STAMP` or derive one from run id and commit sha |
| Maven downloads still appear after enabling cache | new cache key or first run | let one successful run warm the cache |
| `publish` fails on a dirty worktree | `preflight` and `publish` reject local changes by default | commit changes or pass `--allow-dirty true` only when intended |
| GitHub workflow examples mention wrappers you do not have | copied examples assume local wrapper scripts or Make targets | use direct CLI commands from this guide |

## 9. Recommended Documentation Split

Use these docs together:

| Need | Document |
| --- | --- |
| Full GitHub Actions release PR flow used by this repository | [GitHub Actions Release Flow](./github-actions-release.md) |
| Maven Central publishing requirements | [Publish To Maven Central](./publish-to-maven-central.md) |
| Cross-platform release commands | [Development Guide](./development-guide.md) |

## 10. Summary

The practical GitHub Actions path is:

1. validate with `status` in CI
2. generate a reviewed release PR with `plan --apply true`
3. manage GitHub variables and secrets with `render-vars`, `sync-vars`, and `audit-vars`
4. publish snapshots from a dedicated `snapshot` branch with `preflight --snapshot` and `publish --snapshot`
5. publish tagged releases with `preflight --tag ...` and `publish --tag ...`
6. use a Maven Central-specific workflow only when the repository really publishes releases to Central

## 11. References

- GitHub Actions workflow syntax: https://docs.github.com/en/actions/writing-workflows/workflow-syntax-for-github-actions
- GitHub dependency caching: https://docs.github.com/en/actions/concepts/workflows-and-actions/dependency-caching
- `actions/setup-java` caching options: https://github.com/actions/setup-java
