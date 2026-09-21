<script setup>
import { CloseOutline, ContractOutline, LocateOutline, MapOutline } from '@vicons/ionicons5'
import { NIcon } from 'naive-ui'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { usePrintDesignerStore } from '@/stores/print/printDesignerStore'
import { computeMiniMapRegion, scrollFromMiniMapPoint } from './designerView'

const props = defineProps({
  viewportEl: { type: Object, default: null },
  pages: { type: Array, default: () => [] },
  paperWidthMm: { type: Number, required: true },
  paperHeightMm: { type: Number, required: true },
})

const store = usePrintDesignerStore()
const MAP_W = 220
const MAP_H = 160
const PAGE_GAP_MM = 18
const compact = ref(false)
const metrics = ref({ scrollLeft: 0, scrollTop: 0, clientWidth: 0, clientHeight: 0, scrollWidth: 1, scrollHeight: 1 })
const dragging = ref(false)
let dragOffset = { x: 0, y: 0 }
let raf = 0

const contentMm = computed(() => {
  const count = Math.max(1, props.pages.length)
  return {
    widthMm: props.paperWidthMm,
    heightMm: count * props.paperHeightMm + (count - 1) * PAGE_GAP_MM,
  }
})

const mapScale = computed(() => {
  const pad = 8
  const availW = MAP_W - pad * 2
  const availH = (compact.value ? 96 : MAP_H) - pad * 2
  return Math.min(availW / contentMm.value.widthMm, availH / contentMm.value.heightMm)
})

const mapSize = computed(() => ({
  width: Number((contentMm.value.widthMm * mapScale.value).toFixed(2)),
  height: Number((contentMm.value.heightMm * mapScale.value).toFixed(2)),
}))

const region = computed(() => computeMiniMapRegion(metrics.value, mapSize.value.width, mapSize.value.height))

const pagePreviews = computed(() => {
  const doc = store.document
  const margin = doc.paper.marginMm
  const scale = mapScale.value
  return props.pages.map((page, index) => {
    const topMm = index * (props.paperHeightMm + PAGE_GAP_MM)
    const blocks = []
    let bodyY = margin.top + doc.header.heightMm
    for (const surface of page.surfaces || []) {
      let surfaceTop = bodyY
      let surfaceHeight = surface.designHeightMm || 0
      if (surface.pageRole === 'header') {
        surfaceTop = margin.top
        surfaceHeight = doc.header.heightMm
      }
      else if (surface.pageRole === 'footer') {
        surfaceTop = props.paperHeightMm - margin.bottom - doc.footer.heightMm
        surfaceHeight = doc.footer.heightMm
      }
      else {
        bodyY += surfaceHeight + (surface.gapAfterMm || 0)
      }
      for (const element of surface.elements || []) {
        blocks.push({
          key: `${page.number}-${element.id}`,
          left: (margin.left + element.xMm) * scale,
          top: (surfaceTop + element.yMm) * scale,
          width: Math.max(1.5, element.widthMm * scale),
          height: Math.max(1.2, element.heightMm * scale),
        })
      }
    }
    return {
      number: page.number,
      top: topMm * scale,
      width: props.paperWidthMm * scale,
      height: props.paperHeightMm * scale,
      headerTop: margin.top * scale,
      headerHeight: Math.max(0, doc.header.heightMm * scale),
      footerTop: (props.paperHeightMm - margin.bottom - doc.footer.heightMm) * scale,
      footerHeight: Math.max(0, doc.footer.heightMm * scale),
      blocks,
    }
  })
})

function readMetrics() {
  const el = props.viewportEl
  if (!el)
    return
  metrics.value = {
    scrollLeft: el.scrollLeft,
    scrollTop: el.scrollTop,
    clientWidth: el.clientWidth,
    clientHeight: el.clientHeight,
    scrollWidth: el.scrollWidth,
    scrollHeight: el.scrollHeight,
  }
}

