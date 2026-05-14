import { AIComponentSchema } from '@/api/ai/ai.d'

/**
 * 自动布局算法
 * 当 AI 返回的组件不含位置信息时，提供兜底自动布局
 * 策略：网格划分，将画布划分为 cols 列，依次填入组件
 */

interface LayoutCell {
  x: number
  y: number
  w: number
  h: number
  occupied: boolean
}

export function autoLayout(
  components: AIComponentSchema[],
  canvasW: number,
  canvasH: number
): AIComponentSchema[] {
  const padding = 40
  const gap = 20

  // 顶部区域留给标题（约 80px）
  const headerHeight = 80
  const contentTop = padding + headerHeight
  const contentHeight = canvasH - contentTop - padding
  const contentWidth = canvasW - padding * 2

  // 根据组件数量决定列数（加入随机变化）
  const count = components.length
  let cols: number
  if (count <= 3) cols = Math.random() > 0.3 ? 2 : 1
  else if (count <= 6) cols = Math.random() > 0.4 ? 3 : 2
  else if (count <= 10) cols = Math.random() > 0.5 ? 4 : 3
  else cols = Math.random() > 0.4 ? 5 : 4

  const cellWidth = (contentWidth - gap * (cols - 1)) / cols
  // 自动计算行数
  const rows = Math.ceil(count / cols)
  const cellHeight = (contentHeight - gap * (rows - 1)) / rows

  const result: AIComponentSchema[] = []
  let colIndex = 0
  let rowIndex = 0

  for (let i = 0; i < components.length; i++) {
    const comp = { ...components[i] }

    // 如果已有位置信息则保留
    if (comp.x !== undefined && comp.y !== undefined && comp.x > 0 && comp.y > 0) {
      result.push(comp)
      continue
    }

    // 计算网格位置
    const x = padding + colIndex * (cellWidth + gap)
    const y = contentTop + rowIndex * (cellHeight + gap)

    comp.x = Math.round(x)
    comp.y = Math.round(y)
    comp.w = Math.round(cellWidth)
    comp.h = Math.round(cellHeight)

    result.push(comp)

    colIndex++
    if (colIndex >= cols) {
      colIndex = 0
      rowIndex++
    }
  }

  return result
}

interface RectLike {
  x: number
  y: number
  w: number
  h: number
}

const BORDER_KEY_PATTERN = /^Border(0[1-9]|1[0-3])$/
const PANEL_FRAME_KEYS = new Set([
  'PanelFrame',
  'PanelFrame02',
  'PanelFrame03',
  'PanelFrame04',
  'PanelFrame05',
  'PanelFrame06',
  'PanelFrame07',
  'PanelFrame08'
])
const TITLE_KEYS = new Set([
  'TextCommon',
  'TextGradient',
  'ScreenTitle',
  'ScreenTitle02',
  'ScreenTitle03',
  'ScreenTitle04',
  'ScreenTitle05',
  'ScreenTitle06',
  'ScreenTitle07',
  'ScreenTitle08'
])
const HEADER_DECORATION_KEYS = new Set(['Decorates03', 'Decorates06', 'PipelineH'])
const HEADER_INFO_KEYS = new Set(['TimeCommon'])
const LARGE_VISUAL_KEYS = new Set(['MapBase', 'MapAmap', 'ThreeEarth01'])
const PRIMARY_VISUAL_KEYS = new Set([
  'MapBase',
  'MapAmap',
  'ThreeEarth01',
  'LineCommon',
  'LineGradientSingle',
  'LineGradients',
  'LineLinearSingle',
  'BarLine',
  'Graph',
  'Heatmap',
  'Radar',
  'Sankey',
  'VChartLine',
  'VChartArea',
  'VChartPercentArea',
  'VChartScatter'
])
const OVERLAY_DECORATION_KEYS = new Set([
  'GlowBackdrop',
  'DividerLine',
  'Decorates01',
  'Decorates02',
  'Decorates03',
  'Decorates04',
  'Decorates05',
  'Decorates06',
  'PipelineH',
  'PipelineV'
])

