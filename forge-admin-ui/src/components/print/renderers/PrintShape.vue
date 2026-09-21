<script setup>
import { computed } from 'vue'

const props = defineProps({ node: { type: Object, required: true } })
const vertical = computed(() => props.node.type === 'LINE' && props.node.heightMm > props.node.widthMm)
const line = computed(() => props.node.type === 'LINE')
const ellipse = computed(() => props.node.type === 'ELLIPSE')
const borderMm = computed(() => {
  if (line.value)
    return Math.max(props.node.style?.borderWidthMm ?? 0.5, 0.2)
  return Math.max(props.node.style?.borderWidthMm ?? 0.4, 0.35)
})
const borderColor = computed(() => props.node.style?.borderColor || props.node.style?.backgroundColor || '#000000')
const borderStyle = computed(() => props.node.style?.borderStyle || 'solid')
const fill = computed(() => props.node.style?.backgroundColor || 'transparent')
/** Approximate stroke width in SVG user units (viewBox 0..100). */
const strokeWidth = computed(() => Math.min(12, Math.max(0.8, borderMm.value * 2.2)))
const dash = computed(() => borderStyle.value === 'dashed' ? '8 4' : borderStyle.value === 'dotted' ? '2 3' : undefined)
const lineStroke = computed(() => {
  const color = borderColor.value
  const style = ['dashed', 'dotted'].includes(borderStyle.value) ? borderStyle.value : 'solid'
  const width = `${borderMm.value}mm`
  if (vertical.value) {
    return {
      boxSizing: 'content-box',
      width: '0px',
      height: '100%',
      border: 'none',
      borderLeft: `${width} ${style} ${color}`,
      background: 'none',
    }
  }
  return {
    boxSizing: 'content-box',
    width: '100%',
    height: '0px',
    border: 'none',
    borderTop: `${width} ${style} ${color}`,
    background: 'none',
  }
})
</script>

<template>
  <div
    v-if="line"
    class="print-shape line"
    :class="[vertical ? 'vertical' : 'horizontal', borderStyle]"
    :style="lineStroke"
    aria-hidden="true"
  />
  <svg
    v-else-if="ellipse"
    class="print-shape ellipse"
    viewBox="0 0 100 100"
    preserveAspectRatio="none"
    aria-hidden="true"
  >
    <ellipse
      cx="50"
      cy="50"
      :rx="50 - strokeWidth / 2"
      :ry="50 - strokeWidth / 2"
      :fill="fill === 'transparent' ? 'none' : fill"
      :stroke="borderColor"
      :stroke-width="strokeWidth"
      :stroke-dasharray="dash"
    />
  </svg>
  <div
    v-else
    class="print-shape rectangle"
    :style="{
      boxSizing: 'border-box',
      width: '100%',
      height: '100%',
      border: `${borderMm}mm ${borderStyle} ${borderColor}`,
      borderRadius: `${node.style?.borderRadiusMm || 0}mm`,
      backgroundColor: fill,
    }"
  />
</template>

<style scoped>
.print-shape.ellipse {
  display: block;
  width: 100%;
  height: 100%;
  overflow: visible;
}
.print-shape.line {
  display: block;
  overflow: visible;
}
.print-shape.rectangle {
  min-width: 100%;
  min-height: 100%;
}
</style>
