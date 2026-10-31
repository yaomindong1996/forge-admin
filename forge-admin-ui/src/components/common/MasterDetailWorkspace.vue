<template>
  <section
    ref="rootRef"
    class="master-detail-workspace"
    :class="{
      'is-collapsed': collapsed,
      'is-separated': !attached,
      'is-resizing': resizing,
    }"
    :style="workspaceStyle"
  >
    <aside class="master-detail-workspace__aside">
      <slot name="aside" :collapsed="collapsed" />
    </aside>
    <div
      v-if="attached"
      class="master-detail-workspace__divider"
      :class="{ 'is-interactive': canResize }"
      role="separator"
      aria-orientation="vertical"
      :aria-valuenow="canResize ? currentAsideWidth : undefined"
      :aria-valuemin="canResize ? minAsideWidth : undefined"
      :aria-valuemax="canResize ? effectiveMaxAsideWidth : undefined"
      :aria-label="canResize ? '拖动调整左侧宽度' : undefined"
      :title="canResize ? '拖动调整左侧宽度' : undefined"
      :tabindex="canResize ? 0 : -1"
      @pointerdown="onDividerPointerDown"
      @keydown.left.prevent="nudgeAside(-8)"
      @keydown.right.prevent="nudgeAside(8)"
    >
      <span v-if="canResize" class="master-detail-workspace__grip" aria-hidden="true">
        <i /><i /><i />
      </span>
    </div>
    <main class="master-detail-workspace__main">
      <slot />
    </main>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'

defineOptions({ name: 'MasterDetailWorkspace' })

const props = defineProps({
  /** 左侧主对象区域宽度。 */
  asideWidth: {
    type: [Number, String],
    default: 260,
  },
  /** 收起后左侧主对象区域宽度。 */
  collapsedAsideWidth: {
    type: [Number, String],
    default: 64,
  },
  /** 右侧工作区宽度，默认占用剩余空间。 */
  mainWidth: {
    type: [Number, String],
    default: 'minmax(0, 1fr)',
  },
  /** 是否收起左侧主对象区域。 */
  collapsed: {
    type: Boolean,
    default: false,
  },
  /** 是否以同一张工作台承载两侧内容。 */
  attached: {
    type: Boolean,
    default: true,
  },
  /** 是否允许拖动分割线调整左侧宽度（折叠 / 窄屏自动禁用）。 */
  resizable: {
    type: Boolean,
    default: true,
  },
  /** 左侧最小宽度（px）。 */
  minAsideWidth: {
    type: Number,
    default: 200,
  },
  /** 左侧最大宽度（px）；0 表示按容器宽度 40% 与 480 取较小值。 */
  maxAsideWidth: {
    type: Number,
    default: 0,
  },
  /** 写入 localStorage 的键；有值时记忆拖拽宽度。 */
  persistKey: {
    type: String,
    default: '',
  },
})

const rootRef = ref(null)
const currentAsideWidth = ref(resolveInitialWidth())
const resizing = ref(false)
const narrowViewport = ref(false)

let pointerId = null
let startX = 0
let startWidth = 0

const effectiveMaxAsideWidth = computed(() => {
  if (props.maxAsideWidth > 0)
    return props.maxAsideWidth
  const containerWidth = rootRef.value?.clientWidth || 1200
  return Math.min(480, Math.max(props.minAsideWidth + 40, Math.floor(containerWidth * 0.4)))
})

const canResize = computed(() => (
  props.resizable
  && props.attached
  && !props.collapsed
  && !narrowViewport.value
))

const workspaceStyle = computed(() => ({
  '--master-detail-aside-width': `${currentAsideWidth.value}px`,
  '--master-detail-collapsed-aside-width': normalizeSize(props.collapsedAsideWidth),
  '--master-detail-main-width': normalizeSize(props.mainWidth),
}))

watch(() => props.asideWidth, (value) => {
  if (props.persistKey && readPersistedWidth() != null)
    return
  currentAsideWidth.value = clampWidth(toNumberWidth(value, 260))
})

watch(() => props.persistKey, () => {
  currentAsideWidth.value = resolveInitialWidth()
})

onMounted(() => {
  updateNarrowViewport()
  window.addEventListener('resize', updateNarrowViewport)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateNarrowViewport)
  stopResize()
})

function resolveInitialWidth() {
  const persisted = readPersistedWidth()
  if (persisted != null)
    return clampWidth(persisted)
  return clampWidth(toNumberWidth(props.asideWidth, 260))
}

