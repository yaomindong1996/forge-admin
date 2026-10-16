<template>
  <div class="agent-chat-page">
    <!-- 左侧会话列表 -->
    <SessionSidebar
      v-model:keyword="sessionKeyword"
      :sessions="sessions"
      :session-groups="sessionGroups"
      :current-session-id="currentSessionId"
      :agent-options="agentOptions"
      :agent-name-map="agentNameMap"
      :loading-sessions="loadingSessions"
      :has-more-sessions="hasMoreSessions"
      @create="createSession"
      @keyword-input="onKeywordInput"
      @switch="switchSession"
      @toggle-pin="togglePin"
      @rename="openRename"
      @delete="deleteSession"
      @load-more="loadMoreSessions"
    />

    <!-- 右侧对话区域 -->
    <div class="chat-main">
      <!-- Agent 头部信息 -->
      <div class="chat-header">
        <div class="chat-header-left">
          <NButton v-if="agentId" size="small" text title="返回Agent设计器" @click="backToBuilder">
            <template #icon>
              <NIcon><ArrowBackOutline /></NIcon>
            </template>
          </NButton>
          <NAvatar
            v-if="currentAgent"
            round
            :size="30"
            :style="{ background: colorFor(currentAgent.agentCode), color: 'var(--bg-primary)' }"
          >
            <NIcon><SparklesOutline /></NIcon>
          </NAvatar>
          <div v-if="currentAgent" class="chat-header-agent">
            <div class="chat-header-name">
              {{ currentAgent.agentName || currentAgent.agentCode }}
              <NTag v-if="currentAgent.modelName" size="tiny" :bordered="false" type="info" class="model-tag">
                {{ currentAgent.modelName }}
              </NTag>
            </div>
            <div v-if="currentAgent.description" class="chat-header-desc">
              {{ currentAgent.description }}
            </div>
          </div>
        </div>
        <div v-if="currentSessionId" class="chat-header-right">
          <NTag size="small" :type="connectionState.type" :bordered="false" round>
            <span class="conn-dot" :class="`conn-${connectionState.type}`" />
            {{ connectionState.label }}
          </NTag>
        </div>
      </div>

      <template v-if="currentSessionId">
        <!-- 消息区域 -->
        <MessageList
          ref="messageListRef"
          :messages="messages"
          :has-more-messages="hasMoreMessages"
          :loading-more-messages="loadingMoreMessages"
          :is-streaming="isStreaming"
          :current-agent="currentAgent"
          :preset-questions="presetQuestions"
          :last-user-id="lastUserId"
          :last-assistant-id="lastAssistantId"
          :editing-msg-id="editingMsgId"
          :pending-confirm="pendingConfirm"
          :hitl-countdown="hitlCountdown"
          :hitl-timeout="HITL_TIMEOUT_SEC"
          @preset="askPreset"
          @copy="copyMessage"
          @edit-start="startEditMessage"
          @edit-cancel="cancelEdit"
          @edit-confirm="confirmEditResend"
          @delete="deleteMessage"
          @retry="retryMessage"
          @confirm="handleConfirm"
          @cancel-hitl="cancelHitl"
          @load-more-top="loadMoreMessages"
        />

        <!-- 输入区域 -->
        <ChatInput
          ref="chatInputRef"
          v-model="inputText"
          v-model:attachments="attachFileIds"
          :is-streaming="isStreaming"
          @send="handleSend"
          @stop="stopChat"
        />
      </template>

      <div v-else class="empty-state">
        <NIcon size="48" :depth="3">
          <ChatbubblesOutline />
        </NIcon>
        <p>{{ agentOptions.length ? '选择智能体，开始一个新对话' : '暂无可用智能体' }}</p>
        <NDropdown v-if="agentOptions.length" :options="agentOptions" trigger="click" @select="createSession">
          <NButton type="primary" size="small">
            新建对话
          </NButton>
        </NDropdown>
      </div>
    </div>

    <!-- 重命名弹窗（决策 31，标题以服务端为准） -->
    <NModal
      v-model:show="renameVisible"
      preset="dialog"
      title="重命名会话"
      positive-text="确定"
      negative-text="取消"
      @positive-click="confirmRename"
    >
      <NInput v-model:value="renameValue" placeholder="请输入会话名称" maxlength="50" show-count />
    </NModal>
  </div>
