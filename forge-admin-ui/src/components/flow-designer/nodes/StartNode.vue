<script setup>
/**
 * StartNode — 发起人节点卡片
 *
 * 显示：发起人变量（默认 ${initiator}）+ 表单类型 / 表单 key（如有）
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
  const parts = []
  if (c.initiator)
    parts.push(`发起人：\${${c.initiator}}`)
  if (c.formKey)
    parts.push(`表单：${c.formKey}`)
  else if (c.formUrl)
    parts.push(`表单 URL：${c.formUrl}`)
  return parts.join(' · ') || '点击配置发起人'
})
</script>

<template>
  <NodeCard
    :node="node"
    :selected="selected"
    :status="status"
    :readonly="readonly"
    icon="i-mdi-flag-variant-outline"
    color-var="success"
    @click="$emit('click', $event)"
    @delete="$emit('delete', $event)"
    @context-menu="$emit('context-menu', $event)"
  >
    {{ summary }}
  </NodeCard>
</template>
