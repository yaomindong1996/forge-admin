<template>
  <n-modal
    :show="show"
    preset="card"
    class="formula-debugger-modal"
    :title="`公式调试：${targetLabel}`"
    :bordered="false"
    @update:show="emit('update:show', $event)"
  >
    <div class="debugger-panel">
      <header class="debugger-summary">
        <div>
          <span>目标字段</span>
          <strong>{{ targetLabel }}</strong>
        </div>
        <n-tag size="small" :bordered="false" type="info">
          {{ formulaConfig?.type || '未启用' }}
        </n-tag>
      </header>

      <section class="debugger-samples">
        <div class="sample-head">
          <strong>样例值</strong>
          <n-button size="tiny" secondary @click="fillSampleValues">
            填入样例
          </n-button>
        </div>
        <n-empty v-if="!variableFields.length" size="small" description="无依赖变量" />
        <n-form v-else label-placement="top" size="small" :show-feedback="false" class="sample-grid">
          <n-form-item
            v-for="item in variableFields"
            :key="item.field"
            :label="item.label"
          >
            <n-input-number
              v-if="item.inputType === 'number'"
              v-model:value="sampleValues[item.field]"
              :show-button="false"
              class="full-input"
            />
            <n-switch
              v-else-if="item.inputType === 'switch'"
              v-model:value="sampleValues[item.field]"
            />
            <n-input
              v-else
              v-model:value="sampleValues[item.field]"
            />
          </n-form-item>
        </n-form>
      </section>

      <section v-if="debugResult" class="debugger-result" :class="{ failed: !debugResult.success }">
        <div>
          <span>{{ debugResult.success ? '执行成功' : '执行失败' }}</span>
          <strong>{{ formatResultValue(debugResult.result?.[targetFieldCode]) }}</strong>
        </div>
        <div>
          <span>Trace</span>
          <code>{{ debugResult.traceId || '-' }}</code>
        </div>
        <em v-if="debugResult.elapsedMs !== null && debugResult.elapsedMs !== undefined">
          {{ debugResult.elapsedMs }}ms
        </em>
      </section>

      <section v-if="ruleMatchRows.length" class="debugger-rule">
        <div class="rule-debug-head">
          <strong>规则命中</strong>
          <span>{{ ruleMatchSummary }}</span>
        </div>
        <div class="rule-debug-list">
          <div
            v-for="item in ruleMatchRows"
            :key="item.key"
            class="rule-debug-row"
            :class="{ matched: item.matched, group: item.group }"
            :style="{ paddingLeft: `${item.depth * 14}px` }"
          >
            <span>{{ item.label }}</span>
            <n-tag size="small" :type="item.matched ? 'success' : 'warning'" :bordered="false">
              {{ item.matched ? '命中' : '未命中' }}
            </n-tag>
          </div>
        </div>
      </section>

      <FormulaTraceSteps
        v-if="debugResult"
        :steps="debugResult.steps || []"
        :errors="debugResult.errors || []"
      />
    </div>

    <template #footer>
      <div class="debugger-footer">
        <n-button @click="emit('update:show', false)">
          关闭
        </n-button>
        <n-button
          type="primary"
          :loading="debugging"
          :disabled="!canDebug"
          @click="handleDebug"
        >
          执行调试
        </n-button>
      </div>
    </template>
  </n-modal>
</template>

<script setup>
import { useMessage } from 'naive-ui'
import { computed, reactive, ref, watch } from 'vue'
import { debugFormula } from '@/api/formula'
import FormulaTraceSteps from './FormulaTraceSteps.vue'

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
  field: {
    type: Object,
    default: null,
  },
  fields: {
    type: Array,
    default: () => [],
  },
  objectCode: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update:show'])

const message = useMessage()
const sampleValues = reactive({})
const debugging = ref(false)
const debugResult = ref(null)

