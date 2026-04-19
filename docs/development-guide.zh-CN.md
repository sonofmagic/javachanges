# javachanges 开发使用指南


## 1. 概述

`javachanges` 是一个面向 Maven Monorepo 和单模块 Maven 仓库的 Java CLI 工具。

这个仓库当前的使用方式不是：

- `npm install`
- `brew install`
- 下载后直接得到一个全局 `javachanges` 命令

这个仓库当前的使用方式是：

- 安装本机开发依赖：JDK + Maven
- 拉取源码仓库
- 用 Maven 编译并直接运行入口类

> **注意**：仓库根目录目前没有 `mvnw`，所以你需要先在本机安装 Maven。

## 2. 环境要求

### 2.1 最低要求

根据仓库根目录的 [pom.xml](../pom.xml)，当前项目要求：

| 项目 | 要求 |
| --- | --- |
| JDK | Java 8+ |
| Maven | 3.8+ |
| Git | 需要 |
| 目标仓库 | 必须有根 `pom.xml`，并且要么包含 `<modules>`，要么是单模块根 artifact |

### 2.2 开发环境建议

当前仓库默认按 Java 8 开发和验证：

| 场景 | 建议 |
| --- | --- |
| 日常开发 | JDK 8 + Maven 3.9.x |
| 本地默认环境 | 与 `pom.xml` 的 Java 8 编译目标保持一致 |
| 命令执行 | 使用系统安装的 `mvn` |

> **提示**：仓库当前 `source/target` 是 Java 8，日常开发也建议直接使用 Java 8，减少环境差异。

## 3. 安装本机依赖

### 3.1 macOS

如果你使用 Homebrew，可以先安装 Java 8 和 Maven：

```bash
# 安装 Java 8（推荐 Corretto 8）
brew install --cask corretto@8

# 安装 Maven
brew install maven
```

安装后确认命令可用：

```bash
java -version
mvn -v
```

如果你希望新开终端默认就是 Java 8，可以追加：

```bash
export JAVA_HOME="/Library/Java/JavaVirtualMachines/amazon-corretto-8.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
```

然后重新打开终端，或者执行：

```bash
source ~/.zshrc
```

### 3.2 Linux

示例：

```bash
# Debian / Ubuntu
sudo apt install openjdk-8-jdk maven

# Fedora
sudo dnf install java-1.8.0-openjdk-devel maven
```

### 3.3 Windows

你可以使用：

- JDK：Amazon Corretto 8 / Temurin 8 / Oracle JDK 8
- Maven：官方安装包、Scoop、Chocolatey

安装完成后，同样确认：

```bash
java -version
mvn -v
```

## 4. 获取源码并安装项目

### 4.1 克隆仓库

```bash
git clone https://github.com/sonofmagic/javachanges.git
cd javachanges
```

### 4.2 验证构建

第一次进入仓库后，先跑一遍构建：

```bash
mvn -q test
```

这个命令会做两件事：

| 动作 | 说明 |
| --- | --- |
| 下载依赖 | 初始化本地 Maven 缓存 |
| 编译项目 | 确认源码可正常构建 |

> **说明**：当前仓库已经包含面向 CLI 的单元测试，因此 `mvn test` 现在既会验证编译，也会验证命令行为。

### 4.3 安装到本地 Maven 仓库（可选）

如果你希望把这个项目产物安装到本地 Maven 仓库，可以执行：

```bash
mvn install
```

这一步的作用是把构建产物写入 `~/.m2/repository`，但它不会自动给你生成全局 `javachanges` 命令。

## 5. 如何进入开发模式

### 5.1 这个项目里的“开发模式”是什么

这个仓库是 CLI 工具，不是 Web 服务，因此没有典型的：

- 热更新 dev server
- `npm run dev`
- 长驻后台进程

对这个项目来说，“开发模式”通常指：

1. 修改 `src/main/java` 下的源码
2. 用 Maven 重新编译
3. 直接通过 `exec:java` 运行入口类，或者先安装本地 SNAPSHOT 再走 Maven plugin goals
4. 观察命令输出，继续迭代

### 5.2 最常用的开发命令

在仓库根目录执行：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="status --directory /path/to/your/repo"
```

这条命令的含义如下：

| 片段 | 作用 |
| --- | --- |
| `compile` | 编译当前源码 |
| `exec:java` | 直接运行 `main` 入口类 |
| `-DskipTests` | 跳过测试阶段，加快迭代 |
| `-Dexec.args="..."` | 传递 CLI 参数 |

### 5.3 当前分支下最快的 plugin 验证方式

如果你想直接验证面向开发者的 Maven plugin 体验，可以先把当前 SNAPSHOT 安装到本地：

```bash
mvn -q -DskipTests install
```

然后执行独立的 plugin goals：

```bash
mvn io.github.sonofmagic:javachanges:1.2.0-SNAPSHOT:status
mvn io.github.sonofmagic:javachanges:1.2.0-SNAPSHOT:plan -Djavachanges.apply=true
mvn io.github.sonofmagic:javachanges:1.2.0-SNAPSHOT:add -Djavachanges.summary="add release notes command" -Djavachanges.release=minor
```

### 5.4 入口类

当前 CLI 入口类是：

- [src/main/java/io/github/sonofmagic/javachanges/core/JavaChangesCli.java](https://github.com/sonofmagic/javachanges/blob/main/src/main/java/io/github/sonofmagic/javachanges/core/JavaChangesCli.java)

如果你使用 IntelliJ IDEA 或 VS Code，也可以直接以这个类作为运行入口进行调试。

## 6. 开发阶段最常用的运行示例

### 6.1 查看状态

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="status --directory /path/to/your/repo"
```

