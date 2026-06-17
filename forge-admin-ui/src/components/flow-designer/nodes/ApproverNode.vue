<script setup>
/**
 * ApproverNode — 审批人节点卡片
 *
 * 显示：buildApproverSummary 文案 + 操作权限徽章（可选）
 */
import { computed } from 'vue'
import { buildApproverSummary } from '../utils/approver-summary.js'
import NodeCard from './NodeCard.vue'

const props = defineProps({
  node: { type: Object, required: true },
  selected: Boolean,
  status: { type: String, default: null },
  readonly: Boolean,
})

defineEmits(['click', 'delete', 'context-menu'])

const summary = computed(() => buildApproverSummary(props.node?.config || {}))

const isMultiInstance = computed(() => {
  const t = props.node?.config?.multiInstanceType
  return t && t !== 'none'
})
</script>

<template>
  <NodeCard
    :node="node"
    :selected="selected"
    :status="status"
    :readonly="readonly"
    icon="i-mdi-account-check"
    color-var="primary"
    :height="isMultiInstance ? 92 : 80"
    @click="$emit('click', $event)"
    @delete="$emit('delete', $event)"
    @context-menu="$emit('context-menu', $event)"
  >
    <template #title-extra>
      <span
        v-if="isMultiInstance"
        class="text-xs rounded bg-primary/10 px-1.5 py-0.5 text-primary"
      >
        会签
      </span>
    </template>
    {{ summary }}
  </NodeCard>
</template>
