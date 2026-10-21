import { createGridBlock } from '@/components/lowcode-builder/page/page-schema'
import { PAGE_MANAGEMENT_SYSTEM_PAGES } from './page-management'

/** 应用内「个人工作台」自由布局页 ID（与系统页 system:workbench 对齐） */
export const WORKBENCH_PAGE_ID = 'system:workbench'

/** 仅含早期默认布局时可升级；用户手动改过的页面不覆盖 */
export const WORKBENCH_LAYOUT_VERSION = 4

export function isWorkbenchPageId(pageId = '') {
  return String(pageId || '').trim() === WORKBENCH_PAGE_ID
}

export function resolveWorkbenchSystemPage() {
  return PAGE_MANAGEMENT_SYSTEM_PAGES.find(item => item.id === WORKBENCH_PAGE_ID) || {
    id: WORKBENCH_PAGE_ID,
    title: '个人工作台',
    view: 'workbench',
  }
}

export function createWorkbenchVirtualNode() {
  const meta = resolveWorkbenchSystemPage()
  return {
    id: WORKBENCH_PAGE_ID,
    type: 'page',
    title: meta.title || '个人工作台',
    pageType: 'content',
    pageTemplate: 'custom',
    pageShape: 'custom',
    parentId: null,
    navigationVisible: false,
    systemView: 'workbench',
    objectRef: null,
  }
}

function withFlowStyle(block, { x = 24, y = 20, height = 160, heightMode = 'auto' } = {}) {
  return {
    ...block,
    props: {
      ...(block.props || {}),
      style: {
        ...(block.props?.style || {}),
        widthMode: 'full',
        heightMode,
        pageFlowX: x,
        pageFlowY: y,
        pageFlowHeight: height,
        height,
      },
    },
  }
}

/**
 * 所有应用通用的个人工作台默认布局：
 * 待办统计 → 使用提示（不再塞空状态占位，避免「一片空白」观感）
 * 页面标题用外层 summary-head，画布内不再放 page-title，避免重复。
 */
export function createDefaultWorkbenchBlocks() {
  const metrics = withFlowStyle(
    createGridBlock('workspace-summary-metrics', null, { gridX: 0, gridY: 0 }),
    { y: 20, height: 200, heightMode: 'auto' },
  )
  metrics.props = {
    ...metrics.props,
    visibleKeys: ['todo', 'done', 'started', 'cc'],
    columns: 4,
    showHeader: false,
  }

  const tip = withFlowStyle(
    createGridBlock('info-panel', null, { gridX: 0, gridY: 2 }),
    { y: 236, height: 96, heightMode: 'auto' },
  )
  tip.props = {
    ...tip.props,
    title: '使用提示',
    content: '点击上方统计卡片可进入对应列表。也可从左侧导航打开「我的待办 / 我已办的 / 我发送的 / 抄送我的」。',
    type: 'info',
  }

  return [metrics, tip]
}

export function createDefaultWorkbenchPage(title = '个人工作台') {
  return {
    title,
    pageType: 'content',
    layout: {
      workbenchLayoutVersion: WORKBENCH_LAYOUT_VERSION,
      gridLayout: {
        cols: 12,
        rowHeight: 32,
        gap: 8,
        designWidth: 1366,
        layoutType: 'simple-crud',
        items: createDefaultWorkbenchBlocks(),
      },
    },
  }
}

function isUpgradeableDefaultWorkbenchPage(page = null) {
  const items = page?.layout?.gridLayout?.items
  if (!Array.isArray(items) || !items.length)
    return true
  const version = Number(page?.layout?.workbenchLayoutVersion || 0)
  if (version >= WORKBENCH_LAYOUT_VERSION)
    return false
  const types = items.map(item => String(item?.blockType || ''))
  // 早期「单块矮统计」默认布局
  if (items.length === 1 && types[0] === 'workspace-summary-metrics') {
    const height = Number(items[0]?.props?.style?.pageFlowHeight || items[0]?.props?.style?.height || 0)
    return !Number.isFinite(height) || height <= 160 || version < WORKBENCH_LAYOUT_VERSION
  }
  // v3 默认：统计 + 提示 + 空状态；可安全换成 v4（去掉空壳占位）
  if (types.includes('workspace-summary-metrics') && types.includes('empty-state')) {
    const allowed = new Set(['workspace-summary-metrics', 'info-panel', 'empty-state', 'page-title'])
    return types.every(type => allowed.has(type))
  }
  return false
}

