export function getRowDisplayTitle(row = {}) {
  return firstText(
    row.title,
    row.businessSummary,
    row.businessObjectName,
    row.processName,
    row.processTitle,
    row.modelName,
    row.processDefinitionName,
    row.taskName,
    '-',
  )
}

export function getProcessDisplayName(row = {}, fallback = '-') {
  return firstText(
    readableProcessName(row.processName, row),
    readableProcessName(row.processTitle, row),
    readableProcessName(row.modelName, row),
    readableProcessName(row.processDefinitionName, row),
    fallback,
  )
}

export function getBusinessFormDisplayTitle(context = {}, fallback = '业务表单') {
  const objectName = firstText(context.businessObjectName, context.objectName, context.appName, context.businessName)
  const summary = firstText(context.businessSummary, context.summary)
  if (objectName && summary && !summary.includes(objectName))
    return `${objectName} · ${summary}`
  return firstText(summary, objectName, context.formName, fallback)
}

// 任务 createTime 是当前节点到达时间；流程发起时间只能取审批时间轴里的 start 记录
export function getProcessStartTime(history = [], fallback = '-') {
  const start = (Array.isArray(history) ? history : []).find(record => record?.action === 'start')
  return firstText(start?.createTime, start?.completeTime).replace('T', ' ') || fallback
}

export function getTaskHandlerName(row = {}, fallback = '-') {
  if (!row?.assignee && row?.candidateUsers)
    return firstText(row.assigneeName, '待认领')
  return firstText(row.assigneeName, row.assignee, fallback)
}

export function getTaskDisplayName(rowOrName = {}, fallback = '-') {
  const row = rowOrName && typeof rowOrName === 'object' ? rowOrName : {}
  const taskName = firstText(
    typeof rowOrName === 'string' ? rowOrName : '',
    row.taskName,
    row.name,
  )
  const taskCode = firstText(
    row.taskDefKey,
    row.taskDefinitionKey,
    row.activityId,
    row.activityKey,
    row.nodeKey,
  )
  return firstText(stripTrailingTaskCode(taskName, taskCode), fallback)
}

const MAX_TASK_DISPLAY_FIELDS = 6
const DISPLAY_FIELD_SKIP_KEYS = new Set([
  'id',
  'tenantid',
  'delflag',
  'createby',
  'createtime',
  'updateby',
  'updatetime',
  'createdept',
  'recordid',
  'businesskey',
  'processinstanceid',
  'processdefkey',
  'displayfields',
  'fields',
  'items',
  'displayextensions',
])

export function buildTaskBusinessHeadline(row = {}) {
  const objectName = firstText(row.businessObjectName, row.businessType)
  const summary = firstText(row.businessSummary)
  if (objectName && summary && !summary.includes(objectName))
    return `${objectName}：${summary}`
  return firstText(summary, objectName)
}

export function buildTaskDisplayFields(row = {}) {
  const fromExtensions = normalizeDisplayFields(row?.displayExtensions)
  if (fromExtensions.length)
    return fromExtensions.slice(0, MAX_TASK_DISPLAY_FIELDS)
  const params = row?.businessParams && typeof row.businessParams === 'object' && !Array.isArray(row.businessParams)
    ? row.businessParams
    : {}
  const fromParams = normalizeDisplayFields(params.displayFields || params.fields || params.items)
  return fromParams.slice(0, MAX_TASK_DISPLAY_FIELDS)
}

export function hasTaskBusinessSummary(row = {}) {
  return Boolean(buildTaskBusinessHeadline(row) || buildTaskDisplayFields(row).length)
}

function normalizeDisplayFields(source) {
  if (source == null)
    return []
  if (Array.isArray(source))
    return source.map((item, index) => toDisplayField(item, index)).filter(Boolean)
  if (typeof source !== 'object')
    return []
  if (Array.isArray(source.fields))
    return normalizeDisplayFields(source.fields)
  if (Array.isArray(source.items))
    return normalizeDisplayFields(source.items)
  return Object.entries(source)
    .filter(([key]) => !shouldSkipDisplayKey(key))
    .map(([key, value], index) => toDisplayField({ label: key, value }, index))
    .filter(Boolean)
}

