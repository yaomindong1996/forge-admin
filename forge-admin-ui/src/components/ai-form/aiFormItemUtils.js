/** Pure helpers extracted from AiFormItem. */

import { resolveSwitchValuePair } from '@/views/app-center/components/designer/forge-form-designer/field-default-value'
import { isInputLikeFieldType, isNumberLikeField } from './field-type-utils'
import { buildTreeFromFlatRows, decorateLazyTreeNodes, resolveFieldCascadeConfig, resolveFirstFilledOptionField, resolveOptionLoadMode, rowsHaveNestedChildren, shouldBuildTreeOptions } from './option-source-runtime'
import { resolveSelectionLabelFields as buildSelectionLabelFields, ORG_SELECT_FIELD_TYPES, USER_SELECT_FIELD_TYPES } from './selection-label-fields'
import { serializeSelectionLabels } from './selection-multi-value'

const ORG_TREE_SELECT_TYPES = ORG_SELECT_FIELD_TYPES
const USER_SELECT_TYPES = USER_SELECT_FIELD_TYPES

export function resolveSwitchCheckedValue(field = {}) {
  return resolveSwitchValuePair(field).checkedValue
}

export function resolveSwitchUncheckedValue(field = {}) {
  return resolveSwitchValuePair(field).uncheckedValue
}

/**
 * 将设计器 __designerStyle 展平为可挂到 DOM 的 inline style。
 * 编辑态画布与预览/发布态必须共用此映射，否则字号/颜色/边距只在设计器可见。
 */
export function resolveDesignerInlineStyle(source = {}) {
  const designerStyle = source?.props?.__designerStyle
    || source?.__designerStyle
    || (source?.customStyle ? source : null)
  if (!designerStyle || typeof designerStyle !== 'object')
    return {}
  const style = {
    ...(designerStyle.customStyle && typeof designerStyle.customStyle === 'object'
      ? designerStyle.customStyle
      : {}),
  }
  ;[
    'width',
    'height',
    'minHeight',
    'backgroundColor',
    'borderColor',
    'borderStyle',
    'borderWidth',
    'borderRadius',
    'boxShadow',
    'opacity',
  ].forEach((key) => {
    if (designerStyle[key] !== undefined && designerStyle[key] !== null && designerStyle[key] !== '')
      style[key] = designerStyle[key]
  })
  return style
}

export function getPlaceholder(field = {}) {
  // 设计器/运行态常把文案放在 props.placeholder；仅看顶层会回退成「请输入xxx」
  const explicit = String(field.placeholder || field.props?.placeholder || '').trim()
  if (explicit)
    return explicit

  const generation = field.props?.generation || field.generation
  const generationEnabled = generation && typeof generation === 'object'
    && generation.enabled !== false
    && Boolean(generation.ruleCode || generation.codeRuleCode)
  // 自动编号新增态为空；勿用「请输入」误导为坏了
  if (generationEnabled)
    return '保存后自动生成'

  const prefix = (isInputLikeFieldType(field.type) || isNumberLikeField(field)) ? '请输入' : '请选择'
  return `${prefix}${field.label || ''}`
}

/** 读取字段前后缀文案（支持顶层与 props） */
export function resolveFieldAffix(field = {}, key = 'prefix') {
  const raw = field?.[key] ?? field?.props?.[key]
  return String(raw ?? '').trim()
}

/** 读取设计器配置的图标 key（IconSelector / IconRenderer 同源） */
export function resolveFieldIcon(field = {}, key = '') {
  if (!key)
    return ''
  const raw = field?.[key] ?? field?.props?.[key]
  return String(raw ?? '').trim()
}

/** 读取下拉菜单等文本插槽配置 */
export function resolveFieldSlotText(field = {}, key = '') {
  return resolveFieldAffix(field, key)
}

/** 单行输入 type：password 时走密码框（开启密码显隐图标槽） */
export function resolveInputFieldType(field = {}) {
  const raw = String(field?.inputType ?? field?.props?.inputType ?? '').trim().toLowerCase()
  if (raw === 'password')
    return 'password'
  return 'text'
}

/**
 * 数字/金额输入前缀：优先 props.prefix；金额字段可回退 currencySymbol（默认 ¥）。
 */
