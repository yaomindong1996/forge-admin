<template>
  <article class="rule-node" :class="{ group: isGroup, condition: !isGroup }">
    <template v-if="isGroup">
      <header class="rule-node-head">
        <n-select
          :value="nodeModel.operator || 'AND'"
          :options="groupOptions"
          :disabled="disabled"
          size="small"
          class="group-operator"
          @update:value="value => updateNode({ operator: value })"
        />
        <div class="rule-node-actions">
          <n-button size="tiny" secondary :disabled="disabled" @click="addCondition">
            条件
          </n-button>
          <n-button size="tiny" secondary :disabled="disabled" @click="addGroup">
            分组
          </n-button>
          <n-button
            v-if="!root"
            size="tiny"
            text
            type="error"
            :disabled="disabled"
            @click="emit('remove')"
          >
            删除
          </n-button>
        </div>
      </header>

      <div class="rule-children">
        <FormulaConditionRuleNode
          v-for="(child, index) in childNodes"
          :key="child.key || index"
          :node="child"
          :fields="fields"
          :disabled="disabled"
          :depth="depth + 1"
          @update="value => updateChild(index, value)"
          @remove="removeChild(index)"
        />
      </div>
    </template>

    <template v-else>
      <div class="condition-row">
        <n-select
          :value="nodeModel.field"
          :options="fieldOptions"
          :disabled="disabled"
          size="small"
          filterable
          clearable
          placeholder="字段"
          @update:value="value => updateNode({ field: value })"
        />
        <n-select
          :value="nodeModel.op || 'EQ'"
          :options="operatorOptions"
          :disabled="disabled"
          size="small"
          class="operator-select"
          @update:value="handleOperatorChange"
        />
        <n-dynamic-tags
          v-if="isSetOperator"
          :value="arrayValue"
          :disabled="disabled"
          size="small"
          class="value-tags"
          @update:value="value => updateNode({ value })"
        />
        <n-input-number
          v-else-if="showNumberInput"
          :value="numberValue"
          :disabled="disabled"
          :show-button="false"
          size="small"
          class="value-input"
          placeholder="数值"
          @update:value="value => updateNode({ value })"
        />
        <n-switch
          v-else-if="showBooleanInput"
          :value="Boolean(nodeModel.value)"
          :disabled="disabled"
          size="small"
          @update:value="value => updateNode({ value })"
        />
        <n-input
          v-else-if="showValueInput"
          :value="stringValue"
          :disabled="disabled"
          size="small"
          class="value-input"
          placeholder="值"
          @update:value="value => updateNode({ value })"
        />
        <div v-else class="null-value">
          无需值
        </div>
        <n-button
          size="tiny"
          text
          type="error"
          :disabled="disabled"
          @click="emit('remove')"
        >
          删除
        </n-button>
      </div>
    </template>
  </article>
</template>

<script setup>
import { computed } from 'vue'

defineOptions({
  name: 'FormulaConditionRuleNode',
})

const props = defineProps({
  node: {
    type: Object,
    required: true,
  },
  fields: {
    type: Array,
    default: () => [],
  },
  disabled: {
    type: Boolean,
    default: false,
  },
  root: {
    type: Boolean,
    default: false,
  },
  depth: {
    type: Number,
    default: 0,
  },
})

const emit = defineEmits(['update', 'remove'])

const groupOptions = [
  { label: '全部满足 AND', value: 'AND' },
  { label: '任一满足 OR', value: 'OR' },
]
const operatorOptions = [
  { label: '等于', value: 'EQ' },
  { label: '不等于', value: 'NE' },
  { label: '大于', value: 'GT' },
  { label: '大于等于', value: 'GTE' },
  { label: '小于', value: 'LT' },
  { label: '小于等于', value: 'LTE' },
  { label: '属于', value: 'IN' },
  { label: '不属于', value: 'NOT_IN' },
  { label: '包含', value: 'CONTAINS' },
  { label: '开头为', value: 'STARTS_WITH' },
  { label: '结尾为', value: 'ENDS_WITH' },
  { label: '为空', value: 'IS_NULL' },
  { label: '不为空', value: 'NOT_NULL' },
]
const nullOperators = new Set(['IS_NULL', 'NOT_NULL'])
const setOperators = new Set(['IN', 'NOT_IN'])

