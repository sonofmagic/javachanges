# Java 初学者知识点完全指南

## 1. 概述

这份文档不是教你“怎么读 `javachanges` 这个项目”，而是借助这个项目里的真实代码，系统整理一遍 Java 初学者最应该掌握的知识点。

你可以把它理解成：

> 用 `javachanges` 当案例，学习 Java 语言、Java 标准库、Maven、Gradle 模型解析、CLI 开发、测试和工程实践。

这份文档重点覆盖的是“这个项目里确实用到”的知识，而不是泛泛而谈的 Java 全量语法。

| 大类 | 你能学到什么 |
| --- | --- |
| Java 基础语法 | 类、对象、构造器、`final`、`static`、访问修饰符 |
| 抽象能力 | 接口、枚举、结果模型、请求模型 |
| 异常处理 | `IllegalArgumentException`、`IllegalStateException`、`IOException` |
| 标准库 | `Path`、`Files`、集合、正则、`ProcessBuilder` |
| Maven 和 Gradle 模型 | `pom.xml`、`gradle.properties`、依赖、plugin、Mojo、`revision` |
| 测试 | JUnit 5、`@TempDir`、命令输出断言、文件断言 |
| 工程实践 | dry-run、文件驱动、小类拆分、命令分层 |

## 2. 这份文档应该怎么读

如果你是 Java 初学者，推荐这样读：

1. 先看第 3 章，理解 Java 语言基础在这个项目里怎么出现。
2. 再看第 4 章，理解这个项目最常用的 Java 标准库。
3. 然后看第 5 章和第 6 章，理解 Maven、CLI、测试这些工程知识。
4. 最后看第 7 章和第 8 章，把“语法”连接到“工程实践”。

> **提示**：不要一上来就追每一个源码文件。先建立知识点地图，再回头看源码，会轻松很多。

## 3. 这个项目里最核心的 Java 语言知识点

### 3.1 类、对象、字段、构造器

这个项目里最常见的写法，是“一个类负责一件事”。

例如：

- [`core/PublishSupport.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/PublishSupport.java)
- [`core/ReleasePlanner.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/ReleasePlanner.java)
- [`core/ReleaseNotesGenerator.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/ReleaseNotesGenerator.java)

典型结构是：

```java
final class ReleasePlanner {
    private final Path repoRoot;

    ReleasePlanner(Path repoRoot) {
        this.repoRoot = repoRoot;
    }
}
```

这里可以学到：

| 语法 | 作用 |
| --- | --- |
| `class` | 定义类 |
| `final class` | 表示这个类不打算被继承 |
| `private final` 字段 | 构造后不会再变的成员 |
| 构造器 | 创建对象时初始化字段 |

这类对象通常叫“服务对象”或“业务对象”。它们不是纯数据，而是“带上下文的行为”。

### 3.2 `final` 到底在这个项目里是什么意思

`final` 在这个项目里出现得非常多。

| 位置 | 含义 |
| --- | --- |
| `final class` | 类不希望被继承 |
| `final` 字段 | 赋值一次后不再变化 |
| `final` 方法参数 | 表示方法内不打算重新赋值 |

为什么这里喜欢用 `final`：

1. 工具型项目更强调“状态清晰”。
2. 很多对象一旦创建，依赖就不应该再变。
3. 对初学者来说，`final` 有助于理解“这个值是固定的还是可变的”。

### 3.3 访问修饰符

这个项目里你会频繁看到：

| 修饰符 | 说明 |
| --- | --- |
| `public` | 对外暴露，例如 CLI 主入口 |
| 默认不写 | 同包可见，适合同一包内部协作 |
| `private` | 仅类内部使用 |

例如：

- [`core/JavaChangesCli.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/JavaChangesCli.java) 是 `public final class`
- 很多 support / planner / parser 类只是包内可见

这说明一个重要的 Java 设计习惯：

> 默认尽量收紧可见性，只把必须暴露的类和方法设成 `public`。

### 3.4 静态方法和工具类

这个项目里有很多工具类：

- [`core/ReleaseUtils.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/ReleaseUtils.java)
- [`core/ReleaseTextUtils.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/ReleaseTextUtils.java)
- [`core/ReleaseJsonUtils.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/ReleaseJsonUtils.java)
- [`core/ReleaseProcessUtils.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/ReleaseProcessUtils.java)

