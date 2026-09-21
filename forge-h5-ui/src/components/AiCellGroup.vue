<template>
  <view class="ai-cell-group" :class="{ 'ai-cell-group--compact': compact }">
    <text v-if="title" class="ai-cell-group__title">{{ title }}</text>
    <view class="ai-cell-group__body">
      <slot />
    </view>
  </view>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: ''
  },
  compact: {
    type: Boolean,
    default: false
  }
})
</script>

<style lang="scss" scoped>
.ai-cell-group {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
  
  &--compact {
    gap: 10rpx;
  }
}

.ai-cell-group__title {
  margin: 8rpx 0 0 4rpx;
  color: var(--text-secondary);
  font-size: 22rpx;
  font-weight: 650;
  line-height: 1.2;
}

.ai-cell-group__body {
  position: relative;
  z-index: 1;
  overflow: hidden;
  border: 1rpx solid var(--border-color);
  border-radius: var(--radius-card);
  background: #fff;
}
</style>
