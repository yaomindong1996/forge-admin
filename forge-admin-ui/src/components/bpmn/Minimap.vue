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

const expanded = ref(false)
const minimapCanvas = ref(null)
const svgContent = ref('')
const viewportStyle = ref(null)

let eventBus = null
let canvas = null
let updateTimer = null

function toggleMinimap() {
  expanded.value = !expanded.value
  if (expanded.value) {
    setTimeout(() => {
      updateMinimap()
      updateViewport()
    }, 100)
  }
}

async function updateMinimap() {
  if (!expanded.value || !props.modeler || !canvas)
    return

  try {
    const { svg } = await props.modeler.saveSVG()
    if (svg) {
      svgContent.value = svg
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
  if (!expanded.value)
    return
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

    if (expanded.value) {
      setTimeout(() => {
        updateMinimap()
        updateViewport()
      }, 500)
    }
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
.minimap-wrapper {
  --primary: #2563eb;
  --surface: var(--bg-primary);
  --border: #d7dde7;
  --text-secondary: #667085;
  --background: var(--bg-secondary);

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
  border: 1px solid var(--border);
  border-radius: 8px;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.12);
  overflow: hidden;
  transition: box-shadow 200ms cubic-bezier(0.4, 0, 0.2, 1);
}

.minimap-container:hover {
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.14);
}

.minimap-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border);
}

.minimap-title {
  font-size: 12px;
  font-weight: 600;
  color: #344054;
}

.minimap-canvas {
  height: 140px;
  position: relative;
  overflow: hidden;
  cursor: crosshair;
  background: var(--background);
}

.minimap-svg-wrapper {
  pointer-events: none;
  opacity: 0.9;
  width: 100%;
  height: 100%;
}

.minimap-svg-wrapper :deep(svg) {
  width: 100%;
  height: 100%;
  display: block;
}

.minimap-viewport {
  position: absolute;
  border: 2px solid var(--primary);
  background: rgba(37, 99, 235, 0.1);
  pointer-events: none;
  transition: all 100ms ease-out;
  border-radius: 4px;
}

.minimap-toggle {
  background: var(--surface);
  border: 1px solid var(--border);
  box-shadow: 0 6px 14px rgba(15, 23, 42, 0.1);
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
}

.minimap-toggle:hover {
  border-color: #98a2b3;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.14);
  cursor: pointer;
}

/* Focus 状态 */
.minimap-toggle:focus-visible {
  outline: 2px solid var(--primary);
  outline-offset: 2px;
}
</style>
