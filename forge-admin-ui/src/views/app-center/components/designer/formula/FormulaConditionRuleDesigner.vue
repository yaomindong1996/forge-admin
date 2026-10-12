<template>
  <section class="rule-designer">
    <div class="rule-toolbar">
      <div>
        <strong>条件规则</strong>
        <span>{{ compileState.valid ? '已生成表达式快照' : '规则需要修正' }}</span>
      </div>
      <n-button size="tiny" secondary :disabled="disabled" @click="resetRule">
        重置
      </n-button>
    </div>

    <FormulaConditionRuleNode
      :node="ruleModel"
      :fields="allFields"
      :disabled="disabled"
      root
      @update="updateRule"
    />

    <div class="rule-expression">
      <span>表达式</span>
      <code>{{ compiledExpression || '等待规则生成' }}</code>
    </div>

    <div v-if="compileState.errors.length" class="rule-errors">
      <span v-for="error in compileState.errors" :key="error">{{ error }}</span>
    </div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, reactive, watch } from 'vue'
import { compileConditionRule } from '@/api/formula'
import FormulaConditionRuleNode from './FormulaConditionRuleNode.vue'

const props = defineProps({
  form: {
    type: Object,
    required: true,
  },
  allFields: {
    type: Array,
    default: () => [],
  },
  disabled: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['compiled', 'validation'])

const formModel = computed(() => props.form)
const ruleModel = computed(() => normalizeRule(formModel.value.formulaConditionRule))
const fieldDefinitions = computed(() => {
  return (props.allFields || [])
    .filter(item => item && (item.fieldCode || item.field))
    .map(item => ({
      fieldCode: item.fieldCode || item.field,
      fieldName: item.fieldName || item.label || item.fieldCode || item.field,
      dataType: item.dataType || item.fieldType || item.type || item.componentType,
      valueType: item.fieldType || item.type || item.componentType,
    }))
})
const compileState = reactive({
  valid: true,
  expression: '',
  errors: [],
})
const compiledExpression = computed(() => compileState.expression || formModel.value.formulaConditionExpression || '')
let compileTimer = null
let compileSeq = 0

watch(
  () => formModel.value.formulaConditionRule,
  () => scheduleCompile(),
  { deep: true, immediate: true },
)

watch(
  () => props.allFields,
  () => scheduleCompile(),
  { deep: true },
)

onBeforeUnmount(() => {
  window.clearTimeout(compileTimer)
})

function updateRule(value) {
  formModel.value.formulaConditionRule = normalizeRule(value)
  compileRuleLocally()
  scheduleCompile()
}

function resetRule() {
  updateRule(createDefaultRule())
}

function scheduleCompile() {
  compileRuleLocally()
  window.clearTimeout(compileTimer)
  compileTimer = window.setTimeout(() => {
    compileWithBackend()
  }, 220)
}

function compileRuleLocally() {
  const local = compileNode(ruleModel.value)
  compileState.expression = local.expression
  compileState.errors = local.errors
  compileState.valid = local.errors.length === 0
  if (local.expression) {
    emitCompiled(local.expression)
  }
  emit('validation', {
    valid: compileState.valid,
    errors: compileState.errors.map(message => ({ message })),
  })
}

async function compileWithBackend() {
  const seq = ++compileSeq
  try {
    const res = await compileConditionRule({
      rule: ruleModel.value,
      fields: fieldDefinitions.value,
    })
    if (seq !== compileSeq)
      return
    const data = res?.data ?? res
    compileState.valid = Boolean(data?.valid)
    compileState.expression = data?.expression || compileState.expression
    compileState.errors = Array.isArray(data?.errors) ? data.errors : []
    if (data?.expression)
      emitCompiled(data.expression)
    emit('validation', {
      valid: compileState.valid,
      errors: compileState.errors.map(message => ({ message })),
    })
  }
  catch (e) {
    if (seq !== compileSeq)
      return
    compileState.valid = false
    compileState.errors = [e?.message || '条件规则编译失败']
    emit('validation', {
      valid: false,
      errors: compileState.errors.map(message => ({ message })),
    })
  }
}

function emitCompiled(expression) {
  formModel.value.formulaConditionExpression = expression
  formModel.value.formulaExpression = expression
  emit('compiled', expression)
}

function normalizeRule(rule) {
  if (!rule || typeof rule !== 'object')
    return createDefaultRule()
  if (Array.isArray(rule.children) || ['AND', 'OR'].includes(String(rule.operator || '').toUpperCase())) {
    return {
      operator: ['AND', 'OR'].includes(String(rule.operator || '').toUpperCase()) ? String(rule.operator).toUpperCase() : 'AND',
      children: Array.isArray(rule.children) && rule.children.length
        ? rule.children.map(child => normalizeRule(child))
        : [createDefaultCondition()],
    }
  }
  return {
    field: rule.field || fieldDefinitions.value[0]?.fieldCode || '',
    op: rule.op || rule.operator || 'EQ',
    value: normalizeValue(rule.op || rule.operator || 'EQ', rule.value),
  }
}

function createDefaultRule() {
  return {
    operator: 'AND',
    children: [createDefaultCondition()],
  }
}

function createDefaultCondition() {
  return {
    field: fieldDefinitions.value[0]?.fieldCode || '',
    op: 'EQ',
    value: '',
  }
}

function compileNode(node) {
  const errors = []
  const expression = compileNodeInternal(node, errors)
  return { expression, errors }
}

function compileNodeInternal(node, errors) {
  if (!node)
    return ''
  if (Array.isArray(node.children)) {
    if (!node.children.length) {
      errors.push('条件分组不能为空')
      return ''
    }
    const joiner = String(node.operator || 'AND').toUpperCase() === 'OR' ? ' || ' : ' && '
    const parts = node.children.map(child => compileNodeInternal(child, errors)).filter(Boolean)
    return parts.length ? `(${parts.join(joiner)})` : ''
  }
  const field = String(node.field || '').trim()
  const op = String(node.op || 'EQ').toUpperCase()
  if (!field) {
    errors.push('条件字段不能为空')
    return ''
  }
  if (['IS_NULL', 'NOT_NULL'].includes(op))
    return `${field} ${op === 'IS_NULL' ? '==' : '!='} nil`
  if (['IN', 'NOT_IN'].includes(op)) {
    const values = Array.isArray(node.value) ? node.value : []
    if (!values.length)
      errors.push(`${field} 的集合值不能为空`)
    const joiner = op === 'IN' ? ' || ' : ' && '
    const compare = op === 'IN' ? '==' : '!='
    return `(${values.map(value => `${field} ${compare} ${literal(value)}`).join(joiner)})`
  }
  const value = literal(node.value)
  const map = {
    EQ: '==',
    NE: '!=',
    GT: '>',
    GTE: '>=',
    LT: '<',
    LTE: '<=',
  }
  if (map[op])
    return `${field} ${map[op]} ${value}`
  if (op === 'CONTAINS')
    return `string.contains(${field}, ${value})`
  if (op === 'STARTS_WITH')
    return `string.startsWith(${field}, ${value})`
  if (op === 'ENDS_WITH')
    return `string.endsWith(${field}, ${value})`
  errors.push(`不支持的条件操作符: ${op}`)
  return ''
}

function normalizeValue(op, value) {
  const normalized = String(op || '').toUpperCase()
  if (['IS_NULL', 'NOT_NULL'].includes(normalized))
    return null
  if (['IN', 'NOT_IN'].includes(normalized))
    return Array.isArray(value) ? value : []
  return value ?? ''
}

function literal(value) {
  if (value === null || value === undefined || value === '')
    return '\'\''
  if (typeof value === 'number')
    return Number.isFinite(value) ? String(value) : '0'
  if (typeof value === 'boolean')
    return value ? 'true' : 'false'
  return `'${String(value).replace(/\\/g, '\\\\').replace(/'/g, '\\\'')}'`
}
</script>

<style scoped>
.rule-designer {
  display: grid;
  gap: 10px;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: var(--bg-primary);
  padding: 10px 12px;
}

.rule-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.rule-toolbar strong,
.rule-toolbar span {
  display: block;
}

.rule-toolbar strong {
  color: var(--text-primary);
  font-size: 13px;
}

.rule-toolbar span {
  margin-top: 2px;
  color: var(--text-secondary);
  font-size: 12px;
}

.rule-expression {
  display: grid;
  gap: 5px;
}

.rule-expression span {
  color: var(--text-secondary);
  font-size: 12px;
}

.rule-expression code {
  overflow: auto;
  border-radius: 6px;
  background: var(--bg-secondary);
  color: var(--text-primary);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  line-height: 1.6;
  padding: 8px;
}

.rule-errors {
  display: grid;
  gap: 4px;
  border: 1px solid #fecaca;
  border-radius: 6px;
  background: #fef2f2;
  color: #991b1b;
  font-size: 12px;
  line-height: 1.6;
  padding: 8px 10px;
}
</style>
