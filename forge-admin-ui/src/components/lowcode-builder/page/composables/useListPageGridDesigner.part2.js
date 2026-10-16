/** Auto-split part 2 of ListPageGridDesigner setup. */
import { computed, nextTick } from 'vue'
import { businessObjectList } from '@/api/business-app'
import { colorToHexInput, hexInputToColor } from '@/utils/designer-color'
import { mapBlocksInTree } from '../blockTree'
import { expandPanelTypeOptions } from '../listDesignerOptions'
import {
  LIST_PAGE_GRID_COLS,
  buildGridSyncModelSchema,
  resolveListPageBlockMeta,
  syncGridLayoutWithModel,
} from '../page-schema'
export function useListPageGridDesignerPart2(props, emit, deps = {}) {
  const {
    __impl,
    mut,
    designerStore,
    rowHeight,
    gap,
    TREE_PANEL_COLLAPSED_WIDTH,
    canvasScrollRef,
    selectedBlockId,
    sourceModalTab,
    layoutSourceDraft,
    blockSourceDraft,
    sourceError,
    propertyCollapsed,
    canvasDragActive,
    draggedBlockType,
    draggedExistingBlockId,
    movingBlockId,
    movingPixelOffset,
    canvasZoom,
    localLayout,
    blocks,
    runtimeTreeFilter,
    runtimeTreeActiveKey,
    collapsedTreePanelMap,
    colWidth,
    canvasGridWidth,
    canvasGridHeight,
    collapsedTreeFrames,
    selectedBlock,
    selectedBlockStyle,
    treeSourceCatalog,
    treeSourceLoading,
    currentListObjectCodes,
    crudTablePanelFields,
    cancelSourceModalEdit,
  } = deps
  const clamp = (...args) => __impl.clamp(...args)
  const duplicateBlock = (...args) => __impl.duplicateBlock(...args)
  const normalizeDesignerLayout = (...args) => __impl.normalizeDesignerLayout(...args)
  const normalizeGridItems = (...args) => __impl.normalizeGridItems(...args)
  const patchBlock = (...args) => __impl.patchBlock(...args)
  const patchBlockProps = (...args) => __impl.patchBlockProps(...args)
  const patchBlockStyle = (...args) => __impl.patchBlockStyle(...args)
  const removeBlock = (...args) => __impl.removeBlock(...args)
  const reorderBlockLayer = (...args) => __impl.reorderBlockLayer(...args)
  const resolveCrudFieldKey = (...args) => __impl.resolveCrudFieldKey(...args)
  const resolveCrudFieldLabel = (...args) => __impl.resolveCrudFieldLabel(...args)
function applySourceModalCode() {
  const applied = sourceModalTab.value === 'block' && selectedBlock.value
    ? applyBlockSourceCode()
    : applyLayoutSourceCode()
  if (applied)
    cancelSourceModalEdit()
}

function applyLayoutSourceCode() {
  try {
    const parsed = JSON.parse(layoutSourceDraft.value || '{}')
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed))
      throw new Error('画布布局 JSON 必须是对象')
    localLayout.value = normalizeDesignerLayout(syncGridLayoutWithModel(parsed, buildGridSyncModelSchema(props.modelSchema, props.fields), { layoutType: props.layoutType }))
    sourceError.value = ''
    return true
  }
  catch (error) {
    sourceError.value = error?.message || 'JSON 解析失败'
    return false
  }
}

function applyBlockSourceCode() {
  if (!selectedBlock.value) {
    sourceError.value = '请先选中一个区块'
    return false
  }
  try {
    const parsed = JSON.parse(blockSourceDraft.value || '{}')
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed))
      throw new Error('当前区块 JSON 必须是对象')
    const targetId = selectedBlock.value.id
    if (parsed.id && parsed.id !== targetId)
      throw new Error('当前区块 JSON 的 id 不能改成其他区块')
    const replacement = {
      ...parsed,
      id: targetId,
    }
    localLayout.value = {
      ...localLayout.value,
      items: normalizeGridItems(mapBlocksInTree(blocks.value, block => block.id === targetId ? replacement : block)),
    }
    sourceError.value = ''
    return true
  }
  catch (error) {
    sourceError.value = error?.message || 'JSON 解析失败'
    return false
  }
}

function resolveBlockStyle(block) {
  const componentStyle = block.props?.style || {}
  const rect = resolveRuntimeBlockFrame(block, resolveBlockFrame(block))
  const widthMode = resolveBlockWidthMode(block)
  const heightMode = resolveBlockHeightMode(block)
  const collapsedTree = isTreePanelCollapsed(block)
  const style = {
    left: `${rect.x}px`,
    top: `${rect.y}px`,
    minWidth: clampMaxCssSize(resolveAbsoluteCssSize(componentStyle.minWidth), Math.max(0, canvasGridWidth.value - rect.x)),
    maxWidth: resolveAbsoluteCssSize(componentStyle.maxWidth),
    minHeight: resolveAbsoluteCssSize(componentStyle.minHeight),
    maxHeight: resolveAbsoluteCssSize(componentStyle.maxHeight),
    textAlign: componentStyle.textAlign || block.props?.textAlign || block.props?.align || 'left',
  }
  if (collapsedTree)
    style.width = `${rect.width}px`
  else if (widthMode === 'full')
    style.right = '0'
  else
    style.width = `${rect.width}px`
  if (heightMode === 'full')
    style.bottom = '0'
  else if (heightMode === 'auto')
    style.height = 'auto'
  else
    style.height = `${rect.height}px`
  if (movingBlockId.value === block.id) {
    style.transform = `translate3d(${movingPixelOffset.value.x}px, ${movingPixelOffset.value.y}px, 0) scale(0.995)`
  }
  // 预览态：非主列表块抬高层级，避免被 AiCrudPage 大块遮挡
  if (props.readonly && !['AiCrudPage', 'AiTable', 'data-table', 'tree-panel'].includes(block.blockType))
    style.zIndex = 3
  else if (props.readonly && ['AiCrudPage', 'AiTable', 'data-table'].includes(block.blockType))
    style.zIndex = 1
  return style
}

