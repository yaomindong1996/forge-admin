<script setup>
import {
  AddOutline,
  ArrowBackOutline,
  ArrowForwardOutline,
  ArrowRedoOutline,
  ArrowUndoOutline,
  ArrowUpOutline,
  ClipboardOutline,
  CopyOutline,
  CutOutline,
  DuplicateOutline,
  EllipsisHorizontalOutline,
  GridOutline,
  LockClosedOutline,
  LockOpenOutline,
  RemoveOutline,
  SwapHorizontalOutline,
  TrashOutline,
} from '@vicons/ionicons5'
import { NColorPicker, NDropdown, NIcon, NModal, NPopover } from 'naive-ui'
import { computed, h, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { usePrintDesignerStore } from '@/stores/print/printDesignerStore'
import { toPrintColor } from '../protocol/printColor'
import { formatPrintZoom, PRINT_ZOOM_LEVELS } from './designerView'
import { insertStaticTable } from './elementCatalog'
import PrintTableIcon from './PrintTableIcon.vue'
import PrintTableSizePicker from './PrintTableSizePicker.vue'

const store = usePrintDesignerStore()
const clearOpen = ref(false)
const tablePickerOpen = ref(false)
const scrollRef = ref(null)
const canScrollLeft = ref(false)
const canScrollRight = ref(false)
const hasSelection = computed(() => !!store.selectedIds.length)
const canEdit = computed(() => hasSelection.value && !store.hasLockedSelection)
const canStyle = computed(() => {
  if (store.tableCellIds.length && store.activeElement?.type === 'STATIC_TABLE')
    return !store.activeElement?.locked
  if (store.activeElement?.type === 'DATA_TABLE' && store.selectedTableColumns.length)
    return !store.activeElement?.locked
  if (store.activeSurface?.kind === 'TEXT' && !hasSelection.value)
    return true
  return canEdit.value
})
const multi = computed(() => store.selectedIds.length >= 2 && !store.hasLockedSelection)
const canDistribute = computed(() => store.selectedIds.length >= 3 && !store.hasLockedSelection)
const locked = computed(() => !!store.selectedElements.length && store.selectedElements.every(element => element.locked))
const canClear = computed(() => !!store.activeSurface?.elements?.length && !store.hasLockedSelection)
const styleTarget = computed(() => {
  if (store.tableCellIds.length && store.activeElement?.type === 'STATIC_TABLE')
    return store.selectedTableCells[0]
  if (store.activeElement?.type === 'STATIC_TABLE')
    return { style: store.activeElement.headerStyle || {} }
  if (store.activeElement?.type === 'DATA_TABLE' && store.selectedTableColumns.length) {
    const col = store.selectedTableColumns[0]
    const cells = store.tableSelectionCells
    if (cells?.length && cells.every(hit => hit.kind === 'header'))
      return { style: store.activeElement.headerStyle || col.headerStyle || {} }
    const range = store.tableSelectionRange
    if (range && range.top === 0 && range.bottom === 0)
      return { style: col.headerStyle || store.activeElement.headerStyle || {} }
    return col
  }
  if (store.activeElement?.type === 'DATA_TABLE')
    return { style: store.activeElement.headerStyle || {} }
  if (store.activeElement)
    return store.activeElement
  if (store.activeSurface?.kind === 'TEXT')
    return store.activeSurface
  return store.selectedElements[0] || null
})
const fontSize = computed(() => styleTarget.value?.style?.fontSizePt || 10)
const isBold = computed(() => styleTarget.value?.style?.fontWeight === 700)
const isItalic = computed(() => styleTarget.value?.style?.fontStyle === 'italic')
const isUnderline = computed(() => styleTarget.value?.style?.textDecoration === 'underline')
const textAlign = computed(() => styleTarget.value?.style?.textAlign || 'left')
const textColor = computed(() => styleTarget.value?.style?.color || '#000000')
const lineColor = computed(() => styleTarget.value?.style?.borderColor || styleTarget.value?.style?.backgroundColor || '#000000')
const backgroundColor = computed(() => styleTarget.value?.type === 'LINE'
  ? lineColor.value
  : (styleTarget.value?.style?.backgroundColor || '#ffffff'))
const borderColor = computed(() => styleTarget.value?.type === 'LINE'
  ? lineColor.value
  : (styleTarget.value?.style?.borderColor || '#000000'))
const zoomLabel = computed(() => formatPrintZoom(store.zoom))
const paperRotateLabel = computed(() => store.document.paper.orientation === 'PORTRAIT' ? '转为横向' : '转为纵向')
const zoomOptions = PRINT_ZOOM_LEVELS.map(value => ({ label: formatPrintZoom(value), key: String(value) }))

const alignOptions = computed(() => [
  { label: '左对齐', key: 'left', disabled: !multi.value },
  { label: '水平居中', key: 'center', disabled: !multi.value },
  { label: '右对齐', key: 'right', disabled: !multi.value },
  { type: 'divider', key: 'd1' },
  { label: '顶部对齐', key: 'top', disabled: !multi.value },
  { label: '垂直居中', key: 'middle', disabled: !multi.value },
  { label: '底部对齐', key: 'bottom', disabled: !multi.value },
])
const arrangeOptions = computed(() => [
  { label: '水平分散', key: 'dist-h', disabled: !canDistribute.value },
  { label: '垂直分散', key: 'dist-v', disabled: !canDistribute.value },
  { type: 'divider', key: 'd1' },
  { label: '水平间隙 2mm', key: 'gap-h-2', disabled: !multi.value },
  { label: '水平间隙 5mm', key: 'gap-h-5', disabled: !multi.value },
  { label: '垂直间隙 2mm', key: 'gap-v-2', disabled: !multi.value },
  { label: '垂直间隙 5mm', key: 'gap-v-5', disabled: !multi.value },
  { type: 'divider', key: 'd2' },
  { label: '同步宽度 · 最大', key: 'w-max', disabled: !multi.value },
  { label: '同步宽度 · 平均', key: 'w-avg', disabled: !multi.value },
  { label: '同步高度 · 最大', key: 'h-max', disabled: !multi.value },
  { label: '同步高度 · 平均', key: 'h-avg', disabled: !multi.value },
])
const layerOptions = computed(() => [
  { label: '移至顶层', key: 'front', disabled: !canEdit.value },
  { label: '上移一层', key: 'forward', disabled: !canEdit.value },
  { label: '下移一层', key: 'backward', disabled: !canEdit.value },
  { label: '移至底层', key: 'back', disabled: !canEdit.value },
])
const textAlignOptions = computed(() => [
  { label: '文字左对齐', key: 'left', disabled: !canStyle.value },
  { label: '文字居中', key: 'center', disabled: !canStyle.value },
  { label: '文字右对齐', key: 'right', disabled: !canStyle.value },
])
const moreOptions = computed(() => [
  { label: '全选当前区块', key: 'select-all', disabled: !store.activeSurface?.elements?.length },
  { label: '克隆', key: 'duplicate', disabled: !canEdit.value, icon: () => h(NIcon, null, { default: () => h(DuplicateOutline) }) },
  { label: '剪切', key: 'cut', disabled: !canEdit.value, icon: () => h(NIcon, null, { default: () => h(CutOutline) }) },
  { type: 'divider', key: 'd1' },
  { label: paperRotateLabel.value, key: 'rotate-paper', icon: () => h(NIcon, null, { default: () => h(SwapHorizontalOutline) }) },
  { label: store.showGrid ? '隐藏网格' : '显示网格', key: 'grid' },
  { label: store.snapEnabled ? '关闭吸附' : '开启吸附', key: 'snap' },
  { label: store.showGuideLines ? '隐藏参考线' : '显示参考线', key: 'guides' },
  { label: store.userGuides.length ? `清除标线 (${store.userGuides.length})` : '清除标线', key: 'clear-guides', disabled: !store.userGuides.length },
  { label: store.miniMapOpen ? '隐藏概览图' : '显示概览图', key: 'minimap' },
  { type: 'divider', key: 'd2' },
  { label: '清空当前区块', key: 'clear', disabled: !canClear.value },
])

function setColor(key, value) {
  if (!value || !canStyle.value)
    return
  store.patchSelectionStyle({ [key]: toPrintColor(value) })
}
function updateScrollState() {
  const el = scrollRef.value
  if (!el) {
    canScrollLeft.value = false
    canScrollRight.value = false
    return
  }
  canScrollLeft.value = el.scrollLeft > 2
  canScrollRight.value = el.scrollLeft + el.clientWidth < el.scrollWidth - 2
}
function scrollBar(direction) {
  const el = scrollRef.value
  if (!el)
    return
  el.scrollBy({ left: direction * Math.max(120, el.clientWidth * 0.45), behavior: 'smooth' })
}
function requestClearSurface() {
  if (!canClear.value)
    return
  clearOpen.value = true
}
function confirmClearSurface() {
  clearOpen.value = false
  store.clearActiveSurfaceElements()
}
function onInsertTable({ rows, cols }) {
  tablePickerOpen.value = false
  insertStaticTable(store, rows, cols)
}
function onAlign(key) {
  store.alignSelection(key)
}
function onArrange(key) {
  if (key === 'dist-h')
    store.distributeSelection('horizontal')
  else if (key === 'dist-v')
    store.distributeSelection('vertical')
  else if (key.startsWith('gap-h-'))
    store.spaceSelection('horizontal', Number(key.slice(6)))
  else if (key.startsWith('gap-v-'))
    store.spaceSelection('vertical', Number(key.slice(6)))
  else if (key === 'w-max')
    store.syncSelectionSize('widthMm', 'max')
  else if (key === 'w-avg')
    store.syncSelectionSize('widthMm', 'avg')
  else if (key === 'h-max')
    store.syncSelectionSize('heightMm', 'max')
  else if (key === 'h-avg')
    store.syncSelectionSize('heightMm', 'avg')
}
function onLayer(key) {
  store.moveSelectionLayer(key)
}
function onTextAlign(key) {
  store.setSelectionTextAlign(key)
}
function onMore(key) {
  if (key === 'select-all')
    store.selectAll()
  else if (key === 'duplicate')
    store.duplicateSelection()
  else if (key === 'cut')
    store.cutSelection()
  else if (key === 'rotate-paper')
    store.rotatePaper()
  else if (key === 'grid')
    store.toggleGrid()
  else if (key === 'snap')
    store.toggleSnap()
  else if (key === 'guides')
    store.toggleGuideLines()
  else if (key === 'clear-guides')
    store.clearAllUserGuides()
  else if (key === 'minimap')
    store.toggleMiniMap()
  else if (key === 'clear')
    requestClearSurface()
}

let resizeObserver
onMounted(async () => {
  await nextTick()
  updateScrollState()
  resizeObserver = typeof ResizeObserver === 'undefined'
    ? null
    : new ResizeObserver(() => updateScrollState())
  if (scrollRef.value && resizeObserver)
    resizeObserver.observe(scrollRef.value)
  window.addEventListener('resize', updateScrollState)
})
onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  window.removeEventListener('resize', updateScrollState)
})
</script>

