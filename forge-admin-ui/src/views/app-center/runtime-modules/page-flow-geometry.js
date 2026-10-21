import { resolveListPageBlockMeta } from '@/components/lowcode-builder/page/page-schema'
import { shouldUsePageFlowStack } from '@/views/app-center/components/portal/portal-page-runtime-layout'

/**
 * 页面流式画布的几何计算：默认尺寸、瀑布流 Y 坐标、碰撞整理与外层壳样式。
 * 均为纯函数，items（当前页面根级区块列表）由调用方传入。
 */

export const DEFAULT_PAGE_PADDING = Object.freeze({
  top: 8,
  right: 8,
  bottom: 8,
  left: 8,
})

export function readPageBlockLength(value, fallback = 0) {
  const number = Number.parseFloat(String(value || fallback))
  return Number.isFinite(number) ? Math.round(number) : 0
}

/** 统一解析页面内边距；支持数字 / 四边对象 / CSS 简写字符串 */
export function normalizePagePadding(raw, fallback = DEFAULT_PAGE_PADDING) {
  const base = {
    top: Number(fallback.top) || 8,
    right: Number(fallback.right) || 8,
    bottom: Number(fallback.bottom) || 8,
    left: Number(fallback.left) || 8,
  }
  if (raw == null || raw === '')
    return { ...base }
  if (typeof raw === 'number' || (typeof raw === 'string' && raw.trim() && !Number.isNaN(Number(raw)))) {
    const n = Math.max(0, Math.round(Number(raw)))
    return { top: n, right: n, bottom: n, left: n }
  }
  if (typeof raw === 'string') {
    const parts = raw.trim().split(/\s+/).map(part => Math.max(0, readPageBlockLength(part, 0)))
    if (parts.length === 1)
      return { top: parts[0], right: parts[0], bottom: parts[0], left: parts[0] }
    if (parts.length === 2)
      return { top: parts[0], right: parts[1], bottom: parts[0], left: parts[1] }
    if (parts.length === 3)
      return { top: parts[0], right: parts[1], bottom: parts[2], left: parts[1] }
    if (parts.length >= 4)
      return { top: parts[0], right: parts[1], bottom: parts[2], left: parts[3] }
  }
  if (typeof raw === 'object') {
    return {
      top: Math.max(0, readPageBlockLength(raw.top, base.top)),
      right: Math.max(0, readPageBlockLength(raw.right, base.right)),
      bottom: Math.max(0, readPageBlockLength(raw.bottom, base.bottom)),
      left: Math.max(0, readPageBlockLength(raw.left, base.left)),
    }
  }
  return { ...base }
}

export function resolvePagePaddingCss(raw, fallback = DEFAULT_PAGE_PADDING) {
  const pad = normalizePagePadding(raw, fallback)
  return `${pad.top}px ${pad.right}px ${pad.bottom}px ${pad.left}px`
}

/** 整页表面：背景 / 圆角 / 边框（存 gridLayout.pageSurface） */
export const DEFAULT_PAGE_SURFACE = Object.freeze({
  backgroundColor: '',
  borderRadius: 0,
  borderWidth: 0,
  borderColor: '#e5e7eb',
  borderStyle: 'solid',
})

const BORDER_STYLES = new Set(['none', 'solid', 'dashed', 'dotted'])

