<template>
  <aside class="property-panel">
    <template v-if="field">
      <div class="property-head">
        <div>
          <h3>{{ form.fieldName || '字段属性' }}</h3>
          <p>{{ developerMode ? (form.fieldCode || '保存后自动生成字段编码和列名') : '维护表单展示、校验和视图可见性' }}</p>
        </div>
        <n-tag v-if="field.systemField" size="small" :bordered="false">
          系统字段
        </n-tag>
      </div>

      <div class="property-body">
        <n-tabs type="line" animated class="property-tabs">
          <n-tab-pane name="basic" tab="基础属性">
            <n-form label-placement="top" size="small" :show-feedback="false">
              <n-grid :cols="2" :x-gap="12">
                <n-form-item-gi label="字段名称">
                  <n-input v-model:value="form.fieldName" :disabled="field.systemField" placeholder="例如：客户等级" />
                </n-form-item-gi>
                <n-form-item-gi label="字段类型">
                  <n-select
                    v-model:value="form.fieldType"
                    :options="fieldTypeOptions"
                    :disabled="field.systemField"
                    filterable
                  />
                </n-form-item-gi>
              </n-grid>

              <n-grid :cols="developerMode && showStorageOptions ? 3 : 1" :x-gap="12">
                <n-form-item-gi v-if="developerMode" label="字段英文名">
                  <n-input
                    v-model:value="form.fieldCode"
                    :disabled="field.systemField"
                    placeholder="例如：customerLevel"
                  />
                </n-form-item-gi>
                <n-form-item-gi v-if="developerMode && supportsLength" label="字段长度">
                  <n-input-number
                    v-model:value="form.length"
                    :min="1"
                    :max="lengthMax"
                    :show-button="false"
                    :disabled="field.systemField"
                    class="full-input"
                  />
                </n-form-item-gi>
                <n-form-item-gi v-if="developerMode && supportsPrecision" label="小数位">
                  <n-input-number
                    v-model:value="form.precision"
                    :min="0"
                    :max="8"
                    :show-button="false"
                    :disabled="field.systemField"
                    class="full-input"
                  />
                </n-form-item-gi>
              </n-grid>

              <n-form-item label="提示文案">
                <n-input v-model:value="form.placeholder" :disabled="field.systemField" placeholder="请输入提示文案" />
              </n-form-item>

              <n-form-item v-if="form.fieldType === 'REGION'" label="默认地区">
                <RegionTreeSelect v-model="form.defaultValue" size="small" :disabled="field.systemField" />
              </n-form-item>
              <n-form-item v-else label="默认值">
                <n-input v-model:value="form.defaultValue" :disabled="field.systemField" placeholder="可为空" />
              </n-form-item>

              <n-form-item v-if="needsDict" label="系统字典" class="dict-property-item">
                <DictTypeSelect
                  v-model:value="form.dictType"
                  compact
                  :fields="allFields"
                  :disabled="field.systemField"
                />
              </n-form-item>

              <n-form-item label="备注">
                <n-input v-model:value="form.remark" type="textarea" :rows="4" placeholder="字段说明，业务用户可见" />
              </n-form-item>
            </n-form>
          </n-tab-pane>

          <n-tab-pane name="display" tab="显示与校验">
            <n-form label-placement="top" size="small" :show-feedback="false">
              <section v-if="needsDict" class="cascade-config">
                <div class="cascade-config-head">
                  <div>
                    <strong>级联过滤</strong>
                    <span>根据上级字段值过滤当前字典选项。</span>
                  </div>
                  <n-switch
                    :value="form.basicProps.cascade.enabled"
                    :disabled="field.systemField"
                    size="small"
                    @update:value="updateCascadeEnabled"
                  />
                </div>
                <div v-if="form.basicProps.cascade.enabled" class="cascade-grid">
                  <n-form-item label="上级字段">
                    <n-select
                      v-model:value="form.basicProps.cascade.sourceField"
                      :options="cascadeSourceFieldOptions"
                      :disabled="field.systemField"
                      filterable
                      clearable
                      placeholder="选择上级字典或关联字段"
                    />
                  </n-form-item>
                  <n-form-item label="匹配方式">
                    <n-select
                      v-model:value="form.basicProps.cascade.mode"
                      :options="cascadeModeOptions"
                      :disabled="field.systemField"
                    />
                  </n-form-item>
                  <n-form-item v-if="form.basicProps.cascade.mode === 'remoteParam'" label="请求参数名">
                    <n-input
                      v-model:value="form.basicProps.cascade.paramName"
                      :disabled="field.systemField"
                      placeholder="例如：orgId / parentId"
                    />
                  </n-form-item>
                </div>
              </section>

              <div class="switch-grid">
                <label>
                  <span>必填</span>
                  <n-switch v-model:value="form.required" :disabled="field.systemField" size="small" />
                </label>
                <label>
                  <span>唯一校验</span>
                  <n-switch v-model:value="form.advancedProps.unique" :disabled="field.systemField" size="small" />
                </label>
                <label>
                  <span>显示在表单</span>
                  <n-switch v-model:value="form.formVisible" :disabled="field.readonly" size="small" />
                </label>
                <label>
                  <span>显示在列表</span>
                  <n-switch v-model:value="form.listVisible" size="small" />
                </label>
                <label>
                  <span>作为查询条件</span>
                  <n-switch v-model:value="form.searchable" size="small" />
                </label>
                <label>
                  <span>允许导入</span>
                  <n-switch v-model:value="form.importable" :disabled="field.systemField" size="small" />
                </label>
                <label>
                  <span>允许导出</span>
                  <n-switch v-model:value="form.exportable" size="small" />
                </label>
              </div>
            </n-form>
          </n-tab-pane>

          <n-tab-pane v-if="developerMode" name="advanced" tab="开发者属性">
            <n-form label-placement="top" size="small" :show-feedback="false">
              <n-grid :cols="2" :x-gap="12">
                <n-form-item-gi label="字段英文名">
                  <n-input v-model:value="form.fieldCode" :disabled="field.systemField" placeholder="自动生成" />
                </n-form-item-gi>
                <n-form-item-gi label="数据库列名">
                  <n-input v-model:value="form.columnName" :disabled="field.systemField" placeholder="自动生成" />
                </n-form-item-gi>
                <n-form-item-gi label="数据类型">
                  <FieldTypeSelect v-model:value="form.dataType" :disabled="field.systemField" />
                </n-form-item-gi>
                <n-form-item-gi label="控件类型">
                  <n-select v-model:value="form.componentType" :options="componentOptions" filterable clearable />
                </n-form-item-gi>
                <n-form-item-gi label="查询方式">
                  <n-select v-model:value="form.queryType" :options="queryTypeOptions" clearable />
                </n-form-item-gi>
                <n-form-item-gi label="字段状态">
                  <n-select v-model:value="form.fieldStatus" :options="statusOptions" />
                </n-form-item-gi>
              </n-grid>

              <n-grid :cols="2" :x-gap="12">
                <n-form-item-gi label="脱敏类型">
                  <n-select v-model:value="form.sensitiveType" :options="sensitiveOptions" clearable />
                </n-form-item-gi>
                <n-form-item-gi label="加密算法">
                  <n-select v-model:value="form.encryptAlgorithm" :options="encryptOptions" clearable />
                </n-form-item-gi>
              </n-grid>
            </n-form>
          </n-tab-pane>

          <n-tab-pane name="formula" tab="公式与调试" class="formula-tab-pane">
            <n-form label-placement="top" size="small" :show-feedback="false" class="formula-tab-form">
              <FormulaConfigPanel
                :form="form"
                :field="field"
                :all-fields="allFields"
                :relations="relations"
                :saving="saving"
                :formula-validating="formulaValidating"
                :formula-previewing="formulaPreviewing"
                :formula-validate-result="formulaValidateResult"
                :formula-feedback-lines="formulaFeedbackLines"
                :can-open-formula-debugger="canOpenFormulaDebugger"
                :has-formula-tool-fields="hasFormulaToolFields"
                :preview-disabled="!canOpenFormulaPreview"
                @toggle-enabled="updateFormulaEnabled"
                @type-change="onFormulaTypeChange"
                @insert-token="insertFormulaToken"
                @insert-string-token="insertStringToken"
                @condition-expression-change="onConditionExpressionChange"
                @condition-mode-change="onConditionModeChange"
                @condition-rule-compiled="onConditionRuleCompiled"
                @condition-rule-validation="onConditionRuleValidation"
                @trigger-mode="setFormulaTriggerMode"
                @save="$emit('save', payload)"
                @validate="handleValidateFormula"
                @preview="openFormulaPreview"
                @open-debugger="formulaDebuggerVisible = true"
                @open-log="formulaLogVisible = true"
                @open-graph="formulaGraphVisible = true"
              />
            </n-form>
          </n-tab-pane>
        </n-tabs>
      </div>

      <div class="property-footer">
        <n-button secondary :disabled="!changed" @click="resetForm">
          还原
        </n-button>
        <n-button type="primary" :loading="saving" :disabled="saving" @click="$emit('save', payload)">
          保存字段
        </n-button>
      </div>
    </template>

    <n-empty v-else description="选择左侧字段后编辑属性" />

    <n-modal
      v-model:show="formulaPreviewDialogVisible"
      preset="card"
      class="formula-preview-modal"
      :title="`预览计算：${form.fieldName || form.fieldCode || '目标字段'}`"
      :bordered="false"
      :mask-closable="!formulaPreviewing"
    >
      <div class="formula-preview-dialog">
        <div class="formula-preview-summary">
          <div>
            <span>目标字段</span>
            <strong>{{ previewTargetLabel }}</strong>
          </div>
          <n-tag size="small" :bordered="false" type="info">
            {{ form.formulaTriggerMode === 'REALTIME' ? '实时计算' : '保存时计算' }}
          </n-tag>
        </div>

        <div class="formula-preview-expression">
          {{ form.formulaExpression || '未配置表达式' }}
        </div>

        <n-empty
          v-if="!previewVariableFields.length"
          size="small"
          description="当前公式没有识别到变量，将直接计算表达式。"
        />
        <n-form v-else label-placement="top" size="small" :show-feedback="false" class="formula-preview-form">
          <n-form-item
            v-for="item in previewVariableFields"
            :key="item.field"
            :label="item.label"
          >
            <n-input-number
              v-if="item.inputType === 'number'"
              v-model:value="formulaPreviewForm[item.field]"
              :show-button="false"
              class="full-input"
              :placeholder="`请输入${item.label}`"
            />
            <n-switch
              v-else-if="item.inputType === 'switch'"
              v-model:value="formulaPreviewForm[item.field]"
            />
            <n-input
              v-else
              v-model:value="formulaPreviewForm[item.field]"
              :placeholder="`请输入${item.label}`"
            />
          </n-form-item>
        </n-form>

        <div v-if="formulaPreviewResult" class="formula-preview-result" :class="formulaPreviewResult.success ? 'success' : 'error'">
          <template v-if="formulaPreviewResult.success">
            <span>计算结果</span>
            <strong>{{ formatPreviewValue(formulaPreviewResult.result) }}</strong>
            <em v-if="formulaPreviewResult.elapsedMs !== null && formulaPreviewResult.elapsedMs !== undefined">
              {{ formulaPreviewResult.elapsedMs }}ms
            </em>
          </template>
          <template v-else>
            <span>计算失败</span>
            <strong>{{ formulaPreviewResult.errorMessage || '请检查变量值和表达式' }}</strong>
          </template>
        </div>
      </div>

      <template #footer>
        <div class="formula-preview-footer">
          <n-button secondary :disabled="formulaPreviewing" @click="fillPreviewSampleValues">
            填入样例值
          </n-button>
          <div>
            <n-button :disabled="formulaPreviewing" @click="formulaPreviewDialogVisible = false">
              关闭
            </n-button>
            <n-button
              type="primary"
              :loading="formulaPreviewing"
              :disabled="!canPreviewCalculate"
              @click="handlePreviewFormula"
            >
              计算
            </n-button>
          </div>
        </div>
      </template>
    </n-modal>

    <FormulaDebuggerPanel
      v-model:show="formulaDebuggerVisible"
      :field="selectedFormulaField"
      :fields="formulaToolFields"
      :object-code="objectCode"
    />
    <FormulaExecutionLogDrawer
      v-model:show="formulaLogVisible"
      :object-code="objectCode"
      :field-code="form.fieldCode"
    />
    <FormulaDependencyGraph
      v-model:show="formulaGraphVisible"
      :fields="formulaToolFields"
      :object-code="objectCode"
      :current-field-code="form.fieldCode"
    />
  </aside>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { previewFormula, validateFormula } from '@/api/formula'
