<template>
  <div class="go-ai-chat-panel">
    <div class="messages-container" ref="messagesContainerRef">
      <div v-if="aiStore.getChatMessages.length === 0" class="empty-state">
        <n-icon size="40" color="#51d6a9" style="margin-bottom: 12px">
          <SparklesIcon />
        </n-icon>
        <p class="empty-title">Forge AI 助手</p>
        <p class="empty-desc">描述你想要的数据大屏，AI 将帮你自动生成</p>
        <div class="quick-prompts">
          <n-button
            v-for="prompt in quickPrompts"
            :key="prompt"
            size="small"
            secondary
            class="quick-btn"
            @click="useQuickPrompt(prompt)"
          >{{ prompt }}</n-button>
        </div>
      </div>

      <template v-else>
        <div
          v-for="(msg, index) in aiStore.getChatMessages"
          :key="msg.id"
          class="message-item"
          :class="msg.role"
        >
          <div v-if="msg.role === 'assistant'" class="avatar ai-avatar">
            <n-icon size="14" color="#51d6a9"><SparklesIcon /></n-icon>
          </div>
          <div class="bubble-wrapper">
            <div v-if="msg.role === 'assistant' && msg.reasoning?.trim()" class="reasoning-section">
              <div class="reasoning-header" @click="toggleReasoning(index)">
                <div class="reasoning-header-left">
                  <n-icon size="14" color="#51d6a9"><SparklesIcon /></n-icon>
                  <span>思考过程</span>
                  <span v-if="msg.reasoningTime" class="reasoning-duration">用时 {{ msg.reasoningTime }}s</span>
                  <span v-else-if="msg.isReasoning" class="reasoning-duration thinking">思考中...</span>
                </div>
                <span class="reasoning-toggle" :class="{ expanded: expandedReasonings[index] }">⌄</span>
              </div>
              <div
                v-if="expandedReasonings[index]"
                :ref="el => setReasoningContentRef(el, index)"
                class="reasoning-content"
              >
                {{ msg.reasoning }}
              </div>
            </div>
            <div class="bubble" :class="msg.role">
              <div
                v-if="msg.role === 'assistant' && msg.canvasResponse"
                class="generate-result-card"
              >
                <div class="generate-result-icon">
                  <n-icon size="18"><AnalyticsIcon /></n-icon>
                </div>
                <div class="generate-result-main">
                  <div class="generate-result-title">
                    大屏生成完成
                  </div>
                  <div class="generate-result-name">
                    {{ msg.canvasResponse.title || '未命名大屏' }}
                  </div>
                  <div class="generate-result-meta">
                    <span>{{ msg.canvasResponse.components?.length || 0 }} 个组件</span>
                    <span>可应用到当前画布</span>
                  </div>
                </div>
              </div>
              <div
                v-else-if="msg.role === 'assistant' && msg.streaming && msg.progressSteps?.length"
                class="generate-progress-card"
              >
                <div class="generate-progress-title">
                  AI 正在生成大屏
                </div>
                <div class="generate-progress-list">
                  <div
                    v-for="step in msg.progressSteps"
                    :key="step.key"
                    class="generate-progress-step"
                    :class="step.status"
                  >
                    <span class="generate-progress-dot" />
                    <span class="generate-progress-label">{{ step.label }}</span>
                  </div>
                </div>
              </div>
              <span v-else class="msg-content" v-html="renderContent(msg.content)"></span>
              <span v-if="msg.streaming" class="typing-cursor">|</span>
            </div>
            <div v-if="msg.role === 'assistant' && !msg.streaming && msg.canvasResponse" class="msg-actions">
              <n-button size="small" type="primary" ghost @click="applyToCanvas(msg.canvasResponse!)">
                <template #icon><n-icon><AnalyticsIcon /></n-icon></template>
                应用到画布
              </n-button>
            </div>
          </div>
          <div v-if="msg.role === 'user'" class="avatar user-avatar">
            <n-icon size="14"><PersonIcon /></n-icon>
          </div>
        </div>

        <div v-if="aiStore.getGenerating && !hasStreamingMessage" class="message-item assistant">
          <div class="avatar ai-avatar">
            <n-icon size="14" color="#51d6a9"><SparklesIcon /></n-icon>
          </div>
          <div class="bubble assistant">
            <span class="thinking-dots">
              <span></span><span></span><span></span>
            </span>
          </div>
        </div>
      </template>
    </div>

    <div class="style-row">
      <span class="style-label">风格：</span>
      <n-radio-group v-model:value="styleRef" size="small" :disabled="aiStore.getGenerating">
        <n-radio-button value="dark">深色</n-radio-button>
        <n-radio-button value="light">浅色</n-radio-button>
      </n-radio-group>
      <n-tooltip placement="top" trigger="hover">
        <template #trigger>
          <n-button size="tiny" quaternary style="margin-left: 8px" @click="showModeSelect = !showModeSelect">
            <template #icon><n-icon><SettingsSharpIcon /></n-icon></template>
          </n-button>
        </template>
        模式设置
      </n-tooltip>
    </div>

    <n-collapse-transition :show="showModeSelect">
      <div class="mode-row">
        <span class="style-label">模式：</span>
        <n-radio-group v-model:value="chatModeRef" size="small">
          <n-radio-button value="generate">生成大屏</n-radio-button>
          <n-radio-button value="chat">自由对话</n-radio-button>
        </n-radio-group>
      </div>

      <div class="config-grid">
        <div class="config-item config-provider">
          <span class="style-label">供应商：</span>
          <n-select
            v-model:value="selectedProviderId"
            size="small"
            :options="providerOptions"
            :loading="providerLoading"
            :disabled="aiStore.getGenerating || providerOptions.length === 0"
            placeholder="请选择供应商"
          />
        </div>

        <div class="config-item config-model">
          <span class="style-label">模型：</span>
          <n-select
            v-model:value="selectedModelName"
            size="small"
            :options="modelOptions"
            :disabled="aiStore.getGenerating || !selectedProvider"
            placeholder="请选择模型"
            filterable
            tag
          />
        </div>

        <div class="config-item config-temperature">
          <span class="style-label">温度：</span>
          <n-input-number
            v-model:value="temperatureRef"
            size="small"
            :min="0"
            :max="2"
            :step="0.1"
            :precision="1"
            :disabled="aiStore.getGenerating"
          />
        </div>

        <div class="config-item config-max-tokens">
          <span class="style-label">Max Tokens：</span>
          <n-input-number
            v-model:value="maxTokensRef"
            size="small"
            :min="1"
            :step="100"
            clearable
            :disabled="aiStore.getGenerating"
            placeholder="可选"
          />
        </div>
      </div>

      <div v-if="selectedProvider" class="provider-tip">
        当前使用：{{ selectedProvider.providerName }}
        <span v-if="selectedModelName"> / {{ selectedModelName }}</span>
      </div>
    </n-collapse-transition>

    <div v-if="referencedComponents.length" class="selection-context active">
      <div class="selection-context-main">
        <span class="selection-context-icon">
          <n-icon size="14"><LayersIcon /></n-icon>
        </span>
        <div class="selection-context-info">
          <div class="selection-context-title">
            已引用 {{ referencedComponents.length }} 个选中元素
          </div>
          <div class="selection-context-name">
            {{ referencedComponentSummary }}
          </div>
        </div>
      </div>
      <n-tooltip placement="top" trigger="hover">
        <template #trigger>
          <n-button size="tiny" quaternary circle @click="clearReferencedComponents">
            <template #icon><n-icon><CloseIcon /></n-icon></template>
          </n-button>
        </template>
        取消引用
      </n-tooltip>
    </div>

    <div class="input-area">
      <n-input
        v-model:value="inputRef"
        type="textarea"
        :placeholder="inputPlaceholder"
        :rows="5"
        :disabled="aiStore.getGenerating"
        @keydown.enter.exact.prevent="handleSend"
        @keydown.shift.enter.prevent="inputRef += '\n'"
      />
      <div class="input-footer">
        <div class="input-tools">
          <n-tooltip placement="top" trigger="hover">
            <template #trigger>
              <n-button
                class="reference-btn"
                :class="{ active: referencePickMode }"
                size="small"
                quaternary
                circle
                :disabled="aiStore.getGenerating"
                @click="toggleReferencePickMode"
              >
                <template #icon> <img src="~@/assets/images/Click.png" alt="" style="width: 20px"/></template>
              </n-button>
            </template>
            {{ referencePickMode ? '关闭引用模式' : '开启引用模式' }}
          </n-tooltip>
          <span class="hint-text">Enter 发送，Shift+Enter 换行</span>
        </div>
        <n-button v-if="aiStore.getGenerating" type="error" size="small" @click="handleStop">
          <template #icon><n-icon><CloseIcon /></n-icon></template>
          停止生成
        </n-button>
        <n-button
          v-else
          type="primary"
          size="small"
          :disabled="!inputRef.trim() || !selectedProviderId"
          @click="handleSend"
        >
          <template #icon><n-icon><SendIcon /></n-icon></template>
          发送
        </n-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, toRaw, watch } from 'vue'