function normalizeCssColorToken(value, fallback = '') {
  const text = String(value || '').trim()
  if (!text || text.length > 64)
    return fallback
  // 拒绝明显注入片段，只允许常见 CSS 颜色写法
  if (/[;{}]|url\s*\(/i.test(text))
    return fallback
  return text
}

export function normalizePageSurface(raw, fallback = DEFAULT_PAGE_SURFACE) {
  const base = {
    backgroundColor: String(fallback.backgroundColor || ''),
    borderRadius: Math.max(0, readPageBlockLength(fallback.borderRadius, 0)),
    borderWidth: Math.max(0, readPageBlockLength(fallback.borderWidth, 0)),
    borderColor: String(fallback.borderColor || '#e5e7eb'),
    borderStyle: BORDER_STYLES.has(String(fallback.borderStyle || ''))
      ? String(fallback.borderStyle)
      : 'solid',
  }
  if (!raw || typeof raw !== 'object')
    return { ...base }
  const borderStyle = String(raw.borderStyle || '').trim().toLowerCase()
  return {
    backgroundColor: normalizeCssColorToken(raw.backgroundColor, base.backgroundColor),
    borderRadius: Math.max(0, Math.min(48, readPageBlockLength(raw.borderRadius, base.borderRadius))),
    borderWidth: Math.max(0, Math.min(12, readPageBlockLength(raw.borderWidth, base.borderWidth))),
    borderColor: normalizeCssColorToken(raw.borderColor, base.borderColor),
    borderStyle: BORDER_STYLES.has(borderStyle) ? borderStyle : base.borderStyle,
  }
}

/** 转为可直接绑到画布/运行页的 style 对象 */
export function resolvePageSurfaceStyle(raw, fallback = DEFAULT_PAGE_SURFACE) {
  const surface = normalizePageSurface(raw, fallback)
  const style = {}
  if (surface.backgroundColor)
    style.backgroundColor = surface.backgroundColor
  if (surface.borderRadius > 0)
    style.borderRadius = `${surface.borderRadius}px`
  if (surface.borderWidth > 0 && surface.borderStyle !== 'none') {
    style.borderWidth = `${surface.borderWidth}px`
    style.borderStyle = surface.borderStyle || 'solid'
    style.borderColor = surface.borderColor || '#e5e7eb'
  }
  return style
}

/**
 * 将历史「画布绝对坐标」迁移为「内容区相对坐标」（相对 pagePadding 内缘）。
 * 迁移后改上下内边距时块会跟着走，与左右通栏行为一致。
 */
export function migratePageFlowToContentCoords(layout = {}, pagePadding) {
  if (!layout || typeof layout !== 'object')
    return layout
  if (layout.pageFlowCoordSpace === 'content')
    return layout
  const pad = normalizePagePadding(pagePadding ?? layout.pagePadding)
  const items = Array.isArray(layout.items) ? layout.items : []
  return {
    ...layout,
    pageFlowCoordSpace: 'content',
    items: items.map((block) => {
      if (!block || typeof block !== 'object')
        return block
      const style = block.props?.style || {}
      const widthMode = style.widthMode || 'full'
      const x = Number(style.pageFlowX)
      const y = Number(style.pageFlowY)
      const nextStyle = { ...style }
      if (widthMode === 'full')
        nextStyle.pageFlowX = 0
      else if (Number.isFinite(x) && x >= 0)
        nextStyle.pageFlowX = Math.max(0, Math.round(x - pad.left))
      if (Number.isFinite(y) && y >= 0)
        nextStyle.pageFlowY = Math.max(0, Math.round(y - pad.top))
      return {
        ...block,
        props: {
          ...(block.props || {}),
          style: nextStyle,
        },
      }
    }),
  }
}

/** 画布像素坐标 → 内容区相对坐标 */
export function canvasToContentFlowPoint(x, y, pagePadding) {
  const pad = normalizePagePadding(pagePadding)
  return {
    x: Math.max(0, Math.round(Number(x) || 0) - pad.left),
    y: Math.max(0, Math.round(Number(y) || 0) - pad.top),
  }
}

/** 内容区相对坐标 → 画布像素坐标（预览/吸附用） */
export function contentToCanvasFlowPoint(x, y, pagePadding) {
  const pad = normalizePagePadding(pagePadding)
  return {
    x: Math.round((Number(x) || 0) + pad.left),
    y: Math.round((Number(y) || 0) + pad.top),
  }
}

export function resolveDefaultPageBlockHeight(block = {}) {
  const blockType = block.blockType || ''
  if (blockType === 'text-title' || blockType === 'section-divider')
    return 56
  if (blockType === 'page-title')
    return 72
  if (['divider', 'space'].includes(blockType))
    return 24
  if (['custom-html', 'text-tip', 'paragraph'].includes(blockType))
    return 80
  if (blockType === 'info-panel')
    return 88
  if (blockType === 'empty-state')
    return 120
  if (['stats-strip', 'workspace-summary-metrics'].includes(blockType))
    return blockType === 'workspace-summary-metrics' ? 200 : 100
  if (blockType === 'quick-entry')
    return 128
  // 图表画布默认 ≥260px；壳高若仍用 96 会 overflow 叠到邻块上
  if (['bar-chart', 'line-chart'].includes(blockType)) {
    const fromProp = Number(block?.props?.height)
    if (Number.isFinite(fromProp) && fromProp > 120)
      return Math.round(fromProp + 20)
    return 280
  }
  if (blockType === 'AiForm')
    return 128
  if (['grid-layout', 'card', 'box-layout', 'tabs'].includes(blockType))
    return 180
  // 与门户运行态 / 列表设计预览一致，过小会被 overflow 裁成“列表看不全”
  if (['AiCrudPage', 'AiTable', 'data-table'].includes(blockType))
    return 560
  if (['search-form', 'toolbar'].includes(blockType))
    return 220
  return 96
}

export function resolveDefaultPageBlockYFromItems(items = [], index = 0) {
  return items
    .slice(0, Math.max(0, index))
    .reduce((top, item) => top + Number(item.props?.style?.pageFlowHeight || resolveDefaultPageBlockHeight(item)) + 16, 20)
}

/** 会 height:auto 撑开的容器：碰撞盒必须跟实测/较大记录高度走，否则会压到下一块 */
export function isPageFlowGrowBlock(block = {}) {
  const heightMode = block?.props?.style?.heightMode || 'fixed'
  if (heightMode === 'auto' || heightMode === 'full')
    return true
  return ['grid-layout', 'card', 'box-layout', 'tabs'].includes(block?.blockType)
}

export function resolvePageBlockFlowGeometry(block = {}, index = 0, items = []) {
  const style = block.props?.style || {}
  const meta = resolveListPageBlockMeta(block.blockType) || {}
  const x = Number.isFinite(Number(style.pageFlowX)) && Number(style.pageFlowX) >= 0
    ? Number(style.pageFlowX)
    : 24
  const y = Number.isFinite(Number(style.pageFlowY)) && Number(style.pageFlowY) >= 0
    ? Number(style.pageFlowY)
    : resolveDefaultPageBlockYFromItems(items, index)
  const widthMode = style.widthMode || 'full'
  const heightMode = style.heightMode || 'fixed'
  const explicitWidth = readPageBlockLength(style.pageFlowWidth) || readPageBlockLength(style.width)
  // pageFlowHeight 与 height 可能短暂不一致：碰撞取较大值，避免容器已撑开仍按下限叠在一起
  const explicitHeight = Math.max(
    readPageBlockLength(style.pageFlowHeight),
    readPageBlockLength(style.height),
  )
  const width = widthMode === 'full'
    ? 100000
    : explicitWidth || Math.round((Math.min(12, Math.max(3, Number(meta.defaultW) || 6)) / 12) * 1200)
  const defaultHeight = resolveDefaultPageBlockHeight(block)
  const height = heightMode === 'auto'
    ? Math.max(explicitHeight || defaultHeight, 40)
    : heightMode === 'full'
      ? Math.max(explicitHeight || defaultHeight, 180)
      : explicitHeight || defaultHeight
  return { x, y, width, height, right: x + width, bottom: y + height }
}

/**
 * 把 DOM 实测高度写回可撑开块的 pageFlowHeight，供碰撞下推使用。
 * @returns {{ items: any[], changed: boolean }}
 */
export function applyMeasuredPageBlockHeights(items = [], measuredById = new Map(), threshold = 2) {
  let changed = false
  const next = items.map((block) => {
    if (!isPageFlowGrowBlock(block))
      return block
    const measured = Number(measuredById instanceof Map
      ? measuredById.get(block.id)
      : measuredById?.[block.id])
    if (!(measured > 0))
      return block
    const style = block.props?.style || {}
    const stored = Math.max(
      readPageBlockLength(style.pageFlowHeight),
      readPageBlockLength(style.height),
    )
    if (Math.abs(measured - stored) < threshold)
      return block
    changed = true
    return {
      ...block,
      props: {
        ...(block.props || {}),
        style: {
          ...style,
          pageFlowHeight: Math.round(measured),
          height: Math.round(measured),
        },
      },
    }
  })
  return { items: next, changed }
}

/**
 * 根页面的组件是最终页面的布局，不是可重叠的自由画布。
 * 这里仅整理根级块；组合布局中的 children 继续由自己的容器布局管理。
 */
export function resolveRootPageBlockCollisions(items = [], changedBlockId = '') {
  const gap = 16
  const placed = []
  const sorted = items
    .map((block, index) => ({ block, index, geometry: resolvePageBlockFlowGeometry(block, index, items) }))
    .sort((left, right) => left.geometry.y - right.geometry.y || left.geometry.x - right.geometry.x || left.index - right.index)

  const resolvedById = new Map()
  sorted.forEach(({ block, geometry }) => {
    let nextY = geometry.y
    placed.forEach((previous) => {
      const horizontallyOverlapped = geometry.x < previous.right && geometry.right > previous.x
      const needsPushDown = horizontallyOverlapped && nextY < previous.bottom + gap
      if (needsPushDown)
        nextY = previous.bottom + gap
    })

    const resolved = { ...geometry, y: nextY, bottom: nextY + geometry.height }
    placed.push(resolved)
    if (nextY !== geometry.y || block.id === changedBlockId) {
      resolvedById.set(block.id, {
        ...block,
        props: {
          ...(block.props || {}),
          style: {
            ...(block.props?.style || {}),
            pageFlowY: Math.round(nextY),
          },
        },
      })
    }
  })

  return items.map(block => resolvedById.get(block.id) || block)
}

function isContainerGrowBlock(block = {}) {
  // 壳样式：仅按组件类型判定「内容撑开」；heightMode=auto 另有分支
  return ['grid-layout', 'card', 'box-layout', 'tabs'].includes(block?.blockType)
}

export { shouldUsePageFlowStack }

export function resolvePageBlockShellStyle(block = {}, items = [], options = {}) {
  const meta = resolveListPageBlockMeta(block.blockType) || {}
  const style = block.props?.style || {}
  const customWidth = String(style.pageFlowWidth || '').trim()
  const customHeight = Number(style.pageFlowHeight)
  const customX = Number(style.pageFlowX)
  const customY = Number(style.pageFlowY)
  const widthMode = style.widthMode || 'full'
  const heightMode = style.heightMode || 'fixed'
  const frameWidth = readPageBlockLength(style.width)
  const frameHeight = readPageBlockLength(style.height)
  const index = items.findIndex(item => item.id === block.id)
  const pagePadding = normalizePagePadding(options.pagePadding)
  const flowStack = shouldUsePageFlowStack(items, options)
  // 绝对自由布局：pageFlowX/Y 为内容区相对坐标时，壳 left/top = padding + content。
  // 未迁移旧数据仍按画布绝对坐标渲染，避免打开页面时整页下移。
  const useContentCoords = options.pageFlowCoordSpace === 'content'
    || options.coordSpace === 'content'
  const contentX = Number.isFinite(customX) && customX >= 0 ? customX : 0
  const contentY = Number.isFinite(customY) && customY >= 0
    ? customY
    : resolveDefaultPageBlockYFromItems(items, index)
  const canvasLeft = useContentCoords
    ? pagePadding.left + (widthMode === 'full' ? 0 : contentX)
    : (Number.isFinite(customX) && customX >= 0 ? customX : pagePadding.left)
  const canvasTop = useContentCoords
    ? pagePadding.top + contentY
    : (Number.isFinite(customY) && customY >= 0
      ? customY
      : Math.max(pagePadding.top, resolveDefaultPageBlockYFromItems(items, index)))
  const remainingSafeWidth = flowStack
    ? '100%'
    : `calc(100% - ${pagePadding.right}px - ${Math.max(pagePadding.left, canvasLeft)}px)`
  const canvasSafeWidth = flowStack
    ? '100%'
    : `calc(100% - ${pagePadding.left + pagePadding.right}px)`
  const defaultHeight = resolveDefaultPageBlockHeight(block)
  const isListCrud = ['AiCrudPage', 'AiTable', 'data-table'].includes(block.blockType)
  // 自由布局绝对定位：列表过窄/过矮会压扁搜索区与表格，给可用下限（通栏文档流不受影响）
  const FREE_LAYOUT_CRUD_MIN_WIDTH = 640
  const FREE_LAYOUT_CRUD_MIN_HEIGHT = 420
  let resolvedHeight = Math.max(40, customHeight > 0 ? customHeight : frameHeight || defaultHeight)

  // 图表：历史数据可能把 pageFlowHeight/height 写成 96，画布仍按 props.height≈260 绘制并溢出叠层
  if (['bar-chart', 'line-chart'].includes(block.blockType)) {
    const chartCanvas = Number(block.props?.height)
    resolvedHeight = Math.max(
      resolvedHeight,
      defaultHeight,
      Number.isFinite(chartCanvas) && chartCanvas > 120 ? Math.round(chartCanvas + 20) : 0,
    )
  }

  // heightMode=fixed：非容器仍锁死壳高；栅格/卡片等容器始终跟内容撑开（minHeight 保留拖拽下限）
  // heightMode=auto：跟内容走
  const useAutoHeight = heightMode === 'auto'
    || (heightMode !== 'full' && isContainerGrowBlock(block))

  const shared = {
    textAlign: style.textAlign || block.props?.textAlign || block.props?.align || 'left',
    boxSizing: 'border-box',
    // 锚点在壳外 -7px，禁止 overflow 裁切选中框/锚点
    overflow: 'visible',
  }

  if (flowStack) {
    const shell = {
      ...shared,
      position: 'relative',
      left: 'auto',
      top: 'auto',
      right: 'auto',
      bottom: 'auto',
      maxWidth: '100%',
      order: Number.isFinite(customY) && customY >= 0
        ? Math.round(customY)
        : Math.round(resolveDefaultPageBlockYFromItems(items, index)),
    }
    // 文档流也要认固定宽，否则左右锚点改宽被写死成 100%
    if (widthMode === 'fixed') {
      const floored = readPageBlockLength(customWidth) || frameWidth || 0
      shell.width = floored > 0 ? `${Math.max(180, floored)}px` : '100%'
    }
    else if (widthMode === 'auto') {
      shell.width = 'auto'
    }
    else {
      shell.width = '100%'
    }
    if (heightMode === 'full') {
      shell.height = 'auto'
      shell.flex = '1 1 auto'
      shell.minHeight = `${resolvedHeight}px`
      return shell
    }
    if (useAutoHeight) {
      shell.height = 'auto'
      shell.minHeight = `${resolvedHeight}px`
      return shell
    }
    // fixed：锚点框 = 实际高度，可缩小
    shell.height = `${resolvedHeight}px`
    shell.minHeight = `${resolvedHeight}px`
    return shell
  }

  if (isListCrud)
    resolvedHeight = Math.max(resolvedHeight, FREE_LAYOUT_CRUD_MIN_HEIGHT)

  const position = {
    ...shared,
    position: 'absolute',
    left: `${canvasLeft}px`,
    top: `${canvasTop}px`,
    maxWidth: remainingSafeWidth,
  }
  if (heightMode === 'full') {
    position.height = 'auto'
    position.bottom = `${pagePadding.bottom}px`
  }
  else if (useAutoHeight) {
    // 列表 CRUD 禁止 height:auto（子块 100% 会塌成空框），锁成可用像素高
    if (isListCrud) {
      position.height = `${resolvedHeight}px`
      position.minHeight = `${resolvedHeight}px`
    }
    else {
      position.height = 'auto'
      position.minHeight = `${resolvedHeight}px`
    }
  }
  else {
    position.height = `${resolvedHeight}px`
    position.minHeight = `${resolvedHeight}px`
  }
  if (widthMode === 'full') {
    return {
      ...position,
      left: `${pagePadding.left}px`,
      width: canvasSafeWidth,
      maxWidth: canvasSafeWidth,
    }
  }
  if (widthMode === 'auto')
    return { ...position, width: customWidth || `min(${Math.max(280, Math.min(560, frameWidth || 520))}px, ${remainingSafeWidth})` }
  if (widthMode === 'fixed') {
    const rawWidth = readPageBlockLength(customWidth) || frameWidth || 0
    const floored = isListCrud
      ? Math.max(rawWidth, FREE_LAYOUT_CRUD_MIN_WIDTH)
      : rawWidth
    if (floored > 0)
      return { ...position, width: `min(${floored}px, ${remainingSafeWidth})` }
  }
  const columns = Math.min(12, Math.max(3, Number(meta.defaultW) || 6))
  return {
    ...position,
    width: customWidth || `min(${Math.round((columns / 12) * 10000) / 100}%, ${remainingSafeWidth})`,
  }
}
