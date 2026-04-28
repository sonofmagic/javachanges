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
mvn io.github.sonofmagic:javachanges:__JAVACHANGES_CURRENT_SNAPSHOT_VERSION__:manifest-field -Djavachanges.field=releaseVersion -Djavachanges.fresh=true
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
| `--directory /path/to/repo` | 指定目标 Maven 或 Gradle 仓库根目录，或其子目录 |

plugin 说明：

- 所有独立 goal 和 `javachanges:run` 都会自动注入 `--directory ${project.basedir}`，除非你已经显式传了 `--directory`
- 对业务仓库或 CI 来说，也可以直接调用已发布的官方 Maven plugin，不需要额外维护 runner POM：

```bash
mvn -B io.github.sonofmagic:javachanges:__JAVACHANGES_LATEST_RELEASE_VERSION__:run -Djavachanges.args="gitlab-release-plan --directory $CI_PROJECT_DIR --write-plan-files false --execute true"
```

如果你已经在目标仓库的 `pom.xml` 里声明了 plugin，本地最短写法就是：

```bash
mvn javachanges:status
mvn javachanges:plan -Djavachanges.apply=true
mvn javachanges:add -Djavachanges.summary="add release notes command" -Djavachanges.release=minor
mvn javachanges:manifest-field -Djavachanges.field=releaseVersion -Djavachanges.fresh=true
```

Gradle 仓库应使用 CLI jar：

```bash
mvn -q dependency:copy -Dartifact=io.github.sonofmagic:javachanges:__JAVACHANGES_LATEST_RELEASE_VERSION__ -DoutputDirectory=.javachanges
java -jar .javachanges/javachanges-__JAVACHANGES_LATEST_RELEASE_VERSION__.jar status --directory /path/to/gradle-repo
java -jar .javachanges/javachanges-__JAVACHANGES_LATEST_RELEASE_VERSION__.jar plan --directory /path/to/gradle-repo --apply true
```

Gradle 检测要求 `gradle.properties` 中有 `version` 或 `revision`，并且仓库根目录有 `settings.gradle(.kts)` 或 `build.gradle(.kts)`。

> **注意**：有些命令不依赖仓库，例如 `release-version-from-tag`。

## 3. 通过 Maven 安全包装 `javachanges`

如果你通过 `exec-maven-plugin` 执行 `javachanges`，要保证完整 CLI 参数始终放在同一个 `-Dexec.args=...` 值里。不要先定义一个以裸 `-Dexec.args=` 结尾的可复用片段，再把真正命令在后面拼上去；Make、shell 或 CI runner 一旦把后续 token 单独拆开，Maven 就可能把它当成 lifecycle phase。

推荐的 Makefile 写法：

```make
MVNW := ./mvnw
JAVACHANGES_MVN := $(MVNW) -q -DskipTests compile exec:java

jc-version:
	$(JAVACHANGES_MVN) -Dexec.args="version --directory $(CURDIR)"
```

推荐的参数化 Makefile 写法：

```make
MVNW := ./mvnw
JAVACHANGES_MVN := $(MVNW) -q -DskipTests compile exec:java

define RUN_JAVACHANGES
$(JAVACHANGES_MVN) -Dexec.args="$(1) --directory $(CURDIR)"
endef

jc-status:
	$(call RUN_JAVACHANGES,status)
```

推荐的 GitLab CI 写法：

```yaml
script:
  - >
    ./mvnw -q -DskipTests compile exec:java
    -Dexec.args="version --directory $CI_PROJECT_DIR"
```

不推荐：

```make
JAVACHANGES = ./mvnw -q -DskipTests compile exec:java -Dexec.args=

jc-version:
	$(JAVACHANGES) "version --directory $(CURDIR)"
```

为什么会坏：

- `-Dexec.args=` 在前缀变量里已经结束了。
- 后面的 `version --directory ...` 已经不再属于这个 system property。
- Maven 会把它当成位置参数，进而可能解析成 lifecycle phase，于是报 `Unknown lifecycle phase "version --directory ..."`。

实用规则：

- `-Dexec.args="..."` 必须出现在最终命令行里，不要藏在前缀变量尾部
- 每次调用尽量保持成一条完整 shell 命令
- 如果要复用，就封装成 Make function 或仓库脚本
- 在 CI YAML 里优先用一条折叠命令，不要跨变量拼参数片段

## 4. 高价值命令

