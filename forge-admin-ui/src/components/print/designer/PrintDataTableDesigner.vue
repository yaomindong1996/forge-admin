<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import { tableCellStyle, tableFrameStyle } from '../renderers/style'
import { iterDataTableSelectionCells } from './dataTableCellStyles'
import { designerTablePreview } from './designerSample'
import PrintFieldPicker from './PrintFieldPicker.vue'

const props = defineProps({
  element: { type: Object, required: true },
  catalog: { type: Array, default: () => [] },
  context: { type: Object, default: () => ({}) },
  selected: Boolean,
  locked: Boolean,
  activeColumnId: { type: String, default: '' },
  activeColumnIds: { type: Array, default: () => [] },
})
const emit = defineEmits(['move', 'selectColumn', 'selectColumns', 'changeField', 'resizeColumn'])

const sketch = ref(null)
const fieldPickerRef = ref(null)
const fieldPickerColumnId = ref('')
const dragging = ref(null)
/** { row0, col0, row1, col1 } inclusive indices into preview rows/cols */
const range = ref(null)

const rows = computed(() => designerTablePreview(props.element, props.catalog, props.context))
const colCount = computed(() => props.element.columns?.length || 0)
const tableFrame = computed(() => {
  const sample = rows.value?.[0]?.cells?.[0]?.style || {}
  return tableFrameStyle({
    borderWidthMm: sample.borderWidthMm ?? 0.15,
    borderColor: sample.borderColor,
    borderStyle: sample.borderStyle,
  })
})

const fieldPickerValue = computed(() => {
  const column = props.element.columns?.find(item => item.id === fieldPickerColumnId.value)
  return column?.field || ''
})

const selectionBox = computed(() => {
  const box = range.value
  if (box) {
    return {
      top: Math.min(box.row0, box.row1),
      bottom: Math.max(box.row0, box.row1),
      left: Math.min(box.col0, box.col1),
      right: Math.max(box.col0, box.col1),
    }
  }
  const ids = props.activeColumnIds?.length
    ? props.activeColumnIds
    : (props.activeColumnId ? [props.activeColumnId] : [])
  if (!ids.length || !colCount.value)
    return null
  const indices = ids
    .map(id => props.element.columns.findIndex(col => col.id === id))
    .filter(index => index >= 0)
  if (!indices.length)
    return null
  return {
    top: 0,
    bottom: Math.max(0, rows.value.length - 1),
    left: Math.min(...indices),
    right: Math.max(...indices),
  }
})

/** Layout-based rect (no DOM measure / no watch feedback loop). */
const selectionRectStyle = computed(() => {
  const box = selectionBox.value
  if (!box || !colCount.value)
    return null
  const widths = props.element.columns.map(col => Number(col.widthMm) || 0)
  const total = widths.reduce((sum, w) => sum + w, 0) || 1
  const leftMm = widths.slice(0, box.left).reduce((sum, w) => sum + w, 0)
  const widthMm = widths.slice(box.left, box.right + 1).reduce((sum, w) => sum + w, 0)
  const rowCount = Math.max(1, rows.value.length)
  return {
    left: `${(leftMm / total) * 100}%`,
    width: `${(widthMm / total) * 100}%`,
    top: `${(box.top / rowCount) * 100}%`,
    height: `${((box.bottom - box.top + 1) / rowCount) * 100}%`,
  }
})

function onMove(event) {
  if (props.locked || event.button !== 0)
    return
  emit('move', event)
}

function applyColumnSelection(col0, col1, row0, row1) {
  const left = Math.min(col0, col1)
  const right = Math.max(col0, col1)
  const ids = props.element.columns.slice(left, right + 1).map(col => col.id).filter(Boolean)
  if (!ids.length)
    return
  const range = {
    top: Math.min(row0, row1),
    bottom: Math.max(row0, row1),
    left,
    right,
    rowCount: rows.value.length,
  }
  const cells = iterDataTableSelectionCells(rows.value, props.element.columns, range)
    .map(({ kind, kindIndex, columnId }) => ({ kind, kindIndex, columnId }))
  if (ids.length === 1)
    emit('selectColumn', ids[0], range, cells)
  else
    emit('selectColumns', ids, range, cells)
}

function hitCell(event) {
  const el = document.elementFromPoint(event.clientX, event.clientY)
  const host = el?.closest?.('[data-col-start]')
  if (!host || !sketch.value?.contains(host))
    return null
  const row = Number(host.getAttribute('data-row-index'))
  const colStart = Number(host.getAttribute('data-col-start'))
  const colSpan = Number(host.getAttribute('data-col-span') || 1)
  if (!Number.isFinite(row) || !Number.isFinite(colStart))
    return null
  const rect = host.getBoundingClientRect()
  if (colSpan <= 1)
    return { row, col: colStart }
  const ratio = rect.width > 0 ? (event.clientX - rect.left) / rect.width : 0
  const offset = Math.min(colSpan - 1, Math.max(0, Math.floor(ratio * colSpan)))
  return { row, col: colStart + offset }
}

