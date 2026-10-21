/** Pure helpers extracted from GridBlockRenderer. */

import { isPageWidgetComponentKey } from '@/components/lowcode-builder/shared/page-widget-schema'
import { matchSimpleExpression } from '@/components/lowcode-builder/shared/runtime-rules'
import { resolveDefaultSearchComponentType } from './fieldDrawerConfig'

let barcodeModulePromise

export function resolvePreviewChildHeight(child = {}) {
  const height = normalizeCssNumber(child.props?.style?.height)
  if (height)
    return height
  return Math.max(72, Number(child.gridH || 2) * 32 + Math.max(0, Number(child.gridH || 2) - 1) * 8)
}

export function normalizeCssNumberValue(value) {
  if (value === null || value === undefined || value === '' || value === '100%' || value === 'auto')
    return 0
  if (typeof value === 'number')
    return value
  const num = Number(String(value).trim().replace('px', ''))
  return Number.isFinite(num) ? num : 0
}

export function normalizeCssNumber(value) {
  return normalizeCssNumberValue(value)
}

export function hasGridCellChildren(cell = {}) {
  return Array.isArray(cell.children) && cell.children.length > 0
}

export function resolveFormLayoutBlockType(event) {
  const raw = event.dataTransfer?.getData('application/x-forge-form-layout')
  if (!raw)
    return ''
  try {
    return String(JSON.parse(raw)?.componentKey || '').trim()
  }
  catch {
    return ''
  }
}

/** 图表显示高度：优先壳上固定高（锚点拖拽），再回落 props.height */
export function resolveChartBlockHeight(block = {}) {
  const styleH = normalizeCssNumberValue(block?.props?.style?.height)
  const propH = Number(block?.props?.height)
  const fromProp = Number.isFinite(propH) && propH > 120 ? propH : 0
  if (styleH > 120)
    return Math.max(160, Math.round(styleH), fromProp)
  if (fromProp)
    return Math.max(160, Math.round(fromProp))
  return 260
}

export function nestedChildShellStyle(child = {}) {
  const style = child?.props?.style || {}
  const widthMode = style.widthMode || 'full'
  const rawWidth = style.width
  let rawHeight = style.height
  const isChart = ['bar-chart', 'line-chart'].includes(child?.blockType)
  // 图表：style.height 缺失或仍是 100% 时，回落到 props.height，避免壳塌成一条线后画布溢出叠层
  if (isChart && (rawHeight === '' || rawHeight == null || rawHeight === '100%' || rawHeight === 'auto')) {
    const propH = Number(child?.props?.height)
    rawHeight = Number.isFinite(propH) && propH > 120 ? propH + 20 : 280
  }
  const isFullWidth = widthMode === 'full'
    || rawWidth === ''
    || rawWidth === null
    || rawWidth === undefined
    || rawWidth === '100%'
    || rawWidth === 'auto'
  const resolvedHeight = normalizeCssSize(rawHeight, '')
  // 外壳承载选中边框与缩放锚点，必须与子组件实际宽高同步。
  // 父级 .layout-grid-cell-body 是 grid，默认 justify-items:stretch 会无视 width，
  // 固定宽时必须 justifySelf:start，左右锚点才能真正改宽。
  return {
    position: 'relative',
    left: '0',
    top: '0',
    width: isFullWidth ? '100%' : normalizeCssSize(rawWidth, '100%'),
    maxWidth: '100%',
    justifySelf: isFullWidth ? 'stretch' : 'start',
    boxSizing: 'border-box',
    height: resolvedHeight,
    minHeight: isChart && resolvedHeight ? resolvedHeight : undefined,
    // 锚点在壳外伸出，壳必须 visible；图表画布溢出由内部 .page-chart-block / .grid-block 自己裁
    overflow: 'visible',
  }
}

export function normalizeCssSize(value, fallback = '') {
  if (value === null || value === undefined || value === '')
    return fallback
  if (typeof value === 'number')
    return `${value}px`
  const text = String(value).trim()
  if (!text)
    return fallback
  return /^\d+(?:\.\d+)?$/.test(text) ? `${text}px` : text
}

/** 出厂默认富文本；若 title/subtitle 已定制而 content 仍是该值，运行态应回退拼装。 */
export const DEFAULT_PAGE_TITLE_CONTENT = '<h1>页面标题</h1><p>页面说明或当前业务对象摘要</p>'

