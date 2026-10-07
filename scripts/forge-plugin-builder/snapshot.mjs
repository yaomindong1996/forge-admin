import fs from 'node:fs/promises'
import { createHash } from 'node:crypto'
import { safeTarget } from '../forge-plugin/paths.mjs'
import { loadProject } from '../forge-plugin/project.mjs'
import { readRegular, sha256, validateFileNames, writePrivate } from './files.mjs'
import { runProcess } from './process.mjs'
import { ensure } from './errors.mjs'

const limits = Object.freeze({ count: 30000, file: 64 * 1024 * 1024, total: 512 * 1024 * 1024 })
const forbidden = new Set(['.git', '.forge-plugin', 'node_modules', 'target', 'dist', '.DS_Store',
  '.flattened-pom.xml', '.npmrc', '.pnpmfile.cjs', '.aws', '.ssh', '.docker', 'id_rsa', 'id_ed25519'])
const publicEnv = new Set(['.env', '.env.development', '.env.production', '.env.test', '.env.example'])

export async function snapshotSource(config, destination, signal) {
  const git = args => runProcess('/usr/bin/git', ['-c', 'core.fsmonitor=false', '-c', 'core.hooksPath=/dev/null',
    '-C', config.sourceRoot, ...args], { signal, timeoutMs: 10000, outputLimit: 8 * 1024 * 1024,
    env: { PATH: '/usr/bin:/bin', LANG: 'C.UTF-8', GIT_CONFIG_NOSYSTEM: '1', GIT_CONFIG_GLOBAL: '/dev/null' } })
  await verifyRevision(git, config)
  const tree = (await git(['ls-tree', '-rz', '--full-tree', config.commit])).toString('utf8')
  const all = tree.split('\0').filter(Boolean).map(parseEntry)
  validateFileNames(all.map(entry => entry.name), limits.count)
  const context = await loadProject(config.sourceRoot)
  const entries = all.filter(entry => inBuildScope(entry.name, context))
  await fs.mkdir(destination, { mode: 0o700 })
  const hash = createHash('sha256')
  let total = 0
  for (const entry of entries) {
    ensure(!signal?.aborted, 'INTERRUPTED')
    const file = await safeTarget(config.sourceRoot, entry.name)
    const data = await readRegular(file, limits.file)
    total += data.length
    ensure(total <= limits.total, 'SOURCE_SIZE_LIMIT')
    const oid = createHash('sha1').update(`blob ${data.length}\0`).update(data).digest('hex')
    ensure(oid === entry.oid, 'SOURCE_CHANGED')
    hash.update(`${entry.mode}:${Buffer.byteLength(entry.name)}:${entry.name}:${data.length}:${sha256(data)}\n`)
    await writePrivate(destination, entry.name, data, entry.mode === '100755' ? 0o700 : 0o600)
  }
  await verifyRevision(git, config)
  return { commit: config.commit, sha256: hash.digest('hex'), scope: 'admin-build', fileCount: entries.length,
    excludedFileCount: all.length - entries.length, bytes: total }
}

function inBuildScope(name, context) {
  // full 还包含独立 Report/H5/Docker 和大字体库；Admin 构建不需要它们，也不带旧发布 ZIP。
  if (name === `${context.ui}/dist.zip`) {
    return false
  }
  const roots = ['forge.config.json', 'package.json', 'pnpm-lock.yaml', 'pnpm-workspace.yaml', '.gitignore',
    'scripts/forge-create/module-catalog.json']
  return roots.includes(name) || name.startsWith(`${context.server}/`) || name.startsWith(`${context.ui}/`)
}

async function verifyRevision(git, config) {
  ensure((await git(['rev-parse', '--show-toplevel'])).toString('utf8').trim() === config.sourceRoot,
    'SOURCE_NOT_REPO_ROOT')
  ensure((await git(['rev-parse', 'HEAD'])).toString('utf8').trim() === config.commit, 'SOURCE_COMMIT_MISMATCH')
  ensure((await git(['status', '--porcelain=v1', '-z', '--untracked-files=all'])).length === 0, 'SOURCE_DIRTY')
}

function parseEntry(raw) {
  const match = /^(100644|100755) blob ([a-f0-9]{40})\t(.+)$/.exec(raw)
  ensure(match, 'SOURCE_LINK_OR_SUBMODULE')
  const name = match[3]
  const blocked = name.split('/').some(part => forbidden.has(part) || part.endsWith('.local')
    || /^application-(dev|local)\.(yml|yaml|properties)$/.test(part)
    || (part.startsWith('.env') && !publicEnv.has(part)) || /\.(pem|key|p12|pfx|keystore|secrets)$/i.test(part))
  ensure(!blocked, 'SOURCE_LOCAL_CONFIG_OR_OUTPUT')
  return { mode: match[1], oid: match[2], name }
}
