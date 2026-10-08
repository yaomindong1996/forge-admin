import assert from 'node:assert/strict'
import fs from 'node:fs/promises'
import path from 'node:path'
import test from 'node:test'
import { spawnSync } from 'node:child_process'
import { checkEdition } from './edition.mjs'
import { inspectContent, maxFileBytes } from './content.mjs'
import { runEditionCli } from './check-edition.mjs'
import { readInventory, assertIndexUnchanged } from './git.mjs'
import { stripTemplateOnlyGitignore, copyGeneratedPluginTools } from '../forge-create/project-tools.mjs'
import { project, temporary, repository, write, git, commit, snapshot } from '../forge-plugin/fixtures/helpers.mjs'

const dotted = ['com', 'mdframe', 'forge', 'ee'].join('.')
const slashed = dotted.replaceAll('.', '/')
const forbidden = /企业版包前缀/
const installed = /本地安装的插件目录/
const markerRule = /POM 插件标记/

async function host(t) {
  const result = await project(t)
  await write(result.root, '.gitignore', await fs.readFile(path.join(repository, '.gitignore')))
  git(result.root, ['init', '-q'])
  commit(result.root)
  return result
}

function hasRule(result, pattern, source) {
  return result.violations.some(item => pattern.test(item.rule) && (!source || item.source === source))
}

test('clean community tree, empty markers and sample metadata pass without changing files/index', async (t) => {
  const { root } = await host(t)
  await write(root, 'plugins-samples/hello/forge-plugin.json', '{"edition":"community"}')
  await write(root, 'src/same.txt', 'community')
  await write(root, 'src/duplicate.txt', 'community')
  git(root, ['add', '.'])
  const before = await snapshot(root)
  const index = git(root, ['ls-files', '--stage', '-z'])
  const result = await checkEdition(root)
  assert.deepEqual(result.violations, [])
  assert.equal(result.indexed, 8)
  assert.deepEqual(await snapshot(root), before)
  assert.equal(git(root, ['ls-files', '--stage', '-z']), index)
})

for (const [relative, content] of [
  ['src/Private.java', `package ${dotted}; class Private {}`],
  ['src/resource.txt', `${slashed}/Private.class`],
  [`src/${slashed}/Empty.java`, ''],
  ['forge-docs/new-policy.md', `Documentation ${dotted}`],
  ['tests/negative.mjs', `export const text = '${dotted}'`],
  ['src/图标\n资源.bin', Buffer.concat([Buffer.from([0, 255]), Buffer.from(dotted)])],
]) {
  test(`untracked content and paths have no source/doc/test exemption: ${JSON.stringify(relative)}`, async (t) => {
    const { root } = await host(t)
    await write(root, relative, content)
    assert.ok(hasRule(await checkEdition(root), forbidden, '工作区'))
    git(root, ['add', '--', relative])
    assert.ok(hasRule(await checkEdition(root), forbidden, '索引'))
  })
}

test('dirty tracked file and staged blob both checked even when working copy was repaired', async (t) => {
  const { root } = await host(t)
  await write(root, 'src/main.txt', 'community')
  commit(root)
  await write(root, 'src/main.txt', dotted)
  assert.ok(hasRule(await checkEdition(root), forbidden, '工作区'))
  git(root, ['add', 'src/main.txt'])
  await write(root, 'src/main.txt', 'community')
  const result = await checkEdition(root)
  assert.ok(hasRule(result, forbidden, '索引'))
  assert.ok(!hasRule(result, forbidden, '工作区'))
  git(root, ['add', 'src/main.txt'])
  assert.deepEqual((await checkEdition(root)).violations, [])
})

test('staged deletion of a forbidden file passes, unstaged deletion still audits index', async (t) => {
  const { root } = await host(t)
  await write(root, 'src/obsolete.txt', dotted)
  commit(root)
  await fs.unlink(path.join(root, 'src/obsolete.txt'))
  assert.ok(hasRule(await checkEdition(root), forbidden, '索引'))
  git(root, ['add', '-u'])
  assert.deepEqual((await checkEdition(root)).violations, [])
})

