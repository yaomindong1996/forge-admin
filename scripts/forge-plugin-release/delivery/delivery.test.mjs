import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs/promises'
import path from 'node:path'
import net from 'node:net'
import { randomUUID } from 'node:crypto'
import { fixture, write } from '../fixtures/helpers.mjs'
import { publishRelease } from '../vault.mjs'
import { readReleaseConfig } from '../config.mjs'
import { PrivateCosStore } from '../cos/store.mjs'
import { memoryClient } from '../cos/fixtures/helpers.mjs'
import { publishCos } from '../cos/transfer.mjs'
import { composeDocument } from '../compose/document.mjs'
import { executeDelivery } from './executor.mjs'
import { recoverLock } from './session.mjs'
import { assertCurrent, verifyTarget, verifyContainers } from './docker.mjs'
import { verifyRuntime, readHttp } from './runtime.mjs'
import { readDeliveryConfig } from './config.mjs'
import { sha256 } from '../../forge-plugin-builder/files.mjs'

async function setup(t, action = 'publish') {
  const value = await fixture(t)
  const sealed = await publishRelease(await readReleaseConfig(value.file), { reviewed: true })
  const manifest = JSON.parse(await fs.readFile(path.join(value.vaultRoot, sealed.releaseId, 'manifest.json')))
  const stateRoot = path.join(value.root, 'state')
  const outputRoot = path.join(value.root, 'compose')
  const configRoot = path.join(value.root, 'config')
  for (const dir of [stateRoot, outputRoot, configRoot]) await fs.mkdir(dir, { mode: 0o700 })
  await write(configRoot, 'application.yml', 'synthetic-private-config')
  await write(configRoot, 'nginx.conf', 'synthetic-private-config')
  const config = { apiBaseUrl: 'https://example.invalid', workerId: 'delivery-worker', targetId: 'test', stateRoot,
    dockerExecutable: '/usr/bin/true', clientConfigRoot: configRoot,
    cos: { repositoryId: 'local-test', vaultRoot: value.vaultRoot, prefix: 'builds/' },
    compose: { repositoryId: 'local-test', vaultRoot: value.vaultRoot, outputRoot, configRoot,
      projectName: 'test', serverImage: 'java@sha256:' + 'a'.repeat(64),
      uiImage: 'nginx@sha256:' + 'b'.repeat(64), serverPort: 18580, uiPort: 13000 } }
  const id = randomUUID()
  const metadata = { repositoryId: manifest.repositoryId, releaseId: sealed.releaseId,
    manifestSha256: sealed.releaseId.slice(4), pluginId: manifest.plugin.id, pluginVersion: manifest.plugin.version,
    coreVersion: manifest.plugin.coreVersion, resultSha256: manifest.resultSha256,
    result: { packageSha256: manifest.packageSha256, sourceCommit: manifest.source.commit, image: manifest.image,
      sourceSha256: manifest.source.sha256, artifactManifestSha256: manifest.artifactManifestSha256,
      jobId: manifest.jobId, artifactCount: manifest.artifacts.length,
      artifactBytes: manifest.artifacts.reduce((sum, row) => sum + row.bytes, 0) } }
  const memory = memoryClient()
  const store = new PrivateCosStore(config.cos, memory.client)
  const calls = []
  const request = async (connection, operation, body) => {
    calls.push({ operation, body })
    if (operation === 'finish') return { id, status: body.status }
    return { id, targetId: 'test', workerId: 'delivery-worker', status: 'running', action,
      releaseId: sealed.releaseId, previousReleaseId: null, unverifiedReleaseId: null,
      metadata, nonce: body.nonce, lease: body.lease }
  }
  const token = process.env.FORGE_PLUGIN_DELIVERY_TOKEN
  const probe = process.env.FORGE_RUNTIME_PROBE_TOKEN
  process.env.FORGE_PLUGIN_DELIVERY_TOKEN = '1'.repeat(64)
  process.env.FORGE_RUNTIME_PROBE_TOKEN = '2'.repeat(64)
  t.after(() => {
    if (token === undefined) delete process.env.FORGE_PLUGIN_DELIVERY_TOKEN
    else process.env.FORGE_PLUGIN_DELIVERY_TOKEN = token
    if (probe === undefined) delete process.env.FORGE_RUNTIME_PROBE_TOKEN
    else process.env.FORGE_RUNTIME_PROBE_TOKEN = probe
  })
  return { ...value, config, sealed, manifest, metadata, id, store, calls, request }
}

