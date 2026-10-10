import assert from 'node:assert/strict'
import fs from 'node:fs/promises'
import path from 'node:path'
import test from 'node:test'
import { parseStrictJson } from './json.mjs'
import { readZip } from './zip.mjs'
import { readBundle } from './bundle.mjs'
import { validateDescriptor, readDescriptor } from './descriptor.mjs'
import { parsePom } from './xml.mjs'
import { validateRelative } from './paths.mjs'
import { compareVersions, parseVersion, satisfiesVersion } from '../forge-shared/version.mjs'
import { descriptor, pluginFiles, repository, temporary, write, zip } from './fixtures/helpers.mjs'

test('semantic versions: release/prerelease/build and large numeric rules', () => {
  const versions = ['1.0.0-alpha', '1.0.0-alpha.1', '1.0.0-alpha.beta', '1.0.0-beta', '1.0.0-beta.2',
    '1.0.0-beta.11', '1.0.0-rc.1', '1.0.0', '1.2.0', '999999999999999999999.0.0']
  for (let index = 1; index < versions.length; index++) {
    assert.equal(compareVersions(versions[index - 1], versions[index]), -1)
  }
  assert.equal(compareVersions('1.0.0+a', '1.0.0+b'), 0)
  assert.ok(satisfiesVersion('1.2.0', '>=1.2.0 <2.0.0'))
  assert.ok(!satisfiesVersion('1.2.0-beta', '>=1.2.0'))
  assert.throws(() => satisfiesVersion('1.0.0', '>9.0.0 rubbish'))
})

for (const version of ['1.2', '01.2.0', '${revision}', '1.2.0-01', '1.2.0-a..b', '1.2.0+a_b']) {
  test(`invalid semantic version ${version}`, () => assert.throws(() => parseVersion(version)))
}

for (const text of ['{"id":1,"id":2}', '{"a":{"x":1,"x":2}}', '{"id":1,"\\u0069d":2}',
  '{"__proto__":{}}', '{"constructor":{}}', '{"x":NaN}', '[] trailing']) {
  test(`strict JSON rejects ${text}`, () => assert.throws(() => parseStrictJson(text)))
}

test('strict JSON arrays/escaping and depth/byte limits', () => {
  const value = { list: [null, true, false, -0.12e3, { text: 'a " b \\ c' }] }
  assert.deepEqual(parseStrictJson(JSON.stringify(value)), value)
  assert.throws(() => parseStrictJson('[0]', 1))
  assert.throws(() => parseStrictJson('['.repeat(40) + '0' + ']'.repeat(40)))
})

const badDescriptors = [
  { id: '../outside' }, { id: 'A' }, { id: ['demo'] }, { name: '' }, { version: 'bad' }, { edition: 'ce' },
  { requiresCore: '>=2.0.0' }, { requiresCore: '<1.0.0 invalid' }, { unknown: 'field' },
  { features: ['ee.demo'] }, { edition: 'ee', features: ['community.demo'] }, { features: ['a', 'a'] },
  { features: ['A'] }, { server: { module: ['forge-plugin-demo'] } }, { server: { module: '../evil' } },
  { server: { module: 'valid', extra: true } },
  { ui: { dir: '../ui' } }, { ui: { dir: '/ui' } }, { ui: { dir: 'a//b' } }, { server: null, ui: null },
]
for (const override of badDescriptors) {
  test(`descriptor rejects ${JSON.stringify(override)}`, () => {
    assert.throws(() => validateDescriptor(descriptor(override), '1.2.0'))
  })
}

test('runtime descriptor equality ignores key order, not field changes', () => {
  const metadata = descriptor()
  const files = pluginFiles(metadata)
  const runtime = 'server/forge-plugin-demo/src/main/resources/META-INF/forge-plugin.json'
  files.set(runtime, Buffer.from(JSON.stringify(Object.fromEntries(Object.entries(metadata).reverse()))))
  assert.equal(readDescriptor(files, '1.2.0').id, 'demo')
  files.set(runtime, Buffer.from(JSON.stringify({ ...metadata, version: '1.0.1' })))
  assert.throws(() => readDescriptor(files, '1.2.0'), /不一致/)
  files.delete(runtime)
  assert.throws(() => readDescriptor(files, '1.2.0'), /运行时描述/)
})

