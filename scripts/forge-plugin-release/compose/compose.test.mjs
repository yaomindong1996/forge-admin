import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs/promises'
import path from 'node:path'
import { fixture, write } from '../fixtures/helpers.mjs'
import { publishRelease } from '../vault.mjs'
import { readReleaseConfig } from '../config.mjs'
import { readComposeConfig } from './config.mjs'
import { inspectCompose, prepareCompose, verifyCompose } from './prepare.mjs'
import { runComposeCli } from './index.mjs'

async function composeFixture(t, targets) {
  const value = await fixture(t, targets)
  const sealed = await publishRelease(await readReleaseConfig(value.file), { reviewed: true })
  const outputRoot = path.join(value.root, 'compose')
  const configRoot = path.join(value.root, 'runtime-config')
  await fs.mkdir(outputRoot, { mode: 0o700 })
  await fs.mkdir(configRoot, { mode: 0o700 })
  await write(configRoot, 'application.yml', 'synthetic-external-private-config')
  await write(configRoot, 'nginx.conf', 'synthetic-external-nginx-config')
  const config = { protocolVersion: 1, repositoryId: 'local-test', vaultRoot: value.vaultRoot, outputRoot,
    configRoot, projectName: 'test-runtime', serverImage: `java-runtime@sha256:${'a'.repeat(64)}`,
    uiImage: `nginx-runtime@sha256:${'b'.repeat(64)}`, serverPort: 18580, uiPort: 13000 }
  const configFile = path.join(value.root, 'compose-config.json')
  await write(value.root, 'compose-config.json', JSON.stringify(config))
  return { ...value, sealed, config, configFile }
}

test('Compose check is read-only; prepare copies artifacts, not secrets or deployed status', async t => {
  const value = await composeFixture(t)
  const config = await readComposeConfig(value.configFile)
  const checked = await inspectCompose(config, value.sealed.releaseId)
  assert.equal(checked.summary.prepared, false)
  assert.deepEqual(await fs.readdir(config.outputRoot), [])
  const result = await prepareCompose(config, value.sealed.releaseId, { reviewed: true })
  assert.equal(result.prepared, true)
  assert.equal(result.deployed, false)
  assert.equal(result.liveTaskApprovalVerified, false)
  assert.equal(result.requiresRootless, true)
  const compose = JSON.parse(await fs.readFile(path.join(result.packageRoot, 'compose.json')))
  for (const service of Object.values(compose.services)) {
    assert.equal(service.pull_policy, 'never')
    assert.equal(service.read_only, true)
    assert.deepEqual(service.cap_drop, ['ALL'])
    assert.equal(service.ports[0].host_ip, '127.0.0.1')
    assert.ok(service.volumes.every(volume => volume.read_only && !volume.bind.create_host_path))
  }
  const names = await fs.readdir(result.packageRoot)
  assert.deepEqual(names.sort(), ['artifacts', 'compose.json', 'manifest.json', 'preparation.json'])
  for (const artifact of value.result.artifacts) {
    const copied = path.join(result.packageRoot, 'artifacts', artifact.path)
    assert.deepEqual(await fs.readFile(copied), await fs.readFile(path.join(value.artifactRoot, artifact.path)))
    assert.equal((await fs.stat(copied)).nlink, 1)
  }
  assert.ok(!JSON.stringify(compose).includes('synthetic-external-private-config'))
  assert.equal((await verifyCompose(config, value.sealed.releaseId)).packageVerified, true)
  await assert.rejects(prepareCompose(config, value.sealed.releaseId, { reviewed: true }), /COMPOSE_PACKAGE_EXISTS/)
})

for (const kind of ['receipt', 'compose', 'bytes']) {
  test(`verify refuses prepared package ${kind} tampering, even if permissions are restored`, async t => {
    const value = await composeFixture(t)
    const config = await readComposeConfig(value.configFile)
    const prepared = await prepareCompose(config, value.sealed.releaseId, { reviewed: true })
    const name = kind === 'receipt' ? 'preparation.json' : kind === 'compose'
      ? 'compose.json' : 'artifacts/frontend/index.html'
    const file = path.join(prepared.packageRoot, name)
    await fs.chmod(file, 0o600)
    await fs.writeFile(file, 'tampered fixture')
    await fs.chmod(file, 0o400)
    await assert.rejects(verifyCompose(config, value.sealed.releaseId))
  })
}

