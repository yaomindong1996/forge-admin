/** 表单设计器主面板可配置多选的选择类组件 */
export const MULTI_SELECT_COMPONENT_KEYS = [
  'select',
  'dictSelect',
  'userSelect',
  'orgTreeSelect',
  'treeSelect',
  'objectReference',
  'recordSelector',
]

const MULTI_SELECT_COMPONENT_SET = new Set(MULTI_SELECT_COMPONENT_KEYS)

export function supportsMultipleSelect(componentKey = '') {
  return MULTI_SELECT_COMPONENT_SET.has(String(componentKey || '').trim())
}

export function isFieldMultiple(field = {}) {
  if (!field || typeof field !== 'object')
    return false
  const componentKey = field.type || field.componentType || field.componentKey || ''
  if (!supportsMultipleSelect(componentKey))
    return false
  return field.multiple === true
    || field.props?.multiple === true
    || field.basicProps?.multiple === true
    || field.recordSelector?.multiple === true
    || field.props?.recordSelector?.multiple === true
    || field.basicProps?.recordSelector?.multiple === true
}

export function parseSelectionValues(rawValue, multiple = false) {
  if (!multiple) {
    if (Array.isArray(rawValue))
      return rawValue.length ? rawValue[0] : null
    if (rawValue === undefined)
      return null
    return rawValue
  }
  if (rawValue === null || rawValue === undefined || rawValue === '')
    return []
  if (Array.isArray(rawValue))
    return rawValue.map(normalizeStoredItem).filter(isFilledValue)
  return String(rawValue)
    .split(',')
    .map(item => item.trim())
    .filter(Boolean)
}

export function serializeSelectionValues(rawValue, multiple = false) {
  if (!multiple) {
    if (Array.isArray(rawValue))
      return rawValue.length ? rawValue[0] : null
    if (rawValue === undefined || rawValue === '')
      return null
    return rawValue
  }
  return parseSelectionValues(rawValue, true)
    .map(item => String(item ?? '').trim())
    .filter(Boolean)
    .join(',')
}

export function serializeSelectionLabels(rawValue) {
  if (Array.isArray(rawValue))
    return rawValue.map(item => String(item ?? '').trim()).filter(Boolean).join(',')
  if (rawValue === null || rawValue === undefined)
    return ''
  return String(rawValue).trim()
}

function normalizeStoredItem(value) {
  if (value === null || value === undefined)
    return ''
  return typeof value === 'string' ? value.trim() : value
}

function isFilledValue(value) {
  return value !== null && value !== undefined && String(value).trim() !== ''
}