test('invalid UTF-8 descriptor is refused before JSON replacement decoding', () => {
  const files = pluginFiles()
  const text = JSON.stringify(descriptor()).replace('测试插件', 'TEST')
  const raw = Buffer.from(text)
  raw[raw.indexOf('TEST')] = 0xff
  files.set('forge-plugin.json', raw)
  assert.throws(() => readDescriptor(files, '1.2.0'))
})

for (const relative of ['../a', '/a', 'C:/a', 'a\\b', 'a//b', 'a/./b', 'a\0b', 'a.', 'CON', 'a/NUL.txt']) {
  test(`unsafe path ${JSON.stringify(relative)}`, () => assert.throws(() => validateRelative(relative)))
}

for (const method of [0, 8]) {
  test(`ZIP ${method} and directory read the same files`, async t => {
    const files = pluginFiles()
    assert.deepEqual(readZip(zip(files, { method })), files)
    const root = await temporary(t)
    for (const [relative, data] of files) {
      await write(root, relative, data)
    }
    await write(root, 'node_modules/ignored', 'ignored')
    await write(root, '.env.local', 'ignored')
    assert.deepEqual((await readBundle(root)).files, files)
  })
}

for (const names of [['../escape'], ['/escape'], ['a\\b'], ['a', 'A'], ['a', 'a/b'], ['a/', 'A/']]) {
  test(`unsafe ZIP names ${JSON.stringify(names)}`, () => {
    assert.throws(() => readZip(zip(new Map(names.map(name => [name, name.endsWith('/') ? '' : 'x'])))))
  })
}

for (const options of [{ flags: 1 }, { method: 12 }, { mode: 0xa1ff }, { mode: 0x21ff }]) {
  test(`ZIP unsupported ${JSON.stringify(options)}`, () => {
    assert.throws(() => readZip(zip(new Map([['a', 'hello']]), options)))
  })
}

test('ZIP truncation, CRC corruption, huge output and ZIP64 rejected', () => {
  const valid = zip(new Map([['a', 'hello']]))
  assert.throws(() => readZip(valid.subarray(0, -1)))
  const corrupt = Buffer.from(valid)
  corrupt[31] ^= 0xff
  assert.throws(() => readZip(corrupt), /CRC/)
  const huge = Buffer.from(valid)
  const central = huge.indexOf(Buffer.from([0x50, 0x4b, 0x01, 0x02]))
  huge.writeUInt32LE(0xffffffff, central + 24)
  assert.throws(() => readZip(huge), /大小/)
  const zip64 = Buffer.from(valid)
  zip64.writeUInt16LE(45, central + 6)
  assert.throws(() => readZip(zip64), /ZIP64/)
})

test('directory and root symbolic links rejected', async t => {
  const root = await temporary(t)
  await write(root, 'a', 'x')
  await fs.symlink(path.join(root, 'a'), path.join(root, 'link'))
  await assert.rejects(readBundle(root), /链接/)
  const outer = await temporary(t)
  await fs.symlink(root, path.join(outer, 'package'))
  await assert.rejects(readBundle(path.join(outer, 'package')), /软链接/)
})

test('XML parses real POM and rejects unsafe/malformed XML', async () => {
  assert.equal(parsePom(await fs.readFile(path.join(repository, 'forge-server/pom.xml'), 'utf8')).name, 'project')
  for (const value of ['<!DOCTYPE project><project/>', '<project><a></b></project>', '<project/>x',
    '<project/><project/>', '<project>&evil;</project>']) {
    assert.throws(() => parsePom(value))
  }
})
