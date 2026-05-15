<template>
  <div class="agent-console-page">
    <div v-if="viewMode === 'market'" class="agent-market">
      <div class="agent-toolbar">
        <div>
          <div class="page-title">
            智能体管理
          </div>
          <div class="page-subtitle">
            {{ publishedCount }} 个已发布 · {{ draftCount }} 个草稿
          </div>
        </div>
        <NButton type="primary" size="large" @click="handleCreateAgent">
          <template #icon>
            <i class="ai-icon:plus" />
          </template>
          创建智能体
        </NButton>
      </div>

      <div class="agent-filter">
        <n-input
          v-model:value="filter.keyword"
          clearable
          placeholder="搜索名称、编码或描述"
          class="filter-search"
        >
          <template #prefix>
            <i class="ai-icon:search" />
          </template>
        </n-input>
        <n-select
          v-model:value="filter.status"
          :options="statusFilterOptions"
          class="filter-status"
        />
        <NButton secondary @click="loadAgents">
          <template #icon>
            <i class="ai-icon:refresh-cw" />
          </template>
          刷新
        </NButton>
      </div>

      <n-spin :show="agentLoading">
        <div v-if="pagedAgents.length" class="agent-grid">
          <article v-for="agent in pagedAgents" :key="agent.id" class="agent-card">
            <div class="agent-card-header">
              <div class="agent-avatar" :style="{ background: getAgentGradient(agent.agentCode) }">
                {{ getAgentInitial(agent) }}
              </div>
              <div class="agent-card-title">
                <div class="agent-name-row">
                  <span class="agent-name">{{ agent.agentName }}</span>
                  <NTag :type="agent.status === '0' ? 'success' : 'warning'" size="small" round>
                    {{ agent.status === '0' ? '已发布' : '草稿' }}
                  </NTag>
                </div>
                <div class="agent-code">
                  {{ agent.agentCode }}
                </div>
              </div>
            </div>

            <p class="agent-description">
              {{ agent.description || '暂无描述' }}
            </p>

            <div class="agent-meta-grid">
              <div class="agent-meta-item">
                <span>模型</span>
                <strong>{{ agent.modelName || getProviderDefaultModel(agent.providerId) || '-' }}</strong>
              </div>
              <div class="agent-meta-item">
                <span>温度</span>
                <strong>{{ agent.temperature ?? '0.70' }}</strong>
              </div>
            </div>

            <div class="agent-chip-row">
              <NTag v-for="tool in getAgentTools(agent).slice(0, 3)" :key="tool" size="small" round>
                {{ getMcpToolLabel(tool) }}
              </NTag>
              <NTag v-if="getAgentTools(agent).length > 3" size="small" round>
                +{{ getAgentTools(agent).length - 3 }}
              </NTag>
              <span v-if="!getAgentTools(agent).length" class="muted-chip">未配置 MCP</span>
            </div>

            <div class="agent-card-footer">
              <NButton text type="primary" @click="handleEditAgent(agent)">
                编辑
              </NButton>
              <NButton text @click="handleTestAgent(agent)">
                测试
              </NButton>
              <NButton
                text
                :type="agent.status === '0' ? 'warning' : 'success'"
                @click="handleTogglePublish(agent)"
              >
                {{ agent.status === '0' ? '下线' : '发布' }}
              </NButton>
              <NPopconfirm @positive-click="handleDeleteAgent(agent)">
                <template #trigger>
                  <NButton text type="error">
                    删除
                  </NButton>
                </template>
                确定删除该智能体吗？
              </NPopconfirm>
            </div>
          </article>
        </div>

        <div v-else class="empty-state">
          <i class="ai-icon:message-circle" />
          <span>暂无智能体</span>
        </div>
      </n-spin>

      <div v-if="filteredAgents.length > cardPageSize" class="pagination-wrap">
        <n-pagination
          v-model:page="pagination.page"
          :page-count="pageCount"
          size="small"
        />
      </div>
    </div>

    <div v-else class="agent-workbench">
      <div class="workbench-header">
        <div class="workbench-title-block">
          <NButton quaternary circle @click="backToMarket">
            <template #icon>
              <i class="ai-icon:arrow-left" />
            </template>
          </NButton>
          <div>
            <div class="workbench-title">
              {{ agentForm.id ? agentForm.agentName || '编辑智能体' : '创建智能体' }}
            </div>
            <div class="workbench-subtitle">
              {{ agentForm.agentCode || '未设置编码' }}
            </div>
          </div>
        </div>
        <div class="model-dock">
          <n-select
            v-model:value="agentForm.providerId"
            :options="providerOptions"
            class="model-provider-select"
            clearable
            filterable
            size="small"
            placeholder="供应商"
          />
          <n-select
            v-model:value="agentForm.modelName"
            :options="modelOptions"
            :loading="modelLoading"
            class="model-name-select"
            clearable
            filterable
            tag
            size="small"
            placeholder="模型"
          />
          <n-popover trigger="click" placement="bottom-end">
            <template #trigger>
              <NButton size="small" secondary>
                <template #icon>
                  <i class="ai-icon:tool" />
                </template>
                参数
              </NButton>
            </template>
            <div class="model-param-panel">
              <div class="param-row">
                <div class="param-label">
                  <span>温度</span>
                  <small>控制回答发散程度</small>
                </div>
                <div class="param-control">
                  <n-slider
                    v-model:value="agentForm.temperature"
                    :min="0"
                    :max="1"
                    :step="0.01"
                  />
                  <n-input-number
                    v-model:value="agentForm.temperature"
                    :min="0"
                    :max="1"
                    :step="0.01"
                    :precision="2"
                    size="small"
                    class="param-number"
                  />
                </div>
              </div>
              <div class="param-row">
                <div class="param-label">
                  <span>最大 Token</span>
                  <small>限制单次回复长度</small>
                </div>
                <n-input-number
                  v-model:value="agentForm.maxTokens"
                  :min="256"
                  :step="512"
                  size="small"
                  class="token-param-input"
                />
              </div>
            </div>
          </n-popover>
        </div>
        <div class="workbench-actions">
          <NButton :loading="saveLoading" @click="handleSaveDraft">
            保存
          </NButton>
          <NButton type="primary" :loading="publishLoading" @click="handlePublishAgent">
            发布
          </NButton>
        </div>
      </div>

      <div class="workbench-body">
        <section class="builder-panel">
          <n-scrollbar class="builder-scroll">
            <n-form ref="agentFormRef" :model="agentForm" :rules="agentRules" label-placement="top">
              <div class="panel-section">
                <div class="section-title">
                  <i class="ai-icon:user-check" />
                  <span>人设</span>
                </div>
                <n-grid :cols="2" :x-gap="14">
                  <n-form-item-gi label="智能体名称" path="agentName">
                    <n-input v-model:value="agentForm.agentName" placeholder="如 合同审查助手" />
                  </n-form-item-gi>
                  <n-form-item-gi label="智能体编码" path="agentCode">
                    <n-input
                      v-model:value="agentForm.agentCode"
                      :disabled="!!agentForm.id"
                      placeholder="contract_reviewer"
                    />
                  </n-form-item-gi>
                  <n-form-item-gi :span="2" label="描述" path="description">
                    <n-input
                      v-model:value="agentForm.description"
                      type="textarea"
                      :rows="2"
                      placeholder="用于卡片展示和会话识别"
                    />
                  </n-form-item-gi>
                  <n-form-item-gi :span="2" label="系统提示词" path="systemPrompt">
                    <n-input
                      v-model:value="agentForm.systemPrompt"
                      type="textarea"
                      :autosize="{ minRows: 8, maxRows: 16 }"
                      placeholder="定义智能体角色、目标、边界和输出要求"
                    />
                  </n-form-item-gi>
                  <n-form-item-gi :span="2" label="开场白">
                    <n-input
                      v-model:value="agentForm.extraConfig.openingStatement"
                      type="textarea"
                      :rows="3"
                      placeholder="显示在右侧测试会话的首条消息"
                    />
                  </n-form-item-gi>
                </n-grid>
              </div>

              <div class="panel-section">
                <div class="section-title">
                  <i class="ai-icon:tool" />
                  <span>工具与技能</span>
                </div>
                <n-form-item label="MCP 工具">
                  <n-select
                    v-model:value="agentForm.extraConfig.mcpTools"
                    :options="mcpToolOptions"
                    multiple
                    clearable
                    filterable
                    placeholder="选择智能体可调用的 MCP 工具"
                  />
                </n-form-item>
                <n-form-item label="推荐问题">
                  <n-dynamic-tags v-model:value="agentForm.extraConfig.suggestedQuestions" />
                </n-form-item>
                <n-form-item label="Skill 预留">
                  <n-dynamic-tags v-model:value="agentForm.extraConfig.skills" />
                </n-form-item>
              </div>
            </n-form>
          </n-scrollbar>
        </section>

        <section class="context-panel">
          <div class="context-panel-header">
            <div class="section-title">
              <i class="ai-icon:book-open" />
              <span>上下文</span>
              <NTag size="small" round>
                {{ contextConfigs.length }}
              </NTag>
            </div>
            <NButton size="small" type="primary" secondary @click="addContextConfig">
              <template #icon>
                <i class="ai-icon:plus" />
              </template>
              添加
            </NButton>
          </div>

          <n-scrollbar class="context-collapse-scroll">
            <div v-if="contextConfigs.length" class="context-collapse-wrap">
              <n-collapse v-model:expanded-names="expandedContextNames" class="context-collapse">
                <n-collapse-item
                  v-for="(context, index) in contextConfigs"
                  :key="context.localKey"
                  :name="context.localKey"
                >
                  <template #header>
                    <div class="context-collapse-title">
                      <strong>{{ context.configName || `上下文 ${index + 1}` }}</strong>
                      <small>{{ getContextPreview(context) }}</small>
                    </div>
                  </template>
                  <template #header-extra>
                    <div class="context-collapse-extra" @click.stop>
                      <NTag size="small" :type="context.enabled ? 'success' : 'warning'" round>
                        {{ context.configType || 'SPEC' }}
                      </NTag>
                      <n-switch v-model:value="context.enabled" size="small" />
                      <NPopconfirm @positive-click="removeContextConfig(index)">
                        <template #trigger>
                          <NButton quaternary circle size="small" type="error" @click.stop>
                            <template #icon>
                              <i class="ai-icon:trash-2" />
                            </template>
                          </NButton>
                        </template>
                        确定删除该上下文配置吗？
                      </NPopconfirm>
                    </div>
                  </template>

                  <div class="context-form">
                    <div class="context-field-grid">
                      <div class="context-field context-field-name">
                        <div class="context-field-label">
                          名称
                        </div>
                        <n-input v-model:value="context.configName" size="small" placeholder="配置名称" />
                      </div>
                      <div class="context-field">
                        <div class="context-field-label">
                          类型
                        </div>
                        <n-select v-model:value="context.configType" :options="contextTypeOptions" size="small" />
                      </div>
                      <div class="context-field">
                        <div class="context-field-label">
                          排序
                        </div>
                        <n-input-number v-model:value="context.sort" size="small" :min="0" class="sort-input" />
                      </div>
                    </div>
                    <div class="context-field">
                      <div class="context-field-label">
                        内容
                      </div>
                      <n-input
                        v-model:value="context.configContent"
                        type="textarea"
                        class="context-content-input"
                        :autosize="{ minRows: 7, maxRows: 14 }"
                        placeholder="上下文内容会在调用智能体时注入"
                      />
                    </div>
                  </div>
                </n-collapse-item>
              </n-collapse>
            </div>
            <div v-else class="context-empty">
              暂无上下文配置，点击右上角添加
            </div>
          </n-scrollbar>
        </section>

        <section class="chat-panel">
          <div class="chat-header">
            <div class="chat-agent">
              <div class="chat-avatar" :style="{ background: getAgentGradient(agentForm.agentCode) }">
                {{ getAgentInitial(agentForm) }}
              </div>
              <div>
                <div class="chat-title">
                  实时对话测试
                </div>
                <div class="chat-subtitle">
                  {{ agentForm.status === '0' ? '已发布配置' : '草稿不可调用' }}
                </div>
              </div>
            </div>
            <NButton size="small" tertiary :disabled="chatSending" @click="resetConversation">
              <template #icon>
                <i class="ai-icon:refresh-ccw" />
              </template>
              新会话
            </NButton>
          </div>

          <n-scrollbar ref="messageScrollbarRef" class="chat-scroll">
            <div class="chat-message-list">
              <div
                v-for="message in previewMessages"
                :key="message.id"
                class="chat-message"
                :class="message.role === 'user' ? 'message-user' : 'message-assistant'"
              >
                <div class="message-avatar">
                  <i v-if="message.role === 'assistant'" class="ai-icon:message-circle" />
                  <span v-else>我</span>
                </div>
                  <div class="message-body">
                    <div class="message-meta">
                      <span>{{ message.role === 'user' ? '我' : agentForm.agentName || '智能体' }}</span>
                      <span>{{ message.time }}</span>
                    </div>
                    <div v-if="message.reasoning && message.reasoning.trim()" class="reasoning-section">
                      <div class="reasoning-header">
                        <div class="reasoning-header-left">
                          <i class="ai-icon:brain" />
                          <span>思考过程</span>
                          <small v-if="message.reasoningTime">用时 {{ message.reasoningTime }}s</small>
                          <small v-else-if="message.isReasoning">思考中...</small>
                        </div>
                      </div>
                      <div class="reasoning-content">
                        {{ message.reasoning }}
                      </div>
                    </div>
                    <div v-if="message.content || (message.streaming && !message.isReasoning)" class="message-bubble">
                      <template v-if="message.content">
                        {{ message.content }}
                      </template>
                      <span v-if="message.streaming && !message.isReasoning" class="stream-cursor" />
                    </div>
                  </div>
                </div>
            </div>
          </n-scrollbar>

          <div v-if="suggestedQuestionList.length && !chatMessages.length" class="suggestion-row">
            <button
              v-for="question in suggestedQuestionList"
              :key="question"
              type="button"
              class="suggestion-chip"
              @click="useSuggestedQuestion(question)"
            >
              {{ question }}
            </button>
          </div>

          <div class="chat-composer">
            <n-input
              v-model:value="chatInput"
              type="textarea"
              :autosize="{ minRows: 2, maxRows: 5 }"
              placeholder="输入测试问题"
              :disabled="chatSending"
              @keydown.enter.exact.prevent="sendChatMessage"
            />
            <div class="composer-actions">
              <span class="composer-status">
                {{ chatStatusText }}
              </span>
              <NButton v-if="chatSending" type="warning" @click="stopChat">
                停止
              </NButton>
              <NButton v-else type="primary" :disabled="!canSendChat" @click="sendChatMessage">
                <template #icon>
                  <i class="ai-icon:send" />
                </template>
                发送
              </NButton>
            </div>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import {
  agentAdd,
  agentDelete,
  agentGetById,
  agentPage,
  agentUpdate,
  contextConfigAdd,
  contextConfigDelete,
  contextConfigList,
  contextConfigUpdate,
  modelListByProvider,
  providerPage,
  streamAgentChat,
} from '@/api/ai'
import { generateUUID } from '@/utils'

