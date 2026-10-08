import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs/promises'
import path from 'node:path'
import { descriptor, snapshot, pluginFiles, zip } from '../fixtures/helpers.mjs'
import { endpoint, archiveLimit } from './config.mjs'
import { runMarket } from './commands.mjs'
import { validateRelease } from './catalog.mjs'
import { cacheSource } from './source.mjs'
import { marketFixture, sha256 } from './fixtures/helpers.mjs'

test('market URL only accepts HTTPS or literal local HTTP, never credentials/query/alias', () => {
  for (const value of ['https://plugins.example.invalid/api-base', 'http://127.0.0.1:38001']) {
    assert.equal(endpoint(value), value)
  }
  for (const value of ['http://plugins.example.invalid', 'file:///tmp/x', 'https://user:secret@example.invalid',
    'https://example.invalid/?token=secret', 'https://example.invalid/#secret', 'http://127.1',
    'https://example.invalid/a/../b', 'https://example.invalid/%2f', 'https://example.invalid\\x']) {
    assert.throws(() => endpoint(value), /MARKET_URL_INVALID/)
  }
})

test('release accepts exact published long ID and rejects unsafe numeric IDs / overlimit / wrong status', () => {
  const value = { id: '9223372036854775807', version: '1.0.0', edition: 'community', requiresCore: '>=1.2.0',
    sha256: 'a'.repeat(64), archiveBytes: 1000, status: 'PUBLISHED' }
  assert.equal(validateRelease(value).id, value.id)
  for (const override of [{ id: Number(value.id) }, { id: '9223372036854775808' }, { status: 'WITHDRAWN' },
    { archiveBytes: archiveLimit + 1 }, { sha256: 'not-a-digest' }, { version: 'latest' }]) {
    assert.throws(() => validateRelease({ ...value, ...override }))
  }
})

test('owned and explicit versions use real API route contract, tokens never appear in output', async t => {
  const value = await marketFixture(t)
  assert.equal(await runMarket(['owned', value.file], value.output, value.environment), 0)
  assert.equal(await runMarket(['versions', value.file, 'demo'], value.output, value.environment), 0)
  assert.ok(value.requests.every(req => req.token === 'Bearer fixture-customer-token'
    && req.device === 'fixture-device-id'))
  assert.ok(!JSON.stringify(value.logs).includes('fixture-customer-token'))
  assert.equal(JSON.parse(value.logs[1]).versions[0].id, value.release.id)
})

for (const generated of [false, true]) {
  test(`real ZIP check / confirmed install reuse installer (generated=${generated})`, async t => {
    const value = await marketFixture(t, { generated })
    const before = await snapshot(value.root)
    assert.equal(await runMarket(['check', value.file, 'demo', '1.0.0'], value.output, value.environment), 0)
    assert.deepEqual(await snapshot(value.root), before)
    assert.equal(JSON.parse(value.logs[0]).installed, false)
    assert.equal(await runMarket(['add', value.file, 'demo', '1.0.0', '--reviewed'],
      value.output, value.environment), 0)
    const result = JSON.parse(value.logs[1])
    assert.equal(result.installed, true)
    assert.equal(result.deployed, false)
    const config = JSON.parse(await fs.readFile(path.join(value.root, 'forge.config.json')))
    assert.equal(config.plugins[0].version, '1.0.0')
    assert.ok(config.plugins[0].source.endsWith(`${sha256(value.data)}.zip`))
    const relative = `${value.server}/plugins/${generated ? 'core-plugin-demo' : 'forge-plugin-demo'}`
    assert.ok((await fs.stat(path.join(value.root, relative))).isDirectory())
  })
}

test('confirmed replacement still refuses local customized source, and session is erased after command', async t => {
  const value = await marketFixture(t)
  const active = { token: 'fixture-customer-token', deviceId: 'fixture-device-id' }
  value.environment.login = async () => active
  assert.equal(await runMarket(['add', value.file, 'demo', '1.0.0', '--reviewed'],
    value.output, value.environment), 0)
  assert.deepEqual(active, { token: '', deviceId: '' })
  const relative = `${value.server}/plugins/forge-plugin-demo/src/main/java/com/mdframe/forge/plugin/demo/Hello.java`
  await fs.writeFile(path.join(value.root, relative), 'customer customization')
  const before = await snapshot(value.root)
  value.environment.login = async () => ({ token: 'fixture-customer-token', deviceId: 'fixture-device-id' })
  assert.equal(await runMarket(['add', value.file, 'demo', '1.0.0', '--reviewed', '--force'],
    value.output, value.environment), 1)
  assert.deepEqual(await snapshot(value.root), before)
})

