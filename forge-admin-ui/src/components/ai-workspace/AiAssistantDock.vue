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
  cancelInteraction,
  clampLauncherAwayFromHeader,
  shouldAllowClick,
} = useAssistantWindow()

const open = ref(false)
const conversationRef = ref(null)

async function openPanel() {
  // 先判定是否为拖拽；再清掉 pointer capture，避免 launcher 被 v-show 隐藏后坐标被甩到顶栏
  if (!shouldAllowClick()) {
    cancelInteraction()
    return
  }
  cancelInteraction()
  open.value = true
  void store.loadAgents()
  await nextTick()
  conversationRef.value?.focus()
}

function closePanel() {
  cancelInteraction()
  maximized.value = false
  clampLauncherAwayFromHeader()
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
            <div class="header-title-row">
              <strong>AI 助手</strong>
              <span class="header-badge">
                <i class="i-lucide:sparkles" aria-hidden="true" />
                智能工作伙伴
              </span>
            </div>
            <small>智能对话 · 菜单定位 · 流程待办</small>
          </div>
        </div>
      </header>

      <div class="assistant-main">
        <HubAssistantConversation
          ref="conversationRef"
          @navigate="closePanel"
          @locate="closePanel"
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
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  color: var(--text-primary, #111827);
  font: inherit;
  font-size: 13px;
  font-weight: 650;
  cursor: grab;
  touch-action: none;
  user-select: none;
  box-shadow: none;
  transition: transform 150ms ease;
}

.assistant-launcher:hover {
  transform: translateY(-2px);
}

.assistant-launcher:hover .launcher-orb {
  box-shadow:
    0 0 0 3px color-mix(in srgb, var(--primary-color, #2563eb) 34%, transparent),
    0 10px 22px color-mix(in srgb, var(--primary-color, #2563eb) 34%, transparent);
}

.launcher-orb {
  position: relative;
  display: inline-flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, color-mix(in srgb, var(--primary-color, #2563eb) 18%, #e0edff), #fff);
  box-shadow:
    0 0 0 3px color-mix(in srgb, var(--primary-color, #2563eb) 28%, transparent),
    0 8px 18px color-mix(in srgb, var(--primary-color, #2563eb) 28%, transparent);
  transition: box-shadow 150ms ease;
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
  right: 1px;
  bottom: 1px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #22c55e;
  border: 2px solid var(--bg-primary, #fff);
  box-sizing: border-box;
}

.launcher-orb .avatar-online-dot {
  right: 2px;
  bottom: 2px;
  width: 13px;
  height: 13px;
}

.avatar-online-dot.is-lg {
  right: 0;
  bottom: 0;
  width: 11px;
  height: 11px;
}

.launcher-label {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 22px;
  padding: 0 9px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--primary-color, #2563eb) 92%, #0f172a);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.02em;
  line-height: 1;
  white-space: nowrap;
  box-shadow:
    0 1px 0 color-mix(in srgb, #fff 22%, transparent) inset,
    0 4px 10px color-mix(in srgb, var(--primary-color, #2563eb) 35%, transparent);
}

.assistant-panel {
  --assistant-panel-gradient:
    radial-gradient(90% 60% at 8% -4%, rgba(147, 197, 253, 0.55), transparent 52%),
    radial-gradient(70% 50% at 96% 8%, rgba(196, 181, 253, 0.35), transparent 46%),
    linear-gradient(180deg, #eaf2ff 0%, #f3f6fb 38%, #f6f8fc 100%);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.65);
  border-radius: 22px;
  background: var(--assistant-panel-gradient);
  backdrop-filter: blur(10px);
  box-shadow:
    0 26px 60px color-mix(in srgb, #1a3a6e 24%, transparent),
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
  padding: 16px 16px 12px;
}

.assistant-handle {
  display: flex;
  align-items: center;
  gap: 12px;
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
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, color-mix(in srgb, var(--primary-color, #2563eb) 18%, #e0edff), #fff);
  box-shadow:
    0 0 0 2px color-mix(in srgb, var(--primary-color, #2563eb) 24%, transparent),
    0 6px 16px color-mix(in srgb, var(--primary-color, #2563eb) 22%, transparent);
}

.header-copy {
  min-width: 0;
}

.header-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.header-copy strong {
  overflow: hidden;
  font-size: 17px;
  font-weight: 700;
  line-height: 1.3;
  color: var(--text-primary, #111827);
  letter-spacing: 0.01em;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.header-badge {
  display: inline-flex;
  flex: none;
  align-items: center;
  gap: 4px;
  height: 22px;
  padding: 0 8px;
  border: 1px solid color-mix(in srgb, var(--primary-color, #2563eb) 22%, #c7d7f0);
  border-radius: 999px;
  background: color-mix(in srgb, var(--primary-color, #2563eb) 8%, #fff);
  color: var(--primary-color, #2563eb);
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
}

.header-badge i {
  font-size: 12px;
}

.header-copy small {
  display: block;
  margin-top: 4px;
  color: var(--text-tertiary, #6b7280);
  font-size: 12px;
  line-height: 1.35;
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
  right: 6px;
  bottom: 6px;
  width: 28px;
  height: 28px;
  font-size: 16px;
  color: var(--text-secondary);
  cursor: nwse-resize;
  touch-action: none;
  opacity: 0.72;
}

.resize-grip:hover {
  opacity: 1;
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