</template>

<script setup>
import { ArrowBackOutline, ChatbubblesOutline, SparklesOutline } from '@vicons/ionicons5'
import { NAvatar, NButton, NDropdown, NIcon, NInput, NModal, NTag, useMessage } from 'naive-ui'
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  agentGetById,
  agentList,
  messageDelete,
  sessionCreate,
  sessionDeleteByUser,
  sessionMessagesPageByUser,
  sessionPageByUser,
  sessionPin,
  sessionRename,
} from '@/api/ai'
import { renderMarkdown, renderMarkdownStreaming } from './components/agent-chat/chat-markdown'
import { colorFor, formatSessionBucket, parsePresetQuestions } from './components/agent-chat/chat-utils'
import ChatInput from './components/agent-chat/ChatInput.vue'
import MessageList from './components/agent-chat/MessageList.vue'
import SessionSidebar from './components/agent-chat/SessionSidebar.vue'
import { useAgentChatStream } from './composables/useAgentChatStream'
import 'highlight.js/styles/atom-one-dark.css'

defineOptions({ name: 'AiAgentChat' })

const route = useRoute()
const router = useRouter()
const message = useMessage()

// 会话管理
const sessions = ref([])
const currentSessionId = ref(null)
const messages = ref([])
// 变更B#7：消息级上滑分页——初始只加载最近一页，上滑再拉更早
const MESSAGE_PAGE_SIZE = 30
const hasMoreMessages = ref(false)
const loadingMoreMessages = ref(false)
const inputText = ref('')
const isStreaming = ref(false)
const connectionError = ref(false)
const pendingConfirm = ref(null)
const messageListRef = ref(null)
const chatInputRef = ref(null)
const attachFileIds = ref([])
// 消息内联编辑（仅最新用户消息）：编辑态的消息 id（编辑缓冲由 MessageBubble 自持）
const editingMsgId = ref(null)
const hitlCountdown = ref(0)
let renderTimer = null
let keywordTimer = null
const sessionActionLoading = ref({})

// 最新用户/助手消息 id：用于把「编辑并重发」限定在最新用户消息、「重新生成/继续生成」限定在最新助手消息
const lastUserId = computed(() => {
  for (let i = messages.value.length - 1; i >= 0; i -= 1) {
    if (messages.value[i]?.role === 'user')
      return messages.value[i].id
  }
  return null
})
const lastAssistantId = computed(() => {
  for (let i = messages.value.length - 1; i >= 0; i -= 1) {
    if (messages.value[i]?.role === 'assistant')
      return messages.value[i].id
  }
  return null
})

// 连接/生成状态（建议项6）：页面级实时状态指示，配合每条消息的状态标签
const connectionState = computed(() => {
  if (pendingConfirm.value)
    return { label: '等待确认', type: 'warning' }
  if (isStreaming.value)
    return { label: '生成中', type: 'info' }
  if (connectionError.value)
    return { label: '连接异常', type: 'error' }
  return { label: '就绪', type: 'success' }
})

// 分页
const SESSION_PAGE_SIZE = 20
const sessionPageNum = ref(1)
const sessionTotal = ref(0)
const loadingSessions = ref(false)
const sessionKeyword = ref('')
const hasMoreSessions = computed(() => sessions.value.length < sessionTotal.value)

// Agent 信息
const currentAgent = ref(null)
const agentId = ref(null)
const agents = ref([])

// 智能体下拉项（「新建对话」时选择要绑定的智能体；NDropdown 以 key 作值）
const agentOptions = computed(() =>
  agents.value.map(a => ({ label: a.agentName || a.agentCode, key: a.agentCode })),
)
// agentCode → 展示名映射（会话列表项标注所属智能体，直观区分每个会话绑定的智能体）
const agentNameMap = computed(() =>
  Object.fromEntries(agents.value.map(a => [a.agentCode, a.agentName || a.agentCode])),
)

