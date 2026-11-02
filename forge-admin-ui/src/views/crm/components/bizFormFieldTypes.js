/**
 * CRM 业务表单扩展字段：对齐 AiForm fieldComponentSpecs 类型与选项约定。
 */
import { fieldComponentSpecs } from '@/components/lowcode-builder/designer-core/spec/field-components'

/** 纯展示，不作为可录入扩展字段 */
const EXCLUDED_TYPES = new Set(['text'])

/** 保存时必须提供静态 optionsJson */
export const TYPES_NEED_OPTIONS = new Set([
  'select',
  'radio',
  'radioButton',
  'checkbox',
  'transfer',
  'cascader',
  'treeSelect',
  'customSelect',
])

/** 保存时必须提供 dictType */
export const TYPES_NEED_DICT = new Set(['dictSelect'])

/** 树形选项（可带 children） */
export const TYPES_TREE_OPTIONS = new Set(['treeSelect', 'cascader'])

export const BIZ_FORM_COMPONENT_TYPES = fieldComponentSpecs
  .filter(spec => spec?.type && !EXCLUDED_TYPES.has(spec.type))
  .map(spec => spec.type)

export const bizFormTypeOptions = fieldComponentSpecs
  .filter(spec => spec?.type && !EXCLUDED_TYPES.has(spec.type))
  .map(spec => ({
    label: `${spec.label || spec.type}${spec.group ? `（${spec.group}）` : ''}`,
    value: spec.type,
  }))

export function parseOptionsJson(raw) {
  if (!raw)
    return []
  if (Array.isArray(raw))
    return raw
  if (typeof raw !== 'string')
    return []
  try {
    const data = JSON.parse(raw)
    return Array.isArray(data) ? data : []
  }
  catch {
    return []
  }
}

export function serializeOptions(options) {
  const cleaned = (options || [])
    .map(normalizeOptionNode)
    .filter(Boolean)
  return cleaned.length ? JSON.stringify(cleaned) : null
}

function normalizeOptionNode(node) {
  if (!node || typeof node !== 'object')
    return null
  const label = String(node.label ?? '').trim()
  const value = node.value == null ? '' : String(node.value).trim()
  if (!label && !value)
    return null
  const out = {
    label: label || value,
    value: value || label,
  }
  if (Array.isArray(node.children) && node.children.length) {
    const children = node.children.map(normalizeOptionNode).filter(Boolean)
    if (children.length)
      out.children = children
  }
  return out
}

/** 扁平编辑行 → 树（按 parentValue 挂载） */
export function buildOptionsTree(flatRows) {
  const nodes = (flatRows || [])
    .map((row, index) => ({
      key: row.key || `opt-${index}`,
      label: String(row.label ?? '').trim(),
      value: String(row.value ?? '').trim(),
      parentValue: String(row.parentValue ?? '').trim(),
    }))
    .filter(row => row.label || row.value)
  const byValue = new Map()
  nodes.forEach((row) => {
    const value = row.value || row.label
    byValue.set(value, { label: row.label || value, value, children: [] })
  })
  const roots = []
  nodes.forEach((row) => {
    const value = row.value || row.label
    const node = byValue.get(value)
    if (!node)
      return
    if (row.parentValue && byValue.has(row.parentValue) && row.parentValue !== value) {
      byValue.get(row.parentValue).children.push(node)
    }
    else {
      roots.push(node)
    }
  })
  return roots.map(stripEmptyChildren)
}

function stripEmptyChildren(node) {
  const out = { label: node.label, value: node.value }
  if (node.children?.length)
    out.children = node.children.map(stripEmptyChildren)
  return out
}

/** 树 → 扁平编辑行 */
export function flattenOptionsTree(options, parentValue = '') {
  const rows = []
  ;(options || []).forEach((node, index) => {
    const value = node?.value == null ? '' : String(node.value)
    const label = node?.label == null ? value : String(node.label)
    rows.push({
      key: `f-${parentValue}-${value}-${index}`,
      label,
      value,
      parentValue: parentValue || '',
    })
    if (Array.isArray(node?.children) && node.children.length)
      rows.push(...flattenOptionsTree(node.children, value))
  })
  return rows
}

export function validateCustomFieldType(row) {
  const type = String(row.componentType || 'input').trim()
  if (!BIZ_FORM_COMPONENT_TYPES.includes(type))
    return `不支持的字段类型：${type}`
  if (TYPES_NEED_DICT.has(type) && !String(row.dictType || '').trim())
    return `类型「${type}」需要填写字典类型`
  if (TYPES_NEED_OPTIONS.has(type)) {
    const options = parseOptionsJson(row.optionsJson)
    if (!options.length)
      return `类型「${type}」至少需要一个选项`
  }
  return null
}

export function schemeFieldToAiFormField(item) {
  const type = String(item.componentType || 'input').trim() || 'input'
  const options = parseOptionsJson(item.optionsJson)
  const field = {
    type,
    field: item.fieldCode,
    label: item.displayLabel || item.fieldLabel || item.fieldCode,
    showLabel: false,
    clearable: true,
    required: Number(item.required) === 1,
  }
  if (item.dictType)
    field.dictType = item.dictType
  if (options.length) {
    field.options = options
    field.props = { ...(field.props || {}), options }
  }
  if (type === 'checkbox' || type === 'transfer')
    field.multiple = true
  return field
}

export function isExtValueEmpty(value) {
  if (value == null || value === '')
    return true
  if (Array.isArray(value))
    return value.length === 0
  if (typeof value === 'object')
    return Object.keys(value).length === 0
  return false
}
