# 快速开始

[English](/getting-started) | [简体中文](/zh-CN/getting-started)

## 1. 构建 CLI

```bash
mvn -q test
```

## 2. 准备目标仓库

你的目标仓库至少需要满足：

- 已初始化 git
- 有根 `pom.xml`
- 有 `<revision>` 属性
- 有 `CHANGELOG.md`，或者让 `javachanges` 在应用 release plan 时自动创建/更新
- 根 `pom.xml` 中要么有 `<modules>`，要么是单模块根 artifact

## 3. 创建 changeset

Monorepo 示例：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="add --directory /path/to/repo --summary 'add release notes command' --release minor --modules core"
```

单模块示例：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="add --directory /path/to/repo --summary 'add release notes command' --release minor"
```

这个命令会往 `.changesets/` 写入一个 Markdown 文件。

最短手写格式：

```md
---
"your-artifact-id": patch
---

Fix release-notes rendering.
```

Monorepo 示例：

```md
---
"core": minor
"cli": patch
---

Improve CLI parsing and release planning.
```

说明：

- `javachanges add` 默认会生成这种官方 Changesets 风格的 package map
- 正文第一条非空行会作为 `status`、changelog 和 release notes 使用的 summary
- 旧的 `release` / `modules` / `summary` frontmatter 仍然可兼容读取，但新文件建议统一写 package map
- changelog 会按聚合后的 release level 分成 `major`、`minor`、`patch`

## 4. 查看计划

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="plan --directory /path/to/repo"
```

## 5. 应用计划

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="plan --directory /path/to/repo --apply true"
```

应用后会更新：

- 根 `revision`
- `CHANGELOG.md`
- `.changesets/release-plan.json`
- `.changesets/release-plan.md`
