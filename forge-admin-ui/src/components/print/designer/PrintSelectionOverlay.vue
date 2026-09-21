<script setup>
import {
  AddOutline,
  CopyOutline,
  DuplicateOutline,
  RefreshOutline,
  RemoveOutline,
  TextOutline,
  TrashOutline,
} from '@vicons/ionicons5'
import { NColorPicker, NDropdown, NIcon } from 'naive-ui'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { usePrintDesignerStore } from '@/stores/print/printDesignerStore'
import { toPrintColor } from '../protocol/printColor'
import { resizeHandlesForElement, selectionBounds } from './commands'
import { DEFAULT_PRINT_FONT, PRINT_FONT_OPTIONS, printFontLabel } from './printFonts'
import { dataTableRangePaperBounds, staticTableCellPaperBounds } from './tableSelection'
import { usePrintResize } from './usePrintResize'

const store = usePrintDesignerStore()
const resize = usePrintResize(store)
const frameRef = ref(null)
const handleHostRef = ref(null)
const chromePos = ref(null)
const handlePos = ref(null)
const followCells = computed(() => {
  const el = store.activeElement
  return (el?.type === 'STATIC_TABLE' && store.tableCellIds.length > 0)
    || (el?.type === 'DATA_TABLE' && !!store.tableSelectionRange)
})
const handleBounds = computed(() => selectionBounds(store.selectedElements))
const bounds = computed(() => {
  const el = store.activeElement
  if (el?.type === 'STATIC_TABLE' && store.selectedTableCells.length)
    return staticTableCellPaperBounds(el, store.selectedTableCells) || handleBounds.value
  if (el?.type === 'DATA_TABLE' && store.tableSelectionRange)
    return dataTableRangePaperBounds(el, store.tableSelectionRange) || handleBounds.value
  return handleBounds.value
})
const single = computed(() => store.selectedIds.length === 1 && !!store.activeElement && !store.activeElement.locked)
const canEdit = computed(() => !!store.selectedIds.length && !store.hasLockedSelection)
const canStyle = computed(() => {
  if (!canEdit.value)
    return false
  const type = store.activeElement?.type
  return !type || ['TEXT', 'PAGE_NUMBER', 'HTML', 'BARCODE', 'QRCODE', 'STATIC_TABLE', 'DATA_TABLE'].includes(type)
})
const style = computed(() => store.activeElement?.style || {})
const fontSize = computed(() => style.value.fontSizePt || 10)
const fontLabel = computed(() => printFontLabel(style.value.fontFamily || DEFAULT_PRINT_FONT))
const sizeLabel = computed(() => {
  if (!bounds.value)
    return ''
  return `${bounds.value.widthMm.toFixed(1)}mm × ${bounds.value.heightMm.toFixed(1)}mm`
})

const handleMeta = {
  nw: { label: '左上角', cursor: 'nwse-resize' },
  n: { label: '上边', cursor: 'ns-resize' },
  ne: { label: '右上角', cursor: 'nesw-resize' },
  e: { label: '右边', cursor: 'ew-resize' },
  se: { label: '右下角', cursor: 'nwse-resize' },
  s: { label: '下边', cursor: 'ns-resize' },
  sw: { label: '左下角', cursor: 'nesw-resize' },
  w: { label: '左边', cursor: 'ew-resize' },
}
const handles = computed(() => {
  if (!single.value)
    return []
  const keys = resizeHandlesForElement(store.activeElement)
  const line = store.activeElement?.type === 'LINE'
  return keys.map(key => ({
    key,
    ...handleMeta[key],
    bar: line && (key === 'n' || key === 's' || key === 'e' || key === 'w'),
  }))
})

const styleOptions = computed(() => [
  { label: '加粗', key: 'bold' },
  { label: '斜体', key: 'italic' },
  { label: '下划线', key: 'underline' },
  { label: '删除线', key: 'line-through' },
  { label: '上划线', key: 'overline' },
])
const hAlignOptions = computed(() => [
  { label: '左对齐', key: 'left' },
  { label: '居中', key: 'center' },
  { label: '右对齐', key: 'right' },
])
const vAlignOptions = computed(() => [
  { label: '顶部对齐', key: 'top' },
  { label: '垂直居中', key: 'middle' },
  { label: '底部对齐', key: 'bottom' },
])
const layerOptions = computed(() => [
  { label: '移至顶层', key: 'front' },
  { label: '上移一层', key: 'forward' },
  { label: '下移一层', key: 'backward' },
  { label: '移至底层', key: 'back' },
])
const rotateOptions = computed(() => [
  { label: '向左旋转 90°', key: 'left' },
  { label: '向右旋转 90°', key: 'right' },
  { label: '重置旋转', key: 'reset' },
])
const fontOptions = computed(() => PRINT_FONT_OPTIONS.map(item => ({
  label: item.label,
  key: item.value,
  props: { style: `font-family: ${item.value}` },
})))
const colorQuick = computed(() => [
  { label: '黑色', key: '#000000' },
  { label: '红色', key: '#d93838' },
])
const bgQuick = computed(() => [
  { label: '黄色', key: '#fff59d' },
  { label: '无填充', key: 'transparent' },
])