function toDisplayField(item, index) {
  if (item == null)
    return null
  if (typeof item !== 'object') {
    const value = formatDisplayValue(item)
    return value ? { key: `field-${index}`, label: '', value } : null
  }
  const label = firstText(item.label, item.name, item.title, item.fieldLabel, item.key, item.field)
  const value = formatDisplayValue(item.value ?? item.text ?? item.displayValue)
  if (!value)
    return null
  return {
    key: firstText(item.key, item.field, label, `field-${index}`),
    label,
    value,
  }
}

function formatDisplayValue(value) {
  if (value == null || value === '')
    return ''
  if (Array.isArray(value))
    return value.map(item => formatDisplayValue(item)).filter(Boolean).join('、')
  if (typeof value === 'object')
    return ''
  return String(value).trim()
}

function shouldSkipDisplayKey(key) {
  const normalized = String(key || '').replace(/[_-]/g, '').toLowerCase()
  return !normalized || DISPLAY_FIELD_SKIP_KEYS.has(normalized) || normalized.startsWith('process')
}

export function firstText(...values) {
  for (const value of values) {
    const text = String(Array.isArray(value) ? value[0] || '' : value ?? '').trim()
    if (text)
      return text
  }
  return ''
}

function stripTrailingTaskCode(value, code) {
  let text = String(value ?? '').trim()
  const originalText = text
  const normalizedCode = String(code ?? '').trim()
  if (!text)
    return ''

  if (normalizedCode) {
    if (text === normalizedCode)
      return ''
    const escapedCode = escapeRegExp(normalizedCode)
    text = text
      .replace(new RegExp(`\\s*[（(【\\[]\\s*${escapedCode}\\s*[）)】\\]]\\s*$`), '')
      .replace(new RegExp(`\\s*[-:/|]\\s*${escapedCode}\\s*$`), '')
      .replace(new RegExp(`\\s+${escapedCode}\\s*$`), '')
      .trim()
    if (!text || text !== originalText)
      return text
  }

  const genericText = stripGenericTrailingCode(text)
  if (genericText && hasReadableLabel(genericText))
    return genericText
  return text
}

function stripGenericTrailingCode(value) {
  const text = String(value ?? '').trim()
  const pairedText = stripPairedTrailingCode(text)
  if (pairedText)
    return pairedText

  const parts = text.split(/\s+/)
  if (parts.length <= 1)
    return ''
  const candidate = parts.at(-1)
  if (!isTechnicalCode(candidate))
    return ''
  return text.slice(0, text.lastIndexOf(candidate)).trim()
}

function stripPairedTrailingCode(text) {
  const pairs = [
    ['（', '）'],
    ['(', ')'],
    ['【', '】'],
    ['[', ']'],
  ]
  for (const [open, close] of pairs) {
    if (!text.endsWith(close))
      continue
    const openIndex = text.lastIndexOf(open)
    if (openIndex <= 0)
      continue
    const candidate = text.slice(openIndex + open.length, -close.length).trim()
    if (isTechnicalCode(candidate))
      return text.slice(0, openIndex).trim()
  }
  return ''
}

function readableProcessName(value, row = {}) {
  const text = String(value ?? '').trim()
  if (!text)
    return ''
  const codes = [
    row.processDefKey,
    row.processDefinitionKey,
    row.processDefId,
    row.processDefinitionId,
    row.modelKey,
    row.businessType,
  ]
  if (codes.some(code => String(code ?? '').trim() === text))
    return ''
  return text
}

function isTechnicalCode(value) {
  const text = String(value ?? '').trim()
  if (text.length < 3 || !isAsciiLetter(text.charCodeAt(0)))
    return false
  for (let index = 1; index < text.length; index += 1) {
    const code = text.charCodeAt(index)
    if (!isAsciiLetter(code) && !isDigit(code) && !['_', '.', '$', ':', '-'].includes(text[index]))
      return false
  }
  return true
}

function isAsciiLetter(code) {
  return (code >= 65 && code <= 90) || (code >= 97 && code <= 122)
}

function isDigit(code) {
  return code >= 48 && code <= 57
}

function hasReadableLabel(value) {
  return /[\u4E00-\u9FFF]/.test(value) || /\s/.test(value.trim())
}

function escapeRegExp(value) {
  return String(value).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}