import {
  type AiProvider,
  aiChatStream,
  aiGenerateStream,
  getProviderPageApi
} from '@/api/ai'
import type { AIGenerateResponse } from '@/api/ai/ai.d'
import { applyAIToCanvas } from './aiEngine'
import { parseStreamedResponse } from './llmClient'
import { getComponentCatalogText } from './componentRegistry'
import { icon } from '@/plugins'
import type { CreateComponentGroupType, CreateComponentType } from '@/packages/index.d'
import { useAIStore } from '@/store/modules/aiStore/aiStore'
import { useChartEditStore } from '@/store/modules/chartEditStore/chartEditStore'

const { SparklesIcon, SendIcon, AnalyticsIcon, PersonIcon, SettingsSharpIcon, CloseIcon, LayersIcon, Click } = icon.ionicons5
const { Carbon3DCursorIcon } = icon.carbon

const emit = defineEmits(['applied'])

const aiStore = useAIStore()
const chartEditStore = useChartEditStore()

const inputRef = ref('')
const styleRef = ref<'dark' | 'light'>('dark')
const chatModeRef = ref<'generate' | 'chat'>('generate')
const showModeSelect = ref(false)
const messagesContainerRef = ref<HTMLElement>()
const reasoningContentRefs = ref<HTMLElement[]>([])
const providerList = ref<AiProvider[]>([])
const providerLoading = ref(false)
const chatSessionIdRef = ref(aiStore.currentSessionId || createSessionId())
const selectedProviderId = ref<number | string | null>(aiStore.getSelectedProvider?.providerId ?? null)
const selectedModelName = ref(aiStore.getSelectedProvider?.modelName || '')
const temperatureRef = ref(aiStore.getSelectedProvider?.temperature ?? 0.7)
const maxTokensRef = ref<number | null>(aiStore.getSelectedProvider?.maxTokens ?? 384000)

const quickPrompts = [
  '电商销售数据监控大屏',
  '智慧城市运营中心大屏',
  '工厂生产数据监控大屏',
  '财务数据分析大屏'
]

const GENERATE_PROGRESS_STEPS = [
  { key: 'understand', label: '理解需求' },
  { key: 'layout', label: '规划布局' },
  { key: 'charts', label: '生成组件' },
  { key: 'detail', label: '完善配置' },
  { key: 'verify', label: '校验结果' }
]

