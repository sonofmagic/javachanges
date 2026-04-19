# 快速开始

## 1. 从 Maven Central 安装

当前已发布坐标：

- GroupId：`io.github.sonofmagic`
- ArtifactId：`javachanges`
- 当前正式版本：`1.2.0`
- Maven Central 页面：`https://central.sonatype.com/artifact/io.github.sonofmagic/javachanges`
- 直链 jar 地址：`https://repo1.maven.org/maven2/io/github/sonofmagic/javachanges/1.2.0/javachanges-1.2.0.jar`

先把正式发布的 jar 下载到本地：

```bash
mvn -q dependency:copy -Dartifact=io.github.sonofmagic:javachanges:1.2.0 -DoutputDirectory=.javachanges
```

然后运行 CLI：

```bash
java -jar .javachanges/javachanges-1.2.0.jar --help
```

在当前 `main` 分支上，如果你先把 SNAPSHOT 安装到本地，也可以把它直接当作 Maven plugin 使用：

```bash
mvn -q -DskipTests install
mvn io.github.sonofmagic:javachanges:1.2.0-SNAPSHOT:status
mvn io.github.sonofmagic:javachanges:1.2.0-SNAPSHOT:plan -Djavachanges.apply=true
mvn io.github.sonofmagic:javachanges:1.2.0-SNAPSHOT:add -Djavachanges.summary="add release notes command" -Djavachanges.release=minor
mvn io.github.sonofmagic:javachanges:1.2.0-SNAPSHOT:manifest-field -Djavachanges.field=releaseVersion
```

说明：

- 现在 `status`、`plan`、`add`、`manifest-field` 都有独立 goal
- `javachanges:run` 仍然保留，并且会默认把 `--directory` 设成 `${project.basedir}`
- `-Djavachanges.args="..."` 适合你需要完整传递原始 CLI 参数时使用

## 2. 准备目标仓库

你的目标仓库至少需要满足：

- 已初始化 git
- 有根 `pom.xml`
- 有 `<revision>` 属性
- 有 `CHANGELOG.md`，或者让 `javachanges` 在应用 release plan 时自动创建/更新
- 根 `pom.xml` 中要么有 `<modules>`，要么是单模块根 artifact

## 3. 创建 changeset

Monorepo 示例：

```bash
java -jar .javachanges/javachanges-1.2.0.jar add --directory /path/to/repo --summary "add release notes command" --release minor --modules core
```

单模块示例：

```bash
java -jar .javachanges/javachanges-1.2.0.jar add --directory /path/to/repo --summary "add release notes command" --release minor
```

这个命令会往 `.changesets/` 写入一个 Markdown 文件。

最短手写格式：

```md
---
"your-artifact-id": patch
---

Fix release-notes rendering.
```

Monorepo 示例：

```md
---
"core": minor
"cli": patch
---

Improve CLI parsing and release planning.
```

说明：

- `javachanges add` 默认会生成这种官方 Changesets 风格的 package map
- 正文第一条非空行会作为 `status`、changelog 和 release notes 使用的 summary
- 旧的 `release` / `modules` / `summary` frontmatter 仍然可兼容读取，但新文件建议统一写 package map
- changelog 会按聚合后的 release level 分成 `major`、`minor`、`patch`

## 4. 查看计划

```bash
java -jar .javachanges/javachanges-1.2.0.jar plan --directory /path/to/repo
```

## 5. 应用计划

```bash
java -jar .javachanges/javachanges-1.2.0.jar plan --directory /path/to/repo --apply true
```

应用后会更新：

- 根 `revision`
- `CHANGELOG.md`
- `.changesets/release-plan.json`
- `.changesets/release-plan.md`

## 6. 以源码方式进入开发模式

如果你是在开发 `javachanges` 这个仓库本身，才使用源码驱动的开发方式：

```bash
mvn -q test
mvn -q -DskipTests compile exec:java -Dexec.args="status --directory /path/to/your/repo"
```

完整开发流程请看 [Development Guide](./development-guide.md)。
