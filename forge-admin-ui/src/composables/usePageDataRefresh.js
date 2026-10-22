/**
 * 顶栏「刷新」与页面内数据组件联动：
 * reloadTab 会重挂路由视图；同时广播 forge:page-data-refresh，
 * 让未随路由销毁的挂件（图表 / 待办 / 公告等）主动重新拉数。
 */
import { onBeforeUnmount, onMounted, ref } from 'vue'

export const PAGE_DATA_REFRESH_EVENT = 'forge:page-data-refresh'

const pageDataRefreshEpoch = ref(0)

export function bumpPageDataRefresh(reason = 'manual') {
  pageDataRefreshEpoch.value += 1
  if (typeof window === 'undefined')
    return pageDataRefreshEpoch.value
  window.dispatchEvent(new CustomEvent(PAGE_DATA_REFRESH_EVENT, {
    detail: {
      epoch: pageDataRefreshEpoch.value,
      reason,
      at: Date.now(),
    },
  }))
  return pageDataRefreshEpoch.value
}

export function usePageDataRefresh(handler) {
  onMounted(() => {
    if (typeof handler !== 'function' || typeof window === 'undefined')
      return undefined
    const onRefresh = (event) => {
      handler(event?.detail || { epoch: pageDataRefreshEpoch.value })
    }
    window.addEventListener(PAGE_DATA_REFRESH_EVENT, onRefresh)
    onBeforeUnmount(() => {
      window.removeEventListener(PAGE_DATA_REFRESH_EVENT, onRefresh)
    })
    return undefined
  })
  return pageDataRefreshEpoch
}

export function getPageDataRefreshEpoch() {
  return pageDataRefreshEpoch.value
}
