import fs from 'node:fs/promises'
import path from 'node:path'

export function requireCondition(condition, message) {
  if (!condition) {
    throw new Error(message)
  }
}

export function validateRelative(value) {
  requireCondition(typeof value === 'string' && value.length > 0 && value.length <= 1024, '非法相对路径')
  requireCondition(!/[\\:\x00-\x1f\x7f]/.test(value) && !value.startsWith('/'), `路径越界或非法：${value}`)
  const parts = value.split('/')
  requireCondition(parts.every(part => part && !['.', '..'].includes(part)
    && !/[. ]$/.test(part) && !/^(con|prn|aux|nul|com[1-9]|lpt[1-9])(?:\.|$)/i.test(part)), '非法路径段')
  return value
}

export async function statOptional(target) {
  try {
    return await fs.lstat(target)
  }
  catch (error) {
    if (error.code === 'ENOENT') {
      return null
    }
    throw error
  }
}

export async function safeTarget(root, relative, allowLeafLink = false) {
  validateRelative(relative)
  let current = root
  const rootStat = await fs.lstat(root)
  requireCondition(rootStat.isDirectory() && !rootStat.isSymbolicLink(), '工程根目录不能为链接')
  const parts = relative.split('/')
  for (let index = 0; index < parts.length; index++) {
    current = path.join(current, parts[index])
    const stat = await statOptional(current)
    const leaf = index === parts.length - 1
    requireCondition(!stat?.isSymbolicLink() || (leaf && allowLeafLink), `目标路径包含链接：${relative}`)
    requireCondition(!stat || leaf || stat.isDirectory(), `目标父路径不是目录：${relative}`)
  }
  return current
}

export function assertDistinctPaths(values) {
  const normalized = values.map(value => value.normalize('NFC').toLowerCase())
  requireCondition(new Set(normalized).size === normalized.length, '重复或大小写冲突路径')
  for (const value of normalized) {
    const parts = value.split('/')
    parts.pop()
    while (parts.length) {
      requireCondition(!normalized.includes(parts.join('/')), '文件和目录路径冲突')
      parts.pop()
    }
  }
}
