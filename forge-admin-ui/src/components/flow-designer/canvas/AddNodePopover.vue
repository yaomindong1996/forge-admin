<script setup>
import { computed, ref } from 'vue'
import { NODE_MENU_GROUPS } from '../constants/node-menu.js'
import { getNodePresentation } from '../constants/node-presentation.js'

const props = defineProps({ allowTypes: { type: Array, default: null } })
defineEmits(['select'])
const advancedExpanded = ref(false)
const groups = computed(() => NODE_MENU_GROUPS.map(group => ({
  ...group,
  items: group.items.filter(item => !props.allowTypes || props.allowTypes.includes(item.type)),
})).filter(group => group.items.length))
const commonItems = computed(() => groups.value.filter(group => group.label !== '高级').flatMap(group => group.items))
const advancedItems = computed(() => groups.value.find(group => group.label === '高级')?.items || [])
</script>

<template>
  <section class="add-node-popover" aria-label="添加节点" @wheel.stop>
    <h3>添加节点</h3>
    <!-- 常用节点前置，高级类型只是折叠，不裁剪设计器能力 -->
    <div class="add-node-menu-grid">
      <button
        v-for="item in commonItems" :key="item.type" type="button" class="add-node-menu-item"
        :data-type="item.type" @click.stop="$emit('select', item.type)"
      >
        <i :class="getNodePresentation(item.type).icon" :style="{ color: getNodePresentation(item.type).color }" />
        <span>{{ item.label }}</span>
      </button>
    </div>
    <template v-if="advancedItems.length">
      <button
        type="button" class="add-node-advanced-toggle" :aria-expanded="advancedExpanded"
        @click.stop="advancedExpanded = !advancedExpanded"
      >
        <span>高级节点</span>
        <i :class="advancedExpanded ? 'i-lucide:chevron-down' : 'i-lucide:chevron-right'" />
      </button>
      <div v-if="advancedExpanded" class="add-node-menu-grid">
        <button
          v-for="item in advancedItems" :key="item.type" type="button" class="add-node-menu-item"
          :data-type="item.type" @click.stop="$emit('select', item.type)"
        >
          <i :class="getNodePresentation(item.type).icon" :style="{ color: getNodePresentation(item.type).color }" />
          <span>{{ item.label }}</span>
        </button>
      </div>
    </template>
  </section>
</template>

<style scoped>
.add-node-popover {
  width: min(258px, calc(100vw - 56px));
  max-height: calc(100dvh - 80px);
  overflow-y: auto;
  color: var(--text-primary, #1d2129);
}
.add-node-popover h3 {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 500;
}
.add-node-menu-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}
.add-node-menu-item {
  display: flex;
  min-width: 0;
  min-height: 70px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 4px;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 6px;
  background: var(--bg-primary, #fff);
  color: var(--text-secondary, #4e5969);
  font-size: 12px;
  cursor: pointer;
  transition:
    border-color 160ms,
    background-color 160ms;
}
.add-node-menu-item i {
  font-size: 22px;
}
.add-node-menu-item:hover {
  border-color: var(--primary-color, #356de0);
  background: color-mix(in srgb, var(--primary-color, #356de0) 4%, var(--bg-primary, #fff));
}
.add-node-advanced-toggle {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  padding: 12px 0 4px;
  border: none;
  border-top: 1px solid var(--border-light, #e5e7eb);
  background: transparent;
  color: var(--text-secondary, #4e5969);
  font-size: 13px;
  cursor: pointer;
}
.add-node-advanced-toggle[aria-expanded='true'] {
  margin-bottom: 10px;
}
button:focus-visible {
  outline: 2px solid var(--primary-color, #356de0);
  outline-offset: 2px;
}
@media (prefers-reduced-motion: reduce) {
  .add-node-menu-item {
    transition: none;
  }
}
</style>
