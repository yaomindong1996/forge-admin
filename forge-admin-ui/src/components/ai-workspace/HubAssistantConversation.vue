<script setup>
import { NButton, NSelect } from 'naive-ui'
import { computed, nextTick, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import assistantAvatar from '@/assets/ai/assistant-avatar.jpg'
import {
  assistantPageDirectory,
  currentAssistantPage,
  hasApplicationPermission,
} from '@/composables/ai/assistant-page-context'
import { usePermissionStore, useUserStore } from '@/store'
import { useHubAssistantStore } from '@/stores/ai/hubAssistantStore'
import { formatDuration } from '@/views/ai/components/agent-chat/chat-utils'
import { renderMarkdown, renderMarkdownStreaming } from '@/views/ai/components/agent-chat/chat-markdown'
import ChatRunTrace from '@/views/ai/components/agent-chat/ChatRunTrace.vue'
import HubPageGuide from './HubPageGuide.vue'
import 'highlight.js/styles/atom-one-dark.css'

const emit = defineEmits(['navigate', 'openAgents', 'locate'])
const store = useHubAssistantStore()
const router = useRouter()
const route = useRoute()
const permission = usePermissionStore()
const user = useUserStore()
const listRef = ref(null)
const inputRef = ref(null)
const entries = computed(() => assistantPageDirectory(permission, user))
const page = computed(() => currentAssistantPage(route, entries.value))
const can = code => hasApplicationPermission(user, permission, code)
const guideContext = computed(() => ({ page: page.value, entries: entries.value, can }))

const agentOptions = computed(() =>
  store.agents.map(item => ({
    label: item.name,
    value: item.code,
  })),
)

const recommendedPrompts = computed(() => [
  {
    id: 'guide',
    title: page.value ? '这个页面怎么用？' : '你好，介绍一下你可以帮我做什么？',
    desc: page.value ? '快速了解当前页操作入口与使用方式' : '了解助手能力与常用提问方式',
    icon: 'i-lucide:sparkles',
    tone: 'blue',
    text: page.value ? '这个页面怎么用？' : '你好，介绍一下你可以帮我做什么？',
  },
  {
    id: 'role',
    title: '角色怎么配置？',
    desc: '权限分配、角色绑定与常见配置路径',
    icon: 'i-lucide:layers',
    tone: 'purple',
    text: '角色怎么配置？',
  },
  {
    id: 'todo',
    title: '查看我的待办任务',
    desc: '查看待签收与待处理的审批任务',
    icon: 'i-lucide:clipboard-list',
    tone: 'green',
    text: '查看我的待办任务',
  },
])

const dockActions = computed(() => [
  page.value && { id: 'guide', label: '这个页面怎么用', icon: 'i-lucide:map-pin', text: '这个页面怎么用？' },
  { id: 'ask', label: '问个问题', icon: 'i-lucide:message-circle', text: '你好，你能帮我做什么？' },
  { id: 'todo', label: '查待办', icon: 'i-lucide:list-checks', text: '查看我的待办任务' },
  { id: 'agents', label: '管理智能体', icon: 'i-lucide:bot', action: 'agents' },
].filter(Boolean))

const lastTurn = computed(() => store.turns.at(-1))

watch(
  () => [
    store.turns.length,
    lastTurn.value?.answer,
    lastTurn.value?.thinking,
    lastTurn.value?.status,
    lastTurn.value?.tools?.length,
    lastTurn.value?.resources?.skills?.length,
    lastTurn.value?.resources?.knowledge?.length,
  ],
  async () => {
    await nextTick()
    if (listRef.value)
      listRef.value.scrollTop = listRef.value.scrollHeight
  },
  { flush: 'post' },
)

function focus() {
  inputRef.value?.focus?.()
}

function sendMessage(text, agent = null) {
  void store.send(text, agent, guideContext.value)
}

function handleKeydown(event) {
  if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {
    event.preventDefault()
    sendMessage()
  }
}

function runQuickAction(item) {
  if (item.action === 'agents') {
    emit('openAgents')
    return
  }
  if (!store.busy)
    sendMessage(item.text)
}

function goTodoCenter() {
  router.push('/flow/todo')
  emit('navigate')
}

function chooseCandidate(turn, agent) {
  const catalog = store.agents.find(item => item.code === agent?.code)
  sendMessage(turn.question, catalog ? { ...catalog, ...agent } : agent)
}

function renderReplyHtml(turn) {
  if (!turn.answer)
    return ''
  return turn.status === 'streaming'
    ? renderMarkdownStreaming(turn.answer)
    : renderMarkdown(turn.answer)
}

function onReplyClick(event) {
  const btn = event.target?.closest?.('.code-copy-btn')
  if (!btn)
    return
  const code = decodeURIComponent(btn.dataset.code || '')
  navigator.clipboard?.writeText(code).then(() => {
    btn.textContent = '已复制'
    setTimeout(() => { btn.textContent = '复制' }, 1500)
  }).catch(() => {})
}

async function copyTurnAnswer(turn) {
  const text = String(turn?.answer || '').trim()
  if (!text)
    return
  try {
    await navigator.clipboard.writeText(text)
  }
  catch {
    // ignore
  }
}

function hasReplyMeta(turn) {
  return turn?.firstTokenMs != null || turn?.totalMs != null || turn?.tokenUsage != null
}

defineExpose({ focus })
</script>

<template>
  <section class="hub-conversation">
    <div ref="listRef" class="hub-conversation-scroll">
      <div class="hub-agent-strip">
        <i class="hub-agent-strip__icon i-lucide:bot" aria-hidden="true" />
        <NSelect
          v-model:value="store.selectedAgentCode"
          :options="agentOptions"
          clearable
          filterable
          size="small"
          :bordered="false"
          class="hub-agent-strip__select"
          placeholder="自动路由智能体（也可手动指定）"
        />
      </div>

      <div class="hub-conversation-inner">
        <section v-if="!store.turns.length" class="hub-welcome">
          <button type="button" class="dock-hero" @click="emit('openAgents')">
            <span class="dock-hero-avatar" aria-hidden="true">
              <img :src="assistantAvatar" alt="" draggable="false">
            </span>
            <div class="dock-hero-copy">
              <strong>AI 助手随时待命</strong>
              <span>
                直接提问调用已发布智能体，或前往管理配置
                <i class="i-lucide:chevron-right" aria-hidden="true" />
              </span>
            </div>
            <span class="dock-hero-art" aria-hidden="true">
              <span class="hero-window hero-window--back" />
              <span class="hero-window hero-window--front" />
              <i class="hero-sparkle i-lucide:sparkles" />
            </span>
          </button>

          <section class="dock-recommend" aria-label="为您推荐">
            <header class="dock-recommend-head">
              <span class="dock-recommend-mark" aria-hidden="true" />
              为您推荐
            </header>
            <div class="dock-card-list">
              <button
                v-for="item in recommendedPrompts"
                :key="item.id"
                type="button"
                class="dock-card"
                :class="`is-${item.tone}`"
                :disabled="store.busy"
                @click="sendMessage(item.text)"
              >
                <span class="dock-card-icon" aria-hidden="true">
                  <i :class="item.icon" />
                </span>
                <span class="dock-card-copy">
                  <strong>{{ item.title }}</strong>
                  <small>{{ item.desc }}</small>
                </span>
                <span class="dock-card-art" aria-hidden="true">
                  <span class="card-blob" />
                  <span class="card-shape" />
                </span>
                <i class="dock-card-arrow i-lucide:chevron-right" aria-hidden="true" />
              </button>
            </div>
          </section>
        </section>

        <article v-for="turn in store.turns" :key="turn.id" class="assistant-turn">
          <p class="user-message">
            {{ turn.question }}
          </p>

          <div class="assistant-reply">
            <span class="reply-avatar" aria-hidden="true">
              <img :src="assistantAvatar" alt="" draggable="false">
            </span>
            <div class="reply-body">
              <div class="reply-byline">
                <strong>{{ turn.kind === 'GUIDE' ? '操作指引' : (turn.agent?.name || 'AI 助手') }}</strong>
                <span v-if="turn.kind === 'GUIDE'">本地定位</span>
                <span v-else-if="turn.agent">已分派</span>
                <span v-else-if="turn.status === 'routing'">匹配中…</span>
              </div>

              <details
                v-if="turn.thinking"
                class="reply-thinking"
                :open="turn.status === 'streaming' && !turn.answer"
              >
                <summary>思考过程</summary>
                <pre>{{ turn.thinking }}</pre>
              </details>

              <ChatRunTrace
                :tools="turn.tools || []"
                :resources="turn.resources || {}"
              />

              <p v-if="turn.status === 'routing'" class="reply-muted">
                正在识别意图并匹配智能体…
              </p>

              <div v-else-if="turn.kind === 'TODO' && turn.todos" class="reply-todos">
                <p v-if="!turn.todos.available" class="reply-muted">
                  {{ turn.todos.message }}
                </p>
                <template v-else>
                  <div class="reply-todo-head">
                    <span>当前共有 <strong>{{ turn.todos.total }}</strong> 条待办任务</span>
                    <NButton text type="primary" size="tiny" @click="goTodoCenter">
                      前往待办中心
                    </NButton>
                  </div>
                  <ul v-if="turn.todos.items?.length" class="reply-todo-list">
                    <li v-for="item in turn.todos.items" :key="item.taskId">
                      <strong>{{ item.processName || item.taskName }}</strong>
                      <span>{{ item.taskName }}</span>
                    </li>
                  </ul>
                  <p v-else class="reply-muted">
                    暂无待处理任务
                  </p>
                </template>
              </div>

              <div v-else-if="turn.guide" class="reply-guide">
                <p class="reply-muted">
                  已根据你的权限菜单匹配到以下入口，点击后会打开页面并圈选对应操作。
                </p>
                <HubPageGuide :guide="turn.guide" @locate="emit('locate')" />
              </div>

              <template v-else-if="turn.answer || turn.status === 'streaming'">
                <!-- eslint-disable vue/no-v-html -->
                <div
                  v-if="turn.answer"
                  class="reply-rich markdown-body"
                  @click="onReplyClick"
                  v-html="renderReplyHtml(turn)"
                />
                <!-- eslint-enable vue/no-v-html -->
                <p v-else class="reply-muted">
                  正在思考并生成回复…
                </p>
                <span
                  v-if="turn.status === 'streaming' && turn.answer"
                  class="stream-cursor"
                  aria-hidden="true"
                />

                <div v-if="hasReplyMeta(turn)" class="reply-meta">
                  <span v-if="turn.firstTokenMs != null">首字 {{ turn.firstTokenMs }}ms</span>
                  <span v-if="turn.totalMs != null">总耗时 {{ formatDuration(turn.totalMs) }}</span>
                  <span v-if="turn.tokenUsage != null">Token {{ turn.tokenUsage }}</span>
                </div>

                <div v-if="turn.answer && turn.status !== 'streaming'" class="reply-actions">
                  <button type="button" class="reply-action-btn" @click="copyTurnAnswer(turn)">
                    复制
                  </button>
                </div>
              </template>

              <div v-if="turn.candidates?.length" class="reply-candidates">
                <button
                  v-for="cand in turn.candidates"
                  :key="cand.code"
                  type="button"
                  class="dock-action"
                  @click="chooseCandidate(turn, cand)"
                >
                  <i class="i-lucide:bot" aria-hidden="true" />
                  {{ cand.name }}
                </button>
              </div>

              <p v-if="turn.error" class="reply-error" role="alert">
                {{ turn.error }}
              </p>
            </div>
          </div>
        </article>
      </div>
    </div>

    <footer class="hub-conversation-footer">
      <div class="dock-actions" aria-label="快捷入口">
        <button
          v-for="action in dockActions"
          :key="action.id"
          type="button"
          class="dock-action"
          :disabled="store.busy && !action.action"
          @click="runQuickAction(action)"
        >
          <i :class="action.icon" aria-hidden="true" />
          <span>{{ action.label }}</span>
        </button>
      </div>

      <div v-if="page" class="current-page-context" :title="page.trail.join(' › ')">
        <i class="i-lucide:panel-top" aria-hidden="true" />
        <span>当前页面 · {{ page.title }}</span>
      </div>

      <form class="hub-composer" @submit.prevent="sendMessage()">
        <textarea
          ref="inputRef"
          v-model="store.draft"
          rows="1"
          class="hub-composer__input"
          placeholder="问问 「角色怎么配置」「用户在哪里新增」"
          :disabled="store.busy"
          @keydown="handleKeydown"
        />
        <div class="hub-composer__toolbar">
          <button
            v-if="store.busy"
            type="button"
            class="composer-send is-stop"
            aria-label="停止回复"
            title="停止回复"
            @click="store.stopStreaming"
          >
            <i class="i-lucide:square" aria-hidden="true" />
          </button>
          <button
            v-else
            type="submit"
            class="composer-send"
            aria-label="发送消息"
            title="发送消息"
            :disabled="!store.draft.trim()"
          >
            <i class="i-lucide:arrow-up" aria-hidden="true" />
          </button>
        </div>
      </form>

      <p class="dock-disclaimer">
        AI 生成内容仅供参考，请结合实际业务核实。
      </p>
    </footer>
  </section>
</template>

<style scoped>
.hub-conversation {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 8px;
  min-height: 0;
  min-width: 0;
}

/* 上：内容区吃满剩余高度并可滚动；下：操作区固定高度，避免被截断或中间留白 */
.hub-conversation-scroll {
  display: flex;
  flex: 1 1 0;
  flex-direction: column;
  min-height: 0;
  margin: 0 16px;
  overflow: auto;
  overscroll-behavior: contain;
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(14px);
  box-shadow:
    0 12px 30px color-mix(in srgb, #1a3a6e 10%, transparent),
    inset 0 1px 0 rgba(255, 255, 255, 0.85);
}

:global(.dark) .hub-conversation-scroll {
  border-color: var(--border-light);
  background: color-mix(in srgb, var(--bg-primary) 88%, transparent);
  box-shadow: none;
}

.hub-agent-strip {
  position: sticky;
  top: 0;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 44px;
  margin: 12px 12px 0;
  padding: 4px 12px;
  border: 1px solid color-mix(in srgb, #94a9c7 16%, transparent);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 2px 8px color-mix(in srgb, #1a3a6e 5%, transparent);
}

.hub-agent-strip__icon {
  flex: none;
  color: var(--primary-color, #2563eb);
  font-size: 16px;
}

.hub-agent-strip__select {
  flex: 1;
  min-width: 0;
}

.hub-agent-strip__select :deep(.n-base-selection) {
  --n-height: 34px !important;
  --n-font-size: 13px !important;
  --n-border-radius: 10px !important;
}

.hub-conversation-inner {
  display: grid;
  gap: 14px;
  width: 100%;
  padding: 12px 14px 16px;
  box-sizing: border-box;
}

.hub-welcome {
  display: grid;
  gap: 14px;
}

.dock-hero {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  min-height: 84px;
  padding: 14px 16px;
  overflow: hidden;
  border: 1px solid color-mix(in srgb, var(--primary-color, #2563eb) 16%, #c9daf5);
  border-radius: 16px;
  background:
    radial-gradient(120% 140% at 88% 20%, rgba(147, 197, 253, 0.55), transparent 48%),
    linear-gradient(120deg, #e8f1ff 0%, #f4f8ff 48%, #eef4ff 100%);
  color: inherit;
  font: inherit;
  text-align: start;
  cursor: pointer;
  box-shadow: 0 8px 20px color-mix(in srgb, #3b82f6 12%, transparent);
  transition:
    transform 150ms ease,
    box-shadow 150ms ease;
}

.dock-hero:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 26px color-mix(in srgb, #3b82f6 16%, transparent);
}

.dock-hero-avatar {
  position: relative;
  z-index: 1;
  flex: none;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  overflow: hidden;
  box-shadow:
    0 0 0 2px rgba(255, 255, 255, 0.9),
    0 4px 12px color-mix(in srgb, var(--primary-color, #2563eb) 22%, transparent);
}

.dock-hero-avatar img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
  object-position: center 18%;
}

.dock-hero-copy {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 5px;
  min-width: 0;
  flex: 1;
}

.dock-hero-copy strong {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.dock-hero-copy span {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.45;
}

.dock-hero-art {
  position: absolute;
  right: 14px;
  top: 50%;
  z-index: 0;
  width: 72px;
  height: 54px;
  transform: translateY(-50%);
  pointer-events: none;
}

.hero-window {
  position: absolute;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.85);
  box-shadow: 0 6px 14px rgba(37, 99, 235, 0.18);
}

.hero-window--back {
  right: 0;
  top: 4px;
  width: 48px;
  height: 38px;
  background: linear-gradient(160deg, #bfdbfe, #93c5fd);
  transform: rotate(8deg);
}

.hero-window--front {
  left: 4px;
  bottom: 2px;
  width: 44px;
  height: 34px;
  background: linear-gradient(160deg, #fff, #dbeafe);
  transform: rotate(-6deg);
}

.hero-sparkle {
  position: absolute;
  right: 2px;
  top: -2px;
  color: #60a5fa;
  font-size: 16px;
  filter: drop-shadow(0 2px 4px rgba(59, 130, 246, 0.35));
}

.dock-recommend-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  color: #334155;
  font-size: 13px;
  font-weight: 650;
}

.dock-recommend-mark {
  width: 4px;
  height: 14px;
  border-radius: 999px;
  background: linear-gradient(180deg, #60a5fa, #2563eb);
}

.dock-card-list {
  display: grid;
  gap: 10px;
}

.dock-card {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr) 52px 16px;
  align-items: center;
  gap: 10px;
  width: 100%;
  min-height: 72px;
  padding: 12px 12px 12px 12px;
  border: 1px solid color-mix(in srgb, #94a3b8 16%, transparent);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.94);
  color: inherit;
  font: inherit;
  text-align: start;
  cursor: pointer;
  box-shadow: 0 6px 16px color-mix(in srgb, #1e293b 6%, transparent);
  transition:
    transform 150ms ease,
    border-color 150ms ease,
    box-shadow 150ms ease;
}

.dock-card:hover:not(:disabled) {
  transform: translateY(-1px);
  border-color: color-mix(in srgb, var(--primary-color, #2563eb) 28%, transparent);
  box-shadow: 0 10px 22px color-mix(in srgb, #2563eb 12%, transparent);
}

.dock-card:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.dock-card-icon {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  font-size: 18px;
}

.dock-card.is-blue .dock-card-icon {
  background: #eff6ff;
  color: #2563eb;
}

.dock-card.is-purple .dock-card-icon {
  background: #f5f3ff;
  color: #7c3aed;
}

.dock-card.is-green .dock-card-icon {
  background: #ecfdf5;
  color: #059669;
}

.dock-card-copy {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.dock-card-copy strong {
  overflow: hidden;
  color: #0f172a;
  font-size: 13px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dock-card-copy small {
  overflow: hidden;
  color: #94a3b8;
  font-size: 11px;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dock-card-art {
  position: relative;
  width: 52px;
  height: 40px;
  overflow: hidden;
}

.card-blob {
  position: absolute;
  inset: 6px 4px 4px 8px;
  border-radius: 12px;
  opacity: 0.9;
}

.card-shape {
  position: absolute;
  right: 2px;
  top: 4px;
  width: 28px;
  height: 28px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 4px 10px rgba(15, 23, 42, 0.08);
}

.dock-card.is-blue .card-blob {
  background: linear-gradient(135deg, #dbeafe, #bfdbfe);
}

.dock-card.is-blue .card-shape {
  background: linear-gradient(160deg, #fff, #93c5fd);
  transform: rotate(8deg);
}

.dock-card.is-purple .card-blob {
  background: linear-gradient(135deg, #ede9fe, #ddd6fe);
}

.dock-card.is-purple .card-shape {
  background: linear-gradient(160deg, #fff, #c4b5fd);
  transform: rotate(-8deg);
}

.dock-card.is-green .card-blob {
  background: linear-gradient(135deg, #d1fae5, #a7f3d0);
}

.dock-card.is-green .card-shape {
  background: linear-gradient(160deg, #fff, #6ee7b7);
  transform: rotate(6deg);
}

.dock-card-arrow {
  color: #cbd5e1;
  font-size: 16px;
}

.dock-card:hover:not(:disabled) .dock-card-arrow {
  color: var(--primary-color, #2563eb);
}

.assistant-turn {
  display: grid;
  gap: 12px;
  font-size: 13px;
  line-height: 1.65;
}

.user-message {
  justify-self: end;
  max-width: 86%;
  margin: 0;
  padding: 9px 13px;
  border-radius: 14px 14px 4px 14px;
  background: color-mix(in srgb, var(--primary-color, #2563eb) 11%, var(--bg-primary, #fff));
  color: var(--text-primary);
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.assistant-reply {
  display: flex;
  align-items: flex-start;
  gap: 9px;
  min-width: 0;
}

.reply-avatar {
  display: inline-flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  overflow: hidden;
  box-shadow: 0 0 0 1.5px color-mix(in srgb, var(--primary-color, #2563eb) 24%, transparent);
}

.reply-avatar img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
  object-position: center 18%;
}

.reply-body {
  flex: 1;
  min-width: 0;
  padding-top: 1px;
}

.reply-byline {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-secondary);
  font-size: 12px;
}

.reply-byline span {
  font-size: 11px;
  color: var(--text-tertiary);
}

.reply-rich {
  margin-top: 4px;
  color: var(--text-primary);
  font-size: 13px;
  line-height: 1.68;
  word-break: break-word;
}

.reply-rich :deep(p) {
  margin: 0 0 8px;
}

.reply-rich :deep(p:last-child) {
  margin-bottom: 0;
}

.reply-rich :deep(ul),
.reply-rich :deep(ol) {
  margin: 4px 0 8px;
  padding-left: 18px;
}

.reply-rich :deep(:not(pre) > code) {
  padding: 1px 5px;
  border-radius: 4px;
  background: color-mix(in srgb, var(--primary-color, #3b82f6) 10%, transparent);
  font-size: 12px;
}

.reply-rich :deep(.code-block) {
  margin: 10px 0;
  overflow: hidden;
  border-radius: 10px;
  background: #282c34;
}

.reply-rich :deep(.code-block-header) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 7px 14px;
  background: color-mix(in srgb, #fff 6%, transparent);
  color: #abb2bf;
  font-size: 11px;
}

.reply-rich :deep(.code-copy-btn) {
  padding: 2px 10px;
  border: none;
  border-radius: 5px;
  background: color-mix(in srgb, #fff 12%, transparent);
  color: #dfe4ee;
  font-size: 11px;
  cursor: pointer;
}

.reply-rich :deep(.code-copy-btn:hover) {
  background: color-mix(in srgb, #fff 22%, transparent);
}

.reply-rich :deep(.code-block pre) {
  margin: 0;
  padding: 14px 16px;
  overflow-x: auto;
  background: transparent;
}

.reply-rich :deep(.code-block code) {
  padding: 0;
  background: transparent;
  color: inherit;
  font-size: 12px;
  line-height: 1.65;
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
}

.reply-thinking {
  margin: 4px 0 8px;
  padding: 6px 10px;
  border: 1px solid var(--border-light, var(--border));
  border-radius: 8px;
  background: var(--bg-secondary);
  color: var(--text-secondary);
  font-size: 12px;
}

.reply-thinking summary {
  cursor: pointer;
  user-select: none;
}

.reply-thinking pre {
  margin: 6px 0 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
  color: var(--text-tertiary);
}

.stream-cursor {
  display: inline-block;
  width: 7px;
  height: 14px;
  margin-left: 2px;
  vertical-align: text-bottom;
  background: var(--primary-color, #2563eb);
  animation: hub-stream-blink 1s steps(1) infinite;
}

@keyframes hub-stream-blink {
  50% { opacity: 0; }
}

.reply-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 6px;
  color: var(--text-tertiary);
  font-size: 11px;
}

.reply-actions {
  display: flex;
  gap: 6px;
  margin-top: 6px;
}

.reply-action-btn {
  padding: 2px 8px;
  border: 1px solid var(--border-light, var(--border));
  border-radius: 6px;
  background: transparent;
  color: var(--text-secondary);
  font-size: 11px;
  cursor: pointer;
}

.reply-action-btn:hover {
  color: var(--text-primary);
  background: var(--bg-secondary);
}

.reply-muted {
  margin: 4px 0 0;
  color: var(--text-tertiary);
  font-size: 12px;
}

.reply-error {
  margin: 6px 0 0;
  padding: 6px 10px;
  border-radius: 8px;
  background: color-mix(in srgb, var(--error-color, #dc2626) 8%, var(--bg-primary));
  color: var(--error-color, #dc2626);
  font-size: 12px;
}

.reply-todos {
  margin-top: 6px;
  padding: 10px 12px;
  border: 1px solid var(--border-light);
  border-radius: 10px;
  background: var(--bg-secondary);
}

.reply-todo-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 12px;
}

.reply-todo-list {
  display: grid;
  gap: 6px;
  margin: 0;
  padding: 0;
  list-style: none;
  font-size: 12px;
}

.reply-todo-list li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 6px;
  background: var(--bg-primary);
}

.reply-todo-list span {
  color: var(--text-tertiary);
}

.reply-candidates {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.hub-conversation-footer {
  flex: 0 0 auto;
  width: 100%;
  padding: 0 16px 14px;
  box-sizing: border-box;
}

.current-page-context {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-bottom: 8px;
  color: var(--text-secondary, #4b5563);
  font-size: 12px;
  font-weight: 500;
}

.current-page-context i {
  flex: none;
  width: 14px;
  height: 14px;
  color: var(--primary-color, #2563eb);
}

.current-page-context span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.reply-guide {
  margin-top: 4px;
}

.dock-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.dock-action {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 34px;
  padding: 0 13px;
  border: 1px solid color-mix(in srgb, var(--primary-color, #2563eb) 28%, #b7c9e4);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.88);
  color: var(--text-primary, #111827);
  font: inherit;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  box-shadow: 0 1px 3px color-mix(in srgb, #1a3a6e 5%, transparent);
  transition:
    border-color 140ms ease,
    background-color 140ms ease,
    box-shadow 140ms ease;
}

.dock-action i {
  color: var(--primary-color, #2563eb);
  font-size: 14px;
}

.dock-action:hover:not(:disabled) {
  border-color: color-mix(in srgb, var(--primary-color, #2563eb) 48%, transparent);
  background: #fff;
  box-shadow: 0 3px 10px color-mix(in srgb, var(--primary-color, #2563eb) 12%, transparent);
}

.dock-action:disabled {
  opacity: 0.48;
  cursor: not-allowed;
}

.hub-composer {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  min-height: 56px;
  padding: 10px 10px 10px 18px;
  border: 1px solid color-mix(in srgb, #8aa6c8 32%, transparent);
  border-radius: 28px;
  background: #fff;
  box-shadow:
    0 8px 22px color-mix(in srgb, #1a3a6e 10%, transparent),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  transition:
    border-color 150ms ease,
    box-shadow 150ms ease;
}

.hub-composer:focus-within {
  border-color: color-mix(in srgb, var(--primary-color, #2563eb) 50%, transparent);
  box-shadow: 0 10px 24px color-mix(in srgb, var(--primary-color, #2563eb) 16%, transparent);
}

.hub-composer__input {
  flex: 1;
  min-width: 0;
  max-height: 110px;
  padding: 6px 0;
  border: 0;
  outline: none;
  resize: none;
  background: transparent;
  color: var(--text-primary);
  font: inherit;
  font-size: 14px;
  line-height: 1.5;
}

.hub-composer__input::placeholder {
  color: var(--text-tertiary, #9ca3af);
}

.hub-composer__toolbar {
  display: flex;
  flex: none;
  align-items: center;
}

.composer-send {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: linear-gradient(180deg, #3b82f6, #2563eb);
  color: #fff;
  font-size: 17px;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.28);
  transition:
    background-color 140ms ease,
    opacity 140ms ease,
    box-shadow 140ms ease;
}

.composer-send:hover:not(:disabled) {
  background: linear-gradient(180deg, #2563eb, #1d4ed8);
  box-shadow: 0 6px 14px rgba(37, 99, 235, 0.34);
}

.composer-send:disabled {
  background: #93c5fd;
  color: #fff;
  cursor: not-allowed;
  box-shadow: none;
  opacity: 0.85;
}

.composer-send.is-stop {
  background: color-mix(in srgb, var(--text-primary) 14%, transparent);
  color: var(--text-primary);
  font-size: 13px;
}

.dock-disclaimer {
  margin: 10px 4px 0;
  color: var(--text-tertiary, #9ca3af);
  font-size: 11px;
  line-height: 1.45;
  text-align: center;
}
</style>