for (const content of ['{"edition":"ee"}', '{"edition":"future"}', '{}', 'null', '[]', '"community"',
  '{"edition":"ee","edition":"community"}', '{broken', '{"edition":"community","edition":"ee"}']) {
  test(`descriptor cannot conceal edition or parse failure: ${content}`, async (t) => {
    const { root } = await host(t)
    await write(root, 'sample/forge-plugin.json', content)
    assert.ok(hasRule(await checkEdition(root), /插件描述/, '工作区'))
    git(root, ['add', 'sample/forge-plugin.json'])
    await write(root, 'sample/forge-plugin.json', '{"edition":"community"}')
    assert.ok(hasRule(await checkEdition(root), /插件描述/, '索引'))
  })
}

for (const relative of ['forge-server/plugins/demo/index.java', 'forge-admin-ui/src/views/plugins/demo/index.vue']) {
  test(`ignored local installs allowed but forced index entries rejected: ${relative}`, async (t) => {
    const { root } = await host(t)
    await write(root, relative, dotted)
    assert.deepEqual((await checkEdition(root)).violations, [])
    await write(root, relative, 'community')
    git(root, ['add', '-f', '--', relative])
    assert.ok(hasRule(await checkEdition(root), installed, '索引'))
  })
}

const begin = '<!-- forge-plugins:modules:begin -->'
const end = '<!-- forge-plugins:modules:end -->'
for (const replacement of [
  `${begin}\n<module>plugins/demo</module>\n${end}`,
  `${begin}\n<!-- installer comment -->\n${end}`,
  begin,
  `${end}\n${begin}`,
  `${begin}\n${end}\n${begin}\n${end}`,
  '<!-- forge-plugins:unknown:begin -->',
  '',
]) {
  test(`invalid or missing root marker rejected: ${JSON.stringify(replacement)}`, async (t) => {
    const { root, rootPom } = await host(t)
    const content = rootPom.replace(`${begin}\n  ${end}`, replacement)
    await write(root, 'forge-server/pom.xml', content)
    assert.ok(hasRule(await checkEdition(root), markerRule, '工作区'))
    git(root, ['add', 'forge-server/pom.xml'])
    await write(root, 'forge-server/pom.xml', rootPom)
    assert.ok(hasRule(await checkEdition(root), markerRule, '索引'))
  })
}

test('Admin dependencies, nested markers, raw duplicate tokens and malformed XML fail closed', async (t) => {
  const { root, adminPom } = await host(t)
  const relative = 'forge-server/forge-admin-server/pom.xml'
  const endTag = '<!-- forge-plugins:dependencies:end -->'
  for (const content of [adminPom.replace(endTag, `<dependency><artifactId>demo</artifactId></dependency>${endTag}`),
    adminPom.replace('<dependencies>', '<dependencies><profile>')
      .replace('</dependencies>', '</profile></dependencies>'),
    adminPom.replace('</project>', '<custom>forge-plugins:dependencies:begin</custom></project>'),
    adminPom.replace('</project>', '')]) {
    await write(root, relative, content)
    assert.ok(hasRule(await checkEdition(root), markerRule))
  }
})

test('other POMs with plugin markers are checked too; required host cannot be untracked/missing', async (t) => {
  const { root, rootPom } = await host(t)
  await write(root, 'extra/pom.xml', rootPom.replace(end, `<module>installed</module>${end}`))
  assert.ok(hasRule(await checkEdition(root), markerRule, '工作区'))
  await fs.unlink(path.join(root, 'extra/pom.xml'))
  await fs.unlink(path.join(root, 'forge-server/pom.xml'))
  assert.ok(hasRule(await checkEdition(root), /文件不可检查/))
  git(root, ['add', '-u'])
  assert.ok(hasRule(await checkEdition(root), /宿主 POM/, '索引'))
})

test('ignored recovery files and ignored external links are not opened', async (t) => {
  const { root } = await host(t)
  await write(root, '.gitignore', (await fs.readFile(path.join(root, '.gitignore'))) + '\n.forge-plugin/\n')
  await write(root, '.forge-plugin/backups/forge-plugin.json', '{"edition":"ee"}')
  const external = await temporary(t)
  await write(external, 'private.txt', dotted)
  await fs.mkdir(path.join(root, 'forge-server/plugins'), { recursive: true })
  await fs.symlink(external, path.join(root, 'forge-server/plugins/private'))
  assert.deepEqual((await checkEdition(root)).violations, [])
  assert.equal(await fs.readFile(path.join(external, 'private.txt'), 'utf8'), dotted)
})

