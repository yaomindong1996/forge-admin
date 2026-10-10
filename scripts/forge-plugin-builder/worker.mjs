import fs from 'node:fs/promises'
import path from 'node:path'
import { randomBytes } from 'node:crypto'
import { parseStrictJson } from '../forge-plugin/json.mjs'
import { validateConfig } from './config.mjs'
import { executeBuild } from './builder.mjs'
import { readRegular, sha256, writePrivate } from './files.mjs'
import { apiBase, workerRequest } from './worker-http.mjs'
import { maintainLease } from './worker-lease.mjs'
import { reportResult } from './worker-result.mjs'
import { ensure, errorCode } from './errors.mjs'

const fields = ['apiBaseUrl', 'sourceRoot', 'commit', 'workspaceRoot', 'dockerExecutable', 'dockerSocket', 'image']

async function workerConfig(file, options) {
  const value = parseStrictJson((await readRegular(file, 16384)).toString('utf8'), 16384)
  ensure(value && !Array.isArray(value) && Object.keys(value).length === fields.length
    && Object.keys(value).every(key => fields.includes(key)), 'WORKER_CONFIG_INVALID')
  const baseUrl = apiBase(value.apiBaseUrl)
  const { apiBaseUrl: ignored, ...build } = value
  void ignored
  const config = await validateConfig({ ...build, packageSha256: options.sha256,
    packageFile: path.join(value.workspaceRoot, 'package.zip') })
  return { config, baseUrl }
}

// 一次只处理指定且人工审查的任务；失败/网络不确定均不循环领取其它任务。
export async function executeWorker(file, options, dependencies = {}) {
  ensure(options.reviewed === true && /^[a-f0-9-]{36}$/.test(options.taskId)
    && Number.isSafeInteger(options.revision) && options.revision >= 0
    && /^[a-f0-9]{64}$/.test(options.sha256), 'WORKER_REVIEW_REQUIRED')
  const { config, baseUrl } = await workerConfig(file, options)
  const token = process.env.FORGE_PLUGIN_WORKER_TOKEN
  ensure(typeof token === 'string' && /^[a-f0-9]{64}$/.test(token), 'WORKER_TOKEN_REQUIRED')
  const connection = { baseUrl, token, taskId: options.taskId }
  const root = await fs.mkdtemp(path.join(config.workspaceRoot, 'transfer-'))
  const controller = new AbortController()
  const abort = () => controller.abort()
  const leaseToken = randomBytes(32).toString('hex')
  const request = dependencies.request || workerRequest
  const call = (action, body, signal) => request(connection, action, { leaseToken, ...body }, { signal })
  const receipt = { taskId: options.taskId, status: 'unclaimed', deployed: false }
  let lease
  let deadline
  options.signal?.addEventListener('abort', abort, { once: true })
  try {
    ensure(!options.signal?.aborted, 'WORKER_INTERRUPTED')
    const claim = await call('claim', { revision: options.revision, sha256: options.sha256,
      sourceCommit: config.commit, image: config.image }, controller.signal)
    validateClaim(claim, options)
    receipt.status = 'claimed'
    // 随机租约仅保留在私有恢复收据，机器 Bearer 始终不落盘；收据不进入容器。
    await writePrivate(root, 'lease.json', JSON.stringify({ taskId: options.taskId, leaseToken }))
    deadline = setTimeout(abort, 25 * 60 * 1000)
    lease = maintainLease(call, abort, controller.signal, dependencies.interval)
    const archive = await call('archive', {}, controller.signal)
    ensure(Buffer.isBuffer(archive) && archive.length === claim.archiveBytes
      && sha256(archive) === options.sha256, 'WORKER_PACKAGE_DIGEST_MISMATCH')
    await writePrivate(root, 'package.zip', archive)
    const input = { ...config, packageFile: path.join(root, 'package.zip') }
    const build = dependencies.build || executeBuild
    const result = await build(input, { run: true, reviewed: true, force: !!options.force,
      signal: controller.signal, onPhase: lease.advance })
    await lease.stop()
    const reported = controller.signal.aborted && result.status === 'built'
      ? { ...result, status: 'failed', failureCode: 'WORKER_LEASE_LOST', failurePhase: lease.phase } : result
    const report = reportResult(reported, config, lease.phase)
    receipt.jobId = report.jobId
    receipt.result = report
    const finished = await call('finish', { result: report })
    ensure(finished.taskId === options.taskId && finished.sha256 === options.sha256
      && finished.status === (report.success ? 'built' : 'build_failed'), 'WORKER_FINISH_INVALID')
    receipt.status = finished.status
  }
  catch (error) {
    receipt.failureCode = errorCode(error)
    receipt.status = receipt.status === 'unclaimed' ? 'claim_failed' : 'report_pending'
  }
  finally {
    abort()
    clearTimeout(deadline)
    await lease?.stop()
    options.signal?.removeEventListener('abort', abort)
  }
  await writePrivate(root, 'worker-result.json', JSON.stringify(receipt, null, 2))
  return receipt
}

function validateClaim(claim, options) {
  ensure(claim?.taskId === options.taskId && claim.sha256 === options.sha256 && claim.status === 'building'
    && claim.revision === options.revision + 1 && claim.leaseSeconds === 90
    && Number.isSafeInteger(claim.archiveBytes) && claim.archiveBytes > 0 && claim.archiveBytes <= 8 * 1024 * 1024,
  'WORKER_CLAIM_INVALID')
}
