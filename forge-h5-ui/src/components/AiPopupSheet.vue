<template>
  <wd-popup
    :model-value="modelValue"
    position="bottom"
    :modal="mask"
    :close-on-click-modal="closeOnMask"
    :z-index="Number(zIndex)"
    :safe-area-inset-bottom="true"
    root-portal
    @update:model-value="emit('update:modelValue', $event)"
    @click-modal="emit('maskClick')"
    @close="emit('close')"
  >
    <view class="ai-popup-sheet__panel" :class="{ 'is-round': round }" :style="{ maxHeight }">
      <view v-if="showHandle" class="ai-popup-sheet__handle" />
      <slot name="header">
        <view class="ai-popup-sheet__head">
          <view class="ai-popup-sheet__title-block">
            <text v-if="title" class="ai-popup-sheet__title">{{ title }}</text>
            <text v-if="description" class="ai-popup-sheet__desc">{{ description }}</text>
          </view>
          <button v-if="showClose" class="ai-popup-sheet__close" @click="close">×</button>
        </view>
      </slot>
      <scroll-view v-if="scroll" class="ai-popup-sheet__body" scroll-y :show-scrollbar="false" :style="{ maxHeight: bodyMaxHeight }">
        <view class="ai-popup-sheet__content"><slot /></view>
      </scroll-view>
      <view v-else class="ai-popup-sheet__content"><slot /></view>
      <view v-if="$slots.footer" class="ai-popup-sheet__footer"><slot name="footer" /></view>
    </view>
  </wd-popup>
</template>

<script setup>
defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '' },
  description: { type: String, default: '' },
  placement: { type: String, default: 'bottom' },
  maxHeight: { type: String, default: '78vh' },
  bodyMaxHeight: { type: String, default: 'calc(78dvh - 128px)' },
  zIndex: { type: [Number, String], default: 9990 },
  mask: { type: Boolean, default: true },
  closeOnMask: { type: Boolean, default: true },
  showClose: { type: Boolean, default: true },
  showHandle: { type: Boolean, default: false },
  scroll: { type: Boolean, default: true },
  round: { type: Boolean, default: true },
})
const emit = defineEmits(['update:modelValue', 'close', 'maskClick'])
function close() { emit('update:modelValue', false) }
</script>

<style lang="scss" scoped>
.ai-popup-sheet__panel { display: flex; width: 100vw; min-height: 60px; flex-direction: column; overflow: hidden; padding: 16px 16px 0; background: var(--forge-surface, #fff); box-shadow: var(--forge-shadow-float); box-sizing: border-box; }
.ai-popup-sheet__panel.is-round { border-radius: var(--forge-radius-popup, 24px) var(--forge-radius-popup, 24px) 0 0; }
.ai-popup-sheet__handle { width: 36px; height: 4px; margin: -6px auto 10px; border-radius: 2px; background: var(--forge-surface-muted, #ebecf0); }
.ai-popup-sheet__head { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.ai-popup-sheet__title-block { min-width: 0; flex: 1; }
.ai-popup-sheet__title, .ai-popup-sheet__desc { display: block; }
.ai-popup-sheet__title { color: var(--forge-text-primary); font-size: 17px; font-weight: 600; line-height: 1.4; }
.ai-popup-sheet__desc { margin-top: 2px; color: var(--forge-text-tertiary); font-size: 13px; line-height: 1.5; }
.ai-popup-sheet__close { display: flex; width: 32px; height: 32px; align-items: center; justify-content: center; margin: 0 -4px 0 0; padding: 0; border: 0; border-radius: 50%; color: var(--forge-text-secondary); font-size: 20px; line-height: 1; background: var(--forge-surface-subtle, #f7f8fa); }
.ai-popup-sheet__close::after { border: 0; }
.ai-popup-sheet__body { min-height: 0; flex: 1 1 auto; overflow-y: auto; }
.ai-popup-sheet__content { padding-bottom: 4px; }
.ai-popup-sheet__footer { flex: 0 0 auto; margin-top: 12px; padding: 12px 0; background: var(--forge-surface, #fff); }
:deep(.wd-popup) { max-height: 100dvh; overflow: hidden; box-sizing: border-box; background: transparent; }

@media (min-width: 1024px) {
  .ai-popup-sheet__panel {
    width: min(720px, 100vw);
    margin: 0 auto;
  }
}
</style>
