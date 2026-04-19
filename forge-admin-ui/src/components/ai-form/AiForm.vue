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
    :size="size"
  >
    <n-grid :cols="gridCols" :x-gap="xGap" :y-gap="yGap">
      <n-gi
        v-for="field in visibleSchema"
        :key="field.field"
        :span="field.span || 1"
      >
        <AiFormItem
          :field="field"
          :value="formValue[field.field]"
          :form-data="formValue"
          :context="context"
          @update:value="handleFieldChange(field.field, $event)"
        >
          <!-- 支持自定义插槽 -->
          <template v-for="slotName in Object.keys($slots)" #[slotName]="slotProps">
            <slot :name="slotName" v-bind="slotProps" />
          </template>
        </AiFormItem>
      </n-gi>

      <!-- 表单操作区域 -->
      <n-gi
        v-if="$slots.formAction || (enableCollapse && schema.length > maxVisibleFields)"
        :span="formActionSpan"
        style="display: flex; align-items: baseline; justify-content: flex-end"
      >
        <n-space align="baseline">
          <!-- 自定义操作按钮 -->
          <slot name="formAction" :form-data="formValue" />

          <!-- 折叠/展开按钮 -->
          <n-button
            v-if="enableCollapse && schema.length > maxVisibleFields"
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
import AiFormItem from './AiFormItem.vue'

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

// 生成表单验证规则
const formRules = computed(() => {
  const rules = {}
  props.schema.forEach((field) => {
    if (field.rules) {
      rules[field.field] = field.rules
    }
    else if (field.required) {
      rules[field.field] = {
        required: true,
        message: field.requiredMessage || `请${field.type === 'input' ? '输入' : '选择'}${field.label}`,
        trigger: field.trigger || ['blur', 'change'],
      }
    }
  })
  return rules
})

// 可见的表单字段
const visibleSchema = computed(() => {
  let fields = props.schema

  // 应用 vIf 条件过滤
  fields = fields.filter((field) => {
    // 如果有 vIf 函数，执行它
    if (typeof field.vIf === 'function') {
      return field.vIf(formValue.value)
    }

    // 如果有 vIf 布尔值
    if (typeof field.vIf === 'boolean') {
      return field.vIf
    }

    // 默认显示
    return true
  })

  // 移除后面没有字段的 divider
  const fieldsWithoutEmptyDividers = []
  for (let i = 0; i < fields.length; i++) {
    const field = fields[i]

    // 如果是 divider，检查后面是否有非 divider 字段
    if (field.type === 'divider') {
      // 查找下一个非 divider 字段
      let hasNextField = false
      for (let j = i + 1; j < fields.length; j++) {
        if (fields[j].type !== 'divider') {
          hasNextField = true
          break
        }
      }

      // 只有当后面有字段时才添加这个 divider
      if (hasNextField) {
        fieldsWithoutEmptyDividers.push(field)
      }
    }
    else {
      fieldsWithoutEmptyDividers.push(field)
    }
  }

  fields = fieldsWithoutEmptyDividers

  // 应用折叠逻辑
  if (props.enableCollapse && fields.length > props.maxVisibleFields) {
    fields = isCollapsed.value
      ? fields.slice(0, props.maxVisibleFields)
      : fields
  }

  // 合并 showFeedback 到每个字段
  return fields.map(field => ({
    ...field,
    showFeedback: field.showFeedback ?? props.showFeedback,
  }))
})

// 计算 formAction 所占的 span
const formActionSpan = computed(() => {
  if (!props.enableCollapse && !props.$slots?.formAction) {
    return 0
  }

  // 计算已显示字段占用的总 span
  const totalSpan = visibleSchema.value.reduce((sum, field) => {
    return sum + (field.span || 1)
  }, 0)

  // 计算剩余的 span
  const remainingSpan = (props.gridCols - (totalSpan % props.gridCols)) % props.gridCols

  return remainingSpan || props.gridCols
})

// 字段值变化
function handleFieldChange(field, value) {
  formValue.value[field] = value
  emit('update:value', { ...formValue.value })

  // 触发字段变化事件
  const fieldConfig = props.schema.find(f => f.field === field)
  if (fieldConfig?.onChange) {
    fieldConfig.onChange({
      value,
      field: fieldConfig,
      formData: formValue.value,
      context: props.context,
    })
  }
}

// 提交表单
async function handleSubmit() {
  try {
    await formRef.value?.validate()
    emit('submit', { ...formValue.value })
  }
  catch (error) {
    console.log('表单验证失败:', error)
  }
}

// 重置表单
function handleReset() {
  formRef.value?.restoreValidation()
  const resetData = {}
  props.schema.forEach((field) => {
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
</script>
