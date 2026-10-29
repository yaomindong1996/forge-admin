<script setup>
/**
 * 左侧节点货架：拖入画布创建游离节点 + 连接线工具入口。
 */
import { NODE_MENU_GROUPS } from '../constants/node-menu.js'
import { getNodePresentation } from '../constants/node-presentation.js'

defineProps({
  readonly: Boolean,
  tool: { type: String, default: 'select' },
})

const emit = defineEmits(['dragStart', 'connectTool'])

/** 与弹出层同一分组：基础节点（审批流+分支）/ 高级节点 / 连接线 */
const basicItems = NODE_MENU_GROUPS
  .filter(group => group.label !== '高级')
  .flatMap(group => group.items)
const advancedItems = NODE_MENU_GROUPS.find(group => group.label === '高级')?.items || []

function onDragStart(event, type) {
  if (!event.dataTransfer)
    return
  event.dataTransfer.setData('application/x-forge-flow-node', type)
  event.dataTransfer.effectAllowed = 'copy'
  emit('dragStart', type)
}

function iconOf(type) {
  return getNodePresentation(type)?.icon || 'i-lucide:box'
}

function colorOf(type) {
  return getNodePresentation(type)?.color || 'var(--primary-color, #356de0)'
}
</script>

<template>
  <aside class="node-shelf" aria-label="节点库">
    <div class="node-shelf__title">
      节点库
    </div>
    <p class="node-shelf__hint">
      可拖到画布；也可在底部工具栏「＋」添加，用「连线」工具或节点锚点连接。
    </p>
    <div class="node-shelf__group">
      <div class="node-shelf__group-label">
        基础节点
      </div>
      <button
        v-for="item in basicItems"
        :key="item.type"
        type="button"
        class="node-shelf__item"
        :draggable="!readonly"
        :disabled="readonly"
        @dragstart="onDragStart($event, item.type)"
      >
        <i class="node-shelf__icon" :class="iconOf(item.type)" :style="{ color: colorOf(item.type) }" />
        <span>{{ item.label }}</span>
      </button>
    </div>
    <div v-if="advancedItems.length" class="node-shelf__group">
      <div class="node-shelf__group-label">
        高级节点
      </div>
      <button
        v-for="item in advancedItems"
        :key="item.type"
        type="button"
        class="node-shelf__item"
        :draggable="!readonly"
        :disabled="readonly"
        @dragstart="onDragStart($event, item.type)"
      >
        <i class="node-shelf__icon" :class="iconOf(item.type)" :style="{ color: colorOf(item.type) }" />
        <span>{{ item.label }}</span>
      </button>
    </div>
    <div class="node-shelf__group">
      <div class="node-shelf__group-label">
        连接
      </div>
      <button
        type="button"
        class="node-shelf__item"
        :class="{ 'is-active': tool === 'connect' }"
        :disabled="readonly"
        title="先点起始节点，移动出现蓝虚线，再点结束节点完成连线"
        @click="emit('connectTool')"
      >
        <i class="node-shelf__icon i-lucide:move-right" style="color: var(--primary-color, #356de0)" />
        <span>连接线</span>
      </button>
    </div>
  </aside>
</template>

<style scoped>
.node-shelf {
  display: flex;
  width: 168px;
  flex: none;
  flex-direction: column;
  gap: 8px;
  padding: 12px 10px;
  border-right: 1px solid var(--border-light, #e5e7eb);
  background: var(--bg-primary, #fff);
  overflow: auto;
}
.node-shelf__title {
  color: var(--text-primary, #1d2129);
  font-size: 13px;
  font-weight: 600;
}
.node-shelf__hint {
  margin: 0;
  color: var(--text-tertiary, #86909c);
  font-size: 11px;
  line-height: 1.5;
}
.node-shelf__group {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 6px;
}
.node-shelf__group-label {
  margin-bottom: 2px;
  color: var(--text-tertiary, #86909c);
  font-size: 11px;
}
.node-shelf__item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 8px;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 6px;
  background: var(--bg-primary, #fff);
  color: var(--text-secondary, #4e5969);
  font-size: 12px;
  text-align: left;
  cursor: grab;
}
.node-shelf__item:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}
.node-shelf__item:hover:not(:disabled),
.node-shelf__item.is-active {
  border-color: var(--primary-color, #356de0);
  color: var(--text-primary, #1d2129);
  background: color-mix(in srgb, var(--primary-color, #356de0) 6%, #fff);
  cursor: pointer;
}
.node-shelf__icon {
  font-size: 14px;
}
@media (max-width: 760px) {
  .node-shelf {
    display: none;
  }
}
</style>
