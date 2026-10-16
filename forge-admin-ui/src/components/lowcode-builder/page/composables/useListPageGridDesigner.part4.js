/** Auto-split part 4 of ListPageGridDesigner setup. */
import { nextTick, onBeforeUnmount, onMounted, watch } from 'vue'
import {
  findBlockInTree,
  mapBlockSiblingsInTree,
  removeBlockFromTree,
} from '../blockTree'
import {
  LIST_PAGE_DESIGN_WIDTH,
  LIST_PAGE_GRID_COLS,
  createDefaultBlockStyle,
  createDefaultListGridLayout,
  resolveListPageBlockMeta,
} from '../page-schema'
import {
  blockEventActionOptions,
  eventTriggerOptions,
} from '../listDesignerOptions'
import { resolveVerticalPushY } from '../listPageFramePush'
import { isAccidentalSelectionChrome, selectionChromeClearPatch } from '../block-style-chrome'
export function useListPageGridDesignerPart4(props, emit, deps = {}) {
  const {
    __impl,
    mut,
    designerStore,
    rowHeight,
    gap,
    canvasScrollRef,
    selectedBlockId,
    fieldDrawerOpen,
    fieldDrawerMode,
    fieldDrawerInitialField,
    canvasDragActive,
    draggedBlockType,
    draggedExistingBlockId,
    dragOverCell,
    dragOverPoint,
    dragBlockedBlockId,
    activeDropCell,
    movingBlockId,
    movingPreviewBlock,
    movingPixelOffset,
    nestedMovingBlockId,
    canvasViewportWidth,
    canvasZoom,
    localLayout,
    blocks,
    designCanvasWidth,
    canvasGridWidth,
    selectedBlock,
    emitLayoutChange,
    isVerticalFrameOverlap,
    resolveBlockFrame,
    resolveBlockWidthMode,
    resolveBlockHeightMode,
    gridHeightToPixels,
    resolveCssNumber,
    frameToGridPatch,
    selectBlock,
    clearSelection,
    resetCanvasDragState,
    autoScrollCanvasOnPointer,
    resolveDropContainerFromPoint,
    resolveNonContainerDropBlock,
    normalizeGridLayoutCells,
    resolveNestedChildHeight,
    moveExistingBlockToGridCell,
    moveExistingBlockToContainer,
    moveExistingBlockToCanvas,
    pixelToCell,
    pixelToPoint,
    patchBlockProps,
  } = deps
  const createActionParam = (...args) => __impl.createActionParam(...args)
function handleGridCellDragOverHint({ blockId, cellKey } = {}) {
  if (!blockId || !cellKey)
    return
  activeDropCell.value = { containerId: blockId, cellKey }
}

function handleCrudPreviewStateChange(payload = {}) {
  if (!payload.blockId || !payload.patch)
    return
  const block = blocks.value.find(item => item.id === payload.blockId)
  const currentProps = block?.props || {}
  const changedPatch = Object.fromEntries(
    Object.entries(payload.patch).filter(([key, value]) => currentProps[key] !== value),
  )
  if (!Object.keys(changedPatch).length)
    return
  patchBlockProps(payload.blockId, changedPatch)
}

function crudPreviewStatusText(status) {
  if (status === 'loading')
    return '请求中'
  if (status === 'success')
    return '预览成功'
  if (status === 'error')
    return '预览失败'
  return '模拟预览'
}

/** 自由布局画布读 pageFlow*；属性面板改帧时必须同步，否则宽高/坐标与画布脱节 */
function buildPanelOnlyPageFlowStyle(next = {}, widthMode = 'fixed') {
  if (!props.panelOnly)
    return {}
  return {
    pageFlowX: Math.max(0, Math.round(Number(next.x) || 0)),
    pageFlowY: Math.max(0, Math.round(Number(next.y) || 0)),
    pageFlowWidth: widthMode === 'full'
      ? 'calc(100% - 48px)'
      : `${Math.max(24, Math.round(Number(next.width) || 24))}px`,
    pageFlowHeight: Math.max(24, Math.round(Number(next.height) || 24)),
  }
}

function patchBlockFrame(id, patch) {
  const source = blocks.value.find(block => block.id === id)
  if (!source)
    return false
  const current = resolveBlockFrame(source)
  const widthChanged = Object.prototype.hasOwnProperty.call(patch, 'width')
  const heightChanged = Object.prototype.hasOwnProperty.call(patch, 'height')
  const currentWidthMode = resolveBlockWidthMode(source)
  const currentHeightMode = resolveBlockHeightMode(source)
  const nextWidthMode = widthChanged ? 'fixed' : currentWidthMode
  const nextHeightMode = heightChanged ? 'fixed' : currentHeightMode
  const next = {
    ...current,
    ...patch,
  }
  next.width = Math.max(24, Number(next.width) || current.width)
  next.height = Math.max(24, Number(next.height) || current.height)
  next.x = clamp(Number(next.x) || 0, 0, Math.max(0, canvasGridWidth.value - next.width))
  next.y = Math.max(0, Number(next.y) || 0)
  const gridPatch = frameToGridPatch(next)
  const nextWidthValue = nextWidthMode === 'full' ? '100%' : nextWidthMode === 'auto' ? 'auto' : next.width
  const patchedItems = blocks.value.map(block => block.id === id
    ? {
        ...block,
        ...gridPatch,
        props: {
          ...(block.props || {}),
          style: {
            ...createDefaultBlockStyle(),
            ...(block.props?.style || {}),
            x: next.x,
            y: next.y,
            widthMode: nextWidthMode,
            width: nextWidthValue,
            heightMode: nextHeightMode,
            height: next.height,
            ...buildPanelOnlyPageFlowStyle(next, nextWidthMode),
          },
        },
      }
    : block)
  const withTree = applyTreePanelResponsiveMainFrames(source, next, patchedItems)
  // 拉高/拖动撞到下方块时下推，而不是静默失败（否则高度手柄几乎拖不动）
  const nextItems = pushStackedBlocksDown(withTree, {
    ignoreTreeMainFor: source.blockType === 'tree-panel' ? id : '',
  })
  localLayout.value = {
    ...localLayout.value,
    items: normalizeGridItems(nextItems),
  }
  return true
}

/** 按垂直堆叠规则补齐块间距，消除重叠与贴边 */
function pushStackedBlocksDown(items = [], options = {}) {
  const ignoreTreeMainFor = options.ignoreTreeMainFor || ''
  const frames = items
    .filter((block) => {
      if (!ignoreTreeMainFor || block.id === ignoreTreeMainFor)
        return true
      if (isTreeCrudMainBlock(block) && resolveBlockWidthMode(block) === 'full')
        return false
      return true
    })
    .map((block) => {
      const frame = resolveBlockFrame(block)
      return {
        id: block.id,
        x: frame.x,
        y: frame.y,
        width: frame.width,
        height: frame.height,
      }
    })
  const pushedY = resolveVerticalPushY(frames, Math.max(12, Number(gap) || 8))
  return items.map((block) => {
    const nextY = pushedY.get(block.id)
    if (nextY === undefined)
      return block
    const current = resolveBlockFrame(block)
    if (Math.round(current.y) === Math.round(nextY))
      return block
    const nextFrame = { ...current, y: nextY }
    const gridPatch = frameToGridPatch(nextFrame)
    const widthMode = resolveBlockWidthMode(block)
    return {
      ...block,
      ...gridPatch,
      props: {
        ...(block.props || {}),
        style: {
          ...createDefaultBlockStyle(),
          ...(block.props?.style || {}),
          x: nextFrame.x,
          y: nextFrame.y,
          width: block.props?.style?.width ?? nextFrame.width,
          height: block.props?.style?.height ?? nextFrame.height,
          widthMode,
          heightMode: resolveBlockHeightMode(block),
          ...buildPanelOnlyPageFlowStyle(nextFrame, widthMode),
        },
      },
    }
  })
}

function tidyCanvasBlockSpacing() {
  const nextItems = pushStackedBlocksDown(blocks.value)
  localLayout.value = {
    ...localLayout.value,
    items: normalizeGridItems(nextItems),
  }
}

function applyTreePanelResponsiveMainFrames(source = {}, nextTreeFrame = {}, items = []) {
  if (props.layoutType !== 'tree-crud' || source.blockType !== 'tree-panel')
    return items
  const mainX = clamp(
    Math.round((Number(nextTreeFrame.x) || 0) + (Number(nextTreeFrame.width) || 0) + gap),
    0,
    Math.max(0, canvasGridWidth.value - 24),
  )
  const mainWidth = Math.max(24, canvasGridWidth.value - mainX)
  return items.map((block) => {
    if (!isTreeCrudMainBlock(block) || resolveBlockWidthMode(block) !== 'full')
      return block
    const rect = resolveBlockFrame(block)
    if (!isVerticalFrameOverlap(rect, nextTreeFrame))
      return block
    const nextFrame = {
      ...rect,
      x: mainX,
      width: mainWidth,
    }
    const gridPatch = frameToGridPatch(nextFrame)
    return {
      ...block,
      ...gridPatch,
      props: {
        ...(block.props || {}),
        style: {
          ...createDefaultBlockStyle(),
          ...(block.props?.style || {}),
          x: nextFrame.x,
          y: nextFrame.y,
          widthMode: 'full',
          width: '100%',
          height: nextFrame.height,
        },
      },
    }
  })
}

function isTreeCrudMainBlock(block = {}) {
  return props.layoutType === 'tree-crud'
    && ['AiCrudPage', 'AiTable', 'data-table', 'search-form', 'toolbar'].includes(block.blockType)
}

function doesFrameOverlapBlocks(sourceId, frame, options = {}) {
  const source = blocks.value.find(block => block.id === sourceId)
  return blocks.value.some((block) => {
    if (block.id === sourceId)
      return false
    if (options.ignoreTreeMainBlocks && source?.blockType === 'tree-panel' && isTreeCrudMainBlock(block) && resolveBlockWidthMode(block) === 'full')
      return false
    return framesOverlap(frame, resolveBlockFrame(block))
  })
}
__impl.doesFrameOverlapBlocks = doesFrameOverlapBlocks

function framesOverlap(a = {}, b = {}) {
  const aLeft = Number(a.x) || 0
  const aTop = Number(a.y) || 0
  const aRight = aLeft + (Number(a.width) || 0)
  const aBottom = aTop + (Number(a.height) || 0)
  const bLeft = Number(b.x) || 0
  const bTop = Number(b.y) || 0
  const bRight = bLeft + (Number(b.width) || 0)
  const bBottom = bTop + (Number(b.height) || 0)
  return aLeft < bRight && aRight > bLeft && aTop < bBottom && aBottom > bTop
}

function setBlockWidthMode(id, mode = 'full') {
  const source = blocks.value.find(block => block.id === id)
  if (!source)
    return
  const widthMode = ['full', 'auto', 'fixed'].includes(mode) ? mode : 'full'
  const current = resolveBlockFrame(source)
  const fixedRuntimeWidth = props.panelOnly && resolveBlockWidthMode(source) !== 'fixed'
    ? Math.max(280, Math.min(640, Math.round(canvasGridWidth.value * 0.52)))
    : current.width
  const nextFrame = {
    ...current,
    // 切“默认宽度”保持当前视觉宽度（栅格宽度随后由 frameToGridPatch 同步），
    // 仅“填充容器”按语义扩展到画布右缘，避免模式切换时宽度跳变
    width: widthMode === 'full'
      ? Math.max(24, canvasGridWidth.value - current.x)
      : widthMode === 'auto'
        ? Math.min(current.width, Math.max(24, canvasGridWidth.value - current.x))
        : fixedRuntimeWidth,
  }
  const gridPatch = frameToGridPatch(nextFrame)
  localLayout.value = {
    ...localLayout.value,
    items: normalizeGridItems(blocks.value.map(block => block.id === id
      ? {
          ...block,
          ...gridPatch,
          props: {
            ...(block.props || {}),
            style: {
              ...createDefaultBlockStyle(),
              ...(block.props?.style || {}),
              x: nextFrame.x,
              y: nextFrame.y,
              widthMode,
              width: widthMode === 'full' ? '100%' : widthMode === 'auto' ? 'auto' : nextFrame.width,
              height: nextFrame.height,
              ...buildPanelOnlyPageFlowStyle(nextFrame, widthMode),
            },
          },
        }
      : block)),
  }
}

function setBlockHeightMode(id, mode = 'fixed') {
  const source = blocks.value.find(block => block.id === id)
  if (!source)
    return
  const heightMode = ['fixed', 'auto', 'full'].includes(mode) ? mode : 'fixed'
  const current = resolveBlockFrame(source)
  const nextFrame = {
    ...current,
    height: heightMode === 'auto' ? Math.max(48, Math.min(240, current.height)) : current.height,
  }
  const gridPatch = frameToGridPatch(nextFrame)
  const widthMode = resolveBlockWidthMode(source)
  localLayout.value = {
    ...localLayout.value,
    items: normalizeGridItems(blocks.value.map(block => block.id === id
      ? {
          ...block,
          ...gridPatch,
          props: {
            ...(block.props || {}),
            style: {
              ...createDefaultBlockStyle(),
              ...(block.props?.style || {}),
              x: nextFrame.x,
              y: nextFrame.y,
              width: nextFrame.width,
              heightMode,
              height: nextFrame.height,
              ...buildPanelOnlyPageFlowStyle(nextFrame, widthMode),
            },
          },
        }
      : block)),
  }
}

function setBlockContentAlign(id, align = 'left') {
  const source = findBlockInTree(blocks.value, id)
  if (!source)
    return
  const textAlign = align === 'center' ? 'center' : 'left'
  patchBlockProps(id, {
    align: textAlign,
    textAlign,
    style: {
      ...createDefaultBlockStyle(),
      ...(source.props?.style || {}),
      textAlign,
    },
  })
}

function addBlockEvent() {
  if (!selectedBlock.value)
    return
  const list = [...(selectedBlock.value.props?.events || [])]
  list.push({
    id: `evt_${Date.now()}`,
    trigger: selectedBlock.value.blockType === 'tree-panel' ? 'nodeSelect' : 'click',
    action: 'none',
    targetBlockId: '',
    targetPageKey: '',
    targetFormKey: '',
    description: '',
    params: [],
  })
  patchBlockProps(selectedBlock.value.id, { events: list })
  nextTick(() => {
    const rows = document.querySelectorAll('.event-row')
    const last = rows[rows.length - 1]
    last?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
  })
}

function resolvePrimaryClickEvent(block = selectedBlock.value) {
  const eventItem = (block?.props?.events || []).find(item => (item.trigger || 'click') === 'click')
  return {
    trigger: 'click',
    action: 'none',
    targetBlockId: '',
    targetPageKey: '',
    targetFormKey: '',
    requestUrl: '',
    description: '',
    params: [],
    permissionCode: '',
    confirmText: '',
    displayCondition: '',
    successBehavior: 'none',
    ...(eventItem || {}),
  }
}

function updatePrimaryClickEvent(patch = {}) {
  if (!selectedBlock.value)
    return
  const list = [...(selectedBlock.value.props?.events || [])]
  const index = list.findIndex(item => (item.trigger || 'click') === 'click')
  const nextEvent = {
    id: index >= 0 ? list[index].id : `evt_${Date.now()}`,
    ...resolvePrimaryClickEvent(selectedBlock.value),
    ...patch,
    trigger: 'click',
  }
  if (index >= 0)
    list[index] = nextEvent
  else
    list.unshift(nextEvent)
  patchBlockProps(selectedBlock.value.id, { events: list })
}

function updateBlockEvent(eventIdx, patch) {
  if (!selectedBlock.value)
    return
  const list = [...(selectedBlock.value.props?.events || [])]
  list[eventIdx] = { ...(list[eventIdx] || {}), ...patch }
  patchBlockProps(selectedBlock.value.id, { events: list })
}

function removeBlockEvent(eventIdx) {
  if (!selectedBlock.value)
    return
  const list = [...(selectedBlock.value.props?.events || [])]
  list.splice(eventIdx, 1)
  patchBlockProps(selectedBlock.value.id, { events: list })
}

function addBlockEventParam(eventIdx) {
  const eventItem = selectedBlock.value?.props?.events?.[eventIdx]
  if (!eventItem)
    return
  updateBlockEvent(eventIdx, {
    params: [...(eventItem.params || []), createActionParam()],
  })
}

function updateBlockEventParam(eventIdx, paramIdx, patch) {
  const eventItem = selectedBlock.value?.props?.events?.[eventIdx]
  if (!eventItem)
    return
  const params = [...(eventItem.params || [])]
  params[paramIdx] = { ...(params[paramIdx] || {}), ...patch }
  updateBlockEvent(eventIdx, { params })
}

function removeBlockEventParam(eventIdx, paramIdx) {
  const eventItem = selectedBlock.value?.props?.events?.[eventIdx]
  if (!eventItem)
    return
  const params = [...(eventItem.params || [])]
  params.splice(paramIdx, 1)
  updateBlockEvent(eventIdx, { params })
}

function eventTriggerText(trigger) {
  return eventTriggerOptions.find(item => item.value === trigger)?.label || trigger || '触发'
}

function eventActionText(action) {
  return blockEventActionOptions.find(item => item.value === action)?.label || action || '动作'
}

function patchBlockStyle(id, patch) {
  const source = findBlockInTree(blocks.value, id)
  if (!source)
    return
  patchBlockProps(id, {
    style: {
      ...createDefaultBlockStyle(),
      ...(source.props?.style || {}),
      ...patch,
    },
  })
}
__impl.patchBlockStyle = patchBlockStyle

// 自由布局选中时清掉误写入的选中态蓝边（含挂载时已选中：须 immediate）
watch(
  () => selectedBlock.value?.id,
  (id) => {
    if (!props.panelOnly || props.readonly || !id || !selectedBlock.value)
      return
    if (!isAccidentalSelectionChrome(selectedBlock.value.props?.style))
      return
    patchBlockStyle(id, selectionChromeClearPatch())
  },
  { immediate: true },
)

function removeBlock(id) {
  if (!id)
    return
  localLayout.value = {
    ...localLayout.value,
    items: normalizeGridItems(removeBlockFromTree(blocks.value, id)),
  }
  if (selectedBlockId.value === id) {
    selectedBlockId.value = null
  }
}
__impl.removeBlock = removeBlock

function duplicateBlock(id) {
  const source = blocks.value.find(block => block.id === id)
  if (!source) {
    duplicateNestedBlock(id)
    return
  }
  const meta = resolveListPageBlockMeta(source.blockType)
  if (meta?.unique)
    return
  const copy = cloneBlockWithFreshIds(source, `${source.id}_copy_${Date.now()}`)
  const sourceFrame = resolveBlockFrame(source)
  const sourceWidthMode = resolveBlockWidthMode(source)
  const nextFrame = {
    x: clamp(
      sourceFrame.x + 32,
      0,
      sourceWidthMode === 'full'
        ? Math.max(0, canvasGridWidth.value - 24)
        : Math.max(0, canvasGridWidth.value - sourceFrame.width),
    ),
    y: sourceFrame.y + 32,
    width: sourceFrame.width,
    height: sourceFrame.height,
  }
  const gridPatch = frameToGridPatch(nextFrame)
  copy.label = `${source.label || resolveListPageBlockMeta(source.blockType)?.title || '区块'} 副本`
  copy.gridX = gridPatch.gridX
  copy.gridY = gridPatch.gridY
  copy.gridW = gridPatch.gridW
  copy.gridH = gridPatch.gridH
  copy.props = {
    ...(copy.props || {}),
    style: {
      ...createDefaultBlockStyle(),
      ...(copy.props?.style || {}),
      ...nextFrame,
    },
  }
  localLayout.value = {
    ...localLayout.value,
    items: normalizeGridItems([...blocks.value, copy]),
  }
  selectBlock(copy.id)
}
__impl.duplicateBlock = duplicateBlock

function duplicateNestedBlock(id) {
  const source = findBlockInTree(blocks.value, id)
  if (!source)
    return
  const meta = resolveListPageBlockMeta(source.blockType)
  if (meta?.unique)
    return
  const copy = cloneBlockWithFreshIds(source, `${source.id}_copy_${Date.now()}`)
  copy.label = `${source.label || meta?.title || '组件'} 副本`
  let inserted = false
  localLayout.value = {
    ...localLayout.value,
    items: normalizeGridItems(mapBlockSiblingsInTree(blocks.value, (siblings) => {
      const sourceIndex = siblings.findIndex(item => item.id === id)
      if (sourceIndex < 0 || inserted)
        return siblings
      const next = [...siblings]
      next.splice(sourceIndex + 1, 0, copy)
      inserted = true
      return next
    })),
  }
  if (inserted)
    selectBlock(copy.id)
}

function cloneBlockWithFreshIds(block = {}, rootId = '') {
  const copy = JSON.parse(JSON.stringify(block))
  const stamp = Date.now()
  const applyIds = (node, suffix = 'root') => {
    const next = {
      ...node,
      id: suffix === 'root' ? (rootId || `${node.id}_copy_${stamp}`) : `${node.id}_copy_${stamp}_${suffix}`,
    }
    if (Array.isArray(next.children)) {
      next.children = next.children.map((child, index) => applyIds(child, `${suffix}_child_${index}`))
    }
    if (Array.isArray(next.props?.tabs)) {
      next.props = {
        ...(next.props || {}),
        tabs: next.props.tabs.map((tab, tabIndex) => ({
          ...tab,
          children: (tab.children || []).map((child, childIndex) => applyIds(child, `${suffix}_tab_${tabIndex}_${childIndex}`)),
        })),
      }
    }
    if (Array.isArray(next.props?.cells)) {
      next.props = {
        ...(next.props || {}),
        cells: next.props.cells.map((cell, cellIndex) => ({
          ...cell,
          children: (cell.children || []).map((child, childIndex) => applyIds(child, `${suffix}_cell_${cellIndex}_${childIndex}`)),
        })),
      }
    }
    return next
  }
  return applyIds(copy)
}

function reorderBlockLayer(id, offset) {
  const index = blocks.value.findIndex(block => block.id === id)
  if (index < 0)
    return
  const nextIndex = clamp(index + offset, 0, blocks.value.length - 1)
  if (nextIndex === index)
    return
  const items = [...blocks.value]
  const [target] = items.splice(index, 1)
  items.splice(nextIndex, 0, target)
  localLayout.value = {
    ...localLayout.value,
    items,
  }
}
__impl.reorderBlockLayer = reorderBlockLayer

function resetLayout() {
  localLayout.value = normalizeDesignerLayout(createDefaultListGridLayout(props.modelSchema, { layoutType: props.layoutType }))
  clearSelection()
}

function clearCanvas() {
  localLayout.value = normalizeDesignerLayout({
    ...(localLayout.value || {}),
    cols: LIST_PAGE_GRID_COLS,
    rowHeight,
    gap,
    designWidth: designCanvasWidth.value,
    layoutType: props.layoutType,
    items: [],
  })
  fieldDrawerOpen.value = false
  clearSelection()
}

function clamp(value, min, max) {
  const n = Number(value)
  if (!Number.isFinite(n))
    return min
  return Math.min(Math.max(n, min), max)
}
__impl.clamp = clamp

function normalizeDesignerLayout(layout = {}) {
  return {
    ...layout,
    designWidth: clamp(layout.designWidth || LIST_PAGE_DESIGN_WIDTH, 960, 2560),
    items: normalizeGridItems(layout.items || []).map(normalizeTabsBlock),
  }
}
__impl.normalizeDesignerLayout = normalizeDesignerLayout

function normalizeTabsBlock(block = {}) {
  let next = block
  if (Array.isArray(next.children))
    next = { ...next, children: next.children.map(normalizeTabsBlock) }
  if (Array.isArray(next.props?.tabs)) {
    const tabs = next.props.tabs.length
      ? next.props.tabs
      : [{ key: 'tab_1', title: '标签 1', children: [] }]
    next = {
      ...next,
      props: {
        ...(next.props || {}),
        tabs: tabs.map((tab, index) => ({
          ...tab,
          key: tab.key || `tab_${index + 1}`,
          title: tab.title || `标签 ${index + 1}`,
          children: Array.isArray(tab.children) ? tab.children.map(normalizeTabsBlock) : [],
        })),
      },
    }
  }
  if (Array.isArray(next.props?.cells)) {
    next = {
      ...next,
      props: {
        ...(next.props || {}),
        cells: next.props.cells.map(cell => ({
          ...cell,
          children: Array.isArray(cell.children) ? cell.children.map(normalizeTabsBlock) : [],
        })),
      },
    }
  }
  return next
}

function normalizeGridItems(items = []) {
  return items.map((item) => {
    const gridW = clamp(item.gridW, 1, LIST_PAGE_GRID_COLS)
    const gridX = clamp(item.gridX, 0, LIST_PAGE_GRID_COLS - gridW)
    const autoGridH = resolveAutoGridH(item, gridW)
    const gridH = Math.max(autoGridH, Number(item.gridH) || 1)
    return normalizeGridItemFrame({
      ...item,
      gridX,
      gridW,
      gridY: Math.max(0, Number(item.gridY) || 0),
      gridH,
    }, gridH)
  })
}
__impl.normalizeGridItems = normalizeGridItems

function normalizeGridItemFrame(item = {}, gridH = item.gridH) {
  const autoHeight = gridHeightToPixels(gridH || item.gridH || 1)
  const style = item.props?.style || {}
  const currentHeight = resolveCssNumber(style.height, 0)
  if (currentHeight >= autoHeight)
    return item
  return {
    ...item,
    props: {
      ...(item.props || {}),
      style: {
        ...createDefaultBlockStyle(),
        ...style,
        height: autoHeight,
      },
    },
  }
}

function resolveAutoGridH(block = {}, normalizedGridW = block.gridW) {
  const gridW = Number(normalizedGridW) || Number(block.gridW) || LIST_PAGE_GRID_COLS
  if (block.blockType === 'search-form') {
    const fieldRows = Math.max(1, Math.ceil((block.fieldRefs?.length || 0) / resolveFieldColumnCount(gridW)))
    return Math.max(4, gridRowsForPixels(76 + fieldRows * 54))
  }
  if (block.blockType === 'toolbar') {
    const actionRows = Math.max(1, Math.ceil(resolveToolbarActionCount(block) / resolveToolbarColumnCount(gridW)))
    return Math.max(2, gridRowsForPixels(20 + actionRows * 34))
  }
  if (block.blockType === 'data-table') {
    return Math.max(8, gridRowsForPixels(238))
  }
  if (block.blockType === 'AiCrudPage') {
    return Math.max(5, gridRowsForPixels(160))
  }
  if (block.blockType === 'AiTable') {
    return Math.max(8, gridRowsForPixels(250))
  }
  if (block.blockType === 'AiForm') {
    return Math.max(5, gridRowsForPixels(164))
  }
  if (block.blockType === 'detail-info') {
    const fieldRows = Math.max(1, Math.ceil((block.fieldRefs?.length || 0) / Math.max(1, Number(block.props?.columnCount || 2))))
    return Math.max(5, gridRowsForPixels(58 + fieldRows * 54))
  }
  if (block.blockType === 'tree-panel') {
    return Math.max(12, gridRowsForPixels(360))
  }
  if (block.blockType === 'grid-layout') {
    const cells = normalizeGridLayoutCells(block)
    const maxCellHeight = cells.reduce((max, cell) => Math.max(max, Number(cell.minHeight || 0)), 0)
    return Math.max(4, gridRowsForPixels(maxCellHeight + 16))
  }
  if (block.blockType === 'box-layout') {
    const childrenHeight = (block.children || []).reduce((sum, child) => sum + resolveNestedChildHeight(child), 0)
    const gapValue = Math.max(0, Number(block.props?.gap ?? 12))
    return Math.max(4, gridRowsForPixels(childrenHeight + Math.max(0, (block.children || []).length - 1) * gapValue + 24))
  }
  if (['rich-text', 'transfer', 'step-form', 'video-player', 'iframe', 'markdown', 'descriptions', 'signature-pad'].includes(block.blockType))
    return Math.max(4, gridRowsForPixels(resolveCssNumber(block.props?.style?.height, 180)))
  return 1
}

function resolveFieldColumnCount(gridW) {
  if (gridW >= 10)
    return 4
  if (gridW >= 7)
    return 3
  if (gridW >= 4)
    return 2
  return 1
}

function resolveToolbarColumnCount(gridW) {
  if (gridW >= 10)
    return 6
  if (gridW >= 7)
    return 4
  if (gridW >= 4)
    return 3
  return 2
}

function resolveToolbarActionCount(block = {}) {
  const baseActions = Array.isArray(block.props?.actions) ? block.props.actions.length : 0
  const customActions = (block.props?.customActions || []).filter(action => (action.position || 'toolbar') === 'toolbar').length
  return Math.max(1, baseActions + customActions)
}

function gridRowsForPixels(height) {
  return Math.max(1, Math.ceil((Number(height) + gap) / (rowHeight + gap)))
}

// 拖动移动
let moveCtx = null
let nestedMoveCtx = null
function startMove(block, event) {
  if (props.readonly)
    return
  if (event.button !== 0)
    return
  event.preventDefault()
  selectBlock(block.id)
  const rect = resolveBlockFrame(block)
  movingBlockId.value = block.id
  movingPreviewBlock.value = { ...block }
  movingPixelOffset.value = { x: 0, y: 0 }
  designerStore.beginDeferLayoutEmit()
  moveCtx = {
    blockId: block.id,
    startX: event.clientX,
    startY: event.clientY,
    startScrollLeft: canvasScrollRef.value?.scrollLeft || 0,
    startScrollTop: canvasScrollRef.value?.scrollTop || 0,
    originX: rect.x,
    originY: rect.y,
    originW: rect.width,
    originH: rect.height,
  }
  window.addEventListener('pointermove', onMove)
  window.addEventListener('pointerup', endMove)
}

function startNestedMove(block, event) {
  if (props.readonly || !block?.id)
    return
  if (event.button !== 0)
    return
  event.preventDefault()
  selectBlock(block.id)
  draggedExistingBlockId.value = block.id
  draggedBlockType.value = block.blockType || ''
  canvasDragActive.value = true
  nestedMovingBlockId.value = block.id
  const node = event.target?.closest?.('.layout-grid-cell-child')
  const rect = node?.getBoundingClientRect?.()
  const zoom = canvasZoom.value || 1
  nestedMoveCtx = {
    blockId: block.id,
    startX: event.clientX,
    startY: event.clientY,
    pointerOffsetX: rect ? (event.clientX - rect.left) / zoom : 0,
    pointerOffsetY: rect ? (event.clientY - rect.top) / zoom : 0,
    originW: rect ? rect.width / zoom : 120,
    originH: rect ? rect.height / zoom : 48,
    moved: false,
  }
  document.body.classList.add('list-grid-nested-moving')
  updateNestedMovePreview(event)
  window.addEventListener('pointermove', onNestedMove, { passive: false })
  window.addEventListener('pointerup', endNestedMove)
  window.addEventListener('pointercancel', cancelNestedMove)
}

function onNestedMove(event) {
  if (!nestedMoveCtx)
    return
  event.preventDefault()
  const dx = event.clientX - nestedMoveCtx.startX
  const dy = event.clientY - nestedMoveCtx.startY
  if (Math.abs(dx) > 2 || Math.abs(dy) > 2) {
    nestedMoveCtx.moved = true
    mut.suppressNextBlockClick = true
  }
  updateNestedMovePreview(event)
}

function updateNestedMovePreview(event) {
  if (!nestedMoveCtx)
    return
  autoScrollCanvasOnPointer(event)
  canvasDragActive.value = true
  draggedExistingBlockId.value = nestedMoveCtx.blockId
  const block = findBlockInTree(blocks.value, nestedMoveCtx.blockId)
  draggedBlockType.value = block?.blockType || draggedBlockType.value
  const container = resolveDropContainerFromPoint(event.clientX, event.clientY)
  dragBlockedBlockId.value = ''
  activeDropCell.value = container?.block?.blockType === 'grid-layout' && container.cellKey
    ? { containerId: container.id, cellKey: container.cellKey }
    : null
  dragOverCell.value = activeDropCell.value ? null : pixelToCell(event.clientX, event.clientY)
  dragOverPoint.value = pixelToPoint(event.clientX, event.clientY)
}

function endNestedMove(event) {
  if (!nestedMoveCtx)
    return
  const ctx = nestedMoveCtx
  const blockId = ctx.blockId
  const container = resolveDropContainerFromPoint(event.clientX, event.clientY)
  const cellStylePatch = container?.cellRect ? resolveNestedCellDropStyle(event, container, ctx) : {}
  const point = pixelToPoint(event.clientX, event.clientY)
  cleanupNestedMove()
  if (!ctx.moved)
    return
  if (container?.block?.blockType === 'grid-layout' && container.cellKey) {
    moveExistingBlockToGridCell(blockId, container.id, container.cellKey, cellStylePatch)
    return
  }
  if (container?.block && container.block.blockType !== 'grid-layout' && container.block.id !== blockId) {
    moveExistingBlockToContainer(blockId, container.id, container.tabKey)
    return
  }
  moveExistingBlockToCanvas(blockId, point)
}

function cancelNestedMove() {
  cleanupNestedMove()
}

function cleanupNestedMove() {
  nestedMoveCtx = null
  nestedMovingBlockId.value = ''
  document.body.classList.remove('list-grid-nested-moving')
  resetCanvasDragState()
  window.removeEventListener('pointermove', onNestedMove)
  window.removeEventListener('pointerup', endNestedMove)
  window.removeEventListener('pointercancel', cancelNestedMove)
}

function resolveNestedCellDropStyle(event, container = {}, ctx = {}) {
  if (!container.cellRect)
    return {}
  const zoom = canvasZoom.value || 1
  const cellWidth = Math.max(24, container.cellRect.width / zoom - 16)
  const cellHeight = Math.max(24, container.cellRect.height / zoom - 16)
  const nextX = clamp(
    (event.clientX - container.cellRect.left) / zoom - (ctx.pointerOffsetX || 0) - 8,
    0,
    Math.max(0, cellWidth - Math.min(ctx.originW || 24, cellWidth)),
  )
  const nextY = clamp(
    (event.clientY - container.cellRect.top) / zoom - (ctx.pointerOffsetY || 0) - 8,
    0,
    Math.max(0, cellHeight - Math.min(ctx.originH || 24, cellHeight)),
  )
  return {
    x: Math.round(nextX),
    y: Math.round(nextY),
  }
}
function onMove(event) {
  if (!moveCtx)
    return
  const block = findBlockInTree(blocks.value, moveCtx.blockId)
  if (!block)
    return
  autoScrollCanvasOnPointer(event)
  const zoom = canvasZoom.value || 1
  const scrollEl = canvasScrollRef.value
  const scrollDx = (scrollEl?.scrollLeft || 0) - (moveCtx.startScrollLeft || 0)
  const scrollDy = (scrollEl?.scrollTop || 0) - (moveCtx.startScrollTop || 0)
  const rawDx = (event.clientX - moveCtx.startX + scrollDx) / zoom
  const rawDy = (event.clientY - moveCtx.startY + scrollDy) / zoom
  const minDx = -moveCtx.originX
  const widthMode = resolveBlockWidthMode(block)
  const maxDx = widthMode === 'full'
    ? Math.max(0, canvasGridWidth.value - 24 - moveCtx.originX)
    : Math.max(0, canvasGridWidth.value - moveCtx.originW - moveCtx.originX)
  const minDy = -moveCtx.originY
  const offsetX = clamp(rawDx, minDx, maxDx)
  const offsetY = Math.max(rawDy, minDy)
  movingPixelOffset.value = { x: offsetX, y: offsetY }
  canvasDragActive.value = true
  draggedExistingBlockId.value = moveCtx.blockId
  draggedBlockType.value = block.blockType || draggedBlockType.value
  const container = resolveDropContainerFromPoint(event.clientX, event.clientY)
  dragBlockedBlockId.value = ''
  activeDropCell.value = container?.block?.blockType === 'grid-layout' && container.cellKey
    ? { containerId: container.id, cellKey: container.cellKey }
    : null
  const nextFrame = {
    x: Math.round(moveCtx.originX + offsetX),
    y: Math.round(moveCtx.originY + offsetY),
    width: moveCtx.originW,
    height: moveCtx.originH,
  }
  const gridPatch = frameToGridPatch(nextFrame)
  movingPreviewBlock.value = {
    ...block,
    ...gridPatch,
    props: {
      ...(block.props || {}),
      style: {
        ...createDefaultBlockStyle(),
        ...(block.props?.style || {}),
        ...nextFrame,
      },
    },
  }
  if (Math.abs(rawDx) > 2 || Math.abs(rawDy) > 2)
    mut.suppressNextBlockClick = true
}
function endMove() {
  const preview = movingPreviewBlock.value
  if (moveCtx && preview) {
    const block = findBlockInTree(blocks.value, moveCtx.blockId)
    if (block && !dragBlockedBlockId.value) {
      const rect = resolveBlockFrame(preview)
      patchBlockFrame(block.id, { x: rect.x, y: rect.y, width: rect.width, height: rect.height })
    }
  }
  moveCtx = null
  movingBlockId.value = ''
  movingPreviewBlock.value = null
  movingPixelOffset.value = { x: 0, y: 0 }
  resetCanvasDragState()
  flushDeferredLayoutEmit()
  window.removeEventListener('pointermove', onMove)
  window.removeEventListener('pointerup', endMove)
}

// Resize
let resizeCtx = null
let nestedResizeCtx = null
function startResize(block, event, anchor = 'bottom-right') {
  if (props.readonly)
    return
  if (event.button !== 0)
    return
  event.preventDefault()
  const rect = resolveBlockFrame(block)
  designerStore.beginDeferLayoutEmit()
  resizeCtx = {
    blockId: block.id,
    anchor,
    startX: event.clientX,
    startY: event.clientY,
    startScrollLeft: canvasScrollRef.value?.scrollLeft || 0,
    startScrollTop: canvasScrollRef.value?.scrollTop || 0,
    originX: rect.x,
    originY: rect.y,
    originW: rect.width,
    originH: rect.height,
  }
  selectBlock(block.id)
  window.addEventListener('pointermove', onResize)
  window.addEventListener('pointerup', endResize)
}
function onResize(event) {
  if (!resizeCtx)
    return
  autoScrollCanvasOnPointer(event)
  const zoom = canvasZoom.value || 1
  const scrollEl = canvasScrollRef.value
  const scrollDx = (scrollEl?.scrollLeft || 0) - (resizeCtx.startScrollLeft || 0)
  const scrollDy = (scrollEl?.scrollTop || 0) - (resizeCtx.startScrollTop || 0)
  const dw = Math.round((event.clientX - resizeCtx.startX + scrollDx) / zoom)
  const dh = Math.round((event.clientY - resizeCtx.startY + scrollDy) / zoom)
  const block = findBlockInTree(blocks.value, resizeCtx.blockId)
  if (!block)
    return
  const anchor = resizeCtx.anchor || 'bottom-right'
  const minW = 24
  const minH = 24
  const originRight = resizeCtx.originX + resizeCtx.originW
  const originBottom = resizeCtx.originY + resizeCtx.originH
  let nextX = resizeCtx.originX
  let nextY = resizeCtx.originY
  let nextW = resizeCtx.originW
  let nextH = resizeCtx.originH

  if (anchor.includes('right')) {
    nextW = clamp(resizeCtx.originW + dw, minW, Math.max(minW, canvasGridWidth.value - resizeCtx.originX))
  }
  if (anchor.includes('left')) {
    nextX = clamp(resizeCtx.originX + dw, 0, originRight - minW)
    nextW = originRight - nextX
  }
  if (anchor.includes('bottom')) {
    nextH = Math.max(minH, resizeCtx.originH + dh)
  }
  if (anchor.includes('top')) {
    nextY = Math.max(0, Math.min(resizeCtx.originY + dh, originBottom - minH))
    nextH = originBottom - nextY
  }

  const current = resolveBlockFrame(block)
  if (current.x !== nextX || current.y !== nextY || current.width !== nextW || current.height !== nextH)
    patchBlockFrame(block.id, { x: nextX, y: nextY, width: nextW, height: nextH })
}
function endResize() {
  resizeCtx = null
  flushDeferredLayoutEmit()
  window.removeEventListener('pointermove', onResize)
  window.removeEventListener('pointerup', endResize)
}

function startNestedResize(block, event, anchor = 'bottom-right') {
  if (props.readonly || !block?.id)
    return
  if (event.button !== 0)
    return
  event.preventDefault()
  selectBlock(block.id)
  const node = event.target?.closest?.('.layout-grid-cell-child')
  const rect = node?.getBoundingClientRect?.()
  const currentStyle = block.props?.style || {}
  nestedResizeCtx = {
    blockId: block.id,
    anchor,
    startX: event.clientX,
    startY: event.clientY,
    originW: rect?.width || resolveCssNumber(currentStyle.width, 240),
    originH: rect?.height || resolveCssNumber(currentStyle.height, 96),
  }
  window.addEventListener('pointermove', onNestedResize)
  window.addEventListener('pointerup', endNestedResize)
}

function onNestedResize(event) {
  if (!nestedResizeCtx)
    return
  const zoom = canvasZoom.value || 1
  const dx = Math.round((event.clientX - nestedResizeCtx.startX) / zoom)
  const dy = Math.round((event.clientY - nestedResizeCtx.startY) / zoom)
  const anchor = nestedResizeCtx.anchor || 'bottom-right'
  const patch = {}
  if (anchor.includes('right') || anchor.includes('left')) {
    const widthDelta = anchor.includes('left') ? -dx : dx
    patch.widthMode = 'fixed'
    patch.width = Math.max(80, nestedResizeCtx.originW + widthDelta)
  }
  if (anchor.includes('bottom') || anchor.includes('top')) {
    const heightDelta = anchor.includes('top') ? -dy : dy
    patch.height = Math.max(40, nestedResizeCtx.originH + heightDelta)
  }
  if (Object.keys(patch).length)
    patchBlockStyle(nestedResizeCtx.blockId, patch)
}

function endNestedResize() {
  nestedResizeCtx = null
  window.removeEventListener('pointermove', onNestedResize)
  window.removeEventListener('pointerup', endNestedResize)
}

function flushDeferredLayoutEmit() {
  if (designerStore.takeDeferredLayoutEmit())
    emitLayoutChange()
}

function updateCanvasViewportWidth() {
  canvasViewportWidth.value = canvasScrollRef.value?.clientWidth || 0
}

onMounted(() => {
  nextTick(() => {
    updateCanvasViewportWidth()
    if (typeof ResizeObserver !== 'undefined' && canvasScrollRef.value) {
      mut.canvasResizeObserver = new ResizeObserver(updateCanvasViewportWidth)
      mut.canvasResizeObserver.observe(canvasScrollRef.value)
      return
    }
    window.addEventListener('resize', updateCanvasViewportWidth)
  })
})

onBeforeUnmount(() => {
  endMove()
  cancelNestedMove()
  endResize()
  endNestedResize()
  mut.canvasResizeObserver?.disconnect?.()
  mut.canvasResizeObserver = null
  window.removeEventListener('resize', updateCanvasViewportWidth)
})

// Field config drawer（抽屉本体已拆分至 FieldConfigDrawer.vue：选中态走 listDesigner store，写入走 patch 事件回传）
function openFieldDrawer(mode = 'table') {
  fieldDrawerMode.value = mode === 'search' ? 'search' : 'table'
  fieldDrawerInitialField.value = ''
  fieldDrawerOpen.value = true
}

function openInlineFieldDrawer(fieldName = '', mode = 'table') {
  fieldDrawerMode.value = mode === 'search' ? 'search' : 'table'
  fieldDrawerInitialField.value = fieldName
  fieldDrawerOpen.value = true
}


  return {
    ...deps,
    designerStore,
    rowHeight,
    gap,
    canvasScrollRef,
    selectedBlockId,
    fieldDrawerOpen,
    fieldDrawerMode,
    fieldDrawerInitialField,
    canvasDragActive,
    draggedBlockType,
    draggedExistingBlockId,
    dragOverCell,
    dragOverPoint,
    dragBlockedBlockId,
    activeDropCell,
    movingBlockId,
    movingPreviewBlock,
    movingPixelOffset,
    nestedMovingBlockId,
    canvasViewportWidth,
    canvasZoom,
    localLayout,
    blocks,
    designCanvasWidth,
    canvasGridWidth,
    selectedBlock,
    emitLayoutChange,
    isVerticalFrameOverlap,
    resolveBlockFrame,
    resolveBlockWidthMode,
    resolveBlockHeightMode,
    gridHeightToPixels,
    resolveCssNumber,
    frameToGridPatch,
    selectBlock,
    clearSelection,
    resetCanvasDragState,
    autoScrollCanvasOnPointer,
    resolveDropContainerFromPoint,
    resolveNonContainerDropBlock,
    normalizeGridLayoutCells,
    resolveNestedChildHeight,
    moveExistingBlockToGridCell,
    moveExistingBlockToContainer,
    moveExistingBlockToCanvas,
    pixelToCell,
    pixelToPoint,
    patchBlockProps,
    handleGridCellDragOverHint,
    handleCrudPreviewStateChange,
    crudPreviewStatusText,
    patchBlockFrame,
    tidyCanvasBlockSpacing,
    pushStackedBlocksDown,
    applyTreePanelResponsiveMainFrames,
    isTreeCrudMainBlock,
    doesFrameOverlapBlocks,
    framesOverlap,
    setBlockWidthMode,
    setBlockHeightMode,
    setBlockContentAlign,
    addBlockEvent,
    resolvePrimaryClickEvent,
    updatePrimaryClickEvent,
    updateBlockEvent,
    removeBlockEvent,
    addBlockEventParam,
    updateBlockEventParam,
    removeBlockEventParam,
    eventTriggerText,
    eventActionText,
    patchBlockStyle,
    removeBlock,
    duplicateBlock,
    duplicateNestedBlock,
    cloneBlockWithFreshIds,
    reorderBlockLayer,
    resetLayout,
    clearCanvas,
    clamp,
    normalizeDesignerLayout,
    normalizeTabsBlock,
    normalizeGridItems,
    normalizeGridItemFrame,
    resolveAutoGridH,
    resolveFieldColumnCount,
    resolveToolbarColumnCount,
    resolveToolbarActionCount,
    gridRowsForPixels,
    startMove,
    startNestedMove,
    onNestedMove,
    updateNestedMovePreview,
    endNestedMove,
    cancelNestedMove,
    cleanupNestedMove,
    resolveNestedCellDropStyle,
    onMove,
    endMove,
    startResize,
    onResize,
    endResize,
    startNestedResize,
    onNestedResize,
    endNestedResize,
    flushDeferredLayoutEmit,
    updateCanvasViewportWidth,
    openFieldDrawer,
    openInlineFieldDrawer,
  }
}
