<template>
  <aside class="property-panel">
    <div class="panel-head">
      <div>
        <div class="panel-title">
          字段属性
        </div>
        <div class="panel-subtitle">
          {{ field?.field || '未选择字段' }}
        </div>
      </div>
    </div>

    <n-empty v-if="!field" description="请选择中间字段行" />
    <n-form v-else label-placement="top" size="small" class="field-form" :show-feedback="false">
      <n-alert v-if="readonlyField" type="info" :bordered="false" class="readonly-alert">
        系统字段由平台维护，仅用于展示真实表结构，不能手动修改。
      </n-alert>
      <section class="form-section">
        <div class="section-title">
          数据库映射
        </div>
        <n-form-item label="数据库列名">
          <n-input
            :value="field.columnName"
            :disabled="readonlyField"
            placeholder="lower_snake"
            @update:value="updateFieldProp('columnName', $event)"
          />
        </n-form-item>
        <div class="switch-grid">
          <div class="switch-item">
            <span>主键字段</span>
            <n-switch
              :value="field.primaryKey || false"
              :disabled="readonlyField"
              size="small"
              @update:value="updateFieldProp('primaryKey', $event)"
            />
          </div>
          <div class="switch-item">
            <span>系统字段</span>
            <n-switch
              :value="field.systemField || false"
              :disabled="readonlyField"
              size="small"
              @update:value="updateFieldProp('systemField', $event)"
            />
          </div>
        </div>
      </section>

      <section class="form-section">
        <div class="section-title">
          字典与安全
        </div>
        <div v-if="dictSuggestion || securitySuggestion" class="recommend-box">
          <div v-if="dictSuggestion" class="recommend-line">
            <span>推荐字典：{{ dictSuggestion.dictType }}</span>
            <n-button text size="tiny" type="primary" @click="applyDictSuggestion">
              应用
            </n-button>
          </div>
          <div v-if="securitySuggestion" class="recommend-line">
            <span>推荐脱敏：{{ securitySuggestion.sensitiveType || 'NONE' }}</span>
            <n-button text size="tiny" type="primary" @click="applySecuritySuggestion">
              应用
            </n-button>
          </div>
        </div>
        <n-form-item label="字典类型">
          <DictTypeSelect
            :value="field.dictType"
            :fields="fields"
            :disabled="readonlyField"
            @update:value="updateFieldProp('dictType', $event)"
          />
        </n-form-item>
        <n-grid :cols="2" :x-gap="10">
          <n-form-item-gi label="敏感类型">
            <n-select
              :value="field.sensitiveType"
              :options="sensitiveTypeOptions"
              :disabled="readonlyField"
              size="small"
              @update:value="updateFieldProp('sensitiveType', $event)"
            />
          </n-form-item-gi>
          <n-form-item-gi label="加密算法">
            <n-select
              :value="field.encryptAlgorithm"
              clearable
              :disabled="readonlyField"
              size="small"
              :options="encryptOptions"
              @update:value="updateFieldProp('encryptAlgorithm', $event || '')"
            />
          </n-form-item-gi>
        </n-grid>
      </section>
    </n-form>
  </aside>
</template>

<script setup>
import { computed } from 'vue'
import DictTypeSelect from '../shared/DictTypeSelect.vue'
import {
  isLockedSystemField,
  sensitiveTypeOptions,
} from './model-schema'

const props = defineProps({
  field: {
    type: Object,
    default: null,
  },
  fields: {
    type: Array,
    default: () => [],
  },
  domain: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['update:field'])

const encryptOptions = [
  { label: '不加密', value: '' },
  { label: 'SM4', value: 'SM4' },
  { label: 'AES', value: 'AES' },
]
const domainSchema = computed(() => props.domain?.domainSchema || {})
const dictSuggestion = computed(() => findRecommendation(domainSchema.value.dictRecommendations || []))
const securitySuggestion = computed(() => findRecommendation(domainSchema.value.securityPolicies || []))
const readonlyField = computed(() => isLockedSystemField(props.field))

function updateFieldProp(key, value) {
  if (readonlyField.value)
    return
  if (key === 'dictType' && value) {
    patchField({ dictType: value, componentType: 'select' })
    return
  }
  patchField({ [key]: value })
}

function applyDictSuggestion() {
  if (!dictSuggestion.value || readonlyField.value)
    return
  patchField({ dictType: dictSuggestion.value.dictType, componentType: 'select' })
}

function applySecuritySuggestion() {
  if (!securitySuggestion.value || readonlyField.value)
    return
  patchField({
    sensitiveType: securitySuggestion.value.sensitiveType || 'NONE',
    encryptAlgorithm: securitySuggestion.value.encryptAlgorithm || '',
  })
}

function patchField(patch) {
  if (!props.field || readonlyField.value)
    return
  emit('update:field', {
    ...props.field,
    ...patch,
  })
}

function findRecommendation(items) {
  if (!props.field)
    return null
  const text = `${props.field.field || ''} ${props.field.columnName || ''} ${props.field.label || ''}`.toLowerCase()
  return items.find((item) => {
    const pattern = String(item.fieldPattern || '').toLowerCase()
    return pattern && text.includes(pattern)
  }) || null
}
</script>

<style scoped>
.property-panel {
  height: 100%;
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: var(--bg-primary);
  overflow: hidden;
}

.panel-head {
  display: flex;
  min-height: 50px;
  align-items: center;
  justify-content: space-between;
  padding: 0 14px;
  border-bottom: 1px solid #d8dee8;
  background: var(--bg-secondary);
}

.panel-title {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 700;
}

.panel-subtitle {
  margin-top: 2px;
  color: var(--text-secondary);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
}

.field-form {
  display: grid;
  max-height: calc(100% - 50px);
  gap: 12px;
  overflow: auto;
  padding: 12px;
}

.readonly-alert {
  grid-column: 1 / -1;
}

.form-section {
  display: grid;
  gap: 10px;
  border-bottom: 1px solid #eef2f7;
  padding-bottom: 12px;
}

.form-section:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.section-title {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 700;
}

.switch-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.switch-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 34px;
  padding: 0 10px;
  border: 1px solid var(--border-light);
  border-radius: 6px;
  color: var(--text-secondary);
  font-size: 12px;
}

.recommend-box {
  display: grid;
  gap: 6px;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  background: #eff6ff;
  padding: 8px;
}

.recommend-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: #1e40af;
  font-size: 12px;
}
</style>
