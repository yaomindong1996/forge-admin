<template>
  <div ref="messageAreaRef" class="message-area" @scroll="onScroll" @click="onMessageAreaClick">
    <!-- 空状态引导（欢迎语 + 推荐问题） -->
    <div v-if="!messages.length && !isStreaming" class="welcome">
      <NAvatar
        round
        :size="60"
        :style="{ background: colorFor(currentAgent?.agentCode), color: 'var(--bg-primary)' }"
      >
        <NIcon size="32"><SparklesOutline /></NIcon>
      </NAvatar>
      <h3 class="welcome-title">
        {{ currentAgent?.agentName || '智能助手' }}
      </h3>
      <p class="welcome-greeting">
        {{ currentAgent?.greeting || '你好，有什么可以帮你的？直接输入问题开始对话吧。' }}
      </p>
      <div v-if="presetQuestions.length" class="preset-list">
        <div
          v-for="(q, i) in presetQuestions"
          :key="i"
          class="preset-chip"
          @click="emit('preset', q)"
        >
          {{ q }}
        </div>
      </div>
    </div>

    <!-- 居中阅读列（长对话按文档式排版） -->
    <div class="message-col">
      <!-- 上滑加载更早消息提示（变更B#7） -->
      <div v-if="loadingMoreMessages" class="load-more-hint">
        加载更早消息…
      </div>
      <div v-else-if="hasMoreMessages && messages.length" class="load-more-hint load-more-tip">
        上滑加载更早消息
      </div>

      <MessageBubble
        v-for="msg in messages"
        :key="msg.id"
        :msg="msg"
        :current-agent="currentAgent"
        :is-streaming="isStreaming"
        :is-last-user="msg.id === lastUserId"
        :is-last-assistant="msg.id === lastAssistantId"
        :editing="editingMsgId === msg.id"
        @copy="emit('copy', $event)"
        @edit-start="emit('edit-start', $event)"
        @edit-cancel="emit('edit-cancel')"
        @edit-confirm="emit('edit-confirm', $event)"
        @delete="emit('delete', $event)"
        @retry="emit('retry', $event)"
      />

      <!-- HITL 确认对话框（阶段三#4：倒计时 + 等待态引导） -->
      <div v-if="pendingConfirm" class="hitl-confirm">
        <NCard size="small" :bordered="true">
          <template #header>
            <div class="hitl-header">
              <NIcon class="hitl-header-icon"><AlertCircleOutline /></NIcon>
              <span>需要你的确认</span>
            </div>
          </template>
          <p class="hitl-desc">
            智能体请求执行工具
            <NTag size="small" type="warning" :bordered="false">
              {{ pendingConfirm.tool }}
            </NTag>
            。确认后将继续执行并接回本轮回复；拒绝则跳过该工具、继续对话。
          </p>
          <div v-if="hasVal(pendingConfirm.args)" class="tool-args">
            <span class="tool-label">参数</span>
            <pre class="tool-code">{{ formatToolValue(pendingConfirm.args) }}</pre>
          </div>
          <div class="hitl-countdown">
            <NProgress
              type="line"
              :percentage="Math.round(hitlCountdown / hitlTimeout * 100)"
              :height="4"
              :show-indicator="false"
              :status="hitlCountdown <= 10 ? 'warning' : 'success'"
            />
            <span class="hitl-countdown-text">{{ hitlCountdown }} 秒后未操作将自动拒绝</span>
          </div>
          <template #action>
            <NSpace>
              <NButton size="small" @click="emit('cancel-hitl')">
                取消并停止
              </NButton>
              <NButton type="error" size="small" @click="emit('confirm', false)">
                拒绝
              </NButton>
              <NButton type="primary" size="small" @click="emit('confirm', true)">
                确认执行
              </NButton>
            </NSpace>
          </template>
        </NCard>
      </div>
    </div>
    <!-- 回到底部悬浮按钮（上翻查看历史时出现） -->
    <div class="scroll-down-anchor">
      <NButton
        v-show="showScrollDown"
        circle
        secondary
        class="scroll-down-btn"
        @click="scrollToBottom(true)"
      >
        <template #icon>
          <NIcon><ArrowDownOutline /></NIcon>
        </template>
      </NButton>
    </div>
  </div>
</template>

<script setup>
import { AlertCircleOutline, ArrowDownOutline, SparklesOutline } from '@vicons/ionicons5'
import { NAvatar, NButton, NCard, NIcon, NProgress, NSpace, NTag } from 'naive-ui'
import { nextTick, ref } from 'vue'
import { colorFor, formatToolValue, hasVal } from './chat-utils'
import MessageBubble from './MessageBubble.vue'

defineOptions({ name: 'ChatMessageList' })

const props = defineProps({
  messages: { type: Array, default: () => [] },
  // 变更B#7：是否还有更早消息可上滑加载 / 是否正在加载
  hasMoreMessages: { type: Boolean, default: false },
  loadingMoreMessages: { type: Boolean, default: false },
  isStreaming: { type: Boolean, default: false },
  currentAgent: { type: Object, default: null },
  presetQuestions: { type: Array, default: () => [] },
  // 最新用户/助手消息 id：决定气泡上「编辑重发」「重新生成」按钮的显隐
  lastUserId: { type: [String, Number], default: null },
  lastAssistantId: { type: [String, Number], default: null },
  // 内联编辑中的消息 id（父组件持有）
  editingMsgId: { type: [String, Number], default: null },
  // HITL 待确认信息 { interruptId, tool, args }
  pendingConfirm: { type: Object, default: null },
  hitlCountdown: { type: Number, default: 0 },
  hitlTimeout: { type: Number, default: 30 },
})

