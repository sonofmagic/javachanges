---
description: 在 Gradle 单项目和多项目构建中使用 javachanges。
---

# Gradle 使用指南


## 1. Gradle 支持范围

`javachanges` 可以在没有 Maven `pom.xml` 的 Gradle 仓库里做发布规划。

Gradle 路径支持：

- 通过 `gradle.properties` 加 `settings.gradle`、`settings.gradle.kts`、`build.gradle` 或 `build.gradle.kts` 识别仓库根目录
- 从 `gradle.properties` 读取当前版本
- 在 `plan --apply true` 时写回版本
- 从 Gradle `include(...)` 检测 package
- 用 root project name 支持单项目仓库
- changeset、status、changelog、release-plan manifest、GitHub release PR、GitLab release MR

`preflight` 和 `publish` 仍然是 Maven 发布辅助命令，会生成 Maven deploy 命令和 Maven `settings.xml`。Gradle artifact 的实际发布请继续使用 Gradle 原生 publishing task；`javachanges` 负责规划、changelog、manifest 和 release 自动化。

## 2. Gradle 仓库要求

Gradle 仓库应该把根版本放在 `gradle.properties`：

```properties
version=1.4.0-SNAPSHOT
```

`javachanges` 也接受这个兼容 key：

```properties
revision=1.4.0-SNAPSHOT
```

新 Gradle 仓库建议优先使用 `version`，因为这是标准 Gradle project property。

单项目 Gradle 仓库至少应具备：

```text
gradle.properties
settings.gradle
build.gradle
```

或：

```text
gradle.properties
settings.gradle.kts
build.gradle.kts
```

多项目 Gradle 仓库应在 `settings.gradle` 或 `settings.gradle.kts` 中声明 included projects。

Groovy DSL：

```groovy
rootProject.name = 'payments'
include 'api', 'core'
```

Kotlin DSL：

```kotlin
rootProject.name = "payments"
include(":api", ":core")
```

支持嵌套 project path。`include(":tools:cli")` 在 changeset 中对应的 package key 是 `cli`。

## 3. 安装并运行 CLI

Gradle 仓库应使用正式发布的 CLI jar。Maven plugin 仍然适合 Maven 仓库，但 Gradle 构建应直接调用 CLI。

下载 jar：

```bash
mvn -q dependency:copy \
  -Dartifact=io.github.sonofmagic:javachanges:__JAVACHANGES_LATEST_RELEASE_VERSION__ \
  -DoutputDirectory=.javachanges
```

设置一个复用变量：

```bash
export JAVACHANGES="java -jar .javachanges/javachanges-__JAVACHANGES_LATEST_RELEASE_VERSION__.jar"
```

在 Gradle 仓库里执行：

```bash
$JAVACHANGES status --directory .
```

如果你是在 `javachanges` 源码仓库中开发，可以这样指向一个 Gradle 仓库：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="status --directory /path/to/gradle-repo"
```

## 4. 单项目 Gradle 工作流

示例仓库：

```text
my-library/
├── .changesets/
├── CHANGELOG.md
├── build.gradle.kts
├── gradle.properties
└── settings.gradle.kts
```

`settings.gradle.kts`：

```kotlin
rootProject.name = "my-library"
```

`gradle.properties`：

```properties
version=0.8.0-SNAPSHOT
```

创建 patch changeset：

```bash
$JAVACHANGES add --directory . \
  --summary "fix generated release notes" \
  --release patch
```

生成的 changeset 会使用 root project name：

```md
---
"my-library": patch
---