export function resolveNumberFieldPrefix(field = {}, resolveNumberFieldType = () => '') {
  const explicit = resolveFieldAffix(field, 'prefix')
  if (explicit)
    return explicit
  if (typeof resolveNumberFieldType === 'function' && resolveNumberFieldType(field) === 'money') {
    const symbol = String(field?.currencySymbol ?? field?.props?.currencySymbol ?? '¥').trim()
    return symbol || '¥'
  }
  return ''
}

export function resolveNumberFieldSuffix(field = {}) {
  return resolveFieldAffix(field, 'suffix')
}

export function cacheAsyncOptions(field, promise) {
  promise.then((options) => {
    field._cachedOptions = options
  })
}

export function hasEffectiveOptionSource(source) {
  if (!source)
    return false
  if (typeof source === 'string')
    return source.trim() !== ''
  if (typeof source !== 'object')
    return false
  if (['CURRENT_CHILDREN', 'current_children', 'currentChildren'].includes(String(source.type || '')))
    return true
  const type = String(source.type || '').toUpperCase()
  const sourceType = String(source.sourceType || '').toUpperCase()
  // QUERY_SOURCE / BUSINESS_OBJECT 都以 sourceKey 为准；兼容缺 type、只留 sourceType 的旧快照
  if (type === 'QUERY_SOURCE' || sourceType === 'BUSINESS_OBJECT' || sourceType === 'DATASET' || sourceType === 'EXTERNAL_API')
    return Boolean(String(source.sourceKey || '').trim())
  return Boolean(
    String(source.api || source.url || '').trim()
    || String(source.sourceKey || '').trim()
    || Array.isArray(source.options)
    || Array.isArray(source.data),
  )
}

export function buildCurrentChildrenOptions(source = {}, context = {}) {
  const relationKey = String(source.relationKey || source.childKey || '').trim()
  const collections = context?.childCollections && typeof context.childCollections === 'object'
    ? context.childCollections
    : {}
  const rows = relationKey && Array.isArray(collections[relationKey]) ? collections[relationKey] : []
  const valueField = source.valueField || 'id'
  const labelField = source.labelField || 'label'
  const disabledField = source.disabledField || ''
  const seen = new Set()
  return rows
    .filter(row => row && (!source.persistedOnly || hasPersistedOptionId(row)))
    .map((row) => {
      const value = row[valueField]
      if (value === null || value === undefined || value === '' || seen.has(String(value)))
        return null
      seen.add(String(value))
      const label = row[labelField] ?? row.name ?? row.title ?? value
      return {
        ...row,
        value,
        key: row.key ?? value,
        label: String(label),
        ...(disabledField ? { disabled: row[disabledField] === true } : {}),
      }
    })
    .filter(Boolean)
}

export function hasPersistedOptionId(row = {}) {
  const value = row.id ?? row.ID ?? row.recordId
  return value !== null && value !== undefined && String(value).trim() !== ''
}

export function safeParseObject(value = '') {
  const text = String(value || '').trim()
  if (!text)
    return {}
  try {
    const parsed = JSON.parse(text)
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  }
  catch {
    return text.split(/[&\n]/).reduce((result, item) => {
      const content = item.trim()
      if (!content)
        return result
      const separator = content.includes('=') ? '=' : content.includes(':') ? ':' : ''
      if (!separator)
        return result
      const index = content.indexOf(separator)
      const key = content.slice(0, index).trim()
      const itemValue = content.slice(index + 1).trim()
      if (key && itemValue)
        result[key] = itemValue
      return result
    }, {})
  }
}

export function applyTreeLoadParams(params, source = {}, { isTree = false, loadMode = 'full', parentValue } = {}) {
  if (!params || !isTree)
    return
  params.loadMode = loadMode
  if (loadMode !== 'lazy')
    return
  const parentField = String(source.parentField || 'parentId').trim() || 'parentId'
  const effectiveParent = parentValue !== undefined && parentValue !== null
    ? parentValue
    : (source.rootParentValue ?? '')
  params.parentValue = effectiveParent
  params.parentId = effectiveParent
  // 查询源 / 业务对象分页：按父级字段过滤一层；/tree 接口同时吃 parentValue
  if (!(parentField in params) || params[parentField] === undefined || params[parentField] === null)
    params[parentField] = effectiveParent
}

export function isSelectorQueryApi(api = '') {
  return String(api || '').includes('selector/query')
}

