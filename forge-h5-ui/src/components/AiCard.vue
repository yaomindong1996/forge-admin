<template>
  <wd-card class="ai-card" :title="title" type="rectangle" :style="{ borderRadius, border: border ? '' : '0' }">
    <view class="ai-card__body" :style="{ padding }"><slot /></view>
    <template v-if="$slots.footer" #footer><slot name="footer" /></template>
  </wd-card>
</template>

<script setup>
defineProps({
  borderRadius: { type: String, default: '8px' },
  border: { type: Boolean, default: true },
  title: { type: String, default: '' },
  padding: { type: String, default: '16px' },
})
</script>

<style lang="scss" scoped>
.ai-card { overflow: hidden; margin: 0; border: 1rpx solid var(--forge-color-border, #e2e8f0); background: var(--forge-color-surface, #fff); box-shadow: var(--forge-shadow-card, 0 4rpx 16rpx rgba(15, 23, 42, .05)); }
.ai-card__body { box-sizing: border-box; }
:deep(.wd-card__content) { padding: 0; }
:deep(.wd-card__title-content) { border-color: var(--forge-color-border-subtle, #eef2f7); }
</style>
