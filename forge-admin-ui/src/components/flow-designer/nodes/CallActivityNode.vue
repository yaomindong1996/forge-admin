<script setup>
/**
 * CallActivityNode — 调用活动卡片
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
  const c = props.node?.config?.calledElement
  return c ? `调用流程：${c}` : '点击配置被调用流程 ID'
})
</script>

<template>
  <NodeCard
    :node="node" :selected="selected" :status="status" :readonly="readonly"
    icon="i-mdi-phone-forward-outline" color-var="info"
    @click="$emit('click', $event)" @delete="$emit('delete', $event)" @context-menu="$emit('context-menu', $event)"
  >
    {{ summary }}
  </NodeCard>
</template>
