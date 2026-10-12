<script setup>
/**
 * ConditionConfig — 网关条件配置
 *
 * 条件分支支持三种编辑方式：
 * - 审批结果：业务人员选择“同意通过 / 驳回修改”等结果，系统生成表达式
 * - 业务字段条件：用动态表单字段 + 运算符 + 值生成 SpEL 条件表达式
 * - 开发者高级配置：保留原始手写表达式
 *
 * BPMN 导出仍只消费 edge.condition，因此规则配置器必须同步生成标准表达式字符串。
 */
import { computed } from 'vue'
import {
  buildConditionExpression,
  conditionIsBetween,
  conditionNeedsValue,
  createConditionRule,
  normalizeConditionDataType,
  normalizeConditionRule,
  parseConditionExpression as parseSharedConditionExpression,
} from './condition-expression.js'

const props = defineProps({
  node: { type: Object, required: true },
  outgoingEdges: { type: Array, default: () => [] },
  nodes: { type: Array, default: () => [] },
  formFieldCatalog: { type: Array, default: () => [] },
  focusEdgeId: { type: String, default: '' },
  readonly: Boolean,
})

const emit = defineEmits(['update:edge', 'update:config'])

const edges = computed(() => props.outgoingEdges || [])
const focusedEdge = computed(() => props.focusEdgeId ? edges.value.find(edge => edge.id === props.focusEdgeId) : null)
const visibleEdges = computed(() => focusedEdge.value ? [focusedEdge.value] : edges.value)
const focusedBranchIndex = computed(() => focusedEdge.value ? getBranchIndex(focusedEdge.value) : -1)
const isConditionGateway = computed(() => props.node?.nodeType === 'condition')
const nodeNameMap = computed(() => new Map((props.nodes || []).map(node => [node.id, node.name || '未命名节点'])))

const formFields = computed(() => {
  const fieldMap = new Map()
  for (const item of props.formFieldCatalog || []) {
    const field = item?.field || item?.fieldName || item?.name || item?.key
    if (!field || fieldMap.has(field))
      continue
    const dataType = normalizeDataType(item?.dataType || item?.componentType || item?.type)
    fieldMap.set(field, {
      field,
      label: item?.label || item?.title || field,
      dataType,
      required: !!item?.required,
      componentType: item?.componentType || '',
    })
  }
  return Array.from(fieldMap.values())
})

const fieldOptions = computed(() => formFields.value.map(item => ({
  label: item.required ? `${item.label}（必填）` : item.label,
  value: item.field,
  dataType: item.dataType,
})))

const fieldMetaMap = computed(() => new Map(formFields.value.map(item => [item.field, item])))

const logicOptions = [
  { label: '符合全部', value: 'all' },
  { label: '符合任一', value: 'any' },
]

const operatorOptions = [
  { label: '等于', value: 'eq' },
  { label: '不等于', value: 'ne' },
  { label: '大于', value: 'gt' },
  { label: '大于等于', value: 'ge' },
  { label: '小于', value: 'lt' },
  { label: '小于等于', value: 'le' },
  { label: '区间', value: 'between' },
  { label: '包含', value: 'contains' },
  { label: '不包含', value: 'notContains' },
  { label: '为空', value: 'empty' },
  { label: '不为空', value: 'notEmpty' },
]

const approvalResultOptions = [
  {
    label: '同意通过',
    value: 'approve',
    expression: '$' + '{approvalResult == \'approve\'}',
    icon: 'i-material-symbols:check-circle',
    desc: '审批人点击同意后走这条分支',
  },
  {
    label: '驳回修改',
    value: 'reject',
    expression: '$' + '{approvalResult == \'reject\'}',
    icon: 'i-material-symbols:edit-note',
    desc: '审批人要求申请人修改后走这条分支',
  },
  {
    label: '退回上一步',
    value: 'return',
    expression: '$' + '{approvalResult == \'return\'}',
    icon: 'i-material-symbols:keyboard-return',
    desc: '审批人选择退回时走这条分支',
  },
  {
    label: '终止流程',
    value: 'terminate',
    expression: '$' + '{approvalResult == \'terminate\'}',
    icon: 'i-material-symbols:stop-circle',
    desc: '审批人选择终止时走这条分支',
  },
]

