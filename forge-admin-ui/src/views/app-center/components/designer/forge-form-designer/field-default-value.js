/**
 * 表单字段默认值：按组件类型适配编辑器，并解析动态预设（当前时间 / 昨天等）。
 */

export const FORGE_DEFAULT_PRESET_PREFIX = '$forge:'

/** 日期/时间类动态默认值预设 */
export const DATE_DEFAULT_PRESETS = Object.freeze([
  { label: '当前时间', value: `${FORGE_DEFAULT_PRESET_PREFIX}now` },
  { label: '今天', value: `${FORGE_DEFAULT_PRESET_PREFIX}today` },
  { label: '昨天', value: `${FORGE_DEFAULT_PRESET_PREFIX}yesterday` },
  { label: '明天', value: `${FORGE_DEFAULT_PRESET_PREFIX}tomorrow` },
])

const DATE_LIKE_KEYS = new Set([
  'date',
  'datetime',
  'time',
  'month',
  'year',
])

const NUMBER_LIKE_KEYS = new Set([
  'number',
  'inputNumber',
  'integer',
  'money',
  'rate',
  'slider',
])

const OPTION_LIKE_KEYS = new Set([
  'select',
  'dictSelect',
  'radio',
  'radioButton',
  'checkbox',
])

const NO_DEFAULT_EDITOR_KEYS = new Set([
  'fileUpload',
  'imageUpload',
  'upload',
  'barcodeScanner',
  'AiCrudPage',
  'button',
  'divider',
  'groupTitle',
  'title',
  'table',
  'tableGrid',
])

export function isForgeDefaultPreset(value) {
  return typeof value === 'string' && value.startsWith(FORGE_DEFAULT_PRESET_PREFIX)
}

export function parseForgeDefaultPreset(value) {
  if (!isForgeDefaultPreset(value))
    return null
  return value.slice(FORGE_DEFAULT_PRESET_PREFIX.length)
}

export function resolveDefaultValueEditorKind(componentKey = '', fieldAsset = null) {
  const key = String(componentKey || '').trim()
  const fieldType = String(fieldAsset?.fieldType || fieldAsset?.businessFieldType || '').toUpperCase()
  const dataType = String(fieldAsset?.dataType || '').toLowerCase()

  if (!key || NO_DEFAULT_EDITOR_KEYS.has(key))
    return 'none'
  if (key === 'switch' || fieldType === 'SWITCH')
    return 'switch'
  if (NUMBER_LIKE_KEYS.has(key) || ['NUMBER', 'MONEY'].includes(fieldType)
    || ['int', 'integer', 'bigint', 'decimal', 'tinyint', 'double', 'float'].includes(dataType)) {
    if (key === 'switch')
      return 'switch'
    return 'number'
  }
  if (DATE_LIKE_KEYS.has(key) || ['DATE', 'DATETIME'].includes(fieldType)
    || ['date', 'datetime', 'timestamp', 'time'].includes(dataType)) {
    if (key === 'time' || dataType === 'time')
      return 'time'
    return 'date'
  }
  if (OPTION_LIKE_KEYS.has(key))
    return 'option'
  if (key === 'color' || key === 'colorPicker')
    return 'color'
  if (['daterange', 'datetimerange', 'timerange', 'transfer', 'cascader'].includes(key))
    return 'none'
  return 'text'
}

export function resolveSwitchValuePair(component = {}, fieldAsset = null) {
  const props = component?.props || {}
  const explicitChecked = firstDefined(props.checkedValue, component?.checkedValue, fieldAsset?.checkedValue)
  const explicitUnchecked = firstDefined(props.uncheckedValue, component?.uncheckedValue, fieldAsset?.uncheckedValue)
  // 平台 switch 落库 tinyint，默认值对始终用 0/1，避免 DDL 写出 DEFAULT 'true'
  return {
    checkedValue: coerceSwitchBit(explicitChecked, 1),
    uncheckedValue: coerceSwitchBit(explicitUnchecked, 0),
  }
}

function coerceSwitchBit(value, fallback) {
  if (value === undefined || value === null)
    return fallback
  if (value === true || value === 'true' || value === 1 || value === '1')
    return 1
  if (value === false || value === 'false' || value === 0 || value === '0')
    return 0
  return value
}

function firstDefined(...values) {
  for (const value of values) {
    if (value !== undefined && value !== null)
      return value
  }
  return undefined
}

/**
 * 运行时解析字段默认值（静态值或 $forge: 预设）。
 */
export function resolveRuntimeDefaultValue(rawValue, componentKey = '', options = {}) {
  if (rawValue === undefined || rawValue === null || rawValue === '')
    return rawValue === '' ? '' : null

  const preset = parseForgeDefaultPreset(rawValue)
  if (!preset)
    return rawValue

  const now = options.now instanceof Date ? options.now : new Date()
  const key = String(componentKey || options.componentType || options.type || '').toLowerCase()
  const withTime = key === 'datetime' || key === 'datetimerange' || options.withTime === true

  if (preset === 'now')
    return withTime || key === 'time' ? formatDateTime(now, key === 'time') : formatDate(now)
  if (preset === 'today')
    return withTime ? formatDateTime(startOfDay(now)) : formatDate(now)
  if (preset === 'yesterday') {
    const day = addDays(now, -1)
    return withTime ? formatDateTime(startOfDay(day)) : formatDate(day)
  }
  if (preset === 'tomorrow') {
    const day = addDays(now, 1)
    return withTime ? formatDateTime(startOfDay(day)) : formatDate(day)
  }
  return rawValue
}

/**
 * 将数据库 COLUMN_DEFAULT 映射为表单/字段默认值。
 */
