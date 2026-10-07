import fs from 'node:fs/promises'
import path from 'node:path'
import { isDeepStrictEqual } from 'node:util'
import { readBundle, readInstalledDirectory } from '../../scripts/forge-plugin/bundle.mjs'
import { readDescriptor } from '../../scripts/forge-plugin/descriptor.mjs'
import { parseStrictJson } from '../../scripts/forge-plugin/json.mjs'
import { safeTarget } from '../../scripts/forge-plugin/paths.mjs'
import { loadProject, targetsFor } from '../../scripts/forge-plugin/project.mjs'

export async function readUiPluginManifest(root) {
  const context = await loadProject(root)
  const plugins = []
  for (const record of context.config.plugins.filter(item => item.ui)) {
    const relative = targetsFor(context, record).ui
    const directory = await safeTarget(root, relative, record.mode === 'dev')
    const files = await readInstalledDirectory(directory)
    if (record.mode === 'dev') {
      await validateDevSource(directory, record, context.coreVersion)
    }
    else {
      const raw = files.get('.forge-plugin-owned.json')
      const owner = raw && parseStrictJson(raw.toString('utf8'))
      if (!owner || owner.id !== record.id || owner.version !== record.version)
        throw new Error(`UI 插件 ${record.id} 的构建组件/所有权与登记不一致`)
    }
    plugins.push({ id: record.id, name: record.name, version: record.version, edition: record.edition })
  }
  return { coreVersion: context.coreVersion, builtAt: new Date().toISOString(), plugins }
}

async function validateDevSource(directory, record, coreVersion) {
  if (await fs.realpath(directory) !== await fs.realpath(path.join(record.source, record.ui.dir)))
    throw new Error(`开发模式 UI 插件 ${record.id} 的链接目标不一致`)
  const bundle = await readBundle(record.source)
  const descriptor = readDescriptor(bundle.files, coreVersion)
  const { mode, source, checksums, ...declared } = record
  if (!isDeepStrictEqual(descriptor, declared))
    throw new Error(`开发模式 UI 插件 ${record.id} 的源描述已变化，请重新安装`)
}

/** 仅将构建声明写进当前 UI，不暴露 source、包路径、客户配置或源码摘要。 */
export function pluginUiManifest() {
  const virtual = 'virtual:forge-ui-plugin-manifest'
  let root
  return {
    name: 'forge-ui-plugin-manifest',
    configResolved(config) {
      root = path.resolve(config.root, '..')
    },
    resolveId(id) {
      return id === virtual ? `\0${virtual}` : null
    },
    async load(id) {
      if (id !== `\0${virtual}`)
        return null
      return `export default ${JSON.stringify(await readUiPluginManifest(root))}`
    },
  }
}