function isBorder(key?: string) {
  return !!key && BORDER_KEY_PATTERN.test(key)
}

function isPanelFrame(key?: string) {
  return !!key && PANEL_FRAME_KEYS.has(key)
}

function isFrameLike(key?: string) {
  return isBorder(key) || isPanelFrame(key)
}

function isTitleLike(comp: AIComponentSchema) {
  if (comp.key.startsWith('ScreenTitle')) return true
  return TITLE_KEYS.has(comp.key) && comp.y <= 90
}

function isHeaderDecoration(comp: AIComponentSchema) {
  return HEADER_DECORATION_KEYS.has(comp.key) && comp.y <= 160
}

function isHeaderInfo(comp: AIComponentSchema) {
  return HEADER_INFO_KEYS.has(comp.key) && (comp.y <= 120 || comp.x > 1200)
}

function isOverlayDecoration(comp: AIComponentSchema) {
  return OVERLAY_DECORATION_KEYS.has(comp.key)
}

function isTopMetric(comp: AIComponentSchema) {
  return (comp.key === 'FlipperNumber' || comp.key === 'KpiCard') && comp.y >= 80 && comp.y <= 340
}

function overlaps(a: RectLike, b: RectLike) {
  return a.x < b.x + b.w && a.x + a.w > b.x && a.y < b.y + b.h && a.y + a.h > b.y
}

function clamp(value: number, min: number, max: number) {
  return Math.min(Math.max(value, min), max)
}

function normalizeNumber(value: number | undefined, fallback: number) {
  return Number.isFinite(value) ? Number(value) : fallback
}

function normalizeSize(comp: AIComponentSchema, canvasW: number, canvasH: number) {
  comp.w = Math.round(clamp(normalizeNumber(comp.w, 500), 24, canvasW))
  comp.h = Math.round(clamp(normalizeNumber(comp.h, 300), 24, canvasH))
  comp.x = Math.round(clamp(normalizeNumber(comp.x, 20), 0, Math.max(0, canvasW - comp.w)))
  comp.y = Math.round(clamp(normalizeNumber(comp.y, 100), 0, Math.max(0, canvasH - comp.h)))
}

function normalizeHeader(components: AIComponentSchema[], canvasW: number) {
  const title = components.find(isTitleLike)
  if (title) {
    const isScreenTitle = title.key.startsWith('ScreenTitle')
    const titleW = clamp(
      title.w || Math.round(canvasW * (isScreenTitle ? 0.48 : 0.35)),
      Math.min(isScreenTitle ? 600 : 300, canvasW),
      Math.min(isScreenTitle ? 1100 : 1200, canvasW)
    )
    title.w = Math.round(titleW)
    title.h = Math.round(clamp(title.h || (isScreenTitle ? 86 : 60), isScreenTitle ? 64 : 40, isScreenTitle ? 110 : 96))
    // 不强制居中：LLM 给了 x 就保留，没给才居中
    if (title.x === undefined || title.x <= 0) {
      title.x = Math.round((canvasW - title.w) / 2)
    } else {
      title.x = Math.round(clamp(title.x, 10, canvasW - title.w - 10))
    }
    title.y = Math.round(clamp(title.y || (isScreenTitle ? 14 : 16), 8, 40))

    if (title.option) {
      title.option.fontSize = title.option.fontSize || 38
      if (title.key === 'TextGradient') {
        title.option.size = title.option.size && title.option.size > 30 ? title.option.size : title.option.fontSize
      }
      title.option.fontColor = title.option.fontColor || '#ffffff'
      title.option.fontWeight = title.option.fontWeight || 'bold'
      title.option.letterSpacing = title.option.letterSpacing ?? 4
      if (isScreenTitle) {
        title.option.dataset = title.option.dataset || title.title || '数据可视化指挥中心'
      }
    }
  }

  components.forEach(comp => {
    if (isHeaderDecoration(comp)) {
      comp.x = 0
      comp.w = canvasW
      comp.h = Math.round(clamp(comp.h, comp.key === 'PipelineH' ? 8 : 16, comp.key === 'PipelineH' ? 14 : 28))
      comp.y = title ? title.y + title.h + 5 : 82
      if (!comp.option) comp.option = {} as any
      comp.option.dataset = ''
    }

    if (isHeaderInfo(comp)) {
      comp.w = Math.round(clamp(comp.w || 240, 160, 350))
      comp.h = Math.round(clamp(comp.h || 40, 28, 56))
      comp.x = Math.round(canvasW - comp.w - 24)
      comp.y = Math.round(clamp(comp.y || 24, 16, 48))
    }
  })
}

