# javachanges GitLab CI/CD 使用指南


## 1. 概述

这份指南说明如何在 GitLab CI/CD 中使用 `javachanges` 完成：

1. 常规校验
2. GitLab CI/CD 变量管理
3. release merge request 生成
4. 基于生成后的 release plan 创建正式 tag
5. 在 tag pipeline 中执行 Maven 发布
6. Maven 依赖缓存

`javachanges` 目前有两个 GitLab 专用命令：

| 命令 | 作用 |
| --- | --- |
| `gitlab-release-plan` | 创建或更新 release 分支和 release merge request |
| `gitlab-tag-from-plan` | 在 release plan 合入后创建并推送正式 tag |

## 2. `javachanges` 在 GitLab CI/CD 中能做什么

推荐的命令分工如下：

| 目标 | 命令 |
| --- | --- |
| 查看当前待发布状态 | `status` |
| 在本地或 CI 中应用 release plan | `plan --apply true` |
| 根据环境变量生成 Maven settings | `write-settings` |
| 预览 GitLab 变量 | `render-vars --platform gitlab` |
| 检查平台就绪状态 | `doctor-local`、`doctor-platform` |
| 通过 `glab` 同步 GitLab 变量 | `sync-vars --platform gitlab` |
| 通过 `glab variable export` 回读审计 | `audit-vars --platform gitlab` |
| 创建或更新 GitLab release MR | `gitlab-release-plan --execute true` |
| 在 release plan 落地后创建并推送 tag | `gitlab-tag-from-plan --execute true` |
| 做发布前检查 | `preflight` |
| 执行真正的 Maven deploy | `publish --execute true` |

## 3. 变量模型

### 3.1 通用 Maven 仓库变量

`javachanges` 会读取 `env/release.env.example` 里的这些变量：

| 变量 | 必填 | 含义 |
| --- | --- | --- |
| `MAVEN_RELEASE_REPOSITORY_URL` | 是 | release 仓库地址 |
| `MAVEN_SNAPSHOT_REPOSITORY_URL` | 是 | snapshot 仓库地址 |
| `MAVEN_RELEASE_REPOSITORY_ID` | 是 | release 仓库 id |
| `MAVEN_SNAPSHOT_REPOSITORY_ID` | 是 | snapshot 仓库 id |
| `MAVEN_REPOSITORY_USERNAME` | 是，除非你显式拆分了 release/snapshot 凭据 | 通用用户名 |
| `MAVEN_REPOSITORY_PASSWORD` | 是，除非你显式拆分了 release/snapshot 凭据 | 通用密码 |
| `MAVEN_RELEASE_REPOSITORY_USERNAME` | 否 | release 专用用户名 |
| `MAVEN_RELEASE_REPOSITORY_PASSWORD` | 否 | release 专用密码 |
| `MAVEN_SNAPSHOT_REPOSITORY_USERNAME` | 否 | snapshot 专用用户名 |
| `MAVEN_SNAPSHOT_REPOSITORY_PASSWORD` | 否 | snapshot 专用密码 |
| `GITLAB_RELEASE_TOKEN` | 否 | 某些 GitLab release 场景下可选的额外 token |

当你用 `sync-vars` 把这些值同步到 GitLab 时，敏感值会被写成 masked + protected 变量。

### 3.2 GitLab release 分支 / MR 自动化额外依赖的变量

`gitlab-release-plan` 还依赖这些运行时变量：

| 变量 | 来源 |
| --- | --- |
| `CI_PROJECT_ID` | GitLab 内置 CI 变量，或显式传 `--project-id` |
| `CI_DEFAULT_BRANCH` | GitLab 内置 CI 变量 |
| `CI_SERVER_HOST` | GitLab 内置 CI 变量 |
| `CI_SERVER_URL` | GitLab 内置 CI 变量 |
| `CI_PROJECT_PATH` | GitLab 内置 CI 变量 |
| `GITLAB_RELEASE_BOT_USERNAME` | 你自己提供的项目变量 |
| `GITLAB_RELEASE_BOT_TOKEN` | 你自己提供的项目变量 |

