import fs from 'node:fs/promises'
import { constants } from 'node:fs'
import path from 'node:path'
import { createHash } from 'node:crypto'
import { safeTarget } from '../forge-plugin/paths.mjs'
import { ensure } from '../forge-plugin-builder/errors.mjs'

export async function copyArtifacts(source, destination, artifacts, signal) {
  await fs.mkdir(destination, { mode: 0o700 })
  for (const item of artifacts) {
    ensure(!signal?.aborted, 'INTERRUPTED')
    const input = await safeTarget(source, item.path)
    const output = await safeTarget(destination, item.path)
    await fs.mkdir(path.dirname(output), { recursive: true, mode: 0o700 })
    await copyFileVerified(input, output, item, signal)
  }
}

// 流式复制并核对读取句柄的前后状态；不硬链接原产物，也不将大文件整块读入内存。
async function copyFileVerified(input, output, expected, signal) {
  const source = await fs.open(input, constants.O_RDONLY | constants.O_NOFOLLOW | constants.O_NONBLOCK)
  let target
  try {
    const before = await source.stat()
    ensure(before.isFile() && before.nlink === 1 && before.size === expected.bytes, 'ARTIFACT_CHANGED')
    target = await fs.open(output, 'wx', 0o600)
    const hash = createHash('sha256')
    const buffer = Buffer.alloc(65536)
    let bytes = 0
    for (;;) {
      ensure(!signal?.aborted, 'INTERRUPTED')
      const { bytesRead } = await source.read(buffer, 0, buffer.length, null)
      if (!bytesRead) break
      bytes += bytesRead
      ensure(bytes <= expected.bytes, 'ARTIFACT_CHANGED')
      const chunk = buffer.subarray(0, bytesRead)
      hash.update(chunk)
      await writeChunk(target, chunk)
    }
    const after = await source.stat()
    ensure(bytes === expected.bytes && hash.digest('hex') === expected.sha256
      && before.size === after.size && before.mtimeMs === after.mtimeMs && before.ctimeMs === after.ctimeMs,
    'ARTIFACT_CHANGED')
    ensure(!signal?.aborted, 'INTERRUPTED')
    await target.sync()
  }
  finally {
    await target?.close()
    await source.close()
  }
}

async function writeChunk(handle, chunk) {
  let offset = 0
  while (offset < chunk.length) {
    const { bytesWritten } = await handle.write(chunk, offset, chunk.length - offset, null)
    ensure(bytesWritten > 0, 'ARTIFACT_WRITE_FAILED')
    offset += bytesWritten
  }
}

export async function sealTree(root) {
  const entries = await fs.readdir(root)
  for (const name of entries) {
    const target = await safeTarget(root, name)
    const stat = await fs.lstat(target)
    ensure(stat.uid === process.getuid() && !stat.isSymbolicLink(), 'SNAPSHOT_FILE_UNSAFE')
    if (stat.isDirectory()) await sealTree(target)
    else {
      ensure(stat.isFile() && stat.nlink === 1, 'SNAPSHOT_FILE_UNSAFE')
      await fs.chmod(target, 0o400)
    }
  }
  await fs.chmod(root, 0o500)
  await syncDirectory(root)
}

export async function syncDirectory(root) {
  const handle = await fs.open(root, constants.O_RDONLY | constants.O_DIRECTORY | constants.O_NOFOLLOW)
  try { await handle.sync() }
  finally { await handle.close() }
}