它们通常长这样：

```java
final class ReleaseTextUtils {
    private ReleaseTextUtils() {
    }

    static String trimToNull(String value) {
        // ...
    }
}
```

这体现的是典型的工具类模式：

| 特征 | 作用 |
| --- | --- |
| 私有构造器 | 不允许创建实例 |
| `static` 方法 | 直接按类调用 |
| 无状态 | 不保存对象内部状态 |

### 3.5 接口

接口在这个项目里不是最多的，但有非常好的入门例子：

- [`core/MavenCommandModels.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/MavenCommandModels.java)

你可以重点看：

| 名称 | 用途 |
| --- | --- |
| `MavenCommandProbe` | 定义“如何探测 Maven 命令可用性” |
| `MavenCommand` | 保存探测结果 |

接口的核心思想是：

> 先定义能力，再决定实现。

这对于测试尤其重要，因为测试里可以替换成假的实现。

### 3.6 枚举

这个项目里非常适合学习枚举：

- [`core/ReleaseTypes.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/ReleaseTypes.java)

里面有：

| 枚举 | 含义 |
| --- | --- |
| `Platform` | `github` / `gitlab` / `all` |
| `OutputFormat` | `text` / `json` |
| `ReleaseLevel` | `patch` / `minor` / `major` |

为什么不用普通字符串？

| 用字符串 | 用枚举 |
| --- | --- |
| 容易拼错 | 值域固定 |
| 到处要自己判断 | 可以把逻辑写进枚举 |
| 类型信息弱 | 编译器能帮你检查 |

### 3.7 异常

这个项目会大量主动抛异常，而不是默默返回 `null`。

常见异常：

| 异常 | 在这个项目里的典型场景 |
| --- | --- |
| `IllegalArgumentException` | 参数非法，例如缺少必填项 |
| `IllegalStateException` | 运行环境不符合预期，例如找不到 `pom.xml` |
| `IOException` | 文件读取、写入、进程 IO 出错 |
| `InterruptedException` | 等待进程结束时被中断 |

这反映一个非常重要的 Java 工程习惯：

> 调用者传错参数，和程序运行环境坏掉，是两种不同的问题，要用不同异常表达。

## 4. 这个项目里最值得掌握的 Java 标准库

### 4.1 `Path` 和 `Files`

这是这个项目里使用最频繁的标准库。

典型文件：