fix generated release notes
```

查看并应用：

```bash
$JAVACHANGES status --directory .
$JAVACHANGES plan --directory .
$JAVACHANGES plan --directory . --apply true
```

应用后：

- `gradle.properties` 推进到下一个 snapshot 版本
- `CHANGELOG.md` 新增 release section
- 写入 `.changesets/release-plan.json`
- 写入 `.changesets/release-plan.md`
- 删除已消费的 `.changesets/*.md`

## 5. 多项目 Gradle 工作流

示例 `settings.gradle.kts`：

```kotlin
rootProject.name = "payments"
include(":api", ":core", ":tools:cli")
```

检测到的 package name：

| Gradle project path | Changeset package key |
| --- | --- |
| `:api` | `api` |
| `:core` | `core` |
| `:tools:cli` | `cli` |

创建一个影响两个 project 的 changeset：

```bash
$JAVACHANGES add --directory . \
  --summary "add payment retry metadata" \
  --release minor \
  --modules api,core
```

手写格式：

```md
---
"api": minor
"core": minor
---

add payment retry metadata
```

如果变更影响所有检测到的 Gradle project，可以使用 `--modules all`：

```bash
$JAVACHANGES add --directory . \
  --summary "standardize Gradle publication metadata" \
  --release patch \
  --modules all
```

## 6. CI release-plan 自动化

在 GitHub Actions 或 GitLab CI 里，release planning 命令和 Maven 仓库一致：

```bash
java -jar .javachanges/javachanges-__JAVACHANGES_LATEST_RELEASE_VERSION__.jar \
  github-release-plan \
  --directory "$GITHUB_WORKSPACE" \
  --github-repo "$GITHUB_REPOSITORY" \
  --execute true
```

GitLab：

```bash
java -jar .javachanges/javachanges-__JAVACHANGES_LATEST_RELEASE_VERSION__.jar \
  gitlab-release-plan \
  --directory "$CI_PROJECT_DIR" \
  --project-id "$CI_PROJECT_ID" \
  --execute true
```

Gradle 仓库的 release-plan 自动化会 stage `gradle.properties`、`CHANGELOG.md` 和 `.changesets/`。

从已应用的 plan 创建 tag 的方式不变：

```bash
$JAVACHANGES github-tag-from-plan --directory . --execute true
$JAVACHANGES gitlab-tag-from-plan --directory . --execute true
```

## 7. 发布 Gradle artifacts

优先用 `gradle-publish` 作为 Gradle 原生发布任务的 dry-run 交接点：

```bash
$JAVACHANGES gradle-publish --directory . --tag v1.4.0
```

确认渲染出的 Gradle 命令后，再执行：

```bash
$JAVACHANGES gradle-publish --directory . --tag v1.4.0 --execute true
```

snapshot 发布同理：

```bash
$JAVACHANGES gradle-publish --directory . --snapshot true
```

如果只发布某个 Gradle project，传入检测到的 project name：

```bash
$JAVACHANGES gradle-publish --directory . --snapshot true --module api
```

这个命令会渲染 `./gradlew --no-daemon publish -Pversion=...`；传入 `--module api` 时会渲染 `./gradlew --no-daemon :api:publish -Pversion=...`。它不接管 Gradle 仓库凭据，凭据和 publication repository 仍然放在 Gradle build 或 CI 环境里。

如果你需要自定义 Gradle task，仍然可以手动消费 release-plan manifest：

```bash
RELEASE_VERSION="$($JAVACHANGES manifest-field --directory . --field releaseVersion)"
./gradlew customPublishTask -Pversion="$RELEASE_VERSION"
```

如果你的 Gradle build 已经从 `gradle.properties` 读取 `version`，应用 release plan 后该文件已经推进到下一个 snapshot。release tag 和 release notes 使用 manifest，真正发布逻辑仍放在 Gradle build 内部。

## 8. 常见错误

| 现象 | 原因 | 修复方式 |
| --- | --- | --- |
| `Cannot find repository root` | 缺少 `gradle.properties`，或没有 Gradle settings/build 文件 | 添加 `gradle.properties` 和 `settings.gradle(.kts)` 或 `build.gradle(.kts)` |
| `Cannot find version or revision` | `gradle.properties` 没有支持的版本 key | 添加 `version=1.0.0-SNAPSHOT` |
| `Unknown module` | changeset key 不匹配检测到的 project name | 使用 `include(...)` 的最后一段，例如 `:tools:cli` 对应 `cli` |
| `publish` 命令渲染 Maven deploy | `preflight` 和 `publish` 是 Maven 专用辅助命令 | Gradle artifact 发布使用 `gradle-publish` |

## 9. 相关文档

| 需求 | 文档 |
| --- | --- |
| 第一次配置 | [快速开始](./getting-started.md) |
| Maven 仓库流程 | [Maven 使用指南](./maven-guide.md) |
| 命令细节 | [CLI 命令参考](./cli-reference.md) |
| 可复制命令序列 | [命令实战手册](./command-cookbook.md) |
| 生成的 release manifest | [Release Plan Manifest](./release-plan-manifest.md) |
| CI release PR/MR 自动化 | [GitHub Actions 使用指南](./github-actions-guide.md) 和 [GitLab CI/CD 使用指南](./gitlab-ci-guide.md) |
