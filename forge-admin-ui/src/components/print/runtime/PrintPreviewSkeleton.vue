<script setup>
import { NSkeleton } from 'naive-ui'
</script>

<template>
  <div class="print-preview-skeleton" aria-busy="true" aria-label="正在准备打印内容">
    <article class="skeleton-paper">
      <NSkeleton :sharp="false" height="22px" width="42%" />
      <NSkeleton :sharp="false" height="12px" width="68%" />
      <div class="skeleton-table">
        <NSkeleton :sharp="false" height="28px" width="100%" />
        <NSkeleton v-for="index in 6" :key="index" :sharp="false" height="22px" width="100%" />
      </div>
      <NSkeleton :sharp="false" height="12px" width="54%" />
    </article>
  </div>
</template>

<style scoped>
.print-preview-skeleton {
  display: flex;
  justify-content: center;
  padding: 24px 16px 40px;
}
.skeleton-paper {
  width: min(100%, 620px);
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 28px 32px 36px;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 2px;
  background: #fff;
  box-shadow: 0 1px 4px rgb(15 23 42 / 8%);
}
.skeleton-table {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin: 8px 0 4px;
}
</style>
