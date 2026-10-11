import assert from 'node:assert/strict'
import fs from 'node:fs/promises'
import path from 'node:path'
import os from 'node:os'
import test from 'node:test'
import { bootstrap } from './bootstrap.mjs'
import { git, resolveCommit, assertUpgradeBranch, treeEntries } from './git.mjs'
import { generatorFiles, validateOptions, cleanSnapshot } from './policy.mjs'
import { planUpgrade, applyUpgrade, verifyBaseline } from './upgrade.mjs'
import { statePath, generatorPath } from './state.mjs'

const options = { 'project-name': 'acme', 'artifact-prefix': 'acme', 'preset': 'full',
  'base-package': 'com.acme.platform', 'group-id': 'com.acme.platform', 'database-name': 'acme_db' }
const sql = 'acme-server/db/migration/V1.0.1__table.sql'

async function write(root, name, value) {
  await fs.mkdir(path.dirname(path.join(root, name)), { recursive: true })
  await fs.writeFile(path.join(root, name), value)
}

function commit(root, message) {
  git(root, ['add', '--all'])
  git(root, ['-c', 'user.name=同步测试', '-c', 'user.email=test@example.invalid', 'commit', '-m', message])
  return resolveCommit(root, 'HEAD')
}

const generator = `import fs from 'node:fs/promises'; import path from 'node:path';
const args = process.argv.slice(2), target=args[0];
await fs.mkdir(target, {recursive:true});
const source = JSON.parse(await fs.readFile(new URL('../../input.json', import.meta.url),'utf8'));
for(const [name,value] of Object.entries(source)) {
  const file=path.join(target,name);await fs.mkdir(path.dirname(file),{recursive:true});await fs.writeFile(file,value);
}
`

async function fixture(t) {
  const temp = await fs.mkdtemp(path.join(os.tmpdir(), 'forge-sync-test-'))
  t.after(() => fs.rm(temp, { recursive: true, force: true }))
  const source = path.join(temp, 'community')
  await fs.mkdir(source)
  git(source, ['init', '-b', 'main'])
  // 同步器需要 commit-tree 身份；只限定当前测试进程，不修改用户全局设置。
  const envKeys = ['GIT_AUTHOR_NAME', 'GIT_AUTHOR_EMAIL', 'GIT_COMMITTER_NAME', 'GIT_COMMITTER_EMAIL']
  const saved = Object.fromEntries(envKeys.map(key => [key, process.env[key]]))
  Object.assign(process.env, { GIT_AUTHOR_NAME: '同步测试', GIT_AUTHOR_EMAIL: 'test@example.invalid',
    GIT_COMMITTER_NAME: '同步测试', GIT_COMMITTER_EMAIL: 'test@example.invalid' })
  t.after(() => {
    for (const key of envKeys) {
      if (saved[key] === undefined) delete process.env[key]
      else process.env[key] = saved[key]
    }
  })
  for (const file of generatorFiles) await write(source, file, file.endsWith('.json') ? '{}' : '')
  await write(source, generatorFiles[0], generator)
  await write(source, 'forge-server/pom.xml', '<properties><revision>1.2.0</revision></properties>')
  const files = { 'acme-server/pom.xml': '<properties><revision>1.2.0</revision></properties>',
    'shared.txt': 'first\nmiddle\nlast\n', [sql]: 'CREATE TABLE example(id BIGINT);\n',
    'package.json': '{"scripts":{}}', 'AGENTS.md': '# 开发规范\n', 'README.md': '# 项目\n',
    'forge.config.json': JSON.stringify({ projectName: 'acme', artifactPrefix: 'acme', forgeVersion: '1.2.0' }) }
  await write(source, 'input.json', JSON.stringify(files))
  const ref = commit(source, '社区初始代码')
  const root = path.join(temp, 'product')
  await bootstrap({ source, ref, options, target: root, generator: source })
  git(root, ['switch', '-c', 'codex/upgrade'])
  return { source, root, files, ref }
}

