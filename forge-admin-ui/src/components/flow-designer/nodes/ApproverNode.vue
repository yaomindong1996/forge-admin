<script setup>
/**
 * ApproverNode — 审批人节点卡片
 * 配色：蓝色（primary）代表审批
 * 副标题：buildApproverSummary 文案
 */
import { computed } from 'vue'
import { buildApproverSummary } from '../utils/approver-summary.js'
import NodeCard from './NodeCard.vue'

const props = defineProps({
  node: { type: Object, required: true },
  selected: Boolean,
  status: { type: String, default: null },
  readonly: Boolean,
  showAdd: Boolean,
})

defineEmits(['click', 'delete', 'contextMenu', 'add', 'connect'])

const subtitle = computed(() => buildApproverSummary(props.node?.config || {}))

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
    :show-add="showAdd"
    icon="i-lucide:user"
    color-var="primary"
    :subtitle="subtitle"
    @click="$emit('click', $event)"
    @delete="$emit('delete', $event)"
    @context-menu="$emit('contextMenu', $event)"
    @add="$emit('add', $event)"
    @connect="$emit('connect')"
  >
    <template #title-extra>
      <span
        v-if="isMultiInstance"
        class="shrink-0 rounded-sm bg-blue-100 px-1.5 py-0.5 text-[10px] text-blue-600"
      >会签</span>
    </template>
  </NodeCard>
</template>
