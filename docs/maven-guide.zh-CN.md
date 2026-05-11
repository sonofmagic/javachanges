---
description: 在 Maven 单模块仓库和多模块构建中使用 javachanges。
---

# Maven 使用指南


## 1. Maven 支持范围

`javachanges` 可以通过 Maven plugin 或正式发布版 CLI jar 为 Maven 仓库做发布规划。

Maven 路径支持：

- 通过根 `pom.xml` 识别仓库根目录
- 从根 `<revision>` 属性读取当前版本
- 在 `plan --apply true` 时写回版本
- 从 Maven `<modules>` 检测 package
- 用根 `artifactId` 支持单模块仓库
- changeset、status、changelog、release-plan manifest、GitHub release PR、GitLab release MR、preflight 检查和 Maven publish 辅助命令

Maven 仓库的日常使用建议优先走 Maven plugin，因为命令更短，并且默认把 `--directory` 设成当前 Maven 项目的 `${project.basedir}`。

## 2. Maven 仓库要求

Maven 仓库应该把根版本放在 `revision` 属性里：

```xml
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.example</groupId>
  <artifactId>payments</artifactId>
  <version>${revision}</version>

  <properties>
    <revision>1.4.0-SNAPSHOT</revision>
  </properties>
</project>
```

多模块 Maven 仓库应在根 `pom.xml` 中声明 modules：

```xml
<modules>
  <module>modules/api</module>
  <module>modules/core</module>
</modules>
```

每个 module 应有自己的 `artifactId`。这个 `artifactId` 就是 changeset 文件里使用的 package key。

## 3. 安装并运行 Maven plugin

在目标仓库 `pom.xml` 中添加 plugin：

```xml
<plugin>
  <groupId>io.github.sonofmagic</groupId>
  <artifactId>javachanges</artifactId>
  <version>__JAVACHANGES_LATEST_RELEASE_VERSION__</version>
</plugin>
```

然后在 Maven 仓库内执行最短 goal：

```bash
mvn javachanges:setup
mvn javachanges:setup -Djavachanges.directory=/path/to/gradle-repo -Djavachanges.applyGradleTasks=true
mvn javachanges:status
mvn javachanges:next
mvn javachanges:add -Djavachanges.summary="add release notes command" -Djavachanges.release=minor
mvn javachanges:plan
mvn javachanges:plan -Djavachanges.apply=true
mvn javachanges:validate
mvn javachanges:init-gradle-tasks -Djavachanges.directory=/path/to/gradle-repo -Djavachanges.apply=true
mvn javachanges:init-env
mvn javachanges:auth-help -Djavachanges.platform=github
mvn javachanges:render-vars -Djavachanges.envFile=env/release.env.local -Djavachanges.platform=github
mvn javachanges:doctor-local -Djavachanges.envFile=env/release.env.local
mvn javachanges:doctor-platform -Djavachanges.envFile=env/release.env.local -Djavachanges.platform=github
mvn javachanges:audit-vars -Djavachanges.envFile=env/release.env.local -Djavachanges.platform=github
mvn javachanges:write-settings -Djavachanges.settingsMode=release
mvn javachanges:ensure-gpg-public-key
mvn javachanges:init-github-actions
mvn javachanges:github-release-plan -Djavachanges.githubRepo=owner/repo -Djavachanges.writePlanFiles=false
mvn javachanges:github-tag-from-plan -Djavachanges.fresh=true
mvn javachanges:github-release-publish-state -Djavachanges.fresh=true
mvn javachanges:github-release-from-plan -Djavachanges.fresh=true
mvn javachanges:init-gitlab-ci
mvn javachanges:gitlab-release-plan -Djavachanges.projectId=12345 -Djavachanges.writePlanFiles=false
mvn javachanges:gitlab-tag-from-plan -Djavachanges.fresh=true -Djavachanges.fallbackFromReleaseCommit=true
mvn javachanges:gitlab-release -Djavachanges.tag=v1.2.3
mvn javachanges:release-version-from-tag -Djavachanges.tag=core/v1.2.3
mvn javachanges:release-module-from-tag -Djavachanges.tag=core/v1.2.3
mvn javachanges:assert-module -Djavachanges.module=core
mvn javachanges:assert-snapshot
mvn javachanges:assert-release-tag -Djavachanges.tag=v1.2.3
mvn javachanges:doctor-publish -Djavachanges.tag=v1.2.3
mvn javachanges:gradle-publish -Djavachanges.directory=/path/to/gradle-repo -Djavachanges.tag=v1.2.3
mvn javachanges:manifest-field -Djavachanges.field=releaseVersion -Djavachanges.fresh=true
```

`javachanges:write-settings` 默认写入 `${project.basedir}/.m2/settings.xml`。可以用 `-Djavachanges.output=...` 指定其它路径，用 `-Djavachanges.settingsMode=all|release|snapshot` 控制写入哪些 server。