const hasStreamingMessage = computed(() => aiStore.getChatMessages.some(message => message.streaming))
const referencedComponentIds = ref<string[]>([])
const referencePickMode = ref(false)
const currentSelectedComponentIds = computed(() => sortSelectedComponentIds(chartEditStore.getTargetChart.selectId))
const referencedComponents = computed(() =>
  referencedComponentIds.value
    .map(id => findComponentById(id, chartEditStore.getComponentList))
    .filter(Boolean) as Array<CreateComponentType | CreateComponentGroupType>
)
const referencedComponentSummary = computed(() => {
  const names = referencedComponents.value.map(item => getComponentDisplayName(item))
  if (names.length <= 2) return names.join('、')
  return `${names.slice(0, 2).join('、')} 等`
})
const inputPlaceholder = computed(() => {
  if (referencedComponents.value.length) return '针对已引用元素提问，比如：帮我优化这个模块的配色和层级...'
  return chatModeRef.value === 'generate' ? '描述你想要的数据大屏...' : '有什么可以帮你？'
})
const expandedReasonings = ref<Record<number, boolean>>({})
const aiRawContent = ref('')
const aiReasoningContent = ref('')
const aiIsReasoningPhase = ref(false)
const aiReasoningStartTime = ref<number | null>(null)
const aiReasoningEndTime = ref<number | null>(null)

type AIReferencedComponent = {
  id: string
  key?: string
  title?: string
  category?: string
  isGroup?: boolean
  attr?: Record<string, any>
  styles?: Record<string, any>
  status?: Record<string, any>
  request?: Record<string, any>
  option?: Record<string, any>
  children?: AIReferencedComponent[]
}

