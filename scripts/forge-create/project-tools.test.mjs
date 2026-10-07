import assert from 'node:assert/strict'
import fs from 'node:fs/promises'
import os from 'node:os'
import path from 'node:path'
import test from 'node:test'
import { spawnSync } from 'node:child_process'
import { fileURLToPath, pathToFileURL } from 'node:url'
import {
  readForgeVersion, stripTemplateOnlyGitignore, copyGeneratedGitignore, copyGeneratedPluginTools,
  writeGeneratedProjectConfig,
} from './project-tools.mjs'

const begin = '# forge-template-only:begin'
const end = '# forge-template-only:end'

async function fixture(t, files) {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'forge-project-tools-'))
  t.after(() => fs.rm(root, { recursive: true, force: true }))
  for (const [name, content] of Object.entries(files)) {
    const target = path.join(root, name)
    await fs.mkdir(path.dirname(target), { recursive: true })
    await fs.writeFile(target, content)
  }
  return root
}

for (const version of ['1.2.0', '0.0.0', '2.1.3-rc.1+build.004']) {
  test(`reads literal revision ${version} and ignores commented revision`, async (t) => {
    const root = await fixture(t, {
      'forge-server/pom.xml': `<project><properties><!-- <revision>9.9.9</revision> -->
        <revision> ${version} </revision></properties><version>0.1.0</version></project>`,
    })
    assert.equal(await readForgeVersion(root), version)
  })
}

for (const value of ['', '${revision}', '1.2', '01.2.3', '1.2.3-01', '1.2.3-rc..1', '1.2.3+build..1']) {
  test(`rejects invalid revision ${JSON.stringify(value)} without guessing a version`, async (t) => {
    const root = await fixture(t, {
      'forge-server/pom.xml': `<project><properties><revision>${value}</revision></properties></project>`,
    })
    await assert.rejects(readForgeVersion(root), /唯一、合法的明文 revision/)
  })
}

test('missing or repeated revision fails, including revision outside properties', async (t) => {
  for (const body of ['<revision>1.2.0</revision>', '<properties></properties>',
    '<properties><revision>1.2.0</revision><revision>1.2.1</revision></properties>']) {
    const root = await fixture(t, { 'forge-server/pom.xml': `<project>${body}</project>` })
    await assert.rejects(readForgeVersion(root), /唯一、合法的明文 revision/)
  }
})

test('creator rejects unresolved revision before creating the requested output directory', async (t) => {
  const repoRoot = fileURLToPath(new URL('../..', import.meta.url))
  const root = await fixture(t, { 'forge-server/pom.xml': '<properties><revision>${revision}</revision></properties>' })
  for (const file of ['scripts/forge-create/create-project.mjs', 'scripts/forge-create/project-tools.mjs',
    'scripts/forge-create/pom-modules.mjs',
    'scripts/forge-create/source-glue.mjs', 'scripts/forge-create/module-catalog.json',
    'scripts/forge-shared/rename.mjs', 'scripts/forge-shared/files.mjs']) {
    const target = path.join(root, file)
    await fs.mkdir(path.dirname(target), { recursive: true })
    await fs.copyFile(path.join(repoRoot, file), target)
  }
  const outputRoot = path.join(root, 'generated-output')
  const result = spawnSync(process.execPath, [path.join(root, 'scripts/forge-create/create-project.mjs'),
    outputRoot, '--base-package', 'com.acme.example', '--preset', 'minimal-admin'], {encoding: 'utf8'})
  assert.equal(result.status, 1)
  assert.match(result.stderr, /唯一、合法的明文 revision/)
  await assert.rejects(fs.stat(outputRoot), { code: 'ENOENT' })
})

test('gitignore without a template block is byte-for-byte unchanged', () => {
  for (const content of ['', 'target/\n# rule\n', 'target/\r\n!keep\r\n', 'target/']) {
    assert.equal(stripTemplateOnlyGitignore(content), content)
  }
})

for (const newline of ['\n', '\r\n']) {
  test(`strips exactly one complete block and retains ${JSON.stringify(newline)} bytes`, () => {
    const before = `target/${newline}`
    const after = '!plugins/keep'
    const block = [begin, '/forge-server/plugins/', '/forge-admin-ui/src/views/plugins/', end, ''].join(newline)
    assert.equal(stripTemplateOnlyGitignore(before + block + after), before + after)
    assert.equal(stripTemplateOnlyGitignore(begin + newline + end), '')
  })
}

for (const block of [begin, end, `${begin}\n${begin}\n${end}`, `${begin}\n${end}\n${begin}\n${end}`,
  `${begin}\n${end}\n${end}`]) {
  test(`rejects malformed template block ${JSON.stringify(block)}`, () => {
    assert.throws(() => stripTemplateOnlyGitignore(block), /模板专用区块/)
  })
}

test('gitignore copy uses exact block stripping without changing source', async (t) => {
  const source = `target/\n${begin}\n/forge-server/plugins/\n${end}\n!keep`
  const repoRoot = await fixture(t, { '.gitignore': source })
  const outputRoot = await fixture(t, {})
  await copyGeneratedGitignore(repoRoot, outputRoot)
  assert.equal(await fs.readFile(path.join(outputRoot, '.gitignore'), 'utf8'), 'target/\n!keep')
  assert.equal(await fs.readFile(path.join(repoRoot, '.gitignore'), 'utf8'), source)
})

