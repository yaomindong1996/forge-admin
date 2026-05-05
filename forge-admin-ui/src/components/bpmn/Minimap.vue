<template>
  <div v-if="visible" class="minimap-wrapper" :class="{ collapsed: !expanded }">
    <div v-if="expanded" class="minimap-container">
      <div class="minimap-header">
        <span class="minimap-title">小地图</span>
        <n-button text size="tiny" @click="toggleMinimap">
          <i class="i-material-symbols:close" />
        </n-button>
      </div>
      <div
        ref="minimapCanvas"
        class="minimap-canvas"
        @mousedown="startPan"
      >
        <div
          v-if="svgContent"
          class="minimap-svg-wrapper"
          v-html="svgContent"
        />
        <div
          v-if="viewportStyle"
          class="minimap-viewport"
          :style="viewportStyle"
        />
      </div>
    </div>
    <n-button
      v-else
      circle
      size="small"
      class="minimap-toggle"
      @click="toggleMinimap"
    >
      <template #icon>
        <i class="i-material-symbols:map" />
      </template>
    </n-button>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref, watch } from 'vue'

const props = defineProps({
  modeler: { type: Object, default: null },
  visible: { type: Boolean, default: true },
})

const expanded = ref(true)
const minimapCanvas = ref(null)
const svgContent = ref('')
const viewportStyle = ref(null)

let eventBus = null
let canvas = null
let updateTimer = null

const SCALE = 0.15

function toggleMinimap() {
  expanded.value = !expanded.value
  if (expanded.value) {
    setTimeout(() => {
      updateMinimap()
      updateViewport()
    }, 100)
  }
}

function updateMinimap() {
  if (!props.modeler || !canvas)
    return

  try {
    const container = canvas.getContainer()
    const svg = container?.querySelector('svg')
    if (svg) {
      const clone = svg.cloneNode(true)
      clone.removeAttribute('style')
      clone.setAttribute('width', '100%')
      clone.setAttribute('height', '100%')
      clone.setAttribute('preserveAspectRatio', 'xMidYMid meet')
      svgContent.value = clone.outerHTML
    }
  }
  catch (error) {
    console.warn('更新小地图失败:', error)
  }
}

function updateViewport() {
  if (!canvas)
    return

  try {
    const vb = canvas.viewbox()
    const bounds = getTotalBounds()

    if (!bounds)
      return

    const left = ((vb.x - bounds.x) / bounds.width) * 100
    const top = ((vb.y - bounds.y) / bounds.height) * 100
    const width = (vb.width / bounds.width) * 100
    const height = (vb.height / bounds.height) * 100

    viewportStyle.value = {
      left: `${Math.max(0, Math.min(100 - width, left))}%`,
      top: `${Math.max(0, Math.min(100 - height, top))}%`,
      width: `${Math.min(100, width)}%`,
      height: `${Math.min(100, height)}%`,
    }
  }
  catch (error) {
    console.warn('更新视口失败:', error)
  }
}

function getTotalBounds() {
  if (!props.modeler)
    return null

  try {
    const registry = props.modeler.get('elementRegistry')
    const elements = registry.getAll()

    if (elements.length === 0)
      return null

    let minX = Infinity
    let minY = Infinity
    let maxX = -Infinity
    let maxY = -Infinity

    for (const el of elements) {
      if (el.waypoints || el.type === 'label')
        continue
      if (el.x !== undefined && el.y !== undefined) {
        minX = Math.min(minX, el.x)
        minY = Math.min(minY, el.y)
        maxX = Math.max(maxX, el.x + (el.width || 100))
        maxY = Math.max(maxY, el.y + (el.height || 80))
      }
    }

    if (minX === Infinity)
      return null

    return {
      x: minX - 50,
      y: minY - 50,
      width: maxX - minX + 100,
      height: maxY - minY + 100,
    }
  }
  catch (error) {
    console.warn('获取边界失败:', error)
    return null
  }
}

function startPan(e) {
  if (!canvas || !minimapCanvas.value)
    return

  e.preventDefault()
  const rect = minimapCanvas.value.getBoundingClientRect()
  const bounds = getTotalBounds()
  if (!bounds)
    return

  function onMove(ev) {
    const x = ((ev.clientX - rect.left) / rect.width) * bounds.width + bounds.x
    const y = ((ev.clientY - rect.top) / rect.height) * bounds.height + bounds.y
    const vb = canvas.viewbox()
    canvas.viewbox({
      x: x - vb.width / 2,
      y: y - vb.height / 2,
      width: vb.width,
      height: vb.height,
    })
  }

  function onUp() {
    document.removeEventListener('mousemove', onMove)
    document.removeEventListener('mouseup', onUp)
  }

  onMove(e)
  document.addEventListener('mousemove', onMove)
  document.addEventListener('mouseup', onUp)
}

