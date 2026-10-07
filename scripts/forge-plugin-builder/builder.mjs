import fs from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { inspectPluginInstall } from '../forge-plugin/installer.mjs'
import { loadProject, targetsFor } from '../forge-plugin/project.mjs'
import { readInstalledDirectory, fingerprint } from '../forge-plugin/bundle.mjs'
import { safeTarget } from '../forge-plugin/paths.mjs'
import { readZip, limits } from '../forge-plugin/zip.mjs'
import { compareVersions } from '../forge-shared/version.mjs'
import { readRegular, sha256, writePrivate } from './files.mjs'
import { snapshotSource } from './snapshot.mjs'
import { collectArtifacts } from './artifacts.mjs'
import { verifyDocker, runContainer } from './docker.mjs'
import { ensure, errorCode } from './errors.mjs'

const scriptsRoot = fileURLToPath(new URL('..', import.meta.url))

export async function executeBuild(config, options = {}) {
  ensure(!options.run || options.reviewed === true, 'REVIEW_REQUIRED')
  const root = await fs.mkdtemp(path.join(config.workspaceRoot, 'job-'))
  const job = { root, id: path.basename(root), source: path.join(root, 'source'),
    package: path.join(root, 'package.zip'), control: path.join(root, 'control'),
    output: path.join(root, 'artifacts'), force: !!options.force }
  const result = { protocolVersion: 1, jobId: job.id, status: 'failed', deployed: false,
    image: config.image, packageSha256: config.packageSha256 }
  let phase = 'source_snapshot'
  try {
    result.source = await snapshotSource(config, job.source, options.signal)
    phase = 'package_preflight'
    const archive = await readRegular(config.packageFile, limits.archive)
    ensure(sha256(archive) === config.packageSha256, 'PACKAGE_DIGEST_MISMATCH')
    validatePackageFiles(archive)
    await writePrivate(root, 'package.zip', archive)
    phase = 'source_preflight'
    await verifySourceOwnership(config.sourceRoot)
    const context = await loadProject(job.source)
    ensure(context.config.plugins.every(record => record.mode === 'copy'), 'DEV_PLUGIN_UNSUPPORTED')
    result.preflight = await inspectPluginInstall(job.source, job.package, { force: job.force })
    ensure(result.preflight.edition === 'community', 'COMMUNITY_PACKAGE_REQUIRED')
    ensure(!result.preflight.previousVersion
      || compareVersions(result.preflight.version, result.preflight.previousVersion) >= 0, 'PLUGIN_DOWNGRADE')
    await writePrivate(root, 'preflight.json', `${JSON.stringify(result.preflight, null, 2)}\n`)
    if (options.run) {
      phase = 'container_build'
      await buildJob(config, job, options)
      phase = 'artifact_verification'
      result.artifacts = await collectArtifacts(job.output, result.preflight.targets, options.signal)
    }
    result.status = options.run ? 'built' : 'checked'
  }
  catch (error) {
    result.failureCode = errorCode(error)
    result.failurePhase = phase
  }
  await writePrivate(root, 'result.json', `${JSON.stringify(result, null, 2)}\n`)
  return result
}

async function verifySourceOwnership(root) {
  const context = await loadProject(root)
  for (const record of context.config.plugins) {
    ensure(record.mode === 'copy', 'DEV_PLUGIN_UNSUPPORTED')
    for (const [kind, relative] of Object.entries(targetsFor(context, record))) {
      // 快照不带忽略文件，但原安装目录的本地配置也不能被误判成可安全替换。
      const files = await readInstalledDirectory(await safeTarget(root, relative))
      ensure(fingerprint(files) === record.checksums[kind], 'SOURCE_CUSTOMIZATION_BLOCKED')
    }
  }
}

async function buildJob(config, job, options) {
  const dockerConfig = { ...config, clientConfigRoot: path.join(job.root, 'docker-config') }
  await fs.mkdir(dockerConfig.clientConfigRoot, { mode: 0o700 })
  await verifyDocker(dockerConfig, options.execute)
  await fs.mkdir(job.control, { mode: 0o700 })
  await fs.mkdir(job.output, { mode: 0o700 })
  for (const directory of ['forge-plugin', 'forge-shared', 'forge-plugin-builder']) {
    const source = path.join(scriptsRoot, directory)
    const entries = await fs.readdir(source, { withFileTypes: true })
    for (const entry of entries.filter(item => item.name.endsWith('.mjs') && !item.name.endsWith('.test.mjs'))) {
      ensure(entry.isFile(), 'CONTROL_FILE_UNSAFE')
      await writePrivate(job.control, `${directory}/${entry.name}`,
        await readRegular(path.join(source, entry.name), 1024 * 1024))
    }
  }
  await runContainer(dockerConfig, job, options)
}

function validatePackageFiles(archive) {
  const names = [...readZip(archive).keys()]
  const denied = new Set(['.git', '.forge-plugin', 'node_modules', 'target', 'dist', 'logs', '.DS_Store',
    '.flattened-pom.xml', '.forge-plugin-owned.json'])
  ensure(names.every(name => !name.split('/').some(part => denied.has(part) || part.startsWith('.env')
    || /^application-(dev|local)\./.test(part) || /\.(pem|key|p12|pfx|secrets)$/i.test(part))),
  'PACKAGE_LOCAL_CONFIG_OR_OUTPUT')
}