<template>
  <div class="canvas-actionbar-shell" aria-label="画布操作">
    <button
      v-show="canScrollLeft"
      type="button"
      class="scroll-arrow"
      title="向左查看更多工具"
      aria-label="向左滚动工具栏"
      @click="scrollBar(-1)"
    >
      <NIcon :component="ArrowBackOutline" />
    </button>
    <div ref="scrollRef" class="canvas-actionbar" @scroll.passive="updateScrollState">
      <NModal
        v-model:show="clearOpen"
        preset="dialog"
        title="清空当前区块"
        positive-text="清空"
        negative-text="取消"
        type="warning"
        @positive-click="confirmClearSurface"
      >
        确认清空当前区块内的全部元素？此操作可通过撤销恢复。
      </NModal>

      <button type="button" title="撤销" aria-label="撤销" :disabled="!store.canUndo" @click="store.undo()">
        <NIcon :component="ArrowUndoOutline" />
      </button>
      <button type="button" title="重做" aria-label="重做" :disabled="!store.canRedo" @click="store.redo()">
        <NIcon :component="ArrowRedoOutline" />
      </button>

      <span class="gap" />
      <NPopover
        v-model:show="tablePickerOpen"
        trigger="click"
        placement="bottom-start"
        display-directive="show"
      >
        <template #trigger>
          <button
            type="button"
            title="插入空白表格（框选行列）"
            aria-label="插入空白表格"
            :class="{ active: tablePickerOpen }"
            :disabled="!!store.gesture"
          >
            <NIcon :component="PrintTableIcon" />
          </button>
        </template>
        <PrintTableSizePicker @pick="onInsertTable" />
      </NPopover>

      <span class="gap" />
      <NDropdown trigger="click" :options="alignOptions" @select="onAlign">
        <button type="button" title="对齐" aria-label="对齐" :disabled="!multi">
          <span class="align-glyph left"><i /><i /><i /></span>
        </button>
      </NDropdown>
      <NDropdown trigger="click" :options="arrangeOptions" @select="onArrange">
        <button type="button" title="分布与同步" aria-label="分布与同步" :disabled="!multi">
          <span class="text-glyph">排</span>
        </button>
      </NDropdown>
      <NDropdown trigger="click" :options="layerOptions" @select="onLayer">
        <button type="button" title="层级" aria-label="层级" :disabled="!canEdit">
          <NIcon :component="ArrowUpOutline" />
        </button>
      </NDropdown>

      <span class="gap" />
      <button type="button" title="字体加大" aria-label="字体加大" :disabled="!canStyle" @click="store.nudgeSelectionFontSize(1)">
        <span class="text-glyph">A+</span>
      </button>
      <button type="button" title="字体减小" aria-label="字体减小" :disabled="!canStyle" @click="store.nudgeSelectionFontSize(-1)">
        <span class="text-glyph">A-</span>
      </button>
      <button type="button" title="加粗" aria-label="加粗" :class="{ active: isBold }" :disabled="!canStyle" @click="store.toggleSelectionBold()">
        <span class="text-glyph bold">B</span>
      </button>
      <button type="button" title="斜体" aria-label="斜体" :class="{ active: isItalic }" :disabled="!canStyle" @click="store.toggleSelectionItalic()">
        <span class="text-glyph italic">I</span>
      </button>
      <button type="button" title="下划线" aria-label="下划线" :class="{ active: isUnderline }" :disabled="!canStyle" @click="store.toggleSelectionUnderline()">
        <span class="text-glyph underline">U</span>
      </button>
      <NDropdown trigger="click" :options="textAlignOptions" @select="onTextAlign">
        <button type="button" title="文字对齐" aria-label="文字对齐" :disabled="!canStyle">
          <span class="text-glyph">{{ textAlign === 'center' ? '中' : textAlign === 'right' ? '右' : '左' }}</span>
        </button>
      </NDropdown>
      <div class="color-tool" :class="{ disabled: !canStyle }" title="文字颜色">
        <NColorPicker size="small" :value="textColor" :show-alpha="false" :modes="['hex']" :disabled="!canStyle" @update:value="setColor('color', $event)" />
        <span class="color-label">A</span>
      </div>
      <div class="color-tool" :class="{ disabled: !canStyle }" title="背景颜色">
        <NColorPicker size="small" :value="backgroundColor" :show-alpha="false" :modes="['hex']" :disabled="!canStyle" @update:value="setColor('backgroundColor', $event)" />
        <span class="color-label fill">■</span>
      </div>
      <div class="color-tool" :class="{ disabled: !canStyle }" title="边框颜色">
        <NColorPicker size="small" :value="borderColor" :show-alpha="false" :modes="['hex']" :disabled="!canStyle" @update:value="setColor('borderColor', $event)" />
        <span class="color-label">▢</span>
      </div>

      <span class="gap" />
      <button type="button" :title="locked ? '解锁' : '锁定'" :aria-label="locked ? '解锁' : '锁定'" :disabled="!hasSelection" @click="store.toggleSelectionLock()">
        <NIcon :component="locked ? LockClosedOutline : LockOpenOutline" />
      </button>
      <button type="button" title="复制" aria-label="复制" :disabled="!hasSelection" @click="store.copySelection()">
        <NIcon :component="CopyOutline" />
      </button>
      <button type="button" title="粘贴" aria-label="粘贴" :disabled="!store.clipboard.length || !store.activeSurface?.elements" @click="store.pasteSelection()">
        <NIcon :component="ClipboardOutline" />
      </button>
      <button type="button" class="danger" title="删除" aria-label="删除" :disabled="!canEdit" @click="store.removeSelection()">
        <NIcon :component="TrashOutline" />
      </button>

      <span class="gap" />
      <button type="button" title="缩小" aria-label="缩小" :disabled="!store.canZoomOut" @click="store.nudgeZoom(-1)">
        <NIcon :component="RemoveOutline" />
      </button>
      <NDropdown trigger="click" :options="zoomOptions" @select="(key) => store.setZoom(Number(key))">
        <button type="button" class="zoom-value" :title="`缩放 ${zoomLabel}`" :aria-label="`缩放 ${zoomLabel}`">
          {{ zoomLabel }}
        </button>
      </NDropdown>
      <button type="button" title="放大" aria-label="放大" :disabled="!store.canZoomIn" @click="store.nudgeZoom(1)">
        <NIcon :component="AddOutline" />
      </button>
      <button type="button" :class="{ active: store.showGrid }" title="网格" aria-label="网格" @click="store.toggleGrid()">
        <NIcon :component="GridOutline" />
      </button>
      <NDropdown trigger="click" :options="moreOptions" @select="onMore">
        <button type="button" title="更多操作" aria-label="更多操作">
          <NIcon :component="EllipsisHorizontalOutline" />
        </button>
      </NDropdown>

      <span v-if="hasSelection || canStyle" class="selection-meta">{{ fontSize }}pt<span v-if="hasSelection"> · {{ store.selectedIds.length }}</span></span>
    </div>
    <button
      v-show="canScrollRight"
      type="button"
      class="scroll-arrow"
      title="向右查看更多工具"
      aria-label="向右滚动工具栏"
      @click="scrollBar(1)"
    >
      <NIcon :component="ArrowForwardOutline" />
    </button>
  </div>
