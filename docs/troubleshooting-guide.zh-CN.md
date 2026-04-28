---
description: 排查 javachanges 在本地开发、CI、Maven、Gradle 和 Maven Central 发布中的常见故障。
---

# javachanges 故障排查指南


## 1. 概述

这篇文档集中整理了 `javachanges` 在本地开发、GitHub Actions、GitLab CI/CD、Maven 仓库、Gradle 仓库和 Maven 发布里最常见的问题。

适合这些场景：

- `status` 或 `plan` 输出和预期不一致
- CI 提示缺少凭据或变量
- 工作流看起来没问题，但 Maven 发布仍然失败
- 复制 examples 里的 workflow 之后，目标仓库还是跑不起来

## 2. 本地仓库和 changeset 问题

### 2.1 `No pending changesets`

| 现象 | 原因 | 修复方式 |
| --- | --- | --- |
| `status` 显示 `Pending changesets: 0` | `.changesets/*.md` 不存在、已经被消费，或者文件格式不合法 | 用 `add` 新建一个 changeset，然后重新执行 `status` |

检查命令：

```bash
find .changesets -maxdepth 1 -name '*.md'
mvn -q -DskipTests compile exec:java -Dexec.args="status --directory /path/to/repo"
```

### 2.2 `add` 成功了，但 `plan` 影响的模块不对

| 现象 | 原因 | 修复方式 |
| --- | --- | --- |
| release plan 里列出的包不对，或者漏了包 | changeset 里写错了 Maven artifactId | 把 frontmatter key 改成 `pom.xml` 里的真实 artifactId |

正确示例：

```md
---
"core": minor
"api": patch
---

Improve release planning output.
```

### 2.3 `plan` 没有写出 `release-plan.json`

| 现象 | 原因 | 修复方式 |
| --- | --- | --- |
| 执行完 `plan` 之后没有 `.changesets/release-plan.json` | 你运行的是预览模式，没有真正应用 plan | 用 `--apply true` 再执行一次 |

正确命令：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="plan --directory /path/to/repo --apply true"
```

### 2.4 `manifest-field` 读取失败

| 现象 | 原因 | 修复方式 |
| --- | --- | --- |
| `manifest-field` 读不到字段 | 没有写出兼容 manifest | 使用 `manifest-field --fresh true`，或在开启生成文件的情况下应用 plan |

### 2.5 Gradle 仓库根目录无法识别

| 现象 | 原因 | 修复方式 |
| --- | --- | --- |
| Gradle 仓库里出现 `Cannot find repository root` | 缺少 `gradle.properties`，或根目录没有 Gradle settings/build 文件 | 添加 `gradle.properties`，并添加 `settings.gradle(.kts)` 或 `build.gradle(.kts)` |

最小 Gradle 文件：

```text
gradle.properties
settings.gradle.kts
build.gradle.kts
```

`gradle.properties` 必须包含一个支持的版本 key：

```properties
version=1.0.0-SNAPSHOT
```

### 2.6 Gradle changeset 的 package key 写错

| 现象 | 原因 | 修复方式 |
| --- | --- | --- |
| Gradle 多项目构建里出现 `Unknown module` | changeset key 不匹配 `include(...)` project path 的最后一段 | `:api` 用 `api`，`:core` 用 `core`，`:tools:cli` 用 `cli` |

检查 `settings.gradle(.kts)` 并对齐 frontmatter：

```md
---
"api": minor
"core": patch
---