export function mapColumnDefaultToFieldDefault(rawDefault, {
  dataType = '',
  componentType = '',
  fieldType = '',
} = {}) {
  if (rawDefault === null || rawDefault === undefined)
    return null

  let text = String(rawDefault).trim()
  if (!text || /^null$/i.test(text))
    return null

  // PostgreSQL / MySQL 表达式默认
  if (isCurrentTimestampExpression(text)) {
    const key = String(componentType || '').toLowerCase()
    const type = String(dataType || '').toLowerCase()
    if (key === 'date' || type === 'date' || String(fieldType).toUpperCase() === 'DATE')
      return `${FORGE_DEFAULT_PRESET_PREFIX}today`
    return `${FORGE_DEFAULT_PRESET_PREFIX}now`
  }
  if (isCurrentDateExpression(text))
    return `${FORGE_DEFAULT_PRESET_PREFIX}today`

  // 去掉类型转换 / 引号：'ACTIVE'::character varying / b'1' / (0)
  text = stripSqlDefaultDecorators(text)
  if (!text || /^null$/i.test(text))
    return null

  const key = String(componentType || '').toLowerCase()
  const type = String(dataType || '').toLowerCase()
  const bizType = String(fieldType || '').toUpperCase()

  if (key === 'switch' || bizType === 'SWITCH' || type === 'tinyint') {
    if (/^(true|b?'?1'?|1)$/i.test(text))
      return 1
    if (/^(false|b?'?0'?|0)$/i.test(text))
      return 0
  }

  if (NUMBER_LIKE_KEYS.has(key) || ['NUMBER', 'MONEY'].includes(bizType)
    || ['int', 'integer', 'bigint', 'decimal', 'double', 'float', 'tinyint'].includes(type)) {
    const numeric = Number(text)
    if (Number.isFinite(numeric))
      return numeric
  }

  return text
}

function isCurrentTimestampExpression(value) {
  const text = String(value || '').trim().toLowerCase()
  return /^(current_timestamp(\(\))?|current_timestamp\(\d+\)|now\(\)|localtimestamp(\(\))?|sysdate|getdate\(\)|systimestamp)$/i.test(text)
    || text.includes('current_timestamp')
}

function isCurrentDateExpression(value) {
  const text = String(value || '').trim().toLowerCase()
  return /^(current_date|curdate\(\)|today)$/i.test(text)
}

function stripSqlDefaultDecorators(value) {
  let text = String(value || '').trim()
  // PostgreSQL cast: 'x'::character varying
  const castMatch = text.match(/^'(.*)'::[\w\s[\]()]+$/i)
  if (castMatch)
    text = castMatch[1]
  // MySQL bit: b'1'
  const bitMatch = text.match(/^b'([01])'$/i)
  if (bitMatch)
    return bitMatch[1]
  // Wrapped parens
  if (text.startsWith('(') && text.endsWith(')'))
    text = text.slice(1, -1).trim()
  // Quoted literal
  if ((text.startsWith("'") && text.endsWith("'")) || (text.startsWith('"') && text.endsWith('"')))
    text = text.slice(1, -1)
  return text.replace(/''/g, "'")
}

function addDays(date, amount) {
  const next = new Date(date.getTime())
  next.setDate(next.getDate() + amount)
  return next
}

function startOfDay(date) {
  const next = new Date(date.getTime())
  next.setHours(0, 0, 0, 0)
  return next
}

function pad2(value) {
  return String(value).padStart(2, '0')
}

export function formatDate(date) {
  return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}`
}

export function formatDateTime(date, timeOnly = false) {
  const time = `${pad2(date.getHours())}:${pad2(date.getMinutes())}:${pad2(date.getSeconds())}`
  if (timeOnly)
    return time
  return `${formatDate(date)} ${time}`
}

/**
 * 批量解析 schema 字段默认值（用于新增表单初始化）。
 */
export function buildResolvedDefaultValues(fields = [], options = {}) {
  const result = {}
  ;(Array.isArray(fields) ? fields : []).forEach((field) => {
    if (!field?.field)
      return
    const raw = field.defaultValue ?? field.props?.defaultValue
    if (raw === undefined || raw === null)
      return
    result[field.field] = resolveRuntimeDefaultValue(raw, field.type || field.componentType || field.componentKey, {
      ...options,
      withTime: ['datetime', 'datetimerange'].includes(String(field.type || '').toLowerCase()),
    })
  })
  return result
}

/**
 * 设计器画布预览值：优先使用组件配置的默认值（含 $forge: 动态预设），
 * 未配置时回落到组件类型的空值，便于右侧改默认值后中间实时看到效果。
 */
export function resolveDesignerCanvasPreviewValue(component = {}) {
  const key = String(component?.componentKey || component?.type || 'input')
  const props = component?.props || {}
  if (Object.prototype.hasOwnProperty.call(props, 'defaultValue')) {
    const raw = props.defaultValue
    if (raw === undefined || raw === null || raw === '')
      return resolveEmptyDesignerPreviewValue(key, component)
    return resolveRuntimeDefaultValue(raw, key)
  }
  return resolveEmptyDesignerPreviewValue(key, component)
}

function resolveEmptyDesignerPreviewValue(componentKey = 'input', component = {}) {
  if (['checkbox', 'checkboxGroup'].includes(componentKey))
    return []
  if (componentKey === 'switch')
    return resolveSwitchValuePair(component).uncheckedValue
  if (['date', 'datetime', 'month', 'year', 'time', 'daterange', 'datetimerange', 'timerange'].includes(componentKey))
    return null
  if (['number', 'inputNumber', 'integer', 'money', 'rate', 'slider'].includes(componentKey))
    return null
  return null
}
