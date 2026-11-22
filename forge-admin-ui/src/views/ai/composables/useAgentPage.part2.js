/** ai/agent setup part 2. */
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  agentAdd,
  agentDelete,
  agentGetById,
  agentPage,
  agentToolAdd,
  agentToolDelete,
  agentToolPage,
  agentToolUpdate,
  agentUpdate,
  contextConfigAdd,
  contextConfigDelete,
  contextConfigList,
  contextConfigUpdate,
  agentSkillPackages,
  knowledgePage,
  modelListByProvider,
  providerPage,
  routePolicyPage,
  skillAddAgentSkill,
  skillDeleteAgentSkill,
  skillGetAgentSkills,
  skillPage,
  streamAgentChat,
} from '@/api/ai'
import { useDict } from '@/composables/useDict'
import { generateUUID } from '@/utils'
import AgentConfigForm from '../components/AgentConfigForm.vue'
export function applyAgentPagePart2(deps = {}) {
  const {
    __impl, mut, addContextConfig, addTool, backToMarket, buildBaseAgentPayload, createDefaultExtraConfig, createEmptyAgent,
    findAgentByCode, formatRelativeTime, formatTime, getAgentGradient, getAgentInitial, getAgentTools, getContextPreview, getMcpToolLabel,
    getProviderDefaultModel, goToChat, handleAgentPageSizeChange, handleCreateAgent, handleDeleteAgent, handleEditAgent, handleOpenDesigner, handleOpenWorkbenchBaseSettings,
    handlePendingPublishAction, handlePublishAgent, handlePublishMenuUpdate, handleSaveBaseInfo, handleSaveDraft, handleTogglePublish, hydrateAgentForm, loadAgentContexts,
    loadAgentTools, loadAgents, loadModels, loadProviders, loadRoutePolicies, mergeBaseAgentPayload, openBaseModal, openWorkbench,
    parseExtraConfig, parseKnowledgeIds, removeContextConfig, removeTool, resetAgentForm, resetBaseForm, resetModelParams, saveAgent,
    serializeAgent, syncContextConfigs, toIdString, toNullableInteger, toNullableNumber, validateContextConfigs, router, viewMode,
    agentLoading, saveLoading, publishLoading, modelLoading, toolModalVisible, contextModalVisible, agentTools, showAddTool,
    newTool, toolLoading, allSkills, boundSkillIds, skillLoading, skillSaveLoading, knowledgeOptions, knowledgeLoading,
    baseModalVisible, publishMenuVisible, baseSaveLoading, agentFormRef, baseFormRef, messageListRef, agentList, providerList,
    modelList, routePolicyList, contextConfigs, deletedContextIds, expandedContextNames, chatMessages, chatInput, chatSending,
    chatStatusText, sessionId, pagination, filter, baseMode, baseEditingAgent, defaultPrompt, agentForm,
    modelParamEnabled, baseForm, statusFilterOptions, contextTypeOptions, reservedMcpToolOptions, baseRules, agentRules, providerOptions,
    modelOptions, modelSelectionModeOptions, routePolicyOptions, selectedProviderName, selectedModelLabel, pageCount, baseModalTitle, enabledContextCount,
    selectedMcpToolLabels, boundToolCount, suggestedQuestionCount, publishTimeText, openingStatement, previewMessages, suggestedQuestionList, canSendChat,
  } = deps
  const boundSkillPackageLabels = ref([])
  const boundKnowledgeLabels = computed(() => {
    const ids = new Set((agentForm.knowledgeIds || []).map(String))
    return knowledgeOptions.value
      .filter(item => ids.has(String(item.id)))
      .map(item => item.knowledgeName || item.name || item.title)
      .filter(Boolean)
  })

  async function loadBoundSkillPackages() {
    if (!agentForm.id) {
      boundSkillPackageLabels.value = []
      return
    }
    try {
      const res = await agentSkillPackages(agentForm.id)
      const rows = res.code === 200 && Array.isArray(res.data) ? res.data : []
      boundSkillPackageLabels.value = rows.map((row) => {
        const name = row.packageName || row.name || row.packageCode || row.code
        const ver = row.versionName || row.version
        return ver ? `${name} v${ver}` : name
      }).filter(Boolean)
    }
    catch {
      boundSkillPackageLabels.value = []
    }
  }

  async function updateToolEnabled(tool, enabled) {
    const previous = tool.enabled
    tool.enabled = !!enabled
    try {
      await agentToolUpdate({
        ...tool,
        enabled: enabled ? '1' : '0',
        displayTitle: undefined,
        sourceLabel: undefined,
      })
    }
    catch (e) {
      tool.enabled = previous
      window.$message.error(e.message || '更新失败')
    }
  }

  async function loadSkills() {
    skillLoading.value = true
    try {
      const [skillRes, boundRes] = await Promise.all([
        skillPage({ pageNum: 1, pageSize: 200 }),
        skillGetAgentSkills(agentForm.id),
      ])
      allSkills.value = skillRes.data?.records || []
      boundSkillIds.value = (boundRes.data || []).map(s => s.skillId)
    }
    catch {
      allSkills.value = []
      boundSkillIds.value = []
    }
    finally { skillLoading.value = false }
  }

  async function saveSkillBindings() {
    skillSaveLoading.value = true
    try {
      const currentBinds = await skillGetAgentSkills(agentForm.id)
      const currentIds = (currentBinds.data || []).map(s => s.skillId)
      // 新增绑定
      const toAdd = boundSkillIds.value.filter(id => !currentIds.includes(id))
      // 删除绑定
      const toRemove = currentIds.filter(id => !boundSkillIds.value.includes(id))
      for (const skillId of toAdd) {
        await skillAddAgentSkill({ agentId: agentForm.id, skillId })
      }
      for (const skillId of toRemove) {
        await skillDeleteAgentSkill(agentForm.id, skillId)
      }
      window.$message.success('技能绑定已保存')
    }
    catch (e) {
      window.$message.error(e.message || '保存失败')
    }
    finally { skillSaveLoading.value = false }
  }

  async function loadKnowledgeList() {
    knowledgeLoading.value = true
    try {
      // 只取启用状态（status='0'）的知识库作为可选项
      const res = await knowledgePage({ pageNum: 1, pageSize: 200, status: '0' })
      knowledgeOptions.value = res.data?.records || []
    }
    catch {
      knowledgeOptions.value = []
    }
    finally { knowledgeLoading.value = false }
  }

  // 打开工具与技能弹窗时加载数据（不再加载旧版技能）
  watch(toolModalVisible, (visible) => {
    if (visible) {
      loadAgentTools()
      loadKnowledgeList()
      loadBoundSkillPackages()
    }
  })

  watch(() => agentForm.id, (id) => {
    if (id) {
      void loadKnowledgeList()
      void loadBoundSkillPackages()
    }
    else {
      boundSkillPackageLabels.value = []
    }
  })

  function resetConversation() {
    stopChat()
    sessionId.value = generateUUID()
    chatMessages.value = []
    chatStatusText.value = agentForm.status === '0' ? '等待输入' : '发布后可测试'
  }

  function useSuggestedQuestion(question) {
    chatInput.value = question
    sendChatMessage()
  }

  async function sendChatMessage() {
    if (!canSendChat.value) {
      if (agentForm.status !== '0') {
        window.$message.warning('请先发布智能体后再测试')
      }
      return
    }

    const content = chatInput.value.trim()
    const now = formatTime(new Date())
    const assistantMessage = reactive({
      id: generateUUID(),
      role: 'assistant',
      content: '',
      reasoning: '',
      isReasoning: false,
      reasoningTime: null,
      time: now,
      streaming: true,
      toolCalls: [],
      resources: {
        skills: [...boundSkillPackageLabels.value],
        knowledge: [...boundKnowledgeLabels.value],
      },
    })
    const streamId = assistantMessage.id
    const streamState = {
      isReasoning: false,
      reasoningStartTime: null,
    }
    const pendingPayloads = []
    let chunkFlushFrame = null

    function flushPendingChunks() {
      if (chunkFlushFrame !== null) {
        cancelAnimationFrame(chunkFlushFrame)
        chunkFlushFrame = null
      }
      if (mut.activeChatStreamId !== streamId || !pendingPayloads.length) {
        pendingPayloads.length = 0
        return
      }

      const payloads = pendingPayloads.splice(0)
      for (const payload of payloads) {
        handleAgentSSEChunk(payload, assistantMessage, streamState)
      }
      scrollChatToBottom()
    }

    function scheduleChunkFlush(payload) {
      if (mut.activeChatStreamId !== streamId) {
        return
      }
      pendingPayloads.push(payload)
      if (chunkFlushFrame !== null) {
        return
      }
      chunkFlushFrame = requestAnimationFrame(() => {
        chunkFlushFrame = null
        flushPendingChunks()
      })
    }

    chatMessages.value.push({
      id: generateUUID(),
      role: 'user',
      content,
      time: now,
    })
    chatMessages.value.push(assistantMessage)
    chatInput.value = ''
    chatSending.value = true
    mut.activeChatStreamId = streamId
    chatStatusText.value = '生成中'
    await nextTick()
    scrollChatToBottom()

    mut.chatAbortController = streamAgentChat(
      {
        agentCode: agentForm.agentCode,
        message: content,
        userInput: content,
        sessionId: sessionId.value,
        providerId: toIdString(agentForm.providerId) || null,
        modelName: agentForm.modelName,
        temperature: modelParamEnabled.temperature ? toNullableNumber(agentForm.temperature) : null,
        maxTokens: modelParamEnabled.maxTokens ? toNullableInteger(agentForm.maxTokens) : null,
      },
      (eventType, data) => {
        // consumeEventStream 回调签名是 (eventType, data)；包成 { event, data } 交给结构化解析。
        scheduleChunkFlush({ event: eventType, data })
      },
      (data) => {
        flushPendingChunks()
        if (data?.sessionId) {
          sessionId.value = data.sessionId
        }
        finishAgentReasoning(assistantMessage, streamState)
        assistantMessage.streaming = false
        chatSending.value = false
        chatStatusText.value = '完成'
        mut.chatAbortController = null
        mut.activeChatStreamId = null
        scrollChatToBottom()
      },
      (message) => {
        flushPendingChunks()
        finishAgentReasoning(assistantMessage, streamState)
        assistantMessage.streaming = false
        assistantMessage.content = assistantMessage.content || `测试失败：${message}`
        chatSending.value = false
        chatStatusText.value = '测试失败'
        mut.chatAbortController = null
        mut.activeChatStreamId = null
        window.$message.error(message || '测试失败')
        scrollChatToBottom()
      },
    )
  }

  // 消费新引擎（/ai/engine/stream）结构化事件，与主对话页 handleSSEEvent 同协议：
  // THINKING_BLOCK_DELTA→思考(reasoning)，TEXT_BLOCK_START/DELTA→正文(content)，其余更新状态或忽略。
  // payload 由发送处包成 { event, data }（data 已是 JSON.parse 后的对象，增量字段是 data.text）。
  function handleAgentSSEChunk(payload, assistantMessage, streamState) {
    const event = payload?.event || 'message'
    const data = payload?.data || {}

    switch (event) {
      case 'THINKING_BLOCK_DELTA': {
        if (!streamState.isReasoning) {
          streamState.isReasoning = true
          streamState.reasoningStartTime = Date.now()
          assistantMessage.isReasoning = true
        }
        assistantMessage.reasoning += data.text || ''
        chatStatusText.value = '思考中'
        break
      }
      case 'TEXT_BLOCK_START':
        finishAgentReasoning(assistantMessage, streamState)
        break
      case 'TEXT_BLOCK_DELTA':
        finishAgentReasoning(assistantMessage, streamState)
        assistantMessage.content += data.text || ''
        chatStatusText.value = '生成中'
        break
      case 'TOOL_CALL_START':
        if (!Array.isArray(assistantMessage.toolCalls))
          assistantMessage.toolCalls = []
        assistantMessage.toolCalls.push({
          callId: data.toolCallId || '',
          name: data.tool || '工具',
          label: data.label || data.tool || '工具',
          source: data.source || '',
          args: data.args ?? '',
          result: '',
          status: 'running',
          error: '',
        })
        chatStatusText.value = data.label || data.tool ? `调用工具 ${data.label || data.tool}` : '调用工具'
        break
      case 'TOOL_RESULT_END': {
        const tools = assistantMessage.toolCalls || []
        const item = [...tools].reverse().find((t) => {
          if (data.toolCallId && t.callId)
            return t.callId === data.toolCallId
          return t.name === data.tool && t.status === 'running'
        })
        if (item) {
          item.status = data.success === false ? 'error' : 'done'
          item.error = data.success === false ? '工具执行失败' : ''
        }
        break
      }
      case 'TOOL_RESULT_TEXT_DELTA':
      case 'TOOL_RESULT_DATA_DELTA': {
        const tools = assistantMessage.toolCalls || []
        const item = [...tools].reverse().find(t => t.status === 'running')
        if (item)
          item.result = `${item.result || ''}${data.content || ''}`
        break
      }
      case 'HINT_BLOCK':
        if (data.hint)
          window.$message.info(data.hint)
        break
      case 'AGENT_END':
        finishAgentReasoning(assistantMessage, streamState)
        break
      case 'ERROR':
      case 'error':
        if (!assistantMessage.content && (data.message || data.error))
          assistantMessage.content = `测试失败：${data.message || data.error}`
        break
      default:
        // PERSIST_META / AGENT_START / MODEL_CALL_START / MODEL_CALL_END 等：预览无需处理
        break
    }
  }

  function finishAgentReasoning(assistantMessage, streamState) {
    if (!streamState.isReasoning) {
      assistantMessage.isReasoning = false
      return
    }

    streamState.isReasoning = false
    assistantMessage.isReasoning = false
    if (streamState.reasoningStartTime) {
      assistantMessage.reasoningTime = Math.max(1, Math.round((Date.now() - streamState.reasoningStartTime) / 1000))
    }
  }

  function stopChat() {
    if (mut.chatAbortController) {
      mut.chatAbortController.abort()
      mut.chatAbortController = null
    }
    mut.activeChatStreamId = null
    if (mut.chatScrollFrame !== null) {
      cancelAnimationFrame(mut.chatScrollFrame)
      mut.chatScrollFrame = null
    }
    if (chatSending.value) {
      const last = chatMessages.value[chatMessages.value.length - 1]
      if (last) {
        last.streaming = false
        last.isReasoning = false
      }
    }
    chatSending.value = false
  }

  function scrollChatToBottom() {
    if (mut.chatScrollFrame !== null) {
      return
    }
    mut.chatScrollFrame = requestAnimationFrame(() => {
      mut.chatScrollFrame = null
      const el = messageListRef.value
      if (el) {
        scrollLatestReasoningToBottom(el)
        el.scrollTop = el.scrollHeight
      }
    })
  }

  function scrollLatestReasoningToBottom(container) {
    const reasoningContents = container.querySelectorAll('.reasoning-content')
    const latestReasoning = reasoningContents[reasoningContents.length - 1]
    if (latestReasoning) {
      latestReasoning.scrollTop = latestReasoning.scrollHeight
    }
  }

  onMounted(() => {
    loadProviders()
    loadRoutePolicies()
    loadAgents()
  })

  onBeforeUnmount(() => {
    stopChat()
  })
  __impl.updateToolEnabled = updateToolEnabled
  __impl.loadSkills = loadSkills
  __impl.saveSkillBindings = saveSkillBindings
  __impl.loadKnowledgeList = loadKnowledgeList
  __impl.resetConversation = resetConversation
  __impl.useSuggestedQuestion = useSuggestedQuestion
  __impl.sendChatMessage = sendChatMessage
  __impl.handleAgentSSEChunk = handleAgentSSEChunk
  __impl.finishAgentReasoning = finishAgentReasoning
  __impl.stopChat = stopChat
  __impl.scrollChatToBottom = scrollChatToBottom
  __impl.scrollLatestReasoningToBottom = scrollLatestReasoningToBottom

  return {
    ...deps,
  }
}