| 命令 | 作用 | 是否写文件 |
| --- | --- | --- |
| `add` | 创建 changeset | `.changesets/*.md` |
| `status` | 查看当前 release plan | 否 |
| `plan` | 计算当前 release plan | 否 |
| `plan --apply true` | 应用 release plan 并消费 changesets | `pom.xml` 或 `gradle.properties`、`CHANGELOG.md`、`.changesets/release-plan.json`、`.changesets/release-plan.md` |
| `manifest-field` | 读取生成后的 release manifest 字段，或用 `--fresh true` 从当前仓库状态推导 | 否 |
| `release-notes` | 为 tag 生成 release notes | 目标文件 |
| `ensure-gpg-public-key` | 发布并验证当前签名公钥是否已被支持的 keyserver 发现 | 否 |
| `preflight` | 输出发布前校验命令 | 否 |
| `publish` | 输出或执行 Maven 发布命令 | 否 |
| `gradle-publish` | 输出或执行 Gradle 发布命令 | 否 |

## 5. Changeset 相关命令

### 5.1 `add`

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
| `--modules` | 逗号分隔的 Maven artifactId、Gradle project name，或 `all` |
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

### 5.2 `status`

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

### 5.3 `plan`

只计算、不写文件：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="plan --directory /path/to/repo"
```

应用发布计划：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="plan --directory /path/to/repo --apply true"
```

加上 `--apply true` 之后，`javachanges` 会：

1. 更新根 Maven `<revision>` 或 Gradle `gradle.properties` 版本
2. 往 `CHANGELOG.md` 前面插入新的 release section
3. 写入 `.changesets/release-plan.json`
4. 写入 `.changesets/release-plan.md`
5. 删除已消费的 changeset 文件

`github-release-plan` 和 `gitlab-release-plan` 这类自动化命令可以传
`--write-plan-files false`，避免把生成的 `release-plan.json` 和
`release-plan.md` 提交进 release 分支。这个模式下 PR/MR 正文会作为临时文件生成，
后续 tag / release job 应使用 `--fresh true`。

### 5.4 `manifest-field`

读取生成后的 release manifest 字段，或从当前仓库状态推导字段。

示例：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="manifest-field --directory /path/to/repo --field releaseVersion"
mvn -q -DskipTests compile exec:java -Dexec.args="manifest-field --directory /path/to/repo --field releaseVersion --fresh true"
```

常见字段：

| 字段 | 说明 |
| --- | --- |
| `releaseVersion` | 不带 `v` 前缀的发布版本 |
| `nextSnapshotVersion` | 下一个根快照版本 |
| `releaseLevel` | 聚合后的发布类型 |

`--fresh true` 会优先使用仍然存在的 pending changesets。release plan 已经应用、
changesets 已经被消费后，它会从当前快照版本推导 whole-repo 发布元数据，例如
`1.2.0-SNAPSHOT` 会推导出发布版本 `1.2.0`。

## 6. 仓库与版本相关命令

| 命令 | 作用 | 示例 |
| --- | --- | --- |
| `version` | 输出当前根版本 | `version --directory /path/to/repo` |
| `release-version-from-tag` | 从 `v1.2.3` 或 `core/v1.2.3` 里取出 `1.2.3` | `release-version-from-tag --tag v1.2.3` |
| `release-module-from-tag` | 从 `core/v1.2.3` 里取出 package/module 名称 | `release-module-from-tag --tag core/v1.2.3` |
| `assert-module` | 校验某个 Maven artifactId 或 Gradle project name 是否存在 | `assert-module --directory /path/to/repo --module core` |
| `assert-snapshot` | 确认当前版本仍然是 snapshot | `assert-snapshot --directory /path/to/repo` |
| `assert-release-tag` | 校验 tag 是否和当前仓库版本一致 | `assert-release-tag --directory /path/to/repo --tag v1.2.3` |
| `module-selector-args` | 输出构建工具模块选择参数 | `module-selector-args --directory /path/to/repo --module core` |

Maven 仓库中，`module-selector-args --module core` 输出 Maven `-pl :core -am`。
Gradle 仓库中，它输出 Gradle project selector `:core`。

## 7. 环境与 settings 相关命令

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
| `preflight` | 会带上发布动作元数据，以及 `snapshotVersionMode`、`effectiveVersion`、`snapshotBuildStampApplied` 等 snapshot 模式字段 |
| `publish` | 会带上 tag、module、releaseVersion、releaseNotesFile 等发布元数据 |
| `gradle-publish` | 会带上 Gradle 发布动作元数据，例如 tag、module、releaseVersion 和 snapshot mode |
| `github-release-plan` | 会带上 action、是否 skipped、releaseVersion |
| `github-tag-from-plan` | 会带上 action、是否 skipped、releaseVersion、tag |
| `github-release-from-plan` | 会带上 action、tag、releaseVersion、releaseNotesFile |
| `gitlab-release-plan` | 会带上 action、是否 skipped、releaseVersion、projectId |
| `gitlab-tag-from-plan` | 会带上 action、是否 skipped、releaseVersion、releaseModule、tag |
| `gitlab-release` | 会带上 action、projectId、tag、releaseModule、releaseVersion、releaseNotesFile |

这些命令的常用参数：

| 参数 | 适用命令 | 含义 |
| --- | --- | --- |
| `--env-file` | 四者都支持 | 输入 env 文件路径 |
| `--platform` | `render-vars`、`doctor-platform`、`audit-vars` | `github`、`gitlab` 或 `all` |
| `--show-secrets` | `render-vars` | 显示原始 secret，而不是打码 |
| `--github-repo` | `doctor-local`、`doctor-platform`、`audit-vars` | 可选的 GitHub `owner/repo` 标识 |
| `--gitlab-repo` | `doctor-local`、`doctor-platform`、`audit-vars` | 可选的 GitLab `group/project` 标识 |
| `--format json` | 四者都支持 | 把标准输出从文本切换为机器可读 JSON |

### 7.1 `ensure-gpg-public-key`

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

## 8. 发布命令

### 8.1 `preflight`

输出正式发布前的 Maven 校验流程。

快照示例：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="preflight --directory /path/to/repo --snapshot"
```