test('committable symlink rejected without exposing its target content', async (t) => {
  const { root } = await host(t)
  const external = await temporary(t)
  await write(external, 'private.txt', 'DO_NOT_PRINT_PRIVATE_DATA')
  await fs.symlink(path.join(external, 'private.txt'), path.join(root, 'linked.txt'))
  const result = await checkEdition(root)
  assert.ok(hasRule(result, /文件不可检查/))
  assert.ok(!JSON.stringify(result).includes('DO_NOT_PRINT_PRIVATE_DATA'))
  git(root, ['add', 'linked.txt'])
  await assert.rejects(checkEdition(root), /软链接/)
})

test('tracked parent directory swapped for external symlink rejected', async (t) => {
  const { root } = await host(t)
  await write(root, 'safe/code.txt', 'community')
  commit(root)
  const external = await temporary(t)
  await write(external, 'code.txt', dotted)
  await fs.rename(path.join(root, 'safe'), path.join(root, 'original'))
  await fs.symlink(external, path.join(root, 'safe'))
  assert.ok(hasRule(await checkEdition(root), /文件不可检查/))
})

test('submodule and unmerged index entries are never treated as audited blobs', async (t) => {
  const { root } = await host(t)
  const revision = git(root, ['rev-parse', 'HEAD']).trim()
  git(root, ['update-index', '--add', '--cacheinfo', `160000,${revision},module`])
  await assert.rejects(checkEdition(root), /子模块/)
  git(root, ['update-index', '--force-remove', 'module'])
  const oid = git(root, ['hash-object', 'forge-server/pom.xml']).trim()
  const input = `100644 ${oid} 1\tconflict.txt\n100644 ${oid} 2\tconflict.txt\n`
  const result = spawnSync('git', ['-C', root, 'update-index', '--index-info'], { input, encoding: 'utf8' })
  assert.equal(result.status, 0)
  await assert.rejects(checkEdition(root), /冲突/)
})

test('oversized regular working files and index blobs fail without silently skipping', async (t) => {
  const { root } = await host(t)
  await write(root, 'oversized.bin', '')
  await fs.truncate(path.join(root, 'oversized.bin'), maxFileBytes + 1)
  assert.ok(hasRule(await checkEdition(root), /文件不可检查/))
  git(root, ['add', 'oversized.bin'])
  await assert.rejects(checkEdition(root), /64 MiB/)
})

test('CLI supports help, rejects extra arguments/nonrepositories and reports no file contents', async (t) => {
  const { root } = await host(t)
  const messages = []
  const output = { log: value => messages.push(value), error: value => messages.push(value) }
  assert.equal(await runEditionCli([], output, root), 0)
  assert.equal(await runEditionCli(['--help'], output, root), 0)
  assert.equal(await runEditionCli(['--skip'], output, root), 1)
  assert.equal(await runEditionCli([], output, await temporary(t)), 1)
  await write(root, 'forge-plugin.json', '{"edition": DO_NOT_PRINT_PRIVATE_DATA}')
  assert.equal(await runEditionCli([], output, root), 1)
  assert.ok(!messages.join('\n').includes('DO_NOT_PRINT_PRIVATE_DATA'))
})

test('Git environment overrides and replace refs cannot hide the real staged blob', async (t) => {
  const { root } = await host(t)
  await write(root, 'source.txt', dotted)
  git(root, ['add', 'source.txt'])
  const bad = git(root, ['hash-object', 'source.txt']).trim()
  await write(root, 'source.txt', 'community')
  const clean = git(root, ['hash-object', '-w', 'source.txt']).trim()
  git(root, ['replace', bad, clean])
  const previous = process.env.GIT_INDEX_FILE
  process.env.GIT_INDEX_FILE = path.join(root, 'fake-index')
  try {
    assert.ok(hasRule(await checkEdition(root), forbidden, '索引'))
  }
  finally {
    if (previous === undefined) {
      delete process.env.GIT_INDEX_FILE
    }
    else {
      process.env.GIT_INDEX_FILE = previous
    }
  }
})

