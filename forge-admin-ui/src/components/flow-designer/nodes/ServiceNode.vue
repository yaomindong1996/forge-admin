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
})
defineEmits(['click', 'delete', 'context-menu'])

const summary = computed(() => {
  const c = props.node?.config || {}
  if (!c.implementation)
    return '点击配置服务实现'
  const labels = { class: '类', expression: '表达式', delegateExpression: '代理' }
  const label = labels[c.implementationType] || '类'
  return `${label}：${c.implementation}`
})
</script>

<template>
  <NodeCard
    :node="node" :selected="selected" :status="status" :readonly="readonly"
    icon="i-mdi-cog-outline" color-var="info"
    @click="$emit('click', $event)" @delete="$emit('delete', $event)" @context-menu="$emit('context-menu', $event)"
  >
    {{ summary }}
  </NodeCard>
</template>