function scheduleRead() {
  cancelAnimationFrame(raf)
  raf = requestAnimationFrame(readMetrics)
}

function applyScroll(left, top) {
  const el = props.viewportEl
  if (!el)
    return
  const next = scrollFromMiniMapPoint(left, top, metrics.value, mapSize.value.width, mapSize.value.height)
  el.scrollLeft = next.scrollLeft
  el.scrollTop = next.scrollTop
  scheduleRead()
}

function onRegionDown(event) {
  if (event.button !== 0)
    return
  event.preventDefault()
  event.stopPropagation()
  dragging.value = true
  dragOffset = {
    x: event.clientX - region.value.left,
    y: event.clientY - region.value.top,
  }
  window.addEventListener('pointermove', onRegionMove)
  window.addEventListener('pointerup', onRegionUp)
}

function onRegionMove(event) {
  if (!dragging.value)
    return
  const left = event.clientX - dragOffset.x
  const top = event.clientY - dragOffset.y
  const maxLeft = Math.max(0, mapSize.value.width - region.value.width)
  const maxTop = Math.max(0, mapSize.value.height - region.value.height)
  applyScroll(Math.min(maxLeft, Math.max(0, left)), Math.min(maxTop, Math.max(0, top)))
}

function onRegionUp() {
  dragging.value = false
  window.removeEventListener('pointermove', onRegionMove)
  window.removeEventListener('pointerup', onRegionUp)
}

function onMapClick(event) {
  if (dragging.value || event.target.closest('.mini-region'))
    return
  const box = event.currentTarget.getBoundingClientRect()
  const x = event.clientX - box.left - region.value.width / 2
  const y = event.clientY - box.top - region.value.height / 2
  const maxLeft = Math.max(0, mapSize.value.width - region.value.width)
  const maxTop = Math.max(0, mapSize.value.height - region.value.height)
  applyScroll(Math.min(maxLeft, Math.max(0, x)), Math.min(maxTop, Math.max(0, y)))
}

function resetView() {
  const el = props.viewportEl
  if (!el)
    return
  el.scrollTo({ left: 0, top: 0, behavior: 'smooth' })
  scheduleRead()
}

watch(() => props.viewportEl, (el, _, onCleanup) => {
  if (!el)
    return
  readMetrics()
  el.addEventListener('scroll', scheduleRead, { passive: true })
  const observer = typeof ResizeObserver === 'function' ? new ResizeObserver(scheduleRead) : null
  observer?.observe(el)
  onCleanup(() => {
    el.removeEventListener('scroll', scheduleRead)
    observer?.disconnect()
  })
}, { immediate: true })

watch(() => [props.pages.length, props.paperWidthMm, props.paperHeightMm, store.zoom, store.document], scheduleRead, { deep: true })

onMounted(scheduleRead)
onBeforeUnmount(() => {
  cancelAnimationFrame(raf)
  onRegionUp()
})
</script>