/** 选中且可 resize 的顶层区块（锚点渲染在画布层的数据源） */
const selectedResizeBlock = computed(() => {
  if (props.readonly || !selectedBlockId.value)
    return null
  return blocks.value.find(block => block.id === selectedBlockId.value) || null
})

/**
 * 画布层锚点定位：按选中块 rect 计算画布坐标系位置（canvas-grid 自带 scale，
 * 子元素用未缩放像素即可跟随缩放）。高度“填充容器”时视觉高度拉伸到画布底，
 * 底部锚点需用拉伸后的高度定位。
 */
function resolveCanvasAnchorStyle(anchor) {
  const block = selectedResizeBlock.value
  if (!block)
    return null
  const rect = resolveRuntimeBlockFrame(block, resolveBlockFrame(block))
  const half = 5
  const isFullHeight = resolveBlockHeightMode(block) === 'full'
  const visualHeight = isFullHeight
    ? Math.max(rect.height, canvasGridHeight.value - rect.y)
    : rect.height
  let left
  let top
  if (anchor.endsWith('left'))
    left = rect.x - half
  else if (anchor.endsWith('right'))
    left = rect.x + rect.width - half
  else
    left = rect.x + rect.width / 2 - half
  if (anchor.startsWith('top'))
    top = rect.y - half
  else if (anchor.startsWith('bottom'))
    top = rect.y + visualHeight - half
  else
    top = rect.y + visualHeight / 2 - half
  const style = { left: `${left}px`, top: `${top}px` }
  if (movingBlockId.value === block.id)
    style.transform = `translate3d(${movingPixelOffset.value.x}px, ${movingPixelOffset.value.y}px, 0)`
  return style
}

function resolveRuntimeBlockFrame(block, rect) {
  if (!collapsedTreeFrames.value.length)
    return rect
  const next = { ...rect }
  for (const { block: treeBlock, rect: treeRect } of collapsedTreeFrames.value) {
    const delta = Math.max(0, treeRect.width - TREE_PANEL_COLLAPSED_WIDTH)
    if (!delta)
      continue
    if (block.id === treeBlock.id) {
      next.width = TREE_PANEL_COLLAPSED_WIDTH
      continue
    }
    if (!isVerticalFrameOverlap(next, treeRect))
      continue
    const originalTreeRight = treeRect.x + treeRect.width
    if (next.x >= originalTreeRight - 1) {
      next.x -= delta
      next.width += delta
    }
  }
  return next
}

function isTreePanelCollapsed(block = {}) {
  return block.blockType === 'tree-panel' && collapsedTreePanelMap.value[block.id]
}

function isVerticalFrameOverlap(a, b) {
  const aTop = Number(a.y) || 0
  const bTop = Number(b.y) || 0
  return aTop < bTop + (Number(b.height) || 0) && bTop < aTop + (Number(a.height) || 0)
}

function resolveBlockFrame(block = {}) {
  const componentStyle = block.props?.style || {}
  const fallbackX = Number(block.gridX || 0) * (colWidth.value + gap)
  const fallbackY = Number(block.gridY || 0) * (rowHeight + gap)
  const fallbackWidth = gridWidthToPixels(block.gridW || 1)
  const fallbackHeight = gridHeightToPixels(block.gridH || 1)
  // 自由布局画布以 pageFlow* 为准；属性面板必须同源读取，否则改 X/宽高看起来「没生效」
  const flowX = Number(componentStyle.pageFlowX)
  const flowY = Number(componentStyle.pageFlowY)
  const flowH = Number(componentStyle.pageFlowHeight)
  let x = props.panelOnly && Number.isFinite(flowX) && flowX >= 0
    ? flowX
    : resolveCssNumber(componentStyle.x ?? componentStyle.left, fallbackX)
  let y = props.panelOnly && Number.isFinite(flowY) && flowY >= 0
    ? flowY
    : resolveCssNumber(componentStyle.y ?? componentStyle.top, fallbackY)
  const widthMode = resolveBlockWidthMode(block)
  const flowWidth = props.panelOnly
    ? resolveCssNumber(componentStyle.pageFlowWidth, NaN)
    : NaN
  let width = Math.max(
    24,
    Number.isFinite(flowWidth)
      ? flowWidth
      : resolveFrameWidth(componentStyle.width, widthMode, x, fallbackWidth),
  )
  const height = Math.max(
    24,
    props.panelOnly && Number.isFinite(flowH) && flowH > 0
      ? flowH
      : resolveCssNumber(componentStyle.height, fallbackHeight),
  )
  // 组件不允许超出画布：存量数据、属性面板输入过大宽度/偏移时统一兜底（嵌套子块走独立样式不受影响）
  const canvasWidth = canvasGridWidth.value
  x = Math.max(0, Math.min(x, Math.max(0, canvasWidth - 24)))
  if (x + width > canvasWidth)
    width = Math.max(24, canvasWidth - x)
  return { x, y, width, height }
}
__impl.resolveBlockFrame = resolveBlockFrame

function resolveBlockWidthMode(block = {}) {
  const style = block.props?.style || {}
  if (['full', 'auto', 'fixed'].includes(style.widthMode))
    return style.widthMode
  if (style.width === 'auto')
    return 'auto'
  if (style.width === '100%' || style.width === '' || style.width === undefined || style.width === null)
    return 'full'
  return 'fixed'
}
__impl.resolveBlockWidthMode = resolveBlockWidthMode

function resolveBlockHeightMode(block = {}) {
  const mode = block.props?.style?.heightMode
  const normalized = ['fixed', 'auto', 'full'].includes(mode) ? mode : 'fixed'
  // 预览态：AiCrudPage 默认 heightMode=full 会铺满画布底部，盖住其它组件。
  // 只读预览按设计高度渲染，保证指标卡/标题等与设计态一致可见。
  if (props.readonly && normalized === 'full')
    return 'fixed'
  // 设计态：画布上还有伴生块时，禁止 CRUD 铺满到底（分页被拉到画布底、下方组件被盖住）
  if (!props.readonly && normalized === 'full' && shouldDemoteFullHeightCrud(block))
    return 'fixed'
  return normalized
}
__impl.resolveBlockHeightMode = resolveBlockHeightMode

