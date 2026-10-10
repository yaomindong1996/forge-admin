import fs from 'node:fs/promises'
import path from 'node:path'
import net from 'node:net'
import { project, temporary, write, git, commit, pluginFiles, descriptor, zip }
  from '../../forge-plugin/fixtures/helpers.mjs'
import { sha256 } from '../files.mjs'

export async function fixture(t, metadata = descriptor()) {
  const host = await project(t, true)
  git(host.root, ['init', '-q'])
  commit(host.root)
  const workspace = await temporary(t)
  await fs.chmod(workspace, 0o700)
  const delivery = await temporary(t)
  const data = zip(pluginFiles(metadata))
  await write(delivery, 'plugin.zip', data)
  const config = { sourceRoot: await fs.realpath(host.root), commit: git(host.root, ['rev-parse', 'HEAD']).trim(),
    packageFile: path.join(delivery, 'plugin.zip'), packageSha256: sha256(data), workspaceRoot: workspace,
    dockerExecutable: '/usr/bin/docker', dockerSocket: path.join(workspace, 'docker.sock'),
    image: `forge-builder@sha256:${'a'.repeat(64)}` }
  await write(delivery, 'builder.json', JSON.stringify(config))
  return { host, config, file: path.join(delivery, 'builder.json'), delivery, data }
}

export async function socketFixture(t, file) {
  const server = net.createServer()
  await new Promise((resolve, reject) => { server.once('error', reject); server.listen(file, resolve) })
  t.after(() => new Promise(resolve => server.close(resolve)))
}

export function fakeDocker(config, behavior = {}) {
  const calls = []
  let identity = null
  const execute = async (executable, args, options) => {
    calls.push({ executable, args, options })
    const command = args[2]
    if (command === 'info') {
      return Buffer.from(JSON.stringify({ SecurityOptions: behavior.security || ['name=rootless'],
        CgroupVersion: '2', CgroupDriver: 'systemd', MemoryLimit: true, SwapLimit: true,
        CpuCfsPeriod: true, CpuCfsQuota: true, PidsLimit: true, ...behavior.resources }))
    }
    if (command === 'image') {
      return Buffer.from(JSON.stringify(behavior.digests || [config.image]))
    }
    if (command === 'run') {
      const name = args[args.indexOf('--name') + 1]
      const owner = args[args.indexOf('--label') + 1].split('=')[1]
      identity = { name, owner }
      await behavior.build?.(args)
      return Buffer.alloc(0)
    }
    if (command === 'ps') {
      return Buffer.from(behavior.absent ? '' : identity?.name || '')
    }
    if (command === 'inspect') {
      const owner = behavior.wrongOwner ? 'other' : identity.owner
      return Buffer.from(JSON.stringify({ 'forge.plugin-builder.owner': owner }))
    }
    return Buffer.alloc(0)
  }
  return { execute, calls }
}
