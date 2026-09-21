export const DEFAULT_AUDIT_DIFF_LIMIT = 4

export function isMainAuditField(field = {}) {
  return !field.relationKey
    && field.fieldType !== 'CHILD_SUMMARY'
    && field.fieldCode !== '__childRows'
}

export function sortAuditFields(fields = []) {
  return [...fields].sort((left, right) => {
    const mainOrder = Number(isMainAuditField(right)) - Number(isMainAuditField(left))
    if (mainOrder)
      return mainOrder
    return Number(left.sortOrder ?? Number.MAX_SAFE_INTEGER) - Number(right.sortOrder ?? Number.MAX_SAFE_INTEGER)
  })
}

export function visibleAuditFields(fields = [], expanded = false, limit = DEFAULT_AUDIT_DIFF_LIMIT) {
  const sorted = sortAuditFields(fields)
  return expanded ? sorted : sorted.slice(0, limit)
}

export function groupAuditFields(fields = [], expanded = false, limit = DEFAULT_AUDIT_DIFF_LIMIT) {
  const sorted = sortAuditFields(fields)
  const displayed = expanded ? sorted : sorted.slice(0, limit)
  const mainFields = sorted.filter(isMainAuditField)
  const relatedFields = sorted.filter(field => !isMainAuditField(field))
  return [
    {
      key: 'main',
      label: '主表字段',
      total: mainFields.length,
      fields: displayed.filter(isMainAuditField),
    },
    {
      key: 'related',
      label: '子表字段',
      total: relatedFields.length,
      fields: displayed.filter(field => !isMainAuditField(field)),
    },
  ].filter(group => group.fields.length > 0)
}

export function auditFieldSummary(fields = [], fallbackCount = 0) {
  if (!fields.length)
    return `${fallbackCount || 0} 项变化`
  const mainCount = fields.filter(isMainAuditField).length
  const relatedCount = fields.length - mainCount
  return [
    mainCount ? `主表 ${mainCount} 项` : '',
    relatedCount ? `子表 ${relatedCount} 项` : '',
  ].filter(Boolean).join('，')
}

export function auditDiffHeadings(eventType) {
  if (eventType === 'CREATE')
    return { before: '新增前', after: '新增值' }
  if (eventType === 'DELETE')
    return { before: '删除前值', after: '删除后' }
  return { before: '修改前', after: '修改后' }
}

const TECHNICAL_CODE_PATTERN = /^[\w-]+$/

export function isTechnicalAuditCode(value) {
  const text = String(value || '').trim()
  if (!text || /[\u4E00-\u9FFF]/.test(text))
    return false
  return TECHNICAL_CODE_PATTERN.test(text)
}

export function auditFieldTitle(field = {}) {
  const label = String(field.fieldLabel || '').trim()
  if (label && !isTechnicalAuditCode(label) && label !== field.fieldCode && label !== field.relationKey)
    return label
  if (field.fieldType === 'CHILD_SUMMARY' || field.fieldCode === '__childRows')
    return '子表行变更'
  return '未命名字段'
}

/**
 * Normalize a field value view into a display model for audit UI.
 * @returns {{ text: string, kind: 'value'|'empty'|'null'|'absent'|'omitted'|'masked' }} Display text and semantic kind.
 */
export function resolveAuditValueView(view) {
  if (!view)
    return { text: '—', kind: 'empty' }
  if (view.omitted || view.state === 'OMITTED')
    return { text: '仅记录变更', kind: 'omitted' }
  if (view.protectedValue && view.state === 'VALUE')
    return { text: view.display || '已脱敏', kind: 'masked' }
  if (view.state === 'ABSENT')
    return { text: '不存在', kind: 'absent' }
  if (view.state === 'NULL')
    return { text: '空值', kind: 'null' }
  if (view.display)
    return { text: String(view.display), kind: 'value' }
  if (view.value === undefined || view.value === null)
    return { text: '—', kind: 'empty' }
  return { text: String(view.value), kind: 'value' }
}

export function actorInitial(name = '') {
  const text = String(name || '').trim()
  if (!text)
    return '系'
  return text.slice(0, 1).toUpperCase()
}
