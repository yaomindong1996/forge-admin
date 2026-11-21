<script setup>
import { NButton, NSelect } from 'naive-ui'
import { computed, nextTick, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useHubAssistantStore } from '@/stores/ai/hubAssistantStore'
import { renderMarkdown, renderMarkdownStreaming } from '@/views/ai/components/agent-chat/chat-markdown'

const emit = defineEmits(['navigate', 'openAgents'])
const store = useHubAssistantStore()
const router = useRouter()
const listRef = ref(null)
const inputRef = ref(null)

const agentOptions = computed(() =>
  store.agents.map(item => ({
    label: item.name,
    value: item.code,
  })),
)

const recommendedPrompts = [
  '你好，介绍一下你可以帮我做什么？',
  '查看我的待办任务',
  '帮我梳理一份工作周报要点',
]

const dockActions = [
  { id: 'ask', label: '问个问题', icon: 'i-lucide:message-circle', text: '你好，你能帮我做什么？' },
  { id: 'todo', label: '查待办', icon: 'i-lucide:list-checks', text: '查看我的待办任务' },
  { id: 'agents', label: '管理智能体', icon: 'i-lucide:bot', action: 'agents' },
]

const lastTurn = computed(() => store.turns.at(-1))

watch(
  () => [store.turns.length, lastTurn.value?.answer, lastTurn.value?.status, lastTurn.value?.tools?.length],
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

function handleKeydown(event) {
  if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {
    event.preventDefault()
    void store.send()
  }
}

function runQuickAction(item) {
  if (item.action === 'agents') {
    emit('openAgents')
    return
  }
  if (!store.busy)
    void store.send(item.text)
}

function goTodoCenter() {
  router.push('/flow/todo')
  emit('navigate')
}

function chooseCandidate(turn, agent) {
  void store.send(turn.question, agent)
}

function renderReplyHtml(turn) {
  if (!turn.answer)
    return ''
  return turn.status === 'streaming'
    ? renderMarkdownStreaming(turn.answer)
    : renderMarkdown(turn.answer)
}

function toolStatusLabel(status) {
  if (status === 'error')
    return '失败'
  if (status === 'done')
    return '完成'
  return '执行中…'
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
          <button type="button" class="dock-tip" @click="emit('openAgents')">
            <div class="dock-tip-copy">
              <strong>AI 助手随时待命</strong>
              <span>
                直接提问调用已发布智能体，或前往管理配置
                <i class="i-lucide:chevron-right" aria-hidden="true" />
              </span>
            </div>
          </button>

          <div class="dock-recommend">
            <div class="dock-recommend-head">
              <span>为您推荐</span>
            </div>
            <div class="dock-prompt-list">
              <button
                v-for="prompt in recommendedPrompts"
                :key="prompt"
                type="button"
                class="dock-prompt"
                :disabled="store.busy"
                @click="store.send(prompt)"
              >
                <i class="i-lucide:sparkles" aria-hidden="true" />
                <span>{{ prompt }}</span>
              </button>
            </div>
          </div>
        </section>

        <article v-for="turn in store.turns" :key="turn.id" class="assistant-turn">
          <p class="user-message">
            {{ turn.question }}
          </p>

          <div class="assistant-reply">
            <span class="reply-avatar" aria-hidden="true">
              <i class="i-lucide:sparkles" />
            </span>
            <div class="reply-body">
              <div class="reply-byline">
                <strong>{{ turn.agent?.name || 'AI 助手' }}</strong>
                <span v-if="turn.agent">已分派</span>
                <span v-else-if="turn.status === 'routing'">匹配中…</span>
              </div>

              <details v-if="turn.tools.length" class="reply-tools" :open="turn.status === 'streaming'">
                <summary>工具执行 · {{ turn.tools.length }} 项</summary>
                <ul>
                  <li v-for="(tool, idx) in turn.tools" :key="`${tool.name}-${idx}`">
                    <span>{{ tool.name }}</span>
                    <small :class="`is-${tool.status}`">{{ toolStatusLabel(tool.status) }}</small>
                  </li>
                </ul>
              </details>

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

              <!-- eslint-disable vue/no-v-html -->
              <div
                v-else-if="turn.answer"
                class="reply-rich"
                v-html="renderReplyHtml(turn)"
              />
              <!-- eslint-enable vue/no-v-html -->

              <p v-else-if="turn.status === 'streaming'" class="reply-muted">
                正在思考并生成回复…
              </p>

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

      <form class="hub-composer" @submit.prevent="store.send()">
        <textarea
          ref="inputRef"
          v-model="store.draft"
          rows="1"
          class="hub-composer__input"
          placeholder="发消息，Enter 发送（Shift+Enter 换行）"
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
  min-height: 0;
  min-width: 0;
}

.hub-conversation-scroll {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  margin: 0 12px;
  overflow: auto;
  overscroll-behavior: contain;
  border: 1px solid color-mix(in srgb, #9eb6d6 28%, transparent);
  border-radius: 14px;
  background: color-mix(in srgb, var(--bg-primary, #fff) 96%, #eef4ff);
  box-shadow: 0 6px 18px color-mix(in srgb, #1a3a6e 6%, transparent);
}

:global(.dark) .hub-conversation-scroll {
  border-color: var(--border-light);
  background: color-mix(in srgb, var(--bg-primary) 94%, transparent);
}

.hub-agent-strip {
  position: sticky;
  top: 0;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 10px;
  border-bottom: 1px solid color-mix(in srgb, var(--border-light, #e5e7eb) 85%, transparent);
  background: color-mix(in srgb, var(--bg-primary, #fff) 92%, transparent);
  backdrop-filter: blur(6px);
}

.hub-agent-strip__icon {
  flex: none;
  color: var(--primary-color, #2563eb);
  font-size: 14px;
}

.hub-agent-strip__select {
  flex: 1;
  min-width: 0;
}

.hub-conversation-inner {
  display: grid;
  gap: 16px;
  width: 100%;
  padding: 14px;
  box-sizing: border-box;
}

.hub-welcome {
  display: grid;
  gap: 16px;
  padding: 2px 0 4px;
}

.dock-tip {
  display: block;
  width: 100%;
  padding: 13px 14px;
  border: 1px solid color-mix(in srgb, #9eb6d6 30%, transparent);
  border-radius: 12px;
  background: var(--bg-primary, #fff);
  color: inherit;
  font: inherit;
  text-align: start;
  cursor: pointer;
  transition: border-color 150ms ease;
}

.dock-tip:hover {
  border-color: color-mix(in srgb, var(--primary-color, #2563eb) 36%, transparent);
}

.dock-tip-copy {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.dock-tip-copy strong {
  font-size: 14px;
  font-weight: 650;
  color: var(--text-primary);
}

.dock-tip-copy span {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  color: var(--text-tertiary);
  font-size: 12px;
}

.dock-recommend-head {
  margin-bottom: 8px;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 500;
}

.dock-prompt-list {
  display: grid;
  gap: 8px;
}

.dock-prompt {
  display: flex;
  align-items: center;
  gap: 9px;
  min-height: 40px;
  padding: 8px 13px;
  border: 1px solid color-mix(in srgb, #9eb6d6 28%, transparent);
  border-radius: 999px;
  background: var(--bg-primary, #fff);
  color: var(--text-primary);
  font: inherit;
  font-size: 13px;
  line-height: 1.4;
  text-align: start;
  cursor: pointer;
  transition:
    border-color 150ms ease,
    background-color 150ms ease;
}

.dock-prompt i {
  flex: none;
  color: var(--primary-color, #2563eb);
  font-size: 14px;
}

.dock-prompt:hover:not(:disabled) {
  border-color: color-mix(in srgb, var(--primary-color, #2563eb) 38%, transparent);
  background: color-mix(in srgb, var(--primary-color, #2563eb) 4%, var(--bg-primary, #fff));
}

.dock-prompt:disabled {
  opacity: 0.5;
  cursor: not-allowed;
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
  display: grid;
  flex: none;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 9px;
  background: color-mix(in srgb, var(--primary-color, #2563eb) 12%, var(--bg-primary, #fff));
  color: var(--primary-color, #2563eb);
  font-size: 14px;
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

.reply-tools {
  margin-top: 6px;
  padding: 6px 10px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-secondary);
  color: var(--text-secondary);
  font-size: 12px;
}

.reply-tools summary {
  cursor: pointer;
}

.reply-tools ul {
  display: grid;
  gap: 4px;
  margin: 6px 0 0;
  padding: 0;
  list-style: none;
}

.reply-tools li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.reply-tools small.is-done {
  color: #16a34a;
}

.reply-tools small.is-error {
  color: var(--error-color, #dc2626);
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

.reply-rich :deep(code) {
  padding: 1px 5px;
  border-radius: 4px;
  background: var(--bg-secondary);
  font-size: 12px;
}

.reply-rich :deep(pre) {
  margin: 6px 0;
  padding: 10px;
  overflow: auto;
  border-radius: 8px;
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
  flex: none;
  width: 100%;
  padding: 10px 12px 12px;
  box-sizing: border-box;
}

.dock-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}

.dock-action {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-height: 30px;
  padding: 0 11px;
  border: 1px solid color-mix(in srgb, #8aa6c8 35%, transparent);
  border-radius: 999px;
  background: color-mix(in srgb, var(--bg-primary, #fff) 88%, transparent);
  color: var(--text-primary);
  font: inherit;
  font-size: 12px;
  cursor: pointer;
  transition:
    border-color 140ms ease,
    background-color 140ms ease;
}

.dock-action i {
  color: var(--primary-color, #2563eb);
  font-size: 13px;
}

.dock-action:hover:not(:disabled) {
  border-color: color-mix(in srgb, var(--primary-color, #2563eb) 36%, transparent);
  background: var(--bg-primary, #fff);
}

.dock-action:disabled {
  opacity: 0.48;
  cursor: not-allowed;
}

.hub-composer {
  display: flex;
  align-items: flex-end;
  gap: 6px;
  min-height: 46px;
  padding: 7px 7px 7px 14px;
  border: 1px solid color-mix(in srgb, #8aa6c8 38%, transparent);
  border-radius: 23px;
  background: var(--bg-primary, #fff);
  box-shadow: 0 4px 14px color-mix(in srgb, #1a3a6e 8%, transparent);
  transition:
    border-color 150ms ease,
    box-shadow 150ms ease;
}

.hub-composer:focus-within {
  border-color: color-mix(in srgb, var(--primary-color, #2563eb) 48%, transparent);
  box-shadow: 0 6px 18px color-mix(in srgb, var(--primary-color, #2563eb) 14%, transparent);
}

.hub-composer__input {
  flex: 1;
  min-width: 0;
  max-height: 110px;
  padding: 5px 0;
  border: 0;
  outline: none;
  resize: none;
  background: transparent;
  color: var(--text-primary);
  font: inherit;
  font-size: 13px;
  line-height: 1.5;
}

.hub-composer__input::placeholder {
  color: var(--text-tertiary);
}

.hub-composer__toolbar {
  display: flex;
  flex: none;
  align-items: center;
}

.composer-send {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: var(--primary-color, #2563eb);
  color: #fff;
  font-size: 16px;
  cursor: pointer;
  transition:
    background-color 140ms ease,
    opacity 140ms ease;
}

.composer-send:hover:not(:disabled) {
  background: color-mix(in srgb, var(--primary-color, #2563eb) 88%, #000);
}

.composer-send:disabled {
  background: color-mix(in srgb, var(--text-primary) 10%, transparent);
  color: var(--text-tertiary);
  cursor: not-allowed;
}

.composer-send.is-stop {
  background: color-mix(in srgb, var(--text-primary) 14%, transparent);
  color: var(--text-primary);
  font-size: 13px;
}

.dock-disclaimer {
  margin: 8px 4px 0;
  color: var(--text-tertiary);
  font-size: 11px;
  line-height: 1.4;
  text-align: center;
}
</style>
