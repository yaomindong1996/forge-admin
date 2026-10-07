import { constants as fsConstants } from 'node:fs'
import fs from 'node:fs/promises'
import path from 'node:path'

// 与原生成器保持一致的递归遍历，调用者只应传入已复制并校验的隔离源码目录。
export async function collectFiles(rootDir, predicate) {
  const result = []
  if (!(await exists(rootDir))) {
    return result
  }
  const entries = await fs.readdir(rootDir, { withFileTypes: true })
  for (const entry of entries) {
    const entryPath = path.join(rootDir, entry.name)
    if (entry.isDirectory()) {
      result.push(...await collectFiles(entryPath, predicate))
      continue
    }
    if (predicate(entryPath)) {
      result.push(entryPath)
    }
  }
  return result
}

export async function collectDirectories(rootDir, predicate) {
  const result = []
  if (!(await exists(rootDir))) {
    return result
  }
  const entries = await fs.readdir(rootDir, { withFileTypes: true })
  for (const entry of entries) {
    if (!entry.isDirectory()) {
      continue
    }
    const entryPath = path.join(rootDir, entry.name)
    if (predicate(entryPath)) {
      result.push(entryPath)
    }
    result.push(...await collectDirectories(entryPath, predicate))
  }
  return result
}

export async function exists(filePath) {
  try {
    await fs.access(filePath, fsConstants.F_OK)
    return true
  }
  catch {
    return false
  }
}