async function upstreamChange(f, changes) {
  Object.assign(f.files, changes)
  await write(f.source, 'input.json', JSON.stringify(f.files))
  return commit(f.source, '社区增量修复')
}

test('new project records exact provenance, separate history and no push remote', async (t) => {
  const f = await fixture(t)
  const state = await verifyBaseline(f.root)
  assert.equal(state.upstream.commit, f.ref)
  assert.equal(state.upstream.version, '1.2.0')
  assert.equal(state.options['group-id'], 'com.acme.platform')
  assert.equal(git(f.root, ['remote']).trim(), '')
  git(f.root, ['merge-base', '--is-ancestor', state.baseCommit, 'HEAD'])
  assert.equal(git(f.root, ['status', '--porcelain']).trim(), '')
  await assert.rejects(bootstrap({ source: f.source, ref: f.ref, target: f.root,
    options, generator: f.source }), { code: 'EEXIST' })
})

test('three-way merge preserves independent local changes and can abort', async (t) => {
  const f = await fixture(t)
  await write(f.root, 'private-feature.txt', 'private\n')
  commit(f.root, '增加专属功能')
  const oldState = await fs.readFile(path.join(f.root, statePath), 'utf8')
  const ref = await upstreamChange(f, { 'shared.txt': 'fixed\nmiddle\nlast\n' })
  await write(f.source, 'input.json', 'uncommitted corruption must not be used')
  const plan = await planUpgrade({ ...f, ref })
  assert.equal(plan.blocked.length, 0)
  assert.equal(git(f.root, ['status', '--porcelain']).trim(), '')
  const result = await applyUpgrade(f.root)
  assert.equal(result.conflicts, false)
  assert.equal(await fs.readFile(path.join(f.root, 'private-feature.txt'), 'utf8'), 'private\n')
  assert.match(await fs.readFile(path.join(f.root, 'shared.txt'), 'utf8'), /^fixed/)
  await verifyBaseline(f.root)
  git(f.root, ['merge', '--abort'])
  assert.equal(await fs.readFile(path.join(f.root, statePath), 'utf8'), oldState)
  assert.match(await fs.readFile(path.join(f.root, 'shared.txt'), 'utf8'), /^first/)
})

test('overlapping changes remain real Git conflicts without silent overwrites', async (t) => {
  const f = await fixture(t)
  await write(f.root, 'shared.txt', 'private change\nmiddle\nlast\n')
  commit(f.root, '修改公共扩展行为')
  const ref = await upstreamChange(f, { 'shared.txt': 'community change\nmiddle\nlast\n' })
  const plan = await planUpgrade({ ...f, ref })
  assert.equal(plan.conflicted, true)
  assert.equal((await applyUpgrade(f.root)).conflicts, true)
  assert.match(git(f.root, ['diff', '--name-only', '--diff-filter=U']), /shared.txt/)
  await assert.rejects(verifyBaseline(f.root), /未解决冲突/)
})

for (const operation of ['modify', 'delete']) {
  test(`historical migrations reject ${operation} before changing worktree`, async (t) => {
    const f = await fixture(t)
    if (operation === 'delete') delete f.files[sql]
    else f.files[sql] += 'ALTER TABLE example ADD name TEXT;\n'
    const ref = await upstreamChange(f, {})
    const plan = await planUpgrade({ ...f, ref })
    assert.equal(plan.blocked.length, 1)
    await assert.rejects(applyUpgrade(f.root), /历史迁移/)
    assert.equal(git(f.root, ['status', '--porcelain']).trim(), '')
  })
}

