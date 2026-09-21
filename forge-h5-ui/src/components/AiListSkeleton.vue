<template>
  <view class="ai-list-skeleton" :class="{ 'ai-list-skeleton--compact': compact }" aria-busy="true">
    <view v-for="index in rows" :key="index" class="ai-list-skeleton__row">
      <view class="ai-list-skeleton__leading" />
      <view class="ai-list-skeleton__content">
        <view class="ai-list-skeleton__line ai-list-skeleton__line--title" />
        <view class="ai-list-skeleton__line ai-list-skeleton__line--meta" />
      </view>
      <view class="ai-list-skeleton__end" />
    </view>
  </view>
</template>

<script setup>
defineProps({
  rows: { type: Number, default: 5 },
  compact: { type: Boolean, default: false },
})
</script>

<style lang="scss" scoped>
.ai-list-skeleton { display: flex; flex-direction: column; gap: 10rpx; }
.ai-list-skeleton__row { display: flex; min-height: 96rpx; align-items: center; gap: 14rpx; padding: 18rpx; border: 1rpx solid var(--border-color); border-radius: var(--radius-card); background: #fff; box-sizing: border-box; }
.ai-list-skeleton--compact .ai-list-skeleton__row { min-height: 78rpx; padding: 14rpx; }
.ai-list-skeleton__leading, .ai-list-skeleton__line, .ai-list-skeleton__end { background: #edf0f4; animation: skeletonPulse 1.1s ease-in-out infinite alternate; }
.ai-list-skeleton__leading { width: 54rpx; height: 54rpx; flex: 0 0 54rpx; border-radius: 8rpx; }
.ai-list-skeleton__content { display: flex; min-width: 0; flex: 1; flex-direction: column; gap: 12rpx; }
.ai-list-skeleton__line { height: 18rpx; border-radius: 8rpx; }
.ai-list-skeleton__line--title { width: 70%; }
.ai-list-skeleton__line--meta { width: 42%; }
.ai-list-skeleton__end { width: 34rpx; height: 18rpx; border-radius: 8rpx; }
@keyframes skeletonPulse { from { opacity: 0.58; } to { opacity: 1; } }
</style>
