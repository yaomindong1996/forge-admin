/** provider-model.vue setup part 1. */
import { NButton, NPopconfirm, NSwitch, NTag } from 'naive-ui'
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import {
  modelPage as fetchModelPage,
  providerPage as fetchProviderPage,
  modelAdd,
  modelDelete,
  modelGetById,
  modelTest,
  modelUpdate,
  providerAdd,
  providerBatchImportModels,
  providerDelete,
  providerFetchModels,
  providerGetById,
  providerSetDefault,
  providerUpdate,
} from '@/api/ai'
import AuthImage from '@/components/common/AuthImage.vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
export function applyProviderModelPart1() {
  const __impl = {}
  const mut = {}

  const { dict } = useDict('ai_provider_type', 'ai_provider_adapter_type', 'ai_model_type', 'ai_status', 'ai_is_default', 'ai_model_capability_type')

  const providerTypeOptions = computed(() => dict.value.ai_provider_type || [])
  const providerAdapterOptions = computed(() => dict.value.ai_provider_adapter_type || [])
  const modelTypeOptions = computed(() => dict.value.ai_model_type || [])
  const statusOptions = computed(() => dict.value.ai_status || [])
  const isDefaultOptions = computed(() => dict.value.ai_is_default || [])
  const modelCapabilityOptions = computed(() => dict.value.ai_model_capability_type || [])

  const MODEL_TYPE_LABEL = {
    chat: '对话',
    vision: '视觉理解',
    video_understanding: '视频理解',
    audio_understanding: '音频理解',
    asr: '语音识别',
    tts: '语音合成',
    image_generation: '图像生成',
    video_generation: '视频生成',
    embedding: '向量化',
    rerank: '重排',
  }
  const MODEL_TYPE_ORDER = ['chat', 'vision', 'video_understanding', 'audio_understanding', 'asr', 'tts', 'image_generation', 'video_generation', 'embedding', 'rerank']
  const MODEL_TYPE_TAG_COLOR = {
    chat: 'success',
    vision: 'primary',
    video_understanding: 'error',
    audio_understanding: 'info',
    asr: 'default',
    tts: 'info',
    image_generation: 'warning',
    video_generation: 'error',
    embedding: 'info',
    rerank: 'warning',
  }

  function modelTypeLabel(code) {
    return MODEL_TYPE_LABEL[code] || code
  }
  function modelTypeTagColor(code) {
    return MODEL_TYPE_TAG_COLOR[code] || 'default'
  }

  // 类型覆盖下拉：只展示规范化的能力分类（过滤字典中存量的 image/audio 宽泛值）
  const modelTypeOverrideOptions = computed(() =>
    modelTypeOptions.value.filter(o => MODEL_TYPE_ORDER.includes(o.value)))

  /**
   * 根据模型标识启发式推断模型类型（与后端 AiModelType.inferFromModelId 逻辑一致）。
   * @param {string} modelId 模型标识
   * @returns {string} 推断的模型类型 code
   */
  function inferModelType(modelId) {
    if (!modelId)
      return 'chat'
    const lower = modelId.toLowerCase()
    if (lower.includes('embedding') || lower.includes('embed')) return 'embedding'
    if (lower.includes('rerank') || lower.includes('re-rank') || lower.includes('cross-encoder')) return 'rerank'
    if (lower.includes('t2v') || lower.includes('i2v')
      || lower.includes('video-gen') || lower.includes('videogen')
      || lower.includes('maku')) return 'video_generation'
    if (lower.includes('t2i') || lower.includes('i2i')
      || lower.includes('dall-e') || lower.includes('dalle')
      || lower.includes('imagen') || lower.includes('flux')
      || lower.includes('midjourney') || lower.includes('stable-diffusion')
      || lower.includes('sdxl') || lower.includes('cogview')
      || lower.includes('wanx')) return 'image_generation'
    if (lower.includes('video')) return 'video_understanding'
    if (lower.includes('whisper') || lower.includes('asr')
      || lower.includes('speech-to-text') || lower.includes('paraformer')
      || lower.includes('sensevoice')) return 'asr'
    if (lower.includes('tts') || lower.includes('speech-to-speech')
      || lower.includes('speech-synthesis') || lower.includes('cosyvoice')
      || lower.includes('sambert')) return 'tts'
    if (lower.includes('audio')) return 'audio_understanding'
    if (lower.includes('vl') || lower.includes('vision')) return 'vision'
    return 'chat'
  }

  const providerPageSizes = [10, 20, 50]
  const modelPageSizes = [10, 20, 50]
  const modalCardStyle = { maxWidth: '860px', width: 'calc(100vw - 32px)' }

  // 获取可用模型弹窗：固定高度，避免随列表数量伸缩导致窗口跳动
  const fetchModelsModalStyle = { maxWidth: '860px', width: 'calc(100vw - 32px)', height: 'min(640px, calc(100vh - 80px))' }
  // 内容区约束为 flex 列，列表在固定高度内滚动，避免卡片内容溢出（穿模）
  const fetchModelsModalContentStyle = { display: 'flex', flexDirection: 'column', minHeight: '0', overflow: 'hidden' }
  const providerAvatarStyle = { width: '48px', height: '48px', borderRadius: '14px', objectFit: 'cover' }
  const providerListAvatarStyle = { width: '40px', height: '40px', borderRadius: '8px', objectFit: 'cover' }

  const providerSearch = reactive({ name: '', type: null, status: null })
  const providerList = ref([])
  const providerLoading = ref(false)
  const providerPagination = reactive({ pageNum: 1, pageSize: 10, itemCount: 0 })
  const selectedProvider = ref(null)
  const modelList = ref([])
  const modelLoading = ref(false)
  const defaultModelUpdatingId = ref(null)
  const providerDefaultUpdatingId = ref(null)
  const modelPagination = reactive({ pageNum: 1, pageSize: 10, itemCount: 0 })
  const modelSearch = reactive({ modelType: null })
  const providerFormRef = ref(null)
  const modelFormRef = ref(null)

  const providerSelectOptions = computed(() => providerList.value.map(p => ({ label: p.providerName, value: p.id })))

  // 模型行操作 loading：{ id: 'test' | 'edit' | 'delete' }
  const modelRowActionLoading = reactive({})
  __impl.isModelRowActionLoading = (id, type) => modelRowActionLoading[id] === type
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
  const providerApiKeyPlaceholder = computed(() => providerModal.isEdit ? '留空表示不修改' : '请输入 API Key')

  const DASHSCOPE_NATIVE_BASE_URL = 'https://dashscope.aliyuncs.com'
  // 已知供应商类型（字典 ai_provider_type）的默认 OpenAI Compatible 端点，与后端 AiProviderBaseUrlPolicy 对齐
  const PROVIDER_DEFAULT_BASE_URL = {
    alibaba: 'https://dashscope.aliyuncs.com/compatible-mode',
    openai: 'https://api.openai.com/v1',
    zhipu: 'https://open.bigmodel.cn/api/paas/v4',
    moonshot: 'https://api.moonshot.cn/v1',
    deepseek: 'https://api.deepseek.com/v1',
    ollama: 'http://localhost:11434/v1',
  }
  // 已知默认地址集合（含尾斜杠变体）：当前地址是"别的协议/类型的默认"时切换后覆盖，自定义地址不覆盖
  const KNOWN_PROVIDER_DEFAULT_URLS = new Set(
    [DASHSCOPE_NATIVE_BASE_URL, ...Object.values(PROVIDER_DEFAULT_BASE_URL)]
      .flatMap(url => [url, `${url}/`]),
  )

  function resolveDefaultBaseUrl(adapterCode, providerType) {
    if (adapterCode === 'dashscope_native')
      return DASHSCOPE_NATIVE_BASE_URL
    return PROVIDER_DEFAULT_BASE_URL[providerType] || ''
  }

  watch(
    () => [providerModal.form.adapterCode, providerModal.form.providerType],
    ([adapterCode, providerType]) => {
      const resolved = resolveDefaultBaseUrl(adapterCode, providerType)
      if (!resolved)
        return

      const currentBaseUrl = providerModal.form.baseUrl?.trim() || ''
      if (!currentBaseUrl || (KNOWN_PROVIDER_DEFAULT_URLS.has(currentBaseUrl) && currentBaseUrl !== resolved))
        providerModal.form.baseUrl = resolved
    },
  )

  const modelModal = reactive({
    show: false,
    isEdit: false,
    saving: false,
    form: createModelForm(),
  })

  const fetchModelsModal = reactive({
    show: false,
    loading: false,
    importing: false,
    error: '',
    models: [],
    checked: {},
    modelTypes: {}, // { modelId: inferredType } — 每个模型的推断/用户修改后的类型
  })

  const fetchModelsModalSearch = ref('')
  const fetchModelsModalCategory = ref('all')

  // 顶部分类筛选：只显示当前供应商列表里真实存在的分类（全部始终保留）
  const fetchModelsModalCategories = computed(() => {
    const present = new Set(fetchModelsModal.models.map(m => fetchModelsModal.modelTypes[m.id] || inferModelType(m.id)))
    return [
      { label: '全部', value: 'all' },
      ...MODEL_TYPE_ORDER.filter(t => present.has(t)).map(t => ({ label: MODEL_TYPE_LABEL[t], value: t })),
    ]
  })

  // 分类筛选 + 搜索过滤（ID 模糊匹配）
  const fetchModelsModalFilteredModels = computed(() => {
    const keyword = fetchModelsModalSearch.value.trim().toLowerCase()
    const category = fetchModelsModalCategory.value
    return fetchModelsModal.models.filter(m => {
      if (category !== 'all' && (fetchModelsModal.modelTypes[m.id] || inferModelType(m.id)) !== category)
        return false
      if (keyword && !(m.id || '').toLowerCase().includes(keyword))
        return false
      return true
    })
  })

  // 按推断能力类型分组（顺序对齐阿里百炼模型广场分类）
  const fetchModelsModalGroups = computed(() => {
    const groupsMap = new Map()
    for (const model of fetchModelsModalFilteredModels.value) {
      const type = fetchModelsModal.modelTypes[model.id] || inferModelType(model.id)
      if (!groupsMap.has(type))
        groupsMap.set(type, [])
      groupsMap.get(type).push(model)
    }
    return MODEL_TYPE_ORDER
      .filter(t => groupsMap.has(t))
      .map(t => ({ type: t, label: modelTypeLabel(t), models: groupsMap.get(t) }))
  })

  function groupModels(type) {
    return fetchModelsModalGroups.value.find(g => g.type === type)?.models || []
  }
  function isGroupChecked(type) {
    const models = groupModels(type)
    return models.length > 0 && models.every(m => fetchModelsModal.checked[m.id])
  }
  function isGroupIndeterminate(type) {
    const models = groupModels(type)
    const n = models.filter(m => fetchModelsModal.checked[m.id]).length
    return n > 0 && n < models.length
  }
  function handleToggleGroup(type, checked) {
    for (const m of groupModels(type))
      fetchModelsModal.checked[m.id] = checked
  }

  // 导入数量 = 当前筛选视图内已勾选的模型数（切分类/搜索后只统计可见部分）
  const fetchModelsModalVisibleCheckedCount = computed(() => fetchModelsModalFilteredModels.value.filter(m => fetchModelsModal.checked[m.id]).length)

  function createProviderForm() {
    return {
      providerName: '',
      providerType: null,
      adapterCode: 'openai_compatible',
      logo: '',
      baseUrl: '',
      apiKey: '',
      status: '0',
      remark: '',
    }
  }

  function createModelForm(providerId = null) {
    return {
      providerId,
      modelType: null,
      modelId: '',
      modelName: '',
      icon: '',
      maxTokens: null,
      contextWindow: null,
      inputPricePerMillionCent: null,
      outputPricePerMillionCent: null,
      capabilityCodes: [],
      sortOrder: 0,
      isDefault: '0',
      status: '0',
      description: '',
    }
  }

  function handleLogoUploadFinish({ event }) {
    try {
      const res = JSON.parse(event.target.response)
      if (res.code === 200 && res.data) {
        providerModal.form.logo = res.data.fileId || res.data.id || res.data
      }
      else {
        window.$message.error(res.msg || '上传失败')
      }
    }
    catch {
      window.$message.error('上传失败')
    }
    return false
  }

  function handleIconUploadFinish({ event }) {
    try {
      const res = JSON.parse(event.target.response)
      if (res.code === 200 && res.data) {
        modelModal.form.icon = res.data.fileId || res.data.id || res.data
      }
      else {
        window.$message.error(res.msg || '上传失败')
      }
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
    // baseUrl 选填：已知类型/协议选择后自动填充默认地址；azure/custom 留空时后端会明确提示
    baseUrl: [],
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

  const modelColumns = [
    {
      title: '模型名称',
      key: 'modelName',
      width: 150,
      ellipsis: { tooltip: true },
      render(row) { return h('span', { class: 'model-name' }, row.modelName || '未命名模型') },
    },
    {
      title: '模型编码',
      key: 'modelId',
      width: 190,
      ellipsis: { tooltip: true },
      render(row) { return h('code', { class: 'model-code', title: row.modelId }, row.modelId || '—') },
    },
    {
      title: '类型',
      key: 'modelType',
      width: 80,
      render(row) { return h(DictTag, { dictType: 'ai_model_type', value: row.modelType, size: 'small' }) },
    },
    {
      title: '最大 Token',
      key: 'maxTokens',
      width: 90,
      align: 'right',
      render(row) { return row.maxTokens ? row.maxTokens.toLocaleString() : '-' },
    },
    {
      title: '是否默认',
      key: 'isDefault',
      width: 90,
      align: 'center',
      render(row) {
        const isDefault = row.isDefault === '1'
        return h(NSwitch, {
          value: isDefault,
          size: 'small',
          loading: defaultModelUpdatingId.value === row.id,
          disabled: defaultModelUpdatingId.value !== null,
          ariaLabel: isDefault ? '当前默认模型' : '设为默认模型',
          onUpdateValue: value => handleDefaultModelChange(row, value),
        })
      },
    },
    {
      title: '状态',
      key: 'status',
      width: 55,
      align: 'center',
      render(row) { return h(DictTag, { dictType: 'ai_status', value: row.status, size: 'small' }) },
    },
    { title: '描述', key: 'description', width: 170, ellipsis: { tooltip: true } },
    {
      title: '操作',
      key: 'actions',
      width: 160,
      fixed: 'right',
      render(row) {
        const rowLoading = type => isModelRowActionLoading(row.id, type)
        const anyLoading = rowLoading('test') || rowLoading('edit') || rowLoading('delete')
        const actions = [
          h(NButton, { text: true, size: 'small', class: 'text-info', loading: rowLoading('test'), disabled: anyLoading, onClick: () => handleTestModel(row) }, { default: () => '测试' }),
          h(NButton, { text: true, size: 'small', class: 'text-primary', loading: rowLoading('edit'), disabled: anyLoading, onClick: () => handleEditModel(row) }, { default: () => '编辑' }),
          h(NPopconfirm, { onPositiveClick: () => handleDeleteModel(row) }, {
            trigger: () => h(NButton, { text: true, size: 'small', class: 'text-error', loading: rowLoading('delete'), disabled: anyLoading }, { default: () => '删除' }),
            default: () => '确定删除该模型吗？',
          }),
        ]
        return h('div', { class: 'table-actions' }, actions)
      },
    },
  ]

  function providerInitial(provider) {
    return (provider?.providerName || '?').trim().charAt(0).toUpperCase()
  }

  async function handleTestModel(row) {
    if (modelRowActionLoading[row.id])
      return
    modelRowActionLoading[row.id] = 'test'
    try {
      const res = await modelTest(row.id)
      if (res.code === 200)
        window.$message.success(res.data || '连接成功')
      else
        window.$message.error(res.msg || '模型连接失败')
    }
    catch (error) {
      window.$message.error(error.message || '模型连接失败')
    }
    finally {
      delete modelRowActionLoading[row.id]
    }
  }

  function providerModelCount(provider) {
    if (Array.isArray(provider?.models))
      return provider.models.length
    if (!provider?.models)
      return 0
    try {
      const models = JSON.parse(provider.models)
      return Array.isArray(models) ? models.length : 0
    }
    catch {
      return 0
    }
  }

  function formatProviderTime(value) {
    if (!value)
      return '—'
    return String(value).replace('T', ' ').slice(0, 16)
  }

  function paginationPrefix({ itemCount }) {
    return `共 ${itemCount} 条`
  }

  function handleProviderSearch() {
    providerPagination.pageNum = 1
    clearSelectedProvider()
    loadProviders()
  }

  function handleResetProviderSearch() {
    providerSearch.name = ''
    providerSearch.type = null
    providerSearch.status = null
    providerPagination.pageNum = 1
    clearSelectedProvider()
    loadProviders()
  }

  function handleProviderPageChange(page) {
    providerPagination.pageNum = page
    clearSelectedProvider()
    loadProviders()
  }

  function handleProviderPageSizeChange(pageSize) {
    providerPagination.pageSize = pageSize
    providerPagination.pageNum = 1
    clearSelectedProvider()
    loadProviders()
  }

  async function loadProviders() {
    providerLoading.value = true
    try {
      const selectedId = selectedProvider.value?.id
      const params = { pageNum: providerPagination.pageNum, pageSize: providerPagination.pageSize }
      if (providerSearch.name)
        params.providerName = providerSearch.name
      if (providerSearch.type)
        params.providerType = providerSearch.type
      if (providerSearch.status)
        params.status = providerSearch.status
      const res = await fetchProviderPage(params)
      if (res.code === 200 && res.data) {
        providerList.value = res.data.records || []
        providerPagination.itemCount = Number(res.data.total || 0)
        if (selectedId) {
          const current = providerList.value.find(provider => provider.id === selectedId)
          if (current)
            selectedProvider.value = current
        }
      }
    }
    catch {}
    finally {
      providerLoading.value = false
    }
  }

  async function loadModels() {
    if (!selectedProvider.value) {
      modelList.value = []
      return
    }
    modelLoading.value = true
    try {
      const res = await fetchModelPage({
        pageNum: modelPagination.pageNum,
        pageSize: modelPagination.pageSize,
        providerId: selectedProvider.value.id,
        ...(modelSearch.modelType ? { modelType: modelSearch.modelType } : {}),
      })
      if (res.code === 200 && res.data) {
        modelList.value = res.data.records || []
        modelPagination.itemCount = Number(res.data.total || 0)
      }
    }
    catch {}
    finally {
      modelLoading.value = false
    }
  }

  function handleSelectProvider(row) {
    if (selectedProvider.value?.id === row.id)
      return

    selectedProvider.value = row
    modelPagination.pageNum = 1
    loadModels()
  }

  function clearSelectedProvider() {
    selectedProvider.value = null
    modelList.value = []
    modelPagination.pageNum = 1
    modelPagination.itemCount = 0
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

  function handleAddProvider() {
    providerModal.isEdit = false
    providerModal.form = createProviderForm()
    providerModal.show = true
  }

  async function handleEditProvider(row) {
    if (providerEditing.value)
      return
    providerEditing.value = true
    try {
      const res = await providerGetById(row.id)
      if (res.code === 200 && res.data) {
        providerModal.isEdit = true
        providerModal.form = { ...createProviderForm(), ...res.data, apiKey: '' }
        providerModal.show = true
      }
      else {
        window.$message.error(res.msg || '读取供应商失败')
      }
    }
    catch (e) {
      window.$message.error(e.message || '读取供应商失败')
    }
    finally {
      providerEditing.value = false
    }
  }

  async function handleSaveProvider() {
    try {
      await providerFormRef.value?.validate()
    }
    catch { return }
    providerModal.saving = true
    try {
      const payload = { ...providerModal.form }
      if (providerModal.isEdit && !payload.apiKey?.trim())
        delete payload.apiKey
      const res = providerModal.isEdit
        ? await providerUpdate(payload)
        : await providerAdd(payload)
      if (res.code === 200) {
        window.$message.success(providerModal.isEdit ? '更新成功' : '新增成功')
        providerModal.show = false
        await loadProviders()
      }
      else {
        window.$message.error(res.msg || '操作失败')
      }
    }
    catch (e) {
      window.$message.error(e.message || '操作失败')
    }
    finally {
      providerModal.saving = false
    }
  }

  async function handleDeleteProvider(id) {
    try {
      const res = await providerDelete(id)
      if (res.code === 200) {
        window.$message.success('删除成功')
        if (selectedProvider.value?.id === id) {
          clearSelectedProvider()
        }
        await loadProviders()
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
    if (providerDefaultUpdatingId.value !== null)
      return
    providerDefaultUpdatingId.value = row.id
    try {
      const res = await providerSetDefault(row.id)
      if (res.code === 200) {
        window.$message.success('设置成功')
        await loadProviders()
      }
      else {
        window.$message.error(res.msg || '设置失败')
      }
    }
    catch (e) {
      window.$message.error(e.message || '设置失败')
    }
    finally {
      providerDefaultUpdatingId.value = null
    }
  }

  async function handleFetchModels() {
    if (!selectedProvider.value) {
      window.$message.warning('请先选择供应商')
      return
    }
    const provider = selectedProvider.value
    fetchModelsModal.show = true
    fetchModelsModal.loading = true
    fetchModelsModal.error = ''
    fetchModelsModal.models = []
    fetchModelsModal.checked = {}
    fetchModelsModal.modelTypes = {}
    fetchModelsModalSearch.value = ''
    fetchModelsModalCategory.value = 'all'
    try {
      const res = await providerFetchModels(provider.id)
      if (res.code === 200 && Array.isArray(res.data)) {
        fetchModelsModal.models = res.data
        const checked = {}
        const types = {}
        for (const model of res.data) {
          checked[model.id] = true
          types[model.id] = inferModelType(model.id)
        }
        fetchModelsModal.checked = checked
        fetchModelsModal.modelTypes = types
      }
      else {
        fetchModelsModal.error = res.msg || '获取模型失败'
      }
    }
    catch (e) {
      fetchModelsModal.error = e.message || '获取模型失败'
    }
    finally {
      fetchModelsModal.loading = false
    }
  }

  function handleToggleModel(id, checked) {
    fetchModelsModal.checked[id] = checked
  }

  async function handleImportModels() {
    const providerId = selectedProvider.value?.id
    // 只导入当前筛选视图内勾选的模型，与按钮计数保持一致
    const items = fetchModelsModalFilteredModels.value
      .filter(m => fetchModelsModal.checked[m.id])
      .map(m => ({
        modelId: m.id,
        modelType: fetchModelsModal.modelTypes[m.id] || inferModelType(m.id),
      }))
    if (!providerId || items.length === 0)
      return

    fetchModelsModal.importing = true
    try {
      const res = await providerBatchImportModels(providerId, items)
      if (res.code === 200) {
        window.$message.success(`已导入 ${res.data ?? items.length} 个模型`)
        fetchModelsModal.show = false
        modelPagination.pageNum = 1
        await Promise.all([loadModels(), loadProviders()])
      }
      else {
        window.$message.error(res.msg || '导入失败')
      }
    }
    catch (e) {
      window.$message.error(e.message || '导入失败')
    }
    finally {
      fetchModelsModal.importing = false
    }
  }

  function handleAddModel(provider = selectedProvider.value) {
    if (!provider) {
      window.$message.warning('请先选择供应商')
      return
    }
    const providerChanged = selectedProvider.value?.id !== provider.id
    selectedProvider.value = provider
    if (providerChanged) {
      modelPagination.pageNum = 1
      loadModels()
    }
    modelModal.isEdit = false
    modelModal.form = createModelForm(provider.id || null)
    modelModal.show = true
  }

  async function handleEditModel(row) {
    if (modelRowActionLoading[row.id])
      return
    modelRowActionLoading[row.id] = 'edit'
    try {
      const res = await modelGetById(row.id)
      if (res.code === 200 && res.data) {
        modelModal.isEdit = true
        modelModal.form = { ...res.data }
        modelModal.show = true
      }
      else {
        window.$message.error(res.msg || '读取模型失败')
      }
    }
    catch (e) {
      window.$message.error(e.message || '读取模型失败')
    }
    finally {
      delete modelRowActionLoading[row.id]
    }
  }

  async function handleSaveModel() {
    try {
      await modelFormRef.value?.validate()
    }
    catch { return }
    modelModal.saving = true
    try {
      const res = modelModal.isEdit
        ? await modelUpdate(modelModal.form)
        : await modelAdd(modelModal.form)
      if (res.code === 200) {
        window.$message.success(modelModal.isEdit ? '更新成功' : '新增成功')
        modelModal.show = false
        await Promise.all([loadModels(), loadProviders()])
      }
      else {
        window.$message.error(res.msg || '操作失败')
      }
    }
    catch (e) {
      window.$message.error(e.message || '操作失败')
    }
    finally {
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

  onMounted(() => {
    loadProviders()
  })
  __impl.modelTypeLabel = modelTypeLabel
  __impl.modelTypeTagColor = modelTypeTagColor
  __impl.inferModelType = inferModelType
  __impl.resolveDefaultBaseUrl = resolveDefaultBaseUrl
  __impl.groupModels = groupModels
  __impl.isGroupChecked = isGroupChecked
  __impl.isGroupIndeterminate = isGroupIndeterminate
  __impl.handleToggleGroup = handleToggleGroup
  __impl.createProviderForm = createProviderForm
  __impl.createModelForm = createModelForm
  __impl.handleLogoUploadFinish = handleLogoUploadFinish
  __impl.handleIconUploadFinish = handleIconUploadFinish
  __impl.providerInitial = providerInitial
  __impl.handleTestModel = handleTestModel
  __impl.providerModelCount = providerModelCount
  __impl.formatProviderTime = formatProviderTime
  __impl.paginationPrefix = paginationPrefix
  __impl.handleProviderSearch = handleProviderSearch
  __impl.handleResetProviderSearch = handleResetProviderSearch
  __impl.handleProviderPageChange = handleProviderPageChange
  __impl.handleProviderPageSizeChange = handleProviderPageSizeChange
  __impl.loadProviders = loadProviders
  __impl.loadModels = loadModels
  __impl.handleSelectProvider = handleSelectProvider
  __impl.clearSelectedProvider = clearSelectedProvider
  __impl.handleModelPageChange = handleModelPageChange
  __impl.handleModelPageSizeChange = handleModelPageSizeChange
  __impl.handleModelSearch = handleModelSearch
  __impl.handleAddProvider = handleAddProvider
  __impl.handleEditProvider = handleEditProvider
  __impl.handleSaveProvider = handleSaveProvider
  __impl.handleDeleteProvider = handleDeleteProvider
  __impl.handleSetDefault = handleSetDefault
  __impl.handleFetchModels = handleFetchModels
  __impl.handleToggleModel = handleToggleModel
  __impl.handleImportModels = handleImportModels
  __impl.handleAddModel = handleAddModel
  __impl.handleEditModel = handleEditModel
  __impl.handleSaveModel = handleSaveModel
  __impl.handleDeleteModel = handleDeleteModel
  __impl.handleDefaultModelChange = handleDefaultModelChange

  return {
    __impl, mut, clearSelectedProvider, createModelForm, createProviderForm, formatProviderTime, groupModels, handleAddModel,
    handleAddProvider, handleDefaultModelChange, handleDeleteModel, handleDeleteProvider, handleEditModel, handleEditProvider, handleFetchModels, handleIconUploadFinish,
    handleImportModels, handleLogoUploadFinish, handleModelPageChange, handleModelPageSizeChange, handleModelSearch, handleProviderPageChange, handleProviderPageSizeChange, handleProviderSearch,
    handleResetProviderSearch, handleSaveModel, handleSaveProvider, handleSelectProvider, handleSetDefault, handleTestModel, handleToggleGroup, handleToggleModel,
    inferModelType, isGroupChecked, isGroupIndeterminate, isModelRowActionLoading, loadModels, loadProviders, modelTypeLabel, modelTypeTagColor,
    paginationPrefix, providerInitial, providerModelCount, resolveDefaultBaseUrl, providerTypeOptions, providerAdapterOptions, modelTypeOptions, statusOptions,
    isDefaultOptions, modelCapabilityOptions, MODEL_TYPE_LABEL, MODEL_TYPE_ORDER, MODEL_TYPE_TAG_COLOR, modelTypeOverrideOptions, providerPageSizes, modelPageSizes,
    modalCardStyle, fetchModelsModalStyle, fetchModelsModalContentStyle, providerAvatarStyle, providerListAvatarStyle, providerSearch, providerList, providerLoading,
    providerPagination, selectedProvider, modelList, modelLoading, defaultModelUpdatingId, providerDefaultUpdatingId, modelPagination, modelSearch,
    providerFormRef, modelFormRef, providerSelectOptions, modelRowActionLoading, uploadPrefix, uploadHeaders, providerEditing, providerModal,
    providerApiKeyPlaceholder, DASHSCOPE_NATIVE_BASE_URL, PROVIDER_DEFAULT_BASE_URL, KNOWN_PROVIDER_DEFAULT_URLS, modelModal, fetchModelsModal, fetchModelsModalSearch, fetchModelsModalCategory,
    fetchModelsModalCategories, fetchModelsModalFilteredModels, fetchModelsModalGroups, fetchModelsModalVisibleCheckedCount, providerRules, modelRules, modelColumns,
  }
}
