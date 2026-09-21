<script setup>
import { computed } from 'vue'
import { tableCellStyle, tableFrameStyle } from './style'

const props = defineProps({ node: { type: Object, required: true } })
const frame = computed(() => tableFrameStyle({
  borderWidthMm: props.node.table.cells?.[0]?.style?.borderWidthMm ?? 0.15,
  borderColor: props.node.table.cells?.[0]?.style?.borderColor,
  borderStyle: props.node.table.cells?.[0]?.style?.borderStyle,
}))
const tableStyle = computed(() => ({
  // 打印 iframe 不会带上本组件 scoped CSS，布局必须全部走内联。
  display: 'grid',
  width: '100%',
  height: '100%',
  boxSizing: 'border-box',
  gridTemplateColumns: props.node.table.columns.map(column => `${column.widthMm}mm`).join(' '),
  gridTemplateRows: props.node.table.rows.map(row => `${row.heightMm}mm`).join(' '),
  ...frame.value,
}))
function style(cell) {
  return {
    ...tableCellStyle({ borderWidthMm: 0.15, ...cell.style }, { top: cell.row === 0, left: cell.column === 0 }),
    gridColumn: `${cell.column + 1} / span ${cell.colSpan}`,
    gridRow: `${cell.row + 1} / span ${cell.rowSpan}`,
    minWidth: 0,
    minHeight: 0,
    overflow: 'hidden',
  }
}
function imageStyle(cell) {
  const width = cell.imageWidthMm
  const height = cell.imageHeightMm
  return {
    display: 'block',
    width: width ? `${width}mm` : '100%',
    height: height ? `${height}mm` : '100%',
    maxWidth: '100%',
    maxHeight: '100%',
    objectFit: 'contain',
  }
}
</script>

<template>
  <div class="static-table" role="table" :style="tableStyle">
    <div v-for="cell in node.table.cells" :key="cell.id" role="cell" :style="style(cell)">
      <img
        v-if="(cell.type === 'IMAGE' || cell.contentType === 'IMAGE') && cell.src"
        class="cell-image"
        :src="cell.src"
        alt=""
        :style="imageStyle(cell)"
      >
      <span v-else>{{ cell.text }}</span>
    </div>
  </div>
</template>
