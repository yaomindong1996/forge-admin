<template>
  <div class="system-page-skeleton" aria-busy="true" aria-label="页面加载中">
    <div class="system-page-skeleton__metrics">
      <n-skeleton v-for="i in 4" :key="`m-${i}`" height="72px" :sharp="false" />
    </div>
    <div class="system-page-skeleton__toolbar">
      <n-skeleton height="32px" width="42%" :sharp="false" />
      <n-skeleton height="32px" width="96px" :sharp="false" />
    </div>
    <div class="system-page-skeleton__list">
      <n-skeleton v-for="i in 5" :key="`r-${i}`" height="64px" :sharp="false" />
    </div>
  </div>
</template>

<script setup>
import { NSkeleton } from 'naive-ui'
</script>

<style scoped>
.system-page-skeleton {
  display: grid;
  gap: 14px;
  min-height: 360px;
  padding: 4px 2px 16px;
}

.system-page-skeleton__metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.system-page-skeleton__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.system-page-skeleton__list {
  display: grid;
  gap: 8px;
}

@media (max-width: 960px) {
  .system-page-skeleton__metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