function normalizeOverlayDecorations(components: AIComponentSchema[], canvasW: number, canvasH: number) {
  components.forEach(comp => {
    if (!isOverlayDecoration(comp)) return

    if (comp.key === 'GlowBackdrop') {
      comp.w = Math.round(clamp(comp.w || Math.round(canvasW * 0.66), 320, canvasW))
      comp.h = Math.round(clamp(comp.h || Math.round(canvasH * 0.46), 200, canvasH))
      if (comp.x === undefined || comp.x <= 0) {
        comp.x = Math.round((canvasW - comp.w) / 2)
      }
      if (comp.y === undefined || comp.y <= 0) {
        comp.y = Math.round(clamp((canvasH - comp.h) / 2 + 20, 90, canvasH - comp.h - 20))
      }
      return
    }

    if (comp.key === 'DividerLine') {
      const isVertical = comp.option?.direction === 'vertical'
      comp.w = Math.round(clamp(comp.w || (isVertical ? 26 : Math.round(canvasW * 0.62)), 8, canvasW))
      comp.h = Math.round(clamp(comp.h || (isVertical ? Math.round(canvasH * 0.42) : 26), 8, canvasH))
    }
  })
}

function removeRedundantDecorations(components: AIComponentSchema[]) {
  const hasScreenTitle = components.some(comp => comp.key === 'ScreenTitle')
  const headerDecorations = components
    .map((comp, index) => ({ comp, index }))
    .filter(({ comp }) => HEADER_DECORATION_KEYS.has(comp.key) && (comp.y <= 180 || comp.w >= 900))

  const preferredDecoration =
    headerDecorations.find(({ comp }) => comp.key === 'Decorates03' || comp.key === 'Decorates06') ||
    headerDecorations[0]

  const preferredDecorationIndex = preferredDecoration?.index

  return components.filter((comp, index) => {
    if (comp.key === 'CirclePoint') return false
    if (hasScreenTitle && HEADER_DECORATION_KEYS.has(comp.key)) return false
    if (HEADER_DECORATION_KEYS.has(comp.key) && (comp.y <= 180 || comp.w >= 900)) {
      return index === preferredDecorationIndex
    }
    return true
  })
}

