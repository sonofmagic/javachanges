---
description: javachanges 面向 AI 与代理系统公开的文档入口，包括 llms.txt 与完整文档聚合文件。
---

# AI 文档入口

`javachanges` 现在提供了一组稳定的 AI 文档地址，方便大模型、抓取器和代理工作流直接读取。

## 公开地址

| 资源 | URL | 作用 |
| --- | --- | --- |
| 入口页 | `https://javachanges.icebreaker.top/zh-CN/llms-access` | 面向人工阅读的 AI 文档索引页 |
| 精简索引 | `https://javachanges.icebreaker.top/llms.txt` | 指向主要英文文档的轻量链接列表 |
| 完整聚合 | `https://javachanges.icebreaker.top/llms-full.txt` | 整套英文文档的单文件导出 |

## 说明

- `llms.txt` 适合做发现和轻量抓取。
- `llms-full.txt` 适合离线导入、长上下文代理和文档同步任务。
- 当前生成的 LLM 文档聚合只包含英文文档。
- 中文文档仍然会正常发布在站点中，但不会被并入生成的 LLM 聚合文件，避免双语重复上下文。

## 推荐用法

如果 AI 只需要先拿到站点索引，优先使用：

```text
https://javachanges.icebreaker.top/llms.txt
```

如果 AI 需要一次性获取完整文档语料，使用：

```text
https://javachanges.icebreaker.top/llms-full.txt
```

如果需要正常按页面浏览文档，使用站点首页：

```text
https://javachanges.icebreaker.top/
```
