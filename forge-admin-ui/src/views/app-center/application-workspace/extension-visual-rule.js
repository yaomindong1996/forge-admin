const EMPTY_OPERATORS = new Set(['EMPTY', 'NOT_EMPTY'])

export function preferredExtensionObjectId(objects = []) {
  const available = objects.filter(item => objectId(item))
  if (available.length === 1)
    return objectId(available[0])
  const primary = available.filter(item => String(item.objectRole || item.role || '').toUpperCase() === 'PRIMARY')
  return primary.length === 1 ? objectId(primary[0]) : null
}

/**
 * 从当前设计页解析增强预填上下文：页面编码、绑定对象、可匹配入口。
 */
export function resolveExtensionPageContext(pageId, { pages = [], entries = [], objects = [] } = {}) {
  const id = String(pageId || '').trim()
  if (!id)
    return null

  const page = (Array.isArray(pages) ? pages : []).find(item => String(item?.id || '') === id) || null
  const objectRef = page?.objectRef || null
  let resolvedObjectId = objectId(objectRef)
  if (!resolvedObjectId && objectRef?.objectCode) {
    const matchedObject = (Array.isArray(objects) ? objects : []).find(item => item?.objectCode === objectRef.objectCode)
    resolvedObjectId = objectId(matchedObject)
  }

  const matchedEntry = (Array.isArray(entries) ? entries : []).find((item) => {
    const entryPageIds = [
      item?.pageId,
      item?.targetPageId,
      item?.entryPageId,
    ].map(value => String(value || '').trim()).filter(Boolean)
    if (entryPageIds.includes(id))
      return true
    if (resolvedObjectId && String(item?.objectId ?? '') === resolvedObjectId)
      return true
    if (objectRef?.objectCode && item?.objectCode === objectRef.objectCode)
      return true
    return false
  }) || null

  return {
    scopeKey: id,
    pageTitle: page?.title || page?.name || id,
    objectId: resolvedObjectId,
    entryId: matchedEntry?.id == null ? null : String(matchedEntry.id),
  }
}

export function extensionMatchesPage(item = {}, pageId, context = null) {
  const id = String(pageId || '').trim()
  if (!id)
    return false
  if (String(item.scopeKey || '') === id || String(item.pageId || '') === id)
    return true
  if (context?.objectId && String(item.objectId || '') === String(context.objectId))
    return true
  if (context?.entryId && String(item.entryId || '') === String(context.entryId))
    return true
  return false
}

export function extensionPageOptions(pages = [], currentValue = '') {
  const source = Array.isArray(pages) ? pages : []
  const pageMap = new Map(source.map(item => [String(item?.id || ''), item]))
  const options = source
    .filter(item => item?.type === 'page' && item.id)
    .map(item => ({
      label: pageBreadcrumb(item, pageMap),
      value: String(item.id),
    }))
  const value = String(currentValue || '').trim()
  if (value && !options.some(item => item.value === value)) {
    options.unshift({
      label: `${value}（页面已失效，请重新选择）`,
      value,
      invalid: true,
    })
  }
  return options
}

export function extensionFieldOptions(fields = [], currentValue = '', { writable = false } = {}) {
  const options = fields
    .filter(field => fieldCode(field))
    .filter(field => !writable || isWritableExtensionField(field))
    .map(field => ({
      label: `${field.fieldName || field.label || fieldCode(field)}（${fieldCode(field)}）`,
      value: fieldCode(field),
      field,
    }))
  const value = String(currentValue || '').trim()
  if (value && !options.some(item => item.value === value)) {
    options.unshift({
      label: `${value}（字段已失效，请重新选择）`,
      value,
      invalid: true,
    })
  }
  return options
}

