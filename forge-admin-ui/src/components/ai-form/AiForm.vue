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
      class="ai-form"
      :model="formValue"
      :rules="formRules"
      :label-placement="labelPlacement"
      :label-width="labelWidth"
      :label-align="labelAlign"
      :size="size"
  >
    <div class="ai-form-body" :class="{ 'ai-form-body--with-nav': showSectionNav }">
      <nav v-if="showSectionNav" class="ai-form-section-nav" aria-label="表单分组导航">
        <button
            v-for="item in sectionNavItems"
            :key="item.id"
            type="button"
            class="ai-form-section-nav__item"
            :class="{ 'is-active': activeSectionId === item.id }"
            @click="scrollToSection(item.id)"
        >
          <span class="ai-form-section-nav__dot" aria-hidden="true" />
          <span class="ai-form-section-nav__text">{{ item.label }}</span>
        </button>
      </nav>

      <div class="ai-form-content">
        <AiFormLayoutNodes
            :nodes="visibleSchema"
            :form-value="formValue"
            :item-context="itemContext"
            :grid-cols="gridCols"
            :x-gap="xGap"
            :y-gap="yGap"
            :show-feedback="showFeedback"
            @field-change="handleFieldChange"
            @node-action="handleNodeAction"
        >
          <!-- 支持自定义插槽 -->
          <template v-for="slotName in Object.keys($slots)" #[slotName]="slotProps">
            <slot :name="slotName" v-bind="slotProps" />
          </template>

          <template v-if="hasInlineActions" #gridAppend>
            <!-- 表单操作区域 -->
            <n-gi :span="actionCellSpan" class="af-action-cell">
              <n-space align="baseline" :wrap="false">
                <!-- 自定义操作按钮 -->
                <slot name="formAction" :form-data="formValue" />

                <!-- 折叠/展开按钮 -->
                <n-button
                    v-if="showCollapseToggle"
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
          </template>
        </AiFormLayoutNodes>
      </div>
    </div>

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

    <n-modal
        v-model:show="actionModalVisible"
        preset="card"
        :title="actionModalTitle"
        :bordered="false"
        class="ai-form-action-modal"
        style="width: min(760px, 92vw)"
    >
      <AiForm
          v-if="actionModalSchema.length"
          v-model:value="actionModalValue"
          :schema="actionModalSchema"
          :label-placement="actionModalLayout.labelPlacement || labelPlacement"
          :label-width="actionModalLayout.labelWidth ?? labelWidth"
          :label-align="actionModalLayout.labelAlign || labelAlign"
          :size="actionModalLayout.size || size"
          :grid-cols="actionModalLayout.gridCols || actionModalLayout.gridColumns || gridCols"
          :x-gap="actionModalLayout.xGap || actionModalLayout.columnGap || xGap"
          :y-gap="actionModalLayout.yGap || actionModalLayout.rowGap || yGap"
          :show-feedback="actionModalLayout.showFeedback !== false"
          :show-actions="false"
          :context="itemContext"
          :form-assets="formAssets"
      />
      <n-empty v-else description="未找到可渲染的弹窗表单" />
    </n-modal>
  </n-form>
</template>

<script setup>
import { ChevronDownOutline, ChevronUpOutline } from '@vicons/ionicons5'
import { computed, nextTick, ref, useSlots, watch } from 'vue'
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
  formAssets: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:value', 'submit', 'reset', 'cancel', 'nodeAction'])
const slots = useSlots()

const formRef = ref(null)
const formValue = ref({})
const isCollapsed = ref(true)
const activeSectionId = ref('')
const actionModalVisible = ref(false)
const actionModalTitle = ref('业务弹窗')
const actionModalSchema = ref([])
const actionModalValue = ref({})
const actionModalLayout = ref({})

// 初始化表单数据
watch(() => props.value, (newVal) => {
  formValue.value = { ...newVal }
}, { immediate: true, deep: true })

function isFieldVisible(field) {
  if (field.hidden || field.visible === false) {
    return false
  }

  if (typeof field.vIf === 'function') {
    return field.vIf(formValue.value, props.context)
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
        key: field.field,
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
  const needsCustomEmptyValidator = isDateLikeType(field.type) || isSelectionLikeType(field.type) || field.type === 'number' || field.type === 'inputNumber'

  const normalizedRules = rules.map((sourceRule) => {
    const withKeyRule = { ...(sourceRule || {}), key: sourceRule?.key || field.field }
    if (!needsCustomEmptyValidator || !sourceRule?.required || sourceRule.validator)
      return withKeyRule
    const rule = withKeyRule
    rule.validator = (_rule, value) => {
      if (!hasFormValue(value))
        return new Error(rule.message || field.requiredMessage || `请选择${field.label}`)
      return true
    }
    rule.trigger = rule.trigger || 'change'
    delete rule.required
    return rule
  })

  return Array.isArray(fieldRules) ? normalizedRules : normalizedRules[0]
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

  return applySectionNavMarkers(applyShowFeedback(fields))
})

