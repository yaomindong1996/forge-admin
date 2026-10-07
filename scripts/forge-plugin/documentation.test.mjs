import assert from 'node:assert/strict'
import fs from 'node:fs/promises'
import path from 'node:path'
import test from 'node:test'
import { readBundle, fingerprint } from './bundle.mjs'
import { readDescriptor, validateDescriptor } from './descriptor.mjs'
import { parseStrictJson } from './json.mjs'
import { runPluginCli } from './index.mjs'
import { commit, git, project, repository, temporary, write } from './fixtures/helpers.mjs'

const reference = '.agents/skills/forge-project-init/references/plugins.md'
const authorGuide = 'plugins-samples/README.md'
const sample = path.join(repository, 'plugins-samples/forge-plugin-hello')

async function read(relative) {
  return fs.readFile(path.join(repository, relative), 'utf8')
}

// 仅解析文档中的插件 CLI，不执行 Markdown 内其它 shell、构建或数据库建议。
async function documentedCommands(sources) {
  const content = await read(reference)
  const blocks = [...content.matchAll(/```bash\n([\s\S]*?)\n```/g)]
  const lines = blocks.flatMap(match => match[1].split('\n')).filter(line => line.startsWith('pnpm forge:plugin '))
  assert.deepEqual(lines, [
    'pnpm forge:plugin add "/absolute/path/to/plugin-package"',
    'pnpm forge:plugin list',
    'pnpm forge:plugin add "/absolute/path/to/new-plugin-package" --force',
    'pnpm forge:plugin list',
    'pnpm forge:plugin add "/absolute/path/to/plugin-package" --dev',
    'pnpm forge:plugin remove hello',
    'pnpm forge:plugin list',
  ])
  return lines.map(line => line.match(/"[^"]*"|\S+/g).slice(2).map(token => {
    const value = token.replace(/^"|"$/g, '')
    return sources[value] || value
  }))
}

async function cli(root, args) {
  const messages = []
  const output = { log: message => messages.push(message), error: message => messages.push(message) }
  const status = await runPluginCli(args, output, { root, cwd: root })
  return { status, text: messages.join('\n') }
}

async function upgradedSample(t) {
  const source = await temporary(t)
  await fs.cp(sample, source, { recursive: true })
  for (const relative of ['forge-plugin.json',
    'server/forge-plugin-hello/src/main/resources/META-INF/forge-plugin.json']) {
    const metadata = JSON.parse(await fs.readFile(path.join(source, relative)))
    metadata.version = '1.0.1'
    await write(source, relative, JSON.stringify(metadata, null, 2))
  }
  return source
}

async function setup(t, generated) {
  const host = await project(t, generated)
  git(host.root, ['init', '-q'])
  commit(host.root)
  const next = await upgradedSample(t)
  const commands = await documentedCommands({
    '/absolute/path/to/plugin-package': sample,
    '/absolute/path/to/new-plugin-package': next,
  })
  const sources = [fingerprint((await readBundle(sample)).files), fingerprint((await readBundle(next)).files)]
  return { host, next, commands, sources }
}

test('author JSON example passes strict metadata validation and equals the delivered descriptor', async () => {
  const content = await read(authorGuide)
  const examples = [...content.matchAll(/```json\n([\s\S]*?)\n```/g)]
  assert.equal(examples.length, 1)
  const metadata = validateDescriptor(parseStrictJson(examples[0][1]), '1.2.0')
  assert.deepEqual(metadata, readDescriptor((await readBundle(sample)).files, '1.2.0'))
})

test('plugin documentation local links resolve and skill reference is discoverable', async () => {
  for (const relative of [authorGuide, reference, '.agents/skills/forge-project-init/SKILL.md']) {
    const content = await read(relative)
    for (const match of content.matchAll(/\[[^\]]+\]\(([^)]+)\)/g)) {
      assert.ok(!match[1].includes('://'), `unexpected remote dependency: ${match[1]}`)
      await fs.access(path.resolve(repository, path.dirname(relative), match[1].split('#')[0]))
    }
  }
  const root = await read('README.md')
  for (const relative of [authorGuide, reference, 'plugins-samples/forge-plugin-hello/README.md']) {
    assert.ok(root.includes(`](${relative})`))
    await fs.access(path.join(repository, relative))
  }
  const skill = await read('.agents/skills/forge-project-init/SKILL.md')
  assert.ok(skill.includes('[references/plugins.md](references/plugins.md)'))
})