- [`core/ChangesetFileSupport.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/ChangesetFileSupport.java)
- [`core/ReleasePlanFiles.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/ReleasePlanFiles.java)
- [`core/MavenSettingsWriter.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/MavenSettingsWriter.java)

常见 API：

| API | 用途 |
| --- | --- |
| `Path` | 表示路径 |
| `Paths.get(...)` | 构造路径 |
| `Files.exists(...)` | 判断文件是否存在 |
| `Files.readAllBytes(...)` | 读取整个文件 |
| `Files.readAllLines(...)` | 逐行读取文件 |
| `Files.write(...)` | 写文件 |
| `Files.createDirectories(...)` | 创建目录 |
| `Files.deleteIfExists(...)` | 删除文件 |

这个项目很适合你练习“以文件为中心”的 Java 编程。

### 4.2 集合框架

这个项目大量使用集合，而不是复杂 ORM 或框架对象。

常见集合：

| 类型 | 典型用途 |
| --- | --- |
| `List` | 保存 changeset 列表、命令参数、模块列表 |
| `Map` | 保存 frontmatter 字段、JSON 字段、env 变量 |
| `Set` | 去重模块 |

常见实现：

| 实现 | 特点 |
| --- | --- |
| `ArrayList` | 最常见的顺序列表 |
| `LinkedHashMap` | 保留插入顺序的键值对 |
| `LinkedHashSet` | 保留顺序的去重集合 |
| `Collections.singletonList(...)` | 只读单元素列表 |

> **提示**：如果你老是分不清 `List`、`Set`、`Map`，就先记一句话。`List` 看顺序，`Set` 看去重，`Map` 看键值映射。

### 4.3 字符串和文本处理

这个项目不是数据库项目，而是典型的文本处理项目，所以字符串处理特别多。

常见能力：

| 能力 | 场景 |
| --- | --- |
| `trim()` / 判空 | 清洗命令参数 |
| `split(...)` | 拆版本号、拆多行输出 |
| `replaceFirst(...)` | 更新 `pom.xml` 中的 `<revision>` |
| `StringBuilder` | 生成 JSON、Markdown、命令输出 |

推荐重点看：

- [`core/ReleaseTextUtils.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/ReleaseTextUtils.java)
- [`core/ReleasePlan.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/ReleasePlan.java)

### 4.4 正则表达式

这个项目里很多“轻量解析”都靠正则完成。

典型场景：

| 场景 | 示例 |
| --- | --- |
| 提取 `<revision>` | 从 `pom.xml` 里拿版本 |
| 提取 Gradle `version` | 从 `gradle.properties` 里拿版本 |
| 解析 tag | 从 `v1.2.3` 或 `module/v1.2.3` 提取信息 |
| 解析 `MAVEN_OPTS` | 识别 `-Dmaven.repo.local=...` |

核心类：

| 类 | 用途 |
| --- | --- |
| `Pattern` | 编译正则 |
| `Matcher` | 执行匹配 |

这个项目能帮助你建立一个正确认知：

> 正则不是万能解析器，但在边界明确的小文本任务里非常实用。

### 4.5 `ProcessBuilder`

这个项目会调用很多外部命令：

- `git`
- `mvn`
- `gh`
- `glab`

核心就是 `ProcessBuilder`。

典型文件：

- [`core/ReleaseProcessUtils.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/ReleaseProcessUtils.java)
- [`core/PublishRuntime.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/PublishRuntime.java)
- [`core/GitlabReleaseRuntime.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/GitlabReleaseRuntime.java)

你应该掌握：

| 能力 | 说明 |
| --- | --- |
| 传命令数组 | 避免自己拼整条 shell 字符串 |
| 设工作目录 | `builder.directory(...)` |
| 读标准输出和错误输出 | `getInputStream()` / `getErrorStream()` |
| 等待退出码 | `waitFor()` |
| 用退出码判断是否成功 | `0` 通常表示成功 |

### 4.6 输入输出流

虽然这个项目不是网络服务，但依然会接触流。

典型场景：

| 场景 | 类型 |
| --- | --- |
| 读取子进程输出 | `InputStream` |
| CLI 输出 | `PrintStream` |
| 测试里捕获输出 | `ByteArrayOutputStream` |

推荐结合这些文件一起看：

- [`core/JavaChangesCli.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/JavaChangesCli.java)
- [`src/test/java/io/github/sonofmagic/javachanges/core/JavaChangesCliTest.java`](https://github.com/sonofmagic/javachanges/blob/main/src/test/java/io/github/sonofmagic/javachanges/core/JavaChangesCliTest.java)

## 5. 这个项目里最重要的 Maven 知识点

### 5.1 Maven 项目结构

这个项目本身就是标准 Maven 项目。

| 路径 | 作用 |
| --- | --- |
| `pom.xml` | 项目描述、依赖、插件配置 |
| `src/main/java` | 生产代码 |
| `src/test/java` | 测试代码 |
| `target` | 构建产物 |

### 5.2 `pom.xml` 应该先看什么

建议你先关注这几类配置：

| 配置 | 作用 |
| --- | --- |
| `<groupId>` / `<artifactId>` | Maven 坐标 |
| `<version>${revision}</version>` | 版本引用属性 |
| `dependencies` | 依赖库 |
| `build.plugins` | 构建插件 |

这个项目里比较关键的 plugin：

| Plugin | 作用 |
| --- | --- |
| `maven-compiler-plugin` | 控制 Java 编译版本 |
| `maven-surefire-plugin` | 运行测试 |
| `exec-maven-plugin` | 开发时直接运行 CLI |
| `maven-plugin-plugin` | 生成 Maven Plugin 描述 |

### 5.3 什么是 `revision`

这个项目把版本写成：

```xml
<version>${revision}</version>
```

然后在 `properties` 里定义：

```xml
<revision>__JAVACHANGES_CURRENT_SNAPSHOT_VERSION__</revision>
```

这样做的好处是：

| 好处 | 说明 |
| --- | --- |
| 版本集中管理 | 不用到处改版本字符串 |
| 发布脚本更简单 | 只改 `revision` 即可 |
| 多模块更方便 | 更适合统一版本策略 |

### 5.4 什么是 Maven Plugin

这个项目不只是普通 jar，它也是 Maven Plugin。

所以它虽然既可以这样调用：

```bash
java -jar javachanges-__JAVACHANGES_LATEST_RELEASE_VERSION__.jar status
```

但更推荐这样调用：

```bash
mvn javachanges:status
```

这背后能帮你理解：

| 概念 | 说明 |
| --- | --- |
| plugin | Maven 的扩展能力 |
| goal | 插件暴露给用户的命令 |
| Mojo | 一个 goal 对应的 Java 实现类 |

### 5.5 什么是 Mojo

这个项目的 Maven Plugin 代码主要在：

- [`src/main/java/io/github/sonofmagic/javachanges/maven`](https://github.com/sonofmagic/javachanges/tree/main/src/main/java/io/github/sonofmagic/javachanges/maven)

例如：

- `JavaChangesAddMojo`
- `JavaChangesPlanMojo`
- `JavaChangesStatusMojo`

你可以把它简单理解成：

| 术语 | 直观理解 |
| --- | --- |
| Maven goal | 你执行的 `mvn javachanges:status` |
| Mojo 类 | 真正执行这个 goal 的 Java 类 |

## 6. 这个项目里最值得学习的 CLI 与测试知识

### 6.1 CLI 入口长什么样

CLI 主入口：

- [`core/JavaChangesCli.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/JavaChangesCli.java)

