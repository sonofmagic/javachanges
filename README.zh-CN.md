# javachanges

[English](./README.md) | [简体中文](./README.zh-CN.md)

`javachanges` 是一个轻量的 Java CLI，为 Maven Monorepo 和单模块 Maven 仓库提供类似 Changesets 的发布规划工作流。

文档站点：`https://javachanges.icebreaker.top`

它适合这些仓库：

- 需要用 `.changesets/*.md` 文件记录发布意图
- 希望在版本变更落地前先审阅 release plan
- 需要自动生成 changelog 和 release notes
- 需要 CI 友好的发布检查和发布辅助能力
- 需要可选的 GitHub / GitLab 变量同步和审计能力

## 状态

这个仓库是 `javachanges` 的独立源码仓库。

当前代码重点覆盖：

- changeset 的添加与校验
- release plan 的生成
- 根 `revision` 的推进
- changelog 和 release notes 的生成
- 基于环境变量的 Maven settings 生成
- 发布前检查和发布辅助
- GitHub / GitLab 环境变量审计
- GitLab release MR 与 tag 自动化辅助

## 快速开始

环境要求：

- Java 8+
- Maven 3.8+
- 一个 git 仓库
- 一个带根 `pom.xml` 的 Maven 仓库
- 根 `pom.xml` 中要么有 `<modules>`，要么是单模块根 artifact

构建 CLI：

```bash
mvn -q test
```

对目标仓库执行：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="status --directory /path/to/your/repo"
mvn -q -DskipTests compile exec:java -Dexec.args="add --directory /path/to/your/repo"
mvn -q -DskipTests compile exec:java -Dexec.args="plan --directory /path/to/your/repo"
```

## Changeset 格式

`javachanges` 现在默认使用与 Node.js Changesets 相同的核心 Markdown 结构：

```md
---
"javachanges": minor
---

Add GitHub Actions release automation.
```

Monorepo 示例：

```md
---
"core": minor
"cli": patch
---

Improve CLI parsing and release planning.
```

它的含义是：

- frontmatter 里的每个 key 都是一个 Maven artifactId
- 每个 value 都是这个模块对应的语义化版本升级级别：`patch`、`minor`、`major`
- Markdown 正文就是面向用户的变更说明与补充备注

默认行为：

- `javachanges add` 默认会生成这种官方 package-map 风格
- 如果使用 `--modules all`，会为当前仓库检测到的所有 Maven artifactId 写入对应条目
- 正文第一条非空行会被复用为 `status`、release PR、changelog 和 release notes 里的 summary
- changelog 仍然按聚合后的 release level 分成 `major`、`minor`、`patch`

单模块仓库的最短手写格式：

```md
---
"javachanges": patch
---

Fix Windows path handling in release-notes generation.
```

兼容性说明：

- 旧的 `release` / `modules` / `summary` / `type` frontmatter 格式仍然可以继续读取
- 新建 changeset 推荐统一使用上面的官方 package-map 风格

旧格式示例：

```md
---
release: minor
type: ci
modules: javachanges
summary: automate javachanges self-release publishing via GitHub Actions
---
```

字段说明：

- `"artifactId"`
  新格式必填。
  每个 frontmatter key 都是一个 Maven artifactId，通常建议像官方 Changesets 一样加双引号。
  对于单模块仓库，一般就是根 artifactId。
  对于 monorepo，则为每个受影响模块写一条。
- `patch` / `minor` / `major`
  每个 artifactId 对应的必填值。
  表示这个模块贡献的语义化版本升级级别。
  可选值：`patch`、`minor`、`major`。
  一般规则：
  `patch` 用于兼容性修复、文档、杂项、CI、小改动。
  `minor` 用于向后兼容的新功能。
  `major` 用于破坏性变更。
- body
  推荐填写的 Markdown 正文。
  第一条非空正文会被复用为 summary。
  后续段落或列表可以继续写迁移说明、发布备注或补充背景。
- `type`
  旧格式里的可选元数据字段，仅为兼容历史文件而保留。
  新文件一般不再推荐使用。
- `release`、`modules`、`summary`
  旧版 `javachanges` 使用过的 frontmatter 字段。
  现阶段仍可兼容解析，但不再推荐用于新 changeset。

## 仓库结构

- `src/main/java`: CLI 源码
- `docs/`: 文档
- `examples/basic-monorepo/`: 最小 Maven 目标仓库示例
- `website/`: 可发布到 GitHub Pages 的简单静态页面
- `env/release.env.example`: 通用发布环境变量模板

## 命令

高价值命令：

- `add`
- `status`
- `plan`
- `write-settings`
- `init-env`
- `render-vars`
- `doctor-local`
- `doctor-platform`
- `sync-vars`
- `audit-vars`
- `preflight`
- `publish`
- `release-notes`

GitLab 相关辅助：

- `gitlab-release-plan`
- `gitlab-tag-from-plan`

## 文档

- [Overview](docs/index.md)
- [Overview (zh-CN)](docs/index.zh-CN.md)
- [Getting Started](docs/getting-started.md)
- [Getting Started (zh-CN)](docs/getting-started.zh-CN.md)
- [Development Guide](docs/development-guide.md)
- [Development Guide (zh-CN)](docs/development-guide.zh-CN.md)
- [GitHub Actions Release Flow](docs/github-actions-release.md)
- [GitHub Actions Release Flow (zh-CN)](docs/github-actions-release.zh-CN.md)
- [GitHub Actions Usage Guide](docs/github-actions-guide.md)
- [GitHub Actions Usage Guide (zh-CN)](docs/github-actions-guide.zh-CN.md)
- [GitLab CI/CD Usage Guide](docs/gitlab-ci-guide.md)
- [GitLab CI/CD Usage Guide (zh-CN)](docs/gitlab-ci-guide.zh-CN.md)
- [Publish To Maven Central](docs/publish-to-maven-central.md)
- [Publish To Maven Central (zh-CN)](docs/publish-to-maven-central.zh-CN.md)
- [Use Cases](docs/use-cases.md)
- [Use Cases (zh-CN)](docs/use-cases.zh-CN.md)

## License

Apache-2.0
