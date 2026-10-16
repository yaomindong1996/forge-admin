/**
 * 列表自由布局（listGridLayout / pages[].gridLayout）在运行态的解析与判定。
 *
 * 标准列表入口历史上只渲 AiCrudPage，把 listGridLayout 当设计元数据；
 * 当画布上还有树面板、标题、提示等伴生块时，门户改走 RuntimeListGridFlow（纵向文档流），
 * 禁止嵌套完整 ListPageGridDesigner，避免设计态画布样式泄漏。
 */

import {
  LIST_PAGE_DESIGN_WIDTH,
  LIST_PAGE_GRID_COLS,
  LIST_PAGE_GRID_GAP,
  LIST_PAGE_GRID_ROW_HEIGHT,
} from '../page/page-schema-catalog.js'

const CRUD_ONLY_BLOCK_TYPES = new Set(['AiCrudPage', 'AiTable', 'data-table'])

function resolveStyleNumber(value, fallback = 0) {
  if (value === null || value === undefined || value === '' || value === '100%' || value === 'auto')
    return Math.round(fallback)
  if (typeof value === 'number')
    return Math.round(value)
  const num = Number(String(value).trim().replace('px', ''))
  return Number.isFinite(num) ? Math.round(num) : Math.round(fallback)
}

/** 非 list 画布空壳：自定义/弹窗页尚未拖入组件时仍走 RuntimeListGridFlow，避免回退裸 AiCrudPage */
function createEmptyRuntimeListLayout(pageSchema = {}, fallbackLayoutType = 'simple-crud', base = null) {
  const source = base && typeof base === 'object' ? base : {}
  return {
    cols: Number(source.cols) > 0 ? Number(source.cols) : 12,
    rowHeight: Number(source.rowHeight) > 0 ? Number(source.rowHeight) : 32,
    gap: Number.isFinite(Number(source.gap)) ? Number(source.gap) : 8,
    ...source,
    items: Array.isArray(source.items) ? source.items : [],
    layoutType: source.layoutType
      || pageSchema?.layoutType
      || fallbackLayoutType
      || 'simple-crud',
  }
}

export function resolveListGridLayoutFromPageSchema(pageSchema = {}, { pageKey = 'list' } = {}) {
  const key = String(pageKey || 'list').trim() || 'list'
  const pages = Array.isArray(pageSchema?.pages) ? pageSchema.pages : []
  const page = pages.find(item => String(item?.pageKey || '').trim() === key) || null
  if (page?.gridLayout && Array.isArray(page.gridLayout.items))
    return page.gridLayout
  // pages[] 已登记的非 list 页：无 gridLayout 或 items 缺失时仍返回空壳，禁止回退列表 CRUD
  if (key !== 'list' && page)
    return createEmptyRuntimeListLayout(pageSchema, pageSchema?.layoutType || 'simple-crud', page.gridLayout)
  if (key !== 'list')
    return null
  const layout = pageSchema?.listGridLayout
  if (!layout || !Array.isArray(layout.items))
    return null
  return layout
}

export function normalizeRuntimeListGridLayout(
  layout = null,
  pageSchema = {},
  fallbackLayoutType = 'simple-crud',
  { allowEmpty = false, forceLivePreview = true } = {},
) {
  if (!layout || !Array.isArray(layout.items))
    return null
  if (!layout.items.length && !allowEmpty)
    return null
  const items = layout.items.map(item => normalizeFlowBlockItem(item, { forceLivePreview }))
  return {
    ...layout,
    items,
    layoutType: layout.layoutType
      || pageSchema?.layoutType
      || fallbackLayoutType
      || 'simple-crud',
  }
}

/**
 * 去掉设计态绝对定位尺寸，改为运行态通栏文档流可用的块配置。
 * @param {object} [options]
 * @param {boolean} [options.forceLivePreview=true] 门户运行态强制实时数据；列表设计器内预览应传 false
 */
export function normalizeFlowBlockItem(item = {}, options = {}) {
  if (!item || typeof item !== 'object')
    return item
  const forceLivePreview = options.forceLivePreview !== false
  const style = { ...(item.props?.style || {}) }
  const isCrud = item.blockType === 'AiCrudPage'
  // heightMode=full / 设计态固定 px 高会在门户里裁切下方组件
  style.heightMode = 'auto'
  style.widthMode = 'full'
  delete style.width
  delete style.height
  delete style.minWidth
  delete style.maxWidth
  const nextProps = {
    ...(item.props || {}),
    style,
  }
  if (isCrud) {
    if (forceLivePreview) {
      nextProps.previewLiveData = true
      nextProps.previewMode = 'realList'
    }
    else {
      // 设计器内预览：不要把画布静态壳强行改成实时列表（否则查询字段/工具栏与设计态不一致）
      delete nextProps.previewLiveData
      delete nextProps.previewMode
    }
  }
  return {
    ...item,
    props: nextProps,
  }
}