/** 同层存在 tree-panel 以外的伴生块时，AiCrudPage 不能再 heightMode=full */
function shouldDemoteFullHeightCrud(block = {}) {
  if (block.blockType !== 'AiCrudPage')
    return false
  const items = Array.isArray(blocks.value) ? blocks.value : []
  return items.some(item => item?.id
    && item.id !== block.id
    && item.blockType
    && item.blockType !== 'tree-panel')
}

function resolveFrameWidth(value, mode, x = 0, fallback = 320) {
  if (mode === 'full')
    return Math.max(24, canvasGridWidth.value - x)
  if (mode === 'auto')
    // 默认宽度 = 组件当前栅格宽度（限在画布内）：不再武断 clamp 240~520，
    // 避免从满宽/固定宽度切到“默认宽度”时组件宽度骤变
    return Math.max(24, Math.min(fallback || 320, Math.max(24, canvasGridWidth.value - x)))
  return resolveCssNumber(value, fallback)
}

function gridWidthToPixels(gridW = 1) {
  const width = clamp(Number(gridW) || 1, 1, LIST_PAGE_GRID_COLS)
  return width * colWidth.value + (width - 1) * gap
}
__impl.gridWidthToPixels = gridWidthToPixels

function gridHeightToPixels(gridH = 1) {
  const height = Math.max(1, Number(gridH) || 1)
  return height * rowHeight + (height - 1) * gap
}
__impl.gridHeightToPixels = gridHeightToPixels

function resolveCssNumber(value, fallback = 0) {
  if (value === null || value === undefined || value === '' || value === '100%')
    return Math.round(fallback)
  if (typeof value === 'number')
    return Math.round(value)
  const text = String(value).trim()
  if (!text)
    return Math.round(fallback)
  const num = Number(text.replace('px', ''))
  return Number.isFinite(num) ? Math.round(num) : Math.round(fallback)
}
__impl.resolveCssNumber = resolveCssNumber

function frameToGridPatch(frame = {}) {
  const cellW = colWidth.value + gap
  const cellH = rowHeight + gap
  const gridX = clamp(Math.round((Number(frame.x) || 0) / cellW), 0, LIST_PAGE_GRID_COLS - 1)
  const gridY = Math.max(0, Math.round((Number(frame.y) || 0) / cellH))
  const gridW = clamp(Math.max(1, Math.round(((Number(frame.width) || 24) + gap) / cellW)), 1, LIST_PAGE_GRID_COLS - gridX)
  const gridH = Math.max(1, Math.round(((Number(frame.height) || 24) + gap) / cellH))
  return { gridX, gridY, gridW, gridH }
}

function resolveAbsoluteCssSize(value) {
  if (value === null || value === undefined || value === '' || value === '100%')
    return undefined
  if (typeof value === 'number')
    return `${value}px`
  const text = String(value).trim()
  if (!text)
    return undefined
  return /^\d+(?:\.\d+)?$/.test(text) ? `${text}px` : text
}

/** minWidth 不允许超过画布剩余宽度，避免 min 宺透值把组件顶出画布右边界 */
function clampMaxCssSize(cssValue, maxValue) {
  if (!cssValue || maxValue <= 0)
    return undefined
  const num = Number.parseFloat(cssValue)
  if (!Number.isFinite(num))
    return cssValue
  return num > maxValue ? `${Math.round(maxValue)}px` : cssValue
}

function toNumberOrNull(value) {
  if (value === null || value === undefined || value === '' || value === '100%')
    return null
  const num = Number(String(value).replace('px', ''))
  return Number.isFinite(num) ? num : null
}

function normalizeSpacingValue(value) {
  const text = String(value ?? '').trim()
  if (!text)
    return 0
  return text
    .split(/\s+/)
    .map(part => (/^\d+(?:\.\d+)?$/.test(part) ? `${part}px` : part))
    .join(' ')
}

__impl.colorToHexInput = colorToHexInput
__impl.hexInputToColor = hexInputToColor

function updateSelectedBlockBackground(value) {
  if (!selectedBlock.value)
    return
  patchBlockStyle(selectedBlock.value.id, { backgroundColor: hexInputToColor(value, 'transparent') })
}

function updateSelectedBlockTextColor(value) {
  if (!selectedBlock.value)
    return
  const text = String(value || '').trim()
  if (!text)
    return patchBlockStyle(selectedBlock.value.id, { color: '' })
  patchBlockStyle(selectedBlock.value.id, { color: hexInputToColor(value, '#111827') })
}

function updateSelectedBlockOpacity(value) {
  if (!selectedBlock.value)
    return
  const parsed = Number(value)
  if (!Number.isFinite(parsed))
    return
  const pct = Math.max(20, Math.min(100, Math.round(parsed)))
  patchBlockStyle(selectedBlock.value.id, { opacity: pct >= 100 ? '' : Number((pct / 100).toFixed(2)) })
}

function updateSelectedBlockBorderStyle(value) {
  if (!selectedBlock.value)
    return
  patchBlockStyle(selectedBlock.value.id, {
    borderStyle: value || 'solid',
    borderColor: value === 'none'
      ? 'transparent'
      : selectedBlockStyle.value.borderColor === 'transparent'
        ? '#e4e4e7'
        : selectedBlockStyle.value.borderColor,
    borderWidth: value === 'none' ? 0 : selectedBlockStyle.value.borderWidth || 1,
  })
}

function updateSelectedBlockBorderColor(value) {
  if (!selectedBlock.value)
    return
  patchBlockStyle(selectedBlock.value.id, {
    borderColor: hexInputToColor(value, '#e4e4e7'),
    borderStyle: selectedBlockStyle.value.borderStyle === 'none'
      ? 'solid'
      : selectedBlockStyle.value.borderStyle || 'solid',
    borderWidth: selectedBlockStyle.value.borderWidth || 1,
  })
}