function readPersistedWidth() {
  if (!props.persistKey || typeof localStorage === 'undefined')
    return null
  try {
    const raw = localStorage.getItem(storageKey(props.persistKey))
    const value = Number(raw)
    return Number.isFinite(value) ? value : null
  }
  catch {
    return null
  }
}

function writePersistedWidth(width) {
  if (!props.persistKey || typeof localStorage === 'undefined')
    return
  try {
    localStorage.setItem(storageKey(props.persistKey), String(width))
  }
  catch {
    // 隐私模式等写失败时忽略
  }
}

function storageKey(key) {
  return `forge.master-detail.aside-width.${key}`
}

function toNumberWidth(value, fallback) {
  if (typeof value === 'number' && Number.isFinite(value))
    return value
  if (typeof value === 'string') {
    const parsed = Number.parseFloat(value)
    if (Number.isFinite(parsed))
      return parsed
  }
  return fallback
}

function clampWidth(width) {
  const max = props.maxAsideWidth > 0
    ? props.maxAsideWidth
    : Math.max(props.minAsideWidth + 40, 480)
  return Math.min(max, Math.max(props.minAsideWidth, Math.round(width)))
}

function normalizeSize(value) {
  return typeof value === 'number' ? `${value}px` : value
}

function updateNarrowViewport() {
  narrowViewport.value = window.matchMedia('(max-width: 960px)').matches
  currentAsideWidth.value = clampWidth(currentAsideWidth.value)
}

function onDividerPointerDown(event) {
  if (!canResize.value || event.button !== 0)
    return
  event.preventDefault()
  resizing.value = true
  pointerId = event.pointerId
  startX = event.clientX
  startWidth = currentAsideWidth.value
  event.currentTarget.setPointerCapture?.(pointerId)
  window.addEventListener('pointermove', onPointerMove)
  window.addEventListener('pointerup', onPointerUp)
  window.addEventListener('pointercancel', onPointerUp)
}

function onPointerMove(event) {
  if (!resizing.value)
    return
  const next = clampLiveWidth(startWidth + (event.clientX - startX))
  currentAsideWidth.value = next
}

function onPointerUp() {
  if (!resizing.value)
    return
  writePersistedWidth(currentAsideWidth.value)
  stopResize()
}

function stopResize() {
  resizing.value = false
  pointerId = null
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('pointerup', onPointerUp)
  window.removeEventListener('pointercancel', onPointerUp)
}

function clampLiveWidth(width) {
  const max = effectiveMaxAsideWidth.value
  return Math.min(max, Math.max(props.minAsideWidth, Math.round(width)))
}

function nudgeAside(delta) {
  if (!canResize.value)
    return
  currentAsideWidth.value = clampLiveWidth(currentAsideWidth.value + delta)
  writePersistedWidth(currentAsideWidth.value)
}
</script>

