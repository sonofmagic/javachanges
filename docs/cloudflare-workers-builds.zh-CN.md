# Cloudflare Workers Builds 配置指南

如果你希望由 Cloudflare 直接拉取 `sonofmagic/javachanges` GitHub 仓库并部署文档站，而不是继续依赖 GitHub Actions，这份文档就是给你的。

## 1. 为什么这里更适合 Workers Builds

这个仓库已经具备基于 Wrangler 的静态资源部署配置：

- Worker 配置：`wrangler.jsonc`
- 静态输出目录：`website/dist`
- 自定义域名：`javachanges.icebreaker.top`

因此这里更适合直接使用 Cloudflare Workers Builds，而不是额外保留一条 GitHub Actions 部署流水线：

- 部署凭据留在 Cloudflare 侧管理
- GitHub 不再需要 `CLOUDFLARE_API_TOKEN`
- `main` 分支的 push 可以直接在最终托管平台上完成构建和部署

## 2. 仓库侧需要保留的内容

这些文件需要继续放在仓库根目录：

- `package.json`
- `pnpm-lock.yaml`
- `wrangler.jsonc`

当前关键值：

- Worker 名称：`javachanges-docs`
- 构建命令：`pnpm docs:build`
- Wrangler 中的静态资源目录：`./website/dist`
- Node.js 版本：通过 `.node-version` 和 `package.json#engines` 固定为 `22`

一个关键约束：

- 如果你在 Cloudflare Dashboard 里连接的是“已有 Worker”，那么 Dashboard 中选中的 Worker 名称必须和 `wrangler.jsonc` 里的 `name` 一致

## 3. Dashboard 配置方法

在 Cloudflare Dashboard 里：

1. 打开 `Workers & Pages`
2. 打开或创建这个站点对应的 Worker
3. 启用 `Builds`
4. 连接 Git 仓库 `sonofmagic/javachanges`

推荐的构建配置如下：

| 字段 | 建议值 |
| --- | --- |
| Production branch | `main` |
| Root directory | `.` |
| Build command | `pnpm docs:build` |
| Deploy command | `npx wrangler deploy` |

说明：

- 部署命令放在 Cloudflare 一侧执行，不再需要 GitHub 上再跑一遍部署 workflow
- 这里的构建根目录必须是仓库根目录，因为 `package.json` 和 `wrangler.jsonc` 都在这里
- `pnpm docs:build` 会把 VitePress 结果输出到 `website/dist`，然后 Wrangler 会把这个目录作为静态资源上传

## 4. 自定义域名

当前仓库已经把自定义域名 route 写进了 Wrangler 配置：

```jsonc
{
  "routes": [
    {
      "pattern": "javachanges.icebreaker.top",
      "custom_domain": true
    }
  ]
}
```

第一次构建成功后，检查这几项：

1. 确认 Worker 上已经绑定 `javachanges.icebreaker.top`
2. 确认 Cloudflare DNS 记录已经指向这个 Worker 管理的 custom domain
3. 打开 `https://javachanges.icebreaker.top/`
4. 同时检查 `/` 和 `/zh-CN/`

## 5. Secrets 与环境变量

如果使用 Workers Builds，文档部署凭据应当放在 Cloudflare，而不是 GitHub。

这意味着：

- GitHub 仓库里不再需要文档部署用的 secrets
- Cloudflare 会在连接好的构建流水线里处理部署凭据

对于当前这个文档站，暂时不需要额外的 build-time 环境变量。

文档页里展示的正式版号也直接从仓库内提交的 `CHANGELOG.md` 读取，不再依赖构建时实时请求 Maven Central，因此 Cloudflare 构建会更稳定。

## 6. 推荐收口方式

当 Workers Builds 已经接管部署后：

- 删除专门负责 docs 部署的 GitHub Actions workflow
- GitHub CI 只保留校验职责
- 继续把 Wrangler 配置保存在仓库里，保证部署形态可审阅、可追踪

## 7. 检查清单

- 本地 `pnpm docs:build` 能通过
- Cloudflare Build 的生产分支是 `main`
- Cloudflare 中的 Worker 名称与 `javachanges-docs` 一致
- Deploy command 使用 `npx wrangler deploy`
- `https://javachanges.icebreaker.top/` 能正常打开
- 英文和简体中文语言切换都正常

## 8. 相关阅读

| 需求 | 文档 |
| --- | --- |
| 本地文档开发 | [Development Guide](./development-guide.md) |
| VitePress 站点结构 | [Configuration Reference](./configuration-reference.md) |
| 文档总览 | [Overview](./index.md) |
