import fs from 'node:fs/promises'
import { constants } from 'node:fs'
import path from 'node:path'
import { createHash } from 'node:crypto'
import { safeTarget, validateRelative } from '../forge-plugin/paths.mjs'
import { ensure } from './errors.mjs'

export const sha256 = data => createHash('sha256').update(data).digest('hex')

export async function readRegular(file, limit) {
  // 先非阻塞打开再 fstat；路径被换成 FIFO 时也不能等待外部写入者。
  const handle = await fs.open(file, constants.O_RDONLY | constants.O_NOFOLLOW | constants.O_NONBLOCK)
  try {
    const stat = await handle.stat()
    ensure(stat.isFile() && stat.nlink === 1 && stat.size <= limit, 'FILE_UNSAFE')
    // readFile 在并发增长时可能先分配大块内存；始终只分配事先限定的字节数。
    const data = Buffer.alloc(stat.size + 1)
    let offset = 0
    while (offset < data.length) {
      const { bytesRead } = await handle.read(data, offset, data.length - offset, null)
      if (!bytesRead) {
        break
      }
      offset += bytesRead
    }
    ensure(offset === stat.size, 'FILE_CHANGED')
    return data.subarray(0, offset)
  }
  finally {
    await handle.close()
  }
}

export async function writePrivate(root, relative, data, mode = 0o600) {
  const target = await safeTarget(root, relative)
  await fs.mkdir(path.dirname(target), { recursive: true, mode: 0o700 })
  await fs.writeFile(target, data, { flag: 'wx', mode })
}

export function validateFileNames(names, limit) {
  ensure(names.length > 0 && names.length <= limit, 'FILE_COUNT_LIMIT')
  for (const name of names) {
    validateRelative(name)
    ensure(name.split('/').length <= 32, 'PATH_DEPTH_LIMIT')
  }
  // 完整工程可有数万文件，用 Set 做冲突检查，避免逐个父目录线性扫描整份列表。
  const normalized = names.map(name => name.normalize('NFC').toLowerCase())
  const all = new Set(normalized)
  ensure(all.size === names.length, 'PATH_COLLISION')
  for (const name of normalized) {
    const parts = name.split('/')
    parts.pop()
    while (parts.length) {
      ensure(!all.has(parts.join('/')), 'PATH_COLLISION')
      parts.pop()
    }
  }
}
