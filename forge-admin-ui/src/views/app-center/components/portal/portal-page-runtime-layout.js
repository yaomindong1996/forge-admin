import { resolveCrudFormOnly } from '@/components/lowcode-builder/shared/runtime-crud-page-mode'
import { isDataFieldBlockType } from '@/components/lowcode-builder/page/page-schema'

/** 介绍/模板残留的未改文案占位；对象数据页上不应盖住 CRUD。 */
export const DEFAULT_INFO_PANEL_PLACEHOLDER = Object.freeze({
  title: '提示信息',
  content: '在这里展示当前页面的说明、风险提醒或操作结果。',
})

export const DEFAULT_EMPTY_STATE_PLACEHOLDER = Object.freeze({
  title: '暂无数据',
  description: '当前条件下没有可展示的数据',
})

export function isRuntimeAutoHeightBlock(block = {}) {
  if (['AiForm', 'workspace-summary-metrics', 'info-panel', 'empty-state', 'page-title'].includes(block?.blockType))
    return true
  if (block?.blockType !== 'AiCrudPage')
    return false
  return resolveCrudFormOnly(block.props)
}

/** 根级通栏块：运行/设计都走文档流纵向堆叠，避免绝对定位在换行增高时互相覆盖 */
export function shouldUsePageFlowStack(blocks = [], options = {}) {
  const items = Array.isArray(blocks) ? blocks.filter(Boolean) : []
  if (!items.length)
    return false
  // 个人工作台固定文档流，不因某个块被改成固定宽又退回绝对定位
  if (options.forceStack || String(options.pageId || '').trim() === 'system:workbench')
    return true
  // 全是内容自适应块（统计/提示/空状态等）也一律堆叠
  if (items.every(isRuntimeAutoHeightBlock))
    return true
  const allFull = items.every((block) => {
    const widthMode = block?.props?.style?.widthMode || 'full'
    return widthMode === 'full'
  })
  if (allFull)
    return true
  // 单独一块定宽且未横向偏移：保持文档流，避免插图表拖窄时整页跳成绝对定位
  if (items.length === 1) {
    const block = items[0]
    if (['AiCrudPage', 'AiTable', 'data-table'].includes(block?.blockType))
      return false
    const widthMode = block?.props?.style?.widthMode || 'full'
    const x = Number(block?.props?.style?.pageFlowX)
    return widthMode === 'fixed' && (!Number.isFinite(x) || x <= 0)
  }
  return false
}

export function shouldUseContentSizedFlow(blocks = [], options = {}) {
  const items = Array.isArray(blocks) ? blocks.filter(Boolean) : []
  if (!items.length)
    return false
  // 自由布局（存在非通栏块）保留绝对定位；fillHost / runtimePreview 不得压成文档流，
  // 否则预览里 fixed 宽 CRUD 变成左上角窄框、其它块纵向堆叠。
  const freeLayout = !shouldUsePageFlowStack(items, options)
    && !items.every(isRuntimeAutoHeightBlock)
  if (freeLayout)
    return false
  // fillHost：单块继续 100% 填满宿主；多块通栏必须文档流堆叠。
  // 若仍走绝对定位，外层 overflow:hidden 会把列表底（分页/末行）裁掉，和列表设计预览不一致。
  if (options.fillHost)
    return items.length > 1
  if (shouldUsePageFlowStack(items, options))
    return true
  return items.every(isRuntimeAutoHeightBlock)
}

export function sortBlocksByPageFlowY(blocks = []) {
  return [...(Array.isArray(blocks) ? blocks : [])].sort((left, right) => {
    const leftY = Number(left?.props?.style?.pageFlowY)
    const rightY = Number(right?.props?.style?.pageFlowY)
    const safeLeft = Number.isFinite(leftY) ? leftY : 0
    const safeRight = Number.isFinite(rightY) ? rightY : 0
    if (safeLeft !== safeRight)
      return safeLeft - safeRight
    return String(left?.id || '').localeCompare(String(right?.id || ''))
  })
}

export function isDefaultInfoPanelPlaceholder(block = {}) {
  if (block?.blockType !== 'info-panel')
    return false
  const title = String(block?.props?.title ?? DEFAULT_INFO_PANEL_PLACEHOLDER.title).trim()
    || DEFAULT_INFO_PANEL_PLACEHOLDER.title
  const content = String(block?.props?.content || '').trim()
  return title === DEFAULT_INFO_PANEL_PLACEHOLDER.title
    && content === DEFAULT_INFO_PANEL_PLACEHOLDER.content
}

