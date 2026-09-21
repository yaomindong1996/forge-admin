<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  maxRows: { type: Number, default: 8 },
  maxCols: { type: Number, default: 10 },
})
const emit = defineEmits(['pick', 'cancel'])

const hoverRows = ref(0)
const hoverCols = ref(0)
const label = computed(() => {
  if (!hoverRows.value || !hoverCols.value)
    return '移动鼠标选择行列'
  return `${hoverRows.value} × ${hoverCols.value}`
})

watch(() => props.maxRows, () => { hoverRows.value = 0; hoverCols.value = 0 })

function onEnter(row, col) {
  hoverRows.value = row
  hoverCols.value = col
}
function onPick() {
  if (!hoverRows.value || !hoverCols.value)
    return
  emit('pick', { rows: hoverRows.value, cols: hoverCols.value })
}
</script>

<template>
  <div class="table-size-picker" @pointerleave="hoverRows = 0; hoverCols = 0">
    <div class="picker-label">{{ label }}</div>
    <div
      class="picker-grid"
      role="grid"
      :aria-label="label"
      @click="onPick"
    >
      <button
        v-for="row in maxRows"
        :key="`r-${row}`"
        type="button"
        class="picker-row"
        tabindex="-1"
      >
        <i
          v-for="col in maxCols"
          :key="`${row}-${col}`"
          class="picker-cell"
          :class="{ hot: row <= hoverRows && col <= hoverCols }"
          @pointerenter="onEnter(row, col)"
        />
      </button>
    </div>
    <p class="picker-tip">
      点击插入空白表格，可再框选单元格合并
    </p>
  </div>
</template>

<style scoped>
.table-size-picker {
  padding: 8px 10px 10px;
  min-width: 196px;
}
.picker-label {
  margin-bottom: 8px;
  color: var(--text-primary, #0f172a);
  font-size: 12px;
  font-weight: 700;
  text-align: center;
}
.picker-grid {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.picker-row {
  display: flex;
  gap: 3px;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
}
.picker-cell {
  display: block;
  width: 14px;
  height: 14px;
  border: 1px solid #cbd5e1;
  border-radius: 2px;
  background: #fff;
  box-sizing: border-box;
}
.picker-cell.hot {
  border-color: var(--primary-color, #356cde);
  background: color-mix(in srgb, var(--primary-color, #356cde) 22%, #fff);
}
.picker-tip {
  margin: 8px 0 0;
  color: var(--text-tertiary, #64748b);
  font-size: 10px;
  line-height: 1.4;
  text-align: center;
}
</style>
