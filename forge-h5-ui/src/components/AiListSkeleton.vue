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
/* 骨架与真实列表同构：一张白卡片内多行，分隔线从文字处开始 */
.ai-list-skeleton { display: flex; flex-direction: column; overflow: hidden; border-radius: var(--forge-radius-card, 16px); background: var(--forge-surface, #fff); }
.ai-list-skeleton__row { position: relative; display: flex; min-height: 68px; align-items: center; gap: 12px; padding: 12px 16px; box-sizing: border-box; }
.ai-list-skeleton__row + .ai-list-skeleton__row::before { position: absolute; top: 0; right: 0; left: 68px; height: 1px; background: var(--forge-border, #f0f1f2); content: ''; transform: scaleY(0.5); }
.ai-list-skeleton--compact .ai-list-skeleton__row { min-height: 52px; padding: 10px 16px; }
.ai-list-skeleton__leading, .ai-list-skeleton__line, .ai-list-skeleton__end { background: var(--forge-surface-subtle, #f7f8fa); animation: skeletonPulse 1.1s ease-in-out infinite alternate; }
.ai-list-skeleton__leading { width: 40px; height: 40px; flex: 0 0 40px; border-radius: 12px; }
.ai-list-skeleton--compact .ai-list-skeleton__leading { width: 32px; height: 32px; flex-basis: 32px; border-radius: 10px; }
.ai-list-skeleton__content { display: flex; min-width: 0; flex: 1; flex-direction: column; gap: 8px; }
.ai-list-skeleton__line { height: 12px; border-radius: 6px; }
.ai-list-skeleton__line--title { width: 62%; }
.ai-list-skeleton__line--meta { width: 38%; }
.ai-list-skeleton__end { width: 28px; height: 12px; border-radius: 6px; }
@keyframes skeletonPulse { from { opacity: 0.58; } to { opacity: 1; } }
</style>