function createSessionId() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return `ai-session-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function parseModels(models?: string) {
  if (!models) return []
  const trimmed = models.trim()
  if (!trimmed) return []

  try {
    const parsed = JSON.parse(trimmed)
    if (Array.isArray(parsed)) {
      return parsed.map(item => String(item).trim()).filter(Boolean)
    }
  } catch {
    // ignore invalid json
  }

  return trimmed
    .split(/[\n,，]/)
    .map(item => item.trim())
    .filter(Boolean)
}

const providerOptions = computed(() =>
  providerList.value.map(item => ({
    label: `${item.providerName || '未命名供应商'}${item.isDefault === '1' ? '（默认）' : ''}`,
    value: item.id as number | string
  }))
)

const selectedProvider = computed(() =>
  providerList.value.find(item => String(item.id) === String(selectedProviderId.value)) || null
)

const modelOptions = computed(() => {
  if (!selectedProvider.value) return []
  const models = parseModels(selectedProvider.value.models)
  const options = models.map(model => ({ label: model, value: model }))
  if (!options.length && selectedProvider.value.defaultModel) {
    options.push({ label: selectedProvider.value.defaultModel, value: selectedProvider.value.defaultModel })
  }
  return options
})

const scrollToBottom = async () => {
  await nextTick()
  scrollActiveReasoningToBottom()
  if (messagesContainerRef.value) {
    messagesContainerRef.value.scrollTop = messagesContainerRef.value.scrollHeight
  }
  requestAnimationFrame(() => {
    scrollActiveReasoningToBottom()
    if (messagesContainerRef.value) {
      messagesContainerRef.value.scrollTop = messagesContainerRef.value.scrollHeight
    }
  })
}

watch(() => aiStore.getChatMessages.length, scrollToBottom)
watch(
  () => aiStore.getChatMessages[aiStore.getChatMessages.length - 1]?.content,
  scrollToBottom
)

watch(
  () => aiStore.getChatMessages[aiStore.getChatMessages.length - 1]?.reasoning,
  scrollToBottom
)

watch(
  selectedProvider,
  provider => {
    if (!provider) {
      selectedModelName.value = ''
      return
    }
    const models = parseModels(provider.models)
    if (!selectedModelName.value) {
      selectedModelName.value = provider.defaultModel || models[0] || ''
      return
    }
    if (models.length && !models.includes(selectedModelName.value)) {
      selectedModelName.value = provider.defaultModel || models[0] || selectedModelName.value
    }
  },
  { immediate: true }
)

watch([selectedProviderId, selectedModelName], ([newProviderId, newModelName], [oldProviderId, oldModelName]) => {
  if (newProviderId !== oldProviderId || newModelName !== oldModelName) {
    chatSessionIdRef.value = createSessionId()
  }
})

watch([selectedProviderId, selectedModelName, temperatureRef, maxTokensRef, providerList], () => {
  if (!selectedProvider.value) {
    aiStore.setSelectedProvider(null)
    return
  }
  aiStore.setSelectedProvider({
    providerId: selectedProviderId.value || undefined,
    providerName: selectedProvider.value.providerName || '未命名供应商',
    modelName: selectedModelName.value || undefined,
    temperature: temperatureRef.value,
    maxTokens: maxTokensRef.value
  })
})

function renderContent(content: string): string {
  if (!content) return ''
  let escaped = content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')

  escaped = escaped.replace(/```[\s\S]*?```/g, match => {
    return `<pre class="code-block">${match.replace(/```\w*\n?/g, '').replace(/```/g, '')}</pre>`
  })
  escaped = escaped.replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>')
  escaped = escaped.replace(/\n/g, '<br/>')
  return escaped
}

function useQuickPrompt(prompt: string) {
  inputRef.value = prompt
  chatModeRef.value = 'generate'
}

function clearReferencedComponents() {
  referencedComponentIds.value = []
  referencePickMode.value = false
}

function toggleReferencePickMode() {
  referencePickMode.value = !referencePickMode.value
  if (!referencePickMode.value) {
    clearReferencedComponents()
  } else if (currentSelectedComponentIds.value.length) {
    referencedComponentIds.value = [...currentSelectedComponentIds.value]
  }
}

watch(currentSelectedComponentIds, (newIds) => {
  if (!referencePickMode.value) return
  referencedComponentIds.value = [...newIds]
})

function toggleReferencedComponents() {
  if (referencedComponentIds.value.length) {
    clearReferencedComponents()
    return
  }

  if (!currentSelectedComponentIds.value.length) {
    window['$message']?.warning('请先在右侧画布选中要引用的元素')
    return
  }

  referencedComponentIds.value = [...currentSelectedComponentIds.value]
}

function findComponentById(
  id: string,
  list: Array<CreateComponentType | CreateComponentGroupType>
): CreateComponentType | CreateComponentGroupType | undefined {
  for (const item of list) {
    if (item.id === id) return item
    if (item.isGroup && item.groupList?.length) {
      const target = findComponentById(id, item.groupList)
      if (target) return target
    }
  }
  return undefined
}

function sortSelectedComponentIds(ids: string[]) {
  if (!ids.length) return []
  const selectedIdSet = new Set(ids)
  const sortedIds: string[] = []

  const collect = (list: Array<CreateComponentType | CreateComponentGroupType>) => {
    list.forEach(item => {
      if (selectedIdSet.has(item.id)) sortedIds.push(item.id)
      if (item.isGroup && item.groupList?.length) collect(item.groupList)
    })
  }

  collect(chartEditStore.getComponentList)
  return sortedIds
}

function getComponentDisplayName(component: CreateComponentType | CreateComponentGroupType) {
  return component.chartConfig?.title || component.key || component.id || '未命名元素'
}

function buildReferencedComponent(component: CreateComponentType | CreateComponentGroupType): AIReferencedComponent {
  const rawComponent = toRaw(component) as CreateComponentType | CreateComponentGroupType
  const referenced: AIReferencedComponent = {
    id: rawComponent.id,
    key: rawComponent.key || rawComponent.chartConfig?.chartKey,
    title: rawComponent.chartConfig?.title,
    category: rawComponent.chartConfig?.categoryName || rawComponent.chartConfig?.category,
    isGroup: rawComponent.isGroup,
    attr: rawComponent.attr,
    styles: rawComponent.styles,
    status: rawComponent.status,
    request: rawComponent.request,
    option: rawComponent.option
  }

  if (rawComponent.isGroup && rawComponent.groupList?.length) {
    referenced.children = rawComponent.groupList.map(item => buildReferencedComponent(item))
  }

  return referenced
}

function buildSelectedComponentInstruction() {
  if (!referencedComponents.value.length) return ''
  const titles = referencedComponents.value.map(item => `${getComponentDisplayName(item)}(${item.id})`).join('、')
  return [
    '当前用户已经在右侧画布选中并引用了以下元素，请优先围绕这些元素回答：',
    titles,
    '如果用户要求调整或优化，请基于 selectedComponents 中的组件配置给出针对性建议；不要忽略当前引用元素。'
  ].join('\n')
}

function toggleReasoning(index: number) {
  expandedReasonings.value[index] = !expandedReasonings.value[index]
  scrollToBottom()
}

function setReasoningContentRef(el: Element | null, index: number) {
  if (el instanceof HTMLElement) {
    reasoningContentRefs.value[index] = el
  } else {
    delete reasoningContentRefs.value[index]
  }
}

function scrollActiveReasoningToBottom() {
  const activeIndex = aiStore.getChatMessages.findLastIndex(msg => msg.role === 'assistant' && msg.isReasoning)
  const reasoningEl = reasoningContentRefs.value[activeIndex]
  if (reasoningEl) {
    reasoningEl.scrollTop = reasoningEl.scrollHeight
  }
}

function resetStreamingState() {
  aiRawContent.value = ''
  aiReasoningContent.value = ''
  aiIsReasoningPhase.value = false
  aiReasoningStartTime.value = null
  aiReasoningEndTime.value = null
}

function getReasoningTime() {
  if (!aiReasoningStartTime.value)
    return null
  const endTime = aiReasoningEndTime.value || Date.now()
  return Math.max(1, Math.round((endTime - aiReasoningStartTime.value) / 1000))
}

function updateAssistantStreaming(content: string, canvasResponse?: AIGenerateResponse | null) {
  aiStore.updateLastAssistantMessage(content, canvasResponse, {
    reasoning: aiReasoningContent.value,
    isReasoning: aiIsReasoningPhase.value,
    reasoningTime: aiReasoningContent.value ? getReasoningTime() : null,
    progressSteps: getGenerateProgressSteps(aiRawContent.value)
  })
  const lastMessageIndex = aiStore.getChatMessages.length - 1
  const lastMsg = aiStore.getChatMessages[lastMessageIndex]
  if (lastMsg?.role === 'assistant') {
    lastMsg.streaming = aiStore.getGenerating
    if (aiIsReasoningPhase.value) {
      expandedReasonings.value[lastMessageIndex] = true
    }
  }
}

function appendAnswerContent(text: string, mode: 'generate' | 'chat') {
  if (!text)
    return
  aiRawContent.value += text
  const displayContent = mode === 'generate'
    ? buildGenerateStreamingPreview(aiRawContent.value)
    : aiRawContent.value
  updateAssistantStreaming(displayContent, null)
}

function getGenerateProgressSteps(fullText: string) {
  const activeCount = Math.min(
    GENERATE_PROGRESS_STEPS.length,
    Math.max(1, Math.ceil((fullText.trim().length || 1) / 90))
  )
  return GENERATE_PROGRESS_STEPS.map((step, index) => ({
    ...step,
    status: index < activeCount - 1 ? 'done' : index === activeCount - 1 ? 'active' : 'pending'
  }))
}

function consumeReasoningAwareChunk(chunk: string, mode: 'generate' | 'chat') {
  if (!chunk)
    return

  const reasoningDelimiter = '==================== 思考过程 ===================='
  const answerDelimiter = '==================== 完整回复 ===================='
  let remaining = chunk

  while (remaining) {
    const reasoningIndex = remaining.indexOf(reasoningDelimiter)
    const answerIndex = remaining.indexOf(answerDelimiter)

    if (reasoningIndex >= 0 && (answerIndex < 0 || reasoningIndex < answerIndex)) {
      appendAnswerContent(remaining.slice(0, reasoningIndex), mode)
      aiIsReasoningPhase.value = true
      aiReasoningContent.value = ''
      aiReasoningStartTime.value = Date.now()
      aiReasoningEndTime.value = null
      updateAssistantStreaming('', null)
      remaining = remaining.slice(reasoningIndex + reasoningDelimiter.length).replace(/^\s*\n?/, '')
      continue
    }

    if (answerIndex >= 0) {
      const beforeAnswer = remaining.slice(0, answerIndex)
      if (aiIsReasoningPhase.value) {
        aiReasoningContent.value += beforeAnswer
        aiIsReasoningPhase.value = false
        aiReasoningEndTime.value = Date.now()
      } else {
        appendAnswerContent(beforeAnswer, mode)
      }
      remaining = remaining.slice(answerIndex + answerDelimiter.length).replace(/^\s*\n?/, '')
      continue
    }

    if (aiIsReasoningPhase.value) {
      aiReasoningContent.value += remaining
      updateAssistantStreaming('', null)
    } else {
      appendAnswerContent(remaining, mode)
    }
    break
  }
}

function extractAnswerContent(fullText: string) {
  if (aiRawContent.value)
    return aiRawContent.value
  const answerDelimiter = '==================== 完整回复 ===================='
  if (fullText.includes(answerDelimiter)) {
    return fullText.split(answerDelimiter).pop()?.trim() || ''
  }
  return fullText || ''
}

function getCanvasSize() {
  try {
    const config = chartEditStore.getEditCanvasConfig
    if (config?.width && config?.height) {
      return { width: config.width, height: config.height }
    }
  } catch {
    // ignore
  }
  return { width: 1920, height: 1080 }
}

function buildCanvasContext() {
  try {
    const canvasConfig = chartEditStore.getEditCanvasConfig
    const componentList = chartEditStore.getComponentList || []
    const referencedComponentContext = referencedComponents.value.map(item => buildReferencedComponent(item))

    const existingLayouts = componentList.map(item => {
      const component = toRaw(item) as CreateComponentType | CreateComponentGroupType
      return {
        key: component.key || component.chartConfig?.chartKey,
        title: component.chartConfig?.title || '',
        x: component.attr?.x,
        y: component.attr?.y,
        w: component.attr?.w,
        h: component.attr?.h
      }
    })

    return JSON.stringify(
      {
        canvas: {
          projectName: canvasConfig?.projectName,
          width: canvasConfig?.width,
          height: canvasConfig?.height,
          background: canvasConfig?.background || canvasConfig?.backgroundColor
        },
        selectedComponentIds: referencedComponentIds.value,
        selectedComponents: referencedComponentContext,
        selectionInstruction: buildSelectedComponentInstruction(),
        existingComponentLayouts: existingLayouts
      },
      null,
      2
    )
  } catch {
    return ''
  }
}

function buildGenerateStreamingPreview(fullText: string): string {
  return fullText.trim() ? 'AI 正在生成大屏，请稍候...' : 'AI 正在理解需求...'
}

const loadProviders = async () => {
  providerLoading.value = true
  try {
    const res = await getProviderPageApi({ pageNum: 1, pageSize: 100 })
    const records = (res?.data?.records || []).filter(item => item.status !== '1')
    providerList.value = records

    if (!records.length) {
      selectedProviderId.value = null
      return
    }

    const matched = records.find(item => String(item.id) === String(selectedProviderId.value))
    if (matched) return

    const defaultProvider = records.find(item => item.isDefault === '1') || records[0]
    selectedProviderId.value = defaultProvider?.id ?? null
  } catch (error: any) {
    window['$message']?.error('加载 AI 供应商失败: ' + (error?.message || '未知错误'))
  } finally {
    providerLoading.value = false
  }
}

async function handleSend() {
  const content = inputRef.value.trim()
  if (!content || aiStore.getGenerating) return

  if (!selectedProvider.value || !selectedProviderId.value) {
    window['$message']?.warning('请先选择一个可用的 AI 供应商')
    showModeSelect.value = true
    return
  }

  const modelName = selectedModelName.value || selectedProvider.value.defaultModel || parseModels(selectedProvider.value.models)[0]
  if (!modelName) {
    window['$message']?.warning('当前供应商未配置可用模型，请先在 AI 供应商页面维护')
    return
  }

  const requestCanvasContext = buildCanvasContext()
  clearReferencedComponents()
  inputRef.value = ''
  aiStore.setGenerating(true)
  resetStreamingState()

  aiStore.addChatMessage({
    id: `user-${Date.now()}`,
    role: 'user',
    content,
    timestamp: Date.now(),
    sessionId: chatSessionIdRef.value
  })
  aiStore.addChatMessage({
    id: `assistant-${Date.now()}`,
    role: 'assistant',
    content: '',
    timestamp: Date.now(),
    sessionId: chatSessionIdRef.value,
    streaming: true,
    reasoning: '',
    isReasoning: false,
    reasoningTime: null,
    progressSteps: getGenerateProgressSteps(''),
    canvasResponse: null
  })

  await scrollToBottom()
  const abortController = aiStore.getAbortController()

  if (chatModeRef.value === 'generate') {
    const { width, height } = getCanvasSize()
    const generateRequest = {
      prompt: content,
      sessionId: chatSessionIdRef.value,
      style: styleRef.value,
      canvasWidth: width,
      canvasHeight: height,
      componentCatalog: getComponentCatalogText(),
      projectName: chartEditStore.getEditCanvasConfig?.projectName,
      canvasContext: requestCanvasContext,
      providerId: selectedProviderId.value,
      modelName,
      temperature: temperatureRef.value,
      maxTokens: maxTokensRef.value || undefined
    }

    updateAssistantStreaming(buildGenerateStreamingPreview(''), null)

    await aiGenerateStream(
      generateRequest,
      chunk => {
        consumeReasoningAwareChunk(chunk, 'generate')
        scrollToBottom()
      },
      fullText => {
        if (!fullText) {
          aiStore.updateLastAssistantMessage('⏹️ 已停止生成', null, {
            reasoning: aiReasoningContent.value,
            isReasoning: false,
            reasoningTime: aiReasoningContent.value ? getReasoningTime() : null
          })
          aiStore.setGenerating(false)
          scrollToBottom()
          return
        }

        const answerContent = extractAnswerContent(fullText)
        if (!answerContent) {
          aiStore.updateLastAssistantMessage('⏹️ 已停止生成', null, {
            reasoning: aiReasoningContent.value,
            isReasoning: false,
            reasoningTime: aiReasoningContent.value ? getReasoningTime() : null
          })
          aiStore.setGenerating(false)
          scrollToBottom()
          return
        }

        try {
          const canvasResponse = parseStreamedResponse(answerContent)
          const displayText = `✅ 大屏生成完成！\n📊 标题：${canvasResponse.title}\n🧩 共 ${canvasResponse.components.length} 个组件\n\n点击下方按钮应用到画布。`
          aiStore.updateLastAssistantMessage(displayText, canvasResponse, {
            reasoning: aiReasoningContent.value,
            isReasoning: false,
            reasoningTime: aiReasoningContent.value ? getReasoningTime() : null,
            progressSteps: undefined
          })
          aiStore.addHistory(content, canvasResponse)
        } catch (error: any) {
          aiStore.updateLastAssistantMessage(`❌ 生成结果解析失败：${error?.message || '返回内容不是合法 JSON'}`, null, {
            reasoning: aiReasoningContent.value,
            isReasoning: false,
            reasoningTime: aiReasoningContent.value ? getReasoningTime() : null
          })
        } finally {
          aiStore.setGenerating(false)
          scrollToBottom()
        }
      },
      error => {
        aiStore.updateLastAssistantMessage(`❌ 生成失败：${error?.message || '未知错误'}`, null, {
          reasoning: aiReasoningContent.value,
          isReasoning: false,
          reasoningTime: aiReasoningContent.value ? getReasoningTime() : null
        })
        aiStore.setGenerating(false)
        scrollToBottom()
      },
      abortController.signal
    )
    return
  }

  const activeSessionId = aiStore.currentSessionId || chatSessionIdRef.value
  aiStore.setCurrentSessionId(activeSessionId)
  chatSessionIdRef.value = activeSessionId

  await aiChatStream(
    {
      content,
      agentCode: undefined,
      sessionId: activeSessionId,
      projectName: chartEditStore.getEditCanvasConfig?.projectName,
      canvasContext: requestCanvasContext,
      providerId: selectedProviderId.value,
      modelName,
      temperature: temperatureRef.value,
      maxTokens: maxTokensRef.value || undefined
    },
    chunk => {
      consumeReasoningAwareChunk(chunk, 'chat')
      scrollToBottom()
    },
    fullText => {
      if (!fullText) {
        aiStore.updateLastAssistantMessage('⏹️ 已停止生成', undefined, {
          reasoning: aiReasoningContent.value,
          isReasoning: false,
          reasoningTime: aiReasoningContent.value ? getReasoningTime() : null
        })
        aiStore.setGenerating(false)
        scrollToBottom()
        return
      }

      const answerContent = extractAnswerContent(fullText)
      if (!answerContent) {
        aiStore.updateLastAssistantMessage('⏹️ 已停止生成', undefined, {
          reasoning: aiReasoningContent.value,
          isReasoning: false,
          reasoningTime: aiReasoningContent.value ? getReasoningTime() : null
        })
      } else {
        aiStore.updateLastAssistantMessage(answerContent, undefined, {
          reasoning: aiReasoningContent.value,
          isReasoning: false,
          reasoningTime: aiReasoningContent.value ? getReasoningTime() : null
        })
      }
      aiStore.setGenerating(false)
      scrollToBottom()
    },
    error => {
      aiStore.updateLastAssistantMessage(`❌ 请求失败：${error.message}`, undefined, {
        reasoning: aiReasoningContent.value,
        isReasoning: false,
        reasoningTime: aiReasoningContent.value ? getReasoningTime() : null
      })
      aiStore.setGenerating(false)
      scrollToBottom()
    },
    abortController.signal
  )
}

function handleStop() {
  aiIsReasoningPhase.value = false
  aiReasoningEndTime.value = Date.now()
  aiStore.abortGenerating()
  const lastMsg = aiStore.getChatMessages[aiStore.getChatMessages.length - 1]
  if (lastMsg?.role === 'assistant') {
    lastMsg.isReasoning = false
    lastMsg.reasoningTime = lastMsg.reasoning ? getReasoningTime() : null
  }
}

async function applyToCanvas(response: AIGenerateResponse) {
  try {
    await applyAIToCanvas(response, true)
    window['$message'].success('AI 大屏应用成功！')
    emit('applied', response)
  } catch (error) {
    window['$message'].error('应用失败：' + (error as Error).message)
  }
}

onMounted(async () => {
  await loadProviders()
  aiStore.setChatSessions([])
  if (!aiStore.currentSessionId) {
    aiStore.setCurrentSessionId(chatSessionIdRef.value)
  }
  if (!providerList.value.length) {
    window['$message']?.warning('请先在左侧菜单的 AI 供应商 页面配置可用供应商')
  }
})
</script>

<style lang="scss" scoped>
$topHeight: 40px;

.go-ai-chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  width: 100%;
  overflow: hidden;
  background:
    radial-gradient(circle at 18% 0, rgba(81, 214, 169, 0.08), transparent 32%),
    linear-gradient(180deg, rgba(15, 23, 42, 0.2), rgba(2, 6, 23, 0.08));

  .messages-container {
    flex: 1;
    min-height: 0;
    overflow-y: auto;
    padding: 10px 10px 8px;
    display: flex;
    flex-direction: column;
    gap: 12px;

    &::-webkit-scrollbar {
      width: 4px;
    }

    &::-webkit-scrollbar-thumb {
      border-radius: 2px;
      background: rgba(255, 255, 255, 0.15);
    }

    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 100%;
      padding: 18px 14px;
      text-align: center;
      border: 1px solid rgba(81, 214, 169, 0.1);
      border-radius: 14px;
      margin: 10px;
      min-height: 360px;
      background:
        linear-gradient(180deg, rgba(81, 214, 169, 0.08), transparent 42%),
        rgba(15, 23, 42, 0.22);

      .empty-title {
        font-size: 16px;
        font-weight: 600;
        @include fetch-theme('color');
        margin-bottom: 6px;
      }

      .empty-desc {
        font-size: 12px;
        color: #888;
        margin-bottom: 16px;
        line-height: 1.5;
      }

      .quick-prompts {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 7px;
        width: 100%;

        .quick-btn {
          text-align: left;
          font-size: 12px;
          height: 32px;
          justify-content: flex-start;
          border-radius: 9px;
          background: rgba(15, 23, 42, 0.56);
          border: 1px solid rgba(148, 163, 184, 0.1);
        }
      }
    }

    .message-item {
      display: flex;
      gap: 7px;
      align-items: flex-start;

      &.user {
        flex-direction: row-reverse;
      }

      .avatar {
        width: 24px;
        height: 24px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
        margin-top: 2px;

        &.ai-avatar {
          background: rgba(81, 214, 169, 0.15);
          border: 1px solid rgba(81, 214, 169, 0.4);
        }

        &.user-avatar {
          @include fetch-bg-color('background-color3');
          border: 1px solid rgba(255, 255, 255, 0.15);
        }
      }

      .bubble-wrapper {
        display: flex;
        flex-direction: column;
        gap: 6px;
        max-width: calc(100% - 36px);

        .reasoning-section {
          border: 1px solid rgba(81, 214, 169, 0.24);
          border-radius: 10px;
          overflow: hidden;
          background: rgba(81, 214, 169, 0.08);

          .reasoning-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 8px;
            padding: 6px 8px;
            cursor: pointer;
            user-select: none;
            transition: background 0.16s ease;

            &:hover {
              background: rgba(81, 214, 169, 0.1);
            }
          }

          .reasoning-header-left {
            display: flex;
            align-items: center;
            gap: 5px;
            min-width: 0;
            font-size: 12px;
            color: #51d6a9;
            font-weight: 600;
          }

          .reasoning-duration {
            color: rgba(81, 214, 169, 0.7);
            font-size: 11px;
            font-weight: 400;

            &.thinking {
              animation: blink 1.2s infinite;
            }
          }

          .reasoning-toggle {
            color: rgba(255, 255, 255, 0.55);
            font-size: 16px;
            line-height: 1;
            transition: transform 0.18s ease;

            &.expanded {
              transform: rotate(180deg);
            }
          }

          .reasoning-content {
            max-height: 180px;
            overflow-y: auto;
            padding: 8px 10px;
            border-top: 1px solid rgba(81, 214, 169, 0.18);
            white-space: pre-wrap;
            word-break: break-word;
            font-size: 12px;
            line-height: 1.6;
            color: rgba(230, 255, 248, 0.88);
            background: rgba(0, 0, 0, 0.12);

            &::-webkit-scrollbar {
              width: 4px;
            }

            &::-webkit-scrollbar-thumb {
              border-radius: 2px;
              background: rgba(81, 214, 169, 0.32);
            }
          }
        }

        .bubble {
          padding: 8px 10px;
          border-radius: 10px;
          font-size: 13px;
          line-height: 1.55;
          word-break: break-word;

          &.assistant {
            background:
              linear-gradient(180deg, rgba(255, 255, 255, 0.05), transparent),
              rgba(15, 23, 42, 0.56);
            border: 1px solid rgba(148, 163, 184, 0.12);
            @include fetch-theme('color');
            border-bottom-left-radius: 2px;
            box-shadow: 0 8px 22px rgba(0, 0, 0, 0.12);
          }

          &.user {
            background: linear-gradient(135deg, #51d6a9, #38d9ff);
            color: #082016;
            border-bottom-right-radius: 2px;
            box-shadow: 0 8px 20px rgba(81, 214, 169, 0.12);
          }

          .typing-cursor {
            display: inline-block;
            animation: blink 0.8s infinite;
            font-weight: bold;
            color: #51d6a9;
          }

          .thinking-dots {
            display: flex;
            gap: 4px;
            align-items: center;
            padding: 2px 0;

            span {
              width: 6px;
              height: 6px;
              border-radius: 50%;
              background: #51d6a9;
              animation: bounce 1.2s infinite ease-in-out;

              &:nth-child(2) {
                animation-delay: 0.2s;
              }

              &:nth-child(3) {
                animation-delay: 0.4s;
              }
            }
          }

          .generate-progress-card,
          .generate-result-card {
            min-width: 220px;
          }

          .generate-progress-card {
            display: flex;
            flex-direction: column;
            gap: 10px;
          }

          .generate-progress-title {
            font-size: 13px;
            font-weight: 700;
            color: #e8fff8;
          }

          .generate-progress-list {
            display: grid;
            grid-template-columns: 1fr;
            gap: 5px;
          }

          .generate-progress-step {
            height: 26px;
            border: 1px solid rgba(255, 255, 255, 0.1);
            border-radius: 6px;
            display: flex;
            align-items: center;
            gap: 6px;
            padding: 0 8px;
            color: #8b949e;
            background: rgba(255, 255, 255, 0.035);
            font-size: 12px;

            &.done {
              color: rgba(81, 214, 169, 0.86);
              border-color: rgba(81, 214, 169, 0.28);
              background: rgba(81, 214, 169, 0.08);
            }

            &.active {
              color: #1a1a2e;
              border-color: rgba(81, 214, 169, 0.8);
              background: #51d6a9;
              font-weight: 700;
            }
          }

          .generate-progress-dot {
            width: 6px;
            height: 6px;
            border-radius: 50%;
            flex-shrink: 0;
            background: currentColor;
            opacity: 0.85;
          }

          .generate-progress-step.active .generate-progress-dot {
            animation: bounce 1.1s infinite ease-in-out;
          }

          .generate-progress-label {
            min-width: 0;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }

          .generate-result-card {
            display: flex;
            align-items: flex-start;
            gap: 10px;
          }

          .generate-result-icon {
            width: 32px;
            height: 32px;
            border-radius: 8px;
            display: flex;
            align-items: center;
            justify-content: center;
            flex-shrink: 0;
            background: rgba(81, 214, 169, 0.14);
            color: #51d6a9;
            border: 1px solid rgba(81, 214, 169, 0.32);
          }

          .generate-result-main {
            min-width: 0;
            display: flex;
            flex-direction: column;
            gap: 4px;
          }

          .generate-result-title {
            font-size: 13px;
            font-weight: 700;
            color: #e8fff8;
          }

          .generate-result-name {
            font-size: 12px;
            color: rgba(255, 255, 255, 0.78);
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }

          .generate-result-meta {
            display: flex;
            flex-wrap: wrap;
            gap: 5px;

            span {
              border-radius: 999px;
              padding: 2px 7px;
              background: rgba(255, 255, 255, 0.07);
              color: rgba(255, 255, 255, 0.62);
              font-size: 11px;
              line-height: 1.6;
            }
          }
        }

        .msg-actions {
          display: flex;
          gap: 6px;
        }
      }
    }
  }

  .style-row,
  .mode-row,
  .config-grid,
  .selection-context,
  .provider-tip {
    border-top: 1px solid;
    @include fetch-border-color('hover-border-color');
    background: rgba(10, 16, 28, 0.78);
    flex-shrink: 0;
  }

  .style-row,
  .mode-row {
    display: flex;
    align-items: center;
    padding: 7px 10px;
    gap: 7px;

    .style-label {
      font-size: 12px;
      color: #888;
      white-space: nowrap;
    }
  }

  .config-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 7px 8px;
    padding: 8px 10px 9px;

    .config-item {
      display: flex;
      align-items: center;
      gap: 8px;
      min-width: 0;

      .style-label {
        font-size: 12px;
        color: #888;
        white-space: nowrap;
      }

      :deep(.n-base-selection),
      :deep(.n-input-number) {
        flex: 1;
        min-width: 0;
      }
    }

    .config-provider,
    .config-model,
    .config-temperature,
    .config-max-tokens {
      grid-column: span 2;
    }
  }

  .provider-tip {
    padding: 0 10px 8px;
    font-size: 12px;
    color: #7f8c8d;
  }

  .selection-context {
    min-height: 38px;
    padding: 6px 10px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    color: rgba(255, 255, 255, 0.42);

    &.active {
      background: linear-gradient(90deg, rgba(81, 214, 169, 0.12), rgba(81, 214, 169, 0.04));
      border-top-color: rgba(81, 214, 169, 0.22);
      color: rgba(232, 255, 248, 0.88);
    }

    .selection-context-main {
      min-width: 0;
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .selection-context-icon {
      width: 22px;
      height: 22px;
      border-radius: 6px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      color: #51d6a9;
      background: rgba(81, 214, 169, 0.12);
      border: 1px solid rgba(81, 214, 169, 0.26);
    }

    .selection-context-info {
      min-width: 0;
      display: flex;
      flex-direction: column;
      gap: 2px;
    }

    .selection-context-title {
      font-size: 12px;
      font-weight: 700;
      color: #dffcf3;
      line-height: 1.3;
    }

    .selection-context-name {
      min-width: 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      font-size: 11px;
      line-height: 1.4;
    }

    .selection-context-name {
      color: rgba(232, 255, 248, 0.62);
    }
  }

  .input-area {
    padding: 8px 10px 10px;
    flex-shrink: 0;
    background:
      linear-gradient(180deg, rgba(15, 23, 42, 0.72), rgba(2, 6, 23, 0.92));
    border-top: 1px solid rgba(var(--app-theme-rgb), 0.1);

    :deep(.n-input) {
      border-radius: 12px;
      background: rgba(2, 6, 23, 0.52);
    }

    :deep(.n-input__textarea-el) {
      font-size: 13px;
      line-height: 1.55;
    }

    .input-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-top: 7px;
      min-height: 26px;
      gap: 8px;

      .input-tools {
        display: flex;
        align-items: center;
        gap: 6px;
        min-width: 0;
      }

      .reference-btn {
        flex-shrink: 0;
        color: #777;
        width: 28px;
        height: 28px;

        &.active {
          color: #51d6a9;
          background: rgba(81, 214, 169, 0.14);
        }
      }

      .hint-text {
        font-size: 11px;
        color: #666;
        min-width: 0;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }
  }
}

@keyframes blink {
  0%,
  100% {
    opacity: 1;
  }

  50% {
    opacity: 0;
  }
}

@keyframes bounce {
  0%,
  80%,
  100% {
    transform: scale(0.6);
    opacity: 0.5;
  }

  40% {
    transform: scale(1);
    opacity: 1;
  }
}

:deep(.code-block) {
  background: rgba(0, 0, 0, 0.3);
  border-radius: 4px;
  padding: 8px;
  margin: 4px 0;
  font-size: 11px;
  overflow-x: auto;
  white-space: pre-wrap;
  font-family: 'Consolas', 'Monaco', monospace;
}

:deep(.inline-code) {
  background: rgba(81, 214, 169, 0.2);
  color: #51d6a9;
  border-radius: 3px;
  padding: 1px 4px;
  font-size: 12px;
  font-family: 'Consolas', 'Monaco', monospace;
}
</style>
