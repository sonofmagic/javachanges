# javachanges 输出契约说明


## 1. 概述

这篇文档专门说明 `javachanges` 的哪些输出适合给自动化脚本读取，哪些输出只适合给人看，以及当前实现里的输出结构大致长什么样。

适合这些场景：

- 你要围绕 `javachanges` 写 CI 脚本
- 你在判断到底该解析终端输出，还是该读取生成文件
- 你想知道哪些字段是更适合长期依赖的

## 2. 稳定性总览

| 输出 | 面向对象 | 稳定性建议 |
| --- | --- | --- |
| `manifest-field` 标准输出 | 脚本和 CI | 优先使用的机器可读接口 |
| `.changesets/release-plan.json` | 脚本和 CI | 优先使用的机器可读接口 |
| `.changesets/release-plan.md` | Pull Request / Merge Request 正文 | 面向人阅读，结构可能演进 |
| `status` 标准输出 | 本地操作者和审阅者 | 面向人，不要做刚性解析 |
| `render-vars` 标准输出 | 本地操作者 | 默认面向人 |
| `render-vars --format json` 标准输出 | 脚本和 CI | 机器可读 JSON 契约 |
| `doctor-local` 标准输出 | 本地操作者 | 默认面向人 |
| `doctor-local --format json` 标准输出 | 脚本和 CI | 机器可读 JSON 契约 |
| `doctor-platform` 标准输出 | 本地操作者 | 默认面向人 |
| `doctor-platform --format json` 标准输出 | 脚本和 CI | 机器可读 JSON 契约 |
| `sync-vars` dry-run 输出 | 本地操作者 | 只是预览文本，不是稳定 API |
| `audit-vars` 标准输出 | 本地操作者 | 默认面向人 |
| `audit-vars --format json` 标准输出 | 脚本和 CI | 机器可读 JSON 契约 |
| `preflight --format json` 标准输出 | 脚本和 CI | 机器可读发布预检查契约 |
| `publish --format json` 标准输出 | 脚本和 CI | 机器可读发布契约 |
| `github-release-plan --format json` 标准输出 | 脚本和 CI | 机器可读 GitHub release-plan 契约 |
| `github-tag-from-plan --format json` 标准输出 | 脚本和 CI | 机器可读 GitHub tag 契约 |
| `github-release-from-plan --format json` 标准输出 | 脚本和 CI | 机器可读 GitHub Release 契约 |
| `gitlab-release-plan --format json` 标准输出 | 脚本和 CI | 机器可读 GitLab release-plan 契约 |
| `gitlab-tag-from-plan --format json` 标准输出 | 脚本和 CI | 机器可读 GitLab tag 契约 |
| `gitlab-release --format json` 标准输出 | 脚本和 CI | 机器可读 GitLab Release 契约 |

实际建议：

- 自动化如果只需要一个值，优先用 `manifest-field`
- 自动化如果需要多个字段，优先读 `.changesets/release-plan.json`
- 不要把终端标题、列对齐空格或中英文文案当作稳定契约

## 3. `status` 输出

`status` 打印的是给人看的发布摘要。

当前命令：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="status --directory /path/to/repo"
```

当前结构：

```text
Repository: /path/to/repo
Current revision: 0.1.0-SNAPSHOT
Latest whole-repo tag: none
Pending changesets: 1
Release plan:
- Release type: minor
- Affected packages: core, api
- Release version: v0.2.0
- Next snapshot: 0.2.0-SNAPSHOT

Changesets:
- 20260418-add-release-notes.md [minor] (packages: core, api) Add release notes generation workflow.
```

当前行为要点：

- 如果没有待发布 changeset，会输出 `Release plan: none`
- 这个命令当前使用英文标题
- 当内部 `type` 是 `other` 时，可见输出里会省略类型文本

自动化建议：

- 不要解析 bullet 文本或空格对齐
- 如果你需要 `releaseVersion` 或 `releaseLevel`，直接读 manifest

## 4. `manifest-field` 输出

`manifest-field` 是最窄、最适合脚本消费的接口。

命令：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="manifest-field --directory /path/to/repo --field releaseVersion"
```

当前行为：

- 读取 `.changesets/release-plan.json`
- 输出目标字段对应的值
- 很适合在 CI 里拼 PR 标题、tag 名称和发布元数据