const sessionGroups = computed(() => {
  const groups = [
    { key: 'pinned', label: '置顶', items: [] },
    { key: 'today', label: '今天', items: [] },
    { key: 'recent', label: '近7天', items: [] },
    { key: 'older', label: '更早', items: [] },
  ]
  sessions.value.forEach((session) => {
    const key = session.pinned ? 'pinned' : formatSessionBucket(session.updateTime || session.createTime)
    groups.find(group => group.key === key)?.items.push(session)
  })
  return groups.filter(group => group.items.length)
})

const presetQuestions = computed(() => parsePresetQuestions(currentAgent.value?.presetQuestions))

// 重命名
const renameVisible = ref(false)
const renameValue = ref('')
const renameTarget = ref(null)

// ============ 滚动辅助（转调 MessageList 暴露的 scrollToBottom） ============
function scrollToBottom(force) {
  messageListRef.value?.scrollToBottom(force)
}

// 流状态、确认计时与终态处理归入独立编排，不再由页面堆叠实现。
const { startStream, handleConfirm, stopChat, cancelHitl, clearHitlTimer } = useAgentChatStream({
  message,
  messages,
  currentAgent,
  currentSessionId,
  isStreaming,
  connectionError,
  pendingConfirm,
  hitlCountdown,
  scrollToBottom,
  scheduleRender,
  flushRender,
})