test('publish actual stream bytes with renewed approval and distinct success receipt; no secret in audit', async t => {
  const value = await setup(t)
  const result = await executeDelivery(value.config, value.id, { reviewed: true }, value)
  assert.equal(result.deployed, false)
  assert.equal(value.calls.at(-1).body.runtimeVerified, false)
  assert.equal(value.calls.at(-1).body.cosVerified, true)
  const audit = await fs.readFile(path.join(value.config.stateRoot, value.id + '.json'), 'utf8')
  assert.ok(!audit.includes('1'.repeat(64)))
  assert.deepEqual(await fs.readdir(value.config.stateRoot), [value.id + '.json'])
})

test('executor configuration accepts private canonical targets but rejects aliases, credentials and commands', async t => {
  const value = await setup(t)
  const empty = path.join(value.root, 'docker-empty')
  await fs.mkdir(empty, { mode: 0o700 })
  const cos = { ...value.config.cos, protocolVersion: 1, bucket: 'test-1250000000', region: 'ap-beijing',
    sourcePrefix: 'sources/' }
  const compose = { ...value.config.compose, protocolVersion: 1 }
  await write(value.root, 'cos.json', JSON.stringify(cos))
  await write(value.root, 'compose.json', JSON.stringify(compose))
  const document = { protocolVersion: 1, apiBaseUrl: 'https://example.invalid', workerId: 'delivery-worker',
    targetId: 'test', cosConfig: path.join(value.root, 'cos.json'),
    composeConfig: path.join(value.root, 'compose.json'), dockerExecutable: await fs.realpath(process.execPath),
    dockerSocket: path.join(value.root, 'socket/docker.sock'), clientConfigRoot: empty,
    stateRoot: value.config.stateRoot }
  const file = path.join(value.root, 'delivery.json')
  await write(value.root, 'delivery.json', JSON.stringify(document))
  assert.equal((await readDeliveryConfig(file)).targetId, 'test')
  for (const change of [{ workerId: ['delivery-worker'] }, { targetId: ['test'] }, { token: 'not-allowed' },
    { command: 'not-allowed' }, { stateRoot: value.vaultRoot }, { clientConfigRoot: value.config.compose.configRoot }]) {
    await write(value.root, 'delivery.json', JSON.stringify({ ...document, ...change }))
    await assert.rejects(readDeliveryConfig(file))
  }
  const alias = path.join(value.root, 'docker-alias')
  await fs.symlink(document.dockerExecutable, alias)
  await write(value.root, 'delivery.json', JSON.stringify({ ...document, dockerExecutable: alias }))
  await assert.rejects(readDeliveryConfig(file), /EXECUTABLE_UNSAFE/)
})

test('bad claim nonce keeps uncertain local lock; only authenticated manual close can archive it', async t => {
  const value = await setup(t)
  const bad = async (...args) => ({ ...await value.request(...args), nonce: randomUUID() })
  await assert.rejects(executeDelivery(value.config, value.id, { reviewed: true }, { ...value, request: bad }),
    /DELIVERY_RESULT_UNCONFIRMED/)
  await assert.rejects(executeDelivery(value.config, value.id, { reviewed: true }, value), /DELIVERY_TARGET_BUSY/)
  const rejected = async (connection, operation, body) => ({ id: value.id, targetId: 'test', status: 'running', nonce: body.nonce })
  await assert.rejects(recoverLock(value.config, value.id, { reviewed: true }, { request: rejected }),
    /RECOVERY_NOT_CONFIRMED/)
  const request = async (connection, operation, body) => {
    assert.equal(operation, 'recovery')
    return { id: value.id, targetId: 'test', status: 'reconciled', nonce: body.nonce }
  }
  assert.equal((await recoverLock(value.config, value.id, { reviewed: true }, { request })).lockArchived, true)
  assert.deepEqual(await fs.readdir(value.config.stateRoot), ['closed-' + value.id])
})

