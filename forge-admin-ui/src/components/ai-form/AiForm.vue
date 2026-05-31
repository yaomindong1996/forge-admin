<!--
  AI 表单组件 - 通过 JSON 配置动态渲染表单

  使用示例:
  <AiForm
    :schema="formSchema"
    v-model:value="formData"
    :grid-cols="2"
    @submit="handleSubmit"
  />
-->

<template>
  <n-form
    ref="formRef"
    :model="formValue"
    :rules="formRules"
    :label-placement="labelPlacement"
    :label-width="labelWidth"
    :label-align="labelAlign"
    :size="size"
  >
    <AiFormLayoutNodes
      :nodes="visibleSchema"
      :form-value="formValue"
      :item-context="itemContext"
      :grid-cols="gridCols"
      :x-gap="xGap"
      :y-gap="yGap"
      :show-feedback="showFeedback"
      @field-change="handleFieldChange"
    >
      <!-- 支持自定义插槽 -->
      <template v-for="slotName in Object.keys($slots)" #[slotName]="slotProps">
        <slot :name="slotName" v-bind="slotProps" />
      </template>
    </AiFormLayoutNodes>

    <n-grid
      v-if="$slots.formAction || (enableCollapse && visibleFieldSchema.length > maxVisibleFields)"
      :cols="gridCols"
      :x-gap="xGap"
      :y-gap="yGap"
      class="af-action-grid"
    >
      <!-- 表单操作区域 -->
      <n-gi :span="gridCols" class="af-action-cell">
        <n-space align="baseline">
          <!-- 自定义操作按钮 -->
          <slot name="formAction" :form-data="formValue" />

          <!-- 折叠/展开按钮 -->
          <n-button
            v-if="enableCollapse && visibleFieldSchema.length > maxVisibleFields"
            text
            type="primary"
            @click="toggleCollapse"
          >
            {{ isCollapsed ? '展开' : '收起' }}
            <template #icon>
              <n-icon>
                <component :is="isCollapsed ? ChevronDownOutline : ChevronUpOutline" />
              </n-icon>
            </template>
          </n-button>
        </n-space>
      </n-gi>
    </n-grid>

    <!-- 表单操作按钮 -->
    <n-space v-if="showActions" justify="center" :style="{ marginTop: '24px' }">
      <n-button v-if="showSubmit" type="primary" @click="handleSubmit">
        {{ submitText }}
      </n-button>
      <n-button v-if="showReset" @click="handleReset">
        {{ resetText }}
      </n-button>
      <n-button v-if="showCancel" @click="handleCancel">
        {{ cancelText }}
      </n-button>
    </n-space>
  </n-form>
</template>

<script setup>
import { ChevronDownOutline, ChevronUpOutline } from '@vicons/ionicons5'
import { computed, ref, watch } from 'vue'
import AiFormLayoutNodes from './AiFormLayoutNodes.vue'

const props = defineProps({
  // 表单配置 schema
  schema: {
    type: Array,
    required: true,
    default: () => [],
  },
  // 表单数据 (v-model)
  value: {
    type: Object,
    default: () => ({}),
  },
  // 上下文数据，传递给字段的回调函数
  context: {
    type: Object,
    default: () => ({}),
  },
  // 表单布局
  labelPlacement: {
    type: String,
    default: 'left', // 'left' | 'top'
  },
  labelWidth: {
    type: [String, Number],
    default: 'auto',
  },
  labelAlign: {
    type: String,
    default: 'right',
    validator: value => ['left', 'right'].includes(value),
  },
  // 表单尺寸
  size: {
    type: String,
    default: 'medium', // 'small' | 'medium' | 'large'
  },
  // 栅格布局
  gridCols: {
    type: Number,
    default: 1,
  },
  xGap: {
    type: Number,
    default: 12,
  },
  yGap: {
    type: Number,
    default: 0,
  },
  // 操作按钮
  showActions: {
    type: Boolean,
    default: true,
  },
  showSubmit: {
    type: Boolean,
    default: true,
  },
  showReset: {
    type: Boolean,
    default: true,
  },
  showCancel: {
    type: Boolean,
    default: false,
  },
  submitText: {
    type: String,
    default: '提交',
  },
  resetText: {
    type: String,
    default: '重置',
  },
  cancelText: {
    type: String,
    default: '取消',
  },
  // 是否启用折叠功能
  enableCollapse: {
    type: Boolean,
    default: false,
  },
  // 最大显示字段数（超过时显示折叠按钮）
  maxVisibleFields: {
    type: Number,
    default: 6,
  },
  // 是否显示验证反馈（默认显示）
  showFeedback: {
    type: Boolean,
    default: true,
  },
})