const targetFieldCode = computed(() => props.field?.fieldCode || props.field?.field || '')
const formulaConfig = computed(() => props.field?.formulaConfig || null)
const targetLabel = computed(() => {
  const name = props.field?.fieldName || props.field?.label || targetFieldCode.value || '目标字段'
  return `${name}（${targetFieldCode.value || '-'}）`
})
const formulaFields = computed(() => (props.fields || []).filter(field => field?.formulaConfig?.type))
const variableCodes = computed(() => {
  const result = []
  appendList(result, formulaConfig.value?.dependsOn)
  if (formulaConfig.value?.type === 'LOOKUP')
    appendList(result, [formulaConfig.value?.lookup?.sourceField])
  appendExpressionVariables(result, formulaConfig.value?.expression)
  if (formulaConfig.value?.type === 'CONDITIONAL')
    appendExpressionVariables(result, formulaConfig.value?.condition?.expression)
  appendRuleFields(result, formulaConfig.value?.rule)
  return result.filter(code => code !== targetFieldCode.value)
})
const variableFields = computed(() => variableCodes.value.map((fieldCode, index) => {
  const meta = resolveFieldMeta(fieldCode)
  return {
    field: fieldCode,
    label: fieldLabel(meta, fieldCode),
    inputType: resolveInputType(meta, fieldCode),
    sample: guessSampleValue(meta, index, fieldCode),
  }
}))
const canDebug = computed(() => Boolean(targetFieldCode.value && formulaConfig.value?.type && formulaFields.value.length))
const ruleMatchRows = computed(() => {
  if (!debugResult.value || formulaConfig.value?.type !== 'CONDITIONAL' || !formulaConfig.value?.rule)
    return []
  const rows = []
  evaluateRuleNode(formulaConfig.value.rule, debugResult.value.contextBefore || sampleValues, rows, 0, 'rule')
  return rows
})
const ruleMatchSummary = computed(() => {
  const root = ruleMatchRows.value[0]
  if (!root)
    return '暂无规则'
  return root.matched ? '整体条件满足' : '整体条件未满足'
})

watch(
  () => props.show,
  (visible) => {
    if (visible)
      resetSamples()
  },
)

watch(
  () => targetFieldCode.value,
  () => {
    debugResult.value = null
    if (props.show)
      resetSamples()
  },
)

function resetSamples() {
  Object.keys(sampleValues).forEach((key) => {
    delete sampleValues[key]
  })
  fillSampleValues()
  debugResult.value = null
}

function fillSampleValues() {
  variableFields.value.forEach((item) => {
    sampleValues[item.field] = item.sample
  })
}

async function handleDebug() {
  if (!canDebug.value) {
    message.warning('当前字段未配置公式')
    return
  }
  debugging.value = true
  debugResult.value = null
  try {
    const res = await debugFormula({
      objectCode: props.objectCode,
      fieldCode: targetFieldCode.value,
      sampleValues: { ...sampleValues },
      formulas: formulaFields.value.map(toFormulaFieldConfig),
    })
    debugResult.value = res?.data ?? res
  }
  catch (e) {
    debugResult.value = {
      success: false,
      errors: { debug: [e?.message || '调试请求失败'] },
      steps: [],
    }
  }
  finally {
    debugging.value = false
  }
}

function toFormulaFieldConfig(field = {}) {
  const config = field.formulaConfig || {}
  return {
    fieldCode: field.fieldCode || field.field,
    type: config.type || 'CALC',
    mode: config.mode || 'STORED',
    expression: config.expression || '',
    dependsOn: config.dependsOn || [],
    aggregate: config.aggregate || null,
    condition: config.condition || null,
    rule: config.rule || null,
    lookup: config.lookup || null,
    crossObject: config.crossObject || null,
  }
}

function appendList(result, value) {
  ;(Array.isArray(value) ? value : []).forEach((item) => {
    const code = String(item || '').trim()
    if (code && !result.includes(code))
      result.push(code)
  })
}

function appendExpressionVariables(result, expression) {
  extractVariables(expression).forEach((item) => {
    if (!result.includes(item))
      result.push(item)
  })
}

function appendRuleFields(result, node) {
  if (!node || typeof node !== 'object')
    return
  if (Array.isArray(node.children)) {
    node.children.forEach(child => appendRuleFields(result, child))
    return
  }
  const field = String(node.field || '').trim()
  if (field && !result.includes(field))
    result.push(field)
}

