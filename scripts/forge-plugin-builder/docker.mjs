import fs from 'node:fs/promises'
import path from 'node:path'
import { randomUUID } from 'node:crypto'
import { runProcess, cleanEnvironment } from './process.mjs'
import { writePrivate } from './files.mjs'
import { ensure, BuildError } from './errors.mjs'

const label = 'forge.plugin-builder.owner'
export const buildTimeoutMs = 20 * 60 * 1000

export async function verifyDocker(config, execute = runProcess) {
  ensure(process.getuid() !== 0, 'ROOT_COORDINATOR_FORBIDDEN')
  const socket = await fs.lstat(config.dockerSocket).catch(() => { throw new BuildError('DOCKER_SOCKET_UNAVAILABLE') })
  ensure(socket.isSocket() && !socket.isSymbolicLink() && socket.uid === process.getuid(), 'DOCKER_SOCKET_UNSAFE')
  const parent = await fs.lstat(path.dirname(config.dockerSocket))
  ensure(parent.isDirectory() && !parent.isSymbolicLink() && parent.uid === process.getuid()
    && (parent.mode & 0o077) === 0, 'DOCKER_SOCKET_DIRECTORY_UNSAFE')
  const client = dockerClient(config, execute)
  const info = JSON.parse((await client(['info', '--format', '{{json .}}'])).toString('utf8'))
  ensure(Array.isArray(info.SecurityOptions)
    && info.SecurityOptions.some(value => /^name=rootless(?:,|$)/.test(value)), 'ROOTLESS_REQUIRED')
  // rootless + cgroup v1/未委派控制器会忽略限制，不能只检查 run 参数里写了 memory/cpus。
  ensure(info.CgroupVersion === '2' && info.CgroupDriver === 'systemd'
    && ['MemoryLimit', 'SwapLimit', 'CpuCfsPeriod', 'CpuCfsQuota', 'PidsLimit'].every(key => info[key] === true),
  'ROOTLESS_RESOURCE_LIMITS_UNAVAILABLE')
  const digests = JSON.parse((await client(['image', 'inspect', config.image,
    '--format', '{{json .RepoDigests}}'])).toString('utf8'))
  ensure(Array.isArray(digests) && digests.includes(config.image), 'LOCAL_IMAGE_DIGEST_MISMATCH')
}

export function dockerClient(config, execute = runProcess) {
  return (args, options = {}) => execute(config.dockerExecutable,
    ['--host', `unix://${config.dockerSocket}`, ...args],
    { ...options, env: { ...cleanEnvironment,
      DOCKER_CONFIG: config.clientConfigRoot || path.join(config.workspaceRoot, '.docker-empty') } })
}

export function containerArguments(config, job, identity) {
  return ['run', '--name', identity.name, '--label', `${label}=${identity.nonce}`,
    '--pull=never', '--network=none', '--read-only', '--user=0:0', '--cap-drop=ALL',
    '--security-opt=no-new-privileges', '--memory=4g', '--memory-swap=4g', '--cpus=2', '--pids-limit=256',
    '--log-driver=none', '--tmpfs', '/tmp:rw,noexec,nosuid,nodev,size=256m,mode=1777',
    '--tmpfs', '/work:rw,nosuid,nodev,size=6g,mode=0700', '--workdir=/work',
    '--mount', `type=bind,src=${job.source},dst=/source,readonly`,
    '--mount', `type=bind,src=${job.package},dst=/package.zip,readonly`,
    '--mount', `type=bind,src=${job.control},dst=/control,readonly`,
    '--mount', `type=bind,src=${job.output},dst=/output`,
    '--env', 'HOME=/work/home', '--env', 'CI=true', '--env', 'LANG=C.UTF-8',
    '--entrypoint=/usr/local/bin/node', config.image, '/control/forge-plugin-builder/container-entry.mjs',
    ...(job.force ? ['--force'] : [])]
}

export async function runContainer(config, job, options = {}) {
  const identity = { name: `forge-build-${randomUUID()}`, nonce: randomUUID() }
  if (job.root) {
    await writePrivate(job.root, 'container.json', `${JSON.stringify(identity, null, 2)}\n`)
  }
  const client = dockerClient(config, options.execute)
  let failure = null
  try {
    await client(containerArguments(config, job, identity), { timeoutMs: buildTimeoutMs, signal: options.signal })
  }
  catch (error) {
    failure = error
  }
  // 超时/中断只结束 Docker 客户端，容器可能还在；必须核对本次所有权后终止它。
  try {
    await cleanupContainer(client, identity)
  }
  catch {
    throw new BuildError('CONTAINER_CLEANUP_UNVERIFIED')
  }
  if (failure) {
    throw failure
  }
}

async function cleanupContainer(client, identity) {
  const names = (await client(['ps', '-a', '--filter', `name=^/${identity.name}$`,
    '--format', '{{.Names}}'])).toString('utf8').trim()
  if (!names) {
    return
  }
  ensure(names === identity.name, 'CONTAINER_IDENTITY_MISMATCH')
  const labels = JSON.parse((await client(['inspect', '--type=container', '--format',
    '{{json .Config.Labels}}', identity.name])).toString('utf8'))
  ensure(labels?.[label] === identity.nonce, 'CONTAINER_OWNER_MISMATCH')
  await client(['rm', '--force', identity.name], { timeoutMs: 10000 })
}
