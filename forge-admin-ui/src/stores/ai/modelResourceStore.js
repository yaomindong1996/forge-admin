import { defineStore } from 'pinia'
import { computed, reactive, ref, shallowRef, watch } from 'vue'
import {
  modelPage as fetchModelPage,
  providerPage as fetchProviderPage,
  providerTemplates as fetchProviderTemplates,
  modelAdd,
  modelDelete,
  modelGetById,
  modelTest,
  modelUpdate,
  providerAdd,
  providerDelete,
  providerGetById,
  providerSetDefault,
  providerUpdate,
} from '@/api/ai'
import { createModelForm, createProviderForm } from '@/composables/ai/modelResourceUtils'
import { matchesProviderTemplate, normalizeProviderTemplates } from '@/composables/ai/providerCatalogUtils'
import { useModelFormSession } from '@/composables/ai/useModelFormSession'
import { useModelImport } from '@/composables/ai/useModelImport'
import { useDict } from '@/composables/useDict'
import { useAuthStore, useUserStore } from '@/store'

export const useModelResourceStore = defineStore('useModelResourceStore', () => {
  const auth = useAuthStore()
  const user = useUserStore()
  let epoch = 0
  let detailRequestId = 0
  let templateRequestId = 0
  let providerRequestId = 0
  let modelRequestId = 0
  let choiceRequestId = 0

  const railMode = ref('connections')
  const compactPanel = ref('models')
  const providerNames = ref({})
  const providerChoices = ref([])
  const providerError = ref('')
  const modelError = ref('')
  const providerTemplates = ref([])
  const templateError = ref('')
  const templateLoading = ref(false)
  const selectedTemplate = ref(null)
  const catalogSearch = ref('')
  const formTemplateKey = ref(null)

  const { dict } = useDict(
    'ai_provider_type',
    'ai_provider_adapter_type',
    'ai_model_type',
    'ai_status',
    'ai_is_default',
    'ai_model_capability_type',
  )

  const providerTypeOptions = computed(() => dict.value.ai_provider_type || [])
  const providerAdapterOptions = computed(() => {
    const raw = (dict.value.ai_provider_adapter_type || []).filter(item => item.value !== 'dashscope_native')
    return raw.length ? raw : [{ label: 'OpenAI 兼容协议', value: 'openai_compatible' }]
  })
  const modelTypeOptions = computed(() => dict.value.ai_model_type || [])
  const statusOptions = computed(() => dict.value.ai_status || [])
  const isDefaultOptions = computed(() => dict.value.ai_is_default || [])
  const modelCapabilityOptions = computed(() => dict.value.ai_model_capability_type || [])

  const MODEL_TYPE_ORDER = ['chat', 'vision', 'video_understanding', 'audio_understanding', 'asr', 'tts', 'image_generation', 'video_generation', 'embedding', 'rerank']

  function modelTypeLabel(code) {
    return modelTypeOptions.value.find(item => item.value === code)?.label || code
  }

  const modelTypeOverrideOptions = computed(() =>
    modelTypeOptions.value.filter(o => MODEL_TYPE_ORDER.includes(o.value)),
  )

  const modalCardStyle = { maxWidth: '860px', width: 'calc(100vw - 32px)' }
  const fetchModelsModalStyle = { maxWidth: '860px', width: 'calc(100vw - 32px)', height: 'min(640px, calc(100vh - 80px))' }
  const fetchModelsModalContentStyle = { display: 'flex', flexDirection: 'column', minHeight: '0', overflow: 'hidden' }

  const providerSearch = reactive({ name: '', type: null, status: null })
  const providerList = ref([])
  const providerLoading = ref(false)
  const providerPagination = reactive({ pageNum: 1, pageSize: 10, itemCount: 0 })
  const selectedProvider = ref(null)
  const modelList = ref([])
  const modelLoading = ref(false)
  const defaultModelUpdatingId = ref(null)
  const providerDefaultUpdatingId = ref(null)
  const modelPagination = reactive({ pageNum: 1, pageSize: 12, itemCount: 0 })
  const modelSearch = reactive({ modelType: null, modelName: '' })
  const appliedModelSearch = reactive({ modelType: null, modelName: '' })
  const modelChecks = reactive({})
  const providerFormRef = shallowRef(null)
  const modelFormRef = shallowRef(null)

  const providerSelectOptions = computed(() => [
    ...new Map(
      [...providerChoices.value, ...providerList.value, ...(selectedProvider.value ? [selectedProvider.value] : [])]
        .map(p => [String(p.id), { label: p.providerName, value: p.id }]),
    ).values(),
  ])

  const featuredTemplates = computed(() =>
    providerTemplates.value.filter(t =>
      t.featured && `${t.name} ${t.providerType} ${t.providerTypeAliases.join(' ')}`.toLowerCase().includes(catalogSearch.value.trim().toLowerCase()),
    ),
  )
  const templateSelectOptions = computed(() => providerTemplates.value.map(t => ({ label: t.name, value: t.templateKey })))
  const connectionOptions = computed(() => providerList.value.map(p => ({ label: p.providerName, value: p.id })))

  const modelRowActionLoading = reactive({})
  const isModelRowActionLoading = (id, type) => modelRowActionLoading[id] === type

  const uploadPrefix = import.meta.env.VITE_API_BASEURL || '/api'
  const uploadHeaders = computed(() => {
    const token = localStorage.getItem('token') || ''
    return { Authorization: `Bearer ${token}` }
  })

  const providerEditing = ref(false)
  const providerModal = reactive({
    show: false,
    isEdit: false,
    saving: false,
    form: createProviderForm(),
  })

  const formTemplate = computed(() =>
    providerTemplates.value.find(t => t.templateKey === formTemplateKey.value)
    || providerTemplates.value.find(t => matchesProviderTemplate(t, providerModal.form.providerType) && t.adapterCode === providerModal.form.adapterCode)
    || providerTemplates.value.find(t => t.featured && matchesProviderTemplate(t, providerModal.form.providerType)),
  )

  const providerApiKeyPlaceholder = computed(() => providerModal.isEdit ? '留空表示不修改' : '请输入 API Key')
  const PROVIDER_DEFAULT_BASE_URL = computed(() =>
    Object.fromEntries(providerTemplates.value.filter(t => t.featured && t.baseUrl).map(t => [t.providerType, t.baseUrl])),
  )
  const KNOWN_PROVIDER_DEFAULT_URLS = computed(() =>
    new Set(Object.values(PROVIDER_DEFAULT_BASE_URL.value).flatMap(url => [url, `${url}/`])),
  )

  function resolveDefaultBaseUrl(_adapterCode, providerType) {
    return PROVIDER_DEFAULT_BASE_URL.value[providerType] || ''
  }

  watch(
    () => [providerModal.form.adapterCode, providerModal.form.providerType],
    ([adapterCode, providerType]) => {
      const template = providerTemplates.value.find(t => t.templateKey === formTemplateKey.value)
      if (template && (!matchesProviderTemplate(template, providerType) || template.adapterCode !== adapterCode))
        formTemplateKey.value = null
      const resolved = resolveDefaultBaseUrl(adapterCode, providerType)
      if (!resolved)
        return
      const currentBaseUrl = providerModal.form.baseUrl?.trim() || ''
      if (!currentBaseUrl || (KNOWN_PROVIDER_DEFAULT_URLS.value.has(currentBaseUrl) && currentBaseUrl !== resolved))
        providerModal.form.baseUrl = resolved
    },
  )

  const modelModal = reactive({
    show: false,
    isEdit: false,
    saving: false,
    form: createModelForm(),
  })

  const forms = useModelFormSession({ providerModal, modelModal })
  const { providerAdvanced, modelAdvanced, requestCloseProvider, requestCloseModel } = forms

  const importer = useModelImport({
    selectedProvider,
    modelTypeLabel,
    reload: () => Promise.all([loadModels(), loadProviders()]),
  })
  const {
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
    retryFetchModels,
    cancelModelImport,
    handleToggleModel,
    handleImportModels,
  } = importer

  function handleLogoUploadFinish({ event }) {
    try {
      const res = JSON.parse(event.target.response)
      if (res.code === 200 && res.data)
        providerModal.form.logo = res.data.fileId || res.data.id || res.data
      else
        window.$message.error(res.msg || '上传失败')
    }
    catch {
      window.$message.error('上传失败')
    }
    return false
  }

  function handleIconUploadFinish({ event }) {
    try {
      const res = JSON.parse(event.target.response)
      if (res.code === 200 && res.data)
        modelModal.form.icon = res.data.fileId || res.data.id || res.data
      else
        window.$message.error(res.msg || '上传失败')
    }
    catch {
      window.$message.error('上传失败')
    }
    return false
  }

  const providerRules = {
    providerName: [{ required: true, message: '请输入供应商名称', trigger: 'blur' }],
    providerType: [{ required: true, message: '请选择类型', trigger: 'change' }],
    adapterCode: [{ required: true, message: '请选择连接协议', trigger: 'change' }],
    baseUrl: [{
      trigger: 'blur',
      validator(_rule, value) {
        if (!value?.trim() && !resolveDefaultBaseUrl(providerModal.form.adapterCode, providerModal.form.providerType))
          return new Error('请填写此连接的接口地址')
        return true
      },
    }],
    apiKey: [{
      trigger: 'blur',
      validator(_rule, value) {
        if (!providerModal.isEdit && !value?.trim())
          return new Error('请输入 API Key')
        return true
      },
    }],
  }

  const modelRules = {
    providerId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
    modelType: [{ required: true, message: '请选择模型类型', trigger: 'change' }],
    modelId: [{ required: true, message: '请输入模型标识', trigger: 'blur' }],
    modelName: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
  }

  async function handleTestModel(row) {
    const expected = epoch
    if (modelRowActionLoading[row.id])
      return
    modelRowActionLoading[row.id] = 'test'
    try {
      const res = await modelTest(row.id)
      if (epoch !== expected)
        return
      modelChecks[row.id] = {
        ok: res.code === 200,
        message: res.code === 200 ? res.data || '连接成功' : res.msg || '模型连接失败',
        checkedAt: new Date().toLocaleTimeString('zh-CN', { hour12: false }),
      }
      if (res.code === 200)
        window.$message.success(modelChecks[row.id].message)
      else
        window.$message.error(modelChecks[row.id].message)
    }
    catch (error) {
      if (epoch === expected) {
        modelChecks[row.id] = {
          ok: false,
          message: error.message || '模型连接失败',
          checkedAt: new Date().toLocaleTimeString('zh-CN', { hour12: false }),
        }
        window.$message.error(modelChecks[row.id].message)
      }
    }
    finally {
      if (epoch === expected)
        delete modelRowActionLoading[row.id]
    }
  }

  function searchConnections() {
    providerPagination.pageNum = 1
    return loadProviders()
  }

  function handleResetProviderSearch() {
    catalogSearch.value = ''
    providerSearch.name = ''
    providerSearch.type = selectedTemplate.value?.providerType || null
    providerSearch.status = null
    providerPagination.pageNum = 1
    loadProviders()
  }

  function handleProviderPageChange(page) {
    providerPagination.pageNum = page
    loadProviders()
  }

  async function loadProviders() {
    const requestId = ++providerRequestId
    providerLoading.value = true
    providerError.value = ''
    try {
      const res = await fetchProviderPage({
        pageNum: providerPagination.pageNum,
        pageSize: providerPagination.pageSize,
        providerName: providerSearch.name.trim() || undefined,
        providerType: providerSearch.type || undefined,
        status: providerSearch.status || undefined,
      })
      if (requestId !== providerRequestId)
        return
      if (res.code !== 200 || !res.data)
        throw new Error(res.msg || '加载供应商失败')
      providerList.value = res.data.records || []
      for (const provider of providerList.value)
        providerNames.value[String(provider.id)] = provider.providerName
      providerPagination.itemCount = Number(res.data.total || 0)
      if (selectedProvider.value) {
        const current = providerList.value.find(p => String(p.id) === String(selectedProvider.value.id))
        if (current)
          selectedProvider.value = current
      }
      if (selectedTemplate.value && !providerList.value.some(p => String(p.id) === String(selectedProvider.value?.id))) {
        selectedProvider.value = providerList.value[0] || null
        if (selectedProvider.value)
          await loadModels()
      }
    }
    catch (error) {
      if (requestId === providerRequestId)
        providerError.value = error.message || '加载供应商失败'
    }
    finally {
      if (requestId === providerRequestId)
        providerLoading.value = false
    }
  }

  async function loadModels(filters = modelSearch) {
    const requestId = ++modelRequestId
    modelLoading.value = true
    modelError.value = ''
    modelList.value = []
    try {
      const query = {
        pageNum: modelPagination.pageNum,
        pageSize: modelPagination.pageSize,
        providerId: selectedProvider.value?.id || undefined,
        modelName: filters.modelName.trim() || undefined,
        modelType: filters.modelType || undefined,
      }
      const res = await fetchModelPage(query)
      if (requestId !== modelRequestId)
        return
      if (res.code !== 200 || !res.data)
        throw new Error(res.msg || '加载模型失败')
      modelList.value = res.data.records || []
      Object.assign(appliedModelSearch, { modelType: query.modelType || null, modelName: query.modelName || '' })
      modelPagination.itemCount = Number(res.data.total || 0)
      await hydrateProviderNames(modelList.value, requestId)
    }
    catch (error) {
      if (requestId === modelRequestId) {
        modelError.value = error.message || '加载模型失败'
        modelPagination.itemCount = 0
      }
    }
    finally {
      if (requestId === modelRequestId)
        modelLoading.value = false
    }
  }

  function handleSelectProvider(row) {
    compactPanel.value = 'models'
    if (selectedTemplate.value && !matchesProviderTemplate(selectedTemplate.value, row.providerType))
      selectedTemplate.value = null
    if (selectedProvider.value?.id === row.id)
      return
    selectedProvider.value = row
    modelPagination.pageNum = 1
    loadModels()
  }

  function clearSelectedProvider() {
    compactPanel.value = 'models'
    selectedTemplate.value = null
    catalogSearch.value = ''
    providerSearch.type = null
    providerSearch.name = ''
    providerPagination.pageNum = 1
    selectedProvider.value = null
    providerList.value = []
    modelPagination.pageNum = 1
    return Promise.allSettled([loadProviders(), loadModels()])
  }

  function handleModelPageChange(page) {
    modelPagination.pageNum = page
    loadModels()
  }

  function handleModelPageSizeChange(pageSize) {
    modelPagination.pageSize = pageSize
    modelPagination.pageNum = 1
    loadModels()
  }

  function handleModelSearch() {
    modelPagination.pageNum = 1
    loadModels()
  }

  function handleAddProvider(template = null) {
    detailRequestId++
    providerEditing.value = false
    providerAdvanced.value = false
    providerModal.isEdit = false
    providerModal.form = createProviderForm()
    // 若当前租户还没有任何默认供应商，新建时默认勾选“设为默认供应商”
    if (!providerList.value.some(p => p.isDefault === '1'))
      providerModal.form.isDefault = '1'
    formTemplateKey.value = null
    if (template?.templateKey)
      applyProviderTemplate(template.templateKey)
    providerModal.show = true
  }

  function applyProviderTemplate(key) {
    if (providerModal.isEdit)
      return
    const template = providerTemplates.value.find(t => t.templateKey === key)
    if (!template)
      return
    formTemplateKey.value = key
    Object.assign(providerModal.form, {
      providerName: template.name,
      providerType: template.providerType,
      adapterCode: template.adapterCode || 'openai_compatible',
      baseUrl: template.baseUrl || '',
      apiKey: '',
      logo: '',
    })
  }

  async function loadTemplates() {
    const requestId = ++templateRequestId
    templateLoading.value = true
    templateError.value = ''
    try {
      const res = await fetchProviderTemplates()
      if (requestId !== templateRequestId)
        return
      if (res.code !== 200 || !Array.isArray(res.data))
        throw new Error(res.msg || '加载供应商预设失败')
      providerTemplates.value = normalizeProviderTemplates(res.data)
      if (selectedTemplate.value)
        selectedTemplate.value = providerTemplates.value.find(t => t.templateKey === selectedTemplate.value.templateKey) || selectedTemplate.value
    }
    catch (error) {
      if (requestId === templateRequestId)
        templateError.value = error.message || '加载供应商预设失败'
    }
    finally {
      if (requestId === templateRequestId)
        templateLoading.value = false
    }
  }

  async function handleSelectTemplate(template) {
    compactPanel.value = 'models'
    selectedTemplate.value = template
    selectedProvider.value = null
    providerList.value = []
    catalogSearch.value = ''
    providerSearch.name = ''
    providerSearch.type = template.providerType
    providerPagination.pageNum = 1
    modelRequestId++
    modelList.value = []
    modelPagination.itemCount = 0
    modelPagination.pageNum = 1
    modelLoading.value = false
    modelError.value = ''
    await loadProviders()
  }

  function selectConnection(id) {
    const provider = providerList.value.find(p => String(p.id) === String(id))
    if (provider)
      handleSelectProvider(provider)
  }

  async function handleEditProvider(row) {
    if (providerEditing.value)
      return
    providerEditing.value = true
    const expected = epoch
    const requestId = ++detailRequestId
    try {
      const res = await providerGetById(row.id)
      if (epoch !== expected || requestId !== detailRequestId)
        return
      if (res.code === 200 && res.data) {
        formTemplateKey.value = null
        providerAdvanced.value = false
        providerModal.isEdit = true
        providerModal.form = { ...createProviderForm(), ...res.data, apiKey: '' }
        providerModal.show = true
      }
      else {
        window.$message.error(res.msg || '读取供应商失败')
      }
    }
    catch (e) {
      if (epoch === expected && requestId === detailRequestId)
        window.$message.error(e.message || '读取供应商失败')
    }
    finally {
      if (epoch === expected && requestId === detailRequestId)
        providerEditing.value = false
    }
  }

  async function applyRequestedDefaultAfterSave(payload, isEdit) {
    if (payload.isDefault !== '1')
      return
    if (isEdit && payload.id) {
      await providerSetDefault(payload.id)
      return
    }
    const latest = await fetchProviderPage({
      pageNum: 1,
      pageSize: 5,
      providerName: payload.providerName,
      providerType: payload.providerType,
    })
    const created = latest?.code === 200
      ? (latest.data?.records || []).find(item => item.providerName === payload.providerName)
      : null
    if (created?.id)
      await providerSetDefault(created.id)
  }

  async function handleSaveProvider() {
    if (providerModal.saving)
      return
    const expected = epoch
    try {
      await providerFormRef.value?.validate()
    }
    catch {
      return
    }
    if (epoch !== expected || !providerModal.show)
      return
    providerModal.saving = true
    try {
      const isEdit = providerModal.isEdit
      const payload = { ...providerModal.form }
      const wantDefault = payload.isDefault === '1'
      if (isEdit && !payload.apiKey?.trim())
        delete payload.apiKey
      const res = isEdit ? await providerUpdate(payload) : await providerAdd(payload)
      if (epoch !== expected)
        return
      if (res.code !== 200) {
        window.$message.error(res.msg || '操作失败')
        return
      }
      if (wantDefault) {
        await applyRequestedDefaultAfterSave(payload, isEdit).catch(() => {})
      }
      window.$message.success(isEdit ? '更新成功' : '新增成功')
      forms.closeProvider()
      await loadTemplates()
      if (epoch !== expected)
        return
      const template = providerTemplates.value.find(t => t.featured && matchesProviderTemplate(t, payload.providerType))
      if (!isEdit && template)
        await handleSelectTemplate(template)
      else
        await loadProviders()
    }
    catch (e) {
      if (epoch === expected)
        window.$message.error(e.message || '操作失败')
    }
    finally {
      if (epoch === expected)
        providerModal.saving = false
    }
  }

  async function handleDeleteProvider(id) {
    try {
      const res = await providerDelete(id)
      if (res.code === 200) {
        window.$message.success('删除成功')
        if (selectedProvider.value?.id === id) {
          selectedProvider.value = null
          modelRequestId++
          modelList.value = []
          modelPagination.itemCount = 0
        }
        await Promise.allSettled([loadProviders(), loadTemplates()])
        if (!selectedTemplate.value)
          await loadModels()
      }
      else {
        window.$message.error(res.msg || '删除失败')
      }
    }
    catch (e) {
      window.$message.error(e.message || '删除失败')
    }
  }

  async function handleSetDefault(row) {
    if (!row?.id || providerDefaultUpdatingId.value !== null)
      return
    providerDefaultUpdatingId.value = row.id
    try {
      const res = await providerSetDefault(row.id)
      if (res.code === 200) {
        window.$message.success(`已将「${row.providerName}」设为默认供应商`)
        await loadProviders()
      }
      else {
        window.$message.error(res.msg || '设置默认供应商失败')
      }
    }
    catch (e) {
      window.$message.error(e.message || '设置默认供应商失败')
    }
    finally {
      providerDefaultUpdatingId.value = null
    }
  }

  function handleAddModel(provider = selectedProvider.value) {
    detailRequestId++
    providerEditing.value = false
    modelAdvanced.value = false
    modelModal.isEdit = false
    modelModal.form = createModelForm(provider?.id || null)
    modelModal.show = true
    searchProviderChoices('')
  }

  async function handleEditModel(row) {
    if (modelRowActionLoading[row.id])
      return
    modelRowActionLoading[row.id] = 'edit'
    const expected = epoch
    const requestId = ++detailRequestId
    try {
      const res = await modelGetById(row.id)
      if (epoch !== expected || requestId !== detailRequestId)
        return
      if (res.code === 200 && res.data) {
        modelAdvanced.value = false
        modelModal.isEdit = true
        modelModal.form = { ...res.data }
        modelModal.show = true
      }
      else {
        window.$message.error(res.msg || '读取模型失败')
      }
    }
    catch (e) {
      if (epoch === expected && requestId === detailRequestId)
        window.$message.error(e.message || '读取模型失败')
    }
    finally {
      if (epoch === expected)
        delete modelRowActionLoading[row.id]
    }
  }

  async function handleSaveModel() {
    if (modelModal.saving)
      return
    const expected = epoch
    try {
      await modelFormRef.value?.validate()
    }
    catch {
      return
    }
    if (epoch !== expected || !modelModal.show)
      return
    modelModal.saving = true
    try {
      const res = modelModal.isEdit ? await modelUpdate(modelModal.form) : await modelAdd(modelModal.form)
      if (epoch !== expected)
        return
      if (res.code === 200) {
        window.$message.success(modelModal.isEdit ? '更新成功' : '新增成功')
        forms.closeModel()
        await Promise.all([loadModels(), loadProviders()])
      }
      else {
        window.$message.error(res.msg || '操作失败')
      }
    }
    catch (e) {
      if (epoch === expected)
        window.$message.error(e.message || '操作失败')
    }
    finally {
      if (epoch === expected)
        modelModal.saving = false
    }
  }

  async function handleDeleteModel(row) {
    if (modelRowActionLoading[row.id])
      return
    modelRowActionLoading[row.id] = 'delete'
    try {
      const res = await modelDelete(row.id)
      if (res.code === 200) {
        window.$message.success('删除成功')
        if (modelList.value.length === 1 && modelPagination.pageNum > 1)
          modelPagination.pageNum -= 1
        await Promise.all([loadModels(), loadProviders()])
      }
      else {
        window.$message.error(res.msg || '删除失败')
      }
    }
    catch (e) {
      window.$message.error(e.message || '删除失败')
    }
    finally {
      delete modelRowActionLoading[row.id]
    }
  }

  async function handleDefaultModelChange(row, value) {
    if (!value) {
      window.$message.warning('请直接将其他模型设为默认，供应商必须保留一个默认模型')
      return
    }
    if (row.isDefault === '1' || defaultModelUpdatingId.value !== null)
      return
    defaultModelUpdatingId.value = row.id
    try {
      const detail = await modelGetById(row.id)
      if (detail.code !== 200 || !detail.data) {
        window.$message.error(detail.msg || '读取模型失败')
        return
      }
      const res = await modelUpdate({ ...detail.data, isDefault: '1' })
      if (res.code === 200) {
        window.$message.success('默认模型已更新')
        await Promise.all([loadModels(), loadProviders()])
      }
      else {
        window.$message.error(res.msg || '设置默认模型失败')
      }
    }
    catch (e) {
      window.$message.error(e.message || '设置默认模型失败')
    }
    finally {
      defaultModelUpdatingId.value = null
    }
  }

  async function hydrateProviderNames(models, requestId) {
    const missing = [...new Set(models.map(m => String(m.providerId)).filter(id => id && !providerNames.value[id]))]
    await Promise.allSettled(missing.map(async (id) => {
      const res = await providerGetById(id)
      if (requestId === modelRequestId && res.code === 200 && res.data)
        providerNames.value[id] = res.data.providerName
    }))
  }

  async function searchProviderChoices(keyword) {
    const id = ++choiceRequestId
    try {
      const res = await fetchProviderPage({ pageNum: 1, pageSize: 30, providerName: keyword || undefined })
      if (id === choiceRequestId && res.code === 200)
        providerChoices.value = res.data?.records || []
    }
    catch {}
  }

  function resetModelSearch() {
    modelSearch.modelName = ''
    modelSearch.modelType = null
    return handleModelSearch()
  }

  async function initialize() {
    await Promise.allSettled([loadTemplates(), loadProviders(), ...(!selectedTemplate.value ? [loadModels()] : [])])
  }

  function dispose() {
    epoch++
    detailRequestId++
    providerEditing.value = false
    providerModal.saving = false
    modelModal.saving = false
    importer.dispose()
    forms.dispose()
    modelLoading.value = false
    providerLoading.value = false
    modelRequestId++
    providerRequestId++
    choiceRequestId++
    templateRequestId++
    templateLoading.value = false
    providerFormRef.value = null
    modelFormRef.value = null
    providerModal.show = false
    providerModal.form.apiKey = ''
    modelModal.show = false
  }

  function refreshModels() {
    return Promise.allSettled([
      loadProviders(),
      ...(!selectedTemplate.value || selectedProvider.value ? [loadModels(appliedModelSearch)] : []),
    ])
  }

  watch(() => [auth.accessToken, user.userId, user.tenantId, user.userInfo?.activeOrgId], () => {
    dispose()
    providerNames.value = {}
    providerChoices.value = []
    providerTemplates.value = []
    providerList.value = []
    modelList.value = []
    selectedTemplate.value = null
    selectedProvider.value = null
    railMode.value = 'connections'
    compactPanel.value = 'models'
    catalogSearch.value = ''
    Object.assign(providerSearch, { name: '', type: null, status: null })
    Object.assign(modelSearch, { modelName: '', modelType: null })
    Object.assign(appliedModelSearch, { modelName: '', modelType: null })
    providerPagination.pageNum = modelPagination.pageNum = 1
    providerPagination.itemCount = modelPagination.itemCount = 0
    providerError.value = modelError.value = templateError.value = ''
    for (const key of Object.keys(modelChecks)) delete modelChecks[key]
    for (const key of Object.keys(modelRowActionLoading)) delete modelRowActionLoading[key]
  }, { flush: 'sync' })

  return {
    compactPanel,
    searchConnections,
    railMode,
    appliedModelSearch,
    modelChecks,
    refreshModels,
    providerAdvanced,
    modelAdvanced,
    requestCloseProvider,
    requestCloseModel,
    confirmNavigation: forms.confirmNavigation,
    cancelModelImport,
    retryFetchModels,
    providerTemplates,
    templateError,
    templateLoading,
    selectedTemplate,
    catalogSearch,
    formTemplateKey,
    featuredTemplates,
    templateSelectOptions,
    formTemplate,
    connectionOptions,
    loadTemplates,
    handleSelectTemplate,
    applyProviderTemplate,
    selectConnection,
    dict,
    providerTypeOptions,
    providerAdapterOptions,
    modelTypeOptions,
    statusOptions,
    isDefaultOptions,
    modelCapabilityOptions,
    MODEL_TYPE_ORDER,
    modelTypeLabel,
    modelTypeOverrideOptions,
    modalCardStyle,
    fetchModelsModalStyle,
    fetchModelsModalContentStyle,
    providerSearch,
    providerList,
    providerLoading,
    providerPagination,
    selectedProvider,
    modelList,
    modelLoading,
    defaultModelUpdatingId,
    providerDefaultUpdatingId,
    modelPagination,
    modelSearch,
    providerFormRef,
    modelFormRef,
    providerSelectOptions,
    modelRowActionLoading,
    isModelRowActionLoading,
    uploadPrefix,
    uploadHeaders,
    providerEditing,
    providerModal,
    providerApiKeyPlaceholder,
    modelModal,
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
    handleLogoUploadFinish,
    handleIconUploadFinish,
    providerRules,
    modelRules,
    handleTestModel,
    handleResetProviderSearch,
    handleProviderPageChange,
    loadProviders,
    loadModels,
    handleSelectProvider,
    clearSelectedProvider,
    handleModelPageChange,
    handleModelPageSizeChange,
    handleModelSearch,
    handleAddProvider,
    handleEditProvider,
    handleSaveProvider,
    handleDeleteProvider,
    handleSetDefault,
    handleFetchModels,
    handleToggleModel,
    handleImportModels,
    handleAddModel,
    handleEditModel,
    handleSaveModel,
    handleDeleteModel,
    handleDefaultModelChange,
    providerChoices,
    providerError,
    modelError,
    providerNames,
    searchProviderChoices,
    resetModelSearch,
    initialize,
    dispose,
  }
})
