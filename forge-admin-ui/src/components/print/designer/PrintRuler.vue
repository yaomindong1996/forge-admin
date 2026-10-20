<script>
/** Outer ruler track size in millimetres — keep in sync with PrintCanvas ruler-frame. */
export const PRINT_RULER_SIZE_MM = 7
</script>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  lengthMm: { type: Number, required: true },
  orientation: { type: String, default: 'horizontal' },
  /** Kept for callers; position uses element ratio so CSS zoom cannot drift. */
  zoom: { type: Number, default: 1 },
  previewMm: { type: Number, default: null },
  /** 选中区间高亮（纸张坐标系 mm）。 */
  highlightStartMm: { type: Number, default: null },
  highlightEndMm: { type: Number, default: null },
})

const emit = defineEmits(['preview', 'place', 'leave'])

const marks = computed(() => {
  const count = Math.floor(props.lengthMm / 5)
  return Array.from({ length: count + 1 }, (_, index) => {
    const value = index * 5
    return { value, major: value % 10 === 0 }
  })
})

const axis = computed(() => (props.orientation === 'horizontal' ? 'x' : 'y'))

const highlightStyle = computed(() => {
  const start = Number(props.highlightStartMm)
  const end = Number(props.highlightEndMm)
  if (!Number.isFinite(start) || !Number.isFinite(end))
    return null
  const from = Math.max(0, Math.min(start, end))
  const to = Math.min(props.lengthMm, Math.max(start, end))
  if (to - from < 0.05)
    return null
  if (props.orientation === 'horizontal')
    return { left: `${from}mm`, width: `${to - from}mm` }
  return { top: `${from}mm`, height: `${to - from}mm` }
})

/** Map pointer to mm via rect ratio — stays aligned with CSS `left/top: Nmm` under zoom. */
function positionFromEvent(event) {
  const rect = event.currentTarget.getBoundingClientRect()
  const span = props.orientation === 'horizontal' ? rect.width : rect.height
  if (!span)
    return 0
  const offset = props.orientation === 'horizontal'
    ? event.clientX - rect.left
    : event.clientY - rect.top
  const ratio = Math.min(1, Math.max(0, offset / span))
  return Number((ratio * props.lengthMm).toFixed(2))
}

function onMove(event) {
  emit('preview', { axis: axis.value, positionMm: positionFromEvent(event) })
}

function onClick(event) {
  if (event.button !== 0)
    return
  emit('place', { axis: axis.value, positionMm: positionFromEvent(event) })
}

function onLeave() {
  emit('leave')
}
</script>

<template>
  <div
    class="print-ruler"
    :class="orientation"
    role="presentation"
    :title="orientation === 'horizontal' ? '点击添加竖向辅助线' : '点击添加横向辅助线'"
    @pointermove="onMove"
    @pointerleave="onLeave"
    @pointerdown.stop="onClick"
  >
    <span
      v-if="highlightStyle"
      class="ruler-selection-range"
      :style="highlightStyle"
    />
    <span
      v-for="mark in marks"
      :key="mark.value"
      class="ruler-mark"
      :class="{ major: mark.major }"
      :style="orientation === 'horizontal' ? { left: `${mark.value}mm` } : { top: `${mark.value}mm` }"
    >
      <i />
      <small v-if="mark.major">{{ mark.value }}</small>
    </span>
    <span
      v-if="previewMm != null"
      class="ruler-preview-tick"
      :style="orientation === 'horizontal' ? { left: `${previewMm}mm` } : { top: `${previewMm}mm` }"
    />
  </div>
</template>

<style scoped>
.print-ruler {
  position: relative;
  box-sizing: border-box;
  overflow: hidden;
  color: #0f172a;
  background: #eef2f7;
  user-select: none;
  cursor: crosshair;
  touch-action: none;
}
.print-ruler.horizontal {
  width: 100%;
  height: 100%;
  border-bottom: 1px solid #94a3b8;
}
.print-ruler.vertical {
  width: 100%;
  height: 100%;
  border-right: 1px solid #94a3b8;
}
.ruler-mark {
  position: absolute;
  font-family: Arial, sans-serif;
  font-size: 10px;
  font-weight: 700;
  pointer-events: none;
}
.horizontal .ruler-mark {
  bottom: 0;
  height: 100%;
}
.vertical .ruler-mark {
  right: 0;
  width: 100%;
}
.ruler-mark i {
  position: absolute;
  display: block;
  background: #64748b;
}
.horizontal .ruler-mark i {
  bottom: 0;
  width: 1px;
  height: 2.4mm;
}
.vertical .ruler-mark i {
  right: 0;
  width: 2.4mm;
  height: 1px;
}
.horizontal .ruler-mark.major i {
  height: 4mm;
  width: 1.5px;
  background: #0f172a;
}
.vertical .ruler-mark.major i {
  width: 4mm;
  height: 1.5px;
  background: #0f172a;
}
.horizontal .ruler-mark small {
  position: absolute;
  bottom: 4.2mm;
  left: 1px;
  line-height: 1;
  color: #0f172a;
}
.vertical .ruler-mark small {
  position: absolute;
  top: 1px;
  right: 4.2mm;
  line-height: 1;
  color: #0f172a;
  transform: rotate(-90deg);
  transform-origin: right top;
}
.ruler-selection-range {
  position: absolute;
  z-index: 1;
  pointer-events: none;
  background: color-mix(in srgb, var(--primary-color, #356cde) 38%, transparent);
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--primary-color, #356cde) 65%, transparent);
}
.horizontal .ruler-selection-range {
  top: 0;
  bottom: 0;
}
.vertical .ruler-selection-range {
  right: 0;
  left: 0;
}
.ruler-preview-tick {
  position: absolute;
  z-index: 2;
  pointer-events: none;
  background: var(--primary-color, #356cde);
}
.horizontal .ruler-preview-tick {
  top: 0;
  bottom: 0;
  width: 2px;
  transform: translateX(-50%);
}
.vertical .ruler-preview-tick {
  right: 0;
  left: 0;
  height: 2px;
  transform: translateY(-50%);
}
</style>
