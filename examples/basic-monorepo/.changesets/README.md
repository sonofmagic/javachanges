# Changesets

[English](./README.md) | [简体中文](./README.zh-CN.md)

Each Markdown file in this directory describes one pending release for the example monorepo.

## Format

```md
---
"core": minor
"api": minor
---

Add release notes generation workflow.
```

Notes:

- each frontmatter key is a Maven artifactId
- each value is a release bump: `patch`, `minor`, or `major`
- the first non-empty body line becomes the summary shown by `status`, `plan`, and changelog generation