function updateSelectedBlockBorderWidth(value) {
  if (!selectedBlock.value)
    return
  const parsed = Number(value)
  const width = Number.isFinite(parsed) ? Math.max(0, Math.min(32, parsed)) : 1
  if (width <= 0) {
    patchBlockStyle(selectedBlock.value.id, {
      borderWidth: 0,
      borderStyle: 'none',
      borderColor: 'transparent',
    })
    return
  }
  patchBlockStyle(selectedBlock.value.id, {
    borderWidth: width,
    borderStyle: selectedBlockStyle.value.borderStyle === 'none'
      ? 'solid'
      : selectedBlockStyle.value.borderStyle || 'solid',
  })
}
__impl.updateSelectedBlockBorderWidth = updateSelectedBlockBorderWidth
__impl.updateSelectedBlockTextColor = updateSelectedBlockTextColor
__impl.updateSelectedBlockOpacity = updateSelectedBlockOpacity

function selectBlock(blockId) {
  if (props.readonly)
    return
  designerStore.selectBlock(blockId)
  propertyCollapsed.value = false
}
__impl.selectBlock = selectBlock

function handleBlockClick(blockId) {
  if (mut.suppressNextBlockClick) {
    mut.suppressNextBlockClick = false
    return
  }
  selectBlock(blockId)
}

function handleTreePanelCollapseChange(payload = {}) {
  const id = payload.id || payload.blockId
  if (!id)
    return
  collapsedTreePanelMap.value = {
    ...collapsedTreePanelMap.value,
    [id]: payload.collapsed === true,
  }
}

function handleRuntimeTreeSelect(payload = {}) {
  const filterField = payload.filterField
  runtimeTreeActiveKey.value = payload.key || '__all__'
  if (!filterField || payload.clear || payload.value === undefined || payload.value === null || payload.value === '') {
    runtimeTreeFilter.value = {}
    return
  }
  runtimeTreeFilter.value = buildLeftTreeFilterParams({
    filterField,
    value: payload.value,
    includeChildren: payload.includeChildren !== false,
    expandedValues: payload.expandedValues,
  })
}

function clearSelection() {
  if (props.readonly)
    return
  designerStore.clearSelection()
}

function isFieldConfigurableBlock(blockType) {
  return ['search-form', 'data-table', 'AiCrudPage', 'AiTable', 'AiForm'].includes(blockType)
}

function resolveFieldConfigLabel(blockType) {
  if (blockType === 'search-form')
    return '查询字段'
  if (blockType === 'AiForm')
    return '表单字段'
  return '展示字段'
}

function resolveAiCrudVisibleFlags(block = {}) {
  const props = block.props || {}
  const flags = []
  if (props.showSearch !== false)
    flags.push('showSearch')
  if (props.showPagination !== false)
    flags.push('showPagination')
  if (props.hideToolbar !== true)
    flags.push('showToolbar')
  if (props.showRenderModeSwitch !== false)
    flags.push('showRenderModeSwitch')
  return flags
}

function updateAiCrudVisibleFlags(values = []) {
  if (!selectedBlock.value)
    return
  patchBlockProps(selectedBlock.value.id, {
    showSearch: values.includes('showSearch'),
    showPagination: values.includes('showPagination'),
    hideToolbar: !values.includes('showToolbar'),
    showRenderModeSwitch: values.includes('showRenderModeSwitch'),
  })
}

function resolveAiCrudTableFlags(block = {}) {
  const props = block.props || {}
  const flags = []
  if (props.bordered === true)
    flags.push('bordered')
  if (props.striped === true)
    flags.push('striped')
  if (props.hideSelection === true)
    flags.push('hideSelection')
  if (props.resizable !== false)
    flags.push('resizable')
  if (props.searchEnableCollapse !== false)
    flags.push('searchEnableCollapse')
  return flags
}

function updateAiCrudTableFlags(values = []) {
  if (!selectedBlock.value)
    return
  patchBlockProps(selectedBlock.value.id, {
    bordered: values.includes('bordered'),
    striped: values.includes('striped'),
    hideSelection: values.includes('hideSelection'),
    resizable: values.includes('resizable'),
    searchEnableCollapse: values.includes('searchEnableCollapse'),
  })
}

function firstExpandPanel(block = {}) {
  return block.props?.expandConfig?.panels?.[0] || null
}

function updateAiCrudExpandEnabled(enabled) {
  if (!selectedBlock.value)
    return
  const current = selectedBlock.value.props?.expandConfig || {}
  const nextConfig = enabled
    ? {
        enabled: true,
        trigger: current.trigger || 'icon',
        lazy: current.lazy !== false,
        cache: current.cache !== false,
        layout: current.layout || { mode: 'single', density: 'compact', padding: 12 },
        panels: current.panels?.length ? current.panels : [createDefaultAiCrudExpandPanel(selectedBlock.value)],
      }
    : { ...current, enabled: false }
  patchBlockProps(selectedBlock.value.id, { expandConfig: nextConfig })
}

function updateAiCrudExpandConfig(patch = {}) {
  if (!selectedBlock.value)
    return
  const current = selectedBlock.value.props?.expandConfig || {}
  patchBlockProps(selectedBlock.value.id, {
    expandConfig: {
      ...current,
      ...patch,
      enabled: current.enabled === true,
    },
  })
}

function updateFirstAiCrudExpandPanel(patch = {}) {
  if (!selectedBlock.value)
    return
  const current = selectedBlock.value.props?.expandConfig || {}
  const panels = current.panels?.length ? [...current.panels] : [createDefaultAiCrudExpandPanel(selectedBlock.value)]
  panels[0] = normalizeAiCrudExpandPanelPatch({ ...panels[0], ...patch })
  patchBlockProps(selectedBlock.value.id, {
    expandConfig: {
      ...current,
      enabled: true,
      panels,
    },
  })
}

function updateFirstAiCrudExpandDataSource(patch = {}) {
  const panel = firstExpandPanel(selectedBlock.value) || createDefaultAiCrudExpandPanel(selectedBlock.value)
  updateFirstAiCrudExpandPanel({
    dataSource: {
      ...(panel.dataSource || {}),
      ...patch,
    },
  })
}

function updateFirstAiCrudExpandParamsMap(value) {
  updateFirstAiCrudExpandDataSource({ paramsMap: parseJsonObjectProp(value) })
}