function onStyle(key) {
  if (key === 'bold')
    store.toggleSelectionBold()
  else if (key === 'italic')
    store.toggleSelectionItalic()
  else if (key === 'underline')
    store.setSelectionTextDecoration(style.value.textDecoration === 'underline' ? 'none' : 'underline')
  else if (key === 'line-through')
    store.setSelectionTextDecoration(style.value.textDecoration === 'line-through' ? 'none' : 'line-through')
  else if (key === 'overline')
    store.setSelectionTextDecoration(style.value.textDecoration === 'overline' ? 'none' : 'overline')
}

function onRotate(key) {
  if (key === 'left')
    store.rotateSelection(-90)
  else if (key === 'right')
    store.rotateSelection(90)
  else if (key === 'reset')
    store.patchSelected({ rotationDeg: 0 })
}

function setColor(key, value) {
  if (!value || !canStyle.value)
    return
  store.patchSelectionStyle({ [key]: toPrintColor(value) })
}

function updateChromePos() {
  const el = frameRef.value
  if (!el || !bounds.value) {
    chromePos.value = null
    return
  }
  const rect = el.getBoundingClientRect()
  chromePos.value = {
    left: rect.left + rect.width / 2,
    top: rect.top,
    bottom: rect.bottom,
  }
}

function updateHandlePos() {
  const el = handleHostRef.value
  if (!el || !handleBounds.value || store.previewOpen) {
    handlePos.value = null
    return
  }
  const rect = el.getBoundingClientRect()
  handlePos.value = {
    left: rect.left,
    top: rect.top,
    width: rect.width,
    height: rect.height,
  }
}

let raf = 0
function schedulePos() {
  cancelAnimationFrame(raf)
  raf = requestAnimationFrame(() => nextTick(() => {
    updateChromePos()
    updateHandlePos()
  }))
}

onMounted(() => {
  schedulePos()
  window.addEventListener('scroll', schedulePos, true)
  window.addEventListener('resize', schedulePos)
})
onBeforeUnmount(() => {
  cancelAnimationFrame(raf)
  window.removeEventListener('scroll', schedulePos, true)
  window.removeEventListener('resize', schedulePos)
})
watch(handleHostRef, () => schedulePos())
watch(() => [bounds.value, handleBounds.value, store.zoom, store.selectedIds.join(','), store.tableCellIds.join(','), store.tableSelectionRange, store.gesture, store.previewOpen], () => {
  if (store.previewOpen) {
    chromePos.value = null
    handlePos.value = null
    return
  }
  schedulePos()
})
</script>

