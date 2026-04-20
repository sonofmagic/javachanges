# Cloudflare Workers Builds

Use this guide when you want Cloudflare to pull `sonofmagic/javachanges` directly from GitHub and deploy the docs site without any GitHub-side deploy workflow.

## 1. Why use Workers Builds here

This repository already ships a Wrangler config for static assets:

- Worker config: `wrangler.jsonc`
- Static output directory: `website/dist`
- Custom domain: `javachanges.icebreaker.top`

That makes Cloudflare Workers Builds a better fit than a separate GitHub Actions deploy pipeline:

- deployment credentials stay inside Cloudflare
- GitHub no longer needs `CLOUDFLARE_API_TOKEN`
- pushes to `main` can build and deploy from the same platform that hosts the Worker

## 2. Repository-side requirements

Keep these files in the repository root:

- `package.json`
- `pnpm-lock.yaml`
- `wrangler.jsonc`

Current important values:

- Worker name: `javachanges-docs`
- Build command: `pnpm docs:build`
- Static assets directory in Wrangler: `./website/dist`
- Node.js version: `22` via `.node-version` and `package.json#engines`

Important rule:

- if you connect Cloudflare to an existing Worker, the Worker selected in the dashboard must match `name` in `wrangler.jsonc`

## 3. Dashboard setup

In Cloudflare Dashboard:

1. Open `Workers & Pages`
2. Open or create the Worker for this site
3. Enable `Builds`
4. Connect the Git repository `sonofmagic/javachanges`

Recommended build configuration:

| Field | Value |
| --- | --- |
| Production branch | `main` |
| Root directory | `.` |
| Build command | `pnpm docs:build` |
| Deploy command | `npx wrangler deploy` |

Notes:

- keep the deploy command on the Cloudflare side; do not run a second deploy workflow from GitHub
- the build runs from the repository root because both `package.json` and `wrangler.jsonc` live there
- `pnpm docs:build` writes the VitePress output into `website/dist`, and Wrangler uploads that directory as static assets

## 4. Custom domain

This repository already expects the custom domain route below:

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

After the first successful build:

1. Confirm the Worker has the route `javachanges.icebreaker.top`
2. Confirm the DNS record in Cloudflare points at the Worker-managed custom domain
3. Open `https://javachanges.icebreaker.top/`
4. Check both `/` and `/zh-CN/`

## 5. Secrets and variables

For Workers Builds, keep deployment credentials in Cloudflare instead of GitHub.

That means:

- GitHub repository secrets for docs deployment are no longer required
- Cloudflare handles the deploy token internally for the connected build pipeline

For this repository, no extra build-time environment variables are currently required for the docs site.

The docs build also resolves the displayed release version from the checked-in `CHANGELOG.md`, so Cloudflare does not need live access to Maven Central just to render the site.

## 6. Recommended cleanup

When Workers Builds is active:

- remove any GitHub Actions workflow dedicated to docs deployment
- keep CI focused on validation only
- keep Wrangler config in version control so the deployment shape stays reviewable

## 7. Verification checklist

- `pnpm docs:build` passes locally
- Cloudflare Build uses `main`
- Worker name in Cloudflare matches `javachanges-docs`
- Deploy command is `npx wrangler deploy`
- `https://javachanges.icebreaker.top/` opens successfully
- the locale switcher works for English and Simplified Chinese

## 8. Related guides

| Need | Document |
| --- | --- |
| Local docs development | [Development Guide](./development-guide.md) |
| VitePress site structure | [Configuration Reference](./configuration-reference.md) |
| Overall docs index | [Overview](./index.md) |
