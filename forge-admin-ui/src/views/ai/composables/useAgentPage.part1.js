/** ai/agent setup part 1. */
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  agentAdd,
  agentDelete,
  agentGetById,
  agentPage,
  agentToolAdd,
  agentToolDelete,
  agentToolPage,
  agentUpdate,
  contextConfigAdd,
  contextConfigDelete,
  contextConfigList,
  contextConfigUpdate,
  modelListByProvider,
  providerPage,
  routePolicyPage,
} from '@/api/ai'
import { useDict } from '@/composables/useDict'
import { generateUUID } from '@/utils'

export function applyAgentPagePart1() {
  const __impl = {}
  const mut = {
    chatAbortController: null,
    chatScrollFrame: null,
    activeChatStreamId: null,
    hydratingProvider: false,
  }
  const finishAgentReasoning = (...args) => __impl.finishAgentReasoning(...args)
  const handleAgentSSEChunk = (...args) => __impl.handleAgentSSEChunk(...args)
  const loadKnowledgeList = (...args) => __impl.loadKnowledgeList(...args)
  const loadSkills = (...args) => __impl.loadSkills(...args)
  const resetConversation = (...args) => __impl.resetConversation(...args)
  const saveSkillBindings = (...args) => __impl.saveSkillBindings(...args)
  const scrollChatToBottom = (...args) => __impl.scrollChatToBottom(...args)
  const scrollLatestReasoningToBottom = (...args) => __impl.scrollLatestReasoningToBottom(...args)
  const sendChatMessage = (...args) => __impl.sendChatMessage(...args)
  const stopChat = (...args) => __impl.stopChat(...args)
  const updateToolEnabled = (...args) => __impl.updateToolEnabled(...args)
  const useSuggestedQuestion = (...args) => __impl.useSuggestedQuestion(...args)

  const router = useRouter()
  const { dict } = useDict('ai_agent_model_selection_mode')

  const viewMode = ref('market')
  const agentLoading = ref(false)
  const saveLoading = ref(false)
  const publishLoading = ref(false)
  const modelLoading = ref(false)
  const toolModalVisible = ref(false)
  const contextModalVisible = ref(false)

  // 工具管理相关
  const agentTools = ref([])
  const showAddTool = ref(false)
  const newTool = reactive({ source: '', key: '', group: 'default' })
  const toolLoading = ref(false)

  // 技能绑定相关
  const allSkills = ref([])
  const boundSkillIds = ref([])
  const skillLoading = ref(false)
  const skillSaveLoading = ref(false)
  // 知识库绑定相关（knowledgeIds 直接存于 agentForm，随主表单保存）
  const knowledgeOptions = ref([])
  const knowledgeLoading = ref(false)
  const baseModalVisible = ref(false)
  const publishMenuVisible = ref(false)
  const baseSaveLoading = ref(false)
  const agentFormRef = ref(null)
  const baseFormRef = ref(null)
  const messageListRef = ref(null)
  const agentList = ref([])
  const providerList = ref([])
  const modelList = ref([])
  const routePolicyList = ref([])
  const contextConfigs = ref([])
  const deletedContextIds = ref([])
  const expandedContextNames = ref([])
  const chatMessages = ref([])
  const chatInput = ref('')
  const chatSending = ref(false)
  const chatStatusText = ref('等待输入')
  const sessionId = ref(generateUUID())
  const pagination = reactive({ page: 1, pageSize: 12, itemCount: 0 })
  const filter = reactive({ keyword: '', status: 'all' })
  const baseMode = ref('create')
  const baseEditingAgent = ref(null)

  const defaultPrompt = `你是一个企业级智能体。请根据用户问题给出准确、清晰、可执行的回答。

  要求：
  1. 优先基于已配置的上下文和工具能力回答。
  2. 不确定的信息要说明限制，不要编造。
  3. 输出结构清晰，必要时给出步骤或清单。`

  const agentForm = reactive(createEmptyAgent())
  const modelParamEnabled = reactive({
    temperature: true,
    maxTokens: true,
  })
  const baseForm = reactive({
    id: null,
    agentName: '',
    agentCode: '',
    description: '',
  })

  const statusFilterOptions = [
    { label: '全部状态', value: 'all' },
    { label: '已发布', value: '0' },
    { label: '草稿', value: '1' },
  ]

  const contextTypeOptions = [
    { label: 'SPEC', value: 'SPEC' },
    { label: 'RULE', value: 'RULE' },
    { label: 'SAMPLE', value: 'SAMPLE' },
  ]

  const reservedMcpToolOptions = [
    { label: '知识库检索', value: 'knowledge_search' },
    { label: '数据库查询', value: 'database_query' },
    { label: 'HTTP 请求', value: 'http_request' },
    { label: '文件读取', value: 'file_reader' },
    { label: '工作流触发', value: 'workflow_trigger' },
    { label: '代码执行', value: 'code_executor' },
  ]

  const baseRules = {
    agentName: [{ required: true, message: '请输入智能体名称', trigger: 'blur' }],
    agentCode: [
      { required: true, message: '请输入智能体编码', trigger: 'blur' },
      {
        pattern: /^[a-z][a-z0-9_]{2,49}$/,
        message: '编码需以小写字母开头，仅支持小写字母、数字和下划线',
        trigger: 'blur',
      },
    ],
  }

  const agentRules = {
    systemPrompt: [{ required: true, message: '请输入系统提示词', trigger: 'blur' }],
  }

  const providerOptions = computed(() => providerList.value.map(provider => ({
    label: provider.providerName,
    value: toIdString(provider.id),
  })))

  const modelOptions = computed(() => modelList.value.map(model => ({
    label: model.modelName ? `${model.modelName}（${model.modelId}）` : model.modelId,
    value: model.modelId,
  })))

  const modelSelectionModeOptions = computed(() => dict.value.ai_agent_model_selection_mode || [])
  const routePolicyOptions = computed(() => routePolicyList.value.map(policy => ({ label: policy.policyName, value: toIdString(policy.id) })))

  const selectedProviderName = computed(() => {
    const provider = providerList.value.find(item => toIdString(item.id) === toIdString(agentForm.providerId))
    return provider?.providerName || '未选择供应商'
  })

  const selectedModelLabel = computed(() => {
    if (agentForm.modelSelectionMode === 'POLICY') {
      return routePolicyList.value.find(policy => toIdString(policy.id) === toIdString(agentForm.routePolicyId))?.policyName || '选择路由策略'
    }
    if (!agentForm.modelName) {
      if (!agentForm.providerId) {
        return '选择模型'
      }
      return getProviderDefaultModel(agentForm.providerId) || selectedProviderName.value
    }
    const selectedModel = modelList.value.find(model => model.modelId === agentForm.modelName)
    return selectedModel?.modelName || agentForm.modelName
  })

  const pageCount = computed(() => Math.max(1, Math.ceil(pagination.itemCount / pagination.pageSize)))

  const baseModalTitle = computed(() => baseMode.value === 'create' ? '创建智能体' : '编辑基础信息')
  const enabledContextCount = computed(() => contextConfigs.value.filter(config => config.enabled).length)
  const selectedMcpToolLabels = computed(() => (agentForm.extraConfig.mcpTools || []).map(getMcpToolLabel))
  const suggestedQuestionCount = computed(() => (agentForm.extraConfig.suggestedQuestions || []).filter(Boolean).length)
  const publishTimeText = computed(() => {
    if (agentForm.status !== '0') {
      return '尚未发布'
    }
    const publishedAt = agentForm.updateTime || agentForm.createTime
    return publishedAt ? `发布于 ${formatRelativeTime(publishedAt)}` : '已发布'
  })

  const openingStatement = computed(() => {
    return agentForm.extraConfig.openingStatement || `你好，我是${agentForm.agentName || '智能体'}，可以开始测试。`
  })

  const previewMessages = computed(() => {
    if (chatMessages.value.length) {
      return chatMessages.value
    }
    return [{
      id: 'opening',
      role: 'assistant',
      content: openingStatement.value,
      time: formatTime(new Date()),
    }]
  })

  const suggestedQuestionList = computed(() => {
    return (agentForm.extraConfig.suggestedQuestions || []).filter(Boolean).slice(0, 4)
  })

  const canSendChat = computed(() => {
    return !!agentForm.id
      && agentForm.status === '0'
      && !!agentForm.agentCode
      && !!chatInput.value.trim()
      && !chatSending.value
  })

  watch(() => filter.keyword, () => {
    pagination.page = 1
    loadAgents()
  })

  watch(() => filter.status, () => {
    pagination.page = 1
    loadAgents()
  })

  watch(() => pagination.page, () => {
    if (pagination.page > pageCount.value) {
      pagination.page = pageCount.value
    }
  })

  watch(() => pagination.pageSize, () => {
    pagination.page = 1
  })

  watch(() => chatMessages.value.length, () => scrollChatToBottom())

  watch(() => agentForm.providerId, async (providerId) => {
    const shouldResetModel = !mut.hydratingProvider
    await loadModels(providerId)
    if (shouldResetModel) {
      agentForm.modelName = null
    }
  })

  function createEmptyAgent() {
    return {
      id: null,
      agentName: '',
      agentCode: '',
      description: '',
      systemPrompt: defaultPrompt,
      providerId: null,
      modelName: null,
      modelSelectionMode: 'PINNED',
      routePolicyId: null,
      temperature: 0.7,
      maxTokens: 4000,
      status: '1',
      knowledgeIds: [],
      ragMode: 'none',
      extraConfig: createDefaultExtraConfig(),
    }
  }

  function createDefaultExtraConfig() {
    return {
      openingStatement: '',
      suggestedQuestions: [],
      mcpTools: [],
      skills: [],
    }
  }

  function resetAgentForm(agent = createEmptyAgent()) {
    modelParamEnabled.temperature = agent.temperature !== null && agent.temperature !== undefined
    modelParamEnabled.maxTokens = agent.maxTokens !== null && agent.maxTokens !== undefined
    Object.assign(agentForm, {
      ...createEmptyAgent(),
      ...agent,
      id: toIdString(agent.id) || null,
      providerId: toIdString(agent.providerId) || null,
      modelName: agent.modelName || null,
      modelSelectionMode: agent.modelSelectionMode || 'PINNED',
      routePolicyId: toIdString(agent.routePolicyId) || null,
      temperature: Number(agent.temperature ?? 0.7),
      maxTokens: Number(agent.maxTokens ?? 4000),
      status: agent.status ?? '1',
      knowledgeIds: parseKnowledgeIds(agent.knowledgeIds),
      ragMode: agent.ragMode || 'none',
      extraConfig: parseExtraConfig(agent.extraConfig),
    })
  }

  function parseExtraConfig(value) {
    let parsed = {}
    if (value) {
      if (typeof value === 'string') {
        try {
          parsed = JSON.parse(value)
        }
        catch {
          parsed = {}
        }
      }
      else if (typeof value === 'object') {
        parsed = value
      }
    }
    const defaults = createDefaultExtraConfig()
    return {
      ...defaults,
      ...parsed,
      suggestedQuestions: Array.isArray(parsed.suggestedQuestions) ? parsed.suggestedQuestions : [],
      mcpTools: Array.isArray(parsed.mcpTools) ? parsed.mcpTools : [],
      skills: Array.isArray(parsed.skills) ? parsed.skills : [],
    }
  }

  // 知识库ID：后端存 JSON 数组字符串（如 "[1,2]"），表单用数字数组；兼容已是数组的情况
  function parseKnowledgeIds(value) {
    if (Array.isArray(value)) {
      return value.map(Number).filter(v => !Number.isNaN(v))
    }
    if (typeof value === 'string' && value.trim()) {
      try {
        const arr = JSON.parse(value)
        return Array.isArray(arr) ? arr.map(Number).filter(v => !Number.isNaN(v)) : []
      }
      catch {
        return []
      }
    }
    return []
  }

  function serializeAgent(status) {
    const extraConfig = {
      ...agentForm.extraConfig,
      contextCount: contextConfigs.value.length,
    }
    const knowledgeIds = parseKnowledgeIds(agentForm.knowledgeIds)
    return {
      id: agentForm.id,
      agentName: agentForm.agentName,
      agentCode: agentForm.agentCode,
      description: agentForm.description,
      systemPrompt: agentForm.systemPrompt,
      providerId: toIdString(agentForm.providerId) || null,
      modelName: agentForm.modelName,
      modelSelectionMode: agentForm.modelSelectionMode || 'PINNED',
      routePolicyId: agentForm.modelSelectionMode === 'POLICY' ? toIdString(agentForm.routePolicyId) || null : null,
      temperature: modelParamEnabled.temperature ? toNullableNumber(agentForm.temperature) : null,
      maxTokens: modelParamEnabled.maxTokens ? toNullableInteger(agentForm.maxTokens) : null,
      status: status ?? agentForm.status,
      knowledgeIds: JSON.stringify(knowledgeIds),
      ragMode: knowledgeIds.length ? 'smart' : 'none',
      extraConfig: JSON.stringify(extraConfig),
    }
  }

  async function loadAgents() {
    agentLoading.value = true
    try {
      const params = {
        pageNum: pagination.page,
        pageSize: pagination.pageSize,
        keyword: filter.keyword.trim() || undefined,
        status: filter.status === 'all' ? undefined : filter.status,
      }
      const res = await agentPage(params)
      if (res.code === 200 && res.data) {
        const records = res.data.records || []
        pagination.itemCount = Number(res.data.total || 0)
        if (!records.length && pagination.page > 1 && pagination.itemCount > 0) {
          pagination.page = Math.min(pagination.page - 1, pageCount.value)
          await loadAgents()
          return
        }
        agentList.value = records
      }
    }
    catch (error) {
      window.$message.error(error.message || '加载智能体失败')
    }
    finally {
      agentLoading.value = false
    }
  }

  async function handleAgentPageSizeChange(pageSize) {
    pagination.pageSize = pageSize
    pagination.page = 1
    await loadAgents()
  }

  async function loadProviders() {
    try {
      const res = await providerPage({ pageNum: 1, pageSize: 200, status: '0' })
      if (res.code === 200 && res.data) {
        providerList.value = res.data.records || []
      }
    }
    catch {}
  }

  async function loadRoutePolicies() {
    try {
      const res = await routePolicyPage({ pageNum: 1, pageSize: 200, status: '0' })
      if (res.code === 200)
        routePolicyList.value = res.data?.records || []
    }
    catch {}
  }

  async function loadModels(providerId) {
    modelList.value = []
    if (!providerId) {
      return
    }

    modelLoading.value = true
    try {
      const res = await modelListByProvider(providerId)
      if (res.code === 200) {
        modelList.value = res.data || []
      }
    }
    catch {}
    finally {
      modelLoading.value = false
    }
  }

  function toIdString(value) {
    return value === null || value === undefined || value === '' ? '' : String(value)
  }

  function toNullableNumber(value) {
    if (value === null || value === undefined || value === '') {
      return null
    }
    const numericValue = Number(value)
    return Number.isFinite(numericValue) ? numericValue : null
  }

  function toNullableInteger(value) {
    const numericValue = toNullableNumber(value)
    return numericValue === null ? null : Math.round(numericValue)
  }

  async function loadAgentContexts(agentCode) {
    contextConfigs.value = []
    deletedContextIds.value = []
    expandedContextNames.value = []
    if (!agentCode) {
      return
    }

    try {
      const res = await contextConfigList(agentCode)
      if (res.code === 200) {
        contextConfigs.value = (res.data || []).map(config => ({
          ...config,
          localKey: `context_${config.id}`,
          enabled: config.status !== '1',
        }))
        expandedContextNames.value = contextConfigs.value.length ? [contextConfigs.value[0].localKey] : []
      }
    }
    catch {}
  }

  function getProviderDefaultModel(providerId) {
    const provider = providerList.value.find(item => toIdString(item.id) === toIdString(providerId))
    return provider?.defaultModel || ''
  }

  function getAgentInitial(agent) {
    return (agent.agentName || agent.agentCode || 'A').charAt(0).toUpperCase()
  }

  function getAgentGradient(code) {
    const gradients = [
      'linear-gradient(135deg, #2563eb 0%, #0891b2 100%)',
      'linear-gradient(135deg, #059669 0%, #0d9488 100%)',
      'linear-gradient(135deg, #7c3aed 0%, #2563eb 100%)',
      'linear-gradient(135deg, #d97706 0%, #dc2626 100%)',
      'linear-gradient(135deg, #475569 0%, #0f766e 100%)',
    ]
    const seed = (code || 'agent').split('').reduce((total, char) => total + char.charCodeAt(0), 0)
    return gradients[seed % gradients.length]
  }

  function getAgentTools(agent) {
    return parseExtraConfig(agent.extraConfig).mcpTools || []
  }

  function getMcpToolLabel(value) {
    return reservedMcpToolOptions.find(option => option.value === value)?.label || value
  }

  function getContextPreview(context) {
    const content = (context.configContent || '').replace(/\s+/g, ' ').trim()
    if (!content) {
      return '暂无内容'
    }
    return content.length > 48 ? `${content.slice(0, 48)}...` : content
  }

  function formatTime(date) {
    const h = String(date.getHours()).padStart(2, '0')
    const m = String(date.getMinutes()).padStart(2, '0')
    return `${h}:${m}`
  }

  function formatRelativeTime(value) {
    const normalizedValue = typeof value === 'string' ? value.replace(/-/g, '/') : value
    const timestamp = new Date(normalizedValue).getTime()
    if (!Number.isFinite(timestamp)) {
      return '未知时间'
    }

    const diffSeconds = Math.max(0, Math.floor((Date.now() - timestamp) / 1000))
    const minute = 60
    const hour = 60 * minute
    const day = 24 * hour
    const month = 30 * day
    const year = 365 * day

    if (diffSeconds < minute) {
      return '刚刚'
    }
    if (diffSeconds < hour) {
      return `${Math.floor(diffSeconds / minute)} 分钟前`
    }
    if (diffSeconds < day) {
      return `${Math.floor(diffSeconds / hour)} 小时前`
    }
    if (diffSeconds < month) {
      return `${Math.floor(diffSeconds / day)} 天前`
    }
    if (diffSeconds < year) {
      return `${Math.floor(diffSeconds / month)} 个月前`
    }
    return `${Math.floor(diffSeconds / year)} 年前`
  }

  async function openWorkbench(agent) {
    viewMode.value = 'builder'
    baseModalVisible.value = false
    chatInput.value = ''
    await hydrateAgentForm(agent)
    await loadAgentContexts(agent.agentCode)
    resetConversation()
    await nextTick()
    agentFormRef.value?.restoreValidation?.()
  }

  async function hydrateAgentForm(agent) {
    mut.hydratingProvider = true
    resetAgentForm(agent)
    try {
      await nextTick()
      await loadModels(agent.providerId)
    }
    finally {
      mut.hydratingProvider = false
    }
  }

  function resetBaseForm(agent = {}) {
    Object.assign(baseForm, {
      id: toIdString(agent.id) || null,
      agentName: agent.agentName || '',
      agentCode: agent.agentCode || '',
      description: agent.description || '',
    })
  }

  function openBaseModal(agent = null) {
    baseMode.value = agent?.id ? 'edit' : 'create'
    baseEditingAgent.value = agent
    resetBaseForm(agent || {})
    baseModalVisible.value = true
    nextTick(() => baseFormRef.value?.restoreValidation?.())
  }

  function handleOpenWorkbenchBaseSettings() {
    baseMode.value = 'edit'
    baseEditingAgent.value = serializeAgent()
    resetBaseForm(agentForm)
    baseModalVisible.value = true
    nextTick(() => baseFormRef.value?.restoreValidation?.())
  }

  function resetModelParams() {
    modelParamEnabled.temperature = true
    modelParamEnabled.maxTokens = true
    agentForm.temperature = 0.7
    agentForm.maxTokens = 4000
  }

  async function handleCreateAgent() {
    openBaseModal()
  }

  function resetAgentSearch() {
    filter.keyword = ''
    filter.status = 'all'
    pagination.page = 1
    loadAgents()
  }

  function getAgentCardActions(agent) {
    return [
      { label: '编辑基础信息', key: 'edit' },
      { label: agent.status === '0' ? '下线智能体' : '发布智能体', key: 'toggle-status' },
      { label: '删除智能体', key: 'delete' },
    ]
  }

  function handleAgentCardAction(key, agent) {
    if (key === 'edit') {
      handleEditAgent(agent)
      return
    }
    if (key === 'toggle-status') {
      if (agent.status === '0') {
        window.$dialog?.warning({
          title: '下线智能体',
          content: `下线后用户将无法访问「${agent.agentName}」，确定下线吗？`,
          positiveText: '确定下线',
          negativeText: '取消',
          onPositiveClick: () => handleTogglePublish(agent),
        })
      }
      else {
        handleTogglePublish(agent)
      }
      return
    }
    if (key === 'delete') {
      window.$dialog?.warning({
        title: '删除智能体',
        content: `确定删除智能体「${agent.agentName}」吗？删除后不可恢复。`,
        positiveText: '确定删除',
        negativeText: '取消',
        onPositiveClick: () => handleDeleteAgent(agent),
      })
    }
  }

  async function handleEditAgent(agent) {
    try {
      const res = await agentGetById(agent.id)
      if (res.code === 200 && res.data) {
        openBaseModal(res.data)
      }
    }
    catch (error) {
      window.$message.error(error.message || '加载智能体详情失败')
    }
  }

  async function handleOpenDesigner(agent) {
    try {
      const res = await agentGetById(agent.id)
      if (res.code === 200 && res.data) {
        await openWorkbench(res.data)
      }
    }
    catch (error) {
      window.$message.error(error.message || '加载智能体详情失败')
    }
  }

  function buildBaseAgentPayload(status = '1') {
    const defaultProvider = providerList.value.find(item => item.isDefault === '1') || providerList.value[0]
    return {
      ...createEmptyAgent(),
      agentName: baseForm.agentName,
      agentCode: baseForm.agentCode,
      description: baseForm.description,
      providerId: defaultProvider?.id ? toIdString(defaultProvider.id) : null,
      modelName: defaultProvider?.defaultModel || null,
      status,
      knowledgeIds: null,
      ragMode: 'none',
      extraConfig: JSON.stringify(createDefaultExtraConfig()),
    }
  }

  function mergeBaseAgentPayload(agent) {
    return {
      ...agent,
      id: baseForm.id,
      agentName: baseForm.agentName,
      agentCode: baseForm.agentCode,
      description: baseForm.description,
      extraConfig: typeof agent.extraConfig === 'string'
        ? agent.extraConfig
        : JSON.stringify(parseExtraConfig(agent.extraConfig)),
    }
  }

  async function findAgentByCode(agentCode) {
    if (!agentCode) {
      return null
    }
    const res = await agentPage({ pageNum: 1, pageSize: 10, keyword: agentCode })
    if (res.code !== 200) {
      return null
    }
    const records = res.data?.records || []
    return records.find(agent => agent.agentCode === agentCode) || records[0] || null
  }

  async function handleSaveBaseInfo() {
    await baseFormRef.value?.validate()
    baseSaveLoading.value = true
    try {
      const payload = baseMode.value === 'create'
        ? buildBaseAgentPayload('1')
        : mergeBaseAgentPayload(baseEditingAgent.value || {})
      const res = payload.id ? await agentUpdate(payload) : await agentAdd(payload)
      if (res.code !== 200) {
        window.$message.error(res.msg || '保存失败')
        return
      }

      const editingCurrentWorkbench = viewMode.value === 'builder'
        && baseMode.value === 'edit'
        && toIdString(payload.id) === toIdString(agentForm.id)
      await loadAgents()
      baseModalVisible.value = false
      if (editingCurrentWorkbench) {
        agentForm.agentName = payload.agentName
        agentForm.agentCode = payload.agentCode
        agentForm.description = payload.description
      }
      window.$message.success('保存成功')

      if (baseMode.value === 'create') {
        const savedAgent = agentList.value.find(agent => agent.agentCode === payload.agentCode)
          || await findAgentByCode(payload.agentCode)
        if (savedAgent) {
          await openWorkbench(savedAgent)
        }
      }
    }
    catch (error) {
      window.$message.error(error.message || '保存失败')
    }
    finally {
      baseSaveLoading.value = false
    }
  }

  async function validateContextConfigs() {
    const invalidIndex = contextConfigs.value.findIndex(config => !config.configName?.trim() || !config.configContent?.trim())
    if (invalidIndex > -1) {
      expandedContextNames.value = [contextConfigs.value[invalidIndex].localKey]
      window.$message.error('请补全上下文配置名称和内容')
      return false
    }
    return true
  }

  async function saveAgent(status) {
    await agentFormRef.value?.validate()
    const contextValid = await validateContextConfigs()
    if (!contextValid) {
      return false
    }

    const payload = serializeAgent(status)
    const res = payload.id ? await agentUpdate(payload) : await agentAdd(payload)
    if (res.code !== 200) {
      window.$message.error(res.msg || '保存失败')
      return false
    }

    await syncContextConfigs(payload.agentCode)
    await loadAgents()
    const savedAgent = agentList.value.find(agent => agent.agentCode === payload.agentCode)
      || await findAgentByCode(payload.agentCode)
    if (savedAgent) {
      await hydrateAgentForm(savedAgent)
      await loadAgentContexts(savedAgent.agentCode)
    }
    return true
  }

  async function handleSaveDraft() {
    saveLoading.value = true
    try {
      const saved = await saveAgent(agentForm.status || '1')
      if (saved) {
        window.$message.success('保存成功')
      }
    }
    catch (error) {
      window.$message.error(error.message || '保存失败')
    }
    finally {
      saveLoading.value = false
    }
  }

  async function handlePublishAgent() {
    publishLoading.value = true
    try {
      const saved = await saveAgent('0')
      if (saved) {
        window.$message.success('发布成功')
      }
    }
    catch (error) {
      window.$message.error(error.message || '发布失败')
    }
    finally {
      publishLoading.value = false
    }
  }

  async function handlePublishMenuUpdate() {
    publishMenuVisible.value = false
    await handlePublishAgent()
  }

  function handlePendingPublishAction(label) {
    window.$message.info(`${label}功能待定`)
  }

  async function handleTogglePublish(agent) {
    const nextStatus = agent.status === '0' ? '1' : '0'
    try {
      const res = await agentUpdate({ ...agent, status: nextStatus })
      if (res.code === 200) {
        window.$message.success(nextStatus === '0' ? '发布成功' : '已下线')
        await loadAgents()
      }
      else {
        window.$message.error(res.msg || '操作失败')
      }
    }
    catch (error) {
      window.$message.error(error.message || '操作失败')
    }
  }

  async function handleDeleteAgent(agent) {
    try {
      const res = await agentDelete(agent.id)
      if (res.code === 200) {
        window.$message.success('删除成功')
        await loadAgents()
      }
      else {
        window.$message.error(res.msg || '删除失败')
      }
    }
    catch (error) {
      window.$message.error(error.message || '删除失败')
    }
  }

  async function syncContextConfigs(agentCode) {
    for (const id of deletedContextIds.value) {
      await contextConfigDelete(id)
    }

    for (const config of contextConfigs.value) {
      const payload = {
        id: config.id,
        agentCode,
        configName: config.configName,
        configContent: config.configContent,
        configType: config.configType || 'SPEC',
        sort: config.sort ?? 0,
        status: config.enabled ? '0' : '1',
      }

      if (payload.id) {
        await contextConfigUpdate(payload)
      }
      else {
        await contextConfigAdd(payload)
      }
    }
    deletedContextIds.value = []
  }

  function addContextConfig() {
    const localKey = `context_${Date.now()}_${Math.random().toString(36).slice(2)}`
    contextConfigs.value.push({
      localKey,
      agentCode: agentForm.agentCode,
      configName: '',
      configContent: '',
      configType: 'SPEC',
      sort: contextConfigs.value.length + 1,
      enabled: true,
    })
    expandedContextNames.value = [localKey]
  }

  function removeContextConfig(index) {
    const [removed] = contextConfigs.value.splice(index, 1)
    if (removed?.id) {
      deletedContextIds.value.push(removed.id)
    }
    expandedContextNames.value = expandedContextNames.value.filter(name => name !== removed?.localKey)
    if (!expandedContextNames.value.length && contextConfigs.value.length) {
      const nextIndex = Math.min(index, contextConfigs.value.length - 1)
      expandedContextNames.value = [contextConfigs.value[nextIndex].localKey]
    }
  }

  function backToMarket() {
    stopChat()
    viewMode.value = 'market'
  }

  function goToChat(agent) {
    const targetId = agent?.id || agentForm.id
    if (targetId) {
      router.push({ path: '/ai/agent-chat', query: { agentId: targetId } })
    }
  }

  // ============================================================
  // 工具与技能管理
  // ============================================================

  async function loadAgentTools() {
    if (!agentForm.id)
      return
    toolLoading.value = true
    try {
      const res = await agentToolPage({ agentId: agentForm.id, pageNum: 1, pageSize: 200 })
      agentTools.value = res.data?.records || []
    }
    catch { agentTools.value = [] }
    finally { toolLoading.value = false }
  }

  async function addTool() {
    if (!newTool.source || !newTool.key)
      return
    try {
      await agentToolAdd({
        agentId: agentForm.id,
        toolSource: newTool.source,
        toolKey: newTool.key,
        toolGroup: newTool.group || 'default',
        enabled: '1',
      })
      newTool.source = ''
      newTool.key = ''
      showAddTool.value = false
      await loadAgentTools()
    }
    catch (e) {
      window.$message.error(e.message || '添加失败')
    }
  }

  async function removeTool(tool) {
    try {
      await agentToolDelete(tool.id)
      await loadAgentTools()
    }
    catch (e) {
      window.$message.error(e.message || '删除失败')
    }
  }

  __impl.createEmptyAgent = createEmptyAgent
  __impl.createDefaultExtraConfig = createDefaultExtraConfig
  __impl.resetAgentForm = resetAgentForm
  __impl.parseExtraConfig = parseExtraConfig
  __impl.parseKnowledgeIds = parseKnowledgeIds
  __impl.serializeAgent = serializeAgent
  __impl.loadAgents = loadAgents
  __impl.handleAgentPageSizeChange = handleAgentPageSizeChange
  __impl.loadProviders = loadProviders
  __impl.loadRoutePolicies = loadRoutePolicies
  __impl.loadModels = loadModels
  __impl.toIdString = toIdString
  __impl.toNullableNumber = toNullableNumber
  __impl.toNullableInteger = toNullableInteger
  __impl.loadAgentContexts = loadAgentContexts
  __impl.getProviderDefaultModel = getProviderDefaultModel
  __impl.getAgentInitial = getAgentInitial
  __impl.getAgentGradient = getAgentGradient
  __impl.getAgentTools = getAgentTools
  __impl.getMcpToolLabel = getMcpToolLabel
  __impl.getContextPreview = getContextPreview
  __impl.formatTime = formatTime
  __impl.formatRelativeTime = formatRelativeTime
  __impl.openWorkbench = openWorkbench
  __impl.hydrateAgentForm = hydrateAgentForm
  __impl.resetBaseForm = resetBaseForm
  __impl.openBaseModal = openBaseModal
  __impl.handleOpenWorkbenchBaseSettings = handleOpenWorkbenchBaseSettings
  __impl.resetModelParams = resetModelParams
  __impl.handleCreateAgent = handleCreateAgent
  __impl.handleEditAgent = handleEditAgent
  __impl.handleOpenDesigner = handleOpenDesigner
  __impl.buildBaseAgentPayload = buildBaseAgentPayload
  __impl.mergeBaseAgentPayload = mergeBaseAgentPayload
  __impl.findAgentByCode = findAgentByCode
  __impl.handleSaveBaseInfo = handleSaveBaseInfo
  __impl.validateContextConfigs = validateContextConfigs
  __impl.saveAgent = saveAgent
  __impl.handleSaveDraft = handleSaveDraft
  __impl.handlePublishAgent = handlePublishAgent
  __impl.handlePublishMenuUpdate = handlePublishMenuUpdate
  __impl.handlePendingPublishAction = handlePendingPublishAction
  __impl.handleTogglePublish = handleTogglePublish
  __impl.handleDeleteAgent = handleDeleteAgent
  __impl.syncContextConfigs = syncContextConfigs
  __impl.addContextConfig = addContextConfig
  __impl.removeContextConfig = removeContextConfig
  __impl.backToMarket = backToMarket
  __impl.goToChat = goToChat
  __impl.loadAgentTools = loadAgentTools
  __impl.addTool = addTool
  __impl.removeTool = removeTool

  return {
    __impl,
    mut,
    addContextConfig,
    addTool,
    backToMarket,
    buildBaseAgentPayload,
    createDefaultExtraConfig,
    createEmptyAgent,
    findAgentByCode,
    finishAgentReasoning,
    formatRelativeTime,
    formatTime,
    getAgentCardActions,
    getAgentGradient,
    getAgentInitial,
    getAgentTools,
    getContextPreview,
    getMcpToolLabel,
    getProviderDefaultModel,
    goToChat,
    handleAgentCardAction,
    handleAgentPageSizeChange,
    handleAgentSSEChunk,
    handleCreateAgent,
    handleDeleteAgent,
    handleEditAgent,
    handleOpenDesigner,
    handleOpenWorkbenchBaseSettings,
    handlePendingPublishAction,
    handlePublishAgent,
    handlePublishMenuUpdate,
    handleSaveBaseInfo,
    handleSaveDraft,
    handleTogglePublish,
    hydrateAgentForm,
    loadAgentContexts,
    loadAgentTools,
    loadAgents,
    loadKnowledgeList,
    loadModels,
    loadProviders,
    loadRoutePolicies,
    loadSkills,
    mergeBaseAgentPayload,
    openBaseModal,
    openWorkbench,
    parseExtraConfig,
    parseKnowledgeIds,
    removeContextConfig,
    removeTool,
    resetAgentForm,
    resetAgentSearch,
    resetBaseForm,
    resetConversation,
    resetModelParams,
    saveAgent,
    saveSkillBindings,
    scrollChatToBottom,
    scrollLatestReasoningToBottom,
    sendChatMessage,
    serializeAgent,
    stopChat,
    syncContextConfigs,
    toIdString,
    toNullableInteger,
    toNullableNumber,
    updateToolEnabled,
    useSuggestedQuestion,
    validateContextConfigs,
    router,
    viewMode,
    agentLoading,
    saveLoading,
    publishLoading,
    modelLoading,
    toolModalVisible,
    contextModalVisible,
    agentTools,
    showAddTool,
    newTool,
    toolLoading,
    allSkills,
    boundSkillIds,
    skillLoading,
    skillSaveLoading,
    knowledgeOptions,
    knowledgeLoading,
    baseModalVisible,
    publishMenuVisible,
    baseSaveLoading,
    agentFormRef,
    baseFormRef,
    messageListRef,
    agentList,
    providerList,
    modelList,
    routePolicyList,
    contextConfigs,
    deletedContextIds,
    expandedContextNames,
    chatMessages,
    chatInput,
    chatSending,
    chatStatusText,
    sessionId,
    pagination,
    filter,
    baseMode,
    baseEditingAgent,
    defaultPrompt,
    agentForm,
    modelParamEnabled,
    baseForm,
    statusFilterOptions,
    contextTypeOptions,
    reservedMcpToolOptions,
    baseRules,
    agentRules,
    providerOptions,
    modelOptions,
    modelSelectionModeOptions,
    routePolicyOptions,
    selectedProviderName,
    selectedModelLabel,
    pageCount,
    baseModalTitle,
    enabledContextCount,
    selectedMcpToolLabels,
    suggestedQuestionCount,
    publishTimeText,
    openingStatement,
    previewMessages,
    suggestedQuestionList,
    canSendChat,
  }
}
