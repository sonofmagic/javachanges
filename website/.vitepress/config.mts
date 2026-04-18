import { readdirSync } from 'node:fs'
import { resolve } from 'node:path'
import { defineConfig } from 'vitepress'

const repoUrl = 'https://github.com/sonofmagic/javachanges'
const docsDir = resolve(__dirname, '../../docs')
const docsFiles = readdirSync(docsDir).filter((file) => file.endsWith('.md'))

const zhRewrites = Object.fromEntries(
  docsFiles
    .filter((file) => file.endsWith('.zh-CN.md'))
    .map((file) => [file, `zh-CN/${file.replace('.zh-CN.md', '.md')}`]),
)

const guideItems = [
  { text: 'Overview', link: '/' },
  { text: 'Getting Started', link: '/getting-started' },
  { text: 'Development Guide', link: '/development-guide' },
  { text: 'GitHub Actions Release Flow', link: '/github-actions-release' },
  { text: 'GitHub Actions Usage Guide', link: '/github-actions-guide' },
  { text: 'GitLab CI/CD Usage Guide', link: '/gitlab-ci-guide' },
  { text: 'Publish To Maven Central', link: '/publish-to-maven-central' },
  { text: 'Use Cases', link: '/use-cases' },
] as const

const zhGuideItems = [
  { text: '概览', link: '/zh-CN/' },
  { text: '快速开始', link: '/zh-CN/getting-started' },
  { text: '开发指南', link: '/zh-CN/development-guide' },
  { text: 'GitHub Actions 发布流程', link: '/zh-CN/github-actions-release' },
  { text: 'GitHub Actions 使用指南', link: '/zh-CN/github-actions-guide' },
  { text: 'GitLab CI/CD 使用指南', link: '/zh-CN/gitlab-ci-guide' },
  { text: '发布到 Maven Central', link: '/zh-CN/publish-to-maven-central' },
  { text: '使用场景', link: '/zh-CN/use-cases' },
] as const

export default defineConfig({
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
    },
  },
  themeConfig: {
    logo: '/mark.svg',
    siteTitle: 'javachanges',
    search: {
      provider: 'local',
    },
    nav: [
      { text: 'Docs', link: '/getting-started' },
      { text: 'GitHub', link: repoUrl },
    ],
    sidebar: [
      {
        text: 'Guides',
        items: guideItems,
      },
    ],
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
    locales: {
      'zh-CN': {
        nav: [
          { text: '文档', link: '/zh-CN/getting-started' },
          { text: 'GitHub', link: repoUrl },
        ],
        sidebar: [
          {
            text: '指南',
            items: zhGuideItems,
          },
        ],
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
      },
    },
  },
})
