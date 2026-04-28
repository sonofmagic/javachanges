# javachanges

[English](./README.md) | [简体中文](./README.zh-CN.md)

`javachanges` 是一个轻量的 Java CLI，为 Maven Monorepo 和单模块 Maven 仓库提供类似 Changesets 的发布规划工作流。

文档站点：`https://javachanges.icebreaker.top`

它适合这些仓库：

- 需要用 `.changesets/*.md` 文件记录发布意图
- 希望在版本变更落地前先审阅 release plan
- 需要自动生成 changelog 和 release notes
- 需要 CI 友好的发布检查和发布辅助能力
- 需要可选的 GitHub / GitLab 变量同步和审计能力

## 状态

这个仓库是 `javachanges` 的独立源码仓库。

当前代码重点覆盖：

- changeset 的添加与校验
- release plan 的生成
- 根 `revision` 的推进
- changelog 和 release notes 的生成
- 基于环境变量的 Maven settings 生成
- 发布前检查和发布辅助
- GitHub / GitLab 环境变量审计
- GitHub / GitLab release PR / MR 与 tag 自动化辅助

## 快速开始

环境要求：

- Java 8+
- Maven 3.8+
- 一个 git 仓库
- 一个带根 `pom.xml` 的 Maven 仓库
- 根 `pom.xml` 中要么有 `<modules>`，要么是单模块根 artifact

目标仓库里的推荐用法：先声明 Maven plugin，再直接执行最短 goal：

```xml
<plugin>
  <groupId>io.github.sonofmagic</groupId>
  <artifactId>javachanges</artifactId>
  <version><!-- latest released version --></version>
</plugin>
```

然后在该仓库里执行：

```bash
mvn javachanges:status
mvn javachanges:plan -Djavachanges.apply=true
mvn javachanges:add -Djavachanges.summary="add release notes command" -Djavachanges.release=minor
mvn javachanges:manifest-field -Djavachanges.field=releaseVersion
mvn javachanges:run -Djavachanges.args="release-notes --tag v1.2.3"
```

这个 plugin 会默认把 `--directory` 设成当前 Maven 项目的 `${project.basedir}`，所以如果你就是在目标仓库里执行，通常不需要再手动写 `--directory`。通用的 `run` goal 也仍然保留，方便覆盖还没有拆成独立 goal 的命令。

如果你暂时不能修改目标仓库 `pom.xml`，再使用正式发布版 CLI：

```bash
mvn -q dependency:copy -Dartifact=io.github.sonofmagic:javachanges:<released-version> -DoutputDirectory=.javachanges
java -jar .javachanges/javachanges-<released-version>.jar --help
```

在当前 `main` 分支上，如果你先把 SNAPSHOT 安装到本地，也可以把 `javachanges` 当作 Maven plugin 使用：

```bash
./mvnw -q -DskipTests install
mvn io.github.sonofmagic:javachanges:1.6.0-SNAPSHOT:status
mvn io.github.sonofmagic:javachanges:1.6.0-SNAPSHOT:plan -Djavachanges.apply=true
mvn io.github.sonofmagic:javachanges:1.6.0-SNAPSHOT:add -Djavachanges.summary="add release notes command" -Djavachanges.release=minor
mvn io.github.sonofmagic:javachanges:1.6.0-SNAPSHOT:manifest-field -Djavachanges.field=releaseVersion
```

如果你是在开发 `javachanges` 仓库本身，可以直接用这些本地快捷入口：

```bash
./mvnw -B test
./mvnw -B -Pcoverage -Dmaven.repo.local=.m2/repository test
pnpm snapshot:install
pnpm snapshot:preflight
pnpm snapshot:publish:local
pnpm docs:deploy:local
```

这几个脚本和文档里的阶段语义保持一致：

