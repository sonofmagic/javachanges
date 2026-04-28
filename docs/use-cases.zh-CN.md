# 使用场景


## Maven 库 Monorepo

用 `javachanges` 管理多个 artifact 的版本升级，同时保持统一、可审阅的 release plan。

## 单模块 Maven CLI 或库

即使仓库里只有一个可发布的 Maven artifact，也可以用 `javachanges` 保持文件驱动的发布工作流。

## Gradle 多项目构建

用 `javachanges` 管理 `include(...)` 声明的多个 Gradle project，推进 `gradle.properties` 版本，并在 Gradle 发布前生成 release-plan manifest。

配置细节见 [Gradle 使用指南](./gradle-guide.md)。

## 单项目 Gradle 库或应用

即使 Gradle 仓库只有一个 root project，也可以用 `javachanges` 获得可审阅的 release notes、changelog 和 CI tag 流程。

## 内部平台发布自动化

使用 `write-settings`、`render-vars`、`doctor-platform` 和 `audit-vars`，在多个仓库之间统一 CI 变量和 Maven 凭据配置。

如果你使用的是 GitHub Actions，可以继续看 [GitHub Actions Usage Guide](./github-actions-guide.md)。

## GitLab release MR 流程

当你希望根据 pending changesets 自动生成 release branch、merge request 和 tag 时，可以使用 `gitlab-release-plan` 和 `gitlab-tag-from-plan`。

如果你需要完整的 GitLab CI/CD pipeline 示例，可以继续看 [GitLab CI/CD Usage Guide](./gitlab-ci-guide.md)。

## 更安全的发布 dry-run

使用 `preflight` 和 `publish --execute false`，在真正触达目标仓库之前先预览准确的 Maven deploy 命令和生成的 settings 文件。

Gradle artifacts 请把 release-plan manifest 作为 `./gradlew publish` 的输入，发布逻辑仍然保留在 Gradle 构建里。