`javachanges:init-env` 会基于示例模板写入本地 release env 文件。可以用 `-Djavachanges.target=...` 指定目标文件，用 `-Djavachanges.template=...` 指定其它模板，用 `-Djavachanges.force=true` 替换已有文件。`javachanges:auth-help` 可配合 `-Djavachanges.platform=github|gitlab|all` 输出需要配置的认证变量。

env 审阅相关 goal 使用 Maven 风格属性名对应常用 CLI 选项：`-Djavachanges.envFile=...`、`-Djavachanges.platform=github|gitlab|all`、`-Djavachanges.githubRepo=owner/repo` 和 `-Djavachanges.gitlabRepo=group/project`。`javachanges:sync-vars` 默认只是 dry run；确认要更新远端平台变量时再添加 `-Djavachanges.execute=true`。

`javachanges:ensure-gpg-public-key` 会把当前签名公钥发布到支持的 keyserver，并等待它可以被拉取。默认值不适合你的发布环境时，可以用 `-Djavachanges.primaryKeyserver=...`、`-Djavachanges.secondaryKeyserver=...`、`-Djavachanges.attempts=...` 和 `-Djavachanges.retryDelaySeconds=...` 调整。

`javachanges:init-github-actions` 默认写入 `.github/workflows/javachanges-release.yml`，`javachanges:init-gitlab-ci` 默认写入 `.gitlab-ci.yml`。可以用 `-Djavachanges.force=true` 替换已有生成文件，用 `-Djavachanges.buildTool=maven|gradle|auto` 选择模板，用 `-Djavachanges.javachangesVersion=...` 固定生成的 CI 版本。

`javachanges:init-gradle-tasks` 会为 Gradle 仓库写入 `gradle/javachanges.gradle`。用 `-Djavachanges.apply=true` 可以把该脚本追加到根 `build.gradle` 或 `build.gradle.kts`；`javachanges:setup -Djavachanges.applyGradleTasks=true` 会在首次设置中完成同样的接入。

GitHub 发布自动化 goal 会直接映射到对应 CLI 命令。只有在 CI 中或确认要调用 `gh` 时才添加 `-Djavachanges.execute=true`；不传时，release-plan、tag 和 release goal 都保持 dry run。release-plan pull request 默认不会提交 `.changesets/release-plan.*` 文件；只有兼容旧的 manifest 自动化时才需要传 `-Djavachanges.writePlanFiles=true`。

GitLab 发布自动化 goal 也保持相同的 dry run 默认值。只有命令需要调用 GitLab API 或推送 tag 时才添加 `-Djavachanges.execute=true`。`gitlab-tag-from-plan` 还支持 `-Djavachanges.fallbackFromReleaseCommit=true`，用于默认分支从已合并的 `chore(release): release vX.Y.Z` commit 恢复 tag。

当你从 Maven runner 项目调用 Maven plugin、但实际目标是一个 Gradle 仓库时，可以用 `javachanges:gradle-publish` 并通过 `-Djavachanges.directory=...` 指向 Gradle 仓库。没有 Maven 项目的 Gradle 仓库仍建议按 Gradle guide 使用正式发布版 CLI jar。

对还没有专门 Maven goal 的命令，可以使用通用 `run` goal：

```bash
mvn javachanges:run -Djavachanges.args="release-notes --tag v1.2.3"
```

## 4. 不修改 `pom.xml` 时使用正式发布版 CLI

如果暂时不能添加 plugin，可以先从 Maven Central 下载 CLI jar：

```bash
mvn -q dependency:copy \
  -Dartifact=io.github.sonofmagic:javachanges:__JAVACHANGES_LATEST_RELEASE_VERSION__ \
  -DoutputDirectory=.javachanges
```

设置一个复用变量：

```bash
export JAVACHANGES="java -jar .javachanges/javachanges-__JAVACHANGES_LATEST_RELEASE_VERSION__.jar"
```

对 Maven 仓库执行：

```bash
$JAVACHANGES status --directory .
$JAVACHANGES add --directory . --summary "add release notes command" --release minor
$JAVACHANGES plan --directory . --apply true
```

## 5. 单模块 Maven 工作流

示例仓库：

```text
my-library/
├── .changesets/
├── CHANGELOG.md
└── pom.xml
```

`pom.xml`：

```xml
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.example</groupId>
  <artifactId>my-library</artifactId>
  <version>${revision}</version>
  <properties>
    <revision>0.8.0-SNAPSHOT</revision>
  </properties>
</project>
```

创建 patch changeset：

```bash
mvn javachanges:add \
  -Djavachanges.summary="fix generated release notes" \
  -Djavachanges.release=patch
```

CI 或脚本中可以添加 `-Djavachanges.noInteractive=true`，这样缺少 summary 或 release 输入时会直接失败，而不是进入交互提示。如果脚本需要读取已创建 changeset 路径或下一步命令，可以再添加 `-Djavachanges.format=json`。

