#!/bin/sh

set -eu

repo_root=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
cd "$repo_root"

local_repo=".m2/repository"

current_revision="$(
  ./mvnw -q -DskipTests -Dmaven.repo.local="$local_repo" compile exec:java \
    -Dexec.args="version --directory $repo_root" | tail -n 1
)"

case "$current_revision" in
  *-SNAPSHOT) ;;
  *)
    echo "Current revision is not a SNAPSHOT: $current_revision" >&2
    exit 1
    ;;
esac

base_revision="${current_revision%-SNAPSHOT}"
snapshot_build_stamp="${JAVACHANGES_SNAPSHOT_BUILD_STAMP:-$(date -u +%Y%m%d.%H%M%S).$(git rev-parse --short HEAD)}"
resolved_revision="${base_revision}-${snapshot_build_stamp}-SNAPSHOT"

echo "Publishing snapshot revision: $resolved_revision"

./mvnw -B \
  -Dmaven.repo.local="$local_repo" \
  -Pcentral-snapshot-publish \
  -Drevision="$resolved_revision" \
  clean deploy