常用字段：

| 字段 | 含义 |
| --- | --- |
| `releaseVersion` | 不带 `v` 的正式发布版本 |
| `nextSnapshotVersion` | 写回 `pom.xml` 的下一个快照版本 |
| `releaseLevel` | 聚合后的发布级别 |

## 5. `.changesets/release-plan.json`

这是当前最主要的机器可读发布 manifest。

当前结构：

```json
{
  "releaseVersion": "0.2.0",
  "nextSnapshotVersion": "0.2.0-SNAPSHOT",
  "releaseLevel": "minor",
  "generatedAt": "2026-04-19T13:29:58.202943+08:00",
  "changesets": [
    {
      "file": "20260418-add-release-notes.md",
      "release": "minor",
      "type": "other",
      "summary": "Add release notes generation workflow.",
      "modules": ["core", "api"]
    }
  ]
}
```

字段契约：

| 字段 | 含义 |
| --- | --- |
| `releaseVersion` | 不带 `v` 前缀的最终发布版本 |
| `nextSnapshotVersion` | 应用 plan 后推进到的下一个根快照版本 |
| `releaseLevel` | 本次包含的所有 changeset 聚合后的发布级别 |
| `generatedAt` | manifest 生成时间 |
| `changesets[].file` | 原始 changeset 文件名 |
| `changesets[].release` | 该 changeset 的发布升级级别 |
| `changesets[].type` | 兼容旧格式保留的字段，常见值是 `other` |
| `changesets[].summary` | 从 changeset 正文或旧元数据推导出的摘要 |
| `changesets[].modules` | 受影响的 Maven artifactId |

需要注意：

- 为了兼容当前实现，JSON 字段仍然叫 `modules`，即使用户文档里更推荐 `packages`
- `type` 不是发布升级类型，真正有意义的是 `release`

## 6. `.changesets/release-plan.md`

这个文件是给人审阅的 release PR / MR 正文。

当前结构：

```md
## Release Plan

- Release type: `minor`
- Affected packages: `core, api`
- Release version: `v0.2.0`
- Next snapshot: `0.2.0-SNAPSHOT`

## Included Changesets

- `minor` `packages: core, api` Add release notes generation workflow.
```

建议：

- 很适合直接用作 PR 正文
- 不适合作为稳定的机器接口
- 标题和句子文案未来可能调整

## 7. `render-vars` 输出

`render-vars` 用来展示某个 env 文件里的值会如何映射成 GitHub / GitLab 变量与 secrets。

当前命令：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="render-vars --env-file env/release.env.local --platform github"
```

当前 GitHub 结构：

```text
使用 env 文件: env/release.env.local
敏感值默认已打码。传入 --show-secrets true 可显示原值。

== GitHub Actions Variables ==
MAVEN_RELEASE_REPOSITORY_URL             https://repo.example.com/maven-releases/
MAVEN_SNAPSHOT_REPOSITORY_URL            https://repo.example.com/maven-snapshots/
MAVEN_RELEASE_REPOSITORY_ID              maven-releases
MAVEN_SNAPSHOT_REPOSITORY_ID             maven-snapshots

== GitHub Actions Secrets ==
MAVEN_REPOSITORY_USERNAME                PLACEHOLDER
MAVEN_REPOSITORY_PASSWORD                PLACEHOLDER
MAVEN_RELEASE_REPOSITORY_USERNAME        MISSING
```

当前 GitLab 结构：

```text
使用 env 文件: env/release.env.local
敏感值默认已打码。传入 --show-secrets true 可显示原值。

== GitLab CI/CD Variables ==
GITLAB_RELEASE_TOKEN                     OPTIONAL (fallback: CI_JOB_TOKEN)
MAVEN_RELEASE_REPOSITORY_URL             https://repo.example.com/maven-releases/
MAVEN_REPOSITORY_USERNAME                PL****ER
```

当前会出现的值状态：

| 状态 | 含义 |
| --- | --- |
| 原始值 | 当前是有效真实值 |
| `MISSING` | key 不存在 |
| `PLACEHOLDER` | 还是 `replace-me` 这种占位值 |
| 类似 `ab****yz` 的掩码值 | secret 存在，但默认被打码 |

自动化建议：

- 这个输出只是给操作者预览
- 真正的 source of truth 仍然是 env 文件本身，不是这张终端表格

JSON 模式：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="render-vars --env-file env/release.env.local --platform github --format json"
```

