<template>
  <div class="msg-row" :class="msg.role">
    <div class="msg-body">
      <!-- 用户消息 -->
      <template v-if="msg.role === 'user'">
        <div v-if="msg.attachments && msg.attachments.length" class="msg-attachments">
          <AuthImage
            v-for="(fid, i) in msg.attachments"
            :key="i"
            :src="String(fid)"
            preview
            :img-style="{ width: '84px', height: '84px', objectFit: 'cover', borderRadius: '10px' }"
          />
        </div>
        <!-- 编辑态：内联编辑后重发（仅最新用户消息） -->
        <div v-if="editing" class="user-edit-box">
          <NInput
            v-model:value="localEditText"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 6 }"
            placeholder="编辑后重发…"
          />
          <div class="user-edit-actions">
            <NButton size="tiny" @click="emit('edit-cancel')">
              取消
            </NButton>
            <NButton size="tiny" type="primary" :disabled="isStreaming || !localEditText.trim()" @click="onEditConfirm">
              重发
            </NButton>
          </div>
        </div>
        <!-- 展示态 -->
        <template v-else>
          <div v-if="msg.content" class="message-content user-content">
            {{ msg.content }}
          </div>
          <div class="msg-actions user-actions">
            <NTooltip>
              <template #trigger>
                <NButton quaternary size="tiny" @click="emit('copy', msg)">
                  <template #icon>
                    <NIcon><CopyOutline /></NIcon>
                  </template>
                </NButton>
              </template>
              复制
            </NTooltip>
            <NTooltip v-if="isLastUser">
              <template #trigger>
                <NButton quaternary size="tiny" :disabled="isStreaming" @click="emit('edit-start', msg)">
                  <template #icon>
                    <NIcon><CreateOutline /></NIcon>
                  </template>
                </NButton>
              </template>
              编辑并重发
            </NTooltip>
            <NPopconfirm @positive-click="emit('delete', msg)">
              <template #trigger>
                <NButton quaternary size="tiny" :disabled="isStreaming">
                  <template #icon>
                    <NIcon><TrashOutline /></NIcon>
                  </template>
                </NButton>
              </template>
              确定删除这条消息？
            </NPopconfirm>
          </div>
        </template>
      </template>

      <!-- 助手消息 -->
      <template v-else>
        <!-- 助手身份标识（无气泡满宽阅读，用小标识区分角色） -->
        <div class="assistant-head">
          <NIcon class="assistant-mark"><SparklesOutline /></NIcon>
          <span class="assistant-name">{{ currentAgent?.agentName || currentAgent?.agentCode || '助手' }}</span>
        </div>

        <!-- 思考块（生成中默认展开，回答完毕自动收起） -->
        <ThinkingBlock v-if="msg.thinking" :content="msg.thinking" :streaming="msg.streaming" />

        <!-- 工具调用卡片 -->
        <ToolCard
          v-for="(tc, idx) in msg.toolCalls"
          :key="idx"
          :tool-call="tc"
        />

        <!-- 助手状态 -->
        <div v-if="msg.status && msg.status !== 'done'" class="message-status">
          <NTag size="tiny" :type="statusTagType" :bordered="false">
            {{ getStatusLabel(msg.status) }}
          </NTag>
        </div>

        <!-- 文本内容（增量渲染 + 代码高亮，html 由父组件渲染好后传入） -->
        <div v-if="msg.html" class="message-content assistant-content markdown-body" v-html="msg.html" />
        <div v-else-if="msg.streaming && !msg.thinking && !msg.toolCalls.length" class="assistant-content typing-indicator">
          <span class="typing-dot" />
          <span class="typing-dot" />
          <span class="typing-dot" />
        </div>

        <div v-if="msg.firstTokenMs != null || msg.totalMs != null || msg.tokenUsage != null" class="message-meta">
          <span v-if="msg.firstTokenMs != null">首字 {{ msg.firstTokenMs }}ms</span>
          <span v-if="msg.totalMs != null">总耗时 {{ formatDuration(msg.totalMs) }}</span>
          <span v-if="msg.tokenUsage != null">Token {{ msg.tokenUsage }}</span>
        </div>

        <!-- 错误块 -->
        <div v-if="msg.error" class="message-error">
          <div class="message-error-text">
            {{ msg.error }}
          </div>
          <NButton size="tiny" tertiary type="error" @click="emit('retry', msg)">
            重试
          </NButton>
        </div>

        <!-- 消息操作（复制 / 重新生成｜继续生成 / 删除） -->
        <div v-if="!msg.streaming && (msg.content || msg.toolCalls.length)" class="msg-actions">
          <NTooltip>
            <template #trigger>
              <NButton quaternary size="tiny" @click="emit('copy', msg)">
                <template #icon>
                  <NIcon><CopyOutline /></NIcon>
                </template>
              </NButton>
            </template>
            复制
          </NTooltip>
          <NTooltip v-if="isLastAssistant">
            <template #trigger>
              <NButton quaternary size="tiny" :disabled="isStreaming" @click="emit('retry', msg)">
                <template #icon>
                  <NIcon>
                    <PlayForwardOutline v-if="msg.status === 'aborted'" />
                    <RefreshOutline v-else />
                  </NIcon>
                </template>
              </NButton>
            </template>
            {{ msg.status === 'aborted' ? '继续生成' : '重新生成' }}
          </NTooltip>
          <NPopconfirm @positive-click="emit('delete', msg)">
            <template #trigger>
              <NButton quaternary size="tiny" :disabled="isStreaming">
                <template #icon>
                  <NIcon><TrashOutline /></NIcon>
                </template>
              </NButton>
            </template>
            确定删除这条消息？
          </NPopconfirm>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import {
  CopyOutline,
  CreateOutline,
  PlayForwardOutline,
  RefreshOutline,
  SparklesOutline,
  TrashOutline,
} from '@vicons/ionicons5'
import { NButton, NIcon, NInput, NPopconfirm, NTag, NTooltip } from 'naive-ui'
import { computed, ref, watch } from 'vue'
import AuthImage from '@/components/common/AuthImage.vue'
import { formatDuration, getStatusLabel } from './chat-utils'
import ThinkingBlock from './ThinkingBlock.vue'
import ToolCard from './ToolCard.vue'