function updateFirstAiCrudDescriptionFields(value) {
  const fields = Array.isArray(value)
    ? value.map(item => String(item || '').trim()).filter(Boolean)
    : String(value || '').split(/\r?\n|,/).map(item => item.trim()).filter(Boolean)
  updateFirstAiCrudExpandPanel({
    descriptions: {
      ...(firstExpandPanel(selectedBlock.value)?.descriptions || {}),
      fields: fields.map(field => ({ field, label: resolveFieldLabelByCode(field) })),
    },
  })
}

function resolveExpandDescriptionFieldKeys(panel, block = {}) {
  const configured = (panel?.descriptions?.fields || [])
    .map(field => field.field || field.key)
    .filter(Boolean)
  if (configured.length)
    return configured
  return resolveAiCrudDefaultDescriptionFields(block).map(field => field.field).filter(Boolean)
}

function resolveExpandDescriptionFieldOptions() {
  const sourceFields = crudTablePanelFields.value.length
    ? crudTablePanelFields.value
    : props.fields || []
  const seen = new Set()
  return sourceFields
    .map((field) => {
      const value = resolveCrudFieldKey(field)
      if (!value || seen.has(value))
        return null
      seen.add(value)
      const rawLabel = resolveCrudFieldLabel(field)
      return {
        label: `${rawLabel}（${value}）`,
        rawLabel,
        value,
      }
    })
    .filter(Boolean)
}

function resolveExpandDescriptionSelectedFields(panel, block = {}) {
  const optionMap = new Map(resolveExpandDescriptionFieldOptions(block).map(option => [option.value, option]))
  return resolveExpandDescriptionFieldKeys(panel, block).map((field) => {
    const option = optionMap.get(field)
    return {
      field,
      label: option?.rawLabel || resolveFieldLabelByCode(field),
    }
  })
}

function resolveExpandDescriptionPanelFields(panel, block = {}) {
  const selectedKeys = resolveExpandDescriptionFieldKeys(panel, block)
  const selected = new Set(selectedKeys)
  const optionMap = new Map(resolveExpandDescriptionFieldOptions(block).map(option => [option.value, option]))
  const orderedKeys = [
    ...selectedKeys,
    ...Array.from(optionMap.keys()).filter(key => !selected.has(key)),
  ]
  return orderedKeys
    .map((field) => {
      const option = optionMap.get(field)
      if (!option && !selected.has(field))
        return null
      return {
        field,
        label: option?.rawLabel || resolveFieldLabelByCode(field),
        selected: selected.has(field),
      }
    })
    .filter(Boolean)
}

function addFirstAiCrudDescriptionField(field) {
  if (!field)
    return
  const fields = resolveExpandDescriptionFieldKeys(firstExpandPanel(selectedBlock.value), selectedBlock.value)
  updateFirstAiCrudDescriptionFields(Array.from(new Set([...fields, field])))
}

function removeFirstAiCrudDescriptionField(field) {
  const fields = resolveExpandDescriptionFieldKeys(firstExpandPanel(selectedBlock.value), selectedBlock.value)
  updateFirstAiCrudDescriptionFields(fields.filter(item => item !== field))
}

function toggleFirstAiCrudDescriptionField(field, visible) {
  if (visible)
    addFirstAiCrudDescriptionField(field)
  else
    removeFirstAiCrudDescriptionField(field)
}

function handleFirstAiCrudDescriptionPanelReorder(fields = []) {
  updateFirstAiCrudDescriptionFields(fields.filter(field => field?.selected).map(field => field.field))
}

function createDefaultAiCrudExpandPanel(block = {}) {
  const fields = resolveAiCrudDefaultDescriptionFields(block)
  return {
    key: 'summary',
    title: '概览',
    type: 'descriptions',
    dataSource: { type: 'row' },
    descriptions: {
      columns: 3,
      fields,
    },
  }
}

function normalizeAiCrudExpandPanelPatch(panel = {}) {
  const type = panel.type || 'descriptions'
  const quantityPanel = ['quantity-balance', 'quantity-ledger', 'quantity-lock'].includes(type)
  const key = panel.key || (type === 'table' ? 'detailTable' : quantityPanel ? type : 'summary')
  return {
    ...panel,
    key,
    title: panel.title || (type === 'table' ? '明细' : quantityPanel ? expandPanelTypeOptions.find(item => item.value === type)?.label : '概览'),
    type,
    dataSource: quantityPanel ? { ...(panel.dataSource || {}), type: 'quantity', queryType: type } : panel.dataSource || { type: 'row' },
    quantity: quantityPanel ? { ...(panel.quantity || {}), queryType: type } : panel.quantity,
    descriptions: panel.descriptions || { columns: 3, fields: resolveAiCrudDefaultDescriptionFields(selectedBlock.value) },
    table: panel.table || { rowKey: 'id', columns: [], pagination: false, maxHeight: 320 },
  }
}

function resolveAiCrudDefaultDescriptionFields(block = {}) {
  const refs = Array.isArray(block.props?.tableFieldRefs) && block.props.tableFieldRefs.length
    ? block.props.tableFieldRefs
    : Array.isArray(block.fieldRefs) ? block.fieldRefs : []
  return refs.slice(0, 6).map(field => ({ field, label: resolveFieldLabelByCode(field) }))
}

function resolveFieldLabelByCode(fieldCode) {
  const field = props.fields.find(item => item.field === fieldCode || item.fieldCode === fieldCode || item.key === fieldCode)
  return field?.label || field?.title || fieldCode
}

function stringifyJsonProp(value) {
  if (value === undefined || value === null || value === '')
    return ''
  if (typeof value === 'string')
    return value
  return JSON.stringify(value, null, 2)
}

function parseJsonObjectProp(value) {
  const text = String(value || '').trim()
  if (!text)
    return {}
  try {
    const parsed = JSON.parse(text)
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  }
  catch {
    return {}
  }
}

function resolveAiCrudEditFlags(block = {}) {
  const props = block.props || {}
  const flags = []
  if (props.editShowFeedback !== false)
    flags.push('editShowFeedback')
  if (props.hideModalFooter === true)
    flags.push('hideModalFooter')
  if (props.hideDefaultDetailContent === true)
    flags.push('hideDefaultDetailContent')
  return flags
}