test('new migration and merged baseline survive fresh clone', async (t) => {
  const f = await fixture(t)
  const ref = await upstreamChange(f, { 'acme-server/db/migration/V1.0.2__next.sql': 'SELECT 1;\n' })
  await planUpgrade({ ...f, ref })
  await applyUpgrade(f.root)
  await verifyBaseline(f.root)
  commit(f.root, '完成社区同步')
  const clone = `${f.root}-clone`
  git(path.dirname(f.root), ['clone', '--no-local', f.root, clone])
  const state = await verifyBaseline(clone)
  git(clone, ['merge-base', '--is-ancestor', state.baseCommit, 'HEAD'])
  assert.equal(state.upstream.commit, ref)
})

test('dirty files, main branch, changed plan and tampered mapping fail closed', async (t) => {
  const f = await fixture(t)
  git(f.root, ['switch', 'main'])
  assert.throws(() => assertUpgradeBranch(f.root), /升级分支/)
  git(f.root, ['switch', 'codex/upgrade'])
  await write(f.root, 'untracked.txt', 'keep me')
  assert.throws(() => assertUpgradeBranch(f.root), /干净/)
  commit(f.root, '保存改动')
  const ref = await upstreamChange(f, { 'new.txt': 'new' })
  await planUpgrade({ ...f, ref })
  await write(f.root, 'another.txt', 'another')
  commit(f.root, '预览后修改')
  await assert.rejects(applyUpgrade(f.root), /过期/)
  await write(f.root, `${generatorPath}/${generatorFiles[0]}`, 'tamper')
  await assert.rejects(verifyBaseline(f.root), /冻结映射工具/)
})

test('product version is independent from upstream release version', async (t) => {
  const f = await fixture(t)
  await write(f.root, 'acme-server/pom.xml', '<properties><revision>2.0.0</revision></properties>')
  commit(f.root, '产品独立版本')
  await write(f.source, 'forge-server/pom.xml', '<properties><revision>1.3.0</revision></properties>')
  const ref = await upstreamChange(f, {
    'acme-server/pom.xml': '<properties><revision>1.3.0</revision></properties>' })
  await planUpgrade({ ...f, ref })
  await applyUpgrade(f.root)
  assert.match(await fs.readFile(path.join(f.root, 'acme-server/pom.xml'), 'utf8'), /2.0.0/)
  assert.equal((await verifyBaseline(f.root)).upstream.version, '1.3.0')
})

test('policy removes historical artifacts, local credentials and installed plugin files', async (t) => {
  const f = await fixture(t)
  const scratch = `${f.root}-clean`
  const removed = ['acme-server/scripts/deployment/production.json', 'acme-server/plugins/private/pom.xml',
    'acme-admin-ui/.env.development.local', 'acme-admin-ui/.qoder/rules/old.md',
    'acme-report-ui/docs/old.md', 'acme-report-ui/preview.yml']
  for (const name of [...removed, 'code-copilot/memory/preferences.md']) {
    await write(scratch, name, 'secret')
  }
  await write(scratch, 'acme-server/src/App.java', 'class App {}')
  await cleanSnapshot(scratch, options)
  assert.equal(await fs.readFile(path.join(scratch, 'acme-server/src/App.java'), 'utf8'), 'class App {}')
  for (const name of removed) await assert.rejects(fs.stat(path.join(scratch, name)), { code: 'ENOENT' })
  assert.doesNotMatch(await fs.readFile(path.join(scratch, 'code-copilot/memory/preferences.md'), 'utf8'), /secret/)
  await fs.symlink('/etc/hosts', path.join(scratch, 'unsafe'))
  await assert.rejects(cleanSnapshot(scratch, options), /特殊文件/)
})

test('option injection and source symbolic links are rejected', async (t) => {
  assert.throws(() => validateOptions({ ...options, force: true }), /不支持/)
  assert.throws(() => validateOptions({ ...options, 'artifact-prefix': '../other' }), /非法名称/)
  const f = await fixture(t)
  await fs.symlink('/etc/hosts', path.join(f.source, 'outside'))
  const ref = commit(f.source, '含不安全软链接的来源')
  assert.throws(() => treeEntries(f.source, ref), /软链接/)
})