当前 JSON 结构：

```json
{
  "ok": true,
  "command": "render-vars",
  "envFile": "env/release.env.local",
  "platform": "github",
  "showSecrets": false,
  "sections": [
    {
      "title": "GitHub Actions Variables",
      "entries": [
        {
          "label": "MAVEN_RELEASE_REPOSITORY_URL",
          "value": "https://repo.example.com/maven-releases/"
        }
      ]
    }
  ]
}
```

契约说明：

- 标准输出只包含一个 JSON 对象
- 退出码 `0` 表示命令成功
- `sections[].entries[].label` 表示字段名，`value` 表示渲染后的值或状态词

## 8. `doctor-local` 输出

`doctor-local` 会检查本地运行时、env 完整性、平台 CLI 登录状态，以及可选的仓库标识。

当前命令：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="doctor-local --env-file env/release.env.local"
```

当前分段结构：

```text
== 本机运行时 ==
java -version                            OK
./mvnw                                   MISSING
mvn                                      OK
Maven command                           mvn (system)
mvn -q -version                         OK

== 本地 env 文件 ==
env/release.env.local                    OK
MAVEN_REPOSITORY_USERNAME                PLACEHOLDER

== 平台 CLI ==
gh                                       OK
gh auth status                           FAILED
glab                                     MISSING

== 仓库标识 ==
GITHUB_REPO                              NOT_SET
GITLAB_REPO                              NOT_SET
```

当前状态词：

| 状态 | 含义 |
| --- | --- |
| `OK` | 校验通过 |
| `MISSING` | 必需工具、文件或值不存在 |
| `FAILED` | 命令存在，但校验失败 |
| `SKIPPED` | 依赖检查没通过，因此跳过 |
| `PLACEHOLDER` | 值仍然是占位数据 |
| `OPTIONAL` | key 缺失，但本来就是可选的 |
| `NOT_SET` | 可选仓库标识参数没有传 |
| `INVALID` | 传入的仓库标识格式不合法 |

当前实现里一个重要细节：

- 这里会优先使用仓库内的 `./mvnw`
- 如果仓库里没有 wrapper，会回退到系统安装的 `mvn`
- 失败时最后会打印一段面向人的处理建议，并抛错退出

JSON 模式：

```bash
mvn -q -DskipTests compile exec:java -Dexec.args="doctor-local --env-file env/release.env.local --format json"
```

当前 JSON 行为：

- 标准输出只包含一个 JSON 对象
- 退出码 `0` 表示本地检查全部通过
- 非 `0` 退出码表示至少一个必需检查失败
- 返回值会包含 `sections`，失败时还可能包含 `suggestions` 和最终的 `error`

## 9. `doctor-platform` 和 `audit-vars`

`doctor-platform` 用来在真正 sync 或 audit 之前，确认本地 env 和平台 CLI 鉴权是否已经准备好。

当前结构：

```text
使用 env 文件: env/release.env.local

== 本地 env 检查 ==
MAVEN_RELEASE_REPOSITORY_URL             OK
MAVEN_REPOSITORY_USERNAME                PLACEHOLDER

