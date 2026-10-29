<script setup>
/**
 * 添加菜单：与左侧节点库同一数据源（NODE_MENU_GROUPS），并可选「连接线」。
 */
import { computed, ref } from 'vue'
import { NODE_MENU_GROUPS } from '../constants/node-menu.js'
import { getNodePresentation } from '../constants/node-presentation.js'

const props = defineProps({
  allowTypes: { type: Array, default: null },
  title: { type: String, default: '添加节点' },
  hint: { type: String, default: '' },
  /** 是否展示「连接线」入口（与节点库一致） */
  showConnect: { type: Boolean, default: true },
})
const emit = defineEmits(['select', 'connect'])
const advancedExpanded = ref(false)

const groups = computed(() => NODE_MENU_GROUPS.map(group => ({
  ...group,
  items: group.items.filter(item => !props.allowTypes || props.allowTypes.includes(item.type)),
})).filter(group => group.items.length))

/** 基础：审批流 + 分支（与节点库「基础节点」+ 分支一致） */
const commonItems = computed(() => groups.value
  .filter(group => group.label !== '高级')
  .flatMap(group => group.items))

const advancedItems = computed(() => groups.value.find(group => group.label === '高级')?.items || [])

function onConnect() {
  emit('connect')
}
</script>

<template>
  <section class="add-node-popover" aria-label="添加节点" @wheel.stop>
    <h3>{{ title }}</h3>
    <p v-if="hint" class="add-node-hint">
      {{ hint }}
    </p>

    <div class="add-node-section-label">
      基础节点
    </div>
    <div class="add-node-menu-grid">
      <button
        v-for="item in commonItems"
        :key="item.type"
        type="button"
        class="add-node-menu-item"
        :data-type="item.type"
        @click.stop="$emit('select', item.type)"
      >
        <i :class="getNodePresentation(item.type).icon" :style="{ color: getNodePresentation(item.type).color }" />
        <span>{{ item.label }}</span>
      </button>
      <button
        v-if="showConnect"
        type="button"
        class="add-node-menu-item is-connect"
        data-type="connect"
        title="先点起始节点，再点结束节点完成连线"
        @click.stop="onConnect"
      >
        <i class="i-lucide:move-right" style="color: var(--primary-color, #356de0)" />
        <span>连接线</span>
      </button>
    </div>

    <template v-if="advancedItems.length">
      <button
        type="button"
        class="add-node-advanced-toggle"
        :aria-expanded="advancedExpanded"
        @click.stop="advancedExpanded = !advancedExpanded"
      >
        <span>高级节点</span>
        <i :class="advancedExpanded ? 'i-lucide:chevron-down' : 'i-lucide:chevron-right'" />
      </button>
      <div v-if="advancedExpanded" class="add-node-menu-grid">
        <button
          v-for="item in advancedItems"
          :key="item.type"
          type="button"
          class="add-node-menu-item"
          :data-type="item.type"
          @click.stop="$emit('select', item.type)"
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
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 500;
}
.add-node-hint {
  margin: 0 0 10px;
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
  line-height: 1.4;
}
.add-node-section-label {
  margin: 0 0 6px;
  color: var(--text-tertiary, #86909c);
  font-size: 11px;
}
.add-node-menu-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}
.add-node-menu-item {
  display: flex;
  min-width: 0;
  min-height: 64px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 4px;
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
  font-size: 18px;
}
.add-node-menu-item:hover,
.add-node-menu-item.is-connect:hover {
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