function updateAiCrudEditFlags(values = []) {
  if (!selectedBlock.value)
    return
  patchBlockProps(selectedBlock.value.id, {
    editShowFeedback: values.includes('editShowFeedback'),
    hideModalFooter: values.includes('hideModalFooter'),
    hideDefaultDetailContent: values.includes('hideDefaultDetailContent'),
  })
}

function isAiCrudToolbarSwitchOn(block = {}, key) {
  const props = block.props || {}
  if (key === 'add')
    return props.hideAdd !== true
  if (key === 'batchDelete')
    return props.hideBatchDelete !== true
  if (key === 'import')
    return props.showImport !== false
  if (key === 'export')
    return props.showExport !== false
  if (key === 'customQuery')
    return props.enableCustomQuery !== false
  if (key === 'exportTasks')
    return props.showExportTasks !== false
  return false
}

function updateAiCrudToolbarSwitch(key, checked) {
  if (!selectedBlock.value)
    return
  const value = checked === true
  const propPatchMap = {
    add: { hideAdd: !value },
    batchDelete: { hideBatchDelete: !value },
    import: { showImport: value },
    export: { showExport: value },
    customQuery: { enableCustomQuery: value },
    exportTasks: { showExportTasks: value },
  }
  patchBlockProps(selectedBlock.value.id, propPatchMap[key] || {})
}

function updateSelectedBlockHookRules(rules) {
  if (!selectedBlock.value)
    return
  patchBlockProps(selectedBlock.value.id, { crudHookRules: rules || {}, beforeSubmitRules: [] })
}

function resolveSelectedBlockDefaultParams(block = {}) {
  const props = block?.props || {}
  return {
    publicParams: props.publicParams || {},
    publicQuery: props.publicQuery || {},
    formDefaultValues: props.formDefaultValues || {},
    submitDefaultParams: props.submitDefaultParams || {},
  }
}

function updateSelectedBlockDefaultParams(params = {}) {
  if (!selectedBlock.value)
    return
  if (isSameDefaultParams(resolveSelectedBlockDefaultParams(selectedBlock.value), params))
    return
  patchBlockProps(selectedBlock.value.id, {
    publicParams: params.publicParams || {},
    publicQuery: params.publicQuery || {},
    formDefaultValues: params.formDefaultValues || {},
    submitDefaultParams: params.submitDefaultParams || {},
  })
}

function isSameDefaultParams(left = {}, right = {}) {
  return JSON.stringify(normalizeDefaultParams(left)) === JSON.stringify(normalizeDefaultParams(right))
}

function normalizeDefaultParams(source = {}) {
  return ['publicParams', 'publicQuery', 'formDefaultValues', 'submitDefaultParams'].reduce((result, key) => {
    const params = source[key] && typeof source[key] === 'object' && !Array.isArray(source[key])
      ? source[key]
      : {}
    result[key] = Object.keys(params).sort().reduce((next, paramKey) => {
      next[paramKey] = params[paramKey]
      return next
    }, {})
    return result
  }, {})
}

function resolveAiTableVisibleFlags(block = {}) {
  const props = block.props || {}
  const flags = []
  if (props.showToolbar !== false)
    flags.push('showToolbar')
  if (props.showPagination === true)
    flags.push('showPagination')
  if (props.hideSelection !== true)
    flags.push('showSelection')
  if (props.showRefresh !== false)
    flags.push('showRefresh')
  if (props.showDensity !== false)
    flags.push('showDensity')
  if (props.showColumnFilter !== false)
    flags.push('showColumnFilter')
  if (props.showSearchToggle === true)
    flags.push('showSearchToggle')
  if (props.showFullscreen === true)
    flags.push('showFullscreen')
  if (props.showRenderModeSwitch !== false)
    flags.push('showRenderModeSwitch')
  return flags
}

function updateAiTableVisibleFlags(values = []) {
  if (!selectedBlock.value)
    return
  patchBlockProps(selectedBlock.value.id, {
    showToolbar: values.includes('showToolbar'),
    showPagination: values.includes('showPagination'),
    hideSelection: !values.includes('showSelection'),
    showRefresh: values.includes('showRefresh'),
    showDensity: values.includes('showDensity'),
    showColumnFilter: values.includes('showColumnFilter'),
    showSearchToggle: values.includes('showSearchToggle'),
    showFullscreen: values.includes('showFullscreen'),
    showRenderModeSwitch: values.includes('showRenderModeSwitch'),
  })
}

function resolveAiTableStyleFlags(block = {}) {
  const props = block.props || {}
  const flags = []
  if (props.bordered !== false)
    flags.push('bordered')
  if (props.striped === true)
    flags.push('striped')
  if (props.singleLine === true)
    flags.push('singleLine')
  return flags
}

function updateAiTableStyleFlags(values = []) {
  if (!selectedBlock.value)
    return
  patchBlockProps(selectedBlock.value.id, {
    bordered: values.includes('bordered'),
    striped: values.includes('striped'),
    singleLine: values.includes('singleLine'),
  })
}

function resolveAiFormFlags(block = {}) {
  const props = block.props || {}
  const flags = []
  if (props.showActions !== false)
    flags.push('showActions')
  if (props.showSubmit !== false)
    flags.push('showSubmit')
  if (props.showReset !== false)
    flags.push('showReset')
  if (props.showCancel === true)
    flags.push('showCancel')
  if (props.enableCollapse === true)
    flags.push('enableCollapse')
  if (props.showFeedback !== false)
    flags.push('showFeedback')
  return flags
}

function updateAiFormFlags(values = []) {
  if (!selectedBlock.value)
    return
  patchBlockProps(selectedBlock.value.id, {
    showActions: values.includes('showActions'),
    showSubmit: values.includes('showSubmit'),
    showReset: values.includes('showReset'),
    showCancel: values.includes('showCancel'),
    enableCollapse: values.includes('enableCollapse'),
    showFeedback: values.includes('showFeedback'),
  })
}

function findExistingBlockByType(blockType) {
  return blocks.value.find(block => blockContainsType(block, blockType))
}
__impl.findExistingBlockByType = findExistingBlockByType