function normalizeTopMetrics(components: AIComponentSchema[], canvasW: number) {
  components.forEach(comp => {
    if (comp.key !== 'KpiGroup') return
    comp.w = Math.round(clamp(comp.w || Math.round(canvasW * 0.58), 620, canvasW - 40))
    comp.h = Math.round(clamp(comp.h || 112, 90, 140))
    if (comp.x === undefined || comp.x <= 0) {
      comp.x = Math.round((canvasW - comp.w) / 2)
    } else {
      comp.x = Math.round(clamp(comp.x, 20, canvasW - comp.w - 20))
    }
    comp.y = Math.round(clamp(comp.y || 102, 90, 180))
  })

  const metrics = components.filter(isTopMetric)
  if (metrics.length < 2) return

  // 不再强制等宽排列，只在 LLM 没给位置时做兜底均匀分布
  const needsLayout = metrics.some(c => c.x === undefined || c.x <= 0)
  if (!needsLayout) {
    // LLM 给了位置，只做边界安全钳制
    metrics.forEach(c => {
      c.x = Math.round(clamp(c.x, 20, canvasW - c.w - 20))
      c.y = Math.round(clamp(c.y, 90, 220))
      c.w = Math.round(clamp(c.w, 200, 480))
      c.h = Math.round(clamp(c.h, 70, 120))
    })
    return
  }

  // 兜底：均匀分布，但宽度根据数量动态变化
  const gap = Math.round(14 + Math.random() * 16) // 14-30px 随机间距
  const count = metrics.length
  const maxRowWidth = canvasW - 40
  const w = Math.round(clamp((maxRowWidth - gap * (count - 1)) / count, 220, 420))
  const totalW = w * count + gap * (count - 1)
  const startX = Math.round((canvasW - totalW) / 2)
  const y = Math.round(100 + Math.random() * 20) // 100-120px 随机 y

  metrics.forEach((comp, index) => {
    comp.x = startX + index * (w + gap)
    comp.y = y
    comp.w = w
    comp.h = Math.round(clamp(comp.h || 90, 78, 115))
  })
}

function normalizeLargeVisuals(components: AIComponentSchema[], canvasW: number, canvasH: number) {
  components.forEach(comp => {
    if (!LARGE_VISUAL_KEYS.has(comp.key) && !PRIMARY_VISUAL_KEYS.has(comp.key)) return
    const maxW = canvasW - 40
    const maxH = canvasH - 120
    comp.w = Math.round(clamp(comp.w, Math.min(700, maxW), maxW))
    comp.h = Math.round(clamp(comp.h, Math.min(520, maxH), maxH))
    comp.x = Math.round(clamp(comp.x, 20, Math.max(20, canvasW - comp.w - 20)))
    comp.y = Math.round(clamp(comp.y, 100, Math.max(100, canvasH - comp.h - 20)))
  })
}

function getBorderTargetPair(components: AIComponentSchema[], target: AIComponentSchema) {
  const index = components.indexOf(target)
  const border = index > 0 && isFrameLike(components[index - 1].key) ? components[index - 1] : undefined
  return { border, target }
}

function placeTargetWithBorder(
  components: AIComponentSchema[],
  target: AIComponentSchema,
  slot: RectLike,
  placedTargets: Set<AIComponentSchema>
) {
  const { border } = getBorderTargetPair(components, target)
  if (border && isPanelFrame(border.key)) {
    border.x = slot.x
    border.y = slot.y
    border.w = slot.w
    border.h = slot.h
    target.x = slot.x + 8
    target.y = slot.y + 44
    target.w = Math.max(120, slot.w - 16)
    target.h = Math.max(80, slot.h - 52)
  } else {
    target.x = slot.x
    target.y = slot.y
    target.w = slot.w
    target.h = slot.h
  }
  placedTargets.add(target)

  if (border && !isPanelFrame(border.key)) {
    border.x = slot.x - 3
    border.y = slot.y - 3
    border.w = slot.w + 6
    border.h = slot.h + 6
  }
}

function isPlaceableContent(comp: AIComponentSchema) {
  return !isFrameLike(comp.key) &&
    !isTitleLike(comp) &&
    !isOverlayDecoration(comp) &&
    !isHeaderDecoration(comp) &&
    !isHeaderInfo(comp) &&
    !isTopMetric(comp)
}

function shouldWrapWithBorder(comp: AIComponentSchema) {
  return isPlaceableContent(comp)
}

