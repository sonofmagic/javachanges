# javachanges 源码学习完全指南

## 1. 概述

这份文档面向 Java 初学者。

目标不是教你“怎么使用 `javachanges`”，而是帮助你从这个仓库里系统学习：

| 主题 | 你能学到什么 |
| --- | --- |
| Java 基础语法 | 类、对象、接口、枚举、异常、集合、静态方法 |
| 工程结构 | Maven 项目布局、`src/main/java`、`src/test/java` |
| CLI 开发 | `main` 入口、参数解析、命令分发、错误码 |
| 文件与文本处理 | 读写文件、路径处理、Markdown/frontmatter 解析 |
| 进程调用 | 调用 `git`、调用 `mvn`、退出码处理 |
| 发布工程化 | version、changelog、release plan、Maven settings |
| Maven plugin | Mojo、plugin goal、`mvn javachanges:status` 这一类调用 |
| 测试方法 | JUnit 5、临时目录、CLI 输出断言 |

如果你把这份文档看懂，再回头看源码，这个仓库就不会只是“能跑”，而会变成一个很好的 Java 工程化练习项目。

## 2. 先建立整体认识

`javachanges` 是一个 Java CLI 工具，同时也能作为 Maven plugin 使用。

它解决的问题可以概括成一句话：

> 用 `.changesets/*.md` 文件记录 Maven 仓库的发布意图，然后自动生成 release plan、changelog、release notes，并辅助发布。

整个工作流大致是：

```text
开发者写 .changesets/*.md
        ↓
javachanges 读取并解析 changeset
        ↓
计算 release level / release version
        ↓
生成 release-plan.json / release-plan.md
        ↓
更新 pom.xml 的 revision 与 CHANGELOG.md
        ↓
辅助生成 Maven settings / 执行 deploy / 接入 GitHub 或 GitLab
```

所以这个仓库本质上不是“算法项目”，而是一个非常典型的：

- Java 工具型项目
- 以文件系统为核心输入输出
- 带命令行接口
- 带 Maven plugin 入口
- 带测试
- 带文档站

## 3. 这个仓库的目录应该怎么读

先看核心源码目录：

| 目录 | 作用 |
| --- | --- |
| `src/main/java/io/github/sonofmagic/javachanges/core/cli` | CLI 入口和各个命令 |
| `src/main/java/io/github/sonofmagic/javachanges/core/repo` | 仓库文件、changeset、release plan 落盘 |
| `src/main/java/io/github/sonofmagic/javachanges/core/release` | 发布规划、版本模型、release notes |
| `src/main/java/io/github/sonofmagic/javachanges/core/publish` | Maven settings、发布前检查、发布执行 |
| `src/main/java/io/github/sonofmagic/javachanges/core/env` | env 文件、平台变量渲染、doctor、sync、audit |
| `src/main/java/io/github/sonofmagic/javachanges/core/gitlab` | GitLab API、release MR、tag 辅助 |
| `src/main/java/io/github/sonofmagic/javachanges/core/util` | 文本、JSON、模块识别、进程执行等工具 |
| `src/main/java/io/github/sonofmagic/javachanges/maven` | Maven plugin 的 Mojo 实现 |
| `src/test/java` | JUnit 5 测试 |
| `docs` | 文档源文件 |
| `website` | VitePress 文档站配置 |

> **提示**：现在 `core` 下面虽然还有很多文件，但已经按职责分到子目录了。对初学者来说，这比一上来就改 Java package 更容易读。

## 4. Java 初学者必须先看懂的源码入口

推荐阅读顺序：

1. [`src/main/java/io/github/sonofmagic/javachanges/core/cli/JavaChangesCli.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/cli/JavaChangesCli.java)
2. [`src/main/java/io/github/sonofmagic/javachanges/core/cli/JavaChangesCommand.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/cli/JavaChangesCommand.java)
3. [`src/main/java/io/github/sonofmagic/javachanges/core/cli/GeneralCommands.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/cli/GeneralCommands.java)
4. [`src/main/java/io/github/sonofmagic/javachanges/core/release/ReleasePlanner.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/release/ReleasePlanner.java)
5. [`src/main/java/io/github/sonofmagic/javachanges/core/repo/RepoFiles.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/repo/RepoFiles.java)
6. [`src/main/java/io/github/sonofmagic/javachanges/core/env/ReleaseEnvSupport.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/env/ReleaseEnvSupport.java)
7. [`src/main/java/io/github/sonofmagic/javachanges/core/publish/PublishSupport.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/publish/PublishSupport.java)
8. [`src/main/java/io/github/sonofmagic/javachanges/core/gitlab/GitlabReleaseSupport.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/gitlab/GitlabReleaseSupport.java)

