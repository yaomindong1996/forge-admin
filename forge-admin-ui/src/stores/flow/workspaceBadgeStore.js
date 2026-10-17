import { defineStore } from 'pinia'
import { computed, ref, watch } from 'vue'
import { getWorkspaceSummary } from '@/api/workspace'
import { useAuthStore, useUserStore } from '@/store'

/** 菜单 path → 工作台统计字段；/flow 与 /workspace 两套入口共用同一套数 */
const PATH_COUNT_KEYS = {
  '/flow/todo': 'todoCount',
  '/workspace/todo': 'todoCount',
  '/flow/done': 'doneWeekCount',
  '/workspace/done': 'doneWeekCount',
  '/flow/started': 'startedRunningCount',
  '/workspace/started': 'startedRunningCount',
  '/flow/cc': 'ccUnreadCount',
  '/workspace/cc': 'ccUnreadCount',
}

const EMPHASIS_KEYS = new Set(['todoCount', 'ccUnreadCount'])
const POLL_INTERVAL_MS = 60_000

function normalizeMenuPath(path) {
  const value = String(path || '').trim()
  if (!value)
    return ''
  const [pathWithoutHash] = value.split('#')
  const [pathname] = pathWithoutHash.split('?')
  const normalized = pathname.replace(/\/+/g, '/')
  if (!normalized || normalized === '/')
    return normalized
  return normalized.startsWith('/') ? normalized : `/${normalized}`
}

function emptyCounts() {
  return {
    todoCount: 0,
    doneWeekCount: 0,
    startedRunningCount: 0,
    ccUnreadCount: 0,
  }
}

export const useWorkspaceBadgeStore = defineStore('flow-workspace-badge', () => {
  const auth = useAuthStore()
  const user = useUserStore()
  const counts = ref(emptyCounts())
  const loading = ref(false)
  const contextVersion = ref(0)
  const ready = computed(() => Boolean(auth.accessToken && user.userId))

  let pollTimer = null
  let refreshSequence = 0
  let pending = null
  let subscribers = 0

  function onVisibilityChange() {
    if (document.visibilityState === 'visible' && ready.value && subscribers > 0)
      refresh()
  }

  function countKeyForPath(path) {
    return PATH_COUNT_KEYS[normalizeMenuPath(path)] || ''
  }

  function badgeForPath(path) {
    const key = countKeyForPath(path)
    if (!key)
      return 0
    return Number(counts.value[key] || 0)
  }

  function badgeToneForPath(path) {
    const key = countKeyForPath(path)
    return EMPHASIS_KEYS.has(key) ? 'emphasis' : 'muted'
  }

  function formatBadge(value) {
    const num = Number(value || 0)
    if (!Number.isFinite(num) || num <= 0)
      return ''
    return num > 99 ? '99+' : String(Math.floor(num))
  }

  /**
   * 给菜单树挂 badge / badgeTone；目录节点汇总子节点徽标（便于折叠态一眼看到）。
   */
  function attachBadges(items = []) {
    return (items || []).map((item) => {
      const children = Array.isArray(item?.children) ? attachBadges(item.children) : []
      const own = badgeForPath(item?.path)
      const childSum = (children || []).reduce((sum, child) => sum + Number(child.badge || 0), 0)
      const badge = own > 0 ? own : childSum
      const tone = own > 0
        ? badgeToneForPath(item?.path)
        : (children || []).some(child => child.badgeTone === 'emphasis') ? 'emphasis' : 'muted'
      return {
        ...item,
        children,
        badge: badge > 0 ? badge : undefined,
        badgeTone: badge > 0 ? tone : undefined,
        badgeText: formatBadge(badge),
      }
    })
  }

  async function refresh() {
    if (!ready.value)
      return
    if (pending)
      return pending
    const context = contextVersion.value
    const sequence = ++refreshSequence
    loading.value = true
    const task = (async () => {
      try {
        const res = await getWorkspaceSummary()
        if (context !== contextVersion.value || sequence !== refreshSequence)
          return
        if (res?.code !== 200) {
          counts.value = emptyCounts()
          return
        }
        const data = res.data || {}
        counts.value = {
          todoCount: Number(data.todoCount || 0),
          doneWeekCount: Number(data.doneWeekCount || 0),
          startedRunningCount: Number(data.startedRunningCount || 0),
          ccUnreadCount: Number(data.ccUnreadCount || 0),
        }
      }
      catch {
        if (context === contextVersion.value && sequence === refreshSequence)
          counts.value = emptyCounts()
      }
      finally {
        if (context === contextVersion.value && sequence === refreshSequence) {
          loading.value = false
          pending = null
        }
      }
    })()
    pending = task
    return task
  }

  function stopPolling() {
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
  }

  function startPolling() {
    stopPolling()
    if (!ready.value)
      return
    pollTimer = setInterval(() => {
      if (document.visibilityState === 'hidden')
        return
      refresh()
    }, POLL_INTERVAL_MS)
  }

  /** 侧栏等布局挂载时订阅；最后一个卸载时停轮询 */
  function subscribe() {
    subscribers += 1
    if (subscribers === 1) {
      refresh()
      startPolling()
      if (typeof document !== 'undefined')
        document.addEventListener('visibilitychange', onVisibilityChange)
    }
    return () => {
      subscribers = Math.max(0, subscribers - 1)
      if (subscribers === 0) {
        stopPolling()
        if (typeof document !== 'undefined')
          document.removeEventListener('visibilitychange', onVisibilityChange)
      }
    }
  }

  function reset() {
    contextVersion.value += 1
    refreshSequence += 1
    pending = null
    counts.value = emptyCounts()
    loading.value = false
    stopPolling()
  }

  watch(() => [auth.accessToken, user.userId, user.tenantId], () => {
    reset()
    if (ready.value && subscribers > 0) {
      refresh()
      startPolling()
    }
  }, { flush: 'sync' })

  return {
    counts,
    loading,
    ready,
    refresh,
    subscribe,
    attachBadges,
    badgeForPath,
    formatBadge,
  }
})
