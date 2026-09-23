<template>
  <view class="ai-empty">
    <wd-status-tip :image="emptyImage" :image-size="130" :tip="title">
      <template #bottom>
        <text v-if="description" class="ai-empty__desc">{{ description }}</text>
        <view v-if="$slots.default" class="ai-empty__action"><slot /></view>
      </template>
    </wd-status-tip>
  </view>
</template>

<script setup>
import emptyImage from '@/static/images/no-data.png'

defineProps({
  title: { type: String, default: '暂无数据' },
  description: { type: String, default: '' },
  icon: { type: String, default: 'inbox' },
})
</script>

<style lang="scss" scoped>
.ai-empty { display: flex; min-height: 420rpx; align-items: center; justify-content: center; padding: 36rpx 32rpx; box-sizing: border-box; text-align: center; }
.ai-empty__desc { display: block; max-width: 520rpx; margin-top: 8rpx; color: var(--forge-color-text-muted, #86909c); font-size: 23rpx; line-height: 1.45; }
.ai-empty__action { margin-top: 18rpx; }
:deep(.wd-status-tip__text) { color: var(--forge-color-text-strong, #1d2129); font-size: 28rpx; font-weight: 500; }
</style>
