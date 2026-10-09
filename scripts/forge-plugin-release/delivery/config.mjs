import fs from 'node:fs/promises'
import path from 'node:path'
import { ensure } from '../../forge-plugin-builder/errors.mjs'
import { apiBase } from '../../forge-plugin-builder/worker-http.mjs'
import { exactKeys, privateDirectory, readJson } from '../config.mjs'
import { readCosConfig } from '../cos/config.mjs'
import { readComposeConfig } from '../compose/config.mjs'

export async function readDeliveryConfig(file) {
  const { value } = await readJson(file)
  exactKeys(value, ['protocolVersion', 'apiBaseUrl', 'workerId', 'targetId', 'cosConfig', 'composeConfig',
    'dockerExecutable', 'dockerSocket', 'clientConfigRoot', 'stateRoot'])
  ensure(value.protocolVersion === 1 && typeof value.workerId === 'string'
    && /^[a-z0-9][a-z0-9_-]{0,63}$/.test(value.workerId) && typeof value.targetId === 'string'
    && /^[a-z][a-z0-9-]{0,63}$/.test(value.targetId), 'DELIVERY_CONFIG_INVALID')
  const config = { ...value, apiBaseUrl: apiBase(value.apiBaseUrl) }
  for (const name of ['clientConfigRoot', 'stateRoot']) config[name] = await privateDirectory(value[name])
  ensure(path.isAbsolute(value.dockerExecutable) && path.isAbsolute(value.dockerSocket)
    && !/[,:\x00-\x1f\x7f]/.test(value.dockerSocket), 'DELIVERY_DOCKER_INVALID')
  const executable = await fs.lstat(value.dockerExecutable)
  ensure(executable.isFile() && !executable.isSymbolicLink() && (executable.mode & 0o022) === 0
    && (executable.mode & 0o111) !== 0 && [0, process.getuid()].includes(executable.uid),
  'DELIVERY_EXECUTABLE_UNSAFE')
  config.cos = await readCosConfig(value.cosConfig)
  config.compose = await readComposeConfig(value.composeConfig)
  ensure(config.cos.vaultRoot === config.compose.vaultRoot
    && config.cos.repositoryId === config.compose.repositoryId, 'DELIVERY_REPOSITORY_MISMATCH')
  ensure((await fs.readdir(config.clientConfigRoot)).length === 0, 'DELIVERY_EMPTY_DOCKER_CONFIG_REQUIRED')
  const roots = [config.stateRoot, config.clientConfigRoot, config.cos.vaultRoot,
    config.compose.outputRoot, config.compose.configRoot]
  ensure(roots.every((left, index) => roots.every((right, other) => index === other
    || !(left === right || left.startsWith(right + '/') || right.startsWith(left + '/')))),
  'DELIVERY_ROOT_OVERLAP')
  return config
}