function extractVariables(expression) {
  const text = String(expression || '').replace(/'[^']*'|"[^"]*"/g, ' ')
  const variables = []
  const pattern = /[a-z_]\w*/gi
  let match = pattern.exec(text)
  while (match) {
    const token = match[0]
    const previous = text[match.index - 1] || ''
    const nextText = text.slice(match.index + token.length).trimStart()
    if (!reservedToken(token) && previous !== '.' && !nextText.startsWith('(') && !variables.includes(token))
      variables.push(token)
    match = pattern.exec(text)
  }
  return variables
}

function reservedToken(token) {
  return ['true', 'false', 'null', 'nil', 'and', 'or', 'not', 'if', 'else', 'return', 'math', 'string', 'seq', 'date']
    .includes(String(token || '').toLowerCase())
}

function resolveFieldMeta(fieldCode) {
  return (props.fields || []).find((field) => {
    const code = field?.fieldCode || field?.field
    return code === fieldCode
  }) || null
}

function fieldLabel(field, fallback) {
  const name = field?.fieldName || field?.label || fallback
  const code = field?.fieldCode || field?.field || fallback
  return `${name}（${code}）`
}

function resolveInputType(field, fieldCode = '') {
  const fieldType = String(field?.fieldType || '').toUpperCase()
  const componentType = String(field?.componentType || field?.type || '').toLowerCase()
  const dataType = String(field?.dataType || '').toLowerCase()
  const code = String(field?.fieldCode || field?.field || fieldCode).toLowerCase()
  if (fieldType === 'SWITCH' || componentType === 'switch')
    return 'switch'
  if (['NUMBER', 'MONEY'].includes(fieldType)
    || ['number', 'inputnumber', 'input-number'].includes(componentType)
    || ['int', 'integer', 'bigint', 'decimal', 'double', 'float'].includes(dataType)
    || ['qty', 'quantity', 'count', 'num', 'price', 'amount', 'money', 'total', 'rate'].some(key => code.includes(key))) {
    return 'number'
  }
  return 'text'
}

function guessSampleValue(field, index = 0, fallbackCode = '') {
  const code = String(field?.fieldCode || field?.field || fallbackCode).toLowerCase()
  if (['qty', 'quantity', 'count', 'num'].some(key => code.includes(key)))
    return 3
  if (['price', 'amount', 'money', 'fee', 'cost', 'total'].some(key => code.includes(key)))
    return 100
  if (['rate', 'ratio', 'percent'].some(key => code.includes(key)))
    return 0.1
  if (resolveInputType(field, fallbackCode) === 'switch')
    return true
  return index + 1
}

function formatResultValue(value) {
  if (value === null || value === undefined || value === '')
    return '-'
  if (typeof value === 'object')
    return JSON.stringify(value)
  return String(value)
}

function evaluateRuleNode(node, context = {}, rows = [], depth = 0, key = 'rule') {
  if (!node || typeof node !== 'object')
    return false
  if (Array.isArray(node.children)) {
    const operator = String(node.operator || 'AND').toUpperCase()
    const childResults = node.children.map((child, index) => evaluateRuleNode(child, context, rows, depth + 1, `${key}-${index}`))
    const matched = operator === 'OR'
      ? childResults.some(Boolean)
      : childResults.every(Boolean)
    rows.unshift({
      key,
      depth,
      group: true,
      label: operator === 'OR' ? '任一条件满足' : '全部条件满足',
      matched,
    })
    return matched
  }
  const field = String(node.field || '').trim()
  const op = String(node.op || node.operator || 'EQ').toUpperCase()
  const actual = context[field]
  const matched = evaluateRuleCondition(actual, op, node.value)
  rows.push({
    key,
    depth,
    group: false,
    label: `${field || '-'} ${operatorLabel(op)} ${formatResultValue(node.value)}`,
    matched,
  })
  return matched
}