import DictTypeSelect from '@/components/lowcode-builder/shared/DictTypeSelect.vue'
import FieldTypeSelect from '@/components/lowcode-builder/shared/FieldTypeSelect.vue'
import RegionTreeSelect from '@/components/RegionTreeSelect.vue'
import { camelToSnake } from './form-first/namingUtils'
import FormulaConfigPanel from './formula/FormulaConfigPanel.vue'
import FormulaDebuggerPanel from './formula/FormulaDebuggerPanel.vue'
import FormulaDependencyGraph from './formula/FormulaDependencyGraph.vue'
import FormulaExecutionLogDrawer from './formula/FormulaExecutionLogDrawer.vue'

const props = defineProps({
  field: {
    type: Object,
    default: null,
  },
  allFields: {
    type: Array,
    default: () => [],
  },
  relations: {
    type: Array,
    default: () => [],
  },
  objectCode: {
    type: String,
    default: '',
  },
  developerMode: {
    type: Boolean,
    default: false,
  },
  saving: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['save', 'dirtyChange'])

const form = reactive(createFieldForm())
let baseline = ''
let resetting = false

const fieldTypeOptions = [
  { label: '文本', value: 'TEXT' },
  { label: '多行文本', value: 'MULTILINE' },
  { label: '数字', value: 'NUMBER' },
  { label: '金额', value: 'MONEY' },
  { label: '日期', value: 'DATE' },
  { label: '日期时间', value: 'DATETIME' },
  { label: '下拉', value: 'DICT' },
  { label: '单选', value: 'RADIO' },
  { label: '多选', value: 'CHECKBOX' },
  { label: '开关', value: 'SWITCH' },
  { label: '附件', value: 'FILE' },
  { label: '图片', value: 'IMAGE' },
  { label: '人员', value: 'USER' },
  { label: '部门', value: 'DEPT' },
  { label: '地区', value: 'REGION' },
  { label: '引用对象', value: 'REFERENCE' },
]

const componentOptions = [
  { label: '输入框', value: 'input' },
  { label: '多行文本', value: 'textarea' },
  { label: '数字输入', value: 'number' },
  { label: '下拉选择', value: 'select' },
  { label: '单选框', value: 'radio' },
  { label: '多选框', value: 'checkbox' },
  { label: '字典选择', value: 'dictSelect' },
  { label: '日期', value: 'date' },
  { label: '日期时间', value: 'datetime' },
  { label: '开关', value: 'switch' },
  { label: '文件上传', value: 'fileUpload' },
  { label: '图片上传', value: 'imageUpload' },
  { label: '人员选择', value: 'userSelect' },
  { label: '部门树', value: 'orgTreeSelect' },
  { label: '地区树', value: 'regionTreeSelect' },
  { label: '引用对象', value: 'objectReference' },
]

const queryTypeOptions = [
  { label: '包含', value: 'like' },
  { label: '等于', value: 'eq' },
  { label: '大于等于', value: 'ge' },
  { label: '小于等于', value: 'le' },
  { label: '区间', value: 'between' },
  { label: '多值', value: 'in' },
]

const statusOptions = [
  { label: '启用', value: 'ENABLED' },
  { label: '停用', value: 'DISABLED' },
  { label: '隐藏', value: 'HIDDEN' },
]

const sensitiveOptions = [
  { label: '手机号', value: 'PHONE' },
  { label: '身份证', value: 'ID_CARD' },
  { label: '银行卡', value: 'BANK_CARD' },
  { label: '邮箱', value: 'EMAIL' },
]

// 公式校验/预览状态
const formulaValidating = ref(false)
const formulaPreviewing = ref(false)
const formulaValidateResult = ref(null)
const formulaPreviewResult = ref(null)
const formulaPreviewDialogVisible = ref(false)
const formulaDebuggerVisible = ref(false)
const formulaLogVisible = ref(false)
const formulaGraphVisible = ref(false)
const formulaConditionRuleValidation = ref(null)
const formulaPreviewForm = reactive({})
let formulaPreviewTimer = null
let formulaPreviewInitializing = false
const encryptOptions = [
  { label: 'AES', value: 'AES' },
  { label: 'SM4', value: 'SM4' },
]
const cascadeModeOptions = [
  { label: '字典父子(parent_dict_code)', value: 'parentDictCode' },
  { label: '关联字典(linked_dict_type/value)', value: 'linkedDict' },
  { label: '远程参数过滤', value: 'remoteParam' },
]

const payload = computed(() => normalizePayload(form))
const selectedFormulaField = computed(() => ({
  ...(props.field || {}),
  ...payload.value,
}))
const formulaToolFields = computed(() => {
  const current = selectedFormulaField.value
  const currentCode = current.fieldCode || current.field
  if (!currentCode)
    return props.allFields || []
  let matched = false
  const merged = (props.allFields || []).map((item) => {
    const code = item?.fieldCode || item?.field
    if (code !== currentCode)
      return item
    matched = true
    return {
      ...item,
      ...current,
    }
  })
  return matched ? merged : [current, ...merged]
})
const canOpenFormulaDebugger = computed(() => Boolean(selectedFormulaField.value.formulaConfig?.type))
const hasFormulaToolFields = computed(() => formulaToolFields.value.some(item => item?.formulaConfig?.type))
const changed = computed(() => JSON.stringify(payload.value) !== baseline)
const needsDict = computed(() => ['DICT', 'RADIO', 'CHECKBOX'].includes(form.fieldType) || ['select', 'radio', 'checkbox', 'dictSelect'].includes(form.componentType))
const normalizedDataType = computed(() => String(form.dataType || '').toLowerCase())
const supportsLength = computed(() => ['varchar', 'char', 'decimal'].includes(normalizedDataType.value))
const supportsPrecision = computed(() => normalizedDataType.value === 'decimal')
const showStorageOptions = computed(() => supportsLength.value || supportsPrecision.value)
const formulaEnabled = computed(() => !!form.formulaType)
const formulaFeedbackLines = computed(() => {
  const result = formulaValidateResult.value
  if (!result || result.valid)
    return []
  if (Array.isArray(result.errors) && result.errors.length) {
    return result.errors
      .map(item => item?.message || item?.errorMessage || String(item || ''))
      .filter(Boolean)
  }
  return [result.errorMessage || '表达式校验失败']
})
const previewTargetLabel = computed(() => `${form.fieldName || form.fieldCode || '目标字段'}（${form.fieldCode || '-'}）`)
const previewVariableCodes = computed(() => collectFormulaVariableCodes())
const previewVariableFields = computed(() => {
  return previewVariableCodes.value.map((fieldCode) => {
    const meta = resolveDependFieldMeta(fieldCode)
    return {
      field: fieldCode,
      label: buildDependFieldLabel(meta, fieldCode),
      inputType: resolvePreviewInputType(meta, fieldCode),
    }
  })
})
const canPreviewCalculate = computed(() => {
  if (!form.formulaExpression || props.field?.systemField)
    return false
  return previewVariableFields.value.every(item => hasPreviewInputValue(formulaPreviewForm[item.field]))
})
const canOpenFormulaPreview = computed(() => {
  return Boolean(form.formulaExpression)
    && !props.field?.systemField
    && form.formulaType !== 'LOOKUP'
    && !form.formulaCrossObjectEnabled
})
const lengthMax = computed(() => {
  if (normalizedDataType.value === 'decimal')
    return 65
  if (normalizedDataType.value === 'char')
    return 255
  return 2048
})

const dependFieldOptions = computed(() => {
  if (!props.allFields)
    return []
  return props.allFields
    .filter(item => item && item.fieldCode !== form.fieldCode && item.fieldStatus !== 'HIDDEN')
    .map(item => ({
      label: `${item.fieldName || item.label || item.fieldCode}（${item.fieldCode || item.field}）`,
      value: item.fieldCode || item.field,
    }))
})

const cascadeSourceFieldOptions = computed(() => props.allFields
  .filter(item => item && item.fieldCode !== form.fieldCode && item.fieldStatus !== 'HIDDEN')
  .map(item => ({
    label: `${item.fieldName || item.label || item.fieldCode}（${item.fieldCode || item.field}）`,
    value: item.fieldCode || item.field,
  })))

watch(
  () => props.field,
  () => resetForm(),
  { immediate: true, deep: true },
)

watch(
  () => form.fieldType,
  (value, oldValue) => {
    if (resetting || !oldValue || value === oldValue || props.field?.systemField)
      return
    applyFieldTypeDefaults(value)
  },
)

watch(
  () => form.fieldCode,
  (value, oldValue) => {
    if (resetting || props.field?.systemField || !oldValue || value === oldValue)
      return
    const previousColumn = camelToSnake(oldValue)
    if (!form.columnName || form.columnName === previousColumn)
      form.columnName = camelToSnake(value)
  },
)

watch(changed, (value) => {
  if (!resetting)
    emit('dirtyChange', value)
})

watch(() => form.formulaExpression, () => {
  syncFormulaDependsOnFromExpression()
  formulaValidateResult.value = null
  formulaPreviewResult.value = null
  if (formulaPreviewDialogVisible.value) {
    initializePreviewForm()
    schedulePreviewCalculation()
  }
})

watch(() => form.formulaConditionExpression, () => {
  syncFormulaDependsOnFromExpression()
  formulaValidateResult.value = null
  formulaPreviewResult.value = null
})

watch(() => form.formulaDependsOn, () => {
  formulaPreviewResult.value = null
  if (formulaPreviewDialogVisible.value)
    initializePreviewForm()
}, { deep: true })

watch(() => props.allFields, () => {
  syncFormulaDependsOnFromExpression()
}, { deep: true })

watch(formulaPreviewForm, () => {
  if (!formulaPreviewDialogVisible.value || formulaPreviewInitializing)
    return
  schedulePreviewCalculation()
}, { deep: true })

watch(formulaPreviewDialogVisible, (visible) => {
  if (!visible)
    clearPreviewTimer()
})

onBeforeUnmount(() => {
  clearPreviewTimer()
})

function resetForm() {
  resetting = true
  Object.assign(form, createFieldForm(props.field))
  baseline = JSON.stringify(normalizePayload(form))
  formulaValidateResult.value = null
  formulaPreviewResult.value = null
  formulaConditionRuleValidation.value = null
  formulaPreviewDialogVisible.value = false
  resetPreviewForm()
  emit('dirtyChange', false)
  nextTick(() => {
    resetting = false
    syncFormulaDependsOnFromExpression()
  })
}

function createFieldForm(field) {
  const currentField = field || {}
  const basicProps = { ...(currentField.basicProps || {}) }
  return {
    fieldName: currentField.fieldName || '',
    fieldCode: currentField.fieldCode || '',
    columnName: currentField.columnName || '',
    fieldType: currentField.fieldType || 'TEXT',
    dataType: currentField.dataType || 'varchar',
    length: currentField.length ?? 255,
    precision: currentField.precision ?? 0,
    required: !!currentField.required,
    defaultValue: currentField.defaultValue ?? '',
    searchable: !!currentField.searchable,
    listVisible: currentField.listVisible !== false,
    formVisible: currentField.formVisible !== false,
    importable: currentField.importable !== false,
    exportable: currentField.exportable !== false,
    componentType: currentField.componentType || '',
    queryType: currentField.queryType || '',
    dictType: currentField.dictType || '',
    sensitiveType: currentField.sensitiveType || '',
    encryptAlgorithm: currentField.encryptAlgorithm || '',
    sortable: !!currentField.sortable,
    systemField: !!currentField.systemField,
    readonly: !!currentField.readonly,
    fieldStatus: currentField.fieldStatus || 'ENABLED',
    referenceObjectCode: currentField.referenceObjectCode || '',
    referenceDisplayField: currentField.referenceDisplayField || '',
    placeholder: currentField.basicProps?.placeholder || currentField.placeholder || '',
    remark: currentField.remark || '',
    sortOrder: currentField.sortOrder ?? 0,
    basicProps: {
      ...basicProps,
      cascade: createDefaultCascade(basicProps.cascade || currentField.cascade || currentField.props?.cascade),
    },
    advancedProps: { ...(currentField.advancedProps || {}) },
    formulaName: currentField.formulaConfig
      ? (currentField.formulaConfig?.name || currentField.formulaConfig?.formulaName || currentField.fieldName || '')
      : '',
    formulaType: currentField.formulaConfig?.type || '',
    formulaMode: currentField.formulaConfig?.mode || 'STORED',
    formulaTriggerMode: currentField.formulaConfig?.triggerMode || modeToTriggerMode(currentField.formulaConfig?.mode),
    formulaExpression: currentField.formulaConfig?.expression || '',
    formulaDependsOn: currentField.formulaConfig?.dependsOn || [],
    formulaFunctionRefs: currentField.formulaConfig?.functionRefs || [],
    formulaAggregateFunction: currentField.formulaConfig?.aggregate?.function || '',
    formulaAggregateRelationCode: currentField.formulaConfig?.aggregate?.relationCode
      ? String(currentField.formulaConfig.aggregate.relationCode)
      : '',
    formulaAggregateTargetField: currentField.formulaConfig?.aggregate?.targetField || '',
    formulaAggregateFilter: currentField.formulaConfig?.aggregate?.filter || '',
    formulaConditionMode: currentField.formulaConfig?.rule ? 'RULE' : 'EXPRESSION',
    formulaConditionRule: cloneConditionRule(currentField.formulaConfig?.rule),
    formulaConditionExpression: currentField.formulaConfig?.condition?.expression || currentField.formulaConfig?.expression || '',
    formulaConditionTrueValue: currentField.formulaConfig?.condition?.trueValue ?? '',
    formulaConditionFalseValue: currentField.formulaConfig?.condition?.falseValue ?? '',
    formulaLookupRelationCode: currentField.formulaConfig?.lookup?.relationCode || '',
    formulaLookupTargetObjectCode: currentField.formulaConfig?.lookup?.targetObjectCode || '',
    formulaLookupSourceField: currentField.formulaConfig?.lookup?.sourceField || '',
    formulaLookupTargetField: currentField.formulaConfig?.lookup?.targetField || '',
    formulaLookupReturnField: currentField.formulaConfig?.lookup?.returnField || '',
    formulaLookupNotFoundValue: currentField.formulaConfig?.lookup?.notFoundValue ?? '',
    formulaCrossObjectEnabled: Boolean(currentField.formulaConfig?.crossObject),
    formulaCrossObjectPath: currentField.formulaConfig?.crossObject?.path || '',
    formulaCrossObjectRelationCode: currentField.formulaConfig?.crossObject?.relationCode || '',
    formulaCrossObjectTargetObjectCode: currentField.formulaConfig?.crossObject?.targetObjectCode || '',
    formulaCrossObjectReturnField: currentField.formulaConfig?.crossObject?.returnField || '',
    formulaCrossObjectRecomputeMode: currentField.formulaConfig?.crossObject?.recomputeMode || 'ASYNC',
  }
}

function cloneConditionRule(rule) {
  if (!rule || typeof rule !== 'object')
    return createDefaultConditionRule()
  try {
    return JSON.parse(JSON.stringify(rule))
  }
  catch {
    return createDefaultConditionRule()
  }
}

function createDefaultConditionRule() {
  const firstField = (props.allFields || [])
    .find(item => item && item.fieldStatus !== 'HIDDEN' && (item.fieldCode || item.field))
  return {
    operator: 'AND',
    children: [
      {
        field: firstField?.fieldCode || firstField?.field || '',
        op: 'EQ',
        value: '',
      },
    ],
  }
}

function normalizePayload(source) {
  const cascade = normalizeCascade(source.basicProps?.cascade)
  const basicProps = {
    ...(source.basicProps || {}),
    placeholder: source.placeholder || '',
  }
  if (cascade.enabled)
    basicProps.cascade = cascade
  else
    delete basicProps.cascade
  return {
    fieldName: source.fieldName,
    fieldCode: source.fieldCode,
    columnName: source.columnName,
    fieldType: source.fieldType,
    dataType: source.dataType,
    length: source.length,
    precision: source.precision,
    required: source.required,
    defaultValue: source.defaultValue,
    searchable: source.searchable,
    listVisible: source.listVisible,
    formVisible: source.formVisible,
    importable: source.importable,
    exportable: source.exportable,
    componentType: source.componentType,
    queryType: source.queryType,
    dictType: source.dictType,
    sensitiveType: source.sensitiveType,
    encryptAlgorithm: source.encryptAlgorithm,
    sortable: source.sortable,
    systemField: source.systemField,
    readonly: source.readonly,
    fieldStatus: source.fieldStatus,
    referenceObjectCode: source.referenceObjectCode,
    referenceDisplayField: source.referenceDisplayField,
    placeholder: source.placeholder,
    remark: source.remark,
    sortOrder: source.sortOrder,
    basicProps,
    advancedProps: { ...(source.advancedProps || {}) },
    formulaConfig: buildFormulaConfigPayload(source),
  }
}

function buildFormulaConfigPayload(source) {
  if (!source.formulaType)
    return null
  const dependsOn = normalizeFormulaDependsOn(source)
  const expression = source.formulaType === 'LOOKUP'
    ? ''
    : (source.formulaCrossObjectEnabled ? source.formulaCrossObjectPath : source.formulaExpression) || ''
  const config = {
    name: source.formulaName || '',
    type: source.formulaType,
    mode: source.formulaMode || 'STORED',
    triggerMode: source.formulaTriggerMode || modeToTriggerMode(source.formulaMode),
    expression,
    dependsOn,
    functionRefs: normalizeFormulaFunctionRefs(source),
  }
  if (source.formulaType === 'LOOKUP') {
    config.lookup = {
      relationCode: source.formulaLookupRelationCode || '',
      targetObjectCode: source.formulaLookupTargetObjectCode || '',
      sourceField: source.formulaLookupSourceField || '',
      targetField: source.formulaLookupTargetField || '',
      returnField: source.formulaLookupReturnField || '',
      notFoundValue: source.formulaLookupNotFoundValue ?? null,
    }
  }
  if (source.formulaType === 'AGGREGATE' && source.formulaAggregateFunction) {
    config.aggregate = {
      function: source.formulaAggregateFunction,
      relationCode: source.formulaAggregateRelationCode || '',
      targetField: source.formulaAggregateTargetField || '',
      filter: source.formulaAggregateFilter || '',
    }
  }
  if (source.formulaType === 'CONDITIONAL' && source.formulaConditionExpression) {
    config.condition = {
      expression: source.formulaConditionExpression,
      trueValue: source.formulaConditionTrueValue ?? '',
      falseValue: source.formulaConditionFalseValue ?? '',
    }
    if (source.formulaConditionMode === 'RULE') {
      config.rule = cloneConditionRule(source.formulaConditionRule)
      config.ruleMode = 'RULE'
    }
  }
  if (source.formulaType !== 'LOOKUP' && source.formulaCrossObjectEnabled) {
    config.crossObject = {
      path: source.formulaCrossObjectPath || '',
      relationCode: source.formulaCrossObjectRelationCode || '',
      targetObjectCode: source.formulaCrossObjectTargetObjectCode || '',
      returnField: source.formulaCrossObjectReturnField || '',
      recomputeMode: source.formulaCrossObjectRecomputeMode || 'ASYNC',
    }
  }

  return config
}

function onConditionExpressionChange(value) {
  // 条件表达式也作为校验/预览时的执行表达式。
  form.formulaExpression = value
  formulaConditionRuleValidation.value = null
}

function onConditionModeChange(value) {
  formulaValidateResult.value = null
  formulaPreviewResult.value = null
  if (value === 'RULE') {
    form.formulaConditionRule = cloneConditionRule(form.formulaConditionRule)
    return
  }
  formulaConditionRuleValidation.value = null
}

function onConditionRuleCompiled(expression) {
  form.formulaConditionExpression = expression
  form.formulaExpression = expression
  formulaValidateResult.value = null
  formulaPreviewResult.value = null
}

function onConditionRuleValidation(result) {
  formulaConditionRuleValidation.value = result
  if (result && !result.valid)
    formulaValidateResult.value = result
}

function onFormulaTypeChange(value) {
  formulaValidateResult.value = null
  formulaPreviewResult.value = null
  if (!value) {
    clearFormulaForm()
    return
  }
  if (!form.formulaMode)
    setFormulaTriggerMode('ON_SAVE')
  if (value === 'LOOKUP') {
    form.formulaExpression = ''
    form.formulaDependsOn = []
    form.formulaCrossObjectEnabled = false
    setFormulaTriggerMode(form.formulaTriggerMode || 'REALTIME')
  }
  if (value === 'CONDITIONAL') {
    form.formulaConditionMode = form.formulaConditionMode || 'EXPRESSION'
    form.formulaConditionRule = cloneConditionRule(form.formulaConditionRule)
  }
  if (value === 'AGGREGATE')
    clearCrossObjectForm()
}

function updateFormulaEnabled(value) {
  if (value) {
    form.formulaType = form.formulaType || 'CALC'
    form.formulaName = form.formulaName || form.fieldName || '金额计算'
    setFormulaTriggerMode(form.formulaTriggerMode || 'REALTIME')
    return
  }
  clearFormulaForm()
  formulaValidateResult.value = null
  formulaPreviewResult.value = null
}

function clearFormulaForm() {
  form.formulaName = ''
  form.formulaType = ''
  form.formulaMode = 'STORED'
  form.formulaTriggerMode = 'ON_SAVE'
  form.formulaExpression = ''
  form.formulaDependsOn = []
  form.formulaFunctionRefs = []
  form.formulaAggregateFunction = ''
  form.formulaAggregateRelationCode = ''
  form.formulaAggregateTargetField = ''
  form.formulaAggregateFilter = ''
  form.formulaConditionMode = 'EXPRESSION'
  form.formulaConditionRule = createDefaultConditionRule()
  form.formulaConditionExpression = ''
  form.formulaConditionTrueValue = ''
  form.formulaConditionFalseValue = ''
  form.formulaLookupRelationCode = ''
  form.formulaLookupTargetObjectCode = ''
  form.formulaLookupSourceField = ''
  form.formulaLookupTargetField = ''
  form.formulaLookupReturnField = ''
  form.formulaLookupNotFoundValue = ''
  clearCrossObjectForm()
}

function clearCrossObjectForm() {
  form.formulaCrossObjectEnabled = false
  form.formulaCrossObjectPath = ''
  form.formulaCrossObjectRelationCode = ''
  form.formulaCrossObjectTargetObjectCode = ''
  form.formulaCrossObjectReturnField = ''
  form.formulaCrossObjectRecomputeMode = 'ASYNC'
}

function setFormulaTriggerMode(value) {
  form.formulaTriggerMode = value
  form.formulaMode = value === 'REALTIME' ? 'VIRTUAL' : 'STORED'
}

function modeToTriggerMode(mode) {
  return mode === 'VIRTUAL' ? 'REALTIME' : 'ON_SAVE'
}

function insertFormulaToken(value) {
  if (!value || props.field?.systemField) {
    return
  }
  const token = normalizeExpressionToken(value)
  const current = form.formulaExpression || ''
  const joiner = current && !/\s$/.test(current) ? ' ' : ''
  form.formulaExpression = `${current}${joiner}${token}`
}

function insertStringToken() {
  insertFormulaToken('\'\'')
}

function normalizeExpressionToken(value) {
  if (value === '字段') {
    return dependFieldOptions.value[0]?.value || ''
  }
  return String(value)
}

function validateFormulaConfigLocal() {
  const errors = []
  if (!form.formulaType)
    errors.push('请选择公式类型')

  if (form.formulaType === 'LOOKUP') {
    if (!form.formulaLookupRelationCode)
      errors.push('请选择 LOOKUP 对象关系')
    if (!form.formulaLookupTargetObjectCode)
      errors.push('LOOKUP 目标对象不能为空')
    if (!form.formulaLookupSourceField)
      errors.push('请选择当前对象字段')
    if (!form.formulaLookupTargetField)
      errors.push('请选择目标匹配字段')
    if (!form.formulaLookupReturnField)
      errors.push('请选择返回字段')
  }
  else if (form.formulaCrossObjectEnabled) {
    if (!form.formulaCrossObjectRelationCode)
      errors.push('请选择跨对象关系')
    if (!form.formulaCrossObjectTargetObjectCode)
      errors.push('跨对象目标对象不能为空')
    if (!form.formulaCrossObjectReturnField)
      errors.push('请选择跨对象返回字段')
    if (!isOneHopPath(form.formulaCrossObjectPath))
      errors.push('跨对象路径必须是一跳 relation.field')
  }
  else if (form.formulaType === 'AGGREGATE') {
    if (!form.formulaAggregateFunction)
      errors.push('请选择聚合函数')
    if (!form.formulaAggregateRelationCode)
      errors.push('请选择聚合关联对象')
    if (form.formulaAggregateFunction !== 'COUNT' && !form.formulaAggregateTargetField)
      errors.push('请选择聚合目标字段')
  }
  else if (!form.formulaExpression) {
    errors.push('表达式不能为空')
  }

  if (form.formulaType === 'CONDITIONAL' && !form.formulaConditionExpression)
    errors.push('条件表达式不能为空')
  if (form.formulaType === 'CONDITIONAL' && form.formulaConditionMode === 'RULE') {
    if (!form.formulaConditionRule)
      errors.push('条件规则不能为空')
    if (formulaConditionRuleValidation.value && !formulaConditionRuleValidation.value.valid) {
      formulaConditionRuleValidation.value.errors?.forEach((item) => {
        errors.push(item?.message || item?.errorMessage || String(item || '条件规则校验失败'))
      })
    }
  }

  return {
    valid: errors.length === 0,
    errors: errors.map(message => ({ message })),
    variables: normalizeFormulaDependsOn(form),
  }
}

function isOneHopPath(path) {
  const value = String(path || '').trim()
  const firstDot = value.indexOf('.')
  return firstDot > 0 && firstDot === value.lastIndexOf('.') && firstDot < value.length - 1
}

async function handleValidateFormula() {
  const localResult = validateFormulaConfigLocal()
  if (!localResult.valid) {
    formulaValidateResult.value = localResult
    return
  }
  if (form.formulaType === 'LOOKUP' || form.formulaCrossObjectEnabled) {
    formulaValidateResult.value = localResult
    return
  }
  formulaValidating.value = true
  formulaValidateResult.value = null
  try {
    const res = await validateFormula({
      expression: form.formulaExpression,
      type: form.formulaType || 'CALC',
      dependsOn: normalizeFormulaDependsOn(form),
    })
    formulaValidateResult.value = res?.data ?? res
  }
  catch (e) {
    formulaValidateResult.value = { valid: false, errorMessage: e?.message || '验证请求失败' }
  }
  finally {
    formulaValidating.value = false
  }
}

async function openFormulaPreview() {
  if (!canOpenFormulaPreview.value)
    return
  formulaPreviewDialogVisible.value = true
  initializePreviewForm()
  await nextTick()
  schedulePreviewCalculation(0)
}

function buildPreviewSampleValues() {
  const values = {}
  previewVariableCodes.value.forEach((fieldCode, index) => {
    values[fieldCode] = guessFormulaSampleValue(resolveDependFieldMeta(fieldCode), index, fieldCode)
  })
  return values
}

function collectFormulaVariableCodes() {
  const result = []
  if (form.formulaType === 'LOOKUP') {
    appendFormulaVariable(result, form.formulaLookupSourceField)
    return result
  }
  if (form.formulaCrossObjectEnabled)
    return result
  appendFormulaDependsOn(result, form.formulaDependsOn)
  appendExpressionVariables(result, form.formulaExpression)
  if (form.formulaType === 'CONDITIONAL')
    appendExpressionVariables(result, form.formulaConditionExpression)
  return result
}

function normalizeFormulaDependsOn(source) {
  const result = []
  if (source.formulaType === 'LOOKUP') {
    appendFormulaVariable(result, source.formulaLookupSourceField)
    return result.filter(fieldCode => fieldCode !== source.fieldCode)
  }
  if (source.formulaCrossObjectEnabled)
    return []
  appendFormulaDependsOn(result, source.formulaDependsOn)
  appendExpressionVariables(result, source.formulaExpression)
  if (source.formulaType === 'CONDITIONAL')
    appendExpressionVariables(result, source.formulaConditionExpression)
  return result.filter(fieldCode => fieldCode !== source.fieldCode)
}

function normalizeFormulaFunctionRefs(source) {
  const result = []
  if (source.formulaType === 'LOOKUP')
    return result
  appendFormulaFunctionRefs(result, source.formulaExpression)
  if (source.formulaType === 'CONDITIONAL')
    appendFormulaFunctionRefs(result, source.formulaConditionExpression)
  return result
}

function appendFormulaFunctionRefs(result, expression) {
  const text = stripFormulaStringLiterals(expression)
  if (!text)
    return
  const pattern = /([a-z_]\w*(?:\.[a-z_]\w*)*)\s*\(/gi
  let match = pattern.exec(text)
  while (match) {
    const token = match[1]
    if (isManagedFormulaFunctionName(token) && !result.includes(token))
      result.push(token)
    match = pattern.exec(text)
  }
}

function isManagedFormulaFunctionName(token) {
  const value = String(token || '')
  return value.includes('.') || value === 'date_to_string'
}

function syncFormulaDependsOnFromExpression() {
  if (resetting || !formulaEnabled.value || form.formulaType === 'AGGREGATE'
    || form.formulaType === 'LOOKUP' || form.formulaCrossObjectEnabled) {
    return
  }
  const hasExpression = Boolean(String(form.formulaExpression || '').trim())
    || (form.formulaType === 'CONDITIONAL' && Boolean(String(form.formulaConditionExpression || '').trim()))
  const expressionDeps = collectExpressionFieldCodes()
  if (!hasExpression && !form.formulaDependsOn.length)
    return
  if (isSameStringArray(form.formulaDependsOn, expressionDeps))
    return
  form.formulaDependsOn = expressionDeps
}

function collectExpressionFieldCodes() {
  const availableFields = new Set((props.allFields || [])
    .map(item => item?.fieldCode || item?.field)
    .filter(Boolean)
    .filter(fieldCode => fieldCode !== form.fieldCode))
  const result = []
  appendExpressionVariables(result, form.formulaExpression)
  if (form.formulaType === 'CONDITIONAL')
    appendExpressionVariables(result, form.formulaConditionExpression)
  return result.filter(fieldCode => availableFields.has(fieldCode))
}

function appendFormulaDependsOn(result, dependsOn) {
  ;(Array.isArray(dependsOn) ? dependsOn : []).forEach(value => appendFormulaVariable(result, value))
}

function appendExpressionVariables(result, expression) {
  extractFormulaVariables(expression).forEach(value => appendFormulaVariable(result, value))
}

function appendFormulaVariable(result, value) {
  const append = (value) => {
    const fieldCode = String(value || '').trim()
    if (fieldCode && !result.includes(fieldCode))
      result.push(fieldCode)
  }
  append(value)
}

function isSameStringArray(left = [], right = []) {
  const normalizedLeft = Array.isArray(left) ? left : []
  const normalizedRight = Array.isArray(right) ? right : []
  if (normalizedLeft.length !== normalizedRight.length)
    return false
  return normalizedLeft.every((item, index) => item === normalizedRight[index])
}

function extractFormulaVariables(expression) {
  const text = stripFormulaStringLiterals(expression)
  if (!text)
    return []
  const variables = []
  const pattern = /[a-z_]\w*/gi
  let match = pattern.exec(text)
  while (match) {
    const token = match[0]
    const previous = text[match.index - 1] || ''
    const nextIndex = match.index + token.length
    const nextText = text.slice(nextIndex).trimStart()
    if (!isFormulaReservedToken(token) && previous !== '.' && !nextText.startsWith('(') && !variables.includes(token))
      variables.push(token)
    match = pattern.exec(text)
  }
  return variables
}

function stripFormulaStringLiterals(expression) {
  return String(expression || '').replace(/'[^']*'|"[^"]*"/g, ' ')
}

function isFormulaReservedToken(token) {
  return [
    'true',
    'false',
    'null',
    'nil',
    'and',
    'or',
    'not',
    'if',
    'else',
    'return',
    'math',
    'string',
    'seq',
    'date',
  ].includes(String(token || '').toLowerCase())
}

function resolveDependFieldMeta(fieldCode) {
  return (props.allFields || []).find((item) => {
    const code = item?.fieldCode || item?.field
    return code === fieldCode
  }) || null
}

function buildDependFieldLabel(fieldMeta, fallback) {
  const name = fieldMeta?.fieldName || fieldMeta?.label || fallback
  const code = fieldMeta?.fieldCode || fieldMeta?.field || fallback
  return `${name}（${code}）`
}

function resolvePreviewInputType(fieldMeta, fieldCode = '') {
  const fieldType = String(fieldMeta?.fieldType || '').toUpperCase()
  const componentType = String(fieldMeta?.componentType || fieldMeta?.type || '').toLowerCase()
  const dataType = String(fieldMeta?.dataType || '').toLowerCase()
  const code = String(fieldMeta?.fieldCode || fieldMeta?.field || fieldCode).toLowerCase()
  if (fieldType === 'SWITCH' || componentType === 'switch')
    return 'switch'
  if (includesAny(code, ['qty', 'quantity', 'count', 'num', 'number', 'price', 'amount', 'money', 'fee', 'cost', 'total', 'rate', 'ratio', 'percent']))
    return 'number'
  if (['NUMBER', 'MONEY'].includes(fieldType)
    || ['number', 'inputnumber', 'input-number'].includes(componentType)
    || ['int', 'integer', 'bigint', 'decimal', 'double', 'float'].includes(dataType)) {
    return 'number'
  }
  if (!fieldMeta && form.formulaType === 'CALC')
    return 'number'
  return 'text'
}

function guessFormulaSampleValue(fieldMeta, index = 0, fallbackCode = '') {
  const code = String(fieldMeta?.fieldCode || fieldMeta?.field || fallbackCode).toLowerCase()
  const type = String(fieldMeta?.fieldType || fieldMeta?.type || fieldMeta?.componentType || '').toUpperCase()
  const defaultValue = fieldMeta?.defaultValue
  if (defaultValue !== null && defaultValue !== undefined && defaultValue !== '')
    return defaultValue
  if (includesAny(code, ['qty', 'quantity', 'count', 'num', 'number', '数量', '件数']))
    return 3
  if (includesAny(code, ['price', 'amount', 'money', 'fee', 'cost', 'total', '金额', '单价', '价格', '费用', '成本']))
    return 100
  if (includesAny(code, ['rate', 'ratio', 'percent', '折扣', '比例', '率']))
    return 0.1
  if (type.includes('SWITCH') || type.includes('BOOLEAN'))
    return true
  if (type.includes('DATE') || type.includes('TIME'))
    return '2026-01-01'
  if (type.includes('TEXT') || type.includes('VARCHAR') || type.includes('CHAR'))
    return '示例'
  return index + 1
}

function includesAny(value, keywords = []) {
  return keywords.some(keyword => value.includes(keyword))
}

function initializePreviewForm({ overwrite = false } = {}) {
  formulaPreviewInitializing = true
  const sampleValues = buildPreviewSampleValues()
  Object.keys(formulaPreviewForm).forEach((key) => {
    if (!Object.prototype.hasOwnProperty.call(sampleValues, key))
      delete formulaPreviewForm[key]
  })
  Object.entries(sampleValues).forEach(([key, value]) => {
    if (overwrite || !hasPreviewInputValue(formulaPreviewForm[key]))
      formulaPreviewForm[key] = value
  })
  nextTick(() => {
    formulaPreviewInitializing = false
  })
}

function resetPreviewForm() {
  clearPreviewTimer()
  Object.keys(formulaPreviewForm).forEach((key) => {
    delete formulaPreviewForm[key]
  })
}

function fillPreviewSampleValues() {
  initializePreviewForm({ overwrite: true })
  schedulePreviewCalculation(0)
}

function hasPreviewInputValue(value) {
  if (Array.isArray(value))
    return value.length > 0
  return value !== null && value !== undefined && value !== ''
}

function schedulePreviewCalculation(delay = 320) {
  clearPreviewTimer()
  if (!formulaPreviewDialogVisible.value || !canPreviewCalculate.value)
    return
  formulaPreviewTimer = setTimeout(() => {
    formulaPreviewTimer = null
    handlePreviewFormula()
  }, delay)
}

function clearPreviewTimer() {
  if (!formulaPreviewTimer)
    return
  clearTimeout(formulaPreviewTimer)
  formulaPreviewTimer = null
}

function buildFormulaPreviewPayload(sampleValues) {
  const payload = {
    expression: form.formulaExpression,
    type: form.formulaType || 'CALC',
    dependsOn: previewVariableCodes.value,
    sampleValues,
  }
  if (form.formulaType === 'CONDITIONAL') {
    const expression = form.formulaConditionExpression || form.formulaExpression
    payload.expression = expression
    payload.condition = {
      expression,
      trueValue: form.formulaConditionTrueValue ?? '',
      falseValue: form.formulaConditionFalseValue ?? '',
    }
  }
  return payload
}

async function handlePreviewFormula() {
  if (!form.formulaExpression)
    return
  if (!canPreviewCalculate.value) {
    formulaPreviewResult.value = { success: false, errorMessage: '请先填写变量字段值' }
    return
  }
  const sampleValues = { ...formulaPreviewForm }
  formulaPreviewing.value = true
  formulaPreviewResult.value = null
  try {
    const res = await previewFormula(buildFormulaPreviewPayload(sampleValues))
    formulaPreviewResult.value = res?.data ?? res
  }
  catch (e) {
    formulaPreviewResult.value = { success: false, errorMessage: e?.message || '预览请求失败' }
  }
  finally {
    formulaPreviewing.value = false
  }
}

function formatPreviewValue(value) {
  if (value === null || value === undefined || value === '')
    return '-'
  if (typeof value === 'object')
    return JSON.stringify(value)
  return String(value)
}

function updateCascadeEnabled(value) {
  form.basicProps.cascade = createDefaultCascade({
    ...(form.basicProps.cascade || {}),
    enabled: value,
  })
}

function createDefaultCascade(source = {}) {
  return {
    enabled: !!source.enabled,
    sourceField: source.sourceField || '',
    mode: source.mode || source.matchMode || 'linkedDict',
    paramName: source.paramName || '',
    clearOnParentChange: source.clearOnParentChange !== false,
  }
}

function normalizeCascade(source = {}) {
  const cascade = createDefaultCascade(source)
  if (!cascade.enabled || !cascade.sourceField)
    return { ...cascade, enabled: false }
  if (cascade.mode !== 'remoteParam')
    cascade.paramName = ''
  return cascade
}

function applyFieldTypeDefaults(fieldType) {
  const defaults = {
    TEXT: { dataType: 'varchar', componentType: 'input', length: 128, precision: 2, queryType: 'like' },
    MULTILINE: { dataType: 'text', componentType: 'textarea', length: null, precision: 2, queryType: 'like' },
    NUMBER: { dataType: 'int', componentType: 'number', length: 11, precision: 0, queryType: 'eq' },
    MONEY: { dataType: 'decimal', componentType: 'number', length: 18, precision: 2, queryType: 'eq' },
    DATE: { dataType: 'date', componentType: 'date', length: null, precision: null, queryType: 'eq' },
    DATETIME: { dataType: 'datetime', componentType: 'datetime', length: null, precision: null, queryType: 'eq' },
    DICT: { dataType: 'varchar', componentType: 'select', length: 64, precision: 2, queryType: 'eq' },
    RADIO: { dataType: 'varchar', componentType: 'radio', length: 64, precision: 2, queryType: 'eq' },
    CHECKBOX: { dataType: 'varchar', componentType: 'checkbox', length: 255, precision: 2, queryType: 'in' },
    SWITCH: { dataType: 'tinyint', componentType: 'switch', length: 1, precision: 0, queryType: 'eq' },
    FILE: { dataType: 'varchar', componentType: 'fileUpload', length: 512, precision: 2, queryType: 'eq' },
    IMAGE: { dataType: 'varchar', componentType: 'imageUpload', length: 512, precision: 2, queryType: 'eq' },
    USER: { dataType: 'bigint', componentType: 'userSelect', length: null, precision: null, queryType: 'eq' },
    DEPT: { dataType: 'bigint', componentType: 'orgTreeSelect', length: null, precision: null, queryType: 'eq' },
    REGION: { dataType: 'varchar', componentType: 'regionTreeSelect', length: 32, precision: 2, queryType: 'eq' },
    REFERENCE: { dataType: 'bigint', componentType: 'objectReference', length: null, precision: null, queryType: 'eq' },
  }[fieldType]
  if (!defaults)
    return
  Object.assign(form, defaults)
  if (!['DICT', 'RADIO', 'CHECKBOX'].includes(fieldType))
    form.dictType = ''
}

defineExpose({
  resetForm,
  getPayload: () => payload.value,
  hasChanges: () => changed.value,
})
</script>

<style scoped>
.property-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  width: 100%;
  min-height: 0;
  min-width: 0;
  height: 100%;
  background: #fbfcfe;
}

.property-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #e5e7eb;
  padding: 16px;
}

