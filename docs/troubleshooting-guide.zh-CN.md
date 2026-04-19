# javachanges 故障排查指南

[English](/troubleshooting-guide) | [简体中文](/zh-CN/troubleshooting-guide)

## 1. 概述

这篇文档集中整理了 `javachanges` 在本地开发、GitHub Actions、GitLab CI/CD 和 Maven 发布里最常见的问题。

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
| `manifest-field` 读不到字段 | 当前仓库里还没有已经应用过的 release plan | 先执行 `plan --apply true`，再读取字段 |

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

### 3.3 示例 workflow 无法下载 `javachanges`

| 现象 | 原因 | 修复方式 |
| --- | --- | --- |
| `io.github.sonofmagic:javachanges` 下载失败 | `JAVACHANGES_VERSION` 还是占位符 | 在启用 CI 前，把它替换成真实已发布版本 |

示例模板故意写成：

```yaml
JAVACHANGES_VERSION: "REPLACE_WITH_PUBLISHED_VERSION"
```

这只是提醒你必须修改，不是默认可运行值。

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
| release 分支里混入额外噪音文件 | workflow 提交范围过大，或者顺手跑了别的生成器 | 把提交范围限制在 `pom.xml`、`CHANGELOG.md` 和 `.changesets` |

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
| `gitlab-tag-from-plan` 跳过打 tag | `.changesets/release-plan.json` 没变化，或者 `CI_COMMIT_BEFORE_SHA` 不可用 | 检查默认分支 pipeline，确认新的 applied plan 已经被提交 |

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
| CLI 命令列表 | [CLI Reference](./cli-reference.md) |
| Manifest 字段说明 | [Release Plan Manifest](./release-plan-manifest.md) |
| GitHub Actions 自动化 | [GitHub Actions Usage Guide](./github-actions-guide.md) |
| GitLab CI/CD 自动化 | [GitLab CI/CD Usage Guide](./gitlab-ci-guide.md) |