export function isDefaultEmptyStatePlaceholder(block = {}) {
  if (block?.blockType !== 'empty-state')
    return false
  const title = String(block?.props?.title ?? DEFAULT_EMPTY_STATE_PLACEHOLDER.title).trim()
    || DEFAULT_EMPTY_STATE_PLACEHOLDER.title
  const description = String(block?.props?.description || '').trim()
  return title === DEFAULT_EMPTY_STATE_PLACEHOLDER.title
    && description === DEFAULT_EMPTY_STATE_PLACEHOLDER.description
}

/** 对象页上未定制过的装饰占位（提示面板 / 空状态），会叠在 CRUD 上层误导用户。 */
export function isUntouchedDecorativePlaceholder(block = {}) {
  return isDefaultInfoPanelPlaceholder(block) || isDefaultEmptyStatePlaceholder(block)
}

/** 市场包/旧设计稿常用别名 → 运行态正式 blockType */
export const LEGACY_PORTAL_BLOCK_TYPES = Object.freeze({
  intro: 'page-title',
  'metric-card': 'stats-strip',
  'business-list': 'AiCrudPage',
  'business-form': 'AiForm',
  todo: 'info-panel',
  chart: 'stats-strip',
  text: 'custom-html',
  image: 'info-panel',
  columns: 'grid-layout',
  list: 'info-panel',
})

/**
 * 将 intro / metric-card 等别名映射为运行态可渲染类型，并补齐必要 props。
 * gridLayout.items 与旧 layout.items 都要走这里，否则门户会显示「未知区块类型」。
 */
export function normalizePortalLegacyBlock(item = {}, index = 0) {
  const rawType = String(item?.blockType || item?.type || '').trim()
  const blockType = LEGACY_PORTAL_BLOCK_TYPES[rawType] || rawType || 'custom-html'
  const props = { ...(item?.props || {}) }
  if (rawType === 'metric-card' && !Array.isArray(props.metrics)) {
    props.metrics = [{
      label: props.title || props.label || '指标',
      value: props.value ?? '-',
      trend: props.trend || '',
    }]
  }
  if (rawType === 'intro') {
    if (!props.subtitle && props.description)
      props.subtitle = props.description
  }
  if (rawType === 'text' && !props.content && props.text)
    props.content = props.text
  return {
    ...item,
    id: item?.id || `legacy-${index + 1}`,
    blockType,
    props,
  }
}

/**
 * 解析门户页应渲染的区块。
 * - 优先 gridLayout.items；否则回退旧 layout.items
 * - 对象页若只有装饰块或没有数据块，补一个 AiCrudPage
 * - 对象页过滤未改过的提示面板 / 空状态占位，避免盖住列表
 * - 页面标题（page-title）是用户可设计的组件，预览/运行必须保留；工作台标题在外层 chrome，仅工作台页去掉画布内标题防重复
 */
export function resolvePortalPageBlocks({
  page,
  node,
  resolveObjectRef,
  normalizeLegacyBlock,
} = {}) {
  const layout = page?.layout || {}
  const normalize = typeof normalizeLegacyBlock === 'function'
    ? normalizeLegacyBlock
    : normalizePortalLegacyBlock
  const gridItems = Array.isArray(layout.gridLayout?.items)
    ? layout.gridLayout.items.map((item, index) => normalize(item, index))
    : null
  const legacyItems = Array.isArray(layout.items)
    ? layout.items.map((item, index) => normalize(item, index))
    : []
  const rawItems = (gridItems && gridItems.length > 0) ? gridItems : legacyItems
  let items = Array.isArray(rawItems) ? [...rawItems] : []

  // 个人工作台：标题已在外层 summary-head，不重复渲染历史画布 page-title
  const pageId = String(node?.id || page?.id || '').trim()
  if (pageId === 'system:workbench')
    items = items.filter(item => item?.blockType !== 'page-title')

  const objectRef = typeof resolveObjectRef === 'function' ? resolveObjectRef(node || {}) : null
  const isObjectPage = node?.pageType === 'object' && Boolean(objectRef)
  if (isObjectPage) {
    items = items.filter(item => !isUntouchedDecorativePlaceholder(item))
    const hasDataBlock = items.some(item => isDataFieldBlockType(item?.blockType))
    if (!hasDataBlock) {
      items = [{
        id: `portal-object-${node.id}`,
        blockType: 'AiCrudPage',
        props: {
          objectRef,
          style: { widthMode: 'full', heightMode: 'full', pageFlowHeight: 640 },
        },
      }, ...items]
    }
  }
  return items
}