.property-head h3 {
  margin: 0;
  color: #111827;
  font-size: 15px;
  letter-spacing: 0;
}

.property-head p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
}

.property-body {
  display: grid;
  min-height: 0;
  min-width: 0;
  overflow: hidden;
  background: #fff;
  padding: 0 16px 16px;
}

.property-tabs {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  width: 100%;
  height: 100%;
  min-height: 0;
  min-width: 0;
}

.property-tabs :deep(.n-tabs-nav) {
  position: sticky;
  top: 0;
  z-index: 2;
  background: #fbfcfe;
  padding-top: 10px;
}

.property-tabs :deep(.n-tab-pane) {
  height: 100%;
  min-width: 0;
  min-height: 0;
  padding-top: 14px;
}

.property-tabs :deep(.n-tabs-pane-wrapper) {
  min-width: 0;
  min-height: 0;
  background: #fff;
  overflow: auto;
}

.property-tabs :deep(.n-form) {
  width: 100%;
  min-width: 0;
}

.formula-tab-form {
  display: grid;
  height: 100%;
  min-height: 0;
  background: #fff;
}

.property-tabs :deep(.formula-tab-pane) {
  background: #fff;
  padding-top: 8px;
}

.switch-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 16px;
}

.switch-grid label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
  color: #475569;
  font-size: 12px;
  padding: 8px 10px;
}

