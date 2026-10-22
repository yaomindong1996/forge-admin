<script setup>
/**
 * DesignerGridRenderer — 统一栅格渲染组件（P4 核心）
 * @description 表单设计器和列表设计器共用的 CSS Grid 栅格渲染器。
 *   统一布局属性消费（columns / gutter / rowGap / alignItems / justifyItems / cellMinHeight / showCellBorder / cellBackground）。
 *   各设计器通过 #cell 插槽注入格子内容（表单侧递归渲染 col 子节点，列表侧渲染 cells 数组）。
 */
import { computed } from 'vue'
import './grid-renderer.css'

const props = defineProps({
  /** 总列数 */
  columns: { type: Number, default: 24 },
  /** 列间距（px） */
  gutter: { type: Number, default: 16 },
  /** 行间距（px） */
  rowGap: { type: Number, default: 0 },
  /** 格子最小高度（px） */
  cellMinHeight: { type: Number, default: 160 },
  /** 格子内边距（px），覆盖 CSS 默认 8 */
  cellPadding: { type: Number, default: 8 },
  /** 垂直对齐 */
  alignItems: { type: String, default: 'stretch' },
  /** 水平对齐 */
  justifyItems: { type: String, default: 'stretch' },
  /** 是否显示格子虚线边框 */
  showCellBorder: { type: Boolean, default: true },
  /** 格子背景色 */
  cellBackground: { type: String, default: '' },
  /** 格子数据数组（列表侧 cells / 表单侧 col 子节点归一化后的数组） */
  cells: { type: Array, default: () => [] },
  /** 渲染模式 */
  mode: { type: String, default: 'designer', validator: v => ['designer', 'preview'].includes(v) },
  /** 列表画布容器 id：写到外层格子，保证拖放命中 padding 也能解析到 cell */
  containerId: { type: String, default: '' },
  /** 当前高亮投放格子 key */
  activeCellKey: { type: String, default: '' },
})

const emit = defineEmits(['cellDragEnter', 'cellDragOver', 'cellDrop', 'cellContextMenu'])

const safeColumns = computed(() => Math.max(1, Number(props.columns) || 24))
const safeGutter = computed(() => Math.max(0, Number(props.gutter) || 0))
const safeRowGap = computed(() => Math.max(0, Number(props.rowGap) || 0))
const isPreview = computed(() => props.mode === 'preview')

const gridStyle = computed(() => ({
  gridTemplateColumns: `repeat(${safeColumns.value}, minmax(0, 1fr))`,
  columnGap: `${safeGutter.value}px`,
  rowGap: `${safeRowGap.value}px`,
  alignItems: props.alignItems || 'stretch',
  justifyItems: props.justifyItems || 'stretch',
}))

/** 计算单个格子的 CSS 样式 */
function cellStyle(cell = {}) {
  const span = Math.max(1, Math.min(safeColumns.value, Number(cell.span) || 1))
  const padding = Number.isFinite(Number(cell.padding))
    ? Math.max(0, Number(cell.padding))
    : Math.max(0, Number(props.cellPadding ?? 8))
  return {
    gridColumn: `span ${span}`,
    minHeight: `${Math.max(24, Number(cell.minHeight) || props.cellMinHeight || 160)}px`,
    padding: `${padding}px`,
    backgroundColor: props.cellBackground || cell.backgroundColor || 'transparent',
  }
}

function cellKey(cell, index) {
  return cell.key || cell.id || `cell-${index}`
}

function resolveCellKey(cell, index) {
  return String(cellKey(cell, index))
}
</script>

<template>
  <div
    class="designer-grid-renderer"
    :class="{ 'is-preview': isPreview }"
    :style="gridStyle"
  >
    <div
      v-for="(cell, index) in cells"
      :key="cellKey(cell, index)"
      class="designer-grid-cell"
      :class="{
        'has-border': showCellBorder && !isPreview,
        'is-drop-active': !isPreview && activeCellKey && activeCellKey === resolveCellKey(cell, index),
      }"
      :style="cellStyle(cell)"
      :data-cell-key="resolveCellKey(cell, index)"
      :data-cell-index="index"
      :data-grid-cell-key="resolveCellKey(cell, index)"
      :data-grid-container-id="containerId || undefined"
      data-forge-grid-cell="1"
      @dragenter.prevent="!isPreview && emit('cellDragEnter', { cell, cellKey: resolveCellKey(cell, index), event: $event })"
      @dragover.prevent="!isPreview && emit('cellDragOver', { cell, cellKey: resolveCellKey(cell, index), event: $event })"
      @drop.prevent="!isPreview && emit('cellDrop', { cell, cellKey: resolveCellKey(cell, index), event: $event })"
      @contextmenu.prevent="!isPreview && emit('cellContextMenu', { cell, cellKey: resolveCellKey(cell, index), event: $event })"
    >
      <slot name="cell" :cell="cell" :index="index" :style="cellStyle(cell)">
        <div v-if="!isPreview && !cell.children?.length" class="designer-grid-cell-empty">
          拖入组件
        </div>
      </slot>
    </div>

    <div v-if="!cells.length" class="designer-grid-empty">
      无栅格数据
    </div>
  </div>
</template>

<style scoped>
</style>