const itemContext = computed(() => ({
  ...props.context,
  schema: visibleFieldSchema.value,
  allSchema: allFieldSchema.value,
  formAssets: resolveFormAssets(),
  patchFormData,
}))

const showCollapseToggle = computed(() => props.enableCollapse && visibleFieldSchema.value.length > props.maxVisibleFields)
const sectionNavItems = computed(() => collectSectionNavItems(visibleSchema.value))
const showSectionNav = computed(() => sectionNavItems.value.length >= 3)
const hasInlineActions = computed(() => !!slots.formAction || showCollapseToggle.value)
const actionCellSpan = computed(() => {
  const cols = Math.max(1, Number(props.gridCols) || 1)

  if (hasLayoutNodes(visibleSchema.value))
    return cols

  const usedCols = visibleSchema.value.reduce((total, node) => {
    const span = Math.max(1, Math.min(cols, Number(node?.span || 1)))
    return total + span
  }, 0)
  const remainder = usedCols % cols
  return remainder === 0 ? cols : cols - remainder
})

watch(sectionNavItems, (items) => {
  if (!items.length) {
    activeSectionId.value = ''
    return
  }
  if (!items.some(item => item.id === activeSectionId.value))
    activeSectionId.value = items[0].id
}, { immediate: true })

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

  await validateChangedField(field)
}

async function validateChangedField(field) {
  if (!field || !formRules.value[field])
    return
  await nextTick()
  try {
    await formRef.value?.validate(undefined, rule => rule?.key === field)
  }
  catch {
    // 单字段重验只用于同步清理或刷新提示，不阻断用户继续填写。
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

function handleNodeAction(payload = {}) {
  emit('nodeAction', payload)
  const event = payload.event || {}
  if (event.action === 'setValue' && event.targetId) {
    patchFormData({ [event.targetId]: event.value ?? event.targetValue ?? event.whenValue ?? '' })
    return
  }
  if (event.action !== 'openModal')
    return
  const asset = findFormAsset(event.modalFormKey)
  actionModalTitle.value = event.modalTitle || asset?.formName || '业务弹窗'
  actionModalLayout.value = asset?.schema?.layout || {}
  actionModalSchema.value = buildActionModalSchema(asset?.schema)
  actionModalValue.value = buildDefaultModalValue(actionModalSchema.value)
  actionModalVisible.value = true
}

function resolveFormAssets() {
  const contextAssets = Array.isArray(props.context?.formAssets) ? props.context.formAssets : []
  return props.formAssets.length ? props.formAssets : contextAssets
}

function findFormAsset(formKey = '') {
  const assets = resolveFormAssets()
  if (!formKey || formKey === 'current')
    return null
  return assets.find(asset => asset?.formKey === formKey || asset?.schema?.formKey === formKey) || null
}

function buildActionModalSchema(schema = {}) {
  if (!schema)
    return []
  const components = Array.isArray(schema) ? schema : schema.components || []
  return components.map(normalizeDesignerComponentForRuntime).filter(Boolean)
}

function normalizeDesignerComponentForRuntime(component = {}) {
  if (!component || typeof component !== 'object')
    return null
  const componentKey = component.componentKey || component.type || 'input'
  const nodeType = resolveDesignerNodeType(componentKey)
  const children = Array.isArray(component.children)
    ? component.children.map(normalizeDesignerComponentForRuntime).filter(Boolean)
    : []
  const base = {
    ...component,
    ...(component.props || {}),
    componentKey,
    type: normalizeDesignerFieldType(componentKey),
    component: normalizeDesignerFieldType(componentKey),
    field: component.fieldBinding?.fieldCode || component.field || component.id,
    prop: component.fieldBinding?.fieldCode || component.field || component.id,
    label: component.label,
    span: component.layout?.span ?? component.span,
    required: Boolean(component.validation?.required),
    requiredMessage: component.validation?.requiredMessage,
    hidden: Boolean(component.visibility?.hidden),
    readonly: Boolean(component.visibility?.readonly),
    disabled: Boolean(component.visibility?.readonly || component.props?.disabled),
    props: {
      ...(component.props || {}),
      disabled: Boolean(component.visibility?.readonly || component.props?.disabled),
    },
    children,
  }
  if (nodeType) {
    base.nodeType = nodeType
    if (nodeType !== 'field')
      delete base.field
  }
  if (Array.isArray(component.validation?.rules) && component.validation.rules.length)
    base.rules = component.validation.rules.map(rule => ({ ...rule }))
  return base
}

function resolveDesignerNodeType(componentKey = '') {
  if (['row', 'fcRow'].includes(componentKey))
    return 'row'
  if (componentKey === 'col')
    return 'col'
  if (['card', 'elCard'].includes(componentKey))
    return 'card'
  if (['tabs', 'elTabs'].includes(componentKey))
    return 'tabs'
  if (['tabPane', 'elTabPane'].includes(componentKey))
    return 'tabPane'
  if (['collapse', 'elCollapse'].includes(componentKey))
    return 'collapse'
  if (['collapseItem', 'elCollapseItem'].includes(componentKey))
    return 'collapseItem'
  if (['divider', 'elDivider', 'AiFormSectionTitle', 'aiFormSectionTitle', 'formSectionTitle', 'FormSectionTitle'].includes(componentKey))
    return 'divider'
  if (['title', 'fcTitle', 'groupTitle'].includes(componentKey))
    return 'groupTitle'
  if (['button', 'table', 'tableGrid'].includes(componentKey))
    return componentKey
  if (['AiCrudPage', 'aiCrudPage', 'crud', 'crudBlock'].includes(componentKey))
    return 'AiCrudPage'
  return ''
}

function normalizeDesignerFieldType(componentKey = '') {
  if (['inputNumber', 'integer', 'money'].includes(componentKey))
    return 'number'
  if (componentKey === 'colorPicker')
    return 'color'
  if (componentKey === 'upload')
    return 'fileUpload'
  return componentKey || 'input'
}

function buildDefaultModalValue(schema = []) {
  const result = {}
  flattenFieldNodes(schema).forEach((field) => {
    result[field.field] = field.defaultValue ?? field.props?.defaultValue ?? null
  })
  return result
}

// 提交表单
async function handleSubmit() {
  try {
    await validateForm()
    emit('submit', { ...formValue.value })
  }
  catch (error) {
    console.warn('表单验证失败:', error)
  }
}

async function validateForm() {
  try {
    return await formRef.value?.validate()
  }
  catch (error) {
    await revealFirstValidationError(error)
    throw error
  }
}

async function revealFirstValidationError(error) {
  const field = resolveFirstValidationField(error)
  if (props.enableCollapse && isCollapsed.value && field && !findFieldElement(field)) {
    isCollapsed.value = false
    await nextTick()
  }

  await nextTick()

  const target = field ? findFieldElement(field) : findFirstErrorElement()
  if (!target)
    return

  target.scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'nearest' })
  target.classList.remove('ai-form-field-flash')
  void target.offsetWidth
  target.classList.add('ai-form-field-flash')

  window.setTimeout(() => {
    focusFirstControl(target)
  }, 260)
  window.setTimeout(() => {
    target.classList.remove('ai-form-field-flash')
  }, 3200)
}

