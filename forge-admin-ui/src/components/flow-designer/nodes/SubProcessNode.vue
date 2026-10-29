<script setup>
/**
 * SubProcessNode — 子流程卡片
 */
import { computed } from 'vue'
import NodeCard from './NodeCard.vue'

const props = defineProps({
  node: { type: Object, required: true },
  selected: Boolean,
  status: { type: String, default: null },
  readonly: Boolean,
  showAdd: Boolean,
})
defineEmits(['click', 'delete', 'contextMenu', 'add', 'connect'])

const subtitle = computed(() => props.node?.config?.documentation || '内嵌子流程')
</script>

<template>
  <NodeCard
    :node="node" :selected="selected" :status="status" :readonly="readonly" :show-add="showAdd"
    icon="i-lucide:network" color-var="info" :subtitle="subtitle"
    @click="$emit('click', $event)" @delete="$emit('delete', $event)" @context-menu="$emit('contextMenu', $event)"
    @add="$emit('add', $event)"
    @connect="$emit('connect')"
  />
</template>