原因很简单：

- `JavaChangesCli` 告诉你程序从哪里开始
- `JavaChangesCommand` 告诉你命令是怎么挂起来的
- `GeneralCommands` 告诉你用户输入是怎么转成业务调用的
- 后面的几个 support / planner / repo 文件，才是真正的业务逻辑

## 5. 这个仓库里有哪些关键 Java 知识点

### 5.1 类、对象、构造器

这类代码在仓库里非常多：

```java
final class PublishSupport {
    private final Path repoRoot;

    PublishSupport(Path repoRoot, PrintStream out) {
        this.repoRoot = repoRoot;
    }
}
```

这里你能学到：

| 语法 | 含义 |
| --- | --- |
| `class` | 定义一个类 |
| `final class` | 这个类不打算被继承 |
| `private final` 字段 | 构造后不再变化的成员 |
| 构造器 | 用来初始化对象 |

在这个仓库里，大量类都是“带状态的小服务对象”，例如：

- `PublishSupport`
- `ReleasePlanner`
- `ReleaseNotesGenerator`
- `GitlabReleaseSupport`

这是一种很典型的 Java 写法：把“执行某类任务所需的上下文”放进对象字段里。

### 5.2 静态方法与工具类

例如：

- `ReleaseUtils`
- `ReleaseTextUtils`
- `ReleaseJsonUtils`
- `ReleaseProcessUtils`

这些类的特点是：

- 没有实例状态
- 大多数方法都是 `static`
- 常常有私有构造器

典型形式：

```java
private ReleaseTextUtils() {
}
```

这表示“这个类不应该被实例化，只拿来放工具方法”。

这是 Java 里非常常见的工具类模式。

### 5.3 接口

这个仓库里有一个很好的入门例子：

- [`src/main/java/io/github/sonofmagic/javachanges/core/publish/MavenCommandModels.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/publish/MavenCommandModels.java)

里面的 `MavenCommandProbe` 是接口。

你可以把接口理解成：

| 概念 | 说明 |
| --- | --- |
| 接口 | 只定义“要提供什么能力” |
| 实现类 / 匿名实现 | 具体决定“怎么做” |

这个设计的好处是方便测试，因为测试时可以注入假的 probe。

### 5.4 枚举

看这些文件：

- [`src/main/java/io/github/sonofmagic/javachanges/core/release/ReleaseTypes.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/release/ReleaseTypes.java)

其中有：

- `Platform`
- `OutputFormat`
- `ReleaseLevel`

枚举适合表达“有限且固定的一组值”，比如：

| 枚举 | 值 |
| --- | --- |
| `ReleaseLevel` | `PATCH`、`MINOR`、`MAJOR` |
| `OutputFormat` | `TEXT`、`JSON` |
| `Platform` | `GITHUB`、`GITLAB`、`ALL` |

这是比字符串更安全的写法。

### 5.5 异常

这个仓库大量使用：

- `IllegalArgumentException`
- `IllegalStateException`
- `IOException`
- `InterruptedException`

你可以这样理解：

| 异常 | 典型含义 |
| --- | --- |
| `IllegalArgumentException` | 用户传参不对 |
| `IllegalStateException` | 程序运行环境不满足预期 |
| `IOException` | 文件或 IO 出错 |
| `InterruptedException` | 进程等待被中断 |

例如：

- 参数错误：缺少 `--tag`
- 状态错误：找不到 `pom.xml`
- IO 错误：读不到 changeset 文件

## 6. 这个仓库里能学到哪些 Java 标准库

### 6.1 `java.nio.file`

这是本仓库最常用的标准库之一：

| 类型/方法 | 用途 |
| --- | --- |
| `Path` | 表示文件路径 |
| `Paths.get(...)` | 构造路径 |
| `Files.exists(...)` | 判断文件是否存在 |
| `Files.readAllBytes(...)` | 读取整个文件 |
| `Files.readAllLines(...)` | 逐行读取 |
| `Files.write(...)` | 写文件 |
| `Files.createDirectories(...)` | 创建目录 |
| `Files.deleteIfExists(...)` | 删除文件 |

你会在这些场景里看到：

