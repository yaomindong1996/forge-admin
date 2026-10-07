import fs from 'node:fs/promises'
import os from 'node:os'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import { deflateRawSync } from 'node:zlib'
import { crc32 } from '../zip.mjs'

export const repository = fileURLToPath(new URL('../../..', import.meta.url))

export async function temporary(t) {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'forge-installer-test-'))
  t.after(() => fs.rm(root, { recursive: true, force: true }))
  return root
}

export async function write(root, relative, text) {
  const target = path.join(root, relative)
  await fs.mkdir(path.dirname(target), { recursive: true })
  await fs.writeFile(target, text)
}

export async function project(t, generated = false) {
  const root = await temporary(t)
  const options = { projectName: 'acme', javaName: 'Acme', displayName: 'Acme Admin',
    basePackage: 'com.acme.app', groupId: 'com.acme.maven', artifactPrefix: 'acme', moduleArtifactPrefix: 'core',
    databaseName: 'acme', modules: ['admin-server'], frontends: ['admin-ui'], forgeVersion: '1.2.0', plugins: [],
    customSetting: { kept: true } }
  const server = generated ? 'acme-server' : 'forge-server'
  const admin = generated ? 'core-admin-server' : 'forge-admin-server'
  const group = generated ? options.groupId : 'com.mdframe.forge'
  const rootPom = `<project><modelVersion>4.0.0</modelVersion><groupId>${group}</groupId>`
    + `<artifactId>${server}</artifactId><version>\u0024{revision}</version>`
    + '<properties><revision>1.2.0</revision></properties><modules>\n'
    + '  <!-- forge-plugins:modules:begin -->\n  <!-- forge-plugins:modules:end -->\n</modules></project>\n'
  const adminPom = '<project><modelVersion>4.0.0</modelVersion><dependencies>\n'
    + '  <!-- forge-plugins:dependencies:begin -->\n  <!-- forge-plugins:dependencies:end -->\n'
    + '</dependencies></project>\n'
  await write(root, `${server}/pom.xml`, rootPom)
  await write(root, `${server}/${admin}/pom.xml`, adminPom)
  await write(root, `${generated ? 'acme' : 'forge'}-admin-ui/src/main.js`, '// fixture\n')
  await write(root, 'scripts/forge-create/module-catalog.json',
    await fs.readFile(path.join(repository, 'scripts/forge-create/module-catalog.json')))
  if (generated) {
    await write(root, 'forge.config.json', JSON.stringify(options, null, 2))
  }
  return { root, server, admin, rootPom, adminPom, options, ui: generated ? 'acme-admin-ui' : 'forge-admin-ui' }
}

export function descriptor(overrides = {}) {
  return { id: 'demo', name: '测试插件', version: '1.0.0', edition: 'community', requiresCore: '>=1.2.0 <2.0.0',
    features: ['community.demo'], server: { module: 'forge-plugin-demo' }, ui: { dir: 'ui' }, ...overrides }
}

export function pluginFiles(metadata = descriptor()) {
  const files = new Map([['forge-plugin.json', Buffer.from(JSON.stringify(metadata))]])
  if (metadata.server) {
    const prefix = `server/${metadata.server.module}`
    const pom = '<project><modelVersion>4.0.0</modelVersion><parent><groupId>com.mdframe.forge</groupId>'
      + '<artifactId>forge-server</artifactId><version>${revision}</version>'
      + '<relativePath>../../pom.xml</relativePath></parent>'
      + `<artifactId>${metadata.server.module}</artifactId></project>\n`
    files.set(`${prefix}/pom.xml`, Buffer.from(pom))
    files.set(`${prefix}/src/main/resources/META-INF/forge-plugin.json`, Buffer.from(JSON.stringify(metadata)))
    files.set(`${prefix}/src/main/java/com/mdframe/forge/plugin/demo/Hello.java`, Buffer.from(
      'package com.mdframe.forge.plugin.demo;\npublic class Hello {}\n'))
  }
  if (metadata.ui) {
    files.set(`${metadata.ui.dir}/index.vue`, Buffer.from('<template><div>Test</div></template>\n'))
  }
  return files
}

export async function plugin(t, metadata = descriptor()) {
  const root = await temporary(t)
  for (const [relative, data] of pluginFiles(metadata)) {
    await write(root, relative, data)
  }
  return root
}

export function zip(files, options = {}) {
  const locals = []
  const centrals = []
  let offset = 0
  for (const [name, value] of files) {
    const data = Buffer.from(value)
    const rawName = Buffer.from(name)
    const packed = options.method === 8 ? deflateRawSync(data) : data
    const local = Buffer.alloc(30)
    local.writeUInt32LE(0x04034b50)
    local.writeUInt16LE(20, 4)
    local.writeUInt16LE(options.flags ?? 0x800, 6)
    local.writeUInt16LE(options.method ?? 0, 8)
    local.writeUInt32LE(crc32(data), 14)
    local.writeUInt32LE(packed.length, 18)
    local.writeUInt32LE(data.length, 22)
    local.writeUInt16LE(rawName.length, 26)
    const central = Buffer.alloc(46)
    central.writeUInt32LE(0x02014b50)
    central.writeUInt16LE(0x0314, 4)
    central.writeUInt16LE(20, 6)
    local.copy(central, 8, 6, 26)
    central.writeUInt16LE(rawName.length, 28)
    central.writeUInt32LE((options.mode ?? 0) * 65536, 38)
    central.writeUInt32LE(offset, 42)
    locals.push(local, rawName, packed)
    centrals.push(central, rawName)
    offset += local.length + rawName.length + packed.length
  }
  const directory = Buffer.concat(centrals)
  const end = Buffer.alloc(22)
  end.writeUInt32LE(0x06054b50)
  end.writeUInt16LE(files.size, 8)
  end.writeUInt16LE(files.size, 10)
  end.writeUInt32LE(directory.length, 12)
  end.writeUInt32LE(offset, 16)
  return Buffer.concat([...locals, directory, end])
}

export function git(root, args) {
  const result = spawnSync('git', ['-C', root, ...args], { encoding: 'utf8' })
  if (result.status !== 0) {
    throw new Error(result.stderr)
  }
  return result.stdout
}

export function commit(root) {
  git(root, ['add', '.'])
  git(root, ['-c', 'user.name=Fixture', '-c', 'user.email=fixture@example.invalid', 'commit', '-qm', 'fixture'])
}

export async function snapshot(root) {
  const result = {}
  const visit = async relative => {
    const entries = await fs.readdir(path.join(root, relative), { withFileTypes: true })
    for (const entry of entries) {
      if (['.forge-plugin', '.git'].includes(entry.name)) {
        continue
      }
      const next = relative ? `${relative}/${entry.name}` : entry.name
      if (entry.isDirectory()) {
        await visit(next)
      }
      else {
        result[next] = entry.isSymbolicLink() ? `link:${await fs.readlink(path.join(root, next))}`
          : (await fs.readFile(path.join(root, next))).toString('base64')
      }
    }
  }
  await visit('')
  return result
}