defineOptions({ name: 'AiAgent' })

const viewMode = ref('market')
const agentLoading = ref(false)
const saveLoading = ref(false)
const publishLoading = ref(false)
const modelLoading = ref(false)
const agentFormRef = ref(null)
const messageScrollbarRef = ref(null)
const agentList = ref([])
const providerList = ref([])
const modelList = ref([])
const contextConfigs = ref([])
const deletedContextIds = ref([])
const expandedContextNames = ref([])
const chatMessages = ref([])
const chatInput = ref('')
const chatSending = ref(false)
const chatStatusText = ref('等待输入')
const sessionId = ref(generateUUID())
const cardPageSize = 12
const pagination = reactive({ page: 1 })
const filter = reactive({ keyword: '', status: 'all' })
const reasoningDelimiter = '==================== 思考过程 ===================='
const answerDelimiter = '==================== 完整回复 ===================='
let chatAbortController = null
let hydratingProvider = false

const defaultPrompt = `你是一个企业级智能体。请根据用户问题给出准确、清晰、可执行的回答。

要求：
1. 优先基于已配置的上下文和工具能力回答。
2. 不确定的信息要说明限制，不要编造。
3. 输出结构清晰，必要时给出步骤或清单。`

const agentForm = reactive(createEmptyAgent())

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

