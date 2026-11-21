import { computed, reactive, ref } from 'vue'
import { providerBatchImportModels, providerFetchModels } from '@/api/ai'
import { inferModelType } from './modelResourceUtils'

const typeOrder = ['chat', 'vision', 'video_understanding', 'audio_understanding', 'asr', 'tts', 'image_generation', 'video_generation', 'embedding', 'rerank']

export function useModelImport({ selectedProvider, modelTypeLabel, reload }) {
  let sequence = 0
  const fetchModelsModal = reactive({
    show: false,
    loading: false,
    importing: false,
    error: '',
    models: [],
    checked: {},
    modelTypes: {},
    providerId: null,
    providerName: '',
  })
  const fetchModelsModalSearch = ref('')
  const fetchModelsModalCategory = ref('all')

  const fetchModelsModalCategories = computed(() => {
    const present = new Set(fetchModelsModal.models.map(m => fetchModelsModal.modelTypes[m.id] || inferModelType(m.id)))
    return [
      { label: '全部', value: 'all' },
      ...typeOrder.filter(t => present.has(t)).map(t => ({ label: modelTypeLabel(t), value: t })),
    ]
  })

  const fetchModelsModalFilteredModels = computed(() => {
    const keyword = fetchModelsModalSearch.value.trim().toLowerCase()
    return fetchModelsModal.models.filter(m =>
      (fetchModelsModalCategory.value === 'all' || (fetchModelsModal.modelTypes[m.id] || inferModelType(m.id)) === fetchModelsModalCategory.value)
      && (!keyword || String(m.id || '').toLowerCase().includes(keyword)),
    )
  })

  const fetchModelsModalGroups = computed(() =>
    typeOrder
      .map(type => ({
        type,
        label: modelTypeLabel(type),
        models: fetchModelsModalFilteredModels.value.filter(m => (fetchModelsModal.modelTypes[m.id] || inferModelType(m.id)) === type),
      }))
      .filter(group => group.models.length),
  )

  const groupModels = type => fetchModelsModalGroups.value.find(group => group.type === type)?.models || []
  const isGroupChecked = type => groupModels(type).length > 0 && groupModels(type).every(m => fetchModelsModal.checked[m.id])
  const isGroupIndeterminate = (type) => {
    const count = groupModels(type).filter(m => fetchModelsModal.checked[m.id]).length
    return count > 0 && count < groupModels(type).length
  }

  function handleToggleGroup(type, checked) {
    for (const model of groupModels(type))
      fetchModelsModal.checked[model.id] = checked
  }

  function handleToggleModel(id, checked) {
    fetchModelsModal.checked[id] = checked
  }

  const fetchModelsModalVisibleCheckedCount = computed(() =>
    fetchModelsModalFilteredModels.value.filter(m => fetchModelsModal.checked[m.id]).length,
  )

  function dispose() {
    sequence++
    Object.assign(fetchModelsModal, {
      show: false,
      loading: false,
      importing: false,
      error: '',
      models: [],
      checked: {},
      modelTypes: {},
      providerId: null,
      providerName: '',
    })
    fetchModelsModalSearch.value = ''
    fetchModelsModalCategory.value = 'all'
  }

  function cancelModelImport() {
    if (fetchModelsModal.importing)
      return false
    dispose()
    return true
  }

  async function fetchPinnedModels() {
    if (!fetchModelsModal.show || !fetchModelsModal.providerId || fetchModelsModal.importing)
      return
    const requestId = ++sequence
    const providerId = fetchModelsModal.providerId
    Object.assign(fetchModelsModal, { loading: true, error: '', models: [], checked: {}, modelTypes: {} })
    try {
      const response = await providerFetchModels(providerId)
      if (requestId !== sequence || !fetchModelsModal.show)
        return
      if (response.code !== 200 || !Array.isArray(response.data))
        throw new Error(response.msg || '获取模型失败')
      fetchModelsModal.models = response.data.filter(model => model?.id)
      fetchModelsModal.checked = Object.fromEntries(fetchModelsModal.models.map(m => [m.id, true]))
      fetchModelsModal.modelTypes = Object.fromEntries(fetchModelsModal.models.map(m => [m.id, inferModelType(m.id)]))
    }
    catch (error) {
      if (requestId === sequence)
        fetchModelsModal.error = error.message || '获取模型失败'
    }
    finally {
      if (requestId === sequence)
        fetchModelsModal.loading = false
    }
  }

  async function handleFetchModels() {
    if (fetchModelsModal.importing || fetchModelsModal.loading)
      return
    if (!selectedProvider.value?.id) {
      window.$message?.warning('请先选择供应商连接')
      return
    }
    dispose()
    Object.assign(fetchModelsModal, {
      show: true,
      providerId: selectedProvider.value.id,
      providerName: selectedProvider.value.providerName,
    })
    return fetchPinnedModels()
  }

  async function handleImportModels() {
    if (!fetchModelsModal.show || fetchModelsModal.importing || fetchModelsModal.loading || fetchModelsModal.error)
      return
    const items = fetchModelsModalFilteredModels.value
      .filter(m => fetchModelsModal.checked[m.id])
      .map(m => ({ modelId: m.id, modelType: fetchModelsModal.modelTypes[m.id] || inferModelType(m.id) }))
    if (!items.length || !fetchModelsModal.providerId)
      return
    const expected = sequence
    const providerId = fetchModelsModal.providerId
    fetchModelsModal.importing = true
    try {
      const response = await providerBatchImportModels(providerId, items)
      if (expected !== sequence)
        return
      if (response.code !== 200)
        throw new Error(response.msg || '导入失败')
      window.$message?.success(`已向 ${fetchModelsModal.providerName} 导入 ${response.data ?? items.length} 个模型`)
      dispose()
      await reload()
    }
    catch (error) {
      if (expected === sequence)
        window.$message?.error(error.message || '导入失败，勾选内容已保留，可重试')
    }
    finally {
      if (expected === sequence)
        fetchModelsModal.importing = false
    }
  }

  return {
    fetchModelsModal,
    fetchModelsModalSearch,
    fetchModelsModalCategory,
    fetchModelsModalCategories,
    fetchModelsModalFilteredModels,
    fetchModelsModalGroups,
    groupModels,
    isGroupChecked,
    isGroupIndeterminate,
    handleToggleGroup,
    fetchModelsModalVisibleCheckedCount,
    handleFetchModels,
    retryFetchModels: fetchPinnedModels,
    cancelModelImport,
    handleToggleModel,
    handleImportModels,
    dispose,
  }
}