生成的 changeset 会使用根 `artifactId`：

```md
---
"my-library": patch
---

fix generated release notes
```

查看并应用：

```bash
mvn javachanges:status
mvn javachanges:plan
mvn javachanges:plan -Djavachanges.apply=true
```

应用后：

- `pom.xml` 中的 `revision` 属性推进到下一个 snapshot 版本
- `CHANGELOG.md` 新增 release section
- 写入 `.changesets/release-plan.json`
- 写入 `.changesets/release-plan.md`
- 删除已消费的 `.changesets/*.md`

## 6. 多模块 Maven 工作流

示例根 `pom.xml`：

```xml
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.example</groupId>
  <artifactId>payments-parent</artifactId>
  <version>${revision}</version>
  <packaging>pom</packaging>

  <modules>
    <module>modules/api</module>
    <module>modules/core</module>
    <module>tools/cli</module>
  </modules>

  <properties>
    <revision>1.2.0-SNAPSHOT</revision>
  </properties>
</project>
```

检测到的 package key 来自 module 的 `artifactId`：

| Module path | 示例 artifactId | Changeset package key |
| --- | --- | --- |
| `modules/api` | `api` | `api` |
| `modules/core` | `core` | `core` |
| `tools/cli` | `payments-cli` | `payments-cli` |

创建一个影响两个 module 的 changeset：

```bash
mvn javachanges:add \
  -Djavachanges.summary="add payment retry metadata" \
  -Djavachanges.release=minor \
  -Djavachanges.modules=api,core
```

手写格式：

```md
---
"api": minor
"core": minor
---

add payment retry metadata
```

如果变更影响所有检测到的 Maven package，可以使用 `-Djavachanges.modules=all`：

```bash
mvn javachanges:add \
  -Djavachanges.summary="standardize publication metadata" \
  -Djavachanges.release=patch \
  -Djavachanges.modules=all
```

如果下游 job 只需要发布或测试一个 Maven module，可以让 `javachanges` 输出 Maven selector 参数：

```bash
mvn javachanges:module-selector-args -Djavachanges.module=core
```

对 Maven 仓库，它会输出类似：

```text
-pl :core -am
```

## 7. CI release-plan 自动化

GitHub Actions 和 GitLab CI 可以使用和本地一致的 planning 命令。

GitHub：

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

Maven 仓库的 release-plan 自动化会 stage `pom.xml`、`CHANGELOG.md` 和 `.changesets/`。

从已应用的 plan 创建 tag：

```bash
mvn javachanges:run -Djavachanges.args="github-tag-from-plan --execute true"
mvn javachanges:run -Djavachanges.args="gitlab-tag-from-plan --execute true"
```

## 8. 发布 Maven artifacts

启用真实发布前，先使用 `preflight`：

```bash
mvn javachanges:doctor-publish -Djavachanges.tag=v1.2.3
mvn javachanges:preflight -Djavachanges.tag=v1.2.3
```

发布输入准备好后，再执行 publish helper：

```bash
mvn javachanges:publish -Djavachanges.tag=v1.2.3 -Djavachanges.execute=true
```

这个 helper 会渲染 Maven deploy 命令，并可以从环境变量写出 Maven `settings.xml`。完整 Central 发布配置见 [发布到 Maven Central](./publish-to-maven-central.md)。

## 9. 常见错误

| 现象 | 原因 | 修复方式 |
| --- | --- | --- |
| `Cannot find repository root` | 找不到根 `pom.xml` | 在 Maven 仓库内执行，或显式传入 `--directory` |
| `Cannot find version or revision` | 根 `pom.xml` 没有定义 `<revision>` | 在根 `<properties>` 下添加 `<revision>1.0.0-SNAPSHOT</revision>` |
| `Unknown module` | changeset key 不匹配检测到的 module `artifactId` | 使用 module 的 `artifactId`，不要在两者不一致时使用文件夹名 |
| 版本写到了错误文件 | 命令指向了错误的仓库根目录 | 检查 plugin basedir，或用 CLI 显式传入 `--directory` |

## 10. 相关文档

| 需求 | 文档 |
| --- | --- |
| 第一次配置 | [快速开始](./getting-started.md) |
| Gradle 仓库流程 | [Gradle 使用指南](./gradle-guide.md) |
| 命令细节 | [CLI 命令参考](./cli-reference.md) |
| 可复制命令序列 | [命令实战手册](./command-cookbook.md) |
| 生成的 release manifest | [Release Plan Manifest](./release-plan-manifest.md) |
| Maven Central 发布 | [发布到 Maven Central](./publish-to-maven-central.md) |
| CI release PR/MR 自动化 | [GitHub Actions 使用指南](./github-actions-guide.md) 和 [GitLab CI/CD 使用指南](./gitlab-ci-guide.md) |
