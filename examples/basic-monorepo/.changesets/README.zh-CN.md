# Changesets

[English](./README.md) | [简体中文](./README.zh-CN.md)

这个目录里的每个 Markdown 文件都表示这个示例 monorepo 的一条待发布变更。

## 格式

```md
---
"javachanges-basic-monorepo-core": minor
"javachanges-basic-monorepo-api": minor
---

Add release notes generation workflow.
```

说明：

- frontmatter 里的每个 key 都是一个 Maven artifactId
- 每个 value 都是一次发布升级级别：`patch`、`minor`、`major`
- 正文里第一条非空文本会被当成 `status`、`plan` 和 changelog 里的 summary
