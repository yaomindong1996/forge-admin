<template>
  <div class="form-preview">
    <n-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-placement="left"
      label-width="auto"
    >
      <template v-for="item in actualFormItems" :key="item.field">
        <FormItemRender
          :model-value="formData[item.field]"
          :item="item"
          @update:model-value="handleValueUpdate(item.field, $event)"
        />
      </template>
    </n-form>

    <div class="form-actions">
      <n-button @click="handleReset">
        重置
      </n-button>
      <n-button type="primary" @click="handleSubmit">
        提交
      </n-button>
    </div>

    <n-divider>表单数据</n-divider>
    <n-code :code="JSON.stringify(formData, null, 2)" language="json" />
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import FormItemRender from './FormItemRender.vue'

const props = defineProps({
  formItems: {
    type: Array,
    default: () => [],
  },
  schema: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['submit', 'change'])

// 计算实际使用的表单项
const actualFormItems = computed(() => {
  return props.formItems.length > 0 ? props.formItems : props.schema
})

const formRef = ref(null)

// 表单数据
const formData = reactive({})

// 表单规则
const formRules = computed(() => {
  const rules = {}
  actualFormItems.value.forEach((item) => {
    if (item.required) {
      rules[item.field] = [{
        required: true,
        message: item.rules?.[0]?.message || `请输入${item.label}`,
        trigger: ['blur', 'change'],
      }]
    }
  })
  return rules
})

// 初始化表单数据
function initFormData() {
  actualFormItems.value.forEach((item) => {
    if (item.defaultValue !== undefined && item.defaultValue !== null) {
      formData[item.field] = item.defaultValue
    }
    else {
      // 根据类型设置默认值
      switch (item.type) {
        case 'checkbox':
          formData[item.field] = []
          break
        case 'switch':
          formData[item.field] = false
          break
        case 'inputNumber':
        case 'slider':
        case 'rate':
          formData[item.field] = null
          break
        default:
          formData[item.field] = null
      }
    }
  })
}

// 处理值更新
function handleValueUpdate(field, value) {
  formData[field] = value
  emit('change', { field, value, formData: { ...formData } })
}

// 处理值变化
function handleValueChange(field, value) {
  emit('change', { field, value, formData: { ...formData } })
}

// 重置表单
function handleReset() {
  formRef.value?.restoreValidation()
  initFormData()
}

// 提交表单
async function handleSubmit() {
  try {
    await formRef.value?.validate()
    emit('submit', { ...formData })
  }
  catch (errors) {
    console.log('验证失败:', errors)
  }
}

// 获取表单数据
function getFormData() {
  return { ...formData }
}

// 设置表单数据
function setFormData(data) {
  Object.assign(formData, data)
}

// 验证表单
async function validate() {
  return formRef.value?.validate()
}

// 监听表单项变化
watch(() => actualFormItems.value, () => {
  initFormData()
}, { immediate: true, deep: true })

// 暴露方法
defineExpose({
  getFormData,
  setFormData,
  validate,
  reset: handleReset,
})
</script>

<style scoped>
.form-preview {
  padding: 16px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
}
</style>
