<template>
  <!-- 个人设置与租户设置共用，空白布局只在个人设置中提供 -->
  <div class="layout-grid">
    <button
      v-for="item in options" :key="item.name" type="button" class="layout-card"
      :class="{ active: model === item.name }" :aria-pressed="model === item.name" @click="model = item.name"
    >
      <span class="layout-mini" :class="`layout-mini--${item.name}`" aria-hidden="true">
        <span class="mini-side" /><span class="mini-top" /><span class="mini-flyout" /><span class="mini-content" />
      </span>
      <span class="layout-title">{{ item.title }}<i v-if="model === item.name" class="i-lucide:check" /></span>
      <small>{{ item.description }}</small>
    </button>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { layoutSettings } from '@/settings'

const props = defineProps({ tenant: Boolean })
const model = defineModel({ type: String, required: true })
// dropdown-menu 没有独立布局模块；应用门户由路由指定，不作为后台个人布局。
const options = computed(() => layoutSettings.layouts.filter((item) => {
  return !['dropdown-menu', 'app-portal'].includes(item.name) && (!props.tenant || item.name !== 'empty')
}))
</script>

<style scoped>
.layout-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  width: 100%;
}
.layout-card {
  display: grid;
  gap: 6px;
  padding: 12px;
  min-width: 0;
  text-align: left;
  font: inherit;
  border: 1px solid var(--border-light);
  border-radius: 6px;
  background: var(--bg-primary);
  color: var(--text-secondary);
  cursor: pointer;
}
.layout-card:hover,
.layout-card.active {
  border-color: var(--primary-color);
}
.layout-card.active {
  background: color-mix(in srgb, var(--primary-color) 5%, var(--bg-primary));
}
.layout-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
}
.layout-title i {
  color: var(--primary-color);
}
.layout-card small {
  color: var(--text-tertiary);
  font-size: 11px;
  line-height: 1.5;
}
.layout-mini {
  height: 42px;
  position: relative;
  background: var(--bg-secondary);
  border: 1px solid var(--border-light);
  border-radius: 3px;
}
.mini-side,
.mini-top,
.mini-content,
.mini-flyout {
  position: absolute;
  border-radius: 2px;
}
.mini-top {
  inset: 3px 3px auto 22%;
  height: 6px;
  background: color-mix(in srgb, var(--primary-color) 22%, var(--bg-primary));
}
.mini-side {
  inset: 3px auto 3px 3px;
  width: 16%;
  background: color-mix(in srgb, var(--primary-color) 12%, var(--bg-primary));
}
.mini-content {
  inset: 12px 3px 3px 22%;
  border: 1px solid var(--border-light);
  background: var(--bg-primary);
}
.layout-mini--business-workbench .mini-side,
.layout-mini--immersive .mini-side,
.layout-mini--top-menu .mini-side,
.layout-mini--empty .mini-side {
  display: none;
}
.layout-mini--business-workbench .mini-top,
.layout-mini--top-menu .mini-top,
.layout-mini--immersive .mini-top {
  left: 3px;
}
.layout-mini--business-workbench .mini-content,
.layout-mini--top-menu .mini-content,
.layout-mini--immersive .mini-content {
  left: 3px;
}
.layout-mini--simple .mini-top,
.layout-mini--bento .mini-top,
.layout-mini--empty .mini-top {
  display: none;
}
.layout-mini--simple .mini-content {
  top: 3px;
}
.layout-mini--empty .mini-content {
  inset: 3px;
}
.layout-mini--bento .mini-side {
  width: 8%;
}
.mini-flyout {
  display: none;
}
.layout-mini--side-flyout .mini-side {
  width: 12%;
}
.layout-mini--side-flyout .mini-flyout {
  display: block;
  inset: 12px auto 3px 16%;
  width: 22%;
  background: color-mix(in srgb, var(--primary-color) 18%, var(--bg-primary));
}
.layout-mini--side-flyout .mini-content {
  left: 40%;
}
.layout-mini--top-side-menu .mini-top {
  left: 3px;
}
.layout-mini--top-side-menu .mini-side {
  top: 12px;
}
.layout-card:focus-visible {
  outline: 2px solid var(--primary-color);
  outline-offset: 2px;
}
@media (max-width: 600px) {
  .layout-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
