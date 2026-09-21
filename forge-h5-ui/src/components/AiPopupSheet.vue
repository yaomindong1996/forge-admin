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
.ai-popup-sheet__panel { width: 100vw; min-height: 120rpx; padding: 12rpx 24rpx 24rpx; background: #fff; box-sizing: border-box; }
.ai-popup-sheet__panel.is-round { border-radius: 16rpx 16rpx 0 0; }
.ai-popup-sheet__handle { width: 56rpx; height: 5rpx; margin: 0 auto 16rpx; border-radius: 4rpx; background: #c7cfd9; }
.ai-popup-sheet__head { display: flex; align-items: flex-start; gap: 16rpx; margin-bottom: 18rpx; padding-bottom: 18rpx; border-bottom: 1rpx solid var(--border-light); }
.ai-popup-sheet__title-block { min-width: 0; flex: 1; }
.ai-popup-sheet__title, .ai-popup-sheet__desc { display: block; }
.ai-popup-sheet__title { color: var(--text-strong); font-size: 29rpx; font-weight: 650; }
.ai-popup-sheet__desc { margin-top: 5rpx; color: var(--text-muted); font-size: 21rpx; }
.ai-popup-sheet__close { display: flex; width: 48rpx; height: 48rpx; align-items: center; justify-content: center; margin: 0; padding: 0; border: 1rpx solid var(--border-color); border-radius: 6rpx; color: var(--text-secondary); font-size: 32rpx; background: #fff; }
.ai-popup-sheet__close::after { border: 0; }
.ai-popup-sheet__body { min-height: 0; }
.ai-popup-sheet__content { padding-bottom: 8rpx; }
.ai-popup-sheet__footer { padding-top: 18rpx; border-top: 1rpx solid var(--border-light); }
:deep(.wd-popup) { background: transparent; }
</style>