.full-input {
  width: 100%;
}

.dict-property-item {
  max-width: 280px;
}

.dict-property-item :deep(.dict-select-row) {
  grid-template-columns: minmax(0, 1fr) 72px;
}

.dict-property-item :deep(.create-dict-button) {
  width: 72px;
}

.cascade-config {
  display: grid;
  gap: 10px;
  margin-bottom: 16px;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: #fff;
  padding: 12px;
}

.cascade-config-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.cascade-config-head strong,
.cascade-config-head span {
  display: block;
}

.cascade-config-head strong {
  color: #111827;
  font-size: 13px;
}

.cascade-config-head span {
  margin-top: 3px;
  color: #64748b;
  font-size: 12px;
}

.cascade-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 4px;
}

.property-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  border-top: 1px solid #e5e7eb;
  background: #fff;
  padding: 12px 16px;
}

.formula-setting-card {
  display: grid;
  gap: 14px;
  margin-top: 14px;
  border: 1px solid #d7dce5;
  border-radius: 8px;
  background: #f1f3f6;
  padding: 16px 14px 18px;
}

.formula-setting-card.active {
  background: #f1f3f6;
}

.formula-setting-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.formula-setting-head strong {
  color: #1f2937;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0;
}

.formula-form-item {
  margin-bottom: 0;
}

