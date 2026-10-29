<script setup>
/**
 * EndNode — 结束节点
 * 命中盒仅灰圆大小，文案溢出画在下方，保证连线贴到圆点。
 */
import { computed } from 'vue'
import NodeCard from './NodeCard.vue'

const props = defineProps({
  node: { type: Object, required: true },
  selected: Boolean,
  status: { type: String, default: null },
  readonly: Boolean,
  width: { type: Number, default: 20 },
  height: { type: Number, default: 20 },
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
    v-else type="button" class="flow-end-marker"
    :class="{ 'is-selected': selected }"
    :style="{ width: `${width}px`, height: `${height}px` }"
    :data-node-id="node.id" data-node-type="end" :aria-label="`配置${node.name || '流程结束'}`"
    :title="subtitle" @click="$emit('click', node)"
  >
    <span class="flow-end-dot" />
    <span class="flow-end-label">{{ node.name || '结束' }}</span>
  </button>
</template>

<style scoped>
.flow-end-marker {
  position: relative;
  display: block;
  padding: 0;
  border: none;
  background: transparent;
  cursor: pointer;
}
.flow-end-dot {
  display: block;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: var(--text-tertiary, #86909c);
}
.flow-end-label {
  position: absolute;
  top: calc(100% + 6px);
  left: 50%;
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
  line-height: 1;
  white-space: nowrap;
  pointer-events: none;
  transform: translateX(-50%);
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
