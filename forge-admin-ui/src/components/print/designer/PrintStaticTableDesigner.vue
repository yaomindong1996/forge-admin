<script setup>
import { NInputNumber } from 'naive-ui'
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { tableCellStyle, tableFrameStyle } from '../renderers/style'
import { designerBindingText, designerCellImageRef, isDesignerImageCell } from './designerSample'
import { staticTableCellLook, staticTableIdsInRect } from './staticTable'
import { selectionBoundsFromCells } from './tableSelection'

const props = defineProps({
  node: { type: Object, required: true },
  selectedIds: { type: Array, default: () => [] },
  locked: Boolean,
  insertCount: { type: Number, default: 1 },
  catalog: { type: Array, default: () => [] },
  context: { type: Object, default: () => ({}) },
  resolveFile: { type: Function, default: undefined },
})
const emit = defineEmits(['select', 'selectRange', 'change', 'move', 'contextAction', 'setImage', 'pasteImageFile', 'resizeTrack', 'resizeImage', 'update:insertCount'])

const editing = ref('')
const draft = ref('')
const root = ref(null)
const dragging = ref(null)
const contextShow = ref(false)
const contextX = ref(0)
const contextY = ref(0)
const contextCellId = ref('')
const contextCount = ref(1)
const imageSrcMap = ref({})

const tableStyle = computed(() => ({
  gridTemplateColumns: props.node.table.columns.map(column => `${column.widthMm}mm`).join(' '),
  gridTemplateRows: props.node.table.rows.map(row => `${row.heightMm}mm`).join(' '),
  ...tableFrameStyle({
    borderWidthMm: props.node.table.cells?.[0]?.style?.borderWidthMm ?? 0.15,
    borderColor: props.node.table.cells?.[0]?.style?.borderColor,
    borderStyle: props.node.table.cells?.[0]?.style?.borderStyle,
  }),
}))

const selectedCells = computed(() => props.node.table.cells.filter(cell => props.selectedIds.includes(cell.id)))
const selectionBounds = computed(() => selectionBoundsFromCells(selectedCells.value))
const selectionRectStyle = computed(() => {
  const bounds = selectionBounds.value
  if (!bounds)
    return null
  const cols = props.node.table.columns
  const rows = props.node.table.rows
  const left = cols.slice(0, bounds.left).reduce((sum, col) => sum + col.widthMm, 0)
  const top = rows.slice(0, bounds.top).reduce((sum, row) => sum + row.heightMm, 0)
  const width = cols.slice(bounds.left, bounds.right + 1).reduce((sum, col) => sum + col.widthMm, 0)
  const height = rows.slice(bounds.top, bounds.bottom + 1).reduce((sum, row) => sum + row.heightMm, 0)
  return {
    left: `${left}mm`,
    top: `${top}mm`,
    width: `${width}mm`,
    height: `${height}mm`,
  }
})

const contextCell = computed(() => props.node.table.cells.find(item => item.id === contextCellId.value)
  || selectedCells.value[0]
  || null)
const contextIsImage = computed(() => isDesignerImageCell(contextCell.value, props.catalog))
const contextCanMerge = computed(() => props.selectedIds.length >= 2 && !props.locked)
const contextHasSelection = computed(() => props.selectedIds.length > 0)

function style(cell) {
  const look = staticTableCellLook(props.node, cell)
  return {
    ...tableCellStyle(look, { top: cell.row === 0, left: cell.column === 0 }),
    gridColumn: `${cell.column + 1} / span ${cell.colSpan}`,
    gridRow: `${cell.row + 1} / span ${cell.rowSpan}`,
    backgroundColor: look.backgroundColor || 'transparent',
  }
}

function isImageCell(cell) {
  return isDesignerImageCell(cell, props.catalog)
}

function cellText(cell) {
  if (isImageCell(cell))
    return ''
  if (cell.binding?.source === 'FIELD' || cell.binding?.source === 'SYSTEM')
    return designerBindingText(cell.binding, cell.format, props.catalog, props.context)
  if (cell.binding?.source === 'CONSTANT')
    return String(cell.binding.value ?? '')
  if (cell.text != null && cell.text !== '')
    return String(cell.text)
  return ''
}

function imageRef(cell) {
  return designerCellImageRef(cell, props.context)
}

