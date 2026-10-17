import { defineStore } from 'pinia'
import { computed, onScopeDispose, ref, watch } from 'vue'
import { getRecentDownloads } from '@/api/system/downloads'
import { useAuthStore } from '@/store/modules/auth'
import { useUserStore } from '@/store/modules/user'
import { DOWNLOAD_EVENT } from '@/utils/download-events'
import { downloadFile } from '@/utils/file'
import {
  downloadDay,
  downloadScope,
  readHiddenDownloads,
  RECENT_DOWNLOADS_LIMIT,
  writeHiddenDownloads,
} from './recentDownloadsUtils'

export const useRecentDownloadsStore = defineStore('recent-downloads', () => {
  const auth = useAuthStore()
  const user = useUserStore()
  const open = ref(false)
  const loading = ref(false)
  const error = ref('')
  const records = ref([])
  const tasks = ref([])
  const hidden = ref([])
  const page = ref(0)
  const total = ref(0)
  const scope = computed(() => auth.accessToken ? downloadScope(user.tenantId, user.userId) : '')
  const available = computed(() => !!scope.value)
  const seeAll = computed(() => user.isAdmin || user.isTenantAdmin || Number(user.userType) === 1)
  const activeCount = computed(() => tasks.value.filter(task => task.phase === 'receiving').length)
  const visibleRecords = computed(() => records.value.filter(row => !hidden.value.includes(String(row.id))))
  const canClear = computed(() => visibleRecords.value.length > 0 || tasks.value.some(task => task.phase === 'failed'))
  const hasMore = computed(() => records.value.length < Math.min(total.value, RECENT_DOWNLOADS_LIMIT))
  const limitReached = computed(() => total.value > RECENT_DOWNLOADS_LIMIT && !hasMore.value)
  const groups = computed(() => {
    const rows = [
      ...tasks.value.map(task => ({ ...task, key: `task:${task.id}`, createTime: task.startedAt, runtime: true })),
      ...visibleRecords.value.map(row => ({ ...row, key: `record:${row.id}` })),
    ]
    return ['今天', '更早']
      .map(label => ({ label, rows: rows.filter(row => downloadDay(row.createTime) === label) }))
      .filter(group => group.rows.length)
  })
  let generation = 0
  let pending = null
  let refreshQueued = false

  function reset() {
    generation++
    pending = null
    refreshQueued = false
    open.value = false
    loading.value = false
    error.value = ''
    records.value = []
    tasks.value = []
    page.value = 0
    total.value = 0
    hidden.value = readHiddenDownloads(scope.value)
  }

  function refresh(append = false) {
    if (!scope.value)
      return Promise.resolve()
    if (pending)
      return pending
    const current = generation
    const nextPage = append ? page.value + 1 : 1
    loading.value = true
    error.value = ''
    pending = getRecentDownloads(nextPage)
      .then((data) => {
        if (current !== generation)
          return
        const merged = append ? [...records.value, ...data.records] : data.records
        records.value = [...new Map(merged.map(row => [String(row.id), row])).values()].slice(0, RECENT_DOWNLOADS_LIMIT)
        total.value = Number(data.total) || 0
        page.value = nextPage
      })
      .catch(() => {
        if (current === generation)
          error.value = append ? '更多下载记录加载失败，请重试' : '下载记录加载失败，请重试'
      })
      .finally(() => {
        if (current !== generation)
          return
        loading.value = false
        pending = null
        if (refreshQueued) {
          refreshQueued = false
          refresh()
        }
      })
    return pending
  }

  function recorded() {
    if (!open.value)
      return
    if (pending)
      refreshQueued = true
    else
      refresh()
  }

  function receive(event) {
    const task = event.detail
    if (!scope.value || task?.scope !== scope.value || !task.fileId)
      return
    const index = tasks.value.findIndex(row => row.id === task.id)
    if (task.phase === 'saved') {
      tasks.value = tasks.value.filter(row => row.id !== task.id)
      recorded()
      return
    }
    if (!['receiving', 'failed'].includes(task.phase))
      return
    if (index >= 0)
      tasks.value[index] = { ...tasks.value[index], ...task }
    else
      tasks.value.unshift(task)
  }

  async function download(row) {
    if (!row.fileId || tasks.value.some(task => task.fileId === row.fileId && task.phase === 'receiving'))
      return
    tasks.value = tasks.value.filter(task => !(task.fileId === row.fileId && task.phase === 'failed'))
    try {
      await downloadFile(row.fileId, row.fileName)
    }
    catch {
      // 统一下载工具广播失败态；面板提供重试，不再抛出未处理的 Promise。
    }
  }

  function clear() {
    hidden.value = writeHiddenDownloads(scope.value, [...hidden.value, ...records.value.map(row => row.id)])
    tasks.value = tasks.value.filter(task => task.phase === 'receiving')
  }

  watch(() => [scope.value, auth.accessToken, seeAll.value], reset, { immediate: true, flush: 'sync' })
  watch(open, (visible) => {
    if (visible)
      refresh()
  })
  window.addEventListener(DOWNLOAD_EVENT, receive)
  window.addEventListener('forge-download-recorded', recorded)
  onScopeDispose(() => {
    window.removeEventListener(DOWNLOAD_EVENT, receive)
    window.removeEventListener('forge-download-recorded', recorded)
  })

  return {
    available,
    open,
    loading,
    error,
    groups,
    tasks,
    activeCount,
    seeAll,
    canClear,
    hasMore,
    limitReached,
    refresh,
    download,
    clear,
  }
})