plain snapshot 示例：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="preflight --directory /path/to/repo --snapshot --snapshot-version-mode plain"
```

指定快照构建标识：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="preflight --directory /path/to/repo --snapshot --snapshot-build-stamp 20260420.154500.ci001"
```

正式版本示例：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="preflight --directory /path/to/repo --tag v1.2.3"
```

GitLab snapshot 分支默认值示例：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="preflight --directory $CI_PROJECT_DIR"
```

在 plain snapshot 模式下，`preflight` 会明确输出当前使用的是 `plain snapshot`，并保持实际发布版本仍然是 `pom.xml` 里的原始值，例如 `1.2.3-SNAPSHOT`。

### 8.2 `publish`

只输出 Maven 发布命令：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="publish --directory /path/to/repo --tag v1.2.3"
```

真正执行发布：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="publish --directory /path/to/repo --tag v1.2.3 --execute true"
```

快照发布会把根 `1.2.3-SNAPSHOT` 解析成唯一的实际发布版本，例如 `1.2.3-20260420.154500.abc1234-SNAPSHOT`，再通过 `-Drevision=` 注入给 Maven。你也可以通过 `--snapshot-build-stamp` 或环境变量 `JAVACHANGES_SNAPSHOT_BUILD_STAMP` 显式指定构建标识。

如果传入 `--snapshot-version-mode plain`，`publish` 会保持 Maven 实际使用的版本仍然是原始 snapshot revision，例如 `1.2.3-SNAPSHOT`，而不是改写成带 stamp 的版本。`preflight` 和 `publish` 都会打印当前 snapshot mode，方便你在 CI 日志里直接确认这次发布走的是 `plain` 还是 `stamped`。

关于 Maven snapshot 仓库有一个容易混淆的点：

- plain 模式表示项目版本号保持 `1.2.3-SNAPSHOT`
- Maven / Nexus snapshot 仓库通常仍然会把上传后的产物文件名展开成带时间戳和 build number 的 snapshot 文件名
- 这是 Maven snapshot 仓库的标准行为，不是 `javachanges` 又对版本号做了一次改写

GitLab CI 默认行为：

- 如果存在 `CI_COMMIT_TAG`，`publish` 会自动使用它，因此 tag job 只需要执行 `publish --execute true`
- 如果当前分支命中 `.changesets/config.json` / `.changesets/config.jsonc` 的 `snapshotBranch`，`publish` 和 `preflight` 会自动切到 snapshot 模式
- 如果仓库配置里同时设置了 `"snapshotVersionMode": "plain"`，同一条 GitLab snapshot branch 发布链路会自动进入 plain snapshot 模式
- 这样业务仓库就不需要再写 shell 分支来区分 tag 发布和 snapshot 发布

关键参数：

| 参数 | 说明 |
| --- | --- |
| `--snapshot` | 发布当前 snapshot，而不是正式 tag |
| `--snapshot-version-mode` | snapshot 版本策略：`stamped` 或 `plain` |
| `--snapshot-build-stamp` | 显式指定 snapshot 发布标识，覆盖默认的 UTC 时间戳 + git short sha |
| `--tag` | 目标发布 tag |
| `--module` | 限制到单个 Maven artifactId 或 Gradle project name |
| `--allow-dirty` | 允许工作区不干净 |
| `--execute true` | 真正执行最终发布命令，而不是只打印 |

### 8.3 `gradle-publish`

渲染 Gradle 发布命令：

```bash
java -jar .javachanges/javachanges-__JAVACHANGES_LATEST_RELEASE_VERSION__.jar gradle-publish --directory /path/to/repo --tag v1.2.3
```

真正执行：

