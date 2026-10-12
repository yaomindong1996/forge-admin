<template>
  <main class="system-page-layout">
    <slot />
  </main>
</template>

<script setup>
defineOptions({ name: 'SystemPageLayout' })
</script>

<style scoped>
.system-page-layout {
  box-sizing: border-box;
  height: 100%;
  min-height: 0;
  padding: 0;
  background: var(--bg-primary);
}

.system-page-layout :slotted(*) {
  height: 100%;
  min-height: 0;
}
</style>