const mcpToolOptions = [
  { label: '知识库检索', value: 'knowledge_search' },
  { label: '数据库查询', value: 'database_query' },
  { label: 'HTTP 请求', value: 'http_request' },
  { label: '文件读取', value: 'file_reader' },
  { label: '工作流触发', value: 'workflow_trigger' },
  { label: '代码执行', value: 'code_executor' },
]

const agentRules = {
  agentName: [{ required: true, message: '请输入智能体名称', trigger: 'blur' }],
  agentCode: [
    { required: true, message: '请输入智能体编码', trigger: 'blur' },
    {
      pattern: /^[a-z][a-z0-9_]{2,49}$/,
      message: '编码需以小写字母开头，仅支持小写字母、数字和下划线',
      trigger: 'blur',
    },
  ],
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

const filteredAgents = computed(() => {
  const keyword = filter.keyword.trim().toLowerCase()
  return agentList.value.filter((agent) => {
    const matchedStatus = filter.status === 'all' || agent.status === filter.status
    const matchedKeyword = !keyword
      || (agent.agentName || '').toLowerCase().includes(keyword)
      || (agent.agentCode || '').toLowerCase().includes(keyword)
      || (agent.description || '').toLowerCase().includes(keyword)
    return matchedStatus && matchedKeyword
  })
})

const pageCount = computed(() => Math.max(1, Math.ceil(filteredAgents.value.length / cardPageSize)))

const pagedAgents = computed(() => {
  const start = (pagination.page - 1) * cardPageSize
  return filteredAgents.value.slice(start, start + cardPageSize)
})

const publishedCount = computed(() => agentList.value.filter(agent => agent.status === '0').length)
const draftCount = computed(() => agentList.value.filter(agent => agent.status !== '0').length)

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
})

