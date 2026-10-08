import assert from 'node:assert/strict'
import test from 'node:test'
import fs from 'node:fs/promises'
import path from 'node:path'
import { fixture as buildFixture, fakeDocker, socketFixture } from '../forge-plugin-builder/fixtures/helpers.mjs'
import { executeBuild } from '../forge-plugin-builder/builder.mjs'
import { reportResult } from '../forge-plugin-builder/worker-result.mjs'
import { sha256 } from '../forge-plugin-builder/files.mjs'
import { fixture, write } from './fixtures/helpers.mjs'
import { readReleaseConfig } from './config.mjs'
import { publishRelease } from './vault.mjs'
import { verifyRelease } from './verify.mjs'

test('现有Git/ZIP结果直连封存：容器为桩，产物文件和摘要为实际核验', async (t) => {
  const build = await buildFixture(t)
  await socketFixture(t, build.config.dockerSocket)
  const docker = fakeDocker(build.config, { build: async args => {
    const mount = args.find(arg => arg.endsWith('dst=/output'))
    const root = mount.slice('type=bind,src='.length, mount.indexOf(',dst='))
    await write(root, 'backend/admin.jar', 'PK\x03\x04synthetic build integration jar')
    await write(root, 'frontend/index.html', '<main>synthetic integration UI</main>')
  } })
  const result = await executeBuild(build.config, { run: true, reviewed: true, execute: docker.execute })
  assert.equal(result.status, 'built')
  const item = await fixture(t)
  const jobRoot = await fs.realpath(path.join(build.config.workspaceRoot, result.jobId))
  const config = { ...item.config, jobRoot,
    resultSha256: sha256(await fs.readFile(path.join(jobRoot, 'result.json'))) }
  await write(item.root, 'release.json', JSON.stringify(config))
  const sealed = await publishRelease(await readReleaseConfig(item.file), { reviewed: true })
  const report = reportResult(result, build.config, 'artifact_verification')
  assert.equal(sealed.artifactManifestSha256, report.artifactManifestSha256)
  assert.equal(sealed.jobId, report.jobId)
  assert.equal(sealed.artifactCount, report.artifactCount)
  assert.equal(sealed.artifactBytes, report.artifactBytes)
  assert.equal((await verifyRelease(await readReleaseConfig(item.vaultFile, 'verify'), sealed.releaseId)).status,
    'verified')
  assert.equal(sealed.liveTaskApprovalVerified, false)
  assert.equal(sealed.deployed, false)
})
