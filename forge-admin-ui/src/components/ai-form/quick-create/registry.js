import {
  createDictDataQuickCreatePreset,
  isPlatformDictQuickCreateBlocked,
  isPlatformDictType,
  resolveDictTypeFromContext,
} from './presets/dictData'
import { createSystemClientQuickCreatePreset } from './presets/systemClient'
import { createSystemOrgQuickCreatePreset } from './presets/systemOrg'
import { createSystemPostQuickCreatePreset } from './presets/systemPost'
import { createSystemRoleQuickCreatePreset } from './presets/systemRole'

const presetFactories = {
  systemRole: createSystemRoleQuickCreatePreset,
  systemPost: createSystemPostQuickCreatePreset,
  systemOrg: createSystemOrgQuickCreatePreset,
  systemClient: createSystemClientQuickCreatePreset,
  dictData: createDictDataQuickCreatePreset,
}

const presetCache = new Map()

function getPresetInstance(key) {
  if (!presetFactories[key])
    return null
  if (!presetCache.has(key))
    presetCache.set(key, presetFactories[key]())
  return presetCache.get(key)
}

export function listQuickCreatePresetKeys() {
  return Object.keys(presetFactories)
}

export { isPlatformDictType, resolveDictTypeFromContext }

/**
 * 合并字段级 quickCreate 配置与预设定义。
 *
 * dictData 示例（业务字典 dictSelect 字段）：
 * ```js
 * {
 *   field: 'customerLevel',
 *   type: 'dictSelect',
 *   dictType: 'crm_customer_level',
 *   quickCreate: { preset: 'dictData' }, // dictType 默认取字段 dictType
 * }
 * ```
 * 平台字典默认勿配 quickCreate；白名单见 dictData.PLATFORM_DICT_DATA_QUICK_CREATE_ALLOWLIST。
 */
export function resolveQuickCreateDefinition(field = {}) {
  const raw = field?.quickCreate
  if (!raw)
    return null
  const overrides = typeof raw === 'string' ? { preset: raw } : { ...raw }
  const presetKey = overrides.preset
  if (!presetKey)
    return null
  const preset = getPresetInstance(presetKey)
  if (!preset)
    return null
  const dictType = presetKey === 'dictData'
    ? (overrides.dictType || field.dictType || field.props?.dictType || '')
    : overrides.dictType
  // 平台字典未入白名单时直接不解析定义，入口不渲染
  if (presetKey === 'dictData' && isPlatformDictQuickCreateBlocked(dictType, overrides))
    return null
  const permission = overrides.permission || preset.permission
  if (!permission)
    return null
  return {
    ...preset,
    ...overrides,
    preset: presetKey,
    dictType,
    permission,
    linkLabel: overrides.linkLabel || preset.linkLabel,
    modalTitle: overrides.modalTitle || preset.modalTitle,
  }
}
