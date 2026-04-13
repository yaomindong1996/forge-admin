<template>
  <div class="go-ai-chat-panel">
    <!-- 消息列表 -->
    <div class="messages-container" ref="messagesContainerRef">
        <!-- 空状态 -->
        <div v-if="aiStore.getChatMessages.length === 0" class="empty-state">
          <n-icon size="40" color="#51d6a9" style="margin-bottom: 12px">
            <SparklesIcon />
          </n-icon>
          <p class="empty-title">GoView AI 助手</p>
          <p class="empty-desc">描述你想要的数据大屏，AI 将帮你自动生成</p>
          <div class="quick-prompts">
            <n-button
              v-for="p in quickPrompts"
              :key="p"
              size="small"
              secondary
              class="quick-btn"
              @click="useQuickPrompt(p)"
            >{{ p }}</n-button>
          </div>
        </div>

        <!-- 消息气泡 -->
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

      <!-- 风格选择 -->
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

      <!-- 模式选择（可折叠） -->
      <n-collapse-transition :show="showModeSelect">
        <div class="mode-row">
          <span class="style-label">模式：</span>
          <n-radio-group v-model:value="chatModeRef" size="small">
            <n-radio-button value="generate">生成大屏</n-radio-button>
            <n-radio-button value="chat">自由对话</n-radio-button>
          </n-radio-group>
        </div>
      </n-collapse-transition>

      <!-- 输入区域 -->
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
          <n-button
            v-if="aiStore.getGenerating"
            type="error"
            size="small"
            @click="handleStop"
          >
            <template #icon><n-icon><CloseIcon /></n-icon></template>
            停止生成
          </n-button>
          <n-button
            v-else
            type="primary"
            size="small"
            :disabled="!inputRef.trim()"
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
import { ref, nextTick, computed, watch } from 'vue'
import { useAIStore } from '@/store/modules/aiStore/aiStore'
import { useChartEditStore } from '@/store/modules/chartEditStore/chartEditStore'
import { callLLMStream, callLLMChat, parseStreamedResponse } from './llmClient'
import { applyAIToCanvas } from './aiEngine'
import { AIGenerateResponse } from '@/api/ai/ai.d'
import { icon } from '@/plugins'

const { SparklesIcon, SendIcon, AnalyticsIcon, PersonIcon, TrashIcon, SettingsSharpIcon, CloseIcon } = icon.ionicons5

const emit = defineEmits(['applied'])

const aiStore = useAIStore()
const chartEditStore = useChartEditStore()

const inputRef = ref('')
const styleRef = ref<'dark' | 'light'>('dark')
const chatModeRef = ref<'generate' | 'chat'>('generate')
const showModeSelect = ref(false)
const messagesContainerRef = ref<HTMLElement>()

// 快捷提示词
const quickPrompts = [
  '电商销售数据监控大屏',
  '智慧城市运营中心大屏',
  '工厂生产数据监控大屏',
  '财务数据分析大屏'
]

// 是否已有流式消息正在输出
const hasStreamingMessage = computed(() => {
  return aiStore.getChatMessages.some(m => m.streaming)
})

// 自动滚动到底部
const scrollToBottom = async () => {
  await nextTick()
  if (messagesContainerRef.value) {
    messagesContainerRef.value.scrollTop = messagesContainerRef.value.scrollHeight
  }
}

// 监听消息变化，自动滚动
watch(() => aiStore.getChatMessages.length, scrollToBottom)
watch(() => {
  const last = aiStore.getChatMessages[aiStore.getChatMessages.length - 1]
  return last?.content
}, scrollToBottom)

// 渲染消息内容（支持简单的 markdown 代码块高亮）
function renderContent(content: string): string {
  if (!content) return ''
  // 转义 HTML
  let escaped = content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')

  // 代码块
  escaped = escaped.replace(/```[\s\S]*?```/g, match => {
    return `<pre class="code-block">${match.replace(/```\w*\n?/g, '').replace(/```/g, '')}</pre>`
  })

  // 行内代码
  escaped = escaped.replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>')

  // 换行
  escaped = escaped.replace(/\n/g, '<br/>')

  return escaped
}

// 使用快捷提示词
function useQuickPrompt(prompt: string) {
  inputRef.value = prompt
  chatModeRef.value = 'generate'
}

// 清空对话
function clearChat() {
  aiStore.clearChat()
}

// 获取画布尺寸
function getCanvasSize() {
  try {
    const config = chartEditStore.getEditCanvasConfig
    if (config?.width && config?.height) {
      return { width: config.width, height: config.height }
    }
  } catch {}
  return { width: 1920, height: 1080 }
}