function cellImageSrc(cell) {
  const value = imageRef(cell)
  if (!value)
    return ''
  if (value.startsWith('data:image/') || /^https?:\/\//i.test(value) || value.startsWith('blob:'))
    return value
  return imageSrcMap.value[value] || ''
}

watch(
  () => [
    props.context,
    props.node.table.cells.map(cell => (isImageCell(cell) ? `${cell.id}:${imageRef(cell)}` : '')).join('|'),
  ],
  async ([, signature], _, onCleanup) => {
    if (!signature)
      return
    let active = true
    onCleanup(() => {
      active = false
    })
    const next = { ...imageSrcMap.value }
    for (const cell of props.node.table.cells) {
      if (!isImageCell(cell))
        continue
      const value = imageRef(cell)
      if (!value || value.startsWith('data:image/') || /^https?:\/\//i.test(value) || value.startsWith('blob:'))
        continue
      if (next[value])
        continue
      if (!props.resolveFile)
        continue
      try {
        const blob = await props.resolveFile(value)
        if (!active || !(blob instanceof Blob))
          continue
        next[value] = URL.createObjectURL(blob)
      }
      catch {
        /* keep empty */
      }
    }
    if (active)
      imageSrcMap.value = next
  },
  { immediate: true },
)

function emitRange(rowA, colA, rowB, colB) {
  emit('selectRange', staticTableIdsInRect(props.node.table, rowA, colA, rowB, colB))
}

function commitEditing() {
  if (!editing.value)
    return
  const cell = props.node.table.cells.find(item => item.id === editing.value)
  const value = draft.value
  const id = editing.value
  editing.value = ''
  draft.value = ''
  if (cell && !isImageCell(cell))
    emit('change', id, value)
}

function hitFromEvent(event) {
  const el = document.elementFromPoint(event.clientX, event.clientY)
  const host = el?.closest?.('[data-cell-id]')
  if (!host || !root.value?.contains(host))
    return null
  const id = host.getAttribute('data-cell-id')
  return props.node.table.cells.find(cell => cell.id === id) || null
}

function onCellDown(event, cell) {
  if (props.locked || event.button !== 0)
    return
  event.stopPropagation()
  if (editing.value) {
    if (editing.value === cell.id)
      return
    commitEditing()
  }
  if (event.shiftKey || event.metaKey || event.ctrlKey) {
    emit('select', cell.id, true)
    return
  }
  dragging.value = {
    startRow: cell.row,
    startCol: cell.column,
    endRow: cell.row,
    endCol: cell.column,
  }
  emitRange(cell.row, cell.column, cell.row, cell.column)
  event.currentTarget?.closest?.('.print-canvas')?.focus?.({ preventScroll: true })

  const onMove = (next) => {
    if (!dragging.value)
      return
    const hit = hitFromEvent(next)
    if (!hit)
      return
    dragging.value = { ...dragging.value, endRow: hit.row, endCol: hit.column }
    emitRange(dragging.value.startRow, dragging.value.startCol, hit.row, hit.column)
  }
  const onUp = () => {
    dragging.value = null
    window.removeEventListener('pointermove', onMove)
    window.removeEventListener('pointerup', onUp)
  }
  window.addEventListener('pointermove', onMove)
  window.addEventListener('pointerup', onUp)
}

function onContextMenu(event, cell) {
  if (props.locked)
    return
  event.preventDefault()
  event.stopPropagation()
  commitEditing()
  if (!props.selectedIds.includes(cell.id))
    emit('select', cell.id, false)
  contextCellId.value = cell.id
  contextCount.value = Math.max(1, Math.floor(Number(props.insertCount) || 1))
  contextX.value = event.clientX
  contextY.value = event.clientY
  contextShow.value = true
}

function setContextCount(value) {
  const next = Math.max(1, Math.min(50, Math.floor(Number(value) || 1)))
  contextCount.value = next
  emit('update:insertCount', next)
}

function onContextSelect(key) {
  if (!key)
    return
  contextShow.value = false
  emit('contextAction', {
    key,
    cellId: contextCellId.value,
    count: Math.max(1, Math.floor(Number(contextCount.value) || 1)),
  })
}

function onContextPointerDownOutside(event) {
  if (!contextShow.value)
    return
  const menu = event.target?.closest?.('.static-table-context-menu')
  if (menu)
    return
  contextShow.value = false
}

async function startEdit(cell) {
  if (props.locked || isImageCell(cell))
    return
  dragging.value = null
  if (editing.value && editing.value !== cell.id)
    commitEditing()
  editing.value = cell.id
  draft.value = cellText(cell)
  emit('select', cell.id, false)
  await nextTick()
  const editor = root.value?.querySelector('input')
  editor?.focus()
  editor?.select()
}

function finish(cell, save = true) {
  if (editing.value !== cell.id)
    return
  const value = draft.value
  editing.value = ''
  draft.value = ''
  if (save)
    emit('change', cell.id, value)
}

function onMoveHandle(event) {
  if (props.locked || event.button !== 0)
    return
  commitEditing()
  emit('move', event)
}

function onKeyType(event, cell) {
  if (props.locked || editing.value || event.metaKey || event.ctrlKey || event.altKey || isImageCell(cell))
    return
  if (event.key.length !== 1)
    return
  event.preventDefault()
  startEdit(cell).then(() => {
    draft.value = event.key
  })
}

function onPaste(event) {
  if (props.locked)
    return
  const files = [...(event.clipboardData?.files || [])].filter(file => /^image\/(?:png|jpeg|webp)$/i.test(file.type))
  if (!files.length)
    return
  event.preventDefault()
  event.stopPropagation()
  const targetId = props.selectedIds[0] || contextCellId.value
  if (!targetId)
    return
  emit('pasteImageFile', { cellId: targetId, file: files[0] })
}

function startTrackResize(event, axis, index, edge = 'end') {
  if (props.locked || event.button !== 0)
    return
  event.preventDefault()
  event.stopPropagation()
  const tracks = axis === 'column' ? props.node.table.columns : props.node.table.rows
  const startSize = tracks[index]?.[axis === 'column' ? 'widthMm' : 'heightMm']
  if (!Number.isFinite(startSize))
    return
  const start = axis === 'column' ? event.clientX : event.clientY
  const startOrigin = axis === 'column' ? Number(props.node.xMm) : Number(props.node.yMm)
  const rootRect = root.value?.getBoundingClientRect()
  const totalMm = axis === 'column'
    ? props.node.table.columns.reduce((sum, col) => sum + col.widthMm, 0)
    : props.node.table.rows.reduce((sum, row) => sum + row.heightMm, 0)
  const spanPx = axis === 'column' ? rootRect?.width : rootRect?.height
  if (!spanPx || !totalMm)
    return
  const pxPerMm = spanPx / totalMm
  emit('resizeTrack', { axis, index, sizeMm: startSize, phase: 'start' })
  const move = (next) => {
    const deltaMm = ((axis === 'column' ? next.clientX : next.clientY) - start) / pxPerMm
    const nextSize = Math.max(4, startSize + (edge === 'start' ? -deltaMm : deltaMm))
    const applied = startSize - nextSize
    emit('resizeTrack', {
      axis,
      index,
      sizeMm: nextSize,
      originMm: edge === 'start' ? Number((startOrigin + applied).toFixed(3)) : undefined,
      phase: 'move',
    })
  }
  const up = () => {
    window.removeEventListener('pointermove', move)
    window.removeEventListener('pointerup', up)
    emit('resizeTrack', { axis, index, phase: 'end' })
  }
  window.addEventListener('pointermove', move)
  window.addEventListener('pointerup', up)
}

function imageBoxStyle(cell) {
  const style = {}
  if (Number(cell.imageWidthMm) > 0)
    style.width = `${cell.imageWidthMm}mm`
  if (Number(cell.imageHeightMm) > 0)
    style.height = `${cell.imageHeightMm}mm`
  return style
}

function startImageResize(event, cell) {
  if (props.locked || event.button !== 0 || !isImageCell(cell))
    return
  event.preventDefault()
  event.stopPropagation()
  event.currentTarget?.setPointerCapture?.(event.pointerId)
  const startX = event.clientX
  const startY = event.clientY
  const startW = cell.imageWidthMm || Math.max(8, props.node.table.columns[cell.column]?.widthMm * 0.8)
  const startH = cell.imageHeightMm || Math.max(6, props.node.table.rows[cell.row]?.heightMm - 2)
  const rootRect = root.value?.getBoundingClientRect()
  const totalW = props.node.table.columns.reduce((sum, col) => sum + col.widthMm, 0)
  const totalH = props.node.table.rows.reduce((sum, row) => sum + row.heightMm, 0)
  if (!rootRect?.width || !rootRect?.height)
    return
  const xPerMm = rootRect.width / totalW
  const yPerMm = rootRect.height / totalH
  emit('resizeImage', { cellId: cell.id, imageWidthMm: startW, imageHeightMm: startH, phase: 'start' })
  const move = (next) => {
    emit('resizeImage', {
      cellId: cell.id,
      imageWidthMm: Math.max(3, startW + (next.clientX - startX) / xPerMm),
      imageHeightMm: Math.max(3, startH + (next.clientY - startY) / yPerMm),
      phase: 'move',
    })
  }
  const up = () => {
    window.removeEventListener('pointermove', move)
    window.removeEventListener('pointerup', up)
    emit('resizeImage', { cellId: cell.id, phase: 'end' })
  }
  window.addEventListener('pointermove', move)
  window.addEventListener('pointerup', up)
}

function colHandleStyle(index) {
  const left = props.node.table.columns.slice(0, index + 1).reduce((sum, col) => sum + col.widthMm, 0)
  return { left: `${left}mm` }
}
function rowHandleStyle(index) {
  const top = props.node.table.rows.slice(0, index + 1).reduce((sum, row) => sum + row.heightMm, 0)
  return { top: `${top}mm` }
}

onBeforeUnmount(() => {
  window.removeEventListener('pointerdown', onContextPointerDownOutside, true)
  dragging.value = null
  if (editing.value)
    commitEditing()
  Object.values(imageSrcMap.value).forEach((url) => {
    if (typeof url === 'string' && url.startsWith('blob:'))
      URL.revokeObjectURL(url)
  })
})

watch(contextShow, (show) => {
  window.removeEventListener('pointerdown', onContextPointerDownOutside, true)
  if (show)
    window.addEventListener('pointerdown', onContextPointerDownOutside, true)
})
</script>

<template>
  <div class="static-table-shell" :class="{ locked }" @paste="onPaste">
    <button
      type="button"
      class="table-move-handle"
      title="拖动移动表格"
      aria-label="拖动移动表格"
      :disabled="locked"
      @pointerdown.stop="onMoveHandle"
    />
    <div ref="root" class="static-table-designer" role="grid" :style="tableStyle">
      <div
        v-for="cell in node.table.cells"
        :key="cell.id"
        role="gridcell"
        class="static-cell"
        :class="{
          selected: selectedIds.includes(cell.id),
          header: cell.row === 0,
          editing: editing === cell.id,
          image: isImageCell(cell),
        }"
        :style="style(cell)"
        :data-cell-id="cell.id"
        tabindex="0"
        @pointerdown="onCellDown($event, cell)"
        @dblclick.stop="startEdit(cell)"
        @contextmenu="onContextMenu($event, cell)"
        @keydown="onKeyType($event, cell)"
      >
        <span
          v-if="isImageCell(cell)"
          class="cell-image-box"
          :class="{ sized: Number(cell.imageWidthMm) > 0 }"
          :style="imageBoxStyle(cell)"
        >
          <img
            v-if="cellImageSrc(cell)"
            class="cell-image"
            :src="cellImageSrc(cell)"
            alt=""
          >
          <span v-else class="image-placeholder">图片</span>
          <button
            v-if="selectedIds.includes(cell.id) && !locked"
            type="button"
            class="image-resize-handle"
            title="拖动调整图片大小"
            aria-label="拖动调整图片大小"
            @pointerdown.stop="startImageResize($event, cell)"
          />
        </span>
        <input
          v-else-if="editing === cell.id"
          v-model="draft"
          aria-label="单元格内容"
          @pointerdown.stop
          @click.stop
          @blur="finish(cell)"
          @keydown.enter.prevent="finish(cell)"
          @keydown.esc.prevent="finish(cell, false)"
        >
        <template v-else-if="!isImageCell(cell)">
          {{ cellText(cell) }}
        </template>
      </div>
      <div
        class="col-resize-handle outer"
        style="left: 0"
        title="拖动调整列宽"
        @pointerdown.stop="startTrackResize($event, 'column', 0, 'start')"
      />
      <div
        v-for="(col, index) in node.table.columns"
        :key="`col-h-${col.id}`"
        class="col-resize-handle"
        :class="{ outer: index === node.table.columns.length - 1 }"
        :style="colHandleStyle(index)"
        title="拖动调整列宽"
        @pointerdown.stop="startTrackResize($event, 'column', index)"
      />
      <div
        class="row-resize-handle outer"
        style="top: 0"
        title="拖动调整行高"
        @pointerdown.stop="startTrackResize($event, 'row', 0, 'start')"
      />
      <div
        v-for="(row, index) in node.table.rows"
        :key="`row-h-${row.id}`"
        class="row-resize-handle"
        :class="{ outer: index === node.table.rows.length - 1 }"
        :style="rowHandleStyle(index)"
        title="拖动调整行高"
        @pointerdown.stop="startTrackResize($event, 'row', index)"
      />
      <div v-if="selectionRectStyle && !editing" class="selection-rect" :style="selectionRectStyle" />
    </div>

    <Teleport to="body">
      <div
        v-if="contextShow"
        class="static-table-context-menu"
        :style="{ left: `${contextX}px`, top: `${contextY}px` }"
        role="menu"
        @pointerdown.stop
        @contextmenu.prevent
      >
        <button type="button" role="menuitem" :disabled="!contextCanMerge" @click="onContextSelect('merge')">
          合并单元格
        </button>
        <button type="button" role="menuitem" :disabled="props.locked" @click="onContextSelect('split')">
          拆分单元格
        </button>
        <div class="ctx-divider" />
        <button type="button" role="menuitem" :disabled="props.locked" @click="onContextSelect('image-insert')">
          {{ contextIsImage ? '更换图片…' : '插入图片…' }}
        </button>
        <button type="button" role="menuitem" :disabled="props.locked || !contextIsImage" @click="onContextSelect('image-clear')">
          清除图片
        </button>
        <div class="ctx-divider" />
        <button
          type="button"
          class="ctx-insert-row"
          role="menuitem"
          :disabled="props.locked"
          @click="onContextSelect('row-above')"
        >
          <span>在上方插入</span>
          <NInputNumber
            size="tiny"
            :value="contextCount"
            :min="1"
            :max="50"
            :show-button="false"
            :disabled="props.locked"
            @click.stop
            @mousedown.stop
            @update:value="setContextCount"
            @keydown.enter.stop.prevent="onContextSelect('row-above')"
          />
          <span>行</span>
        </button>
        <button
          type="button"
          class="ctx-insert-row"
          role="menuitem"
          :disabled="props.locked"
          @click="onContextSelect('row-below')"
        >
          <span>在下方插入</span>
          <NInputNumber
            size="tiny"
            :value="contextCount"
            :min="1"
            :max="50"
            :show-button="false"
            :disabled="props.locked"
            @click.stop
            @mousedown.stop
            @update:value="setContextCount"
            @keydown.enter.stop.prevent="onContextSelect('row-below')"
          />
          <span>行</span>
        </button>
        <button type="button" role="menuitem" :disabled="props.locked || !contextHasSelection" @click="onContextSelect('row-delete')">
          删除所在行
        </button>
        <div class="ctx-divider" />
        <button
          type="button"
          class="ctx-insert-row"
          role="menuitem"
          :disabled="props.locked"
          @click="onContextSelect('col-left')"
        >
          <span>在左侧插入</span>
          <NInputNumber
            size="tiny"
            :value="contextCount"
            :min="1"
            :max="50"
            :show-button="false"
            :disabled="props.locked"
            @click.stop
            @mousedown.stop
            @update:value="setContextCount"
            @keydown.enter.stop.prevent="onContextSelect('col-left')"
          />
          <span>列</span>
        </button>
        <button
          type="button"
          class="ctx-insert-row"
          role="menuitem"
          :disabled="props.locked"
          @click="onContextSelect('col-right')"
        >
          <span>在右侧插入</span>
          <NInputNumber
            size="tiny"
            :value="contextCount"
            :min="1"
            :max="50"
            :show-button="false"
            :disabled="props.locked"
            @click.stop
            @mousedown.stop
            @update:value="setContextCount"
            @keydown.enter.stop.prevent="onContextSelect('col-right')"
          />
          <span>列</span>
        </button>
        <button type="button" role="menuitem" :disabled="props.locked || !contextHasSelection" @click="onContextSelect('col-delete')">
          删除所在列
        </button>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.static-table-shell {
  position: relative;
  width: 100%;
  height: 100%;
}
.table-move-handle {
  position: absolute;
  top: 0;
  left: 0;
  z-index: 8;
  width: 10px;
  height: 10px;
  padding: 0;
  border: 1px solid #fff;
  border-radius: 1px;
  background: var(--primary-color, #356cde);
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--primary-color, #356cde) 55%, #0f172a);
  cursor: grab;
}
.table-move-handle:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}
.static-table-designer {
  position: relative;
  width: 100%;
  height: 100%;
  display: grid;
  box-sizing: border-box;
  background: #fff;
  cursor: default;
}
.static-cell {
  position: relative;
  z-index: 1;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  cursor: cell;
  user-select: none;
}
.static-cell.editing {
  z-index: 4;
}
.static-cell.image {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1px;
}
.static-cell.selected {
  outline: none;
  box-shadow: none;
}
.static-cell.selected.image {
  z-index: 8;
  overflow: visible;
}
.cell-image-box {
  position: relative;
  display: inline-flex;
  max-width: 100%;
  max-height: 100%;
}
.cell-image-box:not(.sized) {
  width: 100%;
}
.cell-image {
  display: block;
  width: 100%;
  height: 100%;
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}
.image-placeholder {
  color: var(--text-tertiary, #94a3b8);
  font-size: 10px;
}
.image-resize-handle {
  position: absolute;
  right: -5px;
  bottom: -5px;
  z-index: 2;
  width: 14px;
  height: 14px;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: nwse-resize;
  touch-action: none;
}
.image-resize-handle::after {
  content: '';
  position: absolute;
  inset: 3px;
  border: 1px solid #fff;
  border-radius: 1px;
  background: var(--primary-color, #356cde);
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--primary-color, #356cde) 55%, #0f172a);
}
.selection-rect {
  position: absolute;
  z-index: 3;
  box-sizing: border-box;
  border: 2px solid var(--primary-color, #356cde);
  background: color-mix(in srgb, var(--primary-color, #356cde) 6%, transparent);
  pointer-events: none;
}
.static-cell input {
  width: 100%;
  height: 100%;
  box-sizing: border-box;
  padding: 0 2px;
  border: 0;
  outline: 0;
  color: inherit;
  background: #fff;
  font: inherit;
  text-align: inherit;
}
.col-resize-handle {
  position: absolute;
  top: 0;
  z-index: 6;
  width: 4px;
  height: 100%;
  margin-left: -2px;
  cursor: col-resize;
}
.row-resize-handle {
  position: absolute;
  left: 0;
  z-index: 6;
  width: 100%;
  height: 4px;
  margin-top: -2px;
  cursor: row-resize;
}
.col-resize-handle.outer {
  width: 8px;
  margin-left: -4px;
}
.row-resize-handle.outer {
  height: 8px;
  margin-top: -4px;
}
.col-resize-handle:hover,
.row-resize-handle:hover {
  background: color-mix(in srgb, var(--primary-color, #356cde) 35%, transparent);
}
</style>

<style>
.static-table-context-menu {
  position: fixed;
  z-index: 5200;
  min-width: 200px;
  padding: 6px;
  border: 1px solid var(--border-light, #e2e8f0);
  border-radius: 8px;
  background: var(--bg-primary, #fff);
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.16);
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.static-table-context-menu button {
  display: block;
  width: 100%;
  margin: 0;
  padding: 7px 10px;
  border: 0;
  border-radius: 5px;
  background: transparent;
  color: var(--text-primary, #0f172a);
  font-size: 12px;
  text-align: left;
  cursor: pointer;
}
.static-table-context-menu button:hover:not(:disabled) {
  background: color-mix(in srgb, var(--primary-color, #356cde) 10%, #fff);
}
.static-table-context-menu button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.static-table-context-menu .ctx-divider {
  height: 1px;
  margin: 4px 2px;
  background: var(--border-light, #e2e8f0);
}
.static-table-context-menu .ctx-insert-row {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  white-space: nowrap;
}
.static-table-context-menu .ctx-insert-row > span:first-child {
  flex: 0 0 auto;
}
.static-table-context-menu .ctx-insert-row > span:last-child {
  flex: 0 0 auto;
}
.static-table-context-menu .ctx-insert-row .n-input-number {
  width: 44px;
  flex: 0 0 44px;
}
.static-table-context-menu .ctx-insert-row .n-input-number .n-input__input-el {
  text-align: center;
  padding: 0 4px;
}
</style>