function bumpMetricsBlockHeight(items = []) {
  return items.map((item) => {
    if (item?.blockType !== 'workspace-summary-metrics')
      return item
    const style = item.props?.style || {}
    const height = Number(style.pageFlowHeight || style.height || 0)
    if (Number.isFinite(height) && height >= 200 && style.heightMode === 'auto')
      return item
    return {
      ...item,
      props: {
        ...(item.props || {}),
        style: {
          ...style,
          heightMode: 'auto',
          pageFlowHeight: Math.max(200, height || 200),
          height: Math.max(200, height || 200),
        },
      },
    }
  })
}

/** 去掉画布内默认 page-title 后，把剩余块整体上移，消除标题留下的空白 */
function compactWorkbenchFlowY(items = []) {
  const ys = items
    .map(item => Number(item?.props?.style?.pageFlowY))
    .filter(y => Number.isFinite(y) && y >= 0)
  if (!ys.length)
    return items
  const minY = Math.min(...ys)
  const targetTop = 20
  if (minY <= targetTop)
    return items
  const delta = minY - targetTop
  return items.map((item) => {
    const style = item?.props?.style || {}
    const y = Number(style.pageFlowY)
    if (!Number.isFinite(y))
      return item
    return {
      ...item,
      props: {
        ...(item.props || {}),
        style: {
          ...style,
          pageFlowY: Math.max(targetTop, Math.round(y - delta)),
        },
      },
    }
  })
}

function stripDefaultWorkbenchPageTitle(items = []) {
  const first = items[0]
  if (first?.blockType !== 'page-title')
    return compactWorkbenchFlowY(items)
  const title = String(first.props?.title || '').trim()
  if (title && title !== '个人工作台')
    return items
  return compactWorkbenchFlowY(items.slice(1))
}

/**
 * 确保 builder.pages 中存在个人工作台自由布局页。
 * 不写入 nodes（系统页仍由 PAGE_MANAGEMENT_SYSTEM_PAGES 固定展示）。
 */
export function ensureWorkbenchPageInBuilder(schema = {}) {
  const pages = schema.pages && typeof schema.pages === 'object' ? { ...schema.pages } : {}
  const existing = pages[WORKBENCH_PAGE_ID]
  const meta = resolveWorkbenchSystemPage()

  if (isUpgradeableDefaultWorkbenchPage(existing)) {
    const page = createDefaultWorkbenchPage(existing?.title || meta.title || '个人工作台')
    pages[WORKBENCH_PAGE_ID] = {
      ...page,
      title: existing?.title || page.title,
    }
    return {
      schema: { ...schema, pages },
      created: !existing,
      upgraded: Boolean(existing),
      page: pages[WORKBENCH_PAGE_ID],
    }
  }

  const items = existing.layout.gridLayout.items
  const nextItems = stripDefaultWorkbenchPageTitle(bumpMetricsBlockHeight(items))
  const heightChanged = JSON.stringify(nextItems) !== JSON.stringify(items)
  pages[WORKBENCH_PAGE_ID] = {
    ...existing,
    layout: {
      ...(existing.layout || {}),
      workbenchLayoutVersion: Math.max(
        Number(existing.layout?.workbenchLayoutVersion || 0),
        WORKBENCH_LAYOUT_VERSION,
      ),
      gridLayout: {
        ...existing.layout.gridLayout,
        items: nextItems,
      },
    },
  }
  return {
    schema: { ...schema, pages },
    created: false,
    upgraded: heightChanged,
    page: pages[WORKBENCH_PAGE_ID],
  }
}

export function resolveWorkbenchPage(schema = {}) {
  const ensured = ensureWorkbenchPageInBuilder(schema)
  return ensured.page
}
