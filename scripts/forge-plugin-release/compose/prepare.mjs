import fs from 'node:fs/promises'
import path from 'node:path'
import { safeTarget, statOptional } from '../../forge-plugin/paths.mjs'
import { ensure } from '../../forge-plugin-builder/errors.mjs'
import { writePrivate } from '../../forge-plugin-builder/files.mjs'
import { collectArtifacts } from '../../forge-plugin-builder/artifacts.mjs'
import { metadataLimit, normalizeManifest, serialize, targetProfile } from '../manifest.mjs'
import { readJson } from '../config.mjs'
import { verifyRelease } from '../verify.mjs'
import { copyArtifacts, sealTree, syncDirectory } from '../copy.mjs'
import { checkExternalConfig } from './config.mjs'
import { composeDocument } from './document.mjs'

export async function inspectCompose(config, id, signal) {
  const verified = await verifyRelease(config, id, signal)
  const root = await safeTarget(config.vaultRoot, id)
  const { value } = await readJson(await safeTarget(root, 'manifest.json'), metadataLimit)
  const manifest = normalizeManifest(value)
  await checkExternalConfig(config, manifest.targets)
  return { manifest, root, summary: { protocolVersion: 1, releaseId: id,
    manifestSha256: verified.manifestSha256, artifactManifestSha256: manifest.artifactManifestSha256,
    artifactCount: manifest.artifacts.length, deploymentKind: 'docker-compose', requiresRootless: true,
    prepared: false, deployed: false, liveTaskApprovalVerified: false,
    pending: ['target_permissions', 'live_task_approval', 'database_backup', 'migration_review',
      'compose_runtime_validation', 'runtime_plugin_and_ui_verification', 'restore_verification'] } }
}

export async function prepareCompose(config, id, options = {}) {
  ensure(options.reviewed === true, 'COMPOSE_REVIEW_REQUIRED')
  const inspected = await inspectCompose(config, id, options.signal)
  const finalRoot = await safeTarget(config.outputRoot, id)
  ensure(!await statOptional(finalRoot), 'COMPOSE_PACKAGE_EXISTS')
  const staging = await fs.mkdtemp(path.join(config.outputRoot, '.pending-compose-'))
  await fs.chmod(staging, 0o700)
  await copyArtifacts(await safeTarget(inspected.root, 'artifacts'), path.join(staging, 'artifacts'),
    inspected.manifest.artifacts, options.signal)
  const compose = composeDocument(config, { ...inspected.manifest, digest: id.slice(4) })
  await writePrivate(staging, 'compose.json', serialize(compose))
  await writePrivate(staging, 'manifest.json', serialize(inspected.manifest))
  const receipt = { ...inspected.summary, prepared: true }
  await writePrivate(staging, 'preparation.json', serialize(receipt))
  const actual = await collectArtifacts(path.join(staging, 'artifacts'),
    targetProfile(inspected.manifest), options.signal)
  ensure(JSON.stringify(actual) === JSON.stringify(inspected.manifest.artifacts), 'COMPOSE_ARTIFACT_CHANGED')
  // 外部敏感配置仅检查私有文件，不复制到部署包或制品库；封存源再次核验。
  await inspectCompose(config, id, options.signal)
  await sealTree(staging)
  ensure(!options.signal?.aborted, 'INTERRUPTED')
  ensure(!await statOptional(finalRoot), 'COMPOSE_PACKAGE_EXISTS')
  await fs.chmod(staging, 0o700)
  await fs.rename(staging, finalRoot)
  await fs.chmod(finalRoot, 0o500)
  await syncDirectory(config.outputRoot)
  await verifyCompose(config, id, options.signal)
  return { ...receipt, packageRoot: finalRoot }
}

export async function verifyCompose(config, id, signal) {
  const inspected = await inspectCompose(config, id, signal)
  const root = await safeTarget(config.outputRoot, id)
  const names = await fs.readdir(root)
  ensure(names.length === 4 && names.every(name =>
    ['artifacts', 'manifest.json', 'compose.json', 'preparation.json'].includes(name)), 'COMPOSE_PACKAGE_INVALID')
  await checkPackagePermissions(root)
  const expected = {
    'manifest.json': inspected.manifest,
    'compose.json': composeDocument(config, { ...inspected.manifest, digest: id.slice(4) }),
    'preparation.json': { ...inspected.summary, prepared: true },
  }
  for (const [name, value] of Object.entries(expected)) {
    const { data } = await readJson(await safeTarget(root, name), metadataLimit)
    ensure(data.toString('utf8') === serialize(value), 'COMPOSE_PACKAGE_METADATA_CHANGED')
  }
  const actual = await collectArtifacts(path.join(root, 'artifacts'), targetProfile(inspected.manifest), signal)
  ensure(JSON.stringify(actual) === JSON.stringify(inspected.manifest.artifacts), 'COMPOSE_ARTIFACT_CHANGED')
  return { ...inspected.summary, prepared: true, packageVerified: true, deployed: false }
}

async function checkPackagePermissions(root) {
  const stat = await fs.lstat(root)
  ensure(stat.isDirectory() && !stat.isSymbolicLink() && stat.uid === process.getuid()
    && (stat.mode & 0o777) === 0o500, 'COMPOSE_PACKAGE_PERMISSION_INVALID')
  for (const name of await fs.readdir(root)) {
    const file = await safeTarget(root, name)
    const item = await fs.lstat(file)
    if (item.isDirectory()) await checkPackagePermissions(file)
    else ensure(item.isFile() && item.nlink === 1 && item.uid === process.getuid()
      && (item.mode & 0o777) === 0o400, 'COMPOSE_PACKAGE_PERMISSION_INVALID')
  }
}
