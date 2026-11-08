<script setup>
/**
 * BranchHeader — 分支线旁条件标注（小胶囊，不是节点卡）
 *
 * 挂在网关出口连线上，点击打开条件配置；视觉上应轻量，避免被当成审批节点。
 */
import { computed } from 'vue'
import { getNodePresentation } from '../constants/node-presentation.js'
import { summarizeCondition } from '../utils/condition-summary.js'

const props = defineProps({
  edge: { type: Object, required: true },
  position: { type: Object, required: true },
  /** @deprecated 大卡片模式已废弃，保留 prop 以免旧调用报错 */
  card: Boolean,
  compact: Boolean,
  index: { type: Number, default: 1 },
  gatewayType: { type: String, default: 'condition' },
  fields: { type: Array, default: () => [] },
})

const emit = defineEmits(['click', 'pointerDown'])

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
  const summary = summarizeCondition(props.edge, props.fields)
  if (summary)
    return summary
  if (!props.edge?.condition) {
    if (props.gatewayType === 'parallel')
      return `分支 ${props.index}`
    return '配置条件'
  }
  const ruleCount = Array.isArray(props.edge?.conditionRules) ? props.edge.conditionRules.length : 0
  return ruleCount > 1 ? `${ruleCount} 条条件` : '条件已设'
})

const labelTitle = computed(() => {
  if (props.edge?.isDefault)
    return '默认分支（点击配置）'
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

const iconClass = computed(() => (
  props.gatewayType === 'parallel' ? 'i-lucide:split' : 'i-lucide:git-branch'
))

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
  <!-- 线旁胶囊标注：小图标 + 短文案，不占节点位 -->
  <button
    type="button"
    class="branch-header"
    :class="colorClass"
    :style="{
      'left': `${position.x}px`,
      'top': `${position.y}px`,
      'transform': 'translate(-50%, -50%)',
      '--branch-color': getNodePresentation(gatewayType).color,
    }"
    :title="labelTitle"
    :aria-label="'配置分支：' + labelText"
    @pointerdown.stop="emit('pointerDown', $event, edge)"
    @click.stop="emit('click', edge)"
  >
    <span class="branch-header-icon" aria-hidden="true">
      <i :class="iconClass" />
    </span>
    <span class="branch-header-text">{{ labelText }}</span>
  </button>
</template>

<style scoped>
.branch-header {
  position: absolute;
  /* 高于未选中连线(1)，低于节点(4)与选中连线把手(8) */
  z-index: 3;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  max-width: 132px;
  height: 24px;
  padding: 0 8px 0 6px;
  overflow: hidden;
  border: 1px solid color-mix(in srgb, var(--branch-color, #10b981) 28%, var(--border-light, #e5e7eb));
  border-radius: 999px;
  background: color-mix(in srgb, var(--bg-primary, #fff) 92%, var(--branch-color, #10b981));
  color: var(--text-secondary, #4e5969);
  font-size: 12px;
  line-height: 1;
  cursor: pointer;
  box-shadow: 0 1px 2px rgb(15 23 42 / 6%);
  white-space: nowrap;
  transition:
    border-color 160ms ease,
    box-shadow 160ms ease,
    background 160ms ease;
}
.branch-header:hover {
  border-color: var(--primary-color, #356de0);
  box-shadow: 0 2px 6px rgb(15 23 42 / 10%);
}
.branch-header:active {
  cursor: grabbing;
}
.branch-header-icon {
  flex: none;
  width: 12px;
  height: 12px;
  color: var(--branch-color, #10b981);
  font-size: 12px;
}
.branch-header-text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.branch-header.is-default {
  color: var(--text-secondary, #4e5969);
}
.branch-header.is-configured {
  color: var(--text-primary, #1d2129);
  font-weight: 500;
}
.branch-header.is-empty {
  border-style: dashed;
  color: var(--text-tertiary, #86909c);
  background: var(--bg-primary, #fff);
}
.branch-header:focus-visible {
  outline: 2px solid var(--primary-color, #356de0);
  outline-offset: 2px;
}

@media (prefers-reduced-motion: reduce) {
  .branch-header {
    transition: none;
  }
}
</style>
