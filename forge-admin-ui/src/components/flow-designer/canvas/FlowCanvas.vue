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
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useCanvasViewport } from '../composables/useCanvasViewport.js'

const props = defineProps({
  minScale: { type: Number, default: 0.3 },
  maxScale: { type: Number, default: 2.0 },
  initialScale: { type: Number, default: 1 },
  readonly: { type: Boolean, default: false },
  allowNavigation: { type: Boolean, default: false },
})

const emit = defineEmits(['canvasClick', 'canvasDblclick'])

const containerRef = ref(null)
const isPanning = ref(false)
const isSpaceDown = ref(false)
const lastPan = ref({ x: 0, y: 0 })
const navigationEnabled = computed(() => !props.readonly || props.allowNavigation)

const viewport = useCanvasViewport({
  minScale: props.minScale,
  maxScale: props.maxScale,
  initialScale: props.initialScale,
})

const {
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
  if (!navigationEnabled.value)
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
  if (!navigationEnabled.value)
    return
  // 中键 → 平移；空格 + 左键 → 平移；左键点击空白处 → 平移
  // 左键点击节点/按钮时不拦截（让子组件处理 click）
  const isBackground = event.target === containerRef.value
    || event.target?.classList?.contains('canvas-transform')
  const isPanTrigger = event.button === 1
    || (event.button === 0 && (isSpaceDown.value || isBackground))
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
  emit('canvasDblclick', event)
}

function handleClick(event) {
  emit('canvasClick', event)
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
    class="flow-canvas relative h-full w-full select-none overflow-hidden"
    :class="{ 'cursor-grab': navigationEnabled && isSpaceDown && !isPanning, 'cursor-grabbing': isPanning }"
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
    <div class="canvas-toolbar absolute left-4 top-4 z-10 flex items-center bg-white shadow-sm">
      <button class="canvas-tool-btn" aria-label="缩小" :disabled="!navigationEnabled" @click="zoomOut()">
        <i class="i-lucide:minus" />
      </button>
      <span class="canvas-zoom-text text-sm min-w-12 text-center">{{ scalePercent }}%</span>
      <button class="canvas-tool-btn" aria-label="放大" :disabled="!navigationEnabled" @click="zoomIn()">
        <i class="i-lucide:plus" />
      </button>
      <span class="canvas-toolbar-divider" />
      <button class="canvas-tool-btn" aria-label="适应画布" :disabled="!navigationEnabled" @click="resetView()">
        <i class="i-lucide:maximize" />
      </button>
      <slot name="toolbar" />
    </div>
  </div>
</template>

<style scoped>
.flow-canvas {
  outline: none;
  background-color: var(--bg-tertiary);
  background-image:
    radial-gradient(circle, rgba(203, 213, 225, 0.62) 1px, transparent 1.1px),
    linear-gradient(180deg, rgba(243, 244, 246, 0.96), rgba(243, 244, 246, 0.96));
  background-position:
    0 0,
    0 0;
  background-size:
    20px 20px,
    auto;
}

.canvas-toolbar {
  overflow: hidden;
  border: 1px solid var(--border-light);
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.08);
}

.canvas-tool-btn {
  display: inline-flex;
  width: 32px;
  height: 32px;
  align-items: center;
  justify-content: center;
  border-right: 1px solid var(--border-light);
  color: var(--text-secondary);
  transition:
    color 160ms ease,
    background-color 160ms ease;
}

.canvas-tool-btn:hover:not(:disabled) {
  background: var(--bg-secondary);
  color: var(--text-primary);
}

.canvas-tool-btn:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.canvas-zoom-text {
  height: 32px;
  padding: 0 12px;
  border-right: 1px solid var(--border-light);
  color: var(--text-secondary);
  font-weight: 500;
  line-height: 32px;
}

.canvas-toolbar-divider {
  display: none;
}

@media (prefers-reduced-motion: reduce) {
  .canvas-tool-btn {
    transition: none;
  }
}
</style>
