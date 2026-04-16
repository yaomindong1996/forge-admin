<template>
  <div class="go-ai-chat-panel">
    <div class="messages-container" ref="messagesContainerRef">
      <div v-if="aiStore.getChatMessages.length === 0" class="empty-state">
        <n-icon size="40" color="#51d6a9" style="margin-bottom: 12px">
          <SparklesIcon />
        </n-icon>
        <p class="empty-title">GoView AI 助手</p>
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
          v-for="msg in aiStore.getChatMessages"
          :key="msg.id"
          class="message-item"
          :class="msg.role"
        >
          <div v-if="msg.role === 'assistant'" class="avatar ai-avatar">
            <n-icon size="14" color="#51d6a9"><SparklesIcon /></n-icon>
          </div>
          <div class="bubble-wrapper">
            <div class="bubble" :class="msg.role">
              <span class="msg-content" v-html="renderContent(msg.content)"></span>
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

    <div class="input-area">
      <n-input
        v-model:value="inputRef"
        type="textarea"
        :placeholder="chatModeRef === 'generate' ? '描述你想要的数据大屏...' : '有什么可以帮你？'"
        :rows="3"
        :disabled="aiStore.getGenerating"
        @keydown.enter.exact.prevent="handleSend"
        @keydown.shift.enter.prevent="inputRef += '\n'"
      />
      <div class="input-footer">
        <span class="hint-text">Enter 发送，Shift+Enter 换行</span>
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
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import {
  type AiProvider,
  aiChatStream,
  aiGenerateStream,
  getProviderPageApi,
  getSessionListApi,
  getSessionMessagesApi
} from '@/api/ai'
import type { AIGenerateResponse } from '@/api/ai/ai'
import { applyAIToCanvas } from './aiEngine'
import { parseStreamedResponse } from './llmClient'
import { getComponentCatalogText } from './componentRegistry'
import { icon } from '@/plugins'
import { useAIStore } from '@/store/modules/aiStore/aiStore'
import { useChartEditStore } from '@/store/modules/chartEditStore/chartEditStore'

const { SparklesIcon, SendIcon, AnalyticsIcon, PersonIcon, SettingsSharpIcon, CloseIcon } = icon.ionicons5

const emit = defineEmits(['applied'])

const aiStore = useAIStore()
const chartEditStore = useChartEditStore()

const inputRef = ref('')
const styleRef = ref<'dark' | 'light'>('dark')
const chatModeRef = ref<'generate' | 'chat'>('generate')
const showModeSelect = ref(false)
const messagesContainerRef = ref<HTMLElement>()
const providerList = ref<AiProvider[]>([])
const providerLoading = ref(false)
const chatSessionIdRef = ref(aiStore.currentSessionId || createSessionId())
const selectedProviderId = ref<number | string | null>(aiStore.getSelectedProvider?.providerId ?? null)
const selectedModelName = ref(aiStore.getSelectedProvider?.modelName || '')
const temperatureRef = ref(aiStore.getSelectedProvider?.temperature ?? 0.7)
const maxTokensRef = ref<number | null>(aiStore.getSelectedProvider?.maxTokens ?? null)

const quickPrompts = [
  '电商销售数据监控大屏',
  '智慧城市运营中心大屏',
  '工厂生产数据监控大屏',
  '财务数据分析大屏'
]

const GENERATE_PROGRESS_STEPS = [
  '🧠 正在理解你的大屏需求...',
  '🧩 正在规划页面布局与组件组合...',
  '📊 正在生成图表数据结构与画布配置...',
  '✨ 正在整理最终结果...'
]

const hasStreamingMessage = computed(() => aiStore.getChatMessages.some(message => message.streaming))

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
  if (messagesContainerRef.value) {
    messagesContainerRef.value.scrollTop = messagesContainerRef.value.scrollHeight
  }
}

watch(() => aiStore.getChatMessages.length, scrollToBottom)
watch(
  () => aiStore.getChatMessages[aiStore.getChatMessages.length - 1]?.content,
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
    return JSON.stringify(
      {
        canvas: {
          projectName: canvasConfig?.projectName,
          width: canvasConfig?.width,
          height: canvasConfig?.height,
          background: canvasConfig?.background || canvasConfig?.backgroundColor
        },
        components: componentList
      },
      null,
      2
    )
  } catch {
    return ''
  }
}

async function loadSessionHistory() {
  try {
    const sessionRes = await getSessionListApi()
    const sessions = sessionRes?.data || []
    aiStore.setChatSessions(sessions)

    let activeSessionId = aiStore.currentSessionId
    if (!activeSessionId && sessions.length) {
      activeSessionId = sessions[0].id
    }

    if (activeSessionId) {
      aiStore.setCurrentSessionId(activeSessionId)
      chatSessionIdRef.value = activeSessionId
      const messagesRes = await getSessionMessagesApi(activeSessionId)
      const records = messagesRes?.data || []
      aiStore.setChatMessages(
        records.map((item, index) => ({
          id: String(item.id || `${item.role}-${index}`),
          role: item.role === 'assistant' ? 'assistant' : 'user',
          content: item.content,
          timestamp: item.createTime ? new Date(item.createTime).getTime() : Date.now(),
          sessionId: item.sessionId,
          streaming: false,
          canvasResponse: null
        }))
      )
    } else {
      aiStore.setChatMessages([])
    }
  } catch (error: any) {
    window['$message']?.error('加载历史会话失败: ' + (error?.message || '未知错误'))
  }
}

