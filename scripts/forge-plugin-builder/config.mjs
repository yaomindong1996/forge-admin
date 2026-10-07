import fs from 'node:fs/promises'
import path from 'node:path'
import { parseStrictJson } from '../forge-plugin/json.mjs'
import { readRegular } from './files.mjs'
import { ensure } from './errors.mjs'

const fields = ['sourceRoot', 'commit', 'packageFile', 'packageSha256', 'workspaceRoot',
  'dockerExecutable', 'dockerSocket', 'image']
const digest = /^[a-f0-9]{64}$/

export async function readConfig(file) {
  const [major, minor] = process.versions.node.split('.').map(Number)
  ensure(major > 20 || (major === 20 && minor >= 19), 'NODE_VERSION_UNSUPPORTED')
  ensure(typeof process.getuid === 'function' && process.platform !== 'win32', 'POSIX_REQUIRED')
  const value = parseStrictJson((await readRegular(file, 16384)).toString('utf8'), 16384)
  ensure(value && !Array.isArray(value) && typeof value === 'object', 'CONFIG_INVALID')
  ensure(Object.keys(value).length === fields.length && Object.keys(value).every(key => fields.includes(key)),
    'CONFIG_INVALID')
  for (const key of fields) {
    ensure(typeof value[key] === 'string', 'CONFIG_INVALID')
  }
  ensure(/^[a-f0-9]{40}$/.test(value.commit) && digest.test(value.packageSha256), 'CONFIG_DIGEST_INVALID')
  ensure(/^[a-z0-9][a-z0-9./_-]*@sha256:[a-f0-9]{64}$/.test(value.image), 'IMAGE_DIGEST_REQUIRED')
  for (const key of ['sourceRoot', 'packageFile', 'workspaceRoot', 'dockerExecutable', 'dockerSocket']) {
    ensure(path.isAbsolute(value[key]) && !/[,:\x00-\x1f\x7f]/.test(value[key]), 'CONFIG_PATH_INVALID')
  }
  ensure(value.packageFile.endsWith('.zip'), 'ZIP_REQUIRED')
  const rootStat = await fs.lstat(value.workspaceRoot)
  ensure(rootStat.isDirectory() && !rootStat.isSymbolicLink()
    && rootStat.uid === process.getuid() && (rootStat.mode & 0o077) === 0, 'WORKSPACE_NOT_PRIVATE')
  const sourceStat = await fs.lstat(value.sourceRoot)
  ensure(sourceStat.isDirectory() && !sourceStat.isSymbolicLink(), 'SOURCE_UNSAFE')
  const sourceRoot = await fs.realpath(value.sourceRoot)
  const workspaceRoot = await fs.realpath(value.workspaceRoot)
  ensure(disjoint(sourceRoot, workspaceRoot), 'WORKSPACE_OVERLAP')
  return { ...value, sourceRoot, workspaceRoot }
}

function disjoint(left, right) {
  return path.relative(left, right).startsWith(`..${path.sep}`)
    && path.relative(right, left).startsWith(`..${path.sep}`)
}