```bash
java -jar .javachanges/javachanges-__JAVACHANGES_LATEST_RELEASE_VERSION__.jar gradle-publish --directory /path/to/repo --tag v1.2.3 --execute true
```

snapshot 示例：

```bash
java -jar .javachanges/javachanges-__JAVACHANGES_LATEST_RELEASE_VERSION__.jar gradle-publish --directory /path/to/repo --snapshot true
```

自定义 task 示例：

```bash
java -jar .javachanges/javachanges-__JAVACHANGES_LATEST_RELEASE_VERSION__.jar gradle-publish --directory /path/to/repo --tag v1.2.3 --task publishAllPublicationsToMavenRepository
```

`gradle-publish` 会复用和 `publish` 一样的 release / snapshot 版本解析，然后渲染 `./gradlew --no-daemon publish -Pversion=...`。如果传入 `--module api`，会渲染 `./gradlew --no-daemon :api:publish -Pversion=...`。使用 `--task` 可以替换最终 Gradle task name。

Gradle 仓库注意事项：

- 这个命令不会生成 Maven `settings.xml`
- 仓库地址和凭据仍然应该放在 Gradle build 或 CI 环境里
- 如果实际 publication task 名不是 `publish`，请传入 `--task`

## 9. 平台发布命令

### 9.1 GitHub 发布命令

| 命令 | 作用 |
| --- | --- |
| `github-release-plan` | 创建或更新 GitHub release-plan pull request |
| `github-tag-from-plan` | 根据已生成的 release plan 创建并推送正式 tag |
| `github-release-from-plan` | 生成发布元数据，并可选创建或更新 GitHub Release |
| `init-github-actions` | 生成最小可用的 GitHub Actions workflow，串起 release-plan、tag、publish 和 GitHub Release job |

示例：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="github-release-plan --directory /path/to/repo --github-repo owner/repo --write-plan-files false --execute true"
mvn -q -DskipTests compile exec:java -Dexec.args="github-tag-from-plan --directory /path/to/repo --fresh true --execute true"
mvn -q -DskipTests compile exec:java -Dexec.args="github-release-from-plan --directory /path/to/repo --fresh true --release-notes-file target/release-notes.md --execute true"
mvn -q -DskipTests compile exec:java -Dexec.args="github-release-from-plan --directory /path/to/repo --format json"
mvn -q -DskipTests compile exec:java -Dexec.args="init-github-actions --directory /path/to/repo --output .github/workflows/javachanges-release.yml --force true"
mvn -q -DskipTests compile exec:java -Dexec.args="init-github-actions --directory /path/to/gradle-repo --build-tool gradle --output .github/workflows/javachanges-release.yml --force true"
```

GitHub 的 release-plan、tag 和 release 命令也都支持 `--format json`，方便在 CI 里直接消费机器可读输出。

### 9.2 GitLab 发布命令

| 命令 | 作用 |
| --- | --- |
| `gitlab-release-plan` | 创建或更新 GitLab release-plan merge request |
| `gitlab-tag-from-plan` | 根据已生成的 release plan 创建正式 tag |
| `gitlab-release` | 生成 release notes，并为当前 tag 创建或更新 GitLab Release |
| `init-gitlab-ci` | 生成最小可用的 GitLab CI 模板，串起 release-plan、tag、publish 和 GitLab Release job |

示例：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="gitlab-release-plan --directory /path/to/repo --project-id 12345 --write-plan-files false --execute true"
mvn -q -DskipTests compile exec:java -Dexec.args="gitlab-tag-from-plan --directory /path/to/repo --fresh true --execute true"
mvn -q -DskipTests compile exec:java -Dexec.args="gitlab-release --directory /path/to/repo --execute true"
mvn -q -DskipTests compile exec:java -Dexec.args="init-gitlab-ci --directory /path/to/repo --output .gitlab-ci.yml --force true"
mvn -q -DskipTests compile exec:java -Dexec.args="init-gitlab-ci --directory /path/to/gradle-repo --build-tool gradle --output .gitlab-ci.yml --force true"
```

## 10. 帮助输出

可以直接使用 `picocli` 的内置帮助：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="--help"
mvn -q -DskipTests compile exec:java -Dexec.args="plan --help"
```

## 11. 相关文档

| 需求 | 文档 |
| --- | --- |
| 初次上手与本地流程 | [Getting Started](./getting-started.md) |
| 本地开发方式 | [Development Guide](./development-guide.md) |
| 生成出来的 manifest 文件 | [Release Plan Manifest 说明](./release-plan-manifest.md) |
| GitHub Actions 接入 | [GitHub Actions Usage Guide](./github-actions-guide.md) |
| GitLab CI/CD 接入 | [GitLab CI/CD Usage Guide](./gitlab-ci-guide.md) |