const emit = defineEmits(['update:value', 'submit', 'reset', 'cancel'])

const formRef = ref(null)
const formValue = ref({})
const isCollapsed = ref(true)

// 初始化表单数据
watch(() => props.value, (newVal) => {
  formValue.value = { ...newVal }
}, { immediate: true, deep: true })

function isFieldVisible(field) {
  if (field.hidden || field.visible === false) {
    return false
  }

  if (typeof field.vIf === 'function') {
    return field.vIf(formValue.value)
  }

  if (typeof field.vIf === 'boolean') {
    return field.vIf
  }

  return true
}

const conditionVisibleSchema = computed(() => {
  return filterVisibleNodes(props.schema)
})

const allFieldSchema = computed(() => flattenFieldNodes(props.schema))
const visibleFieldSchema = computed(() => flattenFieldNodes(conditionVisibleSchema.value))

// 生成表单验证规则
const formRules = computed(() => {
  const rules = {}
  visibleFieldSchema.value.forEach((field) => {
    if (field.rules) {
      rules[field.field] = normalizeFieldRules(field, field.rules)
    }
    else if (field.required) {
      const inputTypes = ['input', 'textarea', 'number', 'inputNumber']
      const isNumericType = field.type === 'number' || field.type === 'inputNumber'
      const isDateType = isDateLikeType(field.type)
      const isSelectionType = isSelectionLikeType(field.type)
      const rule = {
        required: true,
        message: field.requiredMessage || `请${inputTypes.includes(field.type) ? '输入' : '选择'}${field.label}`,
        trigger: field.trigger || (isNumericType || isDateType || isSelectionType ? 'change' : ['blur', 'change']),
      }
      // number/date/treeSelect 等类型需要自定义 validator，避免 0、数字 ID、数组等有效值被误判为空
      if (isNumericType || isDateType || isSelectionType) {
        rule.validator = (_rule, value) => {
          if (!hasFormValue(value)) {
            return new Error(rule.message)
          }
          return true
        }
        delete rule.required
      }
      rules[field.field] = rule
    }
  })
  return rules
})

function isDateLikeType(type) {
  return ['date', 'datetime', 'daterange', 'datetimerange', 'month', 'year', 'time', 'timerange'].includes(type)
}

function isSelectionLikeType(type) {
  const normalizedType = normalizeSelectionType(type)
  return [
    'select',
    'dictSelect',
    'radio',
    'checkbox',
    'cascader',
    'treeSelect',
    'orgTreeSelect',
    'regionTreeSelect',
    'userSelect',
    'transfer',
    'upload',
    'imageUpload',
    'fileUpload',
  ].includes(normalizedType)
}

function normalizeSelectionType(type) {
  const value = String(type || '')
  if (['orgSelect', 'organizationSelect', 'departmentSelect', 'departmentTreeSelect', 'deptSelect', 'deptTreeSelect', 'elTreeSelect', 'orgName', 'deptName', 'forgeOrgTreeSelect'].includes(value))
    return 'orgTreeSelect'
  if (['userPicker', 'user', 'userName', 'sysUserSelect', 'forgeUserSelect'].includes(value))
    return 'userSelect'
  return value
}

function hasFormValue(value) {
  if (Array.isArray(value))
    return value.length > 0 && value.every(item => item !== null && item !== undefined && item !== '')
  return value !== null && value !== undefined && value !== ''
}

function normalizeFieldRules(field, fieldRules) {
  const rules = Array.isArray(fieldRules) ? fieldRules : [fieldRules]
  if (!isDateLikeType(field.type) && !isSelectionLikeType(field.type) && field.type !== 'number' && field.type !== 'inputNumber')
    return fieldRules

  return rules.map((sourceRule) => {
    if (!sourceRule?.required || sourceRule.validator)
      return sourceRule
    const rule = { ...sourceRule }
    rule.validator = (_rule, value) => {
      if (!hasFormValue(value))
        return new Error(rule.message || field.requiredMessage || `请选择${field.label}`)
      return true
    }
    rule.trigger = rule.trigger || 'change'
    delete rule.required
    return rule
  })
}