function blockContainsType(block = {}, blockType = '') {
  if (block.blockType === blockType)
    return true
  if ((block.children || []).some(child => blockContainsType(child, blockType)))
    return true
  if ((block.props?.tabs || []).some(tab => (tab.children || []).some(child => blockContainsType(child, blockType))))
    return true
  if ((block.props?.cells || []).some(cell => (cell.children || []).some(child => blockContainsType(child, blockType))))
    return true
  return false
}

function handleNestedBlockDragStart(payload = {}) {
  const block = payload.block || {}
  if (!block.id)
    return
  selectBlock(block.id)
  draggedExistingBlockId.value = block.id
  draggedBlockType.value = block.blockType || ''
  canvasDragActive.value = true
}

function scrollBlockIntoView(block) {
  const scrollEl = canvasScrollRef.value
  if (!scrollEl || !block)
    return
  nextTick(() => {
    const rect = resolveBlockFrame(block)
    const zoom = canvasZoom.value || 1
    scrollEl.scrollTo({
      left: Math.max(0, rect.x * zoom - 32),
      top: Math.max(0, rect.y * zoom - 32),
      behavior: 'smooth',
    })
  })
}
__impl.scrollBlockIntoView = scrollBlockIntoView

function resolveBlockMoreOptions(block = {}) {
  const index = blocks.value.findIndex(item => item.id === block.id)
  const meta = resolveListPageBlockMeta(block.blockType)
  return [
    { label: '复制区块', key: 'duplicate', disabled: !!meta?.unique },
    { label: '上移一行', key: 'moveUp' },
    { label: '下移一行', key: 'moveDown' },
    { label: '置顶', key: 'moveTop' },
    { label: '置底', key: 'moveBottom' },
    { type: 'divider', key: 'divider' },
    { label: '前移一层', key: 'layerForward', disabled: index >= blocks.value.length - 1 },
    { label: '后移一层', key: 'layerBackward', disabled: index <= 0 },
    { type: 'divider', key: 'dangerDivider' },
    { label: '删除', key: 'delete' },
  ]
}

function handleBlockMoreSelect(key, block = {}) {
  if (!block?.id)
    return
  selectBlock(block.id)
  if (key === 'duplicate') {
    duplicateBlock(block.id)
    return
  }
  if (key === 'moveUp') {
    patchBlock(block.id, { gridY: Math.max(0, Number(block.gridY) - 1) })
    return
  }
  if (key === 'moveDown') {
    patchBlock(block.id, { gridY: Number(block.gridY) + 1 })
    return
  }
  if (key === 'moveTop') {
    patchBlock(block.id, { gridY: 0 })
    return
  }
  if (key === 'moveBottom') {
    const bottomRow = blocks.value
      .filter(item => item.id !== block.id)
      .reduce((acc, item) => Math.max(acc, item.gridY + item.gridH), 0)
    patchBlock(block.id, { gridY: bottomRow })
    return
  }
  if (key === 'layerForward') {
    reorderBlockLayer(block.id, 1)
    return
  }
  if (key === 'layerBackward') {
    reorderBlockLayer(block.id, -1)
    return
  }
  if (key === 'delete')
    removeBlock(block.id)
}

function handleNestedBlockMenuSelect(payload = {}) {
  const key = payload.key
  const block = payload.block || {}
  if (!block.id)
    return
  selectBlock(block.id)
  if (key === 'duplicate') {
    duplicateBlock(block.id)
    return
  }
  if (key === 'delete')
    removeBlock(block.id)
}

function resolveTreeSourceObjectValue(item = {}) {
  return String(item.objectCode || item.configKey || item.modelCode || '').trim()
}
__impl.resolveTreeSourceObjectValue = resolveTreeSourceObjectValue

function resolveTreeSourceObjectId(item = {}) {
  return item?.id ?? item?.objectId ?? null
}

function isCurrentListObject(item = {}) {
  const codes = new Set(currentListObjectCodes.value)
  return [
    item.configKey,
    item.objectCode,
    item.modelCode,
    item.tableName,
    item.id,
    item.objectId,
  ].some(value => codes.has(String(value || '').trim()))
}
__impl.isCurrentListObject = isCurrentListObject

function unwrapListPayload(payload) {
  if (Array.isArray(payload))
    return payload
  if (Array.isArray(payload?.records))
    return payload.records
  if (Array.isArray(payload?.list))
    return payload.list
  if (Array.isArray(payload?.rows))
    return payload.rows
  if (Array.isArray(payload?.data))
    return payload.data
  return []
}

function mapObjectFieldsToTreeFields(fields = []) {
  return (Array.isArray(fields) ? fields : [])
    .map((field) => {
      const code = String(field.fieldCode || field.field || field.sourceField || '').trim()
      if (!code)
        return null
      // 树主键/父级常是系统字段，不能过滤掉；仅排除无编码字段
      return {
        field: code,
        sourceField: code,
        label: field.fieldName || field.label || code,
        rawLabel: field.fieldName || field.label || code,
        systemField: field.systemField === true,
      }
    })
    .filter(Boolean)
}

function pickTreeFieldCode(fields = [], preferred = [], excluded = []) {
  const list = Array.isArray(fields) ? fields : []
  const excludedSet = new Set(excluded.filter(Boolean))
  for (const name of preferred) {
    const matched = list.find(field => field.field === name && !excludedSet.has(field.field))
    if (matched)
      return matched.field
  }
  const business = list.find(field => !field.systemField && !excludedSet.has(field.field))
  if (business)
    return business.field
  const any = list.find(field => field.field && !excludedSet.has(field.field))
  return any?.field || preferred[0] || ''
}

async function ensureTreeSourceCatalog() {
  if (treeSourceCatalog.value.length)
    return treeSourceCatalog.value
  if (mut.treeSourceCatalogPromise)
    return mut.treeSourceCatalogPromise
  mut.treeSourceCatalogPromise = (async () => {
    treeSourceLoading.value = true
    try {
      // 与子表选对象一致：不强制 suite，避免可选数据源过窄
      const res = await businessObjectList({})
      treeSourceCatalog.value = unwrapListPayload(res?.data).filter(item => resolveTreeSourceObjectValue(item))
      return treeSourceCatalog.value
    }
    catch (error) {
      console.warn('[ListPageGridDesigner] 加载树数据源对象失败', error?.message || error)
      treeSourceCatalog.value = []
      return []
    }
    finally {
      treeSourceLoading.value = false
      mut.treeSourceCatalogPromise = null
    }
  })()
  return mut.treeSourceCatalogPromise
}
__impl.ensureTreeSourceCatalog = ensureTreeSourceCatalog