<template>
  <div
    v-if="bounds && store.selectedIds.length"
    ref="frameRef"
    class="selection-chrome"
    :style="{ left: `${bounds.xMm}mm`, top: `${bounds.yMm}mm`, width: `${bounds.widthMm}mm`, height: `${bounds.heightMm}mm` }"
  >
    <div class="selection-frame" />
  </div>
  <div
    v-if="handleBounds && single"
    ref="handleHostRef"
    class="selection-chrome handle-host"
    :style="{ left: `${handleBounds.xMm}mm`, top: `${handleBounds.yMm}mm`, width: `${handleBounds.widthMm}mm`, height: `${handleBounds.heightMm}mm` }"
  />

  <Teleport to="body">
    <div
      v-if="single && !store.previewOpen"
      class="ele-resize-layer"
      :style="{ left: `${handlePos?.left || 0}px`, top: `${handlePos?.top || 0}px`, width: `${handlePos?.width || 0}px`, height: `${handlePos?.height || 0}px` }"
    >
      <button
        v-for="handle in handles"
        :key="handle.key"
        type="button"
        class="ele-resize-handle"
        :class="[handle.key, { bar: handle.bar }]"
        :style="{ cursor: handle.cursor }"
        :aria-label="handle.label"
        :title="handle.label"
        @pointerdown.stop="resize.start($event, handle.key)"
      />
    </div>
    <template v-if="chromePos && canEdit">
      <div
        class="ele-toolbar"
        :style="{ left: `${chromePos.left}px`, top: `${chromePos.top}px` }"
        @pointerdown.stop
      >
        <template v-if="canStyle && single">
          <button type="button" class="et-tool" title="字体加大" @click="store.nudgeSelectionFontSize(1)">
            <NIcon :component="AddOutline" :size="14" />
          </button>
          <span class="et-display">{{ fontSize }}</span>
          <button type="button" class="et-tool" title="字体减小" @click="store.nudgeSelectionFontSize(-1)">
            <NIcon :component="RemoveOutline" :size="14" />
          </button>
          <NDropdown trigger="click" :options="styleOptions" @select="onStyle">
            <button type="button" class="et-tool" title="字体样式">
              <NIcon :component="TextOutline" :size="14" />
            </button>
          </NDropdown>
          <span class="et-gap" />
          <NDropdown trigger="click" :options="hAlignOptions" @select="store.setSelectionTextAlign">
            <button type="button" class="et-tool" title="水平对齐">
              ⇔
            </button>
          </NDropdown>
          <NDropdown trigger="click" :options="vAlignOptions" @select="store.setSelectionVerticalAlign">
            <button type="button" class="et-tool" title="垂直对齐">
              ⇕
            </button>
          </NDropdown>
          <span class="et-gap" />
          <NDropdown trigger="click" :options="colorQuick" @select="setColor('color', $event)">
            <button type="button" class="et-tool et-color" title="字体颜色">
              <NColorPicker
                :value="style.color || '#000000'"
                :show-alpha="false"
                :modes="['hex']"
                size="small"
                @update:value="setColor('color', $event)"
                @click.stop
              />
            </button>
          </NDropdown>
          <NDropdown trigger="click" :options="bgQuick" @select="setColor('backgroundColor', $event)">
            <button type="button" class="et-tool et-color" title="背景颜色">
              <NColorPicker
                :value="style.backgroundColor && style.backgroundColor !== 'transparent' ? style.backgroundColor : '#ffffff'"
                :show-alpha="false"
                :modes="['hex']"
                size="small"
                @update:value="setColor('backgroundColor', $event)"
                @click.stop
              />
            </button>
          </NDropdown>
          <NDropdown trigger="click" :options="fontOptions" @select="store.setSelectionFontFamily">
            <button type="button" class="et-tool et-font" :title="`字体 · ${fontLabel}`">
              <span>{{ fontLabel }}</span>
            </button>
          </NDropdown>
          <span class="et-gap" />
        </template>
        <NDropdown trigger="click" :options="rotateOptions" @select="onRotate">
          <button type="button" class="et-tool" title="旋转">
            <NIcon :component="RefreshOutline" :size="14" />
          </button>
        </NDropdown>
        <NDropdown trigger="click" :options="layerOptions" @select="store.moveSelectionLayer">
          <button type="button" class="et-tool" title="层级">
            层
          </button>
        </NDropdown>
        <button type="button" class="et-tool" title="克隆" @click="store.duplicateSelection()">
          <NIcon :component="DuplicateOutline" :size="14" />
        </button>
        <button type="button" class="et-tool" title="复制" @click="store.copySelection()">
          <NIcon :component="CopyOutline" :size="14" />
        </button>
        <button type="button" class="et-tool danger" title="删除" @click="store.removeSelection()">
          <NIcon :component="TrashOutline" :size="14" />
        </button>
      </div>
      <div
        v-if="single && !followCells"
        class="ele-size-box"
        :style="{ left: `${chromePos.left}px`, top: `${chromePos.bottom}px` }"
        @pointerdown.stop
      >
        <span class="et-sb-text">{{ sizeLabel }}</span>
        <span class="et-sb-divider" />
        <span class="et-sb-group">
          <span class="et-sb-label">宽</span>
          <button type="button" class="et-sb-btn" @click="store.nudgeSelectionSize('widthMm', -1)">−</button>
          <button type="button" class="et-sb-btn" @click="store.nudgeSelectionSize('widthMm', 1)">+</button>
        </span>
        <span class="et-sb-group">
          <span class="et-sb-label">高</span>
          <button type="button" class="et-sb-btn" @click="store.nudgeSelectionSize('heightMm', -1)">−</button>
          <button type="button" class="et-sb-btn" @click="store.nudgeSelectionSize('heightMm', 1)">+</button>
        </span>
      </div>
    </template>
  </Teleport>
</template>

