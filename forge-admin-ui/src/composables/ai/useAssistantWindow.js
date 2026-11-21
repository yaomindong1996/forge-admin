import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

const MARGIN = 18
const MIN_PANEL_W = 360
const MIN_PANEL_H = 440

function clamp(value, min, max) {
  return Math.min(Math.max(value, min), Math.max(min, max))
}

export function useAssistantWindow() {
  const viewport = ref({
    width: typeof window !== 'undefined' ? window.innerWidth : 1280,
    height: typeof window !== 'undefined' ? window.innerHeight : 800,
  })
  const maximized = ref(false)
  const dragging = ref(false)
  const launcherPos = ref({ right: MARGIN, bottom: MARGIN })
  const panelBox = ref({
    right: MARGIN,
    bottom: MARGIN,
    width: 420,
    height: 640,
  })

  let activeMode = null
  let dragStart = null
  let moved = false

  function handleResize() {
    viewport.value = { width: window.innerWidth, height: window.innerHeight }
  }

  onMounted(() => {
    window.addEventListener('resize', handleResize)
  })

  onBeforeUnmount(() => {
    window.removeEventListener('resize', handleResize)
  })

  function start(mode, event) {
    if (event.button !== 0)
      return
    if (maximized.value && mode !== 'launcher')
      return
    activeMode = mode
    dragStart = {
      x: event.clientX,
      y: event.clientY,
      launcherRight: launcherPos.value.right,
      launcherBottom: launcherPos.value.bottom,
      right: panelBox.value.right,
      bottom: panelBox.value.bottom,
      width: panelBox.value.width,
      height: panelBox.value.height,
    }
    moved = false
    dragging.value = true
    event.currentTarget?.setPointerCapture?.(event.pointerId)
  }

  function move(event) {
    if (!dragStart || !activeMode)
      return
    const dx = event.clientX - dragStart.x
    const dy = event.clientY - dragStart.y
    if (Math.abs(dx) > 4 || Math.abs(dy) > 4)
      moved = true

    const vw = viewport.value.width
    const vh = viewport.value.height

    if (activeMode === 'launcher') {
      launcherPos.value = {
        right: clamp(dragStart.launcherRight - dx, 12, vw - 130),
        bottom: clamp(dragStart.launcherBottom - dy, 12, vh - 56),
      }
      return
    }

    if (activeMode === 'panel') {
      const w = Math.min(panelBox.value.width, vw - 24)
      const h = Math.min(panelBox.value.height, vh - 24)
      panelBox.value = {
        ...panelBox.value,
        right: clamp(dragStart.right - dx, 12, vw - w - 12),
        bottom: clamp(dragStart.bottom - dy, 12, vh - h - 12),
      }
      return
    }

    if (activeMode === 'resize-w') {
      const maxW = Math.min(820, vw - dragStart.right - 12)
      panelBox.value = {
        ...panelBox.value,
        width: clamp(dragStart.width - dx, MIN_PANEL_W, maxW),
      }
      return
    }

    if (activeMode === 'resize') {
      const maxW = Math.min(860, vw - 24)
      const maxH = Math.min(840, vh - 24)
      panelBox.value = {
        ...panelBox.value,
        width: clamp(dragStart.width + dx, MIN_PANEL_W, maxW),
        height: clamp(dragStart.height + dy, MIN_PANEL_H, maxH),
      }
    }
  }

  function end() {
    activeMode = null
    dragStart = null
    dragging.value = false
  }

  function shouldAllowClick() {
    if (moved) {
      moved = false
      return false
    }
    return true
  }

  // Backward-compatible aliases
  function startDrag(event) {
    start('launcher', event)
  }

  const launcherStyle = computed(() => ({
    right: `${launcherPos.value.right}px`,
    bottom: `${launcherPos.value.bottom}px`,
  }))

  const panelStyle = computed(() => {
    if (maximized.value) {
      return {
        right: '16px',
        bottom: '16px',
        width: 'min(920px, calc(100vw - 32px))',
        height: 'min(780px, calc(100vh - 32px))',
      }
    }
    return {
      right: `${clamp(panelBox.value.right, 12, viewport.value.width - 320)}px`,
      bottom: `${clamp(panelBox.value.bottom, 12, viewport.value.height - 320)}px`,
      width: `min(${panelBox.value.width}px, calc(100vw - 24px))`,
      height: `min(${panelBox.value.height}px, calc(100vh - 36px))`,
    }
  })

  return {
    maximized,
    dragging,
    launcherStyle,
    panelStyle,
    start,
    move,
    end,
    startDrag,
    moveDrag: move,
    endDrag: end,
    shouldAllowClick,
  }
}
