<template>
  <wd-card class="ai-card" :class="{ 'ai-card--bordered': border }" :title="title" type="rectangle" :style="{ borderRadius }">
    <view class="ai-card__body" :style="{ padding }"><slot /></view>
    <template v-if="$slots.footer" #footer><slot name="footer" /></template>
  </wd-card>
</template>

<script setup>
defineProps({
  borderRadius: { type: String, default: 'var(--forge-radius-card, 16px)' },
  // 默认无描边无阴影，靠白底与页面底色区分；嵌在白底容器里时再开描边
  border: { type: Boolean, default: false },
  title: { type: String, default: '' },
  padding: { type: String, default: '16px' },
})
</script>

<style lang="scss" scoped>
.ai-card { overflow: hidden; margin: 0; border: 0; background: var(--forge-surface, #fff); box-shadow: none; }
.ai-card--bordered { border: 1px solid var(--forge-border, #f0f1f2); }
.ai-card__body { box-sizing: border-box; }
:deep(.wd-card__content) { padding: 0; }
:deep(.wd-card__title-content) { border-color: var(--forge-border, #f0f1f2); color: var(--forge-text-primary); font-size: 16px; font-weight: 600; }
</style>
