# Changesets

这个目录保存待发布的 release notes。每个面向用户的变更都应该添加一条 changeset。

创建 changeset:

```bash
javachanges add --directory . --summary "描述这次变更" --release patch
```

多模块仓库请使用检测到的模块名:

```bash
javachanges modules --directory .
javachanges add --directory . --modules core --summary "描述这次变更" --release patch
```

Changeset 使用官方 package-map frontmatter 格式:

```md
---
"core": minor
---

描述面向用户的变更。
```

支持的发布级别为 `patch`、`minor` 和 `major`。

检查并应用发布计划:

```bash
javachanges status --directory .
javachanges plan --directory . --apply true
```
