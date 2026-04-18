# javachanges

[English](./README.md) | [简体中文](./README.zh-CN.md)

`javachanges` 是一个轻量的 Java CLI，为 Maven Monorepo 和单模块 Maven 仓库提供类似 Changesets 的发布规划工作流。

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

推荐的最短格式只需要 `release` 和 `summary`：

```md
---
release: minor
summary: add GitHub Actions release automation
---

- Add CI, release-plan PR creation, and publish workflows.
```

默认值：

- `type` 默认是 `other`
- `modules` 默认是 `all`
- 如果省略 `summary`，`javachanges` 会回退到正文第一条非空行

因此下面这种更短的写法也能工作：

```md
---
release: patch
---

Fix Windows path handling in release-notes generation.
```

只有在你需要覆盖默认值时，才需要完整格式：

```md
---
release: minor
type: ci
modules: javachanges
summary: automate javachanges self-release publishing via GitHub Actions
---
```

字段说明：

- `release`
  必填。决定本次 release plan 的语义化版本升级级别。
  可选值：`patch`、`minor`、`major`。
  一般规则：
  `patch` 用于兼容性修复、文档、杂项、CI、小改动。
  `minor` 用于向后兼容的新功能。
  `major` 用于破坏性变更。
- `summary`
  可选但强烈建议填写。
  会显示在 `status` 输出、release PR、changelog 和生成的 release notes 中。
  建议保持简洁、面向用户、使用动词开头。
  如果不填，会自动回退到正文第一条非空行。
- `type`
  可选，默认 `other`。
  用于 changelog 和 release plan 的分组。
  可选值：`feat`、`fix`、`docs`、`build`、`ci`、`test`、`refactor`、`perf`、`chore`、`other`。
  只有当你在意 changelog 分组时才需要显式写它。
- `modules`
  可选，默认 `all`。
  对于 Maven monorepo，可以写成逗号分隔的 artifactId，例如 `core, api`。
  对于单模块仓库，通常无需填写。
  只有当你希望 release plan 记录受影响模块时才需要显式写它。
- body
  可选，frontmatter 后面的自由 Markdown 正文。
  第一条非空正文可能会被用作 summary 回退值。
  在 changelog 渲染时，第一条正文也可能附加到 summary 后面作为补充说明。

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
