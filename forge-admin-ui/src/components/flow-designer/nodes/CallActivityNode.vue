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
  showAdd: Boolean,
})
defineEmits(['click', 'delete', 'contextMenu', 'add', 'connect'])

const subtitle = computed(() => {
  const c = props.node?.config?.calledElement
  return c ? `调用流程：${c}` : '点击配置被调用流程 ID'
})
</script>

<template>
  <NodeCard
    :node="node" :selected="selected" :status="status" :readonly="readonly" :show-add="showAdd"
    icon="i-lucide:phone" color-var="info" :subtitle="subtitle"
    @click="$emit('click', $event)" @delete="$emit('delete', $event)" @context-menu="$emit('contextMenu', $event)"
    @add="$emit('add', $event)"
    @connect="$emit('connect')"
  />
</template>
