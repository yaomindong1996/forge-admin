<script setup>
/**
 * CarbonCopyNode — 抄送人节点卡片
 *
 * 显示：抄送对象（候选人列表 / 表达式）。与 ApproverNode 类似但只读语义（抄送不需要审批）。
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
  if (Array.isArray(c.candidateUsers) && c.candidateUsers.length) {
    const names = c.candidateUserNames || c.candidateUsers
    const head = names.slice(0, 3).join('、')
    return `抄送 ${names.length} 人：${head}${names.length > 3 ? ` 等` : ''}`
  }
  if (c.implementation)
    return `表达式：${c.implementation}`
  return '点击配置抄送人'
})
</script>

<template>
  <NodeCard
    :node="node"
    :selected="selected"
    :status="status"
    :readonly="readonly"
    icon="i-mdi-email-outline"
    color-var="info"
    @click="$emit('click', $event)"
    @delete="$emit('delete', $event)"
    @context-menu="$emit('context-menu', $event)"
  >
    {{ summary }}
  </NodeCard>
</template>