for (const targets of [{ server: 'server-only' }, { ui: 'ui-only' }]) {
  test(`Compose preserves independent target ${Object.keys(targets)[0]}`, async t => {
    const value = await composeFixture(t, targets)
    const result = await prepareCompose(await readComposeConfig(value.configFile), value.sealed.releaseId,
      { reviewed: true })
    const compose = JSON.parse(await fs.readFile(path.join(result.packageRoot, 'compose.json')))
    assert.deepEqual(Object.keys(compose.services), Object.keys(targets))
  })
}

test('unreviewed / interrupted preparation never produces package', async t => {
  const value = await composeFixture(t)
  const config = await readComposeConfig(value.configFile)
  await assert.rejects(prepareCompose(config, value.sealed.releaseId), /COMPOSE_REVIEW_REQUIRED/)
  const controller = new AbortController()
  controller.abort()
  await assert.rejects(prepareCompose(config, value.sealed.releaseId,
    { reviewed: true, signal: controller.signal }), /INTERRUPTED/)
  assert.deepEqual(await fs.readdir(config.outputRoot), [])
})

test('private config requires owned regular file, no links or public access', async t => {
  const value = await composeFixture(t)
  const config = await readComposeConfig(value.configFile)
  const file = path.join(config.configRoot, 'application.yml')
  await fs.chmod(file, 0o644)
  await assert.rejects(inspectCompose(config, value.sealed.releaseId), /COMPOSE_PRIVATE_CONFIG_REQUIRED/)
  await fs.chmod(file, 0o600)
  await fs.link(file, path.join(config.configRoot, 'hardlinked.yml'))
  await assert.rejects(inspectCompose(config, value.sealed.releaseId), /COMPOSE_PRIVATE_CONFIG_REQUIRED/)
  assert.deepEqual(await fs.readdir(config.outputRoot), [])
})

for (const override of [{ serverImage: 'java:latest' }, { projectName: ['test-runtime'] },
  { uiPort: 80 }, { uiPort: 18580 }, { command: 'arbitrary-command' }]) {
  test(`Compose rejects unsafe config ${JSON.stringify(override)}`, async t => {
    const value = await composeFixture(t)
    await write(value.root, 'compose-config.json', JSON.stringify({ ...value.config, ...override }))
    await assert.rejects(readComposeConfig(value.configFile))
    assert.deepEqual(await fs.readdir(value.config.outputRoot), [])
  })
}

test('Compose rejects overlapping roots and corrupted sealed artifact', async t => {
  const value = await composeFixture(t)
  await write(value.root, 'compose-config.json', JSON.stringify({ ...value.config, outputRoot: value.vaultRoot }))
  await assert.rejects(readComposeConfig(value.configFile), /COMPOSE_ROOT_OVERLAP/)
  const artifact = path.join(value.vaultRoot, value.sealed.releaseId, 'artifacts/frontend/index.html')
  await fs.chmod(artifact, 0o600)
  await fs.writeFile(artifact, 'corrupted artifact')
  await assert.rejects(prepareCompose(value.config, value.sealed.releaseId, { reviewed: true }))
  assert.deepEqual(await fs.readdir(value.config.outputRoot), [])
})

test('CLI only has check / reviewed prepare, no deploy shortcut or raw sensitive error', async t => {
  const value = await composeFixture(t)
  const messages = []
  const output = { log: message => messages.push(message), error: message => messages.push(message) }
  for (const args of [['deploy', value.configFile, value.sealed.releaseId],
    ['prepare', value.configFile, value.sealed.releaseId]]) {
    assert.equal(await runComposeCli(args, output), 1)
  }
  assert.equal(await runComposeCli(['check', value.configFile, value.sealed.releaseId], output), 0)
  assert.equal(JSON.parse(messages[2]).deployed, false)
  assert.deepEqual(await fs.readdir(value.config.outputRoot), [])
})
