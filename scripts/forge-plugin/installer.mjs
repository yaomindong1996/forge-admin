import path from 'node:path'
import { readBundle } from './bundle.mjs'
import { readDescriptor, idPattern } from './descriptor.mjs'
import { loadProject, targetsFor, finalModule } from './project.mjs'
import { checkOwned } from './ownership.mjs'
import { preparePoms } from './pom.mjs'
import { stageComponents, validateComponents } from './stage.mjs'
import { transact } from './transaction.mjs'
import { requireCondition, safeTarget, statOptional } from './paths.mjs'

export async function addPlugin(root, source, flags = {}, hooks = {}) {
  const prepared = await prepareInstall(root, source, flags)
  const { context, descriptor, components, old, checks } = prepared
  const plan = { id: descriptor.id, checks }
  return transact(context, plan, async work => {
    const staged = await stageComponents(work, context, { descriptor, components, source, dev: flags.dev })
    const records = replaceRecord(context.config.plugins, staged.record)
    const operations = mergeOperations(context, staged.operations, old)
    return { operations, files: hostFiles(context, records), result: { record: staged.record } }
  }, hooks)
}

// 预检和安装复用同一入口，不以「描述合法」冒充宿主目录/所有权检查通过。
export async function inspectPluginInstall(root, source, flags = {}) {
  const { context, descriptor, old, checks } = await prepareInstall(root, source, flags)
  return { pluginId: descriptor.id, version: descriptor.version, coreVersion: context.coreVersion,
    edition: descriptor.edition, operation: old ? 'replace' : 'install', previousVersion: old?.version || null,
    targets: targetsFor(context, descriptor),
    hostFiles: [context.rootPom, context.adminPom, 'forge.config.json'],
    checks: checks.map(({ relative, digest }) => ({ relative, digest })) }
}

async function prepareInstall(root, source, flags) {
  const context = await loadProject(root)
  requireCondition(!flags.dev || context.template, '--dev 仅支持模板仓库')
  const bundle = await readBundle(source)
  requireCondition(!flags.dev || bundle.directory, '--dev 仅支持目录来源')
  const descriptor = readDescriptor(bundle.files, context.coreVersion)
  const old = context.config.plugins.find(item => item.id === descriptor.id)
  requireCondition(!old || flags.force, '插件已安装，整包覆盖需显式 --force')
  const components = validateComponents(context, bundle, descriptor)
  const checks = old ? await checkOwned(context, old) : []
  await validateTargets(context, descriptor, old, source)
  preparePoms(context, replaceRecord(context.config.plugins, descriptor))
  return { context, descriptor, components, old, checks }
}

export async function removePlugin(root, id, hooks = {}) {
  requireCondition(idPattern.test(id), '插件 ID 非法')
  const context = await loadProject(root)
  const record = context.config.plugins.find(item => item.id === id)
  requireCondition(record, '插件未安装')
  const checks = await checkOwned(context, record)
  const records = context.config.plugins.filter(item => item.id !== id)
  const files = hostFiles(context, records)
  const operations = Object.values(targetsFor(context, record)).map(relative => ({ relative,
    replace: true, allowLink: record.mode === 'dev' }))
  return transact(context, { id, checks }, async () => ({ operations, files, result: { record } }), hooks)
}

export async function listPlugins(root) {
  const context = await loadProject(root)
  preparePoms(context, context.config.plugins)
  return context.config.plugins
}

function hostFiles(context, records) {
  return { ...preparePoms(context, records),
    'forge.config.json': `${JSON.stringify({ ...context.config, plugins: records }, null, 2)}\n` }
}

function replaceRecord(records, record) {
  return [...records.filter(item => item.id !== record.id), record].sort((a, b) => a.id.localeCompare(b.id))
}

async function validateTargets(context, descriptor, old, source) {
  const targets = targetsFor(context, descriptor)
  const oldTargets = new Set(Object.values(old ? targetsFor(context, old) : {}))
  const otherModules = context.config.plugins.filter(item => item.id !== descriptor.id)
    .map(item => finalModule(context, item))
  requireCondition(!descriptor.server || !otherModules.includes(finalModule(context, descriptor)), '插件模块已被其他插件使用')
  for (const relative of Object.values(targets)) {
    const target = await safeTarget(context.root, relative, old?.mode === 'dev' && oldTargets.has(relative))
    requireCondition(oldTargets.has(relative) || !await statOptional(target), `目标目录已占用：${relative}`)
    const relationship = path.relative(target, source)
    requireCondition(relationship && relationship.startsWith(`..${path.sep}`), '插件来源不能位于安装目标内或包含目标')
    const inverse = path.relative(source, target)
    requireCondition(inverse && inverse.startsWith(`..${path.sep}`), '插件来源不能位于安装目标内或包含目标')
  }
}

function mergeOperations(context, incoming, old) {
  const prior = old ? Object.values(targetsFor(context, old)) : []
  const fresh = new Set(incoming.map(item => item.relative))
  const removed = prior.filter(relative => !fresh.has(relative)).map(relative => ({ relative }))
  return [...incoming, ...removed].map(item => ({ ...item, replace: prior.includes(item.relative),
    allowLink: old?.mode === 'dev' && prior.includes(item.relative) }))
}
