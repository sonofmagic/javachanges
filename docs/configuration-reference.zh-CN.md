# javachanges 配置参考大全


## 1. 概述

这份文档是 `javachanges` 的配置总表。

适合这些场景：

- 你想一次性看清有哪些配置面
- 想查 changeset 文件应该怎么写
- 想确认 CLI 参数会影响哪些行为
- 想核对 `env/release.env.example` 里的变量
- 想区分 GitHub / GitLab 平台变量和 secrets

## 2. 配置面总览

| 配置面 | 所在位置 | 典型维护者 |
| --- | --- | --- |
| Changeset 文件 | `.changesets/*.md` | 开发者 |
| CLI 调用参数 | 命令行 / CI workflow YAML | 维护者、CI 编写者 |
| 发布 env 模板 | `env/release.env.example` | 维护者 |
| 平台变量与 secrets | GitHub Actions / GitLab CI/CD 设置页 | 仓库管理员 |
| Maven 发布凭据 | 本地 env、CI secrets、生成后的 `settings.xml` | 发布维护者 |
| 仓库版本模型 | 根 `pom.xml` 的 `<revision>` | 维护者 |

## 3. Changeset 文件配置

### 3.1 推荐格式

`javachanges` 现在默认使用官方 Changesets 风格的 package map：

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

规则如下：

| 部分 | 含义 |
| --- | --- |
| frontmatter key | Maven artifactId |
| frontmatter value | `patch`、`minor`、`major` |
| Markdown 正文 | 面向用户的变更说明和补充备注 |

### 3.2 旧格式兼容

旧版 `javachanges` frontmatter 仍然可以读取：

```md
---
release: minor
type: ci
modules: javachanges
summary: automate javachanges self-release publishing via GitHub Actions
---
```

兼容状态：

| 字段 | 状态 |
| --- | --- |
| `release` | 旧字段，仍然可解析 |
| `modules` | 旧字段，仍然可解析 |
| `summary` | 旧字段，仍然可解析 |
| `type` | 旧元数据字段，仍然可解析 |

> **注意**：新文件建议统一使用官方 package-map 格式，不再推荐继续写这些旧字段。

## 4. 会影响行为的 CLI 参数

### 4.1 `add`

| 参数 | 作用 | 默认值 |
| --- | --- | --- |
| `--summary` | 生成文件正文第一行 | 不传时进入交互输入 |
| `--release` | 为每个选中 package 写入的发布类型 | 不传时进入交互输入 |
| `--modules` | 逗号分隔的 Maven artifactId，或 `all` | `all` |
| `--body` | summary 之后追加的 Markdown 正文 | 空 |
| `--type` | 旧版兼容元数据 | 默认不写 |

### 4.2 `plan`

| 参数 | 作用 | 默认值 |
| --- | --- | --- |
| `--apply` | 应用 release plan 并写文件 | `false` |
| `--directory` | 目标仓库路径 | 当前目录并向上解析 |

### 4.3 `preflight` 和 `publish`

| 参数 | 作用 | 默认值 |
| --- | --- | --- |
| `--snapshot` | 发布当前 snapshot 版本 | `false` |
| `--snapshot-build-stamp` | 显式指定 snapshot 发布标识 | 自动生成 |
| `--tag` | 发布正式 tag，例如 `v1.2.3` | 无 |
| `--module` | 把发布限制到单个 Maven artifactId | 所有 package |
| `--allow-dirty` | 跳过工作区脏检查 | `false` |
| `--execute` | 真正执行发布命令，而不是只打印 | `false` |

### 4.4 GitLab 发布命令

| 命令 | 参数 | 默认来源 |
| --- | --- | --- |
| `gitlab-release-plan` | `--project-id` | `CI_PROJECT_ID` |
| `gitlab-release-plan` | `--target-branch` | `CI_DEFAULT_BRANCH`，再回退到 `main` |
| `gitlab-release-plan` | `--release-branch` | `changeset-release/<target-branch>` |
| `gitlab-tag-from-plan` | `--before-sha` | `CI_COMMIT_BEFORE_SHA` |
| `gitlab-tag-from-plan` | `--current-sha` | `CI_COMMIT_SHA` |

