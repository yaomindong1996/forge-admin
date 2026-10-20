import { computed, getCurrentScope, onScopeDispose, reactive, ref } from 'vue'
import { businessFlowModelBindingsBatch } from '@/api/business-app'
import flowApi from '@/api/flow'
import { extractBusinessBindingRows, mergeModelBusinessBindings } from '../utils/modelCatalog'

/** 仅列表持有查询/分页领域状态，卡片为一层展示组件，不经过设计器多层传递。 */
export function useFlowModelCatalog({ buildModelFormBinding, api = flowApi, queryBindings = businessFlowModelBindingsBatch }) {
  const queryParams = reactive({ modelName: '', category: null, status: null })
  const pagination = reactive({ page: 1, pageSize: 12, itemCount: 0 })
  const appliedQuery = ref({ ...queryParams })
  const dataSource = ref([])
  const loading = ref(false)
  const loadError = ref('')
  const hasLoaded = ref(false)
  const sortMode = ref(false)
  const sortSaving = ref(false)
  const draggingModelId = ref('')
  const hasAppliedFilters = computed(() => Object.values(appliedQuery.value).some(value => value !== null && value !== ''))
  const bindingLoadError = computed(() => dataSource.value.some(row => row.bindingLoadError))
  let requestVersion = 0
  let disposed = false
  let appliedPage = { page: 1, pageSize: 12 }
  let failedRequest = null

  async function enrichModelBusinessBindings(records) {
    if (!records.length)
      return []
    const keys = [...new Set(records.map(row => row.modelKey).filter(Boolean))]
    try {
      const response = keys.length ? await queryBindings(keys) : { code: 200, data: [] }
      if (response?.code !== 200)
        throw new Error('绑定查询失败')
      const grouped = new Map()
      for (const binding of extractBusinessBindingRows(response)) {
        const list = grouped.get(binding.flowModelKey) || []
        grouped.set(binding.flowModelKey, [...list, binding])
      }
      return records.map(row => ({
        ...row,
        businessBindings: mergeModelBusinessBindings(grouped.get(row.modelKey) || [], buildModelFormBinding(row)),
        bindingLoadError: false,
      }))
    }
    catch {
      // 查询失败不能误报“未绑定”；仍保留模型中明确配置的表单上下文供设计器使用。
      return records.map(row => ({ ...row, businessBindings: buildModelFormBinding(row), bindingLoadError: true }))
    }
  }

  async function fetchData(request = {}) {
    if (sortMode.value || disposed)
      return
    const version = ++requestVersion
    const query = request.query || (hasLoaded.value ? { ...appliedQuery.value } : { ...queryParams })
    const requestedPage = request.page || { page: pagination.page, pageSize: pagination.pageSize }
    loading.value = true
    loadError.value = ''
    try {
      const response = await api.getModelPage({
        ...query,
        pageNum: requestedPage.page,
        pageSize: requestedPage.pageSize,
      })
      if (version !== requestVersion || disposed)
        return
      if (response?.code !== 200)
        throw new Error('模型查询失败')
      const rows = await enrichModelBusinessBindings(response.data?.records || [])
      if (version !== requestVersion || disposed)
        return
      dataSource.value = rows
      pagination.itemCount = response.data?.total || 0
      Object.assign(pagination, requestedPage)
      appliedPage = requestedPage
      appliedQuery.value = query
      hasLoaded.value = true
      failedRequest = null
    }
    catch {
      if (version !== requestVersion || disposed)
        return
      loadError.value = hasLoaded.value
        ? '本次查询失败，当前保留上一次查询结果，请重试。'
        : '加载流程模型失败，请重试。'
      Object.assign(pagination, appliedPage)
      failedRequest = { query, page: requestedPage }
    }
    finally {
      if (version === requestVersion && !disposed)
        loading.value = false
    }
  }

  function handleSearch() {
    if (sortMode.value)
      return
    pagination.page = 1
    return fetchData({ query: { ...queryParams, modelName: queryParams.modelName.trim() } })
  }

  function handleReset() {
    if (sortMode.value)
      return
    Object.assign(queryParams, { modelName: '', category: null, status: null })
    return handleSearch()
  }

  function handlePageSizeChange(value) {
    if (sortMode.value)
      return
    pagination.pageSize = value
    pagination.page = 1
    return fetchData()
  }

  function handleFilter(value) {
    queryParams.status = value === 'all' ? null : value
    return handleSearch()
  }

  function handleDragStart(row) {
    if (sortMode.value)
      draggingModelId.value = String(row.id)
  }

  function handleDrop(target) {
    if (!sortMode.value || sortSaving.value)
      return
    const from = dataSource.value.findIndex(row => String(row.id) === draggingModelId.value)
    const to = dataSource.value.findIndex(row => String(row.id) === String(target.id))
    if (from < 0 || to < 0 || from === to)
      return
    const next = [...dataSource.value]
    next.splice(to, 0, next.splice(from, 1)[0])
    dataSource.value = next
    draggingModelId.value = ''
  }

  function cancelModelOrder() {
    if (sortSaving.value)
      return
    sortMode.value = false
    draggingModelId.value = ''
    return fetchData()
  }

  async function saveModelOrder() {
    if (sortSaving.value || !sortMode.value || !dataSource.value.length)
      return
    sortSaving.value = true
    try {
      const base = (pagination.page - 1) * pagination.pageSize
      const response = await api.sortModels({ items: dataSource.value.map((row, index) => ({
        modelId: String(row.id),
        sortOrder: base + index,
      })) })
      if (response?.code !== 200)
        throw new Error('排序保存失败')
      window.$message?.success('本页流程排序已保存')
      sortMode.value = false
      await fetchData()
    }
    catch {
      window.$message?.error('排序保存失败，请重试或取消排序')
    }
    finally {
      sortSaving.value = false
    }
  }

  if (getCurrentScope()) {
    onScopeDispose(() => {
      disposed = true
      requestVersion += 1
    })
  }
  return {
    queryParams,
    pagination,
    appliedQuery,
    dataSource,
    loading,
    loadError,
    hasLoaded,
    hasAppliedFilters,
    bindingLoadError,
    sortMode,
    sortSaving,
    draggingModelId,
    fetchData,
    fetchModelPage: fetchData,
    retryQuery: () => fetchData(failedRequest || {}),
    enrichModelBusinessBindings,
    handleSearch,
    handleReset,
    handleStatusSelect: handleSearch,
    handlePageSizeChange,
    handleFilter,
    handleDragStart,
    handleDrop,
    cancelModelOrder,
    saveModelOrder,
  }
}