Improve Gradle release planning.
```

## 3. Java 和 Maven 环境问题

### 3.1 不同机器构建结果不一致

| 现象 | 原因 | 修复方式 |
| --- | --- | --- |
| 本地和 CI 行为不一样 | Java 版本漂移 | 当前仓库统一用 Java 8 |

确认命令：

```bash
java -version
mvn -v
```

推荐方向：

- Java 8
- Maven 3.8+

### 3.2 `exec:java` 无法运行

| 现象 | 原因 | 修复方式 |
| --- | --- | --- |
| Maven 无法执行 `exec:java` | 依赖还没准备好，或者项目本身没编过 | 先执行一次 `mvn -q test` |

### 3.3 `doctor-local` 提示 `./mvnw` 缺失

| 现象 | 原因 | 修复方式 |
| --- | --- | --- |
| 运行时检查里出现 `./mvnw MISSING` | 目标仓库本身没有提交 Maven wrapper | 如果 `Maven command` 已经解析成 `mvn (system)`，这属于正常回退 |

重点看两件事：

- 如果 `Maven command` 是 `mvn (system)`，说明 fallback 已经生效
- 如果 wrapper 和系统 Maven 都没有，就需要安装 Maven 或补上 wrapper

### 3.4 示例 workflow 无法下载 `javachanges`

| 现象 | 原因 | 修复方式 |
| --- | --- | --- |
| `io.github.sonofmagic:javachanges` 下载失败 | `JAVACHANGES_VERSION` 还是占位符 | 在启用 CI 前，把它替换成真实已发布版本 |

示例模板故意写成：

```yaml
JAVACHANGES_VERSION: "REPLACE_WITH_PUBLISHED_VERSION"
```

这只是提醒你必须修改，不是默认可运行值。

### 3.5 包装 `javachanges` 时 Maven 报 `Unknown lifecycle phase`

| 现象 | 原因 | 修复方式 |
| --- | --- | --- |
| Maven 报 `Unknown lifecycle phase "version --directory ..."` 或类似错误 | `-Dexec.args=` 和真正的 `javachanges` 命令被 Make、shell 或 CI 拼接逻辑拆开了 | 保证完整的 `-Dexec.args="..."` 出现在最终命令行里，或者把整条 Maven 调用封装成一个 Make function / 仓库脚本 |

推荐方向：

- 正确：`./mvnw -q -DskipTests compile exec:java -Dexec.args="version --directory $PWD"`
- 错误：先定义一个以裸 `-Dexec.args=` 结尾的前缀，再在后面追加 `"version --directory ..."`

### 3.6 Gradle 用户误用了 Maven plugin

| 现象 | 原因 | 修复方式 |
| --- | --- | --- |
| Gradle 仓库示例里 `mvn javachanges:...` 不可用 | Maven plugin goal 语法只适用于 Maven | 下载 CLI jar，并执行 `java -jar .javachanges/javachanges-<version>.jar ...` |

Gradle 命令形态：

```bash
java -jar .javachanges/javachanges-__JAVACHANGES_LATEST_RELEASE_VERSION__.jar status --directory .
```

## 4. GitHub Actions 问题

### 4.1 `sync-vars` 或 `audit-vars` 提示缺变量

| 现象 | 原因 | 修复方式 |
| --- | --- | --- |
| 仓库 variables / secrets 不完整 | `env/release.env.local` 里还是占位值，或者仓库名写错了 | 先替换占位值，再重新执行 `render-vars`、`sync-vars` 和 `audit-vars` |

建议顺序：

1. 执行 `init-env`
2. 填好 env 文件
3. 执行 `render-vars --platform github`
4. 执行 `sync-vars --platform github --execute true`
5. 执行 `audit-vars --platform github`

### 4.2 Release PR 工作流总带上无关文件

| 现象 | 原因 | 修复方式 |
| --- | --- | --- |
| release 分支里混入额外噪音文件 | workflow 提交范围过大，或者顺手跑了别的生成器 | 把提交范围限制在 `pom.xml` 或 `gradle.properties`、`CHANGELOG.md` 和 `.changesets` |

### 4.3 开了 cache，Maven 还是在下载依赖

| 现象 | 原因 | 修复方式 |
| --- | --- | --- |
| workflow 里仍然能看到下载日志 | 第一次预热缓存，或者依赖图发生了变化 | 这是正常现象，等一次成功运行把缓存热起来 |

## 5. GitLab CI/CD 问题

### 5.1 一直没有创建 release MR

| 现象 | 原因 | 修复方式 |
| --- | --- | --- |
| `gitlab-release-plan` 被跳过 | 没有待发布 changeset，或者 GitLab 鉴权变量缺失 | 先新增 changeset，并确认 `GITLAB_RELEASE_BOT_USERNAME` / `GITLAB_RELEASE_BOT_TOKEN` 已配置 |

### 5.2 Release tag job 什么都没做

| 现象 | 原因 | 修复方式 |
| --- | --- | --- |
| `gitlab-tag-from-plan` 跳过打 tag | 发布状态没变化，或者 `CI_COMMIT_BEFORE_SHA` 不可用 | 检查默认分支 pipeline，确认版本文件和 changelog 已变化 |

### 5.3 Release MR push 报 `stale info`

| 现象 | 原因 | 修复方式 |
| --- | --- | --- |
| `release_plan_mr` 失败，并出现 `failed to push some refs` / `stale info` | `gitlab-release-plan` 刚解析完远端 `changeset-release/<default-branch>` 的 SHA，就有别的写入方更新了同一个分支 | 直接重跑 pipeline；如果这个分支应当只由 javachanges 持有，需要移除其他写入方 |

### 5.4 Hygiene / secret scanning 扫到了扫描器自己的规则

| 现象 | 原因 | 修复方式 |
| --- | --- | --- |
| hygiene job 命中了 `.gitlab-ci.yml`、`Makefile` 或规则文件，但仓库里并没有新增真实凭据 | 扫描器在自己的配置里匹配到了 token 前缀或私钥标记等规则字面量 | 把 secret 模式集中放到一个独立规则文件，扫描时排除该文件和扫描器自有配置文件，并且只把 allowlist 注释用于少量已审阅例外 |

## 6. 发布和凭据问题

### 6.1 `preflight` 或 `publish` 提示缺少凭据

| 现象 | 原因 | 修复方式 |
| --- | --- | --- |
| 发布命令在 deploy 前就失败 | 发布仓库 URL、server id 或认证信息没配置齐 | 补全 env 文件或 CI secrets，然后重新执行 `doctor-local` 或 `doctor-platform` |

最小共享变量如下：

| 名称 |
| --- |
| `MAVEN_RELEASE_REPOSITORY_URL` |
| `MAVEN_SNAPSHOT_REPOSITORY_URL` |
| `MAVEN_RELEASE_REPOSITORY_ID` |
| `MAVEN_SNAPSHOT_REPOSITORY_ID` |
| `MAVEN_REPOSITORY_USERNAME` |
| `MAVEN_REPOSITORY_PASSWORD` |

### 6.1.1 Gradle 发布不应该使用 `publish`

| 现象 | 原因 | 修复方式 |
| --- | --- | --- |
| Gradle 仓库运行 `publish` 后看到 Maven deploy 输出 | `preflight` 和 `publish` 是 Maven 专用辅助命令 | 用 `manifest-field` 读取 release version，再执行 `./gradlew publish` |

Gradle release handoff：

```bash
RELEASE_VERSION="$(java -jar .javachanges/javachanges-__JAVACHANGES_LATEST_RELEASE_VERSION__.jar manifest-field --directory . --field releaseVersion --fresh true)"
./gradlew publish -Pversion="$RELEASE_VERSION"
```

### 6.2 工作区不干净导致发布失败

| 现象 | 原因 | 修复方式 |
| --- | --- | --- |
| `preflight` 或 `publish` 拒绝继续 | 仓库里还有未提交改动或生成文件 | 先提交或清理工作区，再执行发布 |

只有在你明确接受风险时，才应该使用 `--allow-dirty true`。

### 6.3 发布到 Maven Central 时缺少附件或签名

| 现象 | 原因 | 修复方式 |
| --- | --- | --- |
| 缺少 `sources.jar`、`javadoc.jar` 或签名文件 | `central-publish` profile 没开，或者 GPG 环境没配好 | 回到 Maven Central 指南，把校验链路重新跑一遍 |

校验命令：

```bash
mvn -Pcentral-publish -Dgpg.skip=true verify
```

### 6.4 plain snapshot 模式下仓库里仍然出现带时间戳的文件名

| 现象 | 原因 | 修复方式 |
| --- | --- | --- |
| `publish --snapshot --snapshot-version-mode plain` 已经保持了项目版本号为 `1.2.3-SNAPSHOT`，但仓库里看到的产物文件名仍然像 `1.2.3-20260420.154500-1.jar` | Maven snapshot 仓库会在服务端对 snapshot 产物文件名做标准展开 | 这是预期行为；要确认实际项目版本，请看 `preflight` / `publish` 的输出或 JSON 字段，而不是只看仓库里的最终文件名 |

需要区分两件事：

- plain 模式表示 `javachanges` 不再把 Maven 项目版本改写成 `1.2.3-<stamp>-SNAPSHOT`
- 仓库端把 snapshot 文件名展开成带时间戳格式，仍然是 Maven snapshot 的标准行为

### 6.5 Maven Central 因为无法发现公钥指纹而拒绝签名

| 现象 | 原因 | 修复方式 |
| --- | --- | --- |
| Central 提示无法根据 key fingerprint 找到公钥 | CI 里已经导入了签名私钥，但对应公钥还没有出现在受支持的 keyserver 上 | 在 `deploy` 前先发布公钥，并等待可见性确认完成 |

推荐在 CI 里加入：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="ensure-gpg-public-key --directory $PWD"
```

