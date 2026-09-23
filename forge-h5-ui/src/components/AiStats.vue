<template>
  <view class="ai-stats" :class="[`ai-stats--${theme}`]" :style="{ gridTemplateColumns: `repeat(${items.length || 1}, minmax(0, 1fr))` }">
    <view v-for="(item, index) in items" :key="item.key || index" class="ai-stats__item">
      <text class="ai-stats__value">{{ item.value }}</text>
      <text class="ai-stats__label">{{ item.label }}</text>
    </view>
  </view>
</template>

<script setup>
defineProps({
  items: {
    type: Array,
    default: () => []
  },
  theme: {
    type: String,
    default: 'light',
    validator: value => ['light', 'brand'].includes(value)
  }
})
</script>

<style lang="scss" scoped>
.ai-stats {
  display: grid;
  overflow: hidden;
  width: 100%;
}

.ai-stats__item {
  position: relative;
  min-width: 0;
  text-align: center;
}

.ai-stats__item + .ai-stats__item::before {
  content: '';
  position: absolute;
  top: 10%;
  bottom: 10%;
  left: 0;
  width: 1rpx;
  background: rgba(203, 213, 225, 0.52);
}

.ai-stats__value,
.ai-stats__label {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-stats__value {
  color: #1d2129;
  font-size: 36rpx;
  font-weight: 950;
  line-height: 1.15;
}

.ai-stats__label {
  margin-top: 8rpx;
  color: #4e5969;
  font-size: 22rpx;
  font-weight: 500;
}

.ai-stats--brand .ai-stats__item + .ai-stats__item::before {
  background: rgba(255, 255, 255, 0.22);
}

.ai-stats--brand .ai-stats__value {
  color: #ffffff;
  text-shadow: 0 4rpx 14rpx rgba(15, 23, 42, 0.12);
}

.ai-stats--brand .ai-stats__label {
  color: rgba(255, 255, 255, 0.72);
}
</style>
