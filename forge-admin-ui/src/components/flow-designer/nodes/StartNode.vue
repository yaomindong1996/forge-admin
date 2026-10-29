<script setup>
/**
 * StartNode — 发起人节点卡片
 * 配色：青绿色（teal）代表流程启动
 * 副标题：发起人来源
 */
import NodeCard from './NodeCard.vue'

defineProps({
  node: { type: Object, required: true },
  selected: Boolean,
  status: { type: String, default: null },
  readonly: Boolean,
  showAdd: Boolean,
})

defineEmits(['click', 'delete', 'contextMenu', 'add', 'connect'])

const subtitle = '系统自动记录发起人'
</script>

<template>
  <NodeCard
    :node="node"
    :selected="selected"
    :status="status"
    :readonly="readonly"
    :show-add="showAdd"
    icon="i-lucide:play"
    color-var="success"
    :subtitle="subtitle"
    :deletable="false"
    @click="$emit('click', $event)"
    @context-menu="$emit('contextMenu', $event)"
    @add="$emit('add', $event)"
    @connect="$emit('connect')"
  />
</template>