// 发送消息
async function handleSend() {
  const content = inputRef.value.trim()
  if (!content || aiStore.getGenerating) return

  inputRef.value = ''
  aiStore.setGenerating(true)

  // 添加用户消息
  const userMsgId = `user-${Date.now()}`
  aiStore.addChatMessage({
    id: userMsgId,
    role: 'user',
    content,
    timestamp: Date.now()
  })

  // 添加 AI 占位消息（流式输出时更新）
  const assistantMsgId = `assistant-${Date.now()}`
  aiStore.addChatMessage({
    id: assistantMsgId,
    role: 'assistant',
    content: '',
    timestamp: Date.now(),
    streaming: true,
    canvasResponse: null
  })

  await scrollToBottom()

  // 创建 AbortController
  const abortController = aiStore.getAbortController()

  if (chatModeRef.value === 'generate') {
    // 生成大屏模式
    const { width, height } = getCanvasSize()
    await callLLMStream(
      { prompt: content, style: styleRef.value, canvasWidth: width, canvasHeight: height },
      (chunk) => {
        // 实时更新 assistant 消息内容（使用 store 方法触发响应式更新）
        aiStore.appendStreamingText(chunk)
        scrollToBottom()
      },
      (fullText) => {
        // 完成，尝试解析 JSON
        if (!fullText) {
          // 用户主动中止
          aiStore.updateLastAssistantMessage('⏹️ 已停止生成', null)
          return
        }

        let canvasResponse: AIGenerateResponse | null = null
        let displayText = fullText

        try {
          canvasResponse = parseStreamedResponse(fullText)
          displayText = `✅ 大屏生成完成！\n📊 标题：${canvasResponse.title}\n🧩 共 ${canvasResponse.components.length} 个组件\n\n点击下方按钮应用到画布。`
          aiStore.addHistory(content, canvasResponse)
        } catch (e) {
          displayText = `❌ 生成失败：${(e as Error).message}\n\n原始响应：\n${fullText.slice(0, 500)}...`
        }

        aiStore.updateLastAssistantMessage(displayText, canvasResponse)
        aiStore.setGenerating(false)
        scrollToBottom()
      },
      (error) => {
        aiStore.updateLastAssistantMessage(`❌ 生成失败：${error.message}`, null)
        aiStore.setGenerating(false)
        scrollToBottom()
      },
      abortController.signal
    )
  } else {
    // 自由对话模式
    const systemMsg = {
      role: 'system' as const,
      content: '你是 GoView 数据大屏平台的 AI 助手，可以回答关于数据可视化、大屏设计、ECharts 图表配置等问题。回答要简洁实用。'
    }
    const historyMessages = aiStore.getChatMessages
      .filter(m => !m.streaming)
      .slice(-10)
      .map(m => ({ role: m.role as 'user' | 'assistant', content: m.content }))

    await callLLMChat(
      [systemMsg, ...historyMessages, { role: 'user', content }],
      (chunk) => {
        aiStore.appendStreamingText(chunk)
        scrollToBottom()
      },
      (fullText) => {
        if (!fullText) {
          // 用户主动中止
          aiStore.updateLastAssistantMessage('⏹️ 已停止生成', undefined)
          return
        }
        aiStore.updateLastAssistantMessage(fullText, undefined)
        aiStore.setGenerating(false)
        scrollToBottom()
      },
      (error) => {
        aiStore.updateLastAssistantMessage(`❌ 请求失败：${error.message}`, undefined)
        aiStore.setGenerating(false)
        scrollToBottom()
      },
      abortController.signal
    )
  }
}

// 停止生成
function handleStop() {
  aiStore.abortGenerating()
}

// 应用到画布
async function applyToCanvas(response: AIGenerateResponse) {
  try {
    await applyAIToCanvas(response, true)
    window['$message'].success('AI 大屏应用成功！')
    emit('applied', response)
  } catch (error) {
    window['$message'].error('应用失败：' + (error as Error).message)
  }
}
</script>

<style lang="scss" scoped>
/* 与 ContentCharts 的 menu-width-box 使用相同的绝对高度 */
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

      &::-webkit-scrollbar { width: 4px; }
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
          .quick-btn { text-align: left; font-size: 12px; }
        }
      }

      .message-item {
        display: flex;
        gap: 8px;
        align-items: flex-start;

        &.user { flex-direction: row-reverse; }

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
            border: 1px solid rgba(255,255,255,0.15);
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
              border: 1px solid rgba(255,255,255,0.08);
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
                width: 6px; height: 6px;
                border-radius: 50%;
                background: #51d6a9;
                animation: bounce 1.2s infinite ease-in-out;
                &:nth-child(2) { animation-delay: 0.2s; }
                &:nth-child(3) { animation-delay: 0.4s; }
              }
            }
          }
          .msg-actions { display: flex; gap: 6px; }
        }
      }
    }

  .style-row, .mode-row {
    display: flex;
    align-items: center;
    padding: 6px 10px;
    gap: 8px;
    border-top: 1px solid;
    @include fetch-border-color('hover-border-color');
    @include fetch-bg-color('background-color2');
    flex-shrink: 0;
    .style-label { font-size: 12px; color: #888; white-space: nowrap; }
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
      .hint-text { font-size: 11px; color: #666; }
    }
  }
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}
@keyframes bounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.5; }
  40% { transform: scale(1); opacity: 1; }
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
