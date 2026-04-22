---
description: javachanges 命令总览，覆盖发布计划、环境检查和发布辅助命令。
---

# javachanges CLI 命令参考


## 1. 概述

这份文档是 `javachanges` 的命令参考页。

适合这些场景：

- 你已经理解整体发布流程
- 现在只想查某个命令怎么写
- 想确认参数、输入输出和典型示例

## 2. 调用方式

当前 `main` 分支在本地安装 SNAPSHOT 之后的 Maven plugin 调用方式：

```bash
mvn -q -DskipTests install
mvn io.github.sonofmagic:javachanges:__JAVACHANGES_CURRENT_SNAPSHOT_VERSION__:status
mvn io.github.sonofmagic:javachanges:__JAVACHANGES_CURRENT_SNAPSHOT_VERSION__:plan -Djavachanges.apply=true
mvn io.github.sonofmagic:javachanges:__JAVACHANGES_CURRENT_SNAPSHOT_VERSION__:add -Djavachanges.summary="add release notes command" -Djavachanges.release=minor
mvn io.github.sonofmagic:javachanges:__JAVACHANGES_CURRENT_SNAPSHOT_VERSION__:manifest-field -Djavachanges.field=releaseVersion
mvn io.github.sonofmagic:javachanges:__JAVACHANGES_CURRENT_SNAPSHOT_VERSION__:run -Djavachanges.args="release-notes --tag v1.2.3"
```

开发这个仓库本身时的源码调用方式：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="status --directory /path/to/repo"
```

常见组成部分：

| 片段 | 说明 |
| --- | --- |
| `mvn io.github.sonofmagic:javachanges:__JAVACHANGES_CURRENT_SNAPSHOT_VERSION__:status` | 执行独立的 status goal |
| `mvn io.github.sonofmagic:javachanges:__JAVACHANGES_CURRENT_SNAPSHOT_VERSION__:plan -Djavachanges.apply=true` | 执行独立的 plan goal |
| `mvn io.github.sonofmagic:javachanges:__JAVACHANGES_CURRENT_SNAPSHOT_VERSION__:run -Djavachanges.args="..."` | 对还没有独立 goal 的命令继续走通用桥接 goal |
| `mvn -q -DskipTests compile exec:java` | 编译 CLI 并运行 Java 入口 |
| `-Dexec.args="..."` | 传递 `javachanges` 命令行参数 |
| `--directory /path/to/repo` | 指定目标 Maven 仓库根目录或其子目录 |

plugin 说明：

- 所有独立 goal 和 `javachanges:run` 都会自动注入 `--directory ${project.basedir}`，除非你已经显式传了 `--directory`

如果你已经在目标仓库的 `pom.xml` 里声明了 plugin，本地最短写法就是：

```bash
mvn javachanges:status
mvn javachanges:plan -Djavachanges.apply=true
mvn javachanges:add -Djavachanges.summary="add release notes command" -Djavachanges.release=minor
mvn javachanges:manifest-field -Djavachanges.field=releaseVersion
```

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
| `ensure-gpg-public-key` | 发布并验证当前签名公钥是否已被支持的 keyserver 发现 | 否 |
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
| `render-vars` | 预览 GitHub/GitLab 变量与 secrets，或通过 `--format json` 输出 JSON |
| `doctor-local` | 检查本地发布环境，或通过 `--format json` 输出 JSON |
| `doctor-platform` | 检查远端平台状态，或通过 `--format json` 输出 JSON |
| `sync-vars` | 把变量同步到 GitHub 或 GitLab |
| `audit-vars` | 对比本地 env 和远端平台变量，或通过 `--format json` 输出 JSON |
| `ensure-gpg-public-key` | 上传当前签名公钥，并等待支持的 keyserver 能够查询到它 |

示例：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="render-vars --directory /path/to/repo --env-file env/release.env.local --platform github"
```