### 6.2 添加 changeset

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="add --directory /path/to/your/repo --summary 'add release notes command' --release minor"
```

这条命令现在默认会写出官方 Changesets 风格的文件，例如：

````md
```md
---
"your-artifact-id": minor
---

add release notes command
```
````

### 6.3 生成发布计划

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="plan --directory /path/to/your/repo"
```

### 6.4 应用发布计划

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="plan --directory /path/to/your/repo --apply true"
```

### 6.5 输出帮助信息

如果你只是想先看支持哪些命令，可以先触发内置 usage 输出，例如：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="help"
```

当前源码中实际支持的高价值命令包括：

| 命令 | 作用 |
| --- | --- |
| `add` | 创建 changeset |
| `status` | 查看当前待发布状态 |
| `plan` | 生成或应用发布计划 |
| `write-settings` | 生成 Maven settings |
| `init-env` | 初始化发布环境变量模板 |
| `render-vars` | 渲染平台变量 |
| `doctor-local` | 本地环境诊断 |
| `doctor-platform` | 平台变量诊断 |
| `sync-vars` | 同步平台变量 |
| `audit-vars` | 审计平台变量 |
| `preflight` | 发布前检查 |
| `publish` | 发布辅助 |
| `release-notes` | 生成 release notes |

## 7. 推荐的日常开发流程

### 7.1 本地迭代流程

1. 安装好 JDK 和 Maven
2. 运行 `mvn -q test`，确认仓库能编译
3. 准备一个用于测试的 Maven 仓库
4. 修改 `src/main/java` 下的源码
5. 用 `mvn -q -DskipTests compile exec:java -Dexec.args="..."` 验证行为
6. 确认无误后，再执行 `mvn test` 或 `mvn package`

### 7.2 调试建议

| 方式 | 适用场景 |
| --- | --- |
| `mvn ... exec:java` | 最接近命令行真实使用方式 |
| IDE 直接运行 `JavaChangesCli` | 需要断点调试时 |
| `mvn package` 后再验证 | 需要确认打包结果时 |

## 8. 常见问题

### 8.1 `mvn: command not found`

原因：本机没有安装 Maven，或者 Maven 没有加入 `PATH`。

处理方式：

1. 安装 Maven
2. 重新打开终端
3. 执行 `mvn -v` 确认

### 8.2 `Unable to locate a Java Runtime`

原因：本机没有安装 JDK，或者 `JAVA_HOME` / `PATH` 配置不正确。

处理方式：

1. 安装 JDK
2. 执行 `java -version`
3. 再执行 `mvn -v`

### 8.3 运行后提示仓库结构不符合要求

`javachanges` 目标仓库需要至少满足：

| 要求 | 说明 |
| --- | --- |
| Git 仓库 | 必须已初始化 |
| 根 `pom.xml` | 必须存在 |
| `<modules>` 或单模块根 artifact | 二选一即可 |
| `<revision>` | 用于版本计算 |
| `.changesets/` | 用于记录 changeset |

### 8.4 为什么没有热更新

因为这是一个 Java CLI 项目，不是前端或服务端常驻进程项目。常见做法是每次修改后重新执行一遍：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="status --directory /path/to/your/repo"
```

### 8.5 现在可以直接全局安装成一个命令吗？

这个源码仓库本身还不是“安装后自动得到全局命令”的模式。

当前更稳定的使用方式是：

- 用 `mvn ... exec:java` 直接从源码运行
- 用 Maven 打 jar，再通过 `java -jar ...` 运行
- 在已经发布之后，由 CI 从 Maven Central 下载 jar 使用

### 8.6 怎么安全地调试真实发布链路？

建议按这个顺序来：

1. `status`
2. `plan`
3. 在一次性测试仓库里执行 `plan --apply true`
4. `preflight`
5. 不带 `--execute true` 先执行 `publish`

这样可以在真正 deploy 之前把整个流程观察清楚。

## 9. 总结

你可以把这个项目理解为“通过 Maven 运行的 Java 命令行工具源码仓库”。

最短路径如下：

| 目标 | 命令 |
| --- | --- |
| 安装本机依赖 | `brew install --cask corretto@8 && brew install maven` |
| 验证环境 | `java -version && mvn -v` |
| 拉取项目 | `git clone https://github.com/sonofmagic/javachanges.git` |
| 首次构建 | `mvn -q test` |
| 进入开发模式 | `mvn -q -DskipTests compile exec:java -Dexec.args="status --directory /path/to/your/repo"` |

## 10. 参考资源

| 资源 | 链接 |
| --- | --- |
| Maven 安装文档 | https://maven.apache.org/install.html |
| Maven 运行文档 | https://maven.apache.org/run.html |
| Amazon Corretto 8 下载 | https://docs.aws.amazon.com/corretto/latest/corretto-8-ug/downloads-list.html |
| Amazon Corretto 8 macOS 安装 | https://docs.aws.amazon.com/corretto/latest/corretto-8-ug/macos-install.html |
| 项目说明 | [README.md](https://github.com/sonofmagic/javachanges/blob/main/README.md) |
| 快速开始 | [docs/getting-started.md](./getting-started.md) |
| 故障排查 | [docs/troubleshooting-guide.md](./troubleshooting-guide.md) |
