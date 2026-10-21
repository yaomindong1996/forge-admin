<script setup>
import { computed } from 'vue'
import {
  buildConditionExpression,
  conditionIsBetween,
  conditionNeedsValue,
  createConditionRule,
  normalizeConditionDataType,
  normalizeConditionRule,
  parseConditionExpression,
} from '../flow-designer/panel/condition-expression.js'

const props = defineProps({
  branches: { type: Array, default: () => [] },
  fields: { type: Array, default: () => [] },
  readonly: { type: Boolean, default: false },
})

const emit = defineEmits(['update:branches'])

const fieldOptions = computed(() => {
  const seen = new Set()
  return (props.fields || []).map((item) => {
    const value = item?.fieldCode || item?.code || item?.field || item?.name || item?.key
    const label = item?.fieldName || item?.fieldLabel || item?.label || item?.title || value
    return {
      ...item,
      value,
      label,
      dataType: normalizeConditionDataType(item?.dataType || item?.componentType || item?.type),
    }
  }).filter((item) => {
    if (!item.value || seen.has(item.value))
      return false
    seen.add(item.value)
    return true
  })
})

const operatorOptions = [
  { label: '等于', value: 'eq' },
  { label: '不等于', value: 'ne' },
  { label: '大于', value: 'gt' },
  { label: '大于等于', value: 'ge' },
  { label: '小于', value: 'lt' },
  { label: '小于等于', value: 'le' },
  { label: '在区间内', value: 'between' },
  { label: '包含', value: 'contains' },
  { label: '不包含', value: 'notContains' },
  { label: '为空', value: 'empty' },
  { label: '不为空', value: 'notEmpty' },
]

function conditionBranches() {
  return props.branches.filter(branch => branch?.isDefault !== true)
}

function branchLabel(branch, index) {
  if (branch?.label?.trim())
    return branch.label.trim()
  if (branch?.isDefault)
    return '其他情况'
  const conditionalIndex = conditionBranches().findIndex(item => item?.port === branch?.port)
  return `条件 ${conditionalIndex >= 0 ? conditionalIndex + 1 : index + 1}`
}

function branchLogic(branch) {
  const value = branch?.condition?.logic || branch?.condition?.operator
  return String(value || '').toUpperCase() === 'OR' || value === 'any' ? 'any' : 'all'
}

function branchRules(branch) {
  // 已有 rules 数组（含空数组）以数组为准，避免「删光后又凭空补一条」导致删除无效
  if (Array.isArray(branch?.condition?.rules))
    return branch.condition.rules.map(normalizeConditionRule)
  const parsed = parseConditionExpression(branch?.condition?.expression, fieldOptions.value)
  if (parsed.rules.length)
    return parsed.rules
  return []
}

function updateBranchLabel(index, event) {
  const branches = clone(props.branches)
  branches[index].label = event.target.value
  emitBranches(branches)
}

function updateLogic(index, event) {
  patchCondition(index, branchRules(props.branches[index]), event.target.value)
}

function addRule(branchIndex) {
  const rules = [
    ...branchRules(props.branches[branchIndex]),
    createConditionRule(fieldOptions.value[0]?.value || ''),
  ]
  patchCondition(branchIndex, rules, branchLogic(props.branches[branchIndex]))
}

function updateRule(branchIndex, ruleIndex, patch) {
  const rules = branchRules(props.branches[branchIndex])
  rules[ruleIndex] = normalizeConditionRule({ ...rules[ruleIndex], ...patch })
  patchCondition(branchIndex, rules, branchLogic(props.branches[branchIndex]))
}

function removeRule(branchIndex, ruleIndex) {
  const rules = branchRules(props.branches[branchIndex]).filter((_, index) => index !== ruleIndex)
  patchCondition(branchIndex, rules, branchLogic(props.branches[branchIndex]))
}

function patchCondition(branchIndex, rules, logic) {
  const branches = clone(props.branches)
  const normalizedRules = rules.map(normalizeConditionRule)
  branches[branchIndex].condition = {
    operator: logic === 'any' ? 'OR' : 'AND',
    logic,
    rules: normalizedRules,
    expression: buildConditionExpression(normalizedRules, logic, fieldOptions.value),
  }
  branches[branchIndex].isDefault = undefined
  emitBranches(branches)
}

function addBranch() {
  if (props.readonly || props.branches.length >= 20)
    return
  const branches = clone(props.branches)
  const defaultIndex = branches.findIndex(branch => branch?.isDefault)
  const insertIndex = defaultIndex >= 0 ? defaultIndex : branches.length
  const conditionNumber = branches.filter(branch => !branch?.isDefault).length + 1
  branches.splice(insertIndex, 0, {
    port: nextPort(branches),
    label: `条件 ${conditionNumber}`,
    condition: {
      operator: 'AND',
      logic: 'all',
      rules: [createConditionRule(fieldOptions.value[0]?.value || '')],
      expression: '',
    },
  })
  emitBranches(branches)
}