</template>

<style scoped>
.canvas-actionbar-shell {
  display: flex;
  min-width: 0;
  flex: 1 1 auto;
  align-items: center;
  gap: 4px;
  max-width: 100%;
}
.scroll-arrow {
  width: 28px;
  height: 32px;
  flex: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 1px solid var(--border-light, #cbd5e1);
  border-radius: 4px;
  color: var(--text-secondary, #475569);
  background: var(--bg-primary, #fff);
  cursor: pointer;
  font-size: 16px;
}
.scroll-arrow:hover {
  color: var(--primary-color);
  border-color: color-mix(in srgb, var(--primary-color) 35%, transparent);
}
.canvas-actionbar {
  display: flex;
  min-width: 0;
  flex: 1 1 auto;
  flex-wrap: nowrap;
  align-items: center;
  gap: 2px;
  overflow-x: auto;
  scrollbar-width: none;
}
.canvas-actionbar::-webkit-scrollbar {
  display: none;
}
.gap {
  width: 1px;
  height: 18px;
  margin: 0 5px;
  flex: none;
  background: color-mix(in srgb, var(--border-light, #cbd5e1) 80%, transparent);
}
.canvas-actionbar button {
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 1px solid transparent;
  border-radius: 4px;
  color: var(--text-secondary, #475569);
  background: transparent;
  cursor: pointer;
  font-size: 18px;
  flex: none;
}
.canvas-actionbar button:hover:not(:disabled),
.canvas-actionbar button.active:not(:disabled) {
  color: var(--primary-color);
  border-color: color-mix(in srgb, var(--primary-color) 22%, transparent);
  background: color-mix(in srgb, var(--primary-color) 8%, transparent);
}
.canvas-actionbar button.danger:hover:not(:disabled) {
  color: var(--error-color, #d03050);
  border-color: color-mix(in srgb, var(--error-color, #d03050) 25%, transparent);
  background: color-mix(in srgb, var(--error-color, #d03050) 8%, transparent);
}
.canvas-actionbar button:disabled {
  opacity: 0.28;
  cursor: not-allowed;
}
.text-glyph {
  font-size: 11px;
  font-weight: 700;
  line-height: 1;
}
.text-glyph.bold {
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 14px;
  font-weight: 800;
}
.text-glyph.italic {
  font-family: Georgia, 'Times New Roman', serif;
  font-style: italic;
  font-size: 14px;
}
.text-glyph.underline {
  text-decoration: underline;
  text-underline-offset: 1px;
}
.color-tool {
  position: relative;
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: none;
  margin: 0 2px;
}
.color-tool.disabled {
  opacity: 0.28;
  pointer-events: none;
}
.color-tool :deep(.n-color-picker) {
  width: 28px;
  height: 22px;
}
.color-tool :deep(.n-color-picker-trigger) {
  width: 28px !important;
  height: 22px !important;
  padding: 0 !important;
  border: 1px solid #94a3b8;
  border-radius: 3px;
  box-shadow: inset 0 0 0 1px #fff;
}
.color-tool :deep(.n-color-picker-trigger__fill) {
  inset: 2px;
  border-radius: 1px;
}
.color-tool :deep(.n-color-picker__value),
.color-tool :deep(.n-color-picker-trigger .n-color-picker__value) {
  display: none !important;
}
.color-label {
  position: absolute;
  right: -1px;
  bottom: -1px;
  min-width: 11px;
  padding: 0 2px;
  color: #0f172a;
  background: #fff;
  border-radius: 2px;
  font-size: 10px;
  font-weight: 800;
  line-height: 1.15;
  pointer-events: none;
  box-shadow: 0 0 0 1px #cbd5e1;
}
.color-label.fill {
  font-size: 9px;
}
.zoom-value {
  min-width: 44px !important;
  width: auto !important;
  padding: 0 4px !important;
  font-size: 11px !important;
  font-weight: 700;
}
.selection-meta {
  padding-left: 6px;
  color: var(--text-tertiary, #64748b);
  font-size: 11px;
  white-space: nowrap;
  flex: none;
}
.align-glyph {
  width: 14px;
  height: 14px;
  display: flex;
  flex-direction: column;
  justify-content: space-around;
}
.align-glyph i {
  display: block;
  height: 2px;
  border-radius: 1px;
  background: currentColor;
}
.align-glyph i:nth-child(1) {
  width: 12px;
}
.align-glyph i:nth-child(2) {
  width: 7px;
}
.align-glyph i:nth-child(3) {
  width: 13px;
}
</style>
