<template>
  <aside class="property-panel">
    <template v-if="field">
      <div class="property-head">
        <div>
          <h3>{{ form.fieldName || '字段属性' }}</h3>
          <p>{{ form.fieldCode || '保存后自动生成字段编码和列名' }}</p>
        </div>
        <n-tag v-if="field.systemField" size="small" :bordered="false">
          系统字段
        </n-tag>
      </div>

      <div class="property-body">
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

          <n-form-item label="提示文案">
            <n-input v-model:value="form.placeholder" :disabled="field.systemField" placeholder="请输入提示文案" />
          </n-form-item>

          <n-form-item v-if="form.fieldType === 'REGION'" label="默认地区">
            <RegionTreeSelect v-model="form.defaultValue" size="small" :disabled="field.systemField" />
          </n-form-item>
          <n-form-item v-else label="默认值">
            <n-input v-model:value="form.defaultValue" :disabled="field.systemField" placeholder="可为空" />
          </n-form-item>

          <div class="switch-grid">
            <label>
              <span>必填</span>
              <n-switch v-model:value="form.required" :disabled="field.systemField" size="small" />
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

          <n-form-item label="备注">
            <n-input v-model:value="form.remark" type="textarea" :rows="3" placeholder="字段说明，业务用户可见" />
          </n-form-item>

          <n-collapse v-if="developerMode" class="advanced-collapse">
            <n-collapse-item title="高级属性" name="advanced">
              <n-grid :cols="2" :x-gap="12">
                <n-form-item-gi label="字段编码">
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
                <n-form-item-gi label="长度">
                  <n-input-number v-model:value="form.length" :min="0" :show-button="false" class="full-input" />
                </n-form-item-gi>
                <n-form-item-gi label="小数位">
                  <n-input-number v-model:value="form.precision" :min="0" :max="8" :show-button="false" class="full-input" />
                </n-form-item-gi>
                <n-form-item-gi label="查询方式">
                  <n-select v-model:value="form.queryType" :options="queryTypeOptions" clearable />
                </n-form-item-gi>
                <n-form-item-gi label="字段状态">
                  <n-select v-model:value="form.fieldStatus" :options="statusOptions" />
                </n-form-item-gi>
              </n-grid>

              <n-form-item v-if="needsDict" label="字典类型">
                <DictTypeSelect v-model:value="form.dictType" :fields="allFields" />
              </n-form-item>

              <n-grid :cols="2" :x-gap="12">
                <n-form-item-gi label="脱敏类型">
                  <n-select v-model:value="form.sensitiveType" :options="sensitiveOptions" clearable />
                </n-form-item-gi>
                <n-form-item-gi label="加密算法">
                  <n-select v-model:value="form.encryptAlgorithm" :options="encryptOptions" clearable />
                </n-form-item-gi>
              </n-grid>
            </n-collapse-item>
          </n-collapse>
        </n-form>
      </div>

      <div class="property-footer">
        <n-button secondary :disabled="!changed" @click="resetForm">
          还原
        </n-button>
        <n-button type="primary" :loading="saving" :disabled="!changed" @click="$emit('save', payload)">
          保存字段
        </n-button>
      </div>
    </template>

    <n-empty v-else description="选择左侧字段后编辑属性" />
  </aside>
</template>

<script setup>
import { computed, reactive, watch } from 'vue'
import DictTypeSelect from '@/components/lowcode-builder/shared/DictTypeSelect.vue'
import FieldTypeSelect from '@/components/lowcode-builder/shared/FieldTypeSelect.vue'
import RegionTreeSelect from '@/components/RegionTreeSelect.vue'

const props = defineProps({
  field: {
    type: Object,
    default: null,
  },
  allFields: {
    type: Array,
    default: () => [],
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

const fieldTypeOptions = [
  { label: '文本', value: 'TEXT' },
  { label: '多行文本', value: 'TEXTAREA' },
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
  { label: '字典选择', value: 'dictSelect' },
  { label: '日期', value: 'date' },
  { label: '日期时间', value: 'datetime' },
  { label: '开关', value: 'switch' },
  { label: '文件上传', value: 'fileUpload' },
  { label: '图片上传', value: 'imageUpload' },
  { label: '人员选择', value: 'userSelect' },
  { label: '部门树', value: 'orgTreeSelect' },
  { label: '地区树', value: 'regionTreeSelect' },
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

const encryptOptions = [
  { label: 'AES', value: 'AES' },
  { label: 'SM4', value: 'SM4' },
]

const payload = computed(() => normalizePayload(form))
const changed = computed(() => JSON.stringify(payload.value) !== baseline)
const needsDict = computed(() => ['DICT', 'RADIO', 'CHECKBOX'].includes(form.fieldType) || ['select', 'radio', 'checkbox', 'dictSelect'].includes(form.componentType))

watch(
  () => props.field,
  () => resetForm(),
  { immediate: true, deep: true },
)

watch(changed, value => emit('dirtyChange', value))

function resetForm() {
  Object.assign(form, createFieldForm(props.field))
  baseline = JSON.stringify(normalizePayload(form))
  emit('dirtyChange', false)
}

function createFieldForm(field) {
  const currentField = field || {}
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
    basicProps: { ...(currentField.basicProps || {}) },
    advancedProps: { ...(currentField.advancedProps || {}) },
  }
}

function normalizePayload(source) {
  const basicProps = {
    ...(source.basicProps || {}),
    placeholder: source.placeholder || '',
  }
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
  }
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
  min-height: 0;
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
  min-height: 0;
  overflow: auto;
  padding: 16px;
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

.advanced-collapse {
  margin-top: 4px;
}

.full-input {
  width: 100%;
}

.property-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  border-top: 1px solid #e5e7eb;
  background: #fff;
  padding: 12px 16px;
}
</style>
