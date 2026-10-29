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
import { useFullscreen } from '@vueuse/core'
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useCanvasViewport } from '../composables/useCanvasViewport.js'

const props = defineProps({
  minScale: { type: Number, default: 0.3 },
  maxScale: { type: Number, default: 2.0 },
  initialScale: { type: Number, default: 1 },
  readonly: { type: Boolean, default: false },
  allowNavigation: { type: Boolean, default: false },
  contentBounds: { type: Object, default: null },
  /** 工具模式：pan 时左键拖空白/节点区域都平移画布 */
  tool: { type: String, default: 'select' },
})

const emit = defineEmits(['canvasClick', 'canvasDblclick', 'canvasDrop', 'canvasDragOver'])

const containerRef = ref(null)
const fullscreenTarget = computed(() => containerRef.value?.closest('.model-design-page')
  || containerRef.value?.closest('.ding-flow-designer') || containerRef.value)
const { isFullscreen, isSupported: fullscreenSupported, toggle: toggleFullscreen } = useFullscreen(fullscreenTarget)
const isPanning = ref(false)
const isSpaceDown = ref(false)
let resizeObserver
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
  // 中键 → 平移；空格 / 平移工具 / 点击空白 → 平移
  const isBackground = event.target === containerRef.value
    || event.target?.classList?.contains('canvas-transform')
  const isPanTool = props.tool === 'pan'
  const isPanTrigger = event.button === 1
    || (event.button === 0 && (isSpaceDown.value || isPanTool || isBackground))
  if (!isPanTrigger)
    return
  // 平移工具下即使点在节点上也优先拖动画布
  if (isPanTool && event.button === 0)
    event.preventDefault()
  else if (!isPanTool && !isBackground && event.button !== 1 && !isSpaceDown.value)
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
  // 属性面板和节点名称中输入空格不能触发画布拖动模式。
  if (event.target?.closest?.('input, textarea, select, [contenteditable="true"]'))
    return
  if (event.code === 'Space' && !isSpaceDown.value) {
    isSpaceDown.value = true
  }
}

function handleKeyUp(event) {
  if (event.code === 'Space')
    isSpaceDown.value = false
}

function clearPanState() {
  isSpaceDown.value = false
  handleMouseUp()
}

function fitContents() {
  if (!navigationEnabled.value)
    return
  const bounds = props.contentBounds
  const container = containerRef.value
  if (!bounds || !container)
    return resetView()
  fitToScreen(
    bounds.maxX - bounds.minX,
    bounds.maxY - bounds.minY,
    container.clientWidth,
    container.clientHeight,
    76,
    bounds.minX,
    bounds.minY,
  )
}

function handleFullscreen() {
  toggleFullscreen().catch(() => window.$message?.warning?.('当前浏览器无法进入全屏'))
}

onMounted(() => {
  window.addEventListener('keydown', handleKeyDown)
  window.addEventListener('keyup', handleKeyUp)
  window.addEventListener('blur', clearPanState)
  if (typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => {
      if (props.contentBounds)
        fitContents()
    })
    resizeObserver.observe(containerRef.value)
  }
})

onUnmounted(() => {
  resizeObserver?.disconnect()
  window.removeEventListener('keydown', handleKeyDown)
  window.removeEventListener('keyup', handleKeyUp)
  window.removeEventListener('blur', clearPanState)
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

function handleDrop(event) {
  emit('canvasDrop', event)
}

function handleDragOver(event) {
  emit('canvasDragOver', event)
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
    :class="{
      'cursor-grab': navigationEnabled && (isSpaceDown || tool === 'pan') && !isPanning,
      'cursor-grabbing': isPanning,
      'cursor-crosshair': tool === 'connect',
    }"
    @wheel="handleWheel"
    @mousedown="handleMouseDown"
    @click="handleClick"
    @dblclick="handleDblClick"
    @dragover="handleDragOver"
    @drop="handleDrop"
    @contextmenu.prevent
  >
    <div class="canvas-transform absolute left-0 top-0" :style="transformStyle">
      <!-- 连线在下、节点在上：线绝不能盖住卡片；选中边的端点在节点边外仍可拖 -->
      <slot name="edges" />
      <slot name="nodes" />
    </div>
    <!-- 画布元信息与编辑操作不随图形缩放 -->
    <slot name="overlay" />
    <div class="canvas-pan-hint">
      <i class="i-lucide:hand" /><span>空格 + 拖拽移动画布</span>
    </div>
    <div class="canvas-toolbar" role="toolbar" aria-label="画布视图">
      <button
        type="button" class="canvas-tool-btn" aria-label="缩小" :disabled="!navigationEnabled" @click="zoomOut()"
      >
        <i class="i-lucide:minus" />
      </button>
      <span class="canvas-zoom-text text-sm min-w-12 text-center">{{ scalePercent }}%</span>
      <button
        type="button" class="canvas-tool-btn" aria-label="放大" :disabled="!navigationEnabled" @click="zoomIn()"
      >
        <i class="i-lucide:plus" />
      </button>
      <span class="canvas-toolbar-divider" />
      <button
        type="button" class="canvas-tool-btn canvas-fit-btn" aria-label="适应画布"
        :disabled="!navigationEnabled" @click="fitContents"
      >
        适应画布
      </button>
      <button
        v-if="fullscreenSupported" type="button" class="canvas-tool-btn"
        :aria-label="isFullscreen ? '退出全屏' : '全屏画布'"
        :disabled="!navigationEnabled" @click="handleFullscreen"
      >
        <i :class="isFullscreen ? 'i-lucide:minimize' : 'i-lucide:maximize'" />
      </button>
      <slot name="toolbar" />
    </div>
  </div>
</template>

<style scoped>
.flow-canvas {
  outline: none;
  /* 点阵底略淡，避免和连线抢对比度 */
  background-color: #f4f6f9;
  background-image: radial-gradient(circle, #d5dae2 1px, transparent 1.15px);
  background-size: 20px 20px;
  background-position: 0 0;
}

.canvas-toolbar {
  position: absolute;
  right: 24px;
  bottom: 24px;
  z-index: 10;
  display: flex;
  align-items: center;
  padding: 2px 6px;
  overflow: hidden;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 6px;
  background: var(--bg-primary, #fff);
  box-shadow: 0 2px 8px rgb(0 0 0 / 3%);
}

.canvas-tool-btn {
  display: inline-flex;
  width: 30px;
  height: 32px;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: var(--text-secondary, #4e5969);
  cursor: pointer;
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
  color: var(--text-secondary);
  font-weight: 500;
  line-height: 32px;
}

.canvas-toolbar-divider {
  height: 16px;
  margin: 0 8px;
  border-left: 1px solid var(--border-light, #e5e7eb);
}

.canvas-fit-btn {
  width: auto;
  padding: 0 8px;
  font-size: 12px;
}
.canvas-tool-btn:focus-visible {
  outline: 2px solid var(--primary-color, #356de0);
  outline-offset: -2px;
}
.canvas-pan-hint {
  position: absolute;
  bottom: 26px;
  left: 24px;
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-tertiary, #86909c);
  font-size: 11px;
  pointer-events: none;
}
@media (max-width: 680px) {
  .canvas-toolbar {
    right: 12px;
    bottom: 16px;
  }
  .canvas-pan-hint {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .canvas-tool-btn {
    transition: none;
  }
}
</style>