const emit = defineEmits([
  'preset',
  'copy',
  'edit-start',
  'edit-cancel',
  'edit-confirm',
  'delete',
  'retry',
  'confirm',
  'cancel-hitl',
  'load-more-top',
])

// ============ 滚动锁定（用户上滑查看历史时不强制拉底） ============
const messageAreaRef = ref(null)
let autoScroll = true
// 是否显示「回到底部」悬浮按钮：有消息且当前未贴底
const showScrollDown = ref(false)

function isNearBottom() {
  const el = messageAreaRef.value
  if (!el)
    return true
  return el.scrollHeight - el.scrollTop - el.clientHeight < 120
}

function onScroll() {
  const el = messageAreaRef.value
  if (!el)
    return
  autoScroll = isNearBottom()
  showScrollDown.value = props.messages.length > 0 && !autoScroll
  // 接近顶部且还有更早消息时，请求上滑加载（变更B#7）
  if (el.scrollTop < 80 && props.hasMoreMessages && !props.loadingMoreMessages)
    emit('load-more-top')
}

// force=true 时同时把 autoScroll 复位为 true（等价于原来的「autoScroll=true; scrollToBottom(true)」）
function scrollToBottom(force) {
  if (force)
    autoScroll = true
  nextTick(() => {
    const el = messageAreaRef.value
    if (!el)
      return
    if (force || autoScroll) {
      el.scrollTop = el.scrollHeight
      showScrollDown.value = false
    }
  })
}

// 代码块「复制」按钮（v-html 内，事件委托）
function onMessageAreaClick(e) {
  const btn = e.target.closest?.('.code-copy-btn')
  if (!btn)
    return
  const code = decodeURIComponent(btn.dataset.code || '')
  navigator.clipboard?.writeText(code).then(() => {
    btn.textContent = '已复制'
    setTimeout(() => { btn.textContent = '复制' }, 1500)
  })
}

// 上滑头插更早消息时保持滚动锚点：插入前记录高度/位置，插入后按高度差回补 scrollTop（视口不跳动）
let prependAnchor = null
function beforePrepend() {
  const el = messageAreaRef.value
  prependAnchor = el ? { h: el.scrollHeight, t: el.scrollTop } : null
}
function afterPrepend() {
  const anchor = prependAnchor
  prependAnchor = null
  if (!anchor)
    return
  nextTick(() => {
    const el = messageAreaRef.value
    if (!el)
      return
    el.scrollTop = el.scrollHeight - anchor.h + anchor.t
  })
}

defineExpose({ scrollToBottom, beforePrepend, afterPrepend })
</script>

<style scoped>
.message-area {
  flex: 1;
  padding: 28px 24px 12px;
  overflow-y: auto;
  background: var(--surface);
  scrollbar-width: thin;
  scrollbar-color: rgba(100, 116, 139, 0.3) transparent;
}

.message-area::-webkit-scrollbar {
  width: 10px;
}

.message-area::-webkit-scrollbar-track {
  background: transparent;
}

.message-area::-webkit-scrollbar-thumb {
  background: rgba(100, 116, 139, 0.28);
  border-radius: 8px;
  border: 3px solid transparent;
  background-clip: content-box;
}

.message-area::-webkit-scrollbar-thumb:hover {
  background: rgba(100, 116, 139, 0.45);
  background-clip: content-box;
}

/* 回到底部悬浮按钮（sticky 贴滚动视口底部，height:0 不占布局） */
.scroll-down-anchor {
  position: sticky;
  bottom: 0;
  z-index: 5;
  display: flex;
  justify-content: center;
  height: 0;
  pointer-events: none;
}

.scroll-down-btn {
  pointer-events: auto;
  position: relative;
  bottom: 16px;
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.14);
}

/* 居中阅读列：长对话按文档式排版收进定宽列 */
.message-col {
  max-width: var(--chat-col);
  margin: 0 auto;
}

/* 上滑加载更早消息提示（变更B#7） */
.load-more-hint {
  padding: 8px 0 16px;
  text-align: center;
  font-size: 12px;
  color: var(--text-muted);
}

.load-more-tip {
  opacity: 0.7;
}

/* 欢迎空状态 */
.welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 62%;
  text-align: center;
  padding: 24px;
}

.welcome-title {
  margin: 18px 0 8px;
  font-size: 20px;
  font-weight: 600;
  color: var(--text-strong);
}

.welcome-greeting {
  margin: 0 0 22px;
  max-width: 540px;
  color: var(--text-body);
  font-size: 15px;
  line-height: 1.8;
}

.preset-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
  max-width: 620px;
}

.preset-chip {
  padding: 9px 16px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-soft);
  color: var(--text-body);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.preset-chip:hover {
  border-color: var(--primary);
  color: var(--primary);
  background: var(--primary-light);
}

/* HITL 确认块 */
.hitl-confirm {
  margin: 4px 0 20px;
  max-width: 460px;
}

.hitl-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
}

.hitl-header-icon {
  color: #f0a020;
  font-size: 18px;
}

.hitl-desc {
  margin: 0 0 10px;
  line-height: 1.7;
  color: var(--text-body);
}

.hitl-countdown {
  margin-top: 10px;
}

.hitl-countdown-text {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-muted);
}

/* HITL 参数展示（与工具卡片同款样式，此处独立一份） */
.tool-args {
  margin-top: 6px;
  color: var(--text-muted);
}

.tool-label {
  display: block;
  margin-bottom: 2px;
  font-weight: 500;
  color: var(--text-body);
}

.tool-code {
  margin: 0;
  max-height: 160px;
  padding: 8px 10px;
  overflow: auto;
  background: rgba(0, 0, 0, 0.04);
  border-radius: 8px;
  font-size: 11px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}

:global(.dark) .tool-code {
  background: color-mix(in srgb, var(--bg-primary) 5%, transparent);
}
</style>