.formula-form-item :deep(.n-form-item-label) {
  min-height: auto;
  padding-bottom: 8px;
}

.formula-form-item :deep(.n-form-item-label__text) {
  color: #202938;
  font-size: 15px;
  font-weight: 600;
  line-height: 20px;
}

.formula-form-item :deep(.n-input),
.formula-form-item :deep(.n-base-selection) {
  --n-border-radius: 6px;
  --n-height: 38px;
  background: #f7f8fa;
}

.formula-form-item :deep(.n-input__input-el),
.formula-form-item :deep(.n-base-selection-label__render-label) {
  font-size: 14px;
}

.formula-editor {
  width: 100%;
  overflow: hidden;
  border: 1px solid #cfd6e1;
  border-radius: 8px;
  background: #f7f8fa;
}

.formula-editor :deep(.n-input) {
  border: 0;
  border-radius: 0;
  background: #f7f8fa;
}

.formula-editor :deep(.n-input .n-input__border),
.formula-editor :deep(.n-input .n-input__state-border) {
  display: none;
}

.formula-editor :deep(.n-input__textarea-el) {
  min-height: 176px;
  padding: 22px 24px 14px;
  color: #202938;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 15px;
  line-height: 1.8;
}

.formula-editor-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
  min-height: 54px;
  border-top: 1px solid #d2d8e2;
  background: #e9edf3;
  padding: 9px 10px;
}