`gitlab-tag-from-plan` 还需要：

| 参数或变量 | 含义 |
| --- | --- |
| `--before-sha` 或 `CI_COMMIT_BEFORE_SHA` | 上一个提交的 SHA |
| `--current-sha` 或 `CI_COMMIT_SHA` | 当前提交的 SHA |

## 4. 本地准备

### 4.1 构建 CLI

```bash
mvn -q test
```

### 4.2 初始化本地 env 文件

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="init-env --target env/release.env.local"
```

### 4.3 预览 GitLab 变量

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="render-vars --env-file env/release.env.local --platform gitlab"
```

### 4.4 检查本地就绪状态

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="doctor-local --env-file env/release.env.local --gitlab-repo group/project"
```

### 4.5 使用 `glab` 同步 GitLab 变量

Dry-run：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="sync-vars --env-file env/release.env.local --platform gitlab --repo group/project"
```

真正写入：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="sync-vars --env-file env/release.env.local --platform gitlab --repo group/project --execute true"
```

回读审计：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="audit-vars --env-file env/release.env.local --platform gitlab --gitlab-repo group/project"
```

## 5. 推荐的 GitLab CI/CD Pipeline 拓扑

推荐 stage 顺序：

1. `verify`
2. `release-plan`
3. `tag`
4. `publish`

## 6. `.gitlab-ci.yml` 示例

```yaml
stages:
  - verify
  - release-plan
  - tag
  - publish

default:
  image: maven:3.9.9-eclipse-temurin-8
  cache:
    key:
      files:
        - pom.xml
    paths:
      - .m2/repository

variables:
  MAVEN_OPTS: "-Dmaven.repo.local=.m2/repository"

verify:
  stage: verify
  script:
    - mvn -B verify
    - mvn -B -DskipTests compile exec:java -Dexec.args="status --directory $CI_PROJECT_DIR"
  rules:
    - if: $CI_PIPELINE_SOURCE == "merge_request_event"
    - if: $CI_COMMIT_BRANCH

release_plan_mr:
  stage: release-plan
  script:
    - mvn -B -DskipTests compile
    - mvn -B -DskipTests compile exec:java -Dexec.args="gitlab-release-plan --directory $CI_PROJECT_DIR --execute true"
  rules:
    - if: $CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH

release_tag:
  stage: tag
  script:
    - mvn -B -DskipTests compile
    - >
      mvn -B -DskipTests compile exec:java
      -Dexec.args="gitlab-tag-from-plan --directory $CI_PROJECT_DIR --before-sha $CI_COMMIT_BEFORE_SHA --current-sha $CI_COMMIT_SHA --execute true"
  rules:
    - if: $CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH

publish_release:
  stage: publish
  script:
    - mvn -B -DskipTests compile
    - >
      mvn -B -DskipTests compile exec:java
      -Dexec.args="publish --directory $CI_PROJECT_DIR --tag $CI_COMMIT_TAG --execute true"
  rules:
    - if: $CI_COMMIT_TAG
```

这个示例的分工如下：

| Job | 作用 |
| --- | --- |
| `verify` | 校验仓库并输出当前发布状态 |
| `release_plan_mr` | 创建或更新 release 分支和 merge request |
| `release_tag` | 在默认分支上的 release plan manifest 发生变化后创建正式 tag |
| `publish_release` | 基于正式 Git tag 执行发布 |

## 7. GitLab CI 中更安全的 `script:` 写法

推荐：

- `script:` 里的每一项尽量只放一条命令。
- 长命令用 YAML 折叠标量 `- >` 换行，不要把它写成内联 shell 小程序。
- 优先直接执行 `mvn ...` 或 `java -jar ...`，不要在 `.gitlab-ci.yml` 里临时拼复杂脚本。
- 如果 CI 里必须写文件，优先用 `printf`、`echo`，或者把逻辑放到仓库里的 `scripts/` 脚本。
- 对 GitLab 变量保持显式引用，比如 `"$CI_PROJECT_DIR"`、`"$CI_COMMIT_TAG"`。

