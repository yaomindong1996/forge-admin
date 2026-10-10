import fs from 'node:fs/promises'
import path from 'node:path'
import { createHash } from 'node:crypto'
import { limits, readZip } from './zip.mjs'
import { assertDistinctPaths, requireCondition, validateRelative } from './paths.mjs'

const ignored = new Set(['.git', 'node_modules', 'target', 'logs', '.DS_Store', '.flattened-pom.xml'])

export async function readBundle(source) {
  const stat = await fs.lstat(source)
  requireCondition(!stat.isSymbolicLink(), '插件来源不能是软链接')
  if (stat.isDirectory()) {
    return { files: await readDirectory(source), directory: true }
  }
  requireCondition(stat.isFile() && stat.size <= limits.archive && source.endsWith('.zip'), '来源必须为目录或 ZIP')
  const files = new Map([...readZip(await fs.readFile(source))].filter(([name]) => !excluded(name)))
  return { files, directory: false }
}

function excluded(relative) {
  return relative.split('/').some(name => ignored.has(name)
    || localConfiguration(name))
}

export async function readDirectory(root) {
  return scanDirectory(root, false)
}

export async function readInstalledDirectory(root) {
  return scanDirectory(root, true)
}

function localConfiguration(name) {
  return name.startsWith('.env') || name === 'application-dev.yml'
}

async function scanDirectory(root, installed) {
  const state = { files: new Map(), total: 0, visited: 0, installed }
  await walk(root, '', state)
  assertDistinctPaths([...state.files.keys()])
  return state.files
}

async function walk(root, relative, state) {
  const entries = await fs.readdir(path.join(root, relative), { withFileTypes: true })
  state.visited += entries.length
  requireCondition(state.visited <= limits.count, '插件目录条目数量超过限制')
  for (const entry of entries.sort((a, b) => a.name.localeCompare(b.name))) {
    if (ignored.has(entry.name) || (!state.installed && localConfiguration(entry.name))) {
      continue
    }
    // 源包排除配置是防泄密；覆盖检查恰好相反，先拒绝，甚至不读配置内容/链接目标。
    requireCondition(!state.installed || !localConfiguration(entry.name),
      '插件安装目录包含本地配置，请先人工移出再覆盖/卸载')
    const next = validateRelative(relative ? `${relative}/${entry.name}` : entry.name)
    requireCondition(next.split('/').length <= 32, '插件目录层级超过限制')
    if (entry.isDirectory()) {
      await walk(root, next, state)
    }
    else {
      requireCondition(entry.isFile(), '插件目录包含链接或特殊文件')
      const stat = await fs.lstat(path.join(root, next))
      requireCondition(stat.isFile() && stat.size <= limits.file && state.total + stat.size <= limits.total,
        '插件文件超过大小限制')
      requireCondition(state.files.size < limits.count, '插件文件数量超过限制')
      const data = await fs.readFile(path.join(root, next))
      state.files.set(next, data)
      state.total += data.length
    }
  }
}

export function componentFiles(files, prefix) {
  validateRelative(prefix)
  const result = new Map([...files].filter(([name]) => name.startsWith(`${prefix}/`))
    .map(([name, data]) => [name.slice(prefix.length + 1), data]))
  const reserved = [...result.keys()].some(name => name.toLowerCase() === '.forge-plugin-owned.json')
  requireCondition(result.size > 0 && !reserved, `插件组件缺失或含保留文件：${prefix}`)
  return result
}

export function fingerprint(files) {
  const hash = createHash('sha256')
  for (const [name, data] of [...files].sort(([a], [b]) => a.localeCompare(b))) {
    hash.update(`${Buffer.byteLength(name)}:${name}:${data.length}:`)
    hash.update(data)
  }
  return hash.digest('hex')
}
