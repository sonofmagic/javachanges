# javachanges GitHub Actions 使用指南


## 1. 概述

这份指南说明如何在 GitHub Actions 中使用 `javachanges` 完成：

1. 常规 CI 校验
2. release plan 生成
3. GitHub Actions variables / secrets 管理
4. 发布前检查与正式发布
5. Maven 依赖缓存

本文所有示例都使用直接的 CLI 命令，例如：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="status --directory $PWD"
```

> **注意**：当前仓库文档以直接调用 `javachanges` CLI 为主，仓库根目录并没有内置 Makefile 包装层。

## 2. `javachanges` 在 GitHub Actions 中能做什么

推荐的命令分工如下：

| 目标 | 命令 |
| --- | --- |
| 查看当前待发布状态 | `status` |
| 应用 release plan | `plan --apply true` |
| 从 manifest 读取 `releaseVersion` | `manifest-field --field releaseVersion` |
| 根据环境变量生成 Maven settings | `write-settings --output .m2/settings.xml` |
| 渲染 GitHub 需要的变量和 secrets | `render-vars --env-file env/release.env.local --platform github` |
| 检查本地 / 平台就绪状态 | `doctor-local`、`doctor-platform` |
| 同步 GitHub Actions variables / secrets | `sync-vars --platform github` |
| 回读审计 GitHub Actions variables / secrets | `audit-vars --platform github` |
| 做发布前检查 | `preflight` |
| 执行真正的 Maven deploy | `publish --execute true` |
| 生成 release notes | `release-notes --tag vX.Y.Z --output target/release-notes.md` |

## 3. 推荐的仓库文件布局

如果你要把 `javachanges` 接进 GitHub Actions，建议这些文件纳入版本控制：

| 路径 | 作用 |
| --- | --- |
| `.changesets/*.md` | 待发布变更意图 |
| `.changesets/release-plan.json` | 生成后的发布 manifest |
| `.changesets/release-plan.md` | 生成后的 release PR 正文 |
| `CHANGELOG.md` | 自动更新的 changelog |
| `env/release.env.example` | 发布变量模板 |
| `.github/workflows/*.yml` | CI 与发布工作流 |

## 4. 在接入 GitHub Actions 之前的本地准备

### 4.1 构建 CLI

```bash
mvn -q test
```

### 4.2 初始化本地 env 文件

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="init-env --target env/release.env.local"
```

`env/release.env.example` 中的模板字段如下：

| 变量 | 含义 |
| --- | --- |
| `MAVEN_RELEASE_REPOSITORY_URL` | release 仓库地址 |
| `MAVEN_SNAPSHOT_REPOSITORY_URL` | snapshot 仓库地址 |
| `MAVEN_RELEASE_REPOSITORY_ID` | Maven settings 中的 release server id |
| `MAVEN_SNAPSHOT_REPOSITORY_ID` | Maven settings 中的 snapshot server id |
| `MAVEN_REPOSITORY_USERNAME` | 通用用户名回退值 |
| `MAVEN_REPOSITORY_PASSWORD` | 通用密码回退值 |
| `MAVEN_RELEASE_REPOSITORY_USERNAME` | 可选的 release 专用用户名 |
| `MAVEN_RELEASE_REPOSITORY_PASSWORD` | 可选的 release 专用密码 |
| `MAVEN_SNAPSHOT_REPOSITORY_USERNAME` | 可选的 snapshot 专用用户名 |
| `MAVEN_SNAPSHOT_REPOSITORY_PASSWORD` | 可选的 snapshot 专用密码 |

### 4.3 检查本地环境

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="doctor-local --env-file env/release.env.local --github-repo owner/repo"
```

### 4.4 预览 GitHub variables / secrets

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="render-vars --env-file env/release.env.local --platform github"
```

### 4.5 使用 `gh` 同步 GitHub variables / secrets

Dry-run：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="sync-vars --env-file env/release.env.local --platform github --repo owner/repo"
```

真正写入：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="sync-vars --env-file env/release.env.local --platform github --repo owner/repo --execute true"
```

回读审计：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="audit-vars --env-file env/release.env.local --platform github --github-repo owner/repo"
```

`sync-vars` 会写入这些内容：

| 远端类型 | 名称 |
| --- | --- |
| GitHub Actions variables | `MAVEN_RELEASE_REPOSITORY_URL`、`MAVEN_SNAPSHOT_REPOSITORY_URL`、`MAVEN_RELEASE_REPOSITORY_ID`、`MAVEN_SNAPSHOT_REPOSITORY_ID` |
| GitHub Actions secrets | `MAVEN_REPOSITORY_USERNAME`、`MAVEN_REPOSITORY_PASSWORD`、`MAVEN_RELEASE_REPOSITORY_USERNAME`、`MAVEN_RELEASE_REPOSITORY_PASSWORD`、`MAVEN_SNAPSHOT_REPOSITORY_USERNAME`、`MAVEN_SNAPSHOT_REPOSITORY_PASSWORD` |

## 5. GitHub Actions 工作流模式

### 5.1 只做 CI 校验

适用于 pull request 只做构建和发布状态检查的场景。

```yaml
name: CI

on:
  push:
    branches: [main]
  pull_request:

permissions:
  contents: read

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v5
        with:
          fetch-depth: 0

      - uses: actions/setup-java@v5
        with:
          distribution: corretto
          java-version: '8'
          cache: maven
          cache-dependency-path: pom.xml

      - name: Verify build
        run: mvn -B verify

      - name: Inspect release state
        run: mvn -B -DskipTests compile exec:java -Dexec.args="status --directory $GITHUB_WORKSPACE"
```

### 5.2 自动生成 release PR

适用于 `main` 上累计 `.changesets/*.md`，再自动生成可审阅 release PR 的场景。

```yaml
name: Release Plan

on:
  push:
    branches: [main]
  workflow_dispatch:

permissions:
  contents: write
  pull-requests: write

jobs:
  release-pr:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v5
        with:
          fetch-depth: 0

      - uses: actions/setup-java@v5
        with:
          distribution: corretto
          java-version: '8'
          cache: maven
          cache-dependency-path: pom.xml

      - name: Build CLI
        run: mvn -B -DskipTests compile

      - name: Apply release plan
        run: mvn -B -DskipTests compile exec:java -Dexec.args="plan --directory $GITHUB_WORKSPACE --apply true"

      - name: Read release version
        if: hashFiles('.changesets/release-plan.json') != ''
        id: release_version
        run: |
          value="$(mvn -q -DskipTests compile exec:java -Dexec.args="manifest-field --directory $GITHUB_WORKSPACE --field releaseVersion" | tail -n 1)"
          echo "value=$value" >> "$GITHUB_OUTPUT"

      - name: Commit release plan
        if: hashFiles('.changesets/release-plan.json') != ''
        env:
          GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          RELEASE_VERSION: ${{ steps.release_version.outputs.value }}
        run: |
          git config user.name "github-actions[bot]"
          git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
          git switch -C changeset-release/main
          git add pom.xml CHANGELOG.md .changesets
          git commit -m "chore(release): apply changesets for v${RELEASE_VERSION}"
          git push --force-with-lease origin HEAD:changeset-release/main
          gh pr create \
            --base main \
            --head changeset-release/main \
            --title "chore(release): v${RELEASE_VERSION}" \
            --body-file .changesets/release-plan.md
```

### 5.3 使用 `javachanges publish` 进行通用 Maven 发布

适用于你自己的 Maven release / snapshot 仓库，希望让 `javachanges` 负责：

1. 校验 tag 与版本状态
2. 生成 `.m2/settings.xml`
3. 输出最终 Maven `deploy` 命令
4. 在确认后执行真实发布

```yaml
name: Publish

on:
  push:
    tags:
      - 'v*'

permissions:
  contents: read

jobs:
  publish:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v5
        with:
          fetch-depth: 0

      - uses: actions/setup-java@v5
        with:
          distribution: corretto
          java-version: '8'
          cache: maven
          cache-dependency-path: pom.xml

      - name: Build CLI
        run: mvn -B -DskipTests compile

      - name: Preflight
        env:
          MAVEN_RELEASE_REPOSITORY_URL: ${{ vars.MAVEN_RELEASE_REPOSITORY_URL }}
          MAVEN_SNAPSHOT_REPOSITORY_URL: ${{ vars.MAVEN_SNAPSHOT_REPOSITORY_URL }}
          MAVEN_REPOSITORY_USERNAME: ${{ secrets.MAVEN_REPOSITORY_USERNAME }}
          MAVEN_REPOSITORY_PASSWORD: ${{ secrets.MAVEN_REPOSITORY_PASSWORD }}
        run: |
          mvn -B -DskipTests compile exec:java \
            -Dexec.args="preflight --directory $GITHUB_WORKSPACE --tag ${GITHUB_REF_NAME}"

      - name: Publish
        env:
          MAVEN_RELEASE_REPOSITORY_URL: ${{ vars.MAVEN_RELEASE_REPOSITORY_URL }}
          MAVEN_SNAPSHOT_REPOSITORY_URL: ${{ vars.MAVEN_SNAPSHOT_REPOSITORY_URL }}
          MAVEN_REPOSITORY_USERNAME: ${{ secrets.MAVEN_REPOSITORY_USERNAME }}
          MAVEN_REPOSITORY_PASSWORD: ${{ secrets.MAVEN_REPOSITORY_PASSWORD }}
        run: |
          mvn -B -DskipTests compile exec:java \
            -Dexec.args="publish --directory $GITHUB_WORKSPACE --tag ${GITHUB_REF_NAME} --execute true"
```

> **注意**：这条通用 `publish` 流程依赖的是 `env/release.env.example` 里的那套 release / snapshot 仓库变量。  
> 如果你要做的是 Maven Central 发布，请结合阅读 [Publish To Maven Central](./publish-to-maven-central.md) 和 [GitHub Actions Release Flow](./github-actions-release.md)。

## 6. GitHub Actions 里的 Maven Cache 行为

推荐配置如下：

```yaml
- uses: actions/setup-java@v5
  with:
    distribution: corretto
    java-version: '8'
    cache: maven
    cache-dependency-path: pom.xml
```

它主要能解决这些问题：

| 可以较好缓存 | 不能靠 Maven cache 解决 |
| --- | --- |
| `~/.m2/repository` 里的 Maven 依赖 | `git checkout` 和 `git fetch` |
| Maven plugin 以及 plugin 传递依赖 | JDK 下载与安装 |
| 相同 `pom.xml` 哈希下的重复构建 | GPG 私钥导入 |
| 多条 workflow 复用同一份依赖图 | Sonatype 或其他远端仓库的发布等待时间 |

需要注意的行为：

| 场景 | 结果 |
| --- | --- |
| 新 cache key 第一次出现 | 仍然会下载依赖 |
| `pom.xml` 发生变化 | 可能触发新的 cache key，需要重新下载部分依赖 |
| GitHub-hosted runner 每次都是干净环境 | 缓存来自 GitHub 存储，而不是上一次 runner 本地磁盘 |
| 缓存 `target/` 而不是 Maven 仓库 | 对 Java library CI 通常不是好选择 |

## 7. 推荐的 GitHub Actions 检查顺序

建议按这个顺序组织：

1. `mvn -B verify`
2. `javachanges status`
3. 只有在 release-plan workflow 中才执行 `javachanges plan --apply true`
4. 在真正发布前先执行 `javachanges preflight`
5. 只有在正式发布 workflow 中才执行 `javachanges publish --execute true`

## 8. 常见错误

| 问题 | 原因 | 修复方式 |
| --- | --- | --- |
| release PR 每次内容都异常波动 | release workflow 提交了 `.changesets`、`pom.xml`、`CHANGELOG.md` 之外的文件 | 收紧 release-plan 的提交范围 |
| publish job 提示仓库凭据缺失 | 必需的 vars / secrets 没有同步到 GitHub | 先执行 `render-vars`、`sync-vars`，再执行 `audit-vars` |
| 开了 cache 但日志里还是有下载 | 新 cache key 或第一次跑 | 让至少一轮成功 workflow 先把 cache 热起来 |
| `publish` 因脏工作区失败 | `preflight` / `publish` 默认拒绝未提交修改 | 先提交，或只在明确知道风险时传 `--allow-dirty true` |
| 工作流示例里出现你仓库没有的 wrapper | 复制了依赖本地脚本或 Make 目标的示例 | 使用本文中的直接 CLI 命令 |

## 9. 推荐与哪些文档配合阅读

建议结合下面这些文档一起看：

| 需求 | 文档 |
| --- | --- |
| 当前仓库实际使用的 GitHub release PR 流程 | [GitHub Actions Release Flow](./github-actions-release.md) |
| Maven Central 发布要求 | [Publish To Maven Central](./publish-to-maven-central.md) |
| 跨平台发布命令说明 | [Development Guide](./development-guide.md) |

## 10. 总结

GitHub Actions 中比较实用的路径是：

1. 在 CI 中使用 `status` 校验发布状态
2. 使用 `plan --apply true` 自动生成可审阅 release PR
3. 用 `render-vars`、`sync-vars`、`audit-vars` 管理 GitHub 变量和 secrets
4. 在任何真实 deploy 之前先执行 `preflight`
5. 根据你的仓库类型，选择通用 `publish` 或 Maven Central 专用发布流程

## 11. 参考资料

- GitHub Actions workflow syntax: https://docs.github.com/en/actions/writing-workflows/workflow-syntax-for-github-actions
- GitHub dependency caching: https://docs.github.com/en/actions/concepts/workflows-and-actions/dependency-caching
- `actions/setup-java` cache 选项: https://github.com/actions/setup-java
