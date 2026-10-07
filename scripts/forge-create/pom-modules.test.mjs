import assert from 'node:assert/strict'
import fs from 'node:fs/promises'
import os from 'node:os'
import path from 'node:path'
import test from 'node:test'
import { renderPomModules, replacePomModules } from './pom-modules.mjs'
import { renameSourceTree } from '../forge-shared/rename.mjs'

const begin = '<!-- forge-plugins:modules:begin -->'
const end = '<!-- forge-plugins:modules:end -->'
const emptyMarkers = `        ${begin}\n        ${end}`
const modules = ['forge-admin-server', 'forge-framework']

async function fixture(t, files = {}) {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'forge-pom-modules-'))
  t.after(() => fs.rm(root, { recursive: true, force: true }))
  for (const [name, content] of Object.entries(files)) {
    const target = path.join(root, name)
    await fs.mkdir(path.dirname(target), { recursive: true })
    await fs.writeFile(target, content)
  }
  return root
}

for (const newline of ['\n', '\r\n', '']) {
  test(`unmarked modules retain the original rendering for ${JSON.stringify(newline)}`, () => {
    const before = `<project>${newline}  <modules><module>old</module></modules>`
      + `${newline}<build><plugins/></build></project>`
    const expected = before.replace(/[\t ]*<modules>[\s\S]*?<\/modules>/,
      '    <modules>\n        <module>forge-admin-server</module>\n'
      + '        <module>forge-framework</module>\n    </modules>')
    assert.equal(renderPomModules(before, modules), expected)
  })
}

for (const newline of ['\n', '\r\n']) {
  test(`preserves the empty plugin block while pruning modules with ${JSON.stringify(newline)}`, () => {
    const markers = emptyMarkers.replaceAll('\n', newline)
    const before = `<project>\n    <modules>\n        <module>old</module>\n${markers}\n    </modules>\n</project>`
    const expected = '<project>\n    <modules>\n        <module>forge-admin-server</module>\n'
      + `        <module>forge-framework</module>\n${markers}\n    </modules>\n</project>`
    const after = renderPomModules(before, modules)
    assert.equal(after, expected)
    assert.equal(renderPomModules(after, modules), after)
  })
}

const invalidBodies = [
  begin, end, `${end}\n${begin}`, `${begin}\n${begin}\n${end}`, `${begin}\n${end}\n${end}`,
  `${emptyMarkers}\n${emptyMarkers}`, `${begin}\n<module>plugins/hello</module>\n${end}`,
  `${begin}\n<!-- customer note -->\n${end}`,
]
for (const body of invalidBodies) {
  test(`rejects malformed or nonempty plugin module blocks ${JSON.stringify(body)}`, () => {
    assert.throws(() => renderPomModules(`<project><modules>\n${body}\n</modules></project>`, modules),
      /标记必须唯一、完整、位于 modules 内且为空/)
  })
}

test('rejects module markers outside the rewritten section or without a modules section', () => {
  for (const body of [`<modules><module>old</module></modules>\n${emptyMarkers}`,
    emptyMarkers, `<modules/>\n${emptyMarkers}`]) {
    assert.throws(() => renderPomModules(`<project>${body}</project>`, modules), /POM 插件 modules 标记/)
  }
})

test('without modules or markers the original document stays unchanged', () => {
  const source = '<project>\r\n<dependencies/>\r\n</project>'
  assert.equal(renderPomModules(source, modules), source)
})

test('file rewrite is idempotent and an invalid block never writes the file', async (t) => {
  const source = `<project><modules>\n${emptyMarkers}\n</modules></project>`
  const invalid = `<project><modules>\n${begin}\n<module>plugins/hello</module>\n${end}\n</modules></project>`
  const root = await fixture(t, { 'pom.xml': source, 'invalid.xml': invalid })
  const pomFile = path.join(root, 'pom.xml')
  await replacePomModules(pomFile, modules)
  await replacePomModules(pomFile, modules)
  assert.equal(await fs.readFile(pomFile, 'utf8'), renderPomModules(source, modules))
  await assert.rejects(replacePomModules(path.join(root, 'invalid.xml'), modules), /POM 插件 modules 标记/)
  assert.equal(await fs.readFile(path.join(root, 'invalid.xml'), 'utf8'), invalid)
})

test('missing POM is still a no-op without creating a file', async (t) => {
  const root = await fixture(t)
  await replacePomModules(path.join(root, 'pom.xml'), modules)
  assert.deepEqual(await fs.readdir(root), [])
})

const sourcePoms = [
  ['../../forge-server/pom.xml', 'modules'],
  ['../../forge-server/forge-admin-server/pom.xml', 'dependencies'],
]
for (const [source, section] of sourcePoms) {
  test(`source ${source} has a unique empty marker block inside its direct ${section}`, async () => {
    const content = await fs.readFile(new URL(source, import.meta.url), 'utf8')
    const start = `<!-- forge-plugins:${section}:begin -->`
    const finish = `<!-- forge-plugins:${section}:end -->`
    assert.equal(content.split(start).length - 1, 1)
    assert.equal(content.split(finish).length - 1, 1)
    const sectionBody = content.match(new RegExp(`<${section}>([\\s\\S]*?)<\\/${section}>`))?.[1]
    assert.ok(sectionBody?.includes(start) && sectionBody.includes(finish))
    assert.equal(sectionBody.split(start)[1].split(finish)[0].trim(), '')
  })
}

test('shared rename keeps canonical markers with independent Maven and package prefixes', async (t) => {
  const files = {}
  for (const [source] of sourcePoms) {
    const target = source.includes('forge-admin-server') ? 'forge-admin-server/pom.xml' : 'pom.xml'
    files[target] = await fs.readFile(new URL(source, import.meta.url), 'utf8')
  }
  const root = await fixture(t, files)
  const catalog = JSON.parse(await fs.readFile(new URL('./module-catalog.json', import.meta.url), 'utf8'))
  const options = {
    projectName: 'acme', artifactPrefix: 'platform', moduleArtifactPrefix: 'domain', javaName: 'Acme',
    basePackage: 'com.acme.runtime', groupId: 'org.acme.maven', displayName: 'Acme', databaseName: 'acme_db',
  }
  await renameSourceTree(root, { options, catalog, selection: { frontendIds: new Set() } })
  for (const [file, section] of [['pom.xml', 'modules'], ['domain-admin-server/pom.xml', 'dependencies']]) {
    const content = await fs.readFile(path.join(root, file), 'utf8')
    assert.equal(content.split(`<!-- forge-plugins:${section}:begin -->`).length - 1, 1)
    assert.equal(content.split(`<!-- forge-plugins:${section}:end -->`).length - 1, 1)
    assert.match(content, /<groupId>org\.acme\.maven<\/groupId>/)
    assert.equal(content.includes('com.mdframe.forge'), false)
    assert.equal(content.includes('platform-plugins:'), false)
    assert.equal(content.includes('domain-plugins:'), false)
  }
})
