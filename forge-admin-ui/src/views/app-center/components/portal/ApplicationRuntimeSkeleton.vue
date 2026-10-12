<template>
  <div class="runtime-skeleton" aria-busy="true" aria-label="应用加载中">
    <div class="runtime-skeleton__header">
      <n-skeleton width="32px" height="32px" :sharp="false" />
      <n-skeleton width="120px" height="16px" :sharp="false" />
      <n-skeleton width="200px" height="14px" :sharp="false" />
    </div>
    <div class="runtime-skeleton__tabs">
      <n-skeleton v-for="i in 4" :key="i" width="72px" height="28px" :sharp="false" />
    </div>
    <div class="runtime-skeleton__body">
      <div class="runtime-skeleton__sidebar">
        <n-skeleton v-for="i in 6" :key="i" width="100%" height="20px" :sharp="false" style="margin-bottom: 12px" />
      </div>
      <div class="runtime-skeleton__content">
        <n-skeleton width="60%" height="24px" :sharp="false" />
        <n-skeleton width="100%" height="14px" :sharp="false" style="margin-top: 16px" />
        <n-skeleton width="100%" height="14px" :sharp="false" style="margin-top: 8px" />
        <n-skeleton width="80%" height="14px" :sharp="false" style="margin-top: 8px" />
        <n-skeleton width="100%" height="120px" :sharp="false" style="margin-top: 24px" />
        <n-skeleton width="100%" height="120px" :sharp="false" style="margin-top: 12px" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.runtime-skeleton {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  padding: 16px 20px 24px;
  background: var(--bg-secondary);
  animation: runtime-skeleton-fade 0.3s ease;
}

.runtime-skeleton__header {
  display: flex;
  flex: none;
  gap: 12px;
  align-items: center;
  height: 48px;
  margin-bottom: 12px;
}

.runtime-skeleton__tabs {
  display: flex;
  flex: none;
  gap: 10px;
  margin-bottom: 16px;
}

.runtime-skeleton__body {
  display: flex;
  flex: 1;
  gap: 16px;
  min-height: 0;
}

.runtime-skeleton__sidebar {
  flex: none;
  width: 220px;
  padding: 12px;
  border-radius: 8px;
  background: var(--bg-primary);
}

.runtime-skeleton__content {
  flex: 1;
  min-width: 0;
  padding: 16px;
  border-radius: 8px;
  background: var(--bg-primary);
}

@keyframes runtime-skeleton-fade {
  from { opacity: 0.55; }
  to { opacity: 1; }
}
</style>