你可以从这里学到一个非常标准的 Java CLI 入口模型：

1. 接收 `String[] args`
2. 创建根命令对象
3. 解析参数
4. 执行命令
5. 返回退出码

### 6.2 Picocli

这个项目使用 `picocli` 做命令行解析。

常见注解：

| 注解 | 作用 |
| --- | --- |
| `@Command` | 定义命令 |
| `@Option` | 定义参数 |
| `@ParentCommand` | 子命令拿到父命令 |
| `@Spec` | 访问命令元信息 |

这能帮助你理解：

> Java 写 CLI，不一定要自己手写参数解析，成熟库会让结构清晰很多。

### 6.3 命令分层

这个项目的命令调用链大致是：

```text
JavaChangesCli
  ↓
JavaChangesCommand
  ↓
AddCommand / PlanCommand / PublishCommand / ...
  ↓
RepoFiles / ReleasePlanner / PublishSupport / ...
```

这对应的是一种非常常见的工程分层：

| 层 | 责任 |
| --- | --- |
| CLI 层 | 收参数、打印结果 |
| 业务层 | 处理发布规划、环境检查、发布逻辑 |
| 文件层 | 读写 changeset、manifest、changelog |
| 工具层 | 文本、JSON、进程、模块识别 |

### 6.4 JUnit 5 测试

测试文件：

