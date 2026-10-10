import fs from 'node:fs/promises'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import { readInstalledDirectory, fingerprint } from './bundle.mjs'
import { parseStrictJson } from './json.mjs'
import { targetsFor } from './project.mjs'
import { requireCondition, safeTarget, statOptional } from './paths.mjs'

export async function checkOwned(context, record) {
  const targets = targetsFor(context, record)
  const checks = []
  for (const [kind, relative] of Object.entries(targets)) {
    const target = await safeTarget(context.root, relative, record.mode === 'dev')
    const stat = await statOptional(target)
    requireCondition(stat, `已登记插件目录缺失：${relative}`)
    if (record.mode === 'dev') {
      if (kind === 'server') {
        const digest = await devWrapperDigest(target, record)
        requireCondition(digest === record.checksums.server, '开发接入 POM/链接已修改，请人工检查')
        checks.push({ relative, digest, devRecord: record })
        continue
      }
      requireCondition(stat.isSymbolicLink() && path.resolve(path.dirname(target), await fs.readlink(target))
        === sourceComponent(record, kind), '开发链接目标与登记不一致')
      checks.push({ relative, link: await fs.readlink(target) })
      continue
    }
    requireCondition(stat.isDirectory(), '插件目录不是普通目录')
    const files = await readInstalledDirectory(target)
    const stamp = files.get('.forge-plugin-owned.json')
    requireCondition(stamp, '插件目录缺少安装所有权标记')
    const owner = parseStrictJson(stamp.toString('utf8'))
    requireCondition(owner.id === record.id && owner.version === record.version, '插件所有权标记与登记不一致')
    const digest = fingerprint(files)
    const status = gitStatus(context.root, relative)
    requireCondition(!status.dirty, `插件有未提交修改，请先提交再覆盖/卸载：${relative}`)
    requireCondition((status.tracked && !status.ignored) || digest === record.checksums[kind],
      `插件本地源码已修改：${relative}`)
    checks.push({ relative, digest })
  }
  return checks
}

export async function devWrapperDigest(target, record) {
  requireCondition((await fs.lstat(target)).isDirectory(), '开发后端接入目录不是普通目录')
  const ownerFile = await safeTarget(target, '.forge-plugin-owned.json')
  const pomFile = await safeTarget(target, 'pom.xml')
  const stamp = await fs.readFile(ownerFile)
  const owner = parseStrictJson(stamp.toString('utf8'))
  requireCondition(owner.id === record.id && owner.version === record.version && owner.mode === 'dev'
    && Array.isArray(owner.links) && new Set(owner.links).size === owner.links.length, '开发接入所有权标记非法')
  const children = await fs.readdir(target)
  const outputs = ['target', '.DS_Store', '.flattened-pom.xml']
  const entries = children.filter(name => !outputs.includes(name))
  for (const name of children.filter(value => outputs.includes(value))) {
    const output = await safeTarget(target, name)
    const stat = await fs.lstat(output)
    requireCondition(name === 'target' ? stat.isDirectory() : stat.isFile(), '开发构建输出类型非法')
  }
  requireCondition(entries.length === owner.links.length + 2
    && entries.every(name => ['pom.xml', '.forge-plugin-owned.json', ...owner.links].includes(name)),
    '开发接入目录包含未登记文件')
  for (const name of owner.links) {
    requireCondition(typeof name === 'string' && !name.includes('/'), '开发链接名称非法')
    const link = await safeTarget(target, name, true)
    requireCondition((await fs.lstat(link)).isSymbolicLink()
      && path.resolve(target, await fs.readlink(link)) === path.join(sourceComponent(record, 'server'), name),
    '开发源码链接与登记不一致')
  }
  return fingerprint(new Map([['pom.xml', await fs.readFile(pomFile)], ['.forge-plugin-owned.json', stamp]]))
}

export function sourceComponent(record, kind) {
  return path.join(record.source, kind === 'server' ? `server/${record.server.module}` : record.ui.dir)
}

function gitStatus(root, relative) {
  const probe = spawnSync('git', ['-C', root, 'rev-parse', '--is-inside-work-tree'], { encoding: 'utf8' })
  if (probe.status !== 0) {
    return { tracked: false, dirty: false, ignored: false }
  }
  const result = spawnSync('git', ['-C', root, 'status', '--porcelain=v1', '-z', '--untracked-files=all',
    '--ignored=matching', '--', relative], { encoding: 'utf8', maxBuffer: 4 * 1024 * 1024 })
  requireCondition(result.status === 0, 'Git 改动检查失败，拒绝覆盖')
  const tracked = spawnSync('git', ['-C', root, 'ls-files', '-z', '--', relative], { encoding: 'utf8' })
  requireCondition(tracked.status === 0, 'Git 跟踪检查失败')
  const entries = result.stdout.split('\0').filter(Boolean)
  return { tracked: !!tracked.stdout, dirty: entries.some(entry => !entry.startsWith('!! ')),
    ignored: entries.some(entry => entry.startsWith('!! ')) }
}

export async function verifyChecks(root, checks) {
  for (const check of checks) {
    const target = await safeTarget(root, check.relative, check.link !== undefined)
    if (check.link !== undefined) {
      requireCondition(await fs.readlink(target) === check.link, '插件链接在预检后发生变化')
    }
    else {
      const digest = check.devRecord ? await devWrapperDigest(target, check.devRecord)
        : fingerprint(await readInstalledDirectory(target))
      requireCondition(digest === check.digest, '插件在预检后发生变化')
    }
  }
}
