# javachanges Release Plan Manifest 说明


## 1. 概述

当你执行：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="plan --directory /path/to/repo --apply true"
```

`javachanges` 会生成两个文件：

| 文件 | 用途 |
| --- | --- |
| `.changesets/release-plan.json` | 机器可读的 release manifest |
| `.changesets/release-plan.md` | 给人看的 release PR 正文 |

这份文档说明这两个文件里有什么，以及 CI/CD 一般如何消费它们。

## 2. 生成时机

只有在 `plan --apply true` 成功执行后，这两个文件才会出现或更新。

也就是说：

- `status` 不会写它们
- 不带 `--apply true` 的 `plan` 也不会写它们
- 它们表示的是“已经应用”的 release plan，而不是纯预览结果

## 3. `release-plan.json`

典型结构：

```json
{
  "releaseVersion": "__JAVACHANGES_LATEST_RELEASE_VERSION__",
  "nextSnapshotVersion": "1.3.2-SNAPSHOT",
  "releaseLevel": "minor",
  "generatedAt": "2026-04-19T12:34:56+08:00",
  "changesets": [
    {
      "file": "20260419-example.md",
      "release": "minor",
      "type": "other",
      "summary": "add release notes command",
      "modules": ["javachanges"]
    }
  ]
}
```

字段说明：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `releaseVersion` | string | 不带 `v` 前缀的正式发布版本 |
| `nextSnapshotVersion` | string | 应用计划后写回 `pom.xml` 或 `gradle.properties` 的下一个根快照版本 |
| `releaseLevel` | string | 当前所有待发布 changeset 聚合后的发布类型 |
| `generatedAt` | string | manifest 生成时间 |
| `changesets` | array | 本次被消费的 changeset 列表 |

每个 changeset 条目的字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `file` | string | 原始 changeset 文件名 |
| `release` | string | 这个 changeset 声明的发布类型 |
| `type` | string | 为兼容旧格式保留的字段，常见值是 `other`，新集成里可以忽略 |
| `summary` | string | 从正文第一行或旧 frontmatter 派生出的用户可见摘要 |
| `modules` | array | 这个 changeset 影响到的 Maven artifactId 或 Gradle project name |

> **注意**：当前 JSON 字段名仍然叫 `modules`，这是为了兼容现有实现；用户可见文档层面现在更推荐使用 `packages` 这个术语。
>
> **注意**：如果你使用的是官方 package-map changeset 格式，manifest 里仍然可能出现 `"type": "other"`。它不是发布级别，真正表示发布升级类型的是 `release`。

## 4. `release-plan.md`

这个文件是自动生成的 release PR 正文。

里面通常会包含：

- release type
- affected packages
- release version
- next snapshot version
- included changesets

它适合被用在：

- GitHub Pull Request 正文
- GitLab Merge Request 描述
- release branch 合并前的人审流程

## 5. 常见消费方式

### 5.1 本地脚本

读取单个字段：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="manifest-field --directory /path/to/repo --field releaseVersion"
```

### 5.2 GitHub Actions

常见用途：

- 读取 `releaseVersion` 拼 PR 标题
- 创建最终发布 tag 时读取 `releaseVersion`
- 把 `.changesets/release-plan.md` 直接作为 PR 正文
- Gradle 仓库可以把 `releaseVersion` 传给 `./gradlew publish -Pversion=...`

### 5.3 GitLab CI/CD

常见用途：

- 比较两个提交之间 `release-plan.json` 是否发生变化
- 只有在新的已应用 release plan 存在时才创建正式 tag
- 生成或更新 release-plan merge request 的正文

## 6. 同时会被更新的文件

生成 manifest 时，通常同一个提交里还会一起更新这些文件：

| 文件 | 原因 |
| --- | --- |
| `pom.xml` 或 `gradle.properties` | 根 Maven `<revision>` 或 Gradle 版本推进到下一个 snapshot |
| `CHANGELOG.md` | 插入新的 release section |
| `.changesets/release-plan.json` | 机器可读的发布数据 |
| `.changesets/release-plan.md` | 给人看的 release PR 正文 |

同时，被消费掉的 `.changesets/*.md` 会被删除。

## 7. 常见错误

| 问题 | 原因 | 修复方式 |
| --- | --- | --- |
| `manifest-field` 失败 | 还没执行 `plan --apply true` | 先生成并应用 release plan |
| CI 里一直不打 tag | `release-plan.json` 没变化 | 确认新的 plan 已经被应用并提交 |
| release PR 正文不是最新 | `.changesets/release-plan.md` 没重新生成 | 重跑 release-plan job |
| CI 里的版本读取错了 | workflow 读的是 `pom.xml` 或 `gradle.properties` 而不是 manifest | 改成 `manifest-field --field releaseVersion` |

## 8. 相关文档

| 需求 | 文档 |
| --- | --- |
| 读取 manifest 字段的命令 | [CLI 命令参考](./cli-reference.md) |
| 首次使用 `plan --apply true` | [Getting Started](./getting-started.md) |
| Gradle release handoff | [Gradle 使用指南](./gradle-guide.md) |
| GitHub Actions 如何消费 manifest | [GitHub Actions Usage Guide](./github-actions-guide.md) |
| GitLab CI/CD 如何消费 manifest | [GitLab CI/CD Usage Guide](./gitlab-ci-guide.md) |
