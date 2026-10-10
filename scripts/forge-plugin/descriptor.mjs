import { parseStrictJson } from './json.mjs'
import { parseVersion, satisfiesVersion } from '../forge-shared/version.mjs'
import { requireCondition, validateRelative } from './paths.mjs'
import { decodeUtf8 } from './content.mjs'

export const idPattern = /^[a-z][a-z0-9-]{1,31}$/
export const modulePattern = /^[a-z][a-z0-9-]{1,63}$/

export function validateDescriptor(value, coreVersion) {
  objectKeys(value, ['id', 'name', 'version', 'edition', 'requiresCore', 'features', 'server', 'ui'])
  requireCondition(typeof value.id === 'string' && idPattern.test(value.id), '插件 id 非法')
  requireCondition(typeof value.name === 'string' && value.name.trim() && value.name.length <= 128, '插件名称非法')
  parseVersion(value.version)
  requireCondition(['community', 'ee'].includes(value.edition), '插件 edition 必须是 community/ee')
  requireCondition(typeof value.requiresCore === 'string' && satisfiesVersion(coreVersion, value.requiresCore),
    `插件 ${value.id} 要求 ${value.requiresCore}，当前核心 ${coreVersion}：版本不兼容`)
  requireCondition(Array.isArray(value.features) && value.features.length <= 128, '插件 features 非法')
  for (const feature of value.features) {
    requireCondition(typeof feature === 'string' && feature.length <= 64
      && /^[a-z][a-z0-9]*(?:[._-][a-z0-9]+)*$/.test(feature), '插件 feature 非法')
    requireCondition((value.edition === 'ee') === feature.startsWith('ee.'), '功能前缀与 edition 不一致')
  }
  requireCondition(new Set(value.features).size === value.features.length, '插件 feature 重复')
  if (value.server != null) {
    objectKeys(value.server, ['module'])
    requireCondition(typeof value.server.module === 'string' && modulePattern.test(value.server.module),
      '插件 Maven module 非法')
  }
  if (value.ui != null) {
    objectKeys(value.ui, ['dir'])
    requireCondition(typeof value.ui.dir === 'string' && /^[A-Za-z0-9][A-Za-z0-9_./-]*$/.test(value.ui.dir),
      '插件 UI 目录非法')
    validateRelative(value.ui.dir)
  }
  requireCondition(value.server || value.ui, '插件至少需要一个后端或 UI 组件')
  return value
}

export function readDescriptor(files, coreVersion) {
  const raw = files.get('forge-plugin.json')
  requireCondition(raw, '插件包根目录缺少 forge-plugin.json')
  const descriptor = validateDescriptor(parseStrictJson(decodeUtf8(raw)), coreVersion)
  if (descriptor.server) {
    const runtime = files.get(`server/${descriptor.server.module}/src/main/resources/META-INF/forge-plugin.json`)
    requireCondition(runtime, '后端插件缺少运行时描述文件')
    const value = validateDescriptor(parseStrictJson(decodeUtf8(runtime)), coreVersion)
    requireCondition(canonical(value) === canonical(descriptor), '根描述文件与运行时描述文件不一致')
  }
  return descriptor
}

function canonical(value) {
  if (Array.isArray(value)) {
    return JSON.stringify(value.map(item => JSON.parse(canonical(item))))
  }
  if (value && typeof value === 'object') {
    const entries = Object.keys(value).sort().map(key => [key, JSON.parse(canonical(value[key]))])
    return JSON.stringify(Object.fromEntries(entries))
  }
  return JSON.stringify(value)
}

export function objectKeys(value, allowed) {
  requireCondition(value && typeof value === 'object' && !Array.isArray(value), '描述字段必须是对象')
  requireCondition(Object.keys(value).every(key => allowed.includes(key)), '描述文件包含未知字段')
}
