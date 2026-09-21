import { PRINT_LIMITS, PrintError } from '../protocol/types'

export const CALIBRATION_STORAGE_KEY = 'forge:print:calibration:v1'
export const CALIBRATION_PAPERS = Object.freeze({
  A3: Object.freeze({ label: 'A3', widthMm: 297, heightMm: 420 }),
  A4: Object.freeze({ label: 'A4', widthMm: 210, heightMm: 297 }),
  A5: Object.freeze({ label: 'A5', widthMm: 148, heightMm: 210 }),
  B4: Object.freeze({ label: 'B4', widthMm: 250, heightMm: 353 }),
  B5: Object.freeze({ label: 'B5', widthMm: 176, heightMm: 250 }),
})
export const CALIBRATION_CHECKS = Object.freeze([
  Object.freeze({ key: 'browserScale', label: '浏览器打印缩放为 100%，且未使用“适合页面”' }),
  Object.freeze({ key: 'browserDecorations', label: '已关闭浏览器附加的页眉和页脚' }),
  Object.freeze({ key: 'horizontalRuler', label: '纸面横向 100mm 标尺实测为 100mm' }),
  Object.freeze({ key: 'verticalRuler', label: '纸面纵向 100mm 标尺实测为 100mm' }),
  Object.freeze({ key: 'marginFrame', label: '纸面校准框距四边实测为 10mm' }),
  Object.freeze({ key: 'paperDirection', label: '实际纸张规格和横纵方向与校准页一致' }),
])

const MIN_CALIBRATION_SIDE_MM = 148

function finiteDimension(value, name) {
  const number = Number(value)
  if (!Number.isFinite(number) || number < MIN_CALIBRATION_SIDE_MM || number > PRINT_LIMITS.paperSizeMm) {
    throw new PrintError('INVALID_CALIBRATION_PAPER', `${name}必须在 ${MIN_CALIBRATION_SIDE_MM}–${PRINT_LIMITS.paperSizeMm}mm 之间`)
  }
  return number
}

export function resolveCalibrationPaper(config = {}) {
  const preset = config.preset || 'A4'
  const orientation = config.orientation || 'PORTRAIT'
  if (!['PORTRAIT', 'LANDSCAPE'].includes(orientation))
    throw new PrintError('INVALID_CALIBRATION_ORIENTATION', '校准纸张方向必须是纵向或横向')
  const source = preset === 'CUSTOM'
    ? { label: '自定义', widthMm: finiteDimension(config.widthMm, '纸张短边'), heightMm: finiteDimension(config.heightMm, '纸张长边') }
    : CALIBRATION_PAPERS[preset]
  if (!source)
    throw new PrintError('INVALID_CALIBRATION_PAPER', '不支持的校准纸张规格')
  const shorter = Math.min(source.widthMm, source.heightMm)
  const longer = Math.max(source.widthMm, source.heightMm)
  return Object.freeze({
    preset,
    label: source.label,
    orientation,
    orientationLabel: orientation === 'LANDSCAPE' ? '横向' : '纵向',
    widthMm: orientation === 'LANDSCAPE' ? longer : shorter,
    heightMm: orientation === 'LANDSCAPE' ? shorter : longer,
    baseWidthMm: shorter,
    baseHeightMm: longer,
  })
}

function text(id, xMm, yMm, widthMm, heightMm, value, style = {}) {
  return { id, type: 'TEXT', xMm, yMm, widthMm, heightMm, text: value, style: { fontSizePt: 9, lineHeight: 1.35, ...style } }
}

function line(id, xMm, yMm, widthMm, heightMm, borderWidthMm = 0.2) {
  return { id, type: 'LINE', xMm, yMm, widthMm, heightMm, style: { borderWidthMm, borderColor: '#111111', borderStyle: 'solid' } }
}

function rulerElements(axis, xMm, yMm) {
  const horizontal = axis === 'horizontal'
  const elements = [line(`calibration-${axis}-ruler`, xMm, yMm, horizontal ? 100 : 0.1, horizontal ? 0.1 : 100, 0.25)]
  for (let offset = 0; offset <= 100; offset += 10) {
    const major = offset % 50 === 0
    elements.push(line(
      `calibration-${axis}-tick-${offset}`,
      horizontal ? xMm + offset : xMm,
      horizontal ? yMm - (major ? 2 : 1.4) : yMm + offset,
      horizontal ? 0.1 : major ? 2 : 1.4,
      horizontal ? major ? 2 : 1.4 : 0.1,
      0.2,
    ))
    if (major) {
      elements.push(text(
        `calibration-${axis}-label-${offset}`,
        horizontal ? xMm + offset - 4 : xMm + 3,
        horizontal ? yMm + 2 : yMm + offset - 2.5,
        12,
        5,
        String(offset),
        { fontSizePt: 7 },
      ))
    }
  }
  return elements
}

function deepFreeze(value) {
  if (value && typeof value === 'object' && !Object.isFrozen(value)) {
    Object.values(value).forEach(deepFreeze)
    Object.freeze(value)
  }
  return value
}

