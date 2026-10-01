<script setup>
import { computed } from 'vue'

const props = defineProps({ node: { type: Object, required: true } })
const block = computed(() => props.node.descriptions || {})
const column = computed(() => Math.max(1, Math.min(4, Number(block.value.column) || 3)))
const placement = computed(() => block.value.labelPlacement === 'left' ? 'left' : 'top')
const bordered = computed(() => block.value.bordered === true)
const stacked = computed(() => placement.value === 'top')
const separator = computed(() => placement.value === 'left' && !bordered.value ? (block.value.separator ?? '：') : '')
const size = computed(() => ['small', 'medium', 'large'].includes(block.value.size) ? block.value.size : 'medium')
const align = computed(() => ['left', 'center', 'right'].includes(block.value.labelAlign) ? block.value.labelAlign : 'left')
const labelJustify = computed(() => align.value === 'center' ? 'center' : align.value === 'right' ? 'flex-end' : 'flex-start')
const fontSizePt = computed(() => {
  if (size.value === 'small')
    return 8
  if (size.value === 'large')
    return 11
  return 9.5
})

/** 打印 iframe 吃不到 scoped CSS，布局必须全部内联。 */
const rootStyle = computed(() => ({
  boxSizing: 'border-box',
  width: '100%',
  height: '100%',
  overflow: 'hidden',
  color: '#111827',
  fontFamily: 'Microsoft YaHei, PingFang SC, sans-serif',
  fontSize: `${fontSizePt.value}pt`,
}))
const titleStyle = computed(() => ({
  marginBottom: '1.5mm',
  lineHeight: 1.3,
  textAlign: ['left', 'center', 'right'].includes(block.value.titleAlign) ? block.value.titleAlign : 'left',
  color: block.value.titleColor || '#111827',
  fontSize: `${Number(block.value.titleFontSizePt) || 12}pt`,
  fontWeight: block.value.titleBold === false ? 400 : 700,
}))
const gridStyle = computed(() => ({
  display: 'grid',
  gridTemplateColumns: `repeat(${column.value}, minmax(0, 1fr))`,
  width: '100%',
}))
const labelStyle = computed(() => ({
  boxSizing: 'border-box',
  display: 'flex',
  alignItems: 'center',
  justifyContent: labelJustify.value,
  textAlign: align.value,
  color: '#1e3a5f',
  fontWeight: 600,
  background: bordered.value ? (block.value.labelBackground || '#d9e3f0') : 'transparent',
  ...(stacked.value
    ? { flex: 'none', width: '100%' }
    : { flex: '0 0 34%', width: '34%' }),
  ...(bordered.value ? { padding: '1.6mm 2mm' } : null),
}))
const contentStyle = computed(() => ({
  flex: '1 1 auto',
  minWidth: 0,
  overflowWrap: 'anywhere',
  ...(bordered.value
    ? { padding: '1.6mm 2mm', background: '#fff' }
    : null),
}))

function cellStyle(item) {
  const span = Math.max(1, Math.min(column.value, Number(item.span) || 1))
  return {
    display: 'flex',
    minWidth: 0,
    alignItems: 'stretch',
    flexDirection: stacked.value ? 'column' : 'row',
    gridColumn: `span ${span}`,
    ...(bordered.value
      ? {
          border: '0.25mm solid #c5d0de',
          margin: '0 -0.25mm -0.25mm 0',
          background: '#fff',
        }
      : null),
  }
}
</script>

<template>
  <div :style="rootStyle">
    <div v-if="block.title" :style="titleStyle">
      {{ block.title }}
    </div>
    <div :style="gridStyle">
      <div
        v-for="item in block.items || []"
        :key="item.id"
        :style="cellStyle(item)"
      >
        <div :style="labelStyle">
          {{ item.label }}{{ separator }}
        </div>
        <div :style="contentStyle">
          {{ item.text || '' }}
        </div>
      </div>
    </div>
  </div>
</template>