- [`src/test/java/io/github/sonofmagic/javachanges/core/JavaChangesCliTest.java`](https://github.com/sonofmagic/javachanges/blob/main/src/test/java/io/github/sonofmagic/javachanges/core/JavaChangesCliTest.java)
- [`src/test/java/io/github/sonofmagic/javachanges/maven/JavaChangesMavenPluginSupportTest.java`](https://github.com/sonofmagic/javachanges/blob/main/src/test/java/io/github/sonofmagic/javachanges/maven/JavaChangesMavenPluginSupportTest.java)

你应该重点学：

| 测试点 | 说明 |
| --- | --- |
| `@Test` | 普通测试方法 |
| `@TempDir` | 自动生成临时目录 |
| 直接调用主逻辑 | 不一定要起真实进程 |
| 断言输出 | 验证 `stdout` / `stderr` |
| 断言文件 | 验证 changeset、changelog、plan 文件 |

## 7. 从这个项目里能学到哪些工程实践

### 7.1 dry-run 思维

这个项目很多命令都支持先预览，再真正执行。

这反映的是非常好的工程习惯：

| 做法 | 价值 |
| --- | --- |
| 先打印将执行什么 | 降低误操作风险 |
| `--execute true` 才真正执行 | 默认更安全 |
| 输出具体命令 | 方便排查问题 |

### 7.2 文件驱动

这个项目把发布意图放在文件里，而不是数据库里。

这是一种很典型的工程化思路：

| 优点 | 说明 |
| --- | --- |
| 可版本控制 | 和代码一起进入 Git |
| 可 review | PR 里直接看 changeset |
| 可自动化 | CI 能直接读取 |
| 可追溯 | 能回看历史变更原因 |

### 7.3 小类拆分

这个项目里会看到很多后缀：

| 后缀 | 典型职责 |
| --- | --- |
| `Support` | 业务支撑逻辑 |
| `Runtime` | 运行时环境与命令执行 |
| `Utils` | 通用静态工具 |
| `Files` | 文件读写 |
| `Parser` | 文本解析 |
| `Models` | 数据模型 |

这说明一个很实用的工程原则：

> 当一个文件职责太多时，按职责拆分，比继续堆大类更可维护。

## 8. 初学者适合怎么借这个项目练习 Java

### 8.1 按知识点看代码

如果你想练语法和标准库，建议按这个顺序：

1. [`core/JavaChangesCli.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/JavaChangesCli.java)
2. [`core/GeneralCommands.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/GeneralCommands.java)
3. [`core/ChangesetParser.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/ChangesetParser.java)
4. [`core/ReleasePlanner.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/ReleasePlanner.java)
5. [`core/PublishSupport.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/PublishSupport.java)
6. [`core/ReleaseProcessUtils.java`](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/ReleaseProcessUtils.java)

### 8.2 初学者练习题

| 练习 | 你会练到什么 |
| --- | --- |
| 给 `status` 命令多加一行输出 | 字符串、CLI 输出、测试 |
| 给某个命令增加一个布尔参数 | Picocli、请求模型、命令分发 |
| 调整 changelog 标题文本 | 文件读写、文本处理 |
| 增加一个 JSON 输出字段 | `Map`、JSON 渲染、测试断言 |
| 把一段重复代码抽到工具方法 | 重构意识、静态方法 |

### 8.3 常见误区

**Q1：为什么这个项目里很多类都不是 `public`？**

因为它们只在同一个包内部协作，不需要对外暴露。Java 里收紧可见性通常是更好的默认选择。

**Q2：为什么不用很重的框架？**

因为这是一个命令行工具项目，核心任务是读文件、算版本、执行命令，不需要 Web 框架。

**Q3：为什么会有很多工具类？**

因为文本处理、进程处理、JSON 渲染、模块识别这些逻辑跨多个业务类复用，用静态工具类更直接。

**Q4：为什么大量使用异常，而不是返回错误码字符串？**

因为异常更适合表达“流程中断”，调用链也更清晰，CLI 层统一捕获后再转换成用户可见的报错。

## 9. 总结

如果你是 Java 初学者，这个项目最值得你掌握的不是“发布流程细节”，而是下面这些能力：

| 能力 | 对应关键词 |
| --- | --- |
| 看懂类和对象 | 构造器、字段、`final` |
| 看懂类型抽象 | 接口、枚举、模型类 |
| 看懂文件编程 | `Path`、`Files` |
| 看懂集合 | `List`、`Map`、`Set` |
| 看懂文本处理 | `StringBuilder`、正则 |
| 看懂外部命令执行 | `ProcessBuilder` |
| 看懂 Maven 工程 | `pom.xml`、plugin、Mojo |
| 看懂测试 | JUnit 5、`@TempDir`、断言 |

你真正应该建立的是这种感觉：

> Java 不只是语法题，它更擅长把“文件、命令、流程、规则”组织成稳定的工程工具。

## 10. 参考资源

| 资源 | 说明 |
| --- | --- |
| [Java 官方文档](https://docs.oracle.com/javase/8/docs/) | Java 8 API 查询 |
| [The Java Tutorials](https://docs.oracle.com/javase/tutorial/) | Oracle 的 Java 教程 |
| [Maven 官方文档](https://maven.apache.org/guides/) | Maven 基础与插件机制 |
| [Picocli 官方文档](https://picocli.info/) | Java CLI 参数解析 |
| [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/) | 测试框架说明 |
