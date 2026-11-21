<script setup>
import { nextTick, ref } from 'vue'
import { useRouter } from 'vue-router'
import assistantAvatar from '@/assets/ai/assistant-avatar.jpg'
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
  start,
  move,
  end,
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

function newConversation() {
  store.clear()
  conversationRef.value?.focus()
}

function openAgentsPage() {
  open.value = false
  router.push('/ai/agent')
}
</script>

<template>
  <div class="assistant-dock" :class="{ 'is-dragging': dragging }" @keydown.esc.stop="closePanel">
    <button
      v-show="!open"
      type="button"
      class="assistant-launcher"
      :style="launcherStyle"
      aria-label="打开 AI 助手"
      title="打开 AI 助手（按住可拖动）"
      @pointerdown="start('launcher', $event)"
      @pointermove="move"
      @pointerup="end"
      @pointercancel="end"
      @lostpointercapture="end"
      @click="openPanel"
    >
      <span class="launcher-orb" aria-hidden="true">
        <img :src="assistantAvatar" alt="" class="assistant-avatar-img" draggable="false">
        <span class="avatar-online-dot" />
      </span>
      <span class="launcher-label">AI 助手</span>
    </button>

    <section
      v-if="open"
      class="assistant-panel"
      :class="{ 'is-maximized': maximized }"
      :style="panelStyle"
      role="dialog"
      aria-label="AI 助手"
    >
      <header class="assistant-header">
        <div class="header-actions">
          <button
            v-if="store.turns.length"
            type="button"
            class="icon-button"
            aria-label="新建对话"
            title="新建对话"
            :disabled="store.busy"
            @click="newConversation"
          >
            <i class="i-lucide:message-square-plus" aria-hidden="true" />
          </button>
          <button
            type="button"
            class="icon-button"
            aria-label="管理智能体"
            title="智能体管理"
            @click="openAgentsPage"
          >
            <i class="i-lucide:bot" aria-hidden="true" />
          </button>
          <button
            type="button"
            class="icon-button"
            :aria-label="maximized ? '还原窗口' : '放大窗口'"
            :title="maximized ? '还原窗口' : '放大窗口'"
            @click="maximized = !maximized"
          >
            <i :class="maximized ? 'i-lucide:minimize-2' : 'i-lucide:maximize-2'" aria-hidden="true" />
          </button>
          <button
            type="button"
            class="icon-button"
            aria-label="关闭 AI 助手"
            title="关闭"
            @click="closePanel"
          >
            <i class="i-lucide:x" aria-hidden="true" />
          </button>
        </div>

        <div
          class="assistant-handle"
          :class="{ movable: !maximized }"
          @pointerdown="start('panel', $event)"
          @pointermove="move"
          @pointerup="end"
          @pointercancel="end"
          @lostpointercapture="end"
        >
          <div class="header-avatar" aria-hidden="true">
            <img :src="assistantAvatar" alt="" class="assistant-avatar-img" draggable="false">
            <span class="avatar-online-dot is-lg" />
          </div>
          <div class="header-copy">
            <strong>AI 助手</strong>
            <small>智能对话 · 工具调用 · 流程待办</small>
          </div>
        </div>
      </header>

      <div class="assistant-main">
        <HubAssistantConversation
          ref="conversationRef"
          @navigate="closePanel"
          @open-agents="openAgentsPage"
        />
      </div>

      <button
        v-if="!maximized"
        type="button"
        class="resize-edge"
        aria-label="拖动调整助手宽度"
        title="左右拖动调整宽度"
        @pointerdown="start('resize-w', $event)"
        @pointermove="move"
        @pointerup="end"
        @pointercancel="end"
        @lostpointercapture="end"
      />
      <button
        v-if="!maximized"
        type="button"
        class="resize-grip"
        aria-label="拖动调整助手窗口大小"
        title="拖动缩放窗口"
        @pointerdown="start('resize', $event)"
        @pointermove="move"
        @pointerup="end"
        @pointercancel="end"
        @lostpointercapture="end"
      >
        <i class="i-lucide:grip" aria-hidden="true" />
      </button>
    </section>
  </div>
</template>

<style scoped>
.assistant-dock {
  position: fixed;
  z-index: 1200;
  inset: 0;
  pointer-events: none;
}

.assistant-launcher,
.assistant-panel {
  position: absolute;
  box-sizing: border-box;
  pointer-events: auto;
  color: var(--text-primary);
}