function ensureConsistentPanelBorders(components: AIComponentSchema[]) {
  const panelKeys = Array.from(PANEL_FRAME_KEYS)
  // 同一次布局统一用一个随机风格（除非 LLM 已显式指定）
  const autoPanelKey = panelKeys[Math.floor(Math.random() * panelKeys.length)]
  const result: AIComponentSchema[] = []

  components.forEach(comp => {
    if (isFrameLike(comp.key)) {
      result.push({
        ...comp,
        key: PANEL_FRAME_KEYS.has(comp.key) ? comp.key : 'PanelFrame',
        title: comp.title || '模块框',
        option: {
          ...(comp.option || {}),
          title: comp.option?.title || comp.title || '模块标题'
        }
      })
      return
    }

    const previous = result[result.length - 1]
    if (shouldWrapWithBorder(comp) && !isFrameLike(previous?.key)) {
      result.push({
        key: autoPanelKey,
        y: comp.y - 3,
        w: comp.w + 6,
        h: comp.h + 6,
        title: comp.title || '模块框',
        option: {
          title: comp.title || '模块标题'
        }
      })
    }

    result.push(comp)
  })

  return result
}

function applyLargeVisualTemplate(components: AIComponentSchema[], canvasW: number, canvasH: number) {
  const largeVisual = components.find(comp => LARGE_VISUAL_KEYS.has(comp.key)) ||
    components.find(comp => PRIMARY_VISUAL_KEYS.has(comp.key)) ||
    components
      .filter(isPlaceableContent)
      .filter(comp => comp.w >= 680 || comp.h >= 380)
      .sort((a, b) => b.w * b.h - a.w * a.h)[0]
  if (!largeVisual || canvasW < 1400 || canvasH < 700) return false

  const gap = 20
  const margin = 24
  const contentTop = components.some(isTopMetric) ? 240 : 120
  const bottom = canvasH - margin

  // 多套布局模板，随机选择
  const templates: Array<{ sideW: number; centerW: number; slots: RectLike[]; centerSlot: RectLike }> = [
    // 模板A：左窄-中宽-右窄（经典）
    (() => {
      const sideW = Math.round(clamp(canvasW * 0.22, 380, 540))
      const centerW = canvasW - sideW * 2 - gap * 2 - margin * 2
      const centerX = margin + sideW + gap
      const rightX = centerX + centerW + gap
      const bottomH = Math.round(canvasH * 0.2)
      const largeH = Math.max(400, bottom - contentTop - gap - bottomH)
      const sideH = Math.floor((bottom - contentTop - gap) / 2)
      return {
        sideW, centerW,
        centerSlot: { x: centerX, y: contentTop, w: centerW, h: largeH },
        slots: [
          { x: margin, y: contentTop, w: sideW, h: sideH },
          { x: margin, y: contentTop + sideH + gap, w: sideW, h: sideH },
          { x: rightX, y: contentTop, w: sideW, h: sideH },
          { x: rightX, y: contentTop + sideH + gap, w: sideW, h: sideH },
          { x: centerX, y: contentTop + largeH + gap, w: centerW, h: bottomH }
        ]
      }
    })(),
    // 模板B：左侧大图 + 右侧两列
    (() => {
      const leftW = Math.round(clamp(canvasW * 0.45, 500, 900))
      const rightW = canvasW - leftW - gap - margin * 2
      const rightColW = Math.round((rightW - gap) / 2)
      const largeH = Math.max(420, bottom - contentTop)
      const rightItemH = Math.floor((largeH - gap) / 2)
      return {
        sideW: rightColW, centerW: leftW,
        centerSlot: { x: margin, y: contentTop, w: leftW, h: largeH },
        slots: [
          { x: margin + leftW + gap, y: contentTop, w: rightColW, h: rightItemH },
          { x: margin + leftW + gap + rightColW + gap, y: contentTop, w: rightColW, h: rightItemH },
          { x: margin + leftW + gap, y: contentTop + rightItemH + gap, w: rightColW, h: rightItemH },
          { x: margin + leftW + gap + rightColW + gap, y: contentTop + rightItemH + gap, w: rightColW, h: rightItemH },
          { x: margin, y: contentTop + largeH + gap, w: canvasW - margin * 2, h: bottom - contentTop - largeH - gap }
        ]
      }
    })(),
    // 模板C：右侧大图 + 左侧两列（镜像B）
    (() => {
      const rightW = Math.round(clamp(canvasW * 0.45, 500, 900))
      const leftW = canvasW - rightW - gap - margin * 2
      const leftColW = Math.round((leftW - gap) / 2)
      const largeH = Math.max(420, bottom - contentTop)
      const leftItemH = Math.floor((largeH - gap) / 2)
      return {
        sideW: leftColW, centerW: rightW,
        centerSlot: { x: margin + leftW + gap, y: contentTop, w: rightW, h: largeH },
        slots: [
          { x: margin, y: contentTop, w: leftColW, h: leftItemH },
          { x: margin + leftColW + gap, y: contentTop, w: leftColW, h: leftItemH },
          { x: margin, y: contentTop + leftItemH + gap, w: leftColW, h: leftItemH },
          { x: margin + leftColW + gap, y: contentTop + leftItemH + gap, w: leftColW, h: leftItemH },
          { x: margin, y: contentTop + largeH + gap, w: canvasW - margin * 2, h: bottom - contentTop - largeH - gap }
        ]
      }
    })()
  ]

  const tpl = templates[Math.floor(Math.random() * templates.length)]

  const placedTargets = new Set<AIComponentSchema>()
  placeTargetWithBorder(components, largeVisual, tpl.centerSlot, placedTargets)

  const candidates = components.filter(
    comp => comp !== largeVisual && isPlaceableContent(comp)
  )
  candidates.slice(0, tpl.slots.length).forEach((comp, index) => {
    placeTargetWithBorder(components, comp, tpl.slots[index], placedTargets)
  })

  const removeSet = new Set<AIComponentSchema>(candidates.slice(tpl.slots.length))
  components.forEach((comp, index) => {
    if (!isFrameLike(comp.key)) return
    const target = components[index + 1]
    if (!target || placedTargets.has(target)) return
    removeSet.add(comp)
  })

  for (let i = components.length - 1; i >= 0; i--) {
    if (removeSet.has(components[i])) {
      components.splice(i, 1)
    }
  }

  return true
}

