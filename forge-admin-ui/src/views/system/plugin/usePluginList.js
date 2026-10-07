import { computed, reactive } from 'vue'
import { listRuntimePlugins } from '@/api/system/plugin'
import { useLatestPluginRequest } from './useLatestPluginRequest'

async function fetchList(params) {
  const response = await listRuntimePlugins(params)
  if (response?.code !== 200 || !Array.isArray(response.data?.records))
    throw new Error(response?.message || '插件清单响应无效')
  return response.data
}

export function usePluginList() {
  const query = reactive({ keyword: '', origin: null, pageNum: 1, pageSize: 15 })
  const request = useLatestPluginRequest(fetchList)
  const records = computed(() => request.data.value?.records || [])
  const total = computed(() => request.data.value?.total || 0)
  const metadata = computed(() => request.data.value
    ? { coreVersion: request.data.value.coreVersion, edition: request.data.value.edition }
    : {})

  function load() {
    const { origin, ...params } = query
    return request.run({ ...params, ...(origin ? { origin } : {}) })
  }

  function search() {
    query.pageNum = 1
    return load()
  }

  function reset() {
    query.keyword = ''
    query.origin = null
    return search()
  }

  function changePage(page) {
    query.pageNum = page
    return load()
  }

  function changeSize(size) {
    query.pageSize = size
    return search()
  }

  const { loading, error } = request
  return { query, records, total, metadata, loading, error, load, search, reset, changePage, changeSize }
}
