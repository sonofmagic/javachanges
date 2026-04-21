# Examples Guide 使用指南


## 1. 概述

这篇文档说明仓库里已经提交好的示例工程 `examples/basic-monorepo/` 应该怎么使用。

这个示例刻意保持很小，但已经覆盖了 `javachanges` 的完整链路：

| 路径 | 作用 |
| --- | --- |
| `examples/basic-monorepo/pom.xml` | 带统一 `revision` 的根 Maven monorepo |
| `examples/basic-monorepo/.changesets/` | 待发布变更文件 |
| `examples/basic-monorepo/snapshots/` | `plan --apply true` 之后的整理结果 |
| `examples/basic-monorepo/.github/workflows/` | 最小 GitHub Actions 模板 |
| `examples/basic-monorepo/.gitlab-ci.yml` | 最小 GitLab CI 模板 |

如果你不想只看零散文档，而是想直接参考一套完整的最小项目，就从这个示例开始。

## 2. 仓库结构

这个示例仓库包含两个 Maven 模块：

```text
examples/basic-monorepo/
├── .changesets/
├── .github/workflows/
├── env/
├── modules/
│   ├── api/
│   └── core/
├── snapshots/
├── .gitlab-ci.yml
├── CHANGELOG.md
└── pom.xml
```

核心约定如下：

- 根 `pom.xml` 负责统一管理 `revision`
- 根 `pom.xml` 里声明了 `modules/core` 和 `modules/api`
- `CHANGELOG.md` 在应用 plan 之前就已经存在
- 待发布变更保存在 `.changesets/*.md`

## 3. 示例 changeset 格式

这个示例使用的是官方 Changesets 风格的 package map：

```md
---
"javachanges-basic-monorepo-core": minor
"javachanges-basic-monorepo-api": minor
---

Add release notes generation workflow.

- Demonstrates a feature changeset for a two-module Maven monorepo.
- Shows how `javachanges plan` aggregates release metadata.
```

可以这样理解：

- `javachanges-basic-monorepo-core` 和 `javachanges-basic-monorepo-api` 是 Maven artifactId
- `minor` 表示这两个包都贡献一次 `minor` 升级
- 正文第一条非空文本会被当成 summary
- 后续 bullet 会进入 changelog 补充说明

## 4. 快照对照

在 `javachanges` 源码仓库根目录运行：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="status --directory examples/basic-monorepo"
mvn -q -DskipTests compile exec:java -Dexec.args="plan --directory examples/basic-monorepo --apply true"
```

如果你直接在 `javachanges` 源码仓库里原地运行这个示例，带 Git 语义的版本计算仍然可能看到外层仓库的 tag。
下面 `snapshots/` 里的基线值，对应的是把该示例复制到独立 Git 仓库之后的结果。

如果你不想直接改动示例目录，可以先看 `snapshots/` 里的整理结果：

| 快照文件 | 展示内容 |
| --- | --- |
| `examples/basic-monorepo/snapshots/release-plan.json` | 机器可读的发布元数据 |
| `examples/basic-monorepo/snapshots/release-plan.md` | release PR 正文 |
| `examples/basic-monorepo/snapshots/CHANGELOG.after.md` | 本次发布生成的 changelog 片段 |
| `examples/basic-monorepo/snapshots/pom.after.xml` | 发布后推进到下一个快照版本的根 `revision` |

这个 release-plan 快照对应的关键值如下：

| 字段 | 示例值 |
| --- | --- |
| `releaseVersion` | `0.2.0` |
| `nextSnapshotVersion` | `0.2.0-SNAPSHOT` |
| `releaseLevel` | `minor` |
| `modules` | `javachanges-basic-monorepo-core`、`javachanges-basic-monorepo-api` |

## 5. GitHub Actions 示例

示例仓库里包含 3 份 GitHub Actions 模板：

| 文件 | 作用 |
| --- | --- |
| `examples/basic-monorepo/.github/workflows/ci.yml` | 构建 Maven 仓库并执行 `status` |
| `examples/basic-monorepo/.github/workflows/release-plan.yml` | 应用 plan 并创建 release PR |
| `examples/basic-monorepo/.github/workflows/publish.yml` | release-plan PR 合并后自动发布，并回推 release tag |

这些模板默认假设：

- `javachanges` 以 jar 形式从 Maven Central 下载
- 固定版本由 `JAVACHANGES_VERSION` 控制
- Maven 凭据来自 GitHub Actions variables / secrets
- `actions/setup-java` 开启了 `cache: maven`
- 示例 POM 坐标已经足够唯一，可先安全演示发布，之后再替换成真实命名

这样做的好处是：目标仓库不需要把 `javachanges` 源码整个 vendoring 进去，也能直接复用这套流程。

## 6. GitLab CI 示例

`examples/basic-monorepo/.gitlab-ci.yml` 对应的是同一套生命周期：

1. `verify`
2. `release-plan`
3. `tag`
4. `publish`

模板在 `before_script` 里下载 `javachanges` jar，复用 Maven 依赖缓存，然后依次运行：

- 校验阶段执行 `status`
- 默认分支执行 `gitlab-release-plan --execute true`
- release plan 合并后执行 `gitlab-tag-from-plan --execute true`
- tag pipeline 中直接执行 `publish --execute true`，由命令内部处理 preflight 和 settings 生成

## 7. 如何改造成真实仓库

把这个示例复制到真实项目时，建议按这个顺序调整：

1. 替换示例里的 `groupId`、artifactId 和模块路径
2. 把 `.changesets/*.md` 里的包名改成你的真实 Maven artifactId
3. 更新 `env/release.env.example` 里的仓库地址和认证信息
4. 固定 CI 中要使用的 `JAVACHANGES_VERSION`
5. 在真正开启自动发布前，先本地运行一次 `status` 和 `plan`

## 8. 相关阅读

| 需求 | 文档 |
| --- | --- |
| 本地初始化和第一次生成 release plan | [Getting Started](./getting-started.md) |
| CLI 命令细节 | [CLI Reference](./cli-reference.md) |
| 生成的 manifest 字段说明 | [Release Plan Manifest](./release-plan-manifest.md) |
| GitHub Actions 完整配置 | [GitHub Actions Usage Guide](./github-actions-guide.md) |
| GitLab CI/CD 完整配置 | [GitLab CI/CD Usage Guide](./gitlab-ci-guide.md) |
