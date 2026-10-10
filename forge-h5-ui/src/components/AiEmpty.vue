<template>
  <view class="ai-empty">
    <wd-status-tip :image="emptyImage" :image-size="120" :tip="title">
      <template #bottom>
        <text v-if="description" class="ai-empty__desc">{{ description }}</text>
        <view v-if="$slots.default" class="ai-empty__action"><slot /></view>
      </template>
    </wd-status-tip>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import illustrationEmpty from '@/static/illustrations/empty.png'
import illustrationError from '@/static/illustrations/error.png'
import illustrationSearch from '@/static/illustrations/search.png'

const props = defineProps({
  title: { type: String, default: '暂无数据' },
  description: { type: String, default: '' },
  icon: { type: String, default: 'inbox' },
  type: { type: String, default: 'empty', validator: value => ['empty', 'error', 'search'].includes(value) },
})

// 插画为透明底，可直接叠在任意页面底色或卡片上
const ILLUSTRATIONS = { empty: illustrationEmpty, error: illustrationError, search: illustrationSearch }
const emptyImage = computed(() => ILLUSTRATIONS[props.type] || illustrationEmpty)
</script>

<style lang="scss" scoped>
.ai-empty { display: flex; min-height: 240px; align-items: center; justify-content: center; padding: 24px 16px; box-sizing: border-box; text-align: center; }
.ai-empty__desc { display: block; max-width: 280px; margin-top: 4px; color: var(--forge-text-tertiary, #a2a3a5); font-size: 13px; line-height: 1.5; }
.ai-empty__action { margin-top: 16px; }
:deep(.wd-status-tip) { background: transparent; }
:deep(.wd-status-tip__text) { color: var(--forge-text-secondary, #747677); font-size: 15px; font-weight: 400; }
</style>
