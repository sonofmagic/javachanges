# javachanges CLI 命令参考

[English](/cli-reference) | [简体中文](/zh-CN/cli-reference)

## 1. 概述

这份文档是 `javachanges` 的命令参考页。

适合这些场景：

- 你已经理解整体发布流程
- 现在只想查某个命令怎么写
- 想确认参数、输入输出和典型示例

## 2. 调用方式

本地最常见的调用方式：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="status --directory /path/to/repo"
```

常见组成部分：

| 片段 | 说明 |
| --- | --- |
| `mvn -q -DskipTests compile exec:java` | 编译 CLI 并运行 Java 入口 |
| `-Dexec.args="..."` | 传递 `javachanges` 命令行参数 |
| `--directory /path/to/repo` | 指定目标 Maven 仓库根目录或其子目录 |

> **注意**：有些命令不依赖仓库，例如 `release-version-from-tag`。

## 3. 高价值命令

| 命令 | 作用 | 是否写文件 |
| --- | --- | --- |
| `add` | 创建 changeset | `.changesets/*.md` |
| `status` | 查看当前 release plan | 否 |
| `plan` | 计算当前 release plan | 否 |
| `plan --apply true` | 应用 release plan 并消费 changesets | `pom.xml`、`CHANGELOG.md`、`.changesets/release-plan.json`、`.changesets/release-plan.md` |
| `manifest-field` | 读取生成后的 release manifest 字段 | 否 |
| `release-notes` | 为 tag 生成 release notes | 目标文件 |
| `preflight` | 输出发布前校验命令 | 否 |
| `publish` | 输出或执行实际发布命令 | 否 |

## 4. Changeset 相关命令

### 4.1 `add`

创建一个新的 changeset 文件。

示例：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="add --directory /path/to/repo --summary 'add release notes command' --release minor --modules core"
```

输入参数含义：

| 参数 | 说明 |
| --- | --- |
| `--summary` | 生成文件正文第一行 |
| `--release` | `patch`、`minor`、`major` |
| `--modules` | 逗号分隔的 Maven artifactId，或 `all` |
| `--body` | summary 之后追加的 Markdown 正文 |

生成文件的默认结构：

````md
```md
---
"core": minor
---

add release notes command
```
````

兼容性说明：

- `add` 仍然接受 `--release`、`--modules` 这类旧参数名
- 但实际写出的文件已经是官方 Changesets 风格的 package map

### 4.2 `status`

查看当前待发布的 release plan。

示例：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="status --directory /path/to/repo"
```

典型输出会包含：

- 当前根版本
- 最新 whole-repo tag
- 待处理 changeset 数量
- release plan 摘要
- affected packages
- 每一个待处理 changeset 条目

### 4.3 `plan`

只计算、不写文件：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="plan --directory /path/to/repo"
```

应用发布计划：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="plan --directory /path/to/repo --apply true"
```

加上 `--apply true` 之后，`javachanges` 会：

1. 更新根 `<revision>`
2. 往 `CHANGELOG.md` 前面插入新的 release section
3. 写入 `.changesets/release-plan.json`
4. 写入 `.changesets/release-plan.md`
5. 删除已消费的 changeset 文件

### 4.4 `manifest-field`

读取生成后的 release manifest 字段。

示例：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="manifest-field --directory /path/to/repo --field releaseVersion"
```

常见字段：

| 字段 | 说明 |
| --- | --- |
| `releaseVersion` | 不带 `v` 前缀的发布版本 |
| `nextSnapshotVersion` | 下一个根快照版本 |
| `releaseLevel` | 聚合后的发布类型 |

## 5. 仓库与版本相关命令

| 命令 | 作用 | 示例 |
| --- | --- | --- |
| `version` | 输出当前根 `revision` | `version --directory /path/to/repo` |
| `release-version-from-tag` | 从 `v1.2.3` 或 `core/v1.2.3` 里取出 `1.2.3` | `release-version-from-tag --tag v1.2.3` |
| `release-module-from-tag` | 从 `core/v1.2.3` 里取出 package/module 名称 | `release-module-from-tag --tag core/v1.2.3` |
| `assert-module` | 校验某个 Maven artifactId 是否存在 | `assert-module --directory /path/to/repo --module core` |
| `assert-snapshot` | 确认当前版本仍然是 snapshot | `assert-snapshot --directory /path/to/repo` |
| `assert-release-tag` | 校验 tag 是否和当前仓库版本一致 | `assert-release-tag --directory /path/to/repo --tag v1.2.3` |
| `module-selector-args` | 输出 Maven `-pl` 参数 | `module-selector-args --directory /path/to/repo --module core` |

## 6. 环境与 settings 相关命令

| 命令 | 作用 |
| --- | --- |
| `write-settings` | 生成 Maven `settings.xml` |
| `init-env` | 从示例模板初始化本地 env 文件 |
| `auth-help` | 输出平台认证要求 |
| `render-vars` | 预览 GitHub/GitLab 变量与 secrets |
| `doctor-local` | 检查本地发布环境 |
| `doctor-platform` | 检查远端平台状态 |
| `sync-vars` | 把变量同步到 GitHub 或 GitLab |
| `audit-vars` | 对比本地 env 和远端平台变量 |

示例：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="render-vars --directory /path/to/repo --env-file env/release.env.local --platform github"
```

## 7. 发布命令

### 7.1 `preflight`

输出正式发布前的 Maven 校验流程。

快照示例：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="preflight --directory /path/to/repo --snapshot"
```

正式版本示例：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="preflight --directory /path/to/repo --tag v1.2.3"
```

### 7.2 `publish`

只输出发布命令：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="publish --directory /path/to/repo --tag v1.2.3"
```

真正执行发布：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="publish --directory /path/to/repo --tag v1.2.3 --execute true"
```

关键参数：

| 参数 | 说明 |
| --- | --- |
| `--snapshot` | 发布当前 snapshot，而不是正式 tag |
| `--tag` | 目标发布 tag |
| `--module` | 限制到单个 Maven artifactId |
| `--allow-dirty` | 允许工作区不干净 |
| `--execute true` | 真正执行最终发布命令，而不是只打印 |

## 8. GitLab 发布命令

| 命令 | 作用 |
| --- | --- |
| `gitlab-release-plan` | 创建或更新 GitLab release-plan merge request |
| `gitlab-tag-from-plan` | 根据已生成的 release plan 创建正式 tag |

示例：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="gitlab-release-plan --directory /path/to/repo --project-id 12345 --execute true"
mvn -q -DskipTests compile exec:java -Dexec.args="gitlab-tag-from-plan --directory /path/to/repo --execute true"
```

## 9. 帮助输出

可以直接使用 `picocli` 的内置帮助：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="--help"
mvn -q -DskipTests compile exec:java -Dexec.args="plan --help"
```

## 10. 相关文档

| 需求 | 文档 |
| --- | --- |
| 初次上手与本地流程 | [Getting Started](./getting-started.md) |
| 本地开发方式 | [Development Guide](./development-guide.md) |
| 生成出来的 manifest 文件 | [Release Plan Manifest 说明](./release-plan-manifest.md) |
| GitHub Actions 接入 | [GitHub Actions Usage Guide](./github-actions-guide.md) |
| GitLab CI/CD 接入 | [GitLab CI/CD Usage Guide](./gitlab-ci-guide.md) |
