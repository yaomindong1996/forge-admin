import { ref } from 'vue'

/**
 * scroll-view 自带下拉刷新：正文在 scroll-view 内滚动的页面，页面级 onPullDownRefresh 不会触发。
 * 模板绑定 refresher-enabled、:refresher-triggered="refreshing"、@refresherrefresh="onRefresh"。
 * @param {() => Promise<unknown>} task
 */
export function usePullRefresh(task) {
  const refreshing = ref(false)

  async function onRefresh() {
    if (refreshing.value) return
    refreshing.value = true
    try { await task() }
    finally { refreshing.value = false }
  }

  return { refreshing, onRefresh }
}
