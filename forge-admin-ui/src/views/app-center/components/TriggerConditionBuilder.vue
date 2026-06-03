<template>
  <div class="trigger-condition-builder">
    <div class="condition-toolbar">
      <n-select
        v-model:value="condition.logic"
        :options="logicOptions"
        size="small"
        style="width: 120px"
        @update:value="emitChange"
      />
      <n-button size="small" dashed @click="addRule">
        添加条件
      </n-button>
    </div>
    <div v-if="condition.rules.length" class="condition-list">
      <div v-for="(rule, index) in condition.rules" :key="rule.clientKey" class="condition-row">
        <n-select
          v-model:value="rule.field"
          :options="fieldOptions"
          clearable
          filterable
          placeholder="字段"
          @update:value="emitChange"
        />
        <n-select
          v-model:value="rule.operator"
          :options="operatorOptions"
          placeholder="条件"
          @update:value="emitChange"
        />
        <n-input
          v-model:value="rule.value"
          placeholder="目标值"
          @update:value="emitChange"
        />
        <n-button quaternary circle size="small" @click="removeRule(index)">
          <template #icon>
            <n-icon><TrashOutline /></n-icon>
          </template>
        </n-button>
      </div>
    </div>
    <n-empty v-else description="暂无条件" />
  </div>
</template>

<script setup>
import { TrashOutline } from '@vicons/ionicons5'
import { reactive, watch } from 'vue'

const props = defineProps({
  modelValue: {
    type: String,
    default: '',
  },
  fieldOptions: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:modelValue'])

const condition = reactive(parseCondition(props.modelValue))

const logicOptions = [
  { label: '满足全部', value: 'AND' },
  { label: '满足任一', value: 'OR' },
]

const operatorOptions = [
  { label: '等于', value: 'eq' },
  { label: '不等于', value: 'ne' },
  { label: '大于', value: 'gt' },
  { label: '大于等于', value: 'gte' },
  { label: '小于', value: 'lt' },
  { label: '小于等于', value: 'lte' },
  { label: '包含', value: 'contains' },
  { label: '发生变化', value: 'changed' },
  { label: '变更为', value: 'changed_to' },
]

watch(() => props.modelValue, (value) => {
  const parsed = parseCondition(value)
  const current = serializeCondition(condition)
  const next = serializeCondition(parsed)
  if (current !== next)
    Object.assign(condition, parsed)
})

function addRule() {
  condition.rules.push(createRule())
  emitChange()
}

function removeRule(index) {
  condition.rules.splice(index, 1)
  emitChange()
}

function emitChange() {
  emit('update:modelValue', serializeCondition(condition))
}

function parseCondition(value) {
  const source = safeParse(value)
  if (Array.isArray(source.rules)) {
    return {
      logic: normalizeLogic(source.logic),
      rules: source.rules.map(normalizeRule),
      extras: extractExtras(source),
    }
  }
  if (source.field || source.op || source.operator) {
    return {
      logic: 'AND',
      rules: [normalizeRule(source)],
      extras: extractExtras(source),
    }
  }
  return {
    logic: 'AND',
    rules: [],
    extras: extractExtras(source),
  }
}

function serializeCondition(value) {
  const extras = value.extras && typeof value.extras === 'object' ? value.extras : {}
  const rules = (value.rules || [])
    .map(rule => ({
      field: rule.field || '',
      operator: rule.operator || 'eq',
      value: rule.value ?? '',
    }))
    .filter(rule => rule.field)
  if (!rules.length)
    return Object.keys(extras).length ? JSON.stringify(extras) : ''
  return JSON.stringify({
    ...extras,
    logic: normalizeLogic(value.logic),
    rules,
  })
}

function normalizeRule(rule = {}) {
  return {
    clientKey: rule.clientKey || `rule_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    field: rule.field || '',
    operator: rule.operator || rule.op || 'eq',
    value: rule.value ?? '',
  }
}

function createRule() {
  return normalizeRule({})
}

function normalizeLogic(value) {
  return String(value || 'AND').toUpperCase() === 'OR' ? 'OR' : 'AND'
}

function extractExtras(source = {}) {
  if (!source || typeof source !== 'object')
    return {}
  const extras = { ...source }
  delete extras.logic
  delete extras.rules
  delete extras.field
  delete extras.op
  delete extras.operator
  delete extras.value
  return extras
}

function safeParse(value) {
  if (!value)
    return {}
  try {
    return JSON.parse(value)
  }
  catch {
    return {}
  }
}
</script>

<style scoped>
.trigger-condition-builder {
  display: grid;
  gap: 10px;
  width: 100%;
}

.condition-toolbar {
  display: flex;
  gap: 8px;
  align-items: center;
}

.condition-list {
  display: grid;
  gap: 8px;
}

.condition-row {
  display: grid;
  grid-template-columns: minmax(140px, 1fr) 120px minmax(140px, 1fr) 32px;
  gap: 8px;
  align-items: center;
}

@media (max-width: 720px) {
  .condition-row {
    grid-template-columns: 1fr;
  }
}
</style>
