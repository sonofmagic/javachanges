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
| 仓库版本模型 | Maven 根 `pom.xml` 的 `<revision>`，或 Gradle `gradle.properties` 的 `version` | 维护者 |

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
| frontmatter key | Maven artifactId 或 Gradle project name |
| frontmatter value | `patch`、`minor`、`major` |
| Markdown 正文 | 面向用户的变更说明和补充备注 |

### 3.2 `.changesets/config.json` / `.changesets/config.jsonc`

`javachanges` 也支持仓库级配置文件：

```jsonc
{
  "baseBranch": "main",
  "releaseBranch": "changeset-release/main",
  "snapshotBranch": "snapshot",
  "snapshotVersionMode": "plain",
  "tagStrategy": "whole-repo"
}
```

支持的形式：

- `.changesets/config.json`
- `.changesets/config.jsonc`
- 这两种文件里都允许写 `//` 和 `/* ... */` 注释

当前支持这些字段：

| 字段 | 含义 | 默认值 |
| --- | --- | --- |
| `baseBranch` | release-plan 自动化默认基线分支 | `main` |
| `releaseBranch` | 默认生成的 release 分支名 | `changeset-release/<baseBranch>` |
| `snapshotBranch` | 约定用于 snapshot 发布的分支名 | `snapshot` |
| `snapshotVersionMode` | snapshot 发布策略：`stamped` 或 `plain` | `stamped` |
| `tagStrategy` | release tag 策略：`whole-repo` 或 `per-module` | `whole-repo` |

当前行为：

- 当 CLI 参数和 CI 变量都没显式传入时，GitLab release-plan 默认值会从这个文件读取 `baseBranch` 和 `releaseBranch`
- GitLab tag 命令也会读取这里的 `baseBranch` 和 `releaseBranch`，避免在 release branch 或其他非基线分支误打 tag
- `preflight` 和 `publish` 会读取这里的 `snapshotBranch`，当前 CI 分支命中时自动进入 snapshot 模式
- `preflight` 和 `publish` 也会读取这里的 `snapshotVersionMode`；如果 CLI 显式传了 `--snapshot-version-mode`，则以 CLI 为准
- 当当前分支命中 `snapshotBranch`，并且 `snapshotVersionMode` 配成 `plain` 时，GitLab snapshot 发布 job 可以继续直接执行 `publish --execute true`
- `plan`、`github-tag-from-plan` 和 `gitlab-tag-from-plan` 也会读取 `tagStrategy`；如果配置成 `per-module`，会按本次受影响模块分别创建 `artifactId/vX.Y.Z` tag
- 本仓库里的 GitHub Actions 示例也遵循同一套分支命名约定
- `snapshotBranch` 不再只是文档约定字段，GitLab snapshot 发布链路会真正消费它

### 3.3 旧格式兼容

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
| `--modules` | 逗号分隔的 Maven artifactId、Gradle project name，或 `all` | `all` |
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
| `--snapshot-version-mode` | snapshot 版本策略：`stamped` 或 `plain` | 配置文件值，再回退到 `stamped` |
| `--snapshot-build-stamp` | 显式指定 snapshot 发布标识 | 自动生成 |
| `--tag` | 发布正式 tag，例如 `v1.2.3` | 无 |
| `--module` | 把发布限制到单个 Maven artifactId 或 Gradle project name | 所有 package |
| `--allow-dirty` | 跳过工作区脏检查 | `false` |
| `--execute` | 真正执行发布命令，而不是只打印 | `false` |

这些命令在 GitLab CI 中的默认行为：

- 如果存在 `CI_COMMIT_TAG`，省略 `--tag` 时会自动使用它
- 如果省略 `--snapshot`，并且当前分支命中 `.changesets/config.json` / `.changesets/config.jsonc` 的 `snapshotBranch`，会自动进入 snapshot 模式
- 如果省略 `--snapshot-version-mode`，会继续读取 `.changesets/config.json` / `.changesets/config.jsonc` 里的 `snapshotVersionMode`
- 这意味着 GitLab job 可以直接执行 `publish --execute true`

### 4.4 GitLab 发布命令

| 命令 | 参数 | 默认来源 |
| --- | --- | --- |
| `gitlab-release-plan` | `--project-id` | `CI_PROJECT_ID` |
| `gitlab-release-plan` | `--target-branch` | `CI_DEFAULT_BRANCH`，再回退到 `main` |
| `gitlab-release-plan` | `--release-branch` | `.changesets/config.*` 的 `releaseBranch`，再回退到 `changeset-release/<target-branch>` |
| `gitlab-tag-from-plan` | `--before-sha` | `CI_COMMIT_BEFORE_SHA` |
| `gitlab-tag-from-plan` | `--current-sha` | `CI_COMMIT_SHA` |
| `gitlab-tag-from-plan` | 分支保护逻辑 | `.changesets/config.*` 的 `baseBranch` 和 `releaseBranch` |
| `gitlab-release` | `--tag` | `CI_COMMIT_TAG` |
| `gitlab-release` | `--project-id` | `CI_PROJECT_ID` |
| `gitlab-release` | `--gitlab-host` | `CI_SERVER_HOST` |
| `init-gitlab-ci` | 生成的分支规则 | `.changesets/config.*` 的 `baseBranch` 和 `snapshotBranch` |

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

