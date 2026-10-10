import fs from 'node:fs/promises'
import path from 'node:path'
import { execFileSync } from 'node:child_process'
import { readPomVersion } from './version.mjs'

async function optionalText(file) {
  try { return await fs.readFile(file, 'utf8') }
  catch (error) {
    if (error.code === 'ENOENT') return null
    throw error
  }
}

// 改名工程使用自己的目录前缀；不能从某个前端 package.json 猜测发行版本。
export async function readReleaseProject(root) {
  const raw = await optionalText(path.join(root, 'forge.config.json'))
  const config = raw === null ? {} : JSON.parse(raw)
  if (!config || typeof config !== 'object' || Array.isArray(config)) throw new Error('forge.config.json 格式非法')
  const projectName = config.projectName || 'forge'
  const artifactPrefix = config.artifactPrefix || 'forge'
  for (const value of [projectName, artifactPrefix]) {
    if (!/^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$/.test(value)) throw new Error('版本目录前缀非法')
  }
  const version = await readPomVersion(path.join(root, `${artifactPrefix}-server/pom.xml`))
  return { version, projectName, artifactPrefix }
}

export async function frontendVersionChanges(root) {
  const project = await readReleaseProject(root)
  const changes = []
  for (const frontend of ['admin-ui', 'h5-ui', 'report-ui']) {
    const file = path.join(root, `${project.projectName}-${frontend}/package.json`)
    const source = await optionalText(file)
    if (source === null) continue
    const manifest = JSON.parse(source)
    if (manifest.version !== project.version) {
      changes.push({ file, manifest, expected: project.version })
    }
  }
  return changes
}

export function releaseNotes(markdown, version) {
  if (!markdown) return null
  const lines = markdown.split(/\r?\n/)
  const start = lines.findIndex(line => line.startsWith(`## [${version}]`))
  if (start < 0) return null
  const end = lines.findIndex((line, index) => index > start && /^## /.test(line))
  // 随包说明只显示精确版本段，不能拿 Unreleased 或旧版日志充当当前版本。
  return lines.slice(start, end < 0 ? undefined : end).join('\n').trim()
}

function gitRevision(root) {
  try {
    const commit = execFileSync('git', ['rev-parse', '--verify', 'HEAD'], {
      cwd: root, encoding: 'utf8', timeout: 3000, stdio: ['ignore', 'pipe', 'ignore'],
    }).trim()
    return /^[a-f0-9]{40,64}$/.test(commit) ? commit : null
  }
  catch { return null }
}

export async function createReleaseInfo(root, client) {
  const { version } = await readReleaseProject(root)
  return {
    version,
    client,
    commit: gitRevision(root),
    builtAt: new Date().toISOString(),
    notes: releaseNotes(await optionalText(path.join(root, 'CHANGELOG.md')), version),
  }
}

export function pluginReleaseInfo(root, client) {
  let info
  return {
    name: 'forge-release-info',
    async config() {
      const changes = await frontendVersionChanges(root)
      if (changes.length) {
        throw new Error('前端版本不一致，请在根目录运行版本同步命令：'
          + 'node scripts/forge-shared/version-cli.mjs --sync（或 pnpm version:sync）')
      }
      info = await createReleaseInfo(root, client)
      return { define: { __FORGE_BUILD_INFO__: JSON.stringify(info) } }
    },
    generateBundle() {
      // 独立报表端即使尚未消费全局常量，也应在产物中保留可核对的前端版本。
      this.emitFile({ type: 'asset', fileName: 'version.json', source: JSON.stringify(info, null, 2) })
    },
  }
}
