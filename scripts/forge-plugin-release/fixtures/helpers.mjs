import fs from 'node:fs/promises'
import path from 'node:path'
import os from 'node:os'
import { sha256 } from '../../forge-plugin-builder/files.mjs'
import { collectArtifacts } from '../../forge-plugin-builder/artifacts.mjs'
import { serialize } from '../manifest.mjs'

export async function fixture(t, targets = { server: 'plugins/example', ui: 'example-admin-ui' }) {
  const root = await fs.realpath(await fs.mkdtemp(path.join(os.tmpdir(), 'forge-release-test-')))
  t.after(async () => {
    // 仅测试自行分配的目录；封存文件只读，先恢复测试目录权限以清理 fixture。
    await writable(root)
    await fs.rm(root, { recursive: true, force: true })
  })
  const jobRoot = path.join(root, 'job-synthetic')
  const vaultRoot = path.join(root, 'vault')
  await fs.mkdir(jobRoot, { mode: 0o700 })
  await fs.mkdir(vaultRoot, { mode: 0o700 })
  const artifactRoot = path.join(jobRoot, 'artifacts')
  await fs.mkdir(artifactRoot, { mode: 0o700 })
  if (targets.server) await write(artifactRoot, 'backend/admin.jar', Buffer.from('PK\x03\x04synthetic test jar'))
  if (targets.ui) await write(artifactRoot, 'frontend/index.html', '<main>synthetic fixture</main>')
  const result = { protocolVersion: 1, jobId: 'job-synthetic', status: 'built', deployed: false,
    image: `forge-builder@sha256:${'a'.repeat(64)}`, packageSha256: 'b'.repeat(64),
    source: { commit: 'c'.repeat(40), sha256: 'd'.repeat(64), scope: 'admin-build' },
    preflight: { pluginId: 'example', version: '1.0.0', coreVersion: '1.2.0', edition: 'community',
      operation: 'install', targets }, artifacts: await collectArtifacts(artifactRoot, targets) }
  const config = { protocolVersion: 1, repositoryId: 'local-test', vaultRoot, jobRoot,
    resultSha256: sha256(serialize(result)) }
  const file = path.join(root, 'release.json')
  const vaultFile = path.join(root, 'vault.json')
  await write(jobRoot, 'result.json', serialize(result))
  await write(root, 'release.json', serialize(config))
  await write(root, 'vault.json', serialize({ protocolVersion: 1, repositoryId: config.repositoryId, vaultRoot }))
  return { root, jobRoot, vaultRoot, artifactRoot, result, config, file, vaultFile }
}

export async function write(root, relative, data) {
  const target = path.join(root, relative)
  await fs.mkdir(path.dirname(target), { recursive: true, mode: 0o700 })
  await fs.writeFile(target, data, { mode: 0o600 })
}

export async function updateResult(item, change) {
  change(item.result)
  const data = serialize(item.result)
  await write(item.jobRoot, 'result.json', data)
  item.config.resultSha256 = sha256(data)
  await write(item.root, 'release.json', serialize(item.config))
}

export async function writable(root) {
  const stat = await fs.lstat(root)
  if (stat.isSymbolicLink()) return
  await fs.chmod(root, stat.isDirectory() ? 0o700 : 0o600)
  if (stat.isDirectory()) {
    for (const name of await fs.readdir(root)) await writable(path.join(root, name))
  }
}
