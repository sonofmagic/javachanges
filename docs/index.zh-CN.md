# javachanges

[English](./index.md) | [简体中文](./index.zh-CN.md)

`javachanges` 是一个面向 Maven Monorepo 和单模块 Maven 仓库的发布规划 CLI。

整个工作流保持简单：

1. 开发者在 `.changesets/*.md` 中记录准备发布的变更
2. CI 或维护者查看生成的 release plan
3. release plan 更新根版本和 changelog
4. 发布辅助命令准备 Maven settings 和部署命令

这个工具保持文件驱动，不依赖数据库或托管服务。

## 核心理念

- 把发布意图保存在可版本控制的文件里
- 在真正发布前先审阅 release plan
- 从结构化元数据生成 changelog
- 尽量减少脆弱、难维护的 shell 发布脚本

## CLI 假设

- 一个带根 `pom.xml` 的 Maven 仓库
- 根 `pom.xml` 中要么有 `<modules>`，要么是单模块根 artifact
- 用于版本管理的根 `revision` 属性
- 用来存放进行中发布记录的 `.changesets/` 目录

## 指南

- [Getting Started](./getting-started.md)
- [Development Guide](./development-guide.md)
- [GitHub Actions Release Flow](./github-actions-release.md)
- [GitHub Actions Usage Guide](./github-actions-guide.md)
- [GitLab CI/CD Usage Guide](./gitlab-ci-guide.md)
- [Publish To Maven Central](./publish-to-maven-central.md)
- [Use Cases](./use-cases.md)