结构化输出示例：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="doctor-local --directory /path/to/repo --env-file env/release.env.local --format json"
```

当前支持 `--format json` 的命令：

| 命令 | 说明 |
| --- | --- |
| `render-vars` | 返回值里会带上 `platform` 和 `showSecrets` |
| `doctor-local` | 失败时会包含分组检查结果、建议列表和最终错误信息 |
| `doctor-platform` | 会带上 `platform` 以及 env / CLI 检查分组 |
| `audit-vars` | 会带上 `platform`、审计分组结果，以及失败时的最终错误信息 |

这些命令的常用参数：

| 参数 | 适用命令 | 含义 |
| --- | --- | --- |
| `--env-file` | 四者都支持 | 输入 env 文件路径 |
| `--platform` | `render-vars`、`doctor-platform`、`audit-vars` | `github`、`gitlab` 或 `all` |
| `--show-secrets` | `render-vars` | 显示原始 secret，而不是打码 |
| `--github-repo` | `doctor-local`、`doctor-platform`、`audit-vars` | 可选的 GitHub `owner/repo` 标识 |
| `--gitlab-repo` | `doctor-local`、`doctor-platform`、`audit-vars` | 可选的 GitLab `group/project` 标识 |
| `--format json` | 四者都支持 | 把标准输出从文本切换为机器可读 JSON |

### 6.1 `ensure-gpg-public-key`

在 CI 中完成 GPG 私钥导入后、执行 Maven Central 发布前，建议先运行：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="ensure-gpg-public-key --directory /path/to/repo"
```

这个命令会：

- 从 `gpg` 里读取当前已导入私钥对应的指纹
- 尝试把公钥发布到 `hkps://keyserver.ubuntu.com` 和 `hkps://keys.openpgp.org`
- 重试查询，直到至少一个受支持的 keyserver 能确认这个公钥可见

常用参数：

| 参数 | 说明 |
| --- | --- |
| `--primary-keyserver` | 覆盖主 keyserver 地址 |
| `--secondary-keyserver` | 覆盖备用 keyserver 地址 |
| `--attempts` | 最大探测次数 |
| `--retry-delay-seconds` | 每次探测之间的等待秒数 |

JSON 模式约定：

- 标准输出只包含一个 JSON 对象
- 退出码为 `0` 表示成功，非 `0` 表示校验失败或命令执行错误
- 顶层字段可能包含 `ok`、`command`、`envFile`、`platform`、`showSecrets`、`sections`、`suggestions`、`error`

## 7. 发布命令

### 7.2 `preflight`

输出正式发布前的 Maven 校验流程。

快照示例：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="preflight --directory /path/to/repo --snapshot"
```

指定快照构建标识：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="preflight --directory /path/to/repo --snapshot --snapshot-build-stamp 20260420.154500.ci001"
```

正式版本示例：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="preflight --directory /path/to/repo --tag v1.2.3"
```

### 7.3 `publish`

只输出发布命令：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="publish --directory /path/to/repo --tag v1.2.3"
```

真正执行发布：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="publish --directory /path/to/repo --tag v1.2.3 --execute true"
```

快照发布会把根 `1.2.3-SNAPSHOT` 解析成唯一的实际发布版本，例如 `1.2.3-20260420.154500.abc1234-SNAPSHOT`，再通过 `-Drevision=` 注入给 Maven。你也可以通过 `--snapshot-build-stamp` 或环境变量 `JAVACHANGES_SNAPSHOT_BUILD_STAMP` 显式指定构建标识。

关键参数：

| 参数 | 说明 |
| --- | --- |
| `--snapshot` | 发布当前 snapshot，而不是正式 tag |
| `--snapshot-build-stamp` | 显式指定 snapshot 发布标识，覆盖默认的 UTC 时间戳 + git short sha |
| `--tag` | 目标发布 tag |
| `--module` | 限制到单个 Maven artifactId |
| `--allow-dirty` | 允许工作区不干净 |
| `--execute true` | 真正执行最终发布命令，而不是只打印 |

## 8. 平台发布命令

### 8.1 GitHub 发布命令

| 命令 | 作用 |
| --- | --- |
| `github-release-plan` | 创建或更新 GitHub release-plan pull request |
| `github-tag-from-plan` | 根据已生成的 release plan 创建并推送正式 tag |
| `github-release-from-plan` | 生成发布元数据，并可选创建或更新 GitHub Release |

示例：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="github-release-plan --directory /path/to/repo --github-repo owner/repo --execute true"
mvn -q -DskipTests compile exec:java -Dexec.args="github-tag-from-plan --directory /path/to/repo --execute true"
mvn -q -DskipTests compile exec:java -Dexec.args="github-release-from-plan --directory /path/to/repo --release-notes-file target/release-notes.md --execute true"
```

### 8.2 GitLab 发布命令

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