for (const generated of [false, true]) {
  const label = generated ? 'renamed customer' : 'template'
  test(`documented install/upgrade/remove commands preserve protections in ${label}`, async t => {
    const { host, next, commands, sources } = await setup(t, generated)
    assert.equal((await cli(host.root, ['--help'])).status, 0)
    assert.equal((await cli(host.root, commands[0])).status, 0)
    assert.match((await cli(host.root, commands[1])).text, /hello\s+1\.0\.0\s+copy/)
    commit(host.root)
    const target = `${host.ui}/src/views/plugins/hello/index.vue`
    const customized = '<template><div>客户定制</div></template>\n'
    await write(host.root, target, customized)
    const rejected = await cli(host.root, commands[2])
    assert.equal(rejected.status, 1)
    assert.match(rejected.text, /未提交|改动/)
    assert.equal(await fs.readFile(path.join(host.root, target), 'utf8'), customized)
    assert.match((await cli(host.root, commands[3])).text, /hello\s+1\.0\.0/)
    commit(host.root)
    const upgraded = await cli(host.root, commands[2])
    assert.equal(upgraded.status, 0, upgraded.text)
    const installed = await fs.readFile(path.join(host.root, target))
    assert.deepEqual(installed, (await readBundle(next)).files.get('ui/index.vue'))
    assert.match((await cli(host.root, commands[3])).text, /hello\s+1\.0\.1/)
    await assertBackup(host.root)
    commit(host.root)
    const removed = await cli(host.root, commands[5])
    assert.equal(removed.status, 0, removed.text)
    assert.match(removed.text, /数据库表、数据和迁移历史未删除/)
    assert.equal((await cli(host.root, commands[6])).text, '未安装插件。')
    await assert.rejects(fs.access(path.join(host.root, target)), { code: 'ENOENT' })
    await assertBackup(host.root)
    const currentSources = [fingerprint((await readBundle(sample)).files), fingerprint((await readBundle(next)).files)]
    assert.deepEqual(currentSources, sources)
  })

  const devLabel = generated ? 'rejects customer' : 'links template without changing source'
  test(`documented dev command ${devLabel}`, async t => {
    const { host, commands, sources } = await setup(t, generated)
    const result = await cli(host.root, commands[4])
    assert.equal(result.status, generated ? 1 : 0, result.text)
    if (generated) {
      assert.match(result.text, /模板/)
      assert.equal((await cli(host.root, commands[6])).text, '未安装插件。')
      return
    }
    const moduleRoot = path.join(host.root, host.server, 'plugins/forge-plugin-hello')
    assert.ok((await fs.lstat(path.join(moduleRoot, 'pom.xml'))).isFile())
    assert.ok((await fs.lstat(path.join(moduleRoot, 'src'))).isSymbolicLink())
    assert.match((await cli(host.root, commands[1])).text, /hello\s+1\.0\.0\s+dev/)
    assert.equal((await cli(host.root, commands[5])).status, 0)
    assert.equal(fingerprint((await readBundle(sample)).files), sources[0])
  })
}

async function assertBackup(root) {
  const directory = path.join(root, '.forge-plugin/backups/hello')
  const entries = await fs.readdir(directory)
  assert.ok(entries.length > 0)
  for (const entry of entries) {
    const backup = path.join(directory, entry)
    const plan = parseStrictJson(await fs.readFile(path.join(backup, 'restore.json'), 'utf8'))
    assert.equal(plan.id, 'hello')
    assert.ok(Object.keys(plan.files).some(relative => relative.endsWith('/pom.xml')))
    assert.ok(Object.hasOwn(plan.files, 'forge.config.json'))
    assert.ok(plan.paths.some(item => item.relative.includes('/plugins/')))
  }
}
