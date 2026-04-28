# javachanges 命令实战手册


## 1. 概述

这篇文档是 CLI Reference 的实战补充版。

如果你不想在多份文档之间自己拼命令，而是希望直接拿到一套可以复制改造的命令序列，就用这篇。

下面这些 recipe 默认假设：

- 你是通过 Maven 从源码运行 `javachanges`
- 目标仓库有 Maven `pom.xml` 或 Gradle `gradle.properties`
- Maven 仓库用根 `<revision>` 维护版本，Gradle 仓库用 `gradle.properties` 中的 `version` 或 `revision`

## 2. 共享准备

先统一设置一个仓库路径变量：

```bash
export REPO=/path/to/your/repo
```

后面的命令都直接复用 `"$REPO"`。

如果你要直接测试仓库里自带的示例：

```bash
export REPO=examples/basic-monorepo
```

## 3. Recipe：单模块仓库

适用于根工程只发布一个 Maven artifact 的仓库。

### 3.1 添加一个 patch changeset

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="add --directory $REPO --summary 'fix release note rendering' --release patch"
```

### 3.2 查看待发布状态

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="status --directory $REPO"
mvn -q -DskipTests compile exec:java -Dexec.args="plan --directory $REPO"
```

### 3.3 应用 release plan

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="plan --directory $REPO --apply true"
```

### 3.4 读取生成后的发布版本

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="manifest-field --directory $REPO --field releaseVersion"
```

执行 `plan --apply true` 之后，通常会看到这些结果：

- `pom.xml` 的版本推进到下一个 snapshot
- `CHANGELOG.md` 插入新的发布记录
- 写出 `.changesets/release-plan.json`
- 写出 `.changesets/release-plan.md`

## 4. Recipe：Maven monorepo

适用于一个仓库里有多个 Maven 模块，并且你希望统一审阅一份 release plan 的场景。

### 4.1 添加一条影响多个包的 changeset

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="add --directory $REPO --summary 'add release notes workflow' --release minor --modules core,api"
```

### 4.2 检查受影响包

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="status --directory $REPO"
```

重点看这些输出：

- `Affected packages`
- `Release type`
- 当前 pending changeset 的文件名和 summary

### 4.3 应用 plan 并检查 manifest

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="plan --directory $REPO --apply true"
mvn -q -DskipTests compile exec:java -Dexec.args="manifest-field --directory $REPO --field releaseLevel"
```

### 4.4 后续只针对单模块发布

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="module-selector-args --directory $REPO --module core"
```

这个命令适合下游工作流需要构造 Maven `-pl` 参数时使用。

## 5. Recipe：先准备 CI 变量

适用于你要先把发布环境变量规范化，再去开 GitHub Actions 或 GitLab CI/CD。

### 5.1 初始化本地 env 文件

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="init-env --target env/release.env.local"
```

### 5.2 预览 GitHub 需要的变量和 secrets

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="render-vars --directory $REPO --env-file env/release.env.local --platform github"
```

### 5.3 预览 GitLab 需要的变量

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="render-vars --directory $REPO --env-file env/release.env.local --platform gitlab"
```

### 5.4 校验本地发布输入

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="doctor-local --directory $REPO --env-file env/release.env.local"
```

等真实仓库变量同步完成后，再执行 `doctor-platform`。