watch(() => filter.status, () => {
  pagination.page = 1
})

watch(() => pagination.page, () => {
  if (pagination.page > pageCount.value) {
    pagination.page = pageCount.value
  }
})

watch(() => agentForm.providerId, async (providerId) => {
  const shouldResetModel = !hydratingProvider
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
    temperature: 0.7,
    maxTokens: 4000,
    status: '1',
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
  Object.assign(agentForm, {
    ...createEmptyAgent(),
    ...agent,
    id: toIdString(agent.id) || null,
    providerId: toIdString(agent.providerId) || null,
    modelName: agent.modelName || null,
    temperature: Number(agent.temperature ?? 0.7),
    maxTokens: Number(agent.maxTokens ?? 4000),
    status: agent.status ?? '1',
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

function serializeAgent(status) {
  const extraConfig = {
    ...agentForm.extraConfig,
    contextCount: contextConfigs.value.length,
  }
  return {
    id: agentForm.id,
    agentName: agentForm.agentName,
    agentCode: agentForm.agentCode,
    description: agentForm.description,
    systemPrompt: agentForm.systemPrompt,
    providerId: toIdString(agentForm.providerId) || null,
    modelName: agentForm.modelName,
    temperature: agentForm.temperature,
    maxTokens: agentForm.maxTokens,
    status: status ?? agentForm.status,
    extraConfig: JSON.stringify(extraConfig),
  }
}

async function loadAgents() {
  agentLoading.value = true
  try {
    const res = await agentPage({ pageNum: 1, pageSize: 1000 })
    if (res.code === 200 && res.data) {
      agentList.value = res.data.records || []
    }
  }
  catch (error) {
    window.$message.error(error.message || '加载智能体失败')
  }
  finally {
    agentLoading.value = false
  }
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
  return mcpToolOptions.find(option => option.value === value)?.label || value
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

async function openWorkbench(agent) {
  viewMode.value = 'builder'
  chatInput.value = ''
  hydratingProvider = true
  resetAgentForm(agent)
  await loadModels(agent.providerId)
  hydratingProvider = false
  await loadAgentContexts(agent.agentCode)
  resetConversation()
  await nextTick()
  agentFormRef.value?.restoreValidation?.()
}

async function handleCreateAgent() {
  resetAgentForm()
  contextConfigs.value = []
  deletedContextIds.value = []
  expandedContextNames.value = []
  modelList.value = []
  await openWorkbench(createEmptyAgent())
}

async function handleEditAgent(agent) {
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

async function handleTestAgent(agent) {
  await handleEditAgent(agent)
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
  if (savedAgent) {
    resetAgentForm(savedAgent)
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
  const assistantMessage = {
    id: generateUUID(),
    role: 'assistant',
    content: '',
    reasoning: '',
    isReasoning: false,
    reasoningTime: null,
    time: now,
    streaming: true,
  }
  const streamState = {
    isReasoning: false,
    reasoningStartTime: null,
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
  chatStatusText.value = '生成中'
  await nextTick()
  scrollChatToBottom()

  chatAbortController = streamAgentChat(
    {
      agentCode: agentForm.agentCode,
      message: content,
      userInput: content,
      sessionId: sessionId.value,
      providerId: toIdString(agentForm.providerId) || null,
      modelName: agentForm.modelName,
      temperature: agentForm.temperature,
      maxTokens: agentForm.maxTokens,
    },
    async (payload) => {
      handleAgentSSEChunk(payload, assistantMessage, streamState)
      await nextTick()
      scrollChatToBottom()
    },
    (data) => {
      if (data?.sessionId) {
        sessionId.value = data.sessionId
      }
      finishAgentReasoning(assistantMessage, streamState)
      assistantMessage.streaming = false
      chatSending.value = false
      chatStatusText.value = '完成'
      chatAbortController = null
    },
    (message) => {
      finishAgentReasoning(assistantMessage, streamState)
      assistantMessage.streaming = false
      assistantMessage.content = assistantMessage.content || `测试失败：${message}`
      chatSending.value = false
      chatStatusText.value = '测试失败'
      chatAbortController = null
      window.$message.error(message || '测试失败')
    },
  )
}

function handleAgentSSEChunk(payload, assistantMessage, streamState) {
  const event = typeof payload === 'string' ? 'chunk' : payload?.event
  const data = typeof payload === 'string' ? { content: payload } : payload?.data || {}

  if (event === 'progress') {
    chatStatusText.value = data.message || '生成中'
    return
  }

  const chunkContent = data.content || data.message || ''
  if (!chunkContent) {
    return
  }

  appendAgentChunk(assistantMessage, streamState, chunkContent)
  chatStatusText.value = streamState.isReasoning ? '思考中' : '生成中'
}

function appendAgentChunk(assistantMessage, streamState, chunkContent) {
  let remaining = chunkContent

  while (remaining) {
    const reasoningIndex = remaining.indexOf(reasoningDelimiter)
    const answerIndex = remaining.indexOf(answerDelimiter)
    const nextDelimiter = getNextDelimiter(reasoningIndex, answerIndex)

    if (!nextDelimiter) {
      appendAgentText(assistantMessage, streamState, remaining)
      return
    }

    if (nextDelimiter.index > 0) {
      appendAgentText(assistantMessage, streamState, remaining.slice(0, nextDelimiter.index))
    }

    if (nextDelimiter.type === 'reasoning') {
      streamState.isReasoning = true
      streamState.reasoningStartTime = Date.now()
      assistantMessage.isReasoning = true
      assistantMessage.reasoning = ''
    }
    else {
      finishAgentReasoning(assistantMessage, streamState)
    }

    remaining = remaining.slice(nextDelimiter.index + nextDelimiter.text.length).replace(/^\n+/, '')
  }
}

function appendAgentText(assistantMessage, streamState, text) {
  if (!text) {
    return
  }

  if (streamState.isReasoning) {
    assistantMessage.reasoning += text
  }
  else {
    assistantMessage.content += text
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

function getNextDelimiter(reasoningIndex, answerIndex) {
  const candidates = []
  if (reasoningIndex > -1) {
    candidates.push({ type: 'reasoning', index: reasoningIndex, text: reasoningDelimiter })
  }
  if (answerIndex > -1) {
    candidates.push({ type: 'answer', index: answerIndex, text: answerDelimiter })
  }
  return candidates.sort((a, b) => a.index - b.index)[0] || null
}

function stopChat() {
  if (chatAbortController) {
    chatAbortController.abort()
    chatAbortController = null
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
  const scrollbar = messageScrollbarRef.value
  if (scrollbar?.scrollTo) {
    scrollbar.scrollTo({ top: 999999, behavior: 'smooth' })
  }
}

onMounted(() => {
  loadProviders()
  loadAgents()
})

onBeforeUnmount(() => {
  stopChat()
})
</script>

<style scoped>
.agent-console-page {
  min-height: 100%;
  padding: 24px;
  color: #0f172a;
  overflow: auto;
}

.agent-market {
  width: 100%;
  min-width: 0;
}

.agent-market :deep(.n-spin-content) {
  width: 100%;
}

.agent-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.workbench-header {
  display: grid;
  grid-template-columns: minmax(240px, 1fr) minmax(360px, 560px) auto;
  gap: 14px;
  align-items: center;
  margin-bottom: 18px;
}

.page-title,
.workbench-title {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: 0;
}

.page-subtitle,
.workbench-subtitle,
.chat-subtitle {
  margin-top: 6px;
  color: #64748b;
  font-size: 13px;
}

.agent-filter {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 14px;
  margin-bottom: 18px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(226, 232, 240, 0.85);
  border-radius: 10px;
}

.filter-search {
  max-width: 420px;
}

.filter-status {
  width: 140px;
}

.agent-grid {
  display: grid;
  width: 100%;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.agent-card {
  display: flex;
  min-height: 250px;
  flex-direction: column;
  padding: 18px;
  background: #fff;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 8px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.04);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}

.agent-card:hover {
  border-color: rgba(59, 130, 246, 0.35);
  box-shadow: 0 16px 38px rgba(15, 23, 42, 0.08);
  transform: translateY(-2px);
}

.agent-card-header,
.chat-agent,
.workbench-title-block {
  display: flex;
  align-items: center;
  gap: 12px;
}

.agent-avatar,
.chat-avatar {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  border-radius: 12px;
}

.agent-card-title {
  min-width: 0;
  flex: 1;
}

.agent-name-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.agent-name {
  overflow: hidden;
  font-size: 16px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.agent-code {
  margin-top: 4px;
  color: #64748b;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
}

.agent-description {
  display: -webkit-box;
  min-height: 44px;
  margin: 16px 0;
  overflow: hidden;
  color: #475569;
  font-size: 13px;
  line-height: 1.6;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.agent-meta-grid {
  display: grid;
  grid-template-columns: 1fr 88px;
  gap: 10px;
  margin-bottom: 14px;
}

.agent-meta-item {
  min-width: 0;
  padding: 10px 12px;
  background: #f8fafc;
  border: 1px solid #eef2f7;
  border-radius: 8px;
}

.agent-meta-item span {
  display: block;
  margin-bottom: 4px;
  color: #94a3b8;
  font-size: 12px;
}

.agent-meta-item strong {
  display: block;
  overflow: hidden;
  color: #1e293b;
  font-size: 13px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.agent-chip-row {
  display: flex;
  min-height: 24px;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 16px;
}

.muted-chip {
  color: #94a3b8;
  font-size: 12px;
}

.agent-card-footer {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-top: 12px;
  margin-top: auto;
  border-top: 1px solid #eef2f7;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.empty-state {
  display: flex;
  min-height: 320px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #94a3b8;
  background: #fff;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
}

.empty-state i {
  font-size: 32px;
}

.agent-workbench {
  min-height: calc(100vh - 112px);
}

.model-dock {
  display: grid;
  grid-template-columns: minmax(132px, 0.8fr) minmax(190px, 1.2fr) auto;
  gap: 8px;
  align-items: center;
  justify-self: end;
  width: min(560px, 100%);
  padding: 10px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 8px;
}

.model-provider-select,
.model-name-select {
  min-width: 0;
}

.model-param-panel {
  width: 300px;
  padding: 4px;
}

.param-row {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 10px;
}

.param-row + .param-row {
  border-top: 1px solid #eef2f7;
}

.param-label {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.param-label span {
  color: #0f172a;
  font-size: 13px;
  font-weight: 650;
}

.param-label small {
  color: #94a3b8;
  font-size: 12px;
}

.param-control {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 84px;
  gap: 12px;
  align-items: center;
}

.param-number,
.token-param-input {
  width: 100%;
}

.workbench-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.workbench-body {
  display: grid;
  min-height: calc(100vh - 164px);
  grid-template-columns: minmax(320px, 30%) minmax(320px, 28%) minmax(420px, 1fr);
  gap: 16px;
}

.builder-panel,
.context-panel,
.chat-panel {
  min-height: 0;
  background: #fff;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 8px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.04);
}

.builder-scroll {
  height: calc(100vh - 164px);
}

.panel-section {
  padding: 18px;
  border-bottom: 1px solid #eef2f7;
}

.panel-section:last-child {
  border-bottom: none;
}

.section-title,
.section-title span {
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-title {
  margin-bottom: 14px;
  color: #0f172a;
  font-size: 15px;
  font-weight: 700;
}

.section-title i {
  color: #2563eb;
  font-size: 18px;
}

.context-panel {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 164px);
  overflow: hidden;
}

.context-panel-header {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 16px;
  border-bottom: 1px solid #eef2f7;
}

.context-panel-header .section-title {
  margin-bottom: 0;
}

.context-collapse-scroll {
  flex: 1;
  min-height: 0;
}

.context-collapse-wrap {
  padding: 12px;
}

.context-collapse :deep(.n-collapse-item) {
  overflow: hidden;
  margin-bottom: 10px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.context-collapse :deep(.n-collapse-item:last-child) {
  margin-bottom: 0;
}

.context-collapse :deep(.n-collapse-item__header) {
  align-items: center;
  padding: 12px;
}

.context-collapse :deep(.n-collapse-item__header-main) {
  min-width: 0;
}

.context-collapse :deep(.n-collapse-item__content-wrapper) {
  padding: 0 12px 12px;
}

.context-collapse-title {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.context-collapse-title strong {
  overflow: hidden;
  color: #0f172a;
  font-size: 13px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.context-collapse-title small {
  display: -webkit-box;
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 1;
}

.context-collapse-extra {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-left: 8px;
}

.context-form {
  padding: 12px;
  background: #fff;
  border: 1px solid #eef2f7;
  border-radius: 8px;
}

.context-field-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 108px 86px;
  gap: 10px;
  margin-bottom: 10px;
}

.context-field {
  min-width: 0;
}

.context-field-label {
  margin-bottom: 6px;
  color: #64748b;
  font-size: 12px;
}

.context-content-input {
  width: 100%;
}

.context-content-input :deep(textarea) {
  min-height: 156px;
}

.sort-input {
  width: 100%;
}

.context-empty {
  display: flex;
  min-height: 180px;
  align-items: center;
  justify-content: center;
  margin: 12px;
  padding: 24px;
  color: #94a3b8;
  text-align: center;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
}

.chat-panel {
  display: flex;
  min-width: 0;
  flex-direction: column;
  overflow: hidden;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px;
  border-bottom: 1px solid #eef2f7;
}

.chat-title {
  font-size: 16px;
  font-weight: 700;
}

.chat-scroll {
  min-height: 0;
  flex: 1;
  background: linear-gradient(180deg, #f8fafc 0%, #fff 100%);
}

.chat-message-list {
  display: flex;
  min-height: 100%;
  flex-direction: column;
  gap: 18px;
  padding: 22px;
}

.chat-message {
  display: flex;
  gap: 10px;
  max-width: 84%;
}

.message-user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message-avatar {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  background: #0f172a;
  border-radius: 50%;
}

.message-assistant .message-avatar {
  background: #2563eb;
}

.message-body {
  min-width: 0;
}

.message-meta {
  display: flex;
  gap: 8px;
  margin-bottom: 6px;
  color: #94a3b8;
  font-size: 12px;
}

.message-user .message-meta {
  justify-content: flex-end;
}

.reasoning-section {
  overflow: hidden;
  margin-bottom: 8px;
  color: #475569;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.reasoning-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  border-bottom: 1px solid #e2e8f0;
}

.reasoning-header-left {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  font-size: 12px;
  font-weight: 650;
}

.reasoning-header-left i {
  color: #64748b;
  font-size: 15px;
}

.reasoning-header-left small {
  color: #94a3b8;
  font-weight: 400;
}

.reasoning-content {
  max-height: 180px;
  padding: 10px;
  overflow: auto;
  font-size: 12px;
  line-height: 1.65;
  white-space: pre-wrap;
}

.message-bubble {
  padding: 12px 14px;
  color: #1e293b;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.message-user .message-bubble {
  color: #fff;
  background: #2563eb;
  border-color: #2563eb;
}

.stream-cursor {
  display: inline-block;
  width: 6px;
  height: 16px;
  margin-left: 4px;
  vertical-align: -2px;
  background: currentcolor;
  animation: blink 1s infinite;
}

.suggestion-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px 16px 0;
  border-top: 1px solid #eef2f7;
}

.suggestion-chip {
  max-width: 100%;
  padding: 6px 10px;
  overflow: hidden;
  color: #2563eb;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 999px;
  transition:
    background 0.2s ease,
    border-color 0.2s ease;
}

.suggestion-chip:hover {
  background: #dbeafe;
  border-color: #93c5fd;
}

.chat-composer {
  padding: 16px;
  border-top: 1px solid #eef2f7;
}

.composer-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 10px;
}

.composer-status {
  color: #94a3b8;
  font-size: 12px;
}

@keyframes blink {
  0%,
  45% {
    opacity: 1;
  }
  46%,
  100% {
    opacity: 0;
  }
}

.dark .agent-console-page {
  color: #e2e8f0;
}

.dark .agent-filter,
.dark .agent-card,
.dark .model-dock,
.dark .builder-panel,
.dark .context-panel,
.dark .chat-panel,
.dark .empty-state {
  background: rgba(15, 23, 42, 0.86);
  border-color: rgba(51, 65, 85, 0.8);
}

.dark .page-subtitle,
.dark .workbench-subtitle,
.dark .chat-subtitle,
.dark .agent-code,
.dark .agent-description,
.dark .composer-status,
.dark .message-meta,
.dark .muted-chip {
  color: #94a3b8;
}

.dark .agent-meta-item,
.dark .context-empty,
.dark .context-form,
.dark .reasoning-section,
.dark .context-collapse :deep(.n-collapse-item) {
  background: rgba(30, 41, 59, 0.72);
  border-color: rgba(51, 65, 85, 0.8);
}

.dark .agent-meta-item strong,
.dark .context-collapse-title strong,
.dark .param-label span,
.dark .section-title,
.dark .message-bubble {
  color: #e2e8f0;
}

.dark .panel-section,
.dark .context-panel-header,
.dark .chat-header,
.dark .chat-composer,
.dark .reasoning-header,
.dark .suggestion-row,
.dark .agent-card-footer {
  border-color: rgba(51, 65, 85, 0.8);
}

.dark .param-row + .param-row {
  border-color: rgba(51, 65, 85, 0.8);
}

.dark .context-collapse-title small,
.dark .context-field-label,
.dark .reasoning-content,
.dark .param-label small {
  color: #94a3b8;
}

.dark .chat-scroll {
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.75) 0%, rgba(15, 23, 42, 0.92) 100%);
}

.dark .message-bubble {
  background: rgba(30, 41, 59, 0.92);
  border-color: rgba(51, 65, 85, 0.9);
}

@media (min-width: 1580px) {
  .agent-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 1380px) {
  .workbench-header {
    grid-template-columns: minmax(240px, 1fr) minmax(360px, 560px);
  }

  .workbench-actions {
    grid-column: 2;
    justify-self: end;
  }

  .workbench-body {
    grid-template-columns: minmax(330px, 0.9fr) minmax(430px, 1.1fr);
  }

  .context-panel {
    grid-column: 1;
  }

  .chat-panel {
    grid-column: 2;
    grid-row: 1 / span 2;
  }
}

@media (max-width: 1180px) {
  .agent-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .workbench-header {
    grid-template-columns: 1fr;
  }

  .model-dock {
    justify-self: stretch;
    width: 100%;
  }

  .workbench-actions {
    grid-column: auto;
    justify-self: end;
  }

  .workbench-body {
    grid-template-columns: 1fr;
  }

  .builder-scroll,
  .context-panel {
    height: auto;
    max-height: none;
  }

  .chat-panel {
    grid-column: auto;
    grid-row: auto;
    min-height: 640px;
  }
}

@media (max-width: 760px) {
  .agent-console-page {
    padding: 14px;
  }

  .agent-toolbar,
  .workbench-header,
  .agent-filter {
    align-items: stretch;
    flex-direction: column;
  }

  .filter-search,
  .filter-status {
    width: 100%;
    max-width: none;
  }

  .agent-grid,
  .agent-meta-grid,
  .model-dock,
  .context-field-grid {
    grid-template-columns: 1fr;
  }

  .model-param-panel {
    width: 260px;
  }

  .context-collapse-extra {
    gap: 6px;
  }

  .workbench-actions,
  .composer-actions {
    justify-content: flex-end;
  }

  .chat-message {
    max-width: 100%;
  }
}
</style>