运维说明：

- `doctor-platform --platform gitlab` 现在会同时检查远端 protected variables 和配置里的 `snapshotBranch`
- 如果存在 protected variables，但 snapshot 分支没有被保护，命令会直接失败并给出修复建议，因为 GitLab 不会把 protected variables 注入到这个分支的 pipeline

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

## 8. 仓库版本与发布运行时配置

### 8.1 仓库版本模型

推荐的 Maven 根 `pom.xml` 写法：

```xml
<version>${revision}</version>
```

配合可变版本字段：

```xml
<revision>1.2.3-SNAPSHOT</revision>
```

推荐的 Gradle `gradle.properties` 写法：

```properties
version=1.2.3-SNAPSHOT
```

`javachanges` 也接受 `gradle.properties` 中的 `revision=1.2.3-SNAPSHOT`，但 Gradle 构建建议优先使用 `version`。

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

### 8.3 snapshot 版本模式

`javachanges` 现在支持两种 snapshot 发布模式：

| 模式 | 行为 |
| --- | --- |
| `stamped` | 会先把 `1.2.3-SNAPSHOT` 改写成 `1.2.3-20260420.154500.abc1234-SNAPSHOT` 这类唯一版本，再执行 deploy |
| `plain` | 保持 Maven 实际使用的版本仍然是原始 revision，例如 `1.2.3-SNAPSHOT` |

需要注意：

- plain 模式只表示 Maven 项目版本号不再被 `javachanges` 改写
- Maven / Nexus snapshot 仓库通常仍然会把最终保存的 snapshot 产物文件名展开成带时间戳的形式
- 这属于仓库端的标准 snapshot 行为，不是 `javachanges` 的二次改写

Gradle 说明：

- `preflight` 和 `publish` 是 Maven 专用
- Gradle 构建应使用 `manifest-field --field releaseVersion`，再执行 `./gradlew publish -Pversion=...`
- Gradle release planning 仍然会写入 `gradle.properties`、`CHANGELOG.md` 和 `.changesets/release-plan.*`

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

### 9.3 Gradle 单项目或多项目构建

| 配置面 | 建议 |
| --- | --- |
| 版本文件 | `gradle.properties`，并使用 `version=...-SNAPSHOT` |
| project 检测 | `settings.gradle(.kts)` 中的 `include(...)` |
| changeset 文件 | 每个受影响 Gradle project name 各写一条 frontmatter entry |
| `--modules` | 传 project name，例如 `core,api`，或使用 `all` |
| publish 目标 | Gradle `publish` task，并把 manifest 字段作为输入 |

### 9.4 为什么 Java monorepo 默认推荐 whole-repo tag

`javachanges` 对 Java monorepo 默认采用 `v1.2.3` 这种 whole-repo tag，这是有意的设计。

原因主要有这些：

- 很多 Maven 和 Gradle monorepo 实际上是在发一趟统一版本的 release train，多个 artifact 会一起升到同一个版本
- release PR、changelog、release notes、签名和 Maven Central 发布，通常也是一次仓库级发布动作
- Java 使用者更常按“这一版仓库 / 这一版平台是否兼容”来理解版本，而不只是看某个单独包

这和 npm monorepo 里常见的 Changesets 工作流不太一样。Changesets 更常见的是给每个实际发布的包打自己的 tag，例如 `pkg-a@1.2.3`，因为这些包往往是独立发布、独立消费的。

实际使用时可以这样理解：

- 如果你的 Maven 模块通常一起升级、一起发布，就优先使用 `v1.2.3` 这种 whole-repo tag
- 只有当你的仓库明确把模块当成独立 release 单元时，再考虑 module tag

`javachanges` 仍然可以解析像 `demo-app/v2.0.0` 这样的 module tag，用于后续 release 和 release notes 流程；但 release-plan 自动化默认只会创建一个 whole-repo tag。

如果你显式启用了 `per-module`：

- `plan --apply true` 仍然只会计算一个共享的 `releaseVersion`
- `github-tag-from-plan` 和 `gitlab-tag-from-plan` 会按受影响模块分别创建 tag，例如 `core/v1.2.3`
- `github-release-from-plan` 只适用于最终只解析出单个 tag 的场景；如果一个 plan 会产出多个 module tag，就应改用显式 tag 的 release 命令

## 10. 相关文档

| 需求 | 文档 |
| --- | --- |
| 每个命令的具体语法 | [CLI 命令参考](./cli-reference.md) |
| 生成出来的 manifest 文件 | [Release Plan Manifest 说明](./release-plan-manifest.md) |
| GitHub workflow 模式 | [GitHub Actions 使用指南](./github-actions-guide.md) |
| GitLab workflow 模式 | [GitLab CI/CD 使用指南](./gitlab-ci-guide.md) |
| Maven Central 自举发布 | [发布到 Maven Central](./publish-to-maven-central.md) |
