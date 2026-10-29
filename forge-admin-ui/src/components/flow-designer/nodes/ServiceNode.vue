<script setup>
/**
 * ServiceNode — 服务任务卡片
 * 显示：implementation type + 值
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
  const c = props.node?.config || {}
  if (!c.implementation)
    return '点击配置服务实现'
  const labels = { class: 'Java 类', expression: '表达式', delegateExpression: '代理' }
  const label = labels[c.implementationType] || 'Java 类'
  return `${label}：${c.implementation}`
})
</script>

<template>
  <NodeCard
    :node="node" :selected="selected" :status="status" :readonly="readonly" :show-add="showAdd"
    icon="i-lucide:settings" color-var="info" :subtitle="subtitle"
    @click="$emit('click', $event)" @delete="$emit('delete', $event)" @context-menu="$emit('contextMenu', $event)"
    @add="$emit('add', $event)"
    @connect="$emit('connect')"
  />
</template>
