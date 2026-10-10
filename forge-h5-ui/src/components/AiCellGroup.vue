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
  gap: 8px;

  &--compact {
    gap: 6px;
  }
}

.ai-cell-group__title {
  margin: 4px 0 0 4px;
  color: var(--forge-text-secondary);
  font-size: 13px;
  font-weight: 400;
  line-height: 1.4;
}

.ai-cell-group__body {
  position: relative;
  z-index: 1;
  overflow: hidden;
  border-radius: var(--forge-radius-card);
  background: var(--forge-surface);
}
</style>
