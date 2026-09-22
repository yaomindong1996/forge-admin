import { DEFAULT_WATERMARK_STYLE, WATERMARK_DENSITY_OPTIONS } from '../designer/paperPanelModel'

export const DEFAULT_PRINT_PAGE_WATERMARK = Object.freeze({
  enabled: false,
  text: '',
  showUsername: true,
  showTime: false,
  density: 'normal',
  fontSizePt: DEFAULT_WATERMARK_STYLE.fontSizePt,
  color: DEFAULT_WATERMARK_STYLE.color,
})

function densityOf(value) {
  return WATERMARK_DENSITY_OPTIONS.some(item => item.value === value) ? value : 'normal'
}

function fontSizeOf(value) {
  const size = Number(value)
  return Number.isFinite(size) && size >= 6 && size <= 72 ? size : DEFAULT_WATERMARK_STYLE.fontSizePt
}

function colorOf(value) {
  return typeof value === 'string' && /^#[0-9a-f]{6}$/i.test(value) ? value.toLowerCase() : DEFAULT_WATERMARK_STYLE.color
}

export function normalizePrintPageWatermark(value) {
  const source = value && typeof value === 'object' && !Array.isArray(value) ? value : {}
  return {
    enabled: source.enabled === true,
    text: String(source.text || '').trim().slice(0, 50),
    showUsername: source.showUsername !== false,
    showTime: source.showTime === true,
    density: densityOf(source.density),
    fontSizePt: fontSizeOf(source.fontSizePt),
    color: colorOf(source.color),
  }
}

function formatWatermarkTime(value = new Date()) {
  const pad = part => String(part).padStart(2, '0')
  return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())} ${pad(value.getHours())}:${pad(value.getMinutes())}`
}

export function composePrintPageWatermarkText(watermark, { userName = '', now = new Date() } = {}) {
  const normalized = normalizePrintPageWatermark(watermark)
  if (!normalized.enabled)
    return ''
  return [
    normalized.text,
    normalized.showUsername ? String(userName || '').trim() : '',
    normalized.showTime ? formatWatermarkTime(now) : '',
  ].filter(Boolean).join(' · ').slice(0, 100)
}

export function applyPrintPageWatermark(document, watermark, identity = {}) {
  if (document?.watermark?.enabled === false)
    return document
  const text = composePrintPageWatermarkText(watermark, identity)
  if (!text || !document || typeof document !== 'object')
    return document
  const normalized = normalizePrintPageWatermark(watermark)
  const preset = WATERMARK_DENSITY_OPTIONS.find(item => item.value === normalized.density) || WATERMARK_DENSITY_OPTIONS[1]
  const template = document.watermark || {}
  const next = {
    ...template,
    opacity: template.opacity ?? DEFAULT_WATERMARK_STYLE.opacity,
    rotateDeg: template.rotateDeg ?? DEFAULT_WATERMARK_STYLE.rotateDeg,
    gapXMm: preset.gapXMm,
    gapYMm: preset.gapYMm,
    fontSizePt: normalized.fontSizePt,
    color: normalized.color,
    text,
  }
  delete next.expression
  return {
    ...document,
    watermark: next,
  }
}

function parseOptions(value) {
  if (value && typeof value === 'object' && !Array.isArray(value))
    return value
  if (typeof value !== 'string' || !value.trim())
    return {}
  try {
    const parsed = JSON.parse(value)
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  }
  catch {
    return {}
  }
}

export function readPagePrintWatermark(application, pageId) {
  const nodes = parseOptions(application?.options).inAppBuilder?.nodes
  const scopedPageId = String(pageId || '').trim()
  if (!Array.isArray(nodes) || !scopedPageId)
    return normalizePrintPageWatermark()
  const node = nodes.find(item => String(item?.id || '') === scopedPageId)
  return normalizePrintPageWatermark(node?.printWatermark ?? node?.settings?.printWatermark)
}
