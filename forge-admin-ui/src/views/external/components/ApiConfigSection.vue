<template>
  <div class="api-config-section">
    <div class="api-config-section__header">
      <span v-if="step" class="api-config-section__step">{{ step }}</span>
      <span class="api-config-section__title">{{ title }}</span>
      <span v-if="desc" class="api-config-section__desc">{{ desc }}</span>
    </div>
    <div class="api-config-section__body">
      <slot />
    </div>
  </div>
</template>

<script setup>
defineProps({
  step: { type: [Number, String], default: null },
  title: { type: String, default: '' },
  desc: { type: String, default: '' },
})
</script>

<style scoped>
.api-config-section {
  overflow: hidden;
  min-width: 0;
  border: 1px solid var(--border-light);
  border-radius: 6px;
  background: var(--bg-primary);
}

.api-config-section__header {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 4px 10px;
  padding: 9px 12px;
  border-bottom: 1px solid var(--border-light);
  background: var(--bg-secondary);
}

.api-config-section__step {
  align-self: center;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border: 1px solid var(--border-light);
  border-radius: 50%;
  background: var(--bg-primary);
  color: var(--text-secondary);
  font-size: 11px;
  line-height: 1;
}

.api-config-section__title {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 600;
  line-height: 20px;
  white-space: nowrap;
}

.api-config-section__desc {
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 18px;
}

.api-config-section__body {
  padding: 12px;
}
</style>