async function dockerFixture(t) {
  const value = await setup(t, 'deploy')
  await publishCos(value.config.cos, value.sealed.releaseId, { store: value.store, authorize: async () => {} })
  const socketRoot = path.join(value.root, 'socket')
  await fs.mkdir(socketRoot, { mode: 0o700 })
  const server = net.createServer()
  value.config.dockerSocket = path.join(socketRoot, 'docker.sock')
  await new Promise((resolve, reject) => server.once('error', reject).listen(value.config.dockerSocket, resolve))
  t.after(() => new Promise(resolve => server.close(resolve)))
  const document = composeDocument(value.config.compose, { ...value.manifest, digest: value.sealed.releaseId.slice(4) })
  const rows = Object.entries(document.services).map(([name, spec]) => ({ State: { Running: true },
    Config: { Image: spec.image, User: spec.user, Labels: { ...spec.labels,
      'com.docker.compose.project': 'test', 'com.docker.compose.service': name } },
    HostConfig: { ReadonlyRootfs: true, Privileged: false, CapDrop: ['ALL'], SecurityOpt: ['no-new-privileges'],
      Memory: 2147483648, MemorySwap: 2147483648, NanoCpus: 2000000000, PidsLimit: 256 },
    Mounts: spec.volumes.map(bind => ({ Type: 'bind', RW: false, Destination: bind.target,
      Source: path.resolve(value.config.compose.outputRoot, value.sealed.releaseId, bind.source) })) }))
  const argv = []
  value.execute = async (executable, args) => {
    argv.push(args)
    const command = args.slice(2)
    if (command[0] === 'info') return Buffer.from(JSON.stringify({ SecurityOptions: ['name=rootless'],
      CgroupVersion: '2', CgroupDriver: 'systemd', MemoryLimit: true, SwapLimit: true,
      CpuCfsPeriod: true, CpuCfsQuota: true, PidsLimit: true }))
    if (command[0] === 'image') return Buffer.from(JSON.stringify([command[2]]))
    if (command[1] === 'version') return Buffer.from('2.30.0')
    if (command[0] === 'ps') return Buffer.from('')
    if (command.includes('ps')) return Buffer.from('id1\nid2')
    if (command[0] === 'inspect') return Buffer.from(JSON.stringify(rows))
    return Buffer.from('')
  }
  value.fetch = async (url, options) => {
    if (url.includes('/probe')) return new Response(JSON.stringify({ code: 200, data: {
      nonce: options.headers['X-Forge-Probe-Nonce'], coreVersion: value.manifest.plugin.coreVersion,
      jarSha256: value.manifest.artifacts.find(row => row.path === 'backend/admin.jar').sha256,
      plugins: [{ id: 'example', version: '1.0.0', backendLoaded: true }] } }))
    return new Response(await fs.readFile(path.join(value.artifactRoot, 'frontend/index.html')))
  }
  return { ...value, argv, rows }
}

test('real executor orchestration verifies fixed Compose argv, actual mounts and runtime bytes before success', async t => {
  const value = await dockerFixture(t)
  assert.equal((await executeDelivery(value.config, value.id, { reviewed: true }, value)).runtimeVerified, true)
  const { argv, rows } = value
  const up = argv.find(args => args.includes('up'))
  assert.ok(up.includes('--no-build') && up.includes('never') && !up.includes('down'))
  assert.equal(value.calls.at(-1).body.runtimeVerified, true)
  const client = await verifyTarget(value.config, value.manifest, value.execute)
  rows[0].Mounts[0].RW = true
  await assert.rejects(verifyContainers(client, value.config, value.sealed.releaseId, value.manifest), /MOUNT_MISMATCH/)
  rows[0].Mounts[0].RW = false
  rows[0].HostConfig.Memory = 0
  await assert.rejects(verifyContainers(client, value.config, value.sealed.releaseId, value.manifest), /CONTAINER_INVALID/)
})

