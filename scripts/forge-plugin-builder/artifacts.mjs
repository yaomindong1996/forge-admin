import fs from 'node:fs/promises'
import { constants } from 'node:fs'
import { createHash } from 'node:crypto'
import { safeTarget } from '../forge-plugin/paths.mjs'
import { validateFileNames } from './files.mjs'
import { ensure } from './errors.mjs'

const limits = Object.freeze({ count: 4096, file: 512 * 1024 * 1024, total: 1024 * 1024 * 1024 })

// 不读取容器声明的摘要；退出码和实际文件共同验证，仍不等于运行健康检查。
export async function collectArtifacts(root, targets, signal) {
  const files = []
  const entries = await fs.readdir(root)
  const allowed = Object.keys(targets).map(kind => kind === 'server' ? 'backend' : 'frontend')
  ensure(entries.length === allowed.length && entries.every(entry => allowed.includes(entry)), 'ARTIFACTS_UNEXPECTED')
  for (const directory of allowed) {
    await walk(root, directory, files)
  }
  validateFileNames(files, limits.count)
  if (targets.server) {
    ensure(files.filter(file => file.startsWith('backend/')).join() === 'backend/admin.jar',
      'BACKEND_ARTIFACT_MISSING')
  }
  if (targets.ui) {
    ensure(files.includes('frontend/index.html'), 'UI_ARTIFACT_MISSING')
  }
  const result = []
  let total = 0
  for (const relative of files.sort()) {
    ensure(!signal?.aborted, 'INTERRUPTED')
    const item = await hashArtifact(await safeTarget(root, relative), relative === 'backend/admin.jar')
    total += item.bytes
    ensure(total <= limits.total, 'ARTIFACT_TOTAL_LIMIT')
    result.push({ path: relative, ...item })
  }
  return result
}

async function walk(root, relative, files) {
  const target = await safeTarget(root, relative)
  const stat = await fs.lstat(target)
  ensure(!stat.isSymbolicLink(), 'ARTIFACT_LINK')
  if (stat.isFile()) {
    ensure(stat.nlink === 1 && stat.size > 0 && stat.size <= limits.file, 'ARTIFACT_FILE_UNSAFE')
    files.push(relative)
    ensure(files.length <= limits.count, 'ARTIFACT_COUNT_LIMIT')
    return
  }
  ensure(stat.isDirectory() && relative.split('/').length < 32, 'ARTIFACT_DIRECTORY_UNSAFE')
  const entries = await fs.readdir(target)
  ensure(entries.length > 0 && entries.length <= limits.count, 'ARTIFACT_DIRECTORY_LIMIT')
  for (const entry of entries) {
    await walk(root, `${relative}/${entry}`, files)
  }
}

async function hashArtifact(file, jar) {
  const handle = await fs.open(file, constants.O_RDONLY | constants.O_NOFOLLOW)
  try {
    const before = await handle.stat()
    ensure(before.isFile() && before.nlink === 1 && before.size <= limits.file, 'ARTIFACT_FILE_UNSAFE')
    if (jar) {
      const signature = Buffer.alloc(4)
      await handle.read(signature, 0, 4, 0)
      ensure(signature.equals(Buffer.from([0x50, 0x4b, 0x03, 0x04])), 'BACKEND_JAR_SIGNATURE_INVALID')
    }
    const hash = createHash('sha256')
    let bytes = 0
    for await (const chunk of handle.createReadStream({ start: 0, autoClose: false, highWaterMark: 65536 })) {
      bytes += chunk.length
      ensure(bytes <= limits.file, 'ARTIFACT_SIZE_LIMIT')
      hash.update(chunk)
    }
    const after = await handle.stat()
    ensure(bytes === before.size && before.size === after.size && before.mtimeMs === after.mtimeMs,
      'ARTIFACT_CHANGED')
    return { bytes, sha256: hash.digest('hex') }
  }
  finally {
    await handle.close()
  }
}
