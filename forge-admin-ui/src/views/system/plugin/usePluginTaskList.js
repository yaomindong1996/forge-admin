import { computed, reactive } from 'vue'
import { listPluginTasks } from '@/api/system/pluginTask'
import { taskResponse } from './pluginTaskUtils'
import { useLatestPluginRequest } from './useLatestPluginRequest'

async function fetchTasks(params) {
  const data = taskResponse(await listPluginTasks(params))
  if (!Array.isArray(data.records))
    throw new Error('插件任务列表无效')
  return data
}

export function usePluginTaskList() {
  const query = reactive({ keyword: '', status: null, pageNum: 1, pageSize: 15 })
  const request = useLatestPluginRequest(fetchTasks)
  const records = computed(() => request.data.value?.records || [])
  const total = computed(() => request.data.value?.total || 0)
  function load() {
    const { status, ...params } = query
    return request.run({ ...params, ...(status ? { status } : {}) })
  }
  function search() {
    query.pageNum = 1
    return load()
  }
  function changePage(page) {
    query.pageNum = page
    return load()
  }
  return { query, records, total, loading: request.loading, error: request.error, load, search, changePage }
}