- `./mvnw -B test` 运行默认测试集，并带上构建前置环境检查
- `./mvnw -B -Pcoverage -Dmaven.repo.local=.m2/repository test` 会额外生成 `target/site/jacoco/` 下的 JaCoCo HTML 覆盖率报告
- `snapshot:install` 把当前 `1.6.0-SNAPSHOT` 安装到本地 Maven 仓库
- `snapshot:preflight` 用 `local.dev.001` 预演一次本地 snapshot 发布检查
- `snapshot:publish:local` 通过 `central-publishing-maven-plugin` 发布一个唯一 snapshot 版本
- `docs:deploy:local` 会重新构建 `website/dist`，再通过 Wrangler 在本地启动预览部署

这两个 snapshot 脚本会把 Maven 本地仓库固定到当前仓库下的 `.m2/repository`，避免依赖可写的全局 `~/.m2`。

snapshot 验证地址：

- snapshot 仓库地址：`https://central.sonatype.com/repository/maven-snapshots/`
- snapshot 元数据地址模式：`https://central.sonatype.com/repository/maven-snapshots/io/github/sonofmagic/javachanges/<resolved-snapshot-version>/maven-metadata.xml`
- snapshot 产物地址模式：`https://central.sonatype.com/repository/maven-snapshots/io/github/sonofmagic/javachanges/<resolved-snapshot-version>/javachanges-<timestamped-version>.jar`

Sonatype 当前对 hosted snapshot 没有可用的目录浏览界面，所以实际验证方式通常是直接打开 `maven-metadata.xml`、打开具体产物 URL，或者在 Maven / Gradle 里实际解析一次依赖。

已发布包地址：

- Maven Central 页面：`https://central.sonatype.com/artifact/io.github.sonofmagic/javachanges`
- CLI jar 地址模式：`https://repo1.maven.org/maven2/io/github/sonofmagic/javachanges/<released-version>/javachanges-<released-version>.jar`

正式版 CLI 对目标仓库的用法：

```bash
java -jar .javachanges/javachanges-<released-version>.jar status --directory /path/to/your/repo
java -jar .javachanges/javachanges-<released-version>.jar add --directory /path/to/your/repo
java -jar .javachanges/javachanges-<released-version>.jar plan --directory /path/to/your/repo
```

如果你要开发这个仓库本身，请看 [Development Guide](docs/development-guide.md)。

## Changeset 格式

`javachanges` 现在默认使用与 Node.js Changesets 相同的核心 Markdown 结构：

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

它的含义是：

- frontmatter 里的每个 key 都是一个 Maven artifactId
- 每个 value 都是这个模块对应的语义化版本升级级别：`patch`、`minor`、`major`
- Markdown 正文就是面向用户的变更说明与补充备注

默认行为：

- `javachanges add` 默认会生成这种官方 package-map 风格
- 如果使用 `--modules all`，会为当前仓库检测到的所有 Maven artifactId 写入对应条目
- 正文第一条非空行会被复用为 `status`、release PR、changelog 和 release notes 里的 summary
- changelog 仍然按聚合后的 release level 分成 `major`、`minor`、`patch`

单模块仓库的最短手写格式：

```md
---
"javachanges": patch
---

Fix Windows path handling in release-notes generation.
```

兼容性说明：

- 旧的 `release` / `modules` / `summary` / `type` frontmatter 格式仍然可以继续读取
- 新建 changeset 推荐统一使用上面的官方 package-map 风格

旧格式示例：

```md
---
release: minor
type: ci
modules: javachanges
summary: automate javachanges self-release publishing via GitHub Actions
---
```

字段说明：

- `"artifactId"`
  新格式必填。
  每个 frontmatter key 都是一个 Maven artifactId，通常建议像官方 Changesets 一样加双引号。
  对于单模块仓库，一般就是根 artifactId。
  对于 monorepo，则为每个受影响模块写一条。
- `patch` / `minor` / `major`
  每个 artifactId 对应的必填值。
  表示这个模块贡献的语义化版本升级级别。
  可选值：`patch`、`minor`、`major`。
  一般规则：
  `patch` 用于兼容性修复、文档、杂项、CI、小改动。
  `minor` 用于向后兼容的新功能。
  `major` 用于破坏性变更。