- 读 `pom.xml`
- 读写 `.changesets/*.md`
- 写 `CHANGELOG.md`
- 写 `.m2/settings.xml`
- 写 `target/release-notes.md`

### 6.2 集合框架

常见类型：

| 类型 | 在本仓库中的典型用途 |
| --- | --- |
| `List` | 保存命令参数、changeset 列表、模块列表 |
| `Map` | 保存 frontmatter 字段、JSON 字段、env 键值 |
| `Set` | 去重模块列表 |

你会反复看到：

- `ArrayList`
- `LinkedHashMap`
- `LinkedHashSet`
- `Collections.singletonList(...)`

这正好是 Java 集合基础的真实练习场景。

### 6.3 正则表达式

很多文本解析都用了 `Pattern` 和 `Matcher`，例如：

- 从 `pom.xml` 中提取 `<revision>`
- 解析 tag 里的版本号
- 解析简单 JSON 结构
- 解析 `MAVEN_OPTS`

这是一个很典型的 Java 工程知识点：

> 对结构简单、边界明确的文本，可以直接用正则，而不一定要引入完整解析器。

### 6.4 `ProcessBuilder`

这个仓库频繁调用外部命令：

- `git`
- `mvn`
- `gh`
- `glab`

核心技术就是 `ProcessBuilder`。

你能学到：

| 能力 | 说明 |
| --- | --- |
| 指定命令数组 | 避免自己拼整条 shell 字符串 |
| 指定工作目录 | `builder.directory(...)` |
| 读取标准输出/错误输出 | `getInputStream()` / `getErrorStream()` |
| 等待退出 | `waitFor()` |
| 判断退出码 | `0` 表示成功 |

这对写 Java CLI 工具非常重要。

## 7. 这个仓库里有哪些 Maven 知识点

### 7.1 基础 Maven 项目结构

这个仓库本身就是标准 Maven 项目：

| 路径 | 含义 |
| --- | --- |
| `pom.xml` | Maven 工程描述文件 |
| `src/main/java` | 生产代码 |
| `src/test/java` | 测试代码 |
| `target` | 构建产物 |

### 7.2 `pom.xml` 里的重点

看 [`pom.xml`](../pom.xml)，你至少要关注这些点：

| 配置 | 含义 |
| --- | --- |
| `<groupId>` / `<artifactId>` | 包坐标 |
| `<version>${revision}</version>` | 版本来自 `revision` 属性 |
| `<packaging>jar</packaging>` | 构建 jar |
| `maven-compiler-plugin` | 控制 Java 编译 |
| `maven-plugin-plugin` | 生成 Maven plugin 描述 |
| `exec-maven-plugin` | 开发时直接运行 main |
| `maven-surefire-plugin` | 跑测试 |

### 7.3 什么是 `revision`

这个仓库把版本写成：

```xml
<version>${revision}</version>
```

然后在 properties 里定义：

```xml
<revision>1.2.0-SNAPSHOT</revision>
```

这意味着：

- 根版本是集中管理的
- `plan --apply true` 时可以只改 `<revision>`

这是 Maven release 工程里很常见的做法。

### 7.4 什么是 Maven plugin

这个仓库本身也是一个 Maven plugin。

所以你既可以：

```bash
java -jar javachanges-1.2.0.jar status
```

也可以：

```bash
mvn javachanges:status
```

这背后依赖的是：

- `maven-plugin-api`
- `maven-plugin-annotations`
- `maven-plugin-plugin`

## 8. 什么是 Mojo，为什么文件名后面常见 `Mojo`

在 Maven plugin 里，一个可执行目标通常对应一个 Mojo 类。

本仓库的相关代码在：