function normalizeBorders(components: AIComponentSchema[], canvasW: number, canvasH: number) {
  components.forEach((comp, index) => {
    if (!isFrameLike(comp.key)) return
    const target = components[index + 1]
    if (!target || isFrameLike(target.key)) return
    if (isPanelFrame(comp.key)) {
      comp.x = clamp(target.x - 8, 0, canvasW)
      comp.y = clamp(target.y - 44, 0, canvasH)
      comp.w = clamp(target.w + 16, 24, canvasW - comp.x)
      comp.h = clamp(target.h + 52, 24, canvasH - comp.y)
      return
    }
    comp.x = clamp(target.x - 3, 0, canvasW)
    comp.y = clamp(target.y - 3, 0, canvasH)
    comp.w = clamp(target.w + 6, 24, canvasW - comp.x)
    comp.h = clamp(target.h + 6, 24, canvasH - comp.y)
  })
}

function enforceBorderGaps(components: AIComponentSchema[], canvasW: number) {
  const minGap = 20
  const pairs: Array<{ border: AIComponentSchema; target: AIComponentSchema }> = []

  components.forEach((comp, index) => {
    if (!isFrameLike(comp.key)) return
    const target = components[index + 1]
    if (!target || isFrameLike(target.key)) return
    pairs.push({ border: comp, target })
  })

  pairs
    .sort((a, b) => a.border.x - b.border.x)
    .forEach((pair, index, sortedPairs) => {
      if (index === 0) return
      const prev = sortedPairs[index - 1].border
      const current = pair.border
      if (!overlaps({ ...prev, x: prev.x, w: prev.w }, { ...current, x: current.x, w: current.w })) {
        const sameBand = prev.y < current.y + current.h && prev.y + prev.h > current.y
        const horizontalGap = current.x - (prev.x + prev.w)
        if (sameBand && horizontalGap >= 0 && horizontalGap < minGap) {
          const shift = Math.min(minGap - horizontalGap, canvasW - (current.x + current.w))
          current.x += shift
          pair.target.x += shift
        }
      }
    })
}