### 5.5 输出给 CI 使用的机器可读诊断信息

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="render-vars --directory $REPO --env-file env/release.env.local --platform github --format json"
mvn -q -DskipTests compile exec:java -Dexec.args="doctor-local --directory $REPO --env-file env/release.env.local --format json"
mvn -q -DskipTests compile exec:java -Dexec.args="audit-vars --directory $REPO --env-file env/release.env.local --platform github --github-repo owner/repo --format json"
```

当 shell 步骤需要结构化诊断信息，而不是解析对齐表格文本时，用这两个命令更合适。

## 6. Recipe：Gradle release planning

适用于目标仓库是 Gradle 单项目或多项目构建的场景。

### 6.1 最小 Gradle 设置

```properties
# gradle.properties
version=1.1.0-SNAPSHOT
```

```kotlin
// settings.gradle.kts
rootProject.name = "sample-gradle-library"
include(":core", ":api")
```

### 6.2 使用正式版 CLI jar

```bash
mvn -q dependency:copy -Dartifact=io.github.sonofmagic:javachanges:__JAVACHANGES_LATEST_RELEASE_VERSION__ -DoutputDirectory=.javachanges
export JAVACHANGES="java -jar .javachanges/javachanges-__JAVACHANGES_LATEST_RELEASE_VERSION__.jar"
```

### 6.3 添加、查看并应用 Gradle changeset

```bash
$JAVACHANGES add --directory $REPO --summary "add retry metadata" --release minor --modules core,api
$JAVACHANGES status --directory $REPO
$JAVACHANGES plan --directory $REPO --apply true
```

执行 `plan --apply true` 后通常会看到：

- `gradle.properties` 版本推进到下一个 snapshot
- `CHANGELOG.md` 插入新的发布记录
- 写出 `.changesets/release-plan.json`
- 写出 `.changesets/release-plan.md`

### 6.4 交给 Gradle 发布

```bash
$JAVACHANGES gradle-publish --directory $REPO --tag v1.2.3
$JAVACHANGES gradle-publish --directory $REPO --tag v1.2.3 --execute true
```

完整 Gradle 流程和限制见 [Gradle 使用指南](./gradle-guide.md)。

## 7. Recipe：GitHub Actions release PR 流程

适用于 `main` 持续累积 `.changesets/*.md`，然后由 workflow 自动创建一份可审阅 release PR 的场景。

### 7.1 本地先 dry-run 一次

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="status --directory $REPO"
mvn -q -DskipTests compile exec:java -Dexec.args="plan --directory $REPO --apply true"
```

### 7.2 GitHub Actions 最常消费的字段

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="manifest-field --directory $REPO --field releaseVersion --fresh true"
# releaseLevel 需要写出兼容 manifest 后读取。
mvn -q -DskipTests compile exec:java -Dexec.args="manifest-field --directory $REPO --field releaseLevel"
```

### 7.3 workflow 通常只提交这些文件

- `pom.xml`
- 或 `gradle.properties`
- `CHANGELOG.md`
- 仅在开启兼容输出时提交生成的 release-plan 文件

完整 workflow 示例请看 [GitHub Actions Usage Guide](./github-actions-guide.md) 和 [Examples Guide](./examples-guide.md)。

## 8. Recipe：GitLab release MR 和 tag 流程

适用于 GitLab 统一管理 release branch、merge request 和最终 tag 的场景。

### 8.1 创建或更新 release MR

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="gitlab-release-plan --directory $REPO --project-id 12345 --write-plan-files false --execute true"
```

### 8.2 根据 applied plan 创建最终 tag

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="gitlab-tag-from-plan --directory $REPO --fresh true --before-sha <before-sha> --current-sha <current-sha> --execute true"
```

CI 里最常见的映射如下：

| 输入 | 常见来源 |
| --- | --- |
| `--project-id` | `CI_PROJECT_ID` |
| `--before-sha` | `CI_COMMIT_BEFORE_SHA` |
| `--current-sha` | `CI_COMMIT_SHA` |

完整 pipeline 结构请看 [GitLab CI/CD Usage Guide](./gitlab-ci-guide.md)。

## 9. Recipe：本地安全预演发布

在真正打开发布执行前，建议先跑这组 dry-run。

这一节是 Maven 专用。Gradle artifact 发布请使用 Gradle `publish` task，并按第 6 节消费 release-plan manifest。

### 9.1 校验 snapshot 发布

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="preflight --directory $REPO --snapshot"
```

### 9.2 校验 tag 发布

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="preflight --directory $REPO --tag v1.2.3"
```

### 9.3 渲染真实 publish 命令，但先不执行

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="publish --directory $REPO --tag v1.2.3"
```

只有在打印出来的命令、生成的 settings 和凭据都确认正确之后，才加上 `--execute true`。

## 10. Recipe：当前仓库发布到 Maven Central

这一节只针对 `javachanges` 仓库自身，不适用于所有下游仓库。

### 10.1 校验 Central 发布前置条件

```bash
mvn -Pcentral-publish -Dgpg.skip=true verify
```

### 10.2 执行正式发布

```bash
mvn -Pcentral-publish clean deploy
```

### 10.3 通过 Central 发布当前仓库的本地 snapshot

```bash
pnpm snapshot:publish:local
```

如果你需要的不是 Sonatype Central 发布，而是通用 Maven 仓库发布，就看上面的 `preflight` / `publish` recipe。

## 11. 快速决策表

| 目标 | 从这里开始 |
| --- | --- |
| 单模块仓库，本地生成 release plan | 第 3 节 |
| monorepo 包级发布规划 | 第 4 节 |
| 先准备 CI 变量 | 第 5 节 |
| Gradle release planning | 第 6 节 |
| GitHub release PR 自动化 | 第 7 节 |
| GitLab release MR / tag 自动化 | 第 8 节 |
| 安全预演发布 | 第 9 节 |
| 当前仓库自身发布到 Central | 第 10 节 |

## 12. 相关阅读

| 需求 | 文档 |
| --- | --- |
| 完整命令目录 | [CLI Reference](./cli-reference.md) |
| Maven 命令流程 | [Maven 使用指南](./maven-guide.md) |
| Gradle 命令流程 | [Gradle 使用指南](./gradle-guide.md) |
| 示例仓库结构 | [Examples Guide](./examples-guide.md) |
| 变量总表 | [Configuration Reference](./configuration-reference.md) |
| applied manifest 字段 | [Release Plan Manifest](./release-plan-manifest.md) |
| 故障排查 | [Troubleshooting Guide](./troubleshooting-guide.md) |
