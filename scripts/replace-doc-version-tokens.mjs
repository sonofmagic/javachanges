#!/usr/bin/env node

import { readFile, readdir, writeFile } from 'node:fs/promises'
import { existsSync } from 'node:fs'
import { extname, relative, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const scriptDir = fileURLToPath(new URL('.', import.meta.url))
const repoRoot = resolve(scriptDir, '..')
const distDir = resolve(repoRoot, 'website/dist')
const checkOnly = process.argv.includes('--check')
const textExtensions = new Set([
  '.css',
  '.html',
  '.js',
  '.json',
  '.md',
  '.mjs',
  '.txt',
  '.xml',
])

function extractPomRevision(xml) {
  const match = xml.match(/<revision>([^<]+)<\/revision>/)
  if (!match?.[1]) {
    throw new Error('Unable to resolve <revision> from pom.xml')
  }
  return match[1].trim()
}

function extractLatestReleaseVersion(changelog) {
  const match = changelog.match(/^##\s+(\d+\.\d+\.\d+)\s+-\s+\d{4}-\d{2}-\d{2}$/m)
  if (!match?.[1]) {
    throw new Error('Unable to resolve latest release version from CHANGELOG.md')
  }
  return match[1]
}

async function resolveVersions() {
  const releaseOverride = process.env.JAVACHANGES_DOCS_RELEASE_VERSION?.trim()
  const snapshotOverride = process.env.JAVACHANGES_DOCS_SNAPSHOT_VERSION?.trim()
  const pom = await readFile(resolve(repoRoot, 'pom.xml'), 'utf8')
  const changelog = await readFile(resolve(repoRoot, 'CHANGELOG.md'), 'utf8')

  return {
    latestReleaseVersion: releaseOverride || extractLatestReleaseVersion(changelog),
    currentSnapshotVersion: snapshotOverride || extractPomRevision(pom),
    centralOverviewUrl: 'https://central.sonatype.com/artifact/io.github.sonofmagic/javachanges/overview',
  }
}

function replaceDocVersionTokens(source, versions) {
  return source
    .replaceAll('__JAVACHANGES_LATEST_RELEASE_VERSION__', versions.latestReleaseVersion)
    .replaceAll('__JAVACHANGES_CURRENT_SNAPSHOT_VERSION__', versions.currentSnapshotVersion)
    .replaceAll('__JAVACHANGES_CENTRAL_OVERVIEW_URL__', versions.centralOverviewUrl)
}

async function listTextFiles(root) {
  const entries = await readdir(root, { withFileTypes: true })
  const files = []
  for (const entry of entries) {
    const path = resolve(root, entry.name)
    if (entry.isDirectory()) {
      files.push(...await listTextFiles(path))
    } else if (entry.isFile() && textExtensions.has(extname(entry.name))) {
      files.push(path)
    }
  }
  return files
}

async function main() {
  if (!existsSync(distDir)) {
    throw new Error('website/dist does not exist. Run pnpm docs:build first.')
  }

  const versions = await resolveVersions()
  const files = await listTextFiles(distDir)
  const changed = []
  const unresolved = []

  for (const file of files) {
    const source = await readFile(file, 'utf8')
    const updated = replaceDocVersionTokens(source, versions)
    if (updated !== source) {
      changed.push(relative(repoRoot, file))
      if (!checkOnly) {
        await writeFile(file, updated, 'utf8')
      }
    }
    const remaining = updated.match(/__JAVACHANGES_[A-Z_]+__/g)
    if (remaining) {
      unresolved.push(`${relative(repoRoot, file)}: ${Array.from(new Set(remaining)).join(', ')}`)
    }
  }

  if (checkOnly && changed.length > 0) {
    throw new Error(`Documentation output contains unreplaced version tokens:\n${changed.join('\n')}`)
  }

  if (unresolved.length > 0) {
    throw new Error(`Documentation output contains unknown javachanges tokens:\n${unresolved.join('\n')}`)
  }

  const action = checkOnly ? 'Checked' : 'Updated'
  console.log(`${action} ${files.length} documentation output files; ${changed.length} file(s) contained version tokens.`)
}

main().catch((error) => {
  console.error(error.message)
  process.exit(1)
})
