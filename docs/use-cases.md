# Use Cases

## Maven library monorepo

Use `javachanges` to manage version bumps across multiple artifacts while keeping a single reviewed release plan.

## Internal platform release automation

Use `write-settings`, `render-vars`, `doctor-platform`, and `audit-vars` to standardize CI variables and Maven credentials across repositories.

## GitLab release MR flow

Use `gitlab-release-plan` and `gitlab-tag-from-plan` when you want release branches, merge requests, and tags to be generated from pending changesets.

## Safer publish dry-runs

Use `preflight` and `publish --execute false` to preview the exact Maven deploy command and generated settings before touching a real repository.