function getBranchTitle(index) {
  return `分支 ${index + 1}`
}

function getBranchIndex(edge) {
  const index = edges.value.findIndex(item => item.id === edge?.id)
  return index >= 0 ? index : 0
}

function getBranchTitleByEdge(edge) {
  return getBranchTitle(getBranchIndex(edge))
}

function getTargetName(edge) {
  return nodeNameMap.value.get(edge.target) || '未命名节点'
}

function setDefault(edgeId) {
  if (props.readonly)
    return
  for (const e of edges.value)
    emit('update:edge', e.id, { isDefault: e.id === edgeId })
  emit('update:config', { defaultFlowId: edgeId })
}

function updateCondition(edgeId, value) {
  emit('update:edge', edgeId, {
    condition: value,
    conditionType: 'expression',
    conditionMode: 'advanced',
    advancedCondition: value,
  })
}

function updateMode(edge, mode) {
  if (props.readonly)
    return

  if (mode === 'approvalResult') {
    updateApprovalResult(edge, getApprovalResultValue(edge) || 'approve')
    return
  }

  if (mode === 'advanced') {
    const advancedCondition = getAdvancedCondition(edge)
    const patch = {
      condition: advancedCondition,
      conditionMode: 'advanced',
      conditionType: 'expression',
      advancedCondition,
    }
    if (getMode(edge) === 'rules' && edge.rulesCondition === undefined)
      patch.rulesCondition = edge.condition || ''
    emit('update:edge', edge.id, patch)
    return
  }

  const rules = ensureRules(edge)
  emitRulesPatch(edge.id, rules, getLogic(edge), edge)
}

function getMode(edge) {
  if (edge?.conditionMode)
    return edge.conditionMode
  if (findApprovalResultOption(edge?.condition))
    return 'approvalResult'
  if (edge?.conditionRules?.length)
    return 'rules'
  if (parseConditionExpression(getRulesCondition(edge)).rules.length)
    return 'rules'
  return edge?.condition ? 'advanced' : 'approvalResult'
}

function getApprovalResultValue(edge) {
  const configured = edge?.approvalResult
  if (approvalResultOptions.some(item => item.value === configured))
    return configured
  return findApprovalResultOption(edge?.condition)?.value || ''
}

function updateApprovalResult(edge, value) {
  if (props.readonly)
    return
  const option = approvalResultOptions.find(item => item.value === value)
  if (!option)
    return
  const patch = {
    condition: option.expression,
    conditionType: 'expression',
    conditionMode: 'approvalResult',
    approvalResult: option.value,
    approvalResultLabel: option.label,
  }
  if (getMode(edge) === 'advanced' && edge.advancedCondition === undefined)
    patch.advancedCondition = edge.condition || ''
  if (getMode(edge) === 'rules' && edge.rulesCondition === undefined)
    patch.rulesCondition = edge.condition || ''
  emit('update:edge', edge.id, patch)
}

function findApprovalResultOption(condition) {
  const normalized = normalizeConditionExpression(condition)
  return approvalResultOptions.find(item => normalizeConditionExpression(item.expression) === normalized)
}

function normalizeConditionExpression(value) {
  return String(value || '')
    .trim()
    .replaceAll('"', '\'')
    .replace(/\s+/g, '')
}

function getLogic(edge) {
  if (!edge?.conditionLogic) {
    const parsed = parseConditionExpression(getRulesCondition(edge))
    if (parsed.rules.length)
      return parsed.logic
  }
  return edge?.conditionLogic === 'any' ? 'any' : 'all'
}

function getRules(edge) {
  return ensureRules(edge)
}

function ensureRules(edge) {
  if (Array.isArray(edge?.conditionRules))
    return edge.conditionRules.map(normalizeRule)
  const parsed = parseConditionExpression(getRulesCondition(edge))
  if (parsed.rules.length)
    return parsed.rules
  return [createDefaultRule()]
}

