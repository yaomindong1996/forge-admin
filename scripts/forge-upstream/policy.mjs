import fs from 'node:fs/promises'
import path from 'node:path'
import { collectFiles } from '../forge-shared/files.mjs'

export const mappingVersion = 1

export const generatorFiles = [
  'scripts/forge-create/create-project.mjs', 'scripts/forge-create/module-catalog.json',
  'scripts/forge-create/project-tools.mjs', 'scripts/forge-create/pom-modules.mjs',
  'scripts/forge-create/source-glue.mjs', 'scripts/forge-shared/rename.mjs',
  'scripts/forge-shared/files.mjs', 'scripts/forge-shared/version.mjs',
]

const optionNames = new Set([
  'project-name', 'artifact-prefix', 'module-artifact-prefix', 'java-name', 'display-name',
  'preset', 'base-package', 'group-id', 'database-name', 'include', 'exclude-log-data',
  'admin-title', 'admin-api-prefix', 'admin-port', 'admin-proxy-target', 'admin-public-path', 'admin-base-url',
])

export function validateOptions(options) {
  if (!options || typeof options !== 'object' || Array.isArray(options)) throw new Error('生成选项必须是对象')
  for (const [key, value] of Object.entries(options)) {
    if (!optionNames.has(key) || !['string', 'boolean'].includes(typeof value) || /[\n\r\0]/.test(String(value))) {
      throw new Error(`不支持的生成选项：${key}`)
    }
  }
  for (const key of ['project-name', 'artifact-prefix', 'base-package', 'group-id', 'database-name', 'preset']) {
    if (!options[key]) throw new Error(`缺少生成选项：${key}`)
  }
  for (const key of ['project-name', 'artifact-prefix', 'module-artifact-prefix']) {
    if (options[key] && !/^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$/.test(options[key])) throw new Error(`非法名称：${key}`)
  }
  return options
}

export function protectedPath(name, options) {
  return name.startsWith('.forge/') || name.startsWith(`${options['artifact-prefix']}-server/plugins/`)
    || name.startsWith(`${options['project-name']}-admin-ui/src/views/plugins/`)
    || /(^|\/)(?:application-(?:dev|local)\.yml|\.env(?:\..+)?\.local|\.env|.*\.secrets)$/.test(name)
}

export function immutableMigration(name) {
  return /(?:^|\/)db\/migration\/V[^/]+\.sql$/.test(name)
}

function keepPath(name, options) {
  if (protectedPath(name, options)) return false
  if (/(^|\/)(?:node_modules|target|dist|logs|\.git|\.idea|\.vscode|\.DS_Store)(\/|$)/.test(name)) return false
  if (/^[^/]+-(?:admin|report|h5)-ui\/(?:\.qoder|\.turing_coder_rules|\.clawhub|\.husky|skills|docs|readme)\//
    .test(name)) return false
  if (/^[^/]+-(?:admin|report|h5)-ui\/(?:.*\.timestamp-.*|preview\.yml)$/.test(name)) return false
  if (/^code-copilot\/(?:memory|knowledge|changes|agents|commands)\//.test(name)) return false
  const server = `${options['artifact-prefix']}-server/`
  if (name.startsWith(`${server}scripts/`) && !name.startsWith(`${server}scripts/db/`)) return false
  if (name.startsWith(`${server}db/`) && /\/(?:module|backup|community-export|demo|optional)\//.test(name)) return false
  if (name.startsWith(server) && !name.slice(server.length).includes('/')) {
    return ['pom.xml', '.gitignore'].includes(name.slice(server.length))
  }
  if (/^[^/]+-(?:admin|report|h5)-ui\/[^/]+$/.test(name)) {
    return !/(?:deploy|backup|\.zip$|\.log$)/i.test(name)
      && (!/\.md$/.test(name) || /\/(?:DESIGN|LICENSE|NOTICE|THIRD_PARTY[^/]*)\.md$/.test(name))
  }
  return !name.endsWith('.flattened-pom.xml')
}

// 只清理刚生成的隔离目录，绝不对用户既有工程执行清理。
export async function cleanSnapshot(root, options) {
  for (const file of await collectFiles(root, () => true)) {
    const relative = path.relative(root, file).split(path.sep).join('/')
    const stat = await fs.lstat(file)
    if (!stat.isFile()) throw new Error(`生成物不接受特殊文件：${relative}`)
    if (!keepPath(relative, options)) await fs.unlink(file)
  }
  await fs.mkdir(path.join(root, 'code-copilot/changes'), { recursive: true })
  await fs.writeFile(path.join(root, 'code-copilot/changes/.gitkeep'), '')
  await fs.mkdir(path.join(root, 'code-copilot/memory'), { recursive: true })
  for (const file of ['preferences', 'decisions', 'pitfalls']) {
    await fs.writeFile(path.join(root, `code-copilot/memory/${file}.md`), '# 新工程记录\n\n仅记录本工程事项。\n')
  }
}
