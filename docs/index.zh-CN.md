---
layout: home
title: javachanges
titleTemplate: false
hero:
  name: javachanges
  text: 面向 Maven 仓库的发布规划
  tagline: 用文件记录 changesets，自动生成 changelog，并接入 CI/CD 友好的 Maven 发布流程。
  image:
    src: /logo-horizontal.svg
    alt: javachanges logo
  actions:
    - theme: brand
      text: 快速开始
      link: /zh-CN/getting-started
    - theme: alt
      text: GitHub
      link: https://github.com/sonofmagic/javachanges
features:
  - title: 文件驱动的发布意图
    details: 用官方 Changesets 风格的 `.changesets/*.md` package map 记录发布内容，而不是散落在表格、聊天或脚本里。
  - title: 面向 Maven 的工作流
    details: 围绕根 `revision` 规划版本、生成 changelog，并兼容 monorepo 与单模块仓库。
  - title: 易于自动化
    details: 可接入 GitHub Actions、GitLab CI/CD、Maven Central 发布和变量同步流程。
---

# javachanges

[English](/) | [简体中文](/zh-CN/)

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
- 从兼容 Changesets 的结构化元数据生成 changelog
- 尽量减少脆弱、难维护的 shell 发布脚本

## CLI 假设

- 一个带根 `pom.xml` 的 Maven 仓库
- 根 `pom.xml` 中要么有 `<modules>`，要么是单模块根 artifact
- 用于版本管理的根 `revision` 属性
- 用来存放进行中发布记录的 `.changesets/` 目录

## 指南

- [Getting Started](./getting-started.md)
- [配置参考大全](./configuration-reference.md)
- [CLI 命令参考](./cli-reference.md)
- [Development Guide](./development-guide.md)
- [Release Plan Manifest 说明](./release-plan-manifest.md)
- [GitHub Actions Release Flow](./github-actions-release.md)
- [GitHub Actions Usage Guide](./github-actions-guide.md)
- [GitLab CI/CD Usage Guide](./gitlab-ci-guide.md)
- [Publish To Maven Central](./publish-to-maven-central.md)
- [Use Cases](./use-cases.md)
