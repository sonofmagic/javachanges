# basic-gradle-monorepo 示例

[English](./README.md) | [简体中文](./README.zh-CN.md)

这个目录是一个可以直接参考或复制的 `javachanges` Gradle 示例仓库。

它演示了一个双项目 Gradle monorepo，包含：

- 使用 Gradle project name 的官方 Changesets 风格 `.changesets/*.md`
- `plan --apply true` 生成后的 release-plan 快照
- 最小 GitHub Actions 与 GitLab CI 模板
- 通过 release-plan manifest 交给 Gradle 原生 publishing

## 目录说明

| 路径 | 作用 |
| --- | --- |
| `settings.gradle.kts` | 声明 root project 和 included Gradle projects |
| `gradle.properties` | 保存由 `javachanges` 读取和写回的根 `version` |
| `build.gradle.kts` | 示例 project 共享的 Java 和 `maven-publish` 配置 |
| `modules/core/build.gradle.kts` | `core` Gradle project 示例 |
| `modules/api/build.gradle.kts` | 依赖 `core` 的 `api` Gradle project 示例 |
| `.changesets/20260418-add-release-notes.md` | package-map 风格的待发布变更 |
| `snapshots/` | `plan --apply true` 之后的生成结果 |
| `.github/workflows/` | 最小 GitHub Actions 示例 |
| `.gitlab-ci.yml` | 最小 GitLab CI 示例 |
| `env/release.env.example` | 发布变量示例 |

## 在当前源码仓库里体验这个示例

在 `javachanges` 仓库根目录运行：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="status --directory examples/basic-gradle-monorepo"
mvn -q -DskipTests compile exec:java -Dexec.args="plan --directory examples/basic-gradle-monorepo"
```

如果你只想看应用后的结果，又不想直接改动这个示例目录，可以直接查看 `snapshots/` 里的整理后文件。
这些已提交的 snapshot 是把示例复制到独立 Git 仓库后生成的基线结果；如果你直接在 `javachanges` 源码仓库里运行，Git 相关的版本计算仍然会看到外层仓库的 tag。

## 快照文件

| 路径 | 含义 |
| --- | --- |
| `snapshots/release-plan.json` | 机器可读的发布 manifest |
| `snapshots/release-plan.md` | release PR 正文 |
| `snapshots/CHANGELOG.after.md` | 应用 plan 后预期得到的 changelog |
| `snapshots/gradle.properties.after` | 版本推进后的 `gradle.properties` 示例 |

## CI 模板

这些 workflow / pipeline 模板默认假设 `javachanges` 已经发布，可以从 Maven Central 下载可执行 jar。

| 路径 | 作用 |
| --- | --- |
| `.github/workflows/ci.yml` | 构建 Gradle 仓库并打印待发布状态 |
| `.github/workflows/release-plan.yml` | 生成可审阅的 release-plan Pull Request |
| `.github/workflows/tag-release.yml` | 用 `github-tag-from-plan` 给合并后的 release commit 打 tag 并推送 |
| `.github/workflows/publish.yml` | 在 release tag push 后读取 manifest 并运行 `gradle publish` |
| `.gitlab-ci.yml` | 校验、创建 release MR、按 plan 打 tag、再用 Gradle 发布 |

复制到真实仓库前，请先替换 `JAVACHANGES_VERSION`、仓库地址、认证凭据，以及示例 Gradle project 元数据。