- body
  推荐填写的 Markdown 正文。
  第一条非空正文会被复用为 summary。
  后续段落或列表可以继续写迁移说明、发布备注或补充背景。
- `type`
  旧格式里的可选元数据字段，仅为兼容历史文件而保留。
  新文件一般不再推荐使用。
- `release`、`modules`、`summary`
  旧版 `javachanges` 使用过的 frontmatter 字段。
  现阶段仍可兼容解析，但不再推荐用于新 changeset。

## 仓库结构

- `src/main/java`: CLI 源码
- `docs/`: 文档
- `examples/basic-monorepo/`: 带 CI 模板和 release-plan 快照的最小 Maven 目标仓库示例
- `website/`: 使用 Workers + Wrangler 部署静态资源的 VitePress 文档站
- Cloudflare 可以直接通过 Workers Builds 连接这个仓库，因此 GitHub 不再需要单独负责 docs 部署的 workflow
- `env/release.env.example`: 通用发布环境变量模板

## 命令

高价值命令：

- `add`
- `status`
- `plan`
- `write-settings`
- `init-env`
- `render-vars`
- `doctor-local`
- `doctor-platform`
- `sync-vars`
- `audit-vars`
- `preflight`
- `publish`
- `release-notes`

GitHub 相关辅助：

- `github-release-plan`
- `github-tag-from-plan`

GitLab 相关辅助：

- `gitlab-release-plan`
- `gitlab-tag-from-plan`

## 文档

- [Overview](docs/index.md)
- [Overview (zh-CN)](docs/index.zh-CN.md)
- [Getting Started](docs/getting-started.md)
- [Getting Started (zh-CN)](docs/getting-started.zh-CN.md)
- [Examples Guide 使用指南](docs/examples-guide.md)
- [Examples Guide 使用指南（zh-CN）](docs/examples-guide.zh-CN.md)
- [命令实战手册](docs/command-cookbook.md)
- [命令实战手册（zh-CN）](docs/command-cookbook.zh-CN.md)
- [配置参考大全](docs/configuration-reference.md)
- [配置参考大全（zh-CN）](docs/configuration-reference.zh-CN.md)
- [CLI 命令参考](docs/cli-reference.md)
- [CLI 命令参考（zh-CN）](docs/cli-reference.zh-CN.md)
- [Output Contracts](docs/output-contracts.md)
- [输出契约说明（zh-CN）](docs/output-contracts.zh-CN.md)
- [Development Guide](docs/development-guide.md)
- [Development Guide (zh-CN)](docs/development-guide.zh-CN.md)
- [Release Plan Manifest](docs/release-plan-manifest.md)
- [Release Plan Manifest 说明（zh-CN）](docs/release-plan-manifest.zh-CN.md)
- [Troubleshooting Guide](docs/troubleshooting-guide.md)
- [故障排查指南（zh-CN）](docs/troubleshooting-guide.zh-CN.md)
- [Cloudflare Workers Builds](docs/cloudflare-workers-builds.md)
- [Cloudflare Workers Builds 配置指南（zh-CN）](docs/cloudflare-workers-builds.zh-CN.md)
- [GitHub Actions Release Flow](docs/github-actions-release.md)
- [GitHub Actions Release Flow (zh-CN)](docs/github-actions-release.zh-CN.md)
- [GitHub Actions Usage Guide](docs/github-actions-guide.md)
- [GitHub Actions Usage Guide (zh-CN)](docs/github-actions-guide.zh-CN.md)
- [GitLab CI/CD Usage Guide](docs/gitlab-ci-guide.md)
- [GitLab CI/CD Usage Guide (zh-CN)](docs/gitlab-ci-guide.zh-CN.md)
- [Publish To Maven Central](docs/publish-to-maven-central.md)
- [Publish To Maven Central (zh-CN)](docs/publish-to-maven-central.zh-CN.md)
- [Use Cases](docs/use-cases.md)
- [Use Cases (zh-CN)](docs/use-cases.zh-CN.md)

## License

Apache-2.0
