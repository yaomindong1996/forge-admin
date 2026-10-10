import { randomBytes, randomUUID } from 'node:crypto'
import fs from 'node:fs/promises'
import { ensure, BuildError } from '../../forge-plugin-builder/errors.mjs'
import { safeTarget, statOptional } from '../../forge-plugin/paths.mjs'
import { writePrivate } from '../../forge-plugin-builder/files.mjs'
import { readJson } from '../config.mjs'
import { deliveryRequest } from './http.mjs'

export function session(config, id, signal, call = deliveryRequest) {
  const token = process.env.FORGE_PLUGIN_DELIVERY_TOKEN
  ensure(typeof token === 'string' && /^[a-f0-9]{64}$/.test(token), 'DELIVERY_TOKEN_REQUIRED')
  const lease = randomBytes(32).toString('hex')
  const connection = { baseUrl: config.apiBaseUrl, taskId: id, token }
  const request = async (action, body = {}) => {
    const nonce = randomUUID()
    const value = await call(connection, action, { lease, nonce, ...body },
      { signal: action === 'finish' ? undefined : signal })
    ensure(action === 'finish' || value?.nonce === nonce, 'DELIVERY_NONCE_MISMATCH')
    return value
  }
  return { lease, request }
}

export async function targetLock(config, id) {
  const root = await safeTarget(config.stateRoot, config.targetId + '.lock')
  try { await fs.mkdir(root, { mode: 0o700 }) }
  catch { throw new BuildError('DELIVERY_TARGET_BUSY') }
  const identity = await fs.lstat(root)
  const nonce = randomUUID()
  await writePrivate(root, 'owner.json', JSON.stringify({ id, targetId: config.targetId, nonce }))
  return async () => {
    const current = await fs.lstat(root)
    const { value } = await readJson(await safeTarget(root, 'owner.json'))
    ensure(identity.ino === current.ino && identity.dev === current.dev && value.nonce === nonce
      && value.id === id && value.targetId === config.targetId, 'DELIVERY_LOCK_CHANGED')
    await fs.unlink(await safeTarget(root, 'owner.json'))
    await fs.rmdir(root)
  }
}

export async function recoverLock(config, id, options = {}, dependencies = {}) {
  ensure(options.reviewed === true && /^[a-f0-9-]{36}$/.test(id), 'DELIVERY_REVIEW_REQUIRED')
  const root = await safeTarget(config.stateRoot, config.targetId + '.lock')
  const identity = await fs.lstat(root)
  ensure(identity.isDirectory() && !identity.isSymbolicLink() && identity.uid === process.getuid()
    && (identity.mode & 0o077) === 0, 'DELIVERY_LOCK_INVALID')
  const { value, data } = await readJson(await safeTarget(root, 'owner.json'))
  ensure(value.id === id && value.targetId === config.targetId, 'DELIVERY_LOCK_INVALID')
  const { request } = session(config, id, options.signal, dependencies.request)
  const result = await request('recovery')
  ensure(result.id === id && result.targetId === config.targetId && result.status === 'reconciled',
    'DELIVERY_RECOVERY_NOT_CONFIRMED')
  const current = await fs.lstat(root)
  const reread = await readJson(await safeTarget(root, 'owner.json'))
  ensure(current.ino === identity.ino && current.dev === identity.dev && data.equals(reread.data)
    && (await fs.readdir(root)).length === 1, 'DELIVERY_LOCK_CHANGED')
  const archive = await safeTarget(config.stateRoot, 'closed-' + id)
  ensure(!await statOptional(archive), 'DELIVERY_RECOVERY_EXISTS')
  // 保留锁的原始身份审计，而非强制删除或自动接管活跃执行器。
  await fs.rename(root, archive)
  return { id, targetId: config.targetId, lockArchived: true, deployed: false }
}
