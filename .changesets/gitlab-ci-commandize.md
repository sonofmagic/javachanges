---
"javachanges": minor
---

Commandize the GitLab CI/CD release flow so business repositories can keep `.gitlab-ci.yml` minimal.

- add `gitlab-release` to create or update GitLab Releases from CI tags
- add `init-gitlab-ci` to generate a minimal GitLab CI template
- let GitLab release-plan, tag, and publish commands read `baseBranch`, `releaseBranch`, and `snapshotBranch` from `.changesets/config.json` or `.changesets/config.jsonc`
- add machine-readable `--format json` support for GitLab release-plan, tag, release, and publish commands
- teach `doctor-platform --platform gitlab` to check protected variables and the configured snapshot branch protection