<template>
  <aside v-if="store.miniMapOpen" class="print-minimap" :class="{ compact }" aria-label="概览图">
    <header class="mini-header">
      <span class="mini-title">
        <NIcon :component="MapOutline" :size="13" />
        概览图
      </span>
      <div class="mini-actions">
        <button type="button" title="回到左上角" aria-label="回到左上角" @click="resetView">
          <NIcon :component="LocateOutline" :size="13" />
        </button>
        <button type="button" :title="compact ? '展开' : '缩小'" :aria-label="compact ? '展开' : '缩小'" @click="compact = !compact">
          <NIcon :component="ContractOutline" :size="13" />
        </button>
        <button type="button" title="隐藏概览图" aria-label="隐藏概览图" @click="store.toggleMiniMap(false)">
          <NIcon :component="CloseOutline" :size="13" />
        </button>
      </div>
    </header>
    <div
      class="mini-body"
      :style="{ width: `${MAP_W}px`, height: `${compact ? 96 : MAP_H}px` }"
      @click="onMapClick"
    >
      <div
        class="mini-stage"
        :style="{ width: `${mapSize.width}px`, height: `${mapSize.height}px` }"
      >
        <div
          v-for="page in pagePreviews"
          :key="page.number"
          class="mini-page"
          :style="{ top: `${page.top}px`, width: `${page.width}px`, height: `${page.height}px` }"
        >
          <i class="mini-band header" :style="{ top: `${page.headerTop}px`, height: `${page.headerHeight}px` }" />
          <i class="mini-band footer" :style="{ top: `${page.footerTop}px`, height: `${page.footerHeight}px` }" />
          <i
            v-for="block in page.blocks"
            :key="block.key"
            class="mini-block"
            :style="{ left: `${block.left}px`, top: `${block.top}px`, width: `${block.width}px`, height: `${block.height}px` }"
          />
          <small>{{ page.number }}</small>
        </div>
        <div
          class="mini-region"
          :class="{ dragging }"
          :style="{ left: `${region.left}px`, top: `${region.top}px`, width: `${region.width}px`, height: `${region.height}px` }"
          @pointerdown="onRegionDown"
        />
      </div>
    </div>
  </aside>
</template>

<style scoped>
.print-minimap {
  position: absolute;
  z-index: 40;
  left: 12px;
  bottom: 12px;
  width: 240px;
  overflow: hidden;
  border: 1px solid var(--border-light, #d0d7e2);
  border-radius: 6px;
  background: var(--bg-primary, #fff);
  box-shadow: 0 4px 16px rgb(15 23 42 / 12%);
  color: var(--text-secondary, #475569);
  user-select: none;
}
.mini-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 28px;
  padding: 0 6px 0 8px;
  border-bottom: 1px solid var(--border-light, #e5e7eb);
  background: var(--gray-100, #f6f8fb);
}
.mini-title {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  font-weight: 600;
  color: var(--text-primary, #334155);
}
.mini-actions {
  display: inline-flex;
  gap: 1px;
}
.mini-actions button {
  width: 22px;
  height: 22px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 3px;
  color: var(--text-tertiary, #64748b);
  background: transparent;
  cursor: pointer;
}
.mini-actions button:hover {
  color: var(--primary-color);
  background: color-mix(in srgb, var(--primary-color) 10%, transparent);
}
.mini-body {
  display: grid;
  place-items: center;
  padding: 6px;
  background: var(--gray-100, #eef1f5);
  cursor: crosshair;
}
.mini-stage {
  position: relative;
}
.mini-page {
  position: absolute;
  left: 0;
  overflow: hidden;
  border: 1px solid var(--border-light, #cbd5e1);
  background: #fff;
  box-shadow: 0 1px 2px rgb(15 23 42 / 6%);
}
.mini-page small {
  position: absolute;
  right: 2px;
  bottom: 1px;
  color: var(--text-tertiary, #94a3b8);
  font-size: 8px;
  line-height: 1;
}
.mini-band {
  position: absolute;
  right: 0;
  left: 0;
  display: block;
  background: color-mix(in srgb, var(--text-tertiary, #94a3b8) 12%, transparent);
}
.mini-block {
  position: absolute;
  display: block;
  border-radius: 1px;
  background: color-mix(in srgb, var(--primary-color) 72%, transparent);
  opacity: 0.85;
}
.mini-region {
  position: absolute;
  z-index: 2;
  box-sizing: border-box;
  border: 1.5px solid var(--primary-color);
  border-radius: 2px;
  background: color-mix(in srgb, var(--primary-color) 14%, transparent);
  cursor: grab;
  box-shadow: inset 0 0 0 1px rgb(255 255 255 / 45%);
}
.mini-region.dragging,
.mini-region:active {
  cursor: grabbing;
}
.print-minimap.compact {
  width: 200px;
}
</style>