.formula-token-group,
.formula-tool-group {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.formula-token-group {
  flex: 1 1 220px;
}

.formula-tool-group {
  justify-content: flex-end;
  flex: 0 0 auto;
  margin-left: auto;
}

.formula-token {
  height: 32px;
  border: 0;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  padding: 0 12px;
  white-space: nowrap;
}

.formula-token:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.token-field {
  background: #e8f1ff;
  color: #2563eb;
}

.token-function {
  background: #f0e9ff;
  color: #7c3aed;
}

.token-number {
  background: #e8f8ed;
  color: #16a34a;
}

.token-string {
  background: #fff3d6;
  color: #b7791f;
}

.tool-code-icon,
.tool-fx-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-right: 4px;
  color: #111827;
  font-weight: 800;
}

.formula-tool-button {
  --n-height: 34px;
  --n-border-radius: 7px;
  --n-padding: 0 11px;
  background: #dde3ec;
  color: #1f2937;
  font-weight: 700;
}

.tool-code-icon {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
}

.tool-fx-icon {
  font-size: 16px;
  font-style: italic;
}

.formula-extra-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 2px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  padding: 10px 12px;
}

.formula-trigger-row {
  display: grid;
  gap: 8px;
}

.formula-trigger-row > span {
  color: #1f2937;
  font-size: 15px;
  font-weight: 600;
}