export function createLegacyPageTitleContent(title, subtitle) {
  const safeTitle = escapeRichText(String(title || '页面标题'))
  const safeSubtitle = escapeRichText(String(subtitle || ''))
  return `<h1>${safeTitle}</h1>${safeSubtitle ? `<p>${safeSubtitle}</p>` : ''}`
}

/**
 * page-title 展示内容：富文本 content 优先；
 * content 为空或仍为出厂默认且 title/subtitle 已改时，按 title/subtitle 拼装，避免属性改了界面不变。
 */
export function resolvePageTitleRichContent(blockProps = {}, blockLabel = '') {
  const content = String(blockProps?.content || '').trim()
  const title = blockProps?.title || blockLabel || '页面标题'
  const subtitle = blockProps?.subtitle || ''
  const titleCustomized = String(title) !== '页面标题'
  const subtitleCustomized = Boolean(String(subtitle).trim())
    && String(subtitle) !== '页面说明或当前业务对象摘要'
  if (!content)
    return createLegacyPageTitleContent(title, subtitle)
  if (content === DEFAULT_PAGE_TITLE_CONTENT && (titleCustomized || subtitleCustomized))
    return createLegacyPageTitleContent(title, subtitle)
  return content
}

/** 将 style.css 对象与顶层 color/fontSize 合并进运行态外壳样式（customStyle 之后覆盖）。 */
export function mergeBlockTypographyStyle(resolvedStyle = {}, style = {}) {
  const next = { ...resolvedStyle }
  const cssObject = style?.css && typeof style.css === 'object' && !Array.isArray(style.css)
    ? style.css
    : {}
  Object.entries(cssObject).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '')
      return
    if (typeof value === 'object')
      return
    next[key] = value
  })
  if (style?.color)
    next.color = style.color
  if (style?.fontSize !== undefined && style?.fontSize !== null && style?.fontSize !== '')
    next.fontSize = toCssSize(style.fontSize, undefined)
  return next
}

export function escapeRichText(value) {
  return value.replace(/[&<>"']/g, (char) => {
    if (char === '\"')
      return '&quot;'
    if (char === String.fromCharCode(39))
      return '&#39;'
    return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;' })[char]
  })
}

export function loadBarcodeModule() {
  if (!barcodeModulePromise)
    barcodeModulePromise = import('jsbarcode').then(module => module.default || module)
  return barcodeModulePromise
}

export function componentType(field) {
  return field?.componentType || field?.dataType || 'input'
}

export function resolveTitleFontSize(level = 2) {
  const map = { 1: 28, 2: 22, 3: 18, 4: 16, 5: 14, 6: 13 }
  return map[Number(level)] || 22
}

