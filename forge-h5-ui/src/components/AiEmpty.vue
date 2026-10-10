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
.ai-empty { display: flex; min-height: 240px; align-items: center; justify-content: center; padding: 24px 16px; box-sizing: border-box; text-align: center; }
.ai-empty__desc { display: block; max-width: 280px; margin-top: 4px; color: var(--forge-text-tertiary, #a2a3a5); font-size: 13px; line-height: 1.5; }
.ai-empty__action { margin-top: 16px; }
:deep(.wd-status-tip__text) { color: var(--forge-text-secondary, #747677); font-size: 15px; font-weight: 400; }
</style>