function setDefaultBranch(index) {
  if (props.readonly || props.branches[index]?.isDefault)
    return
  const branches = clone(props.branches)
  let conditionNumber = 0
  branches.forEach((branch, branchIndex) => {
    if (branchIndex === index) {
      branch.isDefault = true
      branch.condition = undefined
      branch.label = '其他情况'
      return
    }
    branch.isDefault = undefined
    conditionNumber += 1
    if (!branch.condition) {
      branch.condition = {
        operator: 'AND',
        logic: 'all',
        rules: [createConditionRule(fieldOptions.value[0]?.value || '')],
        expression: '',
      }
    }
    if (!branch.label || branch.label === '其他情况')
      branch.label = `条件 ${conditionNumber}`
  })
  emitBranches(branches)
}

function removeBranch(index) {
  if (props.readonly || props.branches.length <= 2)
    return
  const branches = clone(props.branches)
  const removedDefault = branches[index]?.isDefault === true
  branches.splice(index, 1)
  if (removedDefault && branches.length) {
    const nextDefault = branches[branches.length - 1]
    nextDefault.isDefault = true
    nextDefault.condition = undefined
    nextDefault.label = '其他情况'
  }
  emitBranches(branches)
}

function emitBranches(branches) {
  emit('update:branches', branches)
}

function nextPort(branches) {
  const ports = new Set(branches.map(branch => branch?.port))
  let sequence = 1
  while (ports.has(`BRANCH_${sequence}`))
    sequence += 1
  return `BRANCH_${sequence}`
}

function clone(value) {
  return JSON.parse(JSON.stringify(value || []))
}
</script>

<template>
  <section class="condition-config-editor">
    <header class="condition-config-head">
      <div>
        <strong>条件分支</strong>
        <span>从上到下依次判断，命中后进入对应分支；默认分支无需设置条件。</span>
      </div>
      <button
        type="button"
        class="add-branch-button"
        :disabled="readonly || branches.length >= 20"
        data-condition-add-branch
        @click="addBranch"
      >
        添加条件分支
      </button>
    </header>

    <div v-if="!fieldOptions.length" class="field-empty-tip">
      当前主业务对象没有可用字段。请先发布对象字段，再配置条件判断。
    </div>

    <article
      v-for="(branch, branchIndex) in branches"
      :key="branch.port"
      class="branch-card"
      :class="{ 'is-default': branch.isDefault }"
      data-condition-branch
    >
      <div class="branch-card-head">
        <label class="branch-name-field">
          <span>{{ branch.isDefault ? '默认分支名称' : '分支名称' }}</span>
          <input
            :value="branchLabel(branch, branchIndex)"
            :disabled="readonly"
            maxlength="40"
            @input="updateBranchLabel(branchIndex, $event)"
          >
        </label>
        <span v-if="branch.isDefault" class="default-badge">默认分支</span>
        <button
          v-else
          type="button"
          class="plain-action"
          :disabled="readonly"
          @click="setDefaultBranch(branchIndex)"
        >
          设为默认
        </button>
        <button
          type="button"
          class="remove-branch-button"
          :disabled="readonly || branches.length <= 2"
          @click="removeBranch(branchIndex)"
        >
          删除分支
        </button>
      </div>

      <div v-if="branch.isDefault" class="default-explanation">
        前面的条件都不满足时进入这里，无需再填写判断规则。
      </div>

      <template v-else>
        <div class="logic-row">
          <span>满足方式</span>
          <select :value="branchLogic(branch)" :disabled="readonly" @change="updateLogic(branchIndex, $event)">
            <option value="all">
              同时满足全部规则
            </option>
            <option value="any">
              满足任意一条规则
            </option>
          </select>
          <button type="button" :disabled="readonly || !fieldOptions.length" @click="addRule(branchIndex)">
            添加判断规则
          </button>
        </div>

        <div v-if="!branchRules(branch).length" class="rule-empty-tip">
          暂无判断规则，请点击「添加判断规则」。
        </div>

        <div
          v-for="(rule, ruleIndex) in branchRules(branch)"
          :key="`${branch.port}-${ruleIndex}`"
          class="condition-rule-row"
          data-condition-rule
        >
          <span class="rule-index">{{ ruleIndex + 1 }}</span>
          <select
            :value="rule.field"
            :disabled="readonly || !fieldOptions.length"
            aria-label="判断字段"
            @change="updateRule(branchIndex, ruleIndex, { field: $event.target.value })"
          >
            <option value="">
              选择业务字段
            </option>
            <option v-for="field in fieldOptions" :key="field.value" :value="field.value">
              {{ field.label }}
            </option>
          </select>
          <select
            :value="rule.operator"
            :disabled="readonly"
            aria-label="判断关系"
            @change="updateRule(branchIndex, ruleIndex, { operator: $event.target.value })"
          >
            <option v-for="operator in operatorOptions" :key="operator.value" :value="operator.value">
              {{ operator.label }}
            </option>
          </select>
          <div v-if="conditionNeedsValue(rule.operator)" class="rule-value" :class="{ 'is-between': conditionIsBetween(rule.operator) }">
            <input
              :value="rule.value"
              :disabled="readonly"
              :placeholder="conditionIsBetween(rule.operator) ? '起始值' : '比较值'"
              aria-label="判断值"
              @input="updateRule(branchIndex, ruleIndex, { value: $event.target.value })"
            >
            <input
              v-if="conditionIsBetween(rule.operator)"
              :value="rule.endValue"
              :disabled="readonly"
              placeholder="结束值"
              aria-label="结束值"
              @input="updateRule(branchIndex, ruleIndex, { endValue: $event.target.value })"
            >
          </div>
          <span v-else class="rule-no-value">无需填写值</span>
          <button
            type="button"
            class="remove-rule-button"
            :disabled="readonly"
            aria-label="删除判断规则"
            @click="removeRule(branchIndex, ruleIndex)"
          >
            删除
          </button>
        </div>
      </template>
    </article>
  </section>
