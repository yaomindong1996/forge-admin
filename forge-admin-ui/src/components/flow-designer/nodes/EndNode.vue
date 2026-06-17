<script setup>
/**
 * EndNode — 结束节点卡片
 * 显示：endType（normal / terminate）
 */
import { computed } from 'vue'
import NodeCard from './NodeCard.vue'

const props = defineProps({
  node: { type: Object, required: true },
  selected: Boolean,
  status: { type: String, default: null },
  readonly: Boolean,
})

defineEmits(['click', 'delete', 'context-menu'])

const summary = computed(() => {
  return props.node?.config?.endType === 'terminate' ? '强制终止流程' : '正常结束'
})
</script>

<template>
  <NodeCard
    :node="node"
    :selected="selected"
    :status="status"
    :readonly="readonly"
    icon="i-mdi-flag-checkered"
    color-var="gray"
    @click="$emit('click', $event)"
    @delete="$emit('delete', $event)"
    @context-menu="$emit('context-menu', $event)"
  >
    {{ summary }}
  </NodeCard>
</template>