export function extensionFieldValueKind(field = {}) {
  const safeField = field || {}
  const fieldType = String(safeField.fieldType || '').toUpperCase()
  const dataType = String(safeField.dataType || '').toUpperCase()
  const componentType = String(safeField.componentType || '').toLowerCase()
  if (safeField.dictType || ['DICT', 'SELECT', 'RADIO'].includes(fieldType))
    return 'DICT'
  if (fieldType === 'SWITCH' || componentType === 'switch' || /BOOL(?:EAN)?|TINYINT\(1\)/.test(dataType))
    return 'BOOLEAN'
  if (/MONEY|NUMBER|INT|LONG|DECIMAL|FLOAT|DOUBLE/.test(`${fieldType} ${dataType}`))
    return 'NUMBER'
  if (/DATETIME|TIMESTAMP/.test(`${fieldType} ${dataType}`) || componentType === 'datetime')
    return 'DATETIME'
  if (fieldType === 'DATE' || dataType === 'DATE' || componentType === 'date')
    return 'DATE'
  return 'TEXT'
}

export function findExtensionField(fields = [], code = '') {
  const value = String(code || '').trim()
  return fields.find(field => fieldCode(field) === value) || null
}

export function isWritableExtensionField(field = {}) {
  const status = String(field.fieldStatus || '').toUpperCase()
  return Boolean(fieldCode(field))
    && field.systemField !== true
    && field.readonly !== true
    && !['DISABLED', 'HIDDEN'].includes(status)
}

export function validateExtensionVisualRule(rule = {}, fields = []) {
  const issues = []
  const catalogReady = fields.length > 0
  const knownFields = new Map(fields.map(field => [fieldCode(field), field]).filter(([code]) => code))
  ;(rule.conditions || []).forEach((condition, index) => {
    const row = index + 1
    const field = knownFields.get(String(condition?.field || '').trim())
    if (!condition?.field)
      issues.push(`条件第 ${row} 行请选择字段`)
    else if (catalogReady && !field)
      issues.push(`条件第 ${row} 行字段已失效，请重新选择`)
    if (!String(condition?.operator || '').trim())
      issues.push(`条件第 ${row} 行请选择比较方式`)
    if (!EMPTY_OPERATORS.has(String(condition?.operator || '').toUpperCase()) && isEmptyValue(condition?.value))
      issues.push(`条件第 ${row} 行请填写比较值`)
  })
  if (!(rule.actions || []).length)
    issues.push('可视化规则至少需要一个动作')
  ;(rule.actions || []).forEach((action, index) => {
    const row = index + 1
    const type = String(action?.actionType || '').toUpperCase()
    if (!type) {
      issues.push(`动作第 ${row} 行请选择动作类型`)
      return
    }
    if (type === 'SET_FIELD') {
      const field = knownFields.get(String(action?.field || '').trim())
      if (!action?.field)
        issues.push(`动作第 ${row} 行请选择目标字段`)
      else if (catalogReady && (!field || !isWritableExtensionField(field)))
        issues.push(`动作第 ${row} 行目标字段已失效或不可写，请重新选择`)
      if (isEmptyValue(action?.value))
        issues.push(`动作第 ${row} 行请填写设置值`)
    }
    else if (type === 'SHOW_MESSAGE' && !String(action?.message || '').trim()) {
      issues.push(`动作第 ${row} 行请填写提示内容`)
    }
    else if (!['SET_FIELD', 'SHOW_MESSAGE'].includes(type) && !String(action?.actionCode || '').trim()) {
      issues.push(`动作第 ${row} 行请选择页面动作`)
    }
  })
  return issues
}

export function operatorNeedsValue(operator) {
  return !EMPTY_OPERATORS.has(String(operator || '').toUpperCase())
}

function objectId(item = {}) {
  const value = item.objectId ?? item.id
  return value == null || value === '' ? null : String(value)
}

function pageBreadcrumb(page, pageMap) {
  const titles = [String(page?.title || page?.name || page?.id || '未命名页面')]
  const visited = new Set([String(page?.id || '')])
  let parentId = page?.parentId == null ? '' : String(page.parentId)
  while (parentId && !visited.has(parentId)) {
    visited.add(parentId)
    const parent = pageMap.get(parentId)
    if (!parent)
      break
    titles.unshift(String(parent.title || parent.name || parent.id || '未命名页面组'))
    parentId = parent.parentId == null ? '' : String(parent.parentId)
  }
  return titles.join(' / ')
}

function fieldCode(field = {}) {
  return String(field.fieldCode || field.field || field.code || '').trim()
}

function isEmptyValue(value) {
  return value === undefined || value === null || (typeof value === 'string' && !value.trim())
}
