<script setup>
/**
 * AddNodePopover — 节点类型选择弹窗内容
 *
 * 使用 NODE_MENU_GROUPS 渲染分组列表。
 * 点击某项时 emit('select', type)；外层负责关闭 Popover 与调用 useFlowDesigner.addNode。
 *
 * Props:
 *   - allowTypes  数组，限制可见节点类型（不在数组内的隐藏）。默认 null = 全部可用
 */
import { computed } from 'vue'
import { NODE_MENU_GROUPS } from '../constants/node-menu.js'

const props = defineProps({
  allowTypes: { type: Array, default: null },
})

const emit = defineEmits(['select'])

const groups = computed(() => {
  if (!props.allowTypes)
    return NODE_MENU_GROUPS
  return NODE_MENU_GROUPS
    .map(g => ({
      ...g,
      items: g.items.filter(it => props.allowTypes.includes(it.type)),
    }))
    .filter(g => g.items.length > 0)
})

function handleClick(type) {
  emit('select', type)
}
</script>

<template>
  <div class="add-node-popover">
    <div
      v-for="group in groups"
      :key="group.label"
      class="add-node-group"
      :class="{ 'is-branch-group': group.label === '分支' }"
    >
      <div class="add-node-group-title">
        {{ group.label }}
      </div>
      <div class="add-node-menu-grid">
        <button
          v-for="item in group.items"
          :key="item.type"
          class="add-node-menu-item"
          :data-type="item.type"
          @click.stop="handleClick(item.type)"
        >
          <span class="add-node-menu-icon" :data-tone="item.tone || 'slate'">
            <i :class="item.icon" />
          </span>
          <span class="add-node-menu-label">
            {{ item.label }}
          </span>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.add-node-popover {
  width: 340px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  color: var(--text-primary);
}

.add-node-group {
  margin-bottom: 0;
}

.add-node-group:last-child {
  margin-bottom: 0;
}

.add-node-group-title {
  padding: 0 4px 8px;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
}

.add-node-menu-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.add-node-group.is-branch-group .add-node-menu-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.add-node-menu-item {
  display: flex;
  min-width: 0;
  min-height: 36px;
  box-sizing: border-box;
  align-items: center;
  justify-content: flex-start;
  gap: 8px;
  padding: 8px;
  border: 1px solid transparent;
  border-radius: 4px;
  appearance: none;
  background: var(--bg-primary);
  color: var(--text-primary);
  cursor: pointer;
  text-align: left;
  transition:
    background-color 160ms ease,
    border-color 160ms ease,
    color 160ms ease;
}

.add-node-group.is-branch-group .add-node-menu-item {
  min-height: 64px;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
  padding: 8px;
  text-align: center;
}

.add-node-menu-item:hover {
  border-color: var(--border-light);
  background: var(--bg-secondary);
}

.add-node-menu-item:focus-visible {
  outline: 2px solid rgba(22, 93, 255, 0.24);
  outline-offset: 2px;
}

.add-node-menu-icon {
  display: inline-flex;
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
}

.add-node-menu-icon[data-tone='blue'] {
  color: #2563eb;
}

.add-node-menu-icon[data-tone='teal'] {
  color: #0d9488;
}

.add-node-menu-icon[data-tone='amber'] {
  color: #d97706;
}

.add-node-menu-icon[data-tone='emerald'] {
  color: #059669;
}

.add-node-menu-icon[data-tone='indigo'] {
  color: #4f46e5;
}

.add-node-menu-icon[data-tone='violet'] {
  color: #7c3aed;
}

.add-node-menu-icon[data-tone='cyan'] {
  color: #0891b2;
}

.add-node-menu-icon[data-tone='sky'] {
  color: #0284c7;
}

.add-node-group.is-branch-group .add-node-menu-icon {
  width: 22px;
  height: 22px;
}

.add-node-menu-icon :deep(i) {
  width: 16px;
  height: 16px;
  font-size: 16px;
}

.add-node-group.is-branch-group .add-node-menu-icon :deep(i) {
  width: 20px;
  height: 20px;
  font-size: 20px;
}

.add-node-menu-label {
  min-width: 0;
  flex: 1;
  overflow: hidden;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 500;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.add-node-group.is-branch-group .add-node-menu-label {
  flex: none;
  width: 100%;
  font-size: 12px;
}

@media (prefers-reduced-motion: reduce) {
  .add-node-menu-item {
    transition: none;
  }
}
</style>