const nodeModel = computed(() => props.node || {})
const childNodes = computed(() => Array.isArray(nodeModel.value.children) ? nodeModel.value.children : [])
const isGroup = computed(() => childNodes.value.length > 0 || ['AND', 'OR'].includes(String(nodeModel.value.operator || '').toUpperCase()))
const currentOperator = computed(() => String(nodeModel.value.op || 'EQ').toUpperCase())
const isSetOperator = computed(() => setOperators.has(currentOperator.value))
const showValueInput = computed(() => !nullOperators.has(currentOperator.value) && !isSetOperator.value)
const selectedField = computed(() => {
  return (props.fields || []).find((item) => {
    const code = item?.fieldCode || item?.field
    return code === nodeModel.value.field
  }) || null
})
const showNumberInput = computed(() => showValueInput.value && resolveInputType(selectedField.value) === 'number')
const showBooleanInput = computed(() => showValueInput.value && resolveInputType(selectedField.value) === 'boolean')
const fieldOptions = computed(() => {
  return (props.fields || [])
    .filter(item => item && item.fieldStatus !== 'HIDDEN')
    .map(item => ({
      label: `${item.fieldName || item.label || item.fieldCode || item.field}（${item.fieldCode || item.field}）`,
      value: item.fieldCode || item.field,
    }))
})
const arrayValue = computed(() => Array.isArray(nodeModel.value.value) ? nodeModel.value.value : [])
const numberValue = computed(() => {
  const value = nodeModel.value.value
  return typeof value === 'number' ? value : null
})
const stringValue = computed(() => {
  const value = nodeModel.value.value
  return value === null || value === undefined || Array.isArray(value) ? '' : String(value)
})

function updateNode(patch) {
  emit('update', {
    ...nodeModel.value,
    ...patch,
  })
}

function addCondition() {
  updateNode({
    operator: nodeModel.value.operator || 'AND',
    children: [
      ...childNodes.value,
      createCondition(),
    ],
  })
}

function addGroup() {
  updateNode({
    operator: nodeModel.value.operator || 'AND',
    children: [
      ...childNodes.value,
      createGroup(),
    ],
  })
}

function updateChild(index, value) {
  const children = [...childNodes.value]
  children.splice(index, 1, value)
  updateNode({ children })
}

function removeChild(index) {
  const children = [...childNodes.value]
  children.splice(index, 1)
  updateNode({ children })
}

function handleOperatorChange(op) {
  if (nullOperators.has(op)) {
    updateNode({ op, value: null })
    return
  }
  if (setOperators.has(op)) {
    updateNode({ op, value: arrayValue.value.length ? arrayValue.value : [] })
    return
  }
  updateNode({ op, value: nodeModel.value.value ?? '' })
}

function createCondition() {
  return {
    field: fieldOptions.value[0]?.value || '',
    op: 'EQ',
    value: '',
  }
}

function createGroup() {
  return {
    operator: 'AND',
    children: [createCondition()],
  }
}

function resolveInputType(field) {
  const fieldType = String(field?.fieldType || '').toUpperCase()
  const componentType = String(field?.componentType || field?.type || '').toLowerCase()
  const dataType = String(field?.dataType || '').toLowerCase()
  if (fieldType === 'SWITCH' || componentType === 'switch' || dataType.includes('bool'))
    return 'boolean'
  if (['NUMBER', 'MONEY'].includes(fieldType)
    || ['number', 'inputnumber', 'input-number'].includes(componentType)
    || ['int', 'integer', 'bigint', 'decimal', 'double', 'float'].includes(dataType)) {
    return 'number'
  }
  return 'text'
}
</script>

<style scoped>
.rule-node {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.rule-node.group {
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: var(--bg-secondary);
  padding: 10px;
}

.rule-node.condition {
  border: 1px solid var(--border-light);
  border-radius: 7px;
  background: var(--bg-primary);
  padding: 8px;
}

.rule-node-head,
.rule-node-actions,
.condition-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.rule-node-head {
  justify-content: space-between;
}

.rule-node-actions {
  flex: 0 0 auto;
}

.group-operator {
  width: 148px;
}

.rule-children {
  display: grid;
  gap: 8px;
  padding-left: 12px;
  border-left: 2px solid #d9e2ef;
}

.condition-row {
  display: grid;
  grid-template-columns: minmax(120px, 1.2fr) minmax(96px, 0.7fr) minmax(110px, 1fr) auto;
}

.operator-select {
  min-width: 96px;
}

.value-input,
.value-tags {
  width: 100%;
  min-width: 0;
}

.null-value {
  min-height: 28px;
  display: flex;
  align-items: center;
  border: 1px dashed var(--border-light);
  border-radius: 6px;
  color: var(--text-secondary);
  font-size: 12px;
  padding: 0 10px;
}

@media (max-width: 760px) {
  .condition-row {
    grid-template-columns: minmax(0, 1fr);
  }

  .rule-node-head {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