export function parseOptionApi(api) {
  const text = String(api || '')
  const [method, ...urlParts] = text.includes('@') ? text.split('@') : ['get', text]
  return {
    method: String(method || 'get').toLowerCase(),
    url: urlParts.join('@') || text,
  }
}

export function finalizeOptionTree(options = [], source = {}, componentType = '') {
  if (!shouldBuildTreeOptions(source, componentType))
    return Array.isArray(options) ? options : []
  const childrenField = source.childrenField || 'children'
  let next = Array.isArray(options) ? options : []
  if (!rowsHaveNestedChildren(next, childrenField) && String(source.parentField || '').trim()) {
    next = buildTreeFromFlatRows(next, {
      valueField: source.valueField || source.keyField || 'id',
      parentField: source.parentField,
      rootParentValue: source.rootParentValue,
    })
  }
  if (resolveOptionLoadMode(source) === 'lazy')
    next = decorateLazyTreeNodes(next)
  return next
}

export function extractOptionRows(data, source = {}, depth = 0) {
  if (Array.isArray(data))
    return data
  if (!data || typeof data !== 'object' || depth > 4)
    return []
  if (source.recordsField) {
    const nested = getNestedValue(data, source.recordsField)
    if (Array.isArray(nested))
      return nested
  }
  for (const key of ['records', 'list', 'rows', 'items']) {
    if (Array.isArray(data[key]))
      return data[key]
  }
  if (Array.isArray(data.data))
    return data.data
  if (data.data && typeof data.data === 'object')
    return extractOptionRows(data.data, source, depth + 1)
  // 外部接口常返回单个对象而非数组：递归进入的内部对象无包装层 key 时视为单行数据
  if (depth > 0 && !('data' in data) && Object.keys(data).length > 0)
    return [data]
  return []
}

export function normalizeOptionNode(row, source = {}, includeChildren = false) {
  if (!row || typeof row !== 'object')
    return null
  const valueField = source.valueField || source.keyField || 'value'
  const keyField = source.keyField || valueField
  const labelField = source.labelField || 'label'
  const childrenField = source.childrenField || 'children'
  const fallbackValueFields = source.fallbackValueFields || ['value', 'key', keyField, 'id', 'orgId', 'deptId', 'code']
  const fallbackLabelFields = source.fallbackLabelFields || ['label', 'name', 'title', 'orgName', 'deptName', 'orgShortName']
  // 显式配置的 valueField/labelField 优先；选择器记录可能只在 _raw 里带展示字段
  const value = resolveFirstFilledOptionField(row, [valueField, ...fallbackValueFields])
  const label = resolveFirstFilledOptionField(row, [labelField, ...fallbackLabelFields])
  if (value === undefined || value === null || value === '')
    return null
  const option = {
    ...row,
    value,
    key: row.key ?? row[keyField] ?? value,
    label: label === undefined || label === null || label === '' ? String(value ?? '') : String(label),
  }
  if (includeChildren) {
    const children = Array.isArray(row[childrenField])
      ? row[childrenField]
      : Array.isArray(row.children)
        ? row.children
        : []
    option.children = children.map(child => normalizeOptionNode(child, source, true)).filter(Boolean)
  }
  return option
}

export function resolveCascadeConfig(field = {}) {
  return resolveFieldCascadeConfig(field)
}

export function normalizeAlign(value) {
  const align = String(value || '').toLowerCase()
  return ['left', 'center', 'right'].includes(align) ? align : 'left'
}

export function getNestedValue(source, path) {
  return String(path || '')
    .split('.')
    .filter(Boolean)
    .reduce((value, key) => value?.[key], source)
}

export function flattenOptionNodes(options = []) {
  const result = []
  const walk = (nodes) => {
    ;(Array.isArray(nodes) ? nodes : []).forEach((node) => {
      if (!node || typeof node !== 'object')
        return
      result.push(node)
      if (Array.isArray(node.children))
        walk(node.children)
    })
  }
  walk(options)
  return result
}

export function isSameOptionValue(left, right) {
  if (left === right)
    return true
  if (left === null || left === undefined || right === null || right === undefined)
    return false
  return String(left) === String(right)
}

export function normalizeRuntimeFieldType(type) {
  const value = String(type || '')
  if (ORG_TREE_SELECT_TYPES.has(value))
    return 'orgTreeSelect'
  if (USER_SELECT_TYPES.has(value))
    return 'userSelect'
  return value
}

