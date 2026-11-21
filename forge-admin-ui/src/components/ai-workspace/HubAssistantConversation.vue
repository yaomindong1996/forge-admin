<script setup>
import { NButton, NInput, NSelect, NTag } from 'naive-ui'
import { computed, nextTick, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useHubAssistantStore } from '@/stores/ai/hubAssistantStore'

const emit = defineEmits(['navigate'])
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

const quickPrompts = [
  '查看我的待办任务',
  '介绍一下你可以帮我做什么',
]

watch(() => store.turns.length, async () => {
  await nextTick()
  if (listRef.value)
    listRef.value.scrollTop = listRef.value.scrollHeight
})

function focus() {
  inputRef.value?.focus?.()
}

function handleKeydown(event) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    void store.send()
  }
}

function goTodoCenter() {
  router.push('/flow/todo')
  emit('navigate')
}

function chooseCandidate(turn, agent) {
  void store.send(turn.question, agent)
}

defineExpose({ focus })
</script>

<template>
  <div class="hub-chat">
    <div class="hub-chat__agent-bar">
      <NSelect
        v-model:value="store.selectedAgentCode"
        :options="agentOptions"
        clearable
        filterable
        size="small"
        placeholder="自动路由（或指定智能体）"
      />
    </div>

    <div ref="listRef" class="hub-chat__messages">
      <div v-if="!store.turns.length" class="hub-chat__welcome">
        <p class="hub-chat__welcome-title">
          你好，我是 AI 助手
        </p>
        <p class="hub-chat__welcome-desc">
          可以直接向已发布的智能体提问、调用 MCP 工具，或快速查询你的流程待办。
        </p>
        <div class="hub-chat__prompts">
          <button
            v-for="prompt in quickPrompts"
            :key="prompt"
            type="button"
            class="hub-prompt-chip"
            @click="store.send(prompt)"
          >
            {{ prompt }}
          </button>
        </div>
      </div>

      <article v-for="turn in store.turns" :key="turn.id" class="hub-turn">
        <div class="hub-turn__q">
          {{ turn.question }}
        </div>

        <div class="hub-turn__a">
          <div v-if="turn.agent" class="hub-turn__agent">
            <NTag size="tiny" type="primary" :bordered="false">
              {{ turn.agent.name }}
            </NTag>
          </div>

          <div v-if="turn.tools.length" class="hub-turn__tools">
            <NTag
              v-for="(tool, idx) in turn.tools"
              :key="`${tool.name}-${idx}`"
              size="tiny"
              :type="tool.status === 'error' ? 'error' : tool.status === 'done' ? 'success' : 'info'"
              :bordered="false"
            >
              工具：{{ tool.name }}
            </NTag>
          </div>

          <div v-if="turn.status === 'routing'" class="hub-turn__muted">
            正在匹配助手…
          </div>

          <div v-else-if="turn.kind === 'TODO' && turn.todos" class="hub-turn__todos">
            <div v-if="!turn.todos.available" class="hub-turn__muted">
              {{ turn.todos.message }}
            </div>
            <template v-else>
              <div class="hub-turn__todo-head">
                <span>当前共有 {{ turn.todos.total }} 条待办任务</span>
                <NButton text type="primary" size="tiny" @click="goTodoCenter">
                  前往待办中心
                </NButton>
              </div>
              <ul v-if="turn.todos.items?.length" class="hub-todo-list">
                <li v-for="item in turn.todos.items" :key="item.taskId">
                  <strong>{{ item.processName || item.taskName }}</strong>
                  <span>{{ item.taskName }}</span>
                </li>
              </ul>
              <div v-else class="hub-turn__muted">
                暂无待处理任务
              </div>
            </template>
          </div>

          <div v-else class="hub-turn__text">
            {{ turn.answer || (turn.status === 'streaming' ? '正在思考…' : '') }}
          </div>

          <div v-if="turn.candidates?.length" class="hub-turn__candidates">
            <button
              v-for="cand in turn.candidates"
              :key="cand.code"
              type="button"
              class="hub-prompt-chip"
              @click="chooseCandidate(turn, cand)"
            >
              {{ cand.name }}
            </button>
          </div>

          <div v-if="turn.error" class="hub-turn__error">
            {{ turn.error }}
          </div>
        </div>
      </article>
    </div>

    <div class="hub-chat__composer">
      <NInput
        ref="inputRef"
        v-model:value="store.draft"
        type="textarea"
        :autosize="{ minRows: 2, maxRows: 4 }"
        placeholder="输入问题，按 Enter 发送（Shift+Enter 换行）"
        :disabled="store.busy"
        @keydown="handleKeydown"
      />
      <div class="hub-chat__composer-actions">
        <NButton v-if="store.busy" size="small" type="warning" secondary @click="store.stopStreaming">
          停止生成
        </NButton>
        <NButton v-else size="small" type="primary" :disabled="!store.draft.trim()" @click="store.send()">
          发送
        </NButton>
      </div>
    </div>
  </div>
</template>

<style scoped>
.hub-chat {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.hub-chat__agent-bar {
  padding: 8px 12px;
  border-bottom: 1px solid var(--border-light, #e5e7eb);
}

.hub-chat__messages {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.hub-chat__welcome {
  padding: 16px 8px;
}

.hub-chat__welcome-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary, #111827);
}

.hub-chat__welcome-desc {
  margin: 6px 0 12px;
  font-size: 12px;
  color: var(--text-tertiary, #6b7280);
  line-height: 1.5;
}

.hub-chat__prompts,
.hub-turn__candidates {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.hub-prompt-chip {
  padding: 5px 10px;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 6px;
  background: var(--bg-secondary, #f6f8fb);
  color: var(--text-primary, #111827);
  font-size: 12px;
  cursor: pointer;
}

.hub-prompt-chip:hover {
  border-color: var(--primary-color, #2563eb);
  color: var(--primary-color, #2563eb);
}

.hub-turn {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.hub-turn__q {
  align-self: flex-end;
  max-width: 85%;
  padding: 8px 10px;
  border-radius: 6px;
  background: var(--primary-color, #2563eb);
  color: #fff;
  font-size: 13px;
  line-height: 1.5;
  white-space: pre-wrap;
}

.hub-turn__a {
  align-self: flex-start;
  width: 100%;
  padding: 10px;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 6px;
  background: var(--bg-secondary, #f6f8fb);
  font-size: 13px;
  line-height: 1.55;
}

.hub-turn__agent,
.hub-turn__tools {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 6px;
}

.hub-turn__text {
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--text-primary, #111827);
}

.hub-turn__muted {
  font-size: 12px;
  color: var(--text-tertiary, #6b7280);
}

.hub-turn__error {
  margin-top: 6px;
  font-size: 12px;
  color: var(--error-color, #dc2626);
}

.hub-turn__todo-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  margin-bottom: 6px;
}

.hub-todo-list {
  margin: 0;
  padding-left: 16px;
  font-size: 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.hub-todo-list li {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.hub-chat__composer {
  padding: 10px 12px;
  border-top: 1px solid var(--border-light, #e5e7eb);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.hub-chat__composer-actions {
  display: flex;
  justify-content: flex-end;
}
</style>
