import { collectExpressionFieldPaths, parseExpressionTemplate } from '../protocol/expression'
import { describeFieldPath } from './fieldGroups'

export const WATERMARK_FIELD_SEP = ' · '

export const DEFAULT_WATERMARK_STYLE = Object.freeze({
  opacity: 0.08,
  rotateDeg: -24,
  gapXMm: 64,
  gapYMm: 42,
  fontSizePt: 14,
  color: '#94a3b8',
})

export const WATERMARK_DENSITY_OPTIONS = [
  { label: '稀疏', value: 'sparse', gapXMm: 96, gapYMm: 72 },
  { label: '适中', value: 'normal', gapXMm: 64, gapYMm: 42 },
  { label: '紧密', value: 'dense', gapXMm: 40, gapYMm: 28 },
]

const FIELD_PATH = /^[a-z_$][\w$]*(?:\.[a-z_$][\w$]*)+$/i
const SEP_LITERAL = /^[·\-–—/]$/

export function buildWatermarkExpression(fields = []) {
  const paths = uniquePaths(fields)
  if (!paths.length)
    return ''
  if (paths.length === 1)
    return paths[0]
  return `CONCAT(${paths.flatMap((path, index) => (index ? [`"${WATERMARK_FIELD_SEP}"`, path] : [path])).join(', ')})`
}

export function parseWatermarkContent(watermark = {}) {
  const expression = String(watermark?.expression || '').trim()
  if (!expression) {
    return {
      text: String(watermark?.text || '').trim(),
      fields: [],
      expression: '',
      parsedOk: true,
    }
  }
  try {
    const fields = collectExpressionFieldPaths(expression)
    const literals = collectExpressionStringLiterals(expression).filter(isContentLiteral)
    return {
      text: String(watermark?.text || '').trim() || literals.join(WATERMARK_FIELD_SEP),
      fields,
      expression,
      parsedOk: true,
    }
  }
  catch {
    return {
      text: String(watermark?.text || '').trim(),
      fields: [],
      expression,
      parsedOk: false,
    }
  }
}

export function watermarkDensityValue(watermark = {}) {
  const x = Number(watermark.gapXMm)
  const y = Number(watermark.gapYMm)
  if (!Number.isFinite(x) && !Number.isFinite(y))
    return 'normal'
  return [...WATERMARK_DENSITY_OPTIONS].sort((left, right) => {
    const dx = Number.isFinite(x) ? x : DEFAULT_WATERMARK_STYLE.gapXMm
    const dy = Number.isFinite(y) ? y : DEFAULT_WATERMARK_STYLE.gapYMm
    const da = Math.abs(left.gapXMm - dx) + Math.abs(left.gapYMm - dy)
    const db = Math.abs(right.gapXMm - dx) + Math.abs(right.gapYMm - dy)
    return da - db
  })[0].value
}

export function nextWatermark(current, patch = {}) {
  const parsed = parseWatermarkContent(current)
  const enabled = patch.enabled !== undefined ? patch.enabled : current?.enabled !== false
  const text = patch.text !== undefined ? String(patch.text || '').trim() : parsed.text
  const fields = patch.fields !== undefined ? uniquePaths(patch.fields) : parsed.fields
  const expression = (!parsed.parsedOk && patch.fields === undefined)
    ? parsed.expression
    : buildWatermarkExpression(fields)
  const next = {
    opacity: pickNumber(patch.opacity, current?.opacity, DEFAULT_WATERMARK_STYLE.opacity),
    rotateDeg: pickNumber(patch.rotateDeg, current?.rotateDeg, DEFAULT_WATERMARK_STYLE.rotateDeg),
    gapXMm: pickNumber(patch.gapXMm, current?.gapXMm, DEFAULT_WATERMARK_STYLE.gapXMm),
    gapYMm: pickNumber(patch.gapYMm, current?.gapYMm, DEFAULT_WATERMARK_STYLE.gapYMm),
    fontSizePt: pickNumber(patch.fontSizePt, current?.fontSizePt, DEFAULT_WATERMARK_STYLE.fontSizePt),
    color: patch.color || current?.color || DEFAULT_WATERMARK_STYLE.color,
  }
  if (text)
    next.text = text.slice(0, 100)
  if (expression)
    next.expression = expression
  if (!enabled)
    next.enabled = false
  if (enabled && !next.text && !next.expression && !hasCustomWatermarkStyle(next))
    return null
  return next
}