defineOptions({ name: 'ChatMessageBubble' })

const props = defineProps({
  // 单条消息对象（结构见父组件 loadMessages / createAssistantMessage）
  msg: { type: Object, required: true },
  // 当前 Agent（用于助手身份标识显示名称）
  currentAgent: { type: Object, default: null },
  // 全局流式中：禁用编辑/重试/删除
  isStreaming: { type: Boolean, default: false },
  // 是否最新用户消息：仅它可「编辑并重发」
  isLastUser: { type: Boolean, default: false },
  // 是否最新助手消息：仅它可「重新生成/继续生成」
  isLastAssistant: { type: Boolean, default: false },
  // 是否处于内联编辑态（父组件持有 editingMsgId 决定）
  editing: { type: Boolean, default: false },
})

const emit = defineEmits(['copy', 'edit-start', 'edit-cancel', 'edit-confirm', 'delete', 'retry'])

// 内联编辑缓冲：进入编辑态时用当前内容初始化，确认后回传父组件
const localEditText = ref('')
watch(() => props.editing, (v) => {
  if (v)
    localEditText.value = props.msg.content || ''
}, { immediate: true })

function onEditConfirm() {
  const text = (localEditText.value || '').trim()
  if (!text)
    return
  emit('edit-confirm', { msg: props.msg, text })
}

const statusTagType = computed(() => {
  const s = props.msg.status
  if (s === 'error')
    return 'error'
  if (s === 'waiting_confirm')
    return 'warning'
  if (s === 'aborted')
    return 'default'
  return 'info'
})
</script>

<style scoped>
/* 消息行（无气泡满宽阅读：助手左对齐满宽，用户右对齐软气泡） */
.msg-row {
  margin-bottom: 28px;
  animation: msg-in 0.25s ease-out;
}

@keyframes msg-in {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.msg-body {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.msg-row.user .msg-body {
  align-items: flex-end;
}

.msg-attachments {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

/* 用户消息气泡（精炼品牌蓝，右对齐） */
.user-content {
  max-width: 82%;
  padding: 11px 16px;
  color: #fff;
  font-size: 15px;
  line-height: 1.75;
  word-break: break-word;
  white-space: pre-wrap;
  background: var(--primary);
  border-radius: 18px 18px 6px 18px;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.22);
}

/* 助手身份标识 */
.assistant-head {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}

.assistant-mark {
  color: var(--primary);
  font-size: 15px;
}

.assistant-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-strong);
  letter-spacing: 0.2px;
}