test('project config preserves custom context and sorted selections with only two new fields', async (t) => {
  const outputRoot = await fixture(t, {})
  const options = {
    projectName: 'acme', javaName: 'Acme', displayName: 'Acme 应用', basePackage: 'com.acme.runtime',
    groupId: 'com.acme.maven', artifactPrefix: 'acme', moduleArtifactPrefix: 'domain', stripModulePrefix: '',
    databaseName: 'acme_db', preset: 'minimal-admin', includeModuleIds: ['h5-ui'], excludeLogData: true,
  }
  const selection = {
    selectedModuleIds: new Set(['plugin-print', 'admin-server']),
    frontendIds: new Set(['h5-ui', 'admin-ui']), deployIds: new Set(['docker']),
  }
  await writeGeneratedProjectConfig({ outputRoot, options, selection, forgeVersion: '1.2.0-rc.1' })
  const {includeModuleIds, ...existing} = options
  const expected = {
    ...existing, includedModules: includeModuleIds, modules: ['admin-server', 'plugin-print'],
    frontends: ['admin-ui', 'h5-ui'], deploy: ['docker'], forgeVersion: '1.2.0-rc.1', plugins: [],
  }
  assert.deepEqual(JSON.parse(await fs.readFile(path.join(outputRoot, 'forge.config.json'))), expected)
  assert.deepEqual([...selection.selectedModuleIds], ['plugin-print', 'admin-server'])
})

test('copies raw runtime tools recursively, omits tests/local files and writes only the plugin script', async (t) => {
  const files = {
    'scripts/forge-plugin/index.mjs': 'export const entry = "ForgeAdminApplication"',
    'scripts/forge-plugin/lib/action.mjs': 'export const module = "forge-plugin-print"',
    'scripts/forge-shared/rename.mjs': 'export const source = "com.mdframe.forge"',
    'scripts/forge-create/module-catalog.json': '{"modules":{"print":{"artifactId":"forge-plugin-print"}}}',
    'scripts/forge-plugin/index.test.mjs': 'not a runtime file',
    'scripts/forge-plugin/fixtures/example.json': 'fixture',
    'scripts/forge-shared/.env.local': 'local configuration',
    'scripts/forge-shared/.DS_Store': 'local metadata',
  }
  const repoRoot = await fixture(t, files)
  const outputRoot = await fixture(t, {})
  await copyGeneratedPluginTools({ repoRoot, outputRoot, projectName: 'acme-runtime' })
  for (const file of Object.keys(files).slice(0, 4)) {
    assert.equal(await fs.readFile(path.join(outputRoot, file), 'utf8'), files[file])
  }
  for (const file of Object.keys(files).slice(4)) {
    await assert.rejects(fs.stat(path.join(outputRoot, file)), { code: 'ENOENT' })
  }
  assert.deepEqual(JSON.parse(await fs.readFile(path.join(outputRoot, 'package.json'), 'utf8')), {
    name: 'acme-runtime', private: true, type: 'module',
    scripts: { 'forge:plugin': 'node scripts/forge-plugin/index.mjs' },
  })
  await assert.rejects(fs.stat(path.join(outputRoot, 'scripts/forge-create/create-project.mjs')), { code: 'ENOENT' })
})

test('rejects symlinks in runtime tools rather than copying files outside the template', async (t) => {
  const repoRoot = await fixture(t, { 'scripts/forge-plugin/index.mjs': '', 'outside.mjs': 'outside' })
  await fs.symlink(path.join(repoRoot, 'outside.mjs'), path.join(repoRoot, 'scripts/forge-plugin/link.mjs'))
  const outputRoot = await fixture(t, {})
  await assert.rejects(copyGeneratedPluginTools({ repoRoot, outputRoot, projectName: 'acme' }), /软链接/)
  await assert.rejects(fs.stat(path.join(outputRoot, 'scripts/forge-plugin/link.mjs')), { code: 'ENOENT' })
})

test('actual runtime shared rules stay canonical and are usable for a second plugin rename', async (t) => {
  const repoRoot = fileURLToPath(new URL('../..', import.meta.url))
  const outputRoot = await fixture(t, {})
  await copyGeneratedPluginTools({ repoRoot, outputRoot, projectName: 'acme-runtime' })
  const moduleUrl = pathToFileURL(path.join(outputRoot, 'scripts/forge-shared/rename.mjs'))
  const { buildArtifactMap, buildApplicationClassMap, buildTextReplacements, applyTextReplacements } =
    await import(moduleUrl.href)
  const catalog = JSON.parse(await fs.readFile(path.join(outputRoot, 'scripts/forge-create/module-catalog.json')))
  const options = {
    projectName: 'acme-runtime', javaName: 'Acme', artifactPrefix: 'acme-runtime', moduleArtifactPrefix: 'domain',
    basePackage: 'com.acme.runtime', groupId: 'com.acme.maven', displayName: 'Acme', databaseName: 'acme_db',
  }
  const artifacts = buildArtifactMap(catalog, options)
  const rules = buildTextReplacements(artifacts, buildApplicationClassMap(options), options, {frontendIds: new Set()})
  assert.equal(artifacts['forge-plugin-print'], 'domain-plugin-print')
  assert.equal(applyTextReplacements('com.mdframe.forge ForgeAdminApplication forge-plugin-print', rules),
    'com.acme.runtime AcmeAdminApplication domain-plugin-print')
  for (const file of ['scripts/forge-shared/rename.mjs', 'scripts/forge-shared/files.mjs',
    'scripts/forge-plugin/index.mjs', 'scripts/forge-create/module-catalog.json']) {
    assert.deepEqual(await fs.readFile(path.join(outputRoot, file)), await fs.readFile(path.join(repoRoot, file)))
  }
})