function buildGenerateStreamingPreview(fullText: string): string {
  const trimmed = fullText.trim()
  if (!trimmed) {
    return GENERATE_PROGRESS_STEPS[0]
  }

  const progress = [...GENERATE_PROGRESS_STEPS]
  if (trimmed.length > 80) progress.push('🔧 正在完善组件配置细节...')
  if (trimmed.length > 160) progress.push('📐 正在校验布局坐标与尺寸...')
  if (trimmed.length > 260) progress.push('✅ 结果即将完成，请稍候...')

  return progress.slice(0, Math.min(progress.length, Math.max(1, Math.ceil(trimmed.length / 80)))).join('\n')
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

  inputRef.value = ''
  aiStore.setGenerating(true)

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
    canvasResponse: null
  })

  await scrollToBottom()
  const abortController = aiStore.getAbortController()

  if (chatModeRef.value === 'generate') {
    const { width, height } = getCanvasSize()
    const generateRequest = {
      prompt: content,
      style: styleRef.value,
      canvasWidth: width,
      canvasHeight: height,
      componentCatalog: getComponentCatalogText(),
      projectName: chartEditStore.getEditCanvasConfig?.projectName,
      canvasContext: buildCanvasContext(),
      providerId: selectedProviderId.value,
      modelName,
      temperature: temperatureRef.value,
      maxTokens: maxTokensRef.value || undefined
    }
    let rawGenerateText = ''

    aiStore.updateLastAssistantMessage(GENERATE_PROGRESS_STEPS[0], null)
    aiStore.getChatMessages[aiStore.getChatMessages.length - 1].streaming = true

    await aiGenerateStream(
      generateRequest,
      chunk => {
        rawGenerateText += chunk
        aiStore.updateLastAssistantMessage(buildGenerateStreamingPreview(rawGenerateText), null)
        aiStore.getChatMessages[aiStore.getChatMessages.length - 1].streaming = true
        scrollToBottom()
      },
      fullText => {
        if (!fullText) {
          aiStore.updateLastAssistantMessage('⏹️ 已停止生成', null)
          aiStore.setGenerating(false)
          scrollToBottom()
          return
        }

        try {
          const canvasResponse = parseStreamedResponse(fullText)
          const displayText = `✅ 大屏生成完成！\n📊 标题：${canvasResponse.title}\n🧩 共 ${canvasResponse.components.length} 个组件\n\n点击下方按钮应用到画布。`
          aiStore.updateLastAssistantMessage(displayText, canvasResponse)
          aiStore.addHistory(content, canvasResponse)
        } catch (error: any) {
          aiStore.updateLastAssistantMessage(`❌ 生成结果解析失败：${error?.message || '返回内容不是合法 JSON'}`, null)
        } finally {
          aiStore.setGenerating(false)
          scrollToBottom()
        }
      },
      error => {
        aiStore.updateLastAssistantMessage(`❌ 生成失败：${error?.message || '未知错误'}`, null)
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
      canvasContext: buildCanvasContext(),
      providerId: selectedProviderId.value,
      modelName,
      temperature: temperatureRef.value,
      maxTokens: maxTokensRef.value || undefined
    },
    chunk => {
      aiStore.appendStreamingText(chunk)
      scrollToBottom()
    },
    fullText => {
      if (!fullText) {
        aiStore.updateLastAssistantMessage('⏹️ 已停止生成', undefined)
      } else {
        aiStore.updateLastAssistantMessage(fullText, undefined)
      }
      aiStore.setGenerating(false)
      scrollToBottom()
    },
    error => {
      aiStore.updateLastAssistantMessage(`❌ 请求失败：${error.message}`, undefined)
      aiStore.setGenerating(false)
      scrollToBottom()
    },
    abortController.signal
  )
}

function handleStop() {
  aiStore.abortGenerating()
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
  await loadSessionHistory()
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
  height: calc(100vh - #{$--header-height} - #{$topHeight});
  width: 100%;
  overflow: hidden;

  .messages-container {
    flex: 1;
    overflow-y: auto;
    padding: 12px 10px;
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
      padding: 20px;
      text-align: center;

      .empty-title {
        font-size: 15px;
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
        display: flex;
        flex-direction: column;
        gap: 6px;
        width: 100%;

        .quick-btn {
          text-align: left;
          font-size: 12px;
        }
      }
    }

    .message-item {
      display: flex;
      gap: 8px;
      align-items: flex-start;

      &.user {
        flex-direction: row-reverse;
      }

      .avatar {
        width: 26px;
        height: 26px;
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
        max-width: calc(100% - 40px);

        .bubble {
          padding: 8px 12px;
          border-radius: 8px;
          font-size: 13px;
          line-height: 1.6;
          word-break: break-word;

          &.assistant {
            @include fetch-bg-color('background-color2');
            border: 1px solid rgba(255, 255, 255, 0.08);
            @include fetch-theme('color');
            border-bottom-left-radius: 2px;
          }

          &.user {
            background: #51d6a9;
            color: #1a1a2e;
            border-bottom-right-radius: 2px;
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
  .provider-tip {
    border-top: 1px solid;
    @include fetch-border-color('hover-border-color');
    @include fetch-bg-color('background-color2');
    flex-shrink: 0;
  }

  .style-row,
  .mode-row {
    display: flex;
    align-items: center;
    padding: 6px 10px;
    gap: 8px;

    .style-label {
      font-size: 12px;
      color: #888;
      white-space: nowrap;
    }
  }

  .config-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 8px 12px;
    padding: 8px 10px;

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
    .config-model {
      grid-column: span 2;
    }
  }

  .provider-tip {
    padding: 0 10px 8px;
    font-size: 12px;
    color: #7f8c8d;
  }

  .input-area {
    padding: 8px 10px;
    flex-shrink: 0;
    @include fetch-bg-color('background-color2');

    .input-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-top: 6px;

      .hint-text {
        font-size: 11px;
        color: #666;
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
