import assert from 'node:assert/strict'
import test from 'node:test'
import fs from 'node:fs/promises'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import { fixture, updateResult, write } from './fixtures/helpers.mjs'
import { readReleaseConfig, readJson } from './config.mjs'
import { checkRelease } from './vault.mjs'

for (const kind of ['unknown-key', 'duplicate-key', 'protocol', 'digest', 'repo', 'array', 'oversize', 'bad-utf8']) {
  test(`配置 ${kind} 失败关闭，不解释命令或 URL`, async (t) => {
    const item = await fixture(t)
    if (kind === 'unknown-key') item.config.command = 'synthetic command'
    if (kind === 'protocol') item.config.protocolVersion = 2
    if (kind === 'digest') item.config.resultSha256 = 'not a digest'
    if (kind === 'repo') item.config.repositoryId = 'https://example.invalid'
    let data = JSON.stringify(item.config)
    if (kind === 'duplicate-key') data = data.replace('"protocolVersion":1', '"protocolVersion":1,"protocolVersion":1')
    if (kind === 'array') data = '[]'
    if (kind === 'oversize') data = ' '.repeat(16385)
    if (kind === 'bad-utf8') data = Buffer.from([0xff])
    await write(item.root, 'release.json', data)
    await assert.rejects(readReleaseConfig(item.file))
    assert.deepEqual(await fs.readdir(item.vaultRoot), [])
  })
}

test('目录必须私有且 canonical；根、目录别名、重叠和 file 链接拒绝', async (t) => {
  const item = await fixture(t)
  await fs.chmod(item.vaultRoot, 0o755)
  await assert.rejects(readReleaseConfig(item.file), /RELEASE_ROOT_NOT_PRIVATE/)
  await fs.chmod(item.vaultRoot, 0o700)
  await fs.symlink(item.vaultRoot, path.join(item.root, 'alias'))
  for (const vaultRoot of [path.join(item.root, 'alias'), `${item.vaultRoot}/../vault`, item.jobRoot, '/']) {
    await write(item.root, 'release.json', JSON.stringify({ ...item.config, vaultRoot }))
    await assert.rejects(readReleaseConfig(item.file))
  }
  await fs.mkdir(path.join(item.jobRoot, 'nested'), { mode: 0o700 })
  const overlapping = { ...item.config, vaultRoot: path.join(item.jobRoot, 'nested') }
  await write(item.root, 'release.json', JSON.stringify(overlapping))
  await assert.rejects(readReleaseConfig(item.file), /RELEASE_ROOT_OVERLAP/)
  await fs.symlink(item.file, path.join(item.root, 'file-link'))
  await assert.rejects(readReleaseConfig(path.join(item.root, 'file-link')))
})

test('拒绝 root 身份和非当前用户的目录，不通过默认用户兜底', async (t) => {
  const item = await fixture(t)
  const getuid = process.getuid
  const geteuid = process.geteuid
  try {
    process.getuid = () => 0
    await assert.rejects(readReleaseConfig(item.file), /NONROOT_POSIX_REQUIRED/)
    process.getuid = getuid
    process.geteuid = () => 0
    await assert.rejects(readReleaseConfig(item.file), /NONROOT_POSIX_REQUIRED/)
    process.geteuid = geteuid
    process.getuid = () => getuid() + 1
    await assert.rejects(readReleaseConfig(item.file), /RELEASE_ROOT_NOT_PRIVATE/)
  }
  finally { process.getuid = getuid; process.geteuid = geteuid }
})

for (const kind of ['edition', 'commit', 'image', 'targets', 'duplicate-file', 'path', 'order',
  'count', 'total', 'bytes', 'extra-field', 'coerced-digest', 'version', 'plugin-id']) {
  test(`报告元数据 ${kind} 不允许封存`, async (t) => {
    const item = await fixture(t)
    await updateResult(item, result => {
      if (kind === 'edition') result.preflight.edition = 'pro'
      if (kind === 'commit') result.source.commit = 'not a commit'
      if (kind === 'image') result.image = 'image:latest'
      if (kind === 'targets') result.preflight.targets = { flow: 'flow' }
      if (kind === 'duplicate-file') result.artifacts.push(result.artifacts[0])
      if (kind === 'path') result.artifacts[0].path = '../escape'
      if (kind === 'order') result.artifacts.reverse()
      if (kind === 'count') result.artifacts = Array(4097).fill(result.artifacts[0])
      if (kind === 'total') result.artifacts = [0, 1, 2].map(index => ({ path: `frontend/${index}.js`,
        bytes: 512 * 1024 * 1024, sha256: 'a'.repeat(64) }))
      if (kind === 'bytes') result.artifacts[0].bytes = 1.5
      if (kind === 'extra-field') result.artifacts[0].command = 'synthetic'
      if (kind === 'coerced-digest') result.source.sha256 = ['a'.repeat(64)]
      if (kind === 'version') result.preflight.version = '01.0.0'
      if (kind === 'plugin-id') result.preflight.pluginId = 'x'.repeat(33)
    })
    await assert.rejects(checkRelease(await readReleaseConfig(item.file)))
  })
}

test('JSON 结果有界、重复/保留键和硬链接拒绝', async (t) => {
  const item = await fixture(t)
  for (const data of ['{"status":"built","status":"built"}', '{"constructor":{}}']) {
    await write(item.root, 'invalid.json', data)
    await assert.rejects(readJson(path.join(item.root, 'invalid.json')))
  }
  await write(item.root, 'invalid.json', '12345')
  await assert.rejects(readJson(path.join(item.root, 'invalid.json'), 4), /FILE_UNSAFE/)
  await fs.link(path.join(item.jobRoot, 'result.json'), path.join(item.root, 'hard-result'))
  await assert.rejects(checkRelease(item.config), /FILE_UNSAFE/)
})

test('FIFO 配置文件立即拒绝，不等外部写入者且不泄露路径', async (t) => {
  const item = await fixture(t)
  const fifo = path.join(item.root, 'pipe')
  assert.equal(spawnSync('mkfifo', [fifo], { encoding: 'utf8' }).status, 0)
  const entry = fileURLToPath(new URL('./index.mjs', import.meta.url))
  const output = spawnSync(process.execPath, [entry, 'check', fifo], {
    encoding: 'utf8', timeout: 1000, killSignal: 'SIGKILL',
  })
  assert.equal(output.error, undefined)
  assert.equal(output.status, 1)
  assert.equal(JSON.parse(output.stderr).code, 'FILE_UNSAFE')
  assert.ok(!output.stderr.includes(item.root))
})