## 5. `env/release.env.example`

当前模板里的变量如下：

| 变量 | 必填 | 含义 |
| --- | --- | --- |
| `MAVEN_RELEASE_REPOSITORY_URL` | 是 | Maven release 仓库地址 |
| `MAVEN_SNAPSHOT_REPOSITORY_URL` | 是 | Maven snapshot 仓库地址 |
| `MAVEN_RELEASE_REPOSITORY_ID` | 是 | Maven settings 中的 release server id |
| `MAVEN_SNAPSHOT_REPOSITORY_ID` | 是 | Maven settings 中的 snapshot server id |
| `MAVEN_REPOSITORY_USERNAME` | 是，除非你拆分了 release/snapshot 凭据 | 通用用户名回退值 |
| `MAVEN_REPOSITORY_PASSWORD` | 是，除非你拆分了 release/snapshot 凭据 | 通用密码回退值 |
| `MAVEN_RELEASE_REPOSITORY_USERNAME` | 否 | release 专用用户名覆盖值 |
| `MAVEN_RELEASE_REPOSITORY_PASSWORD` | 否 | release 专用密码覆盖值 |
| `MAVEN_SNAPSHOT_REPOSITORY_USERNAME` | 否 | snapshot 专用用户名覆盖值 |
| `MAVEN_SNAPSHOT_REPOSITORY_PASSWORD` | 否 | snapshot 专用密码覆盖值 |
| `JAVACHANGES_SNAPSHOT_BUILD_STAMP` | 否 | snapshot 发布标识；未设置时回退到 UTC 时间戳 + git short sha |
| `GITLAB_RELEASE_TOKEN` | 否 | 某些 GitLab release 场景下可选的额外 token |

代码中的解析回退规则：

| 设置项 | 回退逻辑 |
| --- | --- |
| release 用户名/密码 | `MAVEN_RELEASE_REPOSITORY_*`，再回退到通用 `MAVEN_REPOSITORY_*` |
| snapshot 用户名/密码 | `MAVEN_SNAPSHOT_REPOSITORY_*`，再回退到通用 `MAVEN_REPOSITORY_*` |
| release server id | `MAVEN_RELEASE_REPOSITORY_ID`，再回退到 `maven-releases` |
| snapshot server id | `MAVEN_SNAPSHOT_REPOSITORY_ID`，再回退到 `maven-snapshots` |

## 6. GitHub Actions 变量映射

### 6.1 推荐的 GitHub Actions variables

这些适合作为非敏感 variables：

| 名称 |
| --- |
| `MAVEN_RELEASE_REPOSITORY_URL` |
| `MAVEN_SNAPSHOT_REPOSITORY_URL` |
| `MAVEN_RELEASE_REPOSITORY_ID` |
| `MAVEN_SNAPSHOT_REPOSITORY_ID` |

### 6.2 推荐的 GitHub Actions secrets

| 名称 |
| --- |
| `MAVEN_REPOSITORY_USERNAME` |
| `MAVEN_REPOSITORY_PASSWORD` |
| `MAVEN_RELEASE_REPOSITORY_USERNAME` |
| `MAVEN_RELEASE_REPOSITORY_PASSWORD` |
| `MAVEN_SNAPSHOT_REPOSITORY_USERNAME` |
| `MAVEN_SNAPSHOT_REPOSITORY_PASSWORD` |

### 6.3 当前仓库自举发布额外需要的 Maven Central secrets

本仓库自己的正式发布 workflow 还需要：

| Secret | 用途 |
| --- | --- |
| `MAVEN_CENTRAL_USERNAME` | Sonatype Central Portal token 用户名 |
| `MAVEN_CENTRAL_PASSWORD` | Sonatype Central Portal token 密码 |
| `MAVEN_GPG_PRIVATE_KEY` | ASCII armored 私钥 |
| `MAVEN_GPG_PASSPHRASE` | GPG 口令 |

## 7. GitLab CI/CD 变量映射

### 7.1 从 env 文件同步到 GitLab 的变量

当执行 `sync-vars --platform gitlab` 时，这些值会被写入 GitLab variables：