不推荐：

- 在 `script: - |` 里再写 shell heredoc，例如 `cat <<EOF`。
- heredoc 正文和 YAML key / list item 缩进接近，稍微重排就会被 YAML 误判。
- 把大段 shell 直接塞进 `.gitlab-ci.yml`，而不是落到仓库脚本里。

为什么这个坑在 GitLab CI 里高发：

- GitLab 会先把 `.gitlab-ci.yml` 当 YAML 解析，之后才交给 shell。
- heredoc 同时要求 YAML 缩进合法、shell 终止符位置也合法。
- 只要缩进轻微漂移，YAML 就可能把 heredoc 正文当成新的 key，直接报 `could not find expected ':' while scanning a simple key`。
- 用户经常把本地能跑的 shell 片段复制进 `script: - |`，再手动调整缩进，于是特别容易踩坑。

推荐模式：

```yaml
release_tag:
  stage: tag
  script:
    - mvn -B -DskipTests compile
    - >
      mvn -B -DskipTests compile exec:java
      -Dexec.args="gitlab-tag-from-plan --directory $CI_PROJECT_DIR --before-sha $CI_COMMIT_BEFORE_SHA --current-sha $CI_COMMIT_SHA --execute true"
```

避免这样写：

```yaml
release_tag:
  stage: tag
  script:
    - |
      cat <<EOF > release.env
      CI_PROJECT_DIR=$CI_PROJECT_DIR
      CI_COMMIT_SHA=$CI_COMMIT_SHA
      EOF
      mvn -B -DskipTests compile exec:java -Dexec.args="gitlab-tag-from-plan --directory $CI_PROJECT_DIR --execute true"
```

更稳妥的写文件方式：

```yaml
write_env:
  script:
    - printf 'CI_PROJECT_DIR=%s\nCI_COMMIT_SHA=%s\n' "$CI_PROJECT_DIR" "$CI_COMMIT_SHA" > release.env
```

如果准备逻辑比较长，直接下沉到仓库脚本：

```yaml
release_plan_mr:
  script:
    - ./scripts/gitlab-release-plan.sh
```

## 8. GitLab 专用命令的行为说明

### 8.1 `gitlab-release-plan`

默认行为：

| 输入 | 默认值 |
| --- | --- |
| `--project-id` | `CI_PROJECT_ID` |
| `--target-branch` | `CI_DEFAULT_BRANCH`，如果拿不到则回退到 `main` |
| `--release-branch` | `changeset-release/<target-branch>` |

关键行为：

| 条件 | 结果 |
| --- | --- |
| 没有 pending changesets | 跳过 release MR |
| 没有传 `--execute true` | 只做 dry-run |
| 生成 release plan 后没有任何 staged 变更 | 跳过 MR 更新 |
| 已经存在打开中的 release MR | 更新它，而不是新建一个 |
| 远端已经存在 `changeset-release/*` 分支 | 先解析远端当前 SHA，再用显式 `--force-with-lease` 复用并更新该分支 |

补充说明：

- `gitlab-release-plan` 会把 `changeset-release/<target-branch>` 视为自动化拥有的分支。
- 如果远端分支还在，但当前没有匹配的 open MR，命令仍会刷新这个分支，然后重新创建 MR。
- 这样默认分支上的重复 pipeline 可以保持幂等，不需要手工删除远端 release branch。

### 8.2 `gitlab-tag-from-plan`

关键行为：

| 条件 | 结果 |
| --- | --- |
| `beforeSha` 缺失或全 0 | 跳过打 tag |
| `.changesets/release-plan.json` 在两个提交之间没有变化 | 跳过打 tag |
| 远端 tag 已经存在 | 跳过打 tag |
| 没有传 `--execute true` | 只做 dry-run |