.assistant-launcher {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px 6px 7px;
  border: 1px solid color-mix(in srgb, var(--primary-color, #2563eb) 22%, var(--border-light, #e5e7eb));
  border-radius: 999px;
  background: var(--bg-primary, #fff);
  color: var(--text-primary, #111827);
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: grab;
  touch-action: none;
  user-select: none;
  box-shadow:
    0 8px 24px color-mix(in srgb, #152456 14%, transparent),
    0 2px 6px color-mix(in srgb, #152456 6%, transparent);
  transition:
    transform 150ms ease,
    border-color 150ms ease,
    box-shadow 150ms ease;
}

.assistant-launcher:hover {
  transform: translateY(-2px);
  border-color: color-mix(in srgb, var(--primary-color, #2563eb) 46%, var(--border-light, #e5e7eb));
  box-shadow:
    0 12px 28px color-mix(in srgb, var(--primary-color, #2563eb) 18%, transparent),
    0 2px 8px color-mix(in srgb, #152456 8%, transparent);
}

.launcher-orb {
  position: relative;
  display: inline-flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: linear-gradient(135deg, color-mix(in srgb, var(--primary-color, #2563eb) 18%, #e0edff), #fff);
  box-shadow:
    0 0 0 2px color-mix(in srgb, var(--primary-color, #2563eb) 28%, transparent),
    0 3px 10px color-mix(in srgb, var(--primary-color, #2563eb) 28%, transparent);
}

.assistant-avatar-img {
  width: 100%;
  height: 100%;
  display: block;
  border-radius: 50%;
  object-fit: cover;
  object-position: center 18%;
  pointer-events: none;
}

.avatar-online-dot {
  position: absolute;
  right: -1px;
  bottom: -1px;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #22c55e;
  border: 2px solid var(--bg-primary, #fff);
  box-sizing: border-box;
}

.avatar-online-dot.is-lg {
  right: 0;
  bottom: 0;
  width: 11px;
  height: 11px;
}

.launcher-label {
  letter-spacing: 0.01em;
}

.assistant-panel {
  --assistant-panel-gradient:
    radial-gradient(
      120% 70% at 10% -8%,
      color-mix(in srgb, var(--primary-color, #2563eb) 15%, transparent) 0,
      transparent 48%
    ),
    linear-gradient(180deg, color-mix(in srgb, var(--primary-color, #2563eb) 8%, #fff) 0, #f5f8fc 36%, #f7f9fc 100%);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid color-mix(in srgb, var(--primary-color, #2563eb) 14%, var(--border-light, #e5e7eb));
  border-radius: 18px;
  background: var(--assistant-panel-gradient);
  box-shadow:
    0 22px 54px color-mix(in srgb, #1a3a6e 20%, transparent),
    0 2px 10px color-mix(in srgb, #152456 8%, transparent);
}

:global(.dark) .assistant-panel {
  --assistant-panel-gradient:
    radial-gradient(
      120% 70% at 10% -8%,
      color-mix(in srgb, var(--primary-color, #2563eb) 26%, transparent) 0,
      transparent 48%
    ),
    linear-gradient(
      180deg,
      color-mix(in srgb, var(--primary-color, #2563eb) 14%, var(--bg-primary)) 0,
      var(--bg-primary) 42%
    );
  border-color: var(--border-light);
}

.assistant-header {
  position: relative;
  flex: none;
  min-width: 0;
  padding: 14px 14px 10px;
}

.assistant-handle {
  display: flex;
  align-items: center;
  gap: 11px;
  min-width: 0;
  padding-right: 112px;
}

.assistant-handle.movable {
  cursor: grab;
  touch-action: none;
}

.is-dragging :is(.assistant-launcher, .assistant-handle) {
  cursor: grabbing;
  user-select: none;
}

.header-avatar {
  position: relative;
  display: inline-flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, color-mix(in srgb, var(--primary-color, #2563eb) 18%, #e0edff), #fff);
  box-shadow:
    0 0 0 2px color-mix(in srgb, var(--primary-color, #2563eb) 24%, transparent),
    0 6px 16px color-mix(in srgb, var(--primary-color, #2563eb) 22%, transparent);
}

.header-copy {
  min-width: 0;
}

.header-copy strong,
.header-copy small {
  display: block;
  line-height: 1.35;
}

.header-copy strong {
  overflow: hidden;
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.header-copy small {
  margin-top: 2px;
  color: var(--text-tertiary);
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.header-actions {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 3;
  display: flex;
  align-items: center;
  gap: 2px;
}

.icon-button,
.resize-grip {
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  padding: 0;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: var(--text-secondary);
  font-size: 15px;
  cursor: pointer;
  transition:
    background-color 140ms ease,
    color 140ms ease;
}

.icon-button:hover:not(:disabled),
.resize-grip:hover {
  background: color-mix(in srgb, var(--text-primary) 7%, transparent);
  color: var(--text-primary);
}

.icon-button:disabled {
  opacity: 0.42;
  cursor: not-allowed;
}

.assistant-main {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  min-width: 0;
}

.resize-edge {
  position: absolute;
  top: 48px;
  bottom: 28px;
  left: 0;
  z-index: 2;
  width: 7px;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: ew-resize;
  touch-action: none;
}

.resize-edge:hover,
.resize-edge:focus-visible {
  background: color-mix(in srgb, var(--primary-color, #2563eb) 22%, transparent);
}

.resize-grip {
  position: absolute;
  right: 4px;
  bottom: 4px;
  width: 20px;
  height: 20px;
  font-size: 13px;
  color: var(--text-tertiary);
  cursor: nwse-resize;
  touch-action: none;
}

.assistant-dock :is(button, .assistant-handle):focus-visible {
  outline: 2px solid var(--primary-color, #2563eb);
  outline-offset: 1px;
}

@media (prefers-reduced-motion: reduce) {
  .assistant-launcher,
  .icon-button {
    transition: none;
  }
}
</style>
