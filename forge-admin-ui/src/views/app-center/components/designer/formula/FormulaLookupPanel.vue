<template>
  <section class="lookup-panel">
    <n-grid :cols="2" :x-gap="10">
      <n-form-item-gi label="对象关系">
        <n-select
          v-model:value="formModel.formulaLookupRelationCode"
          :options="relationOptions"
          :disabled="disabled"
          filterable
          clearable
          placeholder="选择已配置关系"
          @update:value="handleRelationChange"
        />
      </n-form-item-gi>
      <n-form-item-gi label="目标对象">
        <n-input
          v-model:value="formModel.formulaLookupTargetObjectCode"
          :disabled="true"
          placeholder="选择关系后自动带出"
        />
      </n-form-item-gi>
    </n-grid>

    <n-grid :cols="2" :x-gap="10">
      <n-form-item-gi label="当前对象字段">
        <n-select
          v-model:value="formModel.formulaLookupSourceField"
          :options="sourceFieldOptions"
          :disabled="disabled"
          filterable
          clearable
          placeholder="用于匹配目标对象"
        />
      </n-form-item-gi>
      <n-form-item-gi label="目标匹配字段">
        <n-select
          v-model:value="formModel.formulaLookupTargetField"
          :options="targetFieldOptions"
          :disabled="disabled"
          filterable
          clearable
          tag
          placeholder="例如 id"
        />
      </n-form-item-gi>
    </n-grid>

    <n-grid :cols="2" :x-gap="10">
      <n-form-item-gi label="返回字段">
        <n-select
          v-model:value="formModel.formulaLookupReturnField"
          :options="targetFieldOptions"
          :disabled="disabled"
          filterable
          clearable
          tag
          placeholder="例如 name / level"
        />
      </n-form-item-gi>
      <n-form-item-gi label="未命中值">
        <n-input
          v-model:value="formModel.formulaLookupNotFoundValue"
          :disabled="disabled"
          placeholder="可为空"
        />
      </n-form-item-gi>
    </n-grid>
  </section>
</template>

<script setup>
import { computed, watch } from 'vue'

const props = defineProps({
  form: {
    type: Object,
    required: true,
  },
  relations: {
    type: Array,
    default: () => [],
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

const formModel = computed(() => props.form)
const activeRelations = computed(() => {
  return (props.relations || [])
    .filter(item => item && item.status !== 0 && relationCode(item))
})

const relationOptions = computed(() => {
  return activeRelations.value.map(relation => ({
    label: `${relation.relationName || relation.targetObjectName || relation.targetObjectCode}（${relation.targetObjectCode || '目标对象'}）`,
    value: relationCode(relation),
  }))
})

const selectedRelation = computed(() => {
  return activeRelations.value.find(item => relationCode(item) === formModel.value.formulaLookupRelationCode) || null
})

const sourceFieldOptions = computed(() => {
  return (props.allFields || [])
    .filter(item => item && item.fieldStatus !== 'HIDDEN')
    .map(item => ({
      label: `${item.fieldName || item.label || item.fieldCode || item.field}（${item.fieldCode || item.field}）`,
      value: item.fieldCode || item.field,
    }))
})

const targetFieldOptions = computed(() => {
  const fields = new Set()
  const relation = selectedRelation.value
  append(fields, relation?.targetFieldCode)
  append(fields, relation?.targetField)
  append(fields, relationConfigValue(relation, 'displayField'))
  append(fields, relationConfigValue(relation, 'returnField'))
  append(fields, formModel.value.formulaLookupTargetField)
  append(fields, formModel.value.formulaLookupReturnField)
  return [...fields].map(value => ({ label: value, value }))
})

watch(selectedRelation, (relation) => {
  if (!relation)
    return
  applyRelationDefaults(relation)
}, { immediate: true })

function handleRelationChange() {
  const relation = selectedRelation.value
  if (relation)
    applyRelationDefaults(relation, { force: true })
}

function applyRelationDefaults(relation, { force = false } = {}) {
  formModel.value.formulaLookupTargetObjectCode = relation.targetObjectCode || ''
  if (force || !formModel.value.formulaLookupSourceField) {
    formModel.value.formulaLookupSourceField = relation.sourceFieldCode || ''
  }
  if (force || !formModel.value.formulaLookupTargetField) {
    formModel.value.formulaLookupTargetField = relation.targetFieldCode || 'id'
  }
  if (force || !formModel.value.formulaLookupReturnField) {
    formModel.value.formulaLookupReturnField = relationConfigValue(relation, 'displayField')
      || relation.targetFieldCode
      || 'id'
  }
}

function relationCode(relation = {}) {
  return relation.relationName || relation.targetObjectCode || (relation.id ? String(relation.id) : '')
}

function relationConfigValue(relation, key) {
  if (!relation?.relationConfig)
    return ''
  try {
    const config = typeof relation.relationConfig === 'string'
      ? JSON.parse(relation.relationConfig)
      : relation.relationConfig
    return config?.[key] || ''
  }
  catch {
    return ''
  }
}

function append(target, value) {
  const text = String(value || '').trim()
  if (text)
    target.add(text)
}
</script>

<style scoped>
.lookup-panel {
  display: grid;
  gap: 2px;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: var(--bg-primary);
  padding: 10px 12px;
}
</style>
