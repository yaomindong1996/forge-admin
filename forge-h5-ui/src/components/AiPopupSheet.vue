<template>
  <wd-popup
    class="ai-popup-sheet"
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
  bodyMaxHeight: { type: String, default: 'calc(78vh - 172rpx - env(safe-area-inset-bottom))' },
  zIndex: { type: [Number, String], default: 9990 },
  mask: { type: Boolean, default: true },
  closeOnMask: { type: Boolean, default: true },
  showClose: { type: Boolean, default: true },
  showHandle: { type: Boolean, default: true },
  scroll: { type: Boolean, default: true },
  round: { type: Boolean, default: true },
})
const emit = defineEmits(['update:modelValue', 'close', 'maskClick'])
function close() { emit('update:modelValue', false) }
</script>

<style lang="scss" scoped>
.ai-popup-sheet__panel { width: 100vw; min-height: 120rpx; padding: 16rpx 28rpx 28rpx; background: #fff; box-sizing: border-box; }
.ai-popup-sheet__panel.is-round { border-radius: 24rpx 24rpx 0 0; }
.ai-popup-sheet__handle { width: 72rpx; height: 7rpx; margin: 0 auto 22rpx; border-radius: 999rpx; background: #cbd5e1; }
.ai-popup-sheet__head { display: flex; align-items: flex-start; gap: 18rpx; margin-bottom: 22rpx; }
.ai-popup-sheet__title-block { min-width: 0; flex: 1; }
.ai-popup-sheet__title, .ai-popup-sheet__desc { display: block; }
.ai-popup-sheet__title { color: var(--forge-color-text-strong, #0f172a); font-size: 32rpx; font-weight: 800; }
.ai-popup-sheet__desc { margin-top: 6rpx; color: var(--forge-color-text-secondary, #64748b); font-size: 23rpx; }
.ai-popup-sheet__close { display: flex; width: 56rpx; height: 56rpx; align-items: center; justify-content: center; margin: 0; padding: 0; border: 0; border-radius: 50%; color: #64748b; font-size: 36rpx; background: #f1f5f9; }
.ai-popup-sheet__close::after { border: 0; }
.ai-popup-sheet__body { min-height: 0; }
.ai-popup-sheet__content { padding-bottom: 8rpx; }
.ai-popup-sheet__footer { padding-top: 22rpx; }
:deep(.wd-popup) { background: transparent; }
</style>
