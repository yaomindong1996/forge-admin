import fs from 'node:fs/promises'
import path from 'node:path'
import { inspectContent, installedPaths, maxFileBytes, requiredPoms } from './content.mjs'
import { readInventory, readIndexContents, assertIndexUnchanged } from './git.mjs'

export async function checkEdition(directory) {
  const inventory = await readInventory(directory)
  const violations = []
  const paths = new Set(inventory.indexed.map(entry => entry.relative))
  const references = indexReferences(inventory.indexed)
  for (const relative of requiredPoms.keys()) {
    if (!paths.has(relative)) {
      violations.push({ relative, source: '索引', rule: '宿主 POM 必须进入索引并保留空标记' })
    }
  }
  for (const { oid, bytes } of readIndexContents(inventory.root, inventory.indexed)) {
    inspectIndexed(references.get(oid), bytes, violations)
  }
  const parents = new Set([inventory.root])
  for (const relative of new Set([...paths, ...inventory.others])) {
    await inspectWorking({ root: inventory.root, relative, parents }, violations)
  }
  assertIndexUnchanged(inventory)
  return { violations, indexed: paths.size, working: new Set([...paths, ...inventory.others]).size }
}

function indexReferences(records) {
  const references = new Map()
  for (const record of records) {
    const paths = references.get(record.oid) || []
    paths.push(record.relative)
    references.set(record.oid, paths)
  }
  return references
}

function inspectIndexed(paths, bytes, violations) {
  for (const relative of paths) {
    const rules = inspectContent(relative, bytes)
    if (installedPaths.some(prefix => relative.toLowerCase().startsWith(prefix))) {
      rules.push('本地安装的插件目录禁止进入模板索引')
    }
    for (const rule of rules) {
      violations.push({ relative, source: '索引', rule })
    }
  }
}

async function inspectWorking(context, violations) {
  const { root, relative, parents } = context
  try {
    const target = await safeFile(root, relative, parents)
    const stat = await fs.lstat(target)
    if (!stat.isFile() || stat.size > maxFileBytes) {
      throw new Error('只支持不超过 64 MiB 的普通文件')
    }
    const bytes = await fs.readFile(target)
    if (bytes.length > maxFileBytes) {
      throw new Error('文件超过 64 MiB')
    }
    for (const rule of inspectContent(relative, bytes)) {
      violations.push({ relative, source: '工作区', rule })
    }
  }
  catch (error) {
    // 普通缺失文件仍检查其索引；宿主 POM 缺失不能以“准备删除”绕过当前联调检查。
    if (error.code !== 'ENOENT' || requiredPoms.has(relative)) {
      violations.push({ relative, source: '工作区',
        rule: '文件不可检查：缺失、链接、越界、非普通或超限' })
    }
  }
}

async function safeFile(root, relative, checked) {
  const parts = relative.split('/')
  if (parts.some(part => !part || ['.', '..'].includes(part)) || path.isAbsolute(relative)) {
    throw new Error('非法路径')
  }
  let directory = root
  for (const part of parts.slice(0, -1)) {
    directory = path.join(directory, part)
    if (!checked.has(directory)) {
      const stat = await fs.lstat(directory)
      if (!stat.isDirectory() || stat.isSymbolicLink()) {
        throw new Error('父目录不是普通目录')
      }
      checked.add(directory)
    }
  }
  return path.join(directory, parts.at(-1))
}