/* 助手消息正文（无气泡，满宽文档式阅读） */
.assistant-content {
  padding: 0;
  color: var(--text-body);
  font-size: 15px;
  line-height: 1.8;
  word-break: break-word;
  background: transparent;
  border: none;
}

/* 流式等待：打字指示器（三点跳动） */
.typing-indicator {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 4px 0;
}

.typing-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--primary);
  opacity: 0.4;
  animation: typing-bounce 1.2s infinite ease-in-out;
}

.typing-dot:nth-child(2) {
  animation-delay: 0.18s;
}

.typing-dot:nth-child(3) {
  animation-delay: 0.36s;
}

@keyframes typing-bounce {
  0%,
  60%,
  100% {
    transform: translateY(0);
    opacity: 0.4;
  }
  30% {
    transform: translateY(-5px);
    opacity: 1;
  }
}

.message-status {
  margin-bottom: 8px;
}

.message-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 10px;
  font-size: 12px;
  color: var(--text-muted);
}

.msg-actions {
  display: flex;
  gap: 2px;
  margin-top: 8px;
  opacity: 0;
  transition: opacity 0.15s ease;
}

.msg-row:hover .msg-actions {
  opacity: 1;
}

/* 用户消息操作行右对齐 */
.user-actions {
  justify-content: flex-end;
}

/* 用户消息内联编辑 */
.user-edit-box {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 320px;
  max-width: 82%;
}

.user-edit-actions {
  display: flex;
  gap: 6px;
  justify-content: flex-end;
}

.message-error {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  padding: 8px 12px;
  color: #d03050;
  background: rgba(208, 48, 80, 0.08);
  border-radius: 10px;
  font-size: 12px;
}

.message-error-text {
  flex: 1;
  min-width: 0;
  word-break: break-word;
}

/* Markdown 正文样式（v-html 内容，随 .markdown-body 一起归属本组件） */
.markdown-body :deep(p) {
  margin: 0 0 12px;
}

.markdown-body :deep(p:last-child) {
  margin-bottom: 0;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  margin: 8px 0;
  padding-left: 24px;
}

.markdown-body :deep(li) {
  margin: 4px 0;
}

.markdown-body :deep(a) {
  color: var(--primary);
  text-decoration: none;
}

.markdown-body :deep(a:hover) {
  text-decoration: underline;
}

.markdown-body :deep(:not(pre) > code) {
  padding: 1px 6px;
  background: rgba(37, 99, 235, 0.09);
  border-radius: 5px;
  font-size: 13px;
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
}

:global(.dark) .markdown-body :deep(:not(pre) > code) {
  background: rgba(59, 130, 246, 0.16);
}

.markdown-body :deep(blockquote) {
  margin: 10px 0;
  padding: 4px 14px;
  border-left: 3px solid var(--primary);
  background: rgba(37, 99, 235, 0.06);
  color: var(--text-muted);
  border-radius: 0 8px 8px 0;
}

.markdown-body :deep(table) {
  border-collapse: collapse;
  margin: 10px 0;
  width: 100%;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid var(--border);
  padding: 7px 12px;
  font-size: 14px;
}

.markdown-body :deep(th) {
  background: var(--bg-soft);
  font-weight: 600;
}

/* 代码块 */
.markdown-body :deep(.code-block) {
  margin: 12px 0;
  border-radius: 10px;
  overflow: hidden;
  background: #282c34;
}

.markdown-body :deep(.code-block-header) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 7px 14px;
  background: color-mix(in srgb, var(--bg-primary) 6%, transparent);
  color: #abb2bf;
  font-size: 11px;
}

.markdown-body :deep(.code-copy-btn) {
  padding: 2px 10px;
  border: none;
  border-radius: 5px;
  background: color-mix(in srgb, var(--bg-primary) 12%, transparent);
  color: #dfe4ee;
  font-size: 11px;
  cursor: pointer;
}

.markdown-body :deep(.code-copy-btn:hover) {
  background: color-mix(in srgb, var(--bg-primary) 22%, transparent);
}

.markdown-body :deep(.code-block pre) {
  margin: 0;
  padding: 14px 16px;
  overflow-x: auto;
}

.markdown-body :deep(.code-block code) {
  font-size: 13px;
  line-height: 1.65;
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  background: transparent;
}
</style>
