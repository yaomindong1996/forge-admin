import fs from 'node:fs/promises'
import { constants } from 'node:fs'
import { createHash } from 'node:crypto'
import { safeTarget, statOptional } from '../paths.mjs'
import { readDescriptor } from '../descriptor.mjs'
import { readZip } from '../zip.mjs'
import { archiveLimit, check } from './config.mjs'
import { request } from './http.mjs'

const digest = data => createHash('sha256').update(data).digest('hex')

export async function obtainSource(context, config, selection, options) {
  const { release, id } = selection
  const response = await request(config, `/api/plugins/versions/${release.id}/download`, { ...options, zip: true })
  check(response.sha256 === release.sha256 && response.data.length === release.archiveBytes
    && digest(response.data) === release.sha256, 'MARKET_SOURCE_DIGEST_MISMATCH')
  const metadata = readDescriptor(readZip(response.data), context.coreVersion)
  check(metadata.id === id && metadata.version === release.version && metadata.edition === release.edition
    && metadata.requiresCore === release.requiresCore, 'MARKET_SOURCE_METADATA_MISMATCH')
  return cacheSource(context.root, response.data, release.sha256)
}

// 缓存也是不可信磁盘输入；拒绝软/硬链接、公共权限以及同名但内容不同的包。
export async function cacheSource(root, data, sha256) {
  check(data.length <= archiveLimit && digest(data) === sha256, 'MARKET_SOURCE_DIGEST_MISMATCH')
  check(typeof process.getuid === 'function', 'MARKET_POSIX_REQUIRED')
  for (const relative of ['.forge-plugin', '.forge-plugin/downloads']) {
    const directory = await safeTarget(root, relative)
    await fs.mkdir(directory, { mode: 0o700 }).catch(error => { if (error.code !== 'EEXIST') throw error })
    const stat = await fs.lstat(directory)
    check(stat.isDirectory() && !stat.isSymbolicLink() && stat.uid === process.getuid()
      && (stat.mode & 0o077) === 0, 'MARKET_CACHE_UNSAFE')
  }
  const file = await safeTarget(root, `.forge-plugin/downloads/${sha256}.zip`)
  if (await statOptional(file)) {
    await verifyCache(file, sha256, data.length)
    return file
  }
  const handle = await fs.open(file, constants.O_WRONLY | constants.O_CREAT | constants.O_EXCL
    | constants.O_NOFOLLOW, 0o400)
  try { await handle.writeFile(data); await handle.sync() }
  catch (error) { await fs.unlink(file).catch(() => {}); throw error }
  finally { await handle.close() }
  await verifyCache(file, sha256, data.length)
  return file
}

async function verifyCache(file, sha256, size) {
  const handle = await fs.open(file, constants.O_RDONLY | constants.O_NOFOLLOW | constants.O_NONBLOCK)
  try {
    const stat = await handle.stat()
    check(stat.isFile() && stat.nlink === 1 && stat.uid === process.getuid()
      && (stat.mode & 0o777) === 0o400 && stat.size === size && size <= archiveLimit, 'MARKET_CACHE_UNSAFE')
    const data = Buffer.alloc(size + 1)
    let offset = 0
    while (offset < data.length) {
      const { bytesRead } = await handle.read(data, offset, data.length - offset, null)
      if (!bytesRead) break
      offset += bytesRead
    }
    check(offset === size && digest(data.subarray(0, offset)) === sha256, 'MARKET_CACHE_CHANGED')
  }
  finally { await handle.close() }
}