## 9. 在 GitLab CI/CD 中做通用 Maven 发布

通用 `publish` 辅助命令会负责：

1. 复用 `preflight` 逻辑校验版本、tag 和凭据
2. 生成 `.m2/settings.xml`
3. 读取 release / snapshot 仓库地址
4. 用 CI/CD variables 中的凭据执行 Maven deploy

典型的 tag pipeline 拆分方式：

```yaml
publish_preflight:
  stage: publish
  script:
    - mvn -B -DskipTests compile
    - >
      mvn -B -DskipTests compile exec:java
      -Dexec.args="preflight --directory $CI_PROJECT_DIR --tag $CI_COMMIT_TAG"
  rules:
    - if: $CI_COMMIT_TAG

publish_execute:
  stage: publish
  script:
    - mvn -B -DskipTests compile
    - >
      mvn -B -DskipTests compile exec:java
      -Dexec.args="publish --directory $CI_PROJECT_DIR --tag $CI_COMMIT_TAG --execute true"
  rules:
    - if: $CI_COMMIT_TAG
```

## 10. GitLab CI/CD 中的 Maven Cache 行为

推荐缓存配置：

```yaml
cache:
  key:
    files:
      - pom.xml
  paths:
    - .m2/repository
```

推荐搭配这个运行时参数：

```yaml
variables:
  MAVEN_OPTS: "-Dmaven.repo.local=.m2/repository"
```

它主要改善的是：

| 可以较好缓存 | 不能单靠 GitLab cache 解决 |
| --- | --- |
| Maven 依赖 | Git clone / fetch 开销 |
| Maven plugin | JDK / Docker image 拉取时间 |
| 相同 `pom.xml` 下的重复 pipeline | GitLab API 调用，例如创建 release MR |
| 在共享 cache 后端上的跨 job / 跨 runner 复用 | 远端仓库发布与等待时间 |

需要注意的行为：

| 场景 | 结果 |
| --- | --- |
| 新 cache key 首次出现 | 第一次 pipeline 仍然会下载依赖 |
| `pom.xml` 变化 | 可能触发新的 cache key |
| 不同 runner 且没有共享 cache | 缓存复用会比较弱 |
| GitLab 配置了共享 / 分布式 cache | 跨 runner 复用会更好 |

## 11. 可选的 Hygiene / Secret Scanning 策略

如果你在同一个仓库里再接入 hygiene 或 secret scanning，要先区分两类结果：

- 真实 secret 命中：仓库内容里真的出现了像凭据、token 或私钥一样的值。
- 规则自命中：扫描器在 `.gitlab-ci.yml`、`Makefile` 或规则定义文件里，扫到了它自己的模式字面量，比如 `ghp_`、`glpat-`、`AKIA`、`BEGIN PRIVATE KEY`。

推荐的默认策略：

1. 把 secret 检测模式放到独立文件，例如 `.hygiene/secret-patterns.txt`
2. 扫描时显式排除这个规则文件，以及 `.gitlab-ci.yml`、`Makefile`
3. 继续扫描源码、脚本、文档和业务配置文件
4. allowlist 注释只用于少量、经过评审的个别例外

为什么这是更稳妥的默认值：

- 扫描器配置不属于业务内容，不应该按业务文件同等扫描
- 只排除一个专用规则文件，比把正则散落在 CI YAML 里更容易维护
- 也能避免把模式拆碎后带来的可读性和可移植性问题

推荐示例：

```yaml
hygiene:
  stage: verify
  script:
    - ./scripts/secret-scan.sh
  rules:
    - if: $CI_COMMIT_BRANCH
```

```bash
# scripts/secret-scan.sh
set -eu

scanner scan \
  --rules .hygiene/secret-patterns.txt \
  --exclude .hygiene/secret-patterns.txt \
  --exclude .gitlab-ci.yml \
  --exclude Makefile \
  .
```

方案对比：

