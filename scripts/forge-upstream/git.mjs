import { execFileSync } from 'node:child_process'
import fs from 'node:fs/promises'
import path from 'node:path'

export function git(root, args, options = {}) {
  return execFileSync('git', args, {
    cwd: root, encoding: 'utf8', timeout: 120000, maxBuffer: 64 * 1024 * 1024,
    stdio: ['pipe', 'pipe', 'pipe'], ...options,
  })
}

export function resolveCommit(root, ref) {
  if (!ref || ref.startsWith('-') || /[\r\n\0]/.test(ref)) throw new Error('非法社区提交引用')
  return git(root, ['rev-parse', '--verify', `${ref}^{commit}`]).trim()
}

export function assertUpgradeBranch(root) {
  const branch = git(root, ['symbolic-ref', '--quiet', '--short', 'HEAD']).trim()
  if (['main', 'master'].includes(branch)) throw new Error('请先创建升级分支，禁止直接同步主分支')
  if (git(root, ['status', '--porcelain', '--untracked-files=all']).trim()) {
    throw new Error('工作区必须干净，请先保存现有改动')
  }
  try {
    git(root, ['rev-parse', '--verify', 'MERGE_HEAD'])
  }
  catch { return branch }
  throw new Error('存在未完成合并，请先完成或中止')
}

export function treeEntries(root, commit) {
  return new Map(git(root, ['ls-tree', '-rz', '--full-tree', commit]).split('\0').filter(Boolean).map(row => {
    const [header, name] = row.split('\t')
    const [mode, type, oid] = header.split(' ')
    if (type !== 'blob' || !['100644', '100755'].includes(mode)) {
      throw new Error(`基线不接受软链接、子模块或特殊文件：${name}`)
    }
    if (!name || name.startsWith('/') || name.split('/').includes('..')) throw new Error('基线路径不安全')
    return [name, { mode, oid }]
  }))
}

// 独立索引避免覆盖用户当前索引；新基线 commit 仅存在于客户仓库，不回写社区仓库。
export async function importTree({ root, snapshot, parent, message, indexFile }) {
  const env = { ...process.env, GIT_INDEX_FILE: indexFile, GIT_WORK_TREE: snapshot,
    GIT_DIR: git(root, ['rev-parse', '--absolute-git-dir']).trim() }
  git(snapshot, ['read-tree', '--empty'], { env })
  git(snapshot, ['add', '--all', '--force', '--', '.'], { env })
  const tree = git(snapshot, ['write-tree'], { env }).trim()
  const args = ['commit-tree', tree, ...(parent ? ['-p', parent] : []), '-F', '-']
  const commit = git(root, args, { input: `${message}\n` }).trim()
  await fs.rm(indexFile, { force: true })
  return commit
}

export async function writeJson(file, value) {
  await fs.mkdir(path.dirname(file), { recursive: true })
  await fs.writeFile(file, `${JSON.stringify(value, null, 2)}\n`)
}