// 生成 UUID（会话 ID）
function generateSessionId() {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID()
  }
  return `session-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
}

// ============ Agent 加载 ============
async function loadAgent() {
  const id = route.query.agentId
  if (id) {
    agentId.value = Number(id)
    try {
      const res = await agentGetById(agentId.value)
      currentAgent.value = res.data
    }
    catch { /* ignore */ }
  }
  try {
    const res = await agentList()
    agents.value = res.data || []
  }
  catch { /* ignore */ }
}

// 解析当前活跃 Agent 的完整详情（用于欢迎语 / 预设问题 / 模型名）
// 会话绑定的 agentCode 为权威来源：先用列表浅信息占位（保证头部立即正确、杜绝回显上一个 agent），再拉详情补全
async function resolveActiveAgent(agentCode) {
  if (!agentCode) {
    currentAgent.value = null
    return
  }
  // 已是同一个 agent 且详情完整 → 无需重复请求
  if (currentAgent.value?.agentCode === agentCode && currentAgent.value?.greeting !== undefined)
    return
  const item = agents.value.find(a => a.agentCode === agentCode)
  if (!item?.id) {
    // 列表中找不到（可能已删除）→ 用最小信息占位，避免头部回显上一个会话的 agent
    currentAgent.value = { agentCode, agentName: agentCode }
    return
  }
  // 先用列表浅信息占位，头部立即显示正确的 agent；再异步补全 greeting/预设问题
  currentAgent.value = { ...item }
  try {
    const res = await agentGetById(item.id)
    if (res.data)
      currentAgent.value = res.data
  }
  catch { /* 保留浅信息 */ }
}

// ============ 会话列表（分页 + 搜索 + 过滤） ============
async function loadSessions({ reset = false } = {}) {
  if (loadingSessions.value)
    return
  loadingSessions.value = true
  if (reset)
    sessionPageNum.value = 1
  try {
    const res = await sessionPageByUser({
      pageNum: sessionPageNum.value,
      pageSize: SESSION_PAGE_SIZE,
      keyword: sessionKeyword.value || undefined,
    })
    const page = res.data || {}
    const records = Array.isArray(page) ? page : (page.records || [])
    sessionTotal.value = Array.isArray(page) ? records.length : (page.total ?? records.length)
    sessions.value = reset ? records : [...sessions.value, ...records]
  }
  catch (e) {
    message.error(e.message || '加载会话失败')
  }
  finally {
    loadingSessions.value = false
  }
}

function loadMoreSessions() {
  if (loadingSessions.value || !hasMoreSessions.value)
    return
  sessionPageNum.value += 1
  loadSessions({ reset: false })
}

function onKeywordInput() {
  if (keywordTimer)
    clearTimeout(keywordTimer)
  keywordTimer = setTimeout(() => {
    loadSessions({ reset: true })
  }, 350)
}

// ============ 历史消息回放（优先结构化字段，决策 25） ============
// 单条 DB 记录 → 前端消息对象（loadMessages / loadMoreMessages 共用；:key 用全局唯一的 record id）
function mapRecordToMessage(r) {
  if (r.role === 'user') {
    return {
      id: `hist-${r.id}`,
      recordId: r.id,
      role: 'user',
      content: r.content || '',
      attachments: Array.isArray(r.attachments)
        ? r.attachments.map(a => String(a?.fileId ?? a)).filter(v => v && v !== 'null')
        : [],
    }
  }
  const toolCalls = (r.toolCalls || []).map(tc => ({
    name: tc.toolName || tc.tool || '工具',
    args: tc.args,
    result: tc.result,
    status: tc.status || 'done',
    error: tc.error || '',
  }))
  const content = r.content || ''
  return {
    id: `hist-${r.id}`,
    recordId: r.id,
    role: 'assistant',
    content,
    html: content ? renderMarkdown(content) : '',
    thinking: r.reasoning || '',
    toolCalls,
    status: r.status || 'done',
    error: r.error || '',
    firstTokenMs: r.firstTokenMs ?? null,
    totalMs: r.totalMs ?? null,
    tokenUsage: r.tokenUsage ?? null,
    streaming: false,
  }
}

// 初始加载：只取最近一页，标记是否还有更早消息（变更B#7）
async function loadMessages(sessionId) {
  loadingMoreMessages.value = false
  try {
    const res = await sessionMessagesPageByUser(sessionId, { size: MESSAGE_PAGE_SIZE })
    const data = res.data || {}
    messages.value = (data.list || []).map(mapRecordToMessage)
    hasMoreMessages.value = !!data.hasMore
    scrollToBottom(true)
  }
  catch {
    messages.value = []
    hasMoreMessages.value = false
  }
}

// 上滑加载更早消息：以当前最早消息的 recordId 作游标，头插并保持滚动锚点（视口不跳动）
async function loadMoreMessages() {
  if (!hasMoreMessages.value || loadingMoreMessages.value || isStreaming.value)
    return
  const sessionId = currentSessionId.value
  if (!sessionId)
    return
  const earliest = messages.value.find(m => m.recordId != null)
  const beforeId = earliest?.recordId
  if (beforeId == null)
    return
  loadingMoreMessages.value = true
  try {
    const res = await sessionMessagesPageByUser(sessionId, { beforeId, size: MESSAGE_PAGE_SIZE })
    const data = res.data || {}
    const older = (data.list || []).map(mapRecordToMessage)
    hasMoreMessages.value = !!data.hasMore
    if (older.length) {
      messageListRef.value?.beforePrepend?.()
      messages.value = [...older, ...messages.value]
      messageListRef.value?.afterPrepend?.()
    }
  }
  catch {}
  finally {
    loadingMoreMessages.value = false
  }
}

function createAssistantMessage() {
  const assistantMsg = {
    id: `live-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`,
    role: 'assistant',
    content: '',
    html: '',
    thinking: '',
    toolCalls: [],
    status: 'streaming',
    error: '',
    firstTokenMs: null,
    totalMs: null,
    tokenUsage: null,
    _startedAt: null,
    streaming: true,
  }
  messages.value.push(assistantMsg)
  // 关键：push 进响应式数组后，必须取回「响应式代理」再做增量更新。
  // 若继续用 push 前的原始对象（assistantMsg），`.content/.thinking += ...` 会绕过 Proxy 的
  // set 拦截、不触发依赖，直到 isStreaming 等其它响应式变化才整篇刷新 →「不流式、一次性全出」。
  const reactiveMsg = messages.value[messages.value.length - 1]
  return reactiveMsg
}

function backToBuilder() {
  if (agentId.value)
    router.push({ path: '/ai/agent', query: { agentId: agentId.value, mode: 'builder' } })
  else
    router.push('/ai/agent')
}

onMounted(async () => {
  await loadAgent()
  await loadSessions({ reset: true })
  if (currentAgent.value?.agentCode) {
    // 从 Agent 设计器带 agentId 进入：续接该智能体的最近会话，没有则新建一个绑定它的会话
    const existing = sessions.value.find(s => s.agentCode === currentAgent.value.agentCode)
    if (existing)
      await switchSession(existing.id)
    else
      await createSession(currentAgent.value.agentCode)
  }
  else if (sessions.value.length > 0) {
    await switchSession(sessions.value[0].id)
  }
  // 否则保持空态：用户从「新建对话」下拉选择智能体后再创建
})

onUnmounted(() => {
  clearHitlTimer()
  if (renderTimer)
    clearTimeout(renderTimer)
  if (keywordTimer)
    clearTimeout(keywordTimer)
})

// 显式创建会话并落库（决策 29/30）：创建时即选定并绑定智能体，绑定后不可更改
async function createSession(agentCode) {
  const code = agentCode || currentAgent.value?.agentCode || agents.value[0]?.agentCode
  if (!code) {
    message.warning('请先选择一个智能体')
    return
  }
  // 复用当前空会话：仅当它绑定的是同一个智能体（避免重复空「新对话」；换智能体则必然新建）
  const cur = sessions.value.find(s => s.id === currentSessionId.value)
  if (cur && cur.agentCode === code && messages.value.length === 0
    && (!cur.sessionName || cur.sessionName === '新对话')) {
    await resolveActiveAgent(code)
    return
  }
  const id = generateSessionId()
  try {
    const res = await sessionCreate({ sessionId: id, agentCode: code })
    const vo = res.data || { id, sessionName: '新对话', agentCode: code }
    sessions.value.unshift(vo)
    sessionTotal.value += 1
    currentSessionId.value = vo.id || id
    messages.value = []
    hasMoreMessages.value = false
    pendingConfirm.value = null
    await resolveActiveAgent(code)
  }
  catch (e) {
    message.error(e.message || '创建会话失败')
  }
}

async function switchSession(id) {
  if (currentSessionId.value === id)
    return
  currentSessionId.value = id
  pendingConfirm.value = null
  const session = sessions.value.find(s => s.id === id)
  // 始终按会话自身绑定的 agentCode 回显（含为空的历史会话→置空，杜绝沿用上一个会话的 agent）
  await resolveActiveAgent(session?.agentCode)
  await loadMessages(id)
}

async function deleteSession(id) {
  try {
    await sessionDeleteByUser(id)
    sessions.value = sessions.value.filter(s => s.id !== id)
    sessionTotal.value = Math.max(0, sessionTotal.value - 1)
    if (currentSessionId.value === id) {
      currentSessionId.value = sessions.value.length > 0 ? sessions.value[0].id : null
      if (currentSessionId.value)
        await loadMessages(currentSessionId.value)
      else
        messages.value = []
      if (!currentSessionId.value)
        hasMoreMessages.value = false
    }
    message.success('会话已删除')
  }
  catch (e) {
    message.error(e.message || '删除失败')
  }
}

async function togglePin(session) {
  if (!session || sessionActionLoading.value[session.id])
    return
  sessionActionLoading.value = { ...sessionActionLoading.value, [session.id]: true }
  try {
    await sessionPin(session.id, !session.pinned)
    session.pinned = session.pinned ? 0 : 1
    message.success(session.pinned ? '已置顶' : '已取消置顶')
  }
  catch (e) {
    message.error(e.message || '操作失败')
  }
  finally {
    sessionActionLoading.value = { ...sessionActionLoading.value, [session.id]: false }
  }
}

// ============ 重命名（决策 31，服务端截断） ============
function openRename(session) {
  renameTarget.value = session
  renameValue.value = session.sessionName || ''
  renameVisible.value = true
}

async function confirmRename() {
  const target = renameTarget.value
  const name = renameValue.value.trim()
  if (!target || !name) {
    message.warning('请输入会话名称')
    return false
  }
  try {
    await sessionRename(target.id, name)
    target.sessionName = name
    message.success('已重命名')
    return true
  }
  catch (e) {
    message.error(e.message || '重命名失败')
    return false
  }
}

// ============ 发送 / 流式 ============
function createUserMessage(content, attachments) {
  return {
    id: `user-${Date.now()}`,
    role: 'user',
    content,
    attachments: attachments || [],
  }
}

function handleSend() {
  if (!inputText.value.trim() || isStreaming.value)
    return

  const session = sessions.value.find(s => s.id === currentSessionId.value)
  const agentCode = session?.agentCode || currentAgent.value?.agentCode || agents.value[0]?.agentCode
  if (!agentCode) {
    message.warning('请先选择一个智能体')
    return
  }

  const text = inputText.value.trim()
  const images = attachFileIds.value.slice()
  inputText.value = ''
  attachFileIds.value = []
  chatInputRef.value?.clearAttachments?.()

  const isFirstMessage = !messages.value.some(m => m.role === 'user')

  const userMsg = createUserMessage(text, images)
  messages.value.push(userMsg)

  const assistantMsg = createAssistantMessage()
  assistantMsg.prompt = text
  assistantMsg.sessionId = currentSessionId.value

  scrollToBottom(true)
  startStream(text, assistantMsg, agentCode, images)

  // 首条消息触发服务端重命名（决策 31/34：前端传原文，服务端负责截断）
  if (isFirstMessage && session && (!session.sessionName || session.sessionName === '新对话')) {
    session.sessionName = text
    sessionRename(session.id, text).catch(() => { /* 静默，标题以服务端为准 */ })
  }
}

function getLastUserMessage() {
  for (let i = messages.value.length - 1; i >= 0; i -= 1) {
    const item = messages.value[i]
    if (item && item.role === 'user')
      return item
  }
  return null
}

async function deleteMessage(msg) {
  if (editingMsgId.value === msg.id)
    cancelEdit()
  // 未落库的消息（无 recordId）仅在前端移除
  if (!msg.recordId) {
    messages.value = messages.value.filter(item => item.id !== msg.id)
    return
  }
  try {
    await messageDelete(msg.recordId)
    messages.value = messages.value.filter(item => item.id !== msg.id)
    message.success('消息已删除')
  }
  catch (e) {
    message.error(e.message || '删除失败')
  }
}

function retryMessage(msg) {
  if (isStreaming.value)
    return
  const latestAssistant = [...messages.value].reverse().find(item => item.role === 'assistant')
  if (!latestAssistant || latestAssistant.id !== msg.id) {
    message.warning('只能重试最新一条助手消息')
    return
  }
  const index = messages.value.findIndex(item => item.id === msg.id)
  if (index < 0)
    return
  const promptMsg = [...messages.value.slice(0, index)].reverse().find(item => item.role === 'user')
  const prompt = msg.prompt || promptMsg?.content || ''
  if (!prompt) {
    message.warning('找不到可重试的原始提问')
    return
  }
  const agentCode = sessions.value.find(s => s.id === currentSessionId.value)?.agentCode
    || currentAgent.value?.agentCode || agents.value[0]?.agentCode

  msg.error = ''
  msg.status = 'streaming'
  msg.streaming = true
  msg.content = ''
  msg.html = ''
  msg.thinking = ''
  msg.toolCalls = []
  scrollToBottom(true)
  startStream(prompt, msg, agentCode, promptMsg?.attachments || [], { retryOfRecordId: msg.recordId })
}

function startEditMessage(msg) {
  if (isStreaming.value)
    return
  const latestUser = getLastUserMessage()
  if (!latestUser || latestUser.id !== msg.id) {
    message.warning('只能编辑最新一条用户消息')
    return
  }
  editingMsgId.value = msg.id
}

function cancelEdit() {
  editingMsgId.value = null
}

// MessageBubble 内联编辑确认：payload = { msg, text }
function confirmEditResend({ msg, text }) {
  if (isStreaming.value)
    return
  const latestUser = getLastUserMessage()
  if (!latestUser || latestUser.id !== msg.id) {
    message.warning('只能编辑最新一条用户消息')
    return
  }
  const trimmed = (text || '').trim()
  if (!trimmed) {
    message.warning('请输入要重发的内容')
    return
  }
  if (!msg.recordId) {
    message.warning('消息尚未落库，无法编辑重发')
    return
  }
  const agentCode = sessions.value.find(s => s.id === currentSessionId.value)?.agentCode
    || currentAgent.value?.agentCode || agents.value[0]?.agentCode
  const editUserRecordId = msg.recordId
  const attachments = msg.attachments || []

  // 截断：移除被编辑的用户消息及其后的所有消息（编辑仅限最新用户消息，其后至多一条助手回复）
  const index = messages.value.findIndex(item => item.id === msg.id)
  if (index >= 0)
    messages.value.splice(index)

  editingMsgId.value = null

  // 重建：新用户消息 + 新助手占位；后端软删旧行并落新行，recordId 由 PERSIST_META 回填
  const userMsg = createUserMessage(trimmed, attachments)
  messages.value.push(userMsg)
  const assistantMsg = createAssistantMessage()
  assistantMsg.prompt = trimmed
  assistantMsg.sessionId = currentSessionId.value

  scrollToBottom(true)
  startStream(trimmed, assistantMsg, agentCode, attachments, { editUserRecordId })
}

function askPreset(q) {
  if (isStreaming.value)
    return
  inputText.value = q
  handleSend()
}

async function copyMessage(msg) {
  try {
    await navigator.clipboard.writeText(msg.content || '')
    message.success('已复制')
  }
  catch {
    message.error('复制失败')
  }
}

// ============ 渲染节流（决策 22/23） ============
// renderMarkdown（全量）/ renderMarkdownStreaming（增量）已抽到 ./components/agent-chat/chat-markdown，
// 这里只保留与组件状态耦合的渲染节流：renderTimer 合并高频增量，每 tick 增量渲染并滚动到底。
function scheduleRender(msg) {
  if (renderTimer)
    return
  renderTimer = setTimeout(() => {
    renderTimer = null
    msg.html = renderMarkdownStreaming(msg.content)
    scrollToBottom()
  }, 90)
}

function flushRender(msg) {
  if (renderTimer) {
    clearTimeout(renderTimer)
    renderTimer = null
  }
  if (msg)
    msg.html = renderMarkdown(msg.content)
}
</script>

<style scoped>
.agent-chat-page {
  --primary: #2563eb;
  --primary-hover: #1d4ed8;
  --primary-light: #eff4ff;
  --primary-soft: #e0e9ff;
  --border: #e8ebf0;
  --border-strong: #d7dce4;
  --text-strong: #0f172a;
  --text-body: #334155;
  --text-muted: #6b7280;
  --bg-soft: #f8fafc;
  --surface: var(--bg-primary);
  --chat-col: 760px;
  display: flex;
  height: calc(100vh - 120px);
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 16px;
  box-shadow:
    0 1px 2px rgba(15, 23, 42, 0.03),
    0 12px 32px rgba(15, 23, 42, 0.06);
  overflow: hidden;
}

:global(.dark) .agent-chat-page {
  --primary: #3b82f6;
  --primary-hover: #60a5fa;
  --primary-light: rgba(59, 130, 246, 0.12);
  --primary-soft: rgba(59, 130, 246, 0.2);
  --border: #26303f;
  --border-strong: #33415a;
  --text-strong: #f1f5f9;
  --text-body: #cbd5e1;
  --text-muted: #8695a8;
  --bg-soft: #0f1826;
  --surface: #131c2b;
  background: var(--surface);
  border-color: var(--border);
}

:global(.agent-chat-page ::selection) {
  background: rgba(37, 99, 235, 0.18);
}

/* ============ 右侧对话区 ============ */
.chat-main {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 22px;
  border-bottom: 1px solid var(--border);
  background: var(--surface);
}

.chat-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.chat-header-right {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.conn-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-right: 5px;
  vertical-align: middle;
  background: currentColor;
}

.conn-info {
  animation: conn-pulse 1.2s ease-in-out infinite;
}

@keyframes conn-pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.3;
  }
}

.chat-header-agent {
  min-width: 0;
}

.chat-header-name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-strong);
}

.model-tag {
  font-weight: 400;
}

.chat-header-desc {
  max-width: 460px;
  margin-top: 1px;
  overflow: hidden;
  color: var(--text-muted);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-state {
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--text-muted);
}

.empty-state p {
  font-size: 14px;
}
</style>
