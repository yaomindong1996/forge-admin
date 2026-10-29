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
import { getNodePresentation } from '../constants/node-presentation.js'
import { summarizeCondition } from '../utils/condition-summary.js'

const props = defineProps({
  edge: { type: Object, required: true },
  position: { type: Object, required: true },
  card: Boolean,
  /** 并行等场景用窄标签，少占横向空间 */
  compact: Boolean,
  index: { type: Number, default: 1 },
  gatewayType: { type: String, default: 'condition' },
  fields: { type: Array, default: () => [] },
})

defineEmits(['click'])

const approvalResultLabels = [
  { label: '同意通过', value: 'approve', expression: '$' + '{approvalResult == \'approve\'}' },
  { label: '驳回修改', value: 'reject', expression: '$' + '{approvalResult == \'reject\'}' },
  { label: '退回上一步', value: 'return', expression: '$' + '{approvalResult == \'return\'}' },
  { label: '终止流程', value: 'terminate', expression: '$' + '{approvalResult == \'terminate\'}' },
]

const isCompactCard = computed(() => props.card && (props.compact || props.gatewayType === 'parallel'))

const labelText = computed(() => {
  if (props.edge?.isDefault)
    return props.card ? '其他情况' : '默认'
  const approvalLabel = getApprovalResultLabel(props.edge)
  if (approvalLabel)
    return approvalLabel
  const summary = summarizeCondition(props.edge, props.fields)
  if (props.card && summary)
    return summary
  if (!props.edge?.condition) {
    if (props.gatewayType === 'parallel')
      return isCompactCard.value ? `分支 ${props.index}` : '并行分支'
    return '配置条件'
  }
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

const subtitle = computed(() => {
  if (props.edge?.isDefault)
    return '默认条件'
  if (props.gatewayType === 'parallel')
    return isCompactCard.value ? '同时执行' : `分支 ${props.index} · 同时执行`
  return `条件 ${props.index}`
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
  <button
    type="button" class="branch-header"
    :class="[colorClass, { 'is-card': card, 'is-compact': isCompactCard }]"
    :style="{
      'left': `${position.x}px`,
      'top': `${position.y}px`,
      'transform': 'translate(-50%, -50%)',
      '--branch-color': getNodePresentation(gatewayType).color,
    }"
    :title="labelTitle"
    :aria-label="`配置分支：${labelText}`"
    @click.stop="$emit('click', edge)"
  >
    <span v-if="card" class="branch-header-icon"><i :class="gatewayType === 'parallel' ? 'i-lucide:split' : 'i-lucide:git-branch'" /></span>
    <span class="branch-header-main">
      <span class="branch-header-text">{{ labelText }}</span>
      <span v-if="card" class="branch-header-subtitle">{{ subtitle }}</span>
    </span>
    <i v-if="card && !isCompactCard" class="branch-header-arrow i-lucide:chevron-right" />
  </button>
</template>

<style scoped>
.branch-header {
  position: absolute;
  z-index: 30;
  display: flex;
  align-items: center;
  max-width: 112px;
  padding: 3px 8px;
  overflow: hidden;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 4px;
  background: color-mix(in srgb, var(--bg-primary) 95%, transparent);
  color: var(--text-secondary, #4e5969);
  font-size: 12px;
  cursor: pointer;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: border-color 160ms ease;
}

.branch-header-text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.branch-header:hover {
  border-color: var(--primary-color, #356de0);
}

.branch-header.is-default {
  color: var(--text-secondary, #4e5969);
}

.branch-header.is-configured {
  color: var(--text-primary, #1d2129);
}

.branch-header-main {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 5px;
  text-align: left;
}
.branch-header.is-card {
  width: 280px;
  max-width: none;
  min-height: 64px;
  gap: 12px;
  padding: 12px;
  border-radius: 6px;
  border-style: solid;
  background: var(--bg-primary, #fff);
  color: var(--text-primary, #1d2129);
}
.branch-header.is-card.is-compact {
  width: 118px;
  min-height: 40px;
  gap: 6px;
  padding: 6px 8px;
}
.branch-header.is-card .branch-header-text {
  font-size: 14px;
  font-weight: 500;
}
.branch-header.is-card.is-compact .branch-header-text {
  font-size: 12px;
}
.branch-header.is-card.is-compact .branch-header-main {
  gap: 1px;
}
.branch-header-icon {
  display: flex;
  width: 36px;
  height: 36px;
  flex: none;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  background: color-mix(in srgb, var(--branch-color) 14%, var(--bg-primary, #fff));
  color: var(--branch-color);
  font-size: 22px;
}
.branch-header.is-compact .branch-header-icon {
  width: 22px;
  height: 22px;
  border-radius: 4px;
  font-size: 13px;
}
.branch-header-subtitle {
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
  font-weight: 400;
}
.branch-header.is-compact .branch-header-subtitle {
  font-size: 10px;
}
.branch-header-arrow {
  flex: none;
  color: var(--text-tertiary, #86909c);
}
.branch-header:focus-visible {
  outline: 2px solid var(--primary-color, #356de0);
  outline-offset: 2px;
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
