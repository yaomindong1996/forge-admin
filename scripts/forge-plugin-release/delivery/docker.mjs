import path from 'node:path'
import { verifyDocker, dockerClient } from '../../forge-plugin-builder/docker.mjs'
import { ensure } from '../../forge-plugin-builder/errors.mjs'
import { safeTarget } from '../../forge-plugin/paths.mjs'
import { verifyCompose } from '../compose/prepare.mjs'
import { composeDocument } from '../compose/document.mjs'
import { readJson } from '../config.mjs'
import { normalizeManifest, metadataLimit } from '../manifest.mjs'

export async function verifyTarget(config, manifest, execute) {
  for (const image of [config.compose.serverImage, config.compose.uiImage]) {
    await verifyDocker({ ...config, workspaceRoot: config.stateRoot, image }, execute)
  }
  const client = dockerClient(config, execute)
  const version = (await client(['compose', 'version', '--short'])).toString('utf8').trim()
  ensure(/^v?2\.\d+\.\d+(?:[-+].*)?$/.test(version), 'COMPOSE_V2_REQUIRED')
  return client
}
export function composeArgs(config, id, tail) {
  return ['compose', '--project-name', config.compose.projectName, '--env-file', '/dev/null',
    '--project-directory', path.join(config.compose.outputRoot, id),
    '--file', path.join(config.compose.outputRoot, id, 'compose.json'), ...tail]
}
export async function assertCurrent(client, config, claim, manifest) {
  const expected = claim.previousReleaseId
  const unverified = claim.unverifiedReleaseId
  const names = (await client(['ps', '-a', '--filter', 'label=com.docker.compose.project='
    + config.compose.projectName, '--format', '{{.ID}}'])).toString('utf8').trim().split('\n').filter(Boolean)
  ensure(names.length <= 2, 'DEPLOYMENT_PROJECT_CONFLICT')
  if (!expected && !unverified) { ensure(names.length === 0, 'DEPLOYMENT_UNMANAGED_PROJECT'); return }
  ensure(names.length > 0, 'DEPLOYMENT_PREVIOUS_MISSING')
  const containers = JSON.parse((await client(['inspect', ...names])).toString('utf8'))
  ensure(containers.length === names.length && containers.every(row =>
    [expected, unverified].filter(Boolean).includes(row.Config?.Labels?.['forge.release-id'])
    && row.Config?.Labels?.['com.docker.compose.project'] === config.compose.projectName
    && ['server', 'ui'].includes(row.Config?.Labels?.['com.docker.compose.service'])),
  'DEPLOYMENT_PREVIOUS_MISMATCH')
  const services = containers.map(row => row.Config.Labels['com.docker.compose.service']).sort()
  ensure(new Set(services).size === services.length
    && JSON.stringify(services) === JSON.stringify(manifest.targets), 'DEPLOYMENT_TARGET_PROFILE_CHANGED')
  // 标签只是定位，仍核对上一部署包实际字节和只读挂载，避免误覆盖同名项目。
  for (const row of containers) {
    const id = row.Config.Labels['forge.release-id']
    await verifyCompose(config.compose, id)
    const { value } = await readJson(await safeTarget(config.compose.outputRoot, id + '/manifest.json'), metadataLimit)
    const old = composeDocument(config.compose, { ...normalizeManifest(value), digest: id.slice(4) })
    const service = old.services[row.Config.Labels['com.docker.compose.service']]
    ensure(service && service.image === row.Config.Image, 'DEPLOYMENT_PREVIOUS_MISMATCH')
    checkMounts(row, service, config, id)
  }
}
export async function verifyContainers(client, config, id, manifest) {
  const names = (await client(composeArgs(config, id, ['ps', '-q', '--all']))).toString('utf8')
    .trim().split('\n').filter(Boolean)
  ensure(names.length === manifest.targets.length, 'DEPLOYMENT_CONTAINER_COUNT')
  const rows = JSON.parse((await client(['inspect', ...names])).toString('utf8'))
  const seen = new Set()
  const document = composeDocument(config.compose, { ...manifest, digest: id.slice(4) })
  for (const row of rows) {
    const name = row.Config?.Labels?.['com.docker.compose.service']
    ensure(document.services[name] && !seen.has(name), 'DEPLOYMENT_CONTAINER_INVALID')
    seen.add(name)
    const expected = document.services[name]
    ensure(row.State?.Running === true && row.Config?.Image === expected.image
      && row.Config?.Labels?.['forge.release-id'] === id && row.HostConfig?.ReadonlyRootfs === true
      && row.Config?.Labels?.['com.docker.compose.project'] === config.compose.projectName
      && row.Config?.User === '0:0' && row.HostConfig?.Privileged === false
      && row.HostConfig?.Memory === 2147483648 && row.HostConfig?.MemorySwap === 2147483648
      && row.HostConfig?.NanoCpus === 2000000000 && row.HostConfig?.PidsLimit === 256
      && row.HostConfig?.CapDrop?.includes('ALL')
      && row.HostConfig?.SecurityOpt?.some(value => value.startsWith('no-new-privileges')),
    'DEPLOYMENT_CONTAINER_INVALID')
    checkMounts(row, expected, config, id)
  }
  await verifyCompose(config.compose, id)
}
function checkMounts(row, expected, config, id) {
  for (const bind of expected.volumes) {
    const source = path.resolve(config.compose.outputRoot, id, bind.source)
    ensure(row.Mounts?.some(mount => mount.Type === 'bind' && mount.RW === false
      && mount.Destination === bind.target && mount.Source === source), 'DEPLOYMENT_MOUNT_MISMATCH')
  }
}
export async function switchCompose(client, config, id, context) {
  await verifyCompose(config.compose, id, context.signal)
  await client(composeArgs(config, id, ['config', '--quiet']), { signal: context.signal })
  await context.authorize()
  // 只从真正发起up开始视为结果未知；纯配置检查失败不能冒充发生过切换。
  context.switched = true
  await client(composeArgs(config, id, ['up', '-d', '--no-build', '--pull', 'never',
    '--wait', '--wait-timeout', '120']), { signal: context.signal, timeoutMs: 180000 })
}