.formula-trigger-row :deep(.n-button-group) {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  width: 100%;
}

.formula-trigger-row :deep(.n-button) {
  justify-content: center;
  min-width: 0;
  height: 36px;
  border-radius: 0;
  background: #e7ebf1;
  color: #202938;
  font-size: 15px;
  font-weight: 600;
}

.formula-trigger-row :deep(.n-button:first-child) {
  border-top-left-radius: 6px;
  border-bottom-left-radius: 6px;
}

.formula-trigger-row :deep(.n-button:last-child) {
  border-top-right-radius: 6px;
  border-bottom-right-radius: 6px;
}

.formula-trigger-row :deep(.formula-trigger-option.selected) {
  border-color: #2f58d6;
  background: #edf3ff;
  color: #274ec8;
}

.formula-action-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-top: 2px;
}

.formula-action-row :deep(.n-button:not(.formula-preview-toggle)) {
  min-width: 104px;
  height: 38px;
  border-radius: 9px;
  font-size: 15px;
  font-weight: 700;
}

.formula-preview-toggle {
  margin-left: auto;
  color: #64748b;
  font-size: 12px;
}

.formula-actions {
  margin-bottom: 12px;
}

.formula-feedback {
  margin-top: 2px;
  margin-bottom: 4px;
  padding: 11px 14px;
  border-radius: 6px;
  font-size: 14px;
  line-height: 1.7;
}