function onCellDown(event, rowIndex, cell) {
  if (props.locked || event.button !== 0)
    return
  event.stopPropagation()
  const colStart = Number.isInteger(cell.colStart) ? cell.colStart : 0
  const colEnd = colStart + (cell.colSpan || 1) - 1
  dragging.value = { row0: rowIndex, col0: colStart, row1: rowIndex, col1: colEnd }
  range.value = { row0: rowIndex, col0: colStart, row1: rowIndex, col1: colEnd }
  applyColumnSelection(colStart, colEnd, rowIndex, rowIndex)
  event.currentTarget?.closest?.('.print-canvas')?.focus?.({ preventScroll: true })

  const onMovePtr = (next) => {
    if (!dragging.value)
      return
    const hit = hitCell(next)
    if (!hit)
      return
    dragging.value = { ...dragging.value, row1: hit.row, col1: hit.col }
    range.value = {
      row0: dragging.value.row0,
      col0: dragging.value.col0,
      row1: hit.row,
      col1: hit.col,
    }
    applyColumnSelection(dragging.value.col0, hit.col, dragging.value.row0, hit.row)
  }
  const onUp = () => {
    dragging.value = null
    window.removeEventListener('pointermove', onMovePtr)
    window.removeEventListener('pointerup', onUp)
  }
  window.addEventListener('pointermove', onMovePtr)
  window.addEventListener('pointerup', onUp)
}

function openFieldPicker(columnId) {
  if (props.locked || !columnId)
    return
  fieldPickerColumnId.value = columnId
  emit('selectColumn', columnId, null, [])
  range.value = null
  fieldPickerRef.value?.openPicker()
}

function onFieldPicked(field) {
  if (!fieldPickerColumnId.value || !field)
    return
  emit('changeField', { columnId: fieldPickerColumnId.value, field })
}

function columnIdOf(cell) {
  return cell.key
}

function isSelected(rowIndex, cell) {
  const box = selectionBox.value
  if (!box)
    return false
  const colStart = Number.isInteger(cell.colStart) ? cell.colStart : 0
  const colEnd = colStart + (cell.colSpan || 1) - 1
  if (rowIndex < box.top || rowIndex > box.bottom)
    return false
  return !(colEnd < box.left || colStart > box.right)
}

function isImageCell(cell) {
  return cell?.contentType === 'IMAGE' || cell?.type === 'IMAGE'
}

function colHandleStyle(index) {
  const widths = props.element.columns.map(col => Number(col.widthMm) || 0)
  const total = widths.reduce((sum, w) => sum + w, 0) || 1
  const left = widths.slice(0, index + 1).reduce((sum, w) => sum + w, 0)
  return { left: `${(left / total) * 100}%` }
}

function startColumnResize(event, index) {
  if (props.locked || event.button !== 0)
    return
  event.preventDefault()
  event.stopPropagation()
  const startWidth = props.element.columns[index]?.widthMm
  if (!Number.isFinite(startWidth))
    return
  const startX = event.clientX
  const rootRect = sketch.value?.getBoundingClientRect()
  const totalMm = props.element.columns.reduce((sum, col) => sum + (Number(col.widthMm) || 0), 0)
  if (!rootRect?.width || !totalMm)
    return
  const pxPerMm = rootRect.width / totalMm
  emit('resizeColumn', { index, widthMm: startWidth, phase: 'start' })
  const move = (next) => {
    emit('resizeColumn', {
      index,
      widthMm: Math.max(4, startWidth + (next.clientX - startX) / pxPerMm),
      phase: 'move',
    })
  }
  const up = () => {
    window.removeEventListener('pointermove', move)
    window.removeEventListener('pointerup', up)
    emit('resizeColumn', { index, phase: 'end' })
  }
  window.addEventListener('pointermove', move)
  window.addEventListener('pointerup', up)
}

onBeforeUnmount(() => {
  dragging.value = null
})
</script>