export function sanitizeIframeSrc(value = '') {
  const text = String(value || '').trim()
  if (!/^https?:\/\//i.test(text))
    return ''
  return text
}

export function toOptionalNumber(value) {
  const number = Number(value)
  return Number.isFinite(number) && value !== '' && value !== undefined && value !== null ? number : undefined
}

export function isDefaultAiFormField(field = {}) {
  return field.formVisible !== false
    && field.systemField !== true
    && String(field.fieldStatus || 'ENABLED').toUpperCase() !== 'DISABLED'
}


export function resolveSearchOptionSource(field = {}) {
  const configured = field.optionSource
    || field.props?.optionSource
    || field.basicProps?.optionSource
  if (configured && typeof configured === 'object' && Object.keys(configured).length)
    return configured
  // 查询区不擅自拼本表 /tree：非树表会报「树形父级字段不存在: parentId」
  // 选项源必须与表单字段配置一致（basicProps / props.optionSource）
  return undefined
}

export function toAiFormField(field, mode = 'form') {
  const queryType = field.queryType || field.searchType || 'eq'
  const rawType = field.componentType || field.type || field.dataType || ''
  // 页面挂件组件：保留 componentKey 和 widget 属性，走 PageWidgetRenderer 渲染
  if (field.nodeType === 'widget' || (rawType && isPageWidgetComponentKey(rawType))) {
    return {
      field: field.field || field.fieldCode || field.id,
      label: field.label || field.fieldName || field.field,
      componentKey: rawType || field.componentKey,
      type: rawType || field.componentKey,
      nodeType: 'widget',
      span: field.span || 1,
      props: field.props || {},
      fieldBinding: field.fieldBinding || { mode: 'virtual' },
      showLabel: field.showLabel ?? false,
      showFeedback: field.showFeedback ?? false,
    }
  }
  const type = mode === 'search'
    ? (resolveDefaultSearchComponentType(field) || resolveAiFieldType(field))
    : resolveAiFieldType(field)
  const optionSource = resolveSearchOptionSource(field)
  const placeholderPrefix = ['treeSelect', 'orgTreeSelect', 'regionTreeSelect', 'select', 'dictSelect', 'userSelect', 'cascader'].includes(type)
    ? '请选择'
    : '请输入'
  const includeChildren = mode === 'search'
    && ['treeSelect', 'orgTreeSelect', 'regionTreeSelect'].includes(type)
    && field.includeChildren !== false
    && field.props?.includeChildren !== false
  return {
    field: field.field,
    sourceField: field.sourceField || field.field,
    label: field.label || field.field,
    type,
    componentType: type,
    queryType,
    placeholder: field.placeholder || `${placeholderPrefix}${field.label || field.field}`,
    span: field.span || 1,
    clearable: true,
    multiple: field.multiple === true
      || field.props?.multiple === true
      || field.basicProps?.multiple === true
      || queryType === 'in',
    options: field.options || [],
    dictType: field.dictType || '',
    optionSource,
    includeChildren: includeChildren ? true : field.includeChildren,
    props: {
      ...(field.props || {}),
      ...(optionSource ? { optionSource } : {}),
      ...(includeChildren ? { includeChildren: true } : {}),
    },
  }
}

export function resolveAiFieldType(field) {
  const type = field.componentType || field.type || field.dataType || 'input'
  // 页面挂件类型直接透传（qrcode、barcode、rich-text、markdown 等）
  if (isPageWidgetComponentKey(type))
    return type
  const queryType = String(field.queryType || field.searchType || '').toLowerCase()
  if (queryType === 'between') {
    if (['date', 'daterange'].includes(type))
      return 'daterange'
    if (['datetime', 'datetimerange'].includes(type))
      return 'datetimerange'
    if (['time', 'timerange'].includes(type))
      return 'timerange'
  }
  // UI 组件类型优先于存储类型（treeSelect 存 bigint 时不能退化成 number/input）
  if ([
    'input',
    'textarea',
    'select',
    'dictSelect',
    'radio',
    'checkbox',
    'switch',
    'date',
    'datetime',
    'daterange',
    'datetimerange',
    'time',
    'timerange',
    'cascader',
    'regionTreeSelect',
    'orgTreeSelect',
    'userSelect',
    'treeSelect',
    'objectReference',
    'recordSelector',
  ].includes(type)) {
    return type
  }
  if (['int', 'bigint', 'decimal', 'number'].includes(type))
    return 'number'
  if (field.dictType) {
    return 'dictSelect'
  }
  return 'input'
}

export function trendClass(trend) {
  if (typeof trend !== 'string')
    return ''
  if (trend.startsWith('+'))
    return 'up'
  if (trend.startsWith('-'))
    return 'down'
  return ''
}

export function sampleValue(field, idx) {
  if (!field)
    return '-'
  if (field.dictType)
    return '字典'
  if (field.componentType === 'switch' || field.dataType === 'tinyint')
    return idx % 2 === 0 ? '是' : '否'
  if (['int', 'bigint', 'decimal'].includes(field.dataType))
    return field.dataType === 'decimal' ? `${100 + idx}.00` : String(100 + idx)
  if (['date'].includes(field.dataType))
    return '2026-05-21'
  if (['datetime'].includes(field.dataType))
    return '2026-05-21 09:30:00'
  return field.label ? `${field.label}${idx + 1}` : `示例${idx + 1}`
}

export function extractBlockBoundRows(data) {
  if (Array.isArray(data))
    return data
  if (!data || typeof data !== 'object')
    return []
  const candidates = [
    data.records,
    data.list,
    data.rows,
    data.items,
    data.children,
    data.data,
  ]
  const rows = candidates.find(Array.isArray)
  return rows || []
}

export function normalizeBlockRow(row, index = 0) {
  if (row && typeof row === 'object')
    return row
  return {
    label: String(row ?? `项目${index + 1}`),
    title: String(row ?? `项目${index + 1}`),
    value: row,
    content: String(row ?? ''),
  }
}

export function interpolateText(value = '', data = {}) {
  return String(value || '').replace(/\{\{\s*([\w.$-]+)\s*\}\}|\$\{\s*([\w.$-]+)\s*\}|\$form\.([\w.$-]+)/g, (matched, mustacheKey, dollarKey, formKey) => {
    const key = mustacheKey || dollarKey || formKey
    if (!key)
      return matched
    const result = getNestedRecordValue(data, key)
    return result === undefined || result === null ? '' : String(result)
  })
}

export function getNestedRecordValue(source = {}, path = '') {
  return String(path || '')
    .split('.')
    .filter(Boolean)
    .reduce((value, key) => value?.[key], source)
}

export function parseApiConfigValue(apiConfigValue) {
  const text = String(apiConfigValue || '')
  const [method, ...urlParts] = text.includes('@') ? text.split('@') : ['get', text]
  return {
    method: String(method || 'get').toLowerCase(),
    url: urlParts.join('@') || text,
  }
}

export function extractRuntimeTreeRows(response) {
  const data = response?.data
  if (Array.isArray(data))
    return data
  if (Array.isArray(data?.records))
    return data.records
  if (Array.isArray(data?.rows))
    return data.rows
  if (Array.isArray(data?.list))
    return data.list
  if (Array.isArray(data?.children))
    return data.children
  return []
}

export function countTreeNodes(nodes = [], childrenField = 'children') {
  return (Array.isArray(nodes) ? nodes : []).reduce((count, node) => {
    return count + 1 + countTreeNodes(node?.[childrenField] || [], childrenField)
  }, 0)
}

export function collectTreeKeys(nodes = [], childrenField = 'children') {
  return (Array.isArray(nodes) ? nodes : []).flatMap((node) => {
    const key = node?.key === undefined || node?.key === null ? '' : String(node.key)
    const childKeys = collectTreeKeys(node?.[childrenField] || [], childrenField)
    return key ? [key, ...childKeys] : childKeys
  })
}

export function normalizeRuntimeTreeConfig(source = {}) {
  const targetField = source.targetField || source.nodeValueField || source.valueField || source.keyField || 'id'
  const filterField = source.filterField || source.rightFilterField || source.listFilterField || source.parentField || ''
  return {
    keyField: source.keyField || source.nodeKeyField || 'id',
    labelField: source.labelField || source.displayField || source.nameField || 'label',
    targetField,
    filterField,
    loadMode: source.loadMode === 'lazy' || source.lazy === true ? 'lazy' : 'full',
  }
}

export function resolveTemplatePlaceholder(name = '') {
  return ['$', '{', name, '}'].join('')
}

export function matchDisplayCondition(expression = '', row = {}) {
  return matchSimpleExpression(expression, row)
}

export function parseRuntimeApiConfig(value = '') {
  const text = String(value || '').trim()
  const [method, ...urlParts] = text.includes('@') ? text.split('@') : ['post', text]
  return {
    method: String(method || 'post').toLowerCase(),
    url: urlParts.join('@') || text,
  }
}

export function confirmRuntimeAction(message = '确认执行该操作？') {
  if (!window.$dialog?.warning) {
    const nativeConfirm = globalThis?.confirm
    return Promise.resolve(typeof nativeConfirm !== 'function' || nativeConfirm(message))
  }
  return new Promise((resolve) => {
    window.$dialog.warning({
      title: '确认操作',
      content: message,
      positiveText: '确定',
      negativeText: '取消',
      onPositiveClick: () => resolve(true),
      onNegativeClick: () => resolve(false),
      onClose: () => resolve(false),
      onMaskClick: () => resolve(false),
    })
  })
}

export function toCssSize(value, fallback = '') {
  if (value === null || value === undefined || value === '')
    return fallback
  if (typeof value === 'number')
    return `${value}px`
  const text = String(value).trim()
  return /^\d+(?:\.\d+)?$/.test(text) ? `${text}px` : text
}

export function parseInlineStyle(value = '') {
  if (!value || typeof value !== 'string')
    return {}
  return value.split(';').reduce((style, entry) => {
    const [rawKey, ...rawValue] = entry.split(':')
    const key = rawKey?.trim()
    const cssValue = rawValue.join(':').trim()
    if (!key || !cssValue)
      return style
    const camelKey = key.replace(/-([a-z])/g, (_, char) => char.toUpperCase())
    style[camelKey] = cssValue
    return style
  }, {})
}
