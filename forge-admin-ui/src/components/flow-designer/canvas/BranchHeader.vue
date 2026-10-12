<script setup>
/**
 * BranchHeader — 分支首部条件徽章
 *
 * 用法：在每条分支线的首端（紧贴网关下方）渲染一个带 condition 文本的胶囊，
 * 标识 "if (days > 3)" / "默认" 等。
 *
 * Props:
 *   - edge       branchEdge（含 condition / isDefault / branchId）
 *   - position   { x, y } 画布坐标
 *
 * Events:
 *   - click  点击徽章 → 父组件可打开条件配置抽屉
 */
import { computed } from 'vue'

const props = defineProps({
  edge: { type: Object, required: true },
  position: { type: Object, required: true },
})

defineEmits(['click'])

const approvalResultLabels = [
  { label: '同意通过', value: 'approve', expression: '$' + '{approvalResult == \'approve\'}' },
  { label: '驳回修改', value: 'reject', expression: '$' + '{approvalResult == \'reject\'}' },
  { label: '退回上一步', value: 'return', expression: '$' + '{approvalResult == \'return\'}' },
  { label: '终止流程', value: 'terminate', expression: '$' + '{approvalResult == \'terminate\'}' },
]

const labelText = computed(() => {
  if (props.edge?.isDefault)
    return '默认'
  const approvalLabel = getApprovalResultLabel(props.edge)
  if (approvalLabel)
    return approvalLabel
  if (!props.edge?.condition)
    return '配置条件'
  const ruleCount = Array.isArray(props.edge?.conditionRules) ? props.edge.conditionRules.length : 0
  return ruleCount > 1 ? `${ruleCount} 条条件` : '条件已设'
})

const labelTitle = computed(() => {
  if (props.edge?.isDefault)
    return '默认分支'
  const approvalLabel = getApprovalResultLabel(props.edge)
  if (approvalLabel)
    return approvalLabel
  return props.edge?.condition || '点击配置条件'
})

const colorClass = computed(() => {
  if (props.edge?.isDefault)
    return 'is-default'
  if (props.edge?.condition)
    return 'is-configured'
  return 'is-empty'
})

function getApprovalResultLabel(edge) {
  if (!edge)
    return ''
  if (edge.approvalResultLabel)
    return edge.approvalResultLabel
  const byValue = approvalResultLabels.find(item => item.value === edge.approvalResult)
  if (byValue)
    return byValue.label
  const normalized = normalizeConditionExpression(edge.condition)
  return approvalResultLabels.find(item => normalizeConditionExpression(item.expression) === normalized)?.label || ''
}

function normalizeConditionExpression(value) {
  return String(value || '')
    .trim()
    .replaceAll('"', '\'')
    .replace(/\s+/g, '')
}
</script>

<template>
  <div
    class="branch-header text-xs absolute z-30 flex cursor-pointer items-center px-2 py-0.5"
    :class="colorClass"
    :style="{
      left: `${position.x}px`,
      top: `${position.y}px`,
      transform: 'translate(-50%, -50%)',
    }"
    :title="labelTitle"
    @click.stop="$emit('click', edge)"
  >
    <span class="branch-header-text">{{ labelText }}</span>
  </div>
</template>

<style scoped>
.branch-header {
  max-width: 112px;
  overflow: hidden;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 4px;
  background: color-mix(in srgb, var(--bg-primary) 95%, transparent);
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.08);
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: box-shadow 160ms ease;
}

.branch-header-text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.branch-header:hover {
  box-shadow: 0 3px 8px rgba(15, 23, 42, 0.1);
}

.branch-header.is-default {
  border-color: rgba(245, 158, 11, 0.32);
  background: #fff8ed;
  color: #b45309;
}

.branch-header.is-configured {
  border-color: rgba(53, 109, 255, 0.24);
  background: #f2f6ff;
  color: #245bdb;
}

.branch-header.is-empty {
  border-style: dashed;
  color: var(--text-tertiary);
}

@media (prefers-reduced-motion: reduce) {
  .branch-header {
    transition: none;
  }

  .branch-header:hover {
    box-shadow: 0 1px 3px rgba(15, 23, 42, 0.08);
  }
}
</style>
