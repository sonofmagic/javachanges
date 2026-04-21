# basic-monorepo 示例

[English](./README.md) | [简体中文](./README.zh-CN.md)

这个目录是一个可以直接参考或复制的 `javachanges` 示例仓库。

它演示了一个双模块 Maven monorepo，包含：

- 官方 Changesets 风格的 `.changesets/*.md`
- `plan --apply true` 生成后的 release-plan 快照
- 能通过 CI/CD 发版的最小 GitHub Actions 与 GitLab CI 模板
- 一个用于仓库发布的 `env/release.env.example` 模板

## 目录说明

| 路径 | 作用 |
| --- | --- |
| `pom.xml` | 带统一 `revision` 和可发布 `distributionManagement` 的根 Maven 聚合工程 |
| `modules/core/pom.xml` | `javachanges-basic-monorepo-core` 模块示例 |
| `modules/api/pom.xml` | `javachanges-basic-monorepo-api` 模块示例 |
| `.changesets/20260418-add-release-notes.md` | package-map 风格的待发布变更 |
| `snapshots/` | `plan --apply true` 之后的生成结果 |
| `.github/workflows/` | 最小 GitHub Actions 示例 |
| `.gitlab-ci.yml` | 最小 GitLab CI 示例 |
| `env/release.env.example` | Maven 发布变量示例 |

## 在当前源码仓库里体验这个示例

在 `javachanges` 仓库根目录运行：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="status --directory examples/basic-monorepo"
mvn -q -DskipTests compile exec:java -Dexec.args="plan --directory examples/basic-monorepo"
```

如果你只想看应用后的结果，又不想直接改动这个示例目录，可以直接查看 `snapshots/` 里的整理后文件。
这些已提交的 snapshot 是把示例复制到独立 Git 仓库后生成的基线结果；如果你直接在 `javachanges` 源码仓库里运行，Git 相关的版本计算仍然会看到外层仓库的 tag。

## 快照文件

| 路径 | 含义 |
| --- | --- |
| `snapshots/release-plan.json` | 机器可读的发布 manifest |
| `snapshots/release-plan.md` | release PR 正文 |
| `snapshots/CHANGELOG.after.md` | 应用 plan 后预期得到的 changelog |
| `snapshots/pom.after.xml` | 根 `revision` 推进后的 `pom.xml` 示例 |

## CI 模板

这些 workflow / pipeline 模板默认假设 `javachanges` 已经发布，可以从 Maven Central 下载可执行 jar。
示例 Maven 坐标已经改成相对唯一、可安全演示发布的名字，但复制到真实仓库后仍建议替换成你自己的命名。

| 路径 | 作用 |
| --- | --- |
| `.github/workflows/ci.yml` | 构建 Maven 仓库并打印待发布状态 |
| `.github/workflows/release-plan.yml` | 生成可审阅的 release-plan Pull Request |
| `.github/workflows/tag-release.yml` | 用 `github-tag-from-plan` 给合并后的 release commit 打 tag 并推送 |
| `.github/workflows/publish.yml` | 在 release tag push 后用 `publish --execute true` 完成发布 |
| `.gitlab-ci.yml` | 校验、创建 release MR、按 plan 打 tag、再发布 |

复制到真实仓库前，请先替换 `JAVACHANGES_VERSION`、仓库地址、认证凭据，以及示例 Maven 坐标。
