<script setup>
/**
 * EndNode — 结束节点卡片
 * 配色：灰色代表流程结束
 * 副标题：endType（正常结束 / 强制终止）
 */
import { computed } from 'vue'
import NodeCard from './NodeCard.vue'

const props = defineProps({
  node: { type: Object, required: true },
  selected: Boolean,
  status: { type: String, default: null },
  readonly: Boolean,
  width: { type: Number, default: 224 },
  height: { type: Number, default: 82 },
})

defineEmits(['click', 'delete', 'contextMenu'])

const subtitle = computed(() => {
  return props.node?.config?.endType === 'terminate' ? '强制终止流程' : '正常结束'
})
</script>

<template>
  <NodeCard
    v-if="readonly"
    :node="node"
    :selected="selected"
    :status="status"
    :readonly="readonly"
    icon="i-lucide:square"
    color-var="gray"
    :subtitle="subtitle"
    :deletable="false"
    :width="width" :height="height"
    @click="$emit('click', $event)"
    @context-menu="$emit('contextMenu', $event)"
  />
  <button
    v-else type="button" class="flow-node-card flow-end-marker"
    :class="{ 'is-selected': selected }"
    :style="{ width: `${width}px`, height: `${height}px` }"
    :data-node-id="node.id" data-node-type="end" :aria-label="`配置${node.name || '流程结束'}`"
    :title="subtitle" @click="$emit('click', node)"
  >
    <span class="flow-end-dot" />
    <span>{{ node.name || '流程结束' }}</span>
  </button>
</template>

<style scoped>
.flow-end-marker {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
  cursor: pointer;
}
.flow-end-dot {
  width: 18px;
  height: 18px;
  margin-top: 1px;
  border-radius: 50%;
  background: var(--text-tertiary, #86909c);
}
.flow-end-marker.is-selected .flow-end-dot {
  outline: 2px solid var(--primary-color, #356de0);
  outline-offset: 3px;
}
.flow-end-marker:focus-visible {
  outline: 2px solid var(--primary-color, #356de0);
  outline-offset: 2px;
}
</style>