.formula-feedback.success {
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  color: #065f46;
}

.formula-feedback.error {
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #991b1b;
}

.feedback-success,
.feedback-error {
  display: flex;
  align-items: center;
  gap: 6px;
}

.feedback-error {
  align-items: stretch;
  flex-direction: column;
  gap: 2px;
}

.feedback-error-line {
  display: flex;
  align-items: center;
  gap: 6px;
}

.feedback-success .vars {
  color: #64748b;
  margin-left: 8px;
}

.feedback-success .elapsed {
  color: #94a3b8;
  font-size: 11px;
  margin-left: 6px;
}

.formula-preview-modal {
  width: min(560px, calc(100vw - 32px));
}

.formula-preview-dialog {
  display: grid;
  gap: 14px;
}

.formula-preview-summary {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
  padding: 12px;
}

.formula-preview-summary span,
.formula-preview-summary strong {
  display: block;
}

.formula-preview-summary span {
  color: #64748b;
  font-size: 12px;
}

.formula-preview-summary strong {
  margin-top: 3px;
  color: #1f2937;
  font-size: 14px;
  line-height: 1.5;
}

.formula-preview-expression {
  min-height: 40px;
  border: 1px solid #d7dce5;
  border-radius: 8px;
  background: #f7f8fa;
  color: #1f2937;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  line-height: 1.7;
  padding: 9px 11px;
  word-break: break-all;
}

.formula-preview-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 12px;
}

.formula-preview-form :deep(.n-form-item) {
  margin-bottom: 0;
}

.formula-preview-result {
  display: grid;
  gap: 5px;
  border-radius: 8px;
  padding: 12px 14px;
}

.formula-preview-result span {
  font-size: 12px;
  font-weight: 600;
}

.formula-preview-result strong {
  font-size: 20px;
  line-height: 1.4;
  word-break: break-all;
}

.formula-preview-result em {
  color: #64748b;
  font-size: 12px;
  font-style: normal;
}

.formula-preview-result.success {
  border: 1px solid #a7f3d0;
  background: #ecfdf5;
  color: #065f46;
}

.formula-preview-result.error {
  border: 1px solid #fecaca;
  background: #fef2f2;
  color: #991b1b;
}

.formula-preview-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.formula-preview-footer > div {
  display: flex;
  align-items: center;
  gap: 8px;
}

@media (max-width: 560px) {
  .formula-preview-form {
    grid-template-columns: minmax(0, 1fr);
  }

  .formula-preview-footer {
    align-items: stretch;
    flex-direction: column;
  }

  .formula-preview-footer > div {
    justify-content: flex-end;
  }
}
</style>