function resolveFirstValidationField(error) {
  const errors = flattenValidationErrors(error)
  const first = errors.find(item => item && typeof item === 'object')
  const field = first?.field || first?.path || first?.key || first?.fullField
  return Array.isArray(field) ? field.join('.') : field ? String(field) : ''
}

function scrollToSection(sectionId) {
  const target = findSectionElement(sectionId)
  if (!target)
    return
  activeSectionId.value = sectionId
  target.scrollIntoView({ behavior: 'smooth', block: 'start', inline: 'nearest' })
}

function flattenValidationErrors(source, result = []) {
  if (Array.isArray(source)) {
    source.forEach(item => flattenValidationErrors(item, result))
    return result
  }
  if (source && typeof source === 'object') {
    result.push(source)
    if (Array.isArray(source.errors))
      flattenValidationErrors(source.errors, result)
    if (Array.isArray(source.inner))
      flattenValidationErrors(source.inner, result)
    if (Array.isArray(source.children))
      flattenValidationErrors(source.children, result)
  }
  return result
}

function getFormElement() {
  return formRef.value?.$el || null
}

function findFieldElement(field) {
  const root = getFormElement()
  if (!root || !field)
    return null
  return Array.from(root.querySelectorAll('[data-ai-field]'))
      .find(element => element.getAttribute('data-ai-field') === String(field)) || null
}

function findSectionElement(sectionId) {
  const root = getFormElement()
  if (!root || !sectionId)
    return null
  return root.querySelector(`[data-ai-section="${sectionId}"]`)
}

function findFirstErrorElement() {
  const root = getFormElement()
  if (!root)
    return null
  const errorElement = root.querySelector('.n-form-item--error, .n-form-item--error-status, .n-form-item-feedback__line:not(:empty)')
  return errorElement?.closest?.('[data-ai-field]') || errorElement || null
}

