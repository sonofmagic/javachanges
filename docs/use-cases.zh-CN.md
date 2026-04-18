# 使用场景

[English](./use-cases.md) | [简体中文](./use-cases.zh-CN.md)

## Maven 库 Monorepo

用 `javachanges` 管理多个 artifact 的版本升级，同时保持统一、可审阅的 release plan。

## 单模块 Maven CLI 或库

即使仓库里只有一个可发布的 Maven artifact，也可以用 `javachanges` 保持文件驱动的发布工作流。

## 内部平台发布自动化

使用 `write-settings`、`render-vars`、`doctor-platform` 和 `audit-vars`，在多个仓库之间统一 CI 变量和 Maven 凭据配置。

## GitLab release MR 流程

当你希望根据 pending changesets 自动生成 release branch、merge request 和 tag 时，可以使用 `gitlab-release-plan` 和 `gitlab-tag-from-plan`。

## 更安全的发布 dry-run

使用 `preflight` 和 `publish --execute false`，在真正触达目标仓库之前先预览准确的 Maven deploy 命令和生成的 settings 文件。