<style scoped>
.selection-chrome {
  position: absolute;
  z-index: 4;
  pointer-events: none;
  box-sizing: border-box;
}
.handle-host {
  z-index: 3;
}
.selection-frame {
  position: absolute;
  inset: 0;
  box-shadow: inset 0 0 0 1px var(--primary-color, #356cde);
  pointer-events: none;
}
</style>

<style>
.ele-resize-layer {
  position: fixed;
  z-index: 42;
  pointer-events: none;
}
.ele-resize-handle {
  position: absolute;
  z-index: 1;
  width: 14px;
  height: 14px;
  padding: 0;
  border: 0;
  background: transparent;
  pointer-events: auto;
  touch-action: none;
}
.ele-resize-handle::after {
  content: '';
  position: absolute;
  inset: 3px;
  border: 1.5px solid #356cde;
  border-radius: 2px;
  background: #fff;
  box-shadow: 0 1px 3px rgb(15 23 42 / 18%);
}
.ele-resize-handle:hover::after {
  background: #356cde;
}
.ele-resize-handle.nw {
  top: -8px;
  left: -8px;
}
.ele-resize-handle.n {
  top: -8px;
  left: 50%;
  margin-left: -7px;
}
.ele-resize-handle.ne {
  top: -8px;
  right: -8px;
}
.ele-resize-handle.e {
  top: 50%;
  right: -8px;
  margin-top: -7px;
}
.ele-resize-handle.se {
  right: -8px;
  bottom: -8px;
}
.ele-resize-handle.s {
  bottom: -8px;
  left: 50%;
  margin-left: -7px;
}
.ele-resize-handle.sw {
  left: -8px;
  bottom: -8px;
}
.ele-resize-handle.w {
  top: 50%;
  left: -8px;
  margin-top: -7px;
}
.ele-resize-handle.bar.n,
.ele-resize-handle.bar.s {
  width: 20px;
  height: 12px;
  margin-left: -10px;
}
.ele-resize-handle.bar.e,
.ele-resize-handle.bar.w {
  width: 12px;
  height: 20px;
  margin-top: -10px;
}
.ele-resize-handle.bar::after {
  inset: 3px;
  border-radius: 1px;
}
.ele-toolbar {
  position: fixed;
  /* Below Naive UI overlays (~2000) so dropdowns / color panels aren't covered */
  z-index: 40;
  display: flex;
  align-items: center;
  gap: 2px;
  max-width: min(560px, 94vw);
  padding: 2px 3px;
  border: 1px solid #d0d7e2;
  border-radius: 5px;
  background: #fff;
  box-shadow: 0 4px 12px rgb(15 23 42 / 12%);
  transform: translate(-50%, calc(-100% - 8px));
  white-space: nowrap;
  overflow-x: auto;
  scrollbar-width: thin;
}
.ele-toolbar .et-tool {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 26px;
  height: 26px;
  padding: 0 3px;
  border: 0;
  border-radius: 4px;
  color: #334155;
  background: transparent;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}
.ele-toolbar .et-tool:hover {
  background: #f1f5f9;
  color: #356cde;
}
.ele-toolbar .et-tool.danger:hover {
  color: #d03050;
}
.ele-toolbar .et-tool.et-font {
  max-width: 96px;
  padding: 0 6px;
  font-size: 11px;
}
.ele-toolbar .et-tool.et-font span {
  overflow: hidden;
  text-overflow: ellipsis;
}
.ele-toolbar .et-display {
  min-width: 20px;
  color: #0f172a;
  font-size: 12px;
  font-weight: 700;
  text-align: center;
}
.ele-toolbar .et-gap {
  width: 1px;
  height: 16px;
  margin: 0 3px;
  background: #e2e8f0;
}
.ele-toolbar .et-color .n-color-picker {
  width: 14px;
  height: 14px;
}
.ele-toolbar .et-color .n-color-picker-trigger {
  width: 14px;
  height: 14px;
  padding: 0;
  border: 1px solid #cbd5e1;
}
.ele-toolbar .et-color .n-color-picker__value {
  display: none !important;
}
.ele-size-box {
  position: fixed;
  z-index: 40;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 3px 6px;
  border: 1px solid #d0d7e2;
  border-radius: 4px;
  background: #fff;
  box-shadow: 0 3px 10px rgb(15 23 42 / 12%);
  transform: translate(-50%, 6px);
  color: #475569;
  font-size: 11px;
  white-space: nowrap;
}
.ele-size-box .et-sb-text {
  font-variant-numeric: tabular-nums;
  font-weight: 700;
  color: #334155;
}
.ele-size-box .et-sb-divider {
  width: 1px;
  height: 14px;
  background: #e2e8f0;
}
.ele-size-box .et-sb-group {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}
.ele-size-box .et-sb-btn {
  width: 22px;
  height: 22px;
  padding: 0;
  border: 1px solid #d0d7e2;
  border-radius: 4px;
  background: #fff;
  font-size: 14px;
  line-height: 20px;
  cursor: pointer;
}
.ele-size-box .et-sb-btn:hover {
  border-color: #356cde;
  color: #356cde;
}
</style>