</template>

<style scoped>
.condition-config-editor {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.condition-config-head,
.branch-card-head,
.logic-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.condition-config-head > div {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 3px;
}

.condition-config-head strong {
  color: var(--text-color-1, #0f172a);
  font-size: 14px;
}

.condition-config-head span,
.default-explanation,
.field-empty-tip {
  color: var(--text-color-3, #64748b);
  font-size: 12px;
  line-height: 1.55;
}

.add-branch-button,
.logic-row button,
.plain-action,
.remove-branch-button,
.remove-rule-button {
  min-height: 30px;
  flex: 0 0 auto;
  border: 1px solid rgba(37, 99, 235, 0.3);
  border-radius: 6px;
  background: rgba(37, 99, 235, 0.06);
  padding: 4px 9px;
  color: var(--primary-color, #2563eb);
  cursor: pointer;
  font-size: 12px;
}

.branch-card {
  display: flex;
  flex-direction: column;
  gap: 11px;
  border: 1px solid rgba(148, 163, 184, 0.32);
  border-radius: 8px;
  background: var(--card-color, #fff);
  padding: 12px;
}

.branch-card.is-default {
  border-color: rgba(217, 119, 6, 0.28);
  background: rgba(255, 251, 235, 0.58);
}

.branch-name-field {
  display: grid;
  min-width: 0;
  flex: 1;
  align-items: center;
  gap: 8px;
  grid-template-columns: auto minmax(100px, 1fr);
}

.branch-name-field span,
.logic-row > span {
  color: var(--text-color-2, #334155);
  font-size: 12px;
  font-weight: 600;
}

.branch-name-field input,
.logic-row select,
.condition-rule-row select,
.condition-rule-row input {
  min-height: 32px;
  min-width: 0;
  border: 1px solid rgba(148, 163, 184, 0.42);
  border-radius: 6px;
  background: var(--input-color, #fff);
  padding: 5px 8px;
  color: var(--text-color-1, #0f172a);
  outline: none;
}

.default-badge {
  border-radius: 999px;
  background: rgba(217, 119, 6, 0.12);
  padding: 4px 8px;
  color: #b45309;
  font-size: 11px;
  font-weight: 600;
}

.remove-branch-button,
.remove-rule-button {
  border-color: rgba(220, 38, 38, 0.22);
  background: rgba(220, 38, 38, 0.05);
  color: var(--error-color, #dc2626);
}

.logic-row {
  justify-content: flex-start;
}

.logic-row select {
  min-width: 170px;
}

.logic-row button {
  margin-left: auto;
}

.condition-rule-row {
  display: grid;
  align-items: center;
  gap: 7px;
  grid-template-columns: 24px minmax(105px, 1.2fr) minmax(96px, 0.9fr) minmax(100px, 1fr) auto;
}

.rule-index {
  display: inline-flex;
  width: 22px;
  height: 22px;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(100, 116, 139, 0.11);
  color: var(--text-color-2, #475569);
  font-size: 11px;
}

.rule-value {
  display: grid;
  min-width: 0;
}

.rule-value.is-between {
  gap: 5px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.rule-no-value,
.rule-empty-tip {
  color: var(--text-color-3, #64748b);
  font-size: 12px;
}

.rule-empty-tip {
  line-height: 1.5;
}

.field-empty-tip {
  border-left: 3px solid #d97706;
  background: rgba(255, 251, 235, 0.7);
  padding: 8px 10px;
}

button:disabled,
select:disabled,
input:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

@container node-config (max-width: 620px) {
  .condition-config-head,
  .branch-card-head,
  .logic-row {
    align-items: stretch;
    flex-wrap: wrap;
  }

  .branch-name-field {
    flex-basis: 100%;
  }

  .logic-row select {
    min-width: 0;
    flex: 1;
  }

  .logic-row button {
    margin-left: 0;
  }

  .condition-rule-row {
    grid-template-columns: 24px minmax(0, 1fr) minmax(96px, 0.7fr);
  }

  .condition-rule-row .rule-value,
  .condition-rule-row .rule-no-value {
    grid-column: 2 / -1;
  }

  .condition-rule-row .remove-rule-button {
    justify-self: end;
    grid-column: 2 / -1;
  }
}
</style>
