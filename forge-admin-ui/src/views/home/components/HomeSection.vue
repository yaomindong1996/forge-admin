<script setup>
import WorkspaceIllustration from '@/components/common/WorkspaceIllustration.vue'
import HomeActionIcon from './HomeActionIcon.vue'

defineProps({
  title: { type: String, required: true },
  description: { type: String, default: '' },
  icon: { type: String, default: 'apps' },
  artwork: { type: String, default: '' },
})
</script>

<template>
  <!-- 统一模块身份区与对象级操作 -->
  <section class="home-section">
    <header class="home-section-head">
      <WorkspaceIllustration v-if="artwork" :artwork="artwork" size="card" class="home-section-artwork" />
      <span v-else class="home-section-icon"><HomeActionIcon :name="icon" :size="22" /></span>
      <div class="home-section-copy">
        <h2>{{ title }}</h2>
        <p v-if="description" :title="description">
          {{ description }}
        </p>
      </div>
      <div v-if="$slots.action" class="home-section-action">
        <slot name="action" />
      </div>
    </header>
    <slot />
  </section>
</template>

<style scoped>
.home-section {
  min-width: 0;
  overflow: hidden;
  border: 1px solid var(--home-border, #e5e7eb);
  border-radius: var(--home-radius, 6px);
  background: var(--home-panel, #fff);
  color: var(--home-text, #1f2329);
}
.home-section-head {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 56px;
  padding: 8px 14px;
  border-bottom: 1px solid var(--home-border, #e5e7eb);
}
.home-section-artwork {
  width: 40px;
  height: 40px;
}
.home-section-icon {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  color: var(--home-brand, #0e42d2);
}
.home-section-copy {
  flex: 1;
  min-width: 0;
}
h2 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
}
p {
  margin: 3px 0 0;
  overflow: hidden;
  font-size: 11px;
  line-height: 16px;
  color: var(--home-muted, #86909c);
  text-overflow: ellipsis;
  white-space: nowrap;
}
.home-section-action {
  flex-shrink: 0;
}
.home-section-action :deep(button) {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  min-height: 32px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--home-brand, #0e42d2);
  font: inherit;
  font-size: 12px;
  cursor: pointer;
}
.home-section-action :deep(button:hover) {
  opacity: 0.8;
}
.home-section-action :deep(button:focus-visible) {
  outline: 2px solid var(--home-brand, #0e42d2);
}
</style>