// 可见的表单字段
const visibleSchema = computed(() => {
  let fields = conditionVisibleSchema.value

  if (!hasLayoutNodes(fields))
    fields = removeEmptyDividers(fields)

  // 应用折叠逻辑
  if (props.enableCollapse && !hasLayoutNodes(fields) && fields.length > props.maxVisibleFields) {
    fields = isCollapsed.value
      ? fields.slice(0, props.maxVisibleFields)
      : fields
  }

  // 合并 showFeedback 到每个字段
  return applyShowFeedback(fields)
})

const itemContext = computed(() => ({
  ...props.context,
  schema: visibleFieldSchema.value,
  allSchema: allFieldSchema.value,
  patchFormData,
}))

// 字段值变化
async function handleFieldChange(field, value) {
  formValue.value = {
    ...formValue.value,
    [field]: value,
  }
  emit('update:value', { ...formValue.value })

  // 触发字段变化事件
  const fieldConfig = allFieldSchema.value.find(f => f.field === field)
  if (fieldConfig?.onChange) {
    await fieldConfig.onChange({
      value,
      field: fieldConfig,
      formData: formValue.value,
      context: props.context,
    })
    formValue.value = { ...formValue.value }
    emit('update:value', { ...formValue.value })
  }
}

function patchFormData(patch = {}) {
  const next = {
    ...formValue.value,
  }
  Object.entries(patch).forEach(([key, value]) => {
    if (value === undefined)
      delete next[key]
    else
      next[key] = value
  })
  formValue.value = next
  emit('update:value', { ...formValue.value })
}

// 提交表单
async function handleSubmit() {
  try {
    await formRef.value?.validate()
    emit('submit', { ...formValue.value })
  }
  catch (error) {
    console.warn('表单验证失败:', error)
  }
}

// 重置表单
function handleReset() {
  formRef.value?.restoreValidation()
  const resetData = {}
  allFieldSchema.value.forEach((field) => {
    resetData[field.field] = field.defaultValue ?? null
  })
  formValue.value = resetData
  emit('update:value', { ...resetData })
  emit('reset')
}

// 取消
function handleCancel() {
  emit('cancel')
}

// 切换折叠状态
function toggleCollapse() {
  isCollapsed.value = !isCollapsed.value
}

// 暴露方法给父组件
defineExpose({
  validate: () => formRef.value?.validate(),
  restoreValidation: () => formRef.value?.restoreValidation(),
  reset: handleReset,
  getFormData: () => ({ ...formValue.value }),
})

function filterVisibleNodes(nodes = []) {
  return (Array.isArray(nodes) ? nodes : [])
    .map((node) => {
      if (!node || typeof node !== 'object')
        return null
      if (isRuntimeLayoutNode(node)) {
        const children = filterVisibleNodes(node.children || [])
        if (!children.length && !['divider'].includes(node.nodeType))
          return null
        return {
          ...node,
          children,
        }
      }
      return isFieldVisible(node) ? node : null
    })
    .filter(Boolean)
}

function flattenFieldNodes(nodes = []) {
  const result = []
  const walk = (items = []) => {
    ;(Array.isArray(items) ? items : []).forEach((node) => {
      if (!node || typeof node !== 'object')
        return
      if (isRuntimeLayoutNode(node)) {
        walk(node.children || [])
        return
      }
      if (node.field)
        result.push(node)
    })
  }
  walk(nodes)
  return result
}

function isRuntimeLayoutNode(node = {}) {
  return node.nodeType && node.nodeType !== 'field'
}

function hasLayoutNodes(nodes = []) {
  return (Array.isArray(nodes) ? nodes : []).some(node => isRuntimeLayoutNode(node))
}

function removeEmptyDividers(fields = []) {
  const result = []
  for (let i = 0; i < fields.length; i += 1) {
    const field = fields[i]
    if (field.type !== 'divider') {
      result.push(field)
      continue
    }
    const hasNextField = fields.slice(i + 1).some(item => item.type !== 'divider')
    if (hasNextField)
      result.push(field)
  }
  return result
}

function applyShowFeedback(nodes = []) {
  return (Array.isArray(nodes) ? nodes : []).map((node) => {
    if (isRuntimeLayoutNode(node)) {
      return {
        ...node,
        children: applyShowFeedback(node.children || []),
      }
    }
    return {
      ...node,
      showFeedback: node.showFeedback ?? props.showFeedback,
    }
  })
}
</script>

<style scoped>
.af-action-grid {
  margin-top: 2px;
}

.af-action-cell {
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  min-width: 0;
}
</style>
