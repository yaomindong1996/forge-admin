import assert from 'node:assert/strict'
import { createHash } from 'node:crypto'
import fs from 'node:fs/promises'
import os from 'node:os'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import test from 'node:test'
import { collectManifest, compareManifests } from './manifest.mjs'

const script = fileURLToPath(new URL('./manifest.mjs', import.meta.url))

async function fixture(t) {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'forge-t0-manifest-'))
  t.after(() => fs.rm(root, { recursive: true, force: true }))
  return root
}

test('全部文件按固定路径顺序记录，包含点文件、空文件和二进制', async t => {
  const root = await fixture(t)
  await fs.mkdir(path.join(root, '目录'))
  await fs.writeFile(path.join(root, '目录', '背景.png'), Buffer.from([0, 255, 4, 10]))
  await fs.writeFile(path.join(root, '.gitkeep'), '')
  await fs.writeFile(path.join(root, 'pom.xml'), '<version>1.1.0</version>\n')
  const manifest = await collectManifest(root)
  assert.equal(manifest.fileCount, 3)
  assert.deepEqual(manifest.files.map(file => file.path), ['.gitkeep', 'pom.xml', '目录/背景.png'])
  assert.equal(manifest.files[0].sha256, createHash('sha256').update('').digest('hex'))
  assert.equal(manifest.files[2].bytes, 4)
  assert.deepEqual(await collectManifest(root), manifest)
})

test('新增、删除和仅字节差异都不能通过基线验证', async t => {
  const root = await fixture(t)
  await fs.writeFile(path.join(root, 'keep'), '原始\n')
  await fs.writeFile(path.join(root, 'removed'), '')
  const expected = await collectManifest(root)
  await fs.writeFile(path.join(root, 'keep'), '原始\r\n')
  await fs.unlink(path.join(root, 'removed'))
  await fs.writeFile(path.join(root, 'added'), '')
  assert.deepEqual(compareManifests(expected, await collectManifest(root)), {
    missing: ['removed'], added: ['added'], changed: ['keep'],
  })
})

test('软链接不跟随，不读取工程外内容', async t => {
  const root = await fixture(t)
  await fs.symlink(script, path.join(root, 'external'))
  await assert.rejects(collectManifest(root), /软链接或特殊文件/)
})

test('命令行验证失败返回非零状态，且禁止覆盖已存在的基线', async t => {
  const root = await fixture(t)
  const project = path.join(root, 'project')
  const manifest = path.join(root, 'manifest.json')
  await fs.mkdir(project)
  await fs.writeFile(path.join(project, 'data'), '原始')
  const run = action => spawnSync(process.execPath, [script, action, project, manifest], { encoding: 'utf8' })
  assert.equal(run('record').status, 0)
  assert.equal(run('verify').status, 0)
  const original = await fs.readFile(manifest, 'utf8')
  assert.equal(run('record').status, 1)
  assert.equal(await fs.readFile(manifest, 'utf8'), original)
  await fs.writeFile(path.join(project, 'data'), '修改')
  const result = run('verify')
  assert.equal(result.status, 1)
  assert.match(result.stderr, /基线不一致/)
})

test('存档清单计数、摘要及文件范围与来源记录一致', async () => {
  const provenance = JSON.parse(await fs.readFile(new URL('./provenance.json', import.meta.url), 'utf8'))
  const patch = await fs.readFile(new URL(provenance.versionPatch, import.meta.url))
  assert.equal(createHash('sha256').update(patch).digest('hex'), provenance.versionPatchSha256)
  for (const entry of provenance.cases) {
    const bytes = await fs.readFile(new URL(entry.manifest, import.meta.url))
    assert.equal(createHash('sha256').update(bytes).digest('hex'), entry.manifestSha256)
    const manifest = JSON.parse(bytes.toString('utf8'))
    assert.equal(manifest.schemaVersion, 1)
    assert.equal(manifest.fileCount, entry.fileCount)
    assert.equal(manifest.files.length, entry.fileCount)
    const paths = manifest.files.map(file => file.path)
    assert.equal(new Set(paths).size, paths.length)
    assert.deepEqual(paths, [...paths].sort())
    for (const file of manifest.files) {
      assert.match(file.sha256, /^[a-f0-9]{64}$/)
      assert.ok(Number.isSafeInteger(file.bytes) && file.bytes >= 0)
      assert.doesNotMatch(file.path, /(^|\/)(node_modules|target|\.git)(\/|$)/)
      assert.doesNotMatch(file.path, /(^|\/)(application-dev\.yml|\.env\.local)$/)
      assert.doesNotMatch(file.path, /^code-copilot\/changes\/(?!\.gitkeep$)/)
    }
  }
})