<template>
  <div class="data-table-designer" :class="{ selected, locked }">
    <button
      type="button"
      class="table-move-handle"
      title="拖动移动表格"
      aria-label="拖动移动表格"
      :disabled="locked"
      @pointerdown.stop="onMove"
    />
    <div ref="sketch" class="data-table-sketch" :style="tableFrame">
      <div
        v-for="(row, rowIndex) in rows"
        :key="row.key || rowIndex"
        class="data-table-row"
        :data-row-kind="row.kind"
      >
        <template v-for="(cell, colIndex) in row.cells" :key="`${row.key || rowIndex}-${cell.key}-${colIndex}`">
          <button
            type="button"
            class="data-table-cell"
            :class="{
              'header-cell': row.kind === 'header',
              'selected': isSelected(rowIndex, cell),
              'image': isImageCell(cell),
            }"
            :style="{ ...tableCellStyle(cell.style, { top: rowIndex === 0, left: (cell.colStart ?? colIndex) === 0 }), width: `${cell.widthMm}mm` }"
            :data-row-index="rowIndex"
            :data-col-start="cell.colStart ?? colIndex"
            :data-col-span="cell.colSpan || 1"
            :title="row.kind === 'header' && !element.headerRows?.length ? '拖拽框选多列；双击或点 ▾ 选字段' : '按住拖拽框选'"
            @pointerdown.stop="onCellDown($event, rowIndex, cell)"
            @dblclick.stop="row.kind === 'header' && !element.headerRows?.length && openFieldPicker(columnIdOf(cell))"
          >
            <img v-if="cell.src" class="cell-image" :src="cell.src" alt="">
            <template v-else>
              <span class="cell-label">{{ cell.text || (row.kind === 'header' ? '选择字段' : '') }}</span>
              <span
                v-if="row.kind === 'header' && !locked && !element.headerRows?.length"
                class="cell-caret"
                aria-hidden="true"
                @pointerdown.stop
                @click.stop="openFieldPicker(columnIdOf(cell))"
              >▾</span>
            </template>
          </button>
        </template>
      </div>
      <div
        v-for="(col, index) in element.columns"
        :key="`col-h-${col.id}`"
        class="col-resize-handle"
        :class="{ outer: index === element.columns.length - 1 }"
        :style="colHandleStyle(index)"
        title="拖动调整列宽"
        @pointerdown.stop="startColumnResize($event, index)"
      />
      <div v-if="selectionRectStyle" class="selection-rect" :style="selectionRectStyle" />
    </div>
    <div class="field-picker-host">
      <PrintFieldPicker
        ref="fieldPickerRef"
        hide-trigger
        :value="fieldPickerValue"
        :catalog="catalog"
        :only-under="element.collectionPath"
        placeholder="选择子表字段"
        @update:value="onFieldPicked"
      />
    </div>
  </div>
</template>

<style scoped>
.data-table-designer {
  position: relative;
  width: 100%;
  height: 100%;
  box-sizing: border-box;
  overflow: visible;
  cursor: default;
}
.table-move-handle {
  position: absolute;
  top: 0;
  left: 0;
  z-index: 5;
  width: 12px;
  height: 12px;
  padding: 0;
  border: 1px solid #fff;
  border-radius: 2px;
  background: var(--primary-color, #356cde);
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--primary-color, #356cde) 55%, #0f172a);
  transform: translate(-30%, -30%);
  cursor: move;
  opacity: 0;
  pointer-events: none;
  touch-action: none;
}
.data-table-designer:hover .table-move-handle,
.data-table-designer.selected .table-move-handle {
  opacity: 1;
  pointer-events: auto;
}
.table-move-handle:disabled {
  cursor: not-allowed;
  opacity: 0.4;
}
.data-table-sketch {
  position: relative;
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  overflow: visible;
  background: #fff;
}
.data-table-row {
  display: flex;
  flex: 1 1 0;
  min-height: 0;
  width: 100%;
}
.data-table-cell {
  box-sizing: border-box;
  flex: none;
  display: flex;
  align-items: center;
  gap: 2px;
  margin: 0;
  padding: 1mm;
  border-radius: 0;
  color: inherit;
  background-color: unset;
  font: inherit;
  font-size: 9pt;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: cell;
}
.data-table-cell.image {
  justify-content: center;
  padding: 0.5mm;
}
.cell-image {
  display: block;
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}
.data-table-row[data-row-kind='header'] .data-table-cell,
.header-cell {
  font-weight: 700;
  cursor: cell;
}
.header-cell .cell-label {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}
.header-cell .cell-caret {
  flex: 0 0 auto;
  color: var(--primary-color, #356cde);
  font-size: 10px;
  line-height: 1;
  cursor: pointer;
}
.data-table-cell.selected {
  outline: none;
  box-shadow: none;
}
.selection-rect {
  position: absolute;
  z-index: 4;
  box-sizing: border-box;
  border: 2px solid var(--primary-color, #356cde);
  background: color-mix(in srgb, var(--primary-color, #356cde) 6%, transparent);
  pointer-events: none;
}
.col-resize-handle {
  position: absolute;
  top: 0;
  z-index: 6;
  width: 5px;
  height: 100%;
  margin-left: -2.5px;
  cursor: col-resize;
}
.col-resize-handle.outer {
  width: 8px;
  margin-left: -4px;
}
.col-resize-handle:hover {
  background: color-mix(in srgb, var(--primary-color, #356cde) 35%, transparent);
}
.field-picker-host {
  position: absolute;
  width: 0;
  height: 0;
  overflow: visible;
  pointer-events: none;
}
.data-table-cell:hover {
  filter: brightness(0.97);
}
</style>
