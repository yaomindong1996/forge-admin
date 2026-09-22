<script setup>
import { computed } from 'vue'

const props = defineProps({ node: { type: Object, required: true } })
const block = computed(() => props.node.descriptions || {})
const column = computed(() => Math.max(1, Math.min(4, Number(block.value.column) || 3)))
const placement = computed(() => block.value.labelPlacement === 'left' ? 'left' : 'top')
const bordered = computed(() => block.value.bordered === true)
const separator = computed(() => placement.value === 'left' && !bordered.value ? (block.value.separator ?? '：') : '')
const size = computed(() => ['small', 'medium', 'large'].includes(block.value.size) ? block.value.size : 'medium')
const align = computed(() => ['left', 'center', 'right'].includes(block.value.labelAlign) ? block.value.labelAlign : 'left')
const labelJustify = computed(() => align.value === 'center' ? 'center' : align.value === 'right' ? 'flex-end' : 'flex-start')
const titleStyle = computed(() => ({
  textAlign: ['left', 'center', 'right'].includes(block.value.titleAlign) ? block.value.titleAlign : 'left',
  color: block.value.titleColor || '#111827',
  fontSize: `${Number(block.value.titleFontSizePt) || 12}pt`,
  fontWeight: block.value.titleBold === false ? 400 : 700,
}))
const labelStyle = computed(() => ({
  textAlign: align.value,
  justifyContent: labelJustify.value,
  background: bordered.value ? (block.value.labelBackground || '#d9e3f0') : 'transparent',
}))
</script>

<template>
  <div class="detail-block" :class="[`size-${size}`, { bordered, stacked: placement === 'top' }]">
    <div v-if="block.title" class="detail-title" :style="titleStyle">
      {{ block.title }}
    </div>
    <div class="detail-grid" :style="{ gridTemplateColumns: `repeat(${column}, minmax(0, 1fr))` }">
      <div
        v-for="item in block.items || []"
        :key="item.id"
        class="detail-cell"
        :style="{ gridColumn: `span ${Math.max(1, Math.min(column, Number(item.span) || 1))}` }"
      >
        <div class="detail-label" :style="labelStyle">
          {{ item.label }}{{ separator }}
        </div>
        <div class="detail-content">
          {{ item.text || '' }}
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.detail-block {
  box-sizing: border-box;
  width: 100%;
  height: 100%;
  overflow: hidden;
  color: #111827;
  font-family: 'Microsoft YaHei', 'PingFang SC', sans-serif;
}
.detail-title {
  margin-bottom: 1.5mm;
  line-height: 1.3;
}
.detail-grid {
  display: grid;
}
.detail-cell {
  display: flex;
  min-width: 0;
  align-items: stretch;
}
.stacked .detail-cell {
  flex-direction: column;
}
.detail-label {
  box-sizing: border-box;
  display: flex;
  align-items: center;
  flex: 0 0 34%;
  width: 34%;
  color: #1e3a5f;
  font-weight: 600;
}
.stacked .detail-label {
  flex: none;
  width: 100%;
}
.detail-content {
  flex: 1 1 auto;
  min-width: 0;
  overflow-wrap: anywhere;
}
.bordered .detail-cell {
  border: 0.25mm solid #c5d0de;
  margin: 0 -0.25mm -0.25mm 0;
  background: #fff;
}
.bordered .detail-label,
.bordered .detail-content {
  padding: 1.6mm 2mm;
}
.bordered .detail-content {
  background: #fff;
}
.size-small {
  font-size: 8pt;
}
.size-medium {
  font-size: 9.5pt;
}
.size-large {
  font-size: 11pt;
}
</style>
