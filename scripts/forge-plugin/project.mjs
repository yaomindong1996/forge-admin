import fs from 'node:fs/promises'
import path from 'node:path'
import { buildArtifactMap } from '../forge-shared/rename.mjs'
import { readPomVersion } from '../forge-shared/version.mjs'
import { parseStrictJson } from './json.mjs'
import { validateDescriptor, idPattern } from './descriptor.mjs'
import { parsePom, field, child } from './xml.mjs'
import { requireCondition, safeTarget, statOptional } from './paths.mjs'

const prefix = /^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$/
const javaPackage = /^[a-zA-Z_]\w*(?:\.[a-zA-Z_]\w*)+$/

export async function loadProject(root) {
  const configPath = await safeTarget(root, 'forge.config.json')
  const configText = await statOptional(configPath) ? await fs.readFile(configPath, 'utf8') : null
  const config = configText === null ? null : parseStrictJson(configText, 1024 * 1024)
  requireCondition(configText === null || (config && typeof config === 'object' && !Array.isArray(config)),
    '现有 forge.config.json 必须是有效工程对象，不能按缺失配置覆盖')
  const template = !config || config.projectType === 'template'
  requireCondition(!config || !config.projectType || template, '未知 projectType')
  const options = template ? templateOptions() : generatedOptions(config)
  const catalogFile = await safeTarget(root, 'scripts/forge-create/module-catalog.json')
  const catalog = parseStrictJson(await fs.readFile(catalogFile, 'utf8'), 1024 * 1024)
  const artifactMap = buildArtifactMap(catalog, options)
  const server = `${options.artifactPrefix}-server`
  const rootPom = `${server}/pom.xml`
  const adminPom = `${server}/${artifactMap['forge-admin-server']}/pom.xml`
  const ui = `${options.projectName}-admin-ui`
  const files = { [rootPom]: await readHost(root, rootPom), [adminPom]: await readHost(root, adminPom),
    'forge.config.json': configText }
  requireCondition((await fs.lstat(await safeTarget(root, ui))).isDirectory(), '工程缺少 Admin UI')
  const pom = parsePom(files[rootPom])
  requireCondition(field(pom, 'groupId') === options.groupId && field(pom, 'artifactId') === server,
    '配置与根 POM 坐标不一致')
  const coreVersion = await readPomVersion(path.join(root, rootPom))
  const declaredVersion = field(child(pom, 'properties'), 'revision')
  requireCondition(declaredVersion === coreVersion && field(pom, 'version') === '${revision}',
    '根 POM 必须使用直属 revision 属性及 CI-friendly 版本')
  requireCondition(!config || config.forgeVersion === coreVersion, 'forgeVersion 与根 POM revision 不一致')
  const state = config || { projectType: 'template', forgeVersion: coreVersion, plugins: [] }
  requireCondition(state && typeof state === 'object' && Array.isArray(state.plugins), '工程缺少 plugins 数组')
  const context = { root, template, options, catalog, artifactMap, server, rootPom, adminPom, ui,
    coreVersion, config: state, files, selection: { frontendIds: new Set(config?.frontends || ['admin-ui']) } }
  validateRecords(context)
  return context
}

function templateOptions() {
  return { projectName: 'forge', javaName: 'Forge', displayName: 'Forge Admin', basePackage: 'com.mdframe.forge',
    groupId: 'com.mdframe.forge', artifactPrefix: 'forge', moduleArtifactPrefix: 'forge', databaseName: 'forge' }
}

function generatedOptions(config) {
  requireCondition(config && typeof config === 'object', '工程配置非法')
  for (const key of ['projectName', 'artifactPrefix', 'moduleArtifactPrefix']) {
    requireCondition(typeof config[key] === 'string' && prefix.test(config[key]) && config[key].length <= 64,
      `工程 ${key} 非法`)
  }
  for (const key of ['basePackage', 'groupId']) {
    requireCondition(typeof config[key] === 'string' && javaPackage.test(config[key]) && config[key].length <= 128,
      `工程 ${key} 非法`)
  }
  requireCondition(typeof config.javaName === 'string' && /^[A-Z][A-Za-z0-9]*$/.test(config.javaName), 'javaName 非法')
  requireCondition(typeof config.displayName === 'string' && typeof config.databaseName === 'string', '工程名称缺失')
  requireCondition(Array.isArray(config.modules) && config.modules.includes('admin-server')
    && Array.isArray(config.frontends) && config.frontends.includes('admin-ui'), '安装器仅支持包含 Admin 的工程')
  return config
}

async function readHost(root, relative) {
  const target = await safeTarget(root, relative)
  requireCondition((await fs.lstat(target)).isFile(), `宿主文件不是普通文件：${relative}`)
  return fs.readFile(target, 'utf8')
}

function validateRecords(context) {
  requireCondition(context.config.plugins.length <= 256, '插件登记数量超过限制')
  const ids = new Set()
  const modules = new Set()
  for (const record of context.config.plugins) {
    const { mode, source, checksums, ...descriptor } = record
    validateDescriptor(descriptor, context.coreVersion)
    requireCondition(['copy', 'dev'].includes(mode) && typeof source === 'string' && path.isAbsolute(source),
      '插件来源登记非法')
    requireCondition(mode !== 'dev' || context.template, '--dev 仅支持模板仓库')
    requireCondition(!ids.has(record.id), '插件登记 ID 重复')
    ids.add(record.id)
    const module = finalModule(context, record)
    requireCondition(!module || !modules.has(module), '插件登记 module 重复')
    modules.add(module)
    requireCondition(checksums && typeof checksums === 'object', '插件登记缺少安装摘要')
    for (const value of Object.values(checksums)) {
      requireCondition(typeof value === 'string' && /^[a-f0-9]{64}$/.test(value), '插件摘要非法')
    }
  }
}

export function finalModule(context, descriptor) {
  return descriptor.server?.module.replace(/^forge-/, `${context.options.moduleArtifactPrefix}-`)
}

export function targetsFor(context, descriptor) {
  requireCondition(idPattern.test(descriptor.id), '插件 ID 非法')
  const targets = {}
  if (descriptor.server) {
    targets.server = `${context.server}/plugins/${finalModule(context, descriptor)}`
  }
  if (descriptor.ui) {
    targets.ui = `${context.ui}/src/views/plugins/${descriptor.id}`
  }
  return targets
}
