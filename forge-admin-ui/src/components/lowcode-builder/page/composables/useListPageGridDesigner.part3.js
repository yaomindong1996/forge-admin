/** Auto-split part 3 of ListPageGridDesigner setup. */
import { nextTick } from 'vue'
import { businessObjectFields } from '@/api/business-app'
import {
  collectBlocksInTree,
  findBlockInTree,
  mapBlocksInTree,
  removeBlockFromTree,
} from '../blockTree'
import {
  LIST_PAGE_DESIGN_WIDTH,
  LIST_PAGE_GRID_COLS,
  createDefaultBlockStyle,
  createGridBlock,
  resolveListPageBlockMeta,
} from '../page-schema'
import {
  CANVAS_AUTO_SCROLL_EDGE,
  CANVAS_AUTO_SCROLL_MAX_STEP,
} from '../listDesignerOptions'
export function useListPageGridDesignerPart3(props, emit, deps = {}) {
  const {
    __impl,
    mut,
    rowHeight,
    gap,
    canvasRef,
    canvasScrollRef,
    canvasPreviewMode,
    activeTabKey,
    canvasDragActive,
    draggedBlockType,
    draggedExistingBlockId,
    dragOverCell,
    dragOverPoint,
    dragBlockedBlockId,
    activeDropCell,
    canvasZoom,
    localLayout,
    blocks,
    colWidth,
    canvasGridWidth,
    canvasScaleStyle,
    canvasZoomStageStyle,
    selectedBlock,
    resolveDetachedBlockFrame,
    NESTED_CONTAINER_BLOCK_TYPES,
    isTopLevelBlockId,
    treeSourceFields,
    treeSourceFieldsLoading,
    selectedTreeSourceValue,
    resolveBlockFrame,
    gridWidthToPixels,
    gridHeightToPixels,
    resolveCssNumber,
    frameToGridPatch,
    selectBlock,
    findExistingBlockByType,
    resolveTreeSourceObjectValue,
    resolveTreeSourceObjectId,
    unwrapListPayload,
    mapObjectFieldsToTreeFields,
    pickTreeFieldCode,
    ensureTreeSourceCatalog,
    findTreeSourceObject,
    resolveTreeSourceObject,
  } = deps
  const clamp = (...args) => __impl.clamp(...args)
  const doesFrameOverlapBlocks = (...args) => __impl.doesFrameOverlapBlocks(...args)
  const normalizeGridItems = (...args) => __impl.normalizeGridItems(...args)
async function loadTreeSourceFields(sourceValue = '') {
  const requestId = ++mut.treeSourceFieldRequestId
  const object = await resolveTreeSourceObject(sourceValue)
  const objectId = resolveTreeSourceObjectId(object)
  if (!objectId) {
    if (requestId === mut.treeSourceFieldRequestId)
      treeSourceFields.value = []
    return []
  }
  treeSourceFieldsLoading.value = true
  try {
    const res = await businessObjectFields(objectId)
    if (requestId !== mut.treeSourceFieldRequestId)
      return treeSourceFields.value
    const fields = mapObjectFieldsToTreeFields(unwrapListPayload(res?.data))
    treeSourceFields.value = fields
    return fields
  }
  catch (error) {
    if (requestId === mut.treeSourceFieldRequestId)
      treeSourceFields.value = []
    console.warn('[ListPageGridDesigner] 加载树数据源字段失败', error?.message || error)
    return []
  }
  finally {
    if (requestId === mut.treeSourceFieldRequestId)
      treeSourceFieldsLoading.value = false
  }
}
__impl.loadTreeSourceFields = loadTreeSourceFields

function buildTreeSourceDefaultProps(sourceValue = '', fields = treeSourceFields.value) {
  const object = findTreeSourceObject(sourceValue) || {}
  const sourceModelCode = String(object.objectCode || sourceValue || '').trim()
  // configKey 为空时回退 objectCode，保证能拼出 /ai/crud/{key}/tree
  const sourceConfigKey = String(object.configKey || object.objectCode || sourceValue || '').trim()
  const fieldList = Array.isArray(fields) ? fields : []
  const keyField = pickTreeFieldCode(fieldList, ['id'], [])
  const parentField = pickTreeFieldCode(fieldList, ['parentId', 'pid', 'parentCode'], [keyField])
  const labelField = pickTreeFieldCode(fieldList, ['name', 'title', 'label', 'fieldInput'], [keyField, parentField])
  const targetField = keyField
  // 右表过滤字段属于当前列表对象，切换树源时尽量保留已有配置
  const currentFilterField = selectedBlock.value?.props?.filterField
    || parentField
    || 'parentId'
  return {
    enabled: true,
    sourceModelCode,
    sourceModelName: object.objectName || sourceModelCode,
    sourceTableName: object.tableName || '',
    sourceConfigKey,
    sourceObjectId: resolveTreeSourceObjectId(object),
    treeApi: sourceConfigKey ? `get@/ai/crud/${sourceConfigKey}/tree` : '',
    treeTitle: object.objectName ? `${object.objectName}树` : (selectedBlock.value?.props?.treeTitle || ''),
    keyField: keyField || 'id',
    parentField: parentField || 'parentId',
    labelField: labelField || '',
    targetField: targetField || 'id',
    filterField: currentFilterField,
    childrenField: selectedBlock.value?.props?.childrenField || 'children',
    loadMode: selectedBlock.value?.props?.loadMode || 'full',
  }
}

async function handleTreeSourceChange(sourceValue) {
  if (!selectedBlock.value)
    return
  const nextValue = String(sourceValue || '').trim()
  if (!nextValue) {
    treeSourceFields.value = []
    patchBlockProps(selectedBlock.value.id, {
      sourceModelCode: '',
      sourceModelName: '',
      sourceTableName: '',
      sourceConfigKey: '',
      sourceObjectId: null,
      treeApi: '',
      keyField: '',
      parentField: '',
      labelField: '',
      targetField: '',
    })
    return
  }
  await ensureTreeSourceCatalog()
  const interimObject = findTreeSourceObject(nextValue) || {}
  const interimConfigKey = String(interimObject.configKey || interimObject.objectCode || nextValue).trim()
  // 先写入来源（含可用 treeApi），再异步补字段；避免中间态左侧树无 API
  treeSourceFields.value = []
  patchBlockProps(selectedBlock.value.id, {
    sourceModelCode: String(interimObject.objectCode || nextValue).trim(),
    sourceModelName: interimObject.objectName || '',
    sourceTableName: interimObject.tableName || '',
    sourceConfigKey: interimConfigKey,
    sourceObjectId: resolveTreeSourceObjectId(interimObject),
    treeApi: interimConfigKey ? `get@/ai/crud/${interimConfigKey}/tree` : '',
    keyField: '',
    parentField: '',
    labelField: '',
    targetField: '',
  })
  const fields = await loadTreeSourceFields(nextValue)
  if (!selectedBlock.value)
    return
  const current = selectedTreeSourceValue.value
  const object = findTreeSourceObject(nextValue)
  const aliases = new Set([
    nextValue,
    object?.objectCode,
    object?.configKey,
    resolveTreeSourceObjectValue(object || {}),
  ].map(value => String(value || '').trim()).filter(Boolean))
  // 加载期间用户又换了别的对象，丢弃过期结果
  if (current && !aliases.has(current))
    return
  patchBlockProps(selectedBlock.value.id, buildTreeSourceDefaultProps(nextValue, fields))
}

function handleCanvasDrop(event) {
  if (props.readonly)
    return
  const existingBlockId = event.dataTransfer?.getData('application/x-list-existing-block') || draggedExistingBlockId.value
  const blockType = event.dataTransfer?.getData('application/x-list-block')
  if (existingBlockId) {
    const container = resolveDropContainer(event)
    if (container?.block?.blockType === 'grid-layout' && container.cellKey) {
      resetCanvasDragState()
      moveExistingBlockToGridCell(existingBlockId, container.id, container.cellKey)
      return
    }
    if (container?.block && container.block.blockType !== 'grid-layout' && container.block.id !== existingBlockId) {
      resetCanvasDragState()
      moveExistingBlockToContainer(existingBlockId, container.id, container.tabKey)
      return
    }
    // 绝对画布：指针落在非容器区块上时仍落到画布坐标，允许重叠摆放（不再拦截）
    const point = pixelToPoint(event.clientX, event.clientY)
    resetCanvasDragState()
    moveExistingBlockToCanvas(existingBlockId, point)
    return
  }
  resetCanvasDragState()
  if (!blockType)
    return
  const meta = resolveListPageBlockMeta(blockType)
  if (!meta)
    return
  if (meta.unique && findExistingBlockByType(blockType))
    return
  const container = resolveDropContainer(event)
  if (container) {
    appendContainerChild(container.id, blockType, container.cellKey, container.tabKey)
    return
  }
  const point = pixelToPoint(event.clientX, event.clientY)
  appendBlock(blockType, point)
}

function handleCanvasDragEnter(event) {
  if (props.readonly)
    return
  const blockType = event.dataTransfer?.getData('application/x-list-block') || draggedBlockType.value
  const existingBlockId = event.dataTransfer?.getData('application/x-list-existing-block') || draggedExistingBlockId.value
  if (!blockType && !existingBlockId)
    return
  canvasDragActive.value = true
  draggedExistingBlockId.value = existingBlockId || ''
  draggedBlockType.value = blockType || findBlockInTree(blocks.value, existingBlockId)?.blockType || ''
}

function handleCanvasDragOver(event) {
  if (props.readonly)
    return
  const blockType = event.dataTransfer?.getData('application/x-list-block') || draggedBlockType.value
  const existingBlockId = event.dataTransfer?.getData('application/x-list-existing-block') || draggedExistingBlockId.value
  if (!blockType && !existingBlockId)
    return
  event.dataTransfer.dropEffect = 'copy'
  canvasDragActive.value = true
  draggedExistingBlockId.value = existingBlockId || ''
  draggedBlockType.value = blockType || findBlockInTree(blocks.value, existingBlockId)?.blockType || ''
  autoScrollCanvasOnPointer(event)
  const container = resolveDropContainer(event)
  // 仅容器嵌套高亮 cell；非容器区块不再标红拦截，画布可自由落点
  dragBlockedBlockId.value = ''
  activeDropCell.value = container?.block?.blockType === 'grid-layout' && container.cellKey
    ? { containerId: container.id, cellKey: container.cellKey }
    : null
  dragOverCell.value = activeDropCell.value ? null : pixelToCell(event.clientX, event.clientY)
  dragOverPoint.value = pixelToPoint(event.clientX, event.clientY)
}

function handleCanvasDragLeave(event) {
  if (props.readonly)
    return
  if (event.currentTarget?.contains?.(event.relatedTarget))
    return
  resetCanvasDragState()
}

function resetCanvasDragState() {
  canvasDragActive.value = false
  draggedBlockType.value = ''
  draggedExistingBlockId.value = ''
  dragOverCell.value = null
  dragOverPoint.value = null
  dragBlockedBlockId.value = ''
  activeDropCell.value = null
}

function resolveAutoScrollStep(distanceToEdge) {
  const ratio = clamp((CANVAS_AUTO_SCROLL_EDGE - distanceToEdge) / CANVAS_AUTO_SCROLL_EDGE, 0, 1)
  return Math.max(1, Math.round(ratio * CANVAS_AUTO_SCROLL_MAX_STEP))
}

function autoScrollCanvasOnPointer(event) {
  const scrollEl = canvasScrollRef.value
  if (!scrollEl)
    return
  const rect = scrollEl.getBoundingClientRect()
  let nextLeft = scrollEl.scrollLeft
  let nextTop = scrollEl.scrollTop

  if (event.clientX < rect.left + CANVAS_AUTO_SCROLL_EDGE) {
    nextLeft -= resolveAutoScrollStep(event.clientX - rect.left)
  }
  else if (event.clientX > rect.right - CANVAS_AUTO_SCROLL_EDGE) {
    nextLeft += resolveAutoScrollStep(rect.right - event.clientX)
  }

  if (event.clientY < rect.top + CANVAS_AUTO_SCROLL_EDGE) {
    nextTop -= resolveAutoScrollStep(event.clientY - rect.top)
  }
  else if (event.clientY > rect.bottom - CANVAS_AUTO_SCROLL_EDGE) {
    nextTop += resolveAutoScrollStep(rect.bottom - event.clientY)
  }

  nextLeft = clamp(nextLeft, 0, scrollEl.scrollWidth - scrollEl.clientWidth)
  nextTop = clamp(nextTop, 0, scrollEl.scrollHeight - scrollEl.clientHeight)
  if (nextLeft !== scrollEl.scrollLeft)
    scrollEl.scrollLeft = nextLeft
  if (nextTop !== scrollEl.scrollTop)
    scrollEl.scrollTop = nextTop
}

function appendBlock(blockType, position) {
  const meta = resolveListPageBlockMeta(blockType)
  if (!meta)
    return
  if (meta.unique && blocks.value.some(block => block.blockType === blockType))
    return
  const point = position || { x: 0, y: nextFreePixelTop() }
  const gridPatch = frameToGridPatch({
    x: point.x || 0,
    y: point.y || 0,
    width: gridWidthToPixels(meta.defaultW || 4),
    height: gridHeightToPixels(meta.defaultH || 2),
  })
  const block = createGridBlock(blockType, props.modelSchema, gridPatch)
  if (!block)
    return
  const frame = resolveCanvasDropFrame('', {
    x: clamp(Number(point.x) || 0, 0, Math.max(0, canvasGridWidth.value - gridWidthToPixels(block.gridW))),
    y: Math.max(0, Number(point.y) || 0),
    width: gridWidthToPixels(block.gridW),
    height: gridHeightToPixels(block.gridH),
  })
  Object.assign(block, frameToGridPatch(frame))
  block.props = {
    ...(block.props || {}),
    style: {
      ...createDefaultBlockStyle(),
      ...(block.props?.style || {}),
      ...frame,
    },
  }
  if (block.gridX + block.gridW > LIST_PAGE_GRID_COLS)
    block.gridX = Math.max(0, LIST_PAGE_GRID_COLS - block.gridW)
  localLayout.value = {
    ...localLayout.value,
    items: normalizeGridItems([...blocks.value, block]),
  }
  selectBlock(block.id)
}
__impl.appendBlock = appendBlock

function resolveDropContainer(event) {
  const match = resolveClosestContainerBlock(event.target)
  if (!match?.block)
    return null
  const cellNode = event.target?.closest?.('[data-grid-cell-key][data-grid-container-id]')
  const cellKey = match.block.blockType === 'grid-layout' && cellNode?.dataset?.gridContainerId === match.block.id
    ? cellNode.dataset.gridCellKey || ''
    : ''
  const tabNode = event.target?.closest?.('[data-grid-tab-key][data-grid-container-id]')
  const tabKey = match.block.blockType === 'tabs' && tabNode?.dataset?.gridContainerId === match.block.id
    ? tabNode.dataset.gridTabKey || ''
    : ''
  return {
    block: match.block,
    id: match.block.id,
    cellKey,
    tabKey,
  }
}

function resolveDropContainerFromPoint(clientX, clientY) {
  const target = document.elementFromPoint(clientX, clientY)
  const match = resolveClosestContainerBlock(target)
  if (match?.block) {
    const cellNode = target?.closest?.('[data-grid-cell-key][data-grid-container-id]')
    if (match.block.blockType === 'grid-layout' && cellNode?.dataset?.gridContainerId === match.block.id) {
      return {
        block: match.block,
        id: match.block.id,
        cellKey: cellNode.dataset.gridCellKey || '',
        cellRect: cellNode.getBoundingClientRect?.() || null,
      }
    }
    const tabNode = target?.closest?.('[data-grid-tab-key][data-grid-container-id]')
    return {
      block: match.block,
      id: match.block.id,
      cellKey: '',
      tabKey: match.block.blockType === 'tabs' && tabNode?.dataset?.gridContainerId === match.block.id
        ? tabNode.dataset.gridTabKey || ''
        : '',
    }
  }
  const cellNode = target?.closest?.('[data-grid-cell-key][data-grid-container-id]')
  if (cellNode) {
    const containerId = cellNode.dataset.gridContainerId || ''
    const block = findBlockInTree(blocks.value, containerId)
    if (block?.blockType === 'grid-layout') {
      return {
        block,
        id: block.id,
        cellKey: cellNode.dataset.gridCellKey || '',
        cellRect: cellNode.getBoundingClientRect?.() || null,
      }
    }
  }
  return null
}

function resolveNonContainerDropBlock(event, sourceId = '') {
  const target = document.elementFromPoint(event.clientX, event.clientY) || event.target
  const node = target?.closest?.('[data-block-id]')
  if (!node)
    return null
  const block = findBlockInTree(blocks.value, node.dataset?.blockId || '')
  if (!block || block.id === sourceId || isContainerBlock(block))
    return null
  return block
}

function resolveClosestContainerBlock(target) {
  let node = target?.closest?.('[data-block-id]')
  while (node) {
    const block = findBlockInTree(blocks.value, node.dataset?.blockId || '')
    if (isContainerBlock(block))
      return { node, block }
    node = node.parentElement?.closest?.('[data-block-id]')
  }
  return null
}

function isContainerBlock(block = {}) {
  return ['card', 'tabs', 'grid-layout', 'box-layout'].includes(block?.blockType)
}

function appendContainerChild(containerId, blockType, cellKey = '', tabKey = '') {
  const container = findBlockInTree(blocks.value, containerId)
  if (!container || !blockType)
    return
  // 嵌套深度保护：容器内不可再放入容器（画布 > 容器 > 容器 封顶）
  if (NESTED_CONTAINER_BLOCK_TYPES.includes(blockType) && !isTopLevelBlockId(containerId))
    return
  if (container.blockType === 'tabs') {
    appendTabChild(blockType, containerId, tabKey)
    selectBlock(containerId)
    return
  }
  if (container.blockType === 'box-layout') {
    const child = createContainerChildBlock(blockType)
    if (!child)
      return
    localLayout.value = {
      ...localLayout.value,
      items: normalizeGridItems(mapBlocksInTree(blocks.value, block => block.id === containerId
        ? { ...block, children: [...(block.children || []), child] }
        : block)),
    }
    selectBlock(child.id)
    return
  }
  if (container.blockType === 'grid-layout') {
    const child = appendGridCellChild(containerId, cellKey || normalizeGridLayoutCells(container)[0]?.key, blockType)
    selectBlock(child?.id || containerId)
    return
  }
  const child = createContainerChildBlock(blockType)
  if (!child)
    return
  localLayout.value = {
    ...localLayout.value,
    items: normalizeGridItems(mapBlocksInTree(blocks.value, block => block.id === containerId
      ? { ...block, children: [...(block.children || []), child] }
      : block)),
  }
  selectBlock(child.id)
}

function createContainerChildBlock(blockType) {
  const child = createGridBlock(blockType, props.modelSchema, { gridX: 0, gridY: 0 })
  if (!child)
    return null
  const childHeight = resolveNestedChildHeight(child)
  return {
    ...child,
    id: `${child.id}_child_${Date.now()}`,
    gridX: 0,
    gridY: 0,
    gridW: LIST_PAGE_GRID_COLS,
    gridH: Math.max(1, child.gridH || 2),
    props: {
      ...(child.props || {}),
      style: {
        ...createDefaultBlockStyle(),
        ...(child.props?.style || {}),
        x: 0,
        y: 0,
        widthMode: 'full',
        width: '100%',
        // 顶层默认 height 是 100%；嵌套进 auto 容器后百分比无参照，会表现为错误固定高。
        // 一律落成像素高（resolveNestedChildHeight 已忽略 100%）。
        heightMode: 'fixed',
        height: childHeight,
      },
    },
  }
}

function removeContainerChild(containerId, childId) {
  localLayout.value = {
    ...localLayout.value,
    items: blocks.value.map(block => block.id === containerId
      ? { ...block, children: (block.children || []).filter(child => child.id !== childId) }
      : block),
  }
}

function normalizeGridLayoutCells(block = {}) {
  if (block?.blockType !== 'grid-layout')
    return []
  const columns = Math.max(1, Math.min(24, Number(block.props?.columns || 24)))
  const cells = Array.isArray(block.props?.cells) ? block.props.cells : []
  const sourceCells = cells.length ? cells : [{ key: 'cell_1', title: '栅格 1', span: columns, children: [] }]
  return sourceCells.map((cell, index) => {
    const children = Array.isArray(cell.children) ? cell.children : []
    return {
      key: cell.key || `cell_${index + 1}`,
      title: cell.title ?? `栅格 ${index + 1}`,
      span: clamp(Number(cell.span) || 6, 1, columns),
      children,
      minHeight: Math.max(Number(cell.minHeight || 0), resolveGridCellAutoMinHeight(children)),
    }
  })
}
__impl.normalizeGridLayoutCells = normalizeGridLayoutCells

function resolveNestedChildHeight(child = {}) {
  const styleHeight = resolveCssNumber(child.props?.style?.height, 0)
  if (styleHeight)
    return styleHeight
  return Math.max(72, gridHeightToPixels(child.gridH || 2))
}

function resolveGridCellAutoMinHeight(children = []) {
  if (!children.length)
    return 120
  return children.reduce((sum, child) => sum + resolveNestedChildHeight(child), 0) + Math.max(0, children.length - 1) * 8 + 16
}

function patchGridLayoutCells(containerId, updater) {
  localLayout.value = {
    ...localLayout.value,
    items: normalizeGridItems(mapBlocksInTree(blocks.value, (block) => {
      if (block.id !== containerId || block.blockType !== 'grid-layout')
        return block
      const cells = updater(normalizeGridLayoutCells(block))
      return {
        ...block,
        props: {
          ...(block.props || {}),
          cells,
        },
      }
    })),
  }
}

function appendGridCellChild(containerId, cellKey, blockType) {
  const container = findBlockInTree(blocks.value, containerId)
  if (!container || container.blockType !== 'grid-layout' || !blockType)
    return null
  // 嵌套深度保护：容器内不可再放入容器（画布 > 容器 > 容器 封顶）
  if (NESTED_CONTAINER_BLOCK_TYPES.includes(blockType) && !isTopLevelBlockId(containerId))
    return null
  const child = createContainerChildBlock(blockType)
  if (!child)
    return null
  const targetKey = cellKey || normalizeGridLayoutCells(container)[0]?.key
  localLayout.value = {
    ...localLayout.value,
    items: normalizeGridItems(mapBlocksInTree(blocks.value, (block) => {
      if (block.id !== containerId || block.blockType !== 'grid-layout')
        return block
      const cells = normalizeGridLayoutCells(block).map((cell, index) => {
        const match = cell.key === targetKey || (!targetKey && index === 0)
        return match ? { ...cell, children: [...(cell.children || []), child] } : cell
      })
      return {
        ...block,
        props: {
          ...(block.props || {}),
          cells,
        },
      }
    })),
  }
  return child
}

function removeGridCellChild(containerId, cellKey, childId) {
  patchGridLayoutCells(containerId, cells => cells.map(cell => cell.key === cellKey
    ? { ...cell, children: (cell.children || []).filter(child => child.id !== childId) }
    : cell))
}

function moveExistingBlockToGridCell(blockId, containerId, cellKey, stylePatch = {}) {
  if (!blockId || !containerId || blockId === containerId)
    return
  const source = findBlockInTree(blocks.value, blockId)
  const container = findBlockInTree(blocks.value, containerId)
  if (!source || container?.blockType !== 'grid-layout')
    return
  const cleanedItems = removeBlockFromTree(blocks.value, blockId)
  const prevStyle = source.props?.style || {}
  const movedSource = {
    ...source,
    props: {
      ...(source.props || {}),
      style: {
        ...createDefaultBlockStyle(),
        ...prevStyle,
        ...stylePatch,
        widthMode: stylePatch.widthMode || 'full',
        width: stylePatch.width || '100%',
        heightMode: stylePatch.heightMode || 'fixed',
        height: stylePatch.height ?? resolveNestedChildHeight({
          ...source,
          props: {
            ...(source.props || {}),
            style: {
              ...prevStyle,
              height: prevStyle.height === '100%' ? '' : prevStyle.height,
            },
          },
        }),
      },
    },
  }
  const targetKey = cellKey || normalizeGridLayoutCells(container)[0]?.key
  localLayout.value = {
    ...localLayout.value,
    items: normalizeGridItems(mapBlocksInTree(cleanedItems, (block) => {
      if (block.id !== containerId || block.blockType !== 'grid-layout')
        return block
      const cells = normalizeGridLayoutCells(block).map((cell, index) => {
        const match = cell.key === targetKey || (!targetKey && index === 0)
        return match ? { ...cell, children: [...(cell.children || []), movedSource] } : cell
      })
      return {
        ...block,
        props: {
          ...(block.props || {}),
          cells,
        },
      }
    })),
  }
  selectBlock(blockId)
}

function moveExistingBlockToContainer(blockId, containerId, tabKey = '') {
  if (!blockId || !containerId || blockId === containerId)
    return
  const source = findBlockInTree(blocks.value, blockId)
  const container = findBlockInTree(blocks.value, containerId)
  if (!source || !isContainerBlock(container) || container.blockType === 'grid-layout')
    return
  if (collectBlocksInTree(source).some(block => block.id === containerId))
    return
  const movedSource = {
    ...source,
    gridX: 0,
    gridY: 0,
    gridW: LIST_PAGE_GRID_COLS,
    props: {
      ...(source.props || {}),
      style: {
        ...createDefaultBlockStyle(),
        ...(source.props?.style || {}),
        x: 0,
        y: 0,
        widthMode: 'full',
        width: '100%',
        // 拖入容器时剥掉顶层 100% 高，避免嵌套后被误当成固定撑满
        heightMode: 'fixed',
        height: resolveNestedChildHeight({
          ...source,
          props: {
            ...(source.props || {}),
            style: {
              ...(source.props?.style || {}),
              height: source.props?.style?.height === '100%' ? '' : source.props?.style?.height,
            },
          },
        }),
      },
    },
  }
  const cleanedItems = removeBlockFromTree(blocks.value, blockId)
  localLayout.value = {
    ...localLayout.value,
    items: normalizeGridItems(mapBlocksInTree(cleanedItems, (block) => {
      if (block.id !== containerId)
        return block
      if (block.blockType === 'tabs') {
        const tabs = block.props?.tabs?.length
          ? block.props.tabs
          : [{ key: 'tab_1', title: '标签 1', children: [] }]
        return {
          ...block,
          props: {
            ...(block.props || {}),
            tabs: tabs.map((tab, index) => (tab.key === tabKey || (!tabKey && index === 0))
              ? { ...tab, children: [...(tab.children || []), movedSource] }
              : tab),
          },
        }
      }
      return {
        ...block,
        children: [...(block.children || []), movedSource],
      }
    })),
  }
  selectBlock(blockId)
}

function moveExistingBlockToCanvas(blockId, point = { x: 0, y: 0 }) {
  if (!blockId)
    return
  const source = findBlockInTree(blocks.value, blockId)
  if (!source)
    return
  const meta = resolveListPageBlockMeta(source.blockType)
  const sourceFrame = resolveDetachedBlockFrame(source, meta)
  const width = Math.min(sourceFrame.width, canvasGridWidth.value)
  const height = sourceFrame.height
  const frame = resolveCanvasDropFrame(blockId, {
    x: clamp(Number(point.x) || 0, 0, Math.max(0, canvasGridWidth.value - width)),
    y: Math.max(0, Number(point.y) || 0),
    width,
    height,
  })
  const gridPatch = frameToGridPatch(frame)
  const movedBlock = {
    ...source,
    ...gridPatch,
    props: {
      ...(source.props || {}),
      style: {
        ...createDefaultBlockStyle(),
        ...(source.props?.style || {}),
        ...frame,
        widthMode: 'fixed',
        width: frame.width,
        height: frame.height,
      },
    },
  }
  localLayout.value = {
    ...localLayout.value,
    items: normalizeGridItems([...removeBlockFromTree(blocks.value, blockId), movedBlock]),
  }
  selectBlock(blockId)
}

function updateGridCell(index, patch) {
  if (!selectedBlock.value || selectedBlock.value.blockType !== 'grid-layout')
    return
  patchGridLayoutCells(selectedBlock.value.id, cells => cells.map((cell, idx) => idx === index ? { ...cell, ...patch } : cell))
}

const gridLayoutPresets = [
  {
    key: 'three-top',
    label: '上三栏',
    desc: '一行三等分',
    previewCells: [{ span: 1 }, { span: 1 }, { span: 1 }],
    thumbStyle: { gridTemplateColumns: 'repeat(3, 1fr)' },
    build: columns => ([
      { span: Math.floor(columns / 3) },
      { span: Math.floor(columns / 3) },
      { span: columns - Math.floor(columns / 3) * 2 },
    ]),
  },
  {
    key: 'two-two',
    label: '两行两列',
    desc: '2×2 宫格',
    previewCells: [{ span: 1 }, { span: 1 }, { span: 1 }, { span: 1 }],
    thumbStyle: { gridTemplateColumns: 'repeat(2, 1fr)' },
    build: columns => Array.from({ length: 4 }).map(() => ({ span: Math.floor(columns / 2) })),
  },
  {
    key: 'left-right-2',
    label: '左右两栏',
    desc: '左 1/3 + 右 2/3',
    previewCells: [{ span: 1 }, { span: 2 }],
    thumbStyle: { gridTemplateColumns: '1fr 2fr' },
    build: columns => ([
      { span: Math.floor(columns / 3) },
      { span: columns - Math.floor(columns / 3) },
    ]),
  },
  {
    key: 'sidebar-main',
    label: '左二右一',
    desc: '左侧两格叠放 + 右侧通栏',
    previewCells: [{ span: 1 }, { span: 2 }, { span: 1 }],
    thumbStyle: { gridTemplateColumns: '1fr 2fr', gridTemplateRows: '1fr 1fr' },
    // 用 CSS grid 预览近似；真实 cells：左半两行 + 右半跨两行用 span 表达不了 row-span，
    // 采用「上：左+右」「下：左+右」四格等价布局
    build: columns => {
      const left = Math.floor(columns / 3)
      const right = columns - left
      return [
        { span: left, title: '左上' },
        { span: right, title: '右上' },
        { span: left, title: '左下' },
        { span: right, title: '右下' },
      ]
    },
  },
  {
    key: 'center-stack',
    label: '中通栏',
    desc: '上中下三行通栏',
    previewCells: [{ span: 3 }, { span: 3 }, { span: 3 }],
    thumbStyle: { gridTemplateColumns: '1fr' },
    build: columns => ([
      { span: columns, title: '顶部' },
      { span: columns, title: '中部' },
      { span: columns, title: '底部' },
    ]),
  },
  {
    key: 'header-body-footer',
    label: '顶栏+双栏+底',
    desc: '上通栏、中左右、下通栏',
    previewCells: [{ span: 2 }, { span: 1 }, { span: 1 }, { span: 2 }],
    thumbStyle: { gridTemplateColumns: '1fr 1fr' },
    build: columns => ([
      { span: columns, title: '顶栏' },
      { span: Math.floor(columns / 2), title: '左侧' },
      { span: columns - Math.floor(columns / 2), title: '右侧' },
      { span: columns, title: '底栏' },
    ]),
  },
  {
    key: 'dashboard',
    label: '看板六宫',
    desc: '上三指标 + 下三卡片',
    previewCells: Array.from({ length: 6 }).map(() => ({ span: 1 })),
    thumbStyle: { gridTemplateColumns: 'repeat(3, 1fr)' },
    build: columns => Array.from({ length: 6 }).map((_, index) => ({
      span: Math.floor(columns / 3) + (index % 3 === 2 ? columns - Math.floor(columns / 3) * 3 : 0),
      title: `区域 ${index + 1}`,
    })),
  },
]

function applyGridLayoutPreset(presetKey) {
  if (!selectedBlock.value || selectedBlock.value.blockType !== 'grid-layout')
    return
  const preset = gridLayoutPresets.find(item => item.key === presetKey)
  if (!preset)
    return
  const columns = Math.max(1, Number(selectedBlock.value.props?.columns || 24))
  const previous = normalizeGridLayoutCells(selectedBlock.value)
  const nextDefs = preset.build(columns)
  const nextCells = nextDefs.map((def, index) => {
    const prev = previous[index]
    return {
      key: prev?.key || `cell_${Date.now()}_${index + 1}`,
      title: def.title || prev?.title || `栅格 ${index + 1}`,
      span: clamp(Number(def.span) || 1, 1, columns),
      children: Array.isArray(prev?.children) ? prev.children : [],
      minHeight: prev?.minHeight || Number(selectedBlock.value.props?.cellMinHeight || 120),
    }
  })
  // 多余旧格子的子组件并入最后一个格子，避免丢组件
  if (previous.length > nextCells.length) {
    const overflow = previous.slice(nextCells.length).flatMap(cell => cell.children || [])
    if (overflow.length)
      nextCells[nextCells.length - 1].children = [...(nextCells[nextCells.length - 1].children || []), ...overflow]
  }
  patchBlockProps(selectedBlock.value.id, { cells: nextCells })
}

function updateGridLayoutStructure(patch = {}) {
  if (!selectedBlock.value || selectedBlock.value.blockType !== 'grid-layout')
    return
  const current = selectedBlock.value
  const nextColumns = clamp(Number(patch.columns ?? current.props?.columns ?? 24), 1, 24)
  const nextCells = normalizeGridLayoutCells(current).map(cell => ({
    ...cell,
    span: clamp(Number(cell.span) || 1, 1, nextColumns),
  }))
  patchBlockProps(current.id, {
    ...patch,
    columns: nextColumns,
    cells: nextCells,
  })
}
__impl.updateGridLayoutStructure = updateGridLayoutStructure

function addGridCell() {
  if (!selectedBlock.value || selectedBlock.value.blockType !== 'grid-layout')
    return
  const cells = normalizeGridLayoutCells(selectedBlock.value)
  const columns = Math.max(1, Number(selectedBlock.value.props?.columns || 24))
  const usedSpan = cells.reduce((sum, cell) => sum + (Number(cell.span) || 0), 0)
  const remainder = usedSpan % columns
  const nextSpan = clamp(remainder > 0 ? columns - remainder : Math.min(6, columns), 1, columns)
  const nextCells = [
    ...cells,
    {
      key: `cell_${Date.now()}`,
      title: `栅格 ${cells.length + 1}`,
      span: nextSpan,
      children: [],
    },
  ]
  patchBlockProps(selectedBlock.value.id, {
    cells: nextCells,
  })
}

function removeGridCell(index) {
  if (!selectedBlock.value || selectedBlock.value.blockType !== 'grid-layout')
    return
  const cells = normalizeGridLayoutCells(selectedBlock.value)
  if (cells.length <= 1)
    return
  const nextCells = cells.filter((_, idx) => idx !== index)
  patchBlockProps(selectedBlock.value.id, {
    cells: nextCells,
  })
}

function appendTabChild(blockType, containerId = selectedBlock.value?.id, tabKey = '') {
  const container = findBlockInTree(blocks.value, containerId)
  if (!container || container.blockType !== 'tabs')
    return
  const child = createContainerChildBlock(blockType)
  if (!child)
    return
  const tabs = container.props?.tabs?.length
    ? container.props.tabs
    : [{ key: 'tab1', title: '标签一', children: [] }]
  const requestedKey = tabKey || activeTabKey.value
  const targetKey = tabs.some(tab => tab.key === requestedKey)
    ? requestedKey
    : tabs[0]?.key
  const nextTabs = tabs.map(tab => (tab.key === targetKey || (!targetKey && tab === tabs[0]))
    ? { ...tab, children: [...(tab.children || []), child] }
    : tab)
  localLayout.value = {
    ...localLayout.value,
    items: normalizeGridItems(mapBlocksInTree(blocks.value, block => block.id === container.id
      ? { ...block, props: { ...(block.props || {}), tabs: nextTabs } }
      : block)),
  }
  activeTabKey.value = targetKey || nextTabs[0]?.key || ''
}

function removeTabChild(childId) {
  if (!selectedBlock.value || selectedBlock.value.blockType !== 'tabs')
    return
  const tabs = (selectedBlock.value.props?.tabs || []).map(tab => tab.key === activeTabKey.value
    ? { ...tab, children: (tab.children || []).filter(child => child.id !== childId) }
    : tab)
  patchBlockProps(selectedBlock.value.id, { tabs })
}

function resolveCanvasDropFrame(sourceId = '', frame = {}) {
  if (!doesFrameOverlapBlocks(sourceId, frame))
    return frame
  return {
    ...frame,
    y: nextFreePixelTop(sourceId),
  }
}
__impl.resolveCanvasDropFrame = resolveCanvasDropFrame

function nextFreePixelTop(excludeId = '') {
  return blocks.value.reduce((acc, block) => {
    if (block.id === excludeId)
      return acc
    const rect = resolveBlockFrame(block)
    return Math.max(acc, rect.y + rect.height + gap)
  }, 0)
}

function pixelToCell(clientX, clientY) {
  const rect = canvasRef.value?.getBoundingClientRect()
  if (!rect)
    return { gridX: 0, gridY: 0 }
  const zoom = canvasZoom.value || 1
  const localX = (clientX - rect.left) / zoom
  const localY = (clientY - rect.top) / zoom
  const cellW = colWidth.value + gap
  return {
    gridX: Math.max(0, Math.min(11, Math.floor(localX / cellW))),
    gridY: Math.max(0, Math.floor(localY / (rowHeight + gap))),
  }
}

function pixelToPoint(clientX, clientY) {
  const rect = canvasRef.value?.getBoundingClientRect()
  if (!rect)
    return { x: 0, y: 0 }
  const zoom = canvasZoom.value || 1
  return {
    x: Math.max(0, Math.round((clientX - rect.left) / zoom)),
    y: Math.max(0, Math.round((clientY - rect.top) / zoom)),
  }
}

function patchBlock(id, patch) {
  localLayout.value = {
    ...localLayout.value,
    items: normalizeGridItems(mapBlocksInTree(blocks.value, b => b.id === id
      ? {
          ...b,
          ...patch,
          gridX: clamp(patch.gridX ?? b.gridX, 0, LIST_PAGE_GRID_COLS - 1),
          gridW: clamp(patch.gridW ?? b.gridW, 1, LIST_PAGE_GRID_COLS),
        }
      : b)),
  }
}
__impl.patchBlock = patchBlock

function updateDesignWidth(value) {
  const nextWidth = clamp(value || LIST_PAGE_DESIGN_WIDTH, 375, 2560)
  localLayout.value = {
    ...localLayout.value,
    designWidth: nextWidth,
  }
}

function applyCanvasPreviewMode(value = 'desktop') {
  const modeMap = {
    desktop: { width: 1366, zoom: 1 },
    narrow: { width: 768, zoom: 0.9 },
    modal: { width: 960, zoom: 0.9 },
    drawer: { width: 720, zoom: 0.9 },
    mobile: { width: 390, zoom: 1 },
  }
  const next = modeMap[value] || modeMap.desktop
  canvasPreviewMode.value = value
  updateDesignWidth(next.width)
  updateCanvasZoom(next.zoom)
}

function updateCanvasZoom(value) {
  const next = clamp(Number(value) || 1, 0.5, 1.25)
  const scrollEl = canvasScrollRef.value
  const canvasEl = canvasRef.value
  // 无滚动容器 / readonly（无滚动条）/ 实际无变化：直接赋值
  if (!scrollEl || !canvasEl || props.readonly || Math.abs(next - canvasZoom.value) < 0.001) {
    canvasZoom.value = next
    return
  }
  const oldZoom = canvasZoom.value
  // 记录缩放前：视口中心对应的画布点（未缩放坐标）
  const scrollRect = scrollEl.getBoundingClientRect()
  const canvasRect = canvasEl.getBoundingClientRect()
  const viewCenterX = scrollRect.left + scrollRect.width / 2
  const viewCenterY = scrollRect.top + scrollRect.height / 2
  const anchorX = (viewCenterX - canvasRect.left) / oldZoom
  const anchorY = (viewCenterY - canvasRect.top) / oldZoom
  canvasZoom.value = next
  nextTick(() => {
    // DOM 已更新：canvasScaleStyle / canvasZoomStageStyle 已生效
    const newCanvasRect = canvasEl.getBoundingClientRect()
    // 同一画布点的新视口位置
    const newX = newCanvasRect.left + anchorX * next
    const newY = newCanvasRect.top + anchorY * next
    // 补偿滚动：让该画布点回到视口中心
    scrollEl.scrollLeft += newX - viewCenterX
    scrollEl.scrollTop += newY - viewCenterY
  })
}

function handleCanvasWheel(event) {
  if (!event.ctrlKey && !event.metaKey)
    return
  event.preventDefault()
  const delta = event.deltaY > 0 ? -0.08 : 0.08
  updateCanvasZoom(Number((canvasZoom.value + delta).toFixed(2)))
}

function patchBlockProps(id, patch) {
  localLayout.value = {
    ...localLayout.value,
    items: normalizeGridItems(mapBlocksInTree(blocks.value, b => b.id === id
      ? { ...b, props: { ...(b.props || {}), ...patch } }
      : b)),
  }
}
__impl.patchBlockProps = patchBlockProps

function handleBlockPropsUpdate(payload = {}) {
  if (!payload.blockId || !payload.propsData)
    return
  patchBlockProps(payload.blockId, payload.propsData)
}

function handleTabsActiveChange(payload = {}) {
  if (!payload.blockId || !payload.tabKey)
    return
  activeTabKey.value = payload.tabKey
  // 目录拖拽中不要抢焦点，否则拖进 tabs 会抖选中态
  if (draggedBlockType.value)
    return
  selectBlock(payload.blockId)
}

function handleTabDrop({ blockId, tabKey, blockType } = {}) {
  if (!blockId || !tabKey || !blockType)
    return
  appendTabChild(blockType, blockId, tabKey)
  selectBlock(blockId)
}

function handleGridCellDrop({ blockId, cellKey, blockType } = {}) {
  if (!blockId || !blockType)
    return
  appendGridCellChild(blockId, cellKey, blockType)
  activeDropCell.value = null
  selectBlock(blockId)
}

function handleContainerInsert(payload = {}) {
  const blockId = String(payload.blockId || '').trim()
  const blockType = String(payload.blockType || '').trim()
  if (!blockId || !blockType)
    return
  appendContainerChild(blockId, blockType, payload.cellKey || '', payload.tabKey || '')
}

function handleContainerClear(payload = {}) {
  const blockId = String(payload.blockId || '').trim()
  if (!blockId)
    return
  const container = findBlockInTree(blocks.value, blockId)
  if (!container)
    return
  if (container.blockType === 'grid-layout') {
    const cellKey = String(payload.cellKey || '')
    patchGridLayoutCells(blockId, cells => cells.map(cell => (
      !cellKey || cell.key === cellKey
        ? { ...cell, children: [] }
        : cell
    )))
    selectBlock(blockId)
    return
  }
  if (container.blockType === 'tabs') {
    const tabKey = String(payload.tabKey || '')
    localLayout.value = {
      ...localLayout.value,
      items: normalizeGridItems(mapBlocksInTree(blocks.value, block => block.id !== blockId
        ? block
        : {
            ...block,
            props: {
              ...(block.props || {}),
              tabs: (block.props?.tabs || []).map(tab => (
                !tabKey || tab.key === tabKey
                  ? { ...tab, children: [] }
                  : tab
              )),
            },
          })),
    }
    selectBlock(blockId)
    return
  }
  localLayout.value = {
    ...localLayout.value,
    items: normalizeGridItems(mapBlocksInTree(blocks.value, block => block.id === blockId
      ? { ...block, children: [] }
      : block)),
  }
  selectBlock(blockId)
}


  return {
    ...deps,
    rowHeight,
    gap,
    canvasRef,
    canvasScrollRef,
    canvasPreviewMode,
    activeTabKey,
    canvasDragActive,
    draggedBlockType,
    draggedExistingBlockId,
    dragOverCell,
    dragOverPoint,
    dragBlockedBlockId,
    activeDropCell,
    canvasZoom,
    localLayout,
    blocks,
    colWidth,
    canvasGridWidth,
    canvasScaleStyle,
    canvasZoomStageStyle,
    selectedBlock,
    resolveDetachedBlockFrame,
    NESTED_CONTAINER_BLOCK_TYPES,
    isTopLevelBlockId,
    treeSourceFields,
    treeSourceFieldsLoading,
    selectedTreeSourceValue,
    resolveBlockFrame,
    gridWidthToPixels,
    gridHeightToPixels,
    resolveCssNumber,
    frameToGridPatch,
    selectBlock,
    findExistingBlockByType,
    resolveTreeSourceObjectValue,
    resolveTreeSourceObjectId,
    unwrapListPayload,
    mapObjectFieldsToTreeFields,
    pickTreeFieldCode,
    ensureTreeSourceCatalog,
    findTreeSourceObject,
    resolveTreeSourceObject,
    loadTreeSourceFields,
    buildTreeSourceDefaultProps,
    handleTreeSourceChange,
    handleCanvasDrop,
    handleCanvasDragEnter,
    handleCanvasDragOver,
    handleCanvasDragLeave,
    resetCanvasDragState,
    resolveAutoScrollStep,
    autoScrollCanvasOnPointer,
    appendBlock,
    resolveDropContainer,
    resolveDropContainerFromPoint,
    resolveNonContainerDropBlock,
    resolveClosestContainerBlock,
    isContainerBlock,
    appendContainerChild,
    createContainerChildBlock,
    removeContainerChild,
    normalizeGridLayoutCells,
    resolveNestedChildHeight,
    resolveGridCellAutoMinHeight,
    patchGridLayoutCells,
    appendGridCellChild,
    removeGridCellChild,
    moveExistingBlockToGridCell,
    moveExistingBlockToContainer,
    moveExistingBlockToCanvas,
    updateGridCell,
    gridLayoutPresets,
    applyGridLayoutPreset,
    updateGridLayoutStructure,
    addGridCell,
    removeGridCell,
    appendTabChild,
    removeTabChild,
    resolveCanvasDropFrame,
    nextFreePixelTop,
    pixelToCell,
    pixelToPoint,
    patchBlock,
    updateDesignWidth,
    applyCanvasPreviewMode,
    updateCanvasZoom,
    handleCanvasWheel,
    patchBlockProps,
    handleBlockPropsUpdate,
    handleTabsActiveChange,
    handleTabDrop,
    handleGridCellDrop,
    handleContainerInsert,
    handleContainerClear,
  }
}
