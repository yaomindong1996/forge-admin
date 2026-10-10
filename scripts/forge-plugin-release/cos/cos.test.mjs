import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs/promises'
import path from 'node:path'
import { fixture, write } from '../fixtures/helpers.mjs'
import { readReleaseConfig } from '../config.mjs'
import { publishRelease } from '../vault.mjs'
import { sha256 } from '../../forge-plugin-builder/files.mjs'
import { PrivateCosStore, privateAcl, cosClient } from './store.mjs'
import { readCosConfig } from './config.mjs'
import { publishCos, fetchCos } from './transfer.mjs'
import { memoryClient } from './fixtures/helpers.mjs'

test('sealed actual bytes publish manifest last, no overwrite, verified stream fetch on another vault', async t => {
  const value = await fixture(t)
  const sealed = await publishRelease(await readReleaseConfig(value.file), { reviewed: true })
  const config = { ...value.config, bucket: 'test-100001', region: 'ap-beijing', prefix: 'builds/', sourcePrefix: 'sources/' }
  const memory = memoryClient()
  const store = new PrivateCosStore(config, memory.client)
  let authorizations = 0
  const context = { store, authorize: async () => { authorizations++ } }
  assert.equal((await publishCos(config, sealed.releaseId, context)).deployed, false)
  assert.ok(memory.puts.at(-1).Key.endsWith('/manifest.json'))
  assert.ok(memory.puts.every(row => row.Headers['x-cos-forbid-overwrite'] === 'true' && row.ACL === 'private'))
  assert.equal(authorizations, value.result.artifacts.length + 1)
  await publishCos(config, sealed.releaseId, context)
  const vaultRoot = path.join(value.root, 'downloaded-vault')
  await fs.mkdir(vaultRoot, { mode: 0o700 })
  const result = await fetchCos({ ...config, vaultRoot }, sealed.releaseId, context)
  assert.equal(result.deployed, false)
  for (const file of value.result.artifacts) {
    assert.deepEqual(await fs.readFile(path.join(vaultRoot, sealed.releaseId, 'artifacts', file.path)),
      await fs.readFile(path.join(value.artifactRoot, file.path)))
  }
  assert.equal((await fs.stat(path.join(vaultRoot, sealed.releaseId))).mode & 0o777, 0o500)
})

test('public ACL, public policy, versioning, short/large/changed object rejected', async () => {
  const memory = memoryClient()
  const store = new PrivateCosStore({}, memory.client)
  assert.throws(() => privateAcl({ ACL: 'public-read', Grants: [] }), /COS_ACL/)
  assert.throws(() => privateAcl({ ACL: 'private', Grants: [{ Grantee: { URI: 'AllUsers' } }] }), /COS_PUBLIC/)
  memory.client.getBucketPolicy = async () => ({ Policy: { statement: [{ effect: 'allow', principal: { qcs: ['*'] } }] } })
  await assert.rejects(store.privacy(), /COS_PUBLIC_POLICY/)
  memory.client.getBucketPolicy = async () => ({ Policy: { statement: [] } })
  memory.client.getBucketVersioning = async () => ({ Status: 'Enabled' })
  await assert.rejects(store.privacy(), /COS_VERSIONING/)
  memory.objects.set('sample', Buffer.from('actual'))
  await assert.rejects(store.read('sample', { bytes: 3, sha256: sha256('actual') }))
  await assert.rejects(store.read('sample', { bytes: 9, sha256: sha256('actual') }), /SIZE_MISMATCH/)
  await assert.rejects(store.read('sample', { bytes: 6, sha256: sha256('forged') }), /DIGEST_MISMATCH/)
  let written = ''
  await store.read('sample', { bytes: 6, sha256: sha256('actual') }, async chunk => { written += chunk })
  assert.equal(written, 'actual')
})

test('no manifest on revoked approval; cloud corruption cannot produce local snapshot', async t => {
  const value = await fixture(t)
  const sealed = await publishRelease(await readReleaseConfig(value.file), { reviewed: true })
  const config = { ...value.config, prefix: 'builds/' }
  const memory = memoryClient()
  const store = new PrivateCosStore(config, memory.client)
  await assert.rejects(publishCos(config, sealed.releaseId,
    { store, authorize: async () => { throw new Error('APPROVAL_REVOKED') } }), /APPROVAL_REVOKED/)
  assert.equal(memory.objects.size, 0)
  await publishCos(config, sealed.releaseId, { store, authorize: async () => {} })
  const first = [...memory.objects.keys()].find(key => key.includes('/artifacts/'))
  memory.objects.set(first, Buffer.from('corrupted'))
  const vaultRoot = path.join(value.root, 'downloaded-vault')
  await fs.mkdir(vaultRoot, { mode: 0o700 })
  await assert.rejects(fetchCos({ ...config, vaultRoot }, sealed.releaseId, { store }))
  assert.ok(!(await fs.readdir(vaultRoot)).includes(sealed.releaseId))
})

test('COS config rejects unknown keys and overlapping source/build prefix; SDK requires environment credentials', async t => {
  const value = await fixture(t)
  const config = { protocolVersion: 1, repositoryId: 'local-test', vaultRoot: value.vaultRoot,
    bucket: 'test-100001', region: 'ap-beijing', prefix: 'builds/', sourcePrefix: 'sources/' }
  await write(value.root, 'cos.json', JSON.stringify(config))
  assert.equal((await readCosConfig(path.join(value.root, 'cos.json'))).prefix, 'builds/')
  for (const override of [{ sourcePrefix: 'builds/source/' }, { bucket: ['test-100001'] }, { secretKey: 'not-allowed' }]) {
    await write(value.root, 'cos.json', JSON.stringify({ ...config, ...override }))
    await assert.rejects(readCosConfig(path.join(value.root, 'cos.json')))
  }
  const savedId = process.env.FORGE_ARTIFACT_COS_SECRET_ID
  const savedKey = process.env.FORGE_ARTIFACT_COS_SECRET_KEY
  try {
    delete process.env.FORGE_ARTIFACT_COS_SECRET_ID
    delete process.env.FORGE_ARTIFACT_COS_SECRET_KEY
    await assert.rejects(cosClient(), /COS_CREDENTIAL_REQUIRED/)
    process.env.FORGE_ARTIFACT_COS_SECRET_ID = 'synthetic-not-a-credential'
    process.env.FORGE_ARTIFACT_COS_SECRET_KEY = 'synthetic-not-a-credential'
    const sdk = await cosClient()
    assert.equal(typeof sdk.putObject, 'function')
    assert.equal(sdk.options.FollowRedirect, false)
  }
  finally {
    if (savedId === undefined) delete process.env.FORGE_ARTIFACT_COS_SECRET_ID
    else process.env.FORGE_ARTIFACT_COS_SECRET_ID = savedId
    if (savedKey === undefined) delete process.env.FORGE_ARTIFACT_COS_SECRET_KEY
    else process.env.FORGE_ARTIFACT_COS_SECRET_KEY = savedKey
  }
})