function scheduleUpdate() {
  if (updateTimer)
    clearTimeout(updateTimer)

  updateTimer = setTimeout(() => {
    updateMinimap()
    updateViewport()
  }, 200)
}

function setupEvents() {
  if (!props.modeler)
    return

  try {
    canvas = props.modeler.get('canvas')
    eventBus = props.modeler.get('eventBus')

    eventBus.on('canvas.viewbox.changed', updateViewport)
    eventBus.on('commandStack.changed', scheduleUpdate)
    eventBus.on('elements.changed', scheduleUpdate)

    setTimeout(() => {
      updateMinimap()
      updateViewport()
    }, 500)
  }
  catch (error) {
    console.warn('设置事件监听失败:', error)
  }
}

function cleanupEvents() {
  if (eventBus) {
    try {
      eventBus.off('canvas.viewbox.changed', updateViewport)
      eventBus.off('commandStack.changed', scheduleUpdate)
      eventBus.off('elements.changed', scheduleUpdate)
    }
    catch (error) {
      console.warn('清理事件监听失败:', error)
    }
  }
}

watch(() => props.modeler, (newModeler) => {
  cleanupEvents()
  if (newModeler) {
    setupEvents()
  }
}, { immediate: true })

onMounted(() => {
  if (props.modeler) {
    setupEvents()
  }
})

onUnmounted(() => {
  cleanupEvents()
  if (updateTimer) {
    clearTimeout(updateTimer)
  }
})
</script>

<style scoped>
/* 设计系统变量 */
:root {
  --primary: #6366f1;
  --surface: #ffffff;
  --border: #e2e8f0;
  --text-secondary: #64748b;
  --background: #f5f3ff;
}

.minimap-wrapper {
  position: absolute;
  bottom: 20px;
  right: 20px;
  z-index: 30;
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
}

.minimap-wrapper.collapsed {
  bottom: 20px;
}

.minimap-container {
  width: 220px;
  background: var(--surface);
  border: 2px solid var(--primary);
  border-radius: 14px;
  box-shadow:
    0 10px 15px -3px rgba(99, 102, 241, 0.1),
    0 4px 6px -2px rgba(99, 102, 241, 0.05);
  overflow: hidden;
  transition: box-shadow 200ms cubic-bezier(0.4, 0, 0.2, 1);
}

.minimap-container:hover {
  box-shadow:
    0 20px 25px -5px rgba(99, 102, 241, 0.15),
    0 10px 10px -5px rgba(99, 102, 241, 0.1);
}

.minimap-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: linear-gradient(135deg, var(--primary) 0%, #818cf8 100%);
  border-bottom: 2px solid var(--primary);
}

.minimap-title {
  font-size: 12px;
  font-weight: 600;
  color: white;
  letter-spacing: 0.3px;
}

.minimap-canvas {
  height: 140px;
  position: relative;
  overflow: hidden;
  cursor: crosshair;
  background: var(--background);
}

.minimap-svg-wrapper {
  transform-origin: top left;
  transform: scale(0.15);
  pointer-events: none;
  opacity: 0.8;
  width: 100%;
  height: 100%;
}

.minimap-svg-wrapper :deep(svg) {
  max-width: none;
  max-height: none;
}

.minimap-viewport {
  position: absolute;
  border: 2px solid var(--primary);
  background: rgba(99, 102, 241, 0.1);
  pointer-events: none;
  transition: all 100ms ease-out;
  border-radius: 4px;
}

.minimap-toggle {
  background: var(--surface);
  border: 2px solid var(--primary);
  box-shadow:
    0 10px 15px -3px rgba(99, 102, 241, 0.1),
    0 4px 6px -2px rgba(99, 102, 241, 0.05);
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
}

.minimap-toggle:hover {
  box-shadow:
    0 20px 25px -5px rgba(99, 102, 241, 0.15),
    0 10px 10px -5px rgba(99, 102, 241, 0.1);
  transform: translateY(-2px);
  cursor: pointer;
}

/* Focus 状态 */
.minimap-toggle:focus-visible {
  outline: 2px solid var(--primary);
  outline-offset: 2px;
}
</style>
