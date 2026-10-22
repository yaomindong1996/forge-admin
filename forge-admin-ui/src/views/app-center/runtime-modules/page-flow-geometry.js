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
  if (blockType === 'AiForm') {
    const fieldCount = Array.isArray(block?.fieldRefs) ? block.fieldRefs.length : 0
    const cols = Math.max(1, Number(block?.props?.gridCols) || 2)
    const rows = Math.max(3, Math.ceil(Math.max(fieldCount, 6) / cols))
    return Math.max(280, 72 + rows * 56 + 64)
  }
  if (blockType === 'detail-info') {
    const fieldCount = Array.isArray(block?.fieldRefs) ? block.fieldRefs.length : 0
    const cols = Math.max(1, Number(block?.props?.columnCount) || 2)
    const rows = Math.max(2, Math.ceil(Math.max(fieldCount, 4) / cols))
    return Math.max(220, 64 + rows * 48)
  }
  if (['grid-layout', 'card', 'panel-box', 'box-layout', 'tabs'].includes(blockType))
    return 220
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
  // 表单 / 详情按字段数量撑开，避免固定矮壳裁切内容
  return ['grid-layout', 'card', 'panel-box', 'box-layout', 'tabs', 'AiForm', 'detail-info'].includes(block?.blockType)
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

/** 带间距的轴对齐包围盒是否重叠（根级块避让用） */
export function pageFlowRectsOverlap(a = {}, b = {}, gap = 16) {
  const ax2 = Number(a.right ?? (Number(a.x) + Number(a.width))) || 0
  const ay2 = Number(a.bottom ?? (Number(a.y) + Number(a.height))) || 0
  const bx2 = Number(b.right ?? (Number(b.x) + Number(b.width))) || 0
  const by2 = Number(b.bottom ?? (Number(b.y) + Number(b.height))) || 0
  const ax = Number(a.x) || 0
  const ay = Number(a.y) || 0
  const bx = Number(b.x) || 0
  const by = Number(b.y) || 0
  const g = Math.max(0, Number(gap) || 0)
  return ax < bx2 + g
    && ax2 + g > bx
    && ay < by2 + g
    && ay2 + g > by
}

/** 把矩形下推到不与已放置块重叠的位置 */
export function pushPageFlowRectBelow(placed = [], geometry = {}, gap = 16) {
  let nextY = Number(geometry.y) || 0
  const height = Math.max(1, Number(geometry.height) || 1)
  let guard = 0
  while (guard++ < 64) {
    const candidate = {
      x: Number(geometry.x) || 0,
      y: nextY,
      width: Number(geometry.width) || 0,
      height,
      right: (Number(geometry.x) || 0) + (Number(geometry.width) || 0),
      bottom: nextY + height,
    }
    let hit = null
    placed.forEach((previous) => {
      if (!pageFlowRectsOverlap(candidate, previous, gap))
        return
      if (!hit || previous.bottom > hit.bottom)
        hit = previous
    })
    if (!hit)
      break
    nextY = hit.bottom + gap
  }
  return nextY
}

/**
 * 根页面的组件是最终页面的布局，不是可重叠的自由画布。
 * 这里仅整理根级块；组合布局中的 children 继续由自己的容器布局管理。
 * changedBlockId 优先钉住其落点，其它块绕开（拖放不再把主动块弹走）。
 */
export function resolveRootPageBlockCollisions(items = [], changedBlockId = '') {
  const gap = 16
  const entries = items.map((block, index) => ({
    block,
    index,
    geometry: resolvePageBlockFlowGeometry(block, index, items),
  }))
  const placed = []
  const resolvedById = new Map()

  function commit(entry, nextY) {
    const geometry = entry.geometry
    const resolved = { ...geometry, y: nextY, bottom: nextY + geometry.height }
    placed.push(resolved)
    if (nextY !== geometry.y || entry.block.id === changedBlockId) {
      resolvedById.set(entry.block.id, {
        ...entry.block,
        props: {
          ...(entry.block.props || {}),
          style: {
            ...(entry.block.props?.style || {}),
            pageFlowY: Math.round(nextY),
          },
        },
      })
    }
  }

  const sticky = changedBlockId
    ? entries.find(entry => entry.block.id === changedBlockId)
    : null
  const rest = entries
    .filter(entry => !sticky || entry.block.id !== sticky.block.id)
    .sort((left, right) => left.geometry.y - right.geometry.y
      || left.geometry.x - right.geometry.x
      || left.index - right.index)

  // 有主动块：先钉住落点，其它块绕开。无主动块：全量按 Y 下推避让。
  // 嵌入容器内的 children 不走这里。
  if (sticky)
    commit(sticky, sticky.geometry.y)
  rest.forEach((entry) => {
    commit(entry, pushPageFlowRectBelow(placed, entry.geometry, gap))
  })

  return items.map(block => resolvedById.get(block.id) || block)
}

/**
 * 缩放时把活动边吸附到其它根级块的边/中线，并返回应对齐标线。
 * @param {{ x:number, y:number, width:number, height:number }} rect
 * @param {Array<{ x:number, y:number, width:number, height:number }>} peers
 * @param {string} anchor top|bottom|left|right|corners…
 * @param {number} threshold
 */
