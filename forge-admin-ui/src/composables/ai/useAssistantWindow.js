import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

const DEFAULT_MARGIN = 20

function clamp(value, min, max) {
  return Math.min(Math.max(value, min), Math.max(min, max))
}

export function useAssistantWindow() {
  const viewport = ref({ width: typeof window !== 'undefined' ? window.innerWidth : 1280, height: typeof window !== 'undefined' ? window.innerHeight : 800 })
  const maximized = ref(false)
  const dragging = ref(false)
  const launcherPos = ref({ right: DEFAULT_MARGIN, bottom: DEFAULT_MARGIN })
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

  function startDrag(event) {
    if (event.button !== 0)
      return
    dragStart = {
      x: event.clientX,
      y: event.clientY,
      right: launcherPos.value.right,
      bottom: launcherPos.value.bottom,
    }
    moved = false
    dragging.value = true
    event.currentTarget?.setPointerCapture?.(event.pointerId)
  }

  function moveDrag(event) {
    if (!dragStart)
      return
    const dx = event.clientX - dragStart.x
    const dy = event.clientY - dragStart.y
    if (Math.abs(dx) > 4 || Math.abs(dy) > 4)
      moved = true
    launcherPos.value = {
      right: clamp(dragStart.right - dx, 12, viewport.value.width - 120),
      bottom: clamp(dragStart.bottom - dy, 12, viewport.value.height - 64),
    }
  }

  function endDrag() {
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
        height: 'min(760px, calc(100vh - 32px))',
      }
    }
    return {
      right: `${DEFAULT_MARGIN}px`,
      bottom: `${DEFAULT_MARGIN}px`,
      width: 'min(420px, calc(100vw - 24px))',
      height: 'min(620px, calc(100vh - 48px))',
    }
  })

  return {
    maximized,
    dragging,
    launcherStyle,
    panelStyle,
    startDrag,
    moveDrag,
    endDrag,
    shouldAllowClick,
  }
}
