/**
 * @fileoverview 字段组件目录 — 兼容层
 * @description FIELD_COMPONENT_PALETTE_GROUPS 和 FIELD_COMPONENT_DEFAULTS 由 designer-core 桥接生成，
 *   其余辅助（FORM_FIELD_COMPONENT_KEYS、STRUCTURED_VALUE_COMPONENT_KEYS、resolveFieldComponentDefaults）保留本地。
 */
import { toFieldComponentDefaultsMap, toFieldPaletteGroups } from '@/components/lowcode-builder/designer-core'

export const FIELD_COMPONENT_PALETTE_GROUPS = toFieldPaletteGroups()

export const FIELD_COMPONENT_DEFAULTS = Object.freeze({
  ...toFieldComponentDefaultsMap(),
  // 补充注册表未覆盖的历史组件类型
  time: { fieldType: 'TEXT', businessFieldType: 'TEXT', dataType: 'varchar', componentType: 'time', length: 32, precision: null, queryType: 'eq' },
})

export const FORM_FIELD_COMPONENT_KEYS = new Set(Object.keys(FIELD_COMPONENT_DEFAULTS))

export const STRUCTURED_VALUE_COMPONENT_KEYS = new Set([
  'checkbox',
  'transfer',
  'daterange',
  'datetimerange',
  'timerange',
])

export function resolveFieldComponentDefaults(componentKey = '') {
  return FIELD_COMPONENT_DEFAULTS[componentKey] || FIELD_COMPONENT_DEFAULTS.input
}