function placeWithoutOverlap(
  comp: AIComponentSchema,
  placed: RectLike[],
  canvasW: number,
  canvasH: number,
  margin: number,
  gap: number
) {
  const maxX = Math.max(margin, canvasW - comp.w - margin)
  const maxY = Math.max(margin, canvasH - comp.h - margin)
  comp.x = clamp(comp.x, margin, maxX)
  comp.y = clamp(comp.y, isTitleLike(comp) || isHeaderDecoration(comp) || isHeaderInfo(comp) ? margin : 100, maxY)

  let attempts = 0
  while (placed.some(item => overlaps(comp, item)) && attempts < 80) {
    comp.y += gap
    if (comp.y + comp.h > canvasH - margin) {
      comp.y = 100
      comp.x += Math.max(gap, Math.round(comp.w * 0.35))
    }
    if (comp.x + comp.w > canvasW - margin) {
      comp.x = margin
      comp.w = Math.max(260, Math.min(comp.w, canvasW - margin * 2))
      comp.h = Math.max(120, Math.min(comp.h, canvasH - comp.y - margin))
      break
    }
    attempts++
  }

  comp.x = Math.round(clamp(comp.x, margin, Math.max(margin, canvasW - comp.w - margin)))
  comp.y = Math.round(clamp(comp.y, margin, Math.max(margin, canvasH - comp.h - margin)))
}

/**
 * 规整 AI 返回的布局。
 * 模型仍负责组件选择和数据，程序兜底处理越界、普通组件重叠、标题居中和边框包裹关系。
 */
export function normalizeAILayout(
  components: AIComponentSchema[],
  canvasW: number,
  canvasH: number
): AIComponentSchema[] {
  const margin = 20
  const gap = 20
  const normalized = ensureConsistentPanelBorders(removeRedundantDecorations(
    components.map(comp => ({ ...comp, option: comp.option ? { ...comp.option } : comp.option }))
  ))

  normalized.forEach(comp => normalizeSize(comp, canvasW, canvasH))
  normalizeHeader(normalized, canvasW)
  normalizeOverlayDecorations(normalized, canvasW, canvasH)
  normalizeTopMetrics(normalized, canvasW)
  const usedLargeTemplate = applyLargeVisualTemplate(normalized, canvasW, canvasH)
  if (!usedLargeTemplate) {
    normalizeLargeVisuals(normalized, canvasW, canvasH)
  }

  const placed: RectLike[] = []
  normalized.forEach((comp, index) => {
    if (usedLargeTemplate) return
    if (isFrameLike(comp.key)) return
    if (isOverlayDecoration(comp)) return
    const previous = normalized[index - 1]
    if (previous && isFrameLike(previous.key)) {
      placeWithoutOverlap(comp, placed, canvasW, canvasH, margin, gap)
      placed.push({ x: comp.x, y: comp.y, w: comp.w, h: comp.h })
      return
    }
    if (isHeaderDecoration(comp) || isHeaderInfo(comp)) return
    placeWithoutOverlap(comp, placed, canvasW, canvasH, margin, gap)
    placed.push({ x: comp.x, y: comp.y, w: comp.w, h: comp.h })
  })

  normalizeBorders(normalized, canvasW, canvasH)
  enforceBorderGaps(normalized, canvasW)
  return normalized
}

/**
 * 标题布局：将标题组件居中放置在顶部
 */
export function layoutHeader(
  titleComp: AIComponentSchema,
  canvasW: number
): AIComponentSchema {
  const result = { ...titleComp }
  result.y = 20
  result.x = Math.round((canvasW - (result.w || 700)) / 2)
  result.h = result.h || 60
  result.w = result.w || 700
  return result
}