当前命令会向这些服务发布并验证公钥可见性：

- `hkps://keyserver.ubuntu.com`
- `hkps://keys.openpgp.org`

### 6.6 手动重试正式发布时用了错误的 commit 或 release version

| 现象 | 原因 | 修复方式 |
| --- | --- | --- |
| 手动重试后打出了错误的 tag，或者 `RELEASE_VERSION` 为空 | 重试时没有对准 release PR 的 merge commit，或者 workflow 从错误的代码树里读取了辅助信息 | 通过 `workflow_dispatch` 重跑 `Publish Release`，并传入原始 merge commit SHA 作为 `release_commit_sha` |

当前仓库里的 `publish-release.yml` 已经支持通过 `release_commit_sha` 做手动重试。

## 7. Release plan manifest 相关疑惑

### 7.1 `release-plan.json` 里出现 `type: other`

| 现象 | 原因 | 修复方式 |
| --- | --- | --- |
| manifest 里出现 `"type": "other"` | 当前实现为了兼容旧格式保留了这个字段 | 在新的集成里忽略它，真正有意义的是 `release`、`summary` 和 `modules` |

如果你使用的是官方 package-map changeset 格式，`type` 不是发布级别。真正表示发布升级类型的是 `release`。

## 8. 快速检查表

| 检查项 | 命令或文件 |
| --- | --- |
| Java 版本 | `java -version` |
| Maven 版本 | `mvn -v` |
| Gradle 版本 | `./gradlew -v` |
| 是否存在待发布 changeset | `status` |
| 是否已有 applied manifest | `.changesets/release-plan.json` |
| CI 变量是否正确 | `render-vars` / `audit-vars` |
| 发布输入是否完整 | `doctor-local` / `preflight` |
| 示例工作流占位符是否被替换 | `JAVACHANGES_VERSION` |

## 9. 相关阅读

| 需求 | 文档 |
| --- | --- |
| 本地开发环境 | [Development Guide](./development-guide.md) |
| 示例仓库讲解 | [Examples Guide](./examples-guide.md) |
| Gradle 配置 | [Gradle 使用指南](./gradle-guide.md) |
| CLI 命令列表 | [CLI Reference](./cli-reference.md) |
| Manifest 字段说明 | [Release Plan Manifest](./release-plan-manifest.md) |
| GitHub Actions 自动化 | [GitHub Actions Usage Guide](./github-actions-guide.md) |
| GitLab CI/CD 自动化 | [GitLab CI/CD Usage Guide](./gitlab-ci-guide.md) |
