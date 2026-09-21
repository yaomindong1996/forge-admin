<script setup>
import { computed } from 'vue'
import { tableCellStyle, tableFrameStyle } from './style'

const props = defineProps({ node: { type: Object, required: true } })
const frame = computed(() => {
  const sample = props.node.rows?.[0]?.cells?.[0]?.style || {}
  return tableFrameStyle({
    borderWidthMm: sample.borderWidthMm ?? 0.15,
    borderColor: sample.borderColor,
    borderStyle: sample.borderStyle,
  })
})
</script>

<template>
  <div
    role="table"
    :aria-label="node.id"
    :style="{
      display: 'block',
      width: `${node.widthMm}mm`,
      boxSizing: 'border-box',
      ...frame,
    }"
  >
    <div
      v-for="(row, index) in node.rows"
      :key="row.key ?? index"
      role="row"
      :data-print-row="row.key"
      :data-row-kind="row.kind"
      :style="{
        display: 'flex',
        flexDirection: 'row',
        flexWrap: 'nowrap',
        alignItems: 'stretch',
        width: `${node.widthMm}mm`,
        height: `${row.heightMm}mm`,
        boxSizing: 'border-box',
      }"
    >
      <div
        v-for="(cell, column) in row.cells"
        :key="column"
        :role="row.kind === 'header' ? 'columnheader' : 'cell'"
        :style="{
          ...tableCellStyle(cell.style, { top: index === 0, left: column === 0 }),
          display: 'flex',
          flexDirection: 'row',
          flex: 'none',
          width: `${cell.widthMm}mm`,
          minWidth: `${cell.widthMm}mm`,
          maxWidth: `${cell.widthMm}mm`,
          height: '100%',
          boxSizing: 'border-box',
          overflow: 'hidden',
        }"
      >
        <img
          v-if="cell.type === 'IMAGE' && cell.src"
          :src="cell.src"
          alt=""
          :style="{ display: 'block', width: '100%', height: `${cell.imageHeightMm}mm`, objectFit: 'contain' }"
        >
        <span v-else>{{ cell.text }}</span>
      </div>
    </div>
  </div>
</template>