function focusFirstControl(target) {
  const control = target.querySelector('input, textarea, button, [tabindex]:not([tabindex="-1"])')
  control?.focus?.({ preventScroll: true })
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
  validate: validateForm,
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
          if (!children.length && !isStandaloneRuntimeLayoutNode(node))
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

function applySectionNavMarkers(nodes = []) {
  let sectionIndex = 0
  const walk = (items = []) => (Array.isArray(items) ? items : []).map((node) => {
    if (!node || typeof node !== 'object')
      return node
    if (isDividerNode(node)) {
      sectionIndex += 1
      return {
        ...node,
        __sectionId: node.__sectionId || `ai-form-section-${sectionIndex}`,
      }
    }
    if (isRuntimeLayoutNode(node)) {
      return {
        ...node,
        children: walk(node.children || []),
      }
    }
    return node
  })
  return walk(nodes)
}

function collectSectionNavItems(nodes = []) {
  const items = []
  const walk = (list = []) => {
    ;(Array.isArray(list) ? list : []).forEach((node) => {
      if (!node || typeof node !== 'object')
        return
      if (isDividerNode(node) && node.__sectionId) {
        items.push({
          id: node.__sectionId,
          label: node.label || node.props?.title || '分组信息',
        })
        return
      }
      if (isRuntimeLayoutNode(node))
        walk(node.children || [])
    })
  }
  walk(nodes)
  return items
}

function isDividerNode(node = {}) {
  return !isLegacyGroupTitleNode(node) && ['divider', 'elDivider', 'AiFormSectionTitle', 'aiFormSectionTitle', 'formSectionTitle', 'FormSectionTitle']
    .includes(node.type || node.nodeType || node.componentKey)
}

function isGroupTitleNode(node = {}) {
  return isLegacyGroupTitleNode(node) || ['title', 'fcTitle', 'sectionTitle', 'groupTitle', 'groupHeader', 'GroupHeader', 'titleBlock', 'section']
    .includes(node.type || node.nodeType || node.componentKey)
}

function isStandaloneRuntimeLayoutNode(node = {}) {
  return isDividerNode(node) || isGroupTitleNode(node) || isActionRuntimeNode(node)
}

function isActionRuntimeNode(node = {}) {
  return ['button', 'table', 'AiCrudPage', 'aiCrudPage', 'crud', 'crudBlock']
    .includes(node.type || node.nodeType || node.componentKey)
}

function isLegacyGroupTitleNode(node = {}) {
  const props = node.props || {}
  return node.nodeType === 'divider'
    && !node.componentKey
    && Object.prototype.hasOwnProperty.call(props, 'description')
    && !Object.prototype.hasOwnProperty.call(props, 'title')
}
</script>

<style scoped>
.ai-form {
  min-width: 0;
}

.ai-form-body {
  min-width: 0;
}

.ai-form-body--with-nav {
  display: grid;
  grid-template-columns: 128px minmax(0, 1fr);
  gap: 20px;
  align-items: start;
}

.ai-form-content {
  min-width: 0;
}

.ai-form-section-nav {
  position: sticky;
  top: 8px;
  display: flex;
  flex-direction: column;
  gap: 3px;
  max-height: min(56vh, 520px);
  overflow-y: auto;
  padding: 4px 0 4px 6px;
  border-left: 1px solid rgba(22, 93, 255, 0.12);
}

.ai-form-section-nav__item {
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr);
  gap: 7px;
  align-items: center;
  width: 100%;
  min-height: 28px;
  padding: 4px 8px 4px 4px;
  border: 0;
  border-radius: 5px;
  background: transparent;
  color: #64748b;
  font-size: 12px;
  line-height: 18px;
  text-align: left;
  cursor: pointer;
  transition: color 0.16s ease, background-color 0.16s ease;
}

.ai-form-section-nav__item:hover,
.ai-form-section-nav__item.is-active {
  background: rgba(22, 93, 255, 0.06);
  color: #165dff;
}

.ai-form-section-nav__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  opacity: 0.45;
}

.ai-form-section-nav__item.is-active .ai-form-section-nav__dot {
  opacity: 1;
}

.ai-form-section-nav__text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.af-action-cell {
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  min-width: 0;
}

.af-action-cell :deep(.n-space) {
  flex-wrap: nowrap !important;
}

:global(.dark) .ai-form-section-nav {
  border-left-color: rgba(64, 128, 255, 0.22);
}

:global(.dark) .ai-form-section-nav__item {
  color: #94a3b8;
}

:global(.dark) .ai-form-section-nav__item:hover,
:global(.dark) .ai-form-section-nav__item.is-active {
  background: rgba(64, 128, 255, 0.14);
  color: #94bfff;
}

@media (max-width: 760px) {
  .ai-form-body--with-nav {
    display: block;
  }

  .ai-form-section-nav {
    display: none;
  }
}
</style>
