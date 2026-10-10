<template>
  <view class="home-skeleton" aria-busy="true" aria-label="工作台加载中">
    <!-- 概览卡片 -->
    <view class="home-skeleton__panel home-skeleton__overview">
      <view class="home-skeleton__greeting">
        <view class="home-skeleton__avatar pulse" />
        <view class="home-skeleton__copy">
          <view class="home-skeleton__line home-skeleton__line--title pulse" />
          <view class="home-skeleton__line home-skeleton__line--short pulse" />
        </view>
      </view>
      <view class="home-skeleton__metric-grid">
        <view v-for="index in 3" :key="`metric-${index}`" class="home-skeleton__metric pulse" />
      </view>
    </view>

    <!-- 常用应用 -->
    <view class="home-skeleton__panel home-skeleton__apps">
      <SkeletonHead />
      <view class="home-skeleton__app-grid">
        <view v-for="index in 8" :key="`app-${index}`" class="home-skeleton__app">
          <view class="home-skeleton__app-icon pulse" />
          <view class="home-skeleton__app-label pulse" />
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import SkeletonHead from './SkeletonHead.vue'
</script>

<style lang="scss" scoped>
.home-skeleton { display: flex; flex-direction: column; gap: 12px; }
.home-skeleton__panel { overflow: hidden; border-radius: var(--forge-radius-card); background: var(--forge-surface); }
.home-skeleton__overview { padding: 16px; }
.home-skeleton__greeting { display: flex; align-items: center; gap: 10px; }
.home-skeleton__avatar { width: 40px; height: 40px; flex: 0 0 40px; border-radius: 50%; }
.home-skeleton__copy { min-width: 0; flex: 1; }
.home-skeleton__metric-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; margin-top: 14px; }
.home-skeleton__metric { height: 48px; border-radius: var(--forge-radius-control); }
.home-skeleton__app-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); row-gap: 16px; padding: 4px 4px 16px; }
.home-skeleton__app { display: flex; flex-direction: column; align-items: center; gap: 8px; }
.home-skeleton__app-icon { width: 48px; height: 48px; border-radius: var(--forge-radius-icon); }
.home-skeleton__app-label { width: 44px; height: 10px; border-radius: 6px; }
.home-skeleton__line { width: 72%; height: 10px; border-radius: 6px; }
.home-skeleton__line + .home-skeleton__line { margin-top: 8px; }
.home-skeleton__line--title { width: 48%; height: 13px; }
.home-skeleton__line--short { width: 34%; }
.pulse { background: var(--forge-surface-muted); animation: home-skeleton-pulse 1.1s ease-in-out infinite alternate; }
@keyframes home-skeleton-pulse { from { opacity: .58; } to { opacity: 1; } }
@media (min-width: 1024px) {
  .home-skeleton { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); align-items: start; }
}
</style>