test('runtime mismatch after switch remains uncertain and retains local lock even after server receipt', async t => {
  const value = await dockerFixture(t)
  value.fetch = async () => new Response(JSON.stringify({ code: 200, data: { nonce: 'foreign' } }))
  await assert.rejects(executeDelivery(value.config, value.id, { reviewed: true }, value), /PROBE_MISMATCH/)
  assert.equal(value.calls.at(-1).body.status, 'uncertain')
  assert.equal(value.calls.at(-1).body.runtimeVerified, false)
  assert.ok(value.argv.some(args => args.includes('up')))
  assert.ok(!value.argv.some(args => args.includes('down') || args.includes('rm')))
  assert.deepEqual(await fs.readdir(value.config.stateRoot), ['test.lock'])
  await assert.rejects(executeDelivery(value.config, value.id, { reviewed: true }, value), /DELIVERY_TARGET_BUSY/)
})

test('configuration failure before up is failed without switching or retaining a resolved target lock', async t => {
  const value = await dockerFixture(t)
  const execute = value.execute
  value.execute = async (file, args) => {
    if (args.includes('config')) {
      throw Object.assign(new Error('synthetic configuration failure'), { code: 'COMPOSE_CONFIG_REJECTED' })
    }
    return execute(file, args)
  }
  await assert.rejects(executeDelivery(value.config, value.id, { reviewed: true }, value), /CONFIG_REJECTED/)
  assert.equal(value.calls.at(-1).body.status, 'failed')
  assert.ok(!value.argv.some(args => args.includes('up')))
  assert.deepEqual(await fs.readdir(value.config.stateRoot), [])
})

test('runtime refuses foreign nonce/JAR/plugin/UI bytes and oversized response; startup retry is bounded', async t => {
  const value = await setup(t)
  let attempts = 0
  const fetcher = async () => { attempts++; return new Response('not ready', { status: 503 }) }
  await assert.rejects(verifyRuntime(value.config.compose, value.manifest,
    { fetch: fetcher, startupTimeoutMs: 2 }), /STARTUP_TIMEOUT/)
  assert.ok(attempts >= 1)
  await assert.rejects(verifyRuntime(value.config.compose, value.manifest,
    { fetch: async () => new Response(JSON.stringify({ code: 200, data: { nonce: 'foreign' } })) }), /PROBE_MISMATCH/)
  await assert.rejects(readHttp(new Response('too big'), { bytes: 2 }), /SIZE_LIMIT/)
  await assert.rejects(readHttp(new Response('actual'), { bytes: 6, sha256: sha256('forged') }), /DIGEST_MISMATCH/)
  await assert.rejects(readHttp(new Response('actual', { status: 401 }), { bytes: 6 }), /HTTP_REJECTED/)
})

test('cannot overwrite an unmanaged or foreign previous Compose project', async () => {
  const config = { compose: { projectName: 'test' } }
  const client = async args => Buffer.from(args[0] === 'ps' ? 'container'
    : JSON.stringify([{ Config: { Labels: { 'forge.release-id': 'foreign',
      'com.docker.compose.service': 'server', 'com.docker.compose.project': 'test' } } }]))
  await assert.rejects(assertCurrent(client, config, { previousReleaseId: null }), /UNMANAGED/)
  await assert.rejects(assertCurrent(client, config, { previousReleaseId: 'confirmed' }), /PREVIOUS_MISMATCH/)
  await assert.rejects(assertCurrent(client, config,
    { previousReleaseId: 'confirmed', unverifiedReleaseId: 'foreign' }, { targets: ['server', 'ui'] }),
  /TARGET_PROFILE_CHANGED/)
})