export function composeWatermarkPreview(watermark = {}, catalog = []) {
  if (watermark.enabled === false)
    return ''
  const { text, fields } = parseWatermarkContent(watermark)
  const labels = fields.map(path => fieldLeaf(catalog, path))
  return [text, ...labels].filter(Boolean).join(WATERMARK_FIELD_SEP)
}

export function parseExportFileNameParts(pattern) {
  const source = String(pattern || '').trim()
  if (!source) {
    return { extraText: '', includeTemplate: true, fields: [] }
  }
  const fields = []
  let includeTemplate = false
  const rest = source.replace(/\{\{([^{}]+)\}\}|\{(timestamp|template)\}/g, (full, path, token) => {
    if (path && FIELD_PATH.test(path))
      fields.push(path)
    if (token === 'template')
      includeTemplate = true
    return ''
  })
  const extraText = rest.replace(/[-_\s]+/g, ' ').trim()
  return { extraText, includeTemplate, fields: uniquePaths(fields) }
}

export function buildExportFileNamePattern({ extraText = '', includeTemplate = false, fields = [] } = {}) {
  const parts = []
  const extra = sanitizeExportExtra(extraText)
  if (extra)
    parts.push(extra)
  if (includeTemplate)
    parts.push('{template}')
  uniquePaths(fields).forEach(path => parts.push(`{{${path}}}`))
  return parts.join('-')
}

export function describeExportFileName(parts = {}, catalog = [], { isDefault = false } = {}) {
  if (isDefault)
    return '下载时用模板名和单据名称，并自动加上时间。例如：采购单-华能材料-202609211130.pdf'
  const bits = []
  const extra = sanitizeExportExtra(parts.extraText)
  if (extra)
    bits.push(extra)
  if (parts.includeTemplate)
    bits.push('模板名')
  uniquePaths(parts.fields).forEach(path => bits.push(fieldLeaf(catalog, path)))
  if (!bits.length)
    return '下载时用模板名和单据名称，并自动加上时间。例如：采购单-华能材料-202609211130.pdf'
  bits.push('时间')
  return `下载时会自动加上时间，例如：${bits.join('-')}.pdf`
}

function uniquePaths(fields = []) {
  const seen = new Set()
  const paths = []
  for (const item of fields) {
    const path = String(item || '').trim()
    if (!path || !FIELD_PATH.test(path) || seen.has(path))
      continue
    seen.add(path)
    paths.push(path)
  }
  return paths
}

function collectExpressionStringLiterals(source) {
  const literals = []
  function walk(ast) {
    if (!ast || typeof ast !== 'object')
      return
    if (ast.type === 'string' && ast.value)
      literals.push(String(ast.value))
    if (ast.type === 'call')
      ast.args.forEach(walk)
    if (ast.left)
      walk(ast.left)
    if (ast.right)
      walk(ast.right)
    if (ast.value && typeof ast.value === 'object')
      walk(ast.value)
  }
  const parsed = parseExpressionTemplate(source)
  if (parsed.type === 'expr') {
    walk(parsed.ast)
  }
  else {
    parsed.parts.forEach((part) => {
      if (part.type === 'text' && part.value)
        literals.push(part.value)
      if (part.ast)
        walk(part.ast)
    })
  }
  return literals
}

function isContentLiteral(value) {
  const text = String(value || '').trim()
  return !!text && !SEP_LITERAL.test(text)
}

function hasCustomWatermarkStyle(watermark) {
  return watermark.color !== DEFAULT_WATERMARK_STYLE.color
    || watermark.fontSizePt !== DEFAULT_WATERMARK_STYLE.fontSizePt
    || watermark.gapXMm !== DEFAULT_WATERMARK_STYLE.gapXMm
    || watermark.gapYMm !== DEFAULT_WATERMARK_STYLE.gapYMm
    || watermark.opacity !== DEFAULT_WATERMARK_STYLE.opacity
    || watermark.rotateDeg !== DEFAULT_WATERMARK_STYLE.rotateDeg
}

function pickNumber(patch, current, fallback) {
  const value = patch ?? current
  return Number.isFinite(Number(value)) ? Number(value) : fallback
}

function sanitizeExportExtra(value) {
  return String(value || '')
    .replace(/[\\/{}]/g, '')
    .replace(/\s+/g, ' ')
    .trim()
    .slice(0, 40)
}

function fieldLeaf(catalog, path) {
  const described = describeFieldPath(catalog, path)
  return described.split(' / ').at(-1) || path.split('.').at(-1)
}