test('writable market endpoint config is rejected before customer login', async t => {
  const value = await marketFixture(t)
  await fs.chmod(value.file, 0o666)
  assert.equal(await runMarket(['owned', value.file], value.output, value.environment), 1)
  assert.deepEqual(value.errors, ['MARKET_CONFIG_UNSAFE'])
  assert.equal(value.requests.length, 0)
})

test('install without reviewed, duplicate flags and implicit latest fail before login', async t => {
  const value = await marketFixture(t)
  let logins = 0
  value.environment.login = async () => { logins++; throw new Error('session-secret') }
  for (const args of [['add', value.file, 'demo', '1.0.0'], ['check', value.file, 'demo', 'latest'],
    ['add', value.file, 'demo', '1.0.0', '--reviewed', '--reviewed']]) {
    assert.equal(await runMarket(args, value.output, value.environment), 1)
  }
  assert.equal(logins, 0)
  assert.equal(value.requests.length, 0)
  assert.ok(!value.errors.join().includes('session-secret'))
})

for (const scenario of ['access', 'version', 'compatible', 'digest', 'metadata']) {
  test(`market refuses ${scenario} mismatch before installing`, async t => {
    const metadata = descriptor({ id: scenario === 'metadata' ? 'different' : 'demo' })
    const data = zip(pluginFiles(metadata))
    const value = await marketFixture(t, { download: scenario === 'digest' ? Buffer.from('bad') : data })
    if (scenario === 'access') value.routes['/api/plugins/demo/access'] = false
    if (scenario === 'version') value.release.version = '2.0.0'
    if (scenario === 'compatible') value.release.requiresCore = '>=3.0.0'
    if (scenario === 'metadata') { value.release.sha256 = sha256(data); value.release.archiveBytes = data.length }
    const before = await snapshot(value.root)
    assert.equal(await runMarket(['add', value.file, 'demo', '1.0.0', '--reviewed'],
      value.output, value.environment), 1)
    assert.deepEqual(await snapshot(value.root), before)
    assert.ok(!value.errors.join().includes('fixture-customer-token'))
  })
}

test('existing cache must be private immutable bytes; hardlink / corruption is rejected', async t => {
  const value = await marketFixture(t)
  const hash = sha256(value.data)
  const file = await cacheSource(value.root, value.data, hash)
  assert.equal(await cacheSource(value.root, value.data, hash), file)
  await fs.link(file, path.join(value.root, 'linked.zip'))
  await assert.rejects(cacheSource(value.root, value.data, hash), /MARKET_CACHE_UNSAFE/)
  await fs.unlink(path.join(value.root, 'linked.zip'))
  await fs.chmod(file, 0o600)
  await assert.rejects(cacheSource(value.root, value.data, hash), /MARKET_CACHE_UNSAFE/)
  await fs.writeFile(file, Buffer.alloc(value.data.length))
  await fs.chmod(file, 0o400)
  await assert.rejects(cacheSource(value.root, value.data, hash), /MARKET_CACHE_CHANGED/)
})

test('cache parent symlink is rejected without writing outside project', async t => {
  const value = await marketFixture(t)
  await fs.symlink(path.dirname(value.root), path.join(value.root, '.forge-plugin'))
  await assert.rejects(cacheSource(value.root, value.data, sha256(value.data)))
})

for (const scenario of ['redirect', 'large', 'secret-error', 'duplicate-json', 'type']) {
  test(`HTTP ${scenario} fails safely without leaking server response`, async t => {
    const value = await marketFixture(t, { handler: (_req, res) => {
      if (scenario === 'redirect') { res.statusCode = 302; res.setHeader('Location', 'http://127.0.0.1:1') }
      res.setHeader('Content-Type', scenario === 'type' ? 'text/html' : 'application/json')
      if (scenario === 'large') res.setHeader('Content-Length', 65537)
      if (scenario === 'secret-error') res.statusCode = 500
      res.end('{"code":200,"code":500,"data":"server-secret"}')
      return true
    } })
    assert.equal(await runMarket(['owned', value.file], value.output, value.environment), 1)
    assert.ok(![...value.logs, ...value.errors].join().includes('server-secret'))
  })
}
