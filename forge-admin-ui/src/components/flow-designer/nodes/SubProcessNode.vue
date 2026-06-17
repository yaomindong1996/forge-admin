<script setup>
/**
 * SubProcessNode — 子流程卡片
 */
import NodeCard from './NodeCard.vue'

defineProps({
  node: { type: Object, required: true },
  selected: Boolean,
  status: { type: String, default: null },
  readonly: Boolean,
})
defineEmits(['click', 'delete', 'context-menu'])
</script>

<template>
  <NodeCard
    :node="node" :selected="selected" :status="status" :readonly="readonly"
    icon="i-mdi-sitemap-outline" color-var="info"
    @click="$emit('click', $event)" @delete="$emit('delete', $event)" @context-menu="$emit('context-menu', $event)"
  >
    {{ node?.config?.documentation || '内嵌子流程' }}
  </NodeCard>
</template>