test('template CLI locates its own repository from arbitrary cwd and fails if Git is unavailable', async (t) => {
  const { root } = await host(t)
  const guardFiles = ['scripts/guards', 'scripts/forge-plugin', 'scripts/forge-shared']
  for (const directory of guardFiles) {
    await fs.cp(path.join(repository, directory), path.join(root, directory), {
      recursive: true, filter: file => !file.endsWith('.test.mjs') && !file.includes('/fixtures'),
    })
  }
  await write(root, 'package.json', '{"type":"module"}')
  const entry = path.join(root, 'scripts/guards/check-edition.mjs')
  const result = spawnSync(process.execPath, [entry], { cwd: await temporary(t), encoding: 'utf8' })
  assert.equal(result.status, 0, result.stdout + result.stderr)
  const missing = spawnSync(process.execPath, [entry], { cwd: root, env: { PATH: '/not-installed' }, encoding: 'utf8' })
  assert.equal(missing.status, 1)
  assert.match(missing.stderr, /Git 不可用/)
})

test('actual template ignore block stripped; customer plugin paths become committable, guard not copied', async (t) => {
  const { root } = await host(t)
  const original = await fs.readFile(path.join(repository, '.gitignore'), 'utf8')
  const stripped = stripTemplateOnlyGitignore(original)
  assert.ok(!stripped.includes('forge-template-only'))
  assert.ok(!stripped.includes('/forge-server/plugins/'))
  await write(root, '.gitignore', stripped)
  await write(root, 'forge-server/plugins/customer/index.java', 'community')
  assert.match(git(root, ['ls-files', '--others', '--exclude-standard']), /plugins\/customer\/index.java/)
  const outputRoot = await temporary(t)
  await copyGeneratedPluginTools({ repoRoot: repository, outputRoot, projectName: 'customer' })
  await assert.rejects(fs.stat(path.join(outputRoot, 'scripts/guards')), { code: 'ENOENT' })
  const generated = JSON.parse(await fs.readFile(path.join(outputRoot, 'package.json')))
  assert.deepEqual(Object.keys(generated.scripts), ['forge:plugin', 'forge:plugin-build', 'forge:plugin-release'])
  assert.equal(inspectContent('sample/forge-plugin.json', Buffer.from('{"edition":"community"}')).length, 0)
})

test('multiple Git blob batches keep binary framing and inspect the final file', async (t) => {
  const { root } = await host(t)
  for (let index = 0; index < 270; index++) {
    const text = index === 269 ? dotted : `unique ${index}\n\0binary`
    await write(root, `batch/${String(index).padStart(3, '0')}.bin`, text)
  }
  git(root, ['add', 'batch'])
  const result = await checkEdition(root)
  assert.ok(result.violations.some(item => item.source === '索引' && item.relative === 'batch/269.bin'))
})

test('concurrent index change, unreadable Git object and nested root cannot pass', async (t) => {
  const { root } = await host(t)
  const inventory = await readInventory(root)
  await write(root, 'changed.txt', 'community')
  git(root, ['add', 'changed.txt'])
  assert.throws(() => assertIndexUnchanged(inventory), /索引发生变化/)
  await assert.rejects(readInventory(path.join(root, 'forge-server')), /仓库顶层/)
  const oid = git(root, ['hash-object', 'changed.txt']).trim()
  await fs.unlink(path.join(root, '.git/objects', oid.slice(0, 2), oid.slice(2)))
  await assert.rejects(checkEdition(root), /Git 索引 blob|对象读取失败/)
})

test('special working files fail without waiting for a FIFO producer', async (t) => {
  const { root } = await host(t)
  const target = path.join(root, 'pipe')
  await write(root, 'pipe', 'community')
  commit(root)
  await fs.unlink(target)
  const result = spawnSync('mkfifo', [target], { encoding: 'utf8' })
  assert.equal(result.status, 0)
  assert.ok(hasRule(await checkEdition(root), /文件不可检查/))
})