function evaluateRuleCondition(actual, op, expected) {
  if (op === 'IS_NULL')
    return actual === null || actual === undefined || actual === ''
  if (op === 'NOT_NULL')
    return actual !== null && actual !== undefined && actual !== ''
  if (op === 'IN')
    return (Array.isArray(expected) ? expected : []).some(item => compareLoose(actual, item) === 0)
  if (op === 'NOT_IN')
    return !(Array.isArray(expected) ? expected : []).some(item => compareLoose(actual, item) === 0)
  if (op === 'CONTAINS')
    return String(actual ?? '').includes(String(expected ?? ''))
  if (op === 'STARTS_WITH')
    return String(actual ?? '').startsWith(String(expected ?? ''))
  if (op === 'ENDS_WITH')
    return String(actual ?? '').endsWith(String(expected ?? ''))
  const compared = compareLoose(actual, expected)
  if (op === 'EQ')
    return compared === 0
  if (op === 'NE')
    return compared !== 0
  if (op === 'GT')
    return compared > 0
  if (op === 'GTE')
    return compared >= 0
  if (op === 'LT')
    return compared < 0
  if (op === 'LTE')
    return compared <= 0
  return false
}

function compareLoose(left, right) {
  const leftNumber = Number(left)
  const rightNumber = Number(right)
  if (Number.isFinite(leftNumber) && Number.isFinite(rightNumber))
    return leftNumber === rightNumber ? 0 : (leftNumber > rightNumber ? 1 : -1)
  const leftText = String(left ?? '')
  const rightText = String(right ?? '')
  return leftText === rightText ? 0 : (leftText > rightText ? 1 : -1)
}

function operatorLabel(op) {
  const labels = {
    EQ: '=',
    NE: '!=',
    GT: '>',
    GTE: '>=',
    LT: '<',
    LTE: '<=',
    IN: '属于',
    NOT_IN: '不属于',
    CONTAINS: '包含',
    STARTS_WITH: '开头为',
    ENDS_WITH: '结尾为',
    IS_NULL: '为空',
    NOT_NULL: '不为空',
  }
  return labels[op] || op
}
</script>

<style scoped>
.formula-debugger-modal {
  width: min(920px, calc(100vw - 32px));
}

.debugger-panel {
  display: grid;
  gap: 14px;
}

.debugger-summary,
.debugger-result {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: var(--bg-secondary);
  padding: 12px;
}

.debugger-summary span,
.debugger-result span,
.sample-head strong {
  color: var(--text-secondary);
  font-size: 12px;
}

.debugger-summary strong,
.debugger-result strong,
.debugger-result code {
  display: block;
  margin-top: 4px;
  color: var(--text-primary);
  font-size: 14px;
  line-height: 1.5;
}

.debugger-result {
  align-items: center;
  background: #ecfdf5;
  border-color: #a7f3d0;
}

.debugger-result.failed {
  background: #fef2f2;
  border-color: #fecaca;
}

.debugger-result code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
}

.debugger-result em {
  color: var(--text-secondary);
  font-style: normal;
  font-size: 12px;
}

.debugger-samples {
  display: grid;
  gap: 10px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-primary);
  padding: 12px;
}

.debugger-rule {
  display: grid;
  gap: 10px;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: var(--bg-primary);
  padding: 12px;
}

.rule-debug-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.rule-debug-head strong {
  color: var(--text-primary);
  font-size: 13px;
}

.rule-debug-head span {
  color: var(--text-secondary);
  font-size: 12px;
}

.rule-debug-list {
  display: grid;
  gap: 6px;
}

.rule-debug-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-height: 30px;
  border: 1px solid var(--border-light);
  border-radius: 6px;
  background: var(--bg-secondary);
  color: var(--text-primary);
  font-size: 12px;
  padding: 4px 8px;
}

.rule-debug-row.group {
  background: #eef6ff;
  color: #1d4ed8;
  font-weight: 600;
}

.rule-debug-row.matched {
  border-color: #bbf7d0;
  background: #f0fdf4;
  color: #166534;
}

.sample-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.sample-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px 12px;
}

.sample-grid :deep(.n-form-item) {
  margin-bottom: 0;
}

.full-input {
  width: 100%;
}

.debugger-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

@media (max-width: 760px) {
  .debugger-summary,
  .debugger-result {
    flex-direction: column;
  }

  .sample-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
