import fs from 'node:fs/promises'
import { safeTarget } from '../../forge-plugin/paths.mjs'
import { ensure } from '../../forge-plugin-builder/errors.mjs'
import { exactKeys, privateDirectory, readJson } from '../config.mjs'

const imagePattern = /^[a-z0-9][a-z0-9./_-]*@sha256:[a-f0-9]{64}$/

export async function readComposeConfig(file) {
  const { value } = await readJson(file)
  exactKeys(value, ['protocolVersion', 'repositoryId', 'vaultRoot', 'outputRoot', 'configRoot',
    'projectName', 'serverImage', 'uiImage', 'serverPort', 'uiPort'], 'COMPOSE_CONFIG_INVALID')
  ensure(value.protocolVersion === 1 && typeof value.repositoryId === 'string'
    && /^[a-z][a-z0-9-]{0,63}$/.test(value.repositoryId) && typeof value.projectName === 'string'
    && /^[a-z][a-z0-9-]{0,31}$/.test(value.projectName), 'COMPOSE_CONFIG_INVALID')
  for (const image of [value.serverImage, value.uiImage]) {
    ensure(typeof image === 'string' && imagePattern.test(image), 'COMPOSE_IMAGE_DIGEST_REQUIRED')
  }
  for (const port of [value.serverPort, value.uiPort]) {
    ensure(Number.isSafeInteger(port) && port >= 1024 && port <= 65535, 'COMPOSE_PORT_INVALID')
  }
  ensure(value.serverPort !== value.uiPort, 'COMPOSE_PORT_CONFLICT')
  const roots = {}
  for (const name of ['vaultRoot', 'outputRoot', 'configRoot']) {
    roots[name] = await privateDirectory(value[name])
    // Compose 会解释 $ 变量；路径必须保持字面值，不能被环境变量或别名替换。
    ensure(/^[A-Za-z0-9_./-]+$/.test(roots[name]), 'COMPOSE_PATH_INVALID')
  }
  const paths = Object.values(roots)
  ensure(paths.every((left, index) => paths.every((right, other) => index === other
    || !(left === right || left.startsWith(right + '/') || right.startsWith(left + '/')))), 'COMPOSE_ROOT_OVERLAP')
  return { ...value, ...roots }
}

export async function checkExternalConfig(config, targets) {
  for (const target of targets) {
    const name = target === 'server' ? 'application.yml' : 'nginx.conf'
    const file = await safeTarget(config.configRoot, name)
    const stat = await fs.lstat(file)
    ensure(stat.isFile() && !stat.isSymbolicLink() && stat.nlink === 1 && stat.uid === process.getuid()
      && (stat.mode & 0o077) === 0 && stat.size > 0, 'COMPOSE_PRIVATE_CONFIG_REQUIRED')
  }
}
