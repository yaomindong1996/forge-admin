<template>
  <view class="skeleton-head">
    <view class="skeleton-head__copy">
      <view class="skeleton-head__title" />
      <view class="skeleton-head__subtitle" />
    </view>
    <view class="skeleton-head__action" />
  </view>
</template>

<style lang="scss" scoped>
.skeleton-head { display: flex; min-height: 62px; align-items: center; justify-content: space-between; gap: 10px; padding: 10px 14px; box-sizing: border-box; }
.skeleton-head__copy { min-width: 0; flex: 1; }
.skeleton-head__title, .skeleton-head__subtitle, .skeleton-head__action { border-radius: 6px; background: #f0f1f2; animation: skeleton-head-pulse 1.1s ease-in-out infinite alternate; }
.skeleton-head__title { width: 96px; height: 13px; }
.skeleton-head__subtitle { width: 142px; height: 10px; margin-top: 8px; }
.skeleton-head__action { width: 44px; height: 44px; flex: 0 0 44px; border-radius: var(--radius-control); }
@keyframes skeleton-head-pulse { from { opacity: .58; } to { opacity: 1; } }
</style>