function getAdvancedCondition(edge) {
  if (edge?.advancedCondition !== undefined)
    return edge.advancedCondition
  return edge?.condition || ''
}

function getRulesCondition(edge) {
  if (edge?.rulesCondition !== undefined)
    return edge.rulesCondition
  if (edge?.conditionMode === 'advanced')
    return ''
  return edge?.condition || ''
}

function createDefaultRule() {
  return createConditionRule(formFields.value[0]?.field || '')
}

function normalizeRule(rule = {}) {
  return normalizeConditionRule(rule)
}

function addRule(edge) {
  if (props.readonly)
    return
  const rules = [...ensureRules(edge), createDefaultRule()]
  emitRulesPatch(edge.id, rules, getLogic(edge), edge)
}

function removeRule(edge, index) {
  if (props.readonly)
    return
  const rules = ensureRules(edge).filter((_, idx) => idx !== index)
  emitRulesPatch(edge.id, rules, getLogic(edge), edge)
}

function updateRule(edge, index, patch) {
  if (props.readonly)
    return
  const rules = ensureRules(edge)
  rules[index] = normalizeRule({ ...rules[index], ...patch })
  emitRulesPatch(edge.id, rules, getLogic(edge), edge)
}

function updateLogic(edge, logic) {
  if (props.readonly)
    return
  emitRulesPatch(edge.id, ensureRules(edge), logic, edge)
}

function emitRulesPatch(edgeId, rules, logic, edge = null) {
  const normalizedRules = rules.map(normalizeRule)
  const rulesCondition = buildExpression(normalizedRules, logic)
  const patch = {
    condition: rulesCondition,
    conditionType: 'expression',
    conditionMode: 'rules',
    conditionLogic: logic,
    conditionRules: normalizedRules,
    rulesCondition,
  }
  if (edge && getMode(edge) === 'advanced' && edge.advancedCondition === undefined)
    patch.advancedCondition = edge.condition || ''
  emit('update:edge', edgeId, patch)
}

function buildExpression(rules, logic) {
  return buildConditionExpression(rules, logic, fieldMetaMap.value)
}

function normalizeDataType(type) {
  return normalizeConditionDataType(type)
}

function needValue(operator) {
  return conditionNeedsValue(operator)
}

function isBetween(operator) {
  return conditionIsBetween(operator)
}

function parseConditionExpression(condition) {
  return parseSharedConditionExpression(condition, fieldMetaMap.value)
}
</script>