export function isOrgTreeSelectField(field = {}) {
  return normalizeRuntimeFieldType(field.type || field.componentType || field.componentKey) === 'orgTreeSelect'
}

export function isUserSelectField(field = {}) {
  return normalizeRuntimeFieldType(field.type || field.componentType || field.componentKey) === 'userSelect'
}

export function isObjectReferenceField(field = {}) {
  return normalizeRuntimeFieldType(field.type || field.componentType || field.componentKey) === 'objectReference'
}

export function isRecordSelectorField(field = {}) {
  return normalizeRuntimeFieldType(field.type || field.componentType || field.componentKey) === 'recordSelector'
}

export function firstNonBlank(...values) {
  return values.map(value => String(value ?? '').trim()).find(Boolean) || ''
}

export function resolveSelectionLabelFields(field = {}) {
  const selectionType = isUserSelectField(field) ? 'user' : isOrgTreeSelectField(field) ? 'org' : ''
  return buildSelectionLabelFields(field, selectionType)
}

export function normalizeLabelValue(value) {
  return serializeSelectionLabels(value)
}

export function normalizeDisplayText(value) {
  if (Array.isArray(value)) {
    const text = value.map(item => String(item ?? '').trim()).filter(Boolean).join(', ')
    return text || '-'
  }
  if (value === null || value === undefined)
    return '-'
  const text = String(value).trim()
  return text || '-'
}

export function resolveUserLabel(user = {}) {
  return String(user?.realName || user?.name || user?.nickname || user?.username || '').trim()
}

export function isFilledValue(value) {
  if (Array.isArray(value))
    return value.length > 0
  return value !== null && value !== undefined && String(value).trim() !== ''
}

export function resolveScanErrorMessage(error) {
  switch (error?.code) {
    case 'SCAN_CANCELLED':
      return '已取消扫码'
    case 'SCAN_TIMEOUT':
      return '扫码超时，请重试'
    case 'SCAN_UNSUPPORTED':
      return '当前环境不支持扫码'
    case 'SCAN_PERMISSION_DENIED':
      return '请允许浏览器使用摄像头'
    case 'SCAN_INVALID_RESULT':
      return '扫码结果无效'
    default:
      return '扫码失败，请重试'
  }
}

export function firstDisplayFieldName(displayFields = []) {
  const first = Array.isArray(displayFields) ? displayFields[0] : displayFields
  return String(first || '').split(':')[0].trim() || ''
}

export function resolveFormattedRangeValue(value, index) {
  if (!Array.isArray(value))
    return null
  return normalizeFormattedPickerValue(value[index]) ?? null
}

export function normalizeTimestampPickerValue(value) {
  if (value instanceof Date)
    return Number.isNaN(value.getTime()) ? undefined : value.getTime()
  if (typeof value === 'number')
    return Number.isFinite(value) ? value : undefined
  return undefined
}

export function normalizeFormattedPickerValue(value) {
  if (value === null || value === undefined || value === '')
    return null
  if (typeof value === 'string') {
    const text = value.trim()
    if (!text)
      return null
    if (isSupportedPickerText(text))
      return text
    return null
  }
  return undefined
}

export function isSupportedPickerText(text = '') {
  return /^\d{4}$/.test(text)
    || /^\d{4}-\d{2}$/.test(text)
    || /^\d{4}-\d{2}-\d{2}$/.test(text)
    || /^\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}(?::\d{2})?$/.test(text)
    || /^\d{2}:\d{2}(?::\d{2})?$/.test(text)
}

export function normalizeTimestampRangePickerValue(value) {
  if (!Array.isArray(value))
    return undefined
  const normalized = value.map(item => normalizeTimestampPickerValue(item))
  return normalized.length === 2 && normalized.every(item => item !== undefined)
    ? normalized
    : undefined
}

export function normalizeFormattedRangePickerValue(value) {
  if (value === null || value === undefined || value === '')
    return null
  if (!Array.isArray(value))
    return undefined
  const normalized = value.map(item => normalizeFormattedPickerValue(item))
  if (normalized.every(item => item === null))
    return null
  if (normalized.length === 2 && normalized.every(item => typeof item === 'string'))
    return normalized
  return value.some(item => typeof item === 'string') ? null : undefined
}

