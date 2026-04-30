#!/bin/sh

set -eu

repo_root=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
cd "$repo_root"

task="${1:-all}"
local_maven_repo=".m2/repository"

usage() {
  cat <<'EOF'
Usage: scripts/ci-local.sh <task>

Tasks:
  build            Run the Java build checks from .github/workflows/ci.yml
  docs             Install docs dependencies and build the VitePress site
  release-dry-run  Run local release automation dry-runs without platform writes
  all              Run build, docs, and release-dry-run

The release-dry-run task intentionally avoids --execute true. It does not push
branches, open pull requests, create tags, create releases, or deploy artifacts.
EOF
}

run() {
  printf '\n==> %s\n' "$*"
  "$@"
}

ensure_docs_tooling() {
  if ! command -v pnpm >/dev/null 2>&1; then
    echo "pnpm is required for docs checks. Install pnpm and retry." >&2
    exit 1
  fi
}

seed_mock_publish_env() {
  : "${MAVEN_SNAPSHOT_REPOSITORY_URL:=https://example.invalid/maven-snapshots}"
  : "${MAVEN_SNAPSHOT_REPOSITORY_USERNAME:=local-ci-user}"
  : "${MAVEN_SNAPSHOT_REPOSITORY_PASSWORD:=local-ci-password}"
  : "${MAVEN_SNAPSHOT_REPOSITORY_ID:=local-snapshots}"
  export MAVEN_SNAPSHOT_REPOSITORY_URL
  export MAVEN_SNAPSHOT_REPOSITORY_USERNAME
  export MAVEN_SNAPSHOT_REPOSITORY_PASSWORD
  export MAVEN_SNAPSHOT_REPOSITORY_ID
}

run_build() {
  run ./mvnw -B "-Dmaven.repo.local=$local_maven_repo" verify
  run ./mvnw -B "-Dmaven.repo.local=$local_maven_repo" -Pcentral-publish -Dgpg.skip=true verify
  run ./mvnw -B "-Dmaven.repo.local=$local_maven_repo" -DskipTests compile exec:java "-Dexec.args=status --directory $repo_root"
}

run_docs() {
  ensure_docs_tooling
  run pnpm install --frozen-lockfile
  run pnpm docs:build
}

run_release_dry_run() {
  seed_mock_publish_env
  run ./mvnw -B "-Dmaven.repo.local=$local_maven_repo" -DskipTests compile
  run ./mvnw -B "-Dmaven.repo.local=$local_maven_repo" -DskipTests exec:java "-Dexec.args=github-release-plan --directory $repo_root --github-repo local/mock --write-plan-files false --format json"
  run ./mvnw -B "-Dmaven.repo.local=$local_maven_repo" -DskipTests exec:java "-Dexec.args=gitlab-release-plan --directory $repo_root --project-id 0 --write-plan-files false --format json"
  run ./mvnw -B "-Dmaven.repo.local=$local_maven_repo" -DskipTests exec:java "-Dexec.args=preflight --directory $repo_root --snapshot --snapshot-build-stamp local.ci.001 --allow-dirty true --format json"
  run ./mvnw -B "-Dmaven.repo.local=$local_maven_repo" -DskipTests exec:java "-Dexec.args=publish --directory $repo_root --snapshot --snapshot-build-stamp local.ci.001 --allow-dirty true --format json"
}

case "$task" in
  build)
    run_build
    ;;
  docs)
    run_docs
    ;;
  release-dry-run)
    run_release_dry_run
    ;;
  all)
    run_build
    run_docs
    run_release_dry_run
    ;;
  -h|--help|help)
    usage
    ;;
  *)
    echo "Unknown local CI task: $task" >&2
    echo >&2
    usage >&2
    exit 1
    ;;
esac