export function createPrintCalibrationResult(config = {}) {
  const paper = resolveCalibrationPaper(config)
  const informationWidth = Math.max(70, paper.widthMm - 52)
  const elements = [
    {
      id: 'calibration-margin-frame',
      type: 'RECTANGLE',
      xMm: 10,
      yMm: 10,
      widthMm: paper.widthMm - 20,
      heightMm: paper.heightMm - 20,
      style: { borderWidthMm: 0.25, borderColor: '#111111', borderStyle: 'solid' },
    },
    text('calibration-title', 16, 14, paper.widthMm - 32, 8, 'Forge 打印尺寸校准页', { fontSizePt: 15, fontWeight: 700, textAlign: 'center' }),
    text('calibration-paper', 16, 22, paper.widthMm - 32, 6, `${paper.label} · ${paper.orientationLabel} · ${paper.widthMm} × ${paper.heightMm} mm`, { fontSizePt: 9, textAlign: 'center' }),
    ...rulerElements('horizontal', 20, 34),
    ...rulerElements('vertical', 20, 34),
    text('calibration-horizontal-caption', 52, 38, 60, 5, '横向标尺 100mm', { fontSizePt: 8, textAlign: 'center' }),
    text('calibration-instructions', 34, 47, informationWidth, 48, [
      '打印设置',
      '1. 选择与本页一致的纸张和方向',
      '2. 缩放设为 100%，边距设为“无”',
      '3. 关闭浏览器附加页眉和页脚',
      '4. 打印后用实体直尺测量两条标尺和四边校准框',
      '',
      '浏览器只能确认打印对话框已打开；物理输出结果必须由使用者勾选验收。',
    ].join('\n'), { fontSizePt: 9, lineHeight: 1.45 }),
    text('calibration-vertical-caption', 12, 70, 16, 32, '纵\n向\n100mm', { fontSizePt: 8, textAlign: 'center' }),
    text('calibration-frame-caption', 34, Math.min(paper.heightMm - 25, 105), informationWidth, 12, '外框距纸张四边均为 10mm', { fontSizePt: 9, fontWeight: 600 }),
    text('calibration-footer-note', 16, paper.heightMm - 19, paper.widthMm - 32, 6, '本页不含业务数据 · 验收记录仅保存在当前浏览器', { fontSizePt: 7, textAlign: 'center', color: '#4b5563' }),
  ]
  const result = {
    geometry: { widthMm: paper.widthMm, heightMm: paper.heightMm },
    pages: [{
      number: 1,
      header: { xMm: 0, yMm: 0, elements: [] },
      footer: { xMm: 0, yMm: 0, elements: [] },
      fragments: [{ id: 'calibration-page', kind: 'FIXED', xMm: 0, yMm: 0, widthMm: paper.widthMm, heightMm: paper.heightMm, elements }],
    }],
    warnings: [],
    generatedAt: new Date().toISOString(),
    dataMode: 'CALIBRATION',
    calibration: paper,
  }
  return deepFreeze(result)
}

export function detectPrintCapabilities(environment = globalThis) {
  const windowObject = environment.window || environment
  const documentObject = environment.document || windowObject.document
  const cssObject = environment.CSS || windowObject.CSS
  const printFunction = typeof windowObject.print === 'function'
  const isolatedDocument = typeof documentObject?.createElement === 'function' && !!documentObject.body
  const millimetreCss = typeof cssObject?.supports === 'function' ? cssObject.supports('width', '100mm') : null
  const afterPrintEvent = 'onafterprint' in windowObject || typeof windowObject.matchMedia === 'function'
  return Object.freeze({
    printFunction,
    isolatedDocument,
    millimetreCss,
    afterPrintEvent,
    dialogAvailable: printFunction && isolatedDocument,
    physicalOutputConfirmed: false,
    status: printFunction && isolatedDocument ? 'READY' : 'LIMITED',
  })
}

export function calibrationProfileKey(config = {}) {
  const paper = resolveCalibrationPaper(config)
  return `${paper.preset}:${paper.orientation}:${paper.baseWidthMm}x${paper.baseHeightMm}`
}

function emptyAcceptance(config) {
  return {
    profile: calibrationProfileKey(config),
    checks: Object.fromEntries(CALIBRATION_CHECKS.map(item => [item.key, false])),
    confirmedByUser: false,
    updatedAt: null,
  }
}

function sanitizeAcceptance(config, value) {
  const clean = emptyAcceptance(config)
  if (!value || typeof value !== 'object' || Array.isArray(value))
    return clean
  for (const item of CALIBRATION_CHECKS)
    clean.checks[item.key] = value.checks?.[item.key] === true
  clean.confirmedByUser = CALIBRATION_CHECKS.every(item => clean.checks[item.key])
  clean.updatedAt = typeof value.updatedAt === 'string' && !Number.isNaN(Date.parse(value.updatedAt)) ? value.updatedAt : null
  return clean
}

function localStorageOrNull(storage) {
  if (storage)
    return storage
  return typeof localStorage === 'undefined' ? null : localStorage
}

export function readCalibrationAcceptance(config, storage) {
  const target = localStorageOrNull(storage)
  if (!target)
    return emptyAcceptance(config)
  try {
    const root = JSON.parse(target.getItem(CALIBRATION_STORAGE_KEY) || '{}')
    return sanitizeAcceptance(config, root?.profiles?.[calibrationProfileKey(config)])
  }
  catch {
    return emptyAcceptance(config)
  }
}

export function writeCalibrationAcceptance(config, checks, storage, now = new Date()) {
  const target = localStorageOrNull(storage)
  if (!target)
    throw new PrintError('LOCAL_STORAGE_UNAVAILABLE', '当前浏览器无法保存本机验收记录')
  const profile = calibrationProfileKey(config)
  let root = {}
  try {
    root = JSON.parse(target.getItem(CALIBRATION_STORAGE_KEY) || '{}')
  }
  catch {
    root = {}
  }
  const record = sanitizeAcceptance(config, { checks, updatedAt: now.toISOString() })
  const profiles = root && typeof root.profiles === 'object' && !Array.isArray(root.profiles) ? root.profiles : {}
  target.setItem(CALIBRATION_STORAGE_KEY, JSON.stringify({ version: 1, profiles: { ...profiles, [profile]: record } }))
  return record
}
