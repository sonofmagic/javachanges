import { readdirSync } from 'node:fs'
import { resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { defineConfigWithTheme } from 'vitepress'
import type { DefaultTheme } from 'vitepress'

const repoUrl = 'https://github.com/sonofmagic/javachanges'
const docsDir = resolve(fileURLToPath(new URL('../../docs', import.meta.url)))
const docsFiles = readdirSync(docsDir).filter((file) => file.endsWith('.md'))

const zhRewrites = Object.fromEntries(
  docsFiles
    .filter((file) => file.endsWith('.zh-CN.md'))
    .map((file) => [file, `zh-CN/${file.replace('.zh-CN.md', '.md')}`]),
)

const guideItems: DefaultTheme.SidebarItem[] = [
  {
    text: 'Essentials',
    items: [
      { text: 'Overview', link: '/' },
      { text: 'Getting Started', link: '/getting-started' },
      { text: 'Development Guide', link: '/development-guide' },
      { text: 'Use Cases', link: '/use-cases' },
    ],
  },
  {
    text: 'Reference',
    items: [
      { text: 'Configuration Reference', link: '/configuration-reference' },
      { text: 'CLI Reference', link: '/cli-reference' },
      { text: 'Release Plan Manifest', link: '/release-plan-manifest' },
      { text: 'Publish To Maven Central', link: '/publish-to-maven-central' },
    ],
  },
  {
    text: 'Automation',
    items: [
      { text: 'GitHub Actions Release Flow', link: '/github-actions-release' },
      { text: 'GitHub Actions Usage Guide', link: '/github-actions-guide' },
      { text: 'GitLab CI/CD Usage Guide', link: '/gitlab-ci-guide' },
    ],
  },
]

const zhGuideItems: DefaultTheme.SidebarItem[] = [
  {
    text: '基础',
    items: [
      { text: '概览', link: '/zh-CN/' },
      { text: '快速开始', link: '/zh-CN/getting-started' },
      { text: '开发指南', link: '/zh-CN/development-guide' },
      { text: '使用场景', link: '/zh-CN/use-cases' },
    ],
  },
  {
    text: '参考',
    items: [
      { text: '配置参考大全', link: '/zh-CN/configuration-reference' },
      { text: 'CLI 命令参考', link: '/zh-CN/cli-reference' },
      { text: 'Release Plan Manifest', link: '/zh-CN/release-plan-manifest' },
      { text: '发布到 Maven Central', link: '/zh-CN/publish-to-maven-central' },
    ],
  },
  {
    text: '自动化',
    items: [
      { text: 'GitHub Actions 发布流程', link: '/zh-CN/github-actions-release' },
      { text: 'GitHub Actions 使用指南', link: '/zh-CN/github-actions-guide' },
      { text: 'GitLab CI/CD 使用指南', link: '/zh-CN/gitlab-ci-guide' },
    ],
  },
]

const rootThemeConfig: DefaultTheme.Config = {
  logo: '/mark.svg',
  siteTitle: 'javachanges',
  search: {
    provider: 'local',
  },
  nav: [
    { text: 'Docs', link: '/getting-started' },
    { text: 'GitHub', link: repoUrl },
  ],
  sidebar: guideItems,
  socialLinks: [
    { icon: 'github', link: repoUrl },
  ],
  editLink: {
    pattern: `${repoUrl}/edit/main/docs/:path`,
    text: 'Edit this page on GitHub',
  },
  outlineTitle: 'On this page',
  lastUpdatedText: 'Last updated',
  docFooter: {
    prev: 'Previous page',
    next: 'Next page',
  },
  footer: {
    message: 'Released under the Apache-2.0 License.',
    copyright: 'Copyright © 2026 sonofmagic',
  },
}

const zhThemeConfig: DefaultTheme.Config = {
  nav: [
    { text: '文档', link: '/zh-CN/getting-started' },
    { text: 'GitHub', link: repoUrl },
  ],
  sidebar: zhGuideItems,
  editLink: {
    pattern: `${repoUrl}/edit/main/docs/:path`,
    text: '在 GitHub 上编辑此页',
  },
  outlineTitle: '本页内容',
  lastUpdatedText: '最近更新',
  docFooter: {
    prev: '上一页',
    next: '下一页',
  },
  footer: {
    message: '基于 Apache-2.0 License 发布。',
    copyright: 'Copyright © 2026 sonofmagic',
  },
}

export default defineConfigWithTheme<DefaultTheme.Config>({
  srcDir: '../docs',
  srcExclude: ['README.md'],
  outDir: '../website/dist',
  cleanUrls: true,
  lastUpdated: true,
  sitemap: {
    hostname: 'https://javachanges.icebreaker.top',
  },
  rewrites: zhRewrites,
  title: 'javachanges',
  titleTemplate: ':title | javachanges',
  description: 'Changesets-style release planning for Maven repositories.',
  head: [
    ['link', { rel: 'icon', href: '/mark.svg' }],
    ['meta', { name: 'theme-color', content: '#b54a1d' }],
  ],
  locales: {
    root: {
      label: 'English',
      lang: 'en-US',
      title: 'javachanges',
      description: 'Changesets-style release planning for Maven repositories.',
    },
    'zh-CN': {
      label: '简体中文',
      lang: 'zh-CN',
      title: 'javachanges',
      description: '面向 Maven 仓库的 Changesets 风格发布规划工具。',
      themeConfig: zhThemeConfig,
    },
  },
  themeConfig: rootThemeConfig,
})