== GitHub CLI 检查 ==
gh                                       OK
gh auth status                           FAILED
```

`audit-vars` 则是把本地值和远端平台状态做对比。

当前审计状态词：

| 状态 | 含义 |
| --- | --- |
| `MATCH` | 远端值与本地值一致 |
| `PRESENT` | 远端 secret 已存在 |
| `REMOTE_ONLY` | 远端有值，但本地输入为空或仍是占位值 |
| `SKIPPED` | 没有可比较的有效值 |
| `MISSING_REMOTE` | 本地是真实值，但远端不存在 |
| `MISMATCH` | 本地值和远端值不一致 |

`doctor-platform --format json` 的行为和 `doctor-local --format json` 类似：

- 标准输出只包含一个 JSON 对象
- 退出码 `0` 表示 env 与所选平台检查通过
- 非 `0` 退出码表示鉴权、仓库标识或必需 env 值校验失败
- 返回值会包含 `platform`、`sections`，失败时可能包含最终 `error`

`audit-vars --format json` 现在也提供机器可读契约：

- 标准输出只包含一个 JSON 对象
- 退出码 `0` 表示所有远端变量都符合本地期望状态
- 非 `0` 退出码表示至少一个审计项出现了 `MISSING_REMOTE` / `MISMATCH`，或者平台前置条件失败
- 返回值会包含 `platform`、`sections`，失败时可能包含最终 `error`

GitLab 额外增强：

- `doctor-platform --platform gitlab --format json` 现在会额外返回 protected variables 和 protected branches 检查分组
- 如果存在 protected variables，但配置的 `snapshotBranch` 没有被保护，命令会明确失败，而不是静默通过

## 10. `publish` 和 GitLab 发布 JSON 契约

`preflight`、`publish`、`gitlab-release-plan`、`gitlab-tag-from-plan`、`gitlab-release` 现在共享一组稳定的顶层字段。

当前公共字段：

| 字段 | 含义 |
| --- | --- |
| `ok` | 命令是否成功 |
| `command` | 命令名 |
| `action` | 实际执行或计划执行的动作 |
| `skipped` | 是否明确跳过了工作 |
| `reason` | 跳过原因、dry-run 提示或成功摘要 |
| `releaseVersion` | 已解析好的发布版本 |
| `effectiveVersion` | 实际传给发布链路的版本，已经包含 snapshot mode 的决策结果 |
| `releaseModule` | 已解析好的模块名，whole-repo 时为 `null` |
| `tag` | 相关发布 tag |
| `tagStrategy` | 相关场景下解析出的 tag 策略 |
| `tags` | 相关场景下解析出的发布 tag 列表 |
| `releaseNotesFile` | 相关 release notes 文件路径 |
| `projectId` | 相关 GitLab project id |
| `snapshotVersionMode` | 当前命令在处理 snapshot 时所用的版本模式 |
| `snapshotBuildStampApplied` | `javachanges` 是否真的应用了 snapshot build stamp |

示例：

```json
{
  "ok": true,
  "command": "gitlab-release",
  "action": "create-release",
  "skipped": false,
  "reason": "Created GitLab Release.",
  "releaseVersion": "1.2.3",
  "effectiveVersion": "1.2.3",
  "releaseModule": "core",
  "tag": "core/v1.2.3",
  "tagStrategy": null,
  "tags": null,
  "releaseNotesFile": "/path/to/repo/target/release-notes.md",
  "projectId": "12345",
  "execute": true,
  "dryRun": false,
  "snapshotVersionMode": null,
  "snapshotBuildStampApplied": false
}
```

snapshot 专用示例：

```json
{
  "ok": true,
  "command": "preflight",
  "action": "publish-snapshot",
  "skipped": false,
  "reason": "Preflight checks passed.",
  "releaseVersion": "1.2.3-SNAPSHOT",
  "effectiveVersion": "1.2.3-SNAPSHOT",
  "releaseModule": null,
  "tag": null,
  "releaseNotesFile": null,
  "projectId": null,
  "execute": false,
  "dryRun": true,
  "snapshotVersionMode": "plain",
  "snapshotBuildStampApplied": false
}
```

## 11. 自动化建议

对 CI 和脚本来说：

1. 用 `plan --apply true` 生成 manifest
2. 用 `manifest-field` 或 `release-plan.json` 读取 `releaseVersion`、`releaseLevel` 这类字段
3. 如果脚本需要结构化诊断，使用 `render-vars --format json`、`doctor-local --format json`、`doctor-platform --format json`、`audit-vars --format json`
4. 发布计划相关数据继续优先读 manifest，诊断类 JSON 只用于环境校验与审计流程

避免这样做：

- 解析列对齐空格
- 依赖 `== 本地 env 检查 ==` 这种标题文本
- 依赖失败总结段落里的自然语言表述

## 12. 相关阅读

| 需求 | 文档 |
| --- | --- |
| 完整命令列表 | [CLI Reference](./cli-reference.md) |
| 生成的 manifest 字段说明 | [Release Plan Manifest](./release-plan-manifest.md) |
| 常见命令实战 | [Command Cookbook](./command-cookbook.md) |
| 故障排查 | [Troubleshooting Guide](./troubleshooting-guide.md) |
