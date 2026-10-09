import { Writable } from 'node:stream'
import { finished } from 'node:stream/promises'
import { createHash } from 'node:crypto'
import { ensure, BuildError } from '../../forge-plugin-builder/errors.mjs'

// SDK 只从独立工具包解析；凭证仅由执行器环境提供，不回传 URL/签名/SDK原始异常。
export async function cosClient() {
  const SecretId = process.env.FORGE_ARTIFACT_COS_SECRET_ID
  const SecretKey = process.env.FORGE_ARTIFACT_COS_SECRET_KEY
  ensure(SecretId && SecretKey, 'COS_CREDENTIAL_REQUIRED')
  const { default: COS } = await import('cos-nodejs-sdk-v5')
  return new COS({ SecretId, SecretKey, SecurityToken: process.env.FORGE_ARTIFACT_COS_SESSION_TOKEN,
    Protocol: 'https:', StrictSsl: true, FollowRedirect: false, Timeout: 30000,
    ChunkRetryTimes: 0, FileParallelLimit: 1, ChunkParallelLimit: 1 })
}

export class PrivateCosStore {
  constructor(config, client) {
    this.config = config
    this.client = client
  }
  params(Key) {
    return { Bucket: this.config.bucket, Region: this.config.region, ...(Key ? { Key } : {}) }
  }
  async call(method, params) {
    try { return await this.client[method](params) }
    catch (failure) {
      if (failure.statusCode === 404 || failure.code === 'NoSuchKey' || failure.code === 'NoSuchBucketPolicy')
        throw new BuildError('COS_NOT_FOUND')
      if (failure.statusCode === 409 || failure.statusCode === 412)
        throw new BuildError('COS_OBJECT_EXISTS')
      throw new BuildError('COS_REQUEST_FAILED')
    }
  }
  async privacy() {
    privateAcl(await this.call('getBucketAcl', this.params()))
    try {
      const value = await this.call('getBucketPolicy', this.params())
      const policy = value.Policy
      const statements = policy?.statement || policy?.Statement
      ensure(policy && typeof policy === 'object' && Array.isArray(statements), 'COS_POLICY_INVALID')
      ensure(statements.every(row => {
        const effect = row.effect || row.Effect
        const principal = row.principal || row.Principal
        return ['allow', 'Allow', 'deny', 'Deny'].includes(effect)
          && (effect.toLowerCase() === 'deny' || (principal && !JSON.stringify(principal).includes('*')))
      }), 'COS_PUBLIC_POLICY')
    }
    catch (failure) {
      if (failure.code !== 'COS_NOT_FOUND') throw failure
    }
    const versioning = await this.call('getBucketVersioning', this.params())
    ensure(!versioning.Status || versioning.Status === 'Disabled', 'COS_VERSIONING_UNSUPPORTED')
  }
  async put(Key, Body, bytes, authorize) {
    await authorize()
    try {
      await this.call('putObject', { ...this.params(Key), Body, ContentLength: bytes, ACL: 'private',
        CacheControl: 'no-store', Headers: { 'x-cos-forbid-overwrite': 'true' } })
    }
    catch (failure) {
      if (failure.code !== 'COS_OBJECT_EXISTS') throw failure
      // 重试不得覆盖；已有对象仍必须完整读回验真。
    }
  }
  async read(Key, expected, output) {
    privateAcl(await this.call('getObjectAcl', this.params(Key)))
    const digest = createHash('sha256')
    let bytes = 0
    const chunks = []
    const sink = new Writable({ write(chunk, encoding, done) {
      bytes += chunk.length
      if (bytes > expected.bytes) { done(new BuildError('COS_OBJECT_SIZE_LIMIT')); return }
      digest.update(chunk)
      if (output) output(chunk).then(() => done(), done)
      else if (expected.capture) chunks.push(Buffer.from(chunk))
      else done()
      if (!output && expected.capture) done()
    } })
    // SDK会将网络数据pipe到Output；不使用Body缓冲大制品。
    const completion = finished(sink)
    // SDK在失败时也会向Output发送error，必须先绑定处理器，且等落盘完成再验摘要。
    completion.catch(() => {})
    try {
      await this.call('getObject', { ...this.params(Key), Output: sink })
      await completion
    }
    catch (failure) { sink.destroy(); throw failure }
    ensure(bytes === expected.bytes, 'COS_OBJECT_SIZE_MISMATCH')
    const sha = digest.digest('hex')
    ensure(!expected.sha256 || sha === expected.sha256, 'COS_OBJECT_DIGEST_MISMATCH')
    return { bytes, sha256: sha, data: expected.capture ? Buffer.concat(chunks) : undefined }
  }
}

export function privateAcl(value) {
  ensure(value && ['private', 'default'].includes(value.ACL) && Array.isArray(value.Grants), 'COS_ACL_INVALID')
  ensure(value.Grants.every(row => !row.Grantee?.URI && row.Grantee?.ID), 'COS_PUBLIC_ACL')
}
