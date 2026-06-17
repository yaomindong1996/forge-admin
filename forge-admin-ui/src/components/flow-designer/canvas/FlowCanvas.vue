<script setup>
/**
 * FlowCanvas — 钉钉样式画布容器
 *
 * 职责：
 * - 提供 transform 缩放 / 平移容器
 * - 鼠标滚轮 + Ctrl/Cmd → 以鼠标为锚点缩放
 * - 鼠标滚轮（无修饰键）→ 垂直滚动平移
 * - 按住空格 + 鼠标拖拽 → 平移画布（鼠标变 grab）
 * - 中键拖拽 → 平移
 * - 双击空白 → resetView
 *
 * 暴露给父组件的方法（defineExpose）：
 * - resetView / fitToScreen / zoomIn / zoomOut / setScale
 * - screenToCanvas / canvasToScreen
 * - viewport：完整 useCanvasViewport 返回值
 *
 * Slots：
 * - edges  → SVG 连线层（建议 absolute + pointer-events: none）
 * - nodes  → HTML 节点层（建议 absolute）
 * - toolbar → 不受 transform 影响的覆盖工具栏（自定义）
 */
import { onMounted, onUnmounted, ref } from 'vue'
import { useCanvasViewport } from '../composables/useCanvasViewport.js'

const props = defineProps({
  minScale: { type: Number, default: 0.3 },
  maxScale: { type: Number, default: 2.0 },
  initialScale: { type: Number, default: 1 },
  readonly: { type: Boolean, default: false },
})

const emit = defineEmits(['canvas-click', 'canvas-dblclick'])

const containerRef = ref(null)
const isPanning = ref(false)
const isSpaceDown = ref(false)
const lastPan = ref({ x: 0, y: 0 })

const viewport = useCanvasViewport({
  minScale: props.minScale,
  maxScale: props.maxScale,
  initialScale: props.initialScale,
})

const {
  scale,
  translateX,
  translateY,
  transformStyle,
  scalePercent,
  zoomIn,
  zoomOut,
  setScale,
  pan,
  resetView,
  fitToScreen,
  screenToCanvas,
  canvasToScreen,
} = viewport

/* ---- 滚轮缩放 / 滚动 ---- */

function handleWheel(event) {
  if (props.readonly)
    return
  // Ctrl/Cmd + 滚轮 → 以鼠标位置为锚点缩放
  if (event.ctrlKey || event.metaKey) {
    event.preventDefault()
    const rect = containerRef.value.getBoundingClientRect()
    const cx = event.clientX - rect.left
    const cy = event.clientY - rect.top
    if (event.deltaY < 0)
      zoomIn(cx, cy)
    else
      zoomOut(cx, cy)
    return
  }
  // 普通滚轮 → 垂直滚动（含横向 deltaX）
  event.preventDefault()
  pan(-event.deltaX, -event.deltaY)
}

/* ---- 拖拽平移 ---- */

function handleMouseDown(event) {
  if (props.readonly)
    return
  // 中键 或 空格 + 左键 → 平移
  const isPanTrigger = event.button === 1 || (event.button === 0 && isSpaceDown.value)
  if (!isPanTrigger)
    return
  event.preventDefault()
  isPanning.value = true
  lastPan.value = { x: event.clientX, y: event.clientY }
  window.addEventListener('mousemove', handleMouseMove)
  window.addEventListener('mouseup', handleMouseUp)
}

function handleMouseMove(event) {
  if (!isPanning.value)
    return
  const dx = event.clientX - lastPan.value.x
  const dy = event.clientY - lastPan.value.y
  lastPan.value = { x: event.clientX, y: event.clientY }
  pan(dx, dy)
}

function handleMouseUp() {
  isPanning.value = false
  window.removeEventListener('mousemove', handleMouseMove)
  window.removeEventListener('mouseup', handleMouseUp)
}

/* ---- 空格键检测 ---- */

function handleKeyDown(event) {
  if (event.code === 'Space' && !isSpaceDown.value) {
    isSpaceDown.value = true
  }
}

function handleKeyUp(event) {
  if (event.code === 'Space')
    isSpaceDown.value = false
}

onMounted(() => {
  window.addEventListener('keydown', handleKeyDown)
  window.addEventListener('keyup', handleKeyUp)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeyDown)
  window.removeEventListener('keyup', handleKeyUp)
  window.removeEventListener('mousemove', handleMouseMove)
  window.removeEventListener('mouseup', handleMouseUp)
})

/* ---- 双击空白重置 ---- */

function handleDblClick(event) {
  emit('canvas-dblclick', event)
}

function handleClick(event) {
  emit('canvas-click', event)
}

defineExpose({
  resetView,
  fitToScreen,
  zoomIn,
  zoomOut,
  setScale,
  screenToCanvas,
  canvasToScreen,
  viewport,
  containerRef,
})
</script>

<template>
  <div
    ref="containerRef"
    class="flow-canvas relative h-full w-full select-none overflow-hidden bg-gray-50"
    :class="{ 'cursor-grab': isSpaceDown && !isPanning, 'cursor-grabbing': isPanning }"
    @wheel="handleWheel"
    @mousedown="handleMouseDown"
    @click="handleClick"
    @dblclick="handleDblClick"
    @contextmenu.prevent
  >
    <div class="canvas-transform absolute left-0 top-0" :style="transformStyle">
      <slot name="edges" />
      <slot name="nodes" />
    </div>
    <div class="canvas-toolbar absolute right-4 top-4 z-10 flex items-center gap-2 rounded-lg bg-white px-2 py-1 shadow-md">
      <button class="px-2 py-1 hover:text-primary" :disabled="readonly" @click="zoomOut()">
        <i class="i-mdi-minus" />
      </button>
      <span class="text-sm min-w-12 text-center">{{ scalePercent }}%</span>
      <button class="px-2 py-1 hover:text-primary" :disabled="readonly" @click="zoomIn()">
        <i class="i-mdi-plus" />
      </button>
      <button class="px-2 py-1 hover:text-primary" :disabled="readonly" @click="resetView()">
        <i class="i-mdi-fit-to-page-outline" />
      </button>
      <slot name="toolbar" />
    </div>
  </div>
</template>

<style scoped>
.flow-canvas {
  outline: none;
}
</style>
