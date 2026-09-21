import { usePrintDrag } from './usePrintDrag'

export function usePrintResize(store) {
  const drag = usePrintDrag(store)
  return {
    start(event, handle = 'se') {
      drag.start(event, { resize: true, handle })
    },
  }
}
