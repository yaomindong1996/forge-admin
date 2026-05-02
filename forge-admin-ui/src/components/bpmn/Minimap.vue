<template>
  <div class="minimap-wrapper" :class="{ collapsed: !expanded }">
    <div v-if="expanded" class="minimap-container">
      <div class="minimap-header">
        <span class="minimap-title">小地图</span>
        <n-button text size="tiny" @click="expanded = false">
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
      @click="expanded = true"
    >
      <template #icon>
        <i class="i-material-symbols:map-outline" />
      </template>
    </n-button>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'

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

const SCALE = 0.12

function updateMinimap() {
  if (!props.modeler) return
  try {
    const container = canvas.getContainer()
    const svg = container?.querySelector('svg')
    if (svg) {
      const clone = svg.cloneNode(true)
      clone.removeAttribute('style')
      clone.setAttribute('width', '100%')
      clone.setAttribute('height', '100%')
      svgContent.value = clone.outerHTML
    }
  }
  catch {
    // SVG not ready
  }
}

function updateViewport() {
  if (!canvas) return
  const vb = canvas.viewbox()
  const bounds = getTotalBounds()

  if (!bounds) return

  const scaleX = bounds.width * SCALE
  const scaleY = bounds.height * SCALE

  const left = ((vb.x - bounds.x) / bounds.width) * 100
  const top = ((vb.y - bounds.y) / bounds.height) * 100
  const width = (vb.width / bounds.width) * 100
  const height = (vb.height / bounds.height) * 100

  viewportStyle.value = {
    left: `${Math.max(0, left)}%`,
    top: `${Math.max(0, top)}%`,
    width: `${Math.min(100, width)}%`,
    height: `${Math.min(100, height)}%`,
  }
}

function getTotalBounds() {
  if (!props.modeler) return null
  const registry = props.modeler.get('elementRegistry')
  const elements = registry.getAll()
  if (elements.length === 0) return null

  let minX = Infinity
  let minY = Infinity
  let maxX = -Infinity
  let maxY = -Infinity

  for (const el of elements) {
    if (el.waypoints) continue
    minX = Math.min(minX, el.x)
    minY = Math.min(minY, el.y)
    maxX = Math.max(maxX, el.x + (el.width || 100))
    maxY = Math.max(maxY, el.y + (el.height || 80))
  }

  return { x: minX - 50, y: minY - 50, width: maxX - minX + 100, height: maxY - minY + 100 }
}

function startPan(e) {
  if (!canvas || !minimapCanvas.value) return
  e.preventDefault()
  const rect = minimapCanvas.value.getBoundingClientRect()
  const bounds = getTotalBounds()
  if (!bounds) return

  function onMove(ev) {
    const x = ((ev.clientX - rect.left) / rect.width) * bounds.width
    const y = ((ev.clientY - rect.top) / rect.height) * bounds.height
    canvas.scroll({ dx: x - canvas.viewbox().width / 2, dy: y - canvas.viewbox().height / 2 })
  }

  function onUp() {
    document.removeEventListener('mousemove', onMove)
    document.removeEventListener('mouseup', onUp)
  }

  onMove(e)
  document.addEventListener('mousemove', onMove)
  document.addEventListener('mouseup', onUp)
}

function setupEvents() {
  if (!props.modeler) return
  canvas = props.modeler.get('canvas')
  eventBus = props.modeler.get('eventBus')

  eventBus.on('canvas.viewbox.changed', updateViewport)
  eventBus.on('commandStack.changed', () => {
    setTimeout(updateMinimap, 100)
    setTimeout(updateViewport, 150)
  })

  setTimeout(updateMinimap, 500)
  setTimeout(updateViewport, 600)
}

watch(() => props.modeler, (m) => {
  if (m) {
    canvas = m.get('canvas')
    eventBus = m.get('eventBus')
    setupEvents()
  }
}, { immediate: true })

onMounted(() => {
  if (props.modeler) {
    setupEvents()
  }
})

onUnmounted(() => {
  if (eventBus) {
    eventBus.off('canvas.viewbox.changed', updateViewport)
    eventBus.off('commandStack.changed', updateMinimap)
  }
})
</script>

<style scoped>
.minimap-wrapper {
  position: absolute;
  bottom: 44px;
  right: 12px;
  z-index: 20;
}

.minimap-wrapper.collapsed {
  bottom: 12px;
}

.minimap-container {
  width: 180px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.1);
  overflow: hidden;
}

.minimap-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 8px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.minimap-title {
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
}

.minimap-canvas {
  height: 110px;
  position: relative;
  overflow: hidden;
  cursor: crosshair;
  background: #f5f7fa;
}

.minimap-svg-wrapper {
  transform-origin: top left;
  transform: scale(0.12);
  pointer-events: none;
  opacity: 0.7;
}

.minimap-viewport {
  position: absolute;
  border: 2px solid #3b82f6;
  background: rgba(59, 130, 246, 0.08);
  pointer-events: none;
  transition: all 0.1s ease;
}

.minimap-toggle {
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.12);
}
</style>