export function snapResizeRectToPeers(rect = {}, peers = [], anchor = 'bottom-right', threshold = 6) {
  const snapPx = Math.max(1, Number(threshold) || 6)
  let x = Number(rect.x) || 0
  let y = Number(rect.y) || 0
  let width = Math.max(1, Number(rect.width) || 1)
  let height = Math.max(1, Number(rect.height) || 1)
  const resizeLeft = String(anchor).includes('left')
  const resizeRight = String(anchor).includes('right')
  const resizeTop = String(anchor).includes('top')
  const resizeBottom = String(anchor).includes('bottom')
  let bestX = null
  let bestY = null
  const movingEdgesX = []
  const movingEdgesY = []
  if (resizeLeft)
    movingEdgesX.push({ edge: 'left', value: x })
  if (resizeRight)
    movingEdgesX.push({ edge: 'right', value: x + width })
  if (!resizeLeft && !resizeRight)
    movingEdgesX.push({ edge: 'cx', value: x + width / 2 })
  if (resizeTop)
    movingEdgesY.push({ edge: 'top', value: y })
  if (resizeBottom)
    movingEdgesY.push({ edge: 'bottom', value: y + height })
  if (!resizeTop && !resizeBottom)
    movingEdgesY.push({ edge: 'cy', value: y + height / 2 })

  peers.forEach((peer) => {
    const left = Number(peer.x) || 0
    const top = Number(peer.y) || 0
    const right = left + (Number(peer.width) || 0)
    const bottom = top + (Number(peer.height) || 0)
    const cx = left + (right - left) / 2
    const cy = top + (bottom - top) / 2
    const targetsX = [left, right, cx]
    const targetsY = [top, bottom, cy]
    movingEdgesX.forEach((moving) => {
      targetsX.forEach((to) => {
        const delta = Math.abs(moving.value - to)
        if (delta > snapPx)
          return
        if (bestX && delta >= bestX.delta)
          return
        bestX = { delta, to: Math.round(to), edge: moving.edge }
      })
    })
    movingEdgesY.forEach((moving) => {
      targetsY.forEach((to) => {
        const delta = Math.abs(moving.value - to)
        if (delta > snapPx)
          return
        if (bestY && delta >= bestY.delta)
          return
        bestY = { delta, to: Math.round(to), edge: moving.edge }
      })
    })
  })

  if (bestX) {
    if (bestX.edge === 'left') {
      const right = x + width
      x = bestX.to
      width = Math.max(1, right - x)
    }
    else if (bestX.edge === 'right') {
      width = Math.max(1, bestX.to - x)
    }
  }
  if (bestY) {
    if (bestY.edge === 'top') {
      const bottom = y + height
      y = bestY.to
      height = Math.max(1, bottom - y)
    }
    else if (bestY.edge === 'bottom') {
      height = Math.max(1, bestY.to - y)
    }
  }

  return {
    x: Math.round(x),
    y: Math.round(y),
    width: Math.round(width),
    height: Math.round(height),
    lines: {
      x: bestX ? [bestX.to] : [],
      y: bestY ? [bestY.to] : [],
    },
  }
}

function isContainerGrowBlock(block = {}) {
  // 壳样式：仅按组件类型判定「内容撑开」；heightMode=auto 另有分支
  return ['grid-layout', 'card', 'panel-box', 'box-layout', 'tabs', 'AiForm', 'detail-info'].includes(block?.blockType)
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

  // 图表固定高度：历史脏数据 pageFlowHeight≈96 时抬升，避免画布溢出叠层。
  // 一旦 pageFlowHeight≥160（锚点拖过），必须以壳高为准，否则标线先动、壳高被 props.height+20 顶住缩不下去。
  if (
    heightMode === 'fixed'
    && ['bar-chart', 'line-chart', 'area-chart', 'pie-chart'].includes(block.blockType)
  ) {
    if (!(customHeight >= 160)) {
      const chartCanvas = Number(block.props?.height)
      resolvedHeight = Math.max(
        resolvedHeight,
        defaultHeight,
        Number.isFinite(chartCanvas) && chartCanvas > 120 ? Math.round(chartCanvas + 20) : 0,
      )
    }
  }

  // heightMode=fixed：锁死壳高（含卡片/面板），内部非固定高度子块才能跟着拉高
  // heightMode=auto：跟内容走；未显式 fixed/full 的容器历史数据仍按内容撑开
  const useAutoHeight = heightMode === 'auto'
    || (heightMode !== 'full' && heightMode !== 'fixed' && isContainerGrowBlock(block))

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
  // auto：不要吃 pageFlowWidth 里残留的 px（开属性面板时量到的窄宽），否则看起来像固定宽
  if (widthMode === 'auto')
    return { ...position, width: `min(${Math.max(280, Math.min(560, frameWidth || 520))}px, ${remainingSafeWidth})` }
  if (widthMode === 'fixed') {
    const rawWidth = readPageBlockLength(customWidth) || frameWidth || 0
    const floored = isListCrud
      ? Math.max(rawWidth, FREE_LAYOUT_CRUD_MIN_WIDTH)
      : rawWidth
    // 固定宽用精确 px，避免 min(px, calc%) 实宽落后于对齐标线
    if (floored > 0)
      return { ...position, width: `${floored}px`, maxWidth: remainingSafeWidth }
  }
  const columns = Math.min(12, Math.max(3, Number(meta.defaultW) || 6))
  return {
    ...position,
    width: customWidth || `min(${Math.round((columns / 12) * 10000) / 100}%, ${remainingSafeWidth})`,
  }
}