| 名称 | 是否敏感 | 是否 protected |
| --- | --- | --- |
| `MAVEN_RELEASE_REPOSITORY_URL` | 否 | 否 |
| `MAVEN_SNAPSHOT_REPOSITORY_URL` | 否 | 否 |
| `MAVEN_RELEASE_REPOSITORY_ID` | 否 | 否 |
| `MAVEN_SNAPSHOT_REPOSITORY_ID` | 否 | 否 |
| `MAVEN_REPOSITORY_USERNAME` | 是 | 是 |
| `MAVEN_REPOSITORY_PASSWORD` | 是 | 是 |
| `MAVEN_RELEASE_REPOSITORY_USERNAME` | 是 | 是 |
| `MAVEN_RELEASE_REPOSITORY_PASSWORD` | 是 | 是 |
| `MAVEN_SNAPSHOT_REPOSITORY_USERNAME` | 是 | 是 |
| `MAVEN_SNAPSHOT_REPOSITORY_PASSWORD` | 是 | 是 |
| `GITLAB_RELEASE_TOKEN` | 是 | 是 |

### 7.2 GitLab CI 运行时额外依赖的变量

GitLab release 自动化还依赖这些运行时变量：

| 变量 | 来源 |
| --- | --- |
| `CI_PROJECT_ID` | GitLab 内置变量 |
| `CI_DEFAULT_BRANCH` | GitLab 内置变量 |
| `CI_SERVER_HOST` | GitLab 内置变量 |
| `CI_SERVER_URL` | GitLab 内置变量 |
| `CI_PROJECT_PATH` | GitLab 内置变量 |
| `CI_COMMIT_BEFORE_SHA` | GitLab 内置变量 |
| `CI_COMMIT_SHA` | GitLab 内置变量 |
| `GITLAB_RELEASE_BOT_USERNAME` | 你自行提供的项目变量 |
| `GITLAB_RELEASE_BOT_TOKEN` | 你自行提供的项目变量 |

## 8. Maven 发布运行时配置

### 8.1 仓库版本模型

推荐的根 `pom.xml` 写法：

```xml
<version>${revision}</version>
```

配合可变版本字段：

```xml
<revision>1.2.3-SNAPSHOT</revision>
```

### 8.2 本地 Maven 仓库覆盖

`publish` 和 `preflight` 会识别这样的 `MAVEN_OPTS`：

```bash
export MAVEN_OPTS="-Dmaven.repo.local=.m2/repository"
```

行为如下：

| 配置情况 | 结果 |
| --- | --- |
| 没有 `MAVEN_OPTS` 覆盖 | 使用目标仓库内部的 `.m2/repository` |
| `maven.repo.local` 是相对路径 | 相对目标仓库解析 |
| `maven.repo.local` 是绝对路径 | 直接使用该路径 |

## 9. 不同场景下的推荐默认值

### 9.1 单模块库

| 配置面 | 建议 |
| --- | --- |
| changeset 文件 | 一个 package key，对应根 artifactId |
| `--modules` | 省略或使用 `all` |
| release tag | `v1.2.3` |
| publish 目标 | 所有 package |

### 9.2 Maven monorepo

| 配置面 | 建议 |
| --- | --- |
| changeset 文件 | 每个受影响 artifactId 各写一条 frontmatter entry |
| `--modules` | 在 `add` 时显式传 artifactId |
| release tag | 默认 whole-repo `v1.2.3`，除非你有意使用 module tag |
| publish 目标 | 默认所有 package，需要时再用 `--module` 限制 |

## 10. 相关文档

| 需求 | 文档 |
| --- | --- |
| 每个命令的具体语法 | [CLI 命令参考](./cli-reference.md) |
| 生成出来的 manifest 文件 | [Release Plan Manifest 说明](./release-plan-manifest.md) |
| GitHub workflow 模式 | [GitHub Actions 使用指南](./github-actions-guide.md) |
| GitLab workflow 模式 | [GitLab CI/CD 使用指南](./gitlab-ci-guide.md) |
| Maven Central 自举发布 | [发布到 Maven Central](./publish-to-maven-central.md) |
