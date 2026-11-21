<script setup>
import { NButton } from 'naive-ui'
import { nextTick, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAssistantWindow } from '@/composables/ai/useAssistantWindow'
import { useHubAssistantStore } from '@/stores/ai/hubAssistantStore'
import HubAssistantConversation from './HubAssistantConversation.vue'

const router = useRouter()
const store = useHubAssistantStore()
const {
  maximized,
  dragging,
  launcherStyle,
  panelStyle,
  startDrag,
  moveDrag,
  endDrag,
  shouldAllowClick,
} = useAssistantWindow()

const open = ref(false)
const conversationRef = ref(null)

async function openPanel() {
  if (!shouldAllowClick())
    return
  open.value = true
  void store.loadAgents()
  await nextTick()
  conversationRef.value?.focus()
}

function closePanel() {
  open.value = false
}

function openAgentsPage() {
  open.value = false
  router.push('/ai/agent')
}
</script>

<template>
  <div class="ai-dock" :class="{ 'is-dragging': dragging }" @keydown.esc.stop="closePanel">
    <button
      v-show="!open"
      type="button"
      class="ai-dock__launcher"
      :style="launcherStyle"
      aria-label="打开 AI 助手"
      title="打开 AI 助手（按住可拖动）"
      @pointerdown="startDrag"
      @pointermove="moveDrag"
      @pointerup="endDrag"
      @pointercancel="endDrag"
      @click="openPanel"
    >
      <span class="ai-dock__dot" aria-hidden="true" />
      <span>AI 助手</span>
    </button>

    <section
      v-if="open"
      class="ai-dock__panel"
      :style="panelStyle"
      role="dialog"
      aria-label="AI 助手"
    >
      <header class="ai-dock__header">
        <div class="ai-dock__title">
          <strong>AI 助手</strong>
          <span>对话 · 工具 · 待办</span>
        </div>
        <div class="ai-dock__actions">
          <NButton v-if="store.turns.length" quaternary size="tiny" :disabled="store.busy" @click="store.clear">
            新对话
          </NButton>
          <NButton quaternary size="tiny" @click="openAgentsPage">
            智能体
          </NButton>
          <NButton quaternary size="tiny" @click="maximized = !maximized">
            {{ maximized ? '还原' : '放大' }}
          </NButton>
          <NButton quaternary size="tiny" @click="closePanel">
            关闭
          </NButton>
        </div>
      </header>
      <div class="ai-dock__body">
        <HubAssistantConversation ref="conversationRef" @navigate="closePanel" />
      </div>
    </section>
  </div>
</template>

<style scoped>
.ai-dock {
  position: fixed;
  z-index: 1200;
}

.ai-dock__launcher {
  position: fixed;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 9px 14px;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 999px;
  background: var(--bg-primary, #fff);
  color: var(--text-primary, #111827);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.1);
  user-select: none;
}

.ai-dock__launcher:hover {
  border-color: var(--primary-color, #2563eb);
  color: var(--primary-color, #2563eb);
}

.ai-dock__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--primary-color, #2563eb);
}

.ai-dock__panel {
  position: fixed;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 8px;
  background: var(--bg-primary, #fff);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.14);
  overflow: hidden;
}

.ai-dock__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 12px;
  border-bottom: 1px solid var(--border-light, #e5e7eb);
}

.ai-dock__title {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.ai-dock__title strong {
  font-size: 14px;
  color: var(--text-primary, #111827);
}

.ai-dock__title span {
  font-size: 11px;
  color: var(--text-tertiary, #6b7280);
}

.ai-dock__actions {
  display: flex;
  align-items: center;
  gap: 2px;
}

.ai-dock__body {
  flex: 1;
  min-height: 0;
}
</style>