<style scoped>
.master-detail-workspace {
  display: grid;
  grid-template-columns: var(--master-detail-aside-width) 8px var(--master-detail-main-width);
  height: 100%;
  min-height: 0;
  overflow: hidden;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 6px;
  background: var(--bg-primary, #fff);
}

.master-detail-workspace.is-collapsed {
  grid-template-columns: var(--master-detail-collapsed-aside-width) 8px var(--master-detail-main-width);
}

.master-detail-workspace.is-resizing {
  cursor: col-resize;
  user-select: none;
}

.master-detail-workspace.is-resizing * {
  cursor: col-resize !important;
  user-select: none !important;
}

.master-detail-workspace__aside,
.master-detail-workspace__main {
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.master-detail-workspace__aside {
  background: var(--bg-primary, #fff);
}

.master-detail-workspace__divider {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 0;
  background: var(--border-light, #e5e7eb);
}

.master-detail-workspace__divider.is-interactive {
  cursor: col-resize;
  touch-action: none;
  background: transparent;
}

/* 扩大可点区域，视觉线仍居中 */
.master-detail-workspace__divider.is-interactive::before {
  content: '';
  position: absolute;
  inset: 0 3px;
  border-radius: 999px;
  background: var(--border-light, #e5e7eb);
  transition:
    inset 0.16s ease,
    background-color 0.16s ease,
    box-shadow 0.16s ease;
}

.master-detail-workspace__grip {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  width: 14px;
  height: 36px;
  border: 1px solid color-mix(in srgb, var(--border-light, #e5e7eb) 88%, var(--text-tertiary, #9ca3af));
  border-radius: 999px;
  background: var(--bg-primary, #fff);
  box-shadow: 0 1px 2px color-mix(in srgb, var(--text-primary, #111827) 6%, transparent);
  opacity: 0.72;
  transform: scale(0.92);
  transition:
    opacity 0.16s ease,
    transform 0.16s ease,
    border-color 0.16s ease,
    background-color 0.16s ease,
    box-shadow 0.16s ease;
}

.master-detail-workspace__grip i {
  display: block;
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: var(--text-tertiary, #9ca3af);
  transition: background-color 0.16s ease;
}

.master-detail-workspace__divider.is-interactive:hover::before,
.master-detail-workspace.is-resizing .master-detail-workspace__divider.is-interactive::before,
.master-detail-workspace__divider.is-interactive:focus-visible::before {
  inset: 0 2px;
  background: color-mix(in srgb, var(--primary-color, #2563eb) 62%, var(--border-light, #e5e7eb));
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--primary-color, #2563eb) 16%, transparent);
}

.master-detail-workspace__divider.is-interactive:hover .master-detail-workspace__grip,
.master-detail-workspace.is-resizing .master-detail-workspace__divider.is-interactive .master-detail-workspace__grip,
.master-detail-workspace__divider.is-interactive:focus-visible .master-detail-workspace__grip {
  opacity: 1;
  transform: scale(1);
  border-color: color-mix(in srgb, var(--primary-color, #2563eb) 42%, var(--border-light, #e5e7eb));
  background: color-mix(in srgb, var(--primary-color, #2563eb) 8%, var(--bg-primary, #fff));
  box-shadow: 0 2px 8px color-mix(in srgb, var(--primary-color, #2563eb) 18%, transparent);
}

.master-detail-workspace__divider.is-interactive:hover .master-detail-workspace__grip i,
.master-detail-workspace.is-resizing .master-detail-workspace__divider.is-interactive .master-detail-workspace__grip i,
.master-detail-workspace__divider.is-interactive:focus-visible .master-detail-workspace__grip i {
  background: var(--primary-color, #2563eb);
}

.master-detail-workspace__divider.is-interactive:focus-visible {
  outline: none;
}

.master-detail-workspace__main {
  display: flex;
  flex-direction: column;
  background: var(--bg-primary, #fff);
}

.master-detail-workspace.is-separated {
  grid-template-columns: var(--master-detail-aside-width) var(--master-detail-main-width);
  gap: 8px;
  overflow: visible;
  border: 0;
  border-radius: 0;
  background: transparent;
}

.master-detail-workspace.is-separated.is-collapsed {
  grid-template-columns: var(--master-detail-collapsed-aside-width) var(--master-detail-main-width);
}

:global(.dark) .master-detail-workspace {
  border-color: var(--border-light);
  background: #0f172a;
}

:global(.dark) .master-detail-workspace__aside,
:global(.dark) .master-detail-workspace__main {
  background: #0f172a;
}

:global(.dark) .master-detail-workspace__divider.is-interactive::before {
  background: #334155;
}

:global(.dark) .master-detail-workspace__grip {
  border-color: #475569;
  background: #0f172a;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.35);
}

:global(.dark) .master-detail-workspace__grip i {
  background: #94a3b8;
}

:global(.dark) .master-detail-workspace__divider.is-interactive:hover .master-detail-workspace__grip,
:global(.dark) .master-detail-workspace.is-resizing .master-detail-workspace__divider.is-interactive .master-detail-workspace__grip,
:global(.dark) .master-detail-workspace__divider.is-interactive:focus-visible .master-detail-workspace__grip {
  border-color: color-mix(in srgb, var(--primary-color, #60a5fa) 50%, #475569);
  background: color-mix(in srgb, var(--primary-color, #60a5fa) 14%, #0f172a);
}

:global(.dark) .master-detail-workspace__divider.is-interactive:hover .master-detail-workspace__grip i,
:global(.dark) .master-detail-workspace.is-resizing .master-detail-workspace__divider.is-interactive .master-detail-workspace__grip i,
:global(.dark) .master-detail-workspace__divider.is-interactive:focus-visible .master-detail-workspace__grip i {
  background: #93c5fd;
}

@media (max-width: 960px) {
  .master-detail-workspace,
  .master-detail-workspace.is-collapsed,
  .master-detail-workspace.is-separated,
  .master-detail-workspace.is-separated.is-collapsed {
    grid-template-columns: minmax(0, 1fr);
    grid-template-rows: minmax(220px, 36dvh) minmax(0, 1fr);
    overflow-y: auto;
  }

  .master-detail-workspace__divider {
    display: none;
  }

  .master-detail-workspace__aside {
    border-bottom: 1px solid var(--border-light, #e5e7eb);
  }

  :global(.dark) .master-detail-workspace__aside {
    border-bottom-color: var(--border-light);
  }
}
</style>
