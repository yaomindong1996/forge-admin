<template>
  <div class="portal-skeleton" aria-busy="true" aria-label="应用加载中">
    <header class="portal-skeleton-header">
      <n-skeleton height="22px" width="168px" :sharp="false" />
      <n-skeleton circle :width="28" :height="28" />
    </header>
    <div class="portal-skeleton-shell">
      <aside class="portal-skeleton-side">
        <n-skeleton text :repeat="7" />
      </aside>
      <main class="portal-skeleton-main">
        <n-skeleton height="18px" width="36%" :sharp="false" />
        <n-skeleton height="14px" width="72%" :sharp="false" />
        <n-skeleton height="14px" width="64%" :sharp="false" />
        <n-skeleton height="220px" :sharp="false" />
      </main>
    </div>
  </div>
</template>

<style scoped>
.portal-skeleton {
  display: flex;
  flex-direction: column;
  height: 100vh;
  min-height: 100vh;
  background: #f5f7fa;
  color: #1f2329;
}

.portal-skeleton-header {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  border-bottom: 1px solid #e5e6eb;
  background: #fff;
  padding: 0 18px;
}

.portal-skeleton-shell {
  display: flex;
  flex: 1;
  min-height: 0;
}

.portal-skeleton-side {
  flex: none;
  width: 220px;
  border-right: 1px solid #e5e6eb;
  background: #fff;
  padding: 18px 16px;
}

.portal-skeleton-main {
  display: grid;
  align-content: start;
  flex: 1;
  gap: 12px;
  min-width: 0;
  padding: 20px;
}

@media (max-width: 900px) {
  .portal-skeleton-side {
    display: none;
  }
}
</style>
