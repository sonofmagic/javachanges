---
"javachanges": patch
---

Fix the docs deployment workflow and remove duplicated locale switch links from documentation pages.

- Install `pnpm` before enabling `setup-node`'s pnpm cache in the docs deploy workflow.
- Remove inline `English | 简体中文` links from docs pages now that the website provides built-in locale switching.
