import { onScopeDispose } from 'vue'

// Window listeners keep the gesture alive outside the canvas; every exit cleans up.
export function usePrintDrag(store) {
  let cleanup = () => {}
  function start(event, options = {}) {
    if (event.button !== 0 || !store.selectedIds.length) {
      return
    }
    const { resize = false, handle = 'se' } = options
    cleanup()
    event.preventDefault()
    if (!store.beginGesture())
      return
    const { clientX, clientY, pointerId } = event
    const move = (next) => {
      if (next.pointerId === pointerId) {
        store.moveGesture(next.clientX - clientX, next.clientY - clientY, resize, handle, { snap: !next.altKey })
      }
    }
    const finish = (next) => {
      if (next.pointerId === pointerId) {
        move(next)
        store.endGesture()
        cleanup()
      }
    }
    const cancel = () => {
      store.cancelGesture()
      cleanup()
    }
    const key = (next) => {
      if (next.key === 'Escape') {
        cancel()
      }
    }
    window.addEventListener('pointermove', move)
    window.addEventListener('pointerup', finish)
    window.addEventListener('pointercancel', cancel)
    window.addEventListener('blur', cancel)
    window.addEventListener('keydown', key)
    cleanup = () => {
      window.removeEventListener('pointermove', move)
      window.removeEventListener('pointerup', finish)
      window.removeEventListener('pointercancel', cancel)
      window.removeEventListener('blur', cancel)
      window.removeEventListener('keydown', key)
      cleanup = () => {}
    }
  }
  onScopeDispose(() => {
    cleanup()
    store.cancelGesture()
  })
  return { start }
}
