<template>
  <view class="ai-help-panel">
    <view class="ai-help-panel__heading">
      <text class="ai-help-panel__title">{{ title }}</text>
      <text v-if="description" class="ai-help-panel__description">{{ description }}</text>
    </view>
    <view v-if="items.length" class="ai-help-panel__list">
      <view v-for="(item, index) in items" :key="`${index}-${item}`" class="ai-help-panel__item">
        <view class="ai-help-panel__dot" />
        <text>{{ item }}</text>
      </view>
    </view>
    <slot />
  </view>
</template>

<script setup>
defineProps({
  title: { type: String, default: '填写说明' },
  description: { type: String, default: '' },
  items: { type: Array, default: () => [] },
})
</script>

<style lang="scss" scoped>
.ai-help-panel {
  position: relative;
  padding: 32rpx 32rpx 32rpx 38rpx;
  border: 1rpx solid var(--border-color);
  border-radius: var(--radius-card);
  background: #fff;
  box-sizing: border-box;
}

.ai-help-panel::before {
  position: absolute;
  top: 32rpx;
  bottom: 32rpx;
  left: 20rpx;
  width: 4rpx;
  border-radius: 4rpx;
  background: var(--primary-color);
  content: '';
}

.ai-help-panel__title,
.ai-help-panel__description {
  display: block;
}

.ai-help-panel__title {
  color: var(--text-strong);
  font-size: 32rpx;
  font-weight: 500;
  line-height: 1.3;
}

.ai-help-panel__description {
  margin-top: 12rpx;
  color: var(--text-muted);
  font-size: 26rpx;
  line-height: 1.5;
}

.ai-help-panel__list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  margin-top: 24rpx;
}

.ai-help-panel__item {
  display: flex;
  align-items: flex-start;
  gap: 14rpx;
  color: var(--text-secondary);
  font-size: 26rpx;
  line-height: 1.5;
}

.ai-help-panel__dot {
  width: 8rpx;
  height: 8rpx;
  flex: 0 0 8rpx;
  margin-top: 15rpx;
  border-radius: 50%;
  background: var(--primary-color);
}
</style>