<template>
  <div class="condition-config">
    <div class="condition-summary">
      <div class="condition-summary-main">
        <div class="condition-summary-title">
          分支条件
        </div>
        <div class="condition-summary-desc">
          <template v-if="focusedEdge">
            正在配置 {{ getBranchTitle(focusedBranchIndex) }}，该网关共 {{ edges.length }} 条分支
          </template>
          <template v-else>
            该网关共 {{ edges.length }} 条分支
          </template>
          <span v-if="isConditionGateway">，可使用 {{ formFields.length }} 个表单字段生成表达式</span>
        </div>
      </div>
    </div>

    <div v-if="edges.length === 0" class="condition-empty">
      暂无分支，请先在画布上添加下游节点
    </div>

    <div
      v-for="e in visibleEdges"
      :key="e.id"
      class="condition-branch"
    >
      <div class="condition-branch-header">
        <div class="min-w-0">
          <div class="condition-branch-title">
            {{ getBranchTitleByEdge(e) }}
          </div>
          <div class="condition-branch-target">
            下游节点：{{ getTargetName(e) }}
          </div>
        </div>
        <n-tag v-if="e.isDefault" type="warning" size="small" :bordered="false">
          默认分支
        </n-tag>
      </div>

      <div v-if="e.isDefault" class="condition-default-tip">
        默认分支在 Flowable 中不会执行条件；这里的条件会保留在设计器里，但部署导出时不会写入该分支。
      </div>

      <template v-if="isConditionGateway">
        <div class="condition-mode-switch">
          <button
            type="button"
            class="condition-mode-button"
            :class="{ active: getMode(e) === 'approvalResult' }"
            :disabled="readonly"
            data-test="mode-approval-result"
            @click="updateMode(e, 'approvalResult')"
          >
            审批结果
          </button>
          <button
            type="button"
            class="condition-mode-button"
            :class="{ active: getMode(e) === 'rules' }"
            :disabled="readonly || formFields.length === 0"
            data-test="mode-rules"
            @click="updateMode(e, 'rules')"
          >
            业务字段
          </button>
          <button
            type="button"
            class="condition-mode-button"
            :class="{ active: getMode(e) === 'advanced' }"
            :disabled="readonly"
            data-test="mode-advanced"
            @click="updateMode(e, 'advanced')"
          >
            开发者高级
          </button>
        </div>

        <div v-if="getMode(e) === 'approvalResult'" class="approval-result-panel">
          <button
            v-for="item in approvalResultOptions"
            :key="item.value"
            type="button"
            class="approval-result-card"
            :class="{ active: getApprovalResultValue(e) === item.value }"
            :disabled="readonly"
            :data-test="`approval-result-${item.value}`"
            @click="updateApprovalResult(e, item.value)"
          >
            <span class="approval-result-icon">
              <i :class="item.icon" />
            </span>
            <span class="approval-result-copy">
              <span class="approval-result-title">{{ item.label }}</span>
              <span class="approval-result-desc">{{ item.desc }}</span>
            </span>
          </button>
          <div class="approval-result-tip">
            选择业务动作即可，系统会在导出 BPMN 时自动转换为 Flowable 条件。
          </div>
        </div>

        <div v-else-if="getMode(e) === 'rules' && formFields.length > 0" class="condition-rule-panel">
          <div v-if="getRules(e).length > 0" class="condition-rule-header condition-rule-grid">
            <span />
            <span>字段</span>
            <span>关系</span>
            <span>取值</span>
            <span />
          </div>

          <div
            v-for="(rule, ruleIndex) in getRules(e)"
            :key="`${e.id}-${ruleIndex}`"
            class="condition-rule-row condition-rule-grid"
          >
            <div class="condition-rule-index">
              {{ ruleIndex + 1 }}
            </div>

            <n-select
              class="condition-rule-field"
              :value="rule.field"
              :options="fieldOptions"
              :disabled="readonly"
              size="small"
              filterable
              data-test="rule-field"
              @update:value="updateRule(e, ruleIndex, { field: $event })"
            />

            <n-select
              class="condition-rule-operator"
              :value="rule.operator"
              :options="operatorOptions"
              :disabled="readonly"
              size="small"
              data-test="rule-operator"
              @update:value="updateRule(e, ruleIndex, { operator: $event })"
            />

            <div
              v-if="needValue(rule.operator)"
              class="condition-rule-value-wrap"
              :class="{ 'is-between': isBetween(rule.operator) }"
            >
              <template v-if="isBetween(rule.operator)">
                <n-input
                  class="condition-rule-value"
                  :value="rule.value"
                  :disabled="readonly"
                  placeholder="起"
                  size="small"
                  data-test="rule-value"
                  @update:value="updateRule(e, ruleIndex, { value: $event })"
                />
                <n-input
                  class="condition-rule-value"
                  :value="rule.endValue"
                  :disabled="readonly"
                  placeholder="止"
                  size="small"
                  data-test="rule-end-value"
                  @update:value="updateRule(e, ruleIndex, { endValue: $event })"
                />
              </template>
              <n-input
                v-else
                class="condition-rule-value"
                :value="rule.value"
                :disabled="readonly"
                placeholder="请输入值"
                size="small"
                data-test="rule-value"
                @update:value="updateRule(e, ruleIndex, { value: $event })"
              />
            </div>
            <div v-else class="condition-rule-value-placeholder">
              无需填写
            </div>

            <button
              type="button"
              class="condition-rule-remove"
              :disabled="readonly"
              :aria-label="`删除${getBranchTitleByEdge(e)}条件${ruleIndex + 1}`"
              @click="removeRule(e, ruleIndex)"
            >
              <i class="i-material-symbols:remove-circle-outline" />
            </button>
          </div>

          <div v-if="getRules(e).length === 0" class="condition-rule-empty">
            暂无条件，点击“添加条件”后重新配置。
          </div>

          <button
            type="button"
            class="condition-add-rule"
            :disabled="readonly"
            @click="addRule(e)"
          >
            <i class="i-material-symbols:add-circle-outline" />
            添加条件
          </button>

          <div class="condition-rule-footer">
            <n-radio
              v-for="item in logicOptions"
              :key="item.value"
              :checked="getLogic(e) === item.value"
              :disabled="readonly"
              @click="updateLogic(e, item.value)"
            >
              {{ item.label }}
            </n-radio>
            <span class="condition-expression-preview">{{ e.condition || '完成条件后自动生成表达式' }}</span>
          </div>
        </div>

        <div v-else-if="getMode(e) === 'rules'" class="condition-empty">
          当前流程尚未配置动态表单字段，请先在“更多设置 / 表单配置”中设计或选择动态表单。
        </div>

        <n-form-item
          v-else
          label="开发者高级条件"
          label-placement="top"
          :show-feedback="false"
        >
          <n-input
            class="condition-advanced-input"
            :value="getAdvancedCondition(e)"
            :disabled="readonly"
            type="textarea"
            placeholder="${amount > 10000}"
            :autosize="{ minRows: 2, maxRows: 4 }"
            @update:value="updateCondition(e.id, $event)"
          />
        </n-form-item>
      </template>

      <div v-else class="condition-default-tip">
        当前网关类型只展示分支列表，不需要配置条件表达式。
      </div>

      <div class="condition-default-row">
        <n-radio
          :checked="e.isDefault"
          :disabled="readonly || !isConditionGateway"
          @click="setDefault(e.id)"
        >
          作为默认分支
        </n-radio>
      </div>
    </div>
  </div>
