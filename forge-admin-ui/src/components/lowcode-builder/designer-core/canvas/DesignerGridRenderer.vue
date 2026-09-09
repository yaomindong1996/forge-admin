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
  cellMinHeight: { type: Number, default: 120 },
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
})

const safeColumns = computed(() => Math.max(1, Number(props.columns) || 24))
const safeGutter = computed(() => Math.max(0, Number(props.gutter) || 0))
const safeRowGap = computed(() => Math.max(0, Number(props.rowGap) || 0))

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
  return {
    gridColumn: `span ${span}`,
    minHeight: `${Math.max(24, Number(cell.minHeight) || props.cellMinHeight || 120)}px`,
    backgroundColor: props.cellBackground || cell.backgroundColor || 'transparent',
  }
}

function cellKey(cell, index) {
  return cell.key || cell.id || `cell-${index}`
}
</script>

<template>
  <div
    class="designer-grid-renderer"
    :class="{ 'is-preview': mode === 'preview' }"
    :style="gridStyle"
  >
    <div
      v-for="(cell, index) in cells"
      :key="cellKey(cell, index)"
      class="designer-grid-cell"
      :class="{ 'has-border': showCellBorder }"
      :style="cellStyle(cell)"
      :data-cell-key="cell.key || cell.id"
      :data-cell-index="index"
    >
      <slot name="cell" :cell="cell" :index="index" :style="cellStyle(cell)">
        <!-- 默认插槽内容：空格子占位 -->
        <div v-if="!cell.children?.length" class="designer-grid-cell-empty">
          拖入组件
        </div>
      </slot>
    </div>

    <!-- 无格子时的空态 -->
    <div v-if="!cells.length" class="designer-grid-empty">
      无栅格数据
    </div>
  </div>
</template>

<style scoped>
/* scoped 样式仅处理容器级状态，基础样式在 grid-renderer.css */
</style>