- [`src/main/java/io/github/sonofmagic/javachanges/maven`](https://github.com/sonofmagic/javachanges/tree/main/src/main/java/io/github/sonofmagic/javachanges/maven)

比如：

- `JavaChangesAddMojo`
- `JavaChangesPlanMojo`
- `JavaChangesStatusMojo`

你可以把它简单理解成：

| 名词 | 对应关系 |
| --- | --- |
| Maven goal | 用户执行的 `mvn javachanges:status` |
| Mojo 类 | 真正负责这个 goal 的 Java 类 |

所以 `Mojo` 不是这个仓库发明的词，而是 Maven plugin 世界里的传统术语。

## 9. 这个仓库里有哪些 CLI 开发知识点

### 9.1 `main` 入口

入口在：

- [`src/main/java/io/github/sonofmagic/javachanges/core/cli/JavaChangesCli.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/cli/JavaChangesCli.java)

它做的事情非常标准：

1. 创建根命令对象
2. 交给 Picocli 解析参数
3. 执行命令
4. 返回退出码

### 9.2 Picocli

这个仓库使用 [`picocli`](https://picocli.info/) 做命令行解析。

你可以从这些注解认识它：

| 注解 | 作用 |
| --- | --- |
| `@Command` | 定义命令 |
| `@Option` | 定义参数 |
| `@ParentCommand` | 让子命令拿到父命令对象 |
| `@Spec` | 获取命令元信息 |

这是 Java CLI 里很主流、很好用的一套方案。

### 9.3 命令分层

`javachanges` 的命令分层大致是：

```text
JavaChangesCli
  ↓
JavaChangesCommand
  ↓
AddCommand / PlanCommand / PublishCommand / ...
  ↓
Support / Planner / Repo / Runtime
```

这其实就是很典型的“薄命令层 + 厚服务层”。

## 10. 这个仓库里有哪些文件格式知识点

### 10.1 Changeset 文件

最典型的输入文件就是：

```md
---
"javachanges": minor
---

Improve CLI parsing.
```

这背后涉及：

- Markdown
- frontmatter
- 多模块 package map

### 10.2 Release plan

程序会生成：

| 文件 | 作用 |
| --- | --- |
| `.changesets/release-plan.json` | 机器读取 |
| `.changesets/release-plan.md` | PR / MR 文本 |

### 10.3 `CHANGELOG.md`

这是面向人阅读的变更记录。

程序会把变更按 `major` / `minor` / `patch` 分组插入。

### 10.4 `settings.xml`

发布时会生成 Maven 的 `settings.xml`，把仓库账号密码写进去。

这是 Maven 发布链路的重要一环。

## 11. 这个仓库里有哪些工程化知识点

### 11.1 dry-run 思维

这个仓库很多命令不是一上来就执行，而是先：

- 输出将执行什么
- 让你确认
- 只有 `--execute true` 才真的执行

这是工程化里非常重要的设计习惯。

### 11.2 文件驱动

发布意图写在文件里，而不是数据库里、网页表单里或聊天记录里。

优点：

| 优点 | 说明 |
| --- | --- |
| 可版本控制 | 跟代码一起 review |
| 可追溯 | 每次发布意图都有 git 历史 |
| 可自动化 | CI 直接读取 |

### 11.3 小类拆分

这个仓库最近做了很多整理，你会看到很多“Support / Runtime / Utils / Files / Parser”类。

这反映的是一个很重要的工程实践：

> 当一个文件太大，就按职责拆，而不是继续把所有逻辑堆在一起。

## 12. 测试层面你应该学什么

测试文件主要在：

- [`src/test/java/io/github/sonofmagic/javachanges/core/JavaChangesCliTest.java`](https://github.com/sonofmagic/javachanges/blob/main/src/test/java/io/github/sonofmagic/javachanges/core/JavaChangesCliTest.java)
- [`src/test/java/io/github/sonofmagic/javachanges/maven/JavaChangesMavenPluginSupportTest.java`](https://github.com/sonofmagic/javachanges/blob/main/src/test/java/io/github/sonofmagic/javachanges/maven/JavaChangesMavenPluginSupportTest.java)

你可以重点学：

| 测试点 | 说明 |
| --- | --- |
| `@TempDir` | 为测试创建临时目录 |
| CLI 执行封装 | 直接调用 `JavaChangesCli.execute(...)` |
| 断言输出 | 比较 `stdout` / `stderr` |
| 断言文件内容 | 检查写出的 changeset / changelog |

这非常适合初学者理解“工具类项目怎么测”。

## 13. 推荐学习路线

### 13.1 第一遍：只看流程

按顺序读：

1. `JavaChangesCli`
2. `JavaChangesCommand`
3. `GeneralCommands`
4. `ReleasePlanner`
5. `RepoFiles`

目标：

- 看懂“输入命令后发生了什么”

### 13.2 第二遍：只看文件处理

按顺序读：

1. `ChangesetParser`
2. `ChangesetFileSupport`
3. `ReleasePlanFiles`
4. `ReleaseNotesGenerator`

目标：

- 看懂“怎么从 Markdown 读出数据，再写回文件”

### 13.3 第三遍：只看环境与发布

按顺序读：

1. `ReleaseEnvSupport`
2. `PublishSupport`
3. `MavenSettingsWriter`
4. `GitlabReleaseSupport`

目标：

- 看懂“一个 Java CLI 怎么和真实发布环境交互”

### 13.4 第四遍：回头学工具类

按顺序读：

1. `ReleaseTextUtils`
2. `ReleaseModuleUtils`
3. `ReleaseProcessUtils`
4. `ReleaseJsonUtils`

目标：

- 看懂“公共逻辑为什么要抽出来”

## 14. 最适合初学者动手改的练习

| 练习 | 难度 | 你能练什么 |
| --- | --- | --- |
| 给某个输出加一行提示 | 低 | 字符串、CLI 输出、测试更新 |
| 给某个命令新增一个小参数 | 中 | Picocli、请求模型、业务流转 |
| 给 JSON 输出新增一个字段 | 中 | 模型、JSON 拼接、测试 |
| 给 changelog 新增一个小规则 | 中 | 文本处理、文件写回 |
| 抽一个工具方法到 util | 中 | 重构、访问级别、代码组织 |

> **提示**：如果你是初学者，不建议一上来就改 `ReleaseEnvSupport` 这种大文件。先从 `GeneralCommands`、`ChangesetParser`、`ReleaseTextUtils` 这种边界更清晰的类开始。

## 15. 学这个仓库时最容易卡住的点

| 卡点 | 原因 | 建议 |
| --- | --- | --- |
| 为什么文件在子目录里但 package 没变 | 目录结构和 Java package 不一定完全绑定 | 先接受“物理归类”和“包名边界”是两回事 |
| 为什么很多 helper 都是 package-private | 仓库内部协作优先，不急着暴露 public API | 先理解封装边界 |
| 为什么有 CLI 又有 Maven plugin | 两种调用入口服务不同场景 | 先学 CLI，再学 Mojo |
| 为什么很多地方直接拼字符串 | 这是工具型仓库，很多输出本来就是文本 | 不必一上来追求抽象过度 |
| 为什么还有手写 JSON | 当前 JSON 结构简单，避免引入额外依赖 | 先学清楚再考虑是否值得引库 |

## 16. 总结

如果你把这个仓库当成学习项目，它至少覆盖了这些非常实用的 Java 能力：

| 能力 | 是否覆盖 |
| --- | --- |
| Java 基础语法 | ✅ |
| Maven 项目结构 | ✅ |
| Java CLI 开发 | ✅ |
| 文件读写 | ✅ |
| 正则解析 | ✅ |
| 外部命令调用 | ✅ |
| Maven plugin 开发 | ✅ |
| 单元测试 | ✅ |
| 工程化重构 | ✅ |

它特别适合初学者的地方在于：

- 代码不是“玩具项目”
- 业务逻辑足够真实
- 又没有复杂框架和数据库
- 输入输出大多是文本和文件，容易调试

如果你接下来想继续学习，我建议顺序是：

1. 先读这份文档
2. 再按“推荐学习路线”看源码
3. 然后自己改一个小功能
4. 最后跑测试，观察整个反馈链路

## 17. 速查表

| 我想学什么 | 先看哪里 |
| --- | --- |
| CLI 入口 | `core/cli/JavaChangesCli.java` |
| 参数解析 | `core/cli/JavaChangesCommand.java` |
| 基础命令实现 | `core/cli/GeneralCommands.java` |
| 发布规划 | `core/release/ReleasePlanner.java` |
| changeset 解析 | `core/repo/ChangesetParser.java` |
| 文件写回 | `core/repo/ReleasePlanFiles.java` |
| env 与平台变量 | `core/env/ReleaseEnvSupport.java` |
| Maven 发布 | `core/publish/PublishSupport.java` |
| GitLab 自动化 | `core/gitlab/GitlabReleaseSupport.java` |
| 工具方法 | `core/util/ReleaseUtils.java` 与各个 `*Utils.java` |
| Maven plugin | `src/main/java/io/github/sonofmagic/javachanges/maven` |
| 测试 | `src/test/java` |

## 18. 参考资源

- [Getting Started](./getting-started.md)
- [Development Guide](./development-guide.md)
- [CLI 命令参考](./cli-reference.md)
- [Publish To Maven Central](./publish-to-maven-central.md)
- [GitHub Actions 使用指南](./github-actions-guide.md)
- [GitLab CI/CD 使用指南](./gitlab-ci-guide.md)