/**
 * 与设计态画布 resolveBlockFrame / resolveSchemaItemFrame 对齐：
 * pageFlowY > style.y > gridY×行高；禁止裸 Number(style.y)（空串会变成 0 把块顶到最上）。
 */
function resolveFlowSortY(item = {}, layout = null) {
  const pageFlowY = Number(item?.props?.style?.pageFlowY)
  if (Number.isFinite(pageFlowY))
    return pageFlowY
  const rowH = Number(layout?.rowHeight) > 0 ? Number(layout.rowHeight) : LIST_PAGE_GRID_ROW_HEIGHT
  const gap = Number.isFinite(Number(layout?.gap)) ? Number(layout.gap) : LIST_PAGE_GRID_GAP
  const fallbackY = Number(item?.gridY || 0) * (rowH + gap)
  const style = item?.props?.style || {}
  return resolveStyleNumber(style.y ?? style.top, fallbackY)
}

function resolveFlowSortX(item = {}, layout = null) {
  const pageFlowX = Number(item?.props?.style?.pageFlowX)
  if (Number.isFinite(pageFlowX))
    return pageFlowX
  const designWidth = Number(layout?.designWidth) > 0 ? Number(layout.designWidth) : LIST_PAGE_DESIGN_WIDTH
  const gap = Number.isFinite(Number(layout?.gap)) ? Number(layout.gap) : LIST_PAGE_GRID_GAP
  const colWidth = Math.floor((designWidth - (LIST_PAGE_GRID_COLS - 1) * gap) / LIST_PAGE_GRID_COLS)
  const fallbackX = Number(item?.gridX || 0) * (colWidth + gap)
  const style = item?.props?.style || {}
  return resolveStyleNumber(style.x ?? style.left, fallbackX)
}

export function buildRuntimeListFlowBlocks(layout = null, options = {}) {
  const items = Array.isArray(layout?.items) ? layout.items.filter(Boolean) : []
  return [...items]
    .map(item => normalizeFlowBlockItem(item, options))
    .sort((left, right) => {
      const leftY = resolveFlowSortY(left, layout)
      const rightY = resolveFlowSortY(right, layout)
      if (leftY !== rightY)
        return leftY - rightY
      const leftX = resolveFlowSortX(left, layout)
      const rightX = resolveFlowSortX(right, layout)
      if (leftX !== rightX)
        return leftX - rightX
      return String(left?.id || '').localeCompare(String(right?.id || ''))
    })
}

/** 画布上是否有 CRUD 以外的可见伴生块（树、标题、提示、统计等） */
export function isRichListGridLayout(layout = null) {
  const items = Array.isArray(layout?.items) ? layout.items.filter(Boolean) : []
  if (items.length <= 1)
    return false
  return items.some(item => item?.blockType && !CRUD_ONLY_BLOCK_TYPES.has(item.blockType))
}

export function shouldRenderRuntimeListGrid({
  pageSchema = null,
  modelSchema = null,
  layoutType = '',
  formOnly = false,
  pageKey = 'list',
} = {}) {
  if (formOnly || !pageSchema)
    return false
  // 非 list 页（详情/弹窗/自定义页）有登记就渲（含空壳）；list 页仅在有伴生块时改走自由布局
  const key = String(pageKey || 'list').trim() || 'list'
  const allowEmpty = key !== 'list'
  const raw = resolveListGridLayoutFromPageSchema(pageSchema, { pageKey })
  const normalized = normalizeRuntimeListGridLayout(
    raw,
    pageSchema,
    layoutType || pageSchema?.layoutType || 'simple-crud',
    { allowEmpty },
  )
  if (!normalized)
    return false
  if (allowEmpty)
    return true
  return isRichListGridLayout(normalized)
}

export function resolveRuntimeListGridModel(pageSchema = null, {
  modelSchema = null,
  layoutType = '',
  pageKey = 'list',
  forceLivePreview = true,
} = {}) {
  const key = String(pageKey || 'list').trim() || 'list'
  const allowEmpty = key !== 'list'
  const raw = resolveListGridLayoutFromPageSchema(pageSchema || {}, { pageKey })
  const layout = normalizeRuntimeListGridLayout(
    raw,
    pageSchema || {},
    layoutType || pageSchema?.layoutType || 'simple-crud',
    { allowEmpty, forceLivePreview },
  )
  if (!layout)
    return null
  const fields = Array.isArray(modelSchema?.fields)
    ? modelSchema.fields.map(field => ({
        ...field,
        field: field.field || field.fieldCode || field.columnName || '',
        label: field.label || field.fieldName || field.field || field.fieldCode || field.columnName || '',
        componentType: field.componentType || field.componentKey || field.dataType || 'input',
      })).filter(field => field.field)
    : []
  return {
    layout: {
      ...layout,
      items: buildRuntimeListFlowBlocks(layout),
    },
    fields,
    layoutType: layout.layoutType || layoutType || pageSchema?.layoutType || 'simple-crud',
    modelSchema: modelSchema || {},
  }
}