| 方案 | 优点 | 缺点 | 建议 |
| --- | --- | --- | --- |
| 只排除 `.gitlab-ci.yml` / `Makefile` | 简单，接入快 | 规则以后挪到别的文件时还会继续误报 | 只能当临时止血方案 |
| 把规则移到单独文件并排除 | 责任边界清晰，最容易解释，也最稳定 | 需要多维护一个文件 | 作为默认推荐方案 |
| 把模式拆分后再拼接 | 不依赖排除列表，也能避开字面量命中 | 可读性差，容易写坏，还可能依赖具体工具实现 | 默认不推荐 |
| 使用 allowlist 注释 | 对少量已审阅例外很精确 | 容易越积越多，最后掩盖真实问题，而且常常和具体工具强绑定 | 只用于个别例外 |

避免这样做：

- 直接在 `.gitlab-ci.yml` 里内联 secret 检测正则
- 在 `Makefile` 目标里重复维护同一套模式字面量
- 把 allowlist 当成扫描器配置文件的主抑制手段

## 12. 常见错误

| 问题 | 原因 | 修复方式 |
| --- | --- | --- |
| release MR job 无法 push | 缺少 `GITLAB_RELEASE_BOT_TOKEN` 或 `GITLAB_RELEASE_BOT_USERNAME` | 把 bot 凭据加成项目变量 |
| release MR job 报 `stale info` | javachanges 解析完远端 SHA 之后，又有别的流程更新了同一个 `changeset-release/*` 分支 | 直接重跑 pipeline；如果这个分支名被多个自动流程共用，需要改成单一 owner |
| release tag job 一直不打 tag | `release-plan.json` 没变化，或 `CI_COMMIT_BEFORE_SHA` 不可用 | 检查默认分支 pipeline 和 release plan 产物 |
| pipeline 还没启动 job 就报 `could not find expected ':' while scanning a simple key` | heredoc 或其他多行 shell 内容破坏了 YAML 缩进语义 | 把 `script: - |` + heredoc 改成 `- >`、`printf`，或者仓库内脚本 |
| hygiene / secret scan 命中了 `.gitlab-ci.yml` 或 `Makefile`，但仓库里并没有真实凭据 | 扫描器扫到了自己的规则字面量 | 把模式移到独立规则文件，并排除扫描器自有配置文件 |
| `sync-vars` 没有任何效果 | env 文件里还是占位值 | 先把 `replace-me` 替换成真实值 |
| `audit-vars` 报 `MISMATCH` | 本地 env 与远端项目变量已经不一致 | 重新同步，或明确选择以哪一边为准 |
| publish job 提示 Maven 凭据缺失 | 项目变量没有配置完整 | 先用 `glab` 同步变量，再重跑 pipeline |

## 13. 推荐与哪些文档配合阅读

建议把下面这些文档配合起来看：

| 需求 | 文档 |
| --- | --- |
| 通用发布命令与本地准备 | [Development Guide](./development-guide.md) |
| 当前仓库自己的 GitHub release 流程 | [GitHub Actions Release Flow](./github-actions-release.md) |
| Maven Central 专用发布 | [Publish To Maven Central](./publish-to-maven-central.md) |

## 14. 总结

GitLab CI/CD 中最实用的路径通常是：

1. 用 `status` 做基础校验
2. 用 `gitlab-release-plan` 生成或更新 release MR
3. 用 `gitlab-tag-from-plan` 创建正式 tag
4. 用 `sync-vars` 和 `audit-vars` 管理 GitLab 项目变量
5. 在 tag pipeline 中用 `preflight` 和 `publish` 做正式发布

## 15. 参考资料

- GitLab CI/CD YAML syntax: https://docs.gitlab.com/ci/yaml/
- GitLab CI/CD caching: https://docs.gitlab.com/ci/caching/
- `glab auth login`: https://docs.gitlab.com/cli/auth/login/
- `glab variable set`: https://docs.gitlab.com/cli/variable/set/
- `glab variable export`: https://docs.gitlab.com/cli/variable/export/
