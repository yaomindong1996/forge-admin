import fs from 'node:fs/promises'
import path from 'node:path'
import { createHash } from 'node:crypto'
import { fileURLToPath } from 'node:url'
import { git, resolveCommit, treeEntries } from './git.mjs'
import { mappingVersion, validateOptions, immutableMigration, protectedPath } from './policy.mjs'
import { generatorDigest } from './source.mjs'

export const statePath = '.forge/upstream/baseline.json'
export const generatorPath = '.forge/upstream/generator'

export const digest = value => createHash('sha256').update(value).digest('hex')

export async function normalizerDigest(root = path.dirname(fileURLToPath(import.meta.url))) {
  const hash = createHash('sha256')
  for (const name of ['policy.mjs', 'source.mjs']) hash.update(name).update(await fs.readFile(path.join(root, name)))
  return hash.digest('hex')
}

export async function readState(root) {
  const raw = await fs.readFile(path.join(root, statePath), 'utf8')
  const state = JSON.parse(raw)
  if (state.schemaVersion !== 1 || state.mappingVersion !== mappingVersion) throw new Error('同步协议/映射版本不支持')
  if (state.normalizerSha256 !== await normalizerDigest()) throw new Error('规范化规则变化，需要评审映射升级')
  validateOptions(state.options)
  if (!/^[a-f0-9]{40,64}$/.test(state.baseCommit || '')) throw new Error('缺少准确基线提交')
  resolveCommit(root, state.baseCommit)
  if (await generatorDigest(path.join(root, generatorPath)) !== state.generatorSha256) {
    throw new Error('冻结映射工具发生变化，必须单独评审映射升级，不能静默重新生成旧基线')
  }
  return { state, stateHash: digest(raw) }
}

export function inspectCandidate(root, state, candidate) {
  const before = treeEntries(root, state.baseCommit)
  const after = treeEntries(root, candidate)
  const current = treeEntries(root, 'HEAD')
  const blocked = []
  const changes = []
  for (const name of new Set([...before.keys(), ...after.keys()])) {
    const old = before.get(name)
    const next = after.get(name)
    if (protectedPath(name, state.options)) blocked.push(`${name}：触及受保护目录`)
    if (immutableMigration(name) && old
      && (next?.oid !== old.oid || current.get(name)?.oid !== old.oid)) {
      blocked.push(`${name}：历史迁移不得修改或删除，请在社区新增迁移修复`)
    }
    if (old?.oid === next?.oid && old?.mode === next?.mode) continue
    changes.push({ status: !old ? 'A' : !next ? 'D' : 'M', path: name })
  }
  return { changes, blocked }
}

export function mergePreview(root, candidate) {
  let output
  let conflicted = false
  try { output = git(root, ['merge-tree', '--write-tree', '--name-only', 'HEAD', candidate]) }
  catch (error) {
    if (error.status !== 1 || !error.stdout) throw error
    output = String(error.stdout)
    conflicted = true
  }
  const files = output.trim().split('\n').slice(1).filter(Boolean)
  const end = files.findIndex(line => line.startsWith('Auto-merging'))
  return { conflicted, conflicts: conflicted ? files.slice(0, end < 0 ? files.length : end) : [] }
}

export function gitStateDirectory(root) {
  return path.join(git(root, ['rev-parse', '--absolute-git-dir']).trim(), 'forge-upstream')
}