</template>

<style scoped>
.condition-config {
  padding: 0 18px 18px;
}

.condition-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 0 12px;
}

.condition-summary-main {
  min-width: 0;
}

.condition-summary-title {
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 700;
}

.condition-summary-desc {
  margin-top: 4px;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.condition-empty {
  border: 1px dashed #dbe4ef;
  border-radius: 8px;
  background: var(--bg-secondary);
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.6;
  padding: 10px 12px;
}

.condition-branch {
  min-width: 0;
  margin-top: 12px;
  border: 1px solid #e5eaf3;
  border-radius: 8px;
  background: var(--bg-primary);
  padding: 14px;
}

.condition-branch-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.condition-branch-title {
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 700;
}

.condition-branch-target {
  margin-top: 4px;
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.condition-default-tip {
  margin-top: 12px;
  border-radius: 7px;
  background: var(--bg-secondary);
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.6;
  padding: 9px 10px;
}

.condition-mode-switch {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 4px;
  margin-top: 14px;
  border: 1px solid #dbe4ef;
  border-radius: 8px;
  background: var(--bg-secondary);
  padding: 3px;
}

.condition-mode-button {
  height: 30px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
  transition:
    background 160ms ease,
    color 160ms ease,
    box-shadow 160ms ease;
}

.condition-mode-button.active {
  background: var(--bg-primary);
  color: #2563eb;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.08);
}

.condition-mode-button:disabled {
  color: #cbd5e1;
  cursor: not-allowed;
}

.approval-result-panel {
  display: grid;
  gap: 8px;
  margin-top: 12px;
}

.approval-result-card {
  min-height: 58px;
  border: 1px solid #dbe4ef;
  border-radius: 8px;
  display: flex;
  align-items: flex-start;
  gap: 10px;
  background: var(--bg-primary);
  color: inherit;
  cursor: pointer;
  padding: 10px;
  text-align: left;
  transition:
    border-color 160ms ease,
    background 160ms ease,
    box-shadow 160ms ease;
}

.approval-result-card.active {
  border-color: #2563eb;
  background: #eff6ff;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.12);
}

.approval-result-card:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.approval-result-icon {
  width: 20px;
  height: 20px;
  flex: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #2563eb;
  font-size: 18px;
}

.approval-result-copy {
  min-width: 0;
  display: grid;
  gap: 3px;
}

.approval-result-title {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 700;
  line-height: 1.3;
}

.approval-result-desc,
.approval-result-tip {
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.approval-result-tip {
  border-radius: 7px;
  background: var(--bg-secondary);
  padding: 8px 10px;
}

.condition-rule-panel {
  margin-top: 12px;
  min-width: 0;
}

.condition-rule-grid {
  display: grid;
  grid-template-columns: 28px minmax(126px, 1fr) 104px minmax(118px, 1fr) 28px;
  column-gap: 8px;
  align-items: center;
}

.condition-rule-header {
  margin-bottom: 6px;
  padding: 0 8px;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
}

.condition-rule-row {
  align-items: start;
  margin-top: 10px;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  background: #fbfdff;
  padding: 8px;
}

.condition-rule-remove {
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  color: var(--text-tertiary);
  cursor: pointer;
  font-size: 18px;
  justify-self: end;
  transition:
    color 160ms ease,
    background 160ms ease;
}

.condition-rule-remove:hover:not(:disabled) {
  background: var(--bg-tertiary);
  color: #ef4444;
}

.condition-rule-remove:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.condition-rule-index {
  min-width: 20px;
  height: 24px;
  border-radius: 999px;
  background: #eef4ff;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 24px;
  text-align: center;
}

.condition-rule-field,
.condition-rule-operator,
.condition-rule-value {
  min-width: 0;
  max-width: 100%;
}

.condition-rule-value-wrap {
  min-width: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 6px;
}

.condition-rule-value-wrap.is-between {
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
}

.condition-rule-value-placeholder {
  min-height: 28px;
  display: flex;
  align-items: center;
  border-radius: 6px;
  background: var(--bg-secondary);
  color: var(--text-tertiary);
  font-size: 12px;
  padding: 0 8px;
}

.condition-rule-empty {
  margin-top: 10px;
  border: 1px dashed #dbe4ef;
  border-radius: 8px;
  background: var(--bg-secondary);
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.6;
  padding: 9px 10px;
}

.condition-add-rule {
  margin-top: 12px;
  height: 28px;
  border: none;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: transparent;
  color: #2563eb;
  cursor: pointer;
  font-size: 13px;
}

.condition-add-rule:disabled {
  color: #cbd5e1;
  cursor: not-allowed;
}

.condition-rule-footer {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
  border-top: 1px solid #eef2f7;
  padding-top: 12px;
}

.condition-expression-preview {
  min-width: 0;
  flex: 1;
  flex-basis: 100%;
  max-height: 72px;
  overflow: auto;
  border-radius: 6px;
  background: var(--bg-secondary);
  color: var(--text-tertiary);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', monospace;
  font-size: 11px;
  line-height: 1.5;
  overflow-wrap: anywhere;
  padding: 6px 8px;
  white-space: normal;
  word-break: break-word;
}

.condition-default-row {
  margin-top: 12px;
}

.condition-advanced-input {
  min-width: 0;
  max-width: 100%;
}

:deep(.condition-advanced-input textarea) {
  overflow-wrap: anywhere;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 560px) {
  .condition-rule-header {
    display: none;
  }

  .condition-rule-grid {
    grid-template-columns: 28px minmax(0, 1fr) 28px;
    row-gap: 8px;
  }

  .condition-rule-field,
  .condition-rule-operator,
  .condition-rule-value-wrap,
  .condition-rule-value-placeholder {
    grid-column: 2 / 3;
  }

  .condition-rule-remove {
    grid-column: 3 / 4;
    grid-row: 1 / 2;
  }
}

@media (prefers-reduced-motion: reduce) {
  .condition-mode-button,
  .approval-result-card,
  .condition-rule-remove {
    transition: none;
  }
}
</style>