function findTreeSourceObject(sourceValue = '') {
  const value = String(sourceValue || '').trim()
  if (!value)
    return null
  return treeSourceCatalog.value.find(item => (
    resolveTreeSourceObjectValue(item) === value
    || String(item.objectCode || '') === value
    || String(item.configKey || '') === value
    || String(item.modelCode || '') === value
  )) || null
}

async function resolveTreeSourceObject(sourceValue = '') {
  const value = String(sourceValue || '').trim()
  if (!value)
    return null
  await ensureTreeSourceCatalog()
  let object = findTreeSourceObject(value)
  if (resolveTreeSourceObjectId(object))
    return object
  try {
    const res = await businessObjectList({ objectCode: value })
    const matched = unwrapListPayload(res?.data).find(item => (
      String(item.objectCode || '') === value
      || String(item.configKey || '') === value
      || resolveTreeSourceObjectValue(item) === value
    )) || unwrapListPayload(res?.data)[0] || null
    if (matched) {
      if (!findTreeSourceObject(resolveTreeSourceObjectValue(matched)))
        treeSourceCatalog.value = [...treeSourceCatalog.value, matched]
      return matched
    }
  }
  catch (error) {
    console.warn('[ListPageGridDesigner] 按编码查找树数据源失败', error?.message || error)
  }
  return object
}


  return {
    ...deps,
    designerStore,
    rowHeight,
    gap,
    TREE_PANEL_COLLAPSED_WIDTH,
    canvasScrollRef,
    selectedBlockId,
    sourceModalTab,
    layoutSourceDraft,
    blockSourceDraft,
    sourceError,
    propertyCollapsed,
    canvasDragActive,
    draggedBlockType,
    draggedExistingBlockId,
    movingBlockId,
    movingPixelOffset,
    canvasZoom,
    localLayout,
    blocks,
    runtimeTreeFilter,
    runtimeTreeActiveKey,
    collapsedTreePanelMap,
    colWidth,
    canvasGridWidth,
    canvasGridHeight,
    collapsedTreeFrames,
    selectedBlock,
    selectedBlockStyle,
    treeSourceCatalog,
    treeSourceLoading,
    currentListObjectCodes,
    crudTablePanelFields,
    cancelSourceModalEdit,
    applySourceModalCode,
    applyLayoutSourceCode,
    applyBlockSourceCode,
    resolveBlockStyle,
    selectedResizeBlock,
    resolveCanvasAnchorStyle,
    resolveRuntimeBlockFrame,
    isTreePanelCollapsed,
    isVerticalFrameOverlap,
    resolveBlockFrame,
    resolveBlockWidthMode,
    resolveBlockHeightMode,
    resolveFrameWidth,
    gridWidthToPixels,
    gridHeightToPixels,
    resolveCssNumber,
    frameToGridPatch,
    resolveAbsoluteCssSize,
    clampMaxCssSize,
    toNumberOrNull,
    normalizeSpacingValue,
    colorToHexInput,
    hexInputToColor,
    updateSelectedBlockBackground,
    updateSelectedBlockTextColor,
    updateSelectedBlockOpacity,
    updateSelectedBlockBorderStyle,
    updateSelectedBlockBorderColor,
    updateSelectedBlockBorderWidth,
    selectBlock,
    handleBlockClick,
    handleTreePanelCollapseChange,
    handleRuntimeTreeSelect,
    clearSelection,
    isFieldConfigurableBlock,
    resolveFieldConfigLabel,
    resolveAiCrudVisibleFlags,
    updateAiCrudVisibleFlags,
    resolveAiCrudTableFlags,
    updateAiCrudTableFlags,
    firstExpandPanel,
    updateAiCrudExpandEnabled,
    updateAiCrudExpandConfig,
    updateFirstAiCrudExpandPanel,
    updateFirstAiCrudExpandDataSource,
    updateFirstAiCrudExpandParamsMap,
    updateFirstAiCrudDescriptionFields,
    resolveExpandDescriptionFieldKeys,
    resolveExpandDescriptionFieldOptions,
    resolveExpandDescriptionSelectedFields,
    resolveExpandDescriptionPanelFields,
    addFirstAiCrudDescriptionField,
    removeFirstAiCrudDescriptionField,
    toggleFirstAiCrudDescriptionField,
    handleFirstAiCrudDescriptionPanelReorder,
    createDefaultAiCrudExpandPanel,
    normalizeAiCrudExpandPanelPatch,
    resolveAiCrudDefaultDescriptionFields,
    resolveFieldLabelByCode,
    stringifyJsonProp,
    parseJsonObjectProp,
    resolveAiCrudEditFlags,
    updateAiCrudEditFlags,
    isAiCrudToolbarSwitchOn,
    updateAiCrudToolbarSwitch,
    updateSelectedBlockHookRules,
    resolveSelectedBlockDefaultParams,
    updateSelectedBlockDefaultParams,
    isSameDefaultParams,
    normalizeDefaultParams,
    resolveAiTableVisibleFlags,
    updateAiTableVisibleFlags,
    resolveAiTableStyleFlags,
    updateAiTableStyleFlags,
    resolveAiFormFlags,
    updateAiFormFlags,
    findExistingBlockByType,
    blockContainsType,
    handleNestedBlockDragStart,
    scrollBlockIntoView,
    resolveBlockMoreOptions,
    handleBlockMoreSelect,
    handleNestedBlockMenuSelect,
    resolveTreeSourceObjectValue,
    resolveTreeSourceObjectId,
    isCurrentListObject,
    unwrapListPayload,
    mapObjectFieldsToTreeFields,
    pickTreeFieldCode,
    ensureTreeSourceCatalog,
    findTreeSourceObject,
    resolveTreeSourceObject,
  }
}
