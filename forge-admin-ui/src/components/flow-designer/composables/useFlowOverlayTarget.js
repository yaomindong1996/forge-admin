import { useEventListener } from '@vueuse/core'
import { onMounted, shallowRef } from 'vue'

/** 浏览器全屏只显示全屏元素的后代；Naive 浮层必须跟随，否则菜单会消失在 body 中。 */
export function useFlowOverlayTarget() {
  const target = shallowRef(null)
  const doc = typeof document === 'undefined' ? null : document
  const sync = () => {
    target.value = doc?.fullscreenElement || null
  }
  onMounted(sync)
  useEventListener(doc, 'fullscreenchange', sync)
  return target
}
