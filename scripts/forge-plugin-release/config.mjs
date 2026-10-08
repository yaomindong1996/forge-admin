import fs from 'node:fs/promises'
import path from 'node:path'
import os from 'node:os'
import { parseStrictJson } from '../forge-plugin/json.mjs'
import { readRegular } from '../forge-plugin-builder/files.mjs'
import { ensure } from '../forge-plugin-builder/errors.mjs'

export const digestPattern = /^[a-f0-9]{64}$/
export const releasePattern = /^rel-[a-f0-9]{64}$/

export function exactKeys(value, keys, code = 'RELEASE_METADATA_INVALID') {
  ensure(value && typeof value === 'object' && !Array.isArray(value), code)
  ensure(Object.keys(value).length === keys.length && Object.keys(value).every(key => keys.includes(key)), code)
}

export async function readJson(file, limit = 16384) {
  const data = await readRegular(file, limit)
  try {
    const text = new TextDecoder('utf-8', { fatal: true }).decode(data)
    return { data, value: parseStrictJson(text, limit) }
  }
  catch {
    ensure(false, 'RELEASE_JSON_INVALID')
  }
}

export async function readReleaseConfig(file, mode = 'publish') {
  ensure(['publish', 'verify'].includes(mode), 'RELEASE_COMMAND_INVALID')
  const { value } = await readJson(file)
  const keys = ['protocolVersion', 'repositoryId', 'vaultRoot']
  if (mode === 'publish') keys.push('jobRoot', 'resultSha256')
  exactKeys(value, keys, 'RELEASE_CONFIG_INVALID')
  ensure(value.protocolVersion === 1 && typeof value.repositoryId === 'string'
    && /^[a-z][a-z0-9-]{0,63}$/.test(value.repositoryId), 'RELEASE_CONFIG_INVALID')
  const vaultRoot = await privateDirectory(value.vaultRoot)
  if (mode === 'verify') return { ...value, vaultRoot }
  ensure(typeof value.resultSha256 === 'string' && digestPattern.test(value.resultSha256), 'RESULT_DIGEST_INVALID')
  const jobRoot = await privateDirectory(value.jobRoot)
  ensure(disjoint(jobRoot, vaultRoot), 'RELEASE_ROOT_OVERLAP')
  return { ...value, jobRoot, vaultRoot }
}

// 私有且无路径别名的目录是单用户工具边界；不把本地声明当作服务端授权。
export async function privateDirectory(value) {
  const [major, minor] = process.versions.node.split('.').map(Number)
  ensure(major > 20 || (major === 20 && minor >= 19), 'NODE_VERSION_UNSUPPORTED')
  ensure(typeof process.getuid === 'function' && process.getuid() !== 0
    && typeof process.geteuid === 'function' && process.geteuid() !== 0, 'NONROOT_POSIX_REQUIRED')
  ensure(typeof value === 'string' && path.isAbsolute(value) && path.resolve(value) === value
    && !/[,:\x00-\x1f\x7f]/.test(value), 'RELEASE_PATH_INVALID')
  const stat = await fs.lstat(value)
  ensure(stat.isDirectory() && !stat.isSymbolicLink() && stat.uid === process.getuid()
    && (stat.mode & 0o077) === 0, 'RELEASE_ROOT_NOT_PRIVATE')
  const real = await fs.realpath(value)
  ensure(real === value && real !== path.parse(real).root && real !== await fs.realpath(os.homedir()),
    'RELEASE_ROOT_UNSAFE')
  return real
}

function disjoint(left, right) {
  return path.relative(left, right).startsWith(`..${path.sep}`)
    && path.relative(right, left).startsWith(`..${path.sep}`)
}
