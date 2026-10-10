import assert from 'node:assert/strict'
import fs from 'node:fs/promises'
import path from 'node:path'
import test from 'node:test'
import { editBlock, validateSourcePom, normalizeParentPath } from './pom.mjs'
import { project, plugin, pluginFiles, descriptor, snapshot, write, temporary } from './fixtures/helpers.mjs'
import { addPlugin, removePlugin } from './installer.mjs'

for (const newline of ['\n', '\r\n']) {
  test(`POM only changes marked body, restores original bytes ${JSON.stringify(newline)}`, () => {
    const text = '<project><modules>\n  <module>core</module>\n  <!-- forge-plugins:modules:begin -->\n'
      + '  <!-- forge-plugins:modules:end -->\n</modules><properties><custom>kept</custom></properties></project>'
    const before = text.replaceAll('\n', newline)
    const item = '<module>plugins/demo</module>'
    const installed = editBlock(before, 'modules', { old: [], next: [item] })
    assert.equal(editBlock(installed, 'modules', { old: [item], next: [] }), before)
    assert.match(installed, /<module>core<\/module>/)
    assert.throws(() => editBlock(before, 'modules', { old: [], next: ['<module>core</module>'] }), /重名/)
  })
}

const malformed = [
  '<!-- forge-plugins:modules:begin -->',
  '<!-- forge-plugins:modules:end -->\n<!-- forge-plugins:modules:begin -->',
  '<!-- forge-plugins:modules:begin -->\n<!-- forge-plugins:modules:begin -->\n<!-- forge-plugins:modules:end -->',
  '<!-- forge-plugins:modules:begin -->\n<module>unregistered</module>\n<!-- forge-plugins:modules:end -->',
  '<!-- forge-plugins:modules:begin -->\n<!-- user comment -->\n<!-- forge-plugins:modules:end -->',
]
for (const body of malformed) {
  test(`POM rejects malformed or foreign block ${body.slice(0, 48)}`, () => {
    const text = `<project><modules>\n${body}\n</modules></project>`
    assert.throws(() => editBlock(text, 'modules', { old: [], next: [] }))
  })
}

test('nested markers and dependencyManagement markers do not satisfy direct block contract', () => {
  const body = '<!-- forge-plugins:dependencies:begin -->\n<!-- forge-plugins:dependencies:end -->'
  const text = `<project><dependencyManagement><dependencies>\n${body}\n</dependencies></dependencyManagement>`
    + '<dependencies/></project>'
  assert.throws(() => editBlock(text, 'dependencies', { old: [], next: [] }), /直属/)
})

test('POM parent path is normalized or inserted without altering other bytes', () => {
  const text = pluginFiles().get('server/forge-plugin-demo/pom.xml').toString('utf8')
  validateSourcePom(text, 'forge-plugin-demo')
  assert.equal(normalizeParentPath(text), text)
  const omitted = text.replace('<relativePath>../../pom.xml</relativePath>', '')
  assert.equal(normalizeParentPath(omitted), text)
  assert.equal(normalizeParentPath(text.replace('<relativePath>../../pom.xml</relativePath>', '<relativePath/>')), text)
})

for (const replacement of [
  ['<version>${revision}</version>', '<version>1.0.0</version>'],
  ['</project>', '<version>1.0.0</version></project>'],
  ['</project>', '<properties><revision>1.0.0</revision></properties></project>'],
  ['</project>', '<modules><module>child</module></modules></project>'],
  ['</project>', '<groupId>other.group</groupId></project>'],
]) {
  test(`source POM cannot override parent/coordinates ${replacement[1]}`, () => {
    const text = pluginFiles().get('server/forge-plugin-demo/pom.xml').toString('utf8')
    assert.throws(() => validateSourcePom(text.replace(...replacement), 'forge-plugin-demo'))
  })
}

test('core module and reserved ownership file cannot be installed', async t => {
  const host = await project(t)
  const before = await snapshot(host.root)
  await assert.rejects(addPlugin(host.root, await plugin(t, descriptor({ server: { module: 'forge-starter-core' } }))),
    /核心模块/)
  const source = await plugin(t)
  await write(source, 'ui/.forge-plugin-owned.json', '{}')
  await assert.rejects(addPlugin(host.root, source), /保留文件/)
  assert.deepEqual(await snapshot(host.root), before)
})

test('dev adapts parent path without changing source and rejects changed UI link', async t => {
  const host = await project(t)
  const source = await plugin(t)
  const pomFile = path.join(source, 'server/forge-plugin-demo/pom.xml')
  const pom = await fs.readFile(pomFile, 'utf8')
  await fs.writeFile(pomFile, pom.replace('../../pom.xml', '../pom.xml'))
  await addPlugin(host.root, source, { dev: true })
  assert.equal(await fs.readFile(pomFile, 'utf8'), pom.replace('../../pom.xml', '../pom.xml'))
  const target = path.join(host.root, host.ui, 'src/views/plugins/demo')
  await fs.unlink(target)
  const other = await temporary(t)
  await write(other, 'important.txt', 'keep')
  await fs.symlink(other, target)
  await assert.rejects(removePlugin(host.root, 'demo'), /登记不一致/)
  assert.equal(await fs.readFile(path.join(other, 'important.txt'), 'utf8'), 'keep')
})

test('copy/dev transitions preserve links or copies in backups and never mutate package', async t => {
  const host = await project(t)
  const source = await plugin(t)
  const before = await snapshot(source)
  await addPlugin(host.root, source, { dev: true })
  const copied = await addPlugin(host.root, source, { force: true })
  assert.ok((await fs.lstat(path.join(copied.backup, 'old-0/src'))).isSymbolicLink())
  const linked = await addPlugin(host.root, source, { force: true, dev: true })
  assert.ok((await fs.lstat(path.join(linked.backup, 'old-0'))).isDirectory())
  assert.deepEqual(await snapshot(source), before)
})
